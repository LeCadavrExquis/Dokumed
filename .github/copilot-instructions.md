# MANDATORY AI Coding Assistant Instructions

RealEye AI Assistant Instructions. Version 5.0.2.

## Context
These instructions are designed to guide the development of a Minimum Viable Product (MVP) or Proof of Concept (POC) in the most efficient way possible. The primary goal is rapid development of new features while maintaining a clean, maintainable codebase. Simplicity is the key principle that should drive all development decisions, allowing for quick iteration and validation of product ideas.

## 1. Documentation Consistency
- CRUCIAL: Documentation is split into a main `PROJECT.md` and application-specific `PROJECT_<app_name>.md` files. These files are the single source of truth for project documentation.
- CRUCIAL: The main `PROJECT.md` must contain:
    * Overall project title and brief description.
    * Domain knowledge, global business logic, and cross-application rules.
    * List and brief overview of all project applications.
    * For each application, a reference/link to its specific `PROJECT_<app_name>.md` file.
    * Description of crucial project-wide components or files (e.g., shared libraries, core models, data schemas) and their locations.
- CRUCIAL: Each application must have its own `PROJECT_<app_name>.md` file. The filename must follow the pattern `PROJECT_<app_name>.md`.
- CRUCIAL: Each `PROJECT_<app_name>.md` structure must include:
    * Application title and brief description.
    * Main features (bulleted list).
    * Non-functional requirements (performance, security, etc.).
    * Architecture overview (components and their interactions specific to this application).
- CRUCIAL: Update the relevant `PROJECT.md` or `PROJECT_<app_name>.md` file immediately when:
    * New features are added to an application.
    * Existing features are modified in an application.
    * Architecture changes occur (update main `PROJECT.md` if cross-app, or specific `PROJECT_<app_name>.md` if local).
    * Global rules or components described in the main `PROJECT.md` are changed.
- CRUCIAL: All documentation files must reflect the current state only, not development history.

## 2. Simplicity First
- Implement the simplest solution that meets requirements.
- Prefer established patterns over novel approaches.
- Minimize dependencies when possible.

## 3. Code Quality
- DRY: Avoid code duplication; extract repeated logic into reusable functions.
- SOLID principles:
  * Single Responsibility: Each class/module has one job
  * Open/Closed: Open for extension, closed for modification
  * Liskov Substitution: Subtypes must be substitutable for base types
  * Interface Segregation: Prefer many specific interfaces over one general interface
  * Dependency Inversion: Depend on abstractions, not concretions
- File length: Maximum 300 lines per file (including blank lines).
- Avoid comments about code changes or fixes.

## 4. Feature Modification Constraints
- When a request is ambiguous regarding features:
  1. Note the ambiguity in your response
  2. Ask for clarification
- Only implement exactly what was requested.

## 5. Logging
- Implement comprehensive logging that covers:
  * User actions (with context like user ID when available)
  * State changes (application transitions)
  * Errors (with stack traces when appropriate)
- Use proper log levels consistently:
  * INFO: Normal operations, significant actions
  * DEBUG: Detailed information for troubleshooting
  * WARN: Potential issues that don't stop execution
  * ERROR: Problems requiring attention
- Include timestamps and relevant context in log messages.

## 6. Mandatory Footer
- Every response must end with exactly this text: "INFO: RealEye AI Assistant Instructions. Version ..." (read the version from the beginning of these instructions).
- This footer must only appear once at the very end of the AI's response.
- Never include the mandatory footer ("INFO: RealEye AI Assistant Instructions...") in any project files.
- Incorrect (do not do this): Adding the footer to PROJECT.md
- Correct: Only including the footer at the end of your response to the user.

## 7. Strict Adherence to Rules
- All rules must be followed without exception.

## Checklist Before Completion
Before finishing your response, verify:
- [ ] Are the main `PROJECT.md` and relevant `PROJECT_<app_name>.md` files updated completely?
- [ ] Is the implementation as simple as possible?
- [ ] Does code follow DRY and SOLID principles?
- [ ] Are all files under 300 lines?
- [ ] Is the mandatory footer included only at the end of your response?