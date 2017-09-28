package reactiveq

import kategory.Try
import reactiveq.reflexion.TypeToken
import java.io.Closeable

class ReactiveQ {

    private val onPushReactors = concurrentSetOf<Reactor.OnPush<*>>()
    private val onPullReactors = concurrentSetOf<Reactor.OnPull<*, *>>()


    fun <T : Any> push(typeToken: TypeToken<T>, value: T) {
        val candidates = onPushReactors.mapNotNull { it.takes(typeToken) }
        check(candidates.isNotEmpty()) { "No Reactors for type $typeToken" }
        candidates.forEach { it(value) }
    }

    inline fun <reified T : Any> push(value: T) =
        push(object : TypeToken<T>() {}, value)

    fun <T> onPush(typeToken: TypeToken<T>, f: (T) -> Unit): Closeable =
        onPushReactors.addWithClosable(Reactor.OnPush(typeToken, f))

    inline fun <reified T> onPush(noinline f: (T) -> Unit) =
        onPush(object : TypeToken<T>() {}, f)


    fun <T> pull(resultType: TypeToken<T>): OngoingPull<T> =
        OngoingPull(resultType, this)

    inline fun <reified T> pull(): OngoingPull<T> =
        pull(object : TypeToken<T>() {})

    inline fun <reified T> pullWithoutQuery(): List<Try<T>> =
        pull<T>().withoutQuery()

    fun <T> onPull(outType: TypeToken<T>, actions: OngoingOnPull<T>.() -> Unit): Closeable =
        OngoingOnPull(outType, this).also(actions)

    inline fun <reified T> onPull(noinline f: OngoingOnPull<T>.() -> Unit): Closeable =
        onPull(object : TypeToken<T>() {}, f)

    inline fun <reified T> onPullWithoutQuery(crossinline f: () -> T): Closeable =
        onPull<T> { withoutQuery { f() } }


    inline operator fun invoke(f: ReactiveQ.() -> Unit) = f(this)

    inline operator fun <R> invoke(f: ReactiveQ.() -> R): R = f(this)


    class OngoingOnPull<in T>(
        private val resultType: TypeToken<T>,
        private val reactiveQ: ReactiveQ
    ) : Closeable {

        private val closables = mutableListOf<Closeable>()

        fun withoutQuery(f: () -> T) : Closeable = withQuery<Unit> { f() }

        fun <Q> withQuery(queryType: TypeToken<Q>, f: (Q) -> T): Closeable =
            reactiveQ.onPullReactors.addWithClosable(Reactor.OnPull(queryType, resultType, f)).also {
                closables += it
            }

        inline fun <reified Q> withQuery(noinline f: (Q) -> T): Closeable =
            withQuery(object : TypeToken<Q>() {}, f)

        override fun close() = closables.forEach { it.close() }

    }

    class OngoingPull<out T>(
        private val resultType: TypeToken<T>,
        private val reactiveQ: ReactiveQ
    ) {

        fun withoutQuery(): List<Try<T>> = withQuery(Unit)

        fun <Q> withQuery(queryType: TypeToken<Q>, query: Q) : List<Try<T>> {
            val candidates = reactiveQ.onPullReactors
                .mapNotNull { it.takes(queryType, resultType) }
            check(candidates.isNotEmpty()) { "No Pull Reactors for type: $resultType for query: $queryType" }
            return candidates
                .map { Try.invoke { it(query) } }
        }

        inline fun <reified Q> withQuery(query: Q) : List<Try<T>> =
            withQuery(object : TypeToken<Q>() {}, query)

    }

}

internal fun <T> MutableCollection<T>.addWithClosable(item: T): Closeable {
    add(item)
    return Closeable { remove(item) }
}

