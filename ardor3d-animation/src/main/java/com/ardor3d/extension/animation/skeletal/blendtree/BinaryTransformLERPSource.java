/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.animation.skeletal.blendtree;

import java.util.Map;
import java.util.Set;

import com.ardor3d.extension.animation.skeletal.TransformData;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector3;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class BinaryTransformLERPSource implements BlendTreeSource {

    private BlendTreeSource _sourceA;
    private BlendTreeSource _sourceB;
    private double _blendWeight = 0.0;

    public BinaryTransformLERPSource() {}

    public BinaryTransformLERPSource(final BlendTreeSource sourceA, final BlendTreeSource sourceB) {
        setSourceA(sourceA);
        setSourceB(sourceB);
    }

    public BlendTreeSource getSourceA() {
        return _sourceA;
    }

    public BlendTreeSource getSourceB() {
        return _sourceB;
    }

    public double getBlendWeight() {
        return _blendWeight;
    }

    public void setSourceA(final BlendTreeSource sourceA) {
        _sourceA = sourceA;
    }

    public void setSourceB(final BlendTreeSource sourceB) {
        _sourceB = sourceB;
    }

    public void setBlendWeight(final double blendWeight) {
        _blendWeight = blendWeight;
    }

    @Override
    public Map<String, Object> getSourceData() {

        // XXX: Could we reuse one hashmap here? Or better to always make a new instance.
        final Map<String, Object> rVal = Maps.newHashMap();

        // XXX: Perhaps this can be done once and cached?
        // XXX: NOTE! We lose the non-transform keys here. Is this what we want?

        // grab transform names from the two sources
        final Set<String> transformKeys = Sets.newHashSet();
        final Map<String, ? extends Object> sourceAData = getSourceA().getSourceData();
        final Map<String, ? extends Object> sourceBData = getSourceB().getSourceData();
        for (final String key : sourceAData.keySet()) {
            if (!transformKeys.contains(key) && sourceAData.get(key) instanceof TransformData) {
                transformKeys.add(key);
            }
        }
        for (final String key : sourceBData.keySet()) {
            if (!transformKeys.contains(key) && sourceBData.get(key) instanceof TransformData) {
                transformKeys.add(key);
            }
        }

        Vector3 vectorData;
        double weight, scaleX, scaleY, scaleZ, transX, transY, transZ;
        final Quaternion rotateA = Quaternion.fetchTempInstance();
        final Quaternion rotateB = Quaternion.fetchTempInstance();
        // for each transform related key...
        for (final String key : transformKeys) {
            // Grab the transform data for each clip
            final TransformData transformA = (TransformData) sourceAData.get(key);
            final TransformData transformB = (TransformData) sourceBData.get(key);

            // zero out our data
            scaleX = scaleY = scaleZ = transX = transY = transZ = 0;
            rotateA.setIdentity();
            rotateB.setIdentity();

            // blend in data from A
            weight = transformB != null ? 1 - getBlendWeight() : 1;
            if (transformA != null) {
                vectorData = transformA.getTranslation();
                transX += vectorData.getX() * weight;
                transY += vectorData.getY() * weight;
                transZ += vectorData.getZ() * weight;

                vectorData = transformA.getScale();
                scaleX += vectorData.getX() * weight;
                scaleY += vectorData.getY() * weight;
                scaleZ += vectorData.getZ() * weight;

                rotateA.set(transformA.getRotation());
            }

            // blend in data from B
            weight = transformA != null ? getBlendWeight() : 1;
            if (transformB != null) {
                vectorData = transformB.getTranslation();
                transX += vectorData.getX() * weight;
                transY += vectorData.getY() * weight;
                transZ += vectorData.getZ() * weight;

                vectorData = transformB.getScale();
                scaleX += vectorData.getX() * weight;
                scaleY += vectorData.getY() * weight;
                scaleZ += vectorData.getZ() * weight;

                rotateB.set(transformB.getRotation());
            }

            final TransformData tData = new TransformData();
            if (transformB == null) {
                tData.setRotation(rotateA);
            } else if (transformA == null) {
                tData.setRotation(rotateB);
            } else {
                tData.setRotation(rotateA.slerpLocal(rotateB, weight));
            }
            tData.setScale(scaleX, scaleY, scaleZ);
            tData.setTranslation(transX, transY, transZ);
            rVal.put(key, tData);
        }
        Quaternion.releaseTempInstance(rotateA);
        Quaternion.releaseTempInstance(rotateB);

        return rVal;
    }
}
