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

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.logging.Logger;

import com.ardor3d.extension.ui.backdrop.EmptyBackdrop;
import com.ardor3d.extension.ui.backdrop.UIBackdrop;
import com.ardor3d.extension.ui.border.EmptyBorder;
import com.ardor3d.extension.ui.border.UIBorder;
import com.ardor3d.extension.ui.layout.UILayoutData;
import com.ardor3d.extension.ui.skin.SkinManager;
import com.ardor3d.extension.ui.util.Dimension;
import com.ardor3d.extension.ui.util.Insets;
import com.ardor3d.input.InputState;
import com.ardor3d.input.Key;
import com.ardor3d.input.MouseButton;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Transform;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.BlendState.BlendEquation;
import com.ardor3d.renderer.state.BlendState.DestinationFunction;
import com.ardor3d.renderer.state.BlendState.SourceFunction;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.hint.PickingHint;
import com.ardor3d.ui.text.BMFont;
import com.ardor3d.ui.text.BasicText;

/**
 * Base UI class. All UI components/widgets/controls extend this class.
 */
public abstract class UIComponent extends Node {
    private static Logger logger = Logger.getLogger(UIComponent.class.getName());

    /** If true, use opacity settings to blend the components. Default is false. */
    private static boolean _useTransparency = false;

    /** The opacity of the component currently being rendered. */
    private static float _currentOpacity = 1f;

    /** The internal contents portion of this component. */
    private final Dimension _contentsSize = new Dimension(10, 10);
    /** The absolute minimum size of the internal contents portion of this component. */
    private final Dimension _minimumContentsSize = new Dimension(10, 10);
    /** A spacing between the component's border and its inner content area. */
    private Insets _padding = new Insets(0, 0, 0, 0);
    /** A border around this component. */
    private UIBorder _border = new EmptyBorder();
    /** A spacing between the component's border and other content outside this component. Used during layout. */
    private Insets _margin = new Insets(0, 0, 0, 0);

    /** If false, the contents part of this component is not allowed to be resized on the X axis by layouts. */
    private boolean _layoutX = true;
    /** If false, the contents part of this component is not allowed to be resized on the Y axis by layouts. */
    private boolean _layoutY = true;

    /** The renderer responsible for drawing this component's backdrop. */
    private UIBackdrop _backdrop = new EmptyBackdrop();

    /** White */
    private static final ReadOnlyColorRGBA DEFAULT_FOREGROUND_COLOR = ColorRGBA.WHITE;
    /** The color of any text or other such foreground elements. Inherited from parent if null. */
    private ColorRGBA _foregroundColor = null;

    /** Text to display using UITooltip when hovered over. Inherited from parent if null. */
    private String _tooltipText = null;
    /** The amount of time (in ms) before we should show the tooltip on this component. */
    private int _tooltipPopTime = 1000;

    /** A default font used when font field and all parents font fields are null. */
    private static BMFont _defaultFont = null;
    /** The font to use for text on this component, if needed. Inherited from parent if null. */
    private BMFont _font = null;

    /** Optional information used by a parent container's layout. */
    private UILayoutData _layoutData = null;

    /** If true, this component is drawn. */
    private boolean _visible = true;
    /** If false, this component may optionally disable input to it and its children (as applicable). */
    private boolean _enabled = true;

    /** The opacity of this component. 0 is fully transparent and 1 is fully opaque. The default is 1. */
    private float _opacity = 1.0f;

    /** If we are selected for key focus, we'll redirect that focus to this target if not null. */
    private UIComponent _keyFocusTarget = null;

    /**
     * flag to indicate if a component has touched its content area. Used to determine blending operations for child
     * components.
     */
    private boolean _virginContentArea = false;

    /** The current future task set to show a tooltip in the near future. */
    private FutureTask<Void> _showTask;

    /** The system time when the tool tip should show up next (if currently being timed.) */
    private long _toolDone;

    /** Blend states to use when drawing components as cached container contents. */
    private static BlendState _srcRGBmaxAlphaBlend = UIComponent.createSrcRGBMaxAlphaBlend();
    private static BlendState _blendRGBmaxAlphaBlend = UIComponent.createBlendRGBMaxAlphaBlend();

    protected void applySkin() {
        SkinManager.applyCurrentSkin(this);
        updateMinimumSizeFromContents();
    }

    /**
     * @return true if this component should be considered "enabled"... a concept that is interpreted by each individual
     *         component type.
     */
    public boolean isEnabled() {
        return _enabled;
    }

    /**
     * @param enabled
     *            true if this component should be considered "enabled"... a concept that is interpreted by each
     *            individual component type.
     */
    public void setEnabled(final boolean enabled) {
        _enabled = enabled;
    }

    /**
     * @return true if this component should be drawn.
     */
    public boolean isVisible() {
        return _visible;
    }

