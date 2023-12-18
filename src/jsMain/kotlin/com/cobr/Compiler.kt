package com.cobr

import kotlinx.coroutines.yield
import kotlin.js.Date

data class DebugInfo(
    val address: Int,
    val value: Int,
    val character: Char
)

data class ExecutionDetails(
    val steps: Int = 0,
    val time: Long = 0L
)

sealed interface Instruction {
    data object Increment : Instruction
    data object Decrement : Instruction
    data class OpenLoop(val start: Int, val end: Int) : Instruction
    data class CloseLoop(val start: Int, val end: Int) : Instruction
    data object ShiftRight : Instruction
    data object ShiftLeft : Instruction
    data object Print : Instruction
    data object Prompt : Instruction
    data class Debug(val char: Char) : Instruction
}

data class LoopData(
    var opened: Int = 0,
    var closed: Int = 0
)

data class CompilerError(
    val type: String,
    val addr: Int,
    val char: Char
)

data class CompilerResult(
    val output: String = "",
    val executionDetails: ExecutionDetails = ExecutionDetails(),
    val errors: List<CompilerError> = emptyList()
)

class Compiler {

    private val authorizedChars = listOf(',', '.', '+', '-', '[', ']', '>', '<', '$', '#')

    private var parsedInstructions: List<Instruction> = listOf()
    private var currentInstr: Int = 0
    private var nbSteps: Int = 0

    private var buffer = IntArray(256) { 0 }
    private var addr = 0
    private var output = ""
    private var debug = mutableListOf<DebugInfo>()
    private var startTime = 0L
    private var executionTime = 0L

    suspend fun run(code: String): CompilerResult {
        reset()

        val errors = getErrors(code)

        if (errors.isNotEmpty()) {
            return CompilerResult(
                output = output,
                executionDetails = getDetails(),
                errors = errors
            )
        }

        parsedInstructions = parse(code)
        execute()

        return CompilerResult(
            output = output,
            executionDetails = getDetails(),
            errors = emptyList()
        )
    }

    private fun reset() {
        debug.clear()
        buffer = IntArray(256) { 0 }
        startTime = 0
        addr = 0
        executionTime = 0
        nbSteps = 0
        currentInstr = 0
        output = ""
    }

    private fun parse(code: String): List<Instruction> {
        val loopIndices = mutableListOf<Int>()
        val instructions = mutableListOf<Instruction>()

        code.toCharArray().forEachIndexed { index, char ->
            val instruction = when (char) {
                '+' -> Instruction.Increment
                '-' -> Instruction.Decrement

                '[' -> {
                    loopIndices.add(index)
                    Instruction.OpenLoop(start = index, end = -1)
                }

                ']' -> {
                    val startIndex = loopIndices.removeLast()
                    val endIndex = index

                    val openLoop = instructions[startIndex] as Instruction.OpenLoop
                    instructions[startIndex] = openLoop.copy(end = endIndex)

                    Instruction.CloseLoop(start = startIndex, end = endIndex)
                }

                '>' -> Instruction.ShiftRight
                '<' -> Instruction.ShiftLeft
                '.' -> Instruction.Print
                ',' -> Instruction.Prompt

                else -> Instruction.Debug(char = char)
            }

            instructions.add(instruction)
        }

        instructions.forEach {
            println(it)
        }

        return instructions
    }

    private fun process(): Boolean {
        when (val instruction = parsedInstructions[currentInstr]) {
            Instruction.Increment -> {
                buffer[addr] = buffer[addr] + 1 and 0xff
                currentInstr++
            }

            Instruction.Decrement -> {
                if (buffer[addr] == undefined) {
                    buffer[addr] = 0
                }
                buffer[addr] = buffer[addr] - 1 and 0xff
                currentInstr++
            }

            is Instruction.OpenLoop -> {
                if (buffer[addr] == 0) {
                    currentInstr = instruction.end
                }

                currentInstr++
            }

            is Instruction.CloseLoop -> {
                if (buffer[addr] != 0) {
                    currentInstr = instruction.start
                }

                currentInstr++
            }

            Instruction.ShiftRight -> {
                addr = addr + 1 and 0xfff
                currentInstr++
            }

            Instruction.ShiftLeft -> {
                addr = addr - 1 and 0xfff
                currentInstr++
            }

            Instruction.Print -> {
                output += Char(buffer[addr])
                currentInstr++
            }

            Instruction.Prompt -> {
                val userInput = "" //prompt("Please enter a character:")
                if (userInput != null) {
                    buffer[addr] = userInput[0].code
                }

                currentInstr++
            }

            is Instruction.Debug -> {
                logDebug()
                currentInstr++
            }
        }

        return currentInstr < parsedInstructions.size
    }

    private fun logDebug() {
        val infos = DebugInfo(
            address = addr,
            value = buffer[addr],
            character = Char(buffer[addr])
        )

        if (debug.size > 511) {
            debug.removeFirst()
        }

        debug.add(infos)
    }

    private suspend fun execute() {
        if (startTime == 0L) {
            startTime = Date.now().toLong()
        }

        var keepGoing = (currentInstr < parsedInstructions.size)
        var i = 200000000

        while (keepGoing && i > 0) {
            nbSteps++
            keepGoing = process()
            i--
            yield()
        }

        executionTime = Date.now().toLong() - startTime
    }

    fun interrupt() {
        //TODO
    }

    fun getDetails(): ExecutionDetails {
        return ExecutionDetails(nbSteps, executionTime)
    }

    fun getErrors(code: String): MutableList<CompilerError> {
        val errors = mutableListOf<CompilerError>()
        val loop = LoopData()

        code.forEachIndexed { i, char ->
            if (char in authorizedChars) {
                if (char == '[') {
                    loop.opened++
                } else if (char == ']') {
                    loop.closed++
                }
            } else {
                val error = CompilerError(
                    type = "Unauthorized instruction",
                    addr = i,
                    char = char,
                )

                errors.add(error)
            }
        }

        if (loop.opened != loop.closed) {
            val error = CompilerError(
                type = if (loop.opened > loop.closed) {
                    (loop.opened - loop.closed).toString() + " \"]\" missing"
                } else {
                    (loop.closed - loop.opened).toString() + " \"[\" missing"
                },
                addr = Int.MIN_VALUE,
                char = ' '
            )

            errors.add(error)
        }

        return errors
    }
}
