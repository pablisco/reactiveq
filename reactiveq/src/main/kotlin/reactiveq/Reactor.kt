package reactiveq

import reactiveq.reflexion.TypeToken

internal sealed class Reactor {

    internal data class OnPush<T>(
        private val type: TypeToken<T>,
        private val onPush: (T) -> Unit
    ) : Reactor() {

        @Suppress("UNCHECKED_CAST")
        fun <T2> takes(type: TypeToken<T2>): ((Any) -> Unit)? =
            if (this.type.isAssignableFrom(type)) onPush as (Any) -> Unit
            else null

    }

    internal data class OnPull<Q, T>(
        private val queryType: TypeToken<Q>,
        private val resultType: TypeToken<T>,
        private val onPull: (Q) -> T
    ) : Reactor() {

        @Suppress("UNCHECKED_CAST")
        fun <T2, Q2> takes(queryType: TypeToken<Q2>, resultType: TypeToken<T2>) =
            if (this.queryType.isAssignableFrom(queryType) && this.resultType.isAssignableFrom(resultType)) onPull as (Q2) -> T2
            else null

    }

}