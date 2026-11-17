package com.example.camel.core

enum class CamelThemePreset(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val primaryHex: Long,
    val secondaryHex: Long,
    val backgroundHex: Long,
    val surfaceHex: Long,
    val surfaceVariantHex: Long
) {
    MIDNIGHT_GOLD(
        id = "midnight_gold",
        displayName = "Midnight Gold",
        subtitle = "Obsidian black, deep navy & champagne gold",
        primaryHex = 0xFFD4AF37,
        secondaryHex = 0xFFF3D578,
        backgroundHex = 0xFF07090E,
        surfaceHex = 0xFF0D1B3E,
        surfaceVariantHex = 0xFF132347
    ),
    ROYAL_NAVY(
        id = "royal_navy",
        displayName = "Royal Navy",
        subtitle = "Deep sapphire blue with bright gold trim",
        primaryHex = 0xFFF3D578,
        secondaryHex = 0xFF60A5FA,
        backgroundHex = 0xFF060E24,
        surfaceHex = 0xFF0E2252,
        surfaceVariantHex = 0xFF17316B
    ),
    OBSIDIAN_MINIMAL(
        id = "obsidian_minimal",
        displayName = "Obsidian Minimal",
        subtitle = "Carbon stealth contrast with warm brass",
        primaryHex = 0xFFE5C158,
        secondaryHex = 0xFFCBD5E1,
        backgroundHex = 0xFF050608,
        surfaceHex = 0xFF10131A,
        surfaceVariantHex = 0xFF1B212C
    ),
    EMERALD_CARBON(
        id = "emerald_carbon",
        displayName = "Emerald Carbon",
        subtitle = "Deep graphite slate with emerald & gold accents",
        primaryHex = 0xFF10B981,
        secondaryHex = 0xFFD4AF37,
        backgroundHex = 0xFF050A09,
        surfaceHex = 0xFF0B1F1C,
        surfaceVariantHex = 0xFF12302B
    ),
    AMETHYST_NOIR(
        id = "amethyst_noir",
        displayName = "Amethyst Noir",
        subtitle = "Velvet dark indigo with pale gold highlights",
        primaryHex = 0xFFC084FC,
        secondaryHex = 0xFFF3D578,
        backgroundHex = 0xFF080612,
        surfaceHex = 0xFF15102B,
        surfaceVariantHex = 0xFF211A42
    );

    companion object {
        fun fromId(id: String?): CamelThemePreset =
            entries.firstOrNull { it.id == id } ?: MIDNIGHT_GOLD
    }
}

enum class CamelWallpaperPreset(
    val id: String,
    val displayName: String,
    val topHex: Long,
    val midHex: Long,
    val bottomHex: Long
) {
    GOLDEN_DUNES(
        id = "golden_dunes",
        displayName = "Golden Dunes",
        topHex = 0xFF07090E,
        midHex = 0xFF0D1B3E,
        bottomHex = 0xFF1A1407
    ),
    MIDNIGHT_AURORA(
        id = "midnight_aurora",
        displayName = "Midnight Aurora",
        topHex = 0xFF030712,
        midHex = 0xFF0F2942,
        bottomHex = 0xFF091224
    ),
    SAPPHIRE_GRID(
        id = "sapphire_grid",
        displayName = "Royal Sapphire",
        topHex = 0xFF040B1F,
        midHex = 0xFF132E6B,
        bottomHex = 0xFF060E24
    ),
    CARBON_SILK(
        id = "carbon_silk",
        displayName = "Carbon Silk",
        topHex = 0xFF050507,
        midHex = 0xFF181A20,
        bottomHex = 0xFF0A0B0E
    ),
    CUSTOM_IMAGE(
        id = "custom_image",
        displayName = "Custom Photo",
        topHex = 0xFF07090E,
        midHex = 0xFF0D1B3E,
        bottomHex = 0xFF07090E
    );

    companion object {
        fun fromId(id: String?): CamelWallpaperPreset =
            entries.firstOrNull { it.id == id } ?: GOLDEN_DUNES
    }
}

enum class CamelFontStyle(
    val id: String,
    val displayName: String,
    val description: String
) {
    PLUS_JAKARTA("plus_jakarta", "Plus Jakarta Sans", "Balanced geometric desktop UI"),
    INTER("inter", "Inter Clean", "Crisp high-legibility sans serif"),
    JETBRAINS_MONO("jetbrains_mono", "JetBrains Mono", "Technical developer monospace"),
    PLAYFAIR("playfair", "Playfair Display", "Editorial luxury serif headings");

    companion object {
        fun fromId(id: String?): CamelFontStyle =
            entries.firstOrNull { it.id == id } ?: PLUS_JAKARTA
    }
}
