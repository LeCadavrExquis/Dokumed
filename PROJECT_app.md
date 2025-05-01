# Dokumed App Documentation

## Overview
The Dokumed app is an Android application for managing medical records. It allows users to store, categorize, search, and analyze their medical history.

## Main Features
- Record management (add, edit, delete)
- Different record types (consultations, measurements, clinical data)
- Tag-based organization
- Statistics and insights
- Data export and backup

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

## Medical Record Management
- View a list of medical records.
- View details of a specific medical record, including attached files.
- Open attached files from the detail view.
- Filter records by type, date range, or tags.
- Add new medical records of various types (Consultation, Lab Test, Imaging, Procedure, Measurement, Medication, Symptom).
- Edit existing medical records.
- Delete medical records.
- Attach multiple files (e.g., PDFs, images) to relevant record types (Consultation, Lab Test, Imaging, Procedure).
- Add tags to records for easier categorization and searching.
