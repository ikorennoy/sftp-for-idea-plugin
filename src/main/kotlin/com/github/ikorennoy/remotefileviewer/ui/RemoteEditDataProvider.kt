package com.github.ikorennoy.remotefileviewer.ui

import com.github.ikorennoy.remotefileviewer.filesystem.SftpFileSystem
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.fileChooser.FileElement
import com.intellij.openapi.fileChooser.ex.FileNodeDescriptor
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.treeStructure.Tree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath

class RemoteEditDataProvider(private val project: Project, private val tree: Tree, private val fs: SftpFileSystem) :
    DataProvider {

    override fun getData(dataId: String): Any? {
        return if (CommonDataKeys.NAVIGATABLE_ARRAY.`is`(dataId)) {
            val treePath = tree.selectionPath ?: return null
            val virtualFile = getTargetPath(treePath) ?: return null
            arrayOf(OpenFileDescriptor(project, virtualFile))
        } else if (CommonDataKeys.NAVIGATABLE.`is`(dataId)) {
            val treePath = tree.selectionPath ?: return null
            val virtualFile = getTargetPath(treePath) ?: return null
            OpenFileDescriptor(project, virtualFile)
        } else {
            null
        }
    }

    private fun getTargetPath(treePath: TreePath): VirtualFile? {
        val userObject = (treePath.lastPathComponent as DefaultMutableTreeNode).userObject
        return if (userObject is FileNodeDescriptor) {
            userObject.element.file
        } else {
            null
        }
    }
}
