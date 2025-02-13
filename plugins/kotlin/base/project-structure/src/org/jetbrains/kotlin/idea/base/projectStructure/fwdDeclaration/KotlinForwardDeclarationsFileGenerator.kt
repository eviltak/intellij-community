// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.kotlin.idea.base.projectStructure.fwdDeclaration

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VfsUtil
import org.jetbrains.kotlin.idea.base.projectStructure.moduleInfo.NativeKlibLibraryInfo
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.NativeForwardDeclarationKind
import org.jetbrains.kotlin.name.NativeStandardInteropNames.ExperimentalForeignApi
import org.jetbrains.kotlin.name.NativeStandardInteropNames.cInteropPackage
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString

private val LOG = Logger.getInstance(KotlinForwardDeclarationsFileGenerator::class.java)

/**
 * Generator of synthetic K/N forward declaration files.
 *
 * These files provide the necessary source for otherwise bodiless declarations, thus enabling the usual IDE functionality.
 * The files are not a compilation input, but a mere visualization of K/N forward declaration symbols.
 * It is important to note that since the generated declarations are not the real origin of the symbols,
 * there is currently no mechanism for ensuring the text version represents all symbols details correctly.
 *
 * The generated declarations are grouped into files by package.
 * The exact form of the declaration depends on the package, see [org.jetbrains.kotlin.name.NativeForwardDeclarationKind].
 *
 * @see
 * [KotlinForwardDeclarationsWorkspaceEntity] workspace model entity for storing the information about the extra roots
 * [KotlinForwardDeclarationsModelChangeService] service responsible for launching the generation
 */
object KotlinForwardDeclarationsFileGenerator {
    fun generateForwardDeclarationFiles(libraryInfo: NativeKlibLibraryInfo): Path? {
        if (!Registry.`is`("kotlin.k2.kmp.enabled")) return null

        val groupedClasses = KotlinForwardDeclarationsFqNameExtractor.getGroupedFwdDeclarations(libraryInfo).ifEmpty { return null }
        return generateForwardDeclarationsForFqNames(groupedClasses, libraryInfo.libraryRoot)
    }

    private fun generateForwardDeclarationsForFqNames(groupedFqNames: Map<FqName, List<FqName>>, libraryPath: String): Path {
        val root = KotlinForwardDeclarationsFileSystem.storageRootPath
        val subPath = libraryPath.let { if (it.startsWith("/")) it.drop(1) else it }
        val libraryLocation = root.resolve(subPath)

        groupedFqNames.mapNotNull { (pkg, classes) ->
            val kind = NativeForwardDeclarationKind.packageFqNameToKind[pkg] ?: run {
                LOG.warn("Skipping request to generate K/N forward declarations with an unsupported package $pkg")
                return@mapNotNull null
            }

            generateFile(libraryLocation, pkg, kind, classes)
        }
        val virtualRootFile = VfsUtil.findFile(root, true)
        virtualRootFile?.let {
            VfsUtil.markDirty(true, true, it)
        }
        return libraryLocation
    }

    private fun generateFile(parentDir: Path, pkg: FqName, kind: NativeForwardDeclarationKind, classes: List<FqName>): File {
        val text = createText(pkg, kind, classes)
        return with(parentDir.resolve("${pkg.asString()}.kt").toFile()) {
            parentFile.mkdirs()
            if (exists()) {
                delete()
            }

            createNewFile()
            writeText(text)
            this
        }
    }

    private fun createText(pkg: FqName, kind: NativeForwardDeclarationKind, classes: List<FqName>): String {
        val classKindCodeRepresentation = kind.classKind.codeRepresentation ?: "".also {
            LOG.error("Error generating forward declarations with kind $kind for package $pkg")
        }
        val constructor = when (kind) {
            NativeForwardDeclarationKind.Struct -> "private constructor(rawPtr: $cInteropPackage.NativePtr) "
            NativeForwardDeclarationKind.ObjCClass,
            NativeForwardDeclarationKind.ObjCProtocol -> ""

            else -> ""
        }
        val supertype = when (kind) {
            NativeForwardDeclarationKind.Struct -> "${kind.superClassFqName.asString()}(rawPtr)"
            NativeForwardDeclarationKind.ObjCClass,
            NativeForwardDeclarationKind.ObjCProtocol -> kind.superClassFqName.asString()
        }

        val annotation = "@$cInteropPackage.$ExperimentalForeignApi"
        val classNames = classes.map { it.pathSegments().last().asString() }
        val packageWithImports = """
            package ${pkg.asString()}${System.lineSeparator()}
            
        """.trimIndent()
        val generatedDeclarations = classNames.joinToString(separator = System.lineSeparator()) { className ->
            "$annotation $classKindCodeRepresentation $className $constructor: ${supertype}"
        }
        return packageWithImports + generatedDeclarations
    }

    fun cleanUp(roots: List<File>) {
        if (!Registry.`is`("kotlin.k2.kmp.enabled")) return

        val storageRoot = KotlinForwardDeclarationsFileSystem.storageRootPath

        for (root in roots) {
            if (!root.absolutePath.startsWith(storageRoot.absolutePathString())) {
                LOG.error("Attempt to delete ${root.absolutePath} which is not under ${storageRoot.absolutePathString()}")
                continue
            }

            root.deleteRecursively()
        }
    }
}
