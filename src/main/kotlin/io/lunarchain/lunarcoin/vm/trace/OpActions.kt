package lunar.vm.trace

import io.lunarchain.lunarcoin.util.ByteUtil.toHexString
import lunar.vm.DataWord
import java.util.*

class OpActions {

    companion object Action {

        enum class Name {
            pop,
            push,
            swap,
            extend,
            write,
            put,
            remove,
            clear;
        }

        private var opName: Name? = null
        private var opParams: MutableMap<String, Any>? = null

        fun getName(): Name? {
            return opName
        }

        fun setName(name: Name) {
            this.opName = name
        }

        fun getParams(): MutableMap<String, Any>? {
            return this.opParams
        }

        fun setParams(params: MutableMap<String, Any>) {
            this.opParams = params
        }

        internal fun addParam(name: String, value: Any?): Action {
            if (value != null) {
                if (opParams == null) {
                    opParams = HashMap()
                }
                opParams!![name] = value.toString()
            }
            return this
        }

    }

    private var stack: MutableList<Action> = ArrayList()
    private var memory: MutableList<Action> = ArrayList()
    private var storage: MutableList<Action> = ArrayList()

    fun getStack(): MutableList<Action> {
        return stack
    }

    fun setStack(stack: MutableList<Action>) {
        this.stack = stack
    }

    fun getMemory(): MutableList<Action> {
        return memory
    }

    fun setMemory(memory: MutableList<Action>) {
        this.memory = memory
    }

    fun getStorage(): List<Action> {
        return storage
    }

    fun setStorage(storage: MutableList<Action>) {
        this.storage = storage
    }

    private fun addAction(container: MutableList<Action>, name: Action.Name): Action {
        val action = Action
        action.setName(name)

        container.add(action)

        return action
    }

    fun addStackPop(): Action {
        return addAction(stack, Action.Name.pop)
    }

    fun addStackPush(value: DataWord): Action {
        return addAction(stack, Action.Name.push)
                .addParam("value", value)
    }

    fun addStackSwap(from: Int, to: Int): Action {
        return addAction(stack, Action.Name.swap)
                .addParam("from", from)
                .addParam("to", to)
    }

    fun addMemoryExtend(delta: Long): Action {
        return addAction(memory, Action.Name.extend)
                .addParam("delta", delta)
    }

    fun addMemoryWrite(address: Int, data: ByteArray, size: Int): Action {
        return addAction(memory, Action.Name.write)
                .addParam("address", address)
                .addParam("data", toHexString(data).substring(0, size))
    }

    fun addStoragePut(key: DataWord, value: DataWord): Action {
        return addAction(storage, Action.Name.put)
                .addParam("key", key)
                .addParam("value", value)
    }

    fun addStorageRemove(key: DataWord): Action {
        return addAction(storage, Action.Name.remove)
                .addParam("key", key)
    }

    fun addStorageClear(): Action {
        return addAction(storage, Action.Name.clear)
    }
}