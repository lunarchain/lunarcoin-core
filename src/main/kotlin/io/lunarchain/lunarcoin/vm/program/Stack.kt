package lunar.vm.program

import lunar.vm.DataWord
import lunar.vm.program.listener.ProgramListener
import lunar.vm.program.listener.ProgramListenerAware


class Stack: ProgramListenerAware, java.util.Stack<DataWord>() {

    private var programListener: ProgramListener? = null

    override fun setProgramListener(listener: ProgramListener) {
        this.programListener = listener
    }

    @Synchronized
    override fun pop(): DataWord {
        programListener?.onStackPop()
        return super.pop()
    }

    override fun push(item: DataWord): DataWord {
        programListener?.onStackPush(item)
        return super.push(item)
    }

    fun swap(from: Int, to: Int) {
        if (isAccessible(from) && isAccessible(to) && from != to) {
            programListener?.onStackSwap(from, to)
            val tmp = get(from)
            set(from, set(to, tmp))
        }
    }

    private fun isAccessible(from: Int): Boolean {
        return from >= 0 && from < size
    }

}
