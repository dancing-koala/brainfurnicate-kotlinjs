package com.cobr.parser

data class LoopCounter(
    var opened: Int = 0,
    var closed: Int = 0
) {
    fun isConsistent() = opened == closed
}