/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.image;

import java.io.IOException;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Matrix4;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.math.type.ReadOnlyMatrix4;
import com.ardor3d.util.TextureKey;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * <code>Texture</code> defines a texture object to be used to display an image on a piece of geometry. The image to be
 * displayed is defined by the <code>Image</code> class. All attributes required for texture mapping are contained
 * within this class. This includes mipmapping if desired, magnificationFilter options, apply options and correction
 * options. Default values are as follows: minificationFilter - NearestNeighborNoMipMaps, magnificationFilter -
 * NearestNeighbor, wrap - EdgeClamp on S,T and R, apply - Modulate, enivoronment - None.
 * 
 * @see com.ardor3d.image.Image
 */
public abstract class Texture implements Savable {
    private static final long serialVersionUID = -3642148179543729674L;

    public static boolean DEFAULT_STORE_TEXTURE = false;

    public enum Type {
        /**
         * One dimensional texture. (basically a line)
         */
        OneDimensional,
        /**
         * Two dimensional texture (default). A rectangle.
         */
        TwoDimensional,
        /**
         * Three dimensional texture. (A cube)
         */
        ThreeDimensional,
        /**
         * A set of 6 TwoDimensional textures arranged as faces of a cube facing inwards.
         */
        CubeMap;
    }

    public enum MinificationFilter {

        /**
         * Nearest neighbor interpolation is the fastest and crudest filtering method - it simply uses the color of the
         * texel closest to the pixel center for the pixel color. While fast, this results in aliasing and shimmering
         * during minification. (GL equivalent: GL_NEAREST)
         */
        NearestNeighborNoMipMaps(false),

        /**
         * In this method the four nearest texels to the pixel center are sampled (at texture level 0), and their colors
         * are combined by weighted averages. Though smoother, without mipmaps it suffers the same aliasing and
         * shimmering problems as nearest NearestNeighborNoMipMaps. (GL equivalent: GL_LINEAR)
         */
        BilinearNoMipMaps(false),

        /**
         * Same as NearestNeighborNoMipMaps except that instead of using samples from texture level 0, the closest
         * mipmap level is chosen based on distance. This reduces the aliasing and shimmering significantly, but does
         * not help with blockiness. (GL equivalent: GL_NEAREST_MIPMAP_NEAREST)
         */
        NearestNeighborNearestMipMap(true),

        /**
         * Same as BilinearNoMipMaps except that instead of using samples from texture level 0, the closest mipmap level
         * is chosen based on distance. By using mipmapping we avoid the aliasing and shimmering problems of
         * BilinearNoMipMaps. (GL equivalent: GL_LINEAR_MIPMAP_NEAREST)
         */
        BilinearNearestMipMap(true),

        /**
         * Similar to NearestNeighborNoMipMaps except that instead of using samples from texture level 0, a sample is
         * chosen from each of the closest (by distance) two mipmap levels. A weighted average of these two samples is
         * returned. (GL equivalent: GL_NEAREST_MIPMAP_LINEAR)
         */
        NearestNeighborLinearMipMap(true),

        /**
         * Trilinear filtering is a remedy to a common artifact seen in mipmapped bilinearly filtered images: an abrupt
         * and very noticeable change in quality at boundaries where the renderer switches from one mipmap level to the
         * next. Trilinear filtering solves this by doing a texture lookup and bilinear filtering on the two closest
         * mipmap levels (one higher and one lower quality), and then linearly interpolating the results. This results
         * in a smooth degradation of texture quality as distance from the viewer increases, rather than a series of
         * sudden drops. Of course, closer than Level 0 there is only one mipmap level available, and the algorithm
         * reverts to bilinear filtering (GL equivalent: GL_LINEAR_MIPMAP_LINEAR)
         */
        Trilinear(true);

        private boolean _usesMipMapLevels;

        private MinificationFilter(final boolean usesMipMapLevels) {
            _usesMipMapLevels = usesMipMapLevels;
        }

        public boolean usesMipMapLevels() {
            return _usesMipMapLevels;
        }
    }

    public enum MagnificationFilter {

        /**
         * Nearest neighbor interpolation is the fastest and crudest filtering mode - it simply uses the color of the
         * texel closest to the pixel center for the pixel color. While fast, this results in texture 'blockiness'
         * during magnification. (GL equivalent: GL_NEAREST)
         */
        NearestNeighbor,

        /**
         * In this mode the four nearest texels to the pixel center are sampled (at the closest mipmap level), and their
         * colors are combined by weighted average according to distance. This removes the 'blockiness' seen during
         * magnification, as there is now a smooth gradient of color change from one texel to the next, instead of an
         * abrupt jump as the pixel center crosses the texel boundary. (GL equivalent: GL_LINEAR)
         */
        Bilinear;

    }

    public enum WrapMode {
        /**
         * Only the fractional portion of the coordinate is considered.
         */
        Repeat,
        /**
         * Only the fractional portion of the coordinate is considered, but if the integer portion is odd, we'll use 1 -
         * the fractional portion. (Introduced around OpenGL1.4) Falls back on Repeat if not supported.
         */
        MirroredRepeat,
        /**
         * coordinate will be clamped to [0,1]
         */
        Clamp,
        /**
         * mirrors and clamps the texture coordinate, where mirroring and clamping a value f computes:
         * <code>mirrorClamp(f) = min(1, max(1/(2*N),
         * abs(f)))</code> where N is the size of the one-, two-, or three-dimensional texture image in the direction of
         * wrapping. (Introduced after OpenGL1.4) Falls back on Clamp if not supported.
         */
        MirrorClamp,
        /**
         * coordinate will be clamped to the range [-1/(2N), 1 + 1/(2N)] where N is the size of the texture in the
         * direction of clamping. Falls back on Clamp if not supported.
         */
        BorderClamp,
        /**
         * Wrap mode MIRROR_CLAMP_TO_BORDER_EXT mirrors and clamps to border the texture coordinate, where mirroring and
         * clamping to border a value f computes:
         * <code>mirrorClampToBorder(f) = min(1+1/(2*N), max(1/(2*N), abs(f)))</code> where N is the size of the one-,
         * two-, or three-dimensional texture image in the direction of wrapping." (Introduced after OpenGL1.4) Falls
         * back on BorderClamp if not supported.
         */
        MirrorBorderClamp,
        /**
         * coordinate will be clamped to the range [1/(2N), 1 - 1/(2N)] where N is the size of the texture in the
         * direction of clamping. Falls back on Clamp if not supported.
         */
        EdgeClamp,
        /**
         * mirrors and clamps to edge the texture coordinate, where mirroring and clamping to edge a value f computes:
         * <code>mirrorClampToEdge(f) = min(1-1/(2*N), max(1/(2*N), abs(f)))</code> where N is the size of the one-,
         * two-, or three-dimensional texture image in the direction of wrapping. (Introduced after OpenGL1.4) Falls
         * back on EdgeClamp if not supported.
         */
        MirrorEdgeClamp;
    }

