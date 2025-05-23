package com.test.vendor.db2;

import com.jsql.model.InjectionModel;
import com.jsql.model.exception.JSqlException;
import com.jsql.view.terminal.SystemOutTerminal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junitpioneer.jupiter.RetryingTest;

class Db2BlindSuiteIgnoreIT extends ConcreteDb2SuiteIT {
    // Unstable
    
    @Override
    public void setupInjection() throws Exception {
        InjectionModel model = new InjectionModel();
        this.injectionModel = model;

        model.subscribe(new SystemOutTerminal());

        model.getMediatorUtils().getParameterUtil().initQueryString(
            "http://localhost:8080/blind?tenant=db2&name=1'*"
        );
        
        model.setIsScanning(true);
        
        model
        .getMediatorUtils()
        .getPreferencesUtil()
        .withCountLimitingThreads(3);
        
        model
        .getMediatorUtils()
        .getConnectionUtil()
        .withMethodInjection(model.getMediatorMethod().getQuery())
        .withTypeRequest("GET");
        
        model.setIsScanning(true);
        model.getMediatorVendor().setVendorByUser(model.getMediatorVendor().getDb2());
        model.beginInjection();
    }
    
    @Override
    @RetryingTest(3)
    public void listTables() throws JSqlException {
        super.listTables();
    }

    @AfterEach
    void afterEach() {
        Assertions.assertEquals(
            this.injectionModel.getMediatorStrategy().getBlindBit(),
            this.injectionModel.getMediatorStrategy().getStrategy()
        );
    }
}
