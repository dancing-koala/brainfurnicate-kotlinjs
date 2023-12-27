package com.cobr

import com.cobr.parser.Token
import com.cobr.parser.parse
import com.cobr.runtime.InterpreterResult
import com.cobr.runtime.InterpreterSnapshot
import com.cobr.runtime.Stats
import com.cobr.runtime.interpret
import kotlinx.browser.window
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class ProgramState {
    Idle, Parsing, Running, Error, Interrupted, Done
}

data class AppUiState(
    val currentCode: String = "",
    val programState: ProgramState = ProgramState.Idle,
    val programResult: ProgramResult = ProgramResult(),
)

sealed interface ProgramError {
    val description: String

    data class ParserError(val type: String, val addr: Int, val char: Char) : ProgramError {
        override val description: String = "--> $type at [$addr] <${char}>"
    }

    data class InterpreterError(val type: String, val addr: Int, val token: Token) : ProgramError {
        override val description: String = "--> $type at [$addr] <${token}>"
    }

    data class UnknownError(val exception: Exception) : ProgramError {
        override val description: String = exception.message ?: "Unknown error"
    }
}

data class ProgramResult(
    val output: String = "",
    val stats: Stats = Stats(),
    val errors: List<ProgramError> = emptyList(),
    val snapshots: List<InterpreterSnapshot> = emptyList()
)

class AppModel {
    private val scope = CoroutineScope(window.asCoroutineDispatcher())

    private val mutableUiState = MutableStateFlow(AppUiState())
    val uiState = mutableUiState.asStateFlow()

    private var parseAndRunJob: Job? = null

    fun loadCodePreset(index: Int) {
        mutableUiState.update {
            it.copy(currentCode = presets[index])
        }
    }

    private fun setErrorState(errors: List<ProgramError>) {
        mutableUiState.update {
            it.copy(
                programState = ProgramState.Error,
                programResult = ProgramResult(errors = errors)
            )
        }
    }

    private fun setParsingState(code: String) {
        mutableUiState.update {
            it.copy(
                programState = ProgramState.Parsing,
                currentCode = code
            )
        }
    }

    private fun setRunningState() {
        mutableUiState.update {
            it.copy(programState = ProgramState.Running)
        }
    }

    private fun setDoneState(runtimeResult: InterpreterResult) {
        mutableUiState.update {
            it.copy(
                programState = ProgramState.Done,
                programResult = ProgramResult(
                    output = runtimeResult.output,
                    stats = Stats(
                        steps = runtimeResult.steps,
                        time = runtimeResult.executionTime
                    ),
                    snapshots = runtimeResult.snapshots
                )
            )
        }
    }

    fun parseAndRun(rawCode: String) {
        parseAndRunJob?.cancel()

        parseAndRunJob = scope.launch {
            try {
                setParsingState(rawCode)

                val parserResult = parse(rawCode)

                yield()

                if (!parserResult.success) {
                    setErrorState(parserResult.errors)
                    return@launch
                }

                setRunningState()

                val interpreterResult = interpret(parserResult.tokens)

                yield()

                setDoneState(interpreterResult)
            } catch (e: Exception) {
                setErrorState(errors = listOf(ProgramError.UnknownError(e)))
            }
        }
    }

    fun interrupt() {
        if (uiState.value.programState !in listOf(ProgramState.Running, ProgramState.Parsing)) {
            return
        }

        scope.launch {
            parseAndRunJob?.cancelAndJoin()
            parseAndRunJob = null

            mutableUiState.update {
                it.copy(programState = ProgramState.Interrupted)
            }
        }
    }
}