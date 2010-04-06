/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.collada.jdom.data;

import java.util.List;

import com.ardor3d.extension.animation.skeletal.clip.AnimationClip;
import com.ardor3d.extension.animation.skeletal.clip.JointChannel;
import com.ardor3d.scenegraph.Node;
import com.google.common.collect.Lists;

/**
 * Data storage object meant to hold objects parsed from a Collada file that the user might want to directly access.
 */
public class ColladaStorage {

    private Node _scene;
    private final List<SkinData> _skins = Lists.newArrayList();
    private AssetData assetData;

    private final List<JointChannel> _jointChannels = Lists.newArrayList();
    private AnimationItem animationItemRoot;

    public void setScene(final Node scene) {
        _scene = scene;
    }

    /**
     * @return a Node representing the parsed "visual scene".
     */
    public Node getScene() {
        return _scene;
    }

    /**
     * @return a list of data objects representing each <skin> tag parsed during reading of the visual scene.
     */
    public List<SkinData> getSkins() {
        return _skins;
    }

    public AssetData getAssetData() {
        return assetData;
    }

    public void setAssetData(final AssetData assetData) {
        this.assetData = assetData;
    }

    public List<JointChannel> getJointChannels() {
        return _jointChannels;
    }

    public AnimationItem getAnimationItemRoot() {
        return animationItemRoot;
    }

    public void setAnimationItemRoot(final AnimationItem animationItemRoot) {
        this.animationItemRoot = animationItemRoot;
    }

    public AnimationClip extractChannelsAsClip(final String name) {
        final AnimationClip clip = new AnimationClip(name);
        for (final JointChannel channel : getJointChannels()) {
            clip.addChannel(channel);
        }
        return clip;
    }
}
