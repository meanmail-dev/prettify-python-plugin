package dev.meanmail.prettifypython.settings

import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

class PrettifySettingsComponent {
    private val mappingFields = mutableMapOf<JTextField, JTextField>()
    private val mainPanel: JPanel
    private val mappingsPanel: JPanel

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

        // Add and Reset buttons panel
        val buttonPanel = JPanel(BorderLayout())

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

        buttonPanel.add(addButton, BorderLayout.WEST)
        buttonPanel.add(resetButton, BorderLayout.EAST)

        val scrollPane = JBScrollPane(mappingsPanel)
        scrollPane.border = IdeBorderFactory.createEmptyBorder()
        mainPanel.add(scrollPane, BorderLayout.CENTER)
        mainPanel.add(buttonPanel, BorderLayout.SOUTH)
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
