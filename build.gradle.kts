import com.lagradost.cloudstream3.gradle.CloudstreamExtension
importpackage com.android.build.gradle.BaseExtension

buildscript {
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    id("com.android.library")
    kotlin("android")
    id("com.lagradost.cloudstream3.gradle")
}

configure<CloudstreamExtension> {
    // Eklentinin program içinde görünecek ismi ve ayarları
    name = "GledaiTV"
    description = "GledaiTV Canlı Yayın Eklentisi"
    providerClass = "com.lagradost.cloudstream3.plugins.GledaiTVProvider"
    authors = listOf("Efo1313")
}

configure<BaseExtension> {
    compileSdkVersion(33)
    defaultConfig {
        minSdkVersion(21)
        targetSdkVersion(33)
    }
}
