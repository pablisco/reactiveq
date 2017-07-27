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
        fun onFetch(emitter: () -> T) : Closeable
        fun <P> onQuery(outType: Class<P>, responder: (T) -> P) : Closeable
        fun emit(value: T) : Unit
        fun fetch(): List<Result<T>>
        fun <P> query(outType: Class<P>, query: T) : List<Result<P>>
    }

    private class Connection<T> : Connector<T> {

        private val onEmits = mutableSetOf<(T) -> Unit>()
        private val onFetches = mutableSetOf<() -> T>()
        private val onQueries = mutableSetOf<ResponderWrapper<T, *>>()

        override fun onEmit(receiver: (T) -> Unit) : Closeable =
            onEmits.addWithClosable(receiver)

        override fun onFetch(emitter: () -> T) : Closeable =
            onFetches.addWithClosable(emitter)

        override fun <P> onQuery(outType: Class<P>, responder: (T) -> P) : Closeable =
            onQueries.addWithClosable(ResponderWrapper(outType, responder))

        override fun emit(value: T) =
            onEmits.forEach { it(value) }

        override fun fetch() : List<Result<T>> =
            onFetches.map(this::safeResult)

        @Suppress("UNCHECKED_CAST")
        override fun <P> query(outType: Class<P>, query: T) : List<Result<P>> =
            onQueries
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

inline fun <T, reified P> ReactiveQ.Connector<T>.onQuery(noinline responder: (T) -> P) : Closeable =
    onQuery(P::class.java, responder)

inline fun <T, reified P> ReactiveQ.Connector<T>.query(query: T) : List<ReactiveQ.Result<P>> =
    query(P::class.java, query)
