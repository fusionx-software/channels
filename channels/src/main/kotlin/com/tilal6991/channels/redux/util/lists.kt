package com.tilal6991.channels.redux.util

import com.github.andrewoma.dexx.collection.IndexedList

inline fun <T> TransactingIndexedList<T>.transform(fn: (T) -> T): TransactingIndexedList<T> {
    var list = this
    for (i in 0..size() - 1) {
        val old = list.get(i)
        val new = fn(old)
        if (old !== new) {
            list = list.set(i, new)
        }
    }
    return list
}

fun <T : Comparable<T>> TransactingIndexedList<T>.addSorted(elem: T): TransactingIndexedList<T> {
    val index = binarySearch(elem) { it }
    val elementsBefore = if (index >= 0) index + 1 else -index - 1
    return addAt(elem, elementsBefore)
}

fun <T, U : Comparable<U>> IndexedList<T>.binarySearch(elem: U, selector: (T) -> U): Int {
    var low = 0
    var high = size() - 1

    while (low <= high) {
        val mid = (low + high).ushr(1) // safe from overflows
        val midVal = get(mid)
        val cmp = selector(midVal).compareTo(elem)

        if (cmp < 0) {
            low = mid + 1
        } else if (cmp > 0) {
            high = mid - 1
        } else {
            return mid
        }
    }
    return -(low + 1)
}