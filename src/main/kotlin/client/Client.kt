package client

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.delay
import worker.FS_PROPERTIES
import worker.JSON_MAPPER
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse

class Client

private fun String.toJson(): String = JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(
    JSON_MAPPER.readValue(this.toByteArray())
)

private fun String.toJsonNode(): JsonNode = JSON_MAPPER.readTree(this)

data class Revision(
    var singleHashElementsRange: Int = 0,
    var careersHashTree: List<Int> = listOf(),
    var careersCollection: List<String> = listOf(),
) {
    override fun toString(): String {
        return """
            {
                "singleHashElementsRange": $singleHashElementsRange,
                "careersHashTree": $careersHashTree,
                "careersCollection": $careersCollection
            }
        """.trimIndent().toJson()
    }
}

enum class UpdateStrategy {
    DELTA,
    INITIAL_DELIVERY,
}

data class Update(
    var singleHashElementsRange: Int = 0,
    var careersUpdateStrategy: UpdateStrategy = UpdateStrategy.INITIAL_DELIVERY,
    var careersHashTree: List<Int> = listOf(),
    var careersCollection: List<String> = listOf(),
) {
    override fun toString(): String {
        return """
            {
                "singleHashElementsRange": $singleHashElementsRange,
                "careersUpdateStrategy": "$careersUpdateStrategy",
                "careersHashTree": $careersHashTree,
                "careersCollection": $careersCollection
            }
        """.trimIndent().toJson()
    }
}

suspend fun main() {
    val http = HttpClient.newHttpClient()

    val currentRevision = Revision()
    periodic {
        val update = http.send(
            HttpRequest.newBuilder().POST(
                BodyPublishers.ofString(
                    """
                        {
                            "singleHashElementsRange": ${currentRevision.singleHashElementsRange},
                            "careersHashTree": ${currentRevision.careersHashTree}
                        }
                    """.trimIndent()
                )
            ).header("Content-Type", "application/json").uri(URI.create("http://localhost:8080/snapshot")).build(),
            HttpResponse.BodyHandlers.ofString()
        ).body().toJsonNode()
            .let {
                Update(
                    singleHashElementsRange = it.get("singleHashElementsRange").asInt(),
                    careersUpdateStrategy = it.get("careersUpdateStrategy").textValue().let { UpdateStrategy.valueOf(it) },
                    careersHashTree = it.withArray<JsonNode>("careersHashTree").map { it.asInt() },
                    careersCollection = it.withArray<JsonNode>("careersCollection").map { it.toPrettyString() }
                )
            }

        when (update.careersUpdateStrategy) {
            UpdateStrategy.DELTA -> currentRevision.apply {
                careersCollection = delta(
                    currentRevision.singleHashElementsRange,
                    currentRevision.careersCollection,
                    currentRevision.careersHashTree,
                    update.careersCollection,
                    update.careersHashTree,
                )
            }
            UpdateStrategy.INITIAL_DELIVERY -> currentRevision.apply {
                careersCollection = update.careersCollection
                singleHashElementsRange = update.singleHashElementsRange
            }
        }

        currentRevision.careersHashTree = update.careersHashTree

        println("New Update: $update")
        println("-----------APPLIED TO-----------")
        println("Current Revision: ${currentRevision}\n\n\n")
    }
}

fun delta(
    singleHashElementsRange: Int,
    currentCollection: List<String>,
    currentHashTree: List<Int>,
    updateCollection: List<String>,
    updateHashTree: List<Int>,
): List<String> {
    if (currentHashTree[0] == updateHashTree[0]) return currentCollection
    return buildList {
        var deltaOffset = 0
        for ((index, hash) in updateHashTree.withIndex().drop(1)) {
            if (index + 1 > currentHashTree.size) {
                addAll(
                    updateCollection.asSequence()
                        .drop(deltaOffset * singleHashElementsRange)
                        .take(singleHashElementsRange)
                        .toList()
                )
                deltaOffset++
            } else if (hash != currentHashTree[index]) {
                addAll(
                    updateCollection.asSequence()
                        .drop(deltaOffset * singleHashElementsRange)
                        .take(singleHashElementsRange)
                        .toList()
                )
                deltaOffset++
            } else {
                addAll(
                    currentCollection.asSequence()
                        .drop(singleHashElementsRange * (index - 1))
                        .take(singleHashElementsRange)
                        .toList()
                )
            }
        }
    }
}

private suspend fun periodic(runnable: () -> Unit) {
    while (true) {
        runnable.invoke()
        delay(delayMillis())
    }
}

private fun delayMillis(): Long = File(FS_PROPERTIES).readLines()
    .first { it.startsWith("client.periodic.millis") }
    .split("=")
    .last()
    .toLong().also { println("Client has been executed and delayed: $it ms") }