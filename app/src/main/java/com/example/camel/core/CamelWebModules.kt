package com.example.camel.core

fun CamelAppId.webFolder(): String? = when (this) {
    CamelAppId.HORLOGE -> "horloge"
    CamelAppId.CALCULATOR -> "Gass-os-Calculator"
    CamelAppId.NOTES -> "note"
    CamelAppId.RECORDER -> "recorder"
    CamelAppId.CALENDRIER -> "calendrier"
    CamelAppId.WEATHER -> "weather"
    CamelAppId.MAPS -> "map"
    CamelAppId.MUSIC_PLAYER -> "music"
    CamelAppId.VIDEO_PLAYER -> "video-player"
    CamelAppId.RADIO_GOLD -> "radio"
    CamelAppId.TV_GOLD -> "tv"
    CamelAppId.CAMERA -> "camera"
    CamelAppId.GALERIE -> "galerie"
    CamelAppId.BOUSSOLE -> "boussole"
    CamelAppId.SYSTEM_INFO -> "info"
    CamelAppId.MANAGER_GOLD -> "manager"
    CamelAppId.LINUX_LOADER -> "linux-loader"
    CamelAppId.WINDOWS_LOADER -> "windows-loader"
    else -> null
}
