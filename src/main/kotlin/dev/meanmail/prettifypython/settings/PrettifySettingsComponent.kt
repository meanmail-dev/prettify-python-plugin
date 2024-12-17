package dev.meanmail.prettifypython.settings

import com.intellij.icons.AllIcons
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.treeStructure.Tree
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter
import javax.swing.tree.*

class PrettifySettingsComponent {
    private val rootNode = DefaultMutableTreeNode("Mappings")
    private val treeModel = MappingTreeModel(rootNode)
    private val mappingsTree = Tree(treeModel).apply {
        isRootVisible = false
        showsRootHandles = true
        selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        cellRenderer = MappingTreeCellRenderer()
        transferHandler = MappingTransferHandler(this)
        dragEnabled = true
        dropMode = DropMode.INSERT

        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    val node = lastSelectedPathComponent as? DefaultMutableTreeNode ?: return
                    editMapping(node)
                }
            }
        })
    }
    private val json = Json { prettyPrint = true }
    private var resetButton: AnAction? = null
    private val mainPanel = JPanel(BorderLayout()).apply {
        val toolbarDecorator = ToolbarDecorator.createDecorator(mappingsTree)
            .setAddAction { addNewMapping() }
            .setRemoveAction { removeSelectedMapping() }
            .setEditAction { editMapping(mappingsTree.lastSelectedPathComponent as DefaultMutableTreeNode) }
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

        add(toolbarDecorator.createPanel(), BorderLayout.CENTER)
        border = IdeBorderFactory.createTitledBorder("Symbol Mappings", false)
    }

    private inner class MappingTreeModel(root: TreeNode) : DefaultTreeModel(root) {
        override fun isLeaf(node: Any?): Boolean {
            if (node !is DefaultMutableTreeNode) return true
            return node.userObject is MappingEntry
        }
    }

    private fun editMapping(node: DefaultMutableTreeNode) {
        val mapping = node.userObject as MappingEntry
        val existingCategories = getAllCategories()
        val dialog = MappingDialog(mainPanel, "Edit Mapping", mapping, existingCategories)
        if (dialog.showAndGet()) {
            val updatedMapping = dialog.getMapping()
            if (node.parent != null) {
                val parent = node.parent as DefaultMutableTreeNode
                parent.remove(node)
                addMappingToTree(updatedMapping)
                treeModel.reload()
                expandAllNodes()
            }
        }
    }

    private fun addNewMapping() {
        val existingCategories = getAllCategories()
        val dialog = MappingDialog(mainPanel, "Add New Mapping", existingCategories = existingCategories)
        if (dialog.showAndGet()) {
            val mapping = dialog.getMapping()
            addMappingToTree(mapping)
            treeModel.reload()
            expandAllNodes()
        }
    }

    private fun addMappingToTree(mapping: MappingEntry) {
        // Find or create category node
        val categoryNode = findOrCreateCategoryNode(mapping.category)
        val mappingNode = DefaultMutableTreeNode(mapping)
        treeModel.insertNodeInto(mappingNode, categoryNode, categoryNode.childCount)
        expandAllNodes()
    }

    private fun findOrCreateCategoryNode(category: String): DefaultMutableTreeNode {
        // Search for existing category node
        for (i in 0 until rootNode.childCount) {
            val node = rootNode.getChildAt(i) as DefaultMutableTreeNode
            if (node.userObject.toString() == category) {
                return node
            }
        }

        // Create new category node if not found
        val categoryNode = DefaultMutableTreeNode(category)
        treeModel.insertNodeInto(categoryNode, rootNode, rootNode.childCount)
        return categoryNode
    }

    private fun removeSelectedMapping() {
        val selectedNode = mappingsTree.selectionPath?.lastPathComponent as? DefaultMutableTreeNode ?: return
        if (selectedNode.userObject !is MappingEntry) return

        treeModel.removeNodeFromParent(selectedNode)
    }

    private class MappingTransferable(private val mapping: MappingEntry) : Transferable {
        companion object {
            val mappingFlavor = DataFlavor(MappingEntry::class.java, "Mapping Entry")
        }

        override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(mappingFlavor)
        override fun isDataFlavorSupported(flavor: DataFlavor): Boolean = flavor == mappingFlavor
        override fun getTransferData(flavor: DataFlavor): Any {
            if (!isDataFlavorSupported(flavor)) throw UnsupportedFlavorException(flavor)
            return mapping
        }
    }

    private inner class MappingTransferHandler(private val tree: JTree) : TransferHandler() {
        override fun getSourceActions(c: JComponent): Int = TransferHandler.MOVE

        override fun createTransferable(component: JComponent): Transferable? {
            val node = tree.selectionPath?.lastPathComponent as? DefaultMutableTreeNode ?: return null
            if (node.userObject !is MappingEntry) return null

            return MappingTransferable(node.userObject as MappingEntry)
        }

        override fun canImport(support: TransferSupport): Boolean {
            if (!support.isDrop) return false
            support.setShowDropLocation(true)
            if (!support.isDataFlavorSupported(MappingTransferable.mappingFlavor)) return false

            val dropLocation = support.getDropLocation() as? JTree.DropLocation ?: return false
            val targetPath = dropLocation.path ?: return false
            val targetNode = targetPath.lastPathComponent as DefaultMutableTreeNode

            // Allow dropping only on category nodes (not root)
            return targetNode.parent == rootNode
        }

        override fun importData(support: TransferSupport): Boolean {
            if (!canImport(support)) return false

            val dropLocation = support.getDropLocation() as? JTree.DropLocation ?: return false
            val targetPath = dropLocation.path ?: return false
            val targetNode = targetPath.lastPathComponent as DefaultMutableTreeNode
            val childIndex = dropLocation.childIndex

            val draggedNode = tree.selectionPath?.lastPathComponent as? DefaultMutableTreeNode ?: return false
            val draggedMapping = draggedNode.userObject as? MappingEntry ?: return false

            // Target category is always the category we're dropping on
            val targetCategory = targetNode.userObject.toString()

            // Create new mapping with target category
            val newMapping = draggedMapping.copy(category = targetCategory)

            // Calculate insert index
            val insertIndex = if (childIndex >= 0) childIndex else targetNode.childCount

            // Create and insert the new node
            val newMappingNode = DefaultMutableTreeNode(newMapping)

            // First insert the new node
            treeModel.insertNodeInto(newMappingNode, targetNode, insertIndex)

            // Then remove the old node
            treeModel.removeNodeFromParent(draggedNode)

            // Make sure the new node is visible
            val newPath = TreePath(newMappingNode.path)
            tree.scrollPathToVisible(newPath)
            tree.selectionPath = newPath

            expandAllNodes()
            return true
        }
    }

    private fun importMappings() {
        val fileChooser = JFileChooser().apply {
            dialogTitle = "Import Mappings"
            fileSelectionMode = JFileChooser.FILES_ONLY
            fileFilter = FileNameExtensionFilter("JSON files", "json")
        }

        if (fileChooser.showOpenDialog(mainPanel) == JFileChooser.APPROVE_OPTION) {
            try {
                val jsonString = fileChooser.selectedFile.readText()
                val mappingsData = json.decodeFromString<MappingsData>(jsonString)
                setMappings(mappingsData.mappings)
                Messages.showInfoMessage(
                    mainPanel,
                    "Mappings imported successfully",
                    "Import Successful"
                )
            } catch (e: Exception) {
                Messages.showErrorDialog(
                    mainPanel,
                    "Failed to import mappings: ${e.message}",
                    "Import Failed"
                )
            }
        }
    }

    private fun exportMappings() {
        val fileChooser = JFileChooser().apply {
            dialogTitle = "Export Mappings"
            fileSelectionMode = JFileChooser.FILES_ONLY
            fileFilter = FileNameExtensionFilter("JSON files", "json")
            val now = LocalDateTime.now(ZoneOffset.UTC)
            val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            selectedFile = File("prettify_python_mappings_${now.format(formatter)}.json")
        }

        if (fileChooser.showSaveDialog(mainPanel) == JFileChooser.APPROVE_OPTION) {
            try {
                val mappings = getMappings()
                val pluginId = PluginId.findId("ru.meanmail.plugins.prettify-python")
                    ?: throw IllegalStateException("Plugin ID not found")
                val plugin = PluginManagerCore.getPlugin(pluginId)
                    ?: throw IllegalStateException("Plugin not found")

                val now = LocalDateTime.now(ZoneOffset.UTC)
                val mappingsData = MappingsData(
                    mappings = mappings,
                    ideVersion = ApplicationInfo.getInstance().fullVersion,
                    pluginVersion = plugin.version,
                    mappingsCount = mappings.size,
                    exportDate = now.toString()
                )
                val jsonString = json.encodeToString(mappingsData)
                fileChooser.selectedFile.writeText(jsonString)
                Messages.showInfoMessage(
                    mainPanel,
                    "Mappings exported successfully",
                    "Export Successful"
                )
            } catch (e: Exception) {
                Messages.showErrorDialog(
                    mainPanel,
                    "Failed to export mappings: ${e.message}",
                    "Export Failed"
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
        for (i in 0 until rootNode.childCount) {
            val path = TreePath(arrayOf(rootNode, rootNode.getChildAt(i)))
            mappingsTree.expandPath(path)
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
                    SimpleDataContext.EMPTY_CONTEXT,
                    presentation,
                    "PrettifySettingsComponent",
                    ActionUiKind.NONE,
                    null,
                    0,
                    ActionManager.getInstance()
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

    private fun getAllCategories(): List<String> {
        val categories = mutableListOf<String>()
        for (i in 0 until rootNode.childCount) {
            val categoryNode = rootNode.getChildAt(i) as DefaultMutableTreeNode
            categories.add(categoryNode.userObject.toString())
        }
        return categories
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

private class MappingDialog(
    parent: Component,
    title: String,
    private val mapping: MappingEntry? = null,
    private val existingCategories: List<String>
) : DialogWrapper(parent, true) {
    private val categoryField: JComboBox<String>
    private val fromField: JTextField
    private val toField: JTextField
    private val panel: JPanel

    init {
        this.title = title

        categoryField = JComboBox(existingCategories.toTypedArray())
        categoryField.isEditable = true
        fromField = JTextField()
        toField = JTextField()

        mapping?.let {
            categoryField.selectedItem = it.category
            fromField.text = it.from
            toField.text = it.to
        }

        panel = JPanel(GridBagLayout())
        val constraints = GridBagConstraints()
        constraints.fill = GridBagConstraints.HORIZONTAL
        constraints.weightx = 1.0
        constraints.insets = Insets(5, 5, 5, 5)

        constraints.gridx = 0
        constraints.gridy = 0
        panel.add(JLabel("Category:"), constraints)
        constraints.gridx = 1
        panel.add(categoryField, constraints)

        constraints.gridx = 0
        constraints.gridy = 1
        panel.add(JLabel("From:"), constraints)
        constraints.gridx = 1
        panel.add(fromField, constraints)

        constraints.gridx = 0
        constraints.gridy = 2
        panel.add(JLabel("To:"), constraints)
        constraints.gridx = 1
        panel.add(toField, constraints)

        init()
    }

    override fun createCenterPanel(): JComponent = panel

    fun getMapping(): MappingEntry {
        val category = categoryField.selectedItem?.toString() ?: ""
        val from = fromField.text
        val to = toField.text
        return MappingEntry(
            category = category,
            from = from,
            to = to
        )
    }
}
