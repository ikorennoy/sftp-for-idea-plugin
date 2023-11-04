package com.github.ikorennoy.remotefileviewer.tree

import com.intellij.icons.AllIcons
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.openapi.project.Project

class ErrorNode {

    fun getDescriptor(project: Project, parentDescriptor: NodeDescriptor<*>?): NodeDescriptor<*> {
        return object : NodeDescriptor<Any>(project, parentDescriptor) {
            init {
                icon = AllIcons.RunConfigurations.ToolbarError
                myName = ""
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
