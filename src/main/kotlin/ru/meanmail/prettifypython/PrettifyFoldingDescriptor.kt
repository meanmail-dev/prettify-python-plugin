package ru.meanmail.prettifypython

import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.FoldingGroup
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

class PrettifyFoldingDescriptor(node: PsiElement,
                                range: TextRange,
                                group: FoldingGroup?,
                                private val name: String,
                                private val notExpandable: Boolean) :
        FoldingDescriptor(node.node, range, group) {
    
    override fun getPlaceholderText() = name
    
    override fun isNonExpandable() = notExpandable
}
