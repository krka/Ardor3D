/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.collada.binding.core;

import java.util.HashMap;

import org.jibx.runtime.ITrackSource;

import com.ardor3d.extension.model.collada.binding.ColladaException;
import com.ardor3d.extension.model.collada.binding.DaeLibraries;
import com.ardor3d.extension.model.collada.binding.DaeList;
import com.ardor3d.extension.model.collada.binding.DaeTreeNode;
import com.ardor3d.extension.model.collada.binding.fx.DaeEffect;
import com.ardor3d.extension.model.collada.binding.fx.DaeImage;
import com.ardor3d.extension.model.collada.binding.fx.DaeMaterial;

public class Collada extends DaeTreeNode {
    private final HashMap<String, DaeTreeNode> ids;
    private final HashMap<String, DaeMaterial> boundMaterials;

    private DaeAsset asset;
    private DaeList<DaeLibraries<DaeEffect>> libraryEffects;
    private DaeList<DaeLibraries<DaeImage>> libraryImages;
    private DaeList<DaeLibraries<DaeMaterial>> libraryMaterials;
    private DaeList<DaeLibraries<DaeGeometry>> libraryGeometries;
    private DaeList<DaeLibraries<DaeVisualScene>> libraryVisualScenes;
    private DaeList<DaeLibraries<DaeController>> libraryControllers;
    private DaeList<DaeLibraries<DaeNode>> libraryNodes;
    private DaeScene scene;

    public Collada() {
        ids = new HashMap<String, DaeTreeNode>();
        boundMaterials = new HashMap<String, DaeMaterial>();
    }

    public DaeAsset getAsset() {
        return asset;
    }

    /**
     * @return the libraryEffects
     */
    public DaeList<DaeLibraries<DaeEffect>> getLibraryEffects() {
        return libraryEffects;
    }

    /**
     * @return the libraryImages
     */
    public DaeList<DaeLibraries<DaeImage>> getLibraryImages() {
        return libraryImages;
    }

    /**
     * @return the libraryMaterials
     */
    public DaeList<DaeLibraries<DaeMaterial>> getLibraryMaterials() {
        return libraryMaterials;
    }

    /**
     * @return the libraryGeometries
     */
    public DaeList<DaeLibraries<DaeGeometry>> getLibraryGeometries() {
        return libraryGeometries;
    }

    /**
     * @return the libraryVisualScenes
     */
    public DaeList<DaeLibraries<DaeVisualScene>> getLibraryVisualScenes() {
        return libraryVisualScenes;
    }

    /**
     * @return the libraryControllers
     */
    public DaeList<DaeLibraries<DaeController>> getLibraryControllers() {
        return libraryControllers;
    }

    /**
     * @return the libraryVisualScenes
     */
    public DaeList<DaeLibraries<DaeNode>> getLibraryNodes() {
        return libraryNodes;
    }

    /**
     * @return the scene
     */
    public DaeScene getScene() {
        return scene;
    }

    public void mapId(final String id, final DaeTreeNode node) {
        final DaeTreeNode old = ids.put(id, node);

        if (old != null) {
            throw new ColladaException("Id '" + id + "' previously mapped to node: " + old.toString()
                    + " defined on line " + ((ITrackSource) old).jibx_getLineNumber(), node);
        }
    }

    public void bindMaterial(final String ref, final DaeMaterial material) {
        final DaeMaterial old = boundMaterials.put(ref, material);

        if (old != null) {
            throw new ColladaException("Material '" + ref + "' previously mapped to: " + old.toString()
                    + " defined on line " + ((ITrackSource) old).jibx_getLineNumber(), material);
        }
    }

    public void unbindMaterial(final String ref) {
        boundMaterials.remove(ref);
    }

    @Override
    public String toString() {
        return "Collada root";
    }

    public Iterable<? extends String> getIds() {
        return ids.keySet();
    }

    public DaeTreeNode getTreeNode(final String id) {
        return ids.get(id);
    }

    public DaeMaterial getBoundMaterial(final String ref) {
        return boundMaterials.get(ref);
    }

    public DaeTreeNode resolveUrl(final String url) {
        if (!url.startsWith("#")) {
            throw new ColladaException("Unsupported URL format: " + url, this);
        }

        return getTreeNode(url.substring(1));
    }

    public DaeTreeNode resolveTarget(final String colladaAddress) {
        // TODO: check format

        final int idEndIndex = colladaAddress.indexOf('/');

        if (idEndIndex == -1) {
            throw new ColladaException("Illegal collada address: " + idEndIndex, this);
        }

        final String id = colladaAddress.substring(0, idEndIndex);

        final DaeTreeNode node = getTreeNode(id);

        if (node == null) {
            return null;
        }

        return node.findSubNode(colladaAddress.substring(idEndIndex + 1));
    }

    public static DaeTreeNode findLibraryEntry(final String id, final DaeList<?> daeList) {
        for (int i = 0; i < daeList.size(); i++) {
            final DaeLibraries<?> lib = (DaeLibraries<?>) daeList.get(i);
            final DaeTreeNode node = lib.getLibraries().get(id);
            if (node != null) {
                return node;
            }
        }
        return null;
    }
}
