package com.github.ikorennoy.remoteaccess.operations

import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import java.io.IOException

@Service(Service.Level.PROJECT)
class RemoteOperationsNotifier(val project: Project) {

    fun fileUploaded(fileName: String) {
        getNotificationGroup()
            .createNotification(
                "Upload",
                "File $fileName successfully uploaded",
                NotificationType.INFORMATION
            ).notify(project)
    }

    fun cannotLoadChildren(error: Throwable) {
        getNotificationGroup()
            .createNotification(
                "Error while reading directory: ${error.message}",
                NotificationType.ERROR
            ).notify(project)
    }

    fun cannotDelete(file: RemoteFileInformation, error: IOException, entity: String) {
        getNotificationGroup()
            .createNotification(
                "Delete",
                "Cannot delete $entity ${file.getPath()}: ${error.message}",
                NotificationType.ERROR
            ).notify(project)
    }

    fun cannotCreateChildFile(newFile: String, error: IOException) {
        getNotificationGroup()
            .createNotification(
                "Create",
                "Cannot create file ${newFile}: ${error.message}",
                NotificationType.ERROR
            ).notify(project)
    }

    fun cannotCreateChildDirectory(newDir: String, ex: IOException) {
        getNotificationGroup()
            .createNotification(
                "Create",
                "Cannot create directory ${newDir}: ${ex.message}",
                NotificationType.ERROR
            ).notify(project)
    }

    fun cannotOpenFile(filePath: String, ex: IOException) {
        getNotificationGroup()
            .createNotification(
                "Cannot open file ${filePath}: ${ex.message}",
                NotificationType.ERROR
            ).notify(project)
    }

    fun cannotRename(fromPath: String, toPath: String, ex: IOException) {
        getNotificationGroup()
            .createNotification(
                "Cannot rename $fromPath - $toPath: ${ex.message}",
                NotificationType.ERROR
            ).notify(project)
    }

    fun disconnect(reasonName: String, message: String) {
        getNotificationGroup()
            .createNotification(
                "SSH connection is lost: $reasonName; $message",
                NotificationType.ERROR
            ).notify(project)
    }

    private fun getNotificationGroup(): NotificationGroup {
        return NotificationGroupManager.getInstance()
            .getNotificationGroup("Remote Operations Notifications")
    }

    companion object {
        fun getInstance(project: Project): RemoteOperationsNotifier = project.service()
    }
}
