/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.shadow.stencil;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.light.DirectionalLight;
import com.ardor3d.light.Light;
import com.ardor3d.light.PointLight;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.MaterialState;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.VBOInfo;

/**
 * <code>ShadowVolume</code> Represents the shadow volume mesh for a light and an occluder model
 */
public class ShadowVolume extends Mesh {
    private static final long serialVersionUID = 1L;

    protected Light _light = null;
    protected final Vector3 _position = new Vector3();
    protected final Vector3 _direction = new Vector3();
    protected boolean _update = true;
    protected static int _ordinal = 0;

    /**
     * Constructor for <code>ShadowVolume</code>
     * 
     * @param light
     *            the light for which a volume should be created
     */
    public ShadowVolume(final Light light) {
        super("LV" + _ordinal++);

        _light = light;
        setModelBound(new BoundingBox());
        updateModelBound();

        // Initialize the location and direction of the light
        if (light.getType() == Light.Type.Point) {
            _position.set(((PointLight) light).getLocation());
        } else if (light.getType() == Light.Type.Directional) {
            _direction.set(((DirectionalLight) light).getDirection());
        }

        // It will change so make sure VBO is off
        setVBOInfo(new VBOInfo(false));

        // It will not use the renderqueue, so turn that off:
        setRenderBucketType(RenderBucketType.Skip);

        final MaterialState ms = new MaterialState();
        ms.setAmbient(new ColorRGBA(0.5f, 0.7f, 0.7f, 0.2f));
        ms.setDiffuse(new ColorRGBA(0.5f, 0.7f, 0.7f, 0.2f));
        ms.setEmissive(new ColorRGBA(0.9f, 0.9f, 0.7f, 0.6f));
        ms.setAmbient(ColorRGBA.WHITE);
        ms.setDiffuse(ColorRGBA.WHITE);
        ms.setSpecular(ColorRGBA.WHITE);
        ms.setEmissive(ColorRGBA.WHITE);
        ms.setEnabled(true);
        setRenderState(ms);

        final BlendState bs = new BlendState();
        bs.setBlendEnabled(true);
        bs.setEnabled(true);
        setRenderState(bs);
    }

    /**
     * @return Returns the direction.
     */
    public Vector3 getDirection(final Vector3 store) {
        return store.set(_direction);
    }

    /**
     * @param direction
     *            The direction to set.
     */
    public void setDirection(final ReadOnlyVector3 direction) {
        _direction.set(direction);
    }

    /**
     * @return Returns the position.
     */
    public Vector3 getPosition(final Vector3 store) {
        return store.set(_position);
    }

    /**
     * @param position
     *            The position to set.
     */
    public void setPosition(final ReadOnlyVector3 position) {
        _position.set(position);
    }

    /**
     * @return Returns whether this volume needs updating.
     */
    public boolean isUpdate() {
        return _update;
    }

    /**
     * @param update
     *            sets whether this volume needs updating.
     */
    public void setUpdate(final boolean update) {
        _update = update;
    }

    /**
     * @return Returns the light.
     */
    public Light getLight() {
        return _light;
    }

    /**
     * @param light
     *            The light to set.
     */
    public void setLight(final Light light) {
        _light = light;
    }

}
