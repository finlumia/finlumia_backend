package br.com.finlumia.configurator.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import br.com.finlumia.configurator.models.GenericRegisterRequest;
import br.com.finlumia.shared.exception.FinlumiaException;
import br.com.finlumia.shared.views.DialogDefault;

@Repository
public class GenericRegisterRepository {
    private final DataSource postgresDataSource;

    private class QueryGeneric {

        public static String GET_DATA_TABLE = """
                select
                    tb.tab_schema_name,
                    tb.tab_table_name,
                    fd.fie_field_name,
                    fd.fie_display_order,
                    fd.fie_data_type
                from configurator."TAB" tb
                left join configurator."FIE" fd
                    on tb.k_e_y = fd.fie_table_key
                    and fd.d_e_l_e_t_e = false
                    where tb.tab_slug = ?
                    and tb.d_e_l_e_t_e = false
                order by  fd.fie_display_order asc
                    """;
        public static String INSERT_GENERIC_LINE = """
                insert into ?.?
                    %s
                values
                    %s
                    """;

    }

    protected class DataTable {
        protected String schemaName;
        protected String tableName;
        protected Map<Long, Field> fields;

    }

    protected class Field {
        protected Long displayOrder;
        protected String fieldName;
        protected String fieldType;
        protected String fieldParameter;
    }

    public GenericRegisterRepository(
            @Qualifier("postgresDataSource") DataSource postgresDataSource) {
        this.postgresDataSource = postgresDataSource;
    }

    protected DataTable getGenericList(Long keyUser, GenericRegisterRequest request) {
        try (Connection connection = postgresDataSource.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(QueryGeneric.GET_DATA_TABLE)) {
            preparedStatement.setString(1, request.getSlugTable());
            boolean linhaOne = true;
            ResultSet resultSet = preparedStatement.executeQuery();
            DataTable dataTable = new DataTable();
            Map<Long, Field> fields = new HashMap<>();
            while (resultSet.next()) {
                if (linhaOne) {
                    dataTable.schemaName = resultSet.getString("tab_schema_name");
                    dataTable.tableName = resultSet.getString("tab_table_name");
                    linhaOne = false;
                }
                Field field = new Field();
                field.displayOrder = resultSet.getLong("fie_display_order");
                field.fieldName = resultSet.getString("fie_field_name");
                field.fieldType = resultSet.getString("fie_data_type");
                fields.put(field.displayOrder, field);
            }
            dataTable.fields = fields;
            return dataTable;
        } catch (FinlumiaException e) {
            throw e;
        } catch (Exception e) {
            throw new FinlumiaException(
                    500,
                    "Erro ao buscar fluxo de caixa diário no protheus",
                    "[_getGenericList]: " + e.getMessage());
        }
    }

    protected boolean insertGenericLine(DataTable dataTable, String insertFields, String insertValues) {
        try (Connection connection = postgresDataSource.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(QueryGeneric.INSERT_GENERIC_LINE)) {
            preparedStatement.setString(1, dataTable.schemaName);
            preparedStatement.setString(2, dataTable.tableName);
            preparedStatement.setString(3, insertFields);
            preparedStatement.setString(4, insertValues);
            if(preparedStatement.executeUpdate() >0){
                return true;
            }
            return false;

        } catch (FinlumiaException e) {
            throw e;
        } catch (Exception e) {
            throw new FinlumiaException(
                    500,
                    "Erro ao realizar o insert na tabela",
                    "[insertGenericLine]: " + e.getMessage());
        }
    }

}
