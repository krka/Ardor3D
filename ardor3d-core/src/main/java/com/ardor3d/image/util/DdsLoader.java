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

import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.ardor3d.image.Image;
import com.ardor3d.image.Image.Format;
import com.ardor3d.util.LittleEndien;
import com.ardor3d.util.geom.BufferUtils;

/**
 * 
 * <code>DDSLoader</code> is an image loader that reads in a DirectX DDS file. Supports DXT1, DXT3, DXT5, RGB, RGBA,
 * Gray-scale, Alpha pixel formats. 2D images, mipmapped 2D images, and cubemaps.
 * 
 */
public final class DdsLoader implements ImageLoader {

    private static final Logger logger = Logger.getLogger(DdsLoader.class.getName());

    public DdsLoader() {}

    public Image load(final InputStream fis, final boolean flip) throws IOException {
        final DDSReader reader = new DDSReader(fis);
        reader.loadHeader();
        final List<ByteBuffer> data = reader.readData(flip);

        return new Image(reader._pixelFormat, reader._width, reader._height, 0, data, reader._sizes);
    }

    /**
     * DDS reader
     */
    public static class DDSReader {
        private static final int DDSD_MANDATORY = 0x1007;
        private static final int DDSD_MIPMAPCOUNT = 0x20000;
        private static final int DDSD_LINEARSIZE = 0x80000;
        private static final int DDSD_DEPTH = 0x800000;

        private static final int DDPF_ALPHAPIXELS = 0x1;
        private static final int DDPF_FOURCC = 0x4;
        private static final int DDPF_RGB = 0x40;
        // used by compressonator to mark grayscale images, red channel mask is used for data and bitcount is 8
        private static final int DDPF_GRAYSCALE = 0x20000;
        // used by compressonator to mark alpha images, alpha channel mask is used for data and bitcount is 8
        private static final int DDPF_ALPHA = 0x2;

        private static final int DDSCAPS_COMPLEX = 0x8; // XXX: currently unused
        private static final int DDSCAPS_TEXTURE = 0x1000;
        private static final int DDSCAPS_MIPMAP = 0x400000;

        private static final int DDSCAPS2_CUBEMAP = 0x200;
        private static final int DDSCAPS2_VOLUME = 0x200000;

        private static final int PF_DXT1 = 0x31545844;
        private static final int PF_DXT3 = 0x33545844;
        private static final int PF_DXT5 = 0x35545844;

        private static final double LOG2 = Math.log(2);

        private int _width;
        private int _height;
        private int _depth; // XXX: currently unused
        private int _flags;
        private int _pitchOrSize;
        private int _mipMapCount;
        private int _caps1;
        private int _caps2;

        private boolean _compressed;
        private boolean _grayscaleOrAlpha;
        private Image.Format _pixelFormat;
        private int _bpp;
        private int[] _sizes;

        private int _redMask, _greenMask, _blueMask, _alphaMask;

        private final DataInput _in;

        public DDSReader(final InputStream in) {
            _in = new LittleEndien(in);
        }

        /**
         * Reads the header (first 128 bytes) of a DDS File
         */
        public void loadHeader() throws IOException {
            if (_in.readInt() != 0x20534444 || _in.readInt() != 124) {
                throw new IOException("Not a DDS file");
            }

            _flags = _in.readInt();

            if (!is(_flags, DDSD_MANDATORY)) {
                throw new IOException("Mandatory flags missing");
            }
            if (is(_flags, DDSD_DEPTH)) {
                throw new IOException("Depth not supported");
            }

            _height = _in.readInt();
            _width = _in.readInt();
            _pitchOrSize = _in.readInt();
            _depth = _in.readInt();
            _mipMapCount = _in.readInt();
            if (44 != _in.skipBytes(44)) {
                throw new IOException("Unexpected number of bytes in file - too few.");
            }
            readPixelFormat();
            _caps1 = _in.readInt();
            _caps2 = _in.readInt();
            if (12 != _in.skipBytes(12)) {
                throw new IOException("Unexpected number of bytes in file - too few.");
            }

            if (!is(_caps1, DDSCAPS_TEXTURE)) {
                throw new IOException("File is not a texture");
            }

            if (is(_caps2, DDSCAPS2_VOLUME)) {
                throw new IOException("Volume textures not supported");
            } else {
                _depth = 0;
            }

            final int expectedMipmaps = 1 + (int) Math.ceil(Math.log(Math.max(_height, _width)) / LOG2);

            if (is(_caps1, DDSCAPS_MIPMAP)) {
                if (!is(_flags, DDSD_MIPMAPCOUNT)) {
                    _mipMapCount = expectedMipmaps;
                } else if (_mipMapCount != expectedMipmaps) {
                    // changed to warning- images often do not have the required amount,
                    // or specify that they have mipmaps but include only the top level..
                    logger.warning("Got " + _mipMapCount + "mipmaps, expected" + expectedMipmaps);
                }
            } else {
                _mipMapCount = 1;
            }

            loadSizes();
        }

