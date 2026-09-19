# VitaFit – AI Fitness Tracker

> **Stack:** Java • Android • Firebase • Firebase AI Logic • SQLite

[📂 Repository](https://github.com/Ashutosh9-pan/CodeAlpha_FitnessTrackerApp)

VitaFit is a modern Android fitness tracking application built with Java, Firebase, and Firebase AI Logic. It brings workout logging, progress analytics, reminders, profile management, and several AI-assisted wellness tools together in one mobile application.

> VitaFit provides general fitness and wellness guidance. It is not a substitute for professional medical advice.

## Highlights

- Secure email/password authentication with email verification
- Personalized dashboard with workout summaries
- Add, search, review, and delete workout records
- Fitness statistics and progress visualization
- Export workout history as PDF or CSV
- Share generated workout reports
- Workout reminders with reboot restoration
- Light and dark themes
- Responsive bottom navigation and navigation drawer
- Firebase App Check integration using Play Integrity

## AI Fitness Assistant

VitaFit groups its AI tools inside a single assistant hub:

- **Smart Recommendation** – profile-aware general fitness suggestions
- **AI Coach Chat** – conversational guidance based on saved profile and activity
- **Balanced Diet Planner** – balanced meal suggestions using user preferences
- **AI Workout Planner** – structured workout suggestions based on selected preferences
- **AI Progress Analyzer** – reviews saved workout activity and consistency

AI responses are formatted for clear mobile reading and include safety-focused instructions for balanced, age-appropriate guidance.

## Tech Stack

| Area | Technology |
|---|---|
| Language | Java 17 |
| Platform | Android SDK |
| UI | XML, Material Components, ConstraintLayout, RecyclerView |
| Authentication | Firebase Authentication |
| Database | Firebase Realtime Database |
| Generative AI | Firebase AI Logic |
| App protection | Firebase App Check with Play Integrity |
| Charts | MPAndroidChart |
| Async utilities | Guava ListenableFuture |
| Build system | Gradle Kotlin DSL |

## Screenshots

<p align="center">
  <img src="screenshots/16-splash-screen.jpg" width="220" alt="VitaFit splash screen" />
  <img src="screenshots/15-login-screen.jpg" width="220" alt="VitaFit login screen" />
  <img src="screenshots/14-dashboard-light-mode.jpg" width="220" alt="VitaFit dashboard" />
</p>

<p align="center">
  <img src="screenshots/09-ai-fitness-assistant-hub.jpg" width="220" alt="AI Fitness Assistant hub" />
  <img src="screenshots/08-ai-coach-chat.jpg" width="220" alt="AI Coach Chat" />
  <img src="screenshots/05-ai-workout-planner.jpg" width="220" alt="AI Workout Planner" />
</p>

<p align="center">
  <img src="screenshots/06-ai-diet-planner.jpg" width="220" alt="Balanced Diet Planner" />
  <img src="screenshots/04-ai-progress-analyzer.jpg" width="220" alt="AI Progress Analyzer" />
  <img src="screenshots/10-workout-reminder.jpg" width="220" alt="Workout Reminder" />
</p>

<details>
<summary><strong>View more screenshots</strong></summary>

<p align="center">
  <img src="screenshots/01-navigation-drawer.jpg" width="220" alt="Navigation drawer" />
  <img src="screenshots/02-dashboard-dark-mode.jpg" width="220" alt="Dashboard dark mode" />
  <img src="screenshots/03-user-profile.jpg" width="220" alt="User profile" />
</p>

<p align="center">
  <img src="screenshots/11-fitness-statistics.jpg" width="220" alt="Fitness statistics" />
  <img src="screenshots/12-workout-history-and-export.jpg" width="220" alt="Workout history and export" />
  <img src="screenshots/13-add-workout-screen.jpg" width="220" alt="Add workout screen" />
</p>

</details>

## Requirements

- Android Studio
- JDK 17
- Android device or emulator running Android 7.0 (API 24) or later
- A Firebase project
- Internet connection for Firebase and AI features

## Getting Started

1. Clone the repository:

   ```bash
   git clone https://github.com/Ashutosh9-pan/VitaFit-AI-Fitness-Tracker.git
   ```

2. Open the project in Android Studio.

3. Create or select a Firebase project and register an Android app with this package name:

   ```text
   com.ashutosh.codealpha_fitnesstrackerapp
   ```

4. Download your Firebase `google-services.json` file and place it inside the local `app/` directory.

5. Enable the required Firebase services:

   - Email/Password Authentication
   - Realtime Database
   - Firebase AI Logic
   - App Check for production builds

6. Sync the Gradle files and run the application.

## Security Notes

Sensitive and machine-specific files are intentionally excluded from this repository, including:

- Signing keystores
- Keystore credentials
- `local.properties`
- `google-services.json`
- Generated APK and App Bundle files

Never commit signing passwords, private keys, debug App Check tokens, or other credentials.

## Project Structure

```text
app/src/main/
├── java/        # Activities, adapters, Firebase logic, AI integration
├── res/         # Layouts, menus, drawables, themes, and strings
└── AndroidManifest.xml

screenshots/     # Application screenshots used in this README
```

## Author

**Ashutosh Panwar**

- GitHub: [@Ashutosh9-pan](https://github.com/Ashutosh9-pan)

## Acknowledgements

- Firebase Authentication and Realtime Database
- Firebase AI Logic
- Firebase App Check and Google Play Integrity
- Android Material Components
- MPAndroidChart

