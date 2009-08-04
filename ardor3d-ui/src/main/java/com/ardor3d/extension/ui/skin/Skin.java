/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui.skin;

import com.ardor3d.extension.ui.UIButton;
import com.ardor3d.extension.ui.UICheckBox;
import com.ardor3d.extension.ui.UIComponent;
import com.ardor3d.extension.ui.UIFrame;
import com.ardor3d.extension.ui.UILabel;
import com.ardor3d.extension.ui.UIPanel;
import com.ardor3d.extension.ui.UIProgressBar;
import com.ardor3d.extension.ui.UIRadioButton;
import com.ardor3d.extension.ui.UITab;
import com.ardor3d.extension.ui.UITooltip;

public abstract class Skin {

    public void applyTo(final UIComponent component) {
        // NOTE: Test for subclasses first, then parent class

        // 1. BUTTON TYPES
        if (component instanceof UITab) {
            applyToTab((UITab) component);
        } else if (component instanceof UICheckBox) {
            applyToCheckBox((UICheckBox) component);
        } else if (component instanceof UIRadioButton) {
            applyToRadioButton((UIRadioButton) component);
        } else if (component instanceof UIButton) {
            applyToButton((UIButton) component);
        }

        // 2. OTHER LABEL TYPES
        else if (component instanceof UILabel) {
            applyToLabel((UILabel) component);
        }

        // 3. PANEL TYPES
        else if (component instanceof UIProgressBar) {
            applyToProgressBar((UIProgressBar) component);
        } else if (component instanceof UIPanel) {
            applyToPanel((UIPanel) component);
        }

        // 4. FRAME TYPES
        else if (component instanceof UITooltip) {
            applyToTooltip((UITooltip) component);
        } else if (component instanceof UIFrame) {
            applyToFrame((UIFrame) component);
        }
    }

    protected abstract void applyToTab(UITab component);

    protected abstract void applyToCheckBox(UICheckBox component);

    protected abstract void applyToRadioButton(UIRadioButton component);

    protected abstract void applyToButton(UIButton component);

    protected abstract void applyToLabel(UILabel component);

    protected abstract void applyToPanel(UIPanel component);

    protected abstract void applyToTooltip(UITooltip component);

    protected abstract void applyToFrame(UIFrame component);

    protected abstract void applyToProgressBar(UIProgressBar component);
}
