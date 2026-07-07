-- =============================================================
-- MIGRATION 012 — movement: armazenar o conteúdo do arquivo enviado
--
-- Motivo: upload() descartava os bytes do arquivo (OFX/CSV), guardando
-- só o nome. Sem os bytes, confirmFileImport() não tinha o que
-- processar — por isso a importação de extrato nunca funcionou de
-- verdade. Guardamos o conteúdo temporariamente até o processamento
-- ser confirmado; ImportService limpa a coluna depois de processar.
-- =============================================================

ALTER TABLE movement.import_jobs
    ADD COLUMN IF NOT EXISTS file_content BYTEA;
