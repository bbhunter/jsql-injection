/*******************************************************************************
 * Copyhacked (H) 2012-2025.
 * This program and the accompanying materials
 * are made available under no term at all, use it like
 * you want, but share and discuss it
 * every time possible with every body.
 * 
 * Contributors:
 *      ron190 at ymail dot com - initial implementation
 ******************************************************************************/
package com.jsql.view.scan.interaction;

import com.jsql.model.bean.util.Header;
import com.jsql.model.injection.strategy.AbstractStrategy;
import com.jsql.view.interaction.InteractionCommand;
import com.jsql.view.swing.util.MediatorHelper;

import java.util.Map;

/**
 * Mark the injection as vulnerable to a basic injection.
 */
public class MarkUnionVulnerable implements InteractionCommand {

    private final String url;
    
    @SuppressWarnings("unchecked")
    public MarkUnionVulnerable(Object[] interactionParams) {
        Map<Header, Object> params = (Map<Header, Object>) interactionParams[0];
        this.url = (String) params.get(Header.URL);
    }

    @Override
    public void execute() {
        
        AbstractStrategy strategy = MediatorHelper.model().getMediatorStrategy().getUnion();
        MediatorHelper.managerScan().highlight(this.url, strategy.toString());
    }
}
