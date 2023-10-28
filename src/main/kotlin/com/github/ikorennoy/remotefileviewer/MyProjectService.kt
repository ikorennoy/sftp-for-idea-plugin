package com.github.ikorennoy.remotefileviewer

import com.github.ikorennoy.remotefileviewer.template.FileViewerBundle
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class MyProjectService(project: Project) {

    init {
        thisLogger().info(FileViewerBundle.message("projectService", project.name))
    }

    fun getRandomNumber() = (1..100).random()
}