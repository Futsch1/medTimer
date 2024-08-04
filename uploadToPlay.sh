#!/usr/bin/env bash

fastlane supply --skip_upload_apk=true \
                --skip_upload_aab=true \
                --skip_upload_changelogs=true \
                --sync-image-upload=true
