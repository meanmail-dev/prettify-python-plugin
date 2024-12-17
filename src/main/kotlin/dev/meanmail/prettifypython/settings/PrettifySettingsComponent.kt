package dev.meanmail.prettifypython.settings

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbarPosition
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.tree.TreeUtil
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter
import javax.swing.tree.*

class PrettifySettingsComponent {
    private val mainPanel: JPanel
    private val mappingsTree: Tree
    private val treeModel: DefaultTreeModel
    private val rootNode = DefaultMutableTreeNode("Mappings")
    private val json = Json { prettyPrint = true }
    private var resetButton: AnAction? = null

    init {
        treeModel = DefaultTreeModel(rootNode)
        mappingsTree = Tree(treeModel).apply {
            isRootVisible = false
            showsRootHandles = true
            selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
            cellRenderer = MappingTreeCellRenderer()
            dragEnabled = true
            dropMode = DropMode.ON_OR_INSERT
            transferHandler = MappingTransferHandler()

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

                override fun update(e: AnActionEvent) {
                    e.presentation.isEnabled = !isDefaultMappings()
                }
            }.also { resetButton = it })

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
                // If the category hasn't changed, just update the node
                node.userObject = updatedMapping
                treeModel.nodeChanged(node)
            } else {
                // If the category has changed, move to the new category
                treeModel.removeNodeFromParent(node)
                if (parent != rootNode && parent.childCount == 0) {
                    treeModel.removeNodeFromParent(parent)
                }
                addMappingToTree(updatedMapping)
            }
            expandAllNodes()
        }
    }

    private fun addMapping() {
        val dialog = AddMappingDialog(mainPanel)
        dialog.show()
        if (dialog.isOK) {
            val mapping = dialog.getMapping()
            addMappingToTree(mapping)
            expandAllNodes()
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

    private inner class MappingTransferHandler : TransferHandler() {
        override fun getSourceActions(c: JComponent): Int = TransferHandler.MOVE

        override fun canImport(support: TransferSupport): Boolean {
            if (!support.isDrop) return false
            if (!support.isDataFlavorSupported(DataFlavor.stringFlavor)) return false

            val dropLocation = support.dropLocation as? JTree.DropLocation ?: return false
            val targetPath = dropLocation.path ?: return false  // If targetPath is null, cancel
            val targetNode = targetPath.lastPathComponent as? DefaultMutableTreeNode ?: return false

            // Don't allow dropping on root node
            if (targetNode == rootNode) return false

            // Allow drop only on categories or between mappings
            return when (targetNode.userObject) {
                is String -> true // Category
                is MappingEntry -> true // Between mappings
                else -> false
            }
        }

        override fun createTransferable(component: JComponent): Transferable? {
            val tree = component as? JTree ?: return null
            val node = tree.selectionPath?.lastPathComponent as? DefaultMutableTreeNode ?: return null
            if (node.userObject !is MappingEntry) return null

            return object : Transferable {
                override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(DataFlavor.stringFlavor)
                override fun isDataFlavorSupported(flavor: DataFlavor): Boolean = flavor == DataFlavor.stringFlavor
                override fun getTransferData(flavor: DataFlavor): Any = ""
            }
        }

        override fun importData(support: TransferSupport): Boolean {
            if (!canImport(support)) return false

            val dropLocation = support.dropLocation as JTree.DropLocation
            val targetPath = dropLocation.path ?: return false  // If targetPath is null, cancel
            val targetNode = targetPath.lastPathComponent as DefaultMutableTreeNode

            val draggedNode = mappingsTree.selectionPath?.lastPathComponent as? DefaultMutableTreeNode ?: return false
            val draggedMapping = draggedNode.userObject as? MappingEntry ?: return false

            // Determine target category
            val targetCategory = when (val targetObject = targetNode.userObject) {
                is String -> targetObject // Drop on category
                is MappingEntry -> (targetNode.parent as DefaultMutableTreeNode).userObject as String // Drop between mappings
                else -> return false
            }

            // If category hasn't changed and it's not an insertion between elements, cancel
            if (targetCategory == (draggedNode.parent as DefaultMutableTreeNode).userObject &&
                targetNode.userObject is String && dropLocation.childIndex < 0
            ) {
                return false
            }

            // Create new mapping with updated category
            val newMapping = draggedMapping.copy(category = targetCategory)

            // Remove old node
            treeModel.removeNodeFromParent(draggedNode)
            val oldParent = draggedNode.parent as? DefaultMutableTreeNode
            if (oldParent != null && oldParent != rootNode && oldParent.childCount == 0) {
                treeModel.removeNodeFromParent(oldParent)
            }

            // Add new node
            val newNode = DefaultMutableTreeNode(newMapping)
            val categoryNode = findOrCreateCategoryNode(targetCategory)

            // Calculate insert position
            val insertIndex = when {
                // If dropping on a mapping
                targetNode.userObject is MappingEntry -> {
                    val targetIndex = categoryNode.getIndex(targetNode)
                    // If dropping after the target node
                    if (dropLocation.childIndex > targetIndex) targetIndex + 1 else targetIndex
                }
                // If dropping on a category with specific index
                dropLocation.childIndex >= 0 -> dropLocation.childIndex
                // If dropping at the end of category
                else -> categoryNode.childCount
            }

            treeModel.insertNodeInto(newNode, categoryNode, insertIndex)
            val path = TreePath(categoryNode.path)
            val row = mappingsTree.getRowForPath(path)
            if (row >= 0) {
                TreeUtil.expand(mappingsTree, row)
            }

            return true
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

    private fun expandAllNodes() {
        var row = 0
        while (row < mappingsTree.rowCount) {
            mappingsTree.expandRow(row)
            row++
        }
    }

    fun setMappings(mappings: List<MappingEntry>) {
        rootNode.removeAllChildren()
        mappings.forEach { addMappingToTree(it) }
        treeModel.reload()
        expandAllNodes()
        resetButton?.let { action ->
            val presentation = action.templatePresentation.clone()
            action.update(
                AnActionEvent(
                    null,
                    SimpleDataContext.EMPTY_CONTEXT,
                    "PrettifySettingsComponent",
                    presentation,
                    ActionManager.getInstance(),
                    0
                )
            )
        }
    }

    private fun isDefaultMappings(): Boolean {
        val currentMappings = getMappings().sortedBy { it.from }
        val defaultMappings = PrettifySettings().getDefaultMappings().sortedBy { it.from }

        if (currentMappings.size != defaultMappings.size) return false

        return currentMappings.zip(defaultMappings) { current, default ->
            current.from == default.from &&
                    current.to == default.to &&
                    current.category == default.category
        }.all { it }
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
