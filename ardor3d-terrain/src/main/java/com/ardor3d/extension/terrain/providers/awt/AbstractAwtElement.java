/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.terrain.providers.awt;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.image.BufferedImage;

import com.ardor3d.math.Transform;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.Vector4;
import com.ardor3d.math.type.ReadOnlyTransform;
import com.ardor3d.math.type.ReadOnlyVector4;

public abstract class AbstractAwtElement {

    protected final Transform _transform = new Transform();
    protected Composite _compositeOverride;
    protected ElementUpdateListener _listener;

    protected Vector4 _awtBounds = new Vector4();

    public AbstractAwtElement(final ReadOnlyTransform transform, final Composite compositeOverride) {
        _transform.set(transform);
        _compositeOverride = compositeOverride;
    }

    public ReadOnlyTransform getTransform() {
        return _transform;
    }

    public void setTransform(final ReadOnlyTransform transform) {
        _transform.set(transform);
        updateBounds();
    }

    public Composite getCompositeOverride() {
        return _compositeOverride;
    }

    public void setCompositeOverride(final Composite override) {
        _compositeOverride = override;
        updateBounds();
    }

    public abstract void drawTo(BufferedImage image, ReadOnlyTransform localTransform, int clipmapLevel);

    public abstract void updateBoundsFromElement();

    public void updateBounds() {
        final Vector4 oldBounds = new Vector4(_awtBounds);
        // update using size of element
        updateBoundsFromElement();

        // So apply transform
        final Vector3[] vects = new Vector3[4];
        for (int i = 0; i < 4; i++) {
            vects[i] = new Vector3(_awtBounds.getX(), _awtBounds.getY(), 0);
        }
        vects[1].addLocal(_awtBounds.getZ(), 0, 0);
        vects[2].addLocal(_awtBounds.getZ(), _awtBounds.getW(), 0);
        vects[3].addLocal(0, _awtBounds.getW(), 0);

        // update final bounds info.
        double minX, minY, maxX, maxY;
        minX = minY = Double.POSITIVE_INFINITY;
        maxX = maxY = Double.NEGATIVE_INFINITY;

        for (final Vector3 vect : vects) {
            _transform.applyForward(vect);
            if (vect.getX() < minX) {
                minX = vect.getX();
            }
            if (vect.getX() > maxX) {
                maxX = vect.getX();
            }
            if (vect.getY() < minY) {
                minY = vect.getY();
            }
            if (vect.getY() > maxY) {
                maxY = vect.getY();
            }
        }

        _awtBounds.set(minX, minY, maxX - minX, maxY - minY);

        if (_listener != null) {
            _listener.elementUpdated(oldBounds, _awtBounds);
        }
    }

    public ReadOnlyVector4 getBounds() {
        return _awtBounds;
    }

    public void setUpdateListener(final ElementUpdateListener listener) {
        _listener = listener;
    }

    public static AlphaComposite makeAlphaComposite(final float alpha) {
        final int type = AlphaComposite.SRC_OVER;
        return AlphaComposite.getInstance(type, alpha);
    }
}
