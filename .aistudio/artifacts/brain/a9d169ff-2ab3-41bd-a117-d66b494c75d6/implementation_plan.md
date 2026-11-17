# Camel OS — Unified Android Mini Operating System

Camel OS unifies all independently built Android applications into a single installable APK powered by a modular **Camel Core** shell, shared **Camel Services**, and a Midnight Black, Deep Navy, and Champagne Gold desktop-grade interface.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> **Missing Original App Sources (Pending Upload from Termux)**: After inspecting the active workspace, **only Europass** is currently present. None of the other 17 applications exist in the workspace yet. Per the strict no-fake-replacement rule, **Camel Core + Europass** will be built first, and each additional module will be integrated directly from the Termux archives/files you upload in controlled batches.

- **Confirmed Decision 1 (Source Provisioning)**: Original source projects from Termux will be provided via uploaded archives or files in batches rather than replaced with mock stubs.
- **Confirmed Decision 2 (Batch 1 Priority)**: First integration batch alongside **Camel Core** and **Europass** is **Utilities** (*Horloge*, *Calculator*, *Notes*, *Recorder*).
- **Confirmed Decision 3 (Multitasking & App Hosting)**: Integrated apps open as **adaptive full-screen internal modules** paired with a persistent **Camel OS Taskbar**, recent-apps switcher, and quick return to the desktop dashboard.
- **Confirmed Decision 4 (Live Data & Streams)**: *TV Gold*, *Radio Gold*, and *Weather* will preserve the exact APIs, stream endpoints, and configurations found in their uploaded Termux source projects.

---

### 1. Overview & Integration Inventory

#### Workspace Audit (Current State vs. Pending Uploads)

| Stage | Application Module | Workspace Status | Key Integration Dependencies & Services | Required Permissions / System Notes |
| :--- | :--- | :--- | :--- | :--- |
| **Base** | **Europass** | **Available** | Firebase AI Logic (`gemini-3.8-flash`), App Check, Room DB, Coil, PDF Document Exporter, FileProvider | `INTERNET`, `CAMERA`, `RECORD_AUDIO`, Photo Picker (`PickVisualMedia`) |
| **Batch 1** | **Horloge** | Missing (Termux) | Alarm scheduling, timers, stopwatch, foreground timer notification | `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM` / `VIBRATE` |
| **Batch 1** | **Calculator** | Missing (Termux) | Pure Kotlin / Compose scientific & unit math engine | None |
| **Batch 1** | **Notes** | Missing (Termux) | Local Room persistence (namespace-isolated or unified DB), rich text / document export | None (or SAF export) |
| **Batch 1** | **Recorder** | Missing (Termux) | Android `MediaRecorder` / `AudioRecord`, foreground recording service, local audio storage | `RECORD_AUDIO`, `FOREGROUND_SERVICE_MICROPHONE` |
| **Batch 2** | **Calendrier** | Missing (Termux) | Event database, reminders, notification scheduling | `POST_NOTIFICATIONS` |
| **Batch 2** | **Weather** | Missing (Termux) | Exact Weather API client/endpoints from Termux source | `INTERNET`, `ACCESS_COARSE_LOCATION` (optional) |
| **Batch 2** | **Maps** | Missing (Termux) | Exact Maps SDK / OpenStreetMap / Location service from Termux source | `INTERNET`, `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` |
| **Batch 3** | **Music Player** | Missing (Termux) | Media3 / ExoPlayer foreground playback service (`MediaSessionService`), audio library index | `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `POST_NOTIFICATIONS` |
| **Batch 3** | **Video Player** | Missing (Termux) | Media3 / ExoPlayer video surface, PiP / fullscreen orientation handling | SAF / `PickVisualMedia` |
| **Batch 3** | **Radio Gold** | Missing (Termux) | Exact streaming endpoints from Termux source, Media3 audio foreground service | `INTERNET`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` |
| **Batch 3** | **TV Gold** | Missing (Termux) | Exact IPTV/channel endpoints from Termux source, Media3 HLS/DASH playback | `INTERNET` |
| **Batch 4** | **Camera** | Missing (Termux) | CameraX preview/capture service (`camera-core`, `camera2`, `lifecycle`, `view`) | `CAMERA`, `RECORD_AUDIO` (video) |
| **Batch 4** | **Galerie** | Missing (Termux) | Photo/video viewer & shared media store / SAF integration | Zero-permission Photo Picker / SAF |
| **Batch 4** | **System Info** | Missing (Termux) | Android `BatteryManager`, `ActivityManager`, `StatFs`, `/proc` telemetry | None |
| **Batch 4** | **Manager Gold** | Missing (Termux) | SAF file browser, internal app-module manager, storage cleanup | SAF (`OpenDocumentTree`) |
| **Batch 5** | **Linux Loader** | Missing (Termux) | Native PRoot / terminal / X11 surface integration from Termux source | `INTERNET`, foreground execution service if required |
| **Batch 5** | **Windows Runner** | Missing (Termux) | Native QEMU / display integration from Termux source | `INTERNET` |

