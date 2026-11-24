# HttpUtil TLS 1.2 Fallback Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a split-timeout, `EventListener`-guarded TLS 1.2 fallback with a bounded `(host, port, isProxied)` TTL cache to `HttpUtil.kt` across `getUrlContentWithUserAgent`, `getUrlContent`, and `downloadToFile`.

**Architecture:** Extract a shared internal execution helper (`executeWithTlsFallback`) in `HttpUtil.kt` used by all three fetch methods. For `https://` requests, cap the initial TLS 1.3 attempt timeout, track handshake and header progress via a per-call `EventListener`, retry once with `TlsVersion.TLS_1_2` (+ `ConnectionSpec.CLEARTEXT`) only on pre-header, non-certificate TLS/timeout failures when `!secureConnectCompleted` (or retry with default TLS 1.3 when `secureConnectCompleted` already finished), and cache confirmed TLS 1.2 hosts in a bounded 10-minute in-memory TTL cache with self-healing eviction.

**Tech Stack:** Kotlin, OkHttp 5.1.0 (`ConnectionSpec`, `TlsVersion`, `EventListener`), JUnit 5 (`org.junit.jupiter.api.*`), Gradle (`./gradlew :app:testPlaystoreDebugUnitTest`).

**Spec:** [docs/superpowers/specs/2026-11-24-http-tls12-fallback-design.md](file:///root/.gemini/antigravity-cli/worktrees/KUMO/brainstorming_superpowers/docs/superpowers/specs/2026-11-24-http-tls12-fallback-design.md)

## Global Constraints

- Standard OkHttp 5.1.0 APIs only; do not add dependencies or modify `CoreConfigManager.kt`, `AngConfigManager.kt`, or routing presets.
- Preserve `ConnectionSpec.CLEARTEXT` alongside the `TLS_1_2` `ConnectionSpec` (`MODERN_TLS` -> `TlsVersion.TLS_1_2`).
- Keep `HttpUtil` public method signatures and behavior for `http://` and healthy TLS 1.3 requests unchanged.
- Never retry on HTTP status errors (4xx/5xx), post-header body timeouts, `SSLPeerUnverifiedException`, or `SSLException` with `java.security.cert.CertificateException` in its cause chain.
- Log only `host:port`, `proxied`, and exception via `LogUtil` — never full URLs, paths, query strings, or credentials.
- Timeout caps: `minOf(5_000, maxOf(1, timeout / 2))` for `getUrlContentWithUserAgent` and `getUrlContent`; `minOf(8_000, maxOf(1, timeout / 2))` for `downloadToFile`.
- Cache key: `(host, port, isProxied)` where `isProxied = httpPort != 0`, TTL = 10 minutes, max 64 entries.

## Review Focus

- `SSLException` wrapping a `CertificateException` deep in the cause chain (`SSLException -> ... -> CertificateException`): must not trigger TLS 1.2 retry.
- `followRedirects = true` (`getUrlContent` / `downloadToFile`): `EventListener` flags must reset per hop so a TLS 1.3 handshake on hop 1 does not mask a handshake stall on hop 2, and a redirect to another host must not cache the initial host as TLS 1.2.
- Cached TLS 1.2 host switching networks or hitting a TLS 1.3-only server: must evict the cache entry and retry once with default TLS 1.3.
- `http://` URLs and proxied `httpPort != 0` requests: `http://` bypasses split timeout and TLS 1.2 fallback; proxied and direct requests maintain independent cache entries.
- `getUrlContent` / `downloadToFile` return types (`String?` / `Boolean`) and exception-swallowing contracts must remain intact, while `getUrlContentWithUserAgent` continues to throw `IOException` on unrecovered failure.

---

### Task 1: Add pure fallback classification, timeout budgeting, and bounded TTL cache in `HttpUtil.kt`

**Files:**
- Modify: `app/src/main/java/com/v2ray/ang/util/HttpUtil.kt`
- Test: `app/src/test/java/com/v2ray/ang/HttpUtilTest.kt`

**Interfaces:**
- Produces (internal to `com.v2ray.ang.util.HttpUtil`):
  - `internal fun isRetryableTlsOrTimeout(error: IOException, responseHeadersReceived: Boolean): Boolean`
  - `internal fun computeInitialAttemptTimeoutMs(totalTimeoutMs: Int, maxInitialMs: Int): Int`
  - `internal fun computeRetryTimeoutMs(totalTimeoutMs: Int, elapsedMs: Long): Int`
  - `internal data class TlsFallbackCacheKey(val host: String, val port: Int, val isProxied: Boolean)`
  - `internal fun shouldPreferTls12(key: TlsFallbackCacheKey): Boolean`
  - `internal fun markTls12Preferred(key: TlsFallbackCacheKey)`
  - `internal fun clearTls12Preference(key: TlsFallbackCacheKey)`
  - `internal fun clearTlsFallbackCacheForTesting()`
  - `internal var currentTimeMs: () -> Long`

- [ ] **Step 1: Write failing unit tests for classification, timeout budgeting, and TTL cache**

Add tests to `app/src/test/java/com/v2ray/ang/HttpUtilTest.kt` covering:
- `testIsRetryableTlsOrTimeout_allowsPreHeaderTimeoutAndHandshakeFailures`: `SocketTimeoutException`, `SSLHandshakeException`, `SSLException` with `responseHeadersReceived = false` return `true`; any of them with `responseHeadersReceived = true` return `false`.
- `testIsRetryableTlsOrTimeout_rejectsCertAndUnverifiedErrors`: `SSLPeerUnverifiedException`, `SSLException(CertificateException("expired"))`, nested `SSLException(RuntimeException(CertificateException("untrusted")))`, and plain `IOException("HTTP 500")` return `false`.
- `testComputeAttemptTimeouts_splitsAndClampsCorrectly`:
  - `computeInitialAttemptTimeoutMs(15000, 5000) == 5000`
  - `computeInitialAttemptTimeoutMs(5000, 5000) == 2500`
  - `computeInitialAttemptTimeoutMs(15000, 8000) == 7500`
  - `computeInitialAttemptTimeoutMs(20000, 8000) == 8000`
  - `computeRetryTimeoutMs(15000, 5000L) == 10000`
  - `computeRetryTimeoutMs(15000, 14000L) == 3000` (clamped to floor `minOf(total, maxOf(1, minOf(3_000, total / 2)))`)
- `testTlsFallbackCache_keysByHostPortProxyExpiresAndEvicts`:
  - `shouldPreferTls12(keyDirect)` is `false` initially, `true` after `markTls12Preferred(keyDirect)`, while `keyProxied` (`isProxied = true`) and `keyOtherPort` remain `false`.
  - Advancing `HttpUtil.currentTimeMs` past 10 minutes (`600_001L`) expires the entry.
  - `clearTls12Preference(keyDirect)` removes the entry.
  - Inserting 65 distinct keys keeps cache bounded (oldest evicted, newest retained).

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :app:testPlaystoreDebugUnitTest --tests "com.v2ray.ang.HttpUtilTest"`
Expected: FAIL with unresolved references for `isRetryableTlsOrTimeout`, `computeInitialAttemptTimeoutMs`, `computeRetryTimeoutMs`, `TlsFallbackCacheKey`, etc.

- [ ] **Step 3: Implement classification, timeout budgeting, and bounded TTL cache in `HttpUtil.kt`**

Add to `HttpUtil.kt`:
- Constants: `INITIAL_TLS_HANDSHAKE_CAP_MS = 5_000`, `INITIAL_DOWNLOAD_HANDSHAKE_CAP_MS = 8_000`, `TLS_FALLBACK_CACHE_TTL_MS = 10 * 60 * 1000L`, `TLS_FALLBACK_CACHE_MAX_ENTRIES = 64`.
- `isRetryableTlsOrTimeout(error, responseHeadersReceived)`: return `false` if `responseHeadersReceived`; return `true` for `SocketTimeoutException`; for `SSLException`, return `false` if `error is SSLPeerUnverifiedException` or if walking `cause` chain (with cycle guard) finds `java.security.cert.CertificateException`, else `true`; otherwise `false`.
- `computeInitialAttemptTimeoutMs(totalTimeoutMs, maxInitialMs)`: `minOf(maxInitialMs, maxOf(1, totalTimeoutMs / 2))`.
- `computeRetryTimeoutMs(totalTimeoutMs, elapsedMs)`: `val floor = minOf(totalTimeoutMs, maxOf(1, minOf(3_000, totalTimeoutMs / 2))); (totalTimeoutMs - elapsedMs).coerceIn(floor.toLong(), totalTimeoutMs.toLong()).toInt()`.
- Synchronized bounded LRU-like `LinkedHashMap<TlsFallbackCacheKey, Long>` (access-order or insertion-order with expired sweep + eldest eviction at `> 64`), using `currentTimeMs()`.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :app:testPlaystoreDebugUnitTest --tests "com.v2ray.ang.HttpUtilTest"`
Expected: PASS (all existing + new Task 1 tests green).

- [ ] **Step 5: Commit Task 1**

```bash
git add app/src/main/java/com/v2ray/ang/util/HttpUtil.kt app/src/test/java/com/v2ray/ang/HttpUtilTest.kt
git commit -m "feat(http): add TLS 1.2 fallback classification, timeout budget, and TTL cache"
```

---

### Task 2: Add `EventListener` phase tracking and shared `executeWithTlsFallback` helper across all three `HttpUtil` fetch methods

**Files:**
- Modify: `app/src/main/java/com/v2ray/ang/util/HttpUtil.kt`
- Test: `app/src/test/java/com/v2ray/ang/HttpUtilTest.kt`

**Interfaces:**
- Consumes (from Task 1): `isRetryableTlsOrTimeout`, `computeInitialAttemptTimeoutMs`, `computeRetryTimeoutMs`, `TlsFallbackCacheKey`, `shouldPreferTls12`, `markTls12Preferred`, `clearTls12Preference`, `currentTimeMs`.
- Produces (internal to `HttpUtil`):
  - `internal var clientBuilderFactory: () -> OkHttpClient.Builder = { OkHttpClient.Builder() }`
  - `internal var attemptExecutor: (OkHttpClient, Request, TlsAttemptEventListener) -> Response = { client, req, _ -> client.newCall(req).execute() }`
  - `private fun <T> executeWithTlsFallback(url: String, timeoutMs: Int, initialCapMs: Int, httpPort: Int, proxyUsername: String?, proxyPassword: String?, followRedirects: Boolean, request: Request, responseHandler: (Response) -> T): T`

- [ ] **Step 1: Write failing unit tests for `executeWithTlsFallback` via `getUrlContentWithUserAgent`, `getUrlContent`, and `downloadToFile`**

Add tests to `app/src/test/java/com/v2ray/ang/HttpUtilTest.kt` using `HttpUtil.attemptExecutor` (plus a real loopback `ServerSocket` blackhole test for `secureConnectStart` without `secureConnectEnd`), resetting `clientBuilderFactory`, `attemptExecutor`, `currentTimeMs`, and `clearTlsFallbackCacheForTesting()` in `@AfterEach`:
- `testGetUrlContentWithUserAgent_httpsBlackholeTriggersTls12FallbackAndCachesHost`:
  - First `https://127.0.0.1:<port>/sub` call: simulated pre-header handshake timeout (`SocketTimeoutException` before `secureConnectEnd`) on default TLS 1.3 attempt (verifying `readTimeoutMillis == 5000`), followed by TLS 1.2 retry (verifying `connectionSpecs` contains `TLS_1_2` + `CLEARTEXT`) that succeeds and returns body; verifies `(127.0.0.1, port, false)` is now cached.
  - Second call to same host/port: starts directly with `TLS_1_2` + `CLEARTEXT` and full `15000ms` timeout.
- `testGetUrlContentWithUserAgent_postHandshakeHeaderTimeoutRetriesWithDefaultTls13WithoutCaching`:
  - Simulated `secureConnectEnd` completed, then `SocketTimeoutException` before `responseHeadersEnd`: retries once with default TLS 1.3 (not `TLS_1_2`), succeeds, and does NOT cache TLS 1.2.
- `testGetUrlContentWithUserAgent_cachedTls12FailureEvictsAndSelfHealsToTls13`:
  - Seed cache for `(127.0.0.1, port, false)`, first TLS 1.2 call fails with `SSLHandshakeException`, cache is evicted, retries once with default TLS 1.3 and succeeds.
- `testGetUrlContentWithUserAgent_httpStatusErrorDoesNotRetry`:
  - HTTP 500 response throws `IOException` without retrying.
- `testGetUrlContent_and_downloadToFile_useSharedFallbackWithDownloadCap`:
  - `getUrlContent` on `https://` uses `5000ms` initial cap (for 15s timeout) and falls back to TLS 1.2 on pre-handshake timeout.
  - `downloadToFile` on `https://` uses `7500ms` initial cap (for 15s timeout, `minOf(8000, 15000 / 2)`) and falls back to TLS 1.2 on pre-handshake timeout, writing file contents cleanly.
- `testHttpUrl_bypassesTlsFallbackAndUsesFullTimeout`:
  - `http://` request uses full `15000ms` timeout and does not retry on `SocketTimeoutException`.

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :app:testPlaystoreDebugUnitTest --tests "com.v2ray.ang.HttpUtilTest"`
Expected: FAIL with unresolved `clientBuilderFactory` or fallback assertions failing.

- [ ] **Step 3: Implement `TlsAttemptTracker` (`EventListener`), `TLS_1_2_CONNECTION_SPEC`, and `executeWithTlsFallback` in `HttpUtil.kt` and wire `getUrlContentWithUserAgent`, `getUrlContent`, and `downloadToFile`**

Implementation details in `HttpUtil.kt`:
- Define `TLS_1_2_CONNECTION_SPEC = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS).tlsVersions(TlsVersion.TLS_1_2).build()` and `TLS_1_2_SPECS = listOf(TLS_1_2_CONNECTION_SPEC, ConnectionSpec.CLEARTEXT)`.
- Define `TlsAttemptEventListener` extending `EventListener`:
  - `@Volatile var secureConnectCompleted = false`
  - `@Volatile var responseHeadersReceived = false`
  - `@Volatile var redirected = false`
  - Reset `secureConnectCompleted = false` in `connectStart` and `secureConnectStart`; set `secureConnectCompleted = true` in `secureConnectEnd`.
  - Reset `responseHeadersReceived = false` in `connectStart` and `requestHeadersStart`; set `responseHeadersReceived = true` in `responseHeadersEnd`.
  - Set `redirected = true` in `responseHeadersEnd` if `response.isRedirect`.
- Update `buildOkHttpClient` to accept `connectionSpecs: List<ConnectionSpec>? = null` and `eventListener: EventListener? = null`, starting from `clientBuilderFactory()`.
- Implement `executeWithTlsFallback`:
  - Parse `URI(Utils.fixIllegalUrl(url))`; if `scheme != "https"` or host is null, execute once with full `timeoutMs` and no fallback.
  - Construct `TlsFallbackCacheKey(toIdnDomain(host).lowercase(Locale.ROOT), effectivePort, httpPort != 0)`.
  - If `shouldPreferTls12(cacheKey)`:
    - Try once with `timeoutMs` and `TLS_1_2_SPECS`.
    - On `IOException` where `isRetryableTlsOrTimeout(e, listener.responseHeadersReceived)`: `clearTls12Preference(cacheKey)`, log via `LogUtil.w`, and retry once with `computeRetryTimeoutMs(timeoutMs, elapsed)` and default TLS 1.3 (`connectionSpecs = null`).
  - Else (default TLS 1.3 first):
    - Compute `initialTimeout = computeInitialAttemptTimeoutMs(timeoutMs, initialCapMs)`.
    - Try once with `initialTimeout` and default TLS 1.3.
    - On `IOException` where `isRetryableTlsOrTimeout(e, listener.responseHeadersReceived)`:
      - `retryTimeout = computeRetryTimeoutMs(timeoutMs, elapsed)`
      - If `!listener.secureConnectCompleted`: log fallback via `LogUtil.w`, retry once with `retryTimeout` and `TLS_1_2_SPECS`; if it succeeds and `!retryListener.redirected`, call `markTls12Preferred(cacheKey)`.
      - Else (`listener.secureConnectCompleted == true`): log slow-header retry via `LogUtil.w`, retry once with `retryTimeout` and default TLS 1.3 (do not cache TLS 1.2).
- Wire `getUrlContent`, `getUrlContentWithUserAgent`, and `downloadToFile` through `executeWithTlsFallback` (using `INITIAL_TLS_HANDSHAKE_CAP_MS` for the first two and `INITIAL_DOWNLOAD_HANDSHAKE_CAP_MS` for `downloadToFile`).

- [ ] **Step 4: Run tests and full validation**

Run:
`./gradlew :app:testPlaystoreDebugUnitTest --tests "com.v2ray.ang.HttpUtilTest"`
then full required validation per `AGENTS.md`:
`./gradlew :app:testPlaystoreDebugUnitTest :app:compilePlaystoreDebugKotlin`
Expected: PASS with 0 failures.

- [ ] **Step 5: Commit Task 2**

```bash
git add app/src/main/java/com/v2ray/ang/util/HttpUtil.kt app/src/test/java/com/v2ray/ang/HttpUtilTest.kt
git commit -m "feat(http): add TLS 1.2 handshake fallback across HttpUtil fetch methods"
```
