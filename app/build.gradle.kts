plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Test-only switch, off unless -PalwaysAskForReview=true is passed (the CI "Run workflow"
// input): the rating card then shows on every launch and its buttons change nothing, so it can
// be looked at without waiting 60 days or clearing app data. Never a release: the workflow
// refuses this flag together with a version or a Play upload.
val alwaysAskForReview = providers.gradleProperty("alwaysAskForReview").orNull?.toBoolean() ?: false

// Set for CI builds that are not releases: the commit lands in the version name, so Settings answers
// "which build is this?" — the question that keeps coming up when an install silently keeps the APK
// that was already there. Empty for anything with a version, i.e. for real releases.
val testBuildTag = providers.gradleProperty("testBuildTag").orNull?.take(7)?.takeIf { it.isNotBlank() }

// CI run number for non-release builds. The version code is what Android compares when installing an
// update: without it every test build carried the same 601 and an install could quietly keep the APK
// that was already there, which is exactly how "nothing changed" happened three times in a row.
// Releases keep the plain code derived from the version name.
val buildNumber = providers.gradleProperty("buildNumber").orNull?.toIntOrNull()

android {
    namespace = "com.noapp.container"
    compileSdk = 36

    defaultConfig {
        applicationId = "gift.dhamma.noapp"
        minSdk = 24
        targetSdk = 36
        // The only version to edit. versionName is what people see; versionCode is Play's
        // internal ordering number, which must grow with every upload — derived here so it
        // can't be forgotten or collide: major*10000 + minor*100 + patch (0.5.1 -> 501).
        val appVersion = "0.6.2"
        versionName = appVersion
        if (testBuildTag != null) versionNameSuffix = "-$testBuildTag"
        val baseVersionCode = appVersion.split(".").map { it.toInt() }.let { (major, minor, patch) -> major * 10000 + minor * 100 + patch }
        versionCode = if (testBuildTag != null) baseVersionCode * 1000 + (buildNumber ?: 0) else baseVersionCode
        buildConfigField("boolean", "ALWAYS_ASK_FOR_REVIEW", alwaysAskForReview.toString())
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Roborazzi renders the real composables to PNGs on the JVM (Robolectric + Layoutlib) — the only
    // way to look at this UI without a device. The tests themselves only run with RENDER_SNAPSHOTS=1,
    // so CI stays untouched.
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")

    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.32.2")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.32.2")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("androidx.test.ext:junit:1.2.1")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}

// The snapshot tests are a local tool: Robolectric renders real layouts, which needs Layoutlib and a
// debug manifest, and neither belongs in CI. They are left out of the test run entirely unless asked
// for, so a green build stays green and stay fast.
tasks.withType<Test>().configureEach {
    systemProperty("roborazzi.test.record", System.getenv("RENDER_SNAPSHOTS") == "1")
    if (System.getenv("RENDER_SNAPSHOTS") != "1") {
        exclude("**/DemoSnapshots*", "**/ScreenSnapshots*")
    }
}