    /**
     * @param visible
     *            true if this component should be drawn.
     */
    public void setVisible(final boolean visible) {
        _visible = visible;
    }

    /**
     * @return true if the layout is allowed to change the width of this component.
     */
    public boolean isLayoutResizeableX() {
        return _layoutX;
    }

    /**
     * @param resizeable
     *            true if the layout is allowed to change the width of this component.
     */
    public void setLayoutResizeableX(final boolean resizeable) {
        _layoutX = resizeable;
    }

    /**
     * @return true if the layout is allowed to change the height of this component.
     */
    public boolean isLayoutResizeableY() {
        return _layoutY;
    }

    /**
     * @param resizeable
     *            true if the layout is allowed to change the height of this component.
     */
    public void setLayoutResizeableY(final boolean resizeable) {
        _layoutY = resizeable;
    }

    /**
     * @param resizeable
     *            true if the layout is allowed to change the width AND height of this component.
     */
    public void setLayoutResizeableXY(final boolean resizeable) {
        setLayoutResizeableX(resizeable);
        setLayoutResizeableY(resizeable);
    }

    /**
     * Used primarily during rendering to determine how alpha blending should be done.
     * 
     * @return true if nothing has been drawn by this component or its ancestors yet that would affect its content area.
     */
    public boolean hasVirginContentArea() {
        if (!_virginContentArea) {
            return false;
        } else if (getParent() instanceof UIComponent) {
            return ((UIComponent) getParent()).hasVirginContentArea();
        } else {
            return true;
        }
    }

    /**
     * @param virgin
     *            true if nothing has been drawn by this component yet that would affect its content area.
     */
    public void setVirginContentArea(final boolean virgin) {
        _virginContentArea = virgin;
    }

    /**
     * 
     * @return our font if one is set. Otherwise we will continue up the scenegraph until we find a font. If no font is
     *         found, the default font is used.
     * @see #getDefaultFont()
     */
    public BMFont getFont() {
        BMFont font = _font;
        if (font == null) {
            if (getParent() != null && getParent() instanceof UIComponent) {
                font = ((UIComponent) getParent()).getFont();
            } else {
                font = UIComponent.getDefaultFont();
            }
        }
        return font;
    }

    /**
     * @param font
     *            the font to use (as needed) for this component. Note that this can be inherited by child components if
     *            this is a container class.
     */
    public void setFont(final BMFont font) {
        _font = font;
    }

    /**
     * @return the set layout data object or null if none has been set.
     */
    public UILayoutData getLayoutData() {
        return _layoutData;
    }

    /**
     * @param layoutData
     *            the layout data object set on this component. The object would provide specific layout directions to
     *            the layout class of the container this component is placed in.
     */
    public void setLayoutData(final UILayoutData layoutData) {
        _layoutData = layoutData;
    }

    /**
     * @return the width of this entire component as a whole, including all margins, borders, padding and content.
     */
    public int getComponentWidth() {
        return _contentsSize.getWidth() + getTotalLeft() + getTotalRight();
    }

    /**
     * @return the height of this entire component as a whole, including all margins, borders, padding and content.
     */
    public int getComponentHeight() {
        return _contentsSize.getHeight() + getTotalTop() + getTotalBottom();
    }

    /**
     * Sets the width and height of this component by forcing the content area to be of a proper size such that when the
     * padding, margin and border are added, the total component size match those given.
     * 
     * @param width
     *            the new width of the component
     * @param height
     *            the new height of the component
     */
    public void setComponentSize(final int width, final int height) {
        setComponentWidth(width);
        setComponentHeight(height);
    }

    /**
     * Sets the width of this component by forcing the content area to be of a proper width such that when the padding,
     * margin and border are added, the total component's width matches that given.
     * 
     * @param width
     *            the new width of the component
     */
    public void setComponentWidth(final int width) {
        setComponentWidth(width, false);
    }

    /**
     * Sets the height of this component by forcing the content area to be of a proper height such that when the
     * padding, margin and border are added, the total component's height matches that given.
     * 
     * @param height
     *            the new height of the component
     */
    public void setComponentHeight(final int height) {
        setComponentHeight(height, false);
    }

    /**
     * If this component does not want us to resize it and we've chosen to listen to that (via passing true) then this
     * method returns the current component width. Otherwise, it returns the width contained in _minimumContentsSize +
     * the margin, border and padding values for left and right.
     * 
     * @param obeyResizeRules
     *            true if we want to obey isLayoutResizeableX.
     * @return the width as described.
     */
    public int getMinimumComponentWidth(final boolean obeyResizeRules) {
        if (!obeyResizeRules || isLayoutResizeableX()) {
            return _minimumContentsSize.getWidth() + getTotalLeft() + getTotalRight();
        } else {
            return getComponentWidth();
        }
    }

