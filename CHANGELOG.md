# Changelog - KUMO VPN

All notable changes to the **KUMO VPN** project (forked and adapted from [2dust/v2rayNG](https://github.com/2dust/v2rayNG)) will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## Upstream Synchronization Guide (راهنمای همگام‌سازی با آپ‌استریم)

مخزن بالادست رسمی: `https://github.com/2dust/v2rayNG.git` (ریموت `upstream`)  
شاخه اصلی بالادست: `master`

### ۱. دستورات دریافت و همگام‌سازی آپدیت‌ها:
```bash
# دریافت آخرین تغییرات از آپ‌استریم v2rayNG
git fetch upstream master

# ادغام تغییرات در شاخه اصلی پروژه
git merge upstream/master
```

### ۲. راهنمای حل تعارض‌های احتمالی (Conflict Resolution Policy):
| فایل / مسیر | رفتار مورد انتظار در زمان Merge |
| :--- | :--- |
| `V2rayNG/app/res/values-*/` | پوشه‌های زبان‌های حذف‌شده (`ar`, `bn`, `bqi-rIR`, `ru`, `vi`, `zh-*`) در صورت بازگشت مجدداً باید حذف بمانند (`git rm`). |
| `V2rayNG/app/res/values/strings.xml` | کلیدهای جدید اضافه شده توسط آپ‌استریم پذیرفته شوند، اما کلید `app_name` روی `KUMO VPN` حفظ شود و کلیدهای حذف‌شده (`title_translators`, `routing_preset_russia_whitelist`) بازگردانده نشوند. |
| `V2rayNG/app/res/values-fa/strings.xml` | هر کلید جدیدی که در `strings.xml` انگلیسی اضافه شد، ترجمه فارسی آن به این فایل اضافه گردد تا تطابق ۱۰۰٪ حفظ شود. |
| `V2rayNG/app/build.gradle.kts` | در بلوک `androidResources.localeFilters`، فقط زبان‌های `"en"` و `"fa"` نگه داشته شوند. |
| `V2rayNG/app/.../enums/Language.kt` | حفظ نسخه ساده‌شده شامل ۳ حالت `AUTO`، `ENGLISH` و `PERSIAN`. |
| `V2rayNG/app/res/values/arrays.xml` | آرایه‌های `language_select` و `language_select_value` فقط شامل گزینه‌های `auto`، `en` و `fa` بمانند. |
| `V2rayNG/app/.../AboutActivity.kt` | عدم بازگردانی دکمه ورود به صفحه مترجمان (`TranslatorsActivity`). |
| `V2rayNG/app/.../RoutingSettingActivity.kt` | عدم بازگردانی پریست روتینگ روسیه (`RussiaWhitelist`). |
| `V2rayNG/app/.../RoutingType.kt` | عدم بازگردانی مقدار `WHITE_RUSSIA`. |

---

## [Unreleased]

### Baseline Upstream Reference
- **Upstream Repository:** `2dust/v2rayNG`
- **Baseline Commit:** `6fe3893bfd37865dc677111fa48aef14c0a41efd` (v2.3.10+)

### Added
- **Modern KUMO UI Shell (`ui/kumo`):** Brand-new, consumer-focused user interface adhering to Apple Human Interface Guidelines (HIG) and inspired by the Akatsuki Cloud aesthetic.
- **Akatsuki Cloud Interactive Toggle:** Central hero connection button (`AkatsukiCloudButton`) with 3 animated states (Disconnected, Breathing pulse connecting, and Crimson awakened connected).
- **No-Ripple Apple Press Feedback:** Reusable `appleClickable` modifier with physical spring scale (0.97f) and dimming, eliminating Material ink ripples.
- **Telemetry Capsule:** Inset Grouped pill showing real-time ping latency with dynamic color indicators and traffic speed metrics.
- **Server Selection Bottom Sheet:** Streamlined location picker with Smart Connect (fastest server) and ping test capabilities, without geek-level configuration or deletion clutter.
- **Account & Essential Controls Sheet:** Re-architected subscription management into a simple "My Account" onboarding card, alongside 3 essential consumer toggles (Bypass Iran, Split Tunneling, Auto Connect).
- **New App Launcher Icon:** Replaced launcher and round launcher mipmap icons with the official KUMO Akatsuki Cloud emblem.
- **Primary Entry Point:** Configured `KumoMainActivity` as the sole launcher activity in `AndroidManifest.xml`, leaving legacy `MainActivity` dormant for upstream reference.
- **Automated CI/CD Pipelines (`build.yml` & `ci.yml`):** Implemented automated build, signing, and release pipeline matching PattNG specifications (all 10 APK variants across 5 ABIs and 2 distributions, detached `.sig` GPG signatures, `KUMO-public-key.asc`, and `SHA256SUMS.txt`). Added CI quality gate workflow validating invariants, JVM unit tests, Kotlin compilation, and debug assembly.
- **Invariant & Integrity Verification:** Added automated script (`.github/scripts/verify_invariants.py`) and Kotlin unit test (`KumoInvariantsTest.kt`) enforcing strict 2-language parity, dormant UI state, and package invariants.
- **Dynamic Versioning & APK Naming:** Updated `build.gradle.kts` to output `KUMO_<version>-fdroid_<abi>.apk` and `KUMO_<version>_<abi>.apk` with `-PkumoVersion` build property support.

### Changed
- **Rebranding:** Renamed application name to `KUMO VPN` across English (`values/strings.xml`) and Persian (`values-fa/strings.xml`) string resources.
- **Gradle Resource Filtering:** Restricted `androidResources.localeFilters` in `V2rayNG/app/build.gradle.kts` to only `en` and `fa`.
- **Language Selection:** Simplified `Language.kt` enum to `AUTO`, `ENGLISH`, and `PERSIAN`. Updated `arrays.xml` (`language_select` and `language_select_value`) to match.
- **About Screen:** Removed the `Translators` menu item from `AboutActivity.kt`.
- **Routing Presets:** Removed `RussiaWhitelist` from `RoutingSettingActivity.kt` and `WHITE_RUSSIA` from `RoutingType.kt`.

### Removed
- **Redundant Locales:** Deleted all non-English and non-Persian translation resource folders:
  - `V2rayNG/app/src/main/res/values-ar/` (Arabic)
  - `V2rayNG/app/src/main/res/values-bn/` (Bengali)
  - `V2rayNG/app/src/main/res/values-bqi-rIR/` (Bakhtiari)
  - `V2rayNG/app/src/main/res/values-ru/` (Russian)
  - `V2rayNG/app/src/main/res/values-vi/` (Vietnamese)
  - `V2rayNG/app/src/main/res/values-zh-rCN/` (Simplified Chinese)
  - `V2rayNG/app/src/main/res/values-zh-rTW/` (Traditional Chinese)
- **Translators Screen & Assets:**
  - `V2rayNG/app/src/main/java/com/v2ray/ang/ui/TranslatorsActivity.kt`
  - `V2rayNG/app/src/main/java/com/v2ray/ang/dto/TranslatorsCredit.kt`
  - `V2rayNG/app/src/main/assets/translators.json`
  - Removed `TranslatorsActivity` declaration from `AndroidManifest.xml`.
- **Regional Assets:**
  - `V2rayNG/app/src/main/assets/custom_routing_white_russia`
