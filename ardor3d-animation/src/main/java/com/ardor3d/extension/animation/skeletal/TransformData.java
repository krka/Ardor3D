/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.animation.skeletal;

import java.io.IOException;

import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Transform;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyQuaternion;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

public class TransformData implements Savable {
    private final Quaternion _rotation = new Quaternion(Quaternion.IDENTITY);
    private final Vector3 _scale = new Vector3(Vector3.ONE);
    private final Vector3 _translation = new Vector3(Vector3.ZERO);

    public TransformData() {}

    public TransformData(final TransformData tData) {
        set(tData);
    }

    public void set(final TransformData tData) {
        _rotation.set(tData.getRotation());
        _scale.set(tData.getScale());
        _translation.set(tData.getTranslation());
    }

    public Quaternion getRotation() {
        return _rotation;
    }

    public void setRotation(final ReadOnlyQuaternion rotation) {
        _rotation.set(rotation);
    }

    public void setRotation(final double x, final double y, final double z, final double w) {
        _rotation.set(x, y, z, w);
    }

    public Vector3 getScale() {
        return _scale;
    }

    public void setScale(final ReadOnlyVector3 scale) {
        _scale.set(scale);
    }

    public void setScale(final double x, final double y, final double z) {
        _scale.set(x, y, z);
    }

    public Vector3 getTranslation() {
        return _translation;
    }

    public void setTranslation(final ReadOnlyVector3 translation) {
        _translation.set(translation);
    }

    public void setTranslation(final double x, final double y, final double z) {
        _translation.set(x, y, z);
    }

    public void applyTo(final Transform transform) {
        transform.setIdentity();
        transform.setRotation(getRotation());
        transform.setScale(getScale());
        transform.setTranslation(getTranslation());
    }

    // /////////////////
    // Methods for Savable
    // /////////////////

    public Class<? extends TransformData> getClassTag() {
        return this.getClass();
    }

    public void write(final OutputCapsule capsule) throws IOException {
        capsule.write(_rotation, "rotation", new Quaternion(Quaternion.IDENTITY));
        capsule.write(_scale, "scale", new Vector3(Vector3.ONE));
        capsule.write(_translation, "translation", new Vector3(Vector3.ZERO));
    }

    public void read(final InputCapsule capsule) throws IOException {
        setRotation((Quaternion) capsule.readSavable("rotation", new Quaternion(Quaternion.IDENTITY)));
        setScale((Vector3) capsule.readSavable("scale", new Vector3(Vector3.ONE)));
        setTranslation((Vector3) capsule.readSavable("rotation", new Vector3(Vector3.ZERO)));
    }
}
