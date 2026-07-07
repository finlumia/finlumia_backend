package br.com.finlumia.movement.services.importparsers;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import br.com.finlumia.movement.models.TransactionType;
import br.com.finlumia.shared.exception.FinlumiaException;
import org.springframework.stereotype.Component;

/**
 * OFX 1.x e SGML, nao XML: tags como &lt;TRNAMT&gt;valor nao sao fechadas.
 * Um scanner de tags simples e suficiente para os campos que precisamos
 * (data, valor, descricao) sem depender de biblioteca de terceiros com
 * presenca instavel no Maven Central. OFX 2.x (XML valido, raro em extratos
 * de bancos brasileiros) nao e coberto por este parser.
 */
@Component
public class OfxStatementParser implements StatementParser {

    private static final Pattern TRN_BLOCK = Pattern.compile(
            "<STMTTRN>(.*?)</STMTTRN>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final DateTimeFormatter OFX_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public ParseResult parse(byte[] fileContent) {
        String content = new String(fileContent, StandardCharsets.UTF_8);
        Matcher blocks = TRN_BLOCK.matcher(content);

        List<ParsedTransaction> transactions = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int index = 0;
        while (blocks.find()) {
            index++;
            String block = blocks.group(1);
            try {
                transactions.add(parseBlock(block));
            } catch (Exception e) {
                errors.add("Transacao " + index + ": " + e.getMessage());
            }
        }

        if (transactions.isEmpty() && errors.isEmpty()) {
            throw new FinlumiaException(422, "OFX nao reconhecido",
                    "Nenhuma transacao (<STMTTRN>) foi encontrada no arquivo OFX.");
        }
        return new ParseResult(transactions, errors);
    }

    private ParsedTransaction parseBlock(String block) {
        String dtPosted = extractTag(block, "DTPOSTED");
        String trnAmt = extractTag(block, "TRNAMT");
        String name = extractTag(block, "NAME");
        if (name == null) name = extractTag(block, "MEMO");

        if (dtPosted == null) throw new IllegalArgumentException("DTPOSTED ausente");
        if (trnAmt == null) throw new IllegalArgumentException("TRNAMT ausente");

        LocalDate date = parseOfxDate(dtPosted);
        BigDecimal amount = parseOfxAmount(trnAmt);
        String description = name != null && !name.isBlank() ? name.trim() : "Transacao importada";
        TransactionType type = amount.signum() < 0 ? TransactionType.DESPESA : TransactionType.RECEITA;
        return new ParsedTransaction(date, amount.abs(), type, description);
    }

    private String extractTag(String block, String tag) {
        Matcher m = Pattern.compile("<" + tag + ">([^<\r\n]*)", Pattern.CASE_INSENSITIVE).matcher(block);
        return m.find() ? m.group(1).trim() : null;
    }

    private LocalDate parseOfxDate(String raw) {
        try {
            String datePart = raw.length() >= 8 ? raw.substring(0, 8) : raw;
            return LocalDate.parse(datePart, OFX_DATE);
        } catch (Exception e) {
            throw new IllegalArgumentException("data '" + raw + "' em formato desconhecido");
        }
    }

    private BigDecimal parseOfxAmount(String raw) {
        try {
            return new BigDecimal(raw.trim().replace("+", ""));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("valor '" + raw + "' invalido");
        }
    }
}