    /**
     * If this component does not want us to resize it and we've chosen to listen to that (via passing true) then this
     * method returns the current component height. Otherwise, it returns the height contained in _minimumContentsSize +
     * the margin, border and padding values for top and bottom.
     * 
     * @param obeyResizeRules
     *            true if we want to obey isLayoutResizeableY.
     * @return the height as described.
     */
    public int getMinimumComponentHeight(final boolean obeyResizeRules) {
        if (!obeyResizeRules || isLayoutResizeableY()) {
            return _minimumContentsSize.getHeight() + getTotalTop() + getTotalBottom();
        } else {
            return getComponentHeight();
        }
    }

    /**
     * Sets the width and height of the content area of this component.
     * 
     * @param width
     *            the new width of the content area
     * @param height
     *            the new height of the content area
     */
    public void setContentSize(final int width, final int height) {
        setContentWidth(width);
        setContentHeight(height);
    }

    /**
     * Sets the width of this component's content area.
     * 
     * @param width
     *            the new width of the content area
     */
    public void setContentWidth(final int width) {
        setContentWidth(width, false);
    }

    /**
     * Sets the height of this component's content area.
     * 
     * @param height
     *            the new height of the content area
     */
    public void setContentHeight(final int height) {
        setContentHeight(height, false);
    }

    /**
     * Sets the width of the content area of this component to that given, if we either choose to ignore the rules, or
     * the component is set to allow resize on Y.
     * 
     * @param height
     *            the new height
     * @param obeyResizeRules
     *            true if we want to obey isLayoutResizeableY.
     */
    public void setContentHeight(final int height, final boolean obeyResizeRules) {
        if (!obeyResizeRules || isLayoutResizeableY()) {
            _contentsSize.setHeight(height);
        }
    }

    /**
     * Sets the width of the content area of this component to that given, if we either choose to ignore the rules, or
     * the component is set to allow resize on X.
     * 
     * @param width
     *            the new width
     * @param obeyResizeRules
     *            true if we want to obey isLayoutResizeableX.
     */
    public void setContentWidth(final int width, final boolean obeyResizeRules) {
        if (!obeyResizeRules || isLayoutResizeableX()) {
            _contentsSize.setWidth(width);
        }
    }

    /**
     * Sets the current component height to that given, if we either choose to ignore the rules, or the component is set
     * to allow resize on Y.
     * 
     * @param height
     *            the new height
     * @param obeyResizeRules
     *            true if we want to obey isLayoutResizeableY.
     */
    public void setComponentHeight(final int height, final boolean obeyResizeRules) {
        if (!obeyResizeRules || isLayoutResizeableY()) {
            _contentsSize.setHeight(height - getTotalTop() - getTotalBottom());
        }
    }

    /**
     * Sets the current component width to that given, if we either choose to ignore the rules, or the component is set
     * to allow resize on X.
     * 
     * @param width
     *            the new width
     * @param obeyResizeRules
     *            true if we want to obey isLayoutResizeableX.
     */
    public void setComponentWidth(final int width, final boolean obeyResizeRules) {
        if (!obeyResizeRules || isLayoutResizeableX()) {
            _contentsSize.setWidth(width - getTotalLeft() - getTotalRight());
        }
    }

    /**
     * @return the width of the content area of this component.
     */
    public int getContentWidth() {
        return _contentsSize.getWidth();
    }

    /**
     * @return the height of the content area of this component.
     */
    public int getContentHeight() {
        return _contentsSize.getHeight();
    }

    /**
     * Sets the minimum content size of this component to the values given.
     * 
     * @param width
     * @param height
     */
    public void setMinimumContentSize(final int width, final int height) {
        _minimumContentsSize.set(width, height);
    }

    /**
     * Sets the size of the content area of this component to the current width and height set on _minimumContentsSize
     * (if component is set to allow such resizing.)
     */
    public void compact() {
        if (isLayoutResizeableX()) {
            _contentsSize.setWidth(_minimumContentsSize.getWidth());
        }
        if (isLayoutResizeableY()) {
            _contentsSize.setHeight(_minimumContentsSize.getHeight());
        }
    }

    /**
     * Override this to perform actual layout.
     */
    public void layout() {
        // Let our containers know we are sullied...
        fireComponentDirty();
    }

    /**
     * @return the sum of the bottom side of this component's margin, border and padding (if they are set).
     */
    public int getTotalBottom() {
        int bottom = 0;
        if (getMargin() != null) {
            bottom += getMargin().getBottom();
        }
        if (getBorder() != null) {
            bottom += getBorder().getBottom();
        }
        if (getPadding() != null) {
            bottom += getPadding().getBottom();
        }
        return bottom;
    }

