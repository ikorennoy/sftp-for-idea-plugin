package com.github.ikorennoy.remotefileviewer.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

class DeleteFileAction: AnAction("Delete...") {

    override fun update(e: AnActionEvent) {
        val dataContext = e.dataContext
        val vf = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext)
        println(vf)
        println(dataContext)
    }

    override fun actionPerformed(e: AnActionEvent) {
        println(e)
    }
}