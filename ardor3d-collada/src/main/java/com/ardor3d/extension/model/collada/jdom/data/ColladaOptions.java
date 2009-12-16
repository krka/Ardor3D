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

import com.ardor3d.util.resource.ResourceLocator;

public class ColladaOptions {

    private boolean _loadTextures = true;
    private ResourceLocator _textureLocator = null;
    private ResourceLocator _modelLocator = null;

    public boolean isLoadTextures() {
        return _loadTextures;
    }

    public void setLoadTextures(final boolean loadTextures) {
        _loadTextures = loadTextures;
    }

    public ResourceLocator getTextureLocator() {
        return _textureLocator;
    }

    public void setTextureLocator(final ResourceLocator textureLocator) {
        _textureLocator = textureLocator;
    }

    public boolean hasTextureLocator() {
        return getTextureLocator() != null;
    }

    public ResourceLocator getModelLocator() {
        return _modelLocator;
    }

    public void setModelLocator(final ResourceLocator modelLocator) {
        _modelLocator = modelLocator;
    }

    public boolean hasModelLocator() {
        return getModelLocator() != null;
    }
}