    public enum WrapAxis {
        /**
         * S wrapping (u or "horizontal" wrap)
         */
        S,
        /**
         * T wrapping (v or "vertical" wrap)
         */
        T,
        /**
         * R wrapping (w or "depth" wrap)
         */
        R;
    }

    public enum ApplyMode {
        /**
         * Apply modifier that replaces the previous pixel color with the texture color.
         */
        Replace,
        /**
         * Apply modifier that replaces the color values of the pixel but makes use of the alpha values.
         */
        Decal,
        /**
         * Apply modifier multiples the color of the pixel with the texture color.
         */
        Modulate,
        /**
         * Apply modifier that interpolates the color of the pixel with a blend color using the texture color, such that
         * the final color value is Cv = (1 - Ct) * Cf + BlendColor * Ct Where Ct is the color of the texture and Cf is
         * the initial pixel color.
         */
        Blend,
        /**
         * Apply modifier combines two textures based on the combine parameters set on this texture.
         */
        Combine,
        /**
         * Apply modifier adds two textures.
         */
        Add;
    }

    public enum EnvironmentalMapMode {
        /**
         * Use texture coordinates as they are. (Do not do texture coordinate generation.)
         */
        None,
        /**
         * TODO: add documentation
         */
        EyeLinear,
        /**
         * TODO: add documentation
         */
        ObjectLinear,
        /**
         * TODO: add documentation
         */
        SphereMap,
        /**
         * TODO: add documentation
         */
        NormalMap,
        /**
         * TODO: add documentation
         */
        ReflectionMap;
    }

    public enum CombinerFunctionRGB {
        /** Arg0 */
        Replace,
        /** Arg0 * Arg1 */
        Modulate,
        /** Arg0 + Arg1 */
        Add,
        /** Arg0 + Arg1 - 0.5 */
        AddSigned,
        /** Arg0 * Arg2 + Arg1 * (1 - Arg2) */
        Interpolate,
        /** Arg0 - Arg1 */
        Subtract,
        /**
         * 4 * ((Arg0r - 0.5) * (Arg1r - 0.5) + (Arg0g - 0.5) * (Arg1g - 0.5) + (Arg0b - 0.5) * (Arg1b - 0.5)) [ result
         * placed in R,G,B ]
         */
        Dot3RGB,
        /**
         * 4 * ((Arg0r - 0.5) * (Arg1r - 0.5) + (Arg0g - 0.5) * (Arg1g - 0.5) + (Arg0b - 0.5) * (Arg1b - 0.5)) [ result
         * placed in R,G,B,A ]
         */
        Dot3RGBA;
    }

    public enum CombinerFunctionAlpha {
        /** Arg0 */
        Replace,
        /** Arg0 * Arg1 */
        Modulate,
        /** Arg0 + Arg1 */
        Add,
        /** Arg0 + Arg1 - 0.5 */
        AddSigned,
        /** Arg0 * Arg2 + Arg1 * (1 - Arg2) */
        Interpolate,
        /** Arg0 - Arg1 */
        Subtract;
    }

    public enum CombinerSource {
        /**
         * The incoming fragment color from the previous texture unit. When used on texture unit 0, this is the same as
         * using PrimaryColor.
         */
        Previous,
        /** The blend color set on this texture. */
        Constant,
        /** The incoming fragment color before any texturing is applied. */
        PrimaryColor,
        /** The current texture unit's bound texture. */
        CurrentTexture,
        /** The texture bound on texture unit 0. */
        TextureUnit0,
        /** The texture bound on texture unit 1. */
        TextureUnit1,
        /** The texture bound on texture unit 2. */
        TextureUnit2,
        /** The texture bound on texture unit 3. */
        TextureUnit3,
        /** The texture bound on texture unit 4. */
        TextureUnit4,
        /** The texture bound on texture unit 5. */
        TextureUnit5,
        /** The texture bound on texture unit 6. */
        TextureUnit6,
        /** The texture bound on texture unit 7. */
        TextureUnit7,
        /** The texture bound on texture unit 8. */
        TextureUnit8,
        /** The texture bound on texture unit 9. */
        TextureUnit9,
        /** The texture bound on texture unit 10. */
        TextureUnit10,
        /** The texture bound on texture unit 11. */
        TextureUnit11,
        /** The texture bound on texture unit 12. */
        TextureUnit12,
        /** The texture bound on texture unit 13. */
        TextureUnit13,
        /** The texture bound on texture unit 14. */
        TextureUnit14,
        /** The texture bound on texture unit 15. */
        TextureUnit15,
        /** The texture bound on texture unit 16. */
        TextureUnit16,
        /** The texture bound on texture unit 17. */
        TextureUnit17,
        /** The texture bound on texture unit 18. */
        TextureUnit18,
        /** The texture bound on texture unit 19. */
        TextureUnit19,
        /** The texture bound on texture unit 20. */
        TextureUnit20,
        /** The texture bound on texture unit 21. */
        TextureUnit21,
        /** The texture bound on texture unit 22. */
        TextureUnit22,
        /** The texture bound on texture unit 23. */
        TextureUnit23,
        /** The texture bound on texture unit 24. */
        TextureUnit24,
        /** The texture bound on texture unit 25. */
        TextureUnit25,
        /** The texture bound on texture unit 26. */
        TextureUnit26,
        /** The texture bound on texture unit 27. */
        TextureUnit27,
        /** The texture bound on texture unit 28. */
        TextureUnit28,
        /** The texture bound on texture unit 29. */
        TextureUnit29,
        /** The texture bound on texture unit 30. */
        TextureUnit30,
        /** The texture bound on texture unit 31. */
        TextureUnit31;
    }

    public enum CombinerOperandRGB {
        SourceColor, OneMinusSourceColor, SourceAlpha, OneMinusSourceAlpha;
    }

