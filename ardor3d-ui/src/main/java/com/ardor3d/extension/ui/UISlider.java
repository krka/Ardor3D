/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui;

import java.util.ArrayList;
import java.util.List;

import com.ardor3d.extension.ui.border.SolidBorder;
import com.ardor3d.extension.ui.event.ActionEvent;
import com.ardor3d.extension.ui.event.ActionListener;
import com.ardor3d.extension.ui.event.DragListener;
import com.ardor3d.extension.ui.layout.BorderLayout;
import com.ardor3d.extension.ui.layout.BorderLayoutData;

/**
 * Still experimental.
 */
public class UISlider extends UIPanel implements DragListener {

    private final Orientation orientation;

    private final UIPanel barBackground;
    private final UIButton btSlider;
    private int offset = 0;
    private int maxOffset;
    /** List of action listeners notified when this slider is changed. */
    private final List<ActionListener> _listeners = new ArrayList<ActionListener>();
    private int startDragX;
    private int startDragY;

    /**
     * create a slider widget with a default range of 0..100
     * 
     * @param orientation
     *            the orientation of the slider (Orientation.Horizontal or Orientation.Vertical)
     */
    public UISlider(final Orientation orientation) {
        this(orientation, 100);
    }

    /**
     * create a slider widget with a default range of 0..maxOffset
     * 
     * @param orientation
     *            the orientation of the slider (Orientation.Horizontal or Orientation.Vertical)
     * @param maxOffset
     *            the maximum value the slider can take (inclusive); the minimum value is 0
     */
    public UISlider(final Orientation orientation, final int maxOffset) {
        this.orientation = orientation;
        this.maxOffset = maxOffset;
        barBackground = new UIPanel();
        barBackground.setBorder(new SolidBorder(1, 1, 1, 1));
        btSlider = new UIButton("");
        setLayout(new BorderLayout());
        add(barBackground);
        add(btSlider);

        barBackground.setLayoutData(BorderLayoutData.CENTER);
        applySkin();

        updateMinimumSizeFromContents();
        compact();

        layout();
    }

    private void fireChangeEvent() {
        final ActionEvent event = new ActionEvent(this);
        for (final ActionListener l : _listeners) {
            l.actionPerformed(event);
        }
    }

    /**
     * Add the specified listener to this scrollbar's list of listeners notified when it's changed. Get the current
     * value using the getOffset() function when you receive a change event.
     * 
     * @param listener
     */
    public void addActionListener(final ActionListener listener) {
        _listeners.add(listener);
    }

    public Orientation getOrientation() {
        return orientation;
    }

    @Override
    public void layout() {
        super.layout();
        int freeSpace;
        if (orientation == Orientation.Horizontal) {
            freeSpace = barBackground.getContentWidth() - btSlider.getLocalComponentWidth();
            final int loc = Math.round((float) offset / maxOffset * freeSpace);
            btSlider.setLocalComponentHeight(barBackground.getContentHeight());
            btSlider.setLocalXY(loc, barBackground.getLocalY());
        } else {
            freeSpace = barBackground.getContentHeight() - btSlider.getLocalComponentHeight();
            final int loc = Math.round((float) offset / maxOffset * freeSpace);
            btSlider.setLocalComponentWidth(barBackground.getContentWidth());
            btSlider.setLocalXY(barBackground.getLocalX(), loc);
        }
    }

    /**
     * get the current offset from the beginning of the range or in other words the value of the slider
     * 
     * @return the value as 0 <= offset <= maxOffset
     */
    public int getOffset() {
        return offset;
    }

    /**
     * set the value of the slider
     * 
     * @param offset
     */
    public void setOffset(final int offset) {
        this.offset = offset;
    }

    /**
     * set the maximum value of the slider
     * 
     * @param maxOffset
     */
    public void setMaxOffset(final int maxOffset) {
        this.maxOffset = maxOffset;
    }

    @Override
    public void updateMinimumSizeFromContents() {
        setMinimumContentSize(btSlider.getLocalComponentWidth(), btSlider.getLocalComponentHeight());
    }

    public void drag(final int mouseX, final int mouseY) {
        if (orientation == Orientation.Horizontal) {
            final int freeSpace = barBackground.getContentWidth() - btSlider.getLocalComponentWidth();
            offset = Math.round((mouseX - startDragX) / (float) freeSpace * maxOffset);
            if (offset < 0) {
                offset = 0;
            }
            if (offset > maxOffset) {
                offset = maxOffset;
            }
        } else {
            final int freeSpace = barBackground.getContentHeight() - btSlider.getLocalComponentHeight();
            offset = Math.round((mouseY - startDragY) / (float) freeSpace * maxOffset);
            if (offset < 0) {
                offset = 0;
            }
            if (offset > maxOffset) {
                offset = maxOffset;
            }
        }
        fireChangeEvent();
        layout();
    }

    public void endDrag(final UIComponent component, final int mouseX, final int mouseY) {}

    public boolean isDragHandle(final UIComponent component, final int mouseX, final int mouseY) {
        return component == btSlider;
    }

    public void startDrag(final int mouseX, final int mouseY) {
        startDragX = mouseX;
        startDragY = mouseY;
    }

    @Override
    public void attachedToHud() {
        super.attachedToHud();
        getHud().addDragListener(this);
    }

    @Override
    public void detachedFromHud() {
        if (getHud() != null) {
            getHud().removeDragListener(this);
        }
        super.detachedFromHud();
    }
}
