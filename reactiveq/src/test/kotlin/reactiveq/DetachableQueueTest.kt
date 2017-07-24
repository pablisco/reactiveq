package reactiveq

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown
import org.junit.Test

class DetachableQueueTest {

    @Test
    fun `should be able to add one item`() {
        val queue = DetachableQueue<String>()

        queue.push("some value")

        assertThat(queue).containsOnly("some value")
    }

    @Test
    fun `should be able to add two items`() {
        val queue = DetachableQueue<String>()

        queue.push("some value")
        queue.push("other value")

        assertThat(queue).containsOnly("some value", "other value")
    }

    @Test
    fun `should be able to add three items`() {
        val queue = DetachableQueue<String>()

        queue.push("some value")
        queue.push("other value")
        queue.push("one more value")

        assertThat(queue).containsOnly("some value", "other value", "one more value")
    }

    @Test
    fun `should fail if asking for too many items`() {
        val queue = DetachableQueue<String>()

        try {
            queue.iterator().next()
            failBecauseExceptionWasNotThrown(NoSuchFieldException::class.java)
        } catch(e: NoSuchElementException) {
            assertThat(e).hasMessage("No more items available.")
        }
    }

    @Test
    fun `should allow to detach one element from list`() {
        val queue = DetachableQueue<String>()

        val detachable = queue.push("some value")
        detachable.detach()

        assertThat(queue).isEmpty()
    }

    @Test
    fun `should allow to detach one element from list of two`() {
        val queue = DetachableQueue<String>()

        queue.push("some value")
        val detachable = queue.push("other value")
        detachable.detach()

        assertThat(queue).containsOnly("some value")
    }

    @Test
    fun `should be able to add more values after removing last`() {
        val queue = DetachableQueue<String>()

        queue.push("some value")
        queue.push("other value")
        val detachable = queue.push("one more value")
        detachable.detach()
        queue.push("some other value")

        assertThat(queue).containsOnly("some value", "other value", "some other value")
    }

    @Test
    fun `should be able to get the size of the queue`() {
        val queue = DetachableQueue<String>()

        queue.push("some value")
        queue.push("other value")

        assertThat(queue.size).isEqualTo(2)
    }

}