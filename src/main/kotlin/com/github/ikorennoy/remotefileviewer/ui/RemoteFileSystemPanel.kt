package com.github.ikorennoy.remotefileviewer.ui

import com.github.ikorennoy.remotefileviewer.filesystem.SftpFileSystem
import com.github.ikorennoy.remotefileviewer.template.FileViewerBundle
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileChooser.impl.FileComparator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.TreeUIHelper
import com.intellij.ui.tree.AsyncTreeModel
import com.intellij.ui.tree.StructureTreeModel
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.EditSourceOnDoubleClickHandler
import com.intellij.util.EditSourceOnEnterKeyHandler
import com.intellij.util.IconUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.tree.TreeUtil
import javax.swing.JPanel
import javax.swing.ToolTipManager
import javax.swing.tree.DefaultTreeModel

class RemoteFileSystemPanel(
    private val project: Project
) : SimpleToolWindowPanel(true, true), Disposable {

    internal val REMOTE_FS_KEY: DataKey<RemoteFileSystemPanel> = DataKey.create("remote.file.system.api")

    private val fs = SftpFileSystem()

    private lateinit var fileChooserDescriptor: FileChooserDescriptor
    private lateinit var treeStructure: RemoteFileTreeStructure
    private lateinit var treeModel: StructureTreeModel<RemoteFileTreeStructure>
    private lateinit var tree: Tree

    init {
        toolbar = createToolbarPanel()
        setContent(ScrollPaneFactory.createScrollPane(setupEmptyTree()))
    }

    private fun setupEmptyTree(): Tree {
        return Tree(DefaultTreeModel(null))
    }

    private fun showConnectionDialog() {
        val dialog = ConnectionConfigurationDialog(project, fs)
        if (dialog.showAndGet() && fs.isReady()) {
            drawTree()
        }
    }

    private fun drawTree() {
        fileChooserDescriptor = FileChooserDescriptorFactory.createAllButJarContentsDescriptor()
        fileChooserDescriptor.setRoots(fs.root)
        fileChooserDescriptor.withTreeRootVisible(true)
        treeStructure = RemoteFileTreeStructure(project, fileChooserDescriptor)
        treeModel = StructureTreeModel(treeStructure, FileComparator.getInstance(), this)
        tree = Tree(AsyncTreeModel(treeModel, this))
        TreeUtil.installActions(tree)
        TreeUIHelper.getInstance().installTreeSpeedSearch(tree)
        ToolTipManager.sharedInstance().registerComponent(tree)
        EditSourceOnDoubleClickHandler.install(tree)
        EditSourceOnEnterKeyHandler.install(tree)
        tree.isRootVisible = true
        addDataProvider(RemoteFileSystemDataProvider(this))
        setContent(ScrollPaneFactory.createScrollPane(tree))
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

    private class RemoteFileSystemDataProvider(private val panel: RemoteFileSystemPanel): DataProvider {
        override fun getData(dataId: String): Any? {
            return if (panel.REMOTE_FS_KEY.`is`(dataId)) {
                panel
            } else {
                null
            }
        }
    }
}
