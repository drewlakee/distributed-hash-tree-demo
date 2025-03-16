package models

import org.bson.codecs.pojo.annotations.BsonId
import java.util.*

data class Snapshot(
    @BsonId
    var id: String = UUID.randomUUID().toString(),
    var created: Long,
    // a point at time which insists on initial snapshot delivery on clients
    var lastForcedInitialDelivery: Long,
    var singleHashElementsRange: Int,
    var careersHashTree: List<Int>,
    var careersCollection: List<HashedCareer>,
)
