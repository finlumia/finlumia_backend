package br.com.finlumia.movement.services.importparsers;

import java.util.List;

public record ParseResult(List<ParsedTransaction> transactions, List<String> errors) {}
