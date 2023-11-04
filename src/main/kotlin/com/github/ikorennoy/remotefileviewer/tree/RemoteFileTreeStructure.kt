package com.github.ikorennoy.remotefileviewer.tree

import com.github.ikorennoy.remotefileviewer.remote.RemoteOperations
import com.github.ikorennoy.remotefileviewer.settings.RemoteFileViewerSettingsState
import com.intellij.ide.util.treeView.AbstractTreeStructure
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Iconable
import com.intellij.openapi.vfs.VFileProperty
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.LayeredIcon
import com.intellij.ui.tree.LeafState
import com.intellij.util.IconUtil
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

    override fun getChildElements(element: Any): Array<VirtualFile> {
        return if (element is VirtualFile) {
            element.children
        } else {
            emptyArray()
        }
    }

    override fun getParentElement(element: Any): Any? {
        return if (element is VirtualFile) {
            element.parent
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
        if (element !is VirtualFile) throw IllegalArgumentException("element is not file")
        val icon = getIcon(element)
        val name = element.presentableName
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
        return if (element is VirtualFile) {
            if (element.isDirectory) {
                LeafState.NEVER
            } else {
                LeafState.ALWAYS
            }
        } else {
            LeafState.DEFAULT
        }
    }

    override fun isAlwaysLeaf(element: Any): Boolean {
        return if (element is VirtualFile) {
            element.isDirectory
        } else {
            false
        }
    }

    fun getIcon(file: VirtualFile): Icon {
        return dressIcon(file, IconUtil.getIcon(file, Iconable.ICON_FLAG_READ_STATUS, null))
    }

    private fun dressIcon(file: VirtualFile, baseIcon: Icon): Icon {
        return if (file.isValid && file.`is`(VFileProperty.SYMLINK)) {
            LayeredIcon(baseIcon, PlatformIcons.SYMLINK_ICON)
        } else {
            baseIcon
        }
    }
}
