package br.com.finlumia.movement.services;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import br.com.finlumia.movement.models.CategoryId;
import br.com.finlumia.movement.models.FileImportConfirmRequest;
import br.com.finlumia.movement.models.FileType;
import br.com.finlumia.movement.models.ImportJob;
import br.com.finlumia.movement.models.ImportStatus;
import br.com.finlumia.movement.models.OcrConfirmRequest;
import br.com.finlumia.movement.models.Transaction;
import br.com.finlumia.movement.repositorys.ImportJobRepository;
import br.com.finlumia.movement.repositorys.TransactionRepository;
import br.com.finlumia.movement.services.importparsers.CsvStatementParser;
import br.com.finlumia.movement.services.importparsers.OfxStatementParser;
import br.com.finlumia.movement.services.importparsers.ParseResult;
import br.com.finlumia.movement.services.importparsers.ParsedTransaction;
import br.com.finlumia.movement.views.FileImportResultView;
import br.com.finlumia.movement.views.ImportJobView;
import br.com.finlumia.movement.views.OcrPreviewView;
import br.com.finlumia.movement.views.TransactionView;
import br.com.finlumia.shared.exception.FinlumiaException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImportService {

    private final ImportJobRepository importJobRepository;
    private final TransactionRepository transactionRepository;
    private final CsvStatementParser csvStatementParser;
    private final OfxStatementParser ofxStatementParser;

    public ImportService(ImportJobRepository importJobRepository, TransactionRepository transactionRepository,
                          CsvStatementParser csvStatementParser, OfxStatementParser ofxStatementParser) {
        this.importJobRepository = importJobRepository;
        this.transactionRepository = transactionRepository;
        this.csvStatementParser = csvStatementParser;
        this.ofxStatementParser = ofxStatementParser;
    }

    @Transactional
    public ImportJobView upload(UUID userKey, MultipartFile file) {
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload";
        FileType fileType = FileType.fromContentType(originalName);
        UUID jobId = UUID.randomUUID();

        ImportJob job;
        if (fileType == FileType.IMAGE) {
            job = importJobRepository.save(jobId, userKey, originalName, fileType.getValue());
        } else {
            byte[] content;
            try {
                content = file.getBytes();
            } catch (java.io.IOException e) {
                throw new FinlumiaException(400, "Arquivo invalido", "Nao foi possivel ler o arquivo enviado.");
            }
            job = importJobRepository.save(jobId, userKey, originalName, fileType.getValue(), content);
        }
        return ImportJobView.from(job);
    }

    public OcrPreviewView getJobStatus(UUID userKey, UUID jobId) {
        ImportJob job = importJobRepository.findById(jobId, userKey)
                .orElseThrow(() -> new FinlumiaException(404, "Não encontrado", "Job de importação não encontrado."));
        return OcrPreviewView.from(job);
    }

    @Transactional
    public TransactionView confirmOcr(UUID userKey, UUID jobId, OcrConfirmRequest req) {
        ImportJob job = importJobRepository.findById(jobId, userKey)
                .orElseThrow(() -> new FinlumiaException(404, "Não encontrado", "Job de importação não encontrado."));

        if (job.fileType() != FileType.IMAGE) {
            throw new FinlumiaException(422, "Operação inválida", "Este job não é uma importação via OCR.");
        }
        if (job.status() == ImportStatus.COMPLETED || job.status() == ImportStatus.FAILED) {
            throw new FinlumiaException(422, "Operação inválida", "Job já finalizado.");
        }

        Transaction t = new Transaction(
                UUID.randomUUID(),
                userKey,
                req.type(),
                req.method(),
                req.institution(),
                req.date(),
                req.category(),
                req.description(),
                null,
                req.amount(),
                null,
                null,
                false,
                null,
                null,
                null
        );

        importJobRepository.updateStatus(jobId, ImportStatus.COMPLETED, 1, 1, null);
        return TransactionView.from(transactionRepository.save(t));
    }

    @Transactional
    public FileImportResultView confirmFileImport(UUID userKey, UUID jobId, FileImportConfirmRequest req) {
        ImportJob job = importJobRepository.findById(jobId, userKey)
                .orElseThrow(() -> new FinlumiaException(404, "Não encontrado", "Job de importação não encontrado."));

        if (job.fileType() == FileType.IMAGE) {
            throw new FinlumiaException(422, "Operação inválida", "Use o endpoint de confirmação OCR para imagens.");
        }
        if (job.status() == ImportStatus.COMPLETED || job.status() == ImportStatus.FAILED) {
            throw new FinlumiaException(422, "Operação inválida", "Job já finalizado.");
        }
        if (req.institution() == null) {
            throw new FinlumiaException(422, "Instituição obrigatória", "Selecione a instituição de origem do extrato.");
        }
        if (job.fileContent() == null) {
            throw new FinlumiaException(422, "Arquivo indisponível", "O conteúdo do arquivo não está mais disponível para processamento.");
        }

        importJobRepository.updateStatus(jobId, ImportStatus.PROCESSING, null, null, null);

        ParseResult parsed = job.fileType() == FileType.CSV
                ? csvStatementParser.parse(job.fileContent())
                : ofxStatementParser.parse(job.fileContent());

        int imported = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>(parsed.errors());
        for (ParsedTransaction pt : parsed.transactions()) {
            if (Boolean.TRUE.equals(req.skipDuplicates())
                    && transactionRepository.existsDuplicate(userKey, pt.date(), pt.amount(), pt.description())) {
                skipped++;
                continue;
            }
            try {
                Transaction t = new Transaction(
                        UUID.randomUUID(),
                        userKey,
                        pt.type(),
                        // OFX/CSV nao trazem um metodo de pagamento normalizado; usamos DEBITO
                        // como padrao generico de movimentacao — o usuario ajusta depois se preciso.
                        br.com.finlumia.movement.models.PaymentMethod.DEBITO,
                        req.institution(),
                        pt.date(),
                        CategoryId.OUTROS,
                        pt.description(),
                        null,
                        pt.amount(),
                        null,
                        null,
                        false,
                        null,
                        null,
                        null
                );
                transactionRepository.save(t);
                imported++;
            } catch (Exception e) {
                errors.add(pt.description() + ": " + e.getMessage());
            }
        }

        ImportStatus finalStatus = (imported == 0 && !errors.isEmpty()) ? ImportStatus.FAILED : ImportStatus.COMPLETED;
        importJobRepository.updateStatus(jobId, finalStatus, parsed.transactions().size(), imported,
                errors.isEmpty() ? null : errors);
        importJobRepository.clearFileContent(jobId);

        return new FileImportResultView(imported, skipped, errors.size());
    }
}
