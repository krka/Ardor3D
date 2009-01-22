/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.lwjgl;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.opengl.ARBFragmentShader;
import org.lwjgl.opengl.ARBMultitexture;
import org.lwjgl.opengl.ARBVertexShader;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.EXTTextureFilterAnisotropic;
import org.lwjgl.opengl.GL11;

import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.util.geom.BufferUtils;

public class LwjglContextCapabilities extends ContextCapabilities {

    public LwjglContextCapabilities(final org.lwjgl.opengl.ContextCapabilities caps) {
        final IntBuffer buf = BufferUtils.createIntBuffer(16);

        supportsVBO = caps.GL_ARB_vertex_buffer_object;
        supportsGL1_2 = caps.OpenGL12;
        supportsMultisample = caps.GL_ARB_multisample;

        supportsConstantColor = supportsEq = caps.GL_ARB_imaging;
        supportsSeparateFunc = caps.GL_EXT_blend_func_separate;
        supportsSeparateEq = caps.GL_EXT_blend_equation_separate;
        supportsMinMax = caps.GL_EXT_blend_minmax;
        supportsSubtract = caps.GL_EXT_blend_subtract;

        supportsFogCoords = caps.GL_EXT_fog_coord;
        supportsFragmentProgram = caps.GL_ARB_fragment_program;
        supportsVertexProgram = caps.GL_ARB_vertex_program;

        glslSupported = caps.GL_ARB_shader_objects && caps.GL_ARB_fragment_shader && caps.GL_ARB_vertex_shader
                && caps.GL_ARB_shading_language_100;

        if (glslSupported) {
            GL11.glGetInteger(ARBVertexShader.GL_MAX_VERTEX_ATTRIBS_ARB, buf);
            maxGLSLVertexAttribs = buf.get(0);
        }

        // Pbuffer
        pbufferSupported = caps.GL_ARB_pixel_buffer_object;

        // FBO
        fboSupported = caps.GL_EXT_framebuffer_object;
        if (fboSupported) {
            if (caps.GL_ARB_draw_buffers) {
                GL11.glGetInteger(EXTFramebufferObject.GL_MAX_COLOR_ATTACHMENTS_EXT, buf);
                maxFBOColorAttachments = buf.get(0);
            } else {
                maxFBOColorAttachments = 1;
            }
        } else {
            maxFBOColorAttachments = 0;
        }

        twoSidedStencilSupport = caps.GL_EXT_stencil_two_side;
        stencilWrapSupport = caps.GL_EXT_stencil_wrap;

        // max texture size.
        GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE, buf);
        maxTextureSize = buf.get(0);

        // Check for support of multitextures.
        supportsMultiTexture = caps.GL_ARB_multitexture;

        // Check for support of fixed function dot3 environment settings
        supportsEnvDot3 = caps.GL_ARB_texture_env_dot3;

        // Check for support of fixed function dot3 environment settings
        supportsEnvCombine = caps.GL_ARB_texture_env_combine;

        // Check for support of automatic mipmap generation
        automaticMipMaps = caps.GL_SGIS_generate_mipmap;

        supportsDepthTexture = caps.GL_ARB_depth_texture;
        supportsShadow = caps.GL_ARB_shadow;

        // If we do support multitexturing, find out how many textures we
        // can handle.
        if (supportsMultiTexture) {
            GL11.glGetInteger(ARBMultitexture.GL_MAX_TEXTURE_UNITS_ARB, buf);
            numFixedTexUnits = buf.get(0);
        } else {
            numFixedTexUnits = 1;
        }

        // Go on to check number of texture units supported for vertex and
        // fragment shaders
        if (caps.GL_ARB_shader_objects && caps.GL_ARB_vertex_shader && caps.GL_ARB_fragment_shader) {
            GL11.glGetInteger(ARBVertexShader.GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS_ARB, buf);
            numVertexTexUnits = buf.get(0);
            GL11.glGetInteger(ARBFragmentShader.GL_MAX_TEXTURE_IMAGE_UNITS_ARB, buf);
            numFragmentTexUnits = buf.get(0);
            GL11.glGetInteger(ARBFragmentShader.GL_MAX_TEXTURE_COORDS_ARB, buf);
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
        supportsS3TCCompression = caps.GL_EXT_texture_compression_s3tc;

        // Check for S3 texture compression capability.
        supportsTexture3D = caps.GL_EXT_texture_3d;

        // Check for S3 texture compression capability.
        supportsTextureCubeMap = caps.GL_ARB_texture_cube_map;

        // See if we support anisotropic filtering
        supportsAniso = caps.GL_EXT_texture_filter_anisotropic;

        if (supportsAniso) {
            // Due to LWJGL buffer check, you can't use smaller sized
            // buffers (min_size = 16 for glGetFloat()).
            final FloatBuffer max_a = BufferUtils.createFloatBuffer(16);
            max_a.rewind();

            // Grab the maximum anisotropic filter.
            GL11.glGetFloat(EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, max_a);

            // set max.
            maxAnisotropic = max_a.get(0);
        }

        // See if we support textures that are not power of 2 in size.
        supportsNonPowerTwo = caps.GL_ARB_texture_non_power_of_two;

        // See if we support textures that do not have width == height.
        supportsRectangular = caps.GL_ARB_texture_rectangle;

        supportsMirroredRepeat = caps.GL_ARB_texture_mirrored_repeat;
        supportsMirrorClamp = supportsMirrorEdgeClamp = supportsMirrorBorderClamp = caps.GL_EXT_texture_mirror_clamp;
        supportsBorderClamp = caps.GL_ARB_texture_border_clamp;
        supportsEdgeClamp = supportsGL1_2;

    }

}
