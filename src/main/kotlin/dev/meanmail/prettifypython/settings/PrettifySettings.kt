package dev.meanmail.prettifypython.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import kotlinx.serialization.Serializable

@State(
    name = "PrettifyPythonSettings",
    storages = [Storage("PrettifyPythonSettings.xml")]
)
@Service
class PrettifySettings : PersistentStateComponent<PrettifySettings.State> {

    @Serializable
    data class State(
        var mappings: List<MappingEntry> = DEFAULT_MAPPINGS,
        var categories: Set<String> = DEFAULT_CATEGORIES
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    var mappings: List<Pair<String, String>>
        get() = myState.mappings.map { Pair(it.from, it.to) }
        set(value) {
            val oldCategories = myState.mappings.map { it.category }
            myState.mappings = value.zip(oldCategories) { pair, category ->
                MappingEntry(pair.first, pair.second, category)
            }
        }

    var categories: Set<String>
        get() = myState.categories
        set(value) {
            myState.categories = value
        }

    fun getDefaultMappings(): List<MappingEntry> {
        return DEFAULT_MAPPINGS
    }

    fun resetToDefaults() {
        myState.mappings = getDefaultMappings()
        myState.categories = myState.mappings.map { it.category }.toSet()
    }

    companion object {
        fun getInstance(): PrettifySettings =
            ApplicationManager.getApplication().getService(PrettifySettings::class.java)

        private val DEFAULT_MAPPINGS = listOf(
            MappingEntry(">=", "≥", "Comparison"),
            MappingEntry("<=", "≤", "Comparison"),
            MappingEntry("!=", "≠", "Comparison"),
            MappingEntry("->", "➔", "Arrow"),
            MappingEntry("lambda", "λ", "Keyword"),
            MappingEntry("**", "^", "Operator")
        )

        private val DEFAULT_CATEGORIES = DEFAULT_MAPPINGS.map { it.category }.toSet()
    }
}
