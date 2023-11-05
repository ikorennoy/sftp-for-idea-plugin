package com.github.ikorennoy.remote.tree

import com.github.ikorennoy.remote.operations.RemoteFileInformation
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.openapi.project.Project
import javax.swing.Icon

class RemoteFileSystemTreeNodeDescriptor(
    project: Project,
    parentDescriptor: NodeDescriptor<*>?,
    private val element: RemoteFileInformation,
    icon: Icon,
    name: String,
) : NodeDescriptor<RemoteFileInformation>(project, parentDescriptor) {

    init {
        myName = name
        setIcon(icon)

    }

    override fun update(): Boolean {
        var updated = false

        val newName = element.getPresentableName()
        if (myName != newName) {
            myName = newName
            updated = true
        }

        return updated
    }

    override fun getElement(): RemoteFileInformation {
        return element
    }
}
