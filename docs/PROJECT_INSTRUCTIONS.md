# Android Notebook — Project Instructions

## 1. Project goal

Develop a native Android note-taking application for Google Pixel 8.

The application must be fast, simple, visually polished, reliable, and fully functional offline. It should feel like a real Android application, not a website wrapped in an APK.

The working project name is "Notebook".

## 2. Technology stack

- **Language:** Kotlin.
- **UI:** Jetpack Compose.
- **Design:** Material 3.
- **Database:** Room with SQLite.
- **Architecture:** MVVM with a Repository layer.
- **Asynchronous operations:** Kotlin Coroutines and Flow.
- **Navigation:** Navigation Compose.
- **Settings:** DataStore Preferences.
- **Build system:** Gradle with Kotlin DSL.
- **Dependency injection:** Prefer simple manual dependency injection initially. Do not introduce Hilt unless it provides a clear benefit.

Use stable, mutually compatible dependency versions. Do not upgrade dependencies unnecessarily.

## 3. Core functionality

The first version must support:

### Notes
- Create a note.
- Edit an existing note.
- Delete a note.
- Automatically save changes.
- Display the date of creation and last modification.
- Pin important notes.
- Search notes by title and content.
- Sort notes by modification date and creation date.

### Organization
- Assign colors to notes.
- Create and manage categories or tags.
- Filter notes by category.
- Separate pinned notes from regular notes.
- Provide an archive or trash section if it can be implemented without unnecessary complexity.

### User interface
- Main screen with a list or grid of notes.
- Floating action button for creating a note.
- Dedicated note editor screen.
- Search interface.
- Settings screen.
- Dark and light themes.
- Material 3 components.
- Smooth and restrained animations.
- Responsive layouts suitable for Google Pixel 8.

The interface must be clean, modern, readable, and comfortable to use with one hand.

## 4. Data storage and reliability

- Store all notes locally using Room.
- The application must work without an internet connection.
- Do not require registration or an account.
- Do not use Firebase or cloud storage in the first version.
- Notes must remain available after restarting the application.
- Deleting or updating a note must correctly update the database.
- Implement proper Room database migrations when the schema changes.
- Avoid data loss during application updates.
- Provide a way to export and import notes, preferably using JSON.

Never use temporary in-memory collections as the primary storage for notes.

## 5. Architecture and code quality

Organize the project into clear packages, for example:

- data
- data/database
- data/repository
- domain
- ui
- ui/screens
- ui/components
- ui/theme
- navigation

Keep database operations outside UI composables.

Use ViewModels to manage screen state and expose data through StateFlow or other appropriate Flow APIs.

Follow Android development best practices:
- Single source of truth.
- Separation of concerns.
- Reusable UI components.
- Meaningful naming.
- Small and maintainable classes.
- Proper error handling.

Avoid unnecessary abstractions and overengineering. This is a personal application, not a large enterprise system.

## 6. Development workflow

Work incrementally.

Before implementing a major feature:
1. Explain what will be implemented.
2. Identify which files will be created or modified.
3. Explain important architectural decisions.
4. Implement the feature.
5. Build the application.
6. Fix compilation errors and verify the result.
7. Summarize what changed.

Do not claim that something works unless it has actually been tested.

Do not silently delete existing functionality or overwrite user data.

Before making large architectural changes, explain the reason and obtain approval.

## 7. Testing

Test the following scenarios:

- Creating a note.
- Editing and saving a note.
- Closing and reopening the application.
- Deleting a note.
- Searching for notes.
- Pinning and unpinning notes.
- Switching between light and dark themes.
- Restarting the device or application.
- Exporting and importing notes.

Use unit tests for important database and repository logic where practical.

## 8. Target device and final delivery

The primary target device is Google Pixel 8.

Prioritize:
- Correct display scaling and screen insets.
- Comfortable touch targets.
- Android keyboard integration.
- Smooth scrolling.
- Portrait orientation.
- Compatibility with the Android version installed on the target device.

The final deliverable must be a working Android application that can be installed on Google Pixel 8.

Provide:
- A working Android Studio project.
- A debug APK for initial testing.
- A release APK when the application is ready.
- Clear instructions for installing and updating the application.

## 9. Important rules

- Do not build a web application or React application.
- Do not use React Native, Flutter, or Tauri.
- Use native Android development with Kotlin and Jetpack Compose.
- Do not add unnecessary dependencies.
- Do not introduce cloud functionality unless explicitly requested.
- Prioritize data integrity, offline operation, usability, and maintainability.
- Explain technical decisions clearly because the project is also intended to help me learn Android development.
