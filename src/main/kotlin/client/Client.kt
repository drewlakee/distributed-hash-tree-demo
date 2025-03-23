package client

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.contains
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
import java.time.Instant

class Client

private fun String.toJson(): String = JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(
    JSON_MAPPER.readValue(this.toByteArray())
)

private fun String.toJsonNode(): JsonNode = JSON_MAPPER.readTree(this)

data class Revision(
    var lastInitialDelivery: Long = 0,
    var lastUpdate: Long = 0,
    var singleHashElementsRange: Int = 0,
    var careersHashTree: List<Int> = listOf(),
    var careersCollection: List<String> = listOf(),
) {
    override fun toString(): String {
        return """
            {
                "lastInitialDelivery": $lastInitialDelivery,
                "lastInitialDeliveryHuman": "${Instant.ofEpochMilli(lastInitialDelivery)}",
                "lastUpdate": $lastUpdate,
                "lastUpdateHuman": "${Instant.ofEpochMilli(lastUpdate)}",
                "singleHashElementsRange": $singleHashElementsRange,
                "careersHashTree": $careersHashTree,
                "careersCollection": $careersCollection
            }
        """.trimIndent().toJson()
    }
}

enum class UpdateStrategy {
    DELTA_UPDATE,
    SKIP_UPDATE,
    INITIAL_DELIVERY,
}

data class Update(
    var lastUpdate: Long = 0,
    var singleHashElementsRange: Int? = null,
    var careersUpdateStrategy: UpdateStrategy = UpdateStrategy.INITIAL_DELIVERY,
    var careersHashTree: List<Int>? = null,
    var careersCollection: List<String>? = null,
) {
    override fun toString(): String {
        return """
            {
                "lastUpdate": $lastUpdate,
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
        var request = """
                        {
                            "lastUpdate": ${currentRevision.lastUpdate},
                            "singleHashElementsRange": ${currentRevision.singleHashElementsRange},
                            "careersHashTree": ${currentRevision.careersHashTree}
                        }
                    """.trimIndent()
        println("Request: $request")
        val update = http.send(
            HttpRequest.newBuilder()
                .POST(BodyPublishers.ofString(request))
                .header("Content-Type", "application/json")
                .uri(URI.create("http://localhost:8080/snapshot"))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        ).body().toJsonNode()
            .let {
                Update(
                    lastUpdate = it.get("lastUpdate").asLong(),
                    singleHashElementsRange = if (it.contains("singleHashElementsRange")) it.get("singleHashElementsRange").asInt() else null,
                    careersUpdateStrategy = it.get("careersUpdateStrategy").textValue().let { UpdateStrategy.valueOf(it) },
                    careersHashTree = if (it.contains("careersHashTree")) it.withArray<JsonNode>("careersHashTree").map { it.asInt() } else null,
                    careersCollection = if (it.contains("careersCollection")) it.withArray<JsonNode>("careersCollection").map { it.toPrettyString() } else null
                )
            }

        println("Current Revision: $currentRevision")

        when (update.careersUpdateStrategy) {
            UpdateStrategy.DELTA_UPDATE -> currentRevision.apply {
                careersCollection = delta(
                    currentRevision.singleHashElementsRange,
                    currentRevision.careersCollection,
                    currentRevision.careersHashTree,
                    update.careersCollection!!,
                    update.careersHashTree!!,
                )
                careersHashTree = update.careersHashTree!!
                lastUpdate = update.lastUpdate
            }
            UpdateStrategy.INITIAL_DELIVERY -> currentRevision.apply {
                careersCollection = update.careersCollection!!
                singleHashElementsRange = update.singleHashElementsRange!!
                careersHashTree = update.careersHashTree!!
                lastUpdate = update.lastUpdate
                lastInitialDelivery = update.lastUpdate
            }
            UpdateStrategy.SKIP_UPDATE -> {}
        }

        println("New Update: $update")
        println("New Revision: ${currentRevision}\n\n\n")
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