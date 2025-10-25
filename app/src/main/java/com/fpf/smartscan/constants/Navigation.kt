package com.fpf.smartscan.constants

object Routes {
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val SETTINGS_DETAIL = "settings_detail/{type}"
    const val DONATE = "donate"
    const val HELP = "help"
    fun settingsDetail(type: String) = "settings_detail/$type"
}

object SettingTypes {
    const val THRESHOLD = "threshold"
    const val MODELS = "models"
    const val MANAGE_MODELS = "manage_models"
    const val SEARCHABLE_IMG_DIRS = "searchable_img_dirs"
    const val SEARCHABLE_VID_DIRS = "searchable_vid_dirs"
}
