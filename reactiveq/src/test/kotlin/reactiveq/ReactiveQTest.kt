package reactiveq

import org.assertj.core.api.Assertions.*
import org.junit.Test

class ReactiveQTest {

    @Test fun `should create connection and receive emitted value`() {
        val queue = ReactiveQ()
        var text : String? = null

        queue.connect<String>().onEmit { text = it }
        queue.connect<String>().emit("some value")

        assertThat(text).isEqualTo("some value")
    }

    @Test fun `should stop receiving events after connection closed`() {
        val queue = ReactiveQ()
        var text : String? = null

        val connection = queue.connect<String>().onEmit { text = it }
        connection.close()
        queue.connect<String>().emit("some value")

        assertThat(text).isNull()
    }

    @Test fun `should deliver two different types`() {
        val queue = ReactiveQ()
        var text : String? = null
        var number : Int? = null

        queue.connect<String>().onEmit { text = it }
        queue.connect<Int>().onEmit { number = it }
        queue.connect<String>().emit("some value")
        queue.connect<Int>().emit(123)

        assertThat(text).isEqualTo("some value")
        assertThat(number).isEqualTo(123)
    }

    @Test fun `should emit values from emitter`() {
        val queue = ReactiveQ()

        queue.connect<String>().onReceive { "some value" }

        val results = queue.connect<String>().receive()
        assertThat(results).isNotEmpty()
    }

    @Test fun `should receive exception from emitter`() {
        val queue = ReactiveQ()

        val expectedException = Exception()
        queue.connect<String>().onReceive { throw expectedException }

        val items = queue.connect<String>().receive()

        assertThat(items[0].exception).isEqualTo(expectedException)
    }

    @Test fun `should be able to query Responder`() {
        val queue = ReactiveQ()

        queue.connect<String>().onRespond { query: String -> "some $query" }

        val result = queue.connect<String>().query<String, String>("value")

        assertThat(result[0].value).isEqualTo("some value")
    }

}