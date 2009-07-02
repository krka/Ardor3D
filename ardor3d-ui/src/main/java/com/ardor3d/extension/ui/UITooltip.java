/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui;

import com.ardor3d.extension.ui.backdrop.SolidBackdrop;
import com.ardor3d.extension.ui.border.SolidBorder;
import com.ardor3d.math.ColorRGBA;

/**
 * Defines a component used by the hud to display floating tool tips.
 */
public class UITooltip extends FloatingUIContainer {

    private final AbstractLabelUIComponent _label;

    /**
     * Construct a new UITooltip.
     */
    public UITooltip() {
        // set some default look and feel
        setBackdrop(new SolidBackdrop(ColorRGBA.LIGHT_GRAY));
        setBorder(new SolidBorder(1, 1, 1, 1));
        setForegroundColor(ColorRGBA.BLACK);
        setFrameOpacity(1.0f);

        // setup our text label
        _label = new UILabel("");
        getContentPanel().add(_label);

        // initially this is not visible
        setVisible(false);
    }

    /**
     * @return the label used to display tips.
     */
    public AbstractLabelUIComponent getLabel() {
        return _label;
    }

    @Override
    public UIComponent getUIComponent(final int hudX, final int hudY) {
        // We don't want the tool tip to be "pickable", so always return null.
        return null;
    }
}
