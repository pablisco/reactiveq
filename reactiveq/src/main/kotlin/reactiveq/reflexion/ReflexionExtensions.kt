package reactiveq.reflexion

import java.io.Serializable
import java.lang.reflect.*
import java.lang.reflect.Array as ArrayType
import java.util.*

val EMPTY_TYPE_ARRAY: kotlin.Array<Type> = emptyArray()

/**
 * Returns a new parameterized type, applying {@code typeArguments} to
 * {@code rawType} and enclosed by {@code ownerType}.
 *
 * @return a {@link java.io.Serializable serializable} parameterized type.
 */
fun newParameterizedTypeWithOwner(
    ownerType: Type?,
    rawType: Type,
    vararg typeArguments: Type
): ParameterizedType = ParameterizedTypeImpl(ownerType, rawType, arrayOf(*typeArguments))

/**
 * Returns an array type whose elements are all instances of
 * {@code componentType}.
 *
 * @return a {@link java.io.Serializable serializable} generic array type.
 */
fun arrayTypeOf(componentType: Type): GenericArrayType =
    GenericArrayTypeImpl(componentType)

/**
 * Returns a type that represents an unknown type that extends {@code bound}.
 * For example, if {@code bound} is {@code CharSequence.class}, this returns
 * {@code ? extends CharSequence}. If {@code bound} is {@code Object.class},
 * this returns {@code ?}, which is shorthand for {@code ? extends Object}.
 */
fun subtypeOf(bound: Type): WildcardType = when (bound) {
        is WildcardType -> WildcardTypeImpl(bound.upperBounds, EMPTY_TYPE_ARRAY)
        else -> WildcardTypeImpl(arrayOf(bound), EMPTY_TYPE_ARRAY)
    }

/**
 * Returns a type that represents an unknown supertype of {@code bound}. For
 * example, if {@code bound} is {@code String.class}, this returns {@code ?
 * super String}.
 */
fun supertypeOf(bound: Type): WildcardType = when (bound) {
        is WildcardType -> WildcardTypeImpl(arrayOf<Type>(Object::class.java), bound.lowerBounds)
        else -> WildcardTypeImpl(arrayOf<Type>(Object::class.java), arrayOf(bound))
    }

/**
 * Returns a type that is functionally equal but not necessarily equal
 * according to {@link Object#equals(Object) Object.equals()}. The returned
 * type is {@link java.io.Serializable}.
 */
fun canonicalize(type: Type): Type {
    return when {
        type is Class<*> && type.isArray -> GenericArrayTypeImpl(canonicalize(type.componentType))
        type is ParameterizedType -> ParameterizedTypeImpl(type.ownerType, type.rawType, type.actualTypeArguments)
        type is GenericArrayType -> GenericArrayTypeImpl(type.genericComponentType)
        type is WildcardType -> WildcardTypeImpl(type.upperBounds, type.lowerBounds)
        else -> type
    }
}

fun getRawType(type: Type?): Class<*> {
    return when (type) {
        is Class<*> -> type
        is ParameterizedType -> type.rawType as Class<*>
        is GenericArrayType -> ArrayType.newInstance(getRawType(type.genericComponentType), 0).javaClass
        is TypeVariable<*> -> Object::class.java
        is WildcardType -> getRawType(type.upperBounds[0])
        else -> throw IllegalArgumentException("Expected a Class, ParameterizedType, or " +
            "GenericArrayType, but <$type> is of type ${type?.javaClass?.name}")
    }
}

/**
 * Returns true if {@code a} and {@code b} are equal.
 */
fun equals(a: Type, b: Type): Boolean {
    return a === b ||
        a is ParameterizedType && b is ParameterizedType && equals(a, b) ||
        a is GenericArrayType && b is GenericArrayType && equals(a, b) ||
        a is WildcardType && b is WildcardType && equals(a, b) ||
        a is TypeVariable<*> && b is TypeVariable<*> && equals(a, b)
}

