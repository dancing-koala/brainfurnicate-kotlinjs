package com.cobr

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppUiState(
    val embeddedCode: String = "",
    val compilerRunning: Boolean = false,
    val compilerResult: CompilerResult = CompilerResult()
)

class AppModel {

    private val scope = CoroutineScope(Dispatchers.Default)

    private val mutableUiState = MutableStateFlow(AppUiState())
    val uiState = mutableUiState.asStateFlow()

    private val compiler = Compiler()

    fun loadCode(index: Int) {
        println("index = [${index}]")
        mutableUiState.update {
            it.copy(embeddedCode = embeddedCodeEntries[index])
        }
    }

    fun compile(rawCode: String) {
        scope.launch {
            compiler.interrupt()

            mutableUiState.update {
                it.copy(
                    compilerRunning = true,
                    embeddedCode = rawCode
                )
            }

            val compilerResult = compiler.run(rawCode).also {
                println("compilerResult = ${it}")
            }

            mutableUiState.update {
                it.copy(
                    compilerRunning = false,
                    compilerResult = compilerResult
                )
            }
        }
    }

    fun interruptCompilation() {
        compiler.interrupt()
    }
}