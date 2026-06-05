package com.identityeudi.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class IdentityEudiBackendApplication

fun main(args: Array<String>) {
    runApplication<IdentityEudiBackendApplication>(*args)
}
