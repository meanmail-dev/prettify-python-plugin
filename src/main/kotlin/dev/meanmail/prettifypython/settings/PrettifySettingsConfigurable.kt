package dev.meanmail.prettifypython.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.FileContentUtilCore
import javax.swing.JComponent

class PrettifySettingsConfigurable : Configurable {
    private var settingsComponent: PrettifySettingsComponent? = null
    private val settings = PrettifySettings.getInstance()

    override fun getDisplayName(): String = "Pretty Python"

    override fun createComponent(): JComponent {
        settingsComponent = PrettifySettingsComponent()
        settingsComponent?.setMappings(settings.getState().mappings)
        return settingsComponent!!.getPanel()
    }

    override fun isModified(): Boolean {
        return settingsComponent?.getMappings() != settings.getState().mappings
    }

    override fun apply() {
        settingsComponent?.let { component ->
            settings.getState().mappings = component.getMappings()
            settings.getState().categories = component.getMappings().map { it.category }.toSet()
        }

        // Update all open files
        ApplicationManager.getApplication().invokeLater {
            val filesToReparse = mutableListOf<VirtualFile>()

            ProjectManager.getInstance().openProjects.forEach { project ->
                FileEditorManager.getInstance(project).allEditors.forEach { editor ->
                    if (editor is TextEditor) {
                        editor.file?.let { filesToReparse.add(it) }
                    }
                }
            }

            FileContentUtilCore.reparseFiles(filesToReparse)
        }
    }

    override fun reset() {
        settingsComponent?.setMappings(settings.getState().mappings)
    }

    override fun disposeUIResources() {
        settingsComponent = null
    }
}
