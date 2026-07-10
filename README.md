# WiCompress — Offline Video Compressor & Storage Optimizer

WiCompress is a high-performance offline optimization tool tailored for **Huawei Pura 80 Pro** (EMUI running on Android 12). The application focuses on ultra-fast video compression using hardware acceleration, smart storage suggestions, and visual duplicate detection using JNI-bound perceptual hashes, completely decoupled from Google Mobile Services (GMS).

---

## Technical Architecture & Stack

*   **Language & Runtime:** Kotlin + C++ (JNI Layer for performance-critical mathematical and image operations).
*   **UI System:** Jetpack Compose styled using the **Refined Minimal** design system with dynamic theme controllers, custom cards (~24dp corner radiuses), soft pastels, and background blurs.
*   **Video Engine:** FFmpeg Kit Mobile (`arm64-v8a` target configuration) with native **Android MediaCodec NDK** bindings.
    *   Encoder overrides pass target pipelines using Kirin hardware units: `-c:v h264_mediacodec` or `-c:v hevc_mediacodec`.
*   **Storage & Session Logs:** Room Database (`wicompress_database` SQLite engine).
*   **Background Protection:** Persistent Android `Foreground Service` (configured with `dataSync` metadata flags) working in tandem with `WorkManager` queues to prevent process termination on EMUI's aggressive energy-saving environment.

---

## Directory Structure

```text
WiCompress/
│
├── build.gradle.kts          # Root-level plugin orchestrator
├── settings.gradle.kts       # Subproject compilation settings
├── gradle.properties         # JVM parameters & environment flags
├── gradlew / gradlew.bat     # Gradle wrapper script helpers
│
└── app/
    ├── build.gradle.kts      # Application dependencies (Compose, Room, FFmpeg-Kit, NDK CMake)
    ├── proguard-rules.pro    # Obfuscation exclusion policies (Room DAOs, native JNI mappings)
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── cpp/
        │   │   ├── CMakeLists.txt     # CMake compilation directives
        │   │   └── native-lib.cpp     # C++ 64-bit perceptual hashing (aHash) & Hamming popcount
        │   ├── java/com/widlily/wicompress/
        │   │   ├── MainActivity.kt    # Scoped storage permissions & Bottom Navigation controller
        │   │   ├── WiCompressApp.kt   # Notification channel registrar & Room instantiator
        │   │   ├── data/
        │   │   │   ├── AppDatabase.kt
        │   │   │   ├── dao/CompressionHistoryDao.kt
        │   │   │   ├── entity/            # Rooms entities (CompressionHistory, SystemStats)
        │   │   │   └── model/             # Models (VideoFile, DuplicateGroup, CompressionTask)
        │   │   ├── service/
        │   │   │   ├── CompressionService.kt # Foreground processing engine & Auto-delete checking
        │   │   │   └── CompressionWorker.kt  # WorkManager worker scheduling API
        │   │   ├── ui/
        │   │   │   ├── components/CustomCard.kt
        │   │   │   ├── theme/             # Theme specs (Color, Shape, Type, Theme)
        │   │   │   └── screens/           # Views (Home, Activity, History, Settings, Duplicates)
        │   │   └── util/
        │   │       ├── FFmpegManager.kt   # Kirin hardware compiler & speed trackers
        │   │       ├── ImageHashUtil.kt   # Keyframe bitmap extraction & native JNI wrapper
        │   │       └── MediaStoreHelper.kt # Scoped Storage filters & Android 12 Delete Requests
        │   └── res/
        │       └── values/strings.xml     # String resources
        └── test/                          # Unit testing suite
```

---

## Core Feature Implementations

### 1. Kirin Hardware Acceleration (MediaCodec NDK)
Commands built inside `FFmpegManager` route video frames directly into hardware units:
```kotlin
// Choose hardware accelerated encoder: h264_mediacodec / hevc_mediacodec
val encoder = if (useH265) "hevc_mediacodec" else "h264_mediacodec"
val command = "-y -i \"$inputPath\" -c:v $encoder -b:v $bitrate -c:a aac -b:a 128k \"$outputPath\""
```

### 2. Native Perceptual Hashing (C++)
To detect duplicates, frame buffers are downsampled and evaluated in native code:
*   **Average Hash (aHash):** Rescales frame buffers into an $8 \times 8$ grayscale matrix, calculates the average luminance, and returns a 64-bit integer.
*   **Hamming Distance:** Computes similarity metrics of frame hashes using hardware-level bit count operators (`__builtin_popcountll`).
```cpp
JNIEXPORT jint JNICALL
Java_com_widlily_wicompress_util_ImageHashUtil_computeHammingDistance(JNIEnv *env, jobject thiz, jlong hash1, jlong hash2) {
    uint64_t diff = static_cast<uint64_t>(hash1 ^ hash2);
    return static_cast<jint>(__builtin_popcountll(diff)); // Returns bit counts
}
```

### 3. Android 12 Scoped Storage Integration
*   **File Deletion Consent:** Deletion of original files triggers `MediaStore.createDeleteRequest` which presents the system user-approval prompt.
*   **Validation Check:** Original files are only marked for deletion after checking:
    1.  FFmpeg process output exit code is `0` (Success).
    2.  Output file size is greater than zero.
    3.  Video durations match (within a $1000\text{ms}$ tolerance).

---

## How to Build & Run

1.  Open the folder in **Android Studio**.
2.  Android Studio will automatically download the configured Gradle 8.2 runtime and resolve dependencies.
3.  Ensure Android NDK is installed inside Android Studio (SDK Manager $\rightarrow$ SDK Tools $\rightarrow$ check NDK).
4.  Build the project: `Build` $\rightarrow$ `Make Project`.
5.  Run local unit tests: `./gradlew test` (or execute `ImageHashUtilTest` and `FFmpegManagerTest` inside IDE).
