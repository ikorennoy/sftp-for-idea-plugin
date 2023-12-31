package com.github.ikorennoy.remoteaccess.ui

import com.github.ikorennoy.remoteaccess.convertBytesToHumanReadable
import com.github.ikorennoy.remoteaccess.notifyUpdateFullTree
import com.github.ikorennoy.remoteaccess.notifyUpdateNode
import com.github.ikorennoy.remoteaccess.operations.RemoteFileInformation
import com.github.ikorennoy.remoteaccess.operations.RemoteOperations
import com.github.ikorennoy.remoteaccess.template.RemoteFileAccessBundle
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
import com.intellij.util.text.DateFormatUtil
import net.schmizz.sshj.sftp.FileAttributes
import net.schmizz.sshj.xfer.FilePermission
import javax.swing.JComponent
import javax.swing.JPanel

class AttributesWindowDialog(
    private val project: Project,
    private val selectedFile: RemoteFileInformation,
) : DialogWrapper(project, true) {

    private val panel: JPanel

    private val readByOwner: JBCheckBox =
        JBCheckBox(RemoteFileAccessBundle.message("dialog.RemoteFileAccess.attributes.checkbox.readByOwner"), false)
    private val writeByOwner: JBCheckBox =
        JBCheckBox(RemoteFileAccessBundle.message("dialog.RemoteFileAccess.attributes.checkbox.writeByOwner"), false)
    private val executeByOwner: JBCheckBox =
        JBCheckBox(RemoteFileAccessBundle.message("dialog.RemoteFileAccess.attributes.checkbox.executeByOwner"), false)

    private val readByGroup: JBCheckBox =
        JBCheckBox(RemoteFileAccessBundle.message("dialog.RemoteFileAccess.attributes.checkbox.readByGroup"), false)
    private val writeByGroup: JBCheckBox =
        JBCheckBox(RemoteFileAccessBundle.message("dialog.RemoteFileAccess.attributes.checkbox.writeByGroup"), false)
    private val executeByGroup: JBCheckBox =
        JBCheckBox(RemoteFileAccessBundle.message("dialog.RemoteFileAccess.attributes.checkbox.executeByGroup"), false)

    private val readByOthers: JBCheckBox =
        JBCheckBox(RemoteFileAccessBundle.message("dialog.RemoteFileAccess.attributes.checkbox.readByOthers"), false)
    private val writeByOthers: JBCheckBox =
        JBCheckBox(RemoteFileAccessBundle.message("dialog.RemoteFileAccess.attributes.checkbox.writeByOthers"), false)
    private val executeByOthers: JBCheckBox =
        JBCheckBox(RemoteFileAccessBundle.message("dialog.RemoteFileAccess.attributes.checkbox.executeByOthers"), false)

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
        title = RemoteFileAccessBundle.message("dialog.RemoteFileAccess.attributes.name")
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
                // we need to load parent in IO thread,
                // so we can access it in EDT thread without any IO operations
                selectedFile.getParent()
            }.handleOnEdt(ModalityState.defaultModalityState()) { _, _ ->
                val parent = selectedFile.getParent()
                if (parent != null) {
                    notifyUpdateNode(parent)
                } else {
                    notifyUpdateFullTree()
                }
            }
        }
        super.doOKAction()
    }

    private fun initPanel(): JPanel {
        return panel {
            group(RemoteFileAccessBundle.message("dialog.RemoteFileAccess.attributes.group.permissions.name")) {
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
            group(RemoteFileAccessBundle.message("dialog.RemoteFileAccess.attributes.group.fileInfo.name")) {
                row {
                    label(RemoteFileAccessBundle.message("dialog.RemoteFileAccess.attributes.permissions.userId.label"))
                    cell(userId)
                    label(RemoteFileAccessBundle.message("dialog.RemoteFileAccess.attributes.permissions.accessTime.label"))
                    cell(aTime)
                }.layout(RowLayout.PARENT_GRID)
                row {
                    label(RemoteFileAccessBundle.message("dialog.RemoteFileAccess.attributes.permissions.groupId.label"))
                    cell(groupId)
                    label(RemoteFileAccessBundle.message("dialog.RemoteFileAccess.attributes.permissions.size.label"))
                    cell(size)
                }.layout(RowLayout.PARENT_GRID)
                row {
                    label(RemoteFileAccessBundle.message("dialog.RemoteFileAccess.attributes.permissions.modificationTime.label"))
                    cell(mTime)
                    label(RemoteFileAccessBundle.message("dialog.RemoteFileAccess.attributes.permissions.fileType.label"))
                    cell(type)
                }.layout(RowLayout.PARENT_GRID)
            }
        }
    }

    private fun initFileInformationLabels() {
        val fileAttrs = selectedFile.getAttributes()
        userId.text = fileAttrs.uid.toString()
        groupId.text = fileAttrs.gid.toString()
        mTime.text = DateFormatUtil.formatDate(fileAttrs.mtime * 1000)
        aTime.text = DateFormatUtil.formatDate(fileAttrs.atime * 1000)
        size.text = convertBytesToHumanReadable(fileAttrs.size)
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
