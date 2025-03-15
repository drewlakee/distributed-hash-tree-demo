package models

import org.bson.codecs.pojo.annotations.BsonId
import java.util.*

data class Snapshot(
    @BsonId
    var id: String = UUID.randomUUID().toString(),
    var lastSnapshotCreation: Long,
    var lastGarbageCollectionUpdate: Long,
    var careersHashTree: List<Int>,
    var careersCollection: List<HashedCareer>,
)
