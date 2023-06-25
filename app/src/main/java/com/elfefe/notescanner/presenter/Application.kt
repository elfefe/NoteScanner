package com.elfefe.notescanner.presenter

import android.app.Application

class Application: Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: com.elfefe.notescanner.presenter.Application
    }
}