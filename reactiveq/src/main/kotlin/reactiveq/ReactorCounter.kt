package reactiveq

@Suppress("unused")
data class ReactorCounter<T>(
    val onSendCount: Int,
    val onFetchCount: Int,
    val onQueryCount: Int,
    val type: Class<T>,
    val totalCount: Int = onSendCount + onFetchCount + onQueryCount
) : DoNotReportCounter {
    companion object {

        operator fun <T> invoke(onSendCount: Int, onFetchCount: Int, onQueryCount: Int, type: Class<T>) =
            ReactorCounter(onSendCount, onFetchCount, onQueryCount, type)

        inline operator fun <reified T> invoke(onSendCount: Int, onFetchCount: Int, onQueryCount: Int) =
            ReactorCounter(onSendCount, onFetchCount, onQueryCount, T::class.java)

    }
}

