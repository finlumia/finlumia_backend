package br.com.finlumia.configurator.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import br.com.finlumia.configurator.models.ConfiguratorFieldRow;
import br.com.finlumia.configurator.models.CreateFieldRequest;
import br.com.finlumia.configurator.models.DeleteFieldRequest;
import br.com.finlumia.configurator.models.PendingConfiguratorTable;
import br.com.finlumia.configurator.models.UpdateFieldRequest;
import br.com.finlumia.configurator.repository.FieldRepository;
import br.com.finlumia.configurator.repository.sql.FieldSql;
import br.com.finlumia.configurator.repository.sql.TableSql;
import br.com.finlumia.shared.exception.FinlumiaException;
import br.com.finlumia.shared.views.DialogDefault;

@Service
public class FieldService {

    private final FieldRepository fieldRepository;

    public FieldService(FieldRepository fieldRepository) {
        this.fieldRepository = fieldRepository;
    }

    public DialogDefault createField(Long keyUser, CreateFieldRequest request) {
        return fieldRepository.createField(keyUser, request);
    }

    public DialogDefault updateField(Long keyUser, UpdateFieldRequest request) {
        return fieldRepository.updateField(keyUser, request);
    }

    public DialogDefault deleteField(Long keyUser, DeleteFieldRequest request) {
        return fieldRepository.deleteField(keyUser, request);
    }

    /**
     * Para cada tabela já materializada no banco, adiciona colunas que existem no configurador
     * e ainda não existem na tabela física ({@code ALTER TABLE ... ADD COLUMN}).
     *
     * @param keyUser reservado para auditoria futura (ex.: gravar quem disparou a sincronização)
     */
    @SuppressWarnings("unused")
    public DialogDefault syncPhysicalFieldsFromConfigurator(Long keyUser) {
        List<PendingConfiguratorTable> tables = fieldRepository.fetchPhysicallyCreatedConfiguratorTables();
        int columnsAdded = 0;
        List<String> errors = new ArrayList<>();

        if (tables.isEmpty()) {
            DialogDefault dialog = new DialogDefault();
            dialog.setCode(200);
            dialog.setTitle("Nada a sincronizar");
            dialog.setMensage(
                    "Não há tabelas com criação física concluída. Execute antes a criação física das tabelas.");
            return dialog;
        }

        for (PendingConfiguratorTable table : tables) {
            String schemaName = table.getSchemaName();
            String tableName = table.getTableName();
            try {
                PhysicalDdlHelperService.requireNonBlankIdentifier(schemaName, "tab_schema_name");
                PhysicalDdlHelperService.requireNonBlankIdentifier(tableName, "tab_table_name");

                boolean tableHasPk = fieldRepository.physicalTableHasPrimaryKey(schemaName, tableName);
                List<ConfiguratorFieldRow> fields = fieldRepository
                        .fetchConfiguratorFieldsForPhysicalSync(table.getKey());

                if (fields.isEmpty()) {
                    continue;
                }

                int indexSeq = 0;
                for (ConfiguratorFieldRow f : fields) {
                    if (f.getFieldName() == null || f.getFieldName().isBlank()) {
                        throw new FinlumiaException(400, "Dados incompletos",
                                "Campo com nome vazio na tabela " + schemaName + "." + tableName + ".");
                    }
                    if (fieldRepository.physicalColumnExists(schemaName, tableName, f.getFieldName())) {
                        continue;
                    }

                    String alterSql = buildAlterTableAddColumn(schemaName, tableName, f, tableHasPk);
                    fieldRepository.executeDdlStatement(alterSql);

                    if (f.isPrimaryKey()) {
                        tableHasPk = true;
                    }

                    if (f.isIndexed() && !f.isPrimaryKey() && !f.isUnique()) {
                        indexSeq++;
                        fieldRepository.executeDdlStatement(
                                buildCreateIndexStatement(schemaName, tableName, f, indexSeq));
                    }
                    columnsAdded++;
                }
            } catch (FinlumiaException e) {
                errors.add("Tabela " + PhysicalDdlHelperService.nullSafe(schemaName) + "."
                        + PhysicalDdlHelperService.nullSafe(tableName) + ": " + e.getMessage());
            } catch (Exception e) {
                errors.add("Tabela " + PhysicalDdlHelperService.nullSafe(schemaName) + "."
                        + PhysicalDdlHelperService.nullSafe(tableName) + ": " + e.getMessage());
            }
        }

        if (columnsAdded == 0 && errors.isEmpty()) {
            DialogDefault dialog = new DialogDefault();
            dialog.setCode(200);
            dialog.setTitle("Nada a criar");
            dialog.setMensage("Não há colunas novas pendentes de criação física.");
            return dialog;
        }
        if (columnsAdded == 0) {
            throw new FinlumiaException(
                    500,
                    "Sincronização de campos não concluída",
                    String.join(" ", errors));
        }
        DialogDefault dialog = new DialogDefault();
        dialog.setCode(200);
        dialog.setTitle("Sincronização de campos concluída");
        StringBuilder msg = new StringBuilder();
        msg.append(columnsAdded).append(" coluna(s) adicionada(s) no banco.");
        if (!errors.isEmpty()) {
            msg.append(" Avisos/erros: ").append(String.join(" ", errors));
        }
        dialog.setMensage(msg.toString());
        return dialog;
    }

    private static String buildAlterTableAddColumn(String schemaName, String tableName,
            ConfiguratorFieldRow f, boolean tableHasPk) {
        String qCol = PhysicalDdlHelperService.quoteIdent(f.getFieldName());
        String pgType = PhysicalDdlHelperService.mapConfiguratorTypeToPostgres(f);

        StringBuilder sb = new StringBuilder();
        sb.append(FieldSql.DDL_ALTER_TABLE)
                .append(PhysicalDdlHelperService.quoteIdent(schemaName)).append('.')
                .append(PhysicalDdlHelperService.quoteIdent(tableName))
                .append(" ADD COLUMN ").append(qCol).append(" ").append(pgType);

        boolean serialLike = pgType.toUpperCase(Locale.ROOT).contains("SERIAL");
        boolean notNull = f.isPrimaryKey() || f.isRequired() || serialLike;
        if (notNull) {
            sb.append(" NOT NULL");
        }

        String def = PhysicalDdlHelperService.formatDefaultClause(pgType, f.getDefaultValue());
        if (def != null && !serialLike) {
            sb.append(def);
        }
        if (f.isUnique()) {
            sb.append(" UNIQUE");
        }
        sb.append(PhysicalDdlHelperService.buildInlineReferencesClause(f, schemaName));

        if (f.isPrimaryKey() && !tableHasPk) {
            sb.append(" PRIMARY KEY");
        }

        return sb.toString();
    }

    private static String buildCreateIndexStatement(String schemaName, String tableName,
            ConfiguratorFieldRow f, int sequence) {
        String idxName = "idx_" + tableName + "_" + f.getFieldName() + "_" + sequence;
        if (idxName.length() > 63) {
            idxName = idxName.substring(0, 63);
        }
        return TableSql.DDL_CREATE_INDEX_IF_NOT_EXISTS + PhysicalDdlHelperService.quoteIdent(idxName) + " ON "
                + PhysicalDdlHelperService.quoteIdent(schemaName) + "." + PhysicalDdlHelperService.quoteIdent(tableName)
                + " (" + PhysicalDdlHelperService.quoteIdent(f.getFieldName()) + ")";
    }
}
