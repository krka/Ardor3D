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

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import com.ardor3d.extension.animation.skeletal.clip.AnimationClip;
import com.ardor3d.extension.animation.skeletal.clip.JointChannel;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;
import com.google.common.collect.Lists;

/**
 * Data storage object meant to hold objects parsed from a Collada file that the user might want to directly access.
 */
public class ColladaStorage implements Savable {

    private Node _scene;
    private final List<SkinData> _skins = Lists.newArrayList();
    private AssetData _assetData;

    private final List<JointChannel> _jointChannels = Lists.newArrayList();
    private AnimationItem _animationItemRoot;

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
        return _assetData;
    }

    public void setAssetData(final AssetData assetData) {
        _assetData = assetData;
    }

    public List<JointChannel> getJointChannels() {
        return _jointChannels;
    }

    public AnimationItem getAnimationItemRoot() {
        return _animationItemRoot;
    }

    public void setAnimationItemRoot(final AnimationItem animationItemRoot) {
        _animationItemRoot = animationItemRoot;
    }

    public AnimationClip extractChannelsAsClip(final String name) {
        final AnimationClip clip = new AnimationClip(name);
        for (final JointChannel channel : getJointChannels()) {
            clip.addChannel(channel);
        }
        return clip;
    }

    // /////////////////
    // Methods for Savable
    // /////////////////

    @Override
    public Class<?> getClassTag() {
        return this.getClass();
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        _assetData = (AssetData) capsule.readSavable("assetData", null);
        _scene = (Node) capsule.readSavable("scene", null);
        _skins.addAll(capsule.readSavableList("skins", new LinkedList<SkinData>()));
        _jointChannels.clear();
        _jointChannels.addAll(capsule.readSavableList("jointChannels", new LinkedList<JointChannel>()));
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        capsule.write(_assetData, "assetData", null);
        capsule.write(_scene, "scene", null);
        capsule.writeSavableList(_skins, "skins", new LinkedList<SkinData>());
        capsule.writeSavableList(_jointChannels, "jointChannels", new LinkedList<JointChannel>());
    }
}
