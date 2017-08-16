package reactiveq

import java.io.Closeable

internal class InternalConnection<T>(
    private val type: Class<T>,
    private val reportReactorCounter: (ReactorCounter<T>) -> Unit
) : Connection<T> {

    private val onSendReactors = concurrentSetOf<(T) -> Unit>()
    private val onFetchReactors = concurrentSetOf<() -> T>()
    private val onQueryReactors = concurrentSetOf<TypedReactor<T, *>>()

    override fun onSend(onSendReactor: (T) -> Unit): Closeable =
        onSendReactors.addWithClosable(onSendReactor)

    override fun onFetch(onFetchReactor: () -> T): Closeable =
        onFetchReactors.addWithClosable(onFetchReactor)

    override fun <P> onQuery(outType: Class<P>, onQueryReactor: (T) -> P): Closeable =
        onQueryReactors.addWithClosable(TypedReactor(outType, onQueryReactor))

    fun send(value: T) =
        onSendReactors.forEach { it(value) }

    fun fetch(): List<Result<T>> =
        onFetchReactors.map { it.asResult() }

    @Suppress("UNCHECKED_CAST")
    fun <P> query(outType: Class<P>, query: T): List<Result<P>> =
        onQueryReactors
            .filter { it.outType == outType }
            .map { it as TypedReactor<T, P> }
            .map { Result { it.onQuery(query) } }

    private data class TypedReactor<in T, P>(
        val outType: Class<P>,
        val onQuery: (T) -> P
    )

    private fun <T> MutableCollection<T>.addWithClosable(item: T) : Closeable =
        add(item).let {
            reportReactorChanges()
            Closeable {
                remove(item)
                reportReactorChanges()
            }
        }

    private fun reportReactorChanges() =
        reportReactorCounter(ReactorCounter(
            onSendReactors.size,
            onFetchReactors.size,
            onQueryReactors.size,
            type
        ))

}

