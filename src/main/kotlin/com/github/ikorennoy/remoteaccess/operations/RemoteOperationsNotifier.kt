package com.github.ikorennoy.remoteaccess.operations

import com.github.ikorennoy.remoteaccess.template.RemoteFileAccessBundle
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
                RemoteFileAccessBundle.message("notification.RemoteFileAccess.fileUploaded.title"),
                RemoteFileAccessBundle.message("notification.RemoteFileAccess.fileUploaded.content", fileName),
                NotificationType.INFORMATION
            ).notify(project)
    }

    fun cannotLoadChildren(error: IOException) {
        getNotificationGroup()
            .createNotification(
                RemoteFileAccessBundle.message(
                    "notification.RemoteFileAccess.cannotLoadChildren.content",
                    error.message ?: ""
                ),
                NotificationType.ERROR
            ).notify(project)
    }

    fun cannotDelete(file: RemoteFileInformation, error: IOException, entity: String) {
        getNotificationGroup()
            .createNotification(
                RemoteFileAccessBundle.message("notification.RemoteFileAccess.cannotDelete.title"),
                RemoteFileAccessBundle.message(
                    "notification.RemoteFileAccess.cannotDelete.content",
                    entity,
                    file.getPath(),
                    error.message ?: ""
                ),
                NotificationType.ERROR
            ).notify(project)
    }

    fun cannotCreateChildFile(newFile: String, error: IOException) {
        getNotificationGroup()
            .createNotification(
                RemoteFileAccessBundle.message("notification.RemoteFileAccess.cannotCreateChildFile.title"),
                RemoteFileAccessBundle.message(
                    "notification.RemoteFileAccess.cannotCreateChildFile.content",
                    newFile,
                    error.message ?: ""
                ),
                NotificationType.ERROR
            ).notify(project)
    }

    fun cannotCreateChildDirectory(newDir: String, ex: IOException) {
        getNotificationGroup()
            .createNotification(
                RemoteFileAccessBundle.message("notification.RemoteFileAccess.cannotCreateChildDirectory.title"),
                RemoteFileAccessBundle.message(
                    "notification.RemoteFileAccess.cannotCreateChildDirectory.content",
                    newDir,
                    ex.message ?: ""
                ),
                NotificationType.ERROR
            ).notify(project)
    }

    fun cannotOpenFile(filePath: String, ex: IOException) {
        getNotificationGroup()
            .createNotification(
                RemoteFileAccessBundle.message(
                    "notification.RemoteFileAccess.cannotOpenFile.title.content",
                    filePath,
                    ex.message ?: ""
                ),
                NotificationType.ERROR
            ).notify(project)
    }

    fun cannotSaveFile(filePath: String) {
        getNotificationGroup()
            .createNotification(
                RemoteFileAccessBundle.message("notification.RemoteFileAccess.cannotSaveFile.content", filePath),
                NotificationType.ERROR
            ).notify(project)
    }

    fun cannotRename(fromPath: String, toPath: String, ex: IOException) {
        getNotificationGroup()
            .createNotification(
                RemoteFileAccessBundle.message(
                    "notification.RemoteFileAccess.cannotRename.content",
                    fromPath,
                    toPath,
                    ex.message ?: ""
                ),
                NotificationType.ERROR
            ).notify(project)
    }

    fun disconnect(reasonName: String, message: String) {
        getNotificationGroup()
            .createNotification(
                RemoteFileAccessBundle.message("notification.RemoteFileAccess.disconnect.content", reasonName, message),
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
