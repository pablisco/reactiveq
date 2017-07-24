package reactiveq

import java.util.NoSuchElementException

/**
 * Simple queue that allows for fast removal and iteration.
 */
class DetachableQueue<T> : Iterable<T> {

    private var first: Node<T>? = null
    private var last: Node<T>? = null
    private var _size: Int = 0

    override fun iterator(): Iterator<T> = QueueIterator(first)

    fun push(item: T) : Detachable {
        val node = Node(item)
        if (first == null) {
            first = node
        } else {
            (last?:first)?.next = node
            node.previous = last?:first
            last = node
        }
        _size++
        return NodeDetachable(node, this)
    }

    private data class Node<T>(
        val value: T,
        var previous: Node<T>? = null,
        var next: Node<T>? = null
    )

    private class QueueIterator<out T>(
        private var current: Node<T>?
    ) : Iterator<T> {

        override fun hasNext(): Boolean
            = current != null

        override fun next(): T
            = checkedCurrent.also { current = it.next }.value

        private val checkedCurrent: Node<T>
            get() = current ?: throw NoSuchElementException("No more items available.")
    }

    private class NodeDetachable<T>(
        val node: Node<T>,
        val queue: DetachableQueue<T>
    ): Detachable {
        override fun detach() {
            if (node == queue.first) queue.first = node.next
            if (node == queue.last) queue.last = node.previous
            node.previous?.next = node.next
            node.next?.previous = node.previous
            node.previous = null
            node.next = null
            queue._size--
        }
    }

    val size: Int get() = _size

}

interface Detachable {
    fun detach()
}
