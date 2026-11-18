package com.example

import android.app.Application

/**
 * Application subclass holding the manual DI [AppContainer].
 */
class SalesApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
