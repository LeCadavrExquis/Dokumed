package pl.fzar.dokumed

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.dsl.module
import pl.fzar.dokumed.data.AppDatabase
import pl.fzar.dokumed.data.repository.MedicalRecordRepository
import pl.fzar.dokumed.data.repository.MedicalRecordRepositoryImpl
import pl.fzar.dokumed.data.repository.TagRepository
import pl.fzar.dokumed.data.repository.TagRepositoryImpl
import pl.fzar.dokumed.ui.medicalRecord.MedicalRecordViewModel
import pl.fzar.dokumed.ui.statistics.StatisticsViewModel
import pl.fzar.dokumed.ui.export.ExportViewModel
import pl.fzar.dokumed.util.FileUtil
import kotlin.math.sin

class DokumedApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@DokumedApp)
            modules(appModule)
        }
    }
}

val appModule = module {
    single { AppDatabase.getDatabase(get()) }
    single { get<AppDatabase>().medicalRecordDao() }
    single { get<AppDatabase>().tagDao() }
    
    // Repositories
    single<MedicalRecordRepository> {
        MedicalRecordRepositoryImpl(get(), get())
    }
    single<TagRepository> {
        TagRepositoryImpl(get())
    }

    single { FileUtil }

    // ViewModels
    single { MedicalRecordViewModel(get<MedicalRecordRepository>(), get<TagRepository>(), get()) }
    single { StatisticsViewModel(get<MedicalRecordRepository>()) }
    single { ExportViewModel(get(), get<MedicalRecordRepository>()) }
}
