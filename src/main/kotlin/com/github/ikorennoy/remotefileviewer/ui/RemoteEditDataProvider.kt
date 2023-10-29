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
import com.intellij.util.PathUtil

class RemoteEditDataProvider(private val project: Project, private val tree: Tree, private val fs: RemoteFileSystem) : DataProvider {

    override fun getData(dataId: String): Any? {
        if (!CommonDataKeys.NAVIGATABLE_ARRAY.`is`(dataId)) {
            if (CommonDataKeys.NAVIGATABLE.`is`(dataId)) {
                //OpenFileDescriptor()
                val path = tree.selectionModel.selectionPath ?: return null
                val fsPath = "${path.path.first()}${path.path.sliceArray(1.. path.path.size - 1).joinToString("/")}"
                return OpenFileDescriptor(project, fs.findFileByPath(fsPath))
            }
        } else if (CommonDataKeys.NAVIGATABLE_ARRAY.`is`(dataId)) {
            val path = tree.selectionModel.selectionPath ?: return null
            val fsPath = "${path.path.first()}${path.path.sliceArray(1.. path.path.size - 1).joinToString("/")}"
            return arrayOf(OpenFileDescriptor(project, fs.findFileByPath(fsPath)))
        }
        return null
    }

}