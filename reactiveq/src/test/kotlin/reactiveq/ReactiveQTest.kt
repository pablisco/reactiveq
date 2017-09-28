package reactiveq

import kategory.Try
import kategory.getOrElse
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Test

class ReactiveQTest {

    private val queue: ReactiveQ = ReactiveQ()

    @Test fun `should create connection and receive emitted value`() {
        var text : String? = null

        queue {
            onPush<String> { text = it }
            push("some value")
        }

        assertThat(text).isEqualTo("some value")
    }

    @Test fun `should stop receiving events after connection closed`() {
        var text : String? = "not null"

        queue {
            val closable = onPush<String> { text = it }
            closable.close()
        }

        queue.push("some value")

        assertThat(text).isEqualTo("not null")
    }

    @Test fun `should deliver two different types`() {
        var text : String? = null
        var number : Int? = null

        queue {
            onPush<String> { text = it }
            onPush<Int> { number = it }
            push("some value")
            push(123)
        }

        assertThat(text).isEqualTo("some value")
        assertThat(number).isEqualTo(123)
    }

    @Test fun `should emit values from emitter`() {
        queue.onPull<String> {
            withoutQuery { "some value" }
        }

        val results = queue.pull<String>().withoutQuery()

        assertThat(results).containsOnly(Try.pure("some value"))
    }

    @Test fun `should receive exception from emitter`() {
        val expectedException = Exception()

        queue.onPullWithoutQuery<String> { throw expectedException }
        val items = queue.pull<String>().withoutQuery()

        assertThat(items).containsOnly(Try.raise(expectedException))
    }

    @Test fun `should be able to query Responder`() {
        queue.onPull<String> {
            withQuery<String> { "some $it" }
        }

        val result = queue.pull<String>().withQuery("value")
        assertThat(result).containsOnly(Try.pure("some value"))
    }

    @Test fun `should react to variant reactors`() {
        var text: CharSequence? = null

        queue {
            onPush<CharSequence> { text = it }
            push("some value")
        }

        assertThat(text).isEqualTo("some value")
    }

    data class A<out T>(val value: T)

    @Test fun `should differentiate generics for push`() {
        val stringList = mutableListOf<A<String>>()
        val intList = mutableListOf<A<Int>>()

        queue {
            onPush<A<String>> { stringList += it }
            onPush<A<Int>> { intList += it }
            push(A("some value"))
            push(A(123))
        }

        assertThat(stringList).containsOnly(A("some value"))
        assertThat(intList).containsOnly(A(123))
    }

    @Test fun `should differentiate generics for pull`() {
        queue {
            onPullWithoutQuery { A("some value") }
            onPullWithoutQuery { A(123) }
        }

        val strings = queue.pullWithoutQuery<A<String>>()
        val ints = queue.pullWithoutQuery<A<Int>>()

        assertThat(strings).hasSize(1)
        assertThat(ints).hasSize(1)
        val actualString = strings.first().getOrElse { fail("No sting provided") }
        val actualInt = ints.first().getOrElse { fail("No int provided") }
        assertThat(actualString).isEqualTo(A("some value"))
        assertThat(actualInt).isEqualTo(A(123))
    }

}