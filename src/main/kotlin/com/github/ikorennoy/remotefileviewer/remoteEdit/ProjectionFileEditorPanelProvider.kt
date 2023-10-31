package com.github.ikorennoy.remotefileviewer.remoteEdit

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationProvider
import java.util.function.Function
import javax.swing.JComponent

class ProjectionFileEditorPanelProvider : EditorNotificationProvider {

    override fun collectNotificationData(
        project: Project,
        file: VirtualFile
    ): Function<in FileEditor, out JComponent?> {
         return Function<FileEditor, JComponent?> {
             if (file is LocalVirtualFile) {
                 ProjectionFileEditorPanel(file, it)
             } else {
                 null
             }
         }
    }
}