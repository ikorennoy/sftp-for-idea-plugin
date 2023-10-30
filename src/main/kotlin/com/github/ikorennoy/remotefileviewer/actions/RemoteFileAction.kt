package com.github.ikorennoy.remotefileviewer.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class RemoteFileAction: AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        println(e)
    }
}