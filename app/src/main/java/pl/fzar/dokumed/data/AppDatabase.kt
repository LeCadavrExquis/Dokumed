package pl.fzar.dokumed.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import pl.fzar.dokumed.data.dao.MedicalRecordDao
import pl.fzar.dokumed.data.dao.TagDao
import pl.fzar.dokumed.data.entity.ClinicalDataEntity
import pl.fzar.dokumed.data.entity.MeasurementEntity
import pl.fzar.dokumed.data.entity.MedicalRecordEntity
import pl.fzar.dokumed.data.entity.MedicalRecordTagCrossRef
import pl.fzar.dokumed.data.entity.TagEntity

@Database(entities = [
    MedicalRecordEntity::class,
    ClinicalDataEntity::class,
    MeasurementEntity::class,
    TagEntity::class,
    MedicalRecordTagCrossRef::class,
], version = 2, exportSchema = false)
@TypeConverters(
    MedicalRecordTypeConverter::class,
    LocalDateConverter::class,
    UuidConverter::class,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicalRecordDao(): MedicalRecordDao
    abstract fun tagDao(): TagDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "medical_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}