import argparse
import glob
import json
import os.path
import re
import subprocess
import sys
import typing

import lxml.etree
from lxml import etree as ET

LANGUAGE_NAMES = {
    "ar": "Arabic",
    "bg": "Bulgarian",
    "cs": "Czech",
    "da": "Danish",
    "de": "German",
    "el": "Greek",
    "es": "Spanish",
    "fi": "Finnish",
    "fr": "French",
    "hu": "Hungarian",
    "is": "Icelandic",
    "it": "Italian",
    "iw": "Hebrew",
    "nl": "Dutch",
    "pl": "Polish",
    "pt": "Portuguese",
    "pt-rBR": "Brazilian Portuguese",
    "ru": "Russian",
    "sv": "Swedish",
    "ta": "Tamil",
    "tr": "Turkish",
    "uk": "Ukrainian",
    "zh-rCN": "Simplified Chinese",
    "zh-rTW": "Traditional Chinese",
}


def get_language_from_directory(directory_name: str) -> str:
    return os.path.split(directory_name)[-1].replace("values-", "")


def map_deepl_language(lang: str) -> str:
    if lang == "pt-rBR":
        return "pt-BR"
    return lang if lang != "zh-rCN" else "zh-hans"


def escape(str_xml: str):
    str_xml = str_xml.replace("&", "&amp;")
    str_xml = str_xml.replace("<", "&lt;")
    str_xml = str_xml.replace(">", "&gt;")
    str_xml = str_xml.replace('"', "&quot;")
    str_xml = str_xml.replace("'", "&apos;")
    return str_xml


def load_english_strings() -> typing.Dict[str, str]:
    tree = ET.parse("app/src/main/res/values/strings.xml")
    resources = tree.getroot()
    english_strings: typing.Dict[str, str] = {}
    for string_elem in resources.findall("string"):
        if string_elem.attrib.get("translatable") == "false":
            continue
        english_strings[string_elem.attrib["name"]] = string_elem.text
    return english_strings


def load_language_trees(
    english_strings: typing.Dict[str, str],
    forced_strings: typing.List[str],
) -> typing.Tuple[typing.Dict[str, lxml.etree._ElementTree], typing.List[typing.Tuple[str, str]]]:
    language_tree: typing.Dict[str, lxml.etree._ElementTree] = {}
    translate_list: typing.List[typing.Tuple[str, str]] = []

    for directory in glob.glob("app/src/main/res/*"):
        if directory == "app/src/main/res/values" or not os.path.exists(
            directory + "/strings.xml"
        ):
            continue
        language = get_language_from_directory(directory)
        if language == "nb-rNO":
            continue
        language_tree[language] = ET.parse(directory + "/strings.xml")
        resources = language_tree[language].getroot()
        language_strings = [s.attrib["name"] for s in resources.findall("string")]
        for name in english_strings.keys():
            if (
                name not in language_strings
                or not language_strings[language_strings.index(name)]
                or name in forced_strings
            ):
                translate_list.append((name, language))

    return language_tree, translate_list


def write_language_trees(
    translate_list: typing.List[typing.Tuple[str, str]],
    language_tree: typing.Dict[str, lxml.etree._ElementTree],
) -> None:
    languages = {lang for _, lang in translate_list}
    for language in languages:
        with open(f"app/src/main/res/values-{language}/strings.xml", "wb") as f:
            language_tree[language].write(
                f, encoding="utf-8", xml_declaration=True, pretty_print=True
            )


def translate_with_deepl(
    translate_list: typing.List[typing.Tuple[str, str]],
    english_strings: typing.Dict[str, str],
    language_tree: typing.Dict[str, lxml.etree._ElementTree],
    auth_key: str,
) -> None:
    import deepl
    from deepl import DeepLException

    translator = deepl.Translator(auth_key)

    for string_name, language in translate_list:
        print(f"Translating {string_name} to {language}")
        try:
            translated = translator.translate_text(
                english_strings[string_name],
                target_lang=map_deepl_language(language),
                preserve_formatting=True,
            )
        except DeepLException:
            print("Translation failed")
            continue

        element = language_tree[language].find(f'string[@name="{string_name}"]')
        if element is None:
            element = ET.SubElement(
                language_tree[language].getroot(), "string", name=string_name
            )
        element.text = escape(translated.text)


def _validate_format_specifiers(source: str, translation: str, string_name: str) -> bool:
    pattern = r'%\d*\$?[a-zA-Z]'
    source_specs = sorted(re.findall(pattern, source or ""))
    trans_specs = sorted(re.findall(pattern, translation or ""))
    if source_specs != trans_specs:
        print(
            f"  WARNING: format specifier mismatch in '{string_name}': "
            f"expected {source_specs}, got {trans_specs} — skipping"
        )
        return False
    return True


