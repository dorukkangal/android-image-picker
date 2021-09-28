package com.esafirm.imagepicker.helper

import android.content.Context
import android.content.res.Configuration
import java.util.*

object LocaleManager {
    var language: String? = null
        get() = if (field?.isNotEmpty() == true) field else Locale.getDefault().language

    fun updateResources(context: Context): Context {
        var locale = Locale(language!!)
        locale = normalizeLocale(locale)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    private fun normalizeLocale(localeLanguage: Locale): Locale {
        val ZH = "zh"
        val TW = "TW"
        val CN = "CN"

        val newLocaleLanguage = localeLanguage.toString()

        return if (newLocaleLanguage.length == 5) {
            Locale(
                newLocaleLanguage.substring(0, 2),
                newLocaleLanguage.substring(3, 5).toUpperCase()
            )
        } else if (newLocaleLanguage == ZH) {
            if (Locale.getDefault().country == TW) {
                Locale(ZH, TW)
            } else {
                Locale(ZH, CN)
            }
        } else {
            localeLanguage
        }
    }
}
