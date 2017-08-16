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

fun <T, R> Result<T>.flatMap(f: (T) -> Result<R>): Result<R> = fold({ Result.Failure(it) }, { f(it) })
fun <T, R> Result<T>.map(f: (T) -> R): Result<R> = fold({ Result.Failure(it) }, { Success(f(it)) })
fun <T> Result<T>.getOrElse(default: () -> T): T = fold({ default() }, { it })
fun <T> Result<T>.recoverWith(f: (Throwable) -> Result<T>): Result<T> = fold({ f(it) }, { Success(it) })
fun <T> Result<T>.recover(f: (Throwable) -> T): Result<T> = fold({ Success(f(it)) }, { Success(it) })
fun <T> Result<T>.transform(s: (T) -> Result<T>, f: (Throwable) -> Result<T>): Result<T> = fold({ f(it) }, { flatMap(s) })