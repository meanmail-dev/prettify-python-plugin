package ru.meanmail.prettifypython

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilder
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.impl.source.tree.LeafPsiElement
import java.util.regex.Pattern


class PrettifyFoldingBuilder : FoldingBuilder {
    
    private val stringLiteralPattern = Pattern.compile(
            "\".*?\""
    )
    
    private val prettySymbolMaps = hashMapOf(
            ">=" to "≥",
            "<=" to "≤",
            "!=" to "≠",
            "->" to "➔",
            "lambda" to "λ",
            " / " to " ÷ ",
            " * " to " × ",
            "**" to "^",
            "math.sqrt" to "√",

            "alpha" to "α",
            "beta" to "β",
            "gamma" to "γ",
            "delta" to "δ",
            "epsilon" to "ϵ",
            "zeta" to "ζ",
            "eta" to "η",
            "theta" to "θ",
            "iota" to "ι",
            "kappa" to "κ",
            // lambda
            "mu" to "μ",
            "nu" to "ν",
            "xi" to "ξ",
            "omicrion" to "ο",
            "pi" to "π",
            "rho" to "ρ",
            "sigma" to "σ",
            "tau" to "τ",
            "upsilon" to "υ",
            "phi" to "φ",
            "chi" to "χ",
            "psi" to "ψ",
            "omega" to "ω",

            "capalpha" to "Α",
            "capbeta" to "Β",
            "capgamma" to "Γ",
            "capdelta" to "Δ",
            "capepsilon" to "Ε",
            "capzeta" to "Ζ",
            "capeta" to "H",
            "captheta" to "Θ",
            "capiota" to "Ι",
            "capkappa" to "Κ",
            "caplambda" to "Λ",
            "capmu" to "Μ",
            "capnu" to "N",
            "capxi" to "Ξ",
            "capomicrion" to "O",
            "cappi" to "Π",
            "caprho" to "P",
            "capsigma" to "Σ",
            "captau" to "Τ",
            "capupsilon" to "Υ",
            "capphi" to "Φ",
            "capchi" to "Χ",
            "cappsi" to "Ψ",
            "capomega" to "Ω"

    )
    
    private val symbolPattern = Pattern.compile(
            getPatter()
    )
    
    private fun getPatter(): String {
        return prettySymbolMaps.keys.joinToString(separator = "|")
    }
    
    private val isSymbolInStringLiteral = { text: String, rangeStart: Int, rangeEnd: Int ->
        val matcher = stringLiteralPattern.matcher(text.replace("\n", " "))
        var isInStringLiteral = false
        while (matcher.find()) {
            isInStringLiteral = matcher.start() <= rangeStart && rangeEnd <= matcher.end()
            if (isInStringLiteral) break
        }
        isInStringLiteral
    }
    
    private fun getDescriptors(node: ASTNode): List<FoldingDescriptor> {
        val descriptors = mutableListOf<FoldingDescriptor>()
        
        if (node is LeafPsiElement) {
            val text = node.text
            val matcher = symbolPattern.matcher(text)
            while (matcher.find()) {
                val nodeRange = node.textRange
                val rangeStart = nodeRange.startOffset
                val rangeEnd = nodeRange.endOffset
                
                if (!(isSymbolInStringLiteral(text, rangeStart, rangeEnd))) {
                    val pretty = prettySymbolMaps[text] ?: return listOf()
                    val range = TextRange.create(rangeStart, rangeEnd)
                    descriptors.add(PrettifyFoldingDescriptor(node, range, null,
                            pretty, true))
                }
            }
        } else for (child in node.getChildren(null)) {
            descriptors.addAll(getDescriptors(child))
        }
        
        return descriptors
    }
    
    override fun buildFoldRegions(node: ASTNode, document: Document): Array<out FoldingDescriptor> {
        return getDescriptors(node).toTypedArray()
    }
    
    override fun getPlaceholderText(node: ASTNode) = null
    
    override fun isCollapsedByDefault(node: ASTNode) = true
}
