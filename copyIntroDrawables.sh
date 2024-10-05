#!/bin/bash

screenshots=(2 3 5 1 6)
names=(medicine reminder notification overview analysis)


for i in $(seq 0 4)
do
  cp -f -r ./fastlane/metadata/android/en-US/images/phoneScreenshots/${screenshots[$i]}.png ./app/src/main/res/drawable/intro_${names[$i]}.png
done

locales=('es-ES' 'de-DE' 'fr-FR' 'it-IT' 'zh-rCN' 'nl-NL' 'ru-RU' 'tr-TR')

for l in "${locales[@]}"
do
  mkdir ./app/src/main/res/drawable-"${l:0:2}" 2> /dev/null
  for i in $(seq 0 4)
  do
    cp -f -r ./fastlane/metadata/android/"${l}"/images/phoneScreenshots/${screenshots[$i]}.png ./app/src/main/res/drawable-${l:0:2}/intro_${names[$i]}.png
  done
done