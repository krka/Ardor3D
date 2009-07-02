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

import com.ardor3d.extension.ui.layout.RowLayout;
import com.ardor3d.extension.ui.layout.UILayout;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.hint.PickingHint;

/**
 * Defines a component that can hold and manage other components or containers, using a layout manager to position and
 * potentially resize them.
 */
public abstract class UIContainer extends UIComponent {

    /** Layout responsible for managing the size and position of this container's contents. */
    private UILayout _layout = new RowLayout(true);

    /**
     * Checks to see if a given UIComponent is in this container.
     * 
     * @param comp
     *            the component to look for
     * @return true if the given component is in this container.
     */
    public boolean contains(final UIComponent comp) {
        return contains(comp, false);
    }

    /**
     * Checks to see if a given UIComponent is in this container or (if instructed) its subcontainers.
     * 
     * @param component
     *            the component to look for
     * @param recurse
     *            if true, recursively check any sub-containers for the given component.
     * @return if the given component is found
     */
    public boolean contains(final UIComponent component, final boolean recurse) {
        for (int i = getNumberOfChildren(); --i >= 0;) {
            final Spatial child = getChild(i);
            if (child.equals(component)) {
                return true;
            } else if (recurse && component instanceof UIContainer) {
                if (((UIContainer) component).contains(component, true)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Add a component to this container.
     * 
     * @param component
     *            the component to add
     */
    public void add(final UIComponent component) {
        attachChild(component);
    }

    /**
     * Remove a component from this container.
     * 
     * @param comp
     *            the component to remove
     */
    public void remove(final UIComponent comp) {
        detachChild(comp);
    }

    /**
     * Removes all UI components from this container. If other types of Spatials are attached to this container, they
     * are ignored.
     * 
     * @param comp
     *            the component to remove
     */
    public void removeAllComponents() {
        for (int i = getNumberOfChildren(); --i >= 0;) {
            final Spatial child = getChild(i);
            if (child instanceof UIComponent) {
                remove((UIComponent) child);
            }
        }
    }

    @Override
    public void detachAllChildren() {
        // Override to make sure ui events are called for detach.
        removeAllComponents();

        // do the rest as normal
        super.detachAllChildren();
    }

    /**
     * @param layout
     *            the new layout to use with this container. If null, no layout is done by this container.
     */
    public void setLayout(final UILayout layout) {
        _layout = layout;
    }

    /**
     * @return the layout currently used by this container or null if no layout is used.
     */
    public UILayout getLayout() {
        return _layout;
    }

    @Override
    public void layout() {
        if (_layout != null) {
            _layout.layoutContents(this);
        }

        // call layout on children
        for (int x = 0, max = getNumberOfChildren(); x < max; x++) {
            final Spatial child = getChild(x);
            if (child instanceof UIComponent) {
                ((UIComponent) child).layout();
            }
        }
    }

    @Override
    public void updateMinimumSizeFromContents() {
        // call update on children first
        for (int x = 0, max = getNumberOfChildren(); x < max; x++) {
            final Spatial child = getChild(x);
            if (child instanceof UIComponent) {
                ((UIComponent) child).updateMinimumSizeFromContents();
            }
        }

        // update our min size.
        if (_layout != null) {
            _layout.updateMinimumSizeFromContents(this);
        }
    }

    @Override
    public void attachedToHud() {
        Spatial child;
        for (int i = 0, cSize = getNumberOfChildren(); i < cSize; i++) {
            child = getChild(i);
            if (child != null) {
                if (child instanceof UIComponent) {
                    ((UIComponent) child).attachedToHud();
                }
            }
        }
    }

    @Override
    public void detachedFromHud() {
        Spatial child;
        for (int i = 0, cSize = getNumberOfChildren(); i < cSize; i++) {
            child = getChild(i);
            if (child != null) {
                if (child instanceof UIComponent) {
                    ((UIComponent) child).detachedFromHud();
                }
            }
        }
    }

    /**
     * Push a clip onto the clip stack of the given renderer such that the component is trimmed down to the content area
     * of this container.
     * 
     * @param component
     *            the component being clipped
     * @param renderer
     *            the renderer to push a clip on
     */
    public final void clipContents(final UIComponent component, final Renderer renderer) {

        final int x = component.getHudX();
        final int y = component.getHudY();

        int width = getContentWidth();
        int height = getContentHeight();

        // use the component's width if it is smaller than our content width
        if (component.getComponentWidth() + component.getLocalX() < width) {
            width = component.getComponentWidth();
        }

        // use the component's height if it is smaller than our content height
        if (component.getComponentHeight() + component.getLocalY() < height) {
            height = component.getComponentHeight();
        }

        renderer.pushClip(x, y, (int) (width * getWorldScale().getX()), (int) (height * getWorldScale().getY()));
    }

    @Override
    public UIComponent getUIComponent(final int hudX, final int hudY) {
        if (!getSceneHints().isPickingHintEnabled(PickingHint.Pickable) || !insideMargin(hudX, hudY)) {
            return null;
        }

        UIComponent ret = null;
        UIComponent found = this;

        for (int i = 0; i < getNumberOfChildren(); i++) {
            final Spatial s = getChild(i);
            if (s instanceof UIComponent) {
                final UIComponent comp = (UIComponent) s;
                ret = comp.getUIComponent(hudX, hudY);

                if (ret != null) {
                    found = ret;
                }
            }
        }

        return found;
    }

    @Override
    protected void drawComponent(final Renderer renderer) {

        Spatial child;
        for (int i = 0, cSize = getNumberOfChildren(); i < cSize; i++) {
            child = getChild(i);
            if (child != null) {
                if (child instanceof UIComponent) {
                    clipContents((UIComponent) child, renderer);
                    child.onDraw(renderer);
                    renderer.popClip();
                } else {
                    child.onDraw(renderer);
                }
            }
        }
    }
}
