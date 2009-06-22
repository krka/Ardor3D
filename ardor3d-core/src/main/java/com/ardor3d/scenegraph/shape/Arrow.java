/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scenegraph.shape;

import java.io.IOException;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Quaternion;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

/**
 * <code>Arrow</code> is basically a cylinder with a pyramid on top.
 */
public class Arrow extends Node {
    private static final long serialVersionUID = 1L;

    protected double _length = 1;
    protected double _width = .25;

    protected static final Quaternion rotator = new Quaternion();

    public Arrow() {}

    public Arrow(final String name) {
        super(name);
    }

    public Arrow(final String name, final double length, final double width) {
        super(name);
        _length = length;
        _width = width;

        buildArrow();
    }

    protected void buildArrow() {
        // Start with cylinders:
        final Cylinder base = new Cylinder("base", 4, 16, _width * .75, _length);
        rotator.fromEulerAngles(0, 0, 90 * MathUtils.DEG_TO_RAD);
        base.getMeshData().rotatePoints(rotator);
        base.getMeshData().rotateNormals(rotator);
        attachChild(base);
        base.updateModelBound();

        final Pyramid tip = new Pyramid("tip", 2 * _width, _length / 2f);
        tip.getMeshData().translatePoints(0, _length * .75, 0);
        attachChild(tip);
        tip.updateModelBound();
    }

    @Override
    public void write(final Ardor3DExporter e) throws IOException {
        super.write(e);
        final OutputCapsule capsule = e.getCapsule(this);
        capsule.write(_length, "length", 1);
        capsule.write(_width, "width", .25);
    }

    @Override
    public void read(final Ardor3DImporter e) throws IOException {
        super.read(e);
        final InputCapsule capsule = e.getCapsule(this);
        _length = capsule.readDouble("length", 1);
        _width = capsule.readDouble("width", .25);

    }

    public double getLength() {
        return _length;
    }

    public void setLength(final double length) {
        _length = length;
    }

    public double getWidth() {
        return _width;
    }

    public void setWidth(final double width) {
        _width = width;
    }

    public void setSolidColor(final ColorRGBA color) {
        for (int x = 0; x < getNumberOfChildren(); x++) {
            if (getChild(x) instanceof Mesh) {
                ((Mesh) getChild(x)).setSolidColor(color);
            }
        }
    }

    public void setDefaultColor(final ColorRGBA color) {
        for (int x = 0; x < getNumberOfChildren(); x++) {
            if (getChild(x) instanceof Mesh) {
                ((Mesh) getChild(x)).setDefaultColor(color);
            }
        }
    }

}
