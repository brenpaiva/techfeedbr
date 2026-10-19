import java.util.Properties
import java.net.HttpURLConnection
import java.net.URI

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val releaseRequested = gradle.startParameter.taskNames.any {
    it.contains("release", ignoreCase = true) || it == "assemble"
}
val signingPropertiesFile = rootProject.file("signing.local.properties")
val signingProperties = Properties().apply {
    if (signingPropertiesFile.isFile) signingPropertiesFile.inputStream().use { load(it) }
}
if (releaseRequested) {
    require(signingPropertiesFile.isFile && listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
        .all { !signingProperties.getProperty(it).isNullOrBlank() }) {
        "Configure mobile/signing.local.properties e o keystore local antes de gerar o APK release."
    }
    require(rootProject.file(signingProperties.getProperty("storeFile")).isFile) {
        "O keystore configurado em mobile/signing.local.properties não foi encontrado."
    }
}

val validateReleaseApi by tasks.registering {
    group = "verification"
    description = "Verifica a API HTTPS antes de gerar um APK release."
    doLast {
        val baseUrl = providers.gradleProperty("TECHFEED_RELEASE_API_BASE_URL").get()
        fun response(path: String): Pair<Int, String> {
            val connection = URI.create(baseUrl + path).toURL().openConnection() as HttpURLConnection
            connection.connectTimeout = 30_000
            connection.readTimeout = 30_000
            connection.requestMethod = "GET"
            return try {
                connection.responseCode to if (connection.responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else ""
            } finally {
                connection.disconnect()
            }
        }
        val (healthStatus, healthBody) = runCatching { response("actuator/health") }
            .getOrElse { throw GradleException("Não foi possível acessar a API release por HTTPS.", it) }
        require(healthStatus == 200 && Regex("\"status\"\\s*:\\s*\"UP\"").containsMatchIn(healthBody)) {
            "A API release não respondeu com health UP em HTTPS."
        }
        val (feedStatus, feedBody) = runCatching { response("api/v1/feed") }
            .getOrElse { throw GradleException("Não foi possível consultar o feed release por HTTPS.", it) }
        require(feedStatus == 200 && Regex("\"items\"\\s*:\\s*\\[\\s*\\{").containsMatchIn(feedBody)) {
            "A API release não respondeu com notícias reais no feed por HTTPS."
        }
    }
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    dependsOn(validateReleaseApi)
}

android {
    namespace = "br.com.techfeedbr"
    compileSdk = 37

    defaultConfig {
        applicationId = "br.com.techfeedbr"
        minSdk = 26
        targetSdk = 37
        versionCode = 5
        versionName = "0.4.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    signingConfigs {
        create("release") {
            if (signingPropertiesFile.isFile) {
                storeFile = rootProject.file(signingProperties.getProperty("storeFile"))
                storePassword = signingProperties.getProperty("storePassword")
                keyAlias = signingProperties.getProperty("keyAlias")
                keyPassword = signingProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            val localUrl = providers.gradleProperty("TECHFEED_API_BASE_URL")
                .orElse("http://10.0.2.2:8080/").get()
            buildConfigField("String", "API_BASE_URL", "\"$localUrl\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            val cloudUrl = providers.gradleProperty("TECHFEED_RELEASE_API_BASE_URL").orNull
            if (releaseRequested) {
                require(!cloudUrl.isNullOrBlank() && cloudUrl.startsWith("https://") && cloudUrl.endsWith("/")) {
                    "Configure TECHFEED_RELEASE_API_BASE_URL com a URL HTTPS verificada da API, terminada em /."
                }
            }
            buildConfigField("String", "API_BASE_URL", "\"${cloudUrl ?: "https://example.invalid/"}\"")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.gson)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.androidx.browser)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