    /**
     * @return the sum of the top side of this component's margin, border and padding (if they are set).
     */
    public int getTotalTop() {
        int top = 0;
        if (getMargin() != null) {
            top += getMargin().getTop();
        }
        if (getBorder() != null) {
            top += getBorder().getTop();
        }
        if (getPadding() != null) {
            top += getPadding().getTop();
        }
        return top;
    }

    /**
     * @return the sum of the left side of this component's margin, border and padding (if they are set).
     */
    public int getTotalLeft() {
        int left = 0;
        if (getMargin() != null) {
            left += getMargin().getLeft();
        }
        if (getBorder() != null) {
            left += getBorder().getLeft();
        }
        if (getPadding() != null) {
            left += getPadding().getLeft();
        }
        return left;
    }

    /**
     * @return the sum of the right side of this component's margin, border and padding (if they are set).
     */
    public int getTotalRight() {
        int right = 0;
        if (getMargin() != null) {
            right += getMargin().getRight();
        }
        if (getBorder() != null) {
            right += getBorder().getRight();
        }
        if (getPadding() != null) {
            right += getPadding().getRight();
        }
        return right;
    }

    /**
     * @return the current border set on this component, if any.
     */
    public UIBorder getBorder() {
        return _border;
    }

    /**
     * @param border
     *            the border we wish this component to use. May be null.
     */
    public void setBorder(final UIBorder border) {
        _border = border;
    }

    /**
     * @return the current backdrop set on this component, if any.
     */
    public UIBackdrop getBackdrop() {
        return _backdrop;
    }

    /**
     * @param backdrop
     *            the backdrop we wish this component to use. May be null.
     */
    public void setBackdrop(final UIBackdrop backDrop) {
        _backdrop = backDrop;
    }

    /**
     * @return the current margin set on this component, if any.
     */
    public Insets getMargin() {
        return _margin;
    }

    /**
     * @param margin
     *            the new margin (a spacing between the component's border and other components) to set on this
     *            component. Copied into the component and is allowed to be null.
     */
    public void setMargin(final Insets margin) {
        if (margin == null) {
            _margin = null;
        } else if (_margin == null) {
            _margin = new Insets(margin);
        } else {
            _margin.set(margin);
        }
    }

    /**
     * @return the current margin set on this component, if any.
     */
    public Insets getPadding() {
        return _padding;
    }

    /**
     * @param padding
     *            the new padding (a spacing between the component's border and its inner content area) to set on this
     *            component. Copied into the component and is allowed to be null.
     */
    public void setPadding(final Insets padding) {
        if (padding == null) {
            _padding = null;
        } else if (_padding == null) {
            _padding = new Insets(padding);
        } else {
            _padding.set(padding);
        }
    }

    /**
     * @return true if our parent is a UIHud.
     */
    public boolean isAttachedToHUD() {
        return getParent() instanceof UIHud;
    }

    /**
     * @return the first instance of UIComponent found in this Component's UIComponent ancestry that is attached to the
     *         hud, or null if none are found. Returns "this" component if it is directly attached to the hud.
     */
    public UIComponent getTopLevelComponent() {
        if (isAttachedToHUD()) {
            return this;
        }
        final Node parent = getParent();
        if (parent instanceof UIComponent) {
            return ((UIComponent) parent).getTopLevelComponent();
        } else {
            return null;
        }
    }

    /**
     * @return the first instance of UIHud found in this Component's UIComponent ancestry or null if none are found.
     */
    public UIHud getHud() {
        final Node parent = getParent();
        if (parent instanceof UIHud) {
            return (UIHud) parent;
        } else if (parent instanceof UIComponent) {
            return ((UIComponent) parent).getHud();
        } else {
            return null;
        }
    }

    /**
     * Override to provide an action to take when this component or its top level component are attached to a UIHud.
     */
    public void attachedToHud() {}

    /**
     * Override to provide an action to take when this component or its top level component are removed to a UIHud.
     */
    public void detachedFromHud() {}

    /**
     * @return current screen x coordinate of this component's origin (usually its lower left corner.)
     */
    public int getHudX() {
        return (int) Math.round(getWorldTranslation().getX());
    }

    /**
     * @return current screen y coordinate of this component's origin (usually its lower left corner.)
     */
    public int getHudY() {
        return (int) Math.round(getWorldTranslation().getY());
    }

    /**
     * Sets the screen x,y coordinate of this component's origin (usually its lower left corner.)
     * 
     * @param x
     * @param y
     */
    public void setHudXY(final int x, final int y) {
        final double newX = getHudX() - getTranslation().getX() + x;
        final double newY = getHudY() - getTranslation().getY() + y;
        setTranslation(newX, newY, getTranslation().getZ());
    }

    /**
     * @param x
     *            the new screen x coordinate of this component's origin (usually its lower left corner.)
     */
    public void setHudX(final int x) {
        final ReadOnlyVector3 translation = getTranslation();
        setTranslation(getHudX() - translation.getX() + x, translation.getY(), translation.getZ());
    }

