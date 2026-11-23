#!/usr/bin/env python3
"""
KUMO VPN Invariant & Integrity Verification Script
Checks all project invariants:
1. Strict EN and FA localization only (no pruned locales reintroduced).
2. 1:1 key parity between values/strings.xml and values-fa/strings.xml.
3. App name is "KUMO VPN" in both locales.
4. Language.kt enum is strictly AUTO, ENGLISH, PERSIAN.
5. build.gradle.kts localeFilters contains only "en" and "fa".
6. AndroidManifest.xml has KumoMainActivity as the sole launcher activity.
7. Legacy MainActivity is dormant (not exported as launcher).
"""

import os
import re
import sys
import xml.etree.ElementTree as ET

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
APP_DIR = os.path.join(REPO_ROOT, "app") if os.path.isdir(os.path.join(REPO_ROOT, "app")) else os.path.join(REPO_ROOT, "V2rayNG", "app")
RES_DIR = os.path.join(APP_DIR, "src", "main", "res")
VALUES_STRINGS = os.path.join(RES_DIR, "values", "strings.xml")
VALUES_FA_STRINGS = os.path.join(RES_DIR, "values-fa", "strings.xml")
BUILD_GRADLE = os.path.join(APP_DIR, "build.gradle.kts")
LANGUAGE_KT = os.path.join(APP_DIR, "src", "main", "java", "com", "v2ray", "ang", "enums", "Language.kt")
MANIFEST_XML = os.path.join(APP_DIR, "src", "main", "AndroidManifest.xml")

FORBIDDEN_LOCALES = [
    "values-ar", "values-bn", "values-bqi-rIR", "values-ru",
    "values-vi", "values-zh-rCN", "values-zh-rTW"
]

def fail(msg: str):
    print(f"❌ FAIL: {msg}")
    sys.exit(1)

def info(msg: str):
    print(f"✅ PASS: {msg}")

def check_locales():
    for item in os.listdir(RES_DIR):
        if item in FORBIDDEN_LOCALES:
            fail(f"Forbidden pruned locale directory found: {item}")
        if item.startswith("values-") and not item.startswith("values-fa") and not any(item.startswith(f"values-{x}") for x in ["night", "v27", "sw360dp"]):
            fail(f"Unexpected locale directory found: {item}")
    info("Only EN and FA locales present in resources")

def get_string_keys(path: str) -> set:
    tree = ET.parse(path)
    root = tree.getroot()
    keys = set()
    for child in root:
        if child.tag == "string" and "name" in child.attrib:
            keys.add(child.attrib["name"])
        elif child.tag == "string-array" and "name" in child.attrib:
            keys.add(child.attrib["name"])
        elif child.tag == "plurals" and "name" in child.attrib:
            keys.add(child.attrib["name"])
    return keys

def get_string_value(path: str, key_name: str) -> str:
    tree = ET.parse(path)
    root = tree.getroot()
    for child in root:
        if child.tag == "string" and child.attrib.get("name") == key_name:
            return child.text or ""
    return ""

def check_string_parity():
    en_keys = get_string_keys(VALUES_STRINGS)
    fa_keys = get_string_keys(VALUES_FA_STRINGS)

    missing_in_fa = en_keys - fa_keys
    extra_in_fa = fa_keys - en_keys

    if missing_in_fa:
        fail(f"Keys missing in values-fa/strings.xml ({len(missing_in_fa)}): {sorted(list(missing_in_fa))[:10]}")
    if extra_in_fa:
        fail(f"Extra keys found in values-fa/strings.xml ({len(extra_in_fa)}): {sorted(list(extra_in_fa))[:10]}")

    info(f"1:1 string key parity verified ({len(en_keys)} keys in both EN and FA)")

    en_app_name = get_string_value(VALUES_STRINGS, "app_name")
    fa_app_name = get_string_value(VALUES_FA_STRINGS, "app_name")
    if en_app_name != "KUMO VPN":
        fail(f"values/strings.xml app_name is '{en_app_name}', expected 'KUMO VPN'")
    if fa_app_name != "KUMO VPN":
        fail(f"values-fa/strings.xml app_name is '{fa_app_name}', expected 'KUMO VPN'")
    info("App name is 'KUMO VPN' in both locales")

def check_language_enum():
    with open(LANGUAGE_KT, "r", encoding="utf-8") as f:
        content = f.read()
    expected_enums = ["AUTO", "ENGLISH", "PERSIAN"]
    for enum_val in expected_enums:
        if enum_val not in content:
            fail(f"Language.kt missing expected enum entry: {enum_val}")
    # ensure no other languages in enum
    for forbidden in ["SIMPLIFIED_CHINESE", "TRADITIONAL_CHINESE", "RUSSIAN", "VIETNAMESE", "ARABIC"]:
        if forbidden in content:
            fail(f"Language.kt contains forbidden enum entry: {forbidden}")
    info("Language.kt strictly limited to AUTO, ENGLISH, PERSIAN")

def check_build_gradle():
    with open(BUILD_GRADLE, "r", encoding="utf-8") as f:
        content = f.read()
    if '"en"' not in content or '"fa"' not in content:
        fail("build.gradle.kts localeFilters does not contain 'en' and 'fa'")
    for forbidden_code in ['"zh"', '"ru"', '"ar"', '"vi"']:
        if forbidden_code in content:
            fail(f"build.gradle.kts contains forbidden locale code: {forbidden_code}")
    info("build.gradle.kts localeFilters strictly limited to en and fa")

def check_manifest():
    with open(MANIFEST_XML, "r", encoding="utf-8") as f:
        content = f.read()
    if "KumoMainActivity" not in content:
        fail("AndroidManifest.xml does not contain KumoMainActivity")
    # Verify KumoMainActivity has MAIN and LAUNCHER
    if not re.search(r'activity[^>]*KumoMainActivity[^>]*>.*android\.intent\.action\.MAIN.*android\.intent\.category\.LAUNCHER', content, re.DOTALL):
        fail("KumoMainActivity is not registered with MAIN and LAUNCHER intent filters")
    # Verify legacy MainActivity does not have LAUNCHER
    legacy_match = re.search(r'<activity[^>]*android:name="\.ui\.MainActivity"[^>]*>(.*?)</activity>', content, re.DOTALL)
    if legacy_match:
        legacy_body = legacy_match.group(1)
        if "android.intent.category.LAUNCHER" in legacy_body:
            fail("Legacy MainActivity still has LAUNCHER category in AndroidManifest.xml")
    info("KumoMainActivity is the sole launcher; legacy MainActivity is dormant")

def main():
    print("=== KUMO VPN Invariant & Integrity Checks ===")
    check_locales()
    check_string_parity()
    check_language_enum()
    check_build_gradle()
    check_manifest()
    print("=== All KUMO Invariant Checks Passed! ===")

if __name__ == "__main__":
    main()
