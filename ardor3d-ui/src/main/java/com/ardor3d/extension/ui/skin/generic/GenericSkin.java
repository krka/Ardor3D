/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui.skin.generic;

import com.ardor3d.extension.ui.UIButton;
import com.ardor3d.extension.ui.UICheckBox;
import com.ardor3d.extension.ui.UIFrame;
import com.ardor3d.extension.ui.UIFrameBar;
import com.ardor3d.extension.ui.UIFrameStatusBar;
import com.ardor3d.extension.ui.UILabel;
import com.ardor3d.extension.ui.UIPanel;
import com.ardor3d.extension.ui.UIProgressBar;
import com.ardor3d.extension.ui.UIRadioButton;
import com.ardor3d.extension.ui.UIState;
import com.ardor3d.extension.ui.UITooltip;
import com.ardor3d.extension.ui.backdrop.EmptyBackdrop;
import com.ardor3d.extension.ui.backdrop.GradientBackdrop;
import com.ardor3d.extension.ui.backdrop.ImageBackdrop;
import com.ardor3d.extension.ui.backdrop.SolidBackdrop;
import com.ardor3d.extension.ui.border.EmptyBorder;
import com.ardor3d.extension.ui.border.ImageBorder;
import com.ardor3d.extension.ui.border.SolidBorder;
import com.ardor3d.extension.ui.border.UIBorder;
import com.ardor3d.extension.ui.skin.Skin;
import com.ardor3d.extension.ui.util.Alignment;
import com.ardor3d.extension.ui.util.Insets;
import com.ardor3d.extension.ui.util.SubTex;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Image.Format;
import com.ardor3d.image.Texture.MinificationFilter;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.util.TextureManager;

public class GenericSkin extends Skin {
    protected Texture _sharedTex;

    public GenericSkin() {
        loadTexture();
    }

