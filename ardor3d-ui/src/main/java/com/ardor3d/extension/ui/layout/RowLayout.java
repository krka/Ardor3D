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
import com.ardor3d.extension.ui.util.BoundingRectangle;
import com.ardor3d.scenegraph.Spatial;
import com.google.common.collect.Lists;

/**
 * This layout places components in either a horizontal or vertical row, ordered as they are placed in their container.
 * Depending on settings, the layout may also take any extra space in the container and divide it up equally among child
 * components that are marked as "layout resizeable".
 */
public class RowLayout extends UILayout {

    private static final int MAX_LOOPS = 50;
    private final boolean _horizontal;
    private final boolean _expandsHorizontally;
    private final boolean _expandsVertically;

    /**
     * Construct a new RowLayout
     * 
     * @param horizontal
     *            true if we should lay out horizontally, false if vertically
     */
    public RowLayout(final boolean horizontal) {
        this(horizontal, true, true);
    }

    /**
     * Construct a new RowLayout
     * 
     * @param horizontal
     *            true if we should lay out horizontally, false if vertically
     * @param expandsHorizontally
     *            true (the default) if horizontal free space in the container should be divided up among the child
     *            components.
     * @param expandsVertically
     *            true (the default) if vertical free space in the container should be divided up among the child
     *            components.
     */
    public RowLayout(final boolean horizontal, final boolean expandsHorizontally, final boolean expandsVertically) {
        _horizontal = horizontal;
        _expandsHorizontally = expandsHorizontally;
        _expandsVertically = expandsVertically;
    }

    /**
     * @return true if we lay out horizontally, false if vertically
     */
    public boolean isHorizontal() {
        return _horizontal;
    }

    /**
     * @return true (the default) if horizontal free space in the container should be divided up among the child
     *         components.
     */
    public boolean isExpandsHorizontally() {
        return _expandsHorizontally;
    }

    /**
     * 
     * @return true (the default) if vertical free space in the container should be divided up among the child
     *         components.
     */
    public boolean isExpandsVertically() {
        return _expandsVertically;
    }

