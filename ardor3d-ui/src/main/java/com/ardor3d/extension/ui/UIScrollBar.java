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
import com.ardor3d.extension.ui.event.ActionEvent;
import com.ardor3d.extension.ui.event.ActionListener;
import com.ardor3d.extension.ui.layout.BorderLayout;
import com.ardor3d.extension.ui.layout.BorderLayoutData;

/**
 * NOTE: Experimental still.
 */
public class UIScrollBar extends UIPanel {

    private final Orientation orientation;
    private final UISlider slider;
    private final UIButton btTopLeft;
    private final UIButton btBottomRight;
    private int sliderLength;
    private float relativeSliderLength = 0.5f;
    /** List of action listeners notified when this scrollbar is changed. */
    private final List<ActionListener> _listeners = new ArrayList<ActionListener>();

    public UIScrollBar(final Orientation orientation) {
        this.orientation = orientation;
        slider = new UISlider(orientation);
        slider.setLayoutData(BorderLayoutData.CENTER);
        slider.setBackdrop(new SolidBackdrop(getForegroundColor()));
        btTopLeft = new UIButton(orientation == Orientation.Vertical ? "^" : "<");
        btTopLeft.setPadding(null);
        btBottomRight = new UIButton(orientation == Orientation.Vertical ? "v" : ">");
        btBottomRight.setPadding(null);
        setLayout(new BorderLayout());
        add(btTopLeft);
        add(btBottomRight);
        add(slider);
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
                int offset = slider.getOffset();
                if (direction < 0) {
                    offset -= 10;
                    if (offset < 0) {
                        offset = 0;
                    }
                } else {
                    offset += 10;
                    if (offset > slider.getMaxOffset()) {
                        offset = slider.getMaxOffset();
                    }
                }
                slider.setOffset(offset);
                fireChangeEvent();
            }
        };
        btTopLeft.addActionListener(al);
        btBottomRight.addActionListener(al);
        slider.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                fireChangeEvent();
            }
        });
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
            final int len = Math.max(20, Math.round(relativeSliderLength * slider.getContentWidth()));
            slider.setSliderLength(len);
        } else {
            final int len = Math.max(20, Math.round(relativeSliderLength * slider.getContentHeight()));
            slider.setSliderLength(len);
        }
    }

    public void setRelativeSliderLength(final float relativeSliderLength) {
        this.relativeSliderLength = relativeSliderLength;
    }

    public int getOffset() {
        return slider.getOffset();
    }

    public void setOffset(final int offset) {
        slider.setOffset(offset);
        layout();
        fireComponentDirty();
    }

    public void setMaxOffset(final int maxOffset) {
        slider.setMaxOffset(maxOffset);
    }
}
