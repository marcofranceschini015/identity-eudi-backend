package com.identityeudi.backend.presentation.api

import com.identityeudi.backend.presentation.api.dto.CreatePresentationRequest
import com.identityeudi.backend.presentation.api.dto.CreatePresentationResponse
import com.identityeudi.backend.presentation.api.dto.PollPresentationResponse
import com.identityeudi.backend.presentation.service.PresentationService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/presentation")
class PresentationController(
    private val presentationService: PresentationService,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createPresentation(
        @Valid @RequestBody request: CreatePresentationRequest,
    ): CreatePresentationResponse {
        val created = presentationService.createPresentation(tenant = request.tenant)
        return CreatePresentationResponse.from(created)
    }

    @GetMapping("/{presentationSessionId}")
    fun pollPresentation(
        @PathVariable presentationSessionId: UUID,
    ): PollPresentationResponse {
        val state = presentationService.pollPresentation(presentationSessionId = presentationSessionId)
        return PollPresentationResponse(state = state)
    }
}
