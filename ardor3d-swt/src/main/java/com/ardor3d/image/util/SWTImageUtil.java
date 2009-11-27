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

import java.nio.ByteBuffer;
import java.util.List;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;

import com.ardor3d.image.Image;
import com.google.common.collect.Lists;

/**
 * Utility methods for converting Ardor3D Images to SWT ImageData.
 */
public abstract class SWTImageUtil {

    /**
     * Convert the given Ardor3D Image to a List of ImageData objects. It is a List because Ardor3D Images may contain
     * multiple layers (for example, in the case of cube maps or 3D textures).
     * 
     * @param input
     *            the Ardor3D Image to convert
     * @return the ImageData object(s) created in the conversion
     */
    public static List<ImageData> convertToSWT(final Image input) {
        // convert, using a full white tint (i.e. no applied color change from original data.)
        return convertToSWT(input, null, 1.0);
    }

    /**
     * Convert the given Ardor3D Image to a List of ImageData objects. It is a List because Ardor3D Images may contain
     * multiple layers (for example, in the case of cube maps or 3D textures). The given SWT Color is used to modulate
     * or "tint" the returned image.
     * 
     * TODO: Add support for more formats.
     * 
     * @param input
     *            the Ardor3D Image to convert
     * @param tint
     *            the Color to apply to the generated image
     * @return the ImageData object(s) created in the conversion
     */
    public static List<ImageData> convertToSWT(final Image input, final Color tint, final double alphaTint) {
        // count the number of layers we will be converting.
        final int size = input.getData().size();

        // grab our image width and height
        final int width = input.getWidth(), height = input.getHeight();

        // create our return list
        final List<ImageData> rVal = Lists.newArrayList();

        // Calculate our modulation or "tint" values per channel
        final double tRed = tint != null ? tint.getRed() / 255. : 1.0;
        final double tGreen = tint != null ? tint.getGreen() / 255. : 1.0;
        final double tBlue = tint != null ? tint.getBlue() / 255. : 1.0;
        final double tAlpha = alphaTint;

        // go through each layer
        for (int i = 0; i < size; i++) {
            ImageData image;
            final ByteBuffer data = input.getData(i);
            data.rewind();
            boolean alpha = false;
            switch (input.getFormat()) {
                case RGBA8:
                    alpha = true;
                    // Falls through on purpose.
                case RGB8:
                    if (alpha) {
                        // use alpha data... XXX: Is this right?
                        image = new ImageData(width, height, 32, new PaletteData(0xFF0000, 0x00FF00, 0x0000FF));
                        image.transparentPixel = -1;
                    } else {
                        image = new ImageData(width, height, 24, new PaletteData(0xFF0000, 0x00FF00, 0x0000FF));
                        image.transparentPixel = 255;
                    }
                    int index,
                    r,
                    g,
                    b,
                    a,
                    argb;

                    // Go through each pixel
                    for (int x = 0; x < width; x++) {
                        for (int y = 0; y < height; y++) {
                            index = (alpha ? 4 : 3) * (y * width + x);
                            r = (int) (((data.get(index + 0))) * tRed);
                            g = (int) (((data.get(index + 1))) * tGreen);
                            b = (int) (((data.get(index + 2))) * tBlue);

                            // convert to integer expression
                            argb = ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);

                            // add alpha, if applicable
                            if (alpha) {
                                a = (int) (((data.get(index + 3))) * tAlpha);
                                argb |= (a & 0xFF) << 24;
                                image.setAlpha(x, y, a & 0xFF);
                            }

                            // apply to image
                            image.setPixel(x, y, argb);
                        }
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unhandled image format: " + input.getFormat());
            }

            // add to our list
            rVal.add(image);
        }

        // return list
        return rVal;
    }

}