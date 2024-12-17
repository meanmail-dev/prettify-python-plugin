package dev.meanmail.prettifypython.settings

import kotlinx.serialization.Serializable

@Serializable
data class MappingsData(
    val exportDate: String,
    val pluginVersion: String,
    val ideVersion: String,
    val mappingsCount: Int,
    val comment: String = "",
    val mappings: List<MappingEntry>,
    val categories: Set<String> = emptySet()
)
