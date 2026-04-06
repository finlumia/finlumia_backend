package br.com.finlumia.configurator.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import br.com.finlumia.configurator.models.ConfiguratorFieldRow;
import br.com.finlumia.configurator.models.CreateFieldRequest;
import br.com.finlumia.configurator.models.DeleteFieldRequest;
import br.com.finlumia.configurator.models.PendingConfiguratorTable;
import br.com.finlumia.configurator.models.UpdateFieldRequest;
import br.com.finlumia.configurator.repository.sql.FieldSql;
import br.com.finlumia.shared.exception.FinlumiaException;
import br.com.finlumia.shared.views.DialogDefault;

@Repository
public class FieldRepository {

    protected final DataSource postgresDataSource;

    public FieldRepository(
            @Qualifier("postgresDataSource") DataSource postgresDataSource) {
        this.postgresDataSource = postgresDataSource;
    }

    public DialogDefault createField(Long keyUser, CreateFieldRequest request) {
        try (Connection conn = postgresDataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(FieldSql.INSERT_FIELD)) {

            stmt.setBoolean(1, false);
            stmt.setBoolean(2, false);
            stmt.setLong(3, request.getTableKey());
            stmt.setString(4, request.getFieldName());
            stmt.setString(5, request.getDisplayName());
            stmt.setString(6, request.getDescription());
            stmt.setString(7, request.getDataType());
            stmt.setObject(8, request.getFieldLength());
            stmt.setObject(9, request.getFieldPrecision());
            stmt.setObject(10, request.getFieldScale());
            stmt.setBoolean(11, request.getIsRequired());
            stmt.setBoolean(12, request.getIsPrimaryKey());
            stmt.setBoolean(13, request.getIsForeignKey());
            stmt.setString(14, request.getFkReferenceTable());
            stmt.setString(15, request.getFkReferenceColumn());
            stmt.setString(16, request.getDefaultValue());
            stmt.setBoolean(17, request.getIsUnique());
            stmt.setBoolean(18, request.getIsIndexed());
            stmt.setInt(19, request.getDisplayOrder());
            stmt.setBoolean(20, request.getIsVisible());
            stmt.setBoolean(21, request.getIsEditable());
            stmt.setString(22, request.getSqlScriptDepara());
            stmt.setString(23, request.getSqlMask());
            stmt.setString(24, request.getValidationRegex());
            setMetadataJsonb(stmt, 25, request.getMetadata());
            stmt.setLong(26, keyUser);
            stmt.setLong(27, keyUser);
            stmt.setObject(28, request.getViewHabilit());

            conn.setAutoCommit(false);
            try {
                int rows = stmt.executeUpdate();
                if (rows > 0) {
                    conn.commit();
                    DialogDefault dialog = new DialogDefault();
                    dialog.setCode(201);
                    dialog.setTitle("Gravação realizada com sucesso!");
                    dialog.setMensage("O campo foi gravado com sucesso!");
                    return dialog;
                }
                conn.rollback();
                throw new FinlumiaException(
                        409,
                        "Campo já cadastrado!",
                        "Já existe um campo com este nome para a tabela informada.");
            } catch (FinlumiaException e) {
                throw e;
            } catch (Exception e) {
                conn.rollback();
                throw new FinlumiaException(
                        500,
                        "Erro ao gravar dados do campo!",
                        "Não foi possível gravar o campo no banco de dados. Contate o suporte!");
            }
        } catch (FinlumiaException e) {
            throw e;
        } catch (Exception e) {
            throw new FinlumiaException(
                    500,
                    "Erro ao gravar dados do campo!",
                    "Não foi possível gravar o campo no banco de dados. Contate o suporte!");
        }
    }

