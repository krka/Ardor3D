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
import com.ardor3d.extension.model.collada.binding.core.DaeColor;
import com.ardor3d.extension.model.collada.binding.core.DaeFloat;

public class DaeBlinnPhong extends DaeTreeNode {
    private DaeColor emissionColor;
    private DaeTexture emissionTexture;
    private DaeColor ambientColor;
    private DaeTexture ambientTexture;
    private DaeColor diffuseColor;
    private DaeTexture diffuseTexture;
    private DaeColor specularColor;
    private DaeTexture specularTexture;

    private DaeFloat shininess;

    private DaeColor reflectiveColor;
    private DaeTexture reflectiveTexture;
    private DaeFloat reflectivity;

    private DaeColor transparentColor;
    private DaeTexture transparentTexture;
    private DaeFloat transparency;

    private DaeFloat ioRefraction;

    /**
     * @return the emissionColor
     */
    public DaeColor getEmissionColor() {
        return emissionColor;
    }

    /**
     * @return the emissionTexture
     */
    public DaeTexture getEmissionTexture() {
        return emissionTexture;
    }

    /**
     * @return the ambientColor
     */
    public DaeColor getAmbientColor() {
        return ambientColor;
    }

    /**
     * @return the ambientTexture
     */
    public DaeTexture getAmbientTexture() {
        return ambientTexture;
    }

    /**
     * @return the diffuseColor
     */
    public DaeColor getDiffuseColor() {
        return diffuseColor;
    }

    /**
     * @return the diffuseTexture
     */
    public DaeTexture getDiffuseTexture() {
        return diffuseTexture;
    }

    /**
     * @return the specularColor
     */
    public DaeColor getSpecularColor() {
        return specularColor;
    }

    /**
     * @return the specularTexture
     */
    public DaeTexture getSpecularTexture() {
        return specularTexture;
    }

    /**
     * @return the shininess
     */
    public DaeFloat getShininess() {
        return shininess;
    }

    /**
     * @return the reflectiveColor
     */
    public DaeColor getReflectiveColor() {
        return reflectiveColor;
    }

    /**
     * @return the reflectiveTexture
     */
    public DaeTexture getReflectiveTexture() {
        return reflectiveTexture;
    }

    /**
     * @return the reflectivity
     */
    public DaeFloat getReflectivity() {
        return reflectivity;
    }

    /**
     * @return the transparentColor
     */
    public DaeColor getTransparentColor() {
        return transparentColor;
    }

    /**
     * @return the transparentTexture
     */
    public DaeTexture getTransparentTexture() {
        return transparentTexture;
    }

    /**
     * @return the transparency
     */
    public DaeFloat getTransparency() {
        return transparency;
    }

    /**
     * @return the ioRefraction
     */
    public DaeFloat getIoRefraction() {
        return ioRefraction;
    }
}
