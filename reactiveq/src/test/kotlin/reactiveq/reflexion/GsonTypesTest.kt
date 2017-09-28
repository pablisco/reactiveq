package reactiveq.reflexion

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

import junit.framework.TestCase
import org.assertj.core.api.Assertions
import org.junit.Assert
import org.junit.Test

class GsonTypesTest {

    @Test fun testNewParameterizedTypeWithoutOwner() {
        // List<A>. List is a top-level class
        var type = newParameterizedTypeWithOwner(null, List::class.java, A::class.java)
        TestCase.assertEquals(A::class.java, getFirstTypeArgument(type))

        // A<B>. A is a static inner class.
        type = newParameterizedTypeWithOwner(null, A::class.java, B::class.java)
        TestCase.assertEquals(B::class.java, getFirstTypeArgument(type))

        try {
            // D<A> is not allowed since D is not a static inner class
            val rawType = D::class.java
            val typeArguments = A::class.java
            newParameterizedTypeWithOwner(null, rawType, typeArguments)
            Assertions.failBecauseExceptionWasNotThrown(IllegalArgumentException::class.java)
        } catch (expected: IllegalArgumentException) {
        }

        // A<D> is allowed.
        type = newParameterizedTypeWithOwner(null, A::class.java, D::class.java)
        TestCase.assertEquals(D::class.java, getFirstTypeArgument(type))
    }

    @Test fun testGetFirstTypeArgument() {
        TestCase.assertNull(getFirstTypeArgument(A::class.java))

        val type = newParameterizedTypeWithOwner(null, A::class.java, B::class.java, C::class.java)
        TestCase.assertEquals(B::class.java, getFirstTypeArgument(type))
    }

    private class A
    private class B
    private class C
    private inner class D

    companion object {

        /**
         * Given a parameterized type A&lt;B,C&gt;, returns B. If the specified type is not
         * a generic type, returns null.
         */
        @Throws(Exception::class)
        fun getFirstTypeArgument(type: Type): Type? {
            if (type !is ParameterizedType) return null
            val actualTypeArguments = type.actualTypeArguments
            return if (actualTypeArguments.isEmpty()) null else canonicalize(actualTypeArguments[0])
        }
    }
}