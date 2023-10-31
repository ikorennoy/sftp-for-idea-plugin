package com.github.ikorennoy.remotefileviewer.remoteEdit

import com.github.ikorennoy.remotefileviewer.filesystem.SftpFileSystem
import com.github.ikorennoy.remotefileviewer.filesystem.SftpVirtualFile
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.fileChooser.ex.FileSystemTreeImpl
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.util.ui.UIUtil
import java.io.ByteArrayOutputStream


class EditRemoteFileTask(
    private val project: Project,
    private val fs: SftpFileSystem,
    private val tree: FileSystemTreeImpl,
) : Runnable {


    override fun run() {
        val result = MessageDialogBuilder.yesNo("Edit", "Do you want to edit selected file?")
            .yesText("Edit")
            .icon(UIUtil.getQuestionIcon())
            .ask(project)

        if (!result) return

        CommandProcessor.getInstance().executeCommand(project, {
            object : Task.Modal(project, "Downloading File", true) {

                override fun run(indicator: ProgressIndicator) {

                    val selectedFile = tree.selectedFile as? SftpVirtualFile ?: return
                    val fileSize = selectedFile.length.toDouble()
                    val buffer = ByteArray(10)
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    selectedFile.inputStream.use {
                        var read = it.read(buffer)
                        var readTotal = read
                        indicator.text = selectedFile.path
                        while (read != -1) {
                            byteArrayOutputStream.writeBytes(buffer)
                            indicator.checkCanceled()
                            indicator.fraction = readTotal / fileSize
                            read = it.read(buffer)
                            readTotal += read
                        }
                    }

                    ApplicationManager.getApplication().invokeLater {
                        val localFs = LocalFileSystem.getInstance()
                        val localFileProjection = localFs.createNewFile(selectedFile, byteArrayOutputStream.toByteArray())
                        OpenFileDescriptor(project, localFileProjection).navigateInEditor(project, true)
                    }

                }
            }.queue()
        }, "Downloading File", null)
    }
}