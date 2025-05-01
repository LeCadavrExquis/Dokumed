# Dokumed App Documentation

## Overview
The Dokumed app is an Android *   Fixed bug where clinical data was not re-inserted during medical record updates.
*   Ensured medical record updates (including related data like measurements, clinical data, and tags) are performed within a database transaction to guarantee atomicity and prevent partial data loss on errors.pplication for managing medical records. It allows users to store, categorize, search, and analyze their medical history.

## Main Features
- Record management (add, edit, delete)
- Different record types (consultations, measurements, clinical data)
- Tag-based organization
- Statistics and insights
- Data export and backup
  - Export selected medical records, associated measurements (grouped by description into separate CSVs), and attached files into a single ZIP archive.

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

- **DAOs**:
  - `MedicalRecordDao`: Data access for medical records
  - `TagDao`: Data access for tags

### ViewModels
- `MedicalRecordViewModel`: Manages medical record CRUD operations
  - Uses `saveMedicalRecord()` as the primary method for creating/updating records
  - Properly synchronizes measurements and clinical data from records to its internal state
  - Handles clinical data deletion tracking
  - Maintains operation state (Idle, Loading, Saving, Success, Error)
- `StatisticsViewModel`: Handles statistical analysis of records
- `ExportViewModel`: Manages export functionality

### UI Components
- `MedicalRecordEditScreen`: Screen for adding/editing records
- `MedicalRecordListScreen`: Screen for viewing and filtering records
- `StatisticsScreen`: Screen for viewing statistics and insights
- `StatisticsChart`: Component for rendering different chart types

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

## File Handling
- Attach files (PDF, images) to records from device storage.
- View attached files within the app.
- Open files (PDF, images) from external apps (e.g., email attachments) directly into the "New Record" screen with the file pre-attached.

## Data Visualization
- View trends and charts for specific measurement types over time (placeholder/future feature).
