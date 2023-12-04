package com.github.ikorennoy.remoteaccess.actions

import com.github.ikorennoy.remoteaccess.tree.RemoteFileSystemTree
import com.github.ikorennoy.remoteaccess.ui.AttributesWindowDialog
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

class EditFileAttributesAction: DumbAwareAction(
    "Edit Attributes..."
) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val fsTree = e.getData(RemoteFileSystemTree.DATA_KEY) ?: return
        val file = fsTree.getSelectedFile() ?: return
        AttributesWindowDialog(project, file).show()
    }
}
