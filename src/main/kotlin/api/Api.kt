package api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(
    scanBasePackages = ["api"]
)
class Api

fun main(args: Array<String>) {
    runApplication<Api>(*args)
}