/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer;

import com.ardor3d.framework.DisplaySettings;

public enum TextureRendererFactory {

    INSTANCE;

    private TextureRendererProvider _provider = null;

    public void setProvider(final TextureRendererProvider provider) {
        _provider = provider;
    }

    public TextureRenderer createTextureRenderer(final DisplaySettings settings, final Renderer renderer,
            final ContextCapabilities caps, final TextureRenderer.Target target) {
        return _provider.createTextureRenderer(settings, renderer, caps, target);
    }

}