        /**
         * Reads the PixelFormat structure in a DDS file
         */
        private void readPixelFormat() throws IOException {
            final int pfSize = _in.readInt();
            if (pfSize != 32) {
                throw new IOException("Pixel format size is " + pfSize + ", not 32");
            }

            final int flags = _in.readInt();

            if (is(flags, DDPF_FOURCC)) {
                _compressed = true;
                final int fourcc = _in.readInt();
                if (20 != _in.skipBytes(20)) {
                    throw new IOException("Unexpected number of bytes in file - too few.");
                }

                switch (fourcc) {
                    case PF_DXT1:
                        _bpp = 4;
                        if (is(flags, DDPF_ALPHAPIXELS)) {
                            _pixelFormat = Image.Format.NativeDXT1A;
                        } else {
                            _pixelFormat = Image.Format.NativeDXT1;
                        }
                        break;
                    case PF_DXT3:
                        _bpp = 8;
                        _pixelFormat = Image.Format.NativeDXT3;
                        break;
                    case PF_DXT5:
                        _bpp = 8;
                        _pixelFormat = Image.Format.NativeDXT5;
                        break;
                    default:
                        throw new IOException("Unknown fourcc: " + string(fourcc));
                }

                final int size = ((_width + 3) / 4) * ((_height + 3) / 4) * _bpp * 2;

                if (is(_flags, DDSD_LINEARSIZE)) {
                    if (_pitchOrSize == 0) {
                        logger.warning("Must use linear size with fourcc");
                        _pitchOrSize = size;
                    } else if (_pitchOrSize != size) {
                        logger.warning("Expected size = " + size + ", real = " + _pitchOrSize);
                    }
                } else {
                    _pitchOrSize = size;
                }
            } else {
                _compressed = false;

                // skip fourCC
                _in.readInt();

                _bpp = _in.readInt();
                _redMask = _in.readInt();
                _greenMask = _in.readInt();
                _blueMask = _in.readInt();
                _alphaMask = _in.readInt();

                if (is(flags, DDPF_RGB)) {
                    if (is(flags, DDPF_ALPHAPIXELS)) {
                        _pixelFormat = Format.RGBA8;
                    } else {
                        _pixelFormat = Format.RGB8;
                    }
                } else if (is(flags, DDPF_GRAYSCALE)) {
                    switch (_bpp) {
                        case 4:
                            _pixelFormat = Format.Luminance4;
                            break;
                        case 8:
                            _pixelFormat = Format.Luminance8;
                            break;
                        case 12:
                            _pixelFormat = Format.Luminance12;
                            break;
                        case 16:
                            _pixelFormat = Format.Luminance16;
                            break;
                        default:
                            throw new IOException("Unsupported Grayscale BPP: " + _bpp);
                    }
                    _grayscaleOrAlpha = true;
                } else if (is(flags, DDPF_ALPHA)) {
                    switch (_bpp) {
                        case 4:
                            _pixelFormat = Format.Alpha4;
                            break;
                        case 8:
                            _pixelFormat = Format.Alpha8;
                            break;
                        case 12:
                            _pixelFormat = Format.Alpha12;
                            break;
                        case 16:
                            _pixelFormat = Format.Alpha16;
                            break;
                        default:
                            throw new IOException("Unsupported Alpha BPP: " + _bpp);
                    }
                    _grayscaleOrAlpha = true;
                } else {
                    throw new IOException("Unknown PixelFormat in DDS file");
                }

                final int size = (_bpp / 8 * _width);

                if (is(_flags, DDSD_LINEARSIZE)) {
                    if (_pitchOrSize == 0) {
                        logger.warning("Linear size said to contain valid value but does not");
                        _pitchOrSize = size;
                    } else if (_pitchOrSize != size) {
                        logger.warning("Expected size = " + size + ", real = " + _pitchOrSize);
                    }
                } else {
                    _pitchOrSize = size;
                }
            }
        }

