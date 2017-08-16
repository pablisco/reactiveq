package reactiveq

import reactiveq.Result.Success

/**
 * Type used to represent a result that may fail or be successful.
 *
 * Inspired on the `Try` type in [Kategory](https://github.com/kategory/kategory/)
 */
sealed class Result<out T> {

    companion object {
        operator fun <T> invoke(f: () -> T) : Result<T> =
            try { Success(f()) } catch (e: Throwable) { Failure(e) }

        fun <T> success(t: T) : Result<T> = Success(t)

        fun <T> failure(e: Throwable) : Result<T> = Failure(e)

    }

    fun <B> fold(fa: (Throwable) -> B, fb: (T) -> B): B =
        when (this) {
            is Failure -> fa(exception)
            is Success -> try { fb(value) } catch (e: Throwable) { fa(e) }
        }

    data class Success<out T>(val value: T) : Result<T>()
    data class Failure<out T>(val exception: Throwable) : Result<T>()

}

inline fun <T, R> Result<T>.flatMap(crossinline f: (T) -> Result<R>): Result<R> = fold({ Result.Failure(it) }, { f(it) })
inline fun <T, R> Result<T>.map(crossinline f: (T) -> R): Result<R> = fold({ Result.Failure(it) }, { Success(f(it)) })
inline fun <T> Result<T>.getOrElse(crossinline default: () -> T): T = fold({ default() }, { it })
inline fun <T> Result<T>.recoverWith(crossinline f: (Throwable) -> Result<T>): Result<T> = fold({ f(it) }, { Success(it) })
inline fun <T> Result<T>.recover(crossinline f: (Throwable) -> T): Result<T> = fold({ Success(f(it)) }, { Success(it) })
inline fun <T> Result<T>.transform(crossinline s: (T) -> Result<T>, crossinline f: (Throwable) -> Result<T>): Result<T> = fold({ f(it) }, { flatMap(s) })

fun <T> T.asSuccess(): Result<T> = Result.success(this)
fun <T> Throwable.asFailure(): Result<T> = Result.failure(this)
fun <T> (() -> T).asResult(): Result<T> = Result(this)