    /**
     * @param y
     *            the new screen y coordinate of this component's origin (usually its lower left corner.)
     */
    public void setHudY(final int y) {
        final ReadOnlyVector3 translation = getTranslation();
        setTranslation(translation.getX(), getHudY() - translation.getY() + y, translation.getZ());
    }

    /**
     * @return a local x translation from the parent component's content area.
     */
    public int getLocalX() {
        return (int) Math.round(getTranslation().getX());
    }

    /**
     * @return a local y translation from the parent component's content area.
     */
    public int getLocalY() {
        return (int) Math.round(getTranslation().getY());
    }

    /**
     * Set the x,y translation from the lower left corner of our parent's content area to the origin of this component.
     * 
     * @param x
     * @param y
     */
    public void setLocalXY(final int x, final int y) {
        setTranslation(x, y, getTranslation().getZ());
    }

    /**
     * Set the x translation from the lower left corner of our parent's content area to the origin of this component.
     * 
     * @param x
     */
    public void setLocalX(final int x) {
        final ReadOnlyVector3 translation = getTranslation();
        setTranslation(x, translation.getY(), translation.getZ());
    }

    /**
     * Set the y translation from the lower left corner of our parent's content area to the origin of this component.
     * 
     * @param y
     */
    public void setLocalY(final int y) {
        final ReadOnlyVector3 translation = getTranslation();
        setTranslation(translation.getX(), y, translation.getZ());
    }

    /**
     * @return the currently set foreground color on this component. Does not inherit from ancestors or default, so may
     *         return null.
     */
    public ReadOnlyColorRGBA getLocalForegroundColor() {
        return _foregroundColor;
    }

    /**
     * @return the foreground color associated with this component. If none has been set, we will ask our parent
     *         component and so on. If no component is found in our ancestry with a foreground color, we will use
     *         {@link #DEFAULT_FOREGROUND_COLOR}
     */
    public ReadOnlyColorRGBA getForegroundColor() {
        ReadOnlyColorRGBA foreColor = _foregroundColor;
        if (foreColor == null) {
            if (getParent() != null && getParent() instanceof UIComponent) {
                foreColor = ((UIComponent) getParent()).getForegroundColor();
            } else {
                foreColor = UIComponent.DEFAULT_FOREGROUND_COLOR;
            }
        }
        return foreColor;
    }

    /**
     * @param color
     *            the foreground color of this component.
     */
    public void setForegroundColor(final ReadOnlyColorRGBA color) {
        if (color == null) {
            _foregroundColor = null;
        } else if (_foregroundColor == null) {
            _foregroundColor = new ColorRGBA(color);
        } else {
            _foregroundColor.set(color);
        }
    }

    /**
     * @return this component's tooltip text. If none has been set, we will ask our parent component and so on. returns
     *         null if no tooltips are found.
     */
    public String getTooltipText() {
        if (_tooltipText != null) {
            return _tooltipText;
        } else if (getParent() instanceof UIComponent) {
            return ((UIComponent) getParent()).getTooltipText();
        } else {
            return null;
        }
    }

    /**
     * @param text
     *            the tooltip text of this component.
     */
    public void setTooltipText(final String text) {
        _tooltipText = text;
    }

    /**
     * @return the amount of time in ms to wait before showing a tooltip for this component.
     */
    public int getTooltipPopTime() {
        return _tooltipPopTime;
    }

    /**
     * @param ms
     *            the amount of time in ms to wait before showing a tooltip for this component. This is only granular to
     *            a tenth of a second (or 100ms)
     */
    public void setTooltipPopTime(final int ms) {
        _tooltipPopTime = ms;
    }

    /**
     * Check if our tooltip timer is active and cancel it.
     */
    protected void cancelTooltipTimer() {
        if (_showTask != null && !_showTask.isDone()) {
            _showTask.cancel(true);
            _showTask = null;
        }
    }

    /**
     * @param hudX
     *            the x screen coordinate
     * @param hudY
     *            the y screen coordinate
     * @return true if the given screen coordinates fall inside the margin area of this component (or in other words, is
     *         at the border level or deeper.)
     */
    public boolean insideMargin(final int hudX, final int hudY) {
        final int x = hudX - getMargin().getLeft() - getHudX();
        final int y = hudY - getMargin().getBottom() - getHudY();

        return x >= 0
                && x < (getComponentWidth() - getMargin().getLeft() - getMargin().getRight()) * getWorldScale().getX()
                && y >= 0
                && y < (getComponentHeight() - getMargin().getBottom() - getMargin().getTop()) * getWorldScale().getY();
    }

