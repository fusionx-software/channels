package com.tilal6991.channels.redux.util

import com.github.andrewoma.dexx.collection.*
import com.github.andrewoma.dexx.collection.Vector
import java.util.ArrayList

class TransactingIndexedList<T> private constructor(
        private val actual: IndexedList<T>,
        val transactions: IndexedList<Transaction>,
        private val maxSize: Int,
        private val runningCount: Int) : IndexedList<T> by actual {

    constructor(maxSize: Int = DEFAULT_MAX_SIZE) : this(Vector.empty(), Vector.empty(), maxSize, 0)

    fun transactionNumber(): Int {
        return runningCount
    }

    fun maxSize(): Int {
        return maxSize
    }

    override fun drop(number: Int): TransactingIndexedList<T> {
        // TODO(tilal6991) what if number is greater than or equal to the size of the list.
        val list = if (number <= 0) transactions else maxAppend(transactions, Transaction(REMOVE, 0, -1, number))
        return TransactingIndexedList(actual.drop(number), list, maxSize, runningCount + 1)
    }

    override fun append(elem: T): TransactingIndexedList<T> {
        return TransactingIndexedList(actual.append(elem), maxAppend(transactions,
                Transaction(ADD, actual.size(), -1, 1)), maxSize, runningCount + 1)
    }

    override fun take(number: Int): TransactingIndexedList<T> {
        // TODO(tilal6991) what if number is greater than the less than or equal to zero.
        val list = if (number >= size()) transactions else maxAppend(transactions, Transaction(REMOVE, number, -1, size() - number))
        return TransactingIndexedList(actual.take(number), list, maxSize, runningCount + 1)
    }

    override fun range(from: Int, fromInclusive: Boolean, to: Int, toInclusive: Boolean): TransactingIndexedList<T> {
        val firstEnd = if (fromInclusive) from else from + 1
        val secondStart = if (toInclusive) to + 1 else to

        var rc = runningCount
        var list: IndexedList<Transaction> = transactions
        if (firstEnd != 0) {
            list = maxAppend(list, Transaction(REMOVE, 0, -1, firstEnd))
            rc++
        }
        if (secondStart != size()) {
            list = maxAppend(list, Transaction(REMOVE, secondStart, -1, size() - secondStart))
            rc++
        }

        return TransactingIndexedList(actual.range(from, fromInclusive, to, toInclusive), list, maxSize, rc)
    }

    override fun set(i: Int, elem: T): TransactingIndexedList<T> {
        if (get(i) === elem) {
            return this
        }

        return TransactingIndexedList(actual.set(i, elem),
                maxAppend(transactions, Transaction(CHANGE, i, -1, 1)), maxSize, runningCount + 1)
    }

    override fun prepend(elem: T): TransactingIndexedList<T> {
        return TransactingIndexedList(actual.prepend(elem), maxAppend(transactions,
                Transaction(ADD, 0, -1, 1)), maxSize, runningCount + 1)
    }

    fun addAt(elem: T, index: Int): TransactingIndexedList<T> {
        val list = IndexedLists.builder<T>()
                .addAll(actual.take(index) as Traversable<T>)
                .add(elem)
                .addAll(actual.drop(index) as Traversable<T>)
                .build()
        return TransactingIndexedList(list, maxAppend(transactions,
                Transaction(ADD, index, -1, 1)), maxSize, runningCount + 1)
    }

    fun removeAt(index: Int): TransactingIndexedList<T> {
        val list = IndexedLists.builder<T>()
                .addAll(actual.take(index) as Traversable<T>)
                .addAll(actual.drop(index + 1) as Traversable<T>)
                .build()
        return TransactingIndexedList(list, maxAppend(transactions,
                Transaction(REMOVE, index, -1, 1)), maxSize, runningCount + 1)
    }

    fun move(fromIndex: Int, toIndex: Int): TransactingIndexedList<T> {
        val builder = IndexedLists.builder<T>()
        if (fromIndex == toIndex) {
            return this
        } else if (fromIndex < toIndex) {
            builder.addAll(actual.take(fromIndex) as Traversable<T>)
                    .addAll(actual.range(fromIndex, false, toIndex, false) as Traversable<T>)
                    .add(actual.get(fromIndex))
                    .addAll(actual.range(toIndex, true, size(), false) as Traversable<T>)
        } else {
            builder.addAll(actual.take(toIndex) as Traversable<T>)
                    .add(actual.get(fromIndex))
                    .addAll(actual.range(toIndex, true, fromIndex, false) as Traversable<T>)
                    .addAll(actual.range(fromIndex, false, size(), false) as Traversable<T>)
        }

        return TransactingIndexedList(builder.build(), maxAppend(transactions,
                Transaction(MOVE, fromIndex, toIndex, 1)), maxSize, runningCount + 1)
    }

    private fun maxAppend(list: IndexedList<Transaction>, elem: Transaction): IndexedList<Transaction> {
        if (maxSize >= list.size()) {
            val i = maxSize - list.size() - 1
            return list.drop(i).append(elem)
        }
        return list.append(elem)
    }

    class TransactingBuilder<T> internal constructor() : Builder<T, TransactingIndexedList<T>> {

        private val transactions = ArrayList<Transaction>()
        private var actualBuilder: Builder<T, IndexedList<T>> = IndexedLists.builder<T>()
        private var count = 0
        private var maxSize = DEFAULT_MAX_SIZE

        override fun add(element: T): Builder<T, TransactingIndexedList<T>> {
            transactions.add(Transaction(ADD, count++, -1, 1))
            actualBuilder = actualBuilder.add(element)
            return this
        }

        override fun addAll(elements: Traversable<T>): Builder<T, TransactingIndexedList<T>> {
            transactions.add(Transaction(ADD, count, -1, elements.size()))
            count += elements.size()
            actualBuilder = actualBuilder.addAll(elements)
            return this
        }

        override fun addAll(elements: MutableIterable<T>): Builder<T, TransactingIndexedList<T>> {
            var runningCount = 0
            for (i in elements) {
                actualBuilder = actualBuilder.add(i)
                runningCount++
            }
            transactions.add(Transaction(ADD, count, -1, runningCount))
            count += runningCount
            return this
        }

        override fun addAll(iterator: MutableIterator<T>): Builder<T, TransactingIndexedList<T>> {
            var runningCount = 0
            for (i in iterator) {
                actualBuilder = actualBuilder.add(i)
                runningCount++
            }
            transactions.add(Transaction(ADD, count, -1, runningCount))
            count += runningCount
            return this
        }

        override fun addAll(e1: T, e2: T, vararg es: T): Builder<T, TransactingIndexedList<T>> {
            var runningCount = 2
            actualBuilder = actualBuilder.add(e1).add(e2)
            for (i in es) {
                actualBuilder = actualBuilder.add(i)
                runningCount++
            }
            transactions.add(Transaction(ADD, count, -1, runningCount))
            count += runningCount
            return this
        }

        fun maxSize(size: Int) {
            maxSize = size
        }

        override fun build(): TransactingIndexedList<T> {
            return TransactingIndexedList(actualBuilder.build(),
                    IndexedLists.copyOf(transactions),
                    maxSize,
                    transactions.size)
        }
    }

    class Transaction(val type: Int, val startIndex: Int, val toIndex: Int, val count: Int)

    companion object {
        const val ADD = 1
        const val REMOVE = 2
        const val CHANGE = 3
        const val MOVE = 4
        const val DEFAULT_MAX_SIZE = 100

        fun <T> builder(): TransactingIndexedList.TransactingBuilder<T> {
            return TransactingIndexedList.TransactingBuilder()
        }
    }
}