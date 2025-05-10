package pl.fzar.dokumed.data.remote

import android.content.Context
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import pl.fzar.dokumed.data.model.MedicalRecord
import pl.fzar.dokumed.ui.profile.ProfileScreenState
import java.io.File
import java.io.StringWriter
import com.opencsv.CSVWriter // Added for CSV writing
import java.util.Base64 // Added for Basic Auth encoding

class WebDavServiceImpl(private val applicationContext: Context) : WebDavService {

    private val ktorClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        install(Auth) {
            basic {
                // Credentials will be dynamically set per request
            }
        }
        // expectSuccess = true // Global expectSuccess can be set here if desired
    }

    override suspend fun syncProfileData(
        profileData: ProfileScreenState,
        medicalRecords: List<MedicalRecord>
    ): SyncResult {
        if (profileData.webdavUrl.isBlank() || profileData.webdavUsername.isBlank() || profileData.webdavPassword.isBlank()) {
            return SyncResult(success = false, message = "Error: WebDAV credentials missing.")
        }

        val baseUrl = if (profileData.webdavUrl.endsWith("/")) profileData.webdavUrl else "${profileData.webdavUrl}/"
        val profileJsonFileName = "profile_info.json"
        val medicalRecordsCsvFileName = "medical_records.csv"
        val attachmentsDirName = "attachments/"
        val attachmentsDirUrl = "${baseUrl}${attachmentsDirName}"

        var overallSuccess = true
        var finalMessage = "Sync completed successfully."
        var profileUploadMsg: String? = null
        var csvUploadMsg: String? = null
        val attachmentMsgs = mutableListOf<String>()
        var attachmentsDirMsg: String? = null

        try {
            // Step 1: Upload Profile JSON
            profileUploadMsg = "Uploading profile information..."
            val profileJsonString = Json.encodeToString(profileData.copy(webdavPassword = "")) // Exclude password
            val profileUploadUrl = "${baseUrl}${profileJsonFileName}"

            val profileResponse: HttpResponse = ktorClient.put(profileUploadUrl) {
                headers {
                    val authHeader = "Basic " + Base64.getEncoder().encodeToString("${profileData.webdavUsername}:${profileData.webdavPassword}".toByteArray(Charsets.UTF_8))
                    append(HttpHeaders.Authorization, authHeader)
                }
                contentType(ContentType.Application.Json)
                setBody(profileJsonString)
                // expectSuccess = false // Removed
            }

            if (!profileResponse.status.isSuccess()) {
                val errorBody = try { profileResponse.bodyAsText() } catch (e: Exception) { "Could not read error body." }
                profileUploadMsg = "Failed to upload profile JSON. Status: ${profileResponse.status}. Details: $errorBody"
                overallSuccess = false
            } else {
                profileUploadMsg = "Profile information uploaded."
            }

            // Step 2: Create attachments directory on WebDAV server
            attachmentsDirMsg = "Creating attachments directory..."
            var attachmentsDirReady = false
            try {
                val mkcolResponse: HttpResponse = ktorClient.request(attachmentsDirUrl) {
                    method = HttpMethod("MKCOL")
                    headers {
                        val authHeader = "Basic " + Base64.getEncoder().encodeToString("${profileData.webdavUsername}:${profileData.webdavPassword}".toByteArray(Charsets.UTF_8))
                        append(HttpHeaders.Authorization, authHeader)
                    }
                    // expectSuccess = false // Removed
                }

                if (mkcolResponse.status == HttpStatusCode.Created) {
                    attachmentsDirMsg = "Attachments directory created."
                    attachmentsDirReady = true
                } else if (mkcolResponse.status == HttpStatusCode.MethodNotAllowed || mkcolResponse.status.value == 409) {
                    attachmentsDirMsg = "Attachments directory already exists."
                    attachmentsDirReady = true
                } else if (mkcolResponse.status.isSuccess()) {
                    attachmentsDirMsg = "Attachments directory ready (Status: ${mkcolResponse.status})."
                    attachmentsDirReady = true
                } else {
                    val errorBody = try { mkcolResponse.bodyAsText() } catch (e: Exception) { "Could not read error body." }
                    attachmentsDirMsg = "Failed to create attachments directory. Status: ${mkcolResponse.status}. Details: $errorBody. Attachment uploads may fail."
                    overallSuccess = false
                }
            } catch (e: Exception) {
                attachmentsDirMsg = "Error creating attachments directory: ${e.message}. Attachment uploads may fail."
                overallSuccess = false
            }

            // Step 3: Fetch, Convert, and Upload Medical Records CSV
            if (medicalRecords.isNotEmpty()) {
                csvUploadMsg = "Converting medical records to CSV..."
                val medicalRecordsCsv = convertMedicalRecordsToCsv(medicalRecords)
                val csvUploadUrl = "${baseUrl}${medicalRecordsCsvFileName}"

                csvUploadMsg = "Uploading medical records CSV..."
                val csvResponse: HttpResponse = ktorClient.put(csvUploadUrl) {
                    headers {
                        val authHeader = "Basic " + Base64.getEncoder().encodeToString("${profileData.webdavUsername}:${profileData.webdavPassword}".toByteArray(Charsets.UTF_8))
                        append(HttpHeaders.Authorization, authHeader)
                    }
                    contentType(ContentType.Text.CSV)
                    setBody(medicalRecordsCsv)
                    // expectSuccess = false // Removed
                }

                if (!csvResponse.status.isSuccess()) {
                    val errorBody = try { csvResponse.bodyAsText() } catch (e: Exception) { "Could not read error body." }
                    csvUploadMsg = "Failed to upload medical records CSV. Status: ${csvResponse.status}. Details: $errorBody"
                    overallSuccess = false
                } else {
                    csvUploadMsg = "Medical records CSV uploaded."
                }
            } else {
                csvUploadMsg = "No medical records to upload."
            }

            // Step 4: Upload Attachments
            if (attachmentsDirReady && medicalRecords.isNotEmpty()) {
                var allAttachmentsUploadedSuccessfully = true
                for (record in medicalRecords) {
                    for (clinicalData in record.clinicalData) {
                        clinicalData.filePath?.let { path ->
                            val file = File(path)
                            if (file.exists()) {
                                val attachmentUploadUrl = "${attachmentsDirUrl}${file.name}"
                                try {
                                    val fileUploadResponse: HttpResponse = ktorClient.put(attachmentUploadUrl) {
                                        headers {
                                            val authHeader = "Basic " + Base64.getEncoder().encodeToString("${profileData.webdavUsername}:${profileData.webdavPassword}".toByteArray(Charsets.UTF_8))
                                            append(HttpHeaders.Authorization, authHeader)
                                        }
                                        setBody(file.readBytes())
                                        contentType(ContentType.Application.OctetStream)
                                        // expectSuccess = false // Removed
                                    }

                                    if (!fileUploadResponse.status.isSuccess()) {
                                        allAttachmentsUploadedSuccessfully = false
                                        val errorBody = try { fileUploadResponse.bodyAsText() } catch (e: Exception) { "Could not read error body." }
                                        attachmentMsgs.add("Failed to upload ${file.name}. Status: ${fileUploadResponse.status}. Details: $errorBody")
                                    } else {
                                        attachmentMsgs.add("Uploaded ${file.name}.")
                                    }
                                } catch (e: Exception) {
                                    allAttachmentsUploadedSuccessfully = false
                                    attachmentMsgs.add("Error uploading ${file.name}: ${e.message}")
                                }
                            }
                        }
                    }
                }
                if (!allAttachmentsUploadedSuccessfully) overallSuccess = false
            } else if (!attachmentsDirReady && medicalRecords.any { it.clinicalData.any { cd -> cd.filePath != null } }) {
                attachmentMsgs.add("Skipping attachment uploads as attachments directory is not ready.")
                overallSuccess = false
            }

            if (!overallSuccess) {
                finalMessage = "Sync completed with issues. Check status details."
            }

        } catch (e: io.ktor.client.plugins.ClientRequestException) {
            val response = e.response
            val errorBody = try { response.bodyAsText() } catch (ex: Exception) { "Could not read error body." }
            finalMessage = "Error: Network request failed: ${response.status}. Details: $errorBody. Message: ${e.message}"
            overallSuccess = false
        } catch (e: java.io.IOException) {
            finalMessage = "Error: Network I/O error: ${e.message}"
            overallSuccess = false
        } catch (e: Exception) {
            finalMessage = "Error: An unexpected error occurred during sync: ${e.message}"
            overallSuccess = false
            e.printStackTrace()
        }
        return SyncResult(overallSuccess, finalMessage, profileUploadMsg, csvUploadMsg, attachmentMsgs, attachmentsDirMsg)
    }

    private fun convertMedicalRecordsToCsv(records: List<MedicalRecord>): String {
        val stringWriter = StringWriter()
        CSVWriter(stringWriter).use { csvWriter -> // Use try-with-resources for CSVWriter
            // Define header
            val header = arrayOf(
                "id", "type", "date", "description", "notes", "doctor",
                "tags", "measurements_summary", "clinical_data_files"
            )
            csvWriter.writeNext(header)

            // Write data rows
            records.forEach { record ->
                val clinicalFilePaths = record.clinicalData.mapNotNull { it.filePath?.substringAfterLast('/') }.joinToString(";")
                val row = arrayOf(
                    record.id.toString(),
                    record.type.name,
                    record.date.toString(), // Ensure date is formatted as a string if it's not already
                    record.description ?: "",
                    record.notes ?: "",
                    record.doctor ?: "",
                    record.tags.joinToString(";"),
                    record.measurements.joinToString(";") { "$${it.value}${it.unit}" /* TODO: not implemented */ },
                    clinicalFilePaths
                )
                csvWriter.writeNext(row)
            }
        }
        return stringWriter.toString()
    }

    override fun close() {
        ktorClient.close()
    }
}
