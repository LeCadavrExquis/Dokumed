package pl.fzar.dokumed

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import pl.fzar.dokumed.di.appModule
import pl.fzar.dokumed.di.databaseModule
import pl.fzar.dokumed.di.repositoryModule
import pl.fzar.dokumed.di.securityModule
import pl.fzar.dokumed.di.viewModelModule

class DokumedApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@DokumedApp)
            modules(databaseModule, repositoryModule, viewModelModule, appModule, securityModule)
        }
    }
}