    /**
     * @param hudX
     *            the x screen coordinate
     * @param hudY
     *            the y screen coordinate
     * @return this component (or an appropriate child coordinate in the case of a container) if the given screen
     *         coordinates fall inside the margin area of this component.
     */
    public UIComponent getUIComponent(final int hudX, final int hudY) {
        if (getSceneHints().isPickingHintEnabled(PickingHint.Pickable) && isVisible() && insideMargin(hudX, hudY)) {
            return this;
        }
        return null;
    }

    @Override
    public void updateWorldTransform(final boolean recurse) {
        updateWorldTransform(recurse, true);
    }

    /**
     * Allow skipping updating our own world transform.
     * 
     * @param recurse
     * @param self
     */
    protected void updateWorldTransform(final boolean recurse, final boolean self) {
        if (self) {
            super.updateWorldTransform(false);
        }

        final Node parent = getParent();
        if (parent instanceof UIComponent) {
            // only translate.
            final UIComponent gPar = (UIComponent) parent;
            final Transform t = Transform.fetchTempInstance();
            t.set(getWorldTransform());
            t.translate(gPar.getTotalLeft() * gPar.getWorldScale().getX(), gPar.getTotalBottom()
                    * gPar.getWorldScale().getY(), 0);
            setWorldTransform(t);
            Transform.releaseTempInstance(t);
        }
        if (recurse) {
            for (int i = getNumberOfChildren() - 1; i >= 0; i--) {
                getChild(i).updateWorldTransform(true);
            }
        }
    }

    @Override
    public void draw(final Renderer r) {
        // Don't draw if we are not visible.
        if (!isVisible()) {
            return;
        }

        boolean clearAlpha = false;
        // If we are drawing this component as part of cached container contents, we need to alter the blending to get a
        // texture with the correct color and alpha.
        if (UIContainer.isDrawingStandin()) {
            if (getParent() instanceof UIComponent && !((UIComponent) getParent()).hasVirginContentArea()) {
                // we are drawing a sub component onto a surface that already has color, so do a alpha based color blend
                // and use the max alpha value.
                ContextManager.getCurrentContext().enforceState(UIComponent._blendRGBmaxAlphaBlend);
            } else {
                // we are drawing a top level component onto an empty texture surface, so use the source color modulated
                // by the source alpha and the source alpha value.
                ContextManager.getCurrentContext().enforceState(UIComponent._srcRGBmaxAlphaBlend);
            }
            clearAlpha = true;
        }

        // Call any predraw operation
        predrawComponent(r);

        // Draw the component backdrop
        if (getBackdrop() != null) {
            _virginContentArea = false;
            getBackdrop().draw(r, this);
        } else {
            _virginContentArea = true;
        }

        // draw the component border
        if (getBorder() != null) {
            getBorder().draw(r, this);
        }

        // draw the component, generally editing the content area.
        drawComponent(r);

        // Call any postdraw operation
        postdrawComponent(r);

        // Clear enforced blend, if set.
        if (clearAlpha) {
            ContextManager.getCurrentContext().clearEnforcedState(StateType.Blend);
        }
    }

    /**
     * @param defaultFont
     *            the BMFont to use as "default" across all UI components that do not have one set.
     */
    public static void setDefaultFont(final BMFont defaultFont) {
        UIComponent._defaultFont = defaultFont;
    }

    /**
     * @return the defaultFont. Uses Ardor's default font (arial-24-bold-regular.fnt) if none is set.
     */
    public static BMFont getDefaultFont() {
        if (UIComponent._defaultFont == null) {
            try {
                UIComponent._defaultFont = new BMFont(BasicText.class.getClassLoader().getResource(
                        "com/ardor3d/extension/ui/font/arial-16-bold-regular.fnt"), true);
            } catch (final Exception ex) {
                UIComponent.logger.throwing(BasicText.class.getCanonicalName(), "static font init", ex);
            }
        }
        return UIComponent._defaultFont;
    }

    /**
     * @return a blend state that does alpha blending and writes the max alpha value (source or destination) back to the
     *         color buffer.
     */
    private static BlendState createSrcRGBMaxAlphaBlend() {
        final BlendState state = new BlendState();
        state.setBlendEnabled(true);
        state.setSourceFunctionRGB(SourceFunction.SourceAlpha);
        state.setDestinationFunctionRGB(DestinationFunction.Zero);
        state.setBlendEquationRGB(BlendEquation.Add);
        state.setSourceFunctionAlpha(SourceFunction.One);
        state.setDestinationFunctionAlpha(DestinationFunction.One);
        state.setBlendEquationAlpha(BlendEquation.Max);
        return state;
    }

    /**
     * @return a blend state that does alpha blending and writes the max alpha value (source or destination) back to the
     *         color buffer.
     */
    private static BlendState createBlendRGBMaxAlphaBlend() {
        final BlendState state = new BlendState();
        state.setBlendEnabled(true);
        state.setSourceFunctionRGB(SourceFunction.SourceAlpha);
        state.setDestinationFunctionRGB(DestinationFunction.OneMinusSourceAlpha);
        state.setBlendEquationRGB(BlendEquation.Add);
        state.setSourceFunctionAlpha(SourceFunction.One);
        state.setDestinationFunctionAlpha(DestinationFunction.Zero);
        state.setBlendEquationAlpha(BlendEquation.Add);
        return state;
    }

