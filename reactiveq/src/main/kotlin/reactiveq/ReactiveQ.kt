package reactiveq

import java.io.Closeable

typealias OnEmitReactor<T> = (T) -> Unit
typealias OnFetchReactor<T> = () -> T
typealias OnQueryReactor<T, P> = (T) -> P

class ReactiveQ {

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

        fun onEmit(onEmitReactor: OnEmitReactor<T>) : Closeable
        fun onFetch(onFetchReactor: OnFetchReactor<T>) : Closeable
        fun <P> onQuery(outType: Class<P>, onQueryReactor: OnQueryReactor<T, P>) : Closeable
        fun onReactorsChanged(f: (ReactorCounter) -> Unit) : Closeable

        fun emit(value: T) : Unit
        fun fetch(): List<Result<T>>
        fun <P> query(outType: Class<P>, query: T) : List<Result<P>>
    }

    internal class Connection<T> : Connector<T> {

        private val onEmits = mutableSetOf<OnEmitReactor<T>>()
        private val onFetches = mutableSetOf<OnFetchReactor<T>>()
        private val onQueries = mutableSetOf<TypedReactor<T, *>>()
        private val onReactorChangedSet = mutableSetOf<(ReactorCounter) -> Unit>()

        override fun onEmit(onEmitReactor: OnEmitReactor<T>) : Closeable =
            onEmits.addWithClosable(onEmitReactor)

        override fun onFetch(onFetchReactor: OnFetchReactor<T>) : Closeable =
            onFetches.addWithClosable(onFetchReactor)

        override fun <P> onQuery(outType: Class<P>, onQueryReactor: OnQueryReactor<T, P>) : Closeable =
            onQueries.addWithClosable(TypedReactor(outType, onQueryReactor))

        override fun onReactorsChanged(f: (ReactorCounter) -> Unit): Closeable =
            onReactorChangedSet.addWithClosable(f)

        override fun emit(value: T) =
            onEmits.forEach { it(value) }

        override fun fetch() : List<Result<T>> =
            onFetches.map(this::safeResult)

        @Suppress("UNCHECKED_CAST")
        override fun <P> query(outType: Class<P>, query: T) : List<Result<P>> =
            onQueries
                .filter { it.outType == outType }
                .map { it as TypedReactor<T, P> }
                .map { safeResult { it.onQuery(query) } }

        private data class TypedReactor<in T, P>(
            val outType: Class<P>,
            val onQuery: OnQueryReactor<T, P>
        )

        fun <T> safeResult(f: () -> T) : Result<T> =
            try { Result(f()) } catch (e: Exception) { Result(exception = e) }

        private val reactorCounter
            get() =  ReactorCounter(
                onEmits.size,
                onFetches.size,
                onQueries.size
            )

        private fun <T> MutableCollection<T>.addWithClosable(item: T) =
            add(item).let {
                reportReactorChanges()
                Closeable {
                    remove(item)
                    reportReactorChanges()
                }
            }

        private fun reportReactorChanges() {
            onReactorChangedSet.forEach { it(reactorCounter) }
        }

    }

    data class ReactorCounter(
        val onEmitCount: Int,
        val onFetchCount: Int,
        val onQueryCount: Int,
        val totalCount: Int = onEmitCount + onFetchCount + onQueryCount
    )

}

inline fun <T, reified P> ReactiveQ.Connector<T>.onQuery(noinline onQuery: (T) -> P) : Closeable =
    onQuery(P::class.java, onQuery)

inline fun <T, reified P> ReactiveQ.Connector<T>.query(query: T) : List<ReactiveQ.Result<P>> =
    query(P::class.java, query)

inline operator fun <T> ReactiveQ.Connector<T>.invoke(f: ReactiveQ.Connector<T>.() -> Unit) = f(this)