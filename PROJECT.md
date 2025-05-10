# Project Documentation

## Overview
The Dokumed project is a medical records management application for Android. It helps users track, manage, and export their medical history.

## Project Structure
The project follows a clean architecture approach with the following layers:
- UI (presentation layer)
- Domain (business logic)
- Data (repositories and data sources)

### Key Components

#### Data Layer
- **Entities**: Database objects in the `pl.fzar.dokumed.data.entity` package
- **Relation Classes**: Classes like `MedicalRecordWithDetails` used to fetch entities with their related data (e.g., measurements, clinical data).
- **DAOs**: Data Access Objects in `pl.fzar.dokumed.data.dao` package
- **Repositories**: Abstracting data access in `pl.fzar.dokumed.data.repository` package
- **Models**: Domain models in `pl.fzar.dokumed.data.model` package

#### Presentation Layer
- **ViewModels**: Managing UI state and business logic
- **Composables**: UI components using Jetpack Compose

## Applications

For application-specific documentation, see:
- [App Documentation](PROJECT_app.md) - Includes features like record management, data export, user profiles, medication reminders, and **WebDAV cloud synchronization**.
