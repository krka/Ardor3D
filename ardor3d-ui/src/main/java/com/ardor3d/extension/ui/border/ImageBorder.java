/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui.border;

import com.ardor3d.extension.ui.UIComponent;
import com.ardor3d.extension.ui.util.SubTexUtil;
import com.ardor3d.extension.ui.util.SubTex;
import com.ardor3d.renderer.Renderer;

/**
 * This border takes a set of images and draws them around the edge of a UI component. There are eight possible border
 * images - 4 for the sides of the component and 4 for the corners. Of these, only the sides must be provided. If the
 * corners are null, the top and bottom will stretch to fill in the gaps.
 */
public class ImageBorder extends UIBorder {

    private SubTex _leftEdge = null;
    private SubTex _rightEdge = null;
    private SubTex _topEdge = null;
    private SubTex _bottomEdge = null;

    private SubTex _topLeftCorner = null;
    private SubTex _topRightCorner = null;
    private SubTex _bottomLeftCorner = null;
    private SubTex _bottomRightCorner = null;

    /**
     * Construct this border using the given edge images. The corners will not be drawn.
     * 
     * @param leftEdge
     * @param rightEdge
     * @param topEdge
     * @param bottomEdge
     */
    public ImageBorder(final SubTex leftEdge, final SubTex rightEdge, final SubTex topEdge, final SubTex bottomEdge) {
        super(topEdge.getHeight(), leftEdge.getWidth(), bottomEdge.getHeight(), rightEdge.getWidth());

        _leftEdge = leftEdge;
        _rightEdge = rightEdge;
        _topEdge = topEdge;
        _bottomEdge = bottomEdge;
    }

    /**
     * Construct this border using the given edge and side images.
     * 
     * @param leftEdge
     * @param rightEdge
     * @param topEdge
     * @param bottomEdge
     * @param topLeftCorner
     * @param topRightCorner
     * @param bottomLeftCorner
     * @param bottomRightCorner
     */
    public ImageBorder(final SubTex leftEdge, final SubTex rightEdge, final SubTex topEdge, final SubTex bottomEdge,
            final SubTex topLeftCorner, final SubTex topRightCorner, final SubTex bottomLeftCorner,
            final SubTex bottomRightCorner) {
        super(topEdge.getHeight(), leftEdge.getWidth(), bottomEdge.getHeight(), rightEdge.getWidth());

        _leftEdge = leftEdge;
        _rightEdge = rightEdge;
        _topEdge = topEdge;
        _bottomEdge = bottomEdge;
        _topLeftCorner = topLeftCorner;
        _topRightCorner = topRightCorner;
        _bottomLeftCorner = bottomLeftCorner;
        _bottomRightCorner = bottomRightCorner;
    }

    public SubTex getBottomEdge() {
        return _bottomEdge;
    }

    public void setBottomEdge(final SubTex bottomEdge) {
        _bottomEdge = bottomEdge;
    }

    public SubTex getBottomLeftCorner() {
        return _bottomLeftCorner;
    }

    public void setBottomLeftCorner(final SubTex bottomLeftCorner) {
        _bottomLeftCorner = bottomLeftCorner;
    }

    public SubTex getBottomRightCorner() {
        return _bottomRightCorner;
    }

    public void setBottomRightCorner(final SubTex bottomRightCorner) {
        _bottomRightCorner = bottomRightCorner;
    }

    public SubTex getLeftEdge() {
        return _leftEdge;
    }

    public void setLeftEdge(final SubTex leftEdge) {
        _leftEdge = leftEdge;
    }

    public SubTex getRightEdge() {
        return _rightEdge;
    }

    public void setRightEdge(final SubTex rightEdge) {
        _rightEdge = rightEdge;
    }

    public SubTex getTopEdge() {
        return _topEdge;
    }

    public void setTopEdge(final SubTex topEdge) {
        _topEdge = topEdge;
    }

    public SubTex getTopLeftCorner() {
        return _topLeftCorner;
    }

    public void setTopLeftCorner(final SubTex topLeftCorner) {
        _topLeftCorner = topLeftCorner;
    }

