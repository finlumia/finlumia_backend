package br.com.finlumia.configurator.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import br.com.finlumia.configurator.models.CreateTableRequest;
import br.com.finlumia.configurator.models.DeleteTableRequest;
import br.com.finlumia.configurator.models.UpdateTableRequest;
import br.com.finlumia.configurator.repository.sql.TableSql;
import br.com.finlumia.shared.exception.FinlumiaException;
import br.com.finlumia.shared.views.DialogDefault;
import javax.sql.DataSource;

@Repository
public class TableRepository {
    private final DataSource postgresDataSource;

    public TableRepository(
            @Qualifier("postgresDataSource") DataSource postgresDataSource) {
        this.postgresDataSource = postgresDataSource;
    }

    public DialogDefault insertTable(Long keyUser, CreateTableRequest request) {

        try (Connection connection = postgresDataSource.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(TableSql.INSERT_TABLE)) {
            connection.setAutoCommit(false);

            preparedStatement.setLong(1, keyUser);
            preparedStatement.setString(2, request.getSchemaName());
            preparedStatement.setString(3, request.getTableName());
            preparedStatement.setString(4, request.getDisplayName());
            preparedStatement.setString(5, request.getDescription());
            preparedStatement.setLong(6, keyUser);
            preparedStatement.setLong(7, keyUser);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return new DialogDefault(200, "Table created successfully", "Tabela criada com sucesso");
                }
                throw new FinlumiaException(404, "Falha ao realizar insert de tabela",
                        "Não foi possível realizar o insert da tabela");
            } catch (Exception e) {
                throw new FinlumiaException(500, "Falha ao realizar insert de tabela",
                        "Não foi possível realizar o insert da tabela");
            }

        } catch (Exception e) {
            throw new FinlumiaException(500, "Falha ao realizar insert de tabela",
                    "Não foi possível realizar o insert da tabela");
        }

    }

    public DialogDefault updateTable(Long keyUser, UpdateTableRequest updateTableRequest) {

        try (Connection connection = postgresDataSource.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(TableSql.UPDATE_TABLE)) {
            connection.setAutoCommit(false);
            preparedStatement.setString(1, updateTableRequest.getDisplayName());
            preparedStatement.setString(2, updateTableRequest.getDescription());
            preparedStatement.setLong(3, keyUser);
            preparedStatement.setLong(4, updateTableRequest.getKeyTable());

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return new DialogDefault(200, "Tabela atualizada com sucesso",
                            "Os registros foram atualizados com sucesso");
                }
                throw new FinlumiaException(404, "Falha ao realizar atualização de tabela",
                        "Não foi possível realizar a atualização da tabela");
            } catch (Exception e) {
                throw new FinlumiaException(500, "Falha ao realizar atualização de tabela",
                        "Não foi possível realizar a atualização da tabela");
            }

        } catch (Exception e) {
            throw new FinlumiaException(500, "Falha ao realizar atualização de tabela",
                    "Não foi possível realizar a atualização da tabela");
        }

    }

    public DialogDefault deleteTable(Long keyUser, DeleteTableRequest deleteTableRequest) {

        try (Connection connection = postgresDataSource.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(TableSql.DELETE_TABLE)) {
            connection.setAutoCommit(false);
            preparedStatement.setLong(1, deleteTableRequest.getKeyTable());

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return new DialogDefault(200, "Tabela deletada com sucesso",
                            "Os registros foram deletada com sucesso");
                }
                throw new FinlumiaException(404, "Falha ao realizar o delete  de tabela",
                        "Não foi possível realizar o delete  da tabela");
            } catch (Exception e) {
                throw new FinlumiaException(500, "Falha ao realizar o delete de tabela",
                        "Não foi possível realizar o delete  da tabela");
            }

        } catch (Exception e) {
            throw new FinlumiaException(500, "Falha ao realizar o delete  de tabela",
                    "Não foi possível realizar  o delete  da tabela");
        }

    }




}
