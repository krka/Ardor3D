/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
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

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.renderer.state.GLSLShaderDataLogic;
import com.ardor3d.renderer.state.GLSLShaderObjectsState;
import com.ardor3d.renderer.state.MaterialState;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.hint.DataMode;

/**
 * First iteration of a Geometry Clipmap terrain, without streaming support.
 */
public class GeometryClipmapTerrain extends Node {
    /** The Constant logger. */
    private static final Logger logger = Logger.getLogger(GeometryClipmapTerrain.class.getName());

    private List<ClipmapLevel> _clips;
    private int _visibleLevels = 0;
    private final Camera _terrainCamera;
    private final int _totalSize;
    private final int _clipSideSize;

    private boolean _initialized = false;

    /** Shader for rendering clipmap geometry with morphing. */
    private GLSLShaderObjectsState _geometryClipmapShader;

    private final Vector3 transformedFrustumPos = new Vector3();

    private final float _heightScale;

    public GeometryClipmapTerrain(final Camera camera, final HeightmapPyramid heightmapPyramid, final int clipSideSize,
            final float heightScale) {
        _terrainCamera = camera;
        _totalSize = heightmapPyramid.getSize(0);
        _heightScale = heightScale;
        _clipSideSize = clipSideSize;

        getSceneHints().setRenderBucketType(RenderBucketType.Opaque);
        final CullState cs = new CullState();
        cs.setEnabled(true);
        cs.setCullFace(CullState.Face.Front);
        setRenderState(cs);

        final MaterialState materialState = new MaterialState();
        materialState.setAmbient(new ColorRGBA(1, 1, 1, 1));
        materialState.setDiffuse(new ColorRGBA(1, 1, 1, 1));
        materialState.setSpecular(new ColorRGBA(1, 1, 1, 1));
        materialState.setShininess(64.0f);
        setRenderState(materialState);

        try {
            _clips = new ArrayList<ClipmapLevel>();

            for (int i = 0; i < heightmapPyramid.getHeightmapCount(); i++) {
                final ClipmapLevel clipmap = new ClipmapLevel(i, camera, clipSideSize, heightScale, heightmapPyramid);
                _clips.add(clipmap);
                attachChild(clipmap);

                clipmap.getSceneHints().setDataMode(DataMode.Arrays);

                // clipmap.getSceneHints().setDataMode(DataMode.VBOInterleaved);
                // final FloatBufferData interleavedData = new FloatBufferData();
                // interleavedData.setVboAccessMode(VBOAccessMode.DynamicDraw);
                // clipmap.getMeshData().setInterleavedData(interleavedData);

                // clipmap.getSceneHints().setDataMode(DataMode.VBO);
                // clipmap.getMeshData().getVertexCoords().setVboAccessMode(VBOAccessMode.DynamicDraw);
                // clipmap.getMeshData().getIndices().setVboAccessMode(VBOAccessMode.DynamicDraw);
            }
        } catch (final Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    protected void updateChildren(final double time) {
        super.updateChildren(time);

        for (int i = _clips.size() - 1; i >= 0; i--) {
            if (!_clips.get(i).isReady()) {
                _visibleLevels = i + 1;
                break;
            }
        }

        // TODO: Only run update and refresh if needed. A clipmap only needs to be updated if the camera location has
        // crossed a gridpoint.
        // TODO: Check for each level readiness and time to generate. Drop levels if over a threshold. (for example when
        // moving faster than data can be downloaded or generated)

        // Update vertices.
        for (int i = _clips.size() - 1; i >= _visibleLevels; i--) {
            _clips.get(i).updateVertices();
        }

        // Update indices.
        for (int i = _clips.size() - 1; i >= _visibleLevels; i--) {
            if (i == _visibleLevels) {
                // Level 0 has no nested level, so pass null as parameter.
                _clips.get(i).updateIndices(null);
            } else {
                // All other levels i have the level i-1 nested in.
                _clips.get(i).updateIndices(_clips.get(i - 1));
            }
        }

        for (int i = _clips.size() - 1; i >= _visibleLevels; i--) {
            _clips.get(i).getMeshData().getVertexCoords().setNeedsRefresh(true);
            _clips.get(i).getMeshData().getIndices().setNeedsRefresh(true);
        }
    }

    @Override
    public void draw(final Renderer r) {
        updateShader();

        if (!_initialized) {
            for (int i = _clips.size() - 1; i >= _visibleLevels; i--) {
                final ClipmapLevel clip = _clips.get(i);

                clip.getMeshData().getIndexBuffer().limit(clip.getMeshData().getIndexBuffer().capacity());
            }

            _initialized = true;
        }

        // draw levels from coarse to fine.
        for (int i = _clips.size() - 1; i >= _visibleLevels; i--) {
            final ClipmapLevel clip = _clips.get(i);

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
            getWorldTransform().applyInverse(_terrainCamera.getLocation(), transformedFrustumPos);
            _geometryClipmapShader.setUniform("eyePosition", getWorldTransform().applyInverse(
                    _terrainCamera.getLocation(), transformedFrustumPos));

            return;
        }

        reloadShader();
    }

    public void reloadShader() {
        final ContextCapabilities caps = ContextManager.getCurrentContext().getCapabilities();
        if (caps.isGLSLSupported()) {
            _geometryClipmapShader = new GLSLShaderObjectsState();
            try {
                _geometryClipmapShader.setVertexShader(ClipmapLevel.class.getClassLoader().getResourceAsStream(
                        "com/ardor3d/extension/terrain/geometryClipmapShader.vert"));
                _geometryClipmapShader.setFragmentShader(ClipmapLevel.class.getClassLoader().getResourceAsStream(
                        "com/ardor3d/extension/terrain/geometryClipmapShader.frag"));
                // _geometryClipmapShader.setVertexShader(ClipmapLevel.class.getClassLoader().getResourceAsStream(
                // "com/ardor3d/extension/terrain/geometryClipmapShaderSpecular.vert"));
                // _geometryClipmapShader.setFragmentShader(ClipmapLevel.class.getClassLoader().getResourceAsStream(
                // "com/ardor3d/extension/terrain/geometryClipmapShaderSpecular.frag"));
            } catch (final IOException ex) {
                logger.logp(Level.SEVERE, getClass().getName(), "init(Renderer)", "Could not load shaders.", ex);
            }
            _geometryClipmapShader.setUniform("texture", 0);
            _geometryClipmapShader.setUniform("normalMap", 1);

            _geometryClipmapShader.setUniform("texelSize", 1f / _totalSize);
            _geometryClipmapShader.setUniform("clipSideSize", (float) _clipSideSize);

            _geometryClipmapShader.setShaderDataLogic(new GLSLShaderDataLogic() {
                public void applyData(final GLSLShaderObjectsState shader, final Mesh mesh, final Renderer renderer) {
                    shader.setUniform("vertexDistance", (float) ((ClipmapLevel) mesh).getVertexDistance());
                }
            });
            applyToClips();
            updateWorldRenderStates(false);
        }
    }

    protected void applyToClips() {
        for (int i = _clips.size() - 1; i >= 0; i--) {
            final ClipmapLevel clip = _clips.get(i);
            clip.setRenderState(_geometryClipmapShader);
        }
    }

    public void regenerate() {
        for (int i = _clips.size() - 1; i >= 0; i--) {
            if (!_clips.get(i).isReady()) {
                _visibleLevels = i + 1;
                break;
            }
        }

        // Update vertices.
        for (int i = _clips.size() - 1; i >= _visibleLevels; i--) {
            _clips.get(i).regenerate();
        }

        // Update indices.
        for (int i = _clips.size() - 1; i >= _visibleLevels; i--) {
            if (i == _visibleLevels) {
                // Level 0 has no nested level, so pass null as parameter.
                _clips.get(i).updateIndices(null);
            } else {
                // All other levels i have the level i-1 nested in.
                _clips.get(i).updateIndices(_clips.get(i - 1));
            }
        }

        for (int i = _clips.size() - 1; i >= _visibleLevels; i--) {
            _clips.get(i).getMeshData().getVertexCoords().setNeedsRefresh(true);
            _clips.get(i).getMeshData().getIndices().setNeedsRefresh(true);
        }
    }

    /**
     * @return the visibleLevels
     */
    public int getVisibleLevels() {
        return _visibleLevels;
    }

    /**
     * @param visibleLevels
     *            the visibleLevels to set
     */
    public void setVisibleLevels(final int visibleLevels) {
        _visibleLevels = visibleLevels;
    }

    public float getHeightScale() {
        return _heightScale;
    }

    public void setHeightRange(final float heightRangeMin, final float heightRangeMax) {
        for (int i = _clips.size() - 1; i >= 0; i--) {
            final ClipmapLevel clip = _clips.get(i);
            clip.setHeightRange(heightRangeMin, heightRangeMax);
        }
    }

    public GLSLShaderObjectsState getGeometryClipmapShader() {
        return _geometryClipmapShader;
    }

    public void setGeometryClipmapShader(final GLSLShaderObjectsState shaderState) {
        _geometryClipmapShader = shaderState;
        applyToClips();
    }
}