    public SubTex getTopRightCorner() {
        return _topRightCorner;
    }

    public void setTopRightCorner(final SubTex topRightCorner) {
        _topRightCorner = topRightCorner;
    }

    @Override
    public void draw(final Renderer renderer, final UIComponent comp) {

        final double scaleX = comp.getWorldScale().getX();
        final double scaleY = comp.getWorldScale().getY();

        // get our general width and height
        final int borderWidth = UIBorder.getBorderWidth(comp);
        final int borderHeight = UIBorder.getBorderHeight(comp);

        // Figure out our bottom left corner
        final double dX = comp.getWorldTranslation().getX() + comp.getMargin().getLeft() * scaleX;
        final double dY = comp.getWorldTranslation().getY() + comp.getMargin().getBottom() * scaleY;

        {
            // draw bottom - stretched to fit
            double leftwidth = _bottomLeftCorner != null ? _bottomLeftCorner.getWidth() : 0;
            double rightwidth = _bottomRightCorner != null ? _bottomRightCorner.getWidth() : 0;
            double x = dX + leftwidth * scaleX;
            double y = dY;
            double width = scaleX * (borderWidth - leftwidth - rightwidth);
            double height = scaleY * _bottomEdge.getHeight();
            SubTexUtil.drawStretchedIcon(renderer, _bottomEdge, x, y, width, height);

            // draw top - stretched to fit
            leftwidth = _topLeftCorner != null ? _topLeftCorner.getWidth() : 0;
            rightwidth = _topRightCorner != null ? _topRightCorner.getWidth() : 0;
            x = dX + leftwidth * scaleX;
            y = dY + (borderHeight - _topEdge.getHeight()) * scaleY;
            width = scaleX * (borderWidth - leftwidth - rightwidth);
            height = scaleY * _topEdge.getHeight();
            SubTexUtil.drawStretchedIcon(renderer, _topEdge, x, y, width, height);
        }

        {
            // draw left - stretched to fit
            int bottomHeight = _bottomLeftCorner != null ? _bottomLeftCorner.getHeight() : _bottomEdge.getHeight();
            int topHeight = _topLeftCorner != null ? _topLeftCorner.getHeight() : _topEdge.getHeight();
            double x = dX;
            double y = dY + bottomHeight * scaleY;
            double width = scaleX * _leftEdge.getWidth();
            double height = scaleY * (borderHeight - bottomHeight - topHeight);
            SubTexUtil.drawStretchedIcon(renderer, _leftEdge, x, y, width, height);

            // draw right - stretched to fit
            bottomHeight = _bottomRightCorner != null ? _bottomRightCorner.getHeight() : _bottomEdge.getHeight();
            topHeight = _topRightCorner != null ? _topRightCorner.getHeight() : _topEdge.getHeight();
            x = dX + (borderWidth - _rightEdge.getWidth()) * scaleX;
            y = dY + bottomHeight * scaleY;
            width = scaleX * _rightEdge.getWidth();
            height = scaleY * (borderHeight - bottomHeight - topHeight);
            SubTexUtil.drawStretchedIcon(renderer, _rightEdge, x, y, width, height);
        }

        // draw corners - not stretched
        if (_topLeftCorner != null) {
            SubTexUtil.drawIcon(renderer, _topLeftCorner, dX, dY + (borderHeight - _topLeftCorner.getHeight()) * scaleY, scaleX,
                    scaleY);
        }
        if (_bottomLeftCorner != null) {
            SubTexUtil.drawIcon(renderer, _bottomLeftCorner, dX, dY, scaleX, scaleY);
        }
        if (_topRightCorner != null) {
            SubTexUtil.drawIcon(renderer, _topRightCorner, dX + (borderWidth - _topRightCorner.getWidth()) * scaleX, dY
                    + (borderHeight - _topRightCorner.getHeight()) * scaleY, scaleX, scaleY);
        }
        if (_bottomRightCorner != null) {
            SubTexUtil.drawIcon(renderer, _bottomRightCorner, dX + (borderWidth - _bottomRightCorner.getWidth()) * scaleX, dY,
                    scaleX, scaleY);
        }

    }

}
