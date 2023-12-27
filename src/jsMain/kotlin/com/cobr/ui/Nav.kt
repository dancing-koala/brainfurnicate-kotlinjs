package com.cobr.ui

import com.cobr.ProgramState
import com.cobr.ProgramState.*
import io.kvision.core.Container
import io.kvision.core.onClick
import io.kvision.form.select.selectInput
import io.kvision.html.li
import io.kvision.html.nav
import io.kvision.html.span
import io.kvision.html.ul
import io.kvision.state.ObservableState
import io.kvision.state.bind
import io.kvision.state.stateFlow

fun Container.appNav(
    onRunClick: () -> Unit,
    onInterruptClick: () -> Unit,
    onCodePresetSelected: (id: Int) -> Unit,
    programStateObservable: ObservableState<ProgramState?>
) {
    nav {
        ul(className = "mast") {
            id = "menu-container"

            li(className = "menu-item btn") {
                span(className = "glyphicon glyphicon-play")
            }.onClick { onRunClick() }

            li(className = "menu-item btn") {
                span(className = "glyphicon glyphicon-stop")
            }.onClick { onInterruptClick() }

            li(className = "menu-item btn") {
                id = "program-state"
            }.bind(programStateObservable.stateFlow) {
                val contentValue = when (it) {
                    Idle -> ""
                    Parsing -> "\uFE0F"
                    Running -> "🏃"
                    Error -> "⛔️"
                    Interrupted -> "🛑"
                    Done -> "✅"
                    null -> ""
                }

                span(content = contentValue)
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
                    value?.toIntOrNull()
                        ?.let(onCodePresetSelected)
                }
            }
        }
    }
}