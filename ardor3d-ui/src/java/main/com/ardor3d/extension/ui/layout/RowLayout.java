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
 * This layout places components in either a horizontal or vertical row, ordered as they are placed in their container.
 * Depending on settings, the layout may also take any extra space in the container and divide it up equally among child
 * components that are marked as "layout resizeable".
 */
public class RowLayout extends UILayout {

    private boolean _horizontal = true;
    private boolean _expands = true;

    /**
     * Construct a new RowLayout
     * 
     * @param horizontal
     *            true if we should lay out horizontally, false if vertically
     */
    public RowLayout(final boolean horizontal) {
        _horizontal = horizontal;
    }

    /**
     * Construct a new RowLayout
     * 
     * @param horizontal
     *            true if we should lay out horizontally, false if vertically
     * @param expands
     *            true (the default) if free space in the container should be divided up among the resizeable child
     *            components.
     */
    public RowLayout(final boolean horizontal, final boolean expands) {
        _horizontal = horizontal;
        _expands = expands;
    }

    /**
     * @return true if we lay out horizontally, false if vertically
     */
    public boolean isHorizontal() {
        return _horizontal;
    }

    /**
     * @return true if free space in the container should be divided up among the resizeable child components.
     */
    public boolean isExpands() {
        return _expands;
    }

    public void setExpands(final boolean autoExpands) {
        _expands = autoExpands;
    }

    @Override
    public void layoutContents(final UIContainer container) {

        final List<Spatial> content = container.getChildren();

        // Determine how much space we feel we need.
        final int reqSpace = _horizontal ? getSumOfAllWidths(content) : getSumOfAllHeights(content);

        int freeSpacePerComp = _horizontal ? container.getContentWidth() : container.getContentHeight();

        int expandableComponents = 0;

        // look for expandable components.
        if (_expands && content != null) {
            for (final Spatial s : content) {
                if (!(s instanceof UIComponent)) {
                    continue;
                }
                final UIComponent c = (UIComponent) s;
                if (_horizontal ? c.isLayoutResizeableX() : c.isLayoutResizeableY()) {
                    expandableComponents++;
                }
            }
            // extra space to add to each UIComponent
            if (expandableComponents > 0 && _expands) {
                freeSpacePerComp = (freeSpacePerComp - reqSpace) / expandableComponents;
            }
        }

        // container is not big enough for contents.
        if (freeSpacePerComp < 0) {
            freeSpacePerComp = 0;
        }

        int x = 0;
        int y = (expandableComponents == 0 || !_expands) && !_horizontal ? freeSpacePerComp - reqSpace : 0;

        if (content != null) {
            // go through children and set location and size.
            for (int i = 0; i < content.size(); i++) {
                final Spatial spat = _horizontal ? content.get(i) : content.get(content.size() - i - 1);

                if (!(spat instanceof UIComponent)) {
                    continue;
                }
                final UIComponent comp = (UIComponent) spat;

                if (_horizontal) {

                    comp.setComponentHeight(container.getContentHeight(), true);
                    comp.setComponentWidth((comp.isLayoutResizeableX() && _expands ? freeSpacePerComp : 0)
                            + comp.getMinimumComponentWidth(true), true);

                    comp.setLocalXY(x, container.getContentHeight() / 2 - comp.getComponentHeight() / 2);
                    x += comp.getComponentWidth();
                } else {

                    comp.setComponentHeight(container.getContentWidth(), true);
                    comp.setComponentHeight((comp.isLayoutResizeableY() && _expands ? freeSpacePerComp : 0)
                            + comp.getMinimumComponentHeight(true), true);

                    comp.setLocalXY(container.getContentWidth() / 2 - comp.getComponentWidth() / 2, y);
                    y += comp.getComponentHeight();
                }
            }
        }

    }

    @Override
    public void updateMinimumSizeFromContents(final UIContainer container) {

        int minW = 0, minH = 0;
        if (container.getNumberOfChildren() > 0) {
            final List<Spatial> content = container.getChildren();

            // compute the min width of the container
            for (final Spatial s : content) {
                if (!(s instanceof UIComponent)) {
                    continue;
                }
                final UIComponent comp = (UIComponent) s;
                if (_horizontal) {
                    minW += comp.getMinimumComponentWidth(true);
                } else {
                    if (minW < comp.getMinimumComponentWidth(true)) {
                        minW = comp.getMinimumComponentWidth(true);
                    }
                }
            }

            // compute the min height of the container
            for (final Spatial s : content) {
                if (!(s instanceof UIComponent)) {
                    continue;
                }
                final UIComponent comp = (UIComponent) s;

                if (_horizontal) {
                    if (minH < comp.getMinimumComponentHeight(true)) {
                        minH = comp.getMinimumComponentHeight(true);
                    }
                } else {
                    minH += comp.getMinimumComponentHeight(true);
                }
            }
        }
        container.setMinimumContentSize(minW, minH);
    }

    private int getSumOfAllHeights(final List<Spatial> content) {
        int sum = 0;
        if (content != null) {
            for (final Spatial spat : content) {
                if (spat instanceof UIComponent) {
                    sum += ((UIComponent) spat).getMinimumComponentHeight(true);
                }
            }
        }
        return sum;
    }

    private int getSumOfAllWidths(final List<Spatial> content) {
        int sum = 0;
        if (content != null) {
            for (final Spatial spat : content) {
                if (spat instanceof UIComponent) {
                    sum += ((UIComponent) spat).getMinimumComponentWidth(true);
                }
            }
        }
        return sum;
    }
}
