package com.github.ikorennoy.remotefileviewer.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileSystemTree

class NewFileAction: AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        TODO("Not yet implemented")
    }

    override fun update(e: AnActionEvent) {
        val fsTree = e.getData(FileSystemTree.DATA_KEY) ?: return



        super.update(e)
    }
}