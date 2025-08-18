package com.eomaxl.bankapplication.dto.response;

import com.eomaxl.bankapplication.dto.TransactionDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferResponse {

    private boolean success;
    private String fromAccountNumber;
    private String toAccountNumber;
    private BigDecimal amount;
    private BigDecimal fromBalanceBefore;
    private BigDecimal fromBalanceAfter;
    private BigDecimal toBalanceBefore;
    private BigDecimal toBalanceAfter;
    private List<TransactionDto> transactions;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime transferDate;

    private String errorMessage;
}
