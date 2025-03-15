package models

import org.bson.codecs.pojo.annotations.BsonId

data class CareerRecord(
    @BsonId
    var id: Int,
    var name: String,
    var career: Career,
)
