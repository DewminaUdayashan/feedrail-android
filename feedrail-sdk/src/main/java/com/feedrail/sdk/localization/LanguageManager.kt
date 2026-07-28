package com.feedrail.sdk.localization

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.core.text.layoutDirection
import androidx.core.view.ViewCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class LanguageConfig(
    val code: String,
    val locale: Locale,
    val isRtl: Boolean,
    val localizedContext: Context,
)

data class LanguageOption(val code: String, val displayName: String)

/** Manages runtime language selection for the SDK without affecting the host app locale. */
class LanguageManager(
    private val appContext: Context,
    preferredLocale: String?,
) {
    companion object {
        val supportedLanguages = listOf(
            LanguageOption("en", "English"),
            LanguageOption("ar", "العربية"),
            LanguageOption("de", "Deutsch"),
            LanguageOption("es", "Español"),
            LanguageOption("fr", "Français"),
            LanguageOption("it", "Italiano"),
            LanguageOption("ja", "日本語"),
            LanguageOption("ko", "한국어"),
            LanguageOption("zh-CN", "简体中文"),
            LanguageOption("zh-TW", "繁體中文"),
        )

        private val supportedCodes = supportedLanguages.map { it.code }

        fun validate(code: String): String {
            return supportedCodes.firstOrNull { supported ->
                code == supported || code.startsWith(supported.substringBefore('-'))
            } ?: "en"
        }
    }

    private val _config: MutableStateFlow<LanguageConfig>
    val config: StateFlow<LanguageConfig>

    val currentCode: String get() = _config.value.code
    val currentLocale: Locale get() = _config.value.locale
    val isRtl: Boolean get() = _config.value.isRtl

    init {
        val deviceCode = Locale.getDefault().language
        val resolved = validate(preferredLocale ?: deviceCode)
        _config = MutableStateFlow(buildConfig(resolved))
        config = _config.asStateFlow()
    }

    fun selectLanguage(code: String) {
        _config.value = buildConfig(validate(code))
    }

    fun string(resId: Int): String = _config.value.localizedContext.getString(resId)
    fun string(resId: Int, vararg formatArgs: Any): String =
        _config.value.localizedContext.getString(resId, *formatArgs)

    private fun buildConfig(code: String): LanguageConfig {
        val locale = when (code) {
            "zh-CN" -> Locale.SIMPLIFIED_CHINESE
            "zh-TW" -> Locale.TRADITIONAL_CHINESE
            else -> Locale.forLanguageTag(code)
        }
        val isRtl = locale.layoutDirection == ViewCompat.LAYOUT_DIRECTION_RTL
        val localizedContext = appContext.createConfigurationContext(
            Configuration(appContext.resources.configuration).also { cfg ->
                cfg.setLocale(locale)
            }
        )
        return LanguageConfig(code, locale, isRtl, localizedContext)
    }
}
