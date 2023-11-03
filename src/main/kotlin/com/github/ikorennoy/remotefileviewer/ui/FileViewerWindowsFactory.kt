package com.github.ikorennoy.remotefileviewer.ui

import com.github.ikorennoy.remotefileviewer.filesystem.RemoteFileSystem
import com.github.ikorennoy.remotefileviewer.remote.RemoteConnectionListener
import com.github.ikorennoy.remotefileviewer.remote.RemoteConnectionManager
import com.github.ikorennoy.remotefileviewer.remoteEdit.EditRemoteFileTask
import com.github.ikorennoy.remotefileviewer.settings.RemoteFileViewerSettingsState
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileChooser.FileElement
import com.intellij.openapi.fileChooser.ex.FileSystemTreeImpl
import com.intellij.openapi.fileChooser.impl.FileTreeStructure
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.tree.LeafState
import com.intellij.util.io.isFile
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.tree.TreeUtil
import java.io.InputStream
import java.io.OutputStream

class FileViewerWindowsFactory : ToolWindowFactory, DumbAware {
    private val rootNode by lazy {
        object : VirtualFile() {

            override fun getName(): String {
                val conf = service<RemoteFileViewerSettingsState>()
                return "SFTP (${conf.root})"
            }

            override fun getFileSystem(): VirtualFileSystem {
                return RemoteFileSystem.getInstance()
            }

            override fun getPath(): String {
                return getName()
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


    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // first try to connect on window open
        val configuration = service<RemoteFileViewerSettingsState>()
        var tryConnect = true

        if (configuration.isNotValid()) {
            // show full configuration dialogue
            ShowSettingsUtil.getInstance().showSettingsDialog(
                project,
                "com.github.ikorennoy.remotefileviewer.settings.ui.RemoteFilePluginConfigurable"
            )
            tryConnect = !configuration.isNotValid() // user cancelled settings dialog
        } else {
            if (configuration.password.isEmpty()) {
                // show password prompt dialogue
                val password = Messages.showPasswordDialog(
                    "Enter a password:",
                    "Connecting to: ${configuration.username}@${configuration.host}:${configuration.port}",
                )

                if (password != null) {
                    configuration.password = password.toCharArray()
                } else {
                    // it means user cancelled password enter dialog
                    tryConnect = false
                }
            }
        }

        if (tryConnect) {
            val connManager = service<RemoteConnectionManager>()
            connManager.init()
        }

        val fileChooserDescriptor: FileChooserDescriptor =
            FileChooserDescriptorFactory.createSingleFileOrFolderDescriptor()
        fileChooserDescriptor.isShowFileSystemRoots = false
        fileChooserDescriptor.setRoots(rootNode)
        fileChooserDescriptor.withTreeRootVisible(true)

        val remoteFileSystemTree = FileSystemTreeImpl(project, fileChooserDescriptor)
        remoteFileSystemTree.addOkAction(EditRemoteFileTask(project, remoteFileSystemTree))
        remoteFileSystemTree.registerMouseListener(createActionGroup())
        val remoteFileSystemPanel = RemoteFileSystemPanel(remoteFileSystemTree)


        val toolWindowContent = ContentFactory.getInstance().createContent(remoteFileSystemPanel, null, false)
        toolWindow.contentManager.addContent(toolWindowContent)

        ApplicationManager.getApplication().messageBus.connect()
            .subscribe(RemoteConnectionListener.TOPIC, object : RemoteConnectionListener {
                override fun connectionEstablished() {
                    UIUtil.invokeLaterIfNeeded {
                        remoteFileSystemTree.updateTree()
                        TreeUtil.promiseExpand(remoteFileSystemTree.tree, 1)
                    }
                }
            })
    }

    override fun shouldBeAvailable(project: Project) = true

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

}

class RemoteFileTreeStructure(
    project: Project,
    fileChooserDescriptor: FileChooserDescriptor,
) : FileTreeStructure(project, fileChooserDescriptor) {

    override fun isToBuildChildrenInBackground(element: Any): Boolean {
        return true
    }

    override fun getLeafState(element: Any): LeafState {
        return if (element is FileElement) {
            if (element.file.isDirectory) {
                LeafState.NEVER
            } else {
                LeafState.ALWAYS
            }
        } else {
            LeafState.DEFAULT
        }
    }

    override fun isAlwaysLeaf(element: Any): Boolean {
        return if (element is FileElement) {
            element.file.toNioPath().isFile()
        } else {
            false
        }
    }
}

