package com.github.ikorennoy.remotefileviewer.ui

import com.github.ikorennoy.remotefileviewer.tree.RemoteFileSystemTree
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.ScrollPaneFactory
import com.intellij.util.ui.JBUI
import javax.swing.JPanel

// todo add update for this panel
//  check why not all files opening
//  prob add new create dir action
//  check that everything works with 'bind mounts'
//  start writing tests
//  fix a case when configuration is corrupted (like wrong host and etc), but only password prompt is shown, after firs unsuccessful attempt
//  we should show full configuration page
class RemoteFileSystemPanel(
    tree: RemoteFileSystemTree,
) : SimpleToolWindowPanel(true, true) {

    init {
        toolbar = createToolbarPanel()
        setContent(ScrollPaneFactory.createScrollPane(tree.tree))
        addDataProvider(MyDataProvider(tree))
    }

    private fun createToolbarPanel(): JPanel {
        val toolbarGroup = DefaultActionGroup()
        toolbarGroup.add(ActionManager.getInstance().getAction("RemoteFileSystem.ShowSshConfiguration"))
        toolbarGroup.add(ActionManager.getInstance().getAction("RemoteFileSystem.UpdateTree"))
        toolbarGroup.add(ActionManager.getInstance().getAction("RemoteFileSystem.Disconnect"))

        val actionToolbar = ActionManager.getInstance().createActionToolbar("FVToolbar", toolbarGroup, true)
        actionToolbar.targetComponent = this
        return JBUI.Panels.simplePanel(actionToolbar.component)
    }

    private class MyDataProvider(private val fsTree: RemoteFileSystemTree) : DataProvider {
        override fun getData(dataId: String): Any? {
            return if (RemoteFileSystemTree.DATA_KEY.`is`(dataId)) {
                fsTree
            } else {
                null
            }
        }
    }
}
