// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.java.psi.formatter.java

import com.intellij.JavaTestUtil
import com.intellij.application.options.CodeStyle
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.codeStyle.CommonCodeStyleSettings.WRAP_ALWAYS
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase

class TypeAnnotationFormatterTest : LightJavaCodeInsightFixtureTestCase() {
  private val commonSettings: CommonCodeStyleSettings
    get() = CodeStyle.getSettings(project).getCommonSettings(JavaLanguage.INSTANCE)

  override fun getTestDataPath() = "${JavaTestUtil.getJavaTestDataPath()}/psi/formatter/java/typeAnnotation/"

  override fun setUp() {
    super.setUp()
    commonSettings.METHOD_ANNOTATION_WRAP = WRAP_ALWAYS
    ModuleRootModificationUtil.updateModel(module, DefaultLightProjectDescriptor::addJetBrainsAnnotations)
  }

  fun testKnownAnnotationBeforeType() = doTest()

  fun testKnownAnnotationBeforeTypeParameterList() = doTest()

  fun testCustomAnnotation() = doTest()

  fun testManyKnownAnnotations() = doTest()

  fun testPreserveWrappingSingleAnnotation() = doTest()

  fun testPreserveWrappingManyAnnotations() = doTest()
  private fun doTest() {
    val testName = getTestName(false)
    myFixture.configureByFile("$testName.java")
    WriteCommandAction.runWriteCommandAction(project) { CodeStyleManager.getInstance(project).reformatText(file, 0, editor.document.textLength) }
    myFixture.checkResultByFile("${testName}_after.java")
  }
}