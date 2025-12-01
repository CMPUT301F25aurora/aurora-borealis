plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.aurora"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.aurora"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17

        isCoreLibraryDesugaringEnabled = true
    }
}
//
dependencies {
    //implementation(files("C:\\Users\\omara\\AppData\\Local\\Android\\Sdk\\platforms\\android-36\\android.jar"))

    dependencies {
        // --- Firebase ---
        implementation(platform("com.google.firebase:firebase-bom:34.4.0"))
        implementation("com.google.firebase:firebase-analytics")
        implementation("com.google.firebase:firebase-firestore")
        implementation(libs.firebase.storage)
        implementation(libs.firebase.auth)

        // --- AndroidX / UI ---
        implementation(libs.appcompat)
        implementation(libs.material)
        implementation(libs.activity)
        implementation(libs.constraintlayout)

        // --- Maps & location ---
        implementation("com.google.android.gms:play-services-maps:18.2.0")
        implementation("com.google.android.gms:play-services-location:21.0.1")
        implementation("com.google.android.libraries.places:places:3.3.0")

        // --- QR / barcode scanning ---
        implementation("com.google.zxing:core:3.5.3")
        implementation("com.journeyapps:zxing-android-embedded:4.3.0")

        // --- Glide (images) ---
        implementation("com.github.bumptech.glide:glide:4.16.0")
        annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

        // --- Java 8+ APIs desugaring ---
        coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

        // --- Unit tests (JVM) ---
        // Choose ONE JUnit source; here we use the direct coord
        testImplementation("junit:junit:4.13.2")
        // If you prefer the version-catalog one instead, you can use:
        // testImplementation(libs.junit)

        // Mockito & Hamcrest for unit tests
        testImplementation("org.mockito:mockito-core:5.11.0")
        //testImplementation("org.mockito:mockito-inline:5.11.0")
        testImplementation("org.hamcrest:hamcrest-library:2.2")

        // --- Instrumented tests (on device/emulator) ---
        // Use direct AndroidX test artifacts for clarity:
        androidTestImplementation("androidx.test.ext:junit:1.1.5")
        androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
        androidTestImplementation("androidx.test:core:1.5.0")
        androidTestImplementation("androidx.fragment:fragment-testing:1.6.2")

        // If your libs.ext.junit / libs.espresso.core are just aliases for those,
        // you can delete the duplicated libs.* versions.
    }

}
