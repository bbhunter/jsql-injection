package com.jsql.view.swing.panel.preferences;

import com.jsql.view.swing.panel.PanelPreferences;
import com.jsql.view.swing.util.MediatorHelper;

import javax.swing.*;
import java.util.stream.Stream;

public class PanelStrategies extends JPanel {

    private final JCheckBox checkboxIsStrategyTimeDisabled = new JCheckBox("Disable Time", MediatorHelper.model().getMediatorUtils().getPreferencesUtil().isStrategyTimeDisabled());
    private final JCheckBox checkboxIsStrategyBlindDisabled = new JCheckBox("Disable Blind", MediatorHelper.model().getMediatorUtils().getPreferencesUtil().isStrategyBlindDisabled());
    private final JCheckBox checkboxIsStrategyMultibitDisabled = new JCheckBox("Disable Multibit", MediatorHelper.model().getMediatorUtils().getPreferencesUtil().isStrategyMultibitDisabled());
    private final JCheckBox checkboxIsStrategyErrorDisabled = new JCheckBox("Disable Error", MediatorHelper.model().getMediatorUtils().getPreferencesUtil().isStrategyErrorDisabled());
    private final JCheckBox checkboxIsStrategyStackDisabled = new JCheckBox("Disable Stack", MediatorHelper.model().getMediatorUtils().getPreferencesUtil().isStrategyStackDisabled());
    private final JCheckBox checkboxIsStrategyNormalDisabled = new JCheckBox("Disable Normal", MediatorHelper.model().getMediatorUtils().getPreferencesUtil().isStrategyNormalDisabled());

    public PanelStrategies(PanelPreferences panelPreferences) {
        this.checkboxIsStrategyTimeDisabled.setToolTipText("Skip Time strategy processing");
        this.checkboxIsStrategyBlindDisabled.setToolTipText("Skip Blind strategy processing");
        this.checkboxIsStrategyMultibitDisabled.setToolTipText("Skip Multibit strategy processing");
        this.checkboxIsStrategyErrorDisabled.setToolTipText("Skip Error strategy processing");
        this.checkboxIsStrategyStackDisabled.setToolTipText("Skip Stack strategy processing");
        this.checkboxIsStrategyNormalDisabled.setToolTipText("Skip Normal strategy processing");

        Stream.of(
            this.checkboxIsStrategyTimeDisabled,
            this.checkboxIsStrategyBlindDisabled,
            this.checkboxIsStrategyMultibitDisabled,
            this.checkboxIsStrategyErrorDisabled,
            this.checkboxIsStrategyStackDisabled,
            this.checkboxIsStrategyNormalDisabled
        )
        .forEach(button -> button.addActionListener(panelPreferences.getActionListenerSave()));

        var labelOrigin = new JLabel("<html><b>Choose injection strategies to skip</b></html>");
        labelOrigin.setBorder(PanelGeneral.MARGIN);

        var groupLayout = new GroupLayout(this);
        this.setLayout(groupLayout);

        groupLayout.setHorizontalGroup(
            groupLayout
            .createSequentialGroup()
            .addGroup(
                groupLayout
                .createParallelGroup(GroupLayout.Alignment.LEADING, false)
                .addComponent(labelOrigin)
                .addComponent(this.checkboxIsStrategyTimeDisabled)
                .addComponent(this.checkboxIsStrategyBlindDisabled)
                .addComponent(this.checkboxIsStrategyMultibitDisabled)
                .addComponent(this.checkboxIsStrategyErrorDisabled)
                .addComponent(this.checkboxIsStrategyStackDisabled)
                .addComponent(this.checkboxIsStrategyNormalDisabled)
            )
        );
        
        groupLayout.setVerticalGroup(
            groupLayout
            .createSequentialGroup()
            .addGroup(
                groupLayout
                .createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(labelOrigin)
            )
            .addGroup(
                groupLayout
                .createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(this.checkboxIsStrategyTimeDisabled)
            )
            .addGroup(
                groupLayout
                .createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(this.checkboxIsStrategyBlindDisabled)
            )
            .addGroup(
                groupLayout
                .createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(this.checkboxIsStrategyMultibitDisabled)
            )
            .addGroup(
                groupLayout
                .createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(this.checkboxIsStrategyErrorDisabled)
            )
            .addGroup(
                groupLayout
                .createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(this.checkboxIsStrategyStackDisabled)
            )
            .addGroup(
                groupLayout
                .createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(this.checkboxIsStrategyNormalDisabled)
            )
        );
    }
    
    
    // Getter and setter

    public JCheckBox getCheckboxIsStrategyTimeDisabled() {
        return this.checkboxIsStrategyTimeDisabled;
    }

    public JCheckBox getCheckboxIsStrategyBlindDisabled() {
        return this.checkboxIsStrategyBlindDisabled;
    }

    public JCheckBox getCheckboxIsStrategyStackDisabled() {
        return this.checkboxIsStrategyStackDisabled;
    }

    public JCheckBox getCheckboxIsStrategyMultibitDisabled() {
        return this.checkboxIsStrategyMultibitDisabled;
    }

    public JCheckBox getCheckboxIsStrategyErrorDisabled() {
        return this.checkboxIsStrategyErrorDisabled;
    }

    public JCheckBox getCheckboxIsStrategyNormalDisabled() {
        return this.checkboxIsStrategyNormalDisabled;
    }
}
