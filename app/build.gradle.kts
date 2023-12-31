import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
}

fun Project.runCommand(command: String): String {
    val byteOut = ByteArrayOutputStream()
    val stdErr = ByteArrayOutputStream()
    try {
        project.exec {
            commandLine = command.split(" ")
            standardOutput = byteOut
            errorOutput = stdErr
        }
    } catch (e: Exception) {
        throw Exception("`${command}` failed: ${String(stdErr.toByteArray())}")
    }
    return String(byteOut.toByteArray()).trim()
}

fun Project.getCommitCount(): String {
    return runCommand("git rev-list --count HEAD")
}

fun Project.getGitSha(): String {
    return runCommand("git rev-parse --short HEAD")
}

fun Project.getGitBranch(): String {
    return try {
        runCommand("git symbolic-ref -q --short HEAD")
    } catch (_: Exception) {
        /** probably in CI. If a tag triggered a workflow, only said tag is checked out.
            or it's a detached head, and this will basically mimic [getGitSha]
            in any case, return *something*
        */
        runCommand("git describe --all --exact-match --always")
    }
}

fun getBuildTime(): String {
    val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'")
    df.timeZone = TimeZone.getTimeZone("UTC")
    return df.format(Date())
}

android {
    namespace = "net.lmaotrigine.heartbeat"
    compileSdk = 34

    defaultConfig {
        applicationId = "net.lmaotrigine.heartbeat"
        minSdk = 26
        //noinspection OldTargetApi
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "COMMIT_COUNT", "\"${getCommitCount()}\"")
        buildConfigField("String", "COMMIT_SHA", "\"${getGitSha()}\"")
        buildConfigField("String", "BUILD_TIME", "\"${getBuildTime()}\"")
        buildConfigField("String", "BRANCH", "\"${getGitBranch()}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            multiDexEnabled = true
        }
        debug {
            multiDexEnabled = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)
    implementation(libs.androidx.preference)
    implementation(libs.play.services.ads)
    implementation(libs.core)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
