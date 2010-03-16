/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scene.state.lwjgl.util;

import org.lwjgl.opengl.ARBDepthTexture;
import org.lwjgl.opengl.ARBHalfFloatPixel;
import org.lwjgl.opengl.ARBMultitexture;
import org.lwjgl.opengl.ARBShadow;
import org.lwjgl.opengl.ARBTextureCompression;
import org.lwjgl.opengl.ARBTextureEnvCombine;
import org.lwjgl.opengl.ARBTextureEnvDot3;
import org.lwjgl.opengl.ARBTextureFloat;
import org.lwjgl.opengl.EXTTextureCompressionLATC;
import org.lwjgl.opengl.EXTTextureCompressionS3TC;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.ardor3d.image.Image;
import com.ardor3d.image.Image.Format;
import com.ardor3d.image.Texture.ApplyMode;
import com.ardor3d.image.Texture.CombinerFunctionAlpha;
import com.ardor3d.image.Texture.CombinerFunctionRGB;
import com.ardor3d.image.Texture.CombinerOperandAlpha;
import com.ardor3d.image.Texture.CombinerOperandRGB;
import com.ardor3d.image.Texture.CombinerSource;
import com.ardor3d.image.Texture.DepthTextureCompareFunc;
import com.ardor3d.image.Texture.DepthTextureCompareMode;
import com.ardor3d.image.Texture.DepthTextureMode;
import com.ardor3d.image.Texture.MagnificationFilter;
import com.ardor3d.image.Texture.MinificationFilter;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.state.TextureState.CorrectionType;

public abstract class LwjglTextureUtil {
    public static boolean isCompressedType(final Format format) {
        switch (format) {
            case NativeDXT1:
            case NativeDXT1A:
            case NativeDXT3:
            case NativeDXT5:
            case NativeLATC_L:
            case NativeLATC_LA:
                return true;
            default:
                return false;
        }
    }

