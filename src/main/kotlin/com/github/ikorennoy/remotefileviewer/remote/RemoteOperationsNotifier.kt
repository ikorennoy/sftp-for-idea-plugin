package com.github.ikorennoy.remotefileviewer.remote

import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class RemoteOperationsNotifier(val project: Project) {

    fun notifyFileUploaded() {
        getNotificationGroup()
            .createNotification("File successfully uploaded", NotificationType.INFORMATION)
            .notify(project)
    }

    fun cantLoadChildren(error: Throwable) {
        getNotificationGroup()
            .createNotification("Error while reading a directory: ${error.message}", NotificationType.ERROR)
            .notify(project)
    }

    private fun getNotificationGroup(): NotificationGroup {
        return NotificationGroupManager.getInstance()
            .getNotificationGroup("Remote Operations Notifications")
    }
}
