package lunar.vm.program.listener

interface ProgramListenerAware {
    abstract fun setProgramListener(listener: ProgramListener)
}