        /**
         * Computes the sizes of each mipmap level in bytes, and stores it in sizes_[].
         */
        private void loadSizes() {
            int width = _width;
            int height = _height;

            _sizes = new int[_mipMapCount];

            for (int i = 0; i < _mipMapCount; i++) {
                int size;

                if (_compressed) {
                    size = ((width + 3) / 4) * ((height + 3) / 4) * _bpp * 2;
                } else {
                    size = width * height * _bpp / 8;
                }

                _sizes[i] = ((size + 3) / 4) * 4;

                width = Math.max(width / 2, 1);
                height = Math.max(height / 2, 1);
            }
        }

        /**
         * Flips the given image data on the Y axis.
         * 
         * @param data
         *            Data array containing image data (without mipmaps)
         * @param scanlineSize
         *            Size of a single scanline = width * bytesPerPixel
         * @param height
         *            Height of the image in pixels
         * @return The new data flipped by the Y axis
         */
        public byte[] flipData(final byte[] data, final int scanlineSize, final int height) {
            final byte[] newData = new byte[data.length];

            for (int y = 0; y < height; y++) {
                System.arraycopy(data, y * scanlineSize, newData, (height - y - 1) * scanlineSize, scanlineSize);
            }

            return newData;
        }

        /**
         * Reads a grayscale image with mipmaps from the InputStream
         * 
         * @param flip
         *            Flip the loaded image by Y axis
         * @param totalSize
         *            Total size of the image in bytes including the mipmaps
         * @return A ByteBuffer containing the grayscale image data with mips.
         * @throws java.io.IOException
         *             If an error occured while reading from InputStream
         */
        public ByteBuffer readGrayscale2D(final boolean flip, final int totalSize) throws IOException {
            final ByteBuffer buffer = BufferUtils.createByteBuffer(totalSize);

            if (_bpp == 8) {
                logger.finest("Source image format: R8");
            }

            assert _bpp / 8 == Image.getEstimatedByteSize(_pixelFormat);

            int width = _width;
            int height = _height;

            for (int mip = 0; mip < _mipMapCount; mip++) {
                byte[] data = new byte[_sizes[mip]];
                _in.readFully(data);
                if (flip) {
                    data = flipData(data, width * _bpp / 8, height);
                }
                buffer.put(data);

                width = Math.max(width / 2, 1);
                height = Math.max(height / 2, 1);
            }

            return buffer;
        }

        /**
         * Reads an uncompressed RGB or RGBA image.
         * 
         * @param flip
         *            Flip the image on the Y axis
         * @param totalSize
         *            Size of the image in bytes including mipmaps
         * @return ByteBuffer containing image data with mipmaps in the format specified by pixelFormat_
         * @throws java.io.IOException
         *             If an error occured while reading from InputStream
         */
        public ByteBuffer readRGB2D(final boolean flip, final int totalSize) throws IOException {
            final int redCount = count(_redMask), blueCount = count(_blueMask), greenCount = count(_greenMask), alphaCount = count(_alphaMask);

            if (_redMask == 0x00FF0000 && _greenMask == 0x0000FF00 && _blueMask == 0x000000FF) {
                if (_alphaMask == 0xFF000000 && _bpp == 32) {
                    logger.finest("Data source format: BGRA8");
                } else if (_bpp == 24) {
                    logger.finest("Data source format: BGR8");
                }
            }

            final int sourcebytesPP = _bpp / 8;
            final int targetBytesPP = Image.getEstimatedByteSize(_pixelFormat);

            final ByteBuffer dataBuffer = BufferUtils.createByteBuffer(totalSize);

            int width = _width;
            int height = _height;

            int offset = 0;
            for (int mip = 0; mip < _mipMapCount; mip++) {
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        final byte[] b = new byte[sourcebytesPP];
                        _in.readFully(b);

                        final int i = byte2int(b);

                        final byte red = (byte) (((i & _redMask) >> redCount));
                        final byte green = (byte) (((i & _greenMask) >> greenCount));
                        final byte blue = (byte) (((i & _blueMask) >> blueCount));
                        final byte alpha = (byte) (((i & _alphaMask) >> alphaCount));

                        if (flip) {
                            dataBuffer.position(offset + ((height - y - 1) * width + x) * targetBytesPP);
                            // else
                            // dataBuffer.position(offset + (y * width + x) * targetBytesPP);
                        }

                        if (_alphaMask == 0) {
                            dataBuffer.put(red).put(green).put(blue);
                        } else {
                            dataBuffer.put(red).put(green).put(blue).put(alpha);
                        }
                    }
                }

