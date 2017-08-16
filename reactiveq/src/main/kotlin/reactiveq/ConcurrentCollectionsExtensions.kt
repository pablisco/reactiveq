package reactiveq

import java.util.*
import java.util.concurrent.ConcurrentHashMap

fun <T> concurrentSetOf() : MutableSet<T> = Collections.newSetFromMap(ConcurrentHashMap<T, Boolean>())

fun <T, P> concurrentMapOf() : MutableMap<T, P> = ConcurrentHashMap()