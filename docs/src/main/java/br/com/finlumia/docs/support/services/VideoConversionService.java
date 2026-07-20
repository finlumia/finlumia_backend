package br.com.finlumia.docs.support.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Service
public class VideoConversionService {

    private static final Logger log = LoggerFactory.getLogger(VideoConversionService.class);
    private static final long PROCESS_TIMEOUT_MINUTES = 10;

    private final JdbcTemplate jdbc;
    private final StorageService storageService;
    private final Executor videoConversionExecutor;
    private final String ffmpegBinary;

    public VideoConversionService(
            JdbcTemplate jdbc,
            StorageService storageService,
            @Qualifier("videoConversionExecutor") Executor videoConversionExecutor,
            @Value("${finlumia.ffmpeg.binary-path}") String ffmpegBinary) {
        this.jdbc = jdbc;
        this.storageService = storageService;
        this.videoConversionExecutor = videoConversionExecutor;
        this.ffmpegBinary = ffmpegBinary;
    }

    /**
     * Fire-and-forget: nunca bloqueia quem chamou. Se a fila estiver cheia,
     * marca a conversao como falha imediatamente em vez de propagar erro
     * pra resposta de upload (o anexo bruto ja esta salvo e utilizavel).
     */
    public void enqueue(UUID attachmentId, UUID ticketId, String rawObjectKey) {
        try {
            videoConversionExecutor.execute(() -> process(attachmentId, ticketId, rawObjectKey));
        } catch (TaskRejectedException e) {
            log.error("VIDEO_CONVERSION_REJECTED attachmentId={} ticketId={} reason=fila_cheia", attachmentId, ticketId);
            markFailed(attachmentId, "Fila de conversao cheia, tente novamente mais tarde.");
        }
    }

    private void process(UUID attachmentId, UUID ticketId, String rawObjectKey) {
        Path scratchDir = null;
        try {
            jdbc.update("UPDATE docs.ticket_attachments SET conversion_status = 'processing' WHERE id = ?", attachmentId);

            scratchDir = Files.createTempDirectory("ffmpeg-" + attachmentId);
            Path rawFile = scratchDir.resolve("raw" + extractExtension(rawObjectKey));
            Path thumbFile = scratchDir.resolve("thumb.jpg");
            Path convertedFile = scratchDir.resolve("converted.mp4");
            Path thumbLog = scratchDir.resolve("thumbnail.log");
            Path convertLog = scratchDir.resolve("convert.log");

            storageService.downloadToFile(rawObjectKey, rawFile);
            generateThumbnail(rawFile, thumbFile, thumbLog);
            convertVideo(rawFile, convertedFile, convertLog);

            String convertedKey = storageService.buildConvertedObjectKey(ticketId, attachmentId);
            String thumbnailKey = storageService.buildThumbnailObjectKey(ticketId, attachmentId);
            storageService.uploadFile(convertedKey, convertedFile, "video/mp4");
            storageService.uploadFile(thumbnailKey, thumbFile, "image/jpeg");

            jdbc.update("""
                    UPDATE docs.ticket_attachments
                    SET converted_object_key = ?, thumbnail_object_key = ?,
                        conversion_status = 'completed', converted_at = NOW()
                    WHERE id = ?
                    """, convertedKey, thumbnailKey, attachmentId);
            log.info("VIDEO_CONVERSION_SUCCESS attachmentId={} ticketId={}", attachmentId, ticketId);
        } catch (Exception e) {
            log.error("VIDEO_CONVERSION_FAILURE attachmentId={} ticketId={} reason={}", attachmentId, ticketId, e.getMessage());
            markFailed(attachmentId, truncate(e.getMessage()));
        } finally {
            deleteQuietly(scratchDir);
        }
    }

    private void generateThumbnail(Path rawFile, Path thumbFile, Path logFile) throws IOException, InterruptedException {
        int exit = runFfmpeg(List.of(ffmpegBinary, "-y", "-i", rawFile.toString(),
                "-ss", "00:00:01", "-vframes", "1", "-vf", "scale=-2:480", thumbFile.toString()), logFile);
        if (exit != 0 || !Files.exists(thumbFile)) {
            // Video pode ter menos de 1s de duracao — tenta o primeiro frame.
            int retryExit = runFfmpeg(List.of(ffmpegBinary, "-y", "-i", rawFile.toString(),
                    "-ss", "00:00:00", "-vframes", "1", "-vf", "scale=-2:480", thumbFile.toString()), logFile);
            if (retryExit != 0 || !Files.exists(thumbFile)) {
                throw new IllegalStateException("falha ao gerar miniatura: " + tail(logFile));
            }
        }
    }

    private void convertVideo(Path rawFile, Path convertedFile, Path logFile) throws IOException, InterruptedException {
        // scale=-2:'min(720,ih)' evita upscale de video menor que 720p;
        // -movflags +faststart permite reproducao progressiva no <video> do navegador.
        int exit = runFfmpeg(List.of(ffmpegBinary, "-y", "-i", rawFile.toString(),
                "-vf", "scale=-2:'min(720,ih)'",
                "-c:v", "libx264", "-preset", "veryfast", "-crf", "26",
                "-c:a", "aac", "-b:a", "128k", "-movflags", "+faststart",
                convertedFile.toString()), logFile);
        if (exit != 0 || !Files.exists(convertedFile)) {
            throw new IllegalStateException("falha na conversao de video: " + tail(logFile));
        }
    }

    private int runFfmpeg(List<String> command, Path logFile) throws IOException, InterruptedException {
        // Saida redirecionada pra arquivo (nao pipe): ffmpeg escreve bastante
        // progresso no stderr, e sem drenar um pipe o processo pode travar
        // esperando espaco no buffer.
        ProcessBuilder builder = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectOutput(logFile.toFile());
        Process process = builder.start();
        boolean finished = process.waitFor(PROCESS_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("ffmpeg excedeu o tempo limite de " + PROCESS_TIMEOUT_MINUTES + " minutos");
        }
        return process.exitValue();
    }

    private String tail(Path logFile) {
        try {
            List<String> lines = Files.readAllLines(logFile);
            int from = Math.max(0, lines.size() - 15);
            return String.join(" | ", lines.subList(from, lines.size()));
        } catch (IOException e) {
            return "log indisponivel";
        }
    }

    private void markFailed(UUID attachmentId, String reason) {
        jdbc.update("""
                UPDATE docs.ticket_attachments
                SET conversion_status = 'failed', conversion_error = ?
                WHERE id = ?
                """, reason, attachmentId);
    }

    private String truncate(String message) {
        if (message == null) {
            return "erro desconhecido";
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    private String extractExtension(String objectKey) {
        int dot = objectKey.lastIndexOf('.');
        return dot >= 0 ? objectKey.substring(dot) : ".bin";
    }

    private void deleteQuietly(Path dir) {
        if (dir == null) {
            return;
        }
        try (var stream = Files.walk(dir)) {
            stream.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // best-effort cleanup
                }
            });
        } catch (IOException e) {
            log.warn("VIDEO_CONVERSION_SCRATCH_CLEANUP_FAILURE dir={} reason={}", dir, e.getMessage());
        }
    }
}
