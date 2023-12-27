package com.cobr.ui

import io.kvision.core.Container
import io.kvision.html.Div
import io.kvision.html.div
import io.kvision.html.h3
import io.kvision.html.h5

fun Container.idePart(
    id: String,
    title: String,
    subtitle: String,
    isFullWidth: Boolean,
    extraValue: String,
    extraSetup: Div.() -> Unit,
    content: Container.() -> Unit
) {

    div(className = "ide-part col-xs-12") {
        this.id = id

        if (!isFullWidth) {
            addCssClass("col-md-6")
        }

        div(className = "header") {
            h3(title)
            h5(subtitle)
            div(content = extraValue, className = "extra").extraSetup()
        }

        content()
    }
}