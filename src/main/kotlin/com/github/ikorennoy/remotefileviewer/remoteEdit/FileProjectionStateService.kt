package com.github.ikorennoy.remotefileviewer.remoteEdit

import com.github.ikorennoy.remotefileviewer.filesystem.RemoteFileSystem
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.components.Service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project

@Service
class FileProjectionStateService {

    fun uploadFileToRemote(project: Project, localFileProjection: LocalVirtualFile) {
        val remoteFile = localFileProjection.remoteFile

        CommandProcessor.getInstance().executeCommand(project, {
            object : Task.Modal(project, "Uploading File", true) {
                override fun run(indicator: ProgressIndicator) {
                    if (remoteFile.isWritable) {
                        val fs = remoteFile.fileSystem as RemoteFileSystem
                        // open a temp file and upload new content into it
                        val remoteTempFile = fs.openTempFile(remoteFile)
                        val size = localFileProjection.length.toDouble()
                        val buffer = ByteArray(1024)
                        remoteTempFile.use { remoteFileOs ->
                            localFileProjection.inputStream.use { localFileIs ->
                                var writtenTotal = 0.0
                                var readFromLocal = localFileIs.read(buffer)
                                while (readFromLocal != -1) {
                                    indicator.checkCanceled()
                                    remoteFileOs.write(buffer, 0, readFromLocal)
                                    writtenTotal += readFromLocal
                                    indicator.fraction = writtenTotal / size
                                    readFromLocal = localFileIs.read(buffer)
                                }
                            }
                        }
                        // because most of sftp implementations don't support atomic rename
                        // we have to remove the original file and then do rename
                        fs.removeFile(remoteFile)
                        fs.renameTempFile(remoteFile)

                    }
                }
            }.queue()
        }, "Uploading File", null)
    }
}