    public static int getGLInternalFormat(final Format format) {
        switch (format) {
            // first some frequently used formats
            case RGBA8:
                return GL11.GL_RGBA8;
            case RGB8:
                return GL11.GL_RGB8;
            case Alpha8:
                return GL11.GL_ALPHA8;
            case CompressedRGBA:
                return ARBTextureCompression.GL_COMPRESSED_RGBA_ARB;
            case CompressedRGB:
                return ARBTextureCompression.GL_COMPRESSED_RGB_ARB;
            case CompressedLuminance:
                return ARBTextureCompression.GL_COMPRESSED_LUMINANCE_ARB;
            case CompressedLuminanceAlpha:
                return ARBTextureCompression.GL_COMPRESSED_LUMINANCE_ALPHA_ARB;
            case NativeDXT1:
                return EXTTextureCompressionS3TC.GL_COMPRESSED_RGB_S3TC_DXT1_EXT;
            case NativeDXT1A:
                return EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT1_EXT;
            case NativeDXT3:
                return EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT;
            case NativeDXT5:
                return EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT;
            case NativeLATC_L:
                return EXTTextureCompressionLATC.GL_COMPRESSED_LUMINANCE_LATC1_EXT;
            case NativeLATC_LA:
                return EXTTextureCompressionLATC.GL_COMPRESSED_LUMINANCE_ALPHA_LATC2_EXT;

                // The rest...
            case Alpha4:
                return GL11.GL_ALPHA4;
            case Alpha12:
                return GL11.GL_ALPHA12;
            case Alpha16:
                return GL11.GL_ALPHA16;
            case Luminance4:
                return GL11.GL_LUMINANCE4;
            case Luminance8:
                return GL11.GL_LUMINANCE8;
            case Luminance12:
                return GL11.GL_LUMINANCE12;
            case Luminance16:
                return GL11.GL_LUMINANCE16;
            case Intensity4:
                return GL11.GL_INTENSITY4;
            case Intensity8:
                return GL11.GL_INTENSITY8;
            case Intensity12:
                return GL11.GL_INTENSITY12;
            case Intensity16:
                return GL11.GL_INTENSITY16;
            case Luminance4Alpha4:
                return GL11.GL_LUMINANCE4_ALPHA4;
            case Luminance6Alpha2:
                return GL11.GL_LUMINANCE6_ALPHA2;
            case Luminance8Alpha8:
                return GL11.GL_LUMINANCE8_ALPHA8;
            case Luminance12Alpha4:
                return GL11.GL_LUMINANCE12_ALPHA4;
            case Luminance12Alpha12:
                return GL11.GL_LUMINANCE12_ALPHA12;
            case Luminance16Alpha16:
                return GL11.GL_LUMINANCE16_ALPHA16;
            case R3G3B2:
                return GL11.GL_R3_G3_B2;
            case RGB4:
                return GL11.GL_RGB4;
            case RGB5:
                return GL11.GL_RGB5;
            case RGB10:
                return GL11.GL_RGB10;
            case RGB12:
                return GL11.GL_RGB12;
            case RGB16:
                return GL11.GL_RGB16;
            case RGBA2:
                return GL11.GL_RGBA2;
            case RGBA4:
                return GL11.GL_RGBA4;
            case RGB5A1:
                return GL11.GL_RGB5_A1;
            case RGB10A2:
                return GL11.GL_RGB10_A2;
            case RGBA12:
                return GL11.GL_RGBA12;
            case RGBA16:
                return GL11.GL_RGBA16;
            case Depth16:
                return ARBDepthTexture.GL_DEPTH_COMPONENT16_ARB;
            case Depth24:
                return ARBDepthTexture.GL_DEPTH_COMPONENT24_ARB;
            case Depth32:
                return ARBDepthTexture.GL_DEPTH_COMPONENT32_ARB;
            case RGB16F:
                return ARBTextureFloat.GL_RGB16F_ARB;
            case RGB32F:
                return ARBTextureFloat.GL_RGB32F_ARB;
            case RGBA16F:
                return ARBTextureFloat.GL_RGBA16F_ARB;
            case RGBA32F:
                return ARBTextureFloat.GL_RGBA32F_ARB;
            case Alpha16F:
                return ARBTextureFloat.GL_ALPHA16F_ARB;
            case Alpha32F:
                return ARBTextureFloat.GL_ALPHA32F_ARB;
            case Luminance16F:
                return ARBTextureFloat.GL_LUMINANCE16F_ARB;
            case Luminance32F:
                return ARBTextureFloat.GL_LUMINANCE32F_ARB;
            case LuminanceAlpha16F:
                return ARBTextureFloat.GL_LUMINANCE_ALPHA16F_ARB;
            case LuminanceAlpha32F:
                return ARBTextureFloat.GL_LUMINANCE_ALPHA32F_ARB;
            case Intensity16F:
                return ARBTextureFloat.GL_INTENSITY16F_ARB;
            case Intensity32F:
                return ARBTextureFloat.GL_INTENSITY32F_ARB;
        }
        throw new IllegalArgumentException("Incorrect format set: " + format);
    }

    public static int getGLPixelDataType(final Format format) {
        switch (format) {
            case RGBA16F:
            case RGB16F:
            case Alpha16F:
            case Luminance16F:
            case Intensity16F:
            case LuminanceAlpha16F:
                return ARBHalfFloatPixel.GL_HALF_FLOAT_ARB;
            case RGBA32F:
            case RGB32F:
            case Alpha32F:
            case Luminance32F:
            case Intensity32F:
            case LuminanceAlpha32F:
                return GL11.GL_FLOAT;
            case Alpha16:
            case Luminance16:
            case Luminance16Alpha16:
            case Intensity16:
            case RGB16:
            case RGBA16:
            case Depth16:
                return GL11.GL_SHORT;
            default:
                return GL11.GL_UNSIGNED_BYTE;
        }
    }

    public static int getGLPixelFormat(final Format requestedFormat, final Format imageFormat,
            final ContextCapabilities caps) {
        if (requestedFormat != Image.Format.GuessNoCompression && requestedFormat != Image.Format.Guess) {
            // If we specified a precise format, return that one.
            return getGLPixelFormat(requestedFormat);

        } else if (requestedFormat == Image.Format.Guess) {
            if (imageFormat == Image.Format.RGB8) {
                return getGLPixelFormat(Image.Format.CompressedRGB);
            } else if (imageFormat == Image.Format.RGBA8) {
                return getGLPixelFormat(Image.Format.CompressedRGBA);
            } else if (imageFormat == Image.Format.Luminance8) {
                return getGLPixelFormat(Image.Format.CompressedLuminance);
            } else if (imageFormat == Image.Format.Luminance8Alpha8) {
                return getGLPixelFormat(Image.Format.CompressedLuminanceAlpha);
            }
        }

        // requested format was GuessNoCompression, or we don't support compression, or it's a format we don't compress,
        // so just use image format.
        return getGLPixelFormat(imageFormat);
    }