    public enum CombinerOperandAlpha {
        SourceAlpha, OneMinusSourceAlpha;
    }

    public enum CombinerScale {
        /** No scale (1.0x) */
        One(1.0f),
        /** 2.0x */
        Two(2.0f),
        /** 4.0x */
        Four(4.0f);

        private float scale;

        private CombinerScale(final float scale) {
            this.scale = scale;
        }

        public float floatValue() {
            return scale;
        }
    }

    /**
     * When doing RenderToTexture operations with this texture, this value indicates what content to render into this
     * texture.
     */
    public enum RenderToTextureType {
        /**
         *Each element is an RGB triple. OpenGL converts it to fixed-point or floating-point and assembles it into an
         * RGBA element by attaching 1 for alpha. Each component is then clamped to the range [0,1].
         */
        RGB,
        /**
         * Each element contains all four components. OpenGL converts it to fixed-point or floating-point. Each
         * component is then clamped to the range [0,1].
         */
        RGBA,
        /**
         * Each element is a single depth component clamped to the range [0, 1]. Each component is then clamped to the
         * range [0,1].
         */
        Depth,
        /**
         * Each element is a luminance/alpha pair. OpenGL converts it to fixed-point or floating point, then assembles
         * it into an RGBA element by replicating the luminance value three times for red, green, and blue. Each
         * component is then clamped to the range [0,1].
         */
        Alpha,
        /**
         * Each element is a single luminance value. OpenGL converts it to fixed-point or floating-point, then assembles
         * it into an RGBA element by replicating the luminance value three times for red, green, and blue and attaching
         * 1 for alpha. Each component is then clamped to the range [0,1].
         */
        Luminance,
        /**
         * Each element is a luminance/alpha pair. OpenGL converts it to fixed-point or floating point, then assembles
         * it into an RGBA element by replicating the luminance value three times for red, green, and blue. Each
         * component is then clamped to the range [0,1].
         */
        LuminanceAlpha,
        /**
         * Each element has both luminance (grayness) and alpha (transparency) information, but the luminance and alpha
         * values at every texel are the same. Each component is then clamped to the range [0,1].
         */
        Intensity, Alpha4, Alpha8, Alpha12, Alpha16, Luminance4, Luminance8, Luminance12, Luminance16, Luminance4Alpha4, Luminance6Alpha2, Luminance8Alpha8, Luminance12Alpha4, Luminance12Alpha12, Luminance16Alpha16, Intensity4, Intensity8, Intensity12, Intensity16, R3_G3_B2, RGB4, RGB5, RGB8, RGB10, RGB12, RGB16, RGBA2, RGBA4, RGB5_A1, RGBA8, RGB10_A2, RGBA12, RGBA16,
        // floats
        RGBA32F, RGB32F, Alpha32F, Intensity32F, Luminance32F, LuminanceAlpha32F, RGBA16F, RGB16F, Alpha16F, Intensity16F, Luminance16F, LuminanceAlpha16F;

    }

    /**
     * The shadowing texture compare mode
     */
    public enum DepthTextureCompareMode {
        /** Perform no shadow based comparsion */
        None,
        /** Perform a comparison between source depth and texture depth */
        RtoTexture,
    }

    /**
     * The shadowing texture compare function
     */
    public enum DepthTextureCompareFunc {
        /** Outputs if the source depth is less than the texture depth */
        LessThanEqual,
        /** Outputs if the source depth is greater than the texture depth */
        GreaterThanEqual
    }

    /**
     * The type of depth texture translation to output
     */
    public enum DepthTextureMode {
        /** Output luminance values based on the depth comparison */
        Luminance,
        /** Output alpha values based on the depth comparison */
        Alpha,
        /** Output intensity values based on the depth comparison */
        Intensity
    }

    // Optional String to point to where this texture is located
    private String _imageLocation = null;

    // texture attributes.
    private Image _image = null;
    private final ColorRGBA _blendColor = new ColorRGBA(ColorRGBA.BLACK_NO_ALPHA);
    private final ColorRGBA _borderColor = new ColorRGBA(ColorRGBA.BLACK_NO_ALPHA);

    private final Matrix4 _texMatrix = new Matrix4();

    private float _anisotropicFilterPercent = 0.0f;

    private transient int _textureId;
    private ApplyMode _apply = ApplyMode.Modulate;
    private MinificationFilter _minificationFilter = MinificationFilter.NearestNeighborNoMipMaps;
    private MagnificationFilter _magnificationFilter = MagnificationFilter.Bilinear;
    private EnvironmentalMapMode _envMapMode = EnvironmentalMapMode.None;
    private RenderToTextureType _rttSource = RenderToTextureType.RGBA;

    private int _memReq = 0;
    private boolean _hasBorder = false;

    // The following will only used if apply is set to ApplyMode.Combine
    private CombinerFunctionRGB _combineFuncRGB = CombinerFunctionRGB.Modulate;
    private CombinerSource _combineSrc0RGB = CombinerSource.CurrentTexture;
    private CombinerSource _combineSrc1RGB = CombinerSource.Previous;
    private CombinerSource _combineSrc2RGB = CombinerSource.Constant;
    private CombinerOperandRGB _combineOp0RGB = CombinerOperandRGB.SourceColor;
    private CombinerOperandRGB _combineOp1RGB = CombinerOperandRGB.SourceColor;
    private CombinerOperandRGB _combineOp2RGB = CombinerOperandRGB.SourceAlpha;
    private CombinerScale _combineScaleRGB = CombinerScale.One;

    private CombinerFunctionAlpha _combineFuncAlpha = CombinerFunctionAlpha.Modulate;
    private CombinerSource _combineSrc0Alpha = CombinerSource.CurrentTexture;
    private CombinerSource _combineSrc1Alpha = CombinerSource.Previous;
    private CombinerSource _combineSrc2Alpha = CombinerSource.Constant;
    private CombinerOperandAlpha _combineOp0Alpha = CombinerOperandAlpha.SourceAlpha;
    private CombinerOperandAlpha _combineOp1Alpha = CombinerOperandAlpha.SourceAlpha;
    private CombinerOperandAlpha _combineOp2Alpha = CombinerOperandAlpha.SourceAlpha;
    private CombinerScale _combineScaleAlpha = CombinerScale.One;

