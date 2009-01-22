/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.jogl;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;

import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.util.geom.BufferUtils;

public final class JoglContextCapabilities extends ContextCapabilities {

    public JoglContextCapabilities(final GLAutoDrawable autodrawable) {
        init(autodrawable.getGL());
    }

    public JoglContextCapabilities(final GL gl) {
        init(gl);
    }

    public void init(final GL gl) {
        final IntBuffer buf = BufferUtils.createIntBuffer(16);

        supportsVBO = gl.isExtensionAvailable("GL_ARB_vertex_buffer_object");
        supportsGL1_2 = gl.isExtensionAvailable("GL_VERSION_1_2");
        supportsMultisample = gl.isExtensionAvailable("GL_ARB_multisample");

        supportsConstantColor = supportsEq = gl.isExtensionAvailable("GL_ARB_imaging");
        supportsSeparateFunc = gl.isExtensionAvailable("GL_EXT_blend_func_separate");
        supportsSeparateEq = gl.isExtensionAvailable("GL_EXT_blend_equation_separate");
        supportsMinMax = gl.isExtensionAvailable("GL_EXT_blend_minmax");
        supportsSubtract = gl.isExtensionAvailable("GL_EXT_blend_subtract");

        supportsFogCoords = gl.isExtensionAvailable("GL_EXT_fog_coord");
        supportsFragmentProgram = gl.isExtensionAvailable("GL_ARB_fragment_program");
        supportsVertexProgram = gl.isExtensionAvailable("GL_ARB_vertex_program");

        glslSupported = gl.isExtensionAvailable("GL_ARB_shader_objects")
                && gl.isExtensionAvailable("GL_ARB_fragment_shader") && gl.isExtensionAvailable("GL_ARB_vertex_shader")
                && gl.isExtensionAvailable("GL_ARB_shading_language_100");

        if (glslSupported) {
            gl.glGetIntegerv(GL.GL_MAX_VERTEX_ATTRIBS_ARB, buf);
            maxGLSLVertexAttribs = buf.get(0);
        }

        // Pbuffer
        pbufferSupported = gl.isExtensionAvailable("GL_ARB_pixel_buffer_object");

        // FBO
        fboSupported = gl.isExtensionAvailable("GL_EXT_framebuffer_object");
        if (fboSupported) {
            if (gl.isExtensionAvailable("GL_ARB_draw_buffers")) {
                gl.glGetIntegerv(GL.GL_MAX_COLOR_ATTACHMENTS_EXT, buf);
                maxFBOColorAttachments = buf.get(0);
            } else {
                maxFBOColorAttachments = 1;
            }
        } else {
            maxFBOColorAttachments = 0;
        }

        twoSidedStencilSupport = gl.isExtensionAvailable("GL_EXT_stencil_two_side");
        stencilWrapSupport = gl.isExtensionAvailable("GL_EXT_stencil_wrap");

        // max texture size.
        gl.glGetIntegerv(GL.GL_MAX_TEXTURE_SIZE, buf);
        maxTextureSize = buf.get(0);

        // Check for support of multitextures.
        supportsMultiTexture = gl.isExtensionAvailable("GL_ARB_multitexture");

        // Check for support of fixed function dot3 environment settings
        supportsEnvDot3 = gl.isExtensionAvailable("GL_ARB_texture_env_dot3");

        // Check for support of fixed function dot3 environment settings
        supportsEnvCombine = gl.isExtensionAvailable("GL_ARB_texture_env_combine");

        // Check for support of automatic mipmap generation
        automaticMipMaps = gl.isExtensionAvailable("GL_SGIS_generate_mipmap");

        supportsDepthTexture = gl.isExtensionAvailable("GL_ARB_depth_texture");
        supportsShadow = gl.isExtensionAvailable("GL_ARB_shadow");

        // If we do support multitexturing, find out how many textures we
        // can handle.
        if (supportsMultiTexture) {
            gl.glGetIntegerv(GL.GL_MAX_TEXTURE_UNITS, buf);
            numFixedTexUnits = buf.get(0);
        } else {
            numFixedTexUnits = 1;
        }

        // Go on to check number of texture units supported for vertex and
        // fragment shaders
        if (gl.isExtensionAvailable("GL_ARB_shader_objects") && gl.isExtensionAvailable("GL_ARB_vertex_shader")
                && gl.isExtensionAvailable("GL_ARB_fragment_shader")) {
            gl.glGetIntegerv(GL.GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS_ARB, buf);
            numVertexTexUnits = buf.get(0);
            gl.glGetIntegerv(GL.GL_MAX_TEXTURE_IMAGE_UNITS_ARB, buf);
            numFragmentTexUnits = buf.get(0);
            gl.glGetIntegerv(GL.GL_MAX_TEXTURE_COORDS_ARB, buf);
            numFragmentTexCoordUnits = buf.get(0);
        } else {
            // based on nvidia dev doc:
            // http://developer.nvidia.com/object/General_FAQ.html#t6
            // "For GPUs that do not support GL_ARB_fragment_program and
            // GL_NV_fragment_program, those two limits are set equal to
            // GL_MAX_TEXTURE_UNITS."
            numFragmentTexCoordUnits = numFixedTexUnits;
            numFragmentTexUnits = numFixedTexUnits;

            // We'll set this to 0 for now since we do not know:
            numVertexTexUnits = 0;
        }

        // Now determine the maximum number of supported texture units
        numTotalTexUnits = Math.max(numFragmentTexCoordUnits, Math.max(numFixedTexUnits, Math.max(numFragmentTexUnits,
                numVertexTexUnits)));

        // Check for S3 texture compression capability.
        supportsS3TCCompression = gl.isExtensionAvailable("GL_EXT_texture_compression_s3tc");

        // Check for S3 texture compression capability.
        supportsTexture3D = gl.isExtensionAvailable("GL_EXT_texture_3d");

        // Check for S3 texture compression capability.
        supportsTextureCubeMap = gl.isExtensionAvailable("GL_ARB_texture_cube_map");

        // See if we support anisotropic filtering
        supportsAniso = gl.isExtensionAvailable("GL_EXT_texture_filter_anisotropic");

        if (supportsAniso) {
            final FloatBuffer max_a = BufferUtils.createFloatBuffer(1);
            max_a.rewind();

            // Grab the maximum anisotropic filter.
            gl.glGetFloatv(GL.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, max_a);

            // set max.
            maxAnisotropic = max_a.get(0);
        }

        // See if we support textures that are not power of 2 in size.
        supportsNonPowerTwo = gl.isExtensionAvailable("GL_ARB_texture_non_power_of_two");

        // See if we support textures that do not have width == height.
        supportsRectangular = gl.isExtensionAvailable("GL_ARB_texture_rectangle");

        supportsMirroredRepeat = gl.isExtensionAvailable("GL_ARB_texture_mirrored_repeat");
        supportsMirrorClamp = supportsMirrorBorderClamp = supportsMirrorEdgeClamp = gl
                .isExtensionAvailable("GL_EXT_texture_mirror_clamp");
        supportsBorderClamp = gl.isExtensionAvailable("GL_ARB_texture_border_clamp");
        supportsEdgeClamp = supportsGL1_2;
    }
}
