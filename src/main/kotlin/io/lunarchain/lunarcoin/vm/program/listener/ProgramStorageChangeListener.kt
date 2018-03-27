package lunar.vm.program.listener

import lunar.vm.DataWord

class ProgramStorageChangeListener: ProgramListenerAdaptor() {
    private val diff = java.util.HashMap<DataWord, DataWord>()

    fun merge(diff: Map<DataWord, DataWord>) {
        this.diff.putAll(diff)
    }

    fun getDiff(): Map<DataWord, DataWord> {
        return HashMap(diff)
    }
}