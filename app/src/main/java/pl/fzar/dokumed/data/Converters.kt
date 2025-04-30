package pl.fzar.dokumed.data

import androidx.room.TypeConverter
import kotlinx.datetime.LocalDate
import pl.fzar.dokumed.data.model.MedicalRecordType
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class LocalDateConverter {
    @TypeConverter
    fun fromLocalDate(date: LocalDate): Long {
        return date.toEpochDays().toLong()
    }

    @TypeConverter
    fun toLocalDate(value: Long): LocalDate {
        return LocalDate.fromEpochDays(value.toInt())
    }
}

class MedicalRecordTypeConverter {
    @TypeConverter
    fun fromMedicalRecordType(value: MedicalRecordType): String {
        return value.name
    }

    @TypeConverter
    fun toMedicalRecordType(value: String): MedicalRecordType {
        return MedicalRecordType.valueOf(value)
    }
}

class UuidConverter {
    @OptIn(ExperimentalUuidApi::class)
    @TypeConverter
    fun fromUuid(uuid: Uuid): String {
        return uuid.toString()
    }

    @OptIn(ExperimentalUuidApi::class)
    @TypeConverter
    fun toUuid(value: String): Uuid {
        return Uuid.parse(value)
    }
}