    protected void loadTexture() {
        try {
            _sharedTex = TextureManager.load("/com/ardor3d/extension/ui/skin/generic/genericSkin.png",
                    MinificationFilter.Trilinear, Format.GuessNoCompression, false);
        } catch (final Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void applyToButton(final UIButton component) {

        component.getMargin().set(1, 1, 1, 1);
        component.getPadding().set(2, 14, 2, 14);

        // State values...
        final UIBorder defaultBorder = new ImageBorder(
        // left
                new SubTex(_sharedTex, 47, 11, 4, 10),
                // right
                new SubTex(_sharedTex, 77, 11, 4, 10),
                // top
                new SubTex(_sharedTex, 51, 7, 26, 4),
                // bottom
                new SubTex(_sharedTex, 51, 21, 26, 4),
                // top left
                new SubTex(_sharedTex, 47, 7, 4, 4),
                // top right
                new SubTex(_sharedTex, 77, 7, 4, 4),
                // bottom left
                new SubTex(_sharedTex, 47, 21, 4, 4),
                // bottom right
                new SubTex(_sharedTex, 77, 21, 4, 4));

        final UIBorder overBorder = new ImageBorder(
        // left
                new SubTex(_sharedTex, 47, 33, 4, 10),
                // right
                new SubTex(_sharedTex, 77, 33, 4, 10),
                // top
                new SubTex(_sharedTex, 51, 29, 26, 4),
                // bottom
                new SubTex(_sharedTex, 51, 43, 26, 4),
                // top left
                new SubTex(_sharedTex, 47, 29, 4, 4),
                // top right
                new SubTex(_sharedTex, 77, 29, 4, 4),
                // bottom left
                new SubTex(_sharedTex, 47, 43, 4, 4),
                // bottom right
                new SubTex(_sharedTex, 77, 43, 4, 4));

        final UIBorder pressedBorder = new ImageBorder(
        // left
                new SubTex(_sharedTex, 47, 55, 4, 10),
                // right
                new SubTex(_sharedTex, 77, 55, 4, 10),
                // top
                new SubTex(_sharedTex, 51, 51, 26, 4),
                // bottom
                new SubTex(_sharedTex, 51, 65, 26, 4),
                // top left
                new SubTex(_sharedTex, 47, 51, 4, 4),
                // top right
                new SubTex(_sharedTex, 77, 51, 4, 4),
                // bottom left
                new SubTex(_sharedTex, 47, 65, 4, 4),
                // bottom right
                new SubTex(_sharedTex, 77, 65, 4, 4));

        final ColorRGBA upTop = new ColorRGBA(235 / 255f, 235 / 255f, 235 / 255f, 1);
        final ColorRGBA upBottom = new ColorRGBA(200 / 255f, 200 / 255f, 200 / 255f, 1);
        final GradientBackdrop upBack = new GradientBackdrop(upTop, upTop, upBottom, upBottom);
        final ColorRGBA downTop = new ColorRGBA(181 / 255f, 181 / 255f, 181 / 255f, 1);
        final ColorRGBA downBottom = new ColorRGBA(232 / 255f, 232 / 255f, 232 / 255f, 1);
        final GradientBackdrop downBack = new GradientBackdrop(downTop, downTop, downBottom, downBottom);
        // DEFAULT
        {
            component.getDefaultState().setBorder(defaultBorder);
            component.getDefaultState().setBackdrop(upBack);
            component.getDefaultState().setForegroundColor(ColorRGBA.BLACK);
        }
        // DISABLED
        {
            component.getDisabledState().setBorder(defaultBorder);
            component.getDisabledState().setBackdrop(upBack);
            component.getDisabledState().setForegroundColor(ColorRGBA.GRAY);

            component.getDisabledSelectedState().setBorder(pressedBorder);
            component.getDisabledSelectedState().setBackdrop(downBack);
            component.getDisabledSelectedState().setForegroundColor(ColorRGBA.GRAY);
        }
        // MOUSE OVER
        {
            final ColorRGBA top = new ColorRGBA(241 / 255f, 241 / 255f, 241 / 255f, 1);
            final ColorRGBA bottom = new ColorRGBA(216 / 255f, 216 / 255f, 216 / 255f, 1);
            final GradientBackdrop back = new GradientBackdrop(top, top, bottom, bottom);

            component.getMouseOverState().setBorder(overBorder);
            component.getMouseOverState().setBackdrop(back);
            component.getMouseOverState().setForegroundColor(ColorRGBA.BLACK);
        }
        // PRESSED AND SELECTED
        {
            component.getPressedState().setBorder(pressedBorder);
            component.getPressedState().setBackdrop(downBack);
            component.getPressedState().setForegroundColor(ColorRGBA.BLACK);

            component.getSelectedState().setBorder(pressedBorder);
            component.getSelectedState().setBackdrop(downBack);
            component.getSelectedState().setForegroundColor(ColorRGBA.BLACK);

            component.getMouseOverSelectedState().setBorder(pressedBorder);
            component.getMouseOverSelectedState().setBackdrop(downBack);
            component.getMouseOverSelectedState().setForegroundColor(ColorRGBA.GRAY);
        }
    }

    @Override
    protected void applyToCheckBox(final UICheckBox component) {

        component.getMargin().set(1, 1, 1, 1);
        component.getPadding().set(1, 1, 1, 1);
        component.setBorder(new EmptyBorder());
        component.setBackdrop(new EmptyBackdrop());
        component.setAlignment(Alignment.LEFT);
        component.setGap(4);

        // DEFAULT
        {
            component.getDefaultState().setForegroundColor(ColorRGBA.BLACK);
            component.getDefaultState().setIcon(new SubTex(_sharedTex, 94, 9, 14, 14));
        }
        // DISABLED
        {
            component.getDisabledState().setForegroundColor(ColorRGBA.GRAY);
            component.getDisabledState().setIcon(new SubTex(_sharedTex, 132, 9, 14, 14));
        }
        // MOUSEOVER
        {
            component.getMouseOverState().setForegroundColor(ColorRGBA.BLACK);
            component.getMouseOverState().setIcon(new SubTex(_sharedTex, 113, 9, 14, 14));
        }
        // SELECTED
        {
            component.getSelectedState().setForegroundColor(ColorRGBA.BLACK);
            component.getSelectedState().setIcon(new SubTex(_sharedTex, 94, 25, 14, 14));
        }
        // MOUSEOVER SELECTED
        {
            component.getMouseOverSelectedState().setForegroundColor(ColorRGBA.BLACK);
            component.getMouseOverSelectedState().setIcon(new SubTex(_sharedTex, 113, 25, 14, 14));
        }
        // DISABLED SELECTED
        {
            component.getDisabledSelectedState().setForegroundColor(ColorRGBA.GRAY);
            component.getDisabledSelectedState().setIcon(new SubTex(_sharedTex, 132, 25, 14, 14));
        }
    }

    @Override
    protected void applyToFrame(final UIFrame component) {
        component.setFrameOpacity(1.0f);
        // TITLE BAR
        {
            final UIFrameBar titleBar = component.getTitleBar();
            // Make sure exists and is attached
            if (titleBar != null && titleBar.getParent() == component) {
                titleBar.setMargin(new Insets(0, 0, 0, 0));
                titleBar.setPadding(new Insets(0, 0, 0, 0));
                final UIBorder border = new ImageBorder(
                // left
                        new SubTex(_sharedTex, 4, 11, 6, 6),
                        // right
                        new SubTex(_sharedTex, 30, 11, 6, 6),
                        // top
                        new SubTex(_sharedTex, 10, 5, 20, 6),
                        // bottom
                        new SubTex(_sharedTex, 9, 9, 20, 1),
                        // top left
                        new SubTex(_sharedTex, 4, 5, 6, 6),
                        // top right
                        new SubTex(_sharedTex, 30, 5, 6, 6),
                        // bottom left
                        new SubTex(_sharedTex, 4, 16, 6, 1),
                        // bottom right
                        new SubTex(_sharedTex, 30, 16, 6, 1));
                titleBar.setBorder(border);
                final ColorRGBA top = new ColorRGBA(203 / 255f, 203 / 255f, 203 / 255f, 1);
                final ColorRGBA bottom = new ColorRGBA(208 / 255f, 208 / 255f, 208 / 255f, 1);
                final GradientBackdrop grad = new GradientBackdrop(top, top, bottom, bottom);
                titleBar.setBackdrop(grad);

                titleBar.getTitleLabel().setMargin(new Insets(0, 5, 0, 0));
                titleBar.getTitleLabel().setForegroundColor(ColorRGBA.BLACK);

                // CLOSE BUTTON
                {
                    final UIButton closeButton = titleBar.getCloseButton();
                    if (closeButton != null) {
                        closeButton.setButtonText("");
                        closeButton.setLayoutResizeableXY(false);
                        closeButton.setButtonIcon(new SubTex(_sharedTex, 94, 76, 16, 16));
                        closeButton.getPressedState().setIcon(new SubTex(_sharedTex, 94, 94, 16, 16));
                        for (final UIState state : closeButton.getStates()) {
                            state.setBackdrop(new EmptyBackdrop());
                            state.setBorder(new EmptyBorder());
                            state.setPadding(new Insets(0, 0, 0, 0));
                            state.setMargin(new Insets(1, 1, 1, 1));
                        }
                        closeButton.refreshState();
                    }
                }

                // MINIMIZE BUTTON
                {
                    final UIButton minimizeButton = titleBar.getMinimizeButton();
                    if (minimizeButton != null) {
                        minimizeButton.setButtonText("");
                        minimizeButton.setLayoutResizeableXY(false);
                        minimizeButton.setButtonIcon(new SubTex(_sharedTex, 113, 76, 16, 16));
                        minimizeButton.getPressedState().setIcon(new SubTex(_sharedTex, 113, 94, 16, 16));
                        for (final UIState state : minimizeButton.getStates()) {
                            state.setBackdrop(new EmptyBackdrop());
                            state.setBorder(new EmptyBorder());
                            state.setPadding(new Insets(0, 0, 0, 0));
                            state.setMargin(new Insets(1, 1, 1, 1));
                        }
                        minimizeButton.refreshState();
                    }
                }

                // EXPAND BUTTON
                {
                    final UIButton expandButton = titleBar.getExpandButton();
                    if (expandButton != null) {
                        expandButton.setButtonText("");
                        expandButton.setLayoutResizeableXY(false);
                        expandButton.setButtonIcon(new SubTex(_sharedTex, 132, 76, 16, 16));
                        expandButton.getPressedState().setIcon(new SubTex(_sharedTex, 132, 94, 16, 16));
                        for (final UIState state : expandButton.getStates()) {
                            state.setBackdrop(new EmptyBackdrop());
                            state.setBorder(new EmptyBorder());
                            state.setPadding(new Insets(0, 0, 0, 0));
                            state.setMargin(new Insets(1, 1, 1, 1));
                        }
                        expandButton.refreshState();
                    }
                }

                // MINIMIZE BUTTON
                {
                    final UIButton helpButton = titleBar.getHelpButton();
                    if (helpButton != null) {
                        helpButton.setButtonText("");
                        helpButton.setLayoutResizeableXY(false);
                        helpButton.setButtonIcon(new SubTex(_sharedTex, 151, 76, 16, 16));
                        helpButton.getPressedState().setIcon(new SubTex(_sharedTex, 151, 94, 16, 16));
                        for (final UIState state : helpButton.getStates()) {
                            state.setBackdrop(new EmptyBackdrop());
                            state.setBorder(new EmptyBorder());
                            state.setPadding(new Insets(0, 0, 0, 0));
                            state.setMargin(new Insets(1, 1, 1, 1));
                        }
                        helpButton.refreshState();
                    }
                }
            }
        }

        // BASE PANEL
        {
            final UIPanel base = component.getBasePanel();

            base.setMargin(new Insets(0, 0, 0, 0));
            base.setPadding(new Insets(0, 0, 0, 0));

            final UIBorder border = new ImageBorder(
            // left
                    new SubTex(_sharedTex, 4, 17, 6, 29),
                    // right
                    new SubTex(_sharedTex, 30, 17, 6, 29),
                    // top
                    new SubTex(_sharedTex, 0, 0, 0, 0),
                    // bottom
                    new SubTex(_sharedTex, 10, 46, 20, 7),
                    // top left
                    null,
                    // top right
                    null,
                    // bottom left
                    new SubTex(_sharedTex, 4, 46, 6, 7),
                    // bottom right
                    new SubTex(_sharedTex, 30, 46, 6, 7));
            base.setBorder(border);
            final ColorRGBA top = new ColorRGBA(210 / 255f, 210 / 255f, 210 / 255f, 1);
            final ColorRGBA bottom = new ColorRGBA(244 / 255f, 244 / 255f, 244 / 255f, 1);
            final GradientBackdrop grad = new GradientBackdrop(top, top, bottom, bottom);
            base.setBackdrop(grad);
        }

        // STATUS BAR
        {
            final UIFrameStatusBar statusBar = component.getStatusBar();
            // Make sure exists and is attached
            if (statusBar != null && statusBar.getParent() == component.getBasePanel()) {
                statusBar.setComponentHeight(10);
                statusBar.setLayoutResizeableY(false);

                final UIButton resize = statusBar.getResizeButton();
                if (resize != null && resize.getParent() == statusBar) {
                    for (final UIState state : resize.getStates()) {
                        state.setBackdrop(new EmptyBackdrop());
                        state.setBorder(new EmptyBorder());
                        state.setPadding(new Insets(0, 0, 0, 0));
                        state.setMargin(new Insets(0, 0, 0, 0));
                        state.setForegroundColor(ColorRGBA.GRAY);
                    }
                    resize.refreshState();
                    resize.compact();
                    resize.setLayoutResizeableXY(false);
                }
            }
        }
    }

    @Override
    protected void applyToLabel(final UILabel component) {
        component.getDefaultState().setForegroundColor(ColorRGBA.BLACK);
        component.getDisabledState().setForegroundColor(ColorRGBA.GRAY);
    }

    @Override
    protected void applyToPanel(final UIPanel component) {
        ; // nothing to do
    }

    @Override
    protected void applyToProgressBar(final UIProgressBar component) {
        final ColorRGBA top = new ColorRGBA(235 / 255f, 235 / 255f, 235 / 255f, 1);
        final ColorRGBA bottom = new ColorRGBA(200 / 255f, 200 / 255f, 200 / 255f, 1);
        final GradientBackdrop mainBack = new GradientBackdrop(top, top, bottom, bottom);
        component.getMainPanel().setBackdrop(mainBack);
        component.getMainPanel().setBorder(new EmptyBorder(0, 0, 0, 0));

        final ImageBackdrop barBack = new ImageBackdrop(new SubTex(_sharedTex, 11, 59, 22, 15));
        component.getBar().setBackdrop(barBack);
    }

    @Override
    protected void applyToRadioButton(final UIRadioButton component) {

        component.getMargin().set(1, 1, 1, 1);
        component.getPadding().set(1, 1, 1, 1);
        component.setBorder(new EmptyBorder());
        component.setBackdrop(new EmptyBackdrop());
        component.setAlignment(Alignment.LEFT);
        component.setGap(4);

        // DEFAULT
        {
            component.getDefaultState().setForegroundColor(ColorRGBA.BLACK);
            component.getDefaultState().setIcon(new SubTex(_sharedTex, 94, 42, 14, 14));
        }
        // DISABLED
        {
            component.getDisabledState().setForegroundColor(ColorRGBA.GRAY);
            component.getDisabledState().setIcon(new SubTex(_sharedTex, 132, 42, 14, 14));
        }
        // MOUSEOVER
        {
            component.getMouseOverState().setForegroundColor(ColorRGBA.BLACK);
            component.getMouseOverState().setIcon(new SubTex(_sharedTex, 113, 42, 14, 14));
        }
        // SELECTED
        {
            component.getSelectedState().setForegroundColor(ColorRGBA.BLACK);
            component.getSelectedState().setIcon(new SubTex(_sharedTex, 94, 59, 14, 14));
        }
        // MOUSEOVER SELECTED
        {
            component.getMouseOverSelectedState().setForegroundColor(ColorRGBA.BLACK);
            component.getMouseOverSelectedState().setIcon(new SubTex(_sharedTex, 113, 59, 14, 14));
        }
        // DISABLED SELECTED
        {
            component.getDisabledSelectedState().setForegroundColor(ColorRGBA.GRAY);
            component.getDisabledSelectedState().setIcon(new SubTex(_sharedTex, 132, 59, 14, 14));
        }
    }

    @Override
    protected void applyToTooltip(final UITooltip component) {
        component.setBackdrop(new SolidBackdrop(ColorRGBA.LIGHT_GRAY));
        component.setBorder(new SolidBorder(1, 1, 1, 1));
        component.setForegroundColor(ColorRGBA.BLACK);
        component.setFrameOpacity(1.0f);
    }
}
