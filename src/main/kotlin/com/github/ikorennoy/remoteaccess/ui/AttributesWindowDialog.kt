package com.github.ikorennoy.remoteaccess.ui

import com.github.ikorennoy.remoteaccess.notifyConnectionStatusChanged
import com.github.ikorennoy.remoteaccess.operations.RemoteFileInformation
import com.github.ikorennoy.remoteaccess.operations.RemoteOperations
import com.intellij.collaboration.async.CompletableFutureUtil.handleOnEdt
import com.intellij.collaboration.async.CompletableFutureUtil.submitIOTask
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import net.schmizz.sshj.sftp.FileAttributes
import net.schmizz.sshj.xfer.FilePermission
import javax.swing.JComponent
import javax.swing.JPanel

// todo convert timestamps to human readable
//  convert size to human readable
class AttributesWindowDialog(
    private val project: Project,
    private val selectedFile: RemoteFileInformation,
) : DialogWrapper(project, true) {

    private val panel: JPanel

    private val readByOwner: JBCheckBox = JBCheckBox("Read by owner", false)
    private val writeByOwner: JBCheckBox = JBCheckBox("Write by owner", false)
    private val executeByOwner: JBCheckBox = JBCheckBox("Execute by owner", false)

    private val readByGroup: JBCheckBox = JBCheckBox("Read by group", false)
    private val writeByGroup: JBCheckBox = JBCheckBox("Write by group", false)
    private val executeByGroup: JBCheckBox = JBCheckBox("Execute by owner", false)

    private val readByOthers: JBCheckBox = JBCheckBox("Read by others", false)
    private val writeByOthers: JBCheckBox = JBCheckBox("Write by others", false)
    private val executeByOthers: JBCheckBox = JBCheckBox("Execute by others", false)

    private val userId: JBLabel = JBLabel()
    private val groupId: JBLabel = JBLabel()
    private val mTime: JBLabel = JBLabel()
    private val aTime: JBLabel = JBLabel()
    private val size: JBLabel = JBLabel()
    private val type: JBLabel = JBLabel()

    init {
        initPermissionsCheckboxes()
        initFileInformationLabels()
        panel = initPanel()
        title = "Edit File Attributes"
        init()
    }


    override fun createCenterPanel(): JComponent {
        return panel
    }

    override fun doOKAction() {
        val oldAttributes = selectedFile.getAttributes()

        val newPermissions = getPermissionsFromCheckboxes()
        if (oldAttributes.permissions != newPermissions) {
            val newAttributes = FileAttributes.Builder()
                .withUIDGID(oldAttributes.uid, oldAttributes.gid)
                .withAtimeMtime(oldAttributes.atime, oldAttributes.mtime)
                .withSize(oldAttributes.size)
                .withType(oldAttributes.type)
                .withPermissions(getPermissionsFromCheckboxes())
                .build()

            ProgressManager.getInstance().submitIOTask(EmptyProgressIndicator()) {
                val remoteOperations = RemoteOperations.getInstance(project)
                remoteOperations.updateAttributes(selectedFile, newAttributes)
            }.handleOnEdt(ModalityState.defaultModalityState()) { _, _ ->
                notifyConnectionStatusChanged()
            }
        }
        super.doOKAction()
    }

    private fun initPanel(): JPanel {
        return panel {
            group("Permissions") {
                row {
                    cell(readByOwner)
                    cell(readByGroup)
                    cell(readByOthers)
                }.layout(RowLayout.PARENT_GRID)
                row {
                    cell(writeByOwner)
                    cell(writeByGroup)
                    cell(writeByOthers)
                }.layout(RowLayout.PARENT_GRID)
                row {
                    cell(executeByOwner)
                    cell(executeByGroup)
                    cell(executeByOthers)
                }.layout(RowLayout.PARENT_GRID)
            }
            group("File Information") {
                row {
                    label("User ID: ")
                    cell(userId)
                    label("Access time: ")
                    cell(aTime)
                }.layout(RowLayout.PARENT_GRID)
                row {
                    label("Group ID: ")
                    cell(groupId)
                    label("Size: ")
                    cell(size)
                }.layout(RowLayout.PARENT_GRID)
                row {
                    label("Modification time: ")
                    cell(mTime)
                    label("File type: ")
                    cell(type)
                }.layout(RowLayout.PARENT_GRID)
            }
        }
    }

    private fun initFileInformationLabels() {
        val fileAttrs = selectedFile.getAttributes()
        userId.text = fileAttrs.uid.toString()
        groupId.text = fileAttrs.gid.toString()
        mTime.text = fileAttrs.mtime.toString()
        aTime.text = fileAttrs.atime.toString()
        size.text = fileAttrs.size.toString()
        type.text = fileAttrs.type.name
    }

    private fun initPermissionsCheckboxes() {
        val filePermissions = selectedFile.getAttributes().permissions

        // owner
        if (filePermissions.contains(FilePermission.USR_R)) {
            readByOwner.isSelected = true
        }
        if (filePermissions.contains(FilePermission.USR_W)) {
            writeByOwner.isSelected = true
        }
        if (filePermissions.contains(FilePermission.USR_X)) {
            executeByOwner.isSelected = true
        }

        // group
        if (filePermissions.contains(FilePermission.GRP_R)) {
            readByGroup.isSelected = true
        }
        if (filePermissions.contains(FilePermission.GRP_W)) {
            writeByGroup.isSelected = true
        }
        if (filePermissions.contains(FilePermission.GRP_X)) {
            executeByGroup.isSelected = true
        }

        // others
        if (filePermissions.contains(FilePermission.OTH_R)) {
            executeByOthers.isSelected = true
        }
        if (filePermissions.contains(FilePermission.OTH_W)) {
            executeByOthers.isSelected = true
        }
        if (filePermissions.contains(FilePermission.OTH_X)) {
            executeByOthers.isSelected = true
        }
    }

    private fun getPermissionsFromCheckboxes(): Set<FilePermission> {
        val result: MutableSet<FilePermission> = HashSet()

        // owner
        if (readByOwner.isSelected) {
            result.add(FilePermission.USR_R)
        }
        if (writeByOwner.isSelected) {
            result.add(FilePermission.USR_W)
        }
        if (executeByOwner.isSelected) {
            result.add(FilePermission.USR_X)
        }

        // group
        if (readByGroup.isSelected) {
            result.add(FilePermission.GRP_R)
        }
        if (writeByGroup.isSelected) {
            result.add(FilePermission.GRP_W)
        }
        if (executeByGroup.isSelected) {
            result.add(FilePermission.GRP_X)
        }

        // others
        if (readByOthers.isSelected) {
            result.add(FilePermission.OTH_R)
        }
        if (writeByOthers.isSelected) {
            result.add(FilePermission.OTH_W)
        }
        if (executeByOthers.isSelected) {
            result.add(FilePermission.OTH_X)
        }
        return result
    }
}
