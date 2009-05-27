/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.terrain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.renderer.state.GLSLShaderDataLogic;
import com.ardor3d.renderer.state.GLSLShaderObjectsState;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.AbstractBufferData.VBOAccessMode;
import com.ardor3d.scenegraph.hint.DataMode;

/**
 * First iteration of a Geometry Clipmap terrain, without streaming support.
 */
public class GeometryClipmapTerrain extends Node {
    /** The Constant logger. */
    private static final Logger logger = Logger.getLogger(GeometryClipmapTerrain.class.getName());

    private List<ClipmapLevel> clips;
    private int visibleLevels = 0;
    private final Camera terrainCamera;
    private final int totalSize;
    private final int clipSideSize;
    private final Vector3 lightDirection = new Vector3(1, 1, 2);

    private boolean initialized = false;

    /** Shader for rendering clipmap geometry with morphing. */
    private GLSLShaderObjectsState _geometryClipmapShader;

    public GeometryClipmapTerrain(final Camera camera, final HeightmapPyramid heightmapPyramid, final int clipSideSize,
            final float heightScale) {
        terrainCamera = camera;
        totalSize = heightmapPyramid.getSize(0);
        this.clipSideSize = clipSideSize;

        getSceneHints().setRenderBucketType(RenderBucketType.Opaque);
        final CullState cs = new CullState();
        cs.setEnabled(true);
        cs.setCullFace(CullState.Face.Front);
        setRenderState(cs);

        try {
            clips = new ArrayList<ClipmapLevel>();

            for (int i = 0; i < heightmapPyramid.getHeightmapCount(); i++) {
                final ClipmapLevel clipmap = new ClipmapLevel(i, camera, clipSideSize, heightScale, heightmapPyramid);
                clips.add(clipmap);
                attachChild(clipmap);

                clipmap.getSceneHints().setDataMode(DataMode.VBO);

                // clipmap.getSceneHints().setDataMode(DataMode.VBOInterleaved);
                // final FloatBufferData interleavedData = new FloatBufferData();
                // interleavedData.setVboAccessMode(VBOAccessMode.DynamicDraw);
                // clipmap.getMeshData().setInterleavedData(interleavedData);

                clipmap.getMeshData().getVertexCoords().setVboAccessMode(VBOAccessMode.DynamicDraw);
                clipmap.getMeshData().getIndices().setVboAccessMode(VBOAccessMode.DynamicDraw);
            }
        } catch (final Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    protected void updateChildren(final double time) {
        super.updateChildren(time);

        for (int i = clips.size() - 1; i >= 0; i--) {
            if (!clips.get(i).isReady()) {
                visibleLevels = i + 1;
                break;
            }
        }

        // TODO: only run update and refresh if needed.

        // Update vertices.
        for (int i = clips.size() - 1; i >= visibleLevels; i--) {
            clips.get(i).updateVertices();
        }

        // Update indices.
        for (int i = clips.size() - 1; i >= visibleLevels; i--) {
            if (i == visibleLevels) {
                // Level 0 has no nested level, so pass null as parameter.
                clips.get(i).updateIndices(null);
            } else {
                // All other levels i have the level i-1 nested in.
                clips.get(i).updateIndices(clips.get(i - 1));
            }
        }

        for (int i = clips.size() - 1; i >= visibleLevels; i--) {
            clips.get(i).getMeshData().getVertexCoords().setNeedsRefresh(true);
            clips.get(i).getMeshData().getIndices().setNeedsRefresh(true);
        }
    }

    @Override
    public void draw(final Renderer r) {
        updateShader();

        if (!initialized) {
            for (int i = clips.size() - 1; i >= visibleLevels; i--) {
                final ClipmapLevel clip = clips.get(i);

                clip.getMeshData().getIndexBuffer().limit(clip.getMeshData().getIndexBuffer().capacity());
            }

            initialized = true;
        }

        // draw levels from coarse to fine.
        for (int i = clips.size() - 1; i >= visibleLevels; i--) {
            final ClipmapLevel clip = clips.get(i);

            if (clip.getStripIndex() > 0) {
                clip.draw(r);
            }
        }
    }

    /**
     * Initialize/Update shaders
     */
    public void updateShader() {
        if (_geometryClipmapShader != null) {
            _geometryClipmapShader.setUniform("eyePosition", terrainCamera.getLocation());
            _geometryClipmapShader.setUniform("lightDirection", lightDirection.normalizeLocal());

            return;
        }

        final ContextCapabilities caps = ContextManager.getCurrentContext().getCapabilities();
        if (caps.isGLSLSupported()) {
            _geometryClipmapShader = new GLSLShaderObjectsState();
            try {
                _geometryClipmapShader.setVertexShader(ClipmapLevel.class.getClassLoader().getResourceAsStream(
                        "com/ardor3d/extension/terrain/geometryClipmapShader.vert"));
                _geometryClipmapShader.setFragmentShader(ClipmapLevel.class.getClassLoader().getResourceAsStream(
                        "com/ardor3d/extension/terrain/geometryClipmapShader.frag"));
            } catch (final IOException ex) {
                logger.logp(Level.SEVERE, getClass().getName(), "init(Renderer)", "Could not load shaders.", ex);
            }
            _geometryClipmapShader.setUniform("texture", 0);
            _geometryClipmapShader.setUniform("normalMap", 1);

            _geometryClipmapShader.setUniform("texelSize", 1f / totalSize);
            _geometryClipmapShader.setUniform("clipSideSize", (float) clipSideSize);

            _geometryClipmapShader.setShaderDataLogic(new GLSLShaderDataLogic() {
                public void applyData(final GLSLShaderObjectsState shader, final Mesh mesh, final Renderer renderer) {
                    shader.setUniform("vertexDistance", (float) ((ClipmapLevel) mesh).getVertexDistance());
                }
            });

            // setRenderState(_geometryClipmapShader);
            for (int i = clips.size() - 1; i >= 0; i--) {
                final ClipmapLevel clip = clips.get(i);
                clip.setRenderState(_geometryClipmapShader);
            }
        }
        updateWorldRenderStates(false);
    }

    /**
     * @return the visibleLevels
     */
    public int getVisibleLevels() {
        return visibleLevels;
    }

    /**
     * @param visibleLevels
     *            the visibleLevels to set
     */
    public void setVisibleLevels(final int visibleLevels) {
        this.visibleLevels = visibleLevels;
    }

    /**
     * @param lightDirection
     *            the lightDirection to set
     */
    public void setLightDirection(final ReadOnlyVector3 lightDirection) {
        this.lightDirection.set(lightDirection);
    }
}
