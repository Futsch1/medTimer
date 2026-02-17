package com.futsch1.medtimer.exporters

import androidx.fragment.app.FragmentManager
import com.futsch1.medtimer.helpers.ProgressDialogFragment
import java.io.File

abstract class Export internal constructor(private val fragmentManager: FragmentManager) {
    @Throws(ExporterException::class)
    fun export(file: File) {
        val progressDialog = ProgressDialogFragment()
        progressDialog.show(fragmentManager, "exporting")
        exportInternal(file)
        progressDialog.dismiss()
    }

    @Throws(ExporterException::class)
    protected abstract fun exportInternal(file: File)

    abstract val extension: String
    abstract val type: String

    class ExporterException : Exception()
}
