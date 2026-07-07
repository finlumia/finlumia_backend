package br.com.finlumia.movement.services.importparsers;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.com.finlumia.movement.models.TransactionType;

public record ParsedTransaction(
        LocalDate date,
        BigDecimal amount,
        TransactionType type,
        String description
) {}
