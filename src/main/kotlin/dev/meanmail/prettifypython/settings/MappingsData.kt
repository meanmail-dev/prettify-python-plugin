package dev.meanmail.prettifypython.settings

import kotlinx.serialization.Serializable

@Serializable
data class MappingsData(
    val version: String = "1.0",
    val exportDate: String,
    val pluginVersion: String,
    val ideVersion: String,
    val mappingsCount: Int,
    val comment: String = "",
    val mappings: List<MappingEntry>
)
