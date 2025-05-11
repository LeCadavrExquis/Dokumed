package pl.fzar.dokumed.di

import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import pl.fzar.dokumed.data.AppDatabase
import pl.fzar.dokumed.data.remote.WebDavService
import pl.fzar.dokumed.data.remote.WebDavServiceImpl
import pl.fzar.dokumed.data.repository.MedicalRecordRepository
import pl.fzar.dokumed.data.repository.MedicalRecordRepositoryImpl
import pl.fzar.dokumed.data.repository.ProfileRepository
import pl.fzar.dokumed.data.repository.ProfileRepositoryImpl
import pl.fzar.dokumed.data.repository.TagRepository
import pl.fzar.dokumed.data.repository.TagRepositoryImpl
import pl.fzar.dokumed.security.KeystoreHelper
import pl.fzar.dokumed.security.PinViewModel
import pl.fzar.dokumed.ui.export.ExportViewModel
import pl.fzar.dokumed.ui.medicalRecord.MedicalRecordViewModel
import pl.fzar.dokumed.ui.profile.ProfileViewModel
import pl.fzar.dokumed.ui.statistics.StatisticsViewModel
import pl.fzar.dokumed.util.FileUtil

val databaseModule = module {
    // Database
    single { AppDatabase.getDatabase(androidContext()) }

    // DAOs
    single { get<AppDatabase>().medicalRecordDao() }
    single { get<AppDatabase>().tagDao() }
}

val repositoryModule = module {
    // Repositories
    single<ProfileRepository> { ProfileRepositoryImpl(get()) }
    single<TagRepository> { TagRepositoryImpl(get()) } // Depends on TagDao
    single<MedicalRecordRepository> { MedicalRecordRepositoryImpl(get(), get()) } // Depends on MedicalRecordDao and TagRepository
}

// Create a new module for services
val serviceModule = module {
    single<WebDavService> { WebDavServiceImpl(androidContext()) }
    single { KeystoreHelper(androidApplication()) } // Add KeystoreHelper singleton
}

val utilModule = module {
    single { FileUtil } // Definition for FileUtil
}

val viewModelModule = module {
    // ViewModels
    viewModel {
        ProfileViewModel(
            profileRepository = get(),
            medicalRecordRepository = get(),
            webDavService = get(), // Add WebDavService dependency
            applicationContext = androidContext()
        )
    }
    viewModel { PinViewModel(androidApplication()) } // PinViewModel now takes Application context
    viewModel { MedicalRecordViewModel(get(), get(), get()) } // Added MedicalRecordViewModel
    viewModel { StatisticsViewModel(get()) } // Added StatisticsViewModel
    viewModel { ExportViewModel(androidContext(), get(), get()) } // Added ExportViewModel, assuming context is needed like ProfileVM
}

val appModule = module {
    includes(databaseModule, repositoryModule, serviceModule, utilModule, viewModelModule) // Include utilModule and viewModelModule
}