    public static int getGLPixelFormat(final Format format) {
        switch (format) {
            case RGBA2:
            case RGBA4:
            case RGBA8:
            case RGB5A1:
            case RGB10A2:
            case RGBA12:
            case RGBA16:
            case CompressedRGBA:
            case NativeDXT1A:
            case NativeDXT3:
            case NativeDXT5:
            case RGBA16F:
            case RGBA32F:
                return GL11.GL_RGBA;
            case R3G3B2:
            case RGB4:
            case RGB5:
            case RGB8:
            case RGB10:
            case RGB12:
            case RGB16:
            case CompressedRGB:
            case NativeDXT1:
            case RGB16F:
            case RGB32F:
                return GL11.GL_RGB;
            case Alpha4:
            case Alpha8:
            case Alpha12:
            case Alpha16:
            case Alpha16F:
            case Alpha32F:
                return GL11.GL_ALPHA;
            case Luminance4:
            case Luminance8:
            case Luminance12:
            case Luminance16:
            case Luminance16F:
            case Luminance32F:
            case CompressedLuminance:
            case NativeLATC_L:
                return GL11.GL_LUMINANCE;
            case Intensity4:
            case Intensity8:
            case Intensity12:
            case Intensity16:
            case Intensity16F:
            case Intensity32F:
                return GL11.GL_INTENSITY;
            case Luminance4Alpha4:
            case Luminance6Alpha2:
            case Luminance8Alpha8:
            case Luminance12Alpha4:
            case Luminance12Alpha12:
            case Luminance16Alpha16:
            case LuminanceAlpha16F:
            case LuminanceAlpha32F:
            case CompressedLuminanceAlpha:
            case NativeLATC_LA:
                return GL11.GL_LUMINANCE_ALPHA;
            case Depth16:
            case Depth24:
            case Depth32:
                return GL11.GL_DEPTH_COMPONENT;
        }
        throw new IllegalArgumentException("Incorrect format set: " + format);
    }

    public static int getGLDepthTextureMode(final DepthTextureMode mode) {
        switch (mode) {
            case Alpha:
                return GL11.GL_ALPHA;
            case Luminance:
                return GL11.GL_LUMINANCE;
            case Intensity:
            default:
                return GL11.GL_INTENSITY;
        }
    }

    public static int getGLDepthTextureCompareMode(final DepthTextureCompareMode mode) {
        switch (mode) {
            case RtoTexture:
                return ARBShadow.GL_COMPARE_R_TO_TEXTURE_ARB;
            case None:
            default:
                return GL11.GL_NONE;
        }
    }

    public static int getGLDepthTextureCompareFunc(final DepthTextureCompareFunc func) {
        switch (func) {
            case GreaterThanEqual:
                return GL11.GL_GEQUAL;
            case LessThanEqual:
            default:
                return GL11.GL_LEQUAL;
        }
    }

    public static int getGLMagFilter(final MagnificationFilter magFilter) {
        switch (magFilter) {
            case Bilinear:
                return GL11.GL_LINEAR;
            case NearestNeighbor:
            default:
                return GL11.GL_NEAREST;

        }
    }

    public static int getGLMinFilter(final MinificationFilter filter) {
        switch (filter) {
            case BilinearNoMipMaps:
                return GL11.GL_LINEAR;
            case Trilinear:
                return GL11.GL_LINEAR_MIPMAP_LINEAR;
            case BilinearNearestMipMap:
                return GL11.GL_LINEAR_MIPMAP_NEAREST;
            case NearestNeighborNoMipMaps:
                return GL11.GL_NEAREST;
            case NearestNeighborNearestMipMap:
                return GL11.GL_NEAREST_MIPMAP_NEAREST;
            case NearestNeighborLinearMipMap:
                return GL11.GL_NEAREST_MIPMAP_LINEAR;
        }
        throw new IllegalArgumentException("invalid MinificationFilter type: " + filter);
    }

