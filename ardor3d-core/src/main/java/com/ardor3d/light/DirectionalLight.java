/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.light;

import java.io.IOException;

import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

/**
 * <code>DirectionalLight</code> defines a light that is assumed to be infintely far away (something similar to the
 * sun). This means the direction of the light rays are all parallel. The direction field of this class identifies the
 * direction in which the light is traveling, which is opposite how jME works.
 */
public class DirectionalLight extends Light {
    private static final long serialVersionUID = 1L;

    private final Vector3 _direction = new Vector3(Vector3.UNIT_Z);

    /**
     * Constructor instantiates a new <code>DirectionalLight</code> object. The initial light colors are white and the
     * direction the light travels is along the positive z axis (0,0,1).
     * 
     */
    public DirectionalLight() {}

    /**
     * @return the direction the light traveling in.
     */
    public ReadOnlyVector3 getDirection() {
        return _direction;
    }

    /**
     * @param direction
     *            the direction the light is traveling in.
     */
    public void setDirection(final ReadOnlyVector3 direction) {
        _direction.set(direction);
    }

    /**
     * @param x
     *            the direction the light is traveling in on the x axis.
     * @param y
     *            the direction the light is traveling in on the y axis.
     * @param z
     *            the direction the light is traveling in on the z axis.
     */
    public void setDirection(final double x, final double y, final double z) {
        _direction.set(x, y, z);
    }

    /**
     * <code>getType</code> returns this light's type (Type.Directional).
     * 
     * @see com.ardor3d.light.Light#getType()
     */
    @Override
    public Type getType() {
        return Type.Directional;
    }

    @Override
    public void write(final Ardor3DExporter e) throws IOException {
        super.write(e);
        final OutputCapsule capsule = e.getCapsule(this);
        capsule.write(_direction, "direction", new Vector3(Vector3.UNIT_Z));

    }

    @Override
    public void read(final Ardor3DImporter e) throws IOException {
        super.read(e);
        final InputCapsule capsule = e.getCapsule(this);
        _direction.set((Vector3) capsule.readSavable("direction", new Vector3(Vector3.UNIT_Z)));

    }

}
