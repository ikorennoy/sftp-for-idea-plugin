package com.github.ikorennoy.remotefileviewer.ui

import com.github.ikorennoy.remotefileviewer.tree.RemoteFileSystemTree
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.fileChooser.FileSystemTree
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.ScrollPaneFactory
import com.intellij.util.ui.JBUI
import javax.swing.JPanel
import javax.swing.JTree

// todo add update for this panel
//  check why not all files opening
//  prob add new create dir action
//  check that everything works with 'bind mounts'
//  start writing tests
//  fix a case when configuration is corrupted (like wrong host and etc), but only password prompt is shown, after firs unsuccessful attempt
//  we should show full configuration page
class RemoteFileSystemPanel(
    tree: RemoteFileSystemTree,
) : SimpleToolWindowPanel(true, true), Disposable {

    init {
        toolbar = createToolbarPanel()
        setContent(ScrollPaneFactory.createScrollPane(tree.tree))
        addDataProvider(MyDataProvider(tree))
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

    private class MyDataProvider(private val fsTree: RemoteFileSystemTree) : DataProvider {
        override fun getData(dataId: String): Any? {
            return if (CommonDataKeys.NAVIGATABLE.`is`(dataId)) {
                val file = fsTree.getSelectedFile() ?: return null
                OpenFileDescriptor(fsTree.project, file, 0)
            } else if (CommonDataKeys.NAVIGATABLE_ARRAY.`is`(dataId)) {
                val file = fsTree.getSelectedFile() ?: return null
                arrayOf(OpenFileDescriptor(fsTree.project, file, 0))
            } else {
                null
            }
        }
    }
}
