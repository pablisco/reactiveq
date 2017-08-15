package reactiveq

class ReactiveQ {

    private val connections = mutableMapOf<Class<*>, InternalConnection<*>>()

    @Suppress("UNCHECKED_CAST")
    fun <T> connect(type: Class<T>): Connection<T> =
        connections.getOrPut(type) { createConnection(type) } as Connection<T>

    fun <T> send(type: Class<T>, value: T) =
        connectionsFor(type).forEach { it.send(value) }

    fun <T> fetch(type: Class<T>): List<Result<T>> =
        connectionsFor(type).flatMap { it.fetch() }

    fun <T, P> query(inType: Class<T>, outType: Class<P>, query: T): List<Result<P>> =
        connectionsFor(inType).flatMap { it.query(outType, query) }

    private fun <T> createConnection(type: Class<T>): InternalConnection<T> =
        when {
            !DoNotReportCounter::class.java.isAssignableFrom(type) -> InternalConnection(type, { send(it) })
            else -> InternalConnection(type, { })
        }

    @Suppress("UNCHECKED_CAST")
    private fun <T> connectionsFor(type: Class<T>) : List<InternalConnection<T>> =
        connections.filterKeys { it.isAssignableFrom(type) }
            .values.map { it as InternalConnection<T> }

}

inline fun <reified T> ReactiveQ.connect(): Connection<T> =
    connect(T::class.java)

inline fun <reified T> ReactiveQ.send(value: T) =
    send(T::class.java, value)

inline fun <reified T> ReactiveQ.fetch(): List<Result<T>> =
    fetch(T::class.java)

inline fun <reified T, reified P> ReactiveQ.query(query: T): List<Result<P>> =
    query(T::class.java, P::class.java, query)

inline operator fun ReactiveQ.invoke(f: ReactiveQ.() -> Unit) = f(this)

inline operator fun <R> ReactiveQ.invoke(f: ReactiveQ.() -> R) : R = f(this)


/**
 * Used to avoid reporting reactor counter for a type.
 *
 * When a type extends this interface and a new reactor is registered on
 */
interface DoNotReportCounter

