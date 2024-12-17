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
        var mappings: List<MappingEntry> = DEFAULT_MAPPINGS
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    var mappings: List<Pair<String, String>>
        get() = myState.mappings.map { Pair(it.from, it.to) }
        set(value) {
            myState.mappings = value.map { MappingEntry(it.first, it.second) }
        }

    fun resetToDefaults() {
        mappings = DEFAULT_MAPPINGS_PAIRS
    }

    companion object {
        fun getInstance(): PrettifySettings =
            ApplicationManager.getApplication().getService(PrettifySettings::class.java)

        private val DEFAULT_MAPPINGS = listOf(
            MappingEntry(">=", "≥"),
            MappingEntry("<=", "≤"),
            MappingEntry("!=", "≠"),
            MappingEntry("->", "➔"),
            MappingEntry("lambda", "λ"),
            MappingEntry("**", "^")
        )

        private val DEFAULT_MAPPINGS_PAIRS = DEFAULT_MAPPINGS.map { Pair(it.from, it.to) }
    }
}
