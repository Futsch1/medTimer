package com.futsch1.medtimer.feature.ui.medicine.ocr.di

import com.futsch1.medtimer.feature.ui.medicine.ocr.MlKitPackageTextRecognizer
import com.futsch1.medtimer.feature.ui.medicine.ocr.PackageTextRecognizer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PackageTextRecognizerModule {
    @Binds
    @Singleton
    abstract fun bindPackageTextRecognizer(impl: MlKitPackageTextRecognizer): PackageTextRecognizer
}
