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


    lateinit var tree: FileSystemTreeImpl

    init {
        toolbar = createToolbarPanel()
        setContent(ScrollPaneFactory.createScrollPane(setupEmptyTree()))
    }

    private fun setupEmptyTree(): Tree {
        return Tree(DefaultTreeModel(null))
    }

    private fun showConnectionDialog() {
        val fs = SftpFileSystem.getInstance()
        val dialog = ConnectionConfigurationDialog(project, fs)
        if (dialog.showAndGet() && fs.isReady()) {
            drawTree(fs)
        }
    }

    private fun drawTree(fs: SftpFileSystem) {
        val fileChooserDescriptor: FileChooserDescriptor =
            FileChooserDescriptorFactory.createAllButJarContentsDescriptor()
        fileChooserDescriptor.setRoots(fs.root)
        fileChooserDescriptor.withTreeRootVisible(true)
        tree = FileSystemTreeImpl(project, fileChooserDescriptor)
        tree.registerMouseListener(createActionGroup())
        EditSourceOnDoubleClickHandler.install(tree.tree)
        tree.addOkAction(EditRemoteFileTask(project, fs, tree))
        addDataProvider(MyDataProvider(tree))
        setContent(ScrollPaneFactory.createScrollPane(tree.tree))
    }

    private fun registerTreeActionShortcut(actionId: String) {
        val tree: JTree = tree.tree
        val action = ActionManager.getInstance().getAction(actionId)
        action.registerCustomShortcutSet(action.shortcutSet, tree, this)
    }

    private fun createActionGroup(): DefaultActionGroup {
        registerTreeActionShortcut("FileChooser.Delete")
        registerTreeActionShortcut("FileChooser.Refresh")
        registerTreeActionShortcut("RemoteFileSystem.NewFileAction")
        val group = DefaultActionGroup()
        for (action in (ActionManager.getInstance()
            .getAction("FileChooserToolbar") as DefaultActionGroup).getChildActionsOrStubs()) {
            group.addAction(action)
        }
        group.add(ActionManager.getInstance().getAction("RemoteFileSystem.NewFileAction"))
        return group
    }

    private fun createToolbarPanel(): JPanel {
        val toolbarGroup = DefaultActionGroup()
        toolbarGroup.add(object : AnAction(
            FileViewerBundle.messagePointer("add.new.connection.action.name"),
            FileViewerBundle.messagePointer("add.new.connection.action.description"),
            IconUtil.getAddIcon()
        ) {
            override fun actionPerformed(e: AnActionEvent) {
                showConnectionDialog()
            }
        })

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
