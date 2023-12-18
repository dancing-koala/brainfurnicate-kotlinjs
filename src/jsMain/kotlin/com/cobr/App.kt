package com.cobr

import io.kvision.*
import io.kvision.core.onClick
import io.kvision.form.select.selectInput
import io.kvision.form.text.textAreaInput
import io.kvision.html.*
import io.kvision.i18n.DefaultI18nManager
import io.kvision.i18n.I18n
import io.kvision.panel.root
import io.kvision.state.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class App : Application(), CoroutineScope by CoroutineScope(Dispatchers.Main) {
    init {
        require("css/kvapp.css")
    }

    private val appModel = AppModel()

    override fun start() {
        I18n.manager = DefaultI18nManager(
            mapOf(
                "pl" to require("i18n/messages-pl.json"),
                "en" to require("i18n/messages-en.json")
            )
        )

        val codeInput = ObservableValue("")

        root("kvapp") {
            nav {
                ul(className = "mast") {
                    id = "menu-container"

                    li(className = "menu-item btn") {
                        id = "run-code"
                        span(className = "glyphicon glyphicon-play")
                    }.onClick { appModel.compile(codeInput.value) }

                    li(className = "menu-item btn") {
                        id = "stop-code"
                        span(className = "glyphicon glyphicon-stop")
                    }

                    li(className = "menu-item pull-right") {
                        id = "examples"

                        val options = listOf(
                            "0" to "Fibonacci",
                            "1" to "Quine",
                            "2" to "Bottles",
                            "3" to "Email",
                        )

                        selectInput(options = options, value = "0") {
                            removeCssClass("form-select")
                        }.subscribe { value ->
                            appModel.loadCode(value?.toIntOrNull() ?: 0)
                        }
                    }
                }
            }
            div(className = "mast") {
                id = "ide"

                div(className = "ide-part col-xs-12") {
                    id = "code"

                    div(className = "header") {
                        h3("Code")
                        h5("[ ] < > + - , .")
                        div(content = 0.toString(), className = "extra")
                            .bind(codeInput.stateFlow) {
                                content = it.length.toString()
                            }
                    }

                    textAreaInput {
                        id = "code-input"
                        removeCssClass("form-control")
                    }.bindTo(codeInput.mutableStateFlow)
                        .bind(appModel.uiState) {
                            if (it.embeddedCode != value) {
                                value = it.embeddedCode
                            }
                        }
                }

                div(className = "ide-part col-xs-12 col-md-6") {
                    id = "output"

                    div(className = "header") {
                        h3("Output")
                        h5("A B C D a b c d")
                        div(content = "0s", className = "extra")
                            .bind(appModel.uiState) {
                                content = it.compilerResult.executionDetails.time
                                    .let { time -> time.toDouble() / 1000.0 }
                                    .toString() + "s"
                            }
                    }

                    textAreaInput {
                        removeCssClass("form-control")
                        id = "text"
                        readonly = true
                    }.bind(appModel.uiState) {
                        value = it.compilerResult.output
                    }
                }

                div(className = "ide-part col-xs-12 col-md-6") {
                    id = "output"

                    div(className = "header") {
                        h3("Debug")
                        h5("${'$'} @ ! # ~ ^ \" %")
                        div(content = "0 steps", className = "extra")
                            .bind(appModel.uiState) {
                                content = it.compilerResult.executionDetails.steps
                                    .toString() + " steps"
                            }
                    }

                    div {
                        id = "debug-container"

                        div {
                            id = "debug-table"

                            div(className = "debug-item") {
                                id = "debug-caption"

                                span(content = "Index", className = "debug-col") {
                                    id = "debug-address"
                                }
                                span(content = "Value", className = "debug-col") {
                                    id = "debug-value"
                                }
                                span(content = "Error", className = "debug-col") {
                                    id = "debug-erreur"
                                }
                                span(content = "Character", className = "debug-col") {
                                    id = "debug-character"
                                }
                            }

                            div(className = "debug-item") {

                            }
                        }
                    }
                }
            }
        }
    }
}

val test = """
    <div id="debug" class="ide-part col-xs-12 col-md-6">

            <div id="debug-container">
                <div id="debug-table">
                    <div id="debug-caption" class="debug-item">
                        <span id="debug-address" class="debug-col">Index</span>
                        <span id="debug-value" class="debug-col">Value</span>
                        <span id="debug-erreur" class="debug-col" ng-if="">Error</span>
                        <span id="debug-character" class="debug-col">Character</span>
                    </div>
                    <div ng-repeat="item in debugLog" class="debug-item">
                        <span class="debug-col">{{item.addr}}</span>
                        <span class="debug-col" ng-if="item.val != undefined">{{item.val}}</span>
                        <span class="debug-col" ng-if="item.type != undefined">{{item.type}}</span>
                        <span class="debug-col">{{item.char}}</span>
                    </div>
                </div>
            </div>

        </div>
""".trimIndent()

fun main() {
    startApplication(
        ::App,
        module.hot,
        CoreModule
    )
}
