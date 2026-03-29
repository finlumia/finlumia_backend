package br.com.finlumia.configurator.repository;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;

public class TableRepository {

    protected final DataSource postgresDataSource;

    public TableRepository(
            @Qualifier("postgresDataSource") DataSource postgresDataSource) {
        this.postgresDataSource = postgresDataSource;
    }

    







}
