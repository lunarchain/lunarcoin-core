package io.lunarchain.lunarcoin.util

class ByteArraySet(private val delegate: MutableSet<ByteArrayWrapper>): MutableSet<ByteArray> {

    override var size: Int = 0

    override fun add(element: ByteArray): Boolean {
        val result = delegate.add(ByteArrayWrapper(element))
        size = delegate.size
        return result
    }

    override fun addAll(elements: Collection<ByteArray>): Boolean {
        var ret = false
        for (bytes in elements) {
            ret = ret or add(bytes)
        }
        return ret
    }

    override fun clear() {
        delegate.clear()
        size = delegate.size
    }

    override fun contains(element: ByteArray): Boolean {
        return delegate.contains(ByteArrayWrapper(element))
    }

    override fun containsAll(elements: Collection<ByteArray>): Boolean {
        throw RuntimeException("Not implemented")
    }

    override fun isEmpty(): Boolean {
        return delegate.isEmpty()
    }

    override fun iterator(): MutableIterator<ByteArray> {
        return object : MutableIterator<ByteArray> {

            internal var it: MutableIterator<ByteArrayWrapper> = delegate.iterator()
            override fun hasNext(): Boolean {
                return it.hasNext()
            }

            override fun next(): ByteArray {
                return it.next().getData()
            }

            override fun remove() {
                it.remove()
            }
        }
    }

    override fun remove(element: ByteArray): Boolean {
        val result = delegate.remove(ByteArrayWrapper(element))
        size = delegate.size
        return result
    }

    override fun retainAll(elements: Collection<ByteArray>): Boolean {
        throw RuntimeException("Not implemented")
    }

    override fun removeAll(elements: Collection<ByteArray>): Boolean {
        var changed = false
        for (el in elements) {
            changed = changed or remove(el)
        }
        return changed
    }

}