def translate_with_claude(
    translate_list: typing.List[typing.Tuple[str, str]],
    english_strings: typing.Dict[str, str],
    language_tree: typing.Dict[str, lxml.etree._ElementTree],
) -> None:
    by_language: typing.Dict[str, typing.List[str]] = {}
    for string_name, language in translate_list:
        by_language.setdefault(language, []).append(string_name)

    for language, string_names in sorted(by_language.items()):
        lang_display = LANGUAGE_NAMES.get(language, language)
        strings_input = {name: english_strings[name] for name in string_names}

        prompt = (
            f"You are translating UI strings for MedTimer, an Android medication reminder app.\n\n"
            f"Translate all the following English strings to {lang_display}.\n\n"
            f"Rules:\n"
            f"- Preserve ALL Android format specifiers exactly as-is: %s, %1$s, %2$s, %1$d, %2$d, etc.\n"
            f"- Preserve ALL escape sequences exactly as-is: \\n, \\', etc.\n"
            f"- Do NOT translate the app name 'MedTimer'\n"
            f"- Return ONLY a valid JSON object, no markdown, no code blocks, no explanations\n"
            f"- Use the exact same string keys as provided in the input\n\n"
            f"Input JSON:\n"
            f"{json.dumps(strings_input, ensure_ascii=False, indent=2)}"
        )

        print(f"Translating {len(string_names)} strings to {lang_display} ({language})...")

        try:
            result = subprocess.run(
                ["claude", "-p", prompt],
                capture_output=True,
                text=True,
                timeout=120,
            )
        except FileNotFoundError:
            print("Error: 'claude' CLI not found. Ensure Claude Code is installed and in PATH.")
            sys.exit(1)
        except subprocess.TimeoutExpired:
            print(f"  Timeout for {language}, skipping.")
            continue

        if result.returncode != 0:
            print(f"  Claude error for {language}: {result.stderr[:300]}")
            continue

        text = result.stdout.strip()
        # Strip markdown code fences if Claude wraps the JSON anyway
        text = re.sub(r'^```[a-z]*\n?', '', text, flags=re.MULTILINE)
        text = re.sub(r'\n?```\s*$', '', text, flags=re.MULTILINE)
        text = text.strip()

        try:
            translations = json.loads(text)
        except json.JSONDecodeError as e:
            print(f"  Failed to parse response for {language}: {e}")
            print(f"  Raw output: {text[:300]}")
            continue

        for string_name in string_names:
            if string_name not in translations:
                print(f"  Missing translation for '{string_name}' in {language}")
                continue

            translation = str(translations[string_name])
            if not _validate_format_specifiers(english_strings[string_name], translation, string_name):
                continue

            element = language_tree[language].find(f'string[@name="{string_name}"]')
            if element is None:
                element = ET.SubElement(
                    language_tree[language].getroot(), "string", name=string_name
                )
            element.text = escape(translation)


def main() -> None:
    # Backward-compat: old usage was `translate.py AUTH_KEY [string1 string2 ...]`
    raw_args = sys.argv[1:]
    if raw_args and not raw_args[0].startswith("-"):
        auth_key = raw_args[0]
        rest = raw_args[1:]
        raw_args = ["--auth-key", auth_key]
        if rest:
            raw_args += ["--strings"] + rest

    parser = argparse.ArgumentParser(description="Translate MedTimer string resources")
    parser.add_argument(
        "--backend",
        choices=["deepl", "claude"],
        default="deepl",
        help="Translation backend (default: deepl)",
    )
    parser.add_argument("--auth-key", help="DeepL API auth key (required for deepl backend)")
    parser.add_argument(
        "--strings",
        nargs="*",
        default=[],
        metavar="NAME",
        help="Specific string names to force-retranslate",
    )
    args = parser.parse_args(raw_args)

    if args.backend == "deepl" and not args.auth_key:
        parser.error("--auth-key is required for the deepl backend")

    english_strings = load_english_strings()
    language_tree, translate_list = load_language_trees(english_strings, args.strings or [])

    if not translate_list:
        print("Nothing to translate.")
        return

    if args.backend == "deepl":
        translate_with_deepl(translate_list, english_strings, language_tree, args.auth_key)
    else:
        translate_with_claude(translate_list, english_strings, language_tree)

    write_language_trees(translate_list, language_tree)
    print("Done.")


if __name__ == "__main__":
    main()
