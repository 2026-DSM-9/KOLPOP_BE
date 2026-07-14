package com.dsm9.kolpop.domain.reservation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dsm9.kolpop.domain.reservation.dto.CreateReservationRequest;
import com.dsm9.kolpop.domain.reservation.dto.CreateReservationResponse;
import com.dsm9.kolpop.domain.reservation.dto.ReservationDecisionResponse;
import com.dsm9.kolpop.domain.reservation.dto.ReservationManagementResponse;
import com.dsm9.kolpop.domain.reservation.service.ReservationService;
import com.dsm9.kolpop.global.exception.BusinessException;
import com.dsm9.kolpop.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/reservations")
@Tag(name = "예약", description = "예약 신청 및 예약 관리 API")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    @Operation(summary = "예약 신청")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<CreateReservationResponse>> createReservation(
            @Valid @RequestBody CreateReservationRequest request,
            Authentication authentication
    ) {
        CreateReservationResponse response = reservationService.createReservation(extractUserId(authentication), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/manage")
    @Operation(summary = "임대인 예약 관리 조회")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<ReservationManagementResponse> getManagementReservations(Authentication authentication) {
        return ApiResponse.success(
                reservationService.getManagementReservations(extractUserId(authentication))
        );
    }

    @PatchMapping("/{reservationId}/approve")
    @Operation(summary = "예약 승인")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<ReservationDecisionResponse> approveReservation(
            @PathVariable Long reservationId,
            Authentication authentication
    ) {
        return ApiResponse.success(
                reservationService.approveReservation(extractUserId(authentication), reservationId)
        );
    }

    @PatchMapping("/{reservationId}/reject")
    @Operation(summary = "예약 거절")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<ReservationDecisionResponse> rejectReservation(
            @PathVariable Long reservationId,
            Authentication authentication
    ) {
        return ApiResponse.success(
                reservationService.rejectReservation(extractUserId(authentication), reservationId)
        );
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "인증이 필요합니다.");
        }

        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException exception) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "INVALID_AUTHENTICATION", "인증 정보가 올바르지 않습니다.");
        }
    }
}