---

### 2. User Experience & Visual Design

#### Visual Identity & Theme
- **Aesthetic Direction**: Futuristic luxury desktop OS inside Android — dark obsidian surfaces layered with deep navy glass panels and warm champagne gold focal accents.
- **Color Tokens**:
  - Background Canvas: Midnight Black (`#07090E`)
  - Elevated Panels / Dock / Quick Settings: Deep Navy (`#0D1B3E` and `#132347`)
  - Primary Accent & Active Indicators: Champagne Gold (`#D4AF37` & `#F3D578`)
  - Secondary Status Accents: Emerald (`#10B981`), Amber (`#F59E0B`), Crimson (`#EF4444`)
  - Customizable Theme Presets: Users can toggle between *Midnight Gold (Default)*, *Royal EU Navy*, and *Obsidian Minimal*.
- **Typography**:
  - Display & OS Headers: `Plus Jakarta Sans` (Bold / SemiBold)
  - Body & Dense Lists: `Inter`
  - Technical Telemetry, Clocks & Terminal Output: `JetBrains Mono`
  - Editorial / CV Headings (Europass): `Playfair Display`
- **Brand Identity**: Custom adaptive Camel OS launcher icon (stylized geometric camel silhouette in champagne gold on a deep navy `#0B1A3A` shield) and an atmospheric Camel OS desktop backdrop.

#### Main Desktop & Multitasking Flow
1. **Desktop Dashboard**: Displays live Clock & Date widget, Weather status tile, Hardware Telemetry strip (Battery %, CPU load, RAM, Storage), Quick-Launch pinned modules, and a unified **Search Bar** filtering across all registered apps and actions.
2. **Application Drawer & Menu**: Categorized grid (*Productivity & AI*, *Utilities*, *Media & Streams*, *System & Environments*) showing installed modules vs. pending modules clearly marked as awaiting source upload.
3. **Adaptive Full-Screen Module Host + OS Taskbar**:
   - Tapping any app opens its **real native UI** full-screen inside the Camel OS host frame.
   - A compact, inset-safe **OS Taskbar** stays anchored at the bottom (or left rail on tablets/foldables) showing: *Home Dashboard*, *App Drawer*, *Active / Recent Apps Pill Strip* (1-tap switching without losing state), and *Quick Settings / Notifications trigger*.
4. **Notification Center & Quick Settings Drawer**:
   - Swipe down or tap the status bar to toggle Wi-Fi/Bluetooth shortcuts, theme switcher, Do Not Disturb, media mini-controls, and view unified notifications emitted by Horloge alarms, Recorder, Music Player, and Europass exports.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: True Source Integration vs. Re-implementation**
  - *Chosen Approach*: Build **Camel Core** and **Camel Services** first while preserving the real **Europass** module intact, then ingest each uploaded Termux project batch by batch into dedicated Kotlin sub-packages (`com.aymengharsalli.camelos.apps.<module>`).
  - *Why*: Guarantees zero feature loss and avoids inventing fake data or placeholder streams.
