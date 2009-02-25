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

public class ContextCapabilities {

    protected boolean _supportsVBO = false;
    protected boolean _supportsGL1_2 = false;
    protected boolean _supportsMultisample = false;

    protected boolean _supportsConstantColor = false;
    protected boolean _supportsEq = false;
    protected boolean _supportsSeparateEq = false;
    protected boolean _supportsSeparateFunc = false;
    protected boolean _supportsMinMax = false;
    protected boolean _supportsSubtract = false;

    protected boolean _supportsFogCoords = false;

    protected boolean _supportsTextureLodBias = false;
    protected float _maxTextureLodBias = 0f;

    protected boolean _supportsFragmentProgram = false;
    protected boolean _supportsVertexProgram = false;

    protected boolean _glslSupported = false;
    protected int _maxGLSLVertexAttribs;

    protected boolean _pbufferSupported = false;

    protected boolean _fboSupported = false;
    protected int _maxFBOColorAttachments = 1;

    protected boolean _twoSidedStencilSupport = false;
    protected boolean _stencilWrapSupport = false;

    /** The total number of available auxiliary draw buffers. */
    protected int _numAuxDrawBuffers = -1;

    /** The total number of supported texture units. */
    protected int _numTotalTexUnits = -1;

    /** The number of texture units availible for fixed functionality */
    protected int _numFixedTexUnits = -1;

    /** The number of texture units availible to vertex shader */
    protected int _numVertexTexUnits = -1;

    /** The number of texture units availible to fragment shader */
    protected int _numFragmentTexUnits = -1;

    /** The number of texture coordinate sets available */
    protected int _numFragmentTexCoordUnits = -1;

    /** The max side of a texture supported. */
    protected int _maxTextureSize = -1;

    protected float _maxAnisotropic = -1.0f;

    /** True if multitexturing is supported. */
    protected boolean _supportsMultiTexture = false;

    /** True if combine dot3 is supported. */
    protected boolean _supportsEnvDot3 = false;

    /** True if combine dot3 is supported. */
    protected boolean _supportsEnvCombine = false;

    /** True if anisofiltering is supported. */
    protected boolean _supportsAniso = false;

    /** True if non pow 2 texture sizes are supported. */
    protected boolean _supportsNonPowerTwo = false;

    /** True if rectangular textures are supported (vs. only square textures) */
    protected boolean _supportsRectangular = false;

    /** True if S3TC compression is supported. */
    protected boolean _supportsS3TCCompression = false;

    /** True if Texture3D is supported. */
    protected boolean _supportsTexture3D = false;

    /** True if TextureCubeMap is supported. */
    protected boolean _supportsTextureCubeMap = false;

    /** True if non-GLU mipmap generation (part of FBO) is supported. */
    protected boolean _automaticMipMaps = false;

    /** True if depth textures are supported */
    protected boolean _supportsDepthTexture = false;

    /** True if shadow mapping supported */
    protected boolean _supportsShadow = false;

    protected boolean _supportsMirroredRepeat;
    protected boolean _supportsMirrorClamp;
    protected boolean _supportsMirrorBorderClamp;
    protected boolean _supportsMirrorEdgeClamp;
    protected boolean _supportsBorderClamp;
    protected boolean _supportsEdgeClamp;

    /**
     * @return true if we support Vertex Buffer Objects.
     */
    public boolean isVBOSupported() {
        return _supportsVBO;
    }

    /**
     * @return true if we support all of OpenGL 1.2
     */
    public boolean isOpenGL1_2Supported() {
        return _supportsGL1_2;
    }

    /**
     * @return true if we support multisampling (antialiasing)
     */
    public boolean isMultisampleSupported() {
        return _supportsMultisample;
    }

    /**
     * @return true if we support setting a constant color for use with *Constant* type BlendFunctions.
     */
    public boolean isConstantBlendColorSupported() {
        return _supportsConstantColor;
    }

    /**
     * @return true if we support setting rgb and alpha functions separately for source and destination.
     */
    public boolean isSeparateBlendFunctionsSupported() {
        return _supportsSeparateFunc;
    }

    /**
     * @return true if we support setting the blend equation
     */
    public boolean isBlendEquationSupported() {
        return _supportsEq;
    }

    /**
     * @return true if we support setting the blend equation for alpha and rgb separately
     */
    public boolean isSeparateBlendEquationsSupported() {
        return _supportsSeparateEq;
    }

    /**
     * @return true if we support using min and max blend equations
     */
    public boolean isMinMaxBlendEquationsSupported() {
        return _supportsMinMax;
    }

    /**
     * @return true if we support using subtract blend equations
     */
    public boolean isSubtractBlendEquationsSupported() {
        return _supportsSubtract;
    }

    /**
     * @return true if mesh based fog coords are supported
     */
    public boolean isFogCoordinatesSupported() {
        return _supportsFogCoords;
    }

    /**
     * @return true if texture lod bias is supported
     */
    public boolean isTextureLodBiasSupported() {
        return _supportsTextureLodBias;
    }

    /**
     * @return the max amount of texture lod bias that this context supports.
     */
    public float getMaxLodBias() {
        return _maxTextureLodBias;
    }

    /**
     * @return true if the ARB_shader_objects extension is supported by current graphics configuration.
     */
    public boolean isGLSLSupported() {
        return _glslSupported;
    }

    /**
     * @return true if the ARB_shader_objects extension is supported by current graphics configuration.
     */
    public boolean isPbufferSupported() {
        return _pbufferSupported;
    }

    /**
     * @return true if the EXT_framebuffer_object extension is supported by current graphics configuration.
     */
    public boolean isFBOSupported() {
        return _fboSupported;
    }

