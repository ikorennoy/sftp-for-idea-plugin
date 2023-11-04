package com.github.ikorennoy.remotefileviewer.tree

import com.github.ikorennoy.remotefileviewer.remote.RemoteVirtualFile
import com.github.ikorennoy.remotefileviewer.remote.RemoteOperations
import com.github.ikorennoy.remotefileviewer.settings.RemoteFileViewerSettingsState
import com.intellij.ide.util.treeView.AbstractTreeStructure
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.openapi.components.service
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VFileProperty
import com.intellij.ui.IconManager
import com.intellij.ui.LayeredIcon
import com.intellij.ui.tree.LeafState
import com.intellij.util.PlatformIcons
import javax.swing.Icon

class RemoteFileTreeStructure(
    private val project: Project,
) : AbstractTreeStructure() {

    private val dummyRoot = DummyNode()

    override fun getRootElement(): Any {
        val ops = service<RemoteOperations>()
        return if (!ops.isInitializedAndConnected()) {
            dummyRoot
        } else {
            val conf = service<RemoteFileViewerSettingsState>()
            ops.findFileByPath(conf.root) ?: IllegalStateException("root path can't be null")
        }
    }

    override fun getChildElements(element: Any): Array<RemoteVirtualFile> {
        return if (element is RemoteVirtualFile) {
            element.getChildren()
        } else {
            emptyArray()
        }
    }

    override fun getParentElement(element: Any): Any? {
        return if (element is RemoteVirtualFile) {
            element.getParent()
        } else {
            null
        }
    }

    override fun createDescriptor(element: Any, parentDescriptor: NodeDescriptor<*>?): NodeDescriptor<*> {
        if (element is DummyNode) {
            return object : NodeDescriptor<Any>(project, parentDescriptor) {
                init {
                    icon = PlatformIcons.FOLDER_ICON
                    myName = "SFTP"
                }

                override fun update(): Boolean {
                    return false
                }

                override fun getElement(): Any {
                    return dummyRoot
                }

            }
        }
        if (element !is RemoteVirtualFile) throw IllegalArgumentException("element is not file")
        val icon = getIcon(element)
        val name = element.getPresentableName()
        return RemoteFileNodeDescriptor(project, parentDescriptor, element, icon, name)
    }

    override fun commit() {
    }

    override fun hasSomethingToCommit(): Boolean {
        return false
    }

    override fun isToBuildChildrenInBackground(element: Any): Boolean {
        return false
    }

    override fun getLeafState(element: Any): LeafState {
        return if (element is RemoteVirtualFile) {
            if (element.isDirectory()) {
                LeafState.NEVER
            } else {
                LeafState.ALWAYS
            }
        } else {
            LeafState.DEFAULT
        }
    }

    override fun isAlwaysLeaf(element: Any): Boolean {
        return if (element is RemoteVirtualFile) {
            element.isDirectory()
        } else {
            false
        }
    }

    fun getIcon(file: RemoteVirtualFile): Icon {
        val icon = if (file.isDirectory()) {
            IconManager.getInstance().tooltipOnlyIfComposite(PlatformIcons.FOLDER_ICON);
        } else {
            FileTypeRegistry.getInstance().getFileTypeByFileName(file.getName()).icon
        }
        return dressIcon(file, icon)
    }

    private fun dressIcon(file: RemoteVirtualFile, baseIcon: Icon): Icon {
        return if (file.isValid() && file.`is`(VFileProperty.SYMLINK)) {
            LayeredIcon(baseIcon, PlatformIcons.SYMLINK_ICON)
        } else {
            baseIcon
        }
    }
}
