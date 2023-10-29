package com.github.ikorennoy.remotefileviewer.ui

import com.github.ikorennoy.remotefileviewer.template.FileViewerBundle
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.fields.ExtendableTextComponent
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import kotlinx.coroutines.*
import java.util.concurrent.ThreadLocalRandom
import javax.swing.JComponent
import javax.swing.event.DocumentEvent

class ConnectionConfigurationDialog(project: Project) : DialogWrapper(project) {

    private val usernameField = ExtendableTextField(30)
    private val passwordField = JBPasswordField()

    private val uiDispatcher get() = Dispatchers.EDT + ModalityState.defaultModalityState().asContextElement()
    private val loadingExtension = ExtendableTextComponent.Extension { AnimatedIcon.Default.INSTANCE }

    private val scope = CoroutineScope(SupervisorJob()).also { Disposer.register(disposable) { it.cancel() } }

    private var accessError: ValidationInfo? = null

    init {
        title = FileViewerBundle.message("connection.configuration.dialog.name")
        init()
    }

    val userName: String get() = usernameField.text.orEmpty().trim()
    val password: CharArray get() = passwordField.password


    override fun createCenterPanel(): JComponent = panel {
        row(FileViewerBundle.message("connection.configuration.dialog.username")) {
            cell(usernameField)
                .align(AlignX.FILL)
                .validationOnApply { checkNameNotBlank() ?: accessError }
                .applyToComponent { clearUrlAccessErrorOnTextChanged() }
                .focused()
        }
        row(FileViewerBundle.message("connection.configuration.dialog.password")) {
            cell(passwordField)
                .align(AlignX.FILL)
        }
    }

    override fun doOKAction() {
        scope.launch(uiDispatcher) {
            setLoading(true)
            try {
                accessError = checkAccess()
            } finally {
                setLoading(false)
            }
            if (accessError == null) {
                super.doOKAction()
            } else {
                IdeFocusManager.getGlobalInstance().requestFocus(usernameField, true)
                startTrackingValidation()
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        passwordField.isEnabled = !isLoading

        usernameField.apply { if (isLoading) addExtension(loadingExtension) else removeExtension(loadingExtension) }
        usernameField.isEnabled = !isLoading

        isOKActionEnabled = !isLoading
    }

    private fun checkNameNotBlank(): ValidationInfo? =
        if (userName.isNotEmpty()) null
        else ValidationInfo(FileViewerBundle.message("connection.configuration.dialog.username.empty.validation"), usernameField)

    private suspend fun checkAccess(): ValidationInfo? {
        val result = test()

        if (result) return null

        return ValidationInfo(FileViewerBundle.message("connection.configuration.dialog.cannot.connect.validation"), usernameField).withOKEnabled()
    }

    private suspend fun test(): Boolean {
        withContext(Dispatchers.IO) {
            runBlocking { delay(3000) }
        }
        return ThreadLocalRandom.current().nextBoolean()
    }

    private fun JBTextField.clearUrlAccessErrorOnTextChanged() =
        document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                accessError = null
            }
        })
}