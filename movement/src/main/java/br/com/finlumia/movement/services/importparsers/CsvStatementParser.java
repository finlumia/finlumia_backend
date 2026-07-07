package br.com.finlumia.movement.services.importparsers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import br.com.finlumia.movement.models.TransactionType;
import br.com.finlumia.shared.exception.FinlumiaException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

/**
 * Extratos em CSV variam de banco para banco (delimitador, ordem e nome das
 * colunas). Em vez de um mapeamento fixo por instituicao, reconhece as
 * colunas de data/descricao/valor pelo nome do cabecalho (com variantes em
 * PT/EN e sem acento).
 */
@Component
public class CsvStatementParser implements StatementParser {

    private static final List<String> DATE_HEADERS = List.of("data", "date", "data lancamento", "dt lancamento");
    private static final List<String> DESCRIPTION_HEADERS = List.of("descricao", "description", "historico", "lancamento");
    private static final List<String> AMOUNT_HEADERS = List.of("valor", "amount", "valor (r$)", "montante");

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy")
    );

    @Override
    public ParseResult parse(byte[] fileContent) {
        char delimiter = detectDelimiter(fileContent);
        CSVFormat format = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .setDelimiter(delimiter)
                .setIgnoreEmptyLines(true)
                .build();

        try (var reader = new InputStreamReader(new ByteArrayInputStream(fileContent), StandardCharsets.UTF_8);
             CSVParser parser = format.parse(reader)) {

            Map<String, Integer> headerMap = parser.getHeaderMap();
            String dateHeader = findHeader(headerMap, DATE_HEADERS);
            String descHeader = findHeader(headerMap, DESCRIPTION_HEADERS);
            String amountHeader = findHeader(headerMap, AMOUNT_HEADERS);

            if (dateHeader == null || descHeader == null || amountHeader == null) {
                throw new FinlumiaException(422, "CSV nao reconhecido",
                        "Nao foi possivel identificar as colunas de data, descricao e valor no arquivo CSV.");
            }

            List<ParsedTransaction> transactions = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            for (CSVRecord record : parser) {
                try {
                    LocalDate date = parseDate(record.get(dateHeader));
                    BigDecimal amount = parseAmount(record.get(amountHeader));
                    String description = record.get(descHeader).trim();
                    TransactionType type = amount.signum() < 0 ? TransactionType.DESPESA : TransactionType.RECEITA;
                    transactions.add(new ParsedTransaction(date, amount.abs(), type,
                            description.isEmpty() ? "Transacao importada" : description));
                } catch (Exception e) {
                    errors.add("Linha " + record.getRecordNumber() + ": " + e.getMessage());
                }
            }
            return new ParseResult(transactions, errors);
        } catch (IOException e) {
            throw new FinlumiaException(400, "Arquivo invalido", "Nao foi possivel ler o arquivo CSV.");
        }
    }

    private char detectDelimiter(byte[] content) {
        String firstLine = new String(content, StandardCharsets.UTF_8).lines().findFirst().orElse("");
        long semicolons = firstLine.chars().filter(c -> c == ';').count();
        long commas = firstLine.chars().filter(c -> c == ',').count();
        return semicolons > commas ? ';' : ',';
    }

    private String findHeader(Map<String, Integer> headerMap, List<String> candidates) {
        for (String key : headerMap.keySet()) {
            String normalized = normalize(key);
            for (String candidate : candidates) {
                if (normalized.equals(normalize(candidate))) return key;
            }
        }
        return null;
    }

    private String normalize(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private LocalDate parseDate(String raw) {
        String trimmed = raw.trim();
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(trimmed, fmt);
            } catch (Exception ignored) {
                // tenta o proximo formato
            }
        }
        throw new IllegalArgumentException("data '" + raw + "' em formato desconhecido");
    }

    private BigDecimal parseAmount(String raw) {
        String s = raw.trim().replace("R$", "").replace(" ", "");
        boolean negative = s.startsWith("-") || (s.startsWith("(") && s.endsWith(")"));
        s = s.replace("(", "").replace(")", "").replace("+", "");
        if (s.startsWith("-")) s = s.substring(1);
        if (s.isEmpty()) throw new IllegalArgumentException("valor vazio");

        int lastComma = s.lastIndexOf(',');
        int lastDot = s.lastIndexOf('.');
        if (lastComma > lastDot) {
            s = s.replace(".", "").replace(",", ".");
        } else if (lastDot > lastComma) {
            s = s.replace(",", "");
        }

        try {
            BigDecimal value = new BigDecimal(s);
            return negative ? value.negate() : value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("valor '" + raw + "' invalido");
        }
    }
}
