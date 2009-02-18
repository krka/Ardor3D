/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.collada.binding.fx;

import com.ardor3d.extension.model.collada.binding.DaeTreeNode;
import com.ardor3d.image.Texture.MagnificationFilter;
import com.ardor3d.image.Texture.MinificationFilter;
import com.ardor3d.image.Texture.WrapMode;

public abstract class DaeSampler extends DaeTreeNode {

    public enum DaeWrapMode {
        WRAP(WrapMode.Repeat), MIRROR(WrapMode.MirroredRepeat), CLAMP(WrapMode.EdgeClamp), BORDER(WrapMode.BorderClamp), NONE(
                WrapMode.BorderClamp);

        final WrapMode _wm;

        private DaeWrapMode(final WrapMode ardorWrapMode) {
            _wm = ardorWrapMode;
        }

        public WrapMode getArdor3dWrapMode() {
            return _wm;
        }
    }

    public enum DaeMinFilter {
        NONE(MinificationFilter.NearestNeighborNoMipMaps), NEAREST(MinificationFilter.NearestNeighborNoMipMaps), LINEAR(
                MinificationFilter.BilinearNoMipMaps), NEAREST_MIPMAP_NEAREST(
                MinificationFilter.NearestNeighborNearestMipMap), LINEAR_MIPMAP_NEAREST(
                MinificationFilter.BilinearNearestMipMap), NEAREST_MIPMAP_LINEAR(
                MinificationFilter.NearestNeighborLinearMipMap), LINEAR_MIPMAP_LINEAR(MinificationFilter.Trilinear);

        final MinificationFilter _mf;

        private DaeMinFilter(final MinificationFilter ardorFilter) {
            _mf = ardorFilter;
        }

        public MinificationFilter getArdor3dFilter() {
            return _mf;
        }
    }

    public enum DaeMagFilter {
        NONE(MagnificationFilter.NearestNeighbor), NEAREST(MagnificationFilter.NearestNeighbor), LINEAR(
                MagnificationFilter.Bilinear), NEAREST_MIPMAP_NEAREST(MagnificationFilter.NearestNeighbor), LINEAR_MIPMAP_NEAREST(
                MagnificationFilter.Bilinear), NEAREST_MIPMAP_LINEAR(MagnificationFilter.NearestNeighbor), LINEAR_MIPMAP_LINEAR(
                MagnificationFilter.Bilinear);

        final MagnificationFilter _mf;

        private DaeMagFilter(final MagnificationFilter ardorFilter) {
            _mf = ardorFilter;
        }

        public MagnificationFilter getArdor3dFilter() {
            return _mf;
        }
    }

    private String source;
    private DaeWrapMode wrapS;
    private DaeMinFilter minfilter;
    private DaeMagFilter magfilter;

    public String getSource() {
        return source;
    }

    /**
     * @return the wrapS
     */
    public DaeWrapMode getWrapS() {
        return wrapS;
    }

    /**
     * @return the minfilter
     */
    public DaeMinFilter getMinfilter() {
        return minfilter;
    }

    /**
     * @return the magfilter
     */
    public DaeMagFilter getMagfilter() {
        return magfilter;
    }
}
