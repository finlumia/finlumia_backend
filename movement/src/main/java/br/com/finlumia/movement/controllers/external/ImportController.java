package br.com.finlumia.movement.controllers.external;

import java.util.UUID;

import br.com.finlumia.movement.controllers.ExternalApi;
import br.com.finlumia.movement.models.FileImportConfirmRequest;
import br.com.finlumia.movement.models.OcrConfirmRequest;
import br.com.finlumia.movement.services.ImportService;
import br.com.finlumia.movement.views.FileImportResultView;
import br.com.finlumia.movement.views.ImportJobView;
import br.com.finlumia.movement.views.OcrPreviewView;
import br.com.finlumia.movement.views.TransactionView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@ExternalApi
@RestController
@RequestMapping("/api/v1/transactions/import")
@Tag(name = "Import", description = "Importação de extratos e comprovantes via OFX, CSV ou imagem")
public class ImportController {

    private final ImportService importService;

    public ImportController(ImportService importService) {
        this.importService = importService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload de arquivo", description = "Upload de arquivo OFX, CSV ou imagem. Retorna jobId para acompanhamento.")
    @ApiResponse(responseCode = "202", description = "Arquivo recebido — processamento em andamento")
    @ApiResponse(responseCode = "400", description = "Formato ou tamanho inválido")
    public ResponseEntity<ImportJobView> upload(
            @RequestAttribute("usersKey") UUID userKey,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "institution", required = false) String institution) {
        return ResponseEntity.status(202).body(importService.upload(userKey, file));
    }

    @GetMapping("/{jobId}")
    @Operation(summary = "Status do job", description = "Verifica status do processamento (polling) e retorna prévia OCR se for imagem.")
    @ApiResponse(responseCode = "200", description = "Status retornado")
    @ApiResponse(responseCode = "404", description = "Job não encontrado")
    public ResponseEntity<OcrPreviewView> getJobStatus(
            @RequestAttribute("usersKey") UUID userKey,
            @PathVariable UUID jobId) {
        return ResponseEntity.ok(importService.getJobStatus(userKey, jobId));
    }

    @PostMapping("/{jobId}/confirm")
    @Operation(summary = "Confirmar OCR", description = "Usuário revisa e confirma dados extraídos via OCR antes de salvar o lançamento.")
    @ApiResponse(responseCode = "201", description = "Lançamento criado a partir do OCR")
    @ApiResponse(responseCode = "404", description = "Job não encontrado")
    public ResponseEntity<TransactionView> confirmOcr(
            @RequestAttribute("usersKey") UUID userKey,
            @PathVariable UUID jobId,
            @RequestBody @Valid OcrConfirmRequest request) {
        return ResponseEntity.status(201).body(importService.confirmOcr(userKey, jobId, request));
    }

    @PostMapping("/{jobId}/import")
    @Operation(summary = "Confirmar importação de arquivo", description = "Confirma importação de arquivo OFX/CSV após preview e validação.")
    @ApiResponse(responseCode = "200", description = "Importação concluída")
    @ApiResponse(responseCode = "404", description = "Job não encontrado")
    public ResponseEntity<FileImportResultView> confirmFileImport(
            @RequestAttribute("usersKey") UUID userKey,
            @PathVariable UUID jobId,
            @RequestBody(required = false) FileImportConfirmRequest request) {
        FileImportConfirmRequest req = request != null ? request : new FileImportConfirmRequest(null, null);
        return ResponseEntity.ok(importService.confirmFileImport(userKey, jobId, req));
    }
}
