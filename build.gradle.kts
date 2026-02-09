// Top-level build file
plugins {
    // Kita deklarasikan versi plugin di sini biar rapi
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" apply false
}

// SUDAH BERSIH! 
// Tidak ada lagi perintah "subprojects { apply plugin 'spotless' }" di sini.