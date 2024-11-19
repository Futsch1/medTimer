# How to contribute to MedTimer

All kinds of contributions are highly appreciated and will be treated with utmost respect.

## Reporting bugs and requesting new features

To report bugs or request new features, please use
the [GitHub issue tracker](https://github.com/futsch1/MedTimer/issues).

Before you report a bug or request a new feature, check if the bug is already present of if the
feature has been requested and potentially was rejected in the past.

Please use the bug and feature templates provided by the GitHub issue tracker.

## Contributing translations

Translations are welcome. You can use [Weblate](https://hosted.weblate.org/projects/medtimer/) to
provide your translation or provide a merge request.

### App text translations

When translating, please make sure that all placeholders (`%s` or `%1$s`) are part of the translated
string and were not changed. Translate all strings except those marked as `translatable=false`. If
possible, use [Android Studio](https://developer.android.com/studio/), clone and open the
project to check your translations in the app.

### Store text translations

It is also possible to translate the app's text in the Google Play Store and F-Droid store. The
texts are located in the subfolder `fastlane/metadata/android`. To translate, copy the text files
from `fastlane/metadata/android/en-US` into the respective subfolder with the target language code.

## Contributing code

Code additions are welcome! Please work on an existing issue or open a new one if you want to add
a new feature. It is recommended to wait for feedback from the maintainer before starting on the
coding work to make sure that a contribution would be accepted.

Before opening the pull request, make sure that the code compiles, the unit tests and Android tests
run successfully and that there are no new SonarQube issues introduced.

It is also recommended to add specific tests for the new code.
