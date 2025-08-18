package com.eomaxl.bankapplication.controller;

import com.eomaxl.bankapplication.dto.request.TransferRequest;
import com.eomaxl.bankapplication.dto.response.ApiResponse;
import com.eomaxl.bankapplication.dto.response.TransferResponse;
import com.eomaxl.bankapplication.mapper.BankingMapper;
import com.eomaxl.bankapplication.service.impl.BankingFacadeServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Money Transfer", description = "APIs for money transfer operations")
public class TransferController {

    private final BankingFacadeServiceImpl bankingFacadeService;
    private final BankingMapper mapper;

    @PostMapping
    @Operation(summary = "Transfer money", description = "Transfers money between two accounts")
    public ResponseEntity<ApiResponse<TransferResponse>> transferMoney(@Valid @RequestBody TransferRequest request) {
        log.info("Processing transfer: {} from {} to {}",
                request.getAmount(), request.getFromAccountNumber(), request.getToAccountNumber());

        var transferResult = bankingFacadeService.performTransfer(
                request.getFromAccountNumber(),
                request.getToAccountNumber(),
                request.getAmount(),
                request.getDescription()
        );

        var transferResponse = mapper.toTransferResponse(transferResult);

        if (transferResult.isSuccess()) {
            return ResponseEntity.ok(ApiResponse.success("Transfer completed successfully", transferResponse));
        } else {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Transfer failed: " + transferResult.getErrorMessage()));
        }
    }
}
