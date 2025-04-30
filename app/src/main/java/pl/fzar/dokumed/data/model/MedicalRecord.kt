package pl.fzar.dokumed.data.model

import kotlinx.datetime.LocalDate
import kotlin.uuid.Uuid

open class MedicalRecord(
    open val id: Uuid = Uuid.random(),
    open val date: LocalDate,
    open val type: MedicalRecordType,
    open val description: String?,
    open val notes: String?,
    open val tags: List<String> = emptyList(),
)

val dummyRecords = listOf(
    ConsultationRecord(
        date = LocalDate(2025, 4, 20),
        type = MedicalRecordType.CONSULTATION,
        description = "Kontrolna wizyta u internisty.",
        notes = "Pacjent czuje się dobrze.",
        doctor = "Dr. Anna Kowalska",
        tags = listOf("Ważne", "Internista")
    ),
    MeasurementRecord(
        date = LocalDate(2025, 4, 22),
        type = MedicalRecordType.MEASUREMENT,
        description = "Pomiar ciśnienia krwi.",
        notes = "Zmierzono rano po przebudzeniu.",
        value = 120.0,
        unit = "mmHg",
        tags = listOf(),
        testName = "Ciśnienie krwi"
    ),
    ClinicalDataRecord(
        date = LocalDate(2025, 4, 25),
        type = MedicalRecordType.LAB_TEST,
        description = "Wyniki morfologii.",
        notes = "Wszystkie parametry w normie.",
        testName = "Morfologia krwi",
    ),
    ClinicalDataRecord(
        date = LocalDate(2025, 4, 15),
        type = MedicalRecordType.IMAGING,
        description = "Zdjęcie RTG klatki piersiowej.",
        notes = null,
        testName = "RTG",
        tags = listOf(),
        filePath = null,
        fileMimeType = null,
    ),
    MeasurementRecord(
        date = LocalDate(2025, 4, 23),
        type = MedicalRecordType.SYMPTOM,
        description = "Ból głowy.",
        notes = "Tępy, zlokalizowany w okolicy czołowej.",
        tags = listOf("Ból"),
        testName = "Ból głowy",
        value = null,
        unit = null,
    ),
    MeasurementRecord(
        date = LocalDate(2025, 4, 1),
        type = MedicalRecordType.MEDICATION,
        description = "Przyjmowanie leku na nadciśnienie.",
        notes = "Dawka 5mg raz dziennie rano.",
        tags = listOf("Leki", "Nadciśnienie"),
        testName = "Medicardio",
        value = 2.0,
        unit = "mg"
    ),
    ConsultationRecord(
        date = LocalDate(2025, 4, 24),
        type = MedicalRecordType.REHABILITATION,
        description = "Sesja rehabilitacyjna po urazie kolana.",
        notes = "Wykonano ćwiczenia wzmacniające mięśnie uda.",
        tags = listOf("Rehabilitacja", "Kolano"),
        filePath = null,
        fileMimeType = null,
        doctor = null
    ),
    ConsultationRecord(
        date = LocalDate(2025, 3, 20),
        type = MedicalRecordType.HOSPITALIZATION,
        description = "Pobyt w szpitalu z powodu zapalenia płuc.",
        notes = "Leczenie antybiotykami.",
        tags = listOf("Szpital", "Zapalenie płuc"),
        filePath = null,
        fileMimeType = null,
        doctor = null
    ),
    ClinicalDataRecord(
        date = LocalDate(2025, 4, 5),
        type = MedicalRecordType.PROCEDURE,
        description = "EKG.",
        notes = "Wynik bez istotnych odchyleń.",
        tags = listOf("Diagnostyka"),
        filePath = null,
        fileMimeType = null,
        testName = "EKG",
    ),
    ConsultationRecord(
        date = LocalDate(2025, 2, 1),
        type = MedicalRecordType.SURGERY,
        description = "Operacja usunięcia wyrostka robaczkowego.",
        notes = "Przebieg bez komplikacji.",
        tags = listOf("Operacja"),
        filePath = null,
        fileMimeType = null,
        doctor = "Dr. Piotr Nowak"
    )
)