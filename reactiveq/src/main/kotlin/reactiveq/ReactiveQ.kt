package reactiveq

import java.io.Closeable

class ReactiveQ private constructor() {

    companion object {
        operator fun invoke() : ReactiveQ {
            return ReactiveQ()
        }
    }

    private val connections = mutableMapOf<Class<*>, Connection<*>>()

    inline fun <reified T> onEmit(noinline receiver: (T) -> Unit) : Closeable =
        onEmit(T::class.java, receiver)

    inline fun <reified T> onReceive(noinline receiver: () -> T) : Closeable =
        onReceive(T::class.java, receiver)

    inline fun <reified T, reified P> onRespond(noinline f: (T) -> P) : Closeable =
        onRespond(T::class.java, P::class.java, f)

    fun <T> onEmit(type: Class<T>, receiver: (T) -> Unit) : Closeable =
        getConnection(type).onEmit(receiver)

    fun <T> onReceive(type: Class<T>, emitter: () -> T) : Closeable =
        getConnection(type).onReceive(emitter)

    fun <T, P> onRespond(inType: Class<T>, outType: Class<P>, responder: (T) -> P) : Closeable =
        getConnection(inType).onRespond(outType, responder)

    inline fun <reified T> emit(value: T) =
        emit(T::class.java, value)

    inline fun <reified T> receive(): List<Result<T>> =
        receive(T::class.java)

    inline fun <reified T, reified P> query(query : T) : List<Result<P>> =
        query(T::class.java, P::class.java, query)

    fun <T> emit(type: Class<T>, value: T) =
        getConnection(type).emit(value)

    fun <T> receive(type: Class<T>): List<Result<T>> =
        getConnection(type).receive()

    fun <T, P> query(inType: Class<T>, outType: Class<P>, query : T) : List<Result<P>> =
        getConnection(inType).query(outType, query)

    data class Result<out T>(
        val value: T? = null,
        val exception: Exception? = null
    )

    @Suppress("UNCHECKED_CAST")
    private fun <T> getConnection(type: Class<T>) : Connection<T> =
        connections.getOrPut(type) { Connection<T>() } as Connection<T>

    private class Connection<T> {

        private val receivers = mutableSetOf<(T) -> Unit>()
        private val emitters = mutableSetOf<() -> T>()
        private val responders = mutableSetOf<ResponderWrapper<T, *>>()

        fun onEmit(receiver: (T) -> Unit) : Closeable =
            receivers.addWithClosable(receiver)

        fun onReceive(emitter: () -> T) : Closeable =
            emitters.addWithClosable(emitter)

        fun <P> onRespond(outType: Class<P>, responder: (T) -> P) : Closeable =
            responders.addWithClosable(ResponderWrapper(outType, responder))

        fun emit(value: T) =
            receivers.forEach { it(value) }

        fun receive(): List<Result<T>> =
            emitters.map(this::safeResult)

        @Suppress("UNCHECKED_CAST")
        fun <P> query(outType: Class<P>, query: T) : List<Result<P>> =
            responders
                .filter { it.outType == outType }
                .map { it as ResponderWrapper<T, P> }
                .map { safeResult { it.responder(query) } }

        private data class ResponderWrapper<in T, P>(
            val outType: Class<P>,
            val responder: (T) -> P
        )

        fun <T> safeResult(f: () -> T) : Result<T>
            = try { Result(f()) } catch (e: Exception) { Result(exception = e) }

        private fun <T> MutableCollection<T>.addWithClosable(item: T) =
            add(item).let { Closeable { remove(item) } }

    }

}
