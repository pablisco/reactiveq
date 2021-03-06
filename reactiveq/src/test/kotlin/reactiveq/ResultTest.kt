package reactiveq

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith

@RunWith(Enclosed::class)
class ResultTest {

    class CreationTest {

        @Test fun `should create success from function`() {
            val result = Result { "success" }

            assertThat(result)
                .isInstanceOf(Result.Success::class.java)
                .isEqualTo(Result.success("success"))
        }

        @Test fun `should create failure from function`() {
            val expectedException = Throwable()
            val result = Result<String> { throw expectedException }

            assertThat(result)
                .isInstanceOf(Result.Failure::class.java)
                .isEqualTo(Result.failure<String>(expectedException))
        }

        @Test fun `should create success from extension method`() {
            val success = "success".asSuccess()

            assertThat(success).isEqualTo(Result.success("success"))
        }

        @Test fun `should create failure from extension method`() {
            val expectedError = Throwable()
            val failure = expectedError.asFailure<String>()

            assertThat(failure).isEqualTo(Result.failure<String>(expectedError))
        }

        @Test fun `should create result from function`() {
            val result = { "result" }.asResult()

            assertThat(result).isEqualTo(Result { "result" })
        }

    }

    class SuccessTest {

        private val success = Result.success("success")

        @Test fun `should map with new content`() {
            val result = success.map { "other $it" }

            assertThat(result).isEqualTo(Result.success("other success"))
        }

        @Test fun `should flatMap to new Success`() {
            val result = success.flatMap { Result.success(123) }

            assertThat(result).isEqualTo(Result.success(123))
        }

        @Test fun `should get success value on getOrElse`() {
            val value = success.getOrElse { "not valid" }

            assertThat(value).isEqualTo("success")
        }

        @Test fun `should provide Success on recoveryWith`() {
            val result = success.recoverWith { Result.success(123) }

            assertThat(result).isEqualTo(success)
        }

        @Test fun `should provide Success on recover`() {
            val result = success.recover { 123 }

            assertThat(result).isEqualTo(success)
        }

        @Test fun `should transform success`() {
            val result = success.transform({ Result.success("other $it") }, { throw it })

            assertThat(result).isEqualTo(Result.success("other success"))
        }

    }

    class FailureTest {

        private val failure = Result.failure<String>(Throwable())

        @Test fun `should map to same Failure`() {
            val result = failure.map { "other $it" }

            assertThat(result).isEqualTo(failure)
        }

        @Test fun `should flatMap to same Failure`() {
            val result = failure.flatMap { Result.success(123) }

            assertThat(result).isEqualTo(failure)
        }

        @Test fun `should get alternative value on getOrElse`() {
            val value = failure.getOrElse { "alternative" }

            assertThat(value).isEqualTo("alternative")
        }

        @Test fun `should provide new Success on recoveryWith`() {
            val result = failure.recoverWith { Result.success(123) }

            assertThat(result).isEqualTo(Result.success(123))
        }

        @Test fun `should provide new Success on recover`() {
            val result = failure.recover { 123 }

            assertThat(result).isEqualTo(Result.success(123))
        }

        @Test fun `should transform failure`() {
            val result = failure.transform({ Result.success("other $it") }, { Result.success("alternative") })

            assertThat(result).isEqualTo(Result.success("alternative"))
        }

    }

}
