// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.idea.k2.refactoring.bindToElement

import com.intellij.psi.PsiFile
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.analysis.api.KaAllowAnalysisOnEdt
import org.jetbrains.kotlin.analysis.api.lifetime.allowAnalysisOnEdt
import org.jetbrains.kotlin.idea.test.Directives
import org.jetbrains.kotlin.idea.test.KotlinMultiFileLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.ProjectDescriptorWithStdlibSources
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtSimpleNameExpression

abstract class AbstractK2BindToTest : KotlinMultiFileLightCodeInsightFixtureTestCase() {
    override fun isFirPlugin() = true

    override fun getProjectDescriptor() = ProjectDescriptorWithStdlibSources.getInstanceWithStdlibSources()

    @OptIn(KaAllowAnalysisOnEdt::class)
    override fun doMultiFileTest(files: List<PsiFile>, globalDirectives: Directives) = allowAnalysisOnEdt {
        val mainFile = files.first()
        myFixture.configureFromExistingVirtualFile(mainFile.virtualFile)
        val elem = mainFile.findElementAt(myFixture.caretOffset) ?: error("Couldn't find element at caret")
        val refElement = elem.parentOfType<KtSimpleNameExpression>(withSelf = true)
            ?: elem.parentOfType<KDocName>()
            ?: error("Element at caret isn't of type 'KtSimpleNameExpression'")
        bindElement(refElement)
    }

     abstract fun bindElement(refElement: KtElement)

    protected companion object {
        const val BIND_TO = "BIND_TO"
    }
}