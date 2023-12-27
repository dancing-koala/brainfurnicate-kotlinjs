package com.cobr.runtime

import com.cobr.ProgramError
import com.cobr.parser.Token
import kotlinx.browser.window
import kotlinx.coroutines.yield
import kotlin.js.Date

data class InterpreterResult(
    val success: Boolean = false,
    val output: String = "",
    val errors: List<ProgramError.InterpreterError> = emptyList(),
    val steps: Int = 0,
    val executionTime: Long = 0L,
    val snapshots: List<InterpreterSnapshot> = emptyList()
)

private class Interpreter {
    private val buffer = IntArray(256) { 0 }
    private val outputBuilder = StringBuilder()
    private val snapshots = mutableListOf<InterpreterSnapshot>()

    private var steps: Int = 0
    private var currentTokenIndex = 0
    private var bufferAddress = 0

    private var startTime: Long = 0L

    suspend fun run(tokens: List<Token>): InterpreterResult {
        if (startTime == 0L) {
            startTime = Date.now().toLong()
        }

        var keepGoing = (currentTokenIndex < tokens.size)
        var stepsLeftBeforeOverflow = 200000000

        while (keepGoing && stepsLeftBeforeOverflow > 0) {
            steps++
            process(tokens[currentTokenIndex])
            keepGoing = (currentTokenIndex < tokens.size)

            stepsLeftBeforeOverflow--

            if (stepsLeftBeforeOverflow % 20 == 0) {
                yield()
            }
        }

        val errors = if (stepsLeftBeforeOverflow < 0) {
            listOf(ProgramError.InterpreterError("Steps overflow", currentTokenIndex, tokens[currentTokenIndex]))
        } else {
            emptyList()
        }

        val executionTime = Date.now().toLong() - startTime

        return InterpreterResult(
            success = errors.isEmpty(),
            output = outputBuilder.toString(),
            errors = errors,
            steps = steps,
            executionTime = executionTime,
            snapshots = snapshots
        )
    }

    private fun process(token: Token) {
        when (token) {
            Token.Increment -> {
                buffer[bufferAddress] = buffer[bufferAddress] + 1 and 0xff
                currentTokenIndex++
            }

            Token.Decrement -> {
                if (buffer[bufferAddress] == undefined) {
                    buffer[bufferAddress] = 0
                }
                buffer[bufferAddress] = buffer[bufferAddress] - 1 and 0xff
                currentTokenIndex++
            }

            is Token.OpenLoop -> {
                if (buffer[bufferAddress] == 0) {
                    currentTokenIndex = token.end
                }

                currentTokenIndex++
            }

            is Token.CloseLoop -> {
                if (buffer[bufferAddress] != 0) {
                    currentTokenIndex = token.start
                }

                currentTokenIndex++
            }

            Token.ShiftRight -> {
                bufferAddress = bufferAddress + 1 and 0xfff
                currentTokenIndex++
            }

            Token.ShiftLeft -> {
                bufferAddress = bufferAddress - 1 and 0xfff
                currentTokenIndex++
            }

            Token.Print -> {
                outputBuilder.append(Char(buffer[bufferAddress]))
                currentTokenIndex++
            }

            Token.Prompt -> {
                val userInput = window.prompt("Please enter a character:")
                if (userInput != null) {
                    buffer[bufferAddress] = userInput[0].code
                }

                currentTokenIndex++
            }

            is Token.Debug -> {
                logDebug()
                currentTokenIndex++
            }

            else -> Unit
        }
    }

    private fun logDebug() {
        val infos = InterpreterSnapshot(
            address = bufferAddress,
            value = buffer[bufferAddress],
            character = Char(buffer[bufferAddress])
        )

        if (snapshots.size > 511) {
            snapshots.removeFirst()
        }

        snapshots.add(infos)
    }
}

suspend fun interpret(tokens: List<Token>) = Interpreter().run(tokens)