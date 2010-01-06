/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scene.state.jogl;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;

import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.jogl.JoglRenderer;
import com.ardor3d.renderer.state.MaterialState;
import com.ardor3d.renderer.state.MaterialState.ColorMaterial;
import com.ardor3d.renderer.state.MaterialState.MaterialFace;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.record.MaterialStateRecord;

public abstract class JoglMaterialStateUtil {

    public static void apply(final JoglRenderer renderer, final MaterialState state) {
        final GL gl = GLU.getCurrentGL();

        // ask for the current state record
        final RenderContext context = ContextManager.getCurrentContext();
        final MaterialStateRecord record = (MaterialStateRecord) context.getStateRecord(StateType.Material);
        context.setCurrentState(StateType.Material, state);

        if (state.isEnabled()) {
            final MaterialFace face = state.getMaterialFace();

            // setup color material, if changed.
            applyColorMaterial(state.getColorMaterial(), face, record);

            // apply colors, if needed and not what is currently set.
            applyColor(ColorMaterial.Ambient, state.getAmbient(), face, record);
            applyColor(ColorMaterial.Diffuse, state.getDiffuse(), face, record);
            applyColor(ColorMaterial.Emissive, state.getEmissive(), face, record);
            applyColor(ColorMaterial.Specular, state.getSpecular(), face, record);

            // set our shine
            if (!record.isValid() || face != record.face || record.shininess != state.getShininess()) {
                final int glFace = getGLMaterialFace(state.getMaterialFace());
                gl.glMaterialf(glFace, GL.GL_SHININESS, state.getShininess());
                record.shininess = state.getShininess();
            }

            record.face = face;
        } else {
            // apply defaults
            final MaterialFace face = MaterialState.DEFAULT_MATERIAL_FACE;

            applyColorMaterial(MaterialState.DEFAULT_COLOR_MATERIAL, face, record);

            applyColor(ColorMaterial.Ambient, MaterialState.DEFAULT_AMBIENT, face, record);
            applyColor(ColorMaterial.Diffuse, MaterialState.DEFAULT_DIFFUSE, face, record);
            applyColor(ColorMaterial.Emissive, MaterialState.DEFAULT_EMISSIVE, face, record);
            applyColor(ColorMaterial.Specular, MaterialState.DEFAULT_SPECULAR, face, record);

            // set our shine
            if (!record.isValid() || face != record.face || record.shininess != MaterialState.DEFAULT_SHININESS) {
                final int glFace = getGLMaterialFace(state.getMaterialFace());
                gl.glMaterialf(glFace, GL.GL_SHININESS, MaterialState.DEFAULT_SHININESS);
                record.shininess = MaterialState.DEFAULT_SHININESS;
            }

            record.face = face;
        }

        if (!record.isValid()) {
            record.validate();
        }
    }

    private static void applyColor(final ColorMaterial glMatColor, final ReadOnlyColorRGBA color,
            final MaterialFace face, final MaterialStateRecord record) {
        final GL gl = GLU.getCurrentGL();

        if (!isVertexProvidedColor(glMatColor, record)
                && (!record.isValid() || face != record.face || !record.isSetColor(face, glMatColor, color, record))) {

            record.tempColorBuff.clear();
            record.tempColorBuff.put(color.getRed()).put(color.getGreen()).put(color.getBlue()).put(color.getAlpha());
            record.tempColorBuff.flip();

            final int glFace = getGLMaterialFace(face);
            final int glMat = getGLColorMaterial(glMatColor);
            gl.glMaterialfv(glFace, glMat, record.tempColorBuff);

            record.setColor(face, glMatColor, color);
        }
    }

    private static boolean isVertexProvidedColor(final ColorMaterial glMatColor, final MaterialStateRecord record) {
        switch (glMatColor) {
            case Ambient:
                return record.colorMaterial == ColorMaterial.Ambient
                        || record.colorMaterial == ColorMaterial.AmbientAndDiffuse;
            case Diffuse:
                return record.colorMaterial == ColorMaterial.Diffuse
                        || record.colorMaterial == ColorMaterial.AmbientAndDiffuse;
            case Specular:
                return record.colorMaterial == ColorMaterial.Specular;
            case Emissive:
                return record.colorMaterial == ColorMaterial.Emissive;
        }
        return false;
    }

    private static void applyColorMaterial(final ColorMaterial colorMaterial, final MaterialFace face,
            final MaterialStateRecord record) {
        final GL gl = GLU.getCurrentGL();

        if (!record.isValid() || face != record.face || colorMaterial != record.colorMaterial) {
            if (colorMaterial == ColorMaterial.None) {
                gl.glDisable(GL.GL_COLOR_MATERIAL);
            } else {
                final int glMat = getGLColorMaterial(colorMaterial);
                final int glFace = getGLMaterialFace(face);

                gl.glColorMaterial(glFace, glMat);
                gl.glEnable(GL.GL_COLOR_MATERIAL);
                record.resetColorsForCM(face, colorMaterial);
            }
            record.colorMaterial = colorMaterial;
        }
    }

    /**
     * Converts the color material setting of this state to a GL constant.
     * 
     * @return the GL constant
     */
    private static int getGLColorMaterial(final ColorMaterial material) {
        switch (material) {
            case None:
                return GL.GL_NONE;
            case Ambient:
                return GL.GL_AMBIENT;
            case Diffuse:
                return GL.GL_DIFFUSE;
            case AmbientAndDiffuse:
                return GL.GL_AMBIENT_AND_DIFFUSE;
            case Emissive:
                return GL.GL_EMISSION;
            case Specular:
                return GL.GL_SPECULAR;
        }
        throw new IllegalArgumentException("invalid color material setting: " + material);
    }

    /**
     * Converts the material face setting of this state to a GL constant.
     * 
     * @return the GL constant
     */
    private static int getGLMaterialFace(final MaterialFace face) {
        switch (face) {
            case Front:
                return GL.GL_FRONT;
            case Back:
                return GL.GL_BACK;
            case FrontAndBack:
                return GL.GL_FRONT_AND_BACK;
        }
        throw new IllegalArgumentException("invalid material face setting: " + face);
    }
}
