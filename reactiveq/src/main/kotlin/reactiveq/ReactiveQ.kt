package reactiveq

import java.io.Closeable
import kotlin.reflect.KClass

class ReactiveQ private constructor() {

    companion object {
        operator fun invoke() : ReactiveQ {
            return ReactiveQ()
        }
    }

    private val connections = mutableMapOf<KClass<*>, Connection<*>>()

    inline fun <reified T : Any> connect(noinline receiver: (T) -> Unit) : Closeable
        = connect(T::class, receiver)

    fun <T : Any> connect(type: KClass<T>, receiver: (T) -> Unit) : Closeable
        = getConnection(type).connect(receiver)

    inline fun <reified T : Any> emit(value: T)
        = emit(T::class, value)

    fun <T : Any> emit(type: KClass<T>, value: T)
        = getConnection(type).emit(value)

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> getConnection(type: KClass<T>) : Connection<T>
        = connections.getOrPut(type) { Connection<T>() } as Connection<T>

    private class Connection<T : Any> {

        private val receivers = DetachableQueue<(T) -> Unit>()

        @Suppress("UNCHECKED_CAST")
        fun connect(receiver: (T) -> Unit) : Closeable {
            val detachable = receivers.push(receiver)
            return ReceiverClosable(detachable)
        }

        fun emit(value: T)
            = receivers.forEach { it(value) }

    }

    private class ReceiverClosable(
        private val receiver: Detachable
    ) : Closeable {
        override fun close() {
            receiver.detach()
        }
    }

}
