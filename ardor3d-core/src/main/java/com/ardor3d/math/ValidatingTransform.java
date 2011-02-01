/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.math;

import com.ardor3d.math.type.ReadOnlyMatrix3;
import com.ardor3d.math.type.ReadOnlyMatrix4;
import com.ardor3d.math.type.ReadOnlyQuaternion;
import com.ardor3d.math.type.ReadOnlyTransform;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.util.Ardor3dException;

/**
 * A Transform that checks if any of it's member values are null, NaN or Infinity, and throws an Ardor3dException if so.
 */
public class ValidatingTransform extends Transform {
    public ValidatingTransform() {}

    /**
     * Copy constructor
     * 
     * @param source
     */
    public ValidatingTransform(final ReadOnlyTransform source) {
        super(source);
    }

    private void validate() {
        if (!Transform.isValid(this)) {
            throw new Ardor3dException("Transform is invalid");
        }
    }

    @Override
    public Transform fromHomogeneousMatrix(final ReadOnlyMatrix4 matrix) {
        super.fromHomogeneousMatrix(matrix);
        validate();
        return this;
    }

    @Override
    public void setMatrix(final ReadOnlyMatrix3 matrix) {
        super.setMatrix(matrix);
        validate();
    }

    @Override
    public void setRotation(final ReadOnlyMatrix3 rotation) {
        super.setRotation(rotation);
        validate();
    }

    @Override
    public void setRotation(final ReadOnlyQuaternion rotation) {
        super.setRotation(rotation);
        validate();
    }

    @Override
    public void setScale(final double x, final double y, final double z) {
        super.setScale(x, y, z);
        validate();
    }

    @Override
    public void setScale(final double uniformScale) {
        super.setScale(uniformScale);
        validate();
    }

    @Override
    public void setScale(final ReadOnlyVector3 scale) {
        super.setScale(scale);
        validate();
    }

    @Override
    public void setTranslation(final double x, final double y, final double z) {
        super.setTranslation(x, y, z);
        validate();
    }

    @Override
    public void setTranslation(final ReadOnlyVector3 translation) {
        super.setTranslation(translation);
        validate();
    }

    @Override
    public Transform translate(final double x, final double y, final double z) {
        super.translate(x, y, z);
        validate();
        return this;
    }

    @Override
    public Transform translate(final ReadOnlyVector3 vec) {
        super.translate(vec);
        validate();
        return this;
    }

    @Override
    public Transform multiply(final ReadOnlyTransform transformBy, final Transform store) {
        final Transform transform = super.multiply(transformBy, store);
        if (!Transform.isValid(transform)) {
            throw new Ardor3dException("Transform is invalid");
        }
        return transform;
    }

    @Override
    public Transform invert(final Transform store) {
        final Transform transform = super.invert(store);
        if (!Transform.isValid(transform)) {
            throw new Ardor3dException("Transform is invalid");
        }
        return transform;
    }

    @Override
    public Transform set(final ReadOnlyTransform source) {
        super.set(source);
        validate();
        return this;
    }

    // /////////////////
    // Method for Cloneable
    // /////////////////

    @Override
    public ValidatingTransform clone() {
        return new ValidatingTransform(this);
    }

}