- **Decision 2: Conflict Resolution Strategy**
  - *Chosen Approach*: Standardize a single `applicationId` (`com.aymengharsalli.camelos`), keep one shared `FileProvider` authority (`${applicationId}.fileprovider`) merging all path entries, isolate or unify Room databases under unique database file names (`europass_cv_database`, `notes_database`, `calendar_database`), and route all modules through centralized Camel Core permissions and navigation contracts.
  - *Why*: Eliminates Gradle duplicate resource crashes, FileProvider authority collisions, and database schema clashes across 18 apps.
- **Decision 3: Adaptive Full-Screen App Hosting with State Retention**
  - *Chosen Approach*: Keep active module state holders (`ViewModel`s / service bindings) alive in Camel Core's `TaskManager` so switching between Europass, Notes, Calculator, or Music Player via the OS Taskbar is instant and preserves unsaved work.
  - *Why*: Delivers desktop-like multitasking on both phones and large-screen foldables/tablets without brittle multi-window overlap issues.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────────┐
│                        CAMEL OS HOST (Single APK)                          │
│                   applicationId: com.aymengharsalli.camelos                │
├────────────────────────────────────────────────────────────────────────────┤
│  CAMEL SHELL UI                                                            │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌───────────────────┐  │
│  │  Desktop     │ │  App Drawer  │ │  Taskbar &   │ │  Control Center   │  │
│  │  Dashboard   │ │  & Search    │ │  Recents     │ │  & Notifications  │  │
│  └──────┬───────┘ └──────┬───────┘ └──────┬───────┘ └─────────┬─────────┘  │
├─────────┴────────────────┴────────────────┴───────────────────┴────────────┤
│  CAMEL CORE                                                                │
│  ┌─────────────────────┐ ┌─────────────────────┐ ┌──────────────────────┐  │
│  │  AppRegistry &      │ │  TaskManager        │ │  Unified Theme,      │  │
│  │  Lifecycle Host     │ │  (Active Modules)   │ │  Prefs & Permissions │  │
│  └─────────┬───────────┘ └──────────┬──────────┘ └──────────┬───────────┘  │
├────────────┴────────────────────────┴───────────────────────┴──────────────┤
│  CAMEL SERVICES (Shared OS Services & Native Hardware Bridges)             │
│  • MediaPlaybackService (Media3)   • RecorderService   • AlarmScheduler    │
│  • CameraBridge (CameraX)          • SystemTelemetry   • SharedStorage/SAF │
│  • Firebase AI Logic + App Check   • NotificationBus                       │
├────────────────────────────────────────────────────────────────────────────┤
│  INTEGRATED CAMEL APPLICATIONS (Modular Kotlin Packages)                   │
│  [Ready] Europass                                                          │
│  [Batch 1 - Awaiting Termux Upload] Horloge, Calculator, Notes, Recorder   │
│  [Batch 2 - Awaiting Termux Upload] Calendrier, Weather, Maps              │
│  [Batch 3 - Awaiting Termux Upload] Music, Video, Radio Gold, TV Gold      │
│  [Batch 4 - Awaiting Termux Upload] Camera, Galerie, System Info, Manager  │
│  [Batch 5 - Awaiting Termux Upload] Linux Loader, Windows Runner           │
└────────────────────────────────────────────────────────────────────────────┘
```

#### Controlled Stage Rollout
1. **Stage 0 (Foundation & Europass)**: Stand up **Camel Core** (registry, taskbar, desktop dashboard, unified notifications, quick settings, theme engine, live system telemetry, and custom Camel OS launcher icon) and mount the full **Europass** module inside Camel OS without losing any feature (AI generation, App Check, Room DB, Canva editor, PDF export, Camera/Photo Picker). Validate with `:app:assembleDebug`.
2. **Stage 1 (Batch 1 — Utilities)**: Ingest your uploaded *Horloge*, *Calculator*, *Notes*, and *Recorder* source projects from Termux, resolving package conflicts, unifying Room/alarm/recording services, and validating the build.
3. **Stages 2–5**: Progressively integrate each subsequent uploaded Termux batch (*Productivity*, *Media/Streams*, *Hardware/Files*, *Linux/Windows Environments*), compiling and verifying the unified APK after every batch.
