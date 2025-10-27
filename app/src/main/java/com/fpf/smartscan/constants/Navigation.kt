package com.fpf.smartscan.constants

object Routes {
    const val SEARCH = "search"
    const val SCAN_HISTORY = "scan_history"
    const val SETTINGS = "settings"
    const val SETTINGS_DETAIL = "settings_detail/{type}"
    const val TEST = "test"
    const val DONATE = "donate"
    const val HELP = "help"
    const val TAG_MANAGER = "tag_manager"
    const val TAG_EDIT = "tag_edit/{tagName}"
    const val TAG_ADD = "tag_add"

    fun settingsDetail(type: String) = "settings_detail/$type"
    fun tagEdit(tagName: String) = "tag_edit/$tagName"
}

object SettingTypes {
    const val TARGETS = "targets"
    const val THRESHOLD = "threshold"
    const val DESTINATIONS = "destinations"
    const val ORGANISER_ACCURACY = "organiser_accuracy"
    const val MODELS = "models"
    const val MANAGE_MODELS = "manage_models"
    const val SEARCHABLE_IMG_DIRS = "searchable_img_dirs"
    const val SEARCHABLE_VID_DIRS = "searchable_vid_dirs"
}
