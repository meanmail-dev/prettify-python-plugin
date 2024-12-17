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

    override fun getDisplayName(): String = "Prettify Python"

    override fun createComponent(): JComponent {
        settingsComponent = PrettifySettingsComponent()
        return settingsComponent!!.panel
    }

    override fun isModified(): Boolean {
        val settings = PrettifySettings.getInstance()
        return settingsComponent?.getMappings() != settings.symbolMappings
    }

    override fun apply() {
        val settings = PrettifySettings.getInstance()
        settings.symbolMappings = settingsComponent?.getMappings() ?: mutableMapOf()

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
        val settings = PrettifySettings.getInstance()
        settingsComponent?.setMappings(settings.symbolMappings)
    }

    override fun disposeUIResources() {
        settingsComponent = null
    }
}
