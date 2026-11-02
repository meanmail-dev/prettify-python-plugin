package dev.meanmail.prettifypython.settings

import com.intellij.testFramework.ApplicationRule
import com.intellij.testFramework.EdtTestUtil
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import javax.swing.tree.DefaultMutableTreeNode

class PrettifySettingsComponentTest {
    @get:Rule
    val application = ApplicationRule()

    @Test
    fun editingCategoryDoesNotCastItsNameToMappingEntry() {
        assertNonMappingEditIsIgnored("Mathematical operators")
    }

    @Test
    fun editingRootDoesNotOpenMappingDialog() {
        assertNonMappingEditIsIgnored("Mappings")
    }

    private fun assertNonMappingEditIsIgnored(label: String) {
        EdtTestUtil.runInEdtAndWait<Exception> {
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
}
