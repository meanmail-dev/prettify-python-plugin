package dev.meanmail.prettifypython.settings

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionToolbarPosition
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.table.JBTable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.awt.BorderLayout
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.swing.JFileChooser
import javax.swing.JPanel
import javax.swing.filechooser.FileNameExtensionFilter
import javax.swing.table.AbstractTableModel

class PrettifySettingsComponent {
    private val mainPanel: JPanel
    private val mappingsTable: JBTable
    private val tableModel: MappingsTableModel = MappingsTableModel()
    private val json = Json { prettyPrint = true }

    init {
        mappingsTable = JBTable(tableModel)
        mappingsTable.setShowGrid(false)
        mappingsTable.rowHeight = 22

        val toolbarDecorator = ToolbarDecorator.createDecorator(mappingsTable)
            .setAddAction { addMapping() }
            .setRemoveAction { removeSelectedMappings() }
            .setToolbarPosition(ActionToolbarPosition.RIGHT)
            .addExtraAction(object : AnAction("Import", "Import mappings from JSON", AllIcons.Actions.Download) {
                override fun actionPerformed(e: AnActionEvent) {
                    importMappings()
                }
            })
            .addExtraAction(object : AnAction("Export", "Export mappings to JSON", AllIcons.Actions.Upload) {
                override fun actionPerformed(e: AnActionEvent) {
                    exportMappings()
                }
            })
            .addExtraAction(object :
                AnAction("Reset to Default", "Reset mappings to default values", AllIcons.Actions.Rollback) {
                override fun actionPerformed(e: AnActionEvent) {
                    resetToDefault()
                }
            })

        mainPanel = JPanel(BorderLayout()).apply {
            add(toolbarDecorator.createPanel(), BorderLayout.CENTER)
            border = IdeBorderFactory.createTitledBorder("Symbol Mappings", false)
        }
    }

    private fun addMapping() {
        tableModel.addMapping("", "")
    }

    private fun removeSelectedMappings() {
        val selectedRows = mappingsTable.selectedRows.sortedDescending()
        selectedRows.forEach { row ->
            tableModel.removeMapping(row)
        }
    }

    private fun importMappings() {
        val fileChooser = JFileChooser().apply {
            fileFilter = FileNameExtensionFilter("JSON files", "json")
            dialogTitle = "Import Mappings"
        }

        if (fileChooser.showOpenDialog(mainPanel) == JFileChooser.APPROVE_OPTION) {
            try {
                val jsonContent = fileChooser.selectedFile.readText()
                val importedMappings = json.decodeFromString<List<Pair<String, String>>>(jsonContent)
                tableModel.setMappings(importedMappings)
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
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            selectedFile = File("prettify_python_mappings_$timestamp.json")
        }

        if (fileChooser.showSaveDialog(mainPanel) == JFileChooser.APPROVE_OPTION) {
            try {
                val jsonContent = json.encodeToString(tableModel.getMappings())
                fileChooser.selectedFile.writeText(jsonContent)
            } catch (e: Exception) {
                Messages.showErrorDialog(
                    mainPanel,
                    "Failed to export mappings: ${e.message}",
                    "Export Error"
                )
            }
        }
    }

    private fun resetToDefault() {
        val result = Messages.showYesNoDialog(
            mainPanel,
            "Are you sure you want to reset all mappings to default values?",
            "Reset Mappings",
            Messages.getQuestionIcon()
        )

        if (result == Messages.YES) {
            val settings = PrettifySettings.getInstance()
            settings.resetToDefaults()
            tableModel.setMappings(settings.mappings)
        }
    }

    fun getPanel(): JPanel = mainPanel

    fun getMappings(): List<Pair<String, String>> = tableModel.getMappings()

    fun setMappings(mappings: List<Pair<String, String>>) {
        tableModel.setMappings(mappings)
    }

    private inner class MappingsTableModel : AbstractTableModel() {
        private val columnNames = arrayOf("From", "To")
        private val mappings = mutableListOf<Pair<String, String>>()

        fun addMapping(from: String, to: String) {
            mappings.add(Pair(from, to))
            fireTableRowsInserted(mappings.size - 1, mappings.size - 1)
        }

        fun removeMapping(index: Int) {
            if (index >= 0 && index < mappings.size) {
                mappings.removeAt(index)
                fireTableRowsDeleted(index, index)
            }
        }

        fun getMappings(): List<Pair<String, String>> = mappings.toList()

        fun setMappings(newMappings: List<Pair<String, String>>) {
            mappings.clear()
            mappings.addAll(newMappings)
            fireTableDataChanged()
        }

        override fun getRowCount(): Int = mappings.size

        override fun getColumnCount(): Int = 2

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            val mapping = mappings[rowIndex]
            return when (columnIndex) {
                0 -> mapping.first
                1 -> mapping.second
                else -> throw IllegalArgumentException("Invalid column index")
            }
        }

        override fun getColumnName(column: Int): String = columnNames[column]

        override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = true

        override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int) {
            val current = mappings[rowIndex]
            val newMapping = when (columnIndex) {
                0 -> Pair(aValue.toString(), current.second)
                1 -> Pair(current.first, aValue.toString())
                else -> return
            }
            mappings[rowIndex] = newMapping
            fireTableCellUpdated(rowIndex, columnIndex)
        }
    }
}