    private TextureKey _key = null;
    private transient boolean _storeTexture = DEFAULT_STORE_TEXTURE;

    private DepthTextureCompareMode _depthCompareMode = DepthTextureCompareMode.None;
    private DepthTextureCompareFunc _depthCompareFunc = DepthTextureCompareFunc.GreaterThanEqual;
    private DepthTextureMode _depthMode = DepthTextureMode.Intensity;

    /**
     * Constructor instantiates a new <code>Texture</code> object with default attributes.
     */
    public Texture() {
        _memReq = 0;
    }

    /**
     * <code>setBlendColor</code> sets a color that is used with CombinerSource.Constant
     * 
     * @param color
     *            the new blend color (the default is {@link ColorRGBA#BLACK_NO_ALPHA})
     */
    public void setBlendColor(final ColorRGBA color) {
        _blendColor.set(color);
    }

    /**
     * <code>setBorderColor</code> sets the color used when texture operations encounter the border of a texture.
     * 
     * @param color
     *            the new border color (the default is {@link ColorRGBA#BLACK_NO_ALPHA})
     */
    public void setBorderColor(final ColorRGBA color) {
        _borderColor.set(color);
    }

    /**
     * @return the MinificationFilterMode of this texture.
     */
    public MinificationFilter getMinificationFilter() {
        return _minificationFilter;
    }

    /**
     * @param minificationFilter
     *            the new MinificationFilterMode for this texture.
     * @throws IllegalArgumentException
     *             if minificationFilter is null
     */
    public void setMinificationFilter(final MinificationFilter minificationFilter) {
        if (minificationFilter == null) {
            throw new IllegalArgumentException("minificationFilter can not be null.");
        }
        _minificationFilter = minificationFilter;
    }

    /**
     * @return the MagnificationFilterMode of this texture.
     */
    public MagnificationFilter getMagnificationFilter() {
        return _magnificationFilter;
    }

    /**
     * @param magnificationFilter
     *            the new MagnificationFilter for this texture.
     * @throws IllegalArgumentException
     *             if magnificationFilter is null
     */
    public void setMagnificationFilter(final MagnificationFilter magnificationFilter) {
        if (magnificationFilter == null) {
            throw new IllegalArgumentException("magnificationFilter can not be null.");
        }
        _magnificationFilter = magnificationFilter;
    }

    /**
     * <code>setApply</code> sets the apply mode for this texture.
     * 
     * @param apply
     *            the apply mode for this texture.
     * @throws IllegalArgumentException
     *             if apply is null
     */
    public void setApply(final ApplyMode apply) {
        if (apply == null) {
            throw new IllegalArgumentException("apply can not be null.");
        }
        _apply = apply;
    }

    /**
     * <code>setImage</code> sets the image object that defines the texture.
     * 
     * @param image
     *            the image that defines the texture.
     */
    public void setImage(final Image image) {
        _image = image;
        updateMemoryReq();
    }

    /**
     * <code>getTextureId</code> returns the texture id of this texture. This id is required to be unique to any other
     * texture objects running in the same JVM. However, no guarantees are made that it will be unique, and as such, the
     * user is responsible for this.
     * 
     * @return the id of the texture.
     */
    public int getTextureId() {
        return _textureId;
    }

    /**
     * <code>setTextureId</code> sets the texture id for this texture. Zero means no id is set.
     * 
     * @param textureId
     *            the texture id of this texture.
     */
    public void setTextureId(final int textureId) {
        _textureId = textureId;
    }

    /**
     * <code>getImage</code> returns the image data that makes up this texture. If no image data has been set, this will
     * return null.
     * 
     * @return the image data that makes up the texture.
     */
    public Image getImage() {
        return _image;
    }

    /**
     * <code>getApply</code> returns the apply mode for the texture.
     * 
     * @return the apply mode of the texture.
     */
    public ApplyMode getApply() {
        return _apply;
    }

    /**
     * <code>getBlendColor</code> returns the color set to be used with CombinerSource.Constant for this texture (as
     * applicable) If null, black is assumed.
     * 
     * @return the blend color.
     */
    public ReadOnlyColorRGBA getBlendColor() {
        return _blendColor;
    }

    /**
     * <code>getBorderColor</code> returns the color to be used for border operations. If null, black is assumed.
     * 
     * @return the border color.
     */
    public ReadOnlyColorRGBA getBorderColor() {
        return _borderColor;
    }

    /**
     * <code>setWrap</code> sets the wrap mode of this texture for a particular axis.
     * 
     * @param axis
     *            the texture axis to define a wrapmode on.
     * @param mode
     *            the wrap mode for the given axis of the texture.
     * @throws IllegalArgumentException
     *             if axis or mode are null or invalid for this type of texture
     */
    public abstract void setWrap(WrapAxis axis, WrapMode mode);

    /**
     * <code>setWrap</code> sets the wrap mode of this texture for all axis.
     * 
     * @param mode
     *            the wrap mode for the given axis of the texture.
     * @throws IllegalArgumentException
     *             if mode is null or invalid for this type of texture
     */
    public abstract void setWrap(WrapMode mode);

    /**
     * <code>getWrap</code> returns the wrap mode for a given coordinate axis on this texture.
     * 
     * @param axis
     *            the axis to return for
     * @return the wrap mode of the texture.
     * @throws IllegalArgumentException
     *             if axis is null or invalid for this type of texture
     */
    public abstract WrapMode getWrap(WrapAxis axis);

    public abstract Type getType();

    /**
     * @return Returns the combineFuncRGB.
     */
    public CombinerFunctionRGB getCombineFuncRGB() {
        return _combineFuncRGB;
    }

    /**
     * @param combineFuncRGB
     *            The combineFuncRGB to set.
     * @throws IllegalArgumentException
     *             if combineFuncRGB is null
     */
    public void setCombineFuncRGB(final CombinerFunctionRGB combineFuncRGB) {
        if (combineFuncRGB == null) {
            throw new IllegalArgumentException("invalid CombinerFunctionRGB: null");
        }
        _combineFuncRGB = combineFuncRGB;
    }

    /**
     * @return Returns the combineOp0Alpha.
     */
    public CombinerOperandAlpha getCombineOp0Alpha() {
        return _combineOp0Alpha;
    }

