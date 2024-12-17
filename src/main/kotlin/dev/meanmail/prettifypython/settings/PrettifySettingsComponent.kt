package dev.meanmail.prettifypython.settings

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionToolbarPosition
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.tree.TreeUtil
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeSelectionModel

class PrettifySettingsComponent {
    private val mainPanel: JPanel
    private val mappingsTree: Tree
    private val treeModel: DefaultTreeModel
    private val rootNode = DefaultMutableTreeNode("Mappings")
    private val json = Json { prettyPrint = true }

    init {
        treeModel = DefaultTreeModel(rootNode)
        mappingsTree = Tree(treeModel).apply {
            isRootVisible = false
            showsRootHandles = true
            selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
            cellRenderer = MappingTreeCellRenderer()
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    if (e.clickCount == 2) {
                        editSelectedMapping()
                    }
                }
            })
        }

        val toolbarDecorator = ToolbarDecorator.createDecorator(mappingsTree)
            .setAddAction { addMapping() }
            .setRemoveAction { removeSelectedMapping() }
            .setEditAction { editSelectedMapping() }
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

    private fun editSelectedMapping() {
        val node = mappingsTree.lastSelectedPathComponent as? DefaultMutableTreeNode ?: return
        val mapping = node.userObject as? MappingEntry ?: return

        val dialog = AddMappingDialog(mainPanel, mapping)
        dialog.show()
        if (dialog.isOK) {
            val updatedMapping = dialog.getMapping()
            val parent = node.parent as DefaultMutableTreeNode

            if (updatedMapping.category == mapping.category) {
                // Если категория не изменилась, просто обновляем узел
                node.userObject = updatedMapping
                treeModel.nodeChanged(node)
            } else {
                // Если категория изменилась, перемещаем в новую категорию
                treeModel.removeNodeFromParent(node)
                if (parent != rootNode && parent.childCount == 0) {
                    treeModel.removeNodeFromParent(parent)
                }
                addMappingToTree(updatedMapping)
            }
            TreeUtil.expandAll(mappingsTree)
        }
    }

    private fun addMapping() {
        val dialog = AddMappingDialog(mainPanel)
        dialog.show()
        if (dialog.isOK) {
            val mapping = dialog.getMapping()
            addMappingToTree(mapping)
            TreeUtil.expandAll(mappingsTree)
        }
    }

    private fun addMappingToTree(mapping: MappingEntry) {
        val categoryNode = findOrCreateCategoryNode(mapping.category)
        val mappingNode = DefaultMutableTreeNode(mapping)
        treeModel.insertNodeInto(mappingNode, categoryNode, categoryNode.childCount)
    }

    private fun findOrCreateCategoryNode(category: String): DefaultMutableTreeNode {
        for (i in 0 until rootNode.childCount) {
            val node = rootNode.getChildAt(i) as DefaultMutableTreeNode
            if (node.userObject == category) {
                return node
            }
        }
        val newNode = DefaultMutableTreeNode(category)
        treeModel.insertNodeInto(newNode, rootNode, rootNode.childCount)
        return newNode
    }

    private fun removeSelectedMapping() {
        val node = mappingsTree.lastSelectedPathComponent as? DefaultMutableTreeNode ?: return
        if (node.userObject is MappingEntry) {
            treeModel.removeNodeFromParent(node)
            val parent = node.parent as DefaultMutableTreeNode
            if (parent != rootNode && parent.childCount == 0) {
                treeModel.removeNodeFromParent(parent)
            }
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
                val importedMappings = json.decodeFromString<List<MappingEntry>>(jsonContent)
                setMappings(importedMappings)
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
                val jsonContent = json.encodeToString(getMappings())
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
        val settings = PrettifySettings.getInstance()
        setMappings(settings.getDefaultMappings())
    }

    fun getPanel(): JPanel = mainPanel

    fun getMappings(): List<MappingEntry> {
        val mappings = mutableListOf<MappingEntry>()
        for (i in 0 until rootNode.childCount) {
            val categoryNode = rootNode.getChildAt(i) as DefaultMutableTreeNode
            for (j in 0 until categoryNode.childCount) {
                val mappingNode = categoryNode.getChildAt(j) as DefaultMutableTreeNode
                mappings.add(mappingNode.userObject as MappingEntry)
            }
        }
        return mappings
    }

    fun setMappings(mappings: List<MappingEntry>) {
        rootNode.removeAllChildren()
        treeModel.reload()
        mappings.forEach { addMappingToTree(it) }
        TreeUtil.expandAll(mappingsTree)
    }
}

private class MappingTreeCellRenderer : DefaultTreeCellRenderer() {
    override fun getTreeCellRendererComponent(
        tree: JTree,
        value: Any?,
        sel: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean
    ): Component {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus)

        val node = value as DefaultMutableTreeNode
        when (val userObject = node.userObject) {
            is MappingEntry -> {
                text = "${userObject.from} → ${userObject.to}"
                icon = AllIcons.Nodes.Property
            }

            is String -> {
                text = userObject
                icon = if (expanded) AllIcons.Nodes.Folder else AllIcons.Nodes.Folder
            }
        }
        return this
    }
}

private class AddMappingDialog(parent: Component, private val existingMapping: MappingEntry? = null) :
    DialogWrapper(parent, true) {
    private val fromField = JTextField()
    private val toField = JTextField()
    private val categoryField = JTextField()

    init {
        title = if (existingMapping == null) "Add Mapping" else "Edit Mapping"
        existingMapping?.let {
            fromField.text = it.from
            toField.text = it.to
            categoryField.text = it.category
        }
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = Insets(5, 5, 5, 5)

        gbc.gridx = 0
        gbc.gridy = 0
        panel.add(JLabel("From:"), gbc)
        gbc.gridx = 1
        panel.add(fromField, gbc)

        gbc.gridx = 0
        gbc.gridy = 1
        panel.add(JLabel("To:"), gbc)
        gbc.gridx = 1
        panel.add(toField, gbc)

        gbc.gridx = 0
        gbc.gridy = 2
        panel.add(JLabel("Category:"), gbc)
        gbc.gridx = 1
        panel.add(categoryField, gbc)

        return panel
    }

    fun getMapping(): MappingEntry = MappingEntry(
        from = fromField.text,
        to = toField.text,
        category = categoryField.text
    )
}
