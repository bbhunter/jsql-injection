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

import com.jsql.model.bean.database.AbstractElementDatabase;
import com.jsql.view.interaction.InteractionCommand;
import com.jsql.view.swing.util.MediatorHelper;

/**
 * Stop refreshing the progress bar of an untracked
 * progression (like colum search).
 */
public class EndIndeterminateProgress implements InteractionCommand {
    
    /**
     * The element in the database tree for which the progress ends.
     */
    private final AbstractElementDatabase dataElementDatabase;

    /**
     * @param interactionParams Element to update
     */
    public EndIndeterminateProgress(Object[] interactionParams) {
        this.dataElementDatabase = (AbstractElementDatabase) interactionParams[0];
    }

    @Override
    public void execute() {
        MediatorHelper.treeDatabase().endIndeterminateProgress(this.dataElementDatabase);
    }
}
