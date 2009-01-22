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

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.PixelGrabber;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import com.ardor3d.image.Image;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.util.geom.BufferUtils;

public class AWTImageLoader implements ImageLoader {
    private static final Logger logger = Logger.getLogger(AWTImageLoader.class.getName());

    private static boolean createOnHeap = false;

    public static void registerLoader() {
        final AWTImageLoader loader = new AWTImageLoader();
        ImageLoaderUtil.registerHandler(".JPG", loader);
        ImageLoaderUtil.registerHandler(".GIF", loader);
        ImageLoaderUtil.registerHandler(".PNG", loader);
        ImageLoaderUtil.registerHandler(".BMP", loader);
    }

    public Image load(final InputStream is, final boolean flipImage) throws IOException {
        final BufferedImage image = ImageIO.read(is);
        if (image == null) {
            return null;
        }

        return makeArdor3dImage(image, flipImage);
    }

    public static Image makeArdor3dImage(final BufferedImage image, final boolean flipImage) {
        if (image == null) {
            return null;
        }

        final boolean hasAlpha = hasAlpha(image), grayscale = isGreyscale(image);
        BufferedImage tex;

        if (flipImage
                || ((image).getType() != BufferedImage.TYPE_BYTE_GRAY && (hasAlpha ? (image).getType() != BufferedImage.TYPE_4BYTE_ABGR
                        : (image).getType() != BufferedImage.TYPE_3BYTE_BGR))) {
            // Obtain the image data.
            try {
                tex = new BufferedImage(image.getWidth(null), image.getHeight(null),
                        grayscale ? BufferedImage.TYPE_BYTE_GRAY : hasAlpha ? BufferedImage.TYPE_4BYTE_ABGR
                                : BufferedImage.TYPE_3BYTE_BGR);
            } catch (final IllegalArgumentException e) {
                logger.warning("Problem creating buffered Image: " + e.getMessage());
                return TextureState.getDefaultTextureImage();
            }
            image.getWidth(null);
            image.getHeight(null);

            if (image instanceof BufferedImage) {
                final int imageWidth = image.getWidth(null);
                final int[] tmpData = new int[imageWidth];
                int row = 0;
                final BufferedImage bufferedImage = (image);
                for (int y = image.getHeight(null) - 1; y >= 0; y--) {
                    bufferedImage.getRGB(0, (flipImage ? row++ : y), imageWidth, 1, tmpData, 0, imageWidth);
                    tex.setRGB(0, y, imageWidth, 1, tmpData, 0, imageWidth);
                }
            } else {
                AffineTransform tx = null;
                if (flipImage) {
                    tx = AffineTransform.getScaleInstance(1, -1);
                    tx.translate(0, -image.getHeight(null));
                }
                final Graphics2D g = (Graphics2D) tex.getGraphics();
                g.drawImage(image, tx, null);
                g.dispose();
            }

        } else {
            tex = image;
        }
        // Get a pointer to the image memory
        final byte data[] = (byte[]) tex.getRaster().getDataElements(0, 0, tex.getWidth(), tex.getHeight(), null);
        final ByteBuffer scratch = createOnHeap ? BufferUtils.createByteBufferOnHeap(data.length) : BufferUtils
                .createByteBuffer(data.length);
        scratch.clear();
        scratch.put(data);
        scratch.flip();
        final Image ardorImage = new Image();
        ardorImage.setFormat(grayscale ? Image.Format.Alpha8 : hasAlpha ? Image.Format.RGBA8 : Image.Format.RGB8);
        ardorImage.setWidth(tex.getWidth());
        ardorImage.setHeight(tex.getHeight());
        ardorImage.setData(scratch);
        return ardorImage;
    }

    /**
     * <code>hasAlpha</code> returns true if the specified image has transparent pixels
     * 
     * @param image
     *            Image to check
     * @return true if the specified image has transparent pixels
     */
    protected static boolean hasAlpha(final java.awt.Image image) {
        if (null == image) {
            return false;
        }
        if (image instanceof BufferedImage) {
            final BufferedImage bufferedImage = (BufferedImage) image;
            return bufferedImage.getColorModel().hasAlpha();
        }
        final PixelGrabber pixelGrabber = new PixelGrabber(image, 0, 0, 1, 1, false);
        try {
            pixelGrabber.grabPixels();
            final ColorModel colorModel = pixelGrabber.getColorModel();
            if (colorModel != null) {
                return colorModel.hasAlpha();
            }

            return false;
        } catch (final InterruptedException e) {
            logger.warning("Unable to determine alpha of image: " + image);
        }
        return false;
    }

    /**
     * <code>isGreyscale</code> returns true if the specified image is greyscale.
     * 
     * @param image
     *            Image to check
     * @return true if the specified image is greyscale.
     */
    protected static boolean isGreyscale(final java.awt.Image image) {
        if (null == image) {
            return false;
        }
        if (image instanceof BufferedImage) {
            final BufferedImage bufferedImage = (BufferedImage) image;
            return bufferedImage.getColorModel().getNumComponents() == 1;
        }
        final PixelGrabber pixelGrabber = new PixelGrabber(image, 0, 0, 1, 1, false);
        try {
            pixelGrabber.grabPixels();
            final ColorModel colorModel = pixelGrabber.getColorModel();
            if (colorModel != null) {
                return colorModel.getNumComponents() == 1;
            }

            return false;
        } catch (final InterruptedException e) {
            logger.warning("Unable to determine if image is greyscale: " + image);
        }
        return false;
    }

    public static void setCreateOnHeap(final boolean createOnHeap) {
        AWTImageLoader.createOnHeap = createOnHeap;
    }

    public static boolean isCreateOnHeap() {
        return createOnHeap;
    }
}
