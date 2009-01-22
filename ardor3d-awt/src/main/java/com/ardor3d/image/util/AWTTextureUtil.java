/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.image.util;

import java.awt.image.BufferedImage;

import com.ardor3d.image.Image;
import com.ardor3d.image.Texture;
import com.ardor3d.util.TextureKey;
import com.ardor3d.util.TextureManager;

public class AWTTextureUtil {

    public static Texture loadTexture(final BufferedImage image, final Texture.MinificationFilter minFilter,
            final Image.Format imageFormat, final boolean flipped) {
        final Image imageData = AWTImageLoader.makeArdor3dImage(image, flipped);
        final TextureKey tkey = new TextureKey(null, flipped, imageFormat, minFilter);
        if (image != null) {
            tkey.setFileType("" + image.hashCode());
        }
        return TextureManager.loadFromKey(tkey, imageData, null);
    }

}
