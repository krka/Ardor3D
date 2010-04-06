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

import com.ardor3d.extension.animation.skeletal.AnimationManager;
import com.ardor3d.extension.animation.skeletal.clip.TransformData;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector3;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * <p>
 * Takes two blend sources and uses linear interpolation to merge TransformData values. If one of the sources is null,
 * or does not have a key that the other does, we disregard weighting and use the non-null side's full value.
 * </p>
 *<p>
 * Source data that is not TransformData is not combined, rather A's value will always be used unless it is null.
 * </p>
 */
public class BinaryLERPSource extends AbstractTwoPartSource {

    /**
     * Construct a new lerp source. The two sub sources should be set separately before use.
     */
    public BinaryLERPSource() {}

    /**
     * Construct a new lerp source using the supplied sources.
     * 
     * @param sourceA
     *            our first source.
     * @param sourceB
     *            our second source.
     */
    public BinaryLERPSource(final BlendTreeSource sourceA, final BlendTreeSource sourceB) {
        setSourceA(sourceA);
        setSourceB(sourceB);
    }

    public Map<String, Object> getSourceData(final AnimationManager manager) {
        // grab our data maps from the two sources
        final Map<String, ? extends Object> sourceAData = getSourceA() != null ? getSourceA().getSourceData(manager)
                : null;
        final Map<String, ? extends Object> sourceBData = getSourceB() != null ? getSourceB().getSourceData(manager)
                : null;

        return BinaryLERPSource
                .combineSourceData(sourceAData, sourceBData, manager.getValuesStore().get(getBlendKey()));
    }

    public boolean setTime(final double globalTime, final AnimationManager manager) {
        // set our time on the two sub sources
        boolean foundActive = false;
        if (getSourceA() != null) {
            foundActive |= getSourceA().setTime(globalTime, manager);
        }
        if (getSourceB() != null) {
            foundActive |= getSourceB().setTime(globalTime, manager);
        }
        return foundActive;
    }

    public void resetClips(final AnimationManager manager, final double globalStartTime) {
        // reset our two sub sources
        if (getSourceA() != null) {
            getSourceA().resetClips(manager, globalStartTime);
        }
        if (getSourceB() != null) {
            getSourceB().resetClips(manager, globalStartTime);
        }
    }

    /**
     * Combines two sets of source data maps by matching elements with the same key. Map values of type TransformData
     * are combined via linear interpolation. Other value types are not combined, rather the value from source A is used
     * unless null. Keys that exist only in one map or the other are preserved in the resulting map.
     * 
     * @param sourceAData
     *            our first source map
     * @param sourceBData
     *            our second source map
     * @param blendWeight
     *            our blend weight - used to perform linear interpolation on TransformData values.
     * @return our combined data map.
     */
    public static Map<String, Object> combineSourceData(final Map<String, ? extends Object> sourceAData,
            final Map<String, ? extends Object> sourceBData, final double blendWeight) {
        // XXX: Should blendWeight of 0 or 1 disable non transform data from B/A respectively? Currently blendWeight is
        // ignored in such.
        final Set<String> transformKeys = Sets.newHashSet();
        final Set<String> otherKeys = Sets.newHashSet();
        if (sourceAData != null) {
            for (final String key : sourceAData.keySet()) {
                if (sourceAData.get(key) instanceof TransformData) {
                    if (!transformKeys.contains(key)) {
                        transformKeys.add(key);
                    }
                } else {
                    if (!otherKeys.contains(key)) {
                        otherKeys.add(key);
                    }
                }
            }
        }
        if (sourceBData != null) {
            for (final String key : sourceBData.keySet()) {
                if (sourceBData.get(key) instanceof TransformData) {
                    if (!transformKeys.contains(key)) {
                        transformKeys.add(key);
                    }
                } else {
                    if (!otherKeys.contains(key)) {
                        otherKeys.add(key);
                    }
                }
            }
        }

        final Map<String, Object> rVal = Maps.newHashMap();

        Vector3 vectorData;
        double weight, scaleX, scaleY, scaleZ, transX, transY, transZ;
        final Quaternion rotateA = Quaternion.fetchTempInstance();
        final Quaternion rotateB = Quaternion.fetchTempInstance();
        // for each transform related key...
        for (final String key : transformKeys) {
            // Grab the transform data for each clip
            final TransformData transformA = sourceAData != null ? (TransformData) sourceAData.get(key) : null;
            final TransformData transformB = sourceBData != null ? (TransformData) sourceBData.get(key) : null;

            // zero out our data
            scaleX = scaleY = scaleZ = transX = transY = transZ = 0;
            rotateA.setIdentity();
            rotateB.setIdentity();

            // blend in data from A
            weight = transformB != null ? 1 - blendWeight : 1;
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
            weight = transformA != null ? blendWeight : 1;
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

            // merge rotations
            final TransformData tData = new TransformData();
            if (transformB == null) {
                tData.setRotation(rotateA);
            } else if (transformA == null) {
                tData.setRotation(rotateB);
            } else {
                tData.setRotation(rotateA.slerpLocal(rotateB, weight));
            }

            // set scale/translation
            tData.setScale(scaleX, scaleY, scaleZ);
            tData.setTranslation(transX, transY, transZ);

            // place in our return hash
            rVal.put(key, tData);
        }
        Quaternion.releaseTempInstance(rotateA);
        Quaternion.releaseTempInstance(rotateB);

        // for each non-transform related key...
        for (final String key : otherKeys) {
            // Grab the data for each clip
            final Object dataA = sourceAData != null ? sourceAData.get(key) : null;
            final Object dataB = sourceBData != null ? sourceBData.get(key) : null;

            // A will always override if not null.
            if (dataA != null) {
                // place in our return hash
                rVal.put(key, dataA);
            } else {
                // B must be non null... place in our return hash
                rVal.put(key, dataB);
            }
        }

        return rVal;
    }
}
