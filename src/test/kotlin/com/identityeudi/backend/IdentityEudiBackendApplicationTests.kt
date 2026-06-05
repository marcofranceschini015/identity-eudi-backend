package com.identityeudi.backend

import com.identityeudi.backend.support.PostgresTestContainerBase
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class IdentityEudiBackendApplicationTests : PostgresTestContainerBase() {

    @Test
    fun contextLoads() {
    }
}
