package ru.meanmail.prettifypython

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilder
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.jetbrains.python.psi.PyStringElement
import java.util.regex.Pattern


class PrettifyFoldingBuilder : FoldingBuilder {

    private val prettySymbolMaps = hashMapOf(
            ">=" to "≥",
            "<=" to "≤",
            "!=" to "≠",
            "->" to "➔",
            "lambda" to "λ"
    )

    private val symbolPattern = Pattern.compile(getPattern())

    private fun getPattern(): String {
        return prettySymbolMaps.keys.joinToString(separator = "|")
    }

    private fun getDescriptorsForChildren(node: ASTNode): List<FoldingDescriptor> {
        val descriptors = mutableListOf<FoldingDescriptor>()

        for (child in node.getChildren(null)) {
            descriptors.addAll(getDescriptors(child))
        }

        return descriptors
    }

    private fun getDescriptors(node: ASTNode): List<FoldingDescriptor> {
        if (node !is LeafPsiElement) {
            return getDescriptorsForChildren(node)
        }

        if (node.psi is PyStringElement) {
            return emptyList()
        }

        val descriptors = mutableListOf<FoldingDescriptor>()
        val text = node.text
        val matcher = symbolPattern.matcher(text)

        if (matcher.find()) {
            val nodeRange = node.textRange
            val rangeStart = nodeRange.startOffset
            val rangeEnd = nodeRange.endOffset
            val pretty = prettySymbolMaps[text] ?: return listOf()
            val range = TextRange.create(rangeStart, rangeEnd)
            descriptors.add(PrettifyFoldingDescriptor(node, range, null,
                    pretty, true))
        }

        return descriptors
    }

    override fun buildFoldRegions(node: ASTNode, document: Document): Array<out FoldingDescriptor> {
        return getDescriptors(node).toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode): String? = null

    override fun isCollapsedByDefault(node: ASTNode) = true
}