    /**
     * @param combineOp0Alpha
     *            The combineOp0Alpha to set.
     * @throws IllegalArgumentException
     *             if combineOp0Alpha is null
     */
    public void setCombineOp0Alpha(final CombinerOperandAlpha combineOp0Alpha) {
        if (combineOp0Alpha == null) {
            throw new IllegalArgumentException("invalid CombinerOperandAlpha: null");
        }

        _combineOp0Alpha = combineOp0Alpha;
    }

    /**
     * @return Returns the combineOp0RGB.
     */
    public CombinerOperandRGB getCombineOp0RGB() {
        return _combineOp0RGB;
    }

    /**
     * @param combineOp0RGB
     *            The combineOp0RGB to set.
     * @throws IllegalArgumentException
     *             if combineOp0RGB is null
     */
    public void setCombineOp0RGB(final CombinerOperandRGB combineOp0RGB) {
        if (combineOp0RGB == null) {
            throw new IllegalArgumentException("invalid CombinerOperandRGB: null");
        }
        _combineOp0RGB = combineOp0RGB;
    }

    /**
     * @return Returns the combineOp1Alpha.
     */
    public CombinerOperandAlpha getCombineOp1Alpha() {
        return _combineOp1Alpha;
    }

    /**
     * @param combineOp1Alpha
     *            The combineOp1Alpha to set.
     * @throws IllegalArgumentException
     *             if combineOp1Alpha is null
     */
    public void setCombineOp1Alpha(final CombinerOperandAlpha combineOp1Alpha) {
        if (combineOp1Alpha == null) {
            throw new IllegalArgumentException("invalid CombinerOperandAlpha: null");
        }
        _combineOp1Alpha = combineOp1Alpha;
    }

    /**
     * @return Returns the combineOp1RGB.
     */
    public CombinerOperandRGB getCombineOp1RGB() {
        return _combineOp1RGB;
    }

    /**
     * @param combineOp1RGB
     *            The combineOp1RGB to set.
     * @throws IllegalArgumentException
     *             if combineOp1RGB is null
     */
    public void setCombineOp1RGB(final CombinerOperandRGB combineOp1RGB) {
        if (combineOp1RGB == null) {
            throw new IllegalArgumentException("invalid CombinerOperandRGB: null");
        }
        _combineOp1RGB = combineOp1RGB;
    }

    /**
     * @return Returns the combineOp2Alpha.
     */
    public CombinerOperandAlpha getCombineOp2Alpha() {
        return _combineOp2Alpha;
    }

    /**
     * @param combineOp2Alpha
     *            The combineOp2Alpha to set.
     * @throws IllegalArgumentException
     *             if combineOp2Alpha is null
     */
    public void setCombineOp2Alpha(final CombinerOperandAlpha combineOp2Alpha) {
        if (combineOp2Alpha == null) {
            throw new IllegalArgumentException("invalid CombinerOperandAlpha: null");
        }
        _combineOp2Alpha = combineOp2Alpha;
    }

    /**
     * @return Returns the combineOp2RGB.
     */
    public CombinerOperandRGB getCombineOp2RGB() {
        return _combineOp2RGB;
    }

    /**
     * @param combineOp2RGB
     *            The combineOp2RGB to set.
     * @throws IllegalArgumentException
     *             if combineOp2RGB is null
     */
    public void setCombineOp2RGB(final CombinerOperandRGB combineOp2RGB) {
        if (combineOp2RGB == null) {
            throw new IllegalArgumentException("invalid CombinerOperandRGB: null");
        }
        _combineOp2RGB = combineOp2RGB;
    }

    /**
     * @return Returns the combineScaleAlpha.
     */
    public CombinerScale getCombineScaleAlpha() {
        return _combineScaleAlpha;
    }

    /**
     * @param combineScaleAlpha
     *            The combineScaleAlpha to set.
     * @throws IllegalArgumentException
     *             if combineScaleAlpha is null
     */
    public void setCombineScaleAlpha(final CombinerScale combineScaleAlpha) {
        if (combineScaleAlpha == null) {
            throw new IllegalArgumentException("invalid CombinerScale: null");
        }
        _combineScaleAlpha = combineScaleAlpha;
    }

    /**
     * @return Returns the combineScaleRGB.
     */
    public CombinerScale getCombineScaleRGB() {
        return _combineScaleRGB;
    }

    /**
     * @param combineScaleRGB
     *            The combineScaleRGB to set.
     * @throws IllegalArgumentException
     *             if combineScaleRGB is null
     */
    public void setCombineScaleRGB(final CombinerScale combineScaleRGB) {
        if (combineScaleRGB == null) {
            throw new IllegalArgumentException("invalid CombinerScale: null");
        }
        _combineScaleRGB = combineScaleRGB;
    }

    /**
     * @return Returns the combineSrc0Alpha.
     */
    public CombinerSource getCombineSrc0Alpha() {
        return _combineSrc0Alpha;
    }

    /**
     * @param combineSrc0Alpha
     *            The combineSrc0Alpha to set.
     * @throws IllegalArgumentException
     *             if combineSrc0Alpha is null
     */
    public void setCombineSrc0Alpha(final CombinerSource combineSrc0Alpha) {
        if (combineSrc0Alpha == null) {
            throw new IllegalArgumentException("invalid CombinerSource: null");
        }
        _combineSrc0Alpha = combineSrc0Alpha;
    }

    /**
     * @return Returns the combineSrc0RGB.
     */
    public CombinerSource getCombineSrc0RGB() {
        return _combineSrc0RGB;
    }

    /**
     * @param combineSrc0RGB
     *            The combineSrc0RGB to set.
     * @throws IllegalArgumentException
     *             if combineSrc0RGB is null
     */
    public void setCombineSrc0RGB(final CombinerSource combineSrc0RGB) {
        if (combineSrc0RGB == null) {
            throw new IllegalArgumentException("invalid CombinerSource: null");
        }
        _combineSrc0RGB = combineSrc0RGB;
    }

    /**
     * @return Returns the combineSrc1Alpha.
     */
    public CombinerSource getCombineSrc1Alpha() {
        return _combineSrc1Alpha;
    }

    /**
     * @param combineSrc1Alpha
     *            The combineSrc1Alpha to set.
     * @throws IllegalArgumentException
     *             if combineSrc1Alpha is null
     */
    public void setCombineSrc1Alpha(final CombinerSource combineSrc1Alpha) {
        if (combineSrc1Alpha == null) {
            throw new IllegalArgumentException("invalid CombinerSource: null");
        }
        _combineSrc1Alpha = combineSrc1Alpha;
    }

