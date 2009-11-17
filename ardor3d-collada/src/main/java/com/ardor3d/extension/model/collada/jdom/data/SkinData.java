/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.collada.jdom.data;

import java.util.List;

import com.ardor3d.extension.animation.skeletal.SkeletonPose;
import com.ardor3d.extension.animation.skeletal.SkinnedMesh;
import com.ardor3d.scenegraph.Node;
import com.google.common.collect.Lists;

/**
 * Data class used to hold references to useful objects created during parsing of a single Collada <skin> tag.
 */
public class SkinData {

    private SkeletonPose _pose;
    private Node _skinBaseNode;
    private final List<SkinnedMesh> _skins = Lists.newArrayList();
    private final String _name;

    /**
     * Construct a new SkinData object.
     * 
     * @param name
     *            The name for our skin data store. Should be in the format <i>[controller name][ : ][instance
     *            controller name]</i>. The names of each element should be first the name attribute, if present, or
     *            second their sid/id, or blank if neither are specified. The " : " should be left out if one or both
     *            names are blank. If both names are blank, empty string should be used.
     */
    public SkinData(final String name) {
        _name = name;
    }

    public void setPose(final SkeletonPose pose) {
        _pose = pose;
    }

    /**
     * @return the skeletal pose created for this instance of <skin>. If there are multiple skinned meshes parsed as
     *         part of this skin, they will all share this same pose.
     */
    public SkeletonPose getPose() {
        return _pose;
    }

    public void setSkinBaseNode(final Node skinBaseNode) {
        _skinBaseNode = skinBaseNode;
    }

    /**
     * @return the Node created to hold all SkinnedMesh objects created when parsing this skin.
     */
    public Node getSkinBaseNode() {
        return _skinBaseNode;
    }

    public List<SkinnedMesh> getSkins() {
        return _skins;
    }

    /**
     * @return name
     * @see #SkinData(String)
     */
    public String getName() {
        return _name;
    }
}
