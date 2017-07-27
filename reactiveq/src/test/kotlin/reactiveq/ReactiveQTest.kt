package reactiveq

import org.assertj.core.api.Assertions.*
import org.assertj.core.api.Condition
import org.junit.Test

class ReactiveQTest {

    @Test fun `should create connection and receive emitted value`() {
        val queue = ReactiveQ()
        var text : String? = null

        queue.onEmit<String> { text = it }
        queue.emit("some value")

        assertThat(text).isEqualTo("some value")
    }

    @Test fun `should stop receiving events after connection closed`() {
        val queue = ReactiveQ()
        var text : String? = null

        val connection = queue.onEmit<String> { text = it }
        connection.close()
        queue.emit("some value")

        assertThat(text).isNull()
    }

    @Test fun `should deliver two different types`() {
        val queue = ReactiveQ()
        var text : String? = null
        var number : Int? = null

        queue.onEmit<String> { text = it }
        queue.onEmit<Int> { number = it }
        queue.emit("some value")
        queue.emit(123)

        assertThat(text).isEqualTo("some value")
        assertThat(number).isEqualTo(123)
    }

    @Test fun `should emit values from emitter`() {
        val queue = ReactiveQ()

        queue.onReceive { "some value" }

        val results = queue.receive<String>()
        assertThat(results).isNotEmpty()
    }

    @Test fun `should receive exception from emitter`() {
        val queue = ReactiveQ()

        val expectedException = Exception()
        queue.onReceive<String> { throw expectedException }

        val items = queue.receive<String>()

        assertThat(items[0].exception).isEqualTo(expectedException)
    }

    @Test fun `should be able to query Responder`() {
        val queue = ReactiveQ()

        queue.onRespond { query: String -> "some $query" }

        val result = queue.query<String, String>("value")

        assertThat(result[0].value).isEqualTo("some value")
    }

}