package api

import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoCollection
import models.Snapshot
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ApiContextConfiguration {

    @Bean
    fun snapshotCollection(): MongoCollection<Snapshot> = MongoClient.create("mongodb://localhost:27017")
        .getDatabase("test")
        .getCollection<Snapshot>("snapshots")
}