    /**
     * @return Returns the combineSrc1RGB.
     */
    public CombinerSource getCombineSrc1RGB() {
        return _combineSrc1RGB;
    }

    /**
     * @param combineSrc1RGB
     *            The combineSrc1RGB to set.
     * @throws IllegalArgumentException
     *             if combineSrc1RGB is null
     */
    public void setCombineSrc1RGB(final CombinerSource combineSrc1RGB) {
        if (combineSrc1RGB == null) {
            throw new IllegalArgumentException("invalid CombinerSource: null");
        }
        _combineSrc1RGB = combineSrc1RGB;
    }

    /**
     * @return Returns the combineSrc2Alpha.
     */
    public CombinerSource getCombineSrc2Alpha() {
        return _combineSrc2Alpha;
    }

    /**
     * @param combineSrc2Alpha
     *            The combineSrc2Alpha to set.
     * @throws IllegalArgumentException
     *             if combineSrc2Alpha is null
     */
    public void setCombineSrc2Alpha(final CombinerSource combineSrc2Alpha) {
        if (combineSrc2Alpha == null) {
            throw new IllegalArgumentException("invalid CombinerSource: null");
        }
        _combineSrc2Alpha = combineSrc2Alpha;
    }

    /**
     * @return Returns the combineSrc2RGB.
     */
    public CombinerSource getCombineSrc2RGB() {
        return _combineSrc2RGB;
    }

    /**
     * @param combineSrc2RGB
     *            The combineSrc2RGB to set.
     * @throws IllegalArgumentException
     *             if combineSrc2RGB is null
     */
    public void setCombineSrc2RGB(final CombinerSource combineSrc2RGB) {
        if (combineSrc2RGB == null) {
            throw new IllegalArgumentException("invalid CombinerSource: null");
        }
        _combineSrc2RGB = combineSrc2RGB;
    }

    /**
     * @return Returns the combineFuncAlpha.
     */
    public CombinerFunctionAlpha getCombineFuncAlpha() {
        return _combineFuncAlpha;
    }

    /**
     * @param combineFuncAlpha
     *            The combineFuncAlpha to set.
     * @throws IllegalArgumentException
     *             if combineFuncAlpha is null
     */
    public void setCombineFuncAlpha(final CombinerFunctionAlpha combineFuncAlpha) {
        if (combineFuncAlpha == null) {
            throw new IllegalArgumentException("invalid CombinerFunctionAlpha: null");
        }
        _combineFuncAlpha = combineFuncAlpha;
    }

    /**
     * @param envMapMode
     * @throws IllegalArgumentException
     *             if envMapMode is null
     */
    public void setEnvironmentalMapMode(final EnvironmentalMapMode envMapMode) {
        if (envMapMode == null) {
            throw new IllegalArgumentException("invalid EnvironmentalMapMode: null");
        }
        _envMapMode = envMapMode;
    }

    public EnvironmentalMapMode getEnvironmentalMapMode() {
        return _envMapMode;
    }

    public String getImageLocation() {
        return _imageLocation;
    }

    public void setImageLocation(final String imageLocation) {
        _imageLocation = imageLocation;
    }

    /**
     * @return the anisotropic filtering level for this texture as a percentage (0.0 - 1.0)
     */
    public float getAnisotropicFilterPercent() {
        return _anisotropicFilterPercent;
    }

