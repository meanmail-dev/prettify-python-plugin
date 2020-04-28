package ru.meanmail.prettifypython

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilder
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.jetbrains.python.psi.PyBinaryExpression
import com.jetbrains.python.psi.PyStringElement
import kotlinx.serialization.json.Json
import kotlinx.serialization.*
import kotlinx.serialization.builtins.list
import java.io.File


class PrettifyFoldingBuilder : FoldingBuilder {

    companion object {

        private fun initializePrettySymbolMaps(): HashMap<String, (PsiElement) -> String?>{

             fun initializeFile(path: String,
                                conditions: HashMap<ConversionCondition, (node: PsiElement) -> (Boolean)>,
                                prettySymbolMaps: HashMap<String, (PsiElement) -> String?>,
                                conversionListSerializer: KSerializer<List<Conversion>>) {

                val conversions = Json.parse(conversionListSerializer, File(path).readText())
                for (conversion in conversions) {
                    if (conversion.cond == ConversionCondition.NONE) {
                        prettySymbolMaps[conversion.source] = { _: PsiElement -> conversion.dest }
                    } else {
                        val condition = conditions[conversion.cond]
                        prettySymbolMaps[conversion.source] =
                                { node: PsiElement -> if ((condition!!)(node.parent)) conversion.dest else null }
                    }
                }
            }

            val prettySymbolMaps = hashMapOf<String, (PsiElement) -> String?>()

            val prefix = "src/main/kotlin/ru/meanmail/prettifypython/"
            val BasicConversions = "${prefix}BasicConversions.json"
            val GreekLetters = "${prefix}GreekLetters.json"

            val isPyBinExp = { node: PsiElement -> node.parent is PyBinaryExpression }
            val conditions = hashMapOf<ConversionCondition, (node: PsiElement) -> (Boolean)>(
                    ConversionCondition.PY_BINARY_EXP to isPyBinExp
            )

            val conversionListSerializer: KSerializer<List<Conversion>> = Conversion.serializer().list

            initializeFile(BasicConversions, conditions, prettySymbolMaps, conversionListSerializer)
            initializeFile(GreekLetters, conditions, prettySymbolMaps, conversionListSerializer)
            return prettySymbolMaps
        }

        val prettySymbolMaps = initializePrettySymbolMaps()
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

        val replacerCall = prettySymbolMaps.getOrDefault(text, null)
                ?: return emptyList()

        val replacer = replacerCall(node) ?: return emptyList()
        val nodeRange = node.textRange
        val range = TextRange.create(nodeRange.startOffset,
                nodeRange.endOffset)
        descriptors.add(PrettifyFoldingDescriptor(node, range, null,
                replacer, true))

        return descriptors
    }

    override fun buildFoldRegions(node: ASTNode, document: Document): Array<out FoldingDescriptor> {
        return getDescriptors(node).toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode): String? = null

    override fun isCollapsedByDefault(node: ASTNode) = true
}
