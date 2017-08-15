package reactiveq

import java.io.Closeable

interface Connection<T> {

    fun onSend(onSendReactor: (T) -> Unit): Closeable
    fun onFetch(onFetchReactor: () -> T): Closeable
    fun <P> onQuery(outType: Class<P>, onQueryReactor: (T) -> P): Closeable

}

inline fun <T, reified P> Connection<T>.onQuery(noinline onQuery: (T) -> P): Closeable =
    onQuery(P::class.java, onQuery)

inline operator fun <T> Connection<T>.invoke(f: Connection<T>.() -> Unit) = f(this)