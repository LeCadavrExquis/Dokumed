package pl.fzar.dokumed.di

import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.named

object AppQualifiers {
    val DokumedPrefs: Qualifier = named("DokumedPrefs")
    val DokumedSecurePrefs: Qualifier = named("DokumedSecurePrefs")
}