    public DialogDefault updateField(Long keyUser, UpdateFieldRequest request) {
        try (Connection conn = postgresDataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(FieldSql.UPDATE_FIELD)) {

            stmt.setBoolean(1, request.getLock());
            stmt.setLong(2, request.getTableKey());
            stmt.setString(3, request.getFieldName());
            stmt.setString(4, request.getDisplayName());
            stmt.setString(5, request.getDescription());
            stmt.setString(6, request.getDataType());
            stmt.setObject(7, request.getFieldLength());
            stmt.setObject(8, request.getFieldPrecision());
            stmt.setObject(9, request.getFieldScale());
            stmt.setBoolean(10, request.getIsRequired());
            stmt.setBoolean(11, request.getIsPrimaryKey());
            stmt.setBoolean(12, request.getIsForeignKey());
            stmt.setString(13, request.getFkReferenceTable());
            stmt.setString(14, request.getFkReferenceColumn());
            stmt.setString(15, request.getDefaultValue());
            stmt.setBoolean(16, request.getIsUnique());
            stmt.setBoolean(17, request.getIsIndexed());
            stmt.setInt(18, request.getDisplayOrder());
            stmt.setBoolean(19, request.getIsVisible());
            stmt.setBoolean(20, request.getIsEditable());
            stmt.setString(21, request.getSqlScriptDepara());
            stmt.setString(22, request.getSqlMask());
            stmt.setString(23, request.getValidationRegex());
            setMetadataJsonb(stmt, 24, request.getMetadata());
            stmt.setLong(25, keyUser);
            stmt.setObject(26, request.getViewHabilit());
            stmt.setLong(27, request.getKey());

            conn.setAutoCommit(false);
            try {
                int rows = stmt.executeUpdate();
                if (rows > 0) {
                    conn.commit();
                    DialogDefault dialog = new DialogDefault();
                    dialog.setCode(200);
                    dialog.setTitle("Atualização realizada com sucesso!");
                    dialog.setMensage("O campo foi atualizado com sucesso!");
                    return dialog;
                }
                conn.rollback();
                throw new FinlumiaException(
                        500,
                        "Erro ao atualizar dados do campo!",
                        "Não foi possível atualizar o campo no banco de dados. Contate o suporte!");
            } catch (FinlumiaException e) {
                throw e;
            } catch (Exception e) {
                conn.rollback();
                throw new FinlumiaException(
                        500,
                        "Erro ao atualizar dados do campo!",
                        "Não foi possível atualizar o campo no banco de dados. Contate o suporte!");
            }
        } catch (FinlumiaException e) {
            throw e;
        } catch (Exception e) {
            throw new FinlumiaException(
                    500,
                    "Erro ao atualizar dados do campo!",
                    "Não foi possível atualizar o campo no banco de dados. Contate o suporte!");
        }
    }

    public DialogDefault deleteField(Long keyUser, DeleteFieldRequest request) {
        try (Connection conn = postgresDataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(FieldSql.DELETE_FIELD)) {

            stmt.setLong(1, keyUser);
            stmt.setLong(2, request.getKey());

            conn.setAutoCommit(false);
            try {
                int rows = stmt.executeUpdate();
                if (rows > 0) {
                    conn.commit();
                    DialogDefault dialog = new DialogDefault();
                    dialog.setCode(200);
                    dialog.setTitle("Exclusão realizada com sucesso!");
                    dialog.setMensage("O campo foi excluído com sucesso!");
                    return dialog;
                }
                conn.rollback();
                throw new FinlumiaException(
                        500,
                        "Erro ao excluir dados do campo!",
                        "Não foi possível excluir o campo. Verifique se o registro existe e não está excluído.");
            } catch (FinlumiaException e) {
                throw e;
            } catch (Exception e) {
                conn.rollback();
                throw new FinlumiaException(
                        500,
                        "Erro ao excluir dados do campo!",
                        "Não foi possível excluir o campo no banco de dados. Contate o suporte!");
            }
        } catch (FinlumiaException e) {
            throw e;
        } catch (Exception e) {
            throw new FinlumiaException(
                    500,
                    "Erro ao excluir dados do campo!",
                    "Não foi possível excluir o campo no banco de dados. Contate o suporte!");
        }
    }

