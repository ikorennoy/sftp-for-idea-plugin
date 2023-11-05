package com.github.ikorennoy.remoteaccess.tree

import com.github.ikorennoy.remoteaccess.operations.RemoteOperations
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.ide.util.treeView.ValidateableNode
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.PlatformIcons

class DummyNode(private val project: Project) : ValidateableNode {

    /**
     * Valid until the service is initialized
     */
    override fun isValid(): Boolean {
        return !project.service<RemoteOperations>().isInitializedAndConnected()
    }

    fun getNodeDescriptor(project: Project, parentDescriptor: NodeDescriptor<*>?): NodeDescriptor<Any> {
        return object : NodeDescriptor<Any>(project, parentDescriptor) {
            init {
                icon = PlatformIcons.FOLDER_ICON
                myName = "SFTP"
            }

            override fun update(): Boolean {
                return false
            }

            override fun getElement(): Any {
                return this
            }
        }
    }
}
