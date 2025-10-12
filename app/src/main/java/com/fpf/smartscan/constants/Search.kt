package com.fpf.smartscan.constants

import com.fpf.smartscan.data.MediaType
import com.fpf.smartscan.data.QueryType

val mediaTypeOptions = mapOf(
    MediaType.IMAGE to "Images",
    MediaType.VIDEO to "Videos",
)

val queryOptions = mapOf(
    QueryType.TEXT to "Text",
    QueryType.IMAGE to "Image"
)