package lunar.vm

import lunar.vm.program.Program

interface VMHook {
    abstract fun startPlay(program: Program)
    abstract fun step(program: Program, opcode: OpCode)
    abstract fun stopPlay(program: Program)
}