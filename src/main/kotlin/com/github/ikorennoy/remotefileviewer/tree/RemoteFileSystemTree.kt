package com.github.ikorennoy.remotefileviewer.tree

import com.github.ikorennoy.remotefileviewer.filesystem.RemoteFileSystem
import com.github.ikorennoy.remotefileviewer.remote.RemoteConnectionManager
import com.github.ikorennoy.remotefileviewer.remoteEdit.EditRemoteFileTask
import com.github.ikorennoy.remotefileviewer.settings.RemoteFileViewerSettingsState
import com.github.ikorennoy.remotefileviewer.ui.RemoteFileTreeStructure
import com.intellij.ide.plugins.performAction
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileChooser.ex.FileNodeDescriptor
import com.intellij.openapi.fileChooser.impl.FileComparator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.ui.DoubleClickListener
import com.intellij.ui.PopupHandler
import com.intellij.ui.TreeSpeedSearch
import com.intellij.ui.tree.AsyncTreeModel
import com.intellij.ui.tree.StructureTreeModel
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.EditSourceOnDoubleClickHandler
import com.intellij.util.EditSourceOnEnterKeyHandler
import com.intellij.util.ui.tree.TreeUtil
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.io.InputStream
import java.io.OutputStream
import javax.swing.JComponent
import javax.swing.JTree
import javax.swing.KeyStroke
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath

class RemoteFileSystemTree(val project: Project) : Disposable {

    private val rootNode by lazy {
        object : VirtualFile() {

            override fun getName(): String {
                return "SFTP"
            }

            override fun getFileSystem(): VirtualFileSystem {
                return RemoteFileSystem.getInstance()
            }

            override fun getPath(): String {
//                val conf = service<RemoteFileViewerSettingsState>()
                return "SFTP"
            }

            override fun isWritable(): Boolean {
                return true
            }

            override fun isDirectory(): Boolean {
                return true
            }

            override fun isValid(): Boolean {
                return true
            }

            override fun getParent(): VirtualFile? {
                return null
            }

            override fun getChildren(): Array<VirtualFile> {
                val connManager = service<RemoteConnectionManager>()
                val conf = service<RemoteFileViewerSettingsState>()
                return if (connManager.initialized()) {
                    val originalRoot = fileSystem.findFileByPath(conf.root)
                    if (originalRoot != null) {
                        originalRoot.children
                    } else {
                        emptyArray()
                    }
                } else {
                    emptyArray()
                }
            }

            override fun getOutputStream(
                requestor: Any?,
                newModificationStamp: Long,
                newTimeStamp: Long
            ): OutputStream {
                return OutputStream.nullOutputStream()
            }

            override fun contentsToByteArray(): ByteArray {
                return ByteArray(0)
            }

            override fun getTimeStamp(): Long {
                return 0
            }

            override fun getLength(): Long {
                return 0
            }

            override fun refresh(asynchronous: Boolean, recursive: Boolean, postRunnable: Runnable?) {
            }

            override fun getInputStream(): InputStream {
                return InputStream.nullInputStream()
            }
        }
    }

    private val fileChooserDescriptor: FileChooserDescriptor
    private val treeModel: StructureTreeModel<RemoteFileTreeStructure>
    private val asyncTreeModel: AsyncTreeModel

    val tree: JTree

    init {
        fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFileOrFolderDescriptor()
        fileChooserDescriptor.isShowFileSystemRoots = false
        fileChooserDescriptor.setRoots(rootNode)
        fileChooserDescriptor.withTreeRootVisible(false)
        val remoteTreeStructure = RemoteFileTreeStructure(project, fileChooserDescriptor)
        treeModel = StructureTreeModel(remoteTreeStructure, FileComparator.getInstance(), this)
        asyncTreeModel = AsyncTreeModel(treeModel, this)
        tree = Tree(asyncTreeModel)
        TreeSpeedSearch(tree)
        TreeUtil.installActions(tree)
        PopupHandler.installPopupMenu(tree, createActionGroup(), "RemoteFileSystemTreePopup")

        val editAction = EditRemoteFileTask(project, this)

        tree.registerKeyboardAction(
            { performAction(editAction, true) },
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
            JComponent.WHEN_FOCUSED
        )
        object : DoubleClickListener() {
            override fun onDoubleClick(event: MouseEvent): Boolean {
                performAction(editAction, false)
                return true
            }
        }.installOn(tree)
    }

    fun getSelectedFile(): VirtualFile? {
        val treePath = tree.selectionPath ?: return null
        return getTargetPath(treePath)
    }

    private fun performAction(action: Runnable, toggle: Boolean) {
        val path = tree.selectionPath
        if (path != null) {
            if (asyncTreeModel.isLeaf(path.lastPathComponent)) {
                action.run()
            } else if (toggle) {
                if (tree.isExpanded(path)) {
                    tree.collapsePath(path)
                } else {
                    tree.expandPath(path)
                }
            }
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

    override fun dispose() {
    }

    fun invalidate() {
        treeModel.invalidateAsync()
    }
}
