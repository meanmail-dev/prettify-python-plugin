package dev.meanmail.prettifypython.settings

import com.intellij.util.xmlb.XmlSerializer
import org.junit.Assert.assertEquals
import org.junit.Test

class PrettifySettingsPersistenceTest {
    @Test
    fun customMappingsSurviveXmlRoundTrip() {
        val expected = PrettifySettings.State(
            mappings = listOf(MappingEntry("**", "pow", "Custom operators")),
            categories = setOf("Custom operators"),
        )

        val restored = XmlSerializer.deserialize(
            XmlSerializer.serialize(expected), PrettifySettings.State::class.java,
        )

        assertEquals(expected.mappings, restored.mappings)
        assertEquals(expected.categories, restored.categories)
    }
}