fun equals(a: ParameterizedType, b: ParameterizedType): Boolean =
    a.ownerType == b.ownerType
        && a.rawType == b.rawType
        && Arrays.equals(a.actualTypeArguments, b.actualTypeArguments)

fun equals(a: GenericArrayType, b: GenericArrayType): Boolean =
    a.genericComponentType == b.genericComponentType

fun equals(a: WildcardType, b: WildcardType): Boolean =
    Arrays.equals(a.lowerBounds, b.lowerBounds)
        && Arrays.equals(a.upperBounds, b.upperBounds)

fun equals(a: TypeVariable<*>, b: TypeVariable<*>): Boolean =
    a.genericDeclaration == b.genericDeclaration && a.name == b.name

fun hashCodeOrZero(o: Any?): Int = o?.hashCode() ?: 0

fun typeToString(type: Type): String = if (type is Class<*>) type.name else type.toString()

/**
 * Returns the generic supertype for {@code supertype}. For example, given a class {@code
 * IntegerSet}, the result for when supertype is {@code Set.class} is {@code Set<Integer>} and the
 * result when the supertype is {@code Collection.class} is {@code Collection<Integer>}.
 */
fun getGenericSupertype(context: Type, rawType: Class<*>, toResolve: Class<*>): Type {
    if (toResolve == rawType) {
        return context
    } else {
        if (toResolve.isInterface) {
            val firstOrNull = rawType.interfaces
                .mapIndexed { i, c ->
                    when {
                        c == toResolve -> rawType.genericInterfaces[i]
                        toResolve.isAssignableFrom(c) -> getGenericSupertype(rawType.genericInterfaces[i], c, toResolve)
                        else -> null
                    }
                }
                .filterNotNull().firstOrNull()
            if (firstOrNull != null) {
                return firstOrNull
            }
        }

        if (!rawType.isInterface) {
            val candidate = generateSequence(rawType) { it.superclass }
                .filter { it == toResolve }
                .filter { toResolve.isAssignableFrom(it) }
                .firstOrNull()
            if (candidate != null) {
                return candidate
            }
        }
        return toResolve
    }
}

/**
 * Returns the generic form of {@code supertype}. For example, if this is {@code
 * ArrayList<String>}, this returns {@code Iterable<String>} given the input {@code
 * Iterable.class}.
 *
 * @param supertype a superclass of, or interface implemented by, this.
 */
fun getSupertype(context: Type, contextRawType: Class<*>, supertype: Class<*>): Type {
    require(supertype.isAssignableFrom(contextRawType))
    return resolve(context, contextRawType, getGenericSupertype(context, contextRawType, supertype))
}

/**
 * Returns the component type of this array type.
 * @throws ClassCastException if this type is not an array.
 */
fun getArrayComponentType(array: Type): Type =
    when (array) {
        is GenericArrayType -> array.genericComponentType
        else -> (array as Class<*>).componentType
    }

/**
 * Returns the element type of this collection type.
 * @throws IllegalArgumentException if this type is not a collection.
 */
fun getCollectionElementType(context: Type, contextRawType: Class<*>): Type =
    getSupertype(context, contextRawType, Collection::class.java).run {
        when (this) {
            is WildcardType -> upperBounds[0]
            is ParameterizedType -> actualTypeArguments[0]
            else -> Object::class.java
        }
    }

/**
 * Returns a two element array containing this map's key and value types in
 * positions 0 and 1 respectively.
 */
fun getMapKeyAndValueTypes(context: Type, contextRawType: Class<*>): kotlin.Array<Type> {
    /*
     * Work around a problem with the declaration of java.util.Properties. That
     * class should extend Hashtable<String, String>, but it's declared to
     * extend Hashtable<Object, Object>.
     */
    if (context == Properties::class.java) {
        return arrayOf(String::class.java, String::class.java) // TODO: test subclasses of Properties!
    }

    val mapType = getSupertype(context, contextRawType, Map::class.java)
    // TODO: strip wildcards?
    return when (mapType) {
        is ParameterizedType -> mapType.actualTypeArguments
        else -> arrayOf(Objects::class.java, Object::class.java)
    }
}

