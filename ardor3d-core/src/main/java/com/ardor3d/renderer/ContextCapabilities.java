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

    protected boolean supportsVBO = false;
    protected boolean supportsGL1_2 = false;
    protected boolean supportsMultisample = false;

    protected boolean supportsConstantColor = false;
    protected boolean supportsEq = false;
    protected boolean supportsSeparateEq = false;
    protected boolean supportsSeparateFunc = false;
    protected boolean supportsMinMax = false;
    protected boolean supportsSubtract = false;

    protected boolean supportsFogCoords = false;

    protected boolean supportsFragmentProgram = false;
    protected boolean supportsVertexProgram = false;

    protected boolean glslSupported = false;
    protected int maxGLSLVertexAttribs;

    protected boolean pbufferSupported = false;

    protected boolean fboSupported = false;
    protected int maxFBOColorAttachments = 1;

    protected boolean twoSidedStencilSupport = false;
    protected boolean stencilWrapSupport = false;

    /** The total number of supported texture units. */
    protected int numTotalTexUnits = -1;

    /** The number of texture units availible for fixed functionality */
    protected int numFixedTexUnits = -1;

    /** The number of texture units availible to vertex shader */
    protected int numVertexTexUnits = -1;

    /** The number of texture units availible to fragment shader */
    protected int numFragmentTexUnits = -1;

    /** The number of texture coordinate sets available */
    protected int numFragmentTexCoordUnits = -1;

    /** The max side of a texture supported. */
    protected int maxTextureSize = -1;

    protected float maxAnisotropic = -1.0f;

    /** True if multitexturing is supported. */
    protected boolean supportsMultiTexture = false;

    /** True if combine dot3 is supported. */
    protected boolean supportsEnvDot3 = false;

    /** True if combine dot3 is supported. */
    protected boolean supportsEnvCombine = false;

    /** True if anisofiltering is supported. */
    protected boolean supportsAniso = false;

    /** True if non pow 2 texture sizes are supported. */
    protected boolean supportsNonPowerTwo = false;

    /** True if rectangular textures are supported (vs. only square textures) */
    protected boolean supportsRectangular = false;

    /** True if S3TC compression is supported. */
    protected boolean supportsS3TCCompression = false;

    /** True if Texture3D is supported. */
    protected boolean supportsTexture3D = false;

    /** True if TextureCubeMap is supported. */
    protected boolean supportsTextureCubeMap = false;

    /** True if non-GLU mipmap generation (part of FBO) is supported. */
    protected boolean automaticMipMaps = false;

    /** True if depth textures are supported */
    protected boolean supportsDepthTexture = false;

    /** True if shadow mapping supported */
    protected boolean supportsShadow = false;

    protected boolean supportsMirroredRepeat;
    protected boolean supportsMirrorClamp;
    protected boolean supportsMirrorBorderClamp;
    protected boolean supportsMirrorEdgeClamp;
    protected boolean supportsBorderClamp;
    protected boolean supportsEdgeClamp;

    /**
     * @return true if we support Vertex Buffer Objects.
     */
    public boolean isVBOSupported() {
        return supportsVBO;
    }

    /**
     * @return true if we support all of OpenGL 1.2
     */
    public boolean isOpenGL1_2Supported() {
        return supportsGL1_2;
    }

    /**
     * @return true if we support multisampling (antialiasing)
     */
    public boolean isMultisampleSupported() {
        return supportsMultisample;
    }

    /**
     * @return true if we support setting a constant color for use with *Constant* type BlendFunctions.
     */
    public boolean isConstantBlendColorSupported() {
        return supportsConstantColor;
    }

    /**
     * @return true if we support setting rgb and alpha functions separately for source and destination.
     */
    public boolean isSeparateBlendFunctionsSupported() {
        return supportsSeparateFunc;
    }

    /**
     * @return true if we support setting the blend equation
     */
    public boolean isBlendEquationSupported() {
        return supportsEq;
    }

    /**
     * @return true if we support setting the blend equation for alpha and rgb separately
     */
    public boolean isSeparateBlendEquationsSupported() {
        return supportsSeparateEq;
    }

    /**
     * @return true if we support using min and max blend equations
     */
    public boolean isMinMaxBlendEquationsSupported() {
        return supportsMinMax;
    }

    /**
     * @return true if we support using subtract blend equations
     */
    public boolean isSubtractBlendEquationsSupported() {
        return supportsSubtract;
    }

    /**
     * @return true if mesh based fog coords are supported
     */
    public boolean isFogCoordinatesSupported() {
        return supportsFogCoords;
    }

    /**
     * @return true if the ARB_shader_objects extension is supported by current graphics configuration.
     */
    public boolean isGLSLSupported() {
        return glslSupported;
    }

    /**
     * @return true if the ARB_shader_objects extension is supported by current graphics configuration.
     */
    public boolean isPbufferSupported() {
        return pbufferSupported;
    }

    /**
     * @return true if the EXT_framebuffer_object extension is supported by current graphics configuration.
     */
    public boolean isFBOSupported() {
        return fboSupported;
    }

    /**
     * @return true if we can handle doing separate stencil operations for front and back facing polys in a single pass.
     */
    public boolean isTwoSidedStencilSupported() {
        return twoSidedStencilSupport;
    }

    /**
     * @return true if we can handle wrapping increment/decrement operations.
     */
    public boolean isStencilWrapSupported() {
        return stencilWrapSupport;
    }

    /**
     * <code>getTotalNumberOfUnits</code> returns the total number of texture units this context supports.
     * 
     * @return the total number of texture units supported by the context.
     */
    public int getTotalNumberOfUnits() {
        return numTotalTexUnits;
    }

    /**
     * <code>getNumberOfFixedUnits</code> returns the number of texture units this context supports, for use in the
     * fixed pipeline.
     * 
     * @return the number units.
     */
    public int getNumberOfFixedTextureUnits() {
        return numFixedTexUnits;
    }

    /**
     * <code>getNumberOfVertexUnits</code> returns the number of texture units available to a vertex shader that this
     * context supports.
     * 
     * @return the number of units.
     */
    public int getNumberOfVertexUnits() {
        return numVertexTexUnits;
    }

    /**
     * <code>getNumberOfFragmentUnits</code> returns the number of texture units available to a fragment shader that
     * this context supports.
     * 
     * @return the number of units.
     */
    public int getNumberOfFragmentTextureUnits() {
        return numFragmentTexUnits;
    }

    /**
     * <code>getNumberOfFragmentTexCoordUnits</code> returns the number of texture coordinate sets available that this
     * context supports.
     * 
     * @return the number of units.
     */
    public int getNumberOfFragmentTexCoordUnits() {
        return numFragmentTexCoordUnits;
    }

    /**
     * @return the max size of texture (in terms of # pixels wide) that this context supports.
     */
    public int getMaxTextureSize() {
        return maxTextureSize;
    }

    /**
     * <code>getNumberOfTotalUnits</code> returns the number of texture units this context supports.
     * 
     * @return the number of units.
     */
    public int getNumberOfTotalTextureUnits() {
        return numTotalTexUnits;
    }

    /**
     * <code>getMaxFBOColorAttachments</code> returns the MAX_COLOR_ATTACHMENTS for FBOs that this context supports.
     * 
     * @return the number of buffers.
     */
    public int getMaxFBOColorAttachments() {
        return maxFBOColorAttachments;
    }

    /**
     * Returns the maximum anisotropic filter.
     * 
     * @return The maximum anisotropic filter.
     */
    public float getMaxAnisotropic() {
        return maxAnisotropic;
    }

    /**
     * @return true if multi-texturing is supported in fixed function
     */
    public boolean isMultitextureSupported() {
        return supportsMultiTexture;
    }

    /**
     * @return true we support dot3 environment texture settings
     */
    public boolean isEnvDot3TextureCombineSupported() {
        return supportsEnvDot3;
    }

    /**
     * @return true we support combine environment texture settings
     */
    public boolean isEnvCombineSupported() {
        return supportsEnvCombine;
    }

    /**
     * Returns if S3TC compression is available for textures.
     * 
     * @return true if S3TC is available.
     */
    public boolean isS3TCSupported() {
        return supportsS3TCCompression;
    }

    /**
     * Returns if Texture3D is available for textures.
     * 
     * @return true if Texture3D is available.
     */
    public boolean isTexture3DSupported() {
        return supportsTexture3D;
    }

    /**
     * Returns if TextureCubeMap is available for textures.
     * 
     * @return true if TextureCubeMap is available.
     */
    public boolean isTextureCubeMapSupported() {
        return supportsTextureCubeMap;
    }

    /**
     * Returns if AutomaticMipmap generation is available for textures.
     * 
     * @return true if AutomaticMipmap generation is available.
     */
    public boolean isAutomaticMipmapsSupported() {
        return automaticMipMaps;
    }

    /**
     * @return if Anisotropic texture filtering is supported
     */
    public boolean isAnisoSupported() {
        return supportsAniso;
    }

    /**
     * @return true if non pow 2 texture sizes are supported
     */
    public boolean isNonPowerOfTwoTextureSupported() {
        return supportsNonPowerTwo;
    }

    /**
     * @return if rectangular texture sizes are supported (width != height)
     */
    public boolean isRectangularTextureSupported() {
        return supportsRectangular;
    }

    public boolean isFragmentProgramSupported() {
        return supportsFragmentProgram;
    }

    public boolean isVertexProgramSupported() {
        return supportsVertexProgram;
    }

    public int getMaxGLSLVertexAttributes() {
        return maxGLSLVertexAttribs;
    }

    public boolean isDepthTextureSupported() {
        return supportsDepthTexture;
    }

    public boolean isARBShadowSupported() {
        return supportsShadow;
    }

    public boolean isTextureMirroredRepeatSupported() {
        return supportsMirroredRepeat;
    }

    public boolean isTextureMirrorClampSupported() {
        return supportsMirrorClamp;
    }

    public boolean isTextureMirrorEdgeClampSupported() {
        return supportsMirrorClamp;
    }

    public boolean isTextureMirrorBorderClampSupported() {
        return supportsMirrorBorderClamp;
    }

    public boolean isTextureBorderClampSupported() {
        return supportsBorderClamp;
    }

    public boolean isTextureEdgeClampSupported() {
        return supportsEdgeClamp;
    }

}