    public static int getGLEnvMode(final ApplyMode apply) {
        switch (apply) {
            case Replace:
                return GL11.GL_REPLACE;
            case Blend:
                return GL11.GL_BLEND;
            case Combine:
                return ARBTextureEnvCombine.GL_COMBINE_ARB;
            case Decal:
                return GL11.GL_DECAL;
            case Add:
                return GL11.GL_ADD;
            case Modulate:
                return GL11.GL_MODULATE;
        }
        throw new IllegalArgumentException("invalid ApplyMode type: " + apply);
    }

    public static int getPerspHint(final CorrectionType type) {
        switch (type) {
            case Perspective:
                return GL11.GL_NICEST;
            case Affine:
                return GL11.GL_FASTEST;
        }
        throw new IllegalArgumentException("unknown correction type: " + type);
    }

    public static int getGLCombineOpRGB(final CombinerOperandRGB operand) {
        switch (operand) {
            case SourceColor:
                return GL11.GL_SRC_COLOR;
            case OneMinusSourceColor:
                return GL11.GL_ONE_MINUS_SRC_COLOR;
            case SourceAlpha:
                return GL11.GL_SRC_ALPHA;
            case OneMinusSourceAlpha:
                return GL11.GL_ONE_MINUS_SRC_ALPHA;
        }
        throw new IllegalArgumentException("invalid CombinerOperandRGB type: " + operand);
    }

    public static int getGLCombineOpAlpha(final CombinerOperandAlpha operand) {
        switch (operand) {
            case SourceAlpha:
                return GL11.GL_SRC_ALPHA;
            case OneMinusSourceAlpha:
                return GL11.GL_ONE_MINUS_SRC_ALPHA;
        }
        throw new IllegalArgumentException("invalid CombinerOperandAlpha type: " + operand);
    }

    public static int getGLCombineSrc(final CombinerSource combineSrc) {
        switch (combineSrc) {
            case CurrentTexture:
                return GL11.GL_TEXTURE;
            case PrimaryColor:
                return ARBTextureEnvCombine.GL_PRIMARY_COLOR_ARB;
            case Constant:
                return ARBTextureEnvCombine.GL_CONSTANT_ARB;
            case Previous:
                return ARBTextureEnvCombine.GL_PREVIOUS_ARB;
            case TextureUnit0:
                return ARBMultitexture.GL_TEXTURE0_ARB;
            case TextureUnit1:
                return ARBMultitexture.GL_TEXTURE1_ARB;
            case TextureUnit2:
                return ARBMultitexture.GL_TEXTURE2_ARB;
            case TextureUnit3:
                return ARBMultitexture.GL_TEXTURE3_ARB;
            case TextureUnit4:
                return ARBMultitexture.GL_TEXTURE4_ARB;
            case TextureUnit5:
                return ARBMultitexture.GL_TEXTURE5_ARB;
            case TextureUnit6:
                return ARBMultitexture.GL_TEXTURE6_ARB;
            case TextureUnit7:
                return ARBMultitexture.GL_TEXTURE7_ARB;
            case TextureUnit8:
                return ARBMultitexture.GL_TEXTURE8_ARB;
            case TextureUnit9:
                return ARBMultitexture.GL_TEXTURE9_ARB;
            case TextureUnit10:
                return ARBMultitexture.GL_TEXTURE10_ARB;
            case TextureUnit11:
                return ARBMultitexture.GL_TEXTURE11_ARB;
            case TextureUnit12:
                return ARBMultitexture.GL_TEXTURE12_ARB;
            case TextureUnit13:
                return ARBMultitexture.GL_TEXTURE13_ARB;
            case TextureUnit14:
                return ARBMultitexture.GL_TEXTURE14_ARB;
            case TextureUnit15:
                return ARBMultitexture.GL_TEXTURE15_ARB;
            case TextureUnit16:
                return ARBMultitexture.GL_TEXTURE16_ARB;
            case TextureUnit17:
                return ARBMultitexture.GL_TEXTURE17_ARB;
            case TextureUnit18:
                return ARBMultitexture.GL_TEXTURE18_ARB;
            case TextureUnit19:
                return ARBMultitexture.GL_TEXTURE19_ARB;
            case TextureUnit20:
                return ARBMultitexture.GL_TEXTURE20_ARB;
            case TextureUnit21:
                return ARBMultitexture.GL_TEXTURE21_ARB;
            case TextureUnit22:
                return ARBMultitexture.GL_TEXTURE22_ARB;
            case TextureUnit23:
                return ARBMultitexture.GL_TEXTURE23_ARB;
            case TextureUnit24:
                return ARBMultitexture.GL_TEXTURE24_ARB;
            case TextureUnit25:
                return ARBMultitexture.GL_TEXTURE25_ARB;
            case TextureUnit26:
                return ARBMultitexture.GL_TEXTURE26_ARB;
            case TextureUnit27:
                return ARBMultitexture.GL_TEXTURE27_ARB;
            case TextureUnit28:
                return ARBMultitexture.GL_TEXTURE28_ARB;
            case TextureUnit29:
                return ARBMultitexture.GL_TEXTURE29_ARB;
            case TextureUnit30:
                return ARBMultitexture.GL_TEXTURE30_ARB;
            case TextureUnit31:
                return ARBMultitexture.GL_TEXTURE31_ARB;
        }
        throw new IllegalArgumentException("invalid CombinerSource type: " + combineSrc);
    }