fun resolve(context: Type, contextRawType: Class<*>, toResolve: Type): Type {
    return resolve(context, contextRawType, toResolve, mutableSetOf())
}

fun resolve(context: Type, contextRawType: Class<*>, toResolve: Type, visitedTypeVariables: MutableCollection<TypeVariable<*>>): Type {
    // this implementation is made a little more complicated in an attempt to avoid object-creation
    while (true) {
        if (toResolve is TypeVariable<*>) {
            if (visitedTypeVariables.contains(toResolve)) {
                // cannot reduce due to infinite recursion
                return toResolve
            } else {
                visitedTypeVariables.add(toResolve)
            }
            if (toResolve == resolveTypeVariable(context, contextRawType, toResolve)) {
                return toResolve
            }

        } else if (toResolve is Class<*> && toResolve.isArray) {
            val componentType = toResolve.componentType
            val newComponentType = resolve(context, contextRawType, componentType, visitedTypeVariables)
            return if (componentType == newComponentType) toResolve else arrayTypeOf(newComponentType)
        } else if (toResolve is GenericArrayType) {
            val componentType = toResolve.genericComponentType
            val newComponentType = resolve(context, contextRawType, componentType, visitedTypeVariables);
            return if (componentType == newComponentType) toResolve else arrayTypeOf(newComponentType);
        } else if (toResolve is ParameterizedType) {
            val ownerType = toResolve.ownerType
            val newOwnerType = resolve(context, contextRawType, ownerType, visitedTypeVariables);
            var changed = newOwnerType != ownerType

            return toResolve.actualTypeArguments
                .map { arg ->
                    resolve(context, contextRawType, arg, visitedTypeVariables).also {
                        changed = changed || (it != arg)
                    }
                }
                .let {
                    if (changed) newParameterizedTypeWithOwner(newOwnerType, toResolve.rawType, *it.toTypedArray())
                    else toResolve
                }
        } else if (toResolve is WildcardType) {
            val originalLowerBound = toResolve.lowerBounds
            val originalUpperBound = toResolve.upperBounds
            if (originalLowerBound.size == 1) {
                val lowerBound = resolve(context, contextRawType, originalLowerBound[0], visitedTypeVariables);
                if (lowerBound != originalLowerBound[0]) {
                    return supertypeOf(lowerBound)
                }
            } else if (originalUpperBound.size == 1) {
                val upperBound = resolve(context, contextRawType, originalUpperBound[0], visitedTypeVariables);
                if (upperBound != originalUpperBound[0]) {
                    return subtypeOf(upperBound)
                }
            }
            return toResolve
        } else {
            return toResolve
        }
    }
}

fun resolveTypeVariable(context: Type, contextRawType: Class<*>, unknown: TypeVariable<*>): Type {
    val declaredByRaw = declaringClassOf(unknown)

    // we can't reduce this further
    return if (declaredByRaw != null) {
        val declaredBy = getGenericSupertype(context, contextRawType, declaredByRaw)
        when (declaredBy) {
            is ParameterizedType -> {
                val i = indexOf(declaredByRaw.typeParameters, unknown)
                declaredBy.actualTypeArguments[i]
            }
            else -> unknown
        }
    } else {
        unknown
    }
}

fun indexOf(array: kotlin.Array<*>, toFind: Any): Int {
    val i = array.indexOf(toFind)
    return if (i >= 0) {
        i
    } else {
        throw NoSuchElementException()
    }

}

/**
 * Returns the declaring class of {@code typeVariable}, or {@code null} if it was not declared by
 * a class.
 */
fun declaringClassOf(typeVariable: TypeVariable<*>): Class<*>? =
    typeVariable.genericDeclaration.run { this as? Class<*> }

fun checkNotPrimitive(type: Type) {
    require(type !is Class<*> || !type.isPrimitive)
}

