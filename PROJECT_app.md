## Data Export (Details)

- The export feature uses a repository method `getAllRecordsWithDetails()` to fetch all medical records with their full details (tags, measurements, clinical data, etc.).
- This method is implemented in `MedicalRecordRepository` and `MedicalRecordRepositoryImpl` and is used by `ExportViewModel` to provide complete data for export and filtering.
# Dokumed App Documentation

## Overview
The Dokumed app is an Android application for managing medical records. It allows users to store, categorize, search, and analyze their medical history.

## Main Features
- Record management (add, edit, delete)
- Different record types (consultations, measurements, clinical data)
- Tag-based organization
- Statistics and insights
- Data export and backup
  - Export selected medical records, associated measurements (grouped by description into separate CSVs), and attached files into a single ZIP archive.
  - Option to send the exported ZIP archive via email after successful export.
- User Profile Management
  - View personal medical information: height, weight, blood type, illnesses, medications, allergies.
  - View emergency contact information (name and phone number).
  - View organ donor status.
  - Edit profile details, medication reminders, and cloud sync settings on separate, dedicated screens.
  - Profile data is stored securely using SharedPreferences.
  - Allow users to set reminders for medications, including time, medication name, and dosage (managed in `MedicationReminderScreen`).
- **Onboarding Carousel**:
  - Displayed on the first app launch.
  - Informs the user about the app's purpose and main features.
  - Guides the user through setting up a mandatory PIN for app security.
  - Optionally allows the user to input initial profile information (can be skipped).
- **Cloud Synchronization (WebDAV)**:
  - Users can configure WebDAV server credentials (URL, username, password) in the `CloudSyncScreen`.
  - Synchronization is optional and independent of local storage.
  - Medical records are exported in CSV format.
  - Profile information is exported in JSON format (using Kotlinx Serialization).
  - Attached files associated with medical records are also synced.
- **Android Home Screen Widgets**:
  - **Profile Summary Glance Widget**: Displays a summary of the user\\\'s profile information (name, blood type, allergies, medications, emergency contact, organ donor status, height, weight) directly on the home screen. Uses Glance API and Koin for data fetching from `ProfileRepository`. Updated periodically by `ProfileWidgetUpdateWorker` when profile data changes in the app.
  - **Panic Button Glance Widget**: A home screen button that, when tapped, initiates an emergency sequence: sends an SMS and makes a phone call to a pre-configured emergency contact number. Uses Glance API. Configuration is handled by `PanicWidgetConfigureActivity.kt` and actions by `PanicHandlerActivity.kt`.

## Architecture
The app follows the MVVM (Model-View-ViewModel) architecture pattern with repository abstractions:

### Layers
1. **UI Layer** (Presentation)
   - Composable screens
   - ViewModels

2. **Domain Layer**
   - Use cases (implicit in ViewModels)
   - Domain models

3. **Data Layer**
   - Repositories
   - Data sources (Room database)
   - Entities

## Key Components

### Data Access
- **Repositories**:
  - `MedicalRecordRepository`: Interface for medical record operations
  - `TagRepository`: Interface for tag operations
  - `MedicalRecordRepositoryImpl`: Implementation of the medical record repository
  - `TagRepositoryImpl`: Implementation of the tag repository
  - `ProfileRepository`: Interface for profile data operations (New - uses SharedPreferences)
  - `ProfileRepositoryImpl`: Implementation of the profile repository (New)

- **DAOs**:
  - `MedicalRecordDao`: Data access for medical records
  - `TagDao`: Data access for tags
- **Room Database**:
  - Schema export is configured in `app/build.gradle.kts` to output to the `$projectDir/schemas` directory.

### Services
- **`WebDavService`**: Interface for WebDAV operations, such as profile and medical data synchronization.
- **`WebDavServiceImpl`**: Implementation of `WebDavService`, handling the HTTP communication with a WebDAV server.

### ViewModels
- `MedicalRecordViewModel`: Manages medical record CRUD operations
  - Uses `saveMedicalRecord()` as the primary method for creating/updating records
  - Properly synchronizes measurements and clinical data from records to its internal state
  - Handles clinical data deletion tracking
  - Maintains operation state (Idle, Loading, Saving, Success, Error)
- `ExportViewModel`: Manages state and logic for the data export process, including filtering records for export.
- `StatisticsViewModel`: Handles statistical analysis of records
- `ProfileViewModel`: Manages user profile data, interacting with `ProfileRepository`. It now delegates WebDAV synchronization tasks to `WebDavService`. Its state is used by `ProfileScreen`, `ProfileEditScreen`, `MedicationReminderScreen`, and `CloudSyncScreen`.
- `PinViewModel`: Manages PIN setup and authentication.

