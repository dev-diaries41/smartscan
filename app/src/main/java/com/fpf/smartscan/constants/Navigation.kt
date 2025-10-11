package com.fpf.smartscan.constants

object Routes {
    const val SEARCH = "search"
    const val SCAN_HISTORY = "scanhistory"
    const val SETTINGS = "settings"
    const val SETTINGS_DETAIL = "settingsDetail/{type}"
    const val TEST = "test"
    const val DONATE = "donate"
    const val HELP = "help"
    fun settingsDetail(type: String) = "settingsDetail/$type"
}

object SettingTypes {
    const val TARGETS = "targets"
    const val THRESHOLD = "threshold"
    const val DESTINATIONS = "destinations"
    const val ORGANISER_ACCURACY = "organiserAccuracy"
    const val MODELS = "models"
    const val MANAGE_MODELS = "manageModels"
}
