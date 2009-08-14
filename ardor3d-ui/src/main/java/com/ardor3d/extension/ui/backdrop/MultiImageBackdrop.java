/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui.backdrop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ardor3d.extension.ui.UIComponent;
import com.ardor3d.extension.ui.util.SubTexUtil;
import com.ardor3d.extension.ui.util.TransformedSubTex;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.Renderer;

/**
 * This backdrop paints one or more transformable, ordered images on a colored plane behind the component.
 */
public class MultiImageBackdrop extends SolidBackdrop {

    /** The image(s) to draw. */
    private final List<TransformedSubTex> _images = new ArrayList<TransformedSubTex>();

    /**
     * Construct this back drop, using the default, no alpha backdrop color.
     */
    public MultiImageBackdrop() {
        this(ColorRGBA.BLACK_NO_ALPHA);
    }

    /**
     * Construct this back drop, using the given backdrop color.
     * 
     * @param backDropColor
     *            the color of the backdrop
     */
    public MultiImageBackdrop(final ReadOnlyColorRGBA backDropColor) {
        super(backDropColor);
    }

    public void addImage(final TransformedSubTex entry) {
        _images.add(entry);
    }

    public boolean removeImage(final TransformedSubTex entry) {
        return _images.remove(entry);
    }

    @Override
    public void draw(final Renderer renderer, final UIComponent comp) {
        super.draw(renderer, comp);

        if (_images.size() > 1) {
            Collections.sort(_images);
        }

        final double bgwidth = comp.getWorldScale().getX() * UIBackdrop.getBackdropWidth(comp);
        final double bgheight = comp.getWorldScale().getY() * UIBackdrop.getBackdropHeight(comp);

        for (final TransformedSubTex entry : _images) {
            double x = 0;
            double y = 0;

            switch (entry.getAlignment()) {
                case TOP:
                case MIDDLE:
                case BOTTOM:
                    x = bgwidth / 2;
                    break;
                case TOP_RIGHT:
                case RIGHT:
                case BOTTOM_RIGHT:
                    x = bgwidth;
                    break;
                case TOP_LEFT:
                case LEFT:
                case BOTTOM_LEFT:
                    x = 0;
            }

            switch (entry.getAlignment()) {
                case TOP_LEFT:
                case TOP:
                case TOP_RIGHT:
                    y = bgheight;
                    break;
                case LEFT:
                case MIDDLE:
                case RIGHT:
                    y = bgheight / 2;
                    break;
                case BOTTOM_LEFT:
                case BOTTOM:
                case BOTTOM_RIGHT:
                    y = 0;
            }

            x += (comp.getWorldTranslation().getX() + comp.getMargin().getLeft() + comp.getBorder().getLeft())
                    * comp.getWorldScale().getX();
            y += (comp.getWorldTranslation().getY() + comp.getMargin().getBottom() + comp.getBorder().getBottom())
                    * comp.getWorldScale().getY();

            final double width = entry.getWidth() * comp.getWorldScale().getX();
            final double height = entry.getHeight() * comp.getWorldScale().getY();

            SubTexUtil.drawTransformedSubTex(renderer, entry, (int) Math.round(x), (int) Math.round(y), (int) Math
                    .round(width), (int) Math.round(height), false);
        }
    }
}
