package com.github.ikorennoy.remotefileviewer.ui

import com.github.ikorennoy.remotefileviewer.filesystem.RemoteFileSystem
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.Navigatable
import com.intellij.remote.RemoteFile
import com.intellij.ui.tree.StructureTreeModel
import com.intellij.ui.treeStructure.Tree

class RemoteEditDataProvider(private val project: Project, private val tree: Tree, private val fs: RemoteFileSystem) : DataProvider {

    override fun getData(dataId: String): Any? {
        if (!CommonDataKeys.NAVIGATABLE_ARRAY.`is`(dataId)) {
            if (CommonDataKeys.NAVIGATABLE.`is`(dataId)) {
                //OpenFileDescriptor()
                val path = tree.selectionModel.selectionPath ?: return null

                return OpenFileDescriptor(project, fs.findFileByPath(path.path.joinToString("/")))
            }
        } else if (CommonDataKeys.NAVIGATABLE_ARRAY.`is`(dataId)) {
            val path = tree.selectionModel.selectionPath ?: return null
            val pathOnFs = path.path.joinToString("/")
            return arrayOf(OpenFileDescriptor(project, fs.findFileByPath(pathOnFs)))
        }
        return null
    }
}