### UI Components
- `MedicalRecordEditScreen`: Screen for adding/editing records
- `MedicalRecordListScreen`: Screen for viewing and filtering records
- `StatisticsScreen`: Screen for viewing statistics and insights
- `StatisticsChart`: Component for rendering different chart types
- `SettingsScreen`: Screen for displaying user profile information and providing navigation to `ProfileEditScreen`, `MedicationReminderScreen`, and `CloudSyncScreen`. (Renamed from ProfileScreen)
- `ProfileEditScreen`: Screen for editing general user profile information (New).
- `MedicationReminderScreen`: Screen for managing medication reminder settings (New).
- `CloudSyncScreen`: Screen for managing WebDAV cloud synchronization settings (New).
- `PinScreen`: Screen for PIN authentication.
- `OnboardingScreen`: Multi-step screen for first-time user setup (New).
- `ProfileSummaryWidget.kt`: In-app Composable widget displaying a summary of profile information. Takes `ProfileUIState` as a parameter.
- `ProfileGlanceWidget.kt`: Android Home Screen widget (Glance API) displaying a summary of profile information. Fetches data via `ProfileRepository` (injected by Koin).
- `ProfileGlanceWidgetReceiver.kt`: The `AppWidgetReceiver` for the `ProfileGlanceWidget`.
- `PanicGlanceWidget.kt`: Android Home Screen widget (Glance API) for the panic button functionality.
- `PanicGlanceWidgetReceiver.kt`: The `AppWidgetReceiver` for the `PanicGlanceWidget`.
- `PanicHandlerActivity.kt`: Transparent activity to handle permissions and execute actions (SMS, call) for the `PanicGlanceWidget`.
- `PanicWidgetConfigureActivity.kt`: Configuration activity for the `PanicGlanceWidget`. (Renamed from `PanicButtonWidgetProvider.kt`)
- `ProfileWidgetUpdateWorker.kt`: A `CoroutineWorker` that updates the `ProfileGlanceWidget` when profile data is changed within the app. Enqueued by `ProfileViewModel`.

## Non-functional Requirements
- **Performance**: The app should handle hundreds of medical records without performance degradation
- **Security**: Medical data should be stored securely on the device
- **Usability**: Intuitive UI for easy record management
- **Reliability**: Consistent data storage and retrieval

## Known Issues and Fixes
- **Clinical Data Not Saved in DB (Root Cause: insertMedicalRecord)**: Fixed an issue where clinical data was not saved when using the basic `insertMedicalRecord` method. The code now ensures that the `id` and `medicalRecordId` fields are set for every clinical data item, so all attachments are reliably persisted in the database.
- **Clinical Data Not Visible in App**: Fixed an issue where clinical data was not visible after saving. The mapping between the database entity and the in-memory model now ensures that all required fields (`id`, `recordId`, `filePath`, `fileMimeType`) are set both when saving and loading, so clinical data is always correctly linked and displayed.
- **Record Saving Issue**: Fixed an issue where records weren't being properly saved to the database. The problem was related to the synchronization between the UI state and the ViewModel's internal state. Now records are saved using the `saveMedicalRecord()` method which correctly handles all related data (measurements, clinical data, tags).

- **Clinical Data Not Saved Properly**: Fixed an issue where clinical data attachments were not reliably saved with medical records. The root cause was a mismatch between the in-memory model and the database entity: clinical data items did not always have a unique, stable `id` or the correct `recordId` set before saving. Now, before saving or updating a record, all clinical data items are assigned a unique `id` and the correct `recordId`, ensuring proper persistence and retrieval.

- Fixed bug where clinical data was not re-inserted during medical record updates.

## Medical Record Management
- View a list of medical records.
- View details of a specific medical record, including attached files.
- Open attached files from the detail view.
- Filter records by type, date range, or tags.
- Add new medical records of various types (Consultation, Lab Test, Imaging, Procedure, Measurement, Medication, Symptom).
- Edit existing medical records.
- Delete medical records.
- Attach multiple files (e.g., PDFs, images) to relevant record types (Consultation, Lab Test, Imaging, Procedure).
- **Open files (e.g., PDF, images) from external apps (like email clients) directly into Dokumed, automatically navigating to the 'New Record' screen with the file pre-attached.**
- Add tags to records for easier categorization and searching.

## User Profile
- Users can view their personal medical information including:
  - Height
  - Weight
  - Blood Type
  - Known illnesses
  - Current medications
  - Allergies
  - Emergency contact name and phone number
  - Organ donor status
- Users can edit these details in a dedicated `ProfileEditScreen`.
- Medication reminders (enable, name, dosage, time) can be managed in `MedicationReminderScreen`.
- WebDAV cloud sync settings (URL, username, password) can be managed in `CloudSyncScreen`.
- Data is saved locally using SharedPreferences, managed via the `ProfileRepository`.

## File Handling
- Attach files (PDF, images) to records from device storage.
- View attached files within the app.
- Open files (PDF, images) from external apps (e.g., email attachments) directly into the "New Record" screen with the file pre-attached.

## Data Visualization
- View trends and charts for specific measurement types over time (placeholder/future feature).

# Dokumed Application

## Brief Description
Dokumed is a mobile application for managing medical records securely and efficiently.

## Main Features
-   Secure storage of medical records.
-   Categorization and tagging of records.
-   Data export functionality.
-   Statistical analysis of medical data.
-   User profile management.
-   **Onboarding process shown only on first application launch.**
-   **Optional PIN setup during the onboarding process.**
-   Import medical documents via sharing intents.
-   **Panic Button Widget**: Allows users to place a widget on their home screen that, when tapped, immediately calls a pre-configured emergency contact number.

## Non-functional Requirements
-   Security: PIN protection for app access (if enabled), data encryption at rest.
-   Performance: Smooth UI, quick record retrieval.
-   Usability: Intuitive interface for managing medical records.

## Architecture Overview
-   MVVM (Model-View-ViewModel) architecture pattern.
-   Room for local database storage.
-   Koin for dependency injection.
-   Jetpack Compose for UI.
-   Coroutines for asynchronous operations.
-   **SharedPreferences used to store onboarding completion status.**
-   **PinViewModel manages PIN setup status (persisted, likely via SharedPreferences) and provides it to the UI.**
-   Navigation handled by Jetpack Navigation Compose.
