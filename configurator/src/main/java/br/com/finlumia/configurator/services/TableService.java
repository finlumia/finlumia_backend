package br.com.finlumia.configurator.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import br.com.finlumia.configurator.models.ConfiguratorFieldRow;
import br.com.finlumia.configurator.models.CreateTableRequest;
import br.com.finlumia.configurator.models.DeleteTableRequest;
import br.com.finlumia.configurator.models.PendingConfiguratorTable;
import br.com.finlumia.configurator.models.UpdateTableRequest;
import br.com.finlumia.configurator.repository.TableRepository;
import br.com.finlumia.configurator.repository.sql.TableSql;
import br.com.finlumia.shared.exception.FinlumiaException;
import br.com.finlumia.shared.views.DialogDefault;

@Service
public class TableService {

    private final TableRepository tableRepository;

    public TableService(TableRepository tableRepository) {
        this.tableRepository = tableRepository;
    }

    public DialogDefault createTable(Long keyUser, CreateTableRequest createTableRequest) {
        return tableRepository.createTable(keyUser, createTableRequest);
    }

    public DialogDefault updateTable(Long keyUser, UpdateTableRequest updateTableRequest) {
        return tableRepository.updateTable(keyUser, updateTableRequest);
    }

    public DialogDefault deleteTable(Long keyUser, DeleteTableRequest deleteTableRequest) {
        return tableRepository.deleteTable(keyUser, deleteTableRequest);
    }

    public DialogDefault createPhysicalTablesFromConfigurator(Long keyUser) {
        List<PendingConfiguratorTable> pending = tableRepository.fetchPendingConfiguratorTables();
        int created = 0;
        List<String> errors = new ArrayList<>();

        for (PendingConfiguratorTable table : pending) {
            String schemaName = table.getSchemaName();
            String tableName = table.getTableName();
            try {
                PhysicalDdlHelperService.requireNonBlankIdentifier(schemaName, "tab_schema_name");
                PhysicalDdlHelperService.requireNonBlankIdentifier(tableName, "tab_table_name");

                List<ConfiguratorFieldRow> fields = tableRepository
                        .fetchConfiguratorFieldsForTable(table.getKey());
                if (fields.isEmpty()) {
                    errors.add("Tabela " + schemaName + "." + tableName
                            + ": nenhum campo ativo no configurador.");
                    continue;
                }
                for (ConfiguratorFieldRow f : fields) {
                    if (f.getFieldName() == null || f.getFieldName().isBlank()) {
                        throw new FinlumiaException(400, "Dados incompletos",
                                "Campo com nome vazio na tabela " + schemaName + "." + tableName + ".");
                    }
                }

                tableRepository.executeDdlStatement(
                        TableSql.DDL_CREATE_SCHEMA_IF_NOT_EXISTS + PhysicalDdlHelperService.quoteIdent(schemaName));
                tableRepository.executeDdlStatement(
                        buildCreatePhysicalTableDdl(schemaName, tableName, fields));
                for (String idxSql : buildCreateIndexStatements(schemaName, tableName, fields)) {
                    tableRepository.executeDdlStatement(idxSql);
                }

                if (!tableRepository.markConfiguratorTablePhysicalCreated(keyUser, table.getKey())) {
                    errors.add("Tabela " + schemaName + "." + tableName
                            + ": criada no banco, mas não foi possível atualizar o flag no configurador.");
                    continue;
                }
                created++;
            } catch (FinlumiaException e) {
                errors.add("Tabela " + PhysicalDdlHelperService.nullSafe(schemaName) + "."
                        + PhysicalDdlHelperService.nullSafe(tableName) + ": " + e.getMessage());
            } catch (Exception e) {
                errors.add("Tabela " + PhysicalDdlHelperService.nullSafe(schemaName) + "."
                        + PhysicalDdlHelperService.nullSafe(tableName) + ": " + e.getMessage());
            }
        }

        if (created == 0 && errors.isEmpty()) {
            DialogDefault dialog = new DialogDefault();
            dialog.setCode(200);
            dialog.setTitle("Nada a criar");
            dialog.setMensage("Não há tabelas pendentes de criação física.");
            return dialog;
        }
        if (created == 0) {
            throw new FinlumiaException(
                    500,
                    "Criação física não concluída",
                    String.join(" ", errors));
        }
        DialogDefault dialog = new DialogDefault();
        dialog.setCode(200);
        dialog.setTitle("Criação física concluída");
        StringBuilder msg = new StringBuilder();
        msg.append(created).append(" tabela(s) criada(s) no banco.");
        if (!errors.isEmpty()) {
            msg.append(" Avisos/erros: ").append(String.join(" ", errors));
        }
        dialog.setMensage(msg.toString());
        return dialog;
    }