internal class ParameterizedTypeImpl private constructor(
    private val _ownerType: Type?,
    private val _rawType: Type,
    private val _typeArguments: kotlin.Array<Type>
) : ParameterizedType, Serializable {

    companion object {

        operator fun invoke(original: ParameterizedType) =
            invoke(original.ownerType, original.rawType, original.actualTypeArguments)

        operator fun invoke(ownerType: Type?, rawType: Type, typeArguments: kotlin.Array<Type>): ParameterizedType {
            if (rawType is Class<*>) {
                val isStaticOrTopLevelClass = Modifier.isStatic(rawType.modifiers) || rawType.enclosingClass == null
                require(ownerType != null || isStaticOrTopLevelClass)
            }
            val ownerType = if (ownerType == null) null else canonicalize(ownerType)
            val rawType = canonicalize(rawType)
            val typeArguments = typeArguments.clone().map {
                requireNotNull(it)
                checkNotPrimitive(it)
                canonicalize(it)
            }.toTypedArray()
            return ParameterizedTypeImpl(ownerType, rawType, typeArguments)
        }

    }

    override fun getActualTypeArguments(): Array<Type> = _typeArguments

    override fun getRawType(): Type = _rawType

    override fun getOwnerType(): Type? = _ownerType

    override fun equals(other: Any?): Boolean =
        other is ParameterizedType && equals(this, other)

    override fun hashCode(): Int =
        Arrays.hashCode(_typeArguments) xor _rawType.hashCode() xor hashCodeOrZero(_ownerType)

    override fun toString(): String =
        if (_typeArguments.isEmpty()) {
            typeToString(rawType)
        } else {
            "${typeToString(rawType)}<${_typeArguments.joinToString { typeToString(it) }}>"
        }

}

internal class GenericArrayTypeImpl(
    private val componentType: Type
) : GenericArrayType, Serializable {
    companion object {

        operator fun invoke(original: GenericArrayType) =
            GenericArrayTypeImpl(original.genericComponentType)

        operator fun invoke(componentType: Type) =
            GenericArrayTypeImpl(canonicalize(componentType))

    }

    override fun getGenericComponentType(): Type = componentType

    override fun equals(other: Any?): Boolean = other is GenericArrayType && equals(this, other)

    override fun hashCode(): Int = componentType.hashCode()

    override fun toString(): String = typeToString(componentType) + "[]"

}

/**
 * The WildcardType interface supports multiple upper bounds and multiple
 * lower bounds. We only support what the Java 6 language needs - at most one
 * bound. If a lower bound is set, the upper bound must be Object.class.
 */
internal class WildcardTypeImpl(
    private val _upperBound: Type,
    private val _lowerBound: Type?
) : WildcardType, Serializable {

    companion object {

        operator fun invoke(original: WildcardType) =
            WildcardTypeImpl(original.upperBounds, original.upperBounds)

        operator fun invoke(upperBounds: Array<Type>, lowerBounds: Array<Type>): WildcardTypeImpl {
            require(lowerBounds.size <= 1)
            require(upperBounds.size == 1)
            return if (lowerBounds.size == 1) {
                requireNotNull(lowerBounds[0])
                checkNotPrimitive(lowerBounds[0])
                require(upperBounds[0] == Object::class.java)
                WildcardTypeImpl(Object::class.java, canonicalize(lowerBounds[0]))
            } else {
                requireNotNull(upperBounds[0])
                checkNotPrimitive(upperBounds[0])
                WildcardTypeImpl(canonicalize(upperBounds[0]), null)
            }
        }

    }

    override fun getUpperBounds(): Array<Type> = arrayOf(_upperBound)

    override fun getLowerBounds(): Array<Type> = if (_lowerBound != null) arrayOf(_lowerBound) else EMPTY_TYPE_ARRAY

    override fun equals(other: Any?): Boolean = other is WildcardType && equals(this, other)

    override fun hashCode(): Int =
        (if (_lowerBound != null) 31 + _lowerBound.hashCode() else 1) xor (31 + _upperBound.hashCode())

    override fun toString(): String = when {
        _lowerBound != null -> "? super " + typeToString(_lowerBound)
        upperBounds == Object::class.java -> "?"
        else -> "? extends " + typeToString(_upperBound)
    }

}
