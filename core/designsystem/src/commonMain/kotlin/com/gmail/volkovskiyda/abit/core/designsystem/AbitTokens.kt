package com.gmail.volkovskiyda.abit.core.designsystem

/**
 * Every colour the design names, as plain ARGB longs.
 *
 * Compose-free on purpose: Wear OS uses its own Material 3, whose `ColorScheme` is a different type
 * from the multiplatform one, and a unit test asserting on a token should not have to stand up a
 * composition to read it. [abitLightColorScheme] and [abitDarkColorScheme] are the only places these
 * become `Color`.
 *
 * **The rule that keeps a screen quiet:** the mode colour appears in exactly three places — the ring
 * arc, the mode label and the primary action. Everything else is neutral. A Break screen swaps
 * primary for tertiary in those same three places and never mixes both. Off hours uses no accent at
 * all. Tangerine means Focus and mint means Break, which is why there is no dynamic colour anywhere
 * in this app: a device-tinted scheme would repaint that meaning at random.
 */
object AbitTokens {
    object Light {
        const val PRIMARY = 0xFFC8401A
        const val ON_PRIMARY = 0xFFFFFFFF
        const val PRIMARY_CONTAINER = 0xFFFFDBD0
        const val ON_PRIMARY_CONTAINER = 0xFF3E0F00

        /** The raw tangerine. Ring arcs and large display numerals only — it is 4.4 : 1, not 4.5. */
        const val INVERSE_PRIMARY = 0xFFFF6A3D

        const val SECONDARY = 0xFF57607A
        const val ON_SECONDARY = 0xFFFFFFFF
        const val SECONDARY_CONTAINER = 0xFFDCE3F7
        const val ON_SECONDARY_CONTAINER = 0xFF141B2C

        const val TERTIARY = 0xFF15803D
        const val ON_TERTIARY = 0xFFFFFFFF
        const val TERTIARY_CONTAINER = 0xFFBBF7D0
        const val ON_TERTIARY_CONTAINER = 0xFF052E16

        /** The light theme's break arc: brighter than [TERTIARY], which carries text instead. */
        const val TERTIARY_ARC = 0xFF22C55E

        const val ERROR = 0xFFBA1A1A
        const val ON_ERROR = 0xFFFFFFFF
        const val ERROR_CONTAINER = 0xFFFFDAD6
        const val ON_ERROR_CONTAINER = 0xFF410002

        const val SURFACE = 0xFFF7F8FC
        const val SURFACE_DIM = 0xFFDCE3F7
        const val SURFACE_BRIGHT = 0xFFFFFFFF
        const val SURFACE_CONTAINER_LOWEST = 0xFFFFFFFF
        const val SURFACE_CONTAINER_LOW = 0xFFF1F3F9
        const val SURFACE_CONTAINER = 0xFFEBEEF6
        const val SURFACE_CONTAINER_HIGH = 0xFFE4E8F2
        const val SURFACE_CONTAINER_HIGHEST = 0xFFDCE3F7
        const val SURFACE_VARIANT = 0xFFDCE3F7
        const val ON_SURFACE = 0xFF1B2030
        const val ON_SURFACE_VARIANT = 0xFF4A5266

        /** The icon's ground. */
        const val INVERSE_SURFACE = 0xFF2E3444
        const val INVERSE_ON_SURFACE = 0xFFDCE3F7

        const val OUTLINE = 0xFF737C92
        const val OUTLINE_VARIANT = 0xFFC3CADB
    }

    object Dark {
        const val PRIMARY = 0xFFFF6A3D
        const val ON_PRIMARY = 0xFF3E0F00
        const val PRIMARY_CONTAINER = 0xFF7A2A0E
        const val ON_PRIMARY_CONTAINER = 0xFFFFDBD0
        const val INVERSE_PRIMARY = 0xFFC8401A

        const val SECONDARY = 0xFFB9C3DD
        const val ON_SECONDARY = 0xFF232B3E
        const val SECONDARY_CONTAINER = 0xFF3A4358
        const val ON_SECONDARY_CONTAINER = 0xFFDCE3F7

        const val TERTIARY = 0xFF4ADE80
        const val ON_TERTIARY = 0xFF052E16
        const val TERTIARY_CONTAINER = 0xFF166534
        const val ON_TERTIARY_CONTAINER = 0xFFBBF7D0

        /** Dark needs no separate arc colour: [TERTIARY] is already the bright mint. */
        const val TERTIARY_ARC = TERTIARY

        const val ERROR = 0xFFFFB4AB
        const val ON_ERROR = 0xFF690005
        const val ERROR_CONTAINER = 0xFF93000A
        const val ON_ERROR_CONTAINER = 0xFFFFDAD6

        const val SURFACE = 0xFF151A28
        const val SURFACE_DIM = 0xFF151A28
        const val SURFACE_BRIGHT = 0xFF394054
        const val SURFACE_CONTAINER_LOWEST = 0xFF0F1420
        const val SURFACE_CONTAINER_LOW = 0xFF1D2331
        const val SURFACE_CONTAINER = 0xFF232A3A
        const val SURFACE_CONTAINER_HIGH = 0xFF2E3444
        const val SURFACE_CONTAINER_HIGHEST = 0xFF394054
        const val SURFACE_VARIANT = 0xFF2E3444
        const val ON_SURFACE = 0xFFDCE3F7
        const val ON_SURFACE_VARIANT = 0xFFC3CADB

        const val INVERSE_SURFACE = 0xFFDCE3F7
        const val INVERSE_ON_SURFACE = 0xFF2E3444

        const val OUTLINE = 0xFF838CA2
        const val OUTLINE_VARIANT = 0xFF434B5E
    }

    /**
     * Wear OS only. The one place pure black is allowed: an OLED watch face saves power on it and
     * the convention expects it, but on any other surface it would be a hole in the tonal palette.
     */
    const val WEAR_BACKGROUND = 0xFF000000
}
