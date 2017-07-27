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

    inline fun <reified T : Any> onEmit(noinline receiver: (T) -> Unit) : Closeable
        = onEmit(T::class, receiver)

    inline fun <reified T : Any> onReceive(noinline receiver: () -> T) : Closeable
        = onReceive(T::class, receiver)

    inline fun <reified T : Any, reified P : Any> onRespond(noinline f: (T) -> P) : Closeable
        = onRespond(T::class, P::class, f)

    fun <T : Any> onEmit(type: KClass<T>, receiver: (T) -> Unit) : Closeable
        = getConnection(type).onEmit(receiver)

    fun <T : Any> onReceive(type: KClass<T>, emitter: () -> T) : Closeable
        = getConnection(type).onReceive(emitter)

    fun <T : Any, P : Any> onRespond(inType: KClass<T>, outType: KClass<P>, responder: (T) -> P) : Closeable
        = getConnection(inType).onRespond(outType, responder)

    inline fun <reified T : Any> emit(value: T)
        = emit(T::class, value)

    inline fun <reified T : Any> receive(): List<Result<T>>
        = receive(T::class)

    inline fun <reified T : Any, reified P : Any> query(query : T) : List<Result<P>>
        = query(T::class, P::class, query)

    fun <T : Any> emit(type: KClass<T>, value: T)
        = getConnection(type).emit(value)

    fun <T : Any> receive(type: KClass<T>): List<Result<T>>
        = getConnection(type).receive()

    fun <T : Any, P : Any> query(inType: KClass<T>, outType: KClass<P>, query : T) : List<Result<P>>
        = getConnection(inType).query(outType, query)

    data class Result<out T>(
        val value: T? = null,
        val exception: Exception? = null
    )

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> getConnection(type: KClass<T>) : Connection<T>
        = connections.getOrPut(type) { Connection<T>() } as Connection<T>

    private class Connection<T : Any> {

        private val receivers = DetachableQueue<(T) -> Unit>()
        private val emitters = DetachableQueue<() -> T>()
        private val responders = DetachableQueue<ResponderWrapper<T, *>>()

        fun onEmit(receiver: (T) -> Unit) : Closeable
            = DetachableClosable(receivers.push(receiver))

        fun onReceive(emitter: () -> T) : Closeable
            = DetachableClosable(emitters.push(emitter))

        fun <P : Any> onRespond(outType: KClass<P>, responder: (T) -> P) : Closeable
            = DetachableClosable(responders.push(ResponderWrapper(outType, responder)))

        fun emit(value: T)
            = receivers.forEach { it(value) }

        fun receive(): List<Result<T>>
            = emitters.map(this::safeResult)

        @Suppress("UNCHECKED_CAST")
        fun <P : Any> query(outType: KClass<P>, query: T) : List<Result<P>>
            = responders
                .filter { it.outType == outType }
                .map { it as ResponderWrapper<T, P> }
                .map { safeResult { it.responder(query) } }

        private data class ResponderWrapper<in T : Any, P : Any>(
            val outType: KClass<P>,
            val responder: (T) -> P
        )

        fun <T> safeResult(f: () -> T) : Result<T>
            = try { Result(f()) } catch (e: Exception) { Result(exception = e) }

    }

    private class DetachableClosable(
        private val receiver: Detachable
    ) : Closeable {
        override fun close() {
            receiver.detach()
        }
    }

}
