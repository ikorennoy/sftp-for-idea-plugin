package com.github.ikorennoy.remotefileviewer.ui

import com.github.ikorennoy.remotefileviewer.remoteEdit.EditRemoteFileTask
import com.github.ikorennoy.remotefileviewer.filesystem.SftpFileSystem
import com.github.ikorennoy.remotefileviewer.template.FileViewerBundle
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileChooser.FileSystemTree
import com.intellij.openapi.fileChooser.ex.FileSystemTreeImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.EditSourceOnDoubleClickHandler
import com.intellij.util.IconUtil
import com.intellij.util.ui.JBUI
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.tree.DefaultTreeModel

class RemoteFileSystemPanel(
    private val project: Project
) : SimpleToolWindowPanel(true, true), Disposable {

    private lateinit var tree: FileSystemTreeImpl

    init {
        toolbar = createToolbarPanel()
        val remoteFs = SftpFileSystem.getInstance()
        drawTree(remoteFs)
    }


    private fun drawTree(fs: SftpFileSystem) {
        val fileChooserDescriptor: FileChooserDescriptor =
            FileChooserDescriptorFactory.createAllButJarContentsDescriptor()
        fileChooserDescriptor.setRoots(fs.root)
        fileChooserDescriptor.withTreeRootVisible(true)
        tree = FileSystemTreeImpl(project, fileChooserDescriptor)
        tree.registerMouseListener(createActionGroup())
        tree.addOkAction(EditRemoteFileTask(project, fs, tree))
        addDataProvider(MyDataProvider(tree))
        setContent(ScrollPaneFactory.createScrollPane(tree.tree))
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

    private fun createToolbarPanel(): JPanel {
        val toolbarGroup = DefaultActionGroup()
        toolbarGroup.add(ActionManager.getInstance().getAction("RemoteFileSystem.ShowSshConfiguration"))

        val actionToolbar = ActionManager.getInstance().createActionToolbar("FVToolbar", toolbarGroup, true)
        actionToolbar.targetComponent = this
        return JBUI.Panels.simplePanel(actionToolbar.component)
    }

    override fun dispose() {

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