    /**
     * @return the opacity level set on this component in [0,1], where 0 means completely transparent and 1 is
     *         completely opaque. If useTransparency is false, this will always return 1.
     */
    public float getCombinedOpacity() {
        if (UIComponent._useTransparency) {
            if (getParent() instanceof UIComponent) {
                return _opacity * ((UIComponent) getParent()).getCombinedOpacity();
            } else {
                return _opacity;
            }
        } else {
            return 1.0f;
        }
    }

    /**
     * @return the opacity set on this component directly, not accounting for parent opacity levels.
     */
    public float getLocalOpacity() {
        return _opacity;
    }

    /**
     * Set the opacity level of this component.
     * 
     * @param opacity
     *            value in [0,1], where 0 means completely transparent and 1 is completely opaque.
     */
    public void setOpacity(final float opacity) {
        _opacity = opacity;
    }

    /**
     * Tell all ancestors that use standins, if any, that they need to update any cached graphical representation.
     */
    public void fireComponentDirty() {
        if (getParent() instanceof UIComponent) {
            ((UIComponent) getParent()).fireComponentDirty();
        }
    }

    /**
     * @return true if all components should use their opacity value to blend against other components (and/or the 3d
     *         background scene.)
     */
    public static boolean isUseTransparency() {
        return UIComponent._useTransparency;
    }

    /**
     * @param useTransparency
     *            true if all components should use their opacity value to blend against the 3d scene (and each other)
     */
    public static void setUseTransparency(final boolean useTransparency) {
        UIComponent._useTransparency = useTransparency;
    }

    /**
     * @return the currently rendering component's opacity level. Used by the renderer to alter alpha values based of
     *         component elements.
     */
    public static float getCurrentOpacity() {
        return UIComponent._currentOpacity;
    }

    /**
     * Ask this component to update its minimum allowed size, based on its contents.
     */
    protected void updateMinimumSizeFromContents() {}

    /**
     * Perform any pre-draw operations on this component.
     * 
     * @param renderer
     */
    protected void predrawComponent(final Renderer renderer) {
        UIComponent._currentOpacity = getCombinedOpacity();
    }

    /**
     * Perform any post-draw operations on this component.
     * 
     * @param renderer
     */
    protected void postdrawComponent(final Renderer renderer) {}

    /**
     * Draw this component's contents using the given renderer.
     * 
     * @param renderer
     */
    protected void drawComponent(final Renderer renderer) {}

    // *******************
    // ** INPUT methods
    // *******************

    /**
     * Called when a mouse cursor enters this component.
     * 
     * @param mouseX
     *            mouse x coordinate.
     * @param mouseY
     *            mouse y coordinate.
     * @param state
     *            the current tracked state of the input system.
     */
    public void mouseEntered(final int mouseX, final int mouseY, final InputState state) {
        scheduleToolTip();
    }

    /**
     * 
     */
    private void scheduleToolTip() {
        final UIHud hud = getHud();
        if (hud != null && getTooltipText() != null) {
            final Callable<Void> show = new Callable<Void>() {
                public Void call() throws Exception {

                    while (true) {
                        if (System.currentTimeMillis() >= _toolDone) {
                            break;
                        }
                        Thread.sleep(100);
                    }

                    final UITooltip ttip = hud.getTooltip();

                    // set contents and size
                    ttip.getLabel().setText(getTooltipText());
                    ttip.updateMinimumSizeFromContents();
                    ttip.compact();
                    ttip.layout();

                    // set position based on CURRENT mouse location.
                    int x = hud.getLastMouseX();
                    int y = hud.getLastMouseY();

                    // Try to keep tooltip on screen.
                    if (Camera.getCurrentCamera() != null) {
                        final int displayWidth = Camera.getCurrentCamera().getWidth();
                        final int displayHeight = Camera.getCurrentCamera().getHeight();

                        if (x < 0) {
                            x = 0;
                        } else if (x + ttip.getComponentWidth() > displayWidth) {
                            x = displayWidth - ttip.getComponentWidth();
                        }
                        if (y < 0) {
                            y = 0;
                        } else if (y + ttip.getComponentHeight() > displayHeight) {
                            y = displayHeight - ttip.getComponentHeight();
                        }
                    }
                    ttip.setHudXY(x, y);

                    // fire off that we're dirty
                    ttip.fireComponentDirty();
                    ttip.updateGeometricState(0, true);

                    // show
                    ttip.setVisible(true);
                    return null;
                }
            };
            cancelTooltipTimer();
            resetToolTipTime();
            _showTask = new FutureTask<Void>(show);
            final Thread t = new Thread() {
                @Override
                public void run() {
                    if (_showTask != null && !_showTask.isDone()) {
                        _showTask.run();
                    }
                }
            };
            t.setDaemon(true);
            t.start();
        }
    }

