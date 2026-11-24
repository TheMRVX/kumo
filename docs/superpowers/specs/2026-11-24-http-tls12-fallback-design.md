# HttpUtil TLS 1.2 Fallback Design

**Date:** 2026-11-24  
**Scope:** Small change (`app/src/main/java/com/v2ray/ang/util/HttpUtil.kt` and `app/src/test/java/com/v2ray/ang/HttpUtilTest.kt`)

## Intent & Why

On several Iran ISPs (Irancell `AS44244`, MCI `AS197207`, SabaNet `AS39501`), DPI drops Android Conscrypt/BoringSSL TLS 1.3 `ClientHello` packets (`X25519MLKEM768` + TLS extension permutation) in transit after TCP 3-way handshake succeeds. Because `SSLSocket.startHandshake()` blocks until OkHttp's `readTimeout`, HTTPS requests in `HttpUtil` stall for the full 15s timeout and fail (stacking up to 30s in `AngConfigManager.updateConfigViaSub` across proxy and direct attempts).

Adding a targeted TLS 1.2 fallback inside `HttpUtil` restores reliable HTTPS fetches across subscriptions, app update checks, per-app proxy lists, and geo-asset downloads while keeping TLS 1.3 as the primary default and avoiding any changes to Xray routing (`CoreConfigManager.kt`) so KUMO stays compatible with an upcoming domestic Iran reverse proxy.

## Goals, Non-Goals, and Anti-Goals

