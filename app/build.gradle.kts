import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kover)
}

// Assinatura da versão da Play. Vem de variáveis de ambiente (segredos do GitHub Actions)
// ou de um keystore.properties local que nunca vai para o git. Sem isso, o release sai sem assinatura.
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun signingValue(env: String, prop: String): String? =
    System.getenv(env)?.takeIf { it.isNotBlank() } ?: keystoreProps.getProperty(prop)

val releaseStoreFile = signingValue("BABYLOCK_KEYSTORE_FILE", "storeFile")

android {
    namespace = "app.babylock"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.babylock"
        minSdk = 26
        targetSdk = 36
        // A Play exige um versionCode maior a cada envio: o CI passa o número da execução.
        versionCode = (System.getenv("BABYLOCK_VERSION_CODE") ?: "1").toInt()
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Só os idiomas que o app traduziu (as bibliotecas trazem dezenas; os outros caem no inglês).
        @Suppress("DEPRECATION")
        resourceConfigurations += listOf("en", "pt", "es")
    }

    androidResources {
        // Android 13+: Configurações > Apps > BabyLock > Idioma.
        generateLocaleConfig = true
    }

    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile)
                storePassword = signingValue("BABYLOCK_KEYSTORE_PASSWORD", "storePassword")
                keyAlias = signingValue("BABYLOCK_KEY_ALIAS", "keyAlias")
                keyPassword = signingValue("BABYLOCK_KEY_PASSWORD", "keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }
    bundle {
        // Todos os idiomas no mesmo pacote: quem troca o idioma do app não precisa baixar nada.
        language { enableSplit = false }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
            all {
                it.maxHeapSize = "2g"
                // Mostra a mensagem e a pilha completas no log do CI quando um teste falha.
                it.testLogging {
                    exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
                    showStackTraces = true
                }
            }
        }
        animationsDisabled = true
    }
    lint {
        // Falhas de lint quebram o build; avisos viram relatório.
        abortOnError = true
        checkDependencies = true
        htmlReport = true
        xmlReport = true
    }
}

kover {
    reports {
        filters {
            excludes {
                // Telas e temas são cobertos por testes de UI; gerados pelo compilador ficam fora.
                classes("*ComposableSingletons*", "*_Factory", "app.babylock.ui.theme.*")
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.biometric)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Testes de unidade e de UI na JVM (Robolectric)
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)

    // Testes no emulador (de ponta a ponta)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.uiautomator)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
