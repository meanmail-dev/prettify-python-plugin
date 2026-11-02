package dev.meanmail.prettifypython.settings

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import javax.swing.tree.DefaultMutableTreeNode

class PrettifySettingsComponentTest : BasePlatformTestCase() {
    fun testEditingCategoryDoesNotCastItsNameToMappingEntry() {
        assertNonMappingEditIsIgnored("Mathematical operators")
    }

    fun testEditingRootDoesNotOpenMappingDialog() {
        assertNonMappingEditIsIgnored("Mappings")
    }

    private fun assertNonMappingEditIsIgnored(label: String) {
        val component = PrettifySettingsComponent()
        component.setMappings(listOf(
            MappingEntry("**", "^", "Mathematical operators"),
            MappingEntry("!=", "≠", "Comparison operators"),
        ))
        val mappings = component.getMappings()
        val method = PrettifySettingsComponent::class.java
            .getDeclaredMethod("editMapping", DefaultMutableTreeNode::class.java)
        method.isAccessible = true
        method.invoke(component, DefaultMutableTreeNode(label))
        assertEquals("non-mapping nodes must leave every mapping intact", mappings, component.getMappings())
    }
}
