package worker

import com.fasterxml.jackson.databind.ObjectMapper
import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.runBlocking
import models.*
import java.io.File
import java.time.Instant

class Worker

const val FS_PROPERTIES = "C:\\Users\\user\\IdeaProjects\\sandbox\\src\\main\\resources\\application.properties"
val JSON_MAPPER = ObjectMapper()

private fun <T> T.toJsonAndHashCode(): Int = JSON_MAPPER.writeValueAsString(this).hashCode()
val NULL_HASH_CODE = "null".hashCode()

suspend fun main() {
    val careersCollection = MongoClient.create("mongodb://localhost:27017")
        .getDatabase("test")
        .getCollection<CareerRecord>("careers")
    val snapshotsCollection = MongoClient.create("mongodb://localhost:27017")
        .getDatabase("test")
        .getCollection<Snapshot>("snapshots")

    periodic {
        val careersCollection = runBlocking { careersCollection.find().toCollection(ArrayList()) }
            .map {
                HashedCareer(
                    hashCode = it.toJsonAndHashCode(),
                    item = it,
                )
            }
        val currentSnapshot = runBlocking { snapshotsCollection.find().firstOrNull() }
        val singleHashElementsRange = singleHashElementsRange()
        if (currentSnapshot == null) {
            runBlocking {
                snapshotsCollection.insertOne(
                    Snapshot(
                        created = Instant.now().toEpochMilli(),
                        lastForcedInitialDelivery = Instant.now().toEpochMilli(),
                        careersHashTree = calculateNewHashTree(careersCollection, singleHashElementsRange),
                        careersCollection = careersCollection,
                        singleHashElementsRange = singleHashElementsRange,
                    )
                )
            }
        } else {
            runBlocking {
                val mergedItems = mergeCollections(
                    currentItems = currentSnapshot.careersCollection,
                    newItems = careersCollection,
                    removed = HashedCareer(NULL_HASH_CODE)
                ) { career -> HashedCareer(career.hashCode, career.item) }

                snapshotsCollection.replaceOne(
                    Filters.eq(currentSnapshot.id),
                    currentSnapshot.apply {
                        this.created = Instant.now().toEpochMilli()
                        this.careersHashTree = calculateNewHashTree(mergedItems, singleHashElementsRange)
                        this.careersCollection = mergedItems
                        this.singleHashElementsRange = singleHashElementsRange
                    },
                )
            }
        }
    }
}

private fun <T : HashableEntity> mergeCollections(
    currentItems: List<T>,
    newItems: List<T>,
    removed: T,
    createHash: (T) -> T
): List<T> {
    val merged = mutableListOf<T>()
    val currentIterator = currentItems.iterator()
    val newIterator = newItems.iterator()

    var untilFirstNotRemovedNewItem: T? = null
    currentIterator.forEachRemaining { current ->
        if (newIterator.hasNext() || untilFirstNotRemovedNewItem != null) {
            val newItem: T
            if (untilFirstNotRemovedNewItem != null) {
                newItem = untilFirstNotRemovedNewItem!!
            } else {
                newItem = newIterator.next()
            }
            if (current.isRemoved()) {
                merged.add(current)
                untilFirstNotRemovedNewItem = newItem
                return@forEachRemaining
            } else if (current.identity() != newItem.identity()) {
                merged.add(removed)
                untilFirstNotRemovedNewItem = newItem
                return@forEachRemaining
            }

            merged.add(createHash(newItem))
            untilFirstNotRemovedNewItem = null
            return@forEachRemaining
        }

        // a current snapshot's tail is removed
        merged.add(removed)
    }

    untilFirstNotRemovedNewItem?.run { merged.add(this) }
    newIterator.forEachRemaining { newItem -> merged.add(createHash(newItem)) }

    return merged
}

private fun <T : HashableEntity> calculateNewHashTree(hashedItems: List<T>, singleHashElementsRange: Int): List<Int> {
    return hashedItems.windowed(singleHashElementsRange)
        .map { it.joinToString(prefix = "").hashCode() }
        .let { hashes ->
            buildList {
                add(hashes.joinToString(prefix = "").hashCode())
                addAll(hashes)
            }
        }
}

private suspend fun periodic(runnable: () -> Unit) {
    while (true) {
        runnable.invoke()
        delay(delayMillis())
    }
}

private fun singleHashElementsRange(): Int = File(FS_PROPERTIES).readLines()
    .first { it.startsWith("single.hash.elements.range") }
    .split("=")
    .last()
    .toInt().also { println("single hash elements range is $it") }

private fun delayMillis(): Long = File(FS_PROPERTIES).readLines()
    .first { it.startsWith("worker.periodic.millis") }
    .split("=")
    .last()
    .toLong().also { println("delayed for $it ms") }