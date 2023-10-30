package com.github.ikorennoy.remotefileviewer

import com.github.ikorennoy.remotefileviewer.filesystem.SftpFileSystem
import com.github.ikorennoy.remotefileviewer.ui.RemoteFileSystemPanel
import com.github.ikorennoy.remotefileviewer.ui.RemoteFileTreeStructure
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileChooser.impl.FileComparator
import com.intellij.openapi.project.Project
import com.intellij.ui.PopupHandler
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.TreeUIHelper
import com.intellij.ui.tree.AsyncTreeModel
import com.intellij.ui.tree.StructureTreeModel
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.EditSourceOnDoubleClickHandler
import com.intellij.util.EditSourceOnEnterKeyHandler
import com.intellij.util.ui.tree.TreeUtil
import javax.swing.ToolTipManager
import javax.swing.event.TreeModelEvent
import javax.swing.event.TreeModelListener


val REMOTE_FS_OPERATIONS_KEY: DataKey<RemoteFileSystemTreeImpl> = DataKey.create("RemoteFileSystemTree")

class RemoteFileSystemTreeImpl(
    private val project: Project,
    private val fs: SftpFileSystem
) : Disposable, DataProvider {

    init {
        if (!fs.isReady()) {
            throw IllegalStateException("FileSystem is not initialized")
        }
    }

    private val fileChooserDescriptor: FileChooserDescriptor
    private val treeStructure: RemoteFileTreeStructure
    private val treeModel: StructureTreeModel<RemoteFileTreeStructure>
    private val tree: Tree

    init {
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
        PopupHandler.installPopupMenu(tree, "RemoteFileSystemPopup", "RemoteFsPopup")
    }

    fun getTree(): Tree {
        return tree
    }

    override fun dispose() {

    }

    override fun getData(dataId: String): Any? {

        return null
    }
}
