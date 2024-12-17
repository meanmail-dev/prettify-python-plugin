package dev.meanmail.prettifypython.settings

import kotlinx.serialization.Serializable

@Serializable
data class MappingEntry(
    val from: String = "",
    val to: String = "",
    val category: String = ""
)
