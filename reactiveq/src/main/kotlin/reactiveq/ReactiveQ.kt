package reactiveq

import java.io.Closeable

class ReactiveQ {

    private val connections = mutableMapOf<Class<*>, Connection<*>>()

    inline fun <reified T> connect() : Connection<T> = connect(T::class.java)

    @Suppress("UNCHECKED_CAST")
    fun <T> connect(type: Class<T>) : Connection<T> =
        connections.getOrPut(type) { ConcreteConnection<T>() } as Connection<T>

}

data class Result<out T>(
    val value: T? = null,
    val exception: Exception? = null
)

interface Connection<T> {

    fun onEmit(onEmitReactor: (T) -> Unit) : Closeable
    fun onFetch(onFetchReactor: () -> T) : Closeable
    fun <P> onQuery(outType: Class<P>, onQueryReactor: (T) -> P) : Closeable
    fun onReactorsChanged(f: (ReactorCounter) -> Unit) : Closeable

    fun emit(value: T) : Unit
    fun fetch(): List<Result<T>>
    fun <P> query(outType: Class<P>, query: T) : List<Result<P>>
}

internal class ConcreteConnection<T> : Connection<T> {

    private val onEmits = mutableSetOf<(T) -> Unit>()
    private val onFetches = mutableSetOf<() -> T>()
    private val onQueries = mutableSetOf<TypedReactor<T, *>>()
    private val onReactorChangedSet = mutableSetOf<(ReactorCounter) -> Unit>()

    override fun onEmit(onEmitReactor: (T) -> Unit) : Closeable =
        onEmits.addWithClosable(onEmitReactor)

    override fun onFetch(onFetchReactor: () -> T) : Closeable =
        onFetches.addWithClosable(onFetchReactor)

    override fun <P> onQuery(outType: Class<P>, onQueryReactor: (T) -> P) : Closeable =
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
        val onQuery: (T) -> P
    )

    fun <T> safeResult(f: () -> T) : Result<T> =
        try {
            Result(f())
        } catch (e: Exception) {
            Result(exception = e)
        }

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
    val onQueryCount: Int
) {
    val totalCount: Int = onEmitCount + onFetchCount + onQueryCount
}

inline fun <T, reified P> Connection<T>.onQuery(noinline onQuery: (T) -> P) : Closeable =
    onQuery(P::class.java, onQuery)

inline fun <T, reified P> Connection<T>.query(query: T) : List<Result<P>> =
    query(P::class.java, query)

inline operator fun <T> Connection<T>.invoke(f: Connection<T>.() -> Unit) = f(this)