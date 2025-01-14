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
package com.jsql.view.swing.interaction;

import com.jsql.model.injection.strategy.AbstractStrategy;
import com.jsql.view.interaction.InteractionCommand;
import com.jsql.view.swing.util.MediatorHelper;

/**
 * Mark the injection as vulnerable to a basic injection.
 */
public class MarkNormalVulnerable implements InteractionCommand {
    
    public MarkNormalVulnerable(Object[] interactionParams) {
        // Do nothing
    }

    @Override
    public void execute() {
        AbstractStrategy strategy = MediatorHelper.model().getMediatorStrategy().getNormal();
        MediatorHelper.panelAddressBar().getPanelTrailingAddress().markStrategyVulnerable(strategy);
    }
}
