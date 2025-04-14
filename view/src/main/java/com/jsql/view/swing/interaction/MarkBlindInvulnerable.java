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
 * Mark the injection as invulnerable to a blind injection.
 */
public class MarkBlindInvulnerable implements InteractionCommand {
    
    public MarkBlindInvulnerable(Object[] interactionParams) {
        // Do nothing
    }

    @Override
    public void execute() {
        AbstractStrategy strategy = MediatorHelper.model().getMediatorStrategy().getBlindBit();
        MediatorHelper.panelAddressBar().getPanelTrailingAddress().markStrategyInvulnerable(strategy);
    }
}
