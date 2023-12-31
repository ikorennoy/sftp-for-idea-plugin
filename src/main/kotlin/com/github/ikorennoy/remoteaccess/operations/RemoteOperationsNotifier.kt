package com.github.ikorennoy.remoteaccess.operations

import com.github.ikorennoy.remoteaccess.template.RemoteFileAccessBundle
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

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

    fun cannotLoadChildren(directoryName: String, error: Throwable) {
        getNotificationGroup()
            .createNotification(
                RemoteFileAccessBundle.message(
                    "notification.RemoteFileAccess.cannotLoadChildren.content",
                    directoryName,
                    error.message ?: RemoteFileAccessBundle.unknownReason()
                ),
                NotificationType.ERROR
            ).notify(project)
    }

    fun cannotDelete(file: RemoteFileInformation, error: Throwable, entity: String) {
        getNotificationGroup()
            .createNotification(
                RemoteFileAccessBundle.message("notification.RemoteFileAccess.cannotDelete.title"),
                RemoteFileAccessBundle.message(
                    "notification.RemoteFileAccess.cannotDelete.content",
                    entity,
                    file.getPathFromRemoteRoot(),
                    error.message ?: RemoteFileAccessBundle.unknownReason()
                ),
                NotificationType.ERROR
            ).notify(project)
    }

    fun cannotCreateChildFile(newFile: String, error: Throwable) {
        getNotificationGroup()
            .createNotification(
                RemoteFileAccessBundle.message("notification.RemoteFileAccess.cannotCreateChildFile.title"),
                RemoteFileAccessBundle.message(
                    "notification.RemoteFileAccess.cannotCreateChildFile.content",
                    newFile,
                    error.message ?: RemoteFileAccessBundle.unknownReason()
                ),
                NotificationType.ERROR
            ).notify(project)
    }

    fun cannotCreateChildDirectory(newDir: String, ex: Throwable) {
        getNotificationGroup()
            .createNotification(
                RemoteFileAccessBundle.message("notification.RemoteFileAccess.cannotCreateChildDirectory.title"),
                RemoteFileAccessBundle.message(
                    "notification.RemoteFileAccess.cannotCreateChildDirectory.content",
                    newDir,
                    ex.message ?: RemoteFileAccessBundle.unknownReason()
                ),
                NotificationType.ERROR
            ).notify(project)
    }

    fun cannotOpenFile(filePath: String, ex: Throwable) {
        getNotificationGroup()
            .createNotification(
                RemoteFileAccessBundle.message(
                    "notification.RemoteFileAccess.cannotOpenFile.title.content",
                    filePath,
                    ex.message ?: RemoteFileAccessBundle.unknownReason()
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

    fun cannotFindRoot(root: String, error: Throwable) {
        getNotificationGroup()
            .createNotification(
                RemoteFileAccessBundle.message(
                    "notification.RemoteFileAccess.cannotFindRoot.content",
                    root,
                    error.message ?: RemoteFileAccessBundle.unknownReason()
                ),
                NotificationType.ERROR
            ).notify(project)
    }

    fun cannotSaveFileToRemote(fileName: String, error: Throwable) {
        getNotificationGroup()
            .createNotification(
                RemoteFileAccessBundle.message("notification.RemoteFileAccess.fileUploaded.title"),
                RemoteFileAccessBundle.message(
                    "notification.RemoteFileAccess.cannotUpload.content",
                    fileName,
                    error.message ?: RemoteFileAccessBundle.unknownReason()
                ),
                NotificationType.ERROR
            ).notify(project)
    }

    fun genericDownloadError(error: Throwable) {
        getNotificationGroup()
            .createNotification(
                RemoteFileAccessBundle.message("notification.RemoteFileAccess.genericDownload.name"),
                RemoteFileAccessBundle.message(
                    "notification.RemoteFileAccess.genericDownload.content",
                    error.message ?: RemoteFileAccessBundle.unknownReason()
                ),
                NotificationType.ERROR
            ).notify(project)
    }

    fun genericUploadError(error: Throwable) {
        getNotificationGroup()
            .createNotification(
                RemoteFileAccessBundle.message("notification.RemoteFileAccess.fileUploaded.title"),
                RemoteFileAccessBundle.message(
                    "notification.RemoteFileAccess.genericUpload.content",
                    error.message ?: RemoteFileAccessBundle.unknownReason()
                ),
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
