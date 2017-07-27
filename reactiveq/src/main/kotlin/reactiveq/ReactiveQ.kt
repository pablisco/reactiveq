package reactiveq

import java.io.Closeable

class ReactiveQ private constructor() {

    companion object {
        operator fun invoke() : ReactiveQ {
            return ReactiveQ()
        }
    }

    private val connections = mutableMapOf<Class<*>, Connector<*>>()

    data class Result<out T>(
        val value: T? = null,
        val exception: Exception? = null
    )

    inline fun <reified T> connect() : Connector<T> = connect(T::class.java)

    @Suppress("UNCHECKED_CAST")
    fun <T> connect(type: Class<T>) : Connector<T> =
        connections.getOrPut(type) { Connection<T>() } as Connector<T>

    interface Connector<T> {
        fun onEmit(receiver: (T) -> Unit) : Closeable
        fun onReceive(emitter: () -> T) : Closeable
        fun <P> onRespond(outType: Class<P>, responder: (T) -> P) : Closeable
        fun emit(value: T) : Unit
        fun receive(): List<Result<T>>
        fun <P> query(outType: Class<P>, query: T) : List<Result<P>>
    }

    private class Connection<T> : Connector<T> {

        private val receivers = mutableSetOf<(T) -> Unit>()
        private val emitters = mutableSetOf<() -> T>()
        private val responders = mutableSetOf<ResponderWrapper<T, *>>()

        override fun onEmit(receiver: (T) -> Unit) : Closeable =
            receivers.addWithClosable(receiver)

        override fun onReceive(emitter: () -> T) : Closeable =
            emitters.addWithClosable(emitter)

        override fun <P> onRespond(outType: Class<P>, responder: (T) -> P) : Closeable =
            responders.addWithClosable(ResponderWrapper(outType, responder))

        override fun emit(value: T) =
            receivers.forEach { it(value) }

        override fun receive() : List<Result<T>> =
            emitters.map(this::safeResult)

        @Suppress("UNCHECKED_CAST")
        override fun <P> query(outType: Class<P>, query: T) : List<Result<P>> =
            responders
                .filter { it.outType == outType }
                .map { it as ResponderWrapper<T, P> }
                .map { safeResult { it.responder(query) } }

        private data class ResponderWrapper<in T, P>(
            val outType: Class<P>,
            val responder: (T) -> P
        )

        fun <T> safeResult(f: () -> T) : Result<T> =
            try { Result(f()) } catch (e: Exception) { Result(exception = e) }

        private fun <T> MutableCollection<T>.addWithClosable(item: T) =
            add(item).let { Closeable { remove(item) } }

    }

}

inline fun <T, reified P> ReactiveQ.Connector<T>.onRespond(noinline responder: (T) -> P) : Closeable =
    onRespond(P::class.java, responder)

inline fun <T, reified P> ReactiveQ.Connector<T>.query(query: T) : List<ReactiveQ.Result<P>> =
    query(P::class.java, query)
