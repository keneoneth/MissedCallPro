package com.cd.missedcallpro
import android.app.Application
import com.cd.missedcallpro.data.AppStateStore
import com.cd.missedcallpro.data.FormStore

class App : Application() {
    lateinit var store: AppStateStore
        private set
    lateinit var container: AppContainer
        private set

    lateinit var formStore: FormStore
        private set

    override fun onCreate() {
        super.onCreate()
        store = AppStateStore(this)
        container = AppContainer()
        formStore = FormStore()
    }
}