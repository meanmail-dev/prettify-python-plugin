package ru.meanmail.prettifypython

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilder
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.jetbrains.python.psi.PyBinaryExpression
import com.jetbrains.python.psi.PyStringElement


class PrettifyFoldingBuilder : FoldingBuilder {

    private val prettySymbolMaps = hashMapOf(
            ">=" to "≥",
            "<=" to "≤",
            "!=" to "≠",
            "->" to "➔",
            "lambda" to "λ",
            "**" to "^"
    )

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

        for (entity in prettySymbolMaps.entries) {
            if (text == "**" && node.parent !is PyBinaryExpression)
                continue

            if (entity.key == text) {
                val nodeRange = node.textRange
                val range = TextRange.create(nodeRange.startOffset,
                        nodeRange.endOffset)
                descriptors.add(PrettifyFoldingDescriptor(node, range, null,
                        entity.value, true))
            }
        }

        return descriptors
    }

    override fun buildFoldRegions(node: ASTNode, document: Document): Array<out FoldingDescriptor> {
        return getDescriptors(node).toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode): String? = null

    override fun isCollapsedByDefault(node: ASTNode) = true
}
