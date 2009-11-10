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

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public abstract class AWTImageUtil {

    public static List<BufferedImage> convertToAWT(final com.ardor3d.image.Image input) {
        return convertToAWT(input, Color.WHITE);
    }

    public static List<BufferedImage> convertToAWT(final com.ardor3d.image.Image input, final Color tint) {

        final int size = input.getData().size();
        final int width = input.getWidth(), height = input.getHeight();
        final List<BufferedImage> rVal = new ArrayList<BufferedImage>(size);

        final double tRed = tint != null ? tint.getRed() / 255. : 1.0;
        final double tGreen = tint != null ? tint.getGreen() / 255. : 1.0;
        final double tBlue = tint != null ? tint.getBlue() / 255. : 1.0;
        final double tAlpha = tint != null ? tint.getAlpha() / 255. : 1.0;

        for (int i = 0; i < size; i++) {
            BufferedImage image;
            final ByteBuffer data = input.getData(i);
            data.rewind();
            boolean alpha = false;
            switch (input.getFormat()) {
                // TODO: Add support for more formats.
                case RGBA8:
                    alpha = true;
                case RGB8:
                    if (alpha) {
                        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                    } else {
                        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                    }
                    int index,
                    r,
                    g,
                    b,
                    a,
                    argb;
                    for (int x = 0; x < width; x++) {
                        for (int y = 0; y < height; y++) {
                            index = (alpha ? 4 : 3) * (y * width + x);
                            r = (int) (((data.get(index + 0))) * tRed);
                            g = (int) (((data.get(index + 1))) * tGreen);
                            b = (int) (((data.get(index + 2))) * tBlue);

                            argb = ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);

                            if (alpha) {
                                a = (int) (((data.get(index + 3))) * tAlpha);
                                argb |= (a & 0xFF) << 24;
                            }

                            image.setRGB(x, y, argb);
                        }
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unhandled image format: " + input.getFormat());
            }

            rVal.add(image);
        }

        return rVal;
    }

}
