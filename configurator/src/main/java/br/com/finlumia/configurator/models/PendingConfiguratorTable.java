package br.com.finlumia.configurator.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PendingConfiguratorTable {

    private long key;
    private String schemaName;
    private String tableName;
    private String displayName;
}
