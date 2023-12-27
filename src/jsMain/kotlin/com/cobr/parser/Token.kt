package com.cobr.parser

sealed interface Token {
    data object Unknown : Token
    data object Increment : Token
    data object Decrement : Token
    data class OpenLoop(val start: Int, val end: Int) : Token
    data class CloseLoop(val start: Int, val end: Int) : Token
    data object ShiftRight : Token
    data object ShiftLeft : Token
    data object Print : Token
    data object Prompt : Token
    data class Debug(val char: Char) : Token
}