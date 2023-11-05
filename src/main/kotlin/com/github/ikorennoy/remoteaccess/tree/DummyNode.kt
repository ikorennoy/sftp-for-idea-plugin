package com.github.ikorennoy.remoteaccess.tree

import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.ide.util.treeView.ValidateableNode
import com.intellij.openapi.project.Project

class DummyNode : ValidateableNode {

    override fun isValid(): Boolean {
        return false
    }

    fun getNodeDescriptor(project: Project, parentDescriptor: NodeDescriptor<*>?): NodeDescriptor<Any> {
        return object : NodeDescriptor<Any>(project, parentDescriptor) {
            override fun update(): Boolean {
                return false
            }

            override fun getElement(): Any {
                return this
            }
        }
    }
}
