import glob
import os.path
import sys
import xml.etree.ElementTree as ET

import deepl

# Open English strings.xml file
tree = ET.parse('app/src/main/res/values/strings.xml')
resources = tree.getroot()
strings = resources.findall('string')
english_strings = {}
translated_strings = {}
language_tree = {}
translate_list = []

for string_name in strings:
    if string_name.attrib.get('translatable') == 'false':
        continue
    english_strings[string_name.attrib['name']] = string_name.text

if len(sys.argv) > 2:
    translate_args = sys.argv[2:]
else:
    translate_args = []

# Search for strings in other languages and build the list of strings to be translated
for directory in glob.glob('app/src/main/res/*'):
    if directory == 'app/src/main/res/values' or not os.path.exists(directory + '/strings.xml'):
        continue
    language = directory[-2:]
    language_tree[language] = ET.parse(directory + '/strings.xml')
    resources = language_tree[language].getroot()
    language_strings = []
    for string_name in resources.findall('string'):
        language_strings.append(string_name.attrib['name'])
    for english_string_name in english_strings.keys():
        if english_string_name not in language_strings or not language_strings[language_strings.index(english_string_name)] or english_string_name in translate_args:
            translate_list.append((english_string_name, language))
auth_key = sys.argv[1]

# Translate
new_elements = {}
translator = deepl.Translator(auth_key)
for string_name, language in translate_list:
    print('Translating ' + string_name + ' to ' + language)
    translated_string = translator.translate_text(english_strings[string_name], target_lang=language, preserve_formatting=True)
    new_elements[language] = language_tree[language].find('string[@name="' + string_name + '"]')
    if new_elements[language] is None:
        new_elements[language] = ET.SubElement(language_tree[language].getroot(), 'string', name=string_name)
    new_elements[language].text = translated_string.text

# Collect all languages where a translation string was found
languages = set([tl[1] for tl in translate_list])

# Update the existing strings.xml files
for language in languages:
    with open(f'app/src/main/res/values-{language}/strings.xml', 'wb') as f:
        ET.indent(language_tree[language])
        language_tree[language].write(f, encoding='utf-8')