    @Override
    public void layoutContents(final UIContainer container) {

        final List<Spatial> content = container.getChildren();
        final BoundingRectangle storeA = new BoundingRectangle();
        final BoundingRectangle storeB = new BoundingRectangle();

        // list of components
        final List<UIComponent> comps = Lists.newArrayList();
        for (int i = 0; i < content.size(); i++) {
            final Spatial spat = content.get(i);
            if (spat instanceof UIComponent) {
                final UIComponent comp = (UIComponent) spat;
                final BoundingRectangle rect = comp.getGlobalComponentBounds(storeA);
                final BoundingRectangle minRect = comp.getMinGlobalComponentBounds(storeB);
                if (_horizontal) {
                    comp.fitComponentIn(minRect.getWidth(), rect.getHeight());
                } else {
                    comp.fitComponentIn(rect.getWidth(), minRect.getHeight());
                }
                comps.add(comp);
            }
        }

        if (content != null && comps.size() > 0) {

            // Determine how much space we feel we need.
            final int reqSpace = _horizontal ? getSumOfAllWidths(content) : getSumOfAllHeights(content);

            // How much space do we actually have?
            int freeSpace = (_horizontal ? container.getContentWidth() : container.getContentHeight()) - reqSpace;

            // container is not big enough for contents.
            if (freeSpace < 0) {
                freeSpace = 0;
            }

            int loops = 0;
            do {
                final UIComponent comp = comps.remove(0);
                BoundingRectangle rect = comp.getGlobalComponentBounds(storeA);
                final BoundingRectangle origRect = storeB.set(rect);
                final int extraSize = freeSpace / (comps.size() + 1);
                if (_horizontal) {
                    final int height = _expandsVertically ? container.getContentHeight() : rect.getHeight();
                    final int width = (_expandsHorizontally ? extraSize : 0) + rect.getWidth();
                    if (height == rect.getHeight() && width == rect.getWidth()) {
                        continue;
                    }

                    comp.fitComponentIn(width, height);
                    rect = comp.getGlobalComponentBounds(storeA);
                    if (Math.abs(rect.getWidth() - width) <= 1) {
                        comps.add(comp);
                    }
                    freeSpace -= rect.getWidth() - origRect.getWidth();
                } else {
                    final int width = _expandsHorizontally ? container.getContentWidth() : rect.getWidth();
                    final int height = (_expandsVertically ? extraSize : 0) + rect.getHeight();
                    if (height == rect.getHeight() && width == rect.getWidth()) {
                        continue;
                    }

                    comp.fitComponentIn(width, height);
                    rect = comp.getGlobalComponentBounds(storeA);
                    if (Math.abs(rect.getHeight() - height) <= 1) {
                        comps.add(comp);
                    }
                    freeSpace -= rect.getHeight() - origRect.getHeight();
                }
            } while (freeSpace > 1 && comps.size() > 0 && ++loops <= RowLayout.MAX_LOOPS);

            int x = 0;
            int y = !_expandsVertically && !_horizontal ? container.getContentHeight() - reqSpace : 0;

            // Now, go through children and set proper location.
            for (int i = 0; i < content.size(); i++) {
                final Spatial spat = _horizontal ? content.get(i) : content.get(content.size() - i - 1);

                if (!(spat instanceof UIComponent)) {
                    continue;
                }
                final UIComponent comp = (UIComponent) spat;
                final BoundingRectangle rect = comp.getGlobalComponentBounds(storeA);

                if (_horizontal) {
                    comp.setLocalXY(x - rect.getX(), Math.max(container.getContentHeight() / 2 - rect.getHeight() / 2
                            - rect.getY(), 0));
                    x += rect.getWidth();
                } else {
                    comp.setLocalXY(Math.max(container.getContentWidth() / 2 - rect.getWidth() / 2 - rect.getX(), 0), y
                            - rect.getY());
                    y += rect.getHeight();
                }
            }
        }

    }

    @Override
    public void updateMinimumSizeFromContents(final UIContainer container) {

        int minW = 0, minH = 0;
        if (container.getNumberOfChildren() > 0) {
            final List<Spatial> content = container.getChildren();

            // compute the min width and height of the container
            final BoundingRectangle store = new BoundingRectangle();
            for (final Spatial s : content) {
                if (!(s instanceof UIComponent)) {
                    continue;
                }
                final UIComponent comp = (UIComponent) s;
                final BoundingRectangle rect = comp.getMinGlobalComponentBounds(store);
                if (_horizontal) {
                    minW += rect.getWidth();
                    if (minH < rect.getHeight()) {
                        minH = rect.getHeight();
                    }
                } else {
                    if (minW < rect.getWidth()) {
                        minW = rect.getWidth();
                    }
                    minH += rect.getHeight();
                }
            }
        }
        container.setMinimumContentSize(minW, minH);
    }

    private int getSumOfAllHeights(final List<Spatial> content) {
        int sum = 0;
        if (content != null) {
            final BoundingRectangle store = new BoundingRectangle();
            for (final Spatial spat : content) {
                if (spat instanceof UIComponent) {
                    final BoundingRectangle rect = ((UIComponent) spat).getMinGlobalComponentBounds(store);
                    sum += rect.getHeight();
                }
            }
        }
        return sum;
    }

    private int getSumOfAllWidths(final List<Spatial> content) {
        int sum = 0;
        if (content != null) {
            final BoundingRectangle store = new BoundingRectangle();
            for (final Spatial spat : content) {
                if (spat instanceof UIComponent) {
                    final BoundingRectangle rect = ((UIComponent) spat).getMinGlobalComponentBounds(store);
                    sum += rect.getWidth();
                }
            }
        }
        return sum;
    }
}
