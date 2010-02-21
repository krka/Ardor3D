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

import com.ardor3d.extension.ui.UIScrollBar.Orientation;
import com.ardor3d.extension.ui.event.ActionEvent;
import com.ardor3d.extension.ui.event.ActionListener;
import com.ardor3d.extension.ui.layout.BorderLayout;
import com.ardor3d.extension.ui.layout.BorderLayoutData;
import com.ardor3d.renderer.Renderer;

/**
 * NOTE: Experimental still.
 */
public class UIScrollPanel extends UIPanel {

    private final UIScrollBar verticalScrollBar;
    private final UIScrollBar horizontalScrollBar;
    private UIComponent view;
    private int offsetX;
    private int offsetY;

    public UIScrollPanel() {
        this(null);
    }

    public UIScrollPanel(final UIComponent view) {
        setLayout(new BorderLayout());
        if (view != null) {
            this.view = view;
            view.setLayoutData(BorderLayoutData.CENTER);
            add(view);
        }
        horizontalScrollBar = new UIScrollBar(Orientation.Horizontal);
        horizontalScrollBar.setLayoutData(BorderLayoutData.SOUTH);
        verticalScrollBar = new UIScrollBar(Orientation.Vertical);
        verticalScrollBar.setLayoutData(BorderLayoutData.EAST);
        add(horizontalScrollBar);
        add(verticalScrollBar);
        setDoClip(true);
        horizontalScrollBar.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                offsetX = horizontalScrollBar.getOffset();
                fireComponentDirty();
                updateScrollBarSliders();
            }
        });
        verticalScrollBar.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                offsetY = verticalScrollBar.getOffset();
                fireComponentDirty();
                updateScrollBarSliders();
            }
        });
    }

    @Override
    protected void drawComponent(final Renderer renderer) {
        horizontalScrollBar.onDraw(renderer);
        verticalScrollBar.onDraw(renderer);
        renderer.pushClip(getHudX() + getTotalLeft(), getHudY() + getTotalBottom()
                + horizontalScrollBar.getContentHeight(), getContentWidth() - verticalScrollBar.getContentWidth(),
                getContentHeight() - horizontalScrollBar.getContentHeight());
        // temporary translate the view - this is a hack and there may be a better solution
        // System.out.println("draw view port");
        final int x = view.getLocalX();
        final int y = view.getLocalY();
        view.setLocalXY(x - offsetX, y - offsetY);
        view.updateWorldTransform(true);
        view.draw(renderer);
        view.setLocalXY(x, y);
        renderer.popClip();
    }

    @Override
    public void updateMinimumSizeFromContents() {
        setMinimumContentSize(verticalScrollBar.getContentWidth() + 1, horizontalScrollBar.getContentHeight() + 1);
        updateScrollBarSliders();
    }

    private void updateScrollBarSliders() {
        if (view != null) {
            float rel = (float) getContentHeight() / view.getLocalComponentHeight();
            verticalScrollBar.setRelativeSliderLength(rel);
            rel = (float) offsetY / view.getLocalComponentHeight();
            verticalScrollBar.setRelativeOffset(rel);
            verticalScrollBar.setMaxOffset(view.getLocalComponentHeight() - getContentHeight()
                    + horizontalScrollBar.getLocalComponentHeight());
            rel = (float) getContentWidth() / view.getLocalComponentWidth();
            horizontalScrollBar.setRelativeSliderLength(rel);
            rel = (float) offsetX / view.getLocalComponentWidth();
            horizontalScrollBar.setRelativeOffset(rel);
            horizontalScrollBar.setMaxOffset(view.getLocalComponentWidth() - getContentWidth()
                    + verticalScrollBar.getLocalComponentWidth());
            verticalScrollBar.fireComponentDirty();
            horizontalScrollBar.fireComponentDirty();
        }
    }

    @Override
    public void layout() {
        super.layout();
        updateScrollBarSliders();
    }
}
