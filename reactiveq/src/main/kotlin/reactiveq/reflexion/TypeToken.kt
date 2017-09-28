package reactiveq.reflexion

import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.util.*

abstract class TypeToken<T> {

    val type: Type
    val rawType: Class<T>
    val hashCode: Int

    companion object {

        inline fun <reified T> get() = object : TypeToken<T>(T::class.java) {}

        /**
         * Gets type literal for the array type whose elements are all instances of {@code componentType}.
         */
        fun getArray(componentType: Type): TypeToken<*> = object : TypeToken<Any>(arrayTypeOf(componentType)) {}

        /**
         * Returns the type from super class's type parameter in {@link $Gson$Types#canonicalize
         * canonical form}.
         */
        fun getSuperclassTypeParameter(subclass: Class<*>): Type {
            val superclass = subclass.genericSuperclass
            if (superclass is Class<*>) {
                throw RuntimeException("Missing type parameter.")
            }
            val parameterized = superclass as ParameterizedType
            return canonicalize(parameterized.actualTypeArguments[0])
        }

        /**
         * Gets type literal for the given {@code Type} instance.
         */
        fun get(type: Type): TypeToken<*> = object : TypeToken<Any>(type) {}

        /**
         * Gets type literal for the given {@code Class} instance.
         */
        fun <T> get(type: Class<T>): TypeToken<T> = object : TypeToken<T>(type) {}

        /**
         * Gets type literal for the parameterized type represented by applying {@code typeArguments} to
         * {@code rawType}.
         */
        fun getParameterized(rawType: Type, vararg typeArguments: Type): TypeToken<*> =
            object : TypeToken<Any>(newParameterizedTypeWithOwner(null, rawType, *typeArguments)) {}

        /**
         * Private helper function that performs some assignability checks for
         * the provided GenericArrayType.
         */
        private fun isAssignableFrom(from: Type, to: GenericArrayType): Boolean {
            val toGenericComponentType = to.genericComponentType
            if (toGenericComponentType is ParameterizedType) {
                var t = from
                if (from is GenericArrayType) {
                    t = from.genericComponentType;
                } else if (from is Class<*>) {
                    var classType: Class<*> = from
                    while (classType.isArray) {
                        classType = classType.componentType
                    }
                    t = classType
                }
                return isAssignableFrom(t, toGenericComponentType, HashMap())
            }
            // No generic defined on "to"; therefore, return true and let other
            // checks determine assignability
            return true
        }

        /**
         * Private recursive helper function to actually do the type-safe checking
         * of assignability.
         */
        private fun isAssignableFrom(from: Type?, to: ParameterizedType, typeVarMap: MutableMap<String, Type>): Boolean {
            // First figure out the class and any type information.
            val clazz by lazy { getRawType(from) }
            when(from) {
                null -> return false
                to -> return true
                is ParameterizedType -> {
                    val tArgs = from.actualTypeArguments
                    val tParams = clazz.typeParameters
                    tArgs.zip(tParams).forEach { (arg, value) ->
                        var tmp = arg
                        while (tmp is TypeVariable<*>) {
                            tmp = typeVarMap[tmp.name]
                        }
                        typeVarMap[value.name] = arg
                    }

                    // check if they are equivalent under our current mapping.
                    if (typeEquals(from, to, typeVarMap)) {
                        return true
                    } else {
                        if(clazz.genericInterfaces.any { isAssignableFrom(it, to, HashMap(typeVarMap)) }) {
                            return true
                        }
                    }
                }
            }

            // Interfaces didn't work, try the superclass.
            val sType = clazz.genericSuperclass
            return isAssignableFrom(sType, to, HashMap(typeVarMap))
        }

        /**
         * Checks if two parameterized types are exactly equal, under the variable
         * replacement described in the typeVarMap.
         */
        private fun typeEquals(from: ParameterizedType, to: ParameterizedType, typeVarMap: Map<String, Type>): Boolean =
            if (from.rawType == to.rawType) {
                from.actualTypeArguments
                    .zip(to.actualTypeArguments)
                    .all { (from, to) -> matches(from, to, typeVarMap) }
            } else {
                false
            }

        private fun buildUnexpectedTypeError(token: Type, vararg expected: Class<*>): AssertionError {
            // Build exception message
            return AssertionError("Unexpected type. Expected one of: ${expected.joinToString { it.name }}" +
                " but got: ${token.javaClass.name}, for type token: $token.")
        }

        /**
         * Checks if two types are the same or are equivalent under a variable mapping
         * given in the type map that was provided.
         */
        private fun matches(from: Type, to: Type, typeMap: Map<String, Type>): Boolean =
            to == from || (from is TypeVariable<*> && to == typeMap[from.name])

    }

    @Suppress("UNCHECKED_CAST")
    protected constructor() {
        this.type = getSuperclassTypeParameter(javaClass)
        this.rawType = getRawType(type) as Class<T>
        this.hashCode = type.hashCode()
    }

    @Suppress("UNCHECKED_CAST")
    protected constructor(type: Type) {
        this.type = canonicalize(type)
        this.rawType = getRawType(type) as Class<T>
        this.hashCode = type.hashCode()
    }

    /**
     * Check if this type is assignable from the given class object.
     *
     * @deprecated this implementation may be inconsistent with javac for types
     *     with wildcards.
     */
    fun isAssignableFrom(cls: Class<*>): Boolean {
        return isAssignableFrom(cls as Type)
    }

    /**
     * Check if this type is assignable from the given Type.
     *
     * @deprecated this implementation may be inconsistent with javac for types
     *     with wildcards.
     */
    fun isAssignableFrom(from: Type?): Boolean = when {
        from == null -> false
        type == from -> true
        else -> when (type) {
            is Class<*> -> rawType.isAssignableFrom(getRawType(from))
            is ParameterizedType -> isAssignableFrom(from, type, HashMap())
            is GenericArrayType -> rawType.isAssignableFrom(getRawType(from)) && isAssignableFrom(from, type)
            else -> throw buildUnexpectedTypeError(type, Class::class.java, ParameterizedType::class.java, GenericArrayType::class.java)
        }
    }

    /**
     * Check if this type is assignable from the given type token.
     *
     * @deprecated this implementation may be inconsistent with javac for types
     *     with wildcards.
     */
    fun isAssignableFrom(token: TypeToken<*>): Boolean {
        return isAssignableFrom(token.type)
    }

    override fun hashCode(): Int = this.hashCode

    override fun equals(other: Any?): Boolean =
        other is TypeToken<*> && equals(type, other.type)

    override fun toString(): String =
        typeToString(type)

}
