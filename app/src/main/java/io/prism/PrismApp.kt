package io.prism

import android.app.Application

class PrismApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: PrismApp
            private set
    }
}