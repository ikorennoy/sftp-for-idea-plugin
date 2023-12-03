package com.github.ikorennoy.remoteaccess.edit

import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentSynchronizationVetoer

class LocalTempFileSaveVetoer: FileDocumentSynchronizationVetoer() {

    override fun maySaveDocument(document: Document, isSaveExplicit: Boolean): Boolean {
        return isSaveExplicit
    }
}
