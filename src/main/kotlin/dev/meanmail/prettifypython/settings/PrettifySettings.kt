package dev.meanmail.prettifypython.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(
    name = "PrettifySettings",
    storages = [Storage("PrettifySettings.xml")]
)
class PrettifySettings : PersistentStateComponent<PrettifySettings.State> {
    companion object {
        fun getInstance(): PrettifySettings =
            ApplicationManager.getApplication().getService(PrettifySettings::class.java)

        val DEFAULT_MAPPINGS = mapOf(
            ">=" to "≥",
            "<=" to "≤",
            "!=" to "≠",
            "->" to "➔",
            "lambda" to "λ",
            "**" to "^"
        )
    }

    data class State(
        var symbolMappings: MutableMap<String, String> = DEFAULT_MAPPINGS.toMutableMap()
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    var symbolMappings: Map<String, String>
        get() = myState.symbolMappings
        set(value) {
            myState.symbolMappings = value.toMutableMap()
        }

    fun resetToDefaults() {
        symbolMappings = DEFAULT_MAPPINGS
    }
}
