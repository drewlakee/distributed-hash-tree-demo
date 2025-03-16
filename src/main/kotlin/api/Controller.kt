package api

import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.flow.firstOrNull
import models.CareerRecord
import models.Snapshot
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

data class Request(
    val singleHashElementsRange: Int,
    val careersHashTree: List<Int>,
)

class Response {
    var singleHashElementsRange: Int = 0
    var careersHashTree: List<Int> = listOf()
    var careersCollection: List<CareerRecord?> = listOf()
}

@RestController
class Controller {

    @Autowired
    lateinit var snapshotsCollection: MongoCollection<Snapshot>

    @PostMapping(
        value = ["/snapshot"],
        consumes = ["application/json"],
        produces = ["application/json"]
    )
    suspend fun post(@RequestBody request: Request): ResponseEntity<Response> {
        val snapshot = snapshotsCollection.find().firstOrNull() ?: return ResponseEntity.ok(Response())
        return Response().apply {
            singleHashElementsRange = snapshot.singleHashElementsRange
            careersHashTree = snapshot.careersHashTree
            careersCollection = listOf()

            if (
                request.careersHashTree.isEmpty()
                || request.singleHashElementsRange != snapshot.singleHashElementsRange
                || request.careersHashTree.size > snapshot.careersHashTree.size
            ) {
                careersCollection = snapshot.careersCollection.map { it.item }
            } else if (request.careersHashTree[0] != snapshot.careersHashTree[0] || request.careersHashTree.size <= snapshot.careersHashTree.size) {
                careersCollection = buildList {
                    for ((index, hash) in snapshot.careersHashTree.withIndex().drop(1)) {
                        if (request.careersHashTree.size < index + 1) {
                            addAll(
                                snapshot.careersCollection.asSequence()
                                    .map { it.item }
                                    .drop(snapshot.singleHashElementsRange * (index - 1))
                                    .take(snapshot.singleHashElementsRange)
                                    .toList()
                            )
                        } else if (hash != request.careersHashTree[index]) {
                            addAll(
                                snapshot.careersCollection.asSequence()
                                    .map { it.item }
                                    .drop(snapshot.singleHashElementsRange * (index - 1))
                                    .take(snapshot.singleHashElementsRange)
                                    .toList()
                            )
                        }
                    }
                }
            }
        }.let { ResponseEntity.ok(it) }
    }
}