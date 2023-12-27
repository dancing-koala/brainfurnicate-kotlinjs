package com.cobr.parser

import com.cobr.ProgramError
import kotlin.math.absoluteValue

data class ParserResult(
    val success: Boolean = false,
    val tokens: List<Token>,
    val errors: List<ProgramError.ParserError>
)

private val authorizedChars = listOf(',', '.', '+', '-', '[', ']', '>', '<', '$', '#')

@Suppress("UnnecessaryVariable")
fun parse(rawCode: String): ParserResult {

    val loopIndices = mutableListOf<Int>()
    val tokens = mutableListOf<Token>()
    val loopCounter = LoopCounter()

    val errors = mutableListOf<ProgramError.ParserError>()

    rawCode.toCharArray().forEachIndexed { index, char ->
        if (char !in authorizedChars) {
            errors.add(ProgramError.ParserError("Unknown token", index, char))
            tokens.add(Token.Unknown)
            return@forEachIndexed
        }

        val token = when (char) {
            '+' -> Token.Increment
            '-' -> Token.Decrement

            '[' -> {
                loopCounter.opened++
                loopIndices.add(index)
                Token.OpenLoop(start = index, end = -1)
            }

            ']' -> {
                loopCounter.closed++

                var startIndex = -1
                val endIndex = index

                if (loopIndices.isNotEmpty()) {
                    startIndex = loopIndices.removeLast()

                    val openLoop = tokens[startIndex] as Token.OpenLoop
                    tokens[startIndex] = openLoop.copy(end = endIndex)
                }

                Token.CloseLoop(start = startIndex, end = endIndex)
            }

            '>' -> Token.ShiftRight
            '<' -> Token.ShiftLeft
            '.' -> Token.Print
            ',' -> Token.Prompt

            else -> Token.Debug(char = char)
        }

        tokens.add(token)
    }

    if (!loopCounter.isConsistent()) {
        val (opened, closed) = loopCounter

        val char = if (opened > closed) ']' else '['
        val missingCount = (opened - closed).absoluteValue

        val error = ProgramError.ParserError(
            type = "$missingCount x '$char' missing",
            addr = -1,
            char = char
        )

        errors.add(error)
    }

    return ParserResult(
        success = errors.isEmpty(),
        tokens = tokens,
        errors = errors
    )
}
