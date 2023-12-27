package com.cobr

import com.cobr.ui.appNav
import com.cobr.ui.idePart
import io.kvision.*
import io.kvision.form.text.textAreaInput
import io.kvision.html.div
import io.kvision.html.span
import io.kvision.panel.root
import io.kvision.state.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map

class App : Application(), CoroutineScope by CoroutineScope(Dispatchers.Main) {
    init {
        require("css/kvapp.css")
    }

    private val appModel by lazy { AppModel() }

    override fun start() {
        val codeInput = ObservableValue("")
        val programStateObservable = appModel.uiState.map { it.programState }.observableState

        root("kvapp") {
            appNav(
                onRunClick = { appModel.parseAndRun(codeInput.value) },
                onInterruptClick = appModel::interrupt,
                onCodePresetSelected = appModel::loadCodePreset,
                programStateObservable = programStateObservable
            )

            div(className = "mast") {
                id = "ide"

                idePart(
                    id = "code",
                    title = "Code",
                    subtitle = "[ ] < > + - , .",
                    isFullWidth = true,
                    extraValue = "0",
                    extraSetup = {
                        bind(codeInput.stateFlow) {
                            content = it.length.toString()
                        }
                    }
                ) {
                    textAreaInput {
                        id = "code-input"
                        removeCssClass("form-control")
                    }.bindTo(codeInput.mutableStateFlow)
                        .bind(appModel.uiState) {
                            if (it.currentCode != value) {
                                value = it.currentCode
                            }
                        }
                }

                idePart(
                    "output",
                    "Output",
                    "A B C D a b c d",
                    false,
                    extraValue = "0ms",
                    extraSetup = {
                        bind(appModel.uiState) {
                            content = "${it.programResult.stats.time}ms"
                        }
                    }
                ) {
                    textAreaInput {
                        removeCssClass("form-control")
                        id = "text"
                        readonly = true
                    }.bind(appModel.uiState) { appUiState ->
                        value = if (appUiState.programResult.errors.isEmpty()) {
                            appUiState.programResult.output
                        } else {
                            val errors = appUiState.programResult.errors.joinToString(separator = "\n") { it.description }

                            "Errors:\n$errors"
                        }
                    }
                }

                idePart(
                    "debug",
                    "Debug",
                    "${'$'} @ ! # ~ ^ \" %",
                    false,
                    extraValue = "0 step",
                    extraSetup = {
                        bind(appModel.uiState) {
                            content = "${it.programResult.stats.steps} steps"
                        }
                    }
                ) {
                    div {
                        id = "debug-container"

                        div { id = "debug-table" }.bind(appModel.uiState) {
                            div(className = "debug-item") {
                                id = "debug-caption"

                                span(content = "Index", className = "debug-col") {
                                    id = "debug-address"
                                }
                                span(content = "Value", className = "debug-col") {
                                    id = "debug-value"
                                }
                                span(content = "Character", className = "debug-col") {
                                    id = "debug-character"
                                }
                            }

                            it.programResult.snapshots.forEach {
                                div(className = "debug-item") {
                                    span(content = it.address.toString(), className = "debug-col")
                                    span(content = it.value.toString(), className = "debug-col")
                                    span(content = Char(it.value).toString(), className = "debug-col")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun main() {
    startApplication(
        ::App,
        module.hot,
        CoreModule
    )
}
