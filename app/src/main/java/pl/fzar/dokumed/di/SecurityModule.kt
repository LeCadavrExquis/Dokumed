package pl.fzar.dokumed.di

import android.content.Context
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val securityModule = module {
    single {
        val context = androidContext()
        context.getSharedPreferences("dokumed_secure_prefs", Context.MODE_PRIVATE)
    }
}
