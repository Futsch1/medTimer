#!/bin/bash

screenshots=(2 3 5 1 6)
names=(medicine reminder notification overview analysis)

for i in $(seq 0 4); do
	cp -f -r ./fastlane/metadata/android/en-US/images/phoneScreenshots/"${screenshots[$i]}".png ./app/src/main/res/drawable/intro_"${names[$i]}".png
done

locales=('ar' 'bg' 'da-DK' 'de-DE' 'es-ES' 'el-GR' 'fi-FI' 'fr-FR' 'it-IT' 'nl-NL' 'pl-PL' 'pt-BR' 'ru-RU' 'sv-SE' 'ta-IN' 'tr-TR' 'uk' 'zh-CN' 'zh-TW')

for l in "${locales[@]}"; do
	mkdir ./app/src/main/res/drawable-"${l:0:2}" 2>/dev/null
	for i in $(seq 0 4); do
		if [ -d ./app/src/main/res/drawable-"${l}" ]; then
			cp -f -r ./fastlane/metadata/android/"${l}"/images/phoneScreenshots/"${screenshots[$i]}".png ./app/src/main/res/drawable-"${l}"/intro_"${names[$i]}".png
		elif [ -d ./app/src/main/res/drawable-"${l:0:2}"-r"${l:3:5}" ]; then
			cp -f -r ./fastlane/metadata/android/"${l}"/images/phoneScreenshots/"${screenshots[$i]}".png ./app/src/main/res/drawable-"${l:0:2}"-r"${l:3:5}"/intro_"${names[$i]}".png
		else
			cp -f -r ./fastlane/metadata/android/"${l}"/images/phoneScreenshots/"${screenshots[$i]}".png ./app/src/main/res/drawable-"${l:0:2}"/intro_"${names[$i]}".png
		fi
	done
done