    /**
     * @return true if we can handle doing separate stencil operations for front and back facing polys in a single pass.
     */
    public boolean isTwoSidedStencilSupported() {
        return _twoSidedStencilSupport;
    }

    /**
     * @return true if we can handle wrapping increment/decrement operations.
     */
    public boolean isStencilWrapSupported() {
        return _stencilWrapSupport;
    }

    /**
     * <code>getNumberOfAuxiliaryDrawBuffers</code> returns the total number of available auxiliary draw buffers this
     * context supports.
     * 
     * @return the number of available auxiliary draw buffers supported by the context.
     */
    public int getNumberOfAuxiliaryDrawBuffers() {
        return _numAuxDrawBuffers;
    }

    /**
     * <code>getTotalNumberOfUnits</code> returns the total number of texture units this context supports.
     * 
     * @return the total number of texture units supported by the context.
     */
    public int getTotalNumberOfUnits() {
        return _numTotalTexUnits;
    }

    /**
     * <code>getNumberOfFixedUnits</code> returns the number of texture units this context supports, for use in the
     * fixed pipeline.
     * 
     * @return the number units.
     */
    public int getNumberOfFixedTextureUnits() {
        return _numFixedTexUnits;
    }

    /**
     * <code>getNumberOfVertexUnits</code> returns the number of texture units available to a vertex shader that this
     * context supports.
     * 
     * @return the number of units.
     */
    public int getNumberOfVertexUnits() {
        return _numVertexTexUnits;
    }

    /**
     * <code>getNumberOfFragmentUnits</code> returns the number of texture units available to a fragment shader that
     * this context supports.
     * 
     * @return the number of units.
     */
    public int getNumberOfFragmentTextureUnits() {
        return _numFragmentTexUnits;
    }

    /**
     * <code>getNumberOfFragmentTexCoordUnits</code> returns the number of texture coordinate sets available that this
     * context supports.
     * 
     * @return the number of units.
     */
    public int getNumberOfFragmentTexCoordUnits() {
        return _numFragmentTexCoordUnits;
    }

    /**
     * @return the max size of texture (in terms of # pixels wide) that this context supports.
     */
    public int getMaxTextureSize() {
        return _maxTextureSize;
    }

    /**
     * <code>getNumberOfTotalUnits</code> returns the number of texture units this context supports.
     * 
     * @return the number of units.
     */
    public int getNumberOfTotalTextureUnits() {
        return _numTotalTexUnits;
    }

    /**
     * <code>getMaxFBOColorAttachments</code> returns the MAX_COLOR_ATTACHMENTS for FBOs that this context supports.
     * 
     * @return the number of buffers.
     */
    public int getMaxFBOColorAttachments() {
        return _maxFBOColorAttachments;
    }

    /**
     * Returns the maximum anisotropic filter.
     * 
     * @return The maximum anisotropic filter.
     */
    public float getMaxAnisotropic() {
        return _maxAnisotropic;
    }

    /**
     * @return true if multi-texturing is supported in fixed function
     */
    public boolean isMultitextureSupported() {
        return _supportsMultiTexture;
    }

    /**
     * @return true we support dot3 environment texture settings
     */
    public boolean isEnvDot3TextureCombineSupported() {
        return _supportsEnvDot3;
    }

    /**
     * @return true we support combine environment texture settings
     */
    public boolean isEnvCombineSupported() {
        return _supportsEnvCombine;
    }

    /**
     * Returns if S3TC compression is available for textures.
     * 
     * @return true if S3TC is available.
     */
    public boolean isS3TCSupported() {
        return _supportsS3TCCompression;
    }

    /**
     * Returns if Texture3D is available for textures.
     * 
     * @return true if Texture3D is available.
     */
    public boolean isTexture3DSupported() {
        return _supportsTexture3D;
    }

    /**
     * Returns if TextureCubeMap is available for textures.
     * 
     * @return true if TextureCubeMap is available.
     */
    public boolean isTextureCubeMapSupported() {
        return _supportsTextureCubeMap;
    }

    /**
     * Returns if AutomaticMipmap generation is available for textures.
     * 
     * @return true if AutomaticMipmap generation is available.
     */
    public boolean isAutomaticMipmapsSupported() {
        return _automaticMipMaps;
    }

    /**
     * @return if Anisotropic texture filtering is supported
     */
    public boolean isAnisoSupported() {
        return _supportsAniso;
    }

    /**
     * @return true if non pow 2 texture sizes are supported
     */
    public boolean isNonPowerOfTwoTextureSupported() {
        return _supportsNonPowerTwo;
    }

    /**
     * @return if rectangular texture sizes are supported (width != height)
     */
    public boolean isRectangularTextureSupported() {
        return _supportsRectangular;
    }

    public boolean isFragmentProgramSupported() {
        return _supportsFragmentProgram;
    }

    public boolean isVertexProgramSupported() {
        return _supportsVertexProgram;
    }

    public int getMaxGLSLVertexAttributes() {
        return _maxGLSLVertexAttribs;
    }

    public boolean isDepthTextureSupported() {
        return _supportsDepthTexture;
    }

    public boolean isARBShadowSupported() {
        return _supportsShadow;
    }

    public boolean isTextureMirroredRepeatSupported() {
        return _supportsMirroredRepeat;
    }

    public boolean isTextureMirrorClampSupported() {
        return _supportsMirrorClamp;
    }

    public boolean isTextureMirrorEdgeClampSupported() {
        return _supportsMirrorClamp;
    }

    public boolean isTextureMirrorBorderClampSupported() {
        return _supportsMirrorBorderClamp;
    }

    public boolean isTextureBorderClampSupported() {
        return _supportsBorderClamp;
    }

    public boolean isTextureEdgeClampSupported() {
        return _supportsEdgeClamp;
    }

}
