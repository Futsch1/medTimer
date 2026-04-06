#!/bin/bash

use_optipng=false
if [ "$1" = "--optipng" ]; then
	use_optipng=true
fi

screenshots=(2 3 5 1 6)
names=(medicine reminder notification overview analysis)

dest_files=()

for i in "${!names[@]}"; do
	dest=./app/src/main/res/drawable/intro_"${names[$i]}".png
	cp -f ./fastlane/metadata/android/en-US/images/phoneScreenshots/"${screenshots[$i]}".png "$dest"
	dest_files+=("$dest")
done

locales=('ar' 'bg' 'cs-CZ' 'da-DK' 'de-DE' 'es-ES' 'el-GR' 'fi-FI' 'fr-FR' 'hu-HU' 'it-IT' 'iw-IL' 'nl-NL' 'pl-PL' 'pt-BR' 'ru-RU' 'sv-SE' 'ta-IN' 'tr-TR' 'uk' 'zh-CN' 'zh-TW')

for l in "${locales[@]}"; do
	lang="${l:0:2}"
	region="${l:3:5}"

	if [ -d ./app/src/main/res/drawable-"${l}" ]; then
		dir=./app/src/main/res/drawable-"${l}"
	elif [ -d ./app/src/main/res/drawable-"${lang}"-r"${region}" ]; then
		dir=./app/src/main/res/drawable-"${lang}"-r"${region}"
	else
		mkdir ./app/src/main/res/drawable-"${lang}" 2>/dev/null
		dir=./app/src/main/res/drawable-"${lang}"
	fi

	for i in "${!names[@]}"; do
		dest="${dir}"/intro_"${names[$i]}".png
		cp -f ./fastlane/metadata/android/"${l}"/images/phoneScreenshots/"${screenshots[$i]}".png "$dest"
		dest_files+=("$dest")
	done
done

if $use_optipng; then
	optipng -o5 "${dest_files[@]}"
else
	pngquant --quality=60-80 --force --ext .png "${dest_files[@]}"
fi
