package com.futsch1.medtimer.database.toModel

import com.futsch1.medtimer.core.domain.model.Barcode
import com.futsch1.medtimer.database.BarcodeEntity

fun BarcodeEntity.toModel(): Barcode = Barcode(barcode = barcode, medicineId = medicineId)

fun Barcode.toEntity(): BarcodeEntity = BarcodeEntity(barcode = barcode, medicineId = medicineId)
