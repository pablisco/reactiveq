package reactiveq

import org.assertj.core.api.Assertions.*
import org.junit.Test

class ReactiveQTest {

    private val queue: ReactiveQ = ReactiveQ()

    private val strings by lazy { queue.connect<String>() }
    private val charSequences by lazy { queue.connect<CharSequence>() }
    private val ints by lazy { queue.connect<Int>() }

    @Test fun `should create connection and receive emitted value`() {
        var text : String? = null

        strings.onEmit { text = it }
        strings.emit("some value")

        assertThat(text).isEqualTo("some value")
    }

    @Test fun `should stop receiving events after connection closed`() {
        var text : String? = null

        val closable = strings.onEmit { text = it }
        closable.close()
        strings.emit("some value")

        assertThat(text).isNull()
    }

    @Test fun `should deliver two different types`() {
        var text : String? = null
        var number : Int? = null

        strings.onEmit { text = it }
        strings.emit("some value")
        ints.onEmit { number = it }
        ints.emit(123)

        assertThat(text).isEqualTo("some value")
        assertThat(number).isEqualTo(123)
    }

    @Test fun `should emit values from emitter`() {
        strings.onFetch { "some value" }

        val results = strings.fetch()
        assertThat(results).isNotEmpty()
    }

    @Test fun `should receive exception from emitter`() {
        val expectedException = Exception()
        strings.onFetch { throw expectedException }

        val items = strings.fetch()

        assertThat(items[0].exception).isEqualTo(expectedException)
    }

    @Test fun `should be able to query Responder`() {
        strings.onQuery { query: String -> "some $query" }

        val result = strings.query<String, String>("value")

        assertThat(result[0].value).isEqualTo("some value")
    }

}