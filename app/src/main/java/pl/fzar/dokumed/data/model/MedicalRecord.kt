package pl.fzar.dokumed.data.model

import kotlinx.datetime.LocalDate
import kotlin.uuid.Uuid

data class MedicalRecord(
    val id: Uuid = Uuid.random(),
    val date: LocalDate,
    val type: MedicalRecordType,
    val description: String?,
    val doctor: String?,
    val notes: String?,
    val tags: List<String> = emptyList(),
    val clinicalData: List<ClinicalData> = emptyList(),
    val measurements: List<Measurement> = emptyList(),
)

val dummyRecords = listOf(
    MedicalRecord(
        date = LocalDate(2025, 5, 1),
        type = MedicalRecordType.CONSULTATION,
        description = "Wizyta kontrolna u lekarza rodzinnego",
        notes = "Zalecenia: więcej ruchu",
        tags = listOf("lekarz", "kontrola"),
        doctor = "dr Michał Pałkowski",
    ),
    MedicalRecord(
        date = LocalDate(2025, 4, 28),
        type = MedicalRecordType.MEASUREMENT,
        description = "Ciśnienie krwi",
        notes = "Rano, przed śniadaniem",
        tags = listOf("ciśnienie"),
        measurements = listOf(Measurement(value = 120.0, unit = "mmHg")),
        doctor = "dr Kacper Gała",
    ),
    MedicalRecord(
        date = LocalDate(2025, 4, 20),
        type = MedicalRecordType.LAB_TEST,
        description = "Badanie krwi - morfologia",
        notes = "Wszystko w normie",
        tags = listOf("krew"),
        clinicalData = listOf(ClinicalData(filePath = null, fileMimeType = null)),
        doctor = "dr Patryk Kukła",
    ),
    MedicalRecord(
        date = LocalDate(2025, 4, 15),
        type = MedicalRecordType.IMAGING,
        description = "RTG klatki piersiowej",
        notes = "Brak zmian patologicznych",
        tags = listOf("rtg"),
        clinicalData = listOf(ClinicalData(filePath = null, fileMimeType = null)),
        doctor = "dr Monika Piekło",
    ),
    MedicalRecord(
        date = LocalDate(2025, 4, 10),
        type = MedicalRecordType.SYMPTOM,
        description = "Ból głowy",
        notes = "Tępy, popołudniami",
        tags = listOf("ból", "głowa"),
        measurements = listOf(Measurement(value = null, unit = null)),
        doctor = "dr Kasia Demon",
    ),
    MedicalRecord(
        date = LocalDate(2025, 4, 5),
        type = MedicalRecordType.MEDICATION,
        description = "Amlodypina",
        notes = "Dawka 5mg raz dziennie",
        tags = listOf("leki", "nadciśnienie"),
        measurements = listOf(Measurement(value = 5.0, unit = "mg")),
        doctor = "dr Wacek Placek",
    ),
    MedicalRecord(
        date = LocalDate(2025, 3, 20),
        type = MedicalRecordType.HOSPITALIZATION,
        description = "Pobyt w szpitalu z powodu zapalenia płuc",
        notes = "Leczenie antybiotykami",
        tags = listOf("szpital", "zapalenie płuc"),
        doctor = "dr Piotr Kolanko",
    ),
    MedicalRecord(
        date = LocalDate(2025, 3, 10),
        type = MedicalRecordType.PROCEDURE,
        description = "EKG serca",
        notes = "Wynik prawidłowy",
        tags = listOf("ekg"),
        clinicalData = listOf(ClinicalData(filePath = null, fileMimeType = null)),
        doctor = "dr Paweł Bułka",
    ),
    MedicalRecord(
        date = LocalDate(2025, 2, 1),
        type = MedicalRecordType.SURGERY,
        description = "Operacja usunięcia wyrostka robaczkowego",
        notes = "Przebieg bez komplikacji",
        tags = listOf("operacja", "wyrostek"),
        doctor = "dr Marek Robaczek",
    )
)