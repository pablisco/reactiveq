package reactiveq

import org.assertj.core.api.Assertions.*
import org.junit.Test

class ReactiveQTest {

    companion object {
        private val INVALID_COUNTER = ReactorCounter<String>(-1, -1, -1)
    }

    private val queue: ReactiveQ = ReactiveQ()

    @Test fun `should create connection and receive emitted value`() {
        var text : String? = null

        queue {
            connect<String>().onSend { text = it }
            send("some value")
        }

        assertThat(text).isEqualTo("some value")
    }

    @Test fun `should stop receiving events after connection closed`() {
        var text : String? = "not null"

        queue {
            val closable = connect<String>().onSend { text = it }
            closable.close()
        }

        queue.send("some value")

        assertThat(text).isEqualTo("not null")
    }

    @Test fun `should deliver two different types`() {
        var text : String? = null
        var number : Int? = null

        queue {
            connect<String>().onSend { text = it }
            connect<Int>().onSend { number = it }
            send("some value")
            send(123)
        }

        assertThat(text).isEqualTo("some value")
        assertThat(number).isEqualTo(123)
    }

    @Test fun `should emit values from emitter`() {
        queue.connect<String>().onFetch { "some value" }

        val results = queue.fetch<String>()

        assertThat(results).containsOnly(Result.success("some value"))
    }

    @Test fun `should receive exception from emitter`() {
        val expectedException = Exception()

        queue.connect<String>().onFetch { throw expectedException }
        val items = queue.fetch<String>()

        assertThat(items).containsOnly(Result.failure(expectedException))
    }

    @Test fun `should be able to query Responder`() {
        queue.connect<String>().onQuery { query: String -> "some $query" }

        val result = queue.query<String, String>("value")
        assertThat(result).containsOnly(Result.Success("some value"))
    }

    @Test fun `should notify when Reactor added`() {
        var counter: ReactorCounter<String> = INVALID_COUNTER

        queue {
            connect<ReactorCounter<String>>().onSend { counter = it }
            connect<String>().onSend {  }
        }

        assertThat(counter).isEqualTo(ReactorCounter<String>(1, 0, 0))
    }

    @Test fun `should report when Reactor is removed`() {
        val counters: MutableList<ReactorCounter<String>> = mutableListOf()

        queue {
            connect<ReactorCounter<String>>().onSend { counters += it }
            val closeable = connect<String>().onSend { }
            closeable.close()
        }

        assertThat(counters).containsOnly(
            ReactorCounter(0, 0, 0),
            ReactorCounter(1, 0, 0),
            ReactorCounter(0, 0, 0)
        )
    }

    @Test fun `should react to variant reactors`() {
        var text: CharSequence? = null

        queue {
            connect<CharSequence>().onSend { text = it }
            send("some value")
        }

        assertThat(text).isEqualTo("some value")
    }

}