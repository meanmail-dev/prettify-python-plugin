package dev.meanmail.prettifypython.settings

import kotlinx.serialization.Serializable

@Serializable
data class MappingEntry(
    var from: String = "",
    var to: String = "",
    var category: String = ""
)
