package com.fpf.smartscan.data

enum class MediaType {
    IMAGE, VIDEO
}

enum class QueryType {
    TEXT, IMAGE
}

enum class SearchMode {
    GENERAL, FACE
}

enum class ProcessorStatus {IDLE, ACTIVE, COMPLETE, FAILED }

enum class SortOption {
    SIMILARITY,      // Default - podle podobnosti z vyhledávání
    DATE_NEWEST,     // Podle data vytvoření - nejnovější první
    DATE_OLDEST,     // Podle data vytvoření - nejstarší první
    NAME_ASC,        // Podle jména souboru - A-Z
    NAME_DESC,       // Podle jména souboru - Z-A
    FOLDER           // Podle složky (cesty)
}
