package io.nosyntax.foundation.domain.model.app_config

data class ThemeConfig(
    val colorScheme: ColorSchemeConfig,
    val typography: TypographyConfig,
    val settings: SettingsConfig
) {

    data class SettingsConfig(
        val darkMode: Boolean
    )
}