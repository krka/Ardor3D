/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui.layout;

import java.util.List;

import com.ardor3d.extension.ui.UIComponent;
import com.ardor3d.extension.ui.UIContainer;
import com.ardor3d.scenegraph.Spatial;

/**
 * This layout places components on the edges or in the center of a container, depending on the value of the layout data
 * object they hold. The behavior is meant to be similar to awt's {@link java.awt.BorderLayout BorderLayout}.
 * 
 * @see BorderLayoutData
 */
public class BorderLayout extends UILayout {

    @Override
    public void layoutContents(final UIContainer container) {
        if (container.getNumberOfChildren() < 1) {
            return;
        }
        int widthWest = 0;
        int widthEast = 0;

        int heightNorth = 0;
        int heightSouth = 0;
        final List<Spatial> content = container.getChildren();

        // Go through each component in the given container and determine the width and height of our edges.
        for (final Spatial s : content) {
            if (!(s instanceof UIComponent)) {
                continue;
            }
            final UIComponent comp = (UIComponent) s;

            final BorderLayoutData data = (BorderLayoutData) comp.getLayoutData();
            if (data != null) {
                switch (data) {
                    case NORTH:
                        heightNorth = comp.getMinimumComponentHeight(true);
                        break;
                    case SOUTH:
                        heightSouth = comp.getMinimumComponentHeight(true);
                        break;
                    case EAST:
                        widthEast = comp.getMinimumComponentWidth(true);
                        break;
                    case WEST:
                        widthWest = comp.getMinimumComponentWidth(true);
                        break;
                    case CENTER:
                        // nothing to do
                        break;
                }
            }
        }

        // Using the information from the last pass, set the position and size of each component in the container.
        for (final Spatial s : content) {
            if (!(s instanceof UIComponent)) {
                continue;
            }
            final UIComponent comp = (UIComponent) s;

            final BorderLayoutData data = (BorderLayoutData) comp.getLayoutData();
            if (data != null) {
                switch (data) {
                    case NORTH:
                        comp.setLocalXY(0, container.getContentHeight() - heightNorth);
                        comp.setComponentWidth(container.getContentWidth(), true);
                        comp.setComponentHeight(comp.getMinimumComponentHeight(true), true);
                        break;
                    case SOUTH:
                        comp.setLocalXY(0, 0);
                        comp.setComponentWidth(container.getContentWidth(), true);
                        comp.setComponentHeight(comp.getMinimumComponentHeight(true), true);
                        break;
                    case EAST:
                        comp.setLocalXY(container.getContentWidth() - comp.getMinimumComponentWidth(true) - 1,
                                heightSouth);
                        comp.setComponentWidth(comp.getMinimumComponentWidth(true), true);
                        comp.setComponentHeight(container.getContentHeight() - heightNorth - heightSouth, true);
                        break;
                    case WEST:
                        comp.setLocalXY(0, heightSouth);
                        comp.setComponentWidth(comp.getMinimumComponentWidth(true), true);
                        comp.setComponentHeight(container.getContentHeight() - heightNorth - heightSouth, true);
                        break;
                    case CENTER:
                        comp.setLocalXY(widthWest, heightSouth);
                        comp.setComponentWidth(container.getContentWidth() - widthEast - widthWest, true);
                        comp.setComponentHeight(container.getContentHeight() - heightSouth - heightNorth, true);
                }
            }
        }
    }

    @Override
    public void updateMinimumSizeFromContents(final UIContainer container) {
        container.setMinimumContentSize(getMinimumWidth(container.getChildren()), getMinimumHeight(container
                .getChildren()));
    }

    private int getMinimumHeight(final List<Spatial> content) {
        int minH = 0;
        int maxEWCH = 0;
        if (content != null) {
            for (final Spatial s : content) {
                if (!(s instanceof UIComponent)) {
                    continue;
                }
                final UIComponent comp = (UIComponent) s;
                final BorderLayoutData bld = (BorderLayoutData) comp.getLayoutData();
                if (bld == null) {
                    continue;
                }
                if (bld == BorderLayoutData.SOUTH || bld == BorderLayoutData.NORTH) {
                    minH += comp.getMinimumComponentHeight(true);
                } else {
                    final int h = comp.getMinimumComponentHeight(true);
                    if (h > maxEWCH) {
                        maxEWCH = h;
                    }
                }
            }
        }

        return minH + maxEWCH;
    }

    private int getMinimumWidth(final List<Spatial> content) {
        int minWidth = 0;
        int maxNSWidth = 0;
        if (content != null) {
            for (final Spatial s : content) {
                if (!(s instanceof UIComponent)) {
                    continue;
                }
                final UIComponent comp = (UIComponent) s;
                final BorderLayoutData data = (BorderLayoutData) comp.getLayoutData();
                if (data == BorderLayoutData.EAST || data == BorderLayoutData.WEST || data == BorderLayoutData.CENTER
                        || data == null) {
                    minWidth += comp.getMinimumComponentWidth(true);
                } else {
                    final int width = comp.getMinimumComponentWidth(true);
                    if (width > maxNSWidth) {
                        maxNSWidth = width;
                    }
                }

            }
        }
        return Math.max(minWidth, maxNSWidth);
    }
}