                offset += width * height * targetBytesPP;

                width = Math.max(width / 2, 1);
                height = Math.max(height / 2, 1);
            }

            return dataBuffer;
        }

        /**
         * Reads a DXT compressed image from the InputStream
         * 
         * @param totalSize
         *            Total size of the image in bytes, including mipmaps
         * @return ByteBuffer containing compressed DXT image in the format specified by pixelFormat_
         * @throws java.io.IOException
         *             If an error occured while reading from InputStream
         */
        public ByteBuffer readDXT2D(final int totalSize) throws IOException {
            final byte[] data = new byte[totalSize];
            _in.readFully(data);

            logger.finest("Source image format: DXT");

            final ByteBuffer buffer = BufferUtils.createByteBuffer(totalSize);
            buffer.put(data);
            buffer.rewind();

            return buffer;
        }

        /**
         * Reads the image data from the InputStream in the required format. If the file contains a cubemap image, it is
         * loaded as 6 ByteBuffers (potentially containing mipmaps if they were specified), otherwise a single
         * ByteBuffer is returned for a 2D image.
         * 
         * @param flip
         *            Flip the image data or not. For cubemaps, each of the cubemap faces is flipped individually. If
         *            the image is DXT compressed, no flipping is done.
         * @return An ArrayList containing a single ByteBuffer for a 2D image, or 6 ByteBuffers for a cubemap. The
         *         cubemap ByteBuffer order is PositiveX, NegativeX, PositiveY, NegativeY, PositiveZ, NegativeZ.
         * 
         * @throws java.io.IOException
         *             If an error occured while reading from the stream.
         */
        public List<ByteBuffer> readData(final boolean flip) throws IOException {
            int totalSize = 0;

            for (int i = 0; i < _sizes.length; i++) {
                totalSize += _sizes[i];
            }

            final List<ByteBuffer> allMaps = new ArrayList<ByteBuffer>();
            if (is(_caps2, DDSCAPS2_CUBEMAP)) {
                for (int i = 0; i < 6; i++) {
                    if (_compressed) {
                        allMaps.add(readDXT2D(totalSize));
                    } else if (_grayscaleOrAlpha) {
                        allMaps.add(readGrayscale2D(flip, totalSize));
                    } else {
                        allMaps.add(readRGB2D(flip, totalSize));
                    }
                }
            } else {
                if (_compressed) {
                    allMaps.add(readDXT2D(totalSize));
                } else if (_grayscaleOrAlpha) {
                    allMaps.add(readGrayscale2D(flip, totalSize));
                } else {
                    allMaps.add(readRGB2D(flip, totalSize));
                }
            }

            return allMaps;
        }

        /**
         * Checks if flags contains the specified mask
         */
        private static final boolean is(final int flags, final int mask) {
            return (flags & mask) == mask;
        }

        /**
         * Counts the amount of bits needed to shift till bitmask n is at zero
         * 
         * @param n
         *            Bitmask to test
         */
        private static int count(int n) {
            if (n == 0) {
                return 0;
            }

            int i = 0;
            while ((n & 0x1) == 0) {
                n = n >> 1;
                i++;
                if (i > 32) {
                    throw new RuntimeException(Integer.toHexString(n));
                }
            }

            return i;
        }

        /**
         * Converts a 1 to 4 sized byte array to an integer
         */
        private static int byte2int(final byte[] b) {
            if (b.length == 1) {
                return b[0] & 0xFF;
            } else if (b.length == 2) {
                return (b[0] & 0xFF) | ((b[1] & 0xFF) << 8);
            } else if (b.length == 3) {
                return (b[0] & 0xFF) | ((b[1] & 0xFF) << 8) | ((b[2] & 0xFF) << 16);
            } else if (b.length == 4) {
                return (b[0] & 0xFF) | ((b[1] & 0xFF) << 8) | ((b[2] & 0xFF) << 16) | ((b[3] & 0xFF) << 24);
            } else {
                return 0;
            }
        }

        /**
         * Converts a int representing a FourCC into a String
         */
        private static final String string(final int value) {
            final StringBuffer buf = new StringBuffer();

            buf.append((char) (value & 0xFF));
            buf.append((char) ((value & 0xFF00) >> 8));
            buf.append((char) ((value & 0xFF0000) >> 16));
            buf.append((char) ((value & 0xFF00000) >> 24));

            return buf.toString();
        }
    }

}
