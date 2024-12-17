package dev.meanmail.prettifypython.settings

import com.intellij.openapi.ui.Messages
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.awt.*
import java.io.File
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter

class PrettifySettingsComponent {
    private val mappingFields = mutableMapOf<JTextField, JTextField>()
    private val mainPanel: JPanel
    private val mappingsPanel: JPanel
    private val json = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    init {
        mappingsPanel = JPanel(GridBagLayout())
        val settings = PrettifySettings.getInstance()

        mainPanel = JPanel(BorderLayout())
        mainPanel.border = JBUI.Borders.empty(10)

        val titleLabel = JLabel("Symbol Mappings")
        titleLabel.border = JBUI.Borders.empty(0, 0, 5, 0)
        mainPanel.add(titleLabel, BorderLayout.NORTH)

        val constraints = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            weightx = 1.0
        }

        // Headers
        constraints.gridy = 0
        constraints.gridx = 0
        mappingsPanel.add(JBLabel("Original").apply {
            border = JBUI.Borders.empty(0, 0, 5, 10)
        }, constraints)

        constraints.gridx = 1
        mappingsPanel.add(JBLabel("Replacement").apply {
            border = JBUI.Borders.empty(0, 0, 5, 10)
        }, constraints)

        constraints.gridx = 2
        mappingsPanel.add(JBLabel(""), constraints)

        // Add mapping fields
        var row = 1
        settings.symbolMappings.forEach { (original, replacement) ->
            constraints.gridy = row
            addMappingRow(constraints, original, replacement)
            row++
        }

        // Add buttons panel
        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT))

        val addButton = JButton("Add Mapping")
        addButton.addActionListener {
            constraints.gridy = mappingFields.size + 1
            addMappingRow(constraints)
            mainPanel.revalidate()
            mainPanel.repaint()
        }

        val resetButton = JButton("Reset All to Defaults")
        resetButton.addActionListener {
            setMappings(PrettifySettings.DEFAULT_MAPPINGS)
        }

        val importButton = JButton("Import")
        importButton.addActionListener {
            importMappings()
        }

        val exportButton = JButton("Export")
        exportButton.addActionListener {
            exportMappings()
        }

        buttonPanel.add(addButton)
        buttonPanel.add(resetButton)
        buttonPanel.add(importButton)
        buttonPanel.add(exportButton)

        val scrollPane = JBScrollPane(mappingsPanel)
        scrollPane.border = IdeBorderFactory.createEmptyBorder()
        mainPanel.add(scrollPane, BorderLayout.CENTER)
        mainPanel.add(buttonPanel, BorderLayout.SOUTH)
    }

    private fun importMappings() {
        val fileChooser = JFileChooser().apply {
            fileFilter = FileNameExtensionFilter("JSON files", "json")
            dialogTitle = "Import Mappings"
        }

        if (fileChooser.showOpenDialog(mainPanel) == JFileChooser.APPROVE_OPTION) {
            try {
                val file = fileChooser.selectedFile
                val jsonString = file.readText()
                val mappings = json.decodeFromString<Map<String, String>>(jsonString)
                setMappings(mappings)
            } catch (e: Exception) {
                Messages.showErrorDialog(
                    mainPanel,
                    "Failed to import mappings: ${e.message}",
                    "Import Error"
                )
            }
        }
    }

    private fun exportMappings() {
        val fileChooser = JFileChooser().apply {
            fileFilter = FileNameExtensionFilter("JSON files", "json")
            dialogTitle = "Export Mappings"
        }

        if (fileChooser.showSaveDialog(mainPanel) == JFileChooser.APPROVE_OPTION) {
            try {
                var file = fileChooser.selectedFile
                if (!file.name.endsWith(".json")) {
                    file = File(file.absolutePath + ".json")
                }

                val mappings = getMappings()
                val jsonString = json.encodeToString(mappings)
                file.writeText(jsonString)

                Messages.showInfoMessage(
                    mainPanel,
                    "Mappings exported successfully",
                    "Export Success"
                )
            } catch (e: Exception) {
                Messages.showErrorDialog(
                    mainPanel,
                    "Failed to export mappings: ${e.message}",
                    "Export Error"
                )
            }
        }
    }

    private fun addMappingRow(constraints: GridBagConstraints, original: String = "", replacement: String = "") {
        // Original field
        constraints.gridx = 0
        val originalField = JBTextField(original)
        originalField.preferredSize = Dimension(100, originalField.preferredSize.height)
        mappingsPanel.add(originalField, constraints)

        // Replacement field
        constraints.gridx = 1
        val replacementField = JBTextField(replacement)
        replacementField.preferredSize = Dimension(100, replacementField.preferredSize.height)
        mappingsPanel.add(replacementField, constraints)

        // Delete button
        constraints.gridx = 2
        val deleteButton = JButton("Delete")
        deleteButton.addActionListener {
            mappingFields.remove(originalField)
            mappingsPanel.remove(originalField)
            mappingsPanel.remove(replacementField)
            mappingsPanel.remove(deleteButton)
            mainPanel.revalidate()
            mainPanel.repaint()
        }
        mappingsPanel.add(deleteButton, constraints)

        mappingFields[originalField] = replacementField
    }

    val panel: JPanel get() = mainPanel

    fun getMappings(): Map<String, String> {
        return mappingFields.entries
            .filter { it.key.text.isNotBlank() }
            .associate { it.key.text to it.value.text }
    }

    fun setMappings(mappings: Map<String, String>) {
        mappingFields.clear()
        mappingsPanel.removeAll()

        val constraints = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            weightx = 1.0
        }

        // Headers
        constraints.gridy = 0
        constraints.gridx = 0
        mappingsPanel.add(JBLabel("Original").apply {
            border = JBUI.Borders.empty(0, 0, 5, 10)
        }, constraints)

        constraints.gridx = 1
        mappingsPanel.add(JBLabel("Replacement").apply {
            border = JBUI.Borders.empty(0, 0, 5, 10)
        }, constraints)

        constraints.gridx = 2
        mappingsPanel.add(JBLabel(""), constraints)

        // Add mapping fields
        var row = 1
        mappings.forEach { (original, replacement) ->
            constraints.gridy = row
            addMappingRow(constraints, original, replacement)
            row++
        }

        mainPanel.revalidate()
        mainPanel.repaint()
    }
}
