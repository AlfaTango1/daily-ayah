import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.net.HttpURLConnection
import java.net.URL

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "uz.dailyayah"
    compileSdk = 35

    defaultConfig {
        applicationId = "uz.dailyayah"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "2.0"
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
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}

// ---------------------------------------------------------------------------
// Offline data: the full Quran text, two translations and Arabic fonts are
// downloaded once into src/main/assets before the first build, so the app
// needs no internet on the phone. Delete the folders to download them again.
// ---------------------------------------------------------------------------

fun httpGet(url: String): ByteArray {
    var lastError: Exception? = null
    repeat(3) {
        try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "DailyAyah-build")
            val token = System.getenv("GITHUB_TOKEN")
            if (url.startsWith("https://api.github.com") && !token.isNullOrBlank()) {
                conn.setRequestProperty("Authorization", "Bearer $token")
            }
            conn.connectTimeout = 30_000
            conn.readTimeout = 120_000
            if (conn.responseCode == 200) return conn.inputStream.use { it.readBytes() }
            lastError = RuntimeException("HTTP ${conn.responseCode} for $url")
        } catch (e: Exception) {
            lastError = e
        }
        Thread.sleep(3_000)
    }
    throw GradleException("Download failed: $url", lastError)
}

fun parseJson(bytes: ByteArray): Any = JsonSlurper().parseText(String(bytes, Charsets.UTF_8))

val downloadQuranAssets by tasks.registering {
    description = "Downloads Quran text, translations and Arabic fonts into src/main/assets"
    val assetsDir = file("src/main/assets")
    doLast {
        // 1) Quran text: one ayah per line, 6236 lines per file.
        val quranDir = File(assetsDir, "quran").apply { mkdirs() }
        val editions = linkedMapOf("ar" to "quran-uthmani", "en" to "en.sahih", "ru" to "ru.kuliev")
        for ((name, edition) in editions) {
            val out = File(quranDir, "$name.txt")
            val metaOut = File(quranDir, "surahs.json")
            if (out.length() > 0 && (name != "ar" || metaOut.length() > 0)) continue
            logger.lifecycle("Downloading Quran edition $edition")
            val json = parseJson(httpGet("https://api.alquran.cloud/v1/quran/$edition")) as Map<*, *>
            val surahs = (json["data"] as Map<*, *>)["surahs"] as List<*>
            val lines = ArrayList<String>(6236)
            val meta = ArrayList<Map<String, Any?>>()
            var bismillah: String? = null
            for (s in surahs) {
                s as Map<*, *>
                val surahNumber = (s["number"] as Number).toInt()
                val ayahs = s["ayahs"] as List<*>
                ayahs.forEachIndexed { i, a ->
                    var text = ((a as Map<*, *>)["text"] as String)
                        .replace("\r", " ").replace("\n", " ").trim()
                    if (name == "ar") {
                        if (surahNumber == 1 && i == 0) bismillah = text
                        // The API prefixes ayah 1 of most surahs with the Bismillah; strip it.
                        val b = bismillah
                        if (surahNumber != 1 && i == 0 && b != null && text.startsWith(b) && text.length > b.length) {
                            text = text.substring(b.length).trim()
                        }
                    }
                    lines += text
                }
                meta += mapOf(
                    "number" to surahNumber,
                    "name" to s["name"],
                    "englishName" to s["englishName"],
                    "meaning" to s["englishNameTranslation"],
                    "ayahs" to ayahs.size,
                )
            }
            if (lines.size != 6236) throw GradleException("$edition: expected 6236 ayahs, got ${lines.size}")
            out.writeText(lines.joinToString("\n"), Charsets.UTF_8)
            if (name == "ar") metaOut.writeText(JsonOutput.toJson(meta), Charsets.UTF_8)
        }

        // 2) Arabic fonts from the Google Fonts repository (SIL Open Font License).
        val fontsDir = File(assetsDir, "fonts").apply { mkdirs() }
        val fonts = linkedMapOf(
            "amiri" to "amiri",
            "scheherazade" to "scheherazadenew",
            "naskh" to "notonaskharabic",
            "kufi" to "notokufiarabic",
            "reemkufi" to "reemkufi",
            "lateef" to "lateef",
            "ruqaa" to "arefruqaa",
        )
        for ((key, family) in fonts) {
            val out = File(fontsDir, "$key.ttf")
            if (out.length() > 0) continue
            try {
                val listing = parseJson(httpGet("https://api.github.com/repos/google/fonts/contents/ofl/$family")) as List<*>
                val ttfs = listing.map { it as Map<*, *> }.filter { (it["name"] as String).endsWith(".ttf") }
                val pick = ttfs.firstOrNull { (it["name"] as String).endsWith("-Regular.ttf") }
                    ?: ttfs.firstOrNull { !(it["name"] as String).contains("Italic") }
                    ?: ttfs.first()
                out.writeBytes(httpGet(pick["download_url"] as String))
                logger.lifecycle("Font $key <- ${pick["name"]}")
            } catch (e: Exception) {
                out.delete()
                logger.warn("Font $key not downloaded (${e.message}); the app will fall back to a system font.")
            }
        }
    }
}

tasks.matching { it.name == "preBuild" }.configureEach { dependsOn(downloadQuranAssets) }
