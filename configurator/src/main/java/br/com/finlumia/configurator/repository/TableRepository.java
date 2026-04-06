package br.com.finlumia.configurator.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import br.com.finlumia.configurator.models.ConfiguratorFieldRow;
import br.com.finlumia.configurator.models.CreateTableRequest;
import br.com.finlumia.configurator.models.DeleteTableRequest;
import br.com.finlumia.configurator.models.PendingConfiguratorTable;
import br.com.finlumia.configurator.models.UpdateTableRequest;
import br.com.finlumia.configurator.repository.sql.FieldSql;
import br.com.finlumia.configurator.repository.sql.TableSql;
import br.com.finlumia.shared.exception.FinlumiaException;
import br.com.finlumia.shared.views.DialogDefault;

@Repository
public class TableRepository {

    protected final DataSource postgresDataSource;

    public TableRepository(
            @Qualifier("postgresDataSource") DataSource postgresDataSource) {
        this.postgresDataSource = postgresDataSource;
    }

    public DialogDefault createTable(Long keyUser, CreateTableRequest createTableRequest) {

        try (Connection bancoFinlumia = postgresDataSource.getConnection();
                PreparedStatement createTableStmt = bancoFinlumia.prepareStatement(TableSql.INSERT_TABLE)) {
                    
                    createTableStmt.setBoolean(1, false);
                    createTableStmt.setBoolean(2, false);
                    createTableStmt.setLong(3, createTableRequest.getSchema());
                    createTableStmt.setString(4, createTableRequest.getName());
                    createTableStmt.setString(5, createTableRequest.getDisplayName());
                    createTableStmt.setString(6, createTableRequest.getDisplayDescription());
                    createTableStmt.setLong(7, keyUser);
                    createTableStmt.setLong(8, keyUser);
                    createTableStmt.setBoolean(9, false);

                    bancoFinlumia.setAutoCommit(false);
            try (ResultSet rs = createTableStmt.executeQuery()) {
                if(rs.next()){
                    DialogDefault dialogDefault = new DialogDefault();
                    dialogDefault.setCode(201);
                    dialogDefault.setTitle("Gravação realizada com sucesso!");
                    dialogDefault.setMensage("A tabela foi gravada com sucesso!");
                    return dialogDefault;
                }
                bancoFinlumia.rollback();
                throw new FinlumiaException(
                    500,
                    "Erro ao gravar dados da tabela!",
                    "Não foi possível gravar a tabela no banco de dados. Contate o suporte!"
                );  
            } 
        } catch (FinlumiaException e) {
            throw e;
        } catch (Exception e) {
            throw new FinlumiaException(
                500,
                "Erro ao gravar dados da tabela!",
                "Não foi possível gravar a tabela no banco de dados. Contate o suporte!"
            );
        }

    }


    public DialogDefault updateTable(Long keyUser, UpdateTableRequest updateTableRequest) {

        try (Connection bancoFinlumia = postgresDataSource.getConnection();
                PreparedStatement updateTableStmt = bancoFinlumia.prepareStatement(TableSql.UPDATE_TABLE)) {
    
                    updateTableStmt.setBoolean(1, updateTableRequest.getLock());
                    updateTableStmt.setString(2, updateTableRequest.getSchemaName());
                    updateTableStmt.setString(3, updateTableRequest.getName());
                    updateTableStmt.setString(4, updateTableRequest.getDisplayName());
                    updateTableStmt.setString(5, updateTableRequest.getDisplayDescription());
                    updateTableStmt.setLong(6, keyUser);
                    updateTableStmt.setLong(7, updateTableRequest.getKey());
    
                    bancoFinlumia.setAutoCommit(false);
    
            try {
                int rows = updateTableStmt.executeUpdate();
                if (rows > 0) {
                    bancoFinlumia.commit();
                    DialogDefault dialogDefault = new DialogDefault();
                    dialogDefault.setCode(200);
                    dialogDefault.setTitle("Atualização realizada com sucesso!");
                    dialogDefault.setMensage("A tabela foi atualizada com sucesso!");
                    return dialogDefault;
                }
                bancoFinlumia.rollback();
                throw new FinlumiaException(
                    500,
                    "Erro ao atualizar dados da tabela!",
                    "Não foi possível atualizar a tabela no banco de dados. Contate o suporte!"
                );
            } catch (FinlumiaException e) {
                throw e;
            } catch (Exception e) {
                bancoFinlumia.rollback();
                throw new FinlumiaException(
                    500,
                    "Erro ao atualizar dados da tabela!",
                    "Não foi possível atualizar a tabela no banco de dados. Contate o suporte!"
                );
            }
    
        } catch (FinlumiaException e) {
            throw e;
        } catch (Exception e) {
            throw new FinlumiaException(
                500,
                "Erro ao atualizar dados da tabela!",
                "Não foi possível atualizar a tabela no banco de dados. Contate o suporte!"
            );
        }
    
    }
    
