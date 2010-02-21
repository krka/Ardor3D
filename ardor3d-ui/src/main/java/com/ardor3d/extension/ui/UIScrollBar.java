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

import com.ardor3d.extension.ui.backdrop.SolidBackdrop;
import com.ardor3d.extension.ui.border.SolidBorder;
import com.ardor3d.extension.ui.event.ActionEvent;
import com.ardor3d.extension.ui.event.ActionListener;
import com.ardor3d.extension.ui.layout.BorderLayout;
import com.ardor3d.extension.ui.layout.BorderLayoutData;

/**
 * NOTE: Experimental still.
 */
public class UIScrollBar extends UIPanel {

    public static enum Orientation {
        Horizontal, Vertical
    };

    private final Orientation orientation;
    private final UIPanel barBackground;
    private final UIPanel barForeground;
    private final UIButton btTopLeft;
    private final UIButton btBottomRight;
    private float relativeOffset = 0f;
    private int offset = 0;
    private int maxOffset = 0;
    private int sliderLength;
    private float relativeSliderLength = 0.5f;
    /** List of action listeners notified when this scrollbar is changed. */
    private final List<ActionListener> _listeners = new ArrayList<ActionListener>();

    public UIScrollBar(final Orientation orientation) {
        this.orientation = orientation;
        barBackground = new UIPanel();
        barBackground.setBorder(new SolidBorder(1, 1, 1, 1));
        barForeground = new UIPanel();
        barForeground.setBorder(new SolidBorder(1, 1, 1, 1));
        barForeground.setBackdrop(new SolidBackdrop(getForegroundColor()));
        btTopLeft = new UIButton(orientation == Orientation.Vertical ? "^" : "<");
        btTopLeft.setPadding(null);
        btBottomRight = new UIButton(orientation == Orientation.Vertical ? "v" : ">");
        btBottomRight.setPadding(null);
        setLayout(new BorderLayout());
        add(btTopLeft);
        add(btBottomRight);
        add(barBackground);
        add(barForeground);
        barBackground.setLayoutData(BorderLayoutData.CENTER);
        if (orientation == Orientation.Vertical) {
            btTopLeft.setLayoutData(BorderLayoutData.NORTH);
            btBottomRight.setLayoutData(BorderLayoutData.SOUTH);
        } else {
            btTopLeft.setLayoutData(BorderLayoutData.WEST);
            btBottomRight.setLayoutData(BorderLayoutData.EAST);
        }
        applySkin();

        updateMinimumSizeFromContents();
        compact();

        layout();
        final ActionListener al = new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                int direction;
                if (event.getSource() == btTopLeft) {
                    direction = UIScrollBar.this.orientation == Orientation.Horizontal ? -1 : 1;
                } else {
                    direction = UIScrollBar.this.orientation == Orientation.Horizontal ? 1 : -1;
                }
                if (direction < 0) {
                    offset -= 10;
                    if (offset < 0) {
                        offset = 0;
                    }
                } else {
                    offset += 10;
                    if (offset > maxOffset) {
                        offset = maxOffset;
                    }
                }
                fireChangeEvent();
            }
        };
        btTopLeft.addActionListener(al);
        btBottomRight.addActionListener(al);
    }

    private void fireChangeEvent() {
        final ActionEvent event = new ActionEvent(this);
        for (final ActionListener l : _listeners) {
            l.actionPerformed(event);
        }
    }

    /**
     * Add the specified listener to this scrollbar's list of listeners notified when it's changed.
     * 
     * @param listener
     */
    public void addActionListener(final ActionListener listener) {
        _listeners.add(listener);
    }

    public Orientation getOrientation() {
        return orientation;
    }

    public int getSliderLength() {
        return sliderLength;
    }

    @Override
    public void layout() {
        super.layout();
        if (relativeSliderLength >= 1f) {
            relativeSliderLength = 1f;
        }
        if (orientation == Orientation.Horizontal) {
            final int len = Math.max(20, Math.round(relativeSliderLength * barBackground.getContentWidth()));
            barForeground.setLocalComponentHeight(barBackground.getLocalComponentHeight());
            barForeground.setLocalComponentWidth(len);
            barForeground.setLocalX(Math.round((barBackground.getContentWidth() - len) * relativeOffset
                    + barBackground.getLocalX()));
            barForeground.setLocalY(barBackground.getLocalY());
        } else {
            final int len = Math.max(20, Math.round(relativeSliderLength * barBackground.getContentHeight()));
            barForeground.setLocalComponentHeight(len);
            barForeground.setLocalComponentWidth(barBackground.getLocalComponentWidth());
            barForeground.setLocalX(barBackground.getLocalX());
            barForeground.setLocalY(barBackground.getLocalY()
                    + Math.round((barBackground.getContentHeight() - len) * relativeOffset));
        }
    }

    public void setRelativeSliderLength(final float relativeSliderLength) {
        this.relativeSliderLength = relativeSliderLength;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(final int offset) {
        this.offset = offset;
    }

    public void setRelativeOffset(final float relativeOffset) {
        this.relativeOffset = relativeOffset;
        layout();
        fireComponentDirty();
    }

    public void setMaxOffset(final int maxOffset) {
        this.maxOffset = maxOffset;
    }
}