- **Goals:**
  - Recover automatically from TLS 1.3 DPI handshake stalls by retrying once with `TlsVersion.TLS_1_2` (classical single-packet `ClientHello`, SNI in TCP segment #1).
  - Keep first-hit latency near ~6s (for 15s subscription calls) and repeat-hit latency near ~1s via a bounded in-memory session cache.
  - Share one execution helper across `getUrlContentWithUserAgent`, `getUrlContent`, and `downloadToFile`.
  - Cover the fallback, timeout budgeting, non-retry paths, and cache behavior with deterministic JUnit 5 unit tests.
- **Non-Goals:**
  - Modifying `CoreConfigManager.kt`, `AngConfigManager.kt`, or routing presets (`custom_routing_white_iran`).
  - Persisting TLS preferences to MMKV/disk across app restarts.
  - Adding new third-party dependencies or changing OkHttp versions.
- **Anti-Goals:**
  - Weakening certificate/hostname verification or allowing TLS 1.0/1.1.
  - Retrying on HTTP status errors (4xx/5xx), post-header body timeouts, or `http://` requests.
  - Wrapping Android Conscrypt `SSLSocket` instances (breaks OkHttp 5.1 `SSLSockets.isSupportedSocket` / ALPN / SNI).
  - Logging full subscription URLs, paths, query parameters, or credentials.

## Constraints

- Standard OkHttp 5.1.0 APIs only (`ConnectionSpec`, `TlsVersion`, `EventListener`).
- Preserve `ConnectionSpec.CLEARTEXT` alongside the `TLS_1_2` spec so HTTP proxy `CONNECT` tunnels and non-HTTPS requests remain unaffected.
- Keep `HttpUtil` public method signatures and existing behavior for `http://` and healthy TLS 1.3 requests unchanged.

## Approaches Considered

1. **Approach 1 — Split-budget retry + `EventListener` phase check + bounded TTL host cache *(Picked)***
   - *Design:* Split the caller's `timeout` on `https://` requests so the primary TLS 1.3 attempt caps handshake wait, inspect exact handshake/header state via a per-call `EventListener`, retry with `TLS_1_2` only on pre-handshake failure (or default TLS 1.3 on post-handshake slow-header timeout), and cache confirmed TLS 1.2 hosts in memory with TTL and self-healing eviction.
   - *Trade-off:* Uses 100% standard OkHttp APIs with no extra threads or cancellation races. Healthy TLS 1.3 servers that take longer than the initial cap to emit response headers incur one retry (on default TLS 1.3, without caching TLS 1.2).
2. **Approach 2 — Handshake-only watchdog (`Call.cancel()`) + bounded TTL host cache**
   - *Design:* Keep full `timeout` as `readTimeout` and arm a ~5s timer between `secureConnectStart` and `secureConnectEnd` that cancels the `Call` if handshake stalls.
   - *Trade-off:* Avoids splitting `readTimeout` for slow-header servers, but adds a scheduler/timer lifecycle and cross-thread cancellation synchronization inside `HttpUtil`.
3. **Approach 3 — Stateless split-budget retry (no session cache)**
   - *Design:* Same as Approach 1 without caching host TLS 1.2 preference.
   - *Trade-off:* Simplest code with zero cache state, but pays the ~5s TLS 1.3 stall on every subscription refresh and redirect hop on affected ISPs.

**Why Approach 1:** Hits the ~6s first-hit / ~1s repeat latency goal across all three `HttpUtil` entry points using plain, thread-safe OkHttp APIs with minimal blast radius.

## Decided Design Details

- **Shared helper in `HttpUtil.kt`:**
  - Extract a single internal execution helper (e.g. `executeWithTlsFallback`) used by `getUrlContentWithUserAgent`, `getUrlContent`, and `downloadToFile`.
  - Only apply fallback logic when the request URL scheme is `https`. Non-HTTPS requests execute directly with the full caller `timeout`.
- **Timeout budgeting:**
  - `getUrlContentWithUserAgent` and `getUrlContent`: initial TLS 1.3 attempt cap = `minOf(5_000, maxOf(1, timeout / 2))` (`5_000ms` for 15s subscriptions, `2_500ms` for 5s update/proxy-list checks).
  - `downloadToFile`: initial TLS 1.3 attempt cap = `minOf(8_000, maxOf(1, timeout / 2))` (`7_500ms` for 15s geo-asset downloads) so large mobile downloads have a more generous body-read timeout.
  - Retry attempt timeout: remaining budget `(timeout - elapsedMs)` clamped to a sensible floor (`minOf(timeout, maxOf(1, timeout / 2))` or `minOf(timeout, 3_000)` when `timeout >= 6_000`) so a slow DNS lookup cannot starve the retry.
- **Per-call `EventListener` phase tracking:**
  - Track `secureConnectCompleted`, `responseHeadersReceived`, and `redirected` (resetting `secureConnectCompleted` on `connectStart`/`secureConnectStart` and `responseHeadersReceived` on `connectStart`/`requestHeadersStart` so `followRedirects = true` hops are tracked accurately).
  - Retryable failures: `SocketTimeoutException` or `SSLException` (excluding `SSLPeerUnverifiedException`) **only when `!responseHeadersReceived`**.
  - **Refined retry rule:**
    - If `!secureConnectCompleted`: retry once with `ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS).tlsVersions(TlsVersion.TLS_1_2).build()` + `ConnectionSpec.CLEARTEXT`. If this retry succeeds (and `!redirected`), cache `(host, port, isProxied)` in the TTL cache.
    - If `secureConnectCompleted` (TLS 1.3 handshake succeeded, but server took longer than the initial cap to send response headers): retry once with the **default TLS 1.3 client** on the remaining budget and do **not** cache TLS 1.2.
- **Bounded in-memory TTL cache:**
  - Key: `TlsFallbackCacheKey(host = normalizedHost, port = effectivePort, isProxied = httpPort != 0)`.
  - TTL: 10 minutes; bounded size (e.g., max 64 entries, evicting expired/oldest entries on insert).
  - On cache hit: attempt `TLS_1_2` first with the full `timeout`. If that attempt throws a retryable pre-header `SocketTimeoutException` or `SSLException` (excluding `SSLPeerUnverifiedException`), evict the cache entry and retry once with the default TLS 1.3 client so network switches or TLS 1.3-only servers self-heal.
- **Logging:**
  - Log fallback triggers and cache evictions via `LogUtil` with `host:port`, `proxied`, and exception only (never full URLs or credentials).

## Left to the Builder

- Exact internal helper and cache class/method names and test seams (e.g. injectable clock / call executor or local loopback `ServerSocket`/`SSLServerSocket` test harness in `HttpUtilTest.kt`).
- Exact retry floor clamping edge cases for very small timeouts in unit tests.
- Exact cache eviction implementation (e.g., synchronized bounded map or `ConcurrentHashMap` with expired sweep + oldest eviction).
