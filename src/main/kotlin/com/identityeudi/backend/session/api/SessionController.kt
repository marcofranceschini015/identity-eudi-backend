package com.identityeudi.backend.session.api

import com.identityeudi.backend.session.api.dto.CreateSessionRequest
import com.identityeudi.backend.session.api.dto.CreateSessionResponse
import com.identityeudi.backend.session.service.SessionService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/session")
class SessionController(
    private val sessionService: SessionService,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createSession(
        @Valid @RequestBody request: CreateSessionRequest,
    ): CreateSessionResponse {
        val issued = sessionService.createSession(
            tenant = request.tenant,
            userData = request.userData,
        )
        return CreateSessionResponse.from(issued)
    }
}
