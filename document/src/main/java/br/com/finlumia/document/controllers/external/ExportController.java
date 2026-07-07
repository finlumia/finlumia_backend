package br.com.finlumia.document.controllers.external;

import java.time.LocalDate;
import java.util.UUID;

import br.com.finlumia.document.controllers.ExternalApi;
import br.com.finlumia.document.models.ExportFormat;
import br.com.finlumia.document.models.ExportReportRequest;
import br.com.finlumia.document.models.Period;
import br.com.finlumia.document.services.ExportService;
import br.com.finlumia.document.views.ExportJobView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@ExternalApi
@RestController
@RequestMapping("/api/v1/export")
@Tag(name = "Export", description = "Exportação de dados e relatórios")
public class ExportController {

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping("/transactions")
    @Operation(summary = "Exportar transações", description = "Exporta lançamentos filtrados em PDF, CSV ou XLSX.")
    @ApiResponse(responseCode = "200", description = "Arquivo gerado com sucesso")
    public ResponseEntity<byte[]> exportTransactions(
            @RequestAttribute("usersKey") UUID userKey,
            @RequestParam ExportFormat format,
            @RequestParam(required = false) Period period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category) {
        byte[] file = exportService.exportTransactions(userKey, format, period, periodStart, periodEnd, type, category);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("transactions." + format.getValue())
                                .build()
                                .toString())
                .contentType(resolveMediaType(format))
                .body(file);
    }

    @PostMapping("/report")
    @Operation(summary = "Gerar relatório completo", description = "Gera PDF do relatório completo com KPIs, gráficos e insights do período. Relatórios grandes retornam 202 com jobId para download assíncrono.")
    @ApiResponse(responseCode = "200", description = "Arquivo gerado com sucesso")
    @ApiResponse(responseCode = "202", description = "Exportação enfileirada — use o jobId para acompanhar")
    public ResponseEntity<Object> exportReport(
            @RequestAttribute("usersKey") UUID userKey,
            @RequestBody @Valid ExportReportRequest request) {
        Object result = exportService.exportReport(userKey, request);
        if (result instanceof byte[] file) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment()
                                    .filename("relatorio-finlumia." + request.format().getValue())
                                    .build()
                                    .toString())
                    .contentType(resolveMediaType(request.format()))
                    .body(file);
        }
        return ResponseEntity.accepted().body(result);
    }

    @GetMapping("/{jobId}")
    @Operation(summary = "Status de exportação assíncrona", description = "Verifica status de exportação assíncrona e retorna URL de download quando pronto.")
    @ApiResponse(responseCode = "200", description = "Status retornado com sucesso")
    public ResponseEntity<ExportJobView> getExportJobStatus(
            @RequestAttribute("usersKey") UUID userKey,
            @PathVariable UUID jobId) {
        return ResponseEntity.ok(exportService.getJobStatus(userKey, jobId));
    }

    private MediaType resolveMediaType(ExportFormat format) {
        return switch (format) {
            case PDF -> MediaType.APPLICATION_PDF;
            case CSV -> new MediaType("text", "csv");
            case XLSX -> new MediaType("application", "vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        };
    }
}