    private static String buildCreatePhysicalTableDdl(String schemaName, String tableName,
            List<ConfiguratorFieldRow> fields) {
        List<String> pkCols = new ArrayList<>();
        List<String> fkClauses = new ArrayList<>();

        StringBuilder body = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            ConfiguratorFieldRow f = fields.get(i);
            if (i > 0) {
                body.append(", ");
            }
            String col = PhysicalDdlHelperService.quoteIdent(f.getFieldName());
            String pgType = PhysicalDdlHelperService.mapConfiguratorTypeToPostgres(f);
            body.append(col).append(" ").append(pgType);

            boolean serialLike = pgType.toUpperCase(Locale.ROOT).contains("SERIAL");
            boolean notNull = f.isPrimaryKey() || f.isRequired() || serialLike;
            if (notNull) {
                body.append(" NOT NULL");
            }

            String def = PhysicalDdlHelperService.formatDefaultClause(pgType, f.getDefaultValue());
            if (def != null && !serialLike) {
                body.append(def);
            }
            if (f.isUnique()) {
                body.append(" UNIQUE");
            }
            if (f.isPrimaryKey()) {
                pkCols.add(col);
            }
            if (f.isForeignKey() && f.getFkReferenceTable() != null && !f.getFkReferenceTable().isBlank()
                    && f.getFkReferenceColumn() != null && !f.getFkReferenceColumn().isBlank()) {
                fkClauses.add(PhysicalDdlHelperService.buildFkClause(schemaName, tableName, f, col));
            }
        }

        if (!pkCols.isEmpty()) {
            body.append(", PRIMARY KEY (").append(String.join(", ", pkCols)).append(")");
        }
        for (String fk : fkClauses) {
            body.append(", ").append(fk);
        }

        return TableSql.DDL_CREATE_TABLE_IF_NOT_EXISTS
                + PhysicalDdlHelperService.quoteIdent(schemaName) + "." + PhysicalDdlHelperService.quoteIdent(tableName)
                + " ( " + body + " )";
    }

    private static List<String> buildCreateIndexStatements(String schemaName, String tableName,
            List<ConfiguratorFieldRow> fields) {
        List<String> list = new ArrayList<>();
        int n = 0;
        for (ConfiguratorFieldRow f : fields) {
            if (!f.isIndexed() || f.isPrimaryKey() || f.isUnique()) {
                continue;
            }
            n++;
            String idxName = "idx_" + tableName + "_" + f.getFieldName() + "_" + n;
            if (idxName.length() > 63) {
                idxName = idxName.substring(0, 63);
            }
            list.add(TableSql.DDL_CREATE_INDEX_IF_NOT_EXISTS + PhysicalDdlHelperService.quoteIdent(idxName) + " ON "
                    + PhysicalDdlHelperService.quoteIdent(schemaName) + "." + PhysicalDdlHelperService.quoteIdent(tableName)
                    + " (" + PhysicalDdlHelperService.quoteIdent(f.getFieldName()) + ")");
        }
        return list;
    }
}
