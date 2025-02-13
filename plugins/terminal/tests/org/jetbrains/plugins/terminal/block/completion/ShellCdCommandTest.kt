// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.terminal.block.completion

import com.intellij.terminal.completion.spec.ShellCommandResult
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.runBlocking
import org.jetbrains.plugins.terminal.block.util.ShellCompletionTestFixture
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
internal class ShellCdCommandTest : BasePlatformTestCase() {
  private val expectedDirectories = listOf("directory/", "settings/", ".hiddenDir/")
  private val allFiles = expectedDirectories + listOf("file.txt", ".hidden")

  @Test
  fun `suggest directories and hardcoded suggestions`() = runBlocking {
    val fixture = createFixture(allFiles, expectedPath = ".")
    val actual = fixture.getCompletions("cd ")
    assertSameElements(actual.map { it.name }, expectedDirectories + listOf("-", "~"))
  }

  @Test
  fun `suggest only directories if there is base path`() = runBlocking {
    val fixture = createFixture(allFiles, expectedPath = "src/")
    val actual = fixture.getCompletions("cd src/")
    assertSameElements(actual.map { it.name }, expectedDirectories)
  }

  /**
   * @param expectedPath path used for requesting the child files.
   * @param files files to return on [expectedPath] child files request.
   */
  private fun createFixture(files: List<String>, expectedPath: String): ShellCompletionTestFixture {
    return ShellCompletionTestFixture.builder(project)
      .mockShellCommandResults { command ->
        if (command == "__jetbrains_intellij_get_directory_files $expectedPath") {
          ShellCommandResult.create(files.joinToString("\n"), exitCode = 0)
        }
        else error("Unknown command: $command")
      }
      .build()
  }
}
