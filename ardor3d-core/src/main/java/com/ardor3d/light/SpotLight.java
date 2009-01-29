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

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.util.Ardor3dException;
import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

/**
 * SpotLight defines a light that has a location in space and emits light within a cone. This cone is defined by an
 * angle and exponent. Typically this light's values are attenuated based on the distance of the point light and the
 * object it illuminates.
 */
public class SpotLight extends PointLight {
    private static final long serialVersionUID = 1L;
    // attributes
    private float angle;
    private float exponent;
    private final Vector3 _direction = new Vector3(Vector3.UNIT_Z);

    /**
     * Constructor instantiates a new <code>SpotLight</code> object. The initial position of the light is (0,0,0) with
     * angle 0, and colors white.
     * 
     */
    public SpotLight() {
        super();
        setAmbient(new ColorRGBA(0, 0, 0, 1));
    }

    /**
     * @return the direction the spot light is pointing.
     */
    public ReadOnlyVector3 getDirection() {
        return _direction;
    }

    /**
     * @param direction
     *            the direction the spot light is pointing.
     */
    public void setDirection(final ReadOnlyVector3 direction) {
        _direction.set(direction);
    }

    /**
     * <code>getAngle</code> returns the angle of the spot light.
     * 
     * @see #setAngle(float) for more info
     * @return the angle (in degrees)
     */
    public float getAngle() {
        return angle;
    }

    /**
     * <code>setAngle</code> sets the angle of focus of the spot light measured from the direction vector. Think of this
     * as the angle of a cone. Therefore, if you specify 10 degrees, you will get a 20 degree cone (10 degrees off
     * either side of the direction vector.) 180 degrees means radiate in all directions.
     * 
     * @param angle
     *            the angle (in degrees) which must be between 0 and 90 (inclusive) or the special case 180.
     */
    public void setAngle(final float angle) {
        if (angle < 0 || (angle > 90 && angle != 180)) {
            throw new Ardor3dException("invalid angle.  Angle must be between 0 and 90, or 180");
        }
        this.angle = angle;
    }

    /**
     * <code>getExponent</code> gets the spot exponent of this light.
     * 
     * @see #setExponent(float) for more info
     * @return the spot exponent of this light.
     */
    public float getExponent() {
        return exponent;
    }

    /**
     * <code>setExponent</code> sets the spot exponent of this light. This value represents how focused the light beam
     * is.
     * 
     * @param exponent
     *            the spot exponent of this light. Should be between 0-128
     */
    public void setExponent(final float exponent) {
        this.exponent = exponent;
    }

    /**
     * <code>getType</code> returns the type of this light (Type.Spot).
     * 
     * @see com.ardor3d.light.Light#getType()
     */
    @Override
    public Type getType() {
        return Type.Spot;
    }

    @Override
    public void write(final Ardor3DExporter e) throws IOException {
        super.write(e);
        final OutputCapsule capsule = e.getCapsule(this);
        capsule.write(_direction, "direction", new Vector3(Vector3.UNIT_Z));
        capsule.write(angle, "angle", 0);
        capsule.write(exponent, "exponent", 0);

    }

    @Override
    public void read(final Ardor3DImporter e) throws IOException {
        super.read(e);
        final InputCapsule capsule = e.getCapsule(this);
        _direction.set((Vector3) capsule.readSavable("direction", new Vector3(Vector3.UNIT_Z)));
        angle = capsule.readFloat("angle", 0);
        exponent = capsule.readFloat("exponent", 0);
    }

}
