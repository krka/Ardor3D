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

    protected double length = 1;
    protected double width = .25;

    protected static final Quaternion rotator = new Quaternion();

    public Arrow() {}

    public Arrow(final String name) {
        super(name);
    }

    public Arrow(final String name, final double length, final double width) {
        super(name);
        this.length = length;
        this.width = width;

        buildArrow();
    }

    protected void buildArrow() {
        // Start with cylinders:
        final Cylinder base = new Cylinder("base", 4, 16, width * .75, length);
        rotator.fromAngles(90 * MathUtils.DEG_TO_RAD, 0, 0);
        base.getMeshData().rotatePoints(rotator);
        base.getMeshData().rotateNormals(rotator);
        attachChild(base);
        base.updateModelBound();

        final Pyramid tip = new Pyramid("tip", 2 * width, length / 2f);
        tip.getMeshData().translatePoints(0, length * .75, 0);
        attachChild(tip);
        tip.updateModelBound();
    }

    @Override
    public void write(final Ardor3DExporter e) throws IOException {
        super.write(e);
        final OutputCapsule capsule = e.getCapsule(this);
        capsule.write(length, "length", 1);
        capsule.write(width, "width", .25);
    }

    @Override
    public void read(final Ardor3DImporter e) throws IOException {
        super.read(e);
        final InputCapsule capsule = e.getCapsule(this);
        length = capsule.readDouble("length", 1);
        width = capsule.readDouble("width", .25);

    }

    public double getLength() {
        return length;
    }

    public void setLength(final double length) {
        this.length = length;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(final double width) {
        this.width = width;
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