    /**
     * Called when a mouse cursor leaves this component.
     * 
     * @param mouseX
     *            mouse x coordinate.
     * @param mouseY
     *            mouse y coordinate.
     * @param state
     *            the current tracked state of the input system.
     */
    public void mouseDeparted(final int mouseX, final int mouseY, final InputState state) {
        cancelTooltipTimer();
        final UIHud hud = getHud();

        if (hud != null) {
            hud.getTooltip().setVisible(false);
        }
    }

    /**
     * Called when a mouse button is pressed while the cursor is over this component.
     * 
     * @param button
     *            the button that was pressed
     * @param state
     *            the current tracked state of the input system.
     * @return true if we want to consider the event "consumed" by the UI system.
     */
    public boolean mousePressed(final MouseButton button, final InputState state) {
        // default is to offer event to parent, if it is a UIComponent
        if (getParent() instanceof UIComponent) {
            return ((UIComponent) getParent()).mousePressed(button, state);
        } else {
            return false;
        }
    }

    /**
     * Called when a mouse button is released while the cursor is over this component.
     * 
     * @param button
     *            the button that was released
     * @param state
     *            the current tracked state of the input system.
     * @return true if we want to consider the event "consumed" by the UI system.
     */
    public boolean mouseReleased(final MouseButton button, final InputState state) {
        // default is to offer event to parent, if it is a UIComponent
        if (getParent() instanceof UIComponent) {
            return ((UIComponent) getParent()).mouseReleased(button, state);
        } else {
            return false;
        }
    }

    /**
     * Called when a mouse button is moved while the cursor is over this component.
     * 
     * @param mouseX
     *            mouse x coordinate.
     * @param mouseY
     *            mouse y coordinate.
     * @param state
     *            the current tracked state of the input system.
     * @return true if we want to consider the event "consumed" by the UI system.
     */
    public boolean mouseMoved(final int mouseX, final int mouseY, final InputState state) {
        resetToolTipTime();

        // default is to offer event to parent, if it is a UIComponent
        if (getParent() instanceof UIComponent) {
            return ((UIComponent) getParent()).mouseMoved(mouseX, mouseY, state);
        } else {
            return false;
        }
    }

    private void resetToolTipTime() {
        _toolDone = System.currentTimeMillis() + getTooltipPopTime();
    }

    /**
     * Called when the mouse wheel is moved while the cursor is over this component.
     * 
     * @param wheelDx
     *            the last change of the wheel
     * @param state
     *            the current tracked state of the input system.
     * @return true if we want to consider the event "consumed" by the UI system.
     */
    public boolean mouseWheel(final int wheelDx, final InputState state) {
        // default is to offer event to parent, if it is a UIComponent
        if (getParent() instanceof UIComponent) {
            return ((UIComponent) getParent()).mouseWheel(wheelDx, state);
        } else {
            return false;
        }
    }

    /**
     * Called when this component has focus and a key is pressed.
     * 
     * @param key
     *            the key pressed.
     * @param the
     *            current tracked state of the input system.
     * @return true if we want to consider the event "consumed" by the UI system.
     */
    public boolean keyPressed(final Key key, final InputState state) {
        if (getParent() instanceof UIComponent) {
            return ((UIComponent) getParent()).keyPressed(key, state);
        } else {
            return false;
        }
    }

    /**
     * Called when this component has focus and a key is released.
     * 
     * @param key
     *            the key released.
     * @param the
     *            current tracked state of the input system.
     * @return true if we want to consider the event "consumed" by the UI system.
     */
    public boolean keyReleased(final Key key, final InputState state) {
        if (getParent() instanceof UIComponent) {
            return ((UIComponent) getParent()).keyReleased(key, state);
        } else {
            return false;
        }
    }

    /**
     * Called by the hud when a component is given focus.
     */
    public void gainedFocus() {}

    /**
     * Called by the hud when a component loses focus.
     */
    public void lostFocus() {}

    /**
     * Looks up the scenegraph for a Hud and asks it to set us as the currently focused component.
     */
    public void requestFocus() {
        final UIHud hud = getHud();
        if (hud != null) {
            hud.setFocusedComponent(this);
        }
    }

    /**
     * @return a component we defer to for key focus. Default is null.
     */
    public UIComponent getKeyFocusTarget() {
        return _keyFocusTarget;
    }

    /**
     * @param target
     *            a component to set as the actual focused component if this component receives focus.
     */
    public void setKeyFocusTarget(final UIComponent target) {
        _keyFocusTarget = target;
    }
}