package com.github.ikorennoy.remotefileviewer.ui

import com.github.ikorennoy.remotefileviewer.filesystem.RemoteFileSystem
import com.github.ikorennoy.remotefileviewer.remote.RemoteConnectionListener
import com.github.ikorennoy.remotefileviewer.remoteEdit.EditRemoteFileTask
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileChooser.FileSystemTree
import com.intellij.openapi.fileChooser.ex.FileSystemTreeImpl
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.tree.StructureTreeModel
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.UIUtil
import java.io.IOException
import javax.swing.JTree
import javax.swing.tree.DefaultTreeModel

class FileViewerWindowsFactory : ToolWindowFactory, DumbAware {

    override fun init(toolWindow: ToolWindow) {
        val project = toolWindow.project
        val manager = ToolWindowManager.getInstance(project)

    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // first try to connect on window open
        val remoteFs = RemoteFileSystem.getInstance()
        var remoteFsTree = tryPrepareRemoteFsTree(project, remoteFs)
        val remoteFsPanel = if (remoteFsTree != null) {
            RemoteFileSystemPanel(remoteFsTree.tree, false)
        } else {
            RemoteFileSystemPanel(setupEmptyTree(), true)
        }
        if (remoteFsTree != null) {
            remoteFsPanel.addDataProvider(MyDataProvider(remoteFsTree))
        }
        val toolWindowContent = ContentFactory.getInstance().createContent(remoteFsPanel, null, false)
        toolWindow.contentManager.addContent(toolWindowContent)
        ApplicationManager.getApplication().messageBus.connect().subscribe(RemoteConnectionListener.TOPIC, object : RemoteConnectionListener {
            override fun connectionEstablished() {
                if (remoteFsTree != null) {
                    remoteFsTree?.updateTree()
                } else {
                    remoteFsTree = tryPrepareRemoteFsTree(project, remoteFs)
                    UIUtil.invokeLaterIfNeeded {
                        toolWindow.contentManager.removeContent(toolWindowContent, false)
                        toolWindow.contentManager.addContent(
                            ContentFactory.getInstance()
                                .createContent(RemoteFileSystemPanel(remoteFsTree!!.tree, false), null, false)
                        )
                    }
                }
            }
        })
    }

    private fun setupEmptyTree(): JTree {
        return Tree(DefaultTreeModel(null))
    }

    private fun tryPrepareRemoteFsTree(project: Project, fs: RemoteFileSystem): FileSystemTreeImpl? {
        return try {
            val fileChooserDescriptor: FileChooserDescriptor =
                FileChooserDescriptorFactory.createSingleFileOrFolderDescriptor()
            fileChooserDescriptor.setRoots(fs.getRoot())
            fileChooserDescriptor.withTreeRootVisible(true)
            val tree = FileSystemTreeImpl(project, fileChooserDescriptor)
            tree.registerMouseListener(createActionGroup())
            tree.addOkAction(EditRemoteFileTask(project, fs, tree))
            tree
        } catch (ex: Throwable) {
            ex.printStackTrace()
            null
        }
    }

    private fun createActionGroup(): DefaultActionGroup {
        val showCreate = ActionManager.getInstance().getAction("RemoteFileSystem.ShowCreate")
        val delete = ActionManager.getInstance().getAction("FileChooser.Delete")
        val group = DefaultActionGroup()
        group.add(showCreate)
        group.addSeparator()
        group.add(delete)
        group.addSeparator()
        return group
    }

    override fun shouldBeAvailable(project: Project) = true

    private fun repaintContent(myToolWindow: RemoteFileSystemPanel, content: Content, toolWindow: ToolWindow) {
        myToolWindow.repaint()
    }

    private class MyDataProvider(private val fsTree: FileSystemTree) : DataProvider {
        override fun getData(dataId: String): Any? {
            return if (FileSystemTree.DATA_KEY.`is`(dataId)) {
                fsTree
            } else if (CommonDataKeys.VIRTUAL_FILE.`is`(dataId)) {
                fsTree.selectedFile
            } else if (CommonDataKeys.VIRTUAL_FILE_ARRAY.`is`(dataId)) {
                fsTree.selectedFiles
            } else {
                null
            }
        }
    }

}