    public List<PendingConfiguratorTable> fetchPhysicallyCreatedConfiguratorTables() {
        List<PendingConfiguratorTable> list = new ArrayList<>();
        try (Connection conn = postgresDataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(FieldSql.SELECT_TABLES_FOR_FIELD_PHYSICAL_SYNC);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Object schemaObj = rs.getObject("tab_schema_name");
                Object tableObj = rs.getObject("tab_table_name");
                list.add(new PendingConfiguratorTable(
                        rs.getLong("k_e_y"),
                        schemaObj != null ? String.valueOf(schemaObj).trim() : null,
                        tableObj != null ? String.valueOf(tableObj).trim() : null,
                        rs.getString("tab_display_name")));
            }
            return list;
        } catch (Exception e) {
            throw new FinlumiaException(
                    500,
                    "Erro ao consultar tabelas para sincronização de campos!",
                    "Não foi possível ler o configurador de tabelas. Contate o suporte!");
        }
    }

    public List<ConfiguratorFieldRow> fetchConfiguratorFieldsForPhysicalSync(long tableKey) {
        List<ConfiguratorFieldRow> fields = new ArrayList<>();
        try (Connection conn = postgresDataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(FieldSql.SELECT_FIELDS_BY_TABLE_KEY)) {
            ps.setLong(1, tableKey);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Object fnObj = rs.getObject("fie_field_name");
                    String fieldName = fnObj != null ? String.valueOf(fnObj).trim() : null;
                    fields.add(new ConfiguratorFieldRow(
                            fieldName,
                            rs.getString("fie_data_type"),
                            (Integer) rs.getObject("fie_field_length"),
                            (Integer) rs.getObject("fie_field_precision"),
                            (Integer) rs.getObject("fie_field_scale"),
                            rs.getBoolean("fie_is_required"),
                            rs.getBoolean("fie_is_primary_key"),
                            rs.getBoolean("fie_is_foreign_key"),
                            rs.getString("fie_fk_reference_table"),
                            rs.getString("fie_fk_reference_column"),
                            rs.getString("fie_default_value"),
                            rs.getBoolean("fie_is_unique"),
                            rs.getBoolean("fie_is_indexed")));
                }
            }
            return fields;
        } catch (Exception e) {
            throw new FinlumiaException(
                    500,
                    "Erro ao consultar campos da tabela!",
                    "Não foi possível ler os campos no configurador. Contate o suporte!");
        }
    }

    public boolean physicalColumnExists(String schemaName, String tableName, String columnName) {
        try (Connection conn = postgresDataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(FieldSql.PG_COLUMN_EXISTS)) {
            ps.setString(1, schemaName);
            ps.setString(2, tableName);
            ps.setString(3, columnName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean(1);
                }
                return false;
            }
        } catch (Exception e) {
            throw new FinlumiaException(
                    500,
                    "Erro ao verificar coluna no banco!",
                    "Não foi possível consultar o catálogo do PostgreSQL. Contate o suporte!");
        }
    }

    public boolean physicalTableHasPrimaryKey(String schemaName, String tableName) {
        try (Connection conn = postgresDataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(FieldSql.PG_TABLE_HAS_PRIMARY_KEY)) {
            ps.setString(1, schemaName);
            ps.setString(2, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean(1);
                }
                return false;
            }
        } catch (Exception e) {
            throw new FinlumiaException(
                    500,
                    "Erro ao verificar chave primária!",
                    "Não foi possível consultar o catálogo do PostgreSQL. Contate o suporte!");
        }
    }

    public void executeDdlStatement(String sql) {
        try (Connection conn = postgresDataSource.getConnection();
                Statement st = conn.createStatement()) {
            st.execute(sql);
        } catch (Exception e) {
            throw new FinlumiaException(
                    500,
                    "Erro ao executar DDL!",
                    e.getMessage() != null ? e.getMessage() : "Falha na execução SQL.");
        }
    }

    private static void setMetadataJsonb(PreparedStatement stmt, int index, String metadata)
            throws Exception {
        if (metadata == null || metadata.isBlank()) {
            stmt.setObject(index, null);
            return;
        }
        PGobject json = new PGobject();
        json.setType("jsonb");
        json.setValue(metadata);
        stmt.setObject(index, json);
    }
}
