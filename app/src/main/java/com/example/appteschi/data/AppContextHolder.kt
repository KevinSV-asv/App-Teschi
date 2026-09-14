package com.example.appteschi.data

import android.content.Context

object AppContextHolder {
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun requireContext(): Context = appContext
        ?: throw IllegalStateException("AppContextHolder no está inicializado. Llama a AppContextHolder.init(...) en MainActivity.")
}
