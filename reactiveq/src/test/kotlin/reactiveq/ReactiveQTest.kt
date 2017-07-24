package reactiveq

import org.assertj.core.api.Assertions.*
import org.junit.Test

class ReactiveQTest {

    @Test fun `should create connection and receive emitted value`() {
        val queue = ReactiveQ()
        var text : String? = null

        queue.connect<String> { text = it }
        queue.emit("some value")

        assertThat(text).isEqualTo("some value")
    }

    @Test fun `should stop receiving events after connection closed`() {
        val queue = ReactiveQ()
        var text : String? = null

        val connection = queue.connect<String> { text = it }
        connection.close()
        queue.emit("some value")

        assertThat(text).isNull()
    }

    @Test fun `should deliver two different types`() {
        val queue = ReactiveQ()
        var text : String? = null
        var number : Int? = null

        queue.connect<String> { text = it }
        queue.connect<Int> { number = it }
        queue.emit("some value")
        queue.emit(123)

        assertThat(text).isEqualTo("some value")
        assertThat(number).isEqualTo(123)
    }

}