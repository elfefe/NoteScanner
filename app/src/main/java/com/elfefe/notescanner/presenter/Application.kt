package com.elfefe.notescanner.presenter

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class Application: Application() {
    val scope = CoroutineScope(Dispatchers.Default)
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: com.elfefe.notescanner.presenter.Application
    }
}