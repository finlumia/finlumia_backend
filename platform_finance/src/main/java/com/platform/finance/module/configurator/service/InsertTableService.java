package com.platform.finance.module.configurator.service;

import com.platform.finance.module.configurator.model.InsertTableModel;
import com.platform.finance.module.configurator.view.InsertTableView;
import org.springframework.stereotype.Service;

@Service
public class InsertTableService {

    private InsertTableView InsertLine(InsertTableModel requestInsertTable){
        return new InsertTableView();
    }


}