    public static int getGLCombineFuncAlpha(final CombinerFunctionAlpha combineFunc) {
        switch (combineFunc) {
            case Modulate:
                return GL11.GL_MODULATE;
            case Replace:
                return GL11.GL_REPLACE;
            case Add:
                return GL11.GL_ADD;
            case AddSigned:
                return ARBTextureEnvCombine.GL_ADD_SIGNED_ARB;
            case Subtract:
                return ARBTextureEnvCombine.GL_SUBTRACT_ARB;
            case Interpolate:
                return ARBTextureEnvCombine.GL_INTERPOLATE_ARB;
        }
        throw new IllegalArgumentException("invalid CombinerFunctionAlpha type: " + combineFunc);
    }

    public static int getGLCombineFuncRGB(final CombinerFunctionRGB combineFunc) {
        switch (combineFunc) {
            case Modulate:
                return GL11.GL_MODULATE;
            case Replace:
                return GL11.GL_REPLACE;
            case Add:
                return GL11.GL_ADD;
            case AddSigned:
                return ARBTextureEnvCombine.GL_ADD_SIGNED_ARB;
            case Subtract:
                return ARBTextureEnvCombine.GL_SUBTRACT_ARB;
            case Interpolate:
                return ARBTextureEnvCombine.GL_INTERPOLATE_ARB;
            case Dot3RGB:
                return ARBTextureEnvDot3.GL_DOT3_RGB_ARB;
            case Dot3RGBA:
                return ARBTextureEnvDot3.GL_DOT3_RGBA_ARB;
        }
        throw new IllegalArgumentException("invalid CombinerFunctionRGB type: " + combineFunc);
    }

    public static int bytesPerPixel(final int format, final int type) {
        int n, m;

        switch (format) {
            case GL11.GL_COLOR_INDEX:
            case GL11.GL_STENCIL_INDEX:
            case GL11.GL_DEPTH_COMPONENT:
            case GL11.GL_RED:
            case GL11.GL_GREEN:
            case GL11.GL_BLUE:
            case GL11.GL_ALPHA:
            case GL11.GL_LUMINANCE:
                n = 1;
                break;
            case GL11.GL_LUMINANCE_ALPHA:
                n = 2;
                break;
            case GL11.GL_RGB:
            case GL12.GL_BGR:
                n = 3;
                break;
            case GL11.GL_RGBA:
            case GL12.GL_BGRA:
                n = 4;
                break;
            default:
                n = 0;
        }

        switch (type) {
            case GL11.GL_UNSIGNED_BYTE:
            case GL11.GL_BYTE:
            case GL11.GL_BITMAP:
                m = 1;
                break;
            case GL11.GL_UNSIGNED_SHORT:
            case GL11.GL_SHORT:
            case ARBHalfFloatPixel.GL_HALF_FLOAT_ARB:
                m = 2;
                break;
            case GL11.GL_UNSIGNED_INT:
            case GL11.GL_INT:
            case GL11.GL_FLOAT:
                m = 4;
                break;
            default:
                m = 0;
        }

        return n * m;
    }
}
