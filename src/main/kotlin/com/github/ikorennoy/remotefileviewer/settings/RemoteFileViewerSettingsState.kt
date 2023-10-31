package com.github.ikorennoy.remotefileviewer.settings

import com.intellij.openapi.components.*
import com.intellij.util.xmlb.annotations.Transient

@Service
@State(
    name = "com.github.ikorennoy.remotefileviewer.settings.RemoteFileViewerSettingsState",
    storages = [Storage("RemoteFileViewerPlugin.xml")]
)
class RemoteFileViewerSettingsState :
    SimplePersistentStateComponent<RemoteFileViewerSettingsState.ConfigurationState>(ConfigurationState()) {

    var host: String
        get() = state.host ?: ""
        set(value) {
            state.host = value
        }

    var port: Int
        get() = state.port
        set(value) {
            state.port = value
        }

    var root: String
        get() = state.root ?: ""
        set(value) {
            state.root = value
        }

    var username: String
        get() = state.username ?: ""
        set(value) {
            state.username = value
        }

    var password: CharArray
        get() = state.password
        set(value) {
            state.password = value
        }

    class ConfigurationState : BaseState() {
        var host by string()
        var port: Int by property(22)
        var username by string()
        var root by string()

        @Transient
        var password: CharArray = CharArray(0)
    }
}
