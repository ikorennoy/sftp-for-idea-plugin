package com.github.ikorennoy.remotefileviewer.remoteEdit

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.ui.EditorNotificationPanel
import com.intellij.util.ui.JBUI
import java.awt.Dimension
import java.awt.FlowLayout

class ProjectionFileEditorPanel(
    private val file: LocalVirtualFile,
    private val editor: FileEditor
) : EditorNotificationPanel(editor, getToolbarBackground(), null, Status.Info) {


    init {
        myLinksPanel.layout = FlowLayout()
        val saveAction = ActionManager.getInstance().getAction("RemoteFileSystem.UploadCurrentFileProjection")
        saveAction.registerCustomShortcutSet(ActionManager.getInstance().getAction("SaveAll").shortcutSet, editor.component)
        myLinksPanel.add(
            createButton(
                saveAction
            )
        )
    }

    private fun createButton(action: AnAction): ActionButton {
        val icon = action.templatePresentation.icon
        val width = icon.iconWidth
        val height = icon.iconHeight
        val button = ActionButton(action, null, "unknown", Dimension(width, height))
        button.setIconInsets(JBUI.insets(0, 3))
        return button
    }
}
