/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scene.state.jogl.util;

import javax.media.opengl.GL;

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
import com.ardor3d.renderer.state.TextureState.CorrectionType;

public abstract class JoglTextureUtil {
    public static boolean isCompressedType(final Format format) {
        switch (format) {
            case NativeDXT1:
            case NativeDXT1A:
            case NativeDXT3:
            case NativeDXT5:
                return true;
            default:
                return false;
        }
    }

    public static int getGLDataFormat(final Format format) {
        switch (format) {
            // first some frequently used formats
            case RGBA8:
                return GL.GL_RGBA8;
            case RGB8:
                return GL.GL_RGB8;
            case Alpha8:
                return GL.GL_ALPHA8;
            case RGB_TO_DXT1:
            case NativeDXT1:
                return GL.GL_COMPRESSED_RGB_S3TC_DXT1_EXT;
            case RGBA_TO_DXT1:
            case NativeDXT1A:
                return GL.GL_COMPRESSED_RGBA_S3TC_DXT1_EXT;
            case RGBA_TO_DXT3:
            case NativeDXT3:
                return GL.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT;
            case RGBA_TO_DXT5:
            case NativeDXT5:
                return GL.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT;

                // The rest...
            case Alpha4:
                return GL.GL_ALPHA4;
            case Alpha12:
                return GL.GL_ALPHA12;
            case Alpha16:
                return GL.GL_ALPHA16;
            case Luminance4:
                return GL.GL_LUMINANCE4;
            case Luminance8:
                return GL.GL_LUMINANCE8;
            case Luminance12:
                return GL.GL_LUMINANCE12;
            case Luminance16:
                return GL.GL_LUMINANCE16;
            case Intensity4:
                return GL.GL_INTENSITY4;
            case Intensity8:
                return GL.GL_INTENSITY8;
            case Intensity12:
                return GL.GL_INTENSITY12;
            case Intensity16:
                return GL.GL_INTENSITY16;
            case Luminance4Alpha4:
                return GL.GL_LUMINANCE4_ALPHA4;
            case Luminance6Alpha2:
                return GL.GL_LUMINANCE6_ALPHA2;
            case Luminance8Alpha8:
                return GL.GL_LUMINANCE8_ALPHA8;
            case Luminance12Alpha4:
                return GL.GL_LUMINANCE12_ALPHA4;
            case Luminance12Alpha12:
                return GL.GL_LUMINANCE12_ALPHA12;
            case Luminance16Alpha16:
                return GL.GL_LUMINANCE16_ALPHA16;
            case R3G3B2:
                return GL.GL_R3_G3_B2;
            case RGB4:
                return GL.GL_RGB4;
            case RGB5:
                return GL.GL_RGB5;
            case RGB10:
                return GL.GL_RGB10;
            case RGB12:
                return GL.GL_RGB12;
            case RGB16:
                return GL.GL_RGB16;
            case RGBA2:
                return GL.GL_RGBA2;
            case RGBA4:
                return GL.GL_RGBA4;
            case RGB5A1:
                return GL.GL_RGB5_A1;
            case RGB10A2:
                return GL.GL_RGB10_A2;
            case RGBA12:
                return GL.GL_RGBA12;
            case RGBA16:
                return GL.GL_RGBA16;
            case Depth16:
                return GL.GL_DEPTH_COMPONENT16_ARB;
            case Depth24:
                return GL.GL_DEPTH_COMPONENT24_ARB;
            case Depth32:
                return GL.GL_DEPTH_COMPONENT32_ARB;
            case RGB16F:
                return GL.GL_RGB16F_ARB;
            case RGB32F:
                return GL.GL_RGB32F_ARB;
            case RGBA16F:
                return GL.GL_RGBA16F_ARB;
            case RGBA32F:
                return GL.GL_RGBA32F_ARB;
            case Alpha16F:
                return GL.GL_ALPHA16F_ARB;
            case Alpha32F:
                return GL.GL_ALPHA32F_ARB;
            case Luminance16F:
                return GL.GL_LUMINANCE16F_ARB;
            case Luminance32F:
                return GL.GL_LUMINANCE32F_ARB;
            case LuminanceAlpha16F:
                return GL.GL_LUMINANCE_ALPHA16F_ARB;
            case LuminanceAlpha32F:
                return GL.GL_LUMINANCE_ALPHA32F_ARB;
            case Intensity16F:
                return GL.GL_INTENSITY16F_ARB;
            case Intensity32F:
                return GL.GL_INTENSITY32F_ARB;
        }
        throw new IllegalArgumentException("Incorrect format set: " + format);
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
            case RGBA_TO_DXT1:
            case NativeDXT1A:
            case RGBA_TO_DXT3:
            case NativeDXT3:
            case RGBA_TO_DXT5:
            case NativeDXT5:
            case RGBA16F:
            case RGBA32F:
                return GL.GL_RGBA;
            case R3G3B2:
            case RGB4:
            case RGB5:
            case RGB8:
            case RGB10:
            case RGB12:
            case RGB16:
            case RGB_TO_DXT1:
            case NativeDXT1:
            case RGB16F:
            case RGB32F:
                return GL.GL_RGB;
            case Alpha4:
            case Alpha8:
            case Alpha12:
            case Alpha16:
            case Alpha16F:
            case Alpha32F:
                return GL.GL_ALPHA;
            case Luminance4:
            case Luminance8:
            case Luminance12:
            case Luminance16:
            case Luminance16F:
            case Luminance32F:
                return GL.GL_LUMINANCE;
            case Intensity4:
            case Intensity8:
            case Intensity12:
            case Intensity16:
            case Intensity16F:
            case Intensity32F:
                return GL.GL_INTENSITY;
            case Luminance4Alpha4:
            case Luminance6Alpha2:
            case Luminance8Alpha8:
            case Luminance12Alpha4:
            case Luminance12Alpha12:
            case Luminance16Alpha16:
            case LuminanceAlpha16F:
            case LuminanceAlpha32F:
                return GL.GL_LUMINANCE_ALPHA;
            case Depth16:
            case Depth24:
            case Depth32:
                return GL.GL_DEPTH_COMPONENT;
        }
        throw new IllegalArgumentException("Incorrect format set: " + format);
    }

    public static int getGLDepthTextureMode(final DepthTextureMode mode) {
        switch (mode) {
            case Alpha:
                return GL.GL_ALPHA;
            case Luminance:
                return GL.GL_LUMINANCE;
            case Intensity:
            default:
                return GL.GL_INTENSITY;
        }
    }

    public static int getGLDepthTextureCompareMode(final DepthTextureCompareMode mode) {
        switch (mode) {
            case RtoTexture:
                return GL.GL_COMPARE_R_TO_TEXTURE_ARB;
            case None:
            default:
                return GL.GL_NONE;
        }
    }

    public static int getGLDepthTextureCompareFunc(final DepthTextureCompareFunc func) {
        switch (func) {
            case GreaterThanEqual:
                return GL.GL_GEQUAL;
            case LessThanEqual:
            default:
                return GL.GL_LEQUAL;
        }
    }

    public static int getGLMagFilter(final MagnificationFilter magFilter) {
        switch (magFilter) {
            case Bilinear:
                return GL.GL_LINEAR;
            case NearestNeighbor:
            default:
                return GL.GL_NEAREST;

        }
    }

    public static int getGLMinFilter(final MinificationFilter filter) {
        switch (filter) {
            case BilinearNoMipMaps:
                return GL.GL_LINEAR;
            case Trilinear:
                return GL.GL_LINEAR_MIPMAP_LINEAR;
            case BilinearNearestMipMap:
                return GL.GL_LINEAR_MIPMAP_NEAREST;
            case NearestNeighborNoMipMaps:
                return GL.GL_NEAREST;
            case NearestNeighborNearestMipMap:
                return GL.GL_NEAREST_MIPMAP_NEAREST;
            case NearestNeighborLinearMipMap:
                return GL.GL_NEAREST_MIPMAP_LINEAR;
        }
        throw new IllegalArgumentException("invalid MinificationFilter type: " + filter);
    }

    public static int getGLEnvMode(final ApplyMode apply) {
        switch (apply) {
            case Replace:
                return GL.GL_REPLACE;
            case Blend:
                return GL.GL_BLEND;
            case Combine:
                return GL.GL_COMBINE;
            case Decal:
                return GL.GL_DECAL;
            case Add:
                return GL.GL_ADD;
            case Modulate:
                return GL.GL_MODULATE;
        }
        throw new IllegalArgumentException("invalid ApplyMode type: " + apply);
    }

    public static int getPerspHint(final CorrectionType type) {
        switch (type) {
            case Perspective:
                return GL.GL_NICEST;
            case Affine:
                return GL.GL_FASTEST;
        }
        throw new IllegalArgumentException("unknown correction type: " + type);
    }

    public static int getGLCombineOpRGB(final CombinerOperandRGB operand) {
        switch (operand) {
            case SourceColor:
                return GL.GL_SRC_COLOR;
            case OneMinusSourceColor:
                return GL.GL_ONE_MINUS_SRC_COLOR;
            case SourceAlpha:
                return GL.GL_SRC_ALPHA;
            case OneMinusSourceAlpha:
                return GL.GL_ONE_MINUS_SRC_ALPHA;
        }
        throw new IllegalArgumentException("invalid CombinerOperandRGB type: " + operand);
    }

    public static int getGLCombineOpAlpha(final CombinerOperandAlpha operand) {
        switch (operand) {
            case SourceAlpha:
                return GL.GL_SRC_ALPHA;
            case OneMinusSourceAlpha:
                return GL.GL_ONE_MINUS_SRC_ALPHA;
        }
        throw new IllegalArgumentException("invalid CombinerOperandAlpha type: " + operand);
    }

    public static int getGLCombineSrc(final CombinerSource combineSrc) {
        switch (combineSrc) {
            case CurrentTexture:
                return GL.GL_TEXTURE;
            case PrimaryColor:
                return GL.GL_PRIMARY_COLOR;
            case Constant:
                return GL.GL_CONSTANT;
            case Previous:
                return GL.GL_PREVIOUS;
            case TextureUnit0:
                return GL.GL_TEXTURE0;
            case TextureUnit1:
                return GL.GL_TEXTURE1;
            case TextureUnit2:
                return GL.GL_TEXTURE2;
            case TextureUnit3:
                return GL.GL_TEXTURE3;
            case TextureUnit4:
                return GL.GL_TEXTURE4;
            case TextureUnit5:
                return GL.GL_TEXTURE5;
            case TextureUnit6:
                return GL.GL_TEXTURE6;
            case TextureUnit7:
                return GL.GL_TEXTURE7;
            case TextureUnit8:
                return GL.GL_TEXTURE8;
            case TextureUnit9:
                return GL.GL_TEXTURE9;
            case TextureUnit10:
                return GL.GL_TEXTURE10;
            case TextureUnit11:
                return GL.GL_TEXTURE11;
            case TextureUnit12:
                return GL.GL_TEXTURE12;
            case TextureUnit13:
                return GL.GL_TEXTURE13;
            case TextureUnit14:
                return GL.GL_TEXTURE14;
            case TextureUnit15:
                return GL.GL_TEXTURE15;
            case TextureUnit16:
                return GL.GL_TEXTURE16;
            case TextureUnit17:
                return GL.GL_TEXTURE17;
            case TextureUnit18:
                return GL.GL_TEXTURE18;
            case TextureUnit19:
                return GL.GL_TEXTURE19;
            case TextureUnit20:
                return GL.GL_TEXTURE20;
            case TextureUnit21:
                return GL.GL_TEXTURE21;
            case TextureUnit22:
                return GL.GL_TEXTURE22;
            case TextureUnit23:
                return GL.GL_TEXTURE23;
            case TextureUnit24:
                return GL.GL_TEXTURE24;
            case TextureUnit25:
                return GL.GL_TEXTURE25;
            case TextureUnit26:
                return GL.GL_TEXTURE26;
            case TextureUnit27:
                return GL.GL_TEXTURE27;
            case TextureUnit28:
                return GL.GL_TEXTURE28;
            case TextureUnit29:
                return GL.GL_TEXTURE29;
            case TextureUnit30:
                return GL.GL_TEXTURE30;
            case TextureUnit31:
                return GL.GL_TEXTURE31;
        }
        throw new IllegalArgumentException("invalid CombinerSource type: " + combineSrc);
    }

    public static int getGLCombineFuncAlpha(final CombinerFunctionAlpha combineFunc) {
        switch (combineFunc) {
            case Modulate:
                return GL.GL_MODULATE;
            case Replace:
                return GL.GL_REPLACE;
            case Add:
                return GL.GL_ADD;
            case AddSigned:
                return GL.GL_ADD_SIGNED;
            case Subtract:
                return GL.GL_SUBTRACT;
            case Interpolate:
                return GL.GL_INTERPOLATE;
        }
        throw new IllegalArgumentException("invalid CombinerFunctionAlpha type: " + combineFunc);
    }

    public static int getGLCombineFuncRGB(final CombinerFunctionRGB combineFunc) {
        switch (combineFunc) {
            case Modulate:
                return GL.GL_MODULATE;
            case Replace:
                return GL.GL_REPLACE;
            case Add:
                return GL.GL_ADD;
            case AddSigned:
                return GL.GL_ADD_SIGNED;
            case Subtract:
                return GL.GL_SUBTRACT;
            case Interpolate:
                return GL.GL_INTERPOLATE;
            case Dot3RGB:
                return GL.GL_DOT3_RGB;
            case Dot3RGBA:
                return GL.GL_DOT3_RGBA;
        }
        throw new IllegalArgumentException("invalid CombinerFunctionRGB type: " + combineFunc);
    }
}
