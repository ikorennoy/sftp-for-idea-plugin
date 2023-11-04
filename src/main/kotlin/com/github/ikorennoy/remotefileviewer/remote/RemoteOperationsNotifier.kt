package com.github.ikorennoy.remotefileviewer.remote

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class RemoteOperationsNotifier(val project: Project) {

    fun notifyFileUploaded() {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Remote Operations Notifications")
            .createNotification("File successfully uploaded", NotificationType.INFORMATION)
            .notify(project)
    }
}
