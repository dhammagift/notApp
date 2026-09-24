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
        val appVersion = "0.6.1"
        versionName = appVersion
        if (testBuildTag != null) versionNameSuffix = "-$testBuildTag"
        versionCode = appVersion.split(".").map { it.toInt() }.let { (major, minor, patch) -> major * 10000 + minor * 100 + patch }
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

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
