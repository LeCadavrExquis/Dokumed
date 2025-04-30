package pl.fzar.dokumed.data.model

import android.content.Context
import androidx.annotation.StringRes
import pl.fzar.dokumed.R

enum class MedicalRecordType {
    CONSULTATION,
    MEASUREMENT,
    LAB_TEST,
    IMAGING,
    SYMPTOM,
    MEDICATION,
    REHABILITATION,
    HOSPITALIZATION,
    PROCEDURE,
    SURGERY,
}

val consultationRecords = listOf(
    MedicalRecordType.CONSULTATION,
    MedicalRecordType.SURGERY,
    MedicalRecordType.HOSPITALIZATION,
    MedicalRecordType.PROCEDURE,
    MedicalRecordType.REHABILITATION,
)

val measurementRecords = listOf(
    MedicalRecordType.MEASUREMENT,
    MedicalRecordType.SYMPTOM,
    MedicalRecordType.MEDICATION,
)

val clinicalDataRecords = listOf(
    MedicalRecordType.LAB_TEST,
    MedicalRecordType.IMAGING,
)

@StringRes
fun MedicalRecordType.getStringResId(): Int {
    return when (this) {
        MedicalRecordType.CONSULTATION -> R.string.medical_record_type_consultation
        MedicalRecordType.MEASUREMENT -> R.string.medical_record_type_measurement
        MedicalRecordType.LAB_TEST -> R.string.medical_record_type_lab_test
        MedicalRecordType.IMAGING -> R.string.medical_record_type_imaging
        MedicalRecordType.SYMPTOM -> R.string.medical_record_type_symptom
        MedicalRecordType.MEDICATION -> R.string.medical_record_type_medication
        MedicalRecordType.REHABILITATION -> R.string.medical_record_type_rehabilitation
        MedicalRecordType.HOSPITALIZATION -> R.string.medical_record_type_hospitalization
        MedicalRecordType.PROCEDURE -> R.string.medical_record_type_procedure
        MedicalRecordType.SURGERY -> R.string.medical_record_type_surgery
    }
}

fun MedicalRecordType.getLocalizedString(context: Context): String {
    return context.getString(this.getStringResId())
}