    public DialogDefault deleteTable(Long keyUser, DeleteTableRequest deleteTableRequest) {
    
        try (Connection bancoFinlumia = postgresDataSource.getConnection();
                PreparedStatement deleteTableStmt = bancoFinlumia.prepareStatement(TableSql.DELETE_TABLE)) {
    
                    deleteTableStmt.setLong(1, keyUser);
                    deleteTableStmt.setLong(2, deleteTableRequest.getKey());
    
                    bancoFinlumia.setAutoCommit(false);
    
            try {
                int rows = deleteTableStmt.executeUpdate();
                if (rows > 0) {
                    bancoFinlumia.commit();
                    DialogDefault dialogDefault = new DialogDefault();
                    dialogDefault.setCode(200);
                    dialogDefault.setTitle("Exclusão realizada com sucesso!");
                    dialogDefault.setMensage("A tabela foi excluída com sucesso!");
                    return dialogDefault;
                }
                bancoFinlumia.rollback();
                throw new FinlumiaException(
                    500,
                    "Erro ao excluir dados da tabela!",
                    "Não foi possível excluir a tabela. Verifique se ela já foi criada fisicamente no banco de dados!"
                );
            } catch (FinlumiaException e) {
                throw e;
            } catch (Exception e) {
                bancoFinlumia.rollback();
                throw new FinlumiaException(
                    500,
                    "Erro ao excluir dados da tabela!",
                    "Não foi possível excluir a tabela no banco de dados. Contate o suporte!"
                );
            }
    
        } catch (FinlumiaException e) {
            throw e;
        } catch (Exception e) {
            throw new FinlumiaException(
                500,
                "Erro ao excluir dados da tabela!",
                "Não foi possível excluir a tabela no banco de dados. Contate o suporte!"
            );
        }
    
    }

    public List<PendingConfiguratorTable> fetchPendingConfiguratorTables() {
        List<PendingConfiguratorTable> list = new ArrayList<>();
        try (Connection conn = postgresDataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(TableSql.CREATE_TABLE_SELECT_PENDING);
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
                    "Erro ao consultar tabelas pendentes!",
                    "Não foi possível ler o configurador de tabelas. Contate o suporte!");
        }
    }

    public List<ConfiguratorFieldRow> fetchConfiguratorFieldsForTable(long tableKey) {
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

    public boolean markConfiguratorTablePhysicalCreated(long keyUser, long tableKey) {
        try (Connection conn = postgresDataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(TableSql.UPDATE_TABLE_MARK_PHYSICAL_CREATED)) {
            ps.setLong(1, keyUser);
            ps.setLong(2, tableKey);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            throw new FinlumiaException(
                    500,
                    "Erro ao atualizar flag de criação física!",
                    "Não foi possível marcar a tabela como criada. Contate o suporte!");
        }
    }

}
