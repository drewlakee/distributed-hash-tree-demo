package api

import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoCollection
import models.CareerRecord
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ApiContextConfiguration {

    @Bean
    fun careerRecordsCollection(): MongoCollection<CareerRecord> = MongoClient.create("mongodb://localhost:27017")
        .getDatabase("test")
        .getCollection<CareerRecord>("documents")
}