    /**
     * @param percent
     *            the anisotropic filtering level for this texture as a percentage (0.0 - 1.0)
     */
    public void setAnisotropicFilterPercent(float percent) {
        if (percent > 1.0f) {
            percent = 1.0f;
        } else if (percent < 0.0f) {
            percent = 0.0f;
        }
        _anisotropicFilterPercent = percent;
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof Texture)) {
            return false;
        }

        final Texture that = (Texture) other;
        if (_textureId != that._textureId) {
            return false;
        }
        if (_textureId == 0) {
            if (getImage() != null && !getImage().equals(that.getImage())) {
                return false;
            }
            if (getImage() == null && that.getImage() != null) {
                return false;
            }
            if (getAnisotropicFilterPercent() != that.getAnisotropicFilterPercent()) {
                return false;
            }
            if (getApply() != that.getApply()) {
                return false;
            }
            if (getCombineFuncAlpha() != that.getCombineFuncAlpha()) {
                return false;
            }
            if (getCombineFuncRGB() != that.getCombineFuncRGB()) {
                return false;
            }
            if (getCombineOp0Alpha() != that.getCombineOp0Alpha()) {
                return false;
            }
            if (getCombineOp1RGB() != that.getCombineOp1RGB()) {
                return false;
            }
            if (getCombineOp2Alpha() != that.getCombineOp2Alpha()) {
                return false;
            }
            if (getCombineOp2RGB() != that.getCombineOp2RGB()) {
                return false;
            }
            if (getCombineScaleAlpha() != that.getCombineScaleAlpha()) {
                return false;
            }
            if (getCombineScaleRGB() != that.getCombineScaleRGB()) {
                return false;
            }
            if (getCombineSrc0Alpha() != that.getCombineSrc0Alpha()) {
                return false;
            }
            if (getCombineSrc0RGB() != that.getCombineSrc0RGB()) {
                return false;
            }
            if (getCombineSrc1Alpha() != that.getCombineSrc1Alpha()) {
                return false;
            }
            if (getCombineSrc1RGB() != that.getCombineSrc1RGB()) {
                return false;
            }
            if (getCombineSrc2Alpha() != that.getCombineSrc2Alpha()) {
                return false;
            }
            if (getCombineSrc2RGB() != that.getCombineSrc2RGB()) {
                return false;
            }
            if (getEnvironmentalMapMode() != that.getEnvironmentalMapMode()) {
                return false;
            }
            if (getMagnificationFilter() != that.getMagnificationFilter()) {
                return false;
            }
            if (getMinificationFilter() != that.getMinificationFilter()) {
                return false;
            }
            if (!_blendColor.equals(that._blendColor)) {
                return false;
            }
            if (!_borderColor.equals(that._borderColor)) {
                return false;
            }
        }
        return true;
    }

    public abstract Texture createSimpleClone();

    /**
     * Retrieve a basic clone of this Texture (ie, clone everything but the image data, which is shared)
     * 
     * @return Texture
     */
    public Texture createSimpleClone(final Texture rVal) {
        rVal.setApply(_apply);
        rVal.setCombineFuncAlpha(_combineFuncAlpha);
        rVal.setCombineFuncRGB(_combineFuncRGB);
        rVal.setCombineOp0Alpha(_combineOp0Alpha);
        rVal.setCombineOp0RGB(_combineOp0RGB);
        rVal.setCombineOp1Alpha(_combineOp1Alpha);
        rVal.setCombineOp1RGB(_combineOp1RGB);
        rVal.setCombineOp2Alpha(_combineOp2Alpha);
        rVal.setCombineOp2RGB(_combineOp2RGB);
        rVal.setCombineScaleAlpha(_combineScaleAlpha);
        rVal.setCombineScaleRGB(_combineScaleRGB);
        rVal.setCombineSrc0Alpha(_combineSrc0Alpha);
        rVal.setCombineSrc0RGB(_combineSrc0RGB);
        rVal.setCombineSrc1Alpha(_combineSrc1Alpha);
        rVal.setCombineSrc1RGB(_combineSrc1RGB);
        rVal.setCombineSrc2Alpha(_combineSrc2Alpha);
        rVal.setCombineSrc2RGB(_combineSrc2RGB);
        rVal.setEnvironmentalMapMode(_envMapMode);
        rVal.setMinificationFilter(_minificationFilter);
        rVal.setMagnificationFilter(_magnificationFilter);
        rVal.setHasBorder(_hasBorder);
        rVal.setAnisotropicFilterPercent(_anisotropicFilterPercent);
        rVal.setImage(_image); // NOT CLONED.
        rVal._memReq = _memReq;
        rVal.setImageLocation(_imageLocation);
        rVal.setTextureId(_textureId);
        rVal.setBlendColor(_blendColor != null ? _blendColor.clone() : null);
        rVal.setTextureMatrix(_texMatrix);
        if (getTextureKey() != null) {
            rVal.setTextureKey(getTextureKey());
        }
        return rVal;
    }

    public ReadOnlyMatrix4 getTextureMatrix() {
        return _texMatrix;
    }

    public void setTextureMatrix(final ReadOnlyMatrix4 matrix) {
        _texMatrix.set(matrix);
    }

    /**
     * @return Returns the rttSource.
     */
    public RenderToTextureType getRTTSource() {
        return _rttSource;
    }

    /**
     * @param rttSource
     *            The rttSource to set.
     * @throws IllegalArgumentException
     *             if rttSource is null
     */
    public void setRenderToTextureType(final RenderToTextureType rttSource) {
        if (rttSource == null) {
            throw new IllegalArgumentException("invalid RenderToTextureType: null");
        }
        _rttSource = rttSource;
    }

    /**
     * @return the estimated footprint of this texture in bytes
     */
    public int getMemoryReq() {
        return _memReq;
    }

    public void updateMemoryReq() {
        if (_image != null) {
            final int width = _image.getWidth(), height = _image.getHeight();
            _memReq = width * height;
            final int bpp = Image.getEstimatedByteSize(_image.getFormat());
            _memReq *= bpp;
            if (getMinificationFilter().usesMipMapLevels() || _image.hasMipmaps()) {
                if (MathUtils.isPowerOfTwo(_image.getWidth()) && MathUtils.isPowerOfTwo(_image.getHeight())) {
                    _memReq *= 1.33333f;
                } else {
                    _memReq *= 2.0f; // XXX: Is this right?
                }
            }
        }
    }

    public void write(final Ardor3DExporter e) throws IOException {

        final OutputCapsule capsule = e.getCapsule(this);
        capsule.write(_imageLocation, "imageLocation", null);
        if (_storeTexture) {
            capsule.write(_image, "image", null);
        }
        capsule.write(_blendColor, "blendColor", new ColorRGBA(ColorRGBA.BLACK_NO_ALPHA));
        capsule.write(_borderColor, "borderColor", new ColorRGBA(ColorRGBA.BLACK_NO_ALPHA));
        capsule.write(_texMatrix, "texMatrix", new Matrix4(Matrix4.IDENTITY));
        capsule.write(_hasBorder, "hasBorder", false);
        capsule.write(_anisotropicFilterPercent, "anisotropicFilterPercent", 0.0f);
        capsule.write(_minificationFilter, "minificationFilter", MinificationFilter.NearestNeighborNoMipMaps);
        capsule.write(_magnificationFilter, "magnificationFilter", MagnificationFilter.Bilinear);
        capsule.write(_apply, "apply", ApplyMode.Modulate);
        capsule.write(_envMapMode, "envMapMode", EnvironmentalMapMode.None);
        capsule.write(_rttSource, "rttSource", RenderToTextureType.RGBA);
        capsule.write(_memReq, "memReq", 0);
        capsule.write(_combineFuncRGB, "combineFuncRGB", CombinerFunctionRGB.Replace);
        capsule.write(_combineFuncAlpha, "combineFuncAlpha", CombinerFunctionAlpha.Replace);
        capsule.write(_combineSrc0RGB, "combineSrc0RGB", CombinerSource.CurrentTexture);
        capsule.write(_combineSrc1RGB, "combineSrc1RGB", CombinerSource.Previous);
        capsule.write(_combineSrc2RGB, "combineSrc2RGB", CombinerSource.Constant);
        capsule.write(_combineSrc0Alpha, "combineSrc0Alpha", CombinerSource.CurrentTexture);
        capsule.write(_combineSrc1Alpha, "combineSrc1Alpha", CombinerSource.Previous);
        capsule.write(_combineSrc2Alpha, "combineSrc2Alpha", CombinerSource.Constant);
        capsule.write(_combineOp0RGB, "combineOp0RGB", CombinerOperandRGB.SourceColor);
        capsule.write(_combineOp1RGB, "combineOp1RGB", CombinerOperandRGB.SourceColor);
        capsule.write(_combineOp2RGB, "combineOp2RGB", CombinerOperandRGB.SourceAlpha);
        capsule.write(_combineOp0Alpha, "combineOp0Alpha", CombinerOperandAlpha.SourceAlpha);
        capsule.write(_combineOp1Alpha, "combineOp1Alpha", CombinerOperandAlpha.SourceAlpha);
        capsule.write(_combineOp2Alpha, "combineOp2Alpha", CombinerOperandAlpha.SourceAlpha);
        capsule.write(_combineScaleRGB, "combineScaleRGB", CombinerScale.One);
        capsule.write(_combineScaleAlpha, "combineScaleAlpha", CombinerScale.One);
        if (!_storeTexture) {
            capsule.write(_key, "textureKey", null);
        }
    }

    public void read(final Ardor3DImporter e) throws IOException {
        final InputCapsule capsule = e.getCapsule(this);
        _imageLocation = capsule.readString("imageLocation", null);
        _minificationFilter = capsule.readEnum("minificationFilter", MinificationFilter.class,
                MinificationFilter.NearestNeighborNoMipMaps);
        _image = (Image) capsule.readSavable("image", null);
        if (_image == null) {
            _key = (TextureKey) capsule.readSavable("textureKey", null);
            if (_key != null && _key.getLocation() != null) {
                _key.setMinificationFilter(_minificationFilter);
                TextureManager.loadFromKey(_key, null, this);
            }
        }
        _blendColor.set((ColorRGBA) capsule.readSavable("blendColor", new ColorRGBA(ColorRGBA.BLACK_NO_ALPHA)));
        _borderColor.set((ColorRGBA) capsule.readSavable("borderColor", new ColorRGBA(ColorRGBA.BLACK_NO_ALPHA)));
        _texMatrix.set((Matrix4) capsule.readSavable("texMatrix", new Matrix4(Matrix4.IDENTITY)));
        _hasBorder = capsule.readBoolean("hasBorder", false);
        _anisotropicFilterPercent = capsule.readFloat("anisotropicFilterPercent", 0.0f);
        _magnificationFilter = capsule.readEnum("magnificationFilter", MagnificationFilter.class,
                MagnificationFilter.Bilinear);
        _apply = capsule.readEnum("apply", ApplyMode.class, ApplyMode.Modulate);
        _envMapMode = capsule.readEnum("envMapMode", EnvironmentalMapMode.class, EnvironmentalMapMode.None);
        _rttSource = capsule.readEnum("rttSource", RenderToTextureType.class, RenderToTextureType.RGBA);
        _memReq = capsule.readInt("memReq", 0);
        _combineFuncRGB = capsule.readEnum("combineFuncRGB", CombinerFunctionRGB.class, CombinerFunctionRGB.Replace);
        _combineFuncAlpha = capsule.readEnum("combineFuncAlpha", CombinerFunctionAlpha.class,
                CombinerFunctionAlpha.Replace);
        _combineSrc0RGB = capsule.readEnum("combineSrc0RGB", CombinerSource.class, CombinerSource.CurrentTexture);
        _combineSrc1RGB = capsule.readEnum("combineSrc1RGB", CombinerSource.class, CombinerSource.Previous);
        _combineSrc2RGB = capsule.readEnum("combineSrc2RGB", CombinerSource.class, CombinerSource.Constant);
        _combineSrc0Alpha = capsule.readEnum("combineSrc0Alpha", CombinerSource.class, CombinerSource.CurrentTexture);
        _combineSrc1Alpha = capsule.readEnum("combineSrc1Alpha", CombinerSource.class, CombinerSource.Previous);
        _combineSrc2Alpha = capsule.readEnum("combineSrc2Alpha", CombinerSource.class, CombinerSource.Constant);
        _combineOp0RGB = capsule.readEnum("combineOp0RGB", CombinerOperandRGB.class, CombinerOperandRGB.SourceColor);
        _combineOp1RGB = capsule.readEnum("combineOp1RGB", CombinerOperandRGB.class, CombinerOperandRGB.SourceColor);
        _combineOp2RGB = capsule.readEnum("combineOp2RGB", CombinerOperandRGB.class, CombinerOperandRGB.SourceAlpha);
        _combineOp0Alpha = capsule.readEnum("combineOp0Alpha", CombinerOperandAlpha.class,
                CombinerOperandAlpha.SourceAlpha);
        _combineOp1Alpha = capsule.readEnum("combineOp1Alpha", CombinerOperandAlpha.class,
                CombinerOperandAlpha.SourceAlpha);
        _combineOp2Alpha = capsule.readEnum("combineOp2Alpha", CombinerOperandAlpha.class,
                CombinerOperandAlpha.SourceAlpha);
        _combineScaleRGB = capsule.readEnum("combineScaleRGB", CombinerScale.class, CombinerScale.One);
        _combineScaleAlpha = capsule.readEnum("combineScaleAlpha", CombinerScale.class, CombinerScale.One);
    }

    public Class<? extends Texture> getClassTag() {
        return this.getClass();
    }

    public void setTextureKey(final TextureKey tkey) {
        _key = tkey;
    }

    public TextureKey getTextureKey() {
        return _key;
    }

    public boolean isStoreTexture() {
        return _storeTexture;
    }

    public void setStoreTexture(final boolean storeTexture) {
        _storeTexture = storeTexture;
    }

    public boolean hasBorder() {
        return _hasBorder;
    }

    public void setHasBorder(final boolean hasBorder) {
        _hasBorder = hasBorder;
    }

    /**
     * Get the depth texture compare function
     * 
     * @return The depth texture compare function
     */
    public DepthTextureCompareFunc getDepthCompareFunc() {
        return _depthCompareFunc;
    }

    /**
     * Set the depth texture compare function
     * 
     * param depthCompareFunc The depth texture compare function
     */
    public void setDepthCompareFunc(final DepthTextureCompareFunc depthCompareFunc) {
        _depthCompareFunc = depthCompareFunc;
    }

    /**
     * Get the depth texture apply mode
     * 
     * @return The depth texture apply mode
     */
    public DepthTextureMode getDepthMode() {
        return _depthMode;
    }

    /**
     * Set the depth texture apply mode
     * 
     * param depthMode The depth texture apply mode
     */
    public void setDepthMode(final DepthTextureMode depthMode) {
        _depthMode = depthMode;
    }

    /**
     * Get the depth texture compare mode
     * 
     * @return The depth texture compare mode
     */
    public DepthTextureCompareMode getDepthCompareMode() {
        return _depthCompareMode;
    }

    /**
     * Set the depth texture compare mode
     * 
     * @param depthCompareMode
     *            The depth texture compare mode
     */
    public void setDepthCompareMode(final DepthTextureCompareMode depthCompareMode) {
        _depthCompareMode = depthCompareMode;
    }
}
