package pl.fzar.dokumed.di

import android.content.Context
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import pl.fzar.dokumed.di.AppQualifiers // Import the qualifiers

val securityModule = module {
    single(AppQualifiers.DokumedSecurePrefs) { // Use the named qualifier
        val context = androidContext()
        context.getSharedPreferences("dokumed_secure_prefs", Context.MODE_PRIVATE)
    }
}
