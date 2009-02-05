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

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.logging.Logger;

import javax.media.opengl.GL;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLException;
import javax.media.opengl.glu.GLU;

import com.ardor3d.image.Image;
import com.ardor3d.image.Texture;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Matrix4;
import com.ardor3d.math.Transform;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.InterleavedFormat;
import com.ardor3d.renderer.NormalsMode;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.queue.RenderQueue;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.ClipState;
import com.ardor3d.renderer.state.ColorMaskState;
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.renderer.state.FogState;
import com.ardor3d.renderer.state.FragmentProgramState;
import com.ardor3d.renderer.state.GLSLShaderObjectsState;
import com.ardor3d.renderer.state.LightState;
import com.ardor3d.renderer.state.MaterialState;
import com.ardor3d.renderer.state.OffsetState;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.ShadingState;
import com.ardor3d.renderer.state.StencilState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.VertexProgramState;
import com.ardor3d.renderer.state.WireframeState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.record.LineRecord;
import com.ardor3d.renderer.state.record.RendererRecord;
import com.ardor3d.scene.state.jogl.JoglBlendStateUtil;
import com.ardor3d.scene.state.jogl.JoglClipStateUtil;
import com.ardor3d.scene.state.jogl.JoglColorMaskStateUtil;
import com.ardor3d.scene.state.jogl.JoglCullStateUtil;
import com.ardor3d.scene.state.jogl.JoglFogStateUtil;
import com.ardor3d.scene.state.jogl.JoglFragmentProgramStateUtil;
import com.ardor3d.scene.state.jogl.JoglLightStateUtil;
import com.ardor3d.scene.state.jogl.JoglMaterialStateUtil;
import com.ardor3d.scene.state.jogl.JoglOffsetStateUtil;
import com.ardor3d.scene.state.jogl.JoglShaderObjectsStateUtil;
import com.ardor3d.scene.state.jogl.JoglShadingStateUtil;
import com.ardor3d.scene.state.jogl.JoglStencilStateUtil;
import com.ardor3d.scene.state.jogl.JoglTextureStateUtil;
import com.ardor3d.scene.state.jogl.JoglVertexProgramStateUtil;
import com.ardor3d.scene.state.jogl.JoglWireframeStateUtil;
import com.ardor3d.scene.state.jogl.JoglZBufferStateUtil;
import com.ardor3d.scene.state.jogl.util.JoglRendererUtil;
import com.ardor3d.scene.state.jogl.util.JoglTextureUtil;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Renderable;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.TexCoords;
import com.ardor3d.scenegraph.VBOInfo;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.Ardor3dException;
import com.ardor3d.util.Debug;
import com.ardor3d.util.WeakIdentityCache;
import com.ardor3d.util.geom.BufferUtils;
import com.ardor3d.util.stat.StatCollector;
import com.ardor3d.util.stat.StatType;

/**
 * <code>JoglRenderer</code> provides an implementation of the <code>Renderer</code> interface using the JOGL API.
 * 
 * @see com.ardor3d.renderer.Renderer
 */
public class JoglRenderer extends Renderer {
    private static final Logger logger = Logger.getLogger(JoglRenderer.class.getName());

    private JoglFont _font;

    private boolean _inOrthoMode;

    private FloatBuffer _oldVertexBuffer;

    private FloatBuffer _oldNormalBuffer;

    private FloatBuffer _oldColorBuffer;

    private FloatBuffer _oldInterleavedBuffer;

    private final FloatBuffer[] _oldTextureBuffers;

    private int _prevNormMode = GL.GL_ZERO;

    private int _prevTextureNumber = 0;

    protected WeakIdentityCache<Buffer, Integer> _vboMap = new WeakIdentityCache<Buffer, Integer>();

    private final DoubleBuffer _transformBuffer = BufferUtils.createDoubleBuffer(16);
    {
        _transformBuffer.position(15);
        _transformBuffer.put(1.0);
    }

    private final Matrix4 _transformMatrix = new Matrix4();

    private final IntBuffer _idBuff = BufferUtils.createIntBuffer(16);

    /** List of default rendering states for this specific renderer type */
    protected static final EnumMap<RenderState.StateType, RenderState> defaultStateList = new EnumMap<RenderState.StateType, RenderState>(
            RenderState.StateType.class);

    /**
     * Constructor instantiates a new <code>JoglRenderer</code> object. The size of the rendering window is passed
     * during construction.
     */
    public JoglRenderer() {
        logger.fine("JoglRenderer created.");

        _queue = new RenderQueue(this);
        _oldTextureBuffers = new FloatBuffer[TextureState.MAX_TEXTURES];

        // Create our defaults as needed.
        synchronized (defaultStateList) {
            if (defaultStateList.size() == 0) {
                for (final RenderState.StateType type : RenderState.StateType.values()) {
                    final RenderState state = RenderState.createState(type);
                    state.setEnabled(false);
                    defaultStateList.put(type, state);
                }
            }
        }
    }

    @Override
    public void setBackgroundColor(final ReadOnlyColorRGBA c) {
        final GL gl = GLU.getCurrentGL();

        _backgroundColor.set(c);
        gl.glClearColor(_backgroundColor.getRed(), _backgroundColor.getGreen(), _backgroundColor.getBlue(),
                _backgroundColor.getAlpha());
    }

    @Override
    public void clearZBuffer() {
        final GL gl = GLU.getCurrentGL();

        if (defaultStateList.containsKey(RenderState.StateType.ZBuffer)) {
            applyState(defaultStateList.get(RenderState.StateType.ZBuffer));
        }
        gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
    }

    @Override
    public void clearColorBuffer() {
        final GL gl = GLU.getCurrentGL();

        gl.glClear(GL.GL_COLOR_BUFFER_BIT);
    }

    @Override
    public void clearStencilBuffer() {
        final GL gl = GLU.getCurrentGL();

        // grab our camera to get width and height info.
        final Camera cam = ContextManager.getCurrentContext().getCurrentCamera();

        // Clear the stencil buffer
        gl.glClearStencil(0);
        gl.glStencilMask(~0);
        gl.glDisable(GL.GL_DITHER);
        gl.glEnable(GL.GL_SCISSOR_TEST);
        gl.glScissor(0, 0, cam.getWidth(), cam.getHeight());
        gl.glClear(GL.GL_STENCIL_BUFFER_BIT);
        gl.glDisable(GL.GL_SCISSOR_TEST);
    }

    @Override
    public void clearBuffers() {
        final GL gl = GLU.getCurrentGL();

        // make sure no funny business is going on in the z before clearing.
        if (defaultStateList.containsKey(RenderState.StateType.ZBuffer)) {
            defaultStateList.get(RenderState.StateType.ZBuffer).setNeedsRefresh(true);
            applyState(defaultStateList.get(RenderState.StateType.ZBuffer));
        }
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
    }

    @Override
    public void clearStrictBuffers() {
        final GL gl = GLU.getCurrentGL();

        // grab our camera to get width and height info.
        final Camera cam = ContextManager.getCurrentContext().getCurrentCamera();

        gl.glDisable(GL.GL_DITHER);
        gl.glEnable(GL.GL_SCISSOR_TEST);
        gl.glScissor(0, 0, cam.getWidth(), cam.getHeight());
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        gl.glDisable(GL.GL_SCISSOR_TEST);
        gl.glEnable(GL.GL_DITHER);
    }

    @Override
    public void flushFrame(final boolean doSwap) {
        final GL gl = GLU.getCurrentGL();

        renderBuckets();

        reset();

        gl.glFlush();
        if (doSwap) {

            applyState(defaultStateList.get(RenderState.StateType.ColorMask));

            if (Debug.stats) {
                StatCollector.startStat(StatType.STAT_DISPLAYSWAP_TIMER);
            }

            GLContext.getCurrent().getGLDrawable().swapBuffers();
            if (Debug.stats) {
                StatCollector.endStat(StatType.STAT_DISPLAYSWAP_TIMER);
            }
        }

        _vboMap.expunge();

        if (Debug.stats) {
            StatCollector.addStat(StatType.STAT_FRAMES, 1);
        }
    }

    // XXX: look more at this
    public void reset() {
        _oldColorBuffer = _oldNormalBuffer = _oldVertexBuffer = null;
        Arrays.fill(_oldTextureBuffers, null);
    }

    @Override
    public boolean isInOrthoMode() {
        return _inOrthoMode;
    }

    @Override
    public void setOrtho() {
        final GL gl = GLU.getCurrentGL();

        if (_inOrthoMode) {
            throw new Ardor3dException("Already in Orthographic mode.");
        }
        // set up ortho mode
        final RendererRecord matRecord = ContextManager.getCurrentContext().getRendererRecord();
        JoglRendererUtil.switchMode(matRecord, GL.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        final Camera camera = ContextManager.getCurrentContext().getCurrentCamera();
        final double viewportWidth = camera.getWidth() * (camera.getViewPortRight() - camera.getViewPortLeft());
        final double viewportHeight = camera.getHeight() * (camera.getViewPortTop() - camera.getViewPortBottom());
        gl.glOrtho(0, viewportWidth, 0, viewportHeight, -1, 1);
        JoglRendererUtil.switchMode(matRecord, GL.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        _inOrthoMode = true;
    }

    @Override
    public void unsetOrtho() {
        final GL gl = GLU.getCurrentGL();

        if (!_inOrthoMode) {
            throw new Ardor3dException("Not in Orthographic mode.");
        }
        // remove ortho mode, and go back to original
        // state
        final RendererRecord matRecord = ContextManager.getCurrentContext().getRendererRecord();
        JoglRendererUtil.switchMode(matRecord, GL.GL_PROJECTION);
        gl.glPopMatrix();
        JoglRendererUtil.switchMode(matRecord, GL.GL_MODELVIEW);
        gl.glPopMatrix();
        _inOrthoMode = false;
    }

    @Override
    public void grabScreenContents(final ByteBuffer buff, final Image.Format format, final int x, final int y,
            final int w, final int h) {
        final GL gl = GLU.getCurrentGL();

        final int pixFormat = JoglTextureUtil.getGLPixelFormat(format);
        gl.glReadPixels(x, y, w, h, pixFormat, GL.GL_UNSIGNED_BYTE, buff);
    }

    @Override
    public void draw(final Spatial s) {
        if (s != null) {
            s.onDraw(this);
        }
    }

    @Override
    public void draw(final BasicText t) {
        if (_font == null) {
            _font = new JoglFont();
        }
        _font.setColor(t.getTextColor());
        applyStates(t._getWorldRenderStates());
        if (Debug.stats) {
            StatCollector.startStat(StatType.STAT_RENDER_TIMER);
        }

        final ReadOnlyVector3 scale = t.getWorldScale();
        _font.print(this, t.getTranslation().getX(), t.getTranslation().getY(), scale, t.getText(), 0);

        if (Debug.stats) {
            StatCollector.endStat(StatType.STAT_RENDER_TIMER);
        }
    }

    @Override
    public boolean checkAndAdd(final Spatial s) {
        final RenderBucketType rqMode = s.getRenderBucketType();
        if (rqMode != RenderBucketType.Skip) {
            getQueue().addToQueue(s, rqMode);
            return true;
        }
        return false;
    }

    /**
     * re-initializes the GL context for rendering of another piece of geometry.
     */
    protected void postdrawGeometry(final Mesh g) {
    // Nothing to do here yet
    }

    @Override
    public void flushGraphics() {
        final GL gl = GLU.getCurrentGL();

        gl.glFlush();
    }

    @Override
    public void finishGraphics() {
        final GL gl = GLU.getCurrentGL();

        gl.glFinish();
    }

    private void applyNormalMode(final NormalsMode normMode, final Transform worldTransform) {
        final GL gl = GLU.getCurrentGL();
        final RenderContext context = ContextManager.getCurrentContext();
        final ContextCapabilities caps = context.getCapabilities();

        switch (normMode) {
            case NormalizeIfScaled:
                final ReadOnlyVector3 scale = worldTransform.getScale();
                if (!(scale.getX() == 1.0 && scale.getY() == 1.0 && scale.getZ() == 1.0)) {
                    if (scale.getX() == scale.getY() && scale.getY() == scale.getZ() && caps.isOpenGL1_2Supported()
                            && _prevNormMode != GL.GL_RESCALE_NORMAL) {
                        if (_prevNormMode == GL.GL_NORMALIZE) {
                            gl.glDisable(GL.GL_NORMALIZE);
                        }
                        gl.glEnable(GL.GL_RESCALE_NORMAL);
                        _prevNormMode = GL.GL_RESCALE_NORMAL;
                    } else if (_prevNormMode != GL.GL_NORMALIZE) {
                        if (_prevNormMode == GL.GL_RESCALE_NORMAL) {
                            gl.glDisable(GL.GL_RESCALE_NORMAL);
                        }
                        gl.glEnable(GL.GL_NORMALIZE);
                        _prevNormMode = GL.GL_NORMALIZE;
                    }
                } else {
                    if (_prevNormMode == GL.GL_RESCALE_NORMAL) {
                        gl.glDisable(GL.GL_RESCALE_NORMAL);
                        _prevNormMode = GL.GL_ZERO;
                    } else if (_prevNormMode == GL.GL_NORMALIZE) {
                        gl.glDisable(GL.GL_NORMALIZE);
                        _prevNormMode = GL.GL_ZERO;
                    }
                }
                break;
            case AlwaysNormalize:
                if (_prevNormMode != GL.GL_NORMALIZE) {
                    if (_prevNormMode == GL.GL_RESCALE_NORMAL) {
                        gl.glDisable(GL.GL_RESCALE_NORMAL);
                    }
                    gl.glEnable(GL.GL_NORMALIZE);
                    _prevNormMode = GL.GL_NORMALIZE;
                }
                break;
            case UseProvided:
            default:
                if (_prevNormMode == GL.GL_RESCALE_NORMAL) {
                    gl.glDisable(GL.GL_RESCALE_NORMAL);
                    _prevNormMode = GL.GL_ZERO;
                } else if (_prevNormMode == GL.GL_NORMALIZE) {
                    gl.glDisable(GL.GL_NORMALIZE);
                    _prevNormMode = GL.GL_ZERO;
                }
                break;
        }
    }

    @Override
    public void deleteVBO(final Buffer buffer) {
        final Integer i = removeFromVBOCache(buffer);
        if (i != null) {
            deleteVBO(i.intValue());
        }
    }

    @Override
    public void deleteVBO(final int vboid) {
        if (vboid < 1) {
            return;
        }
        final RendererRecord rendRecord = ContextManager.getCurrentContext().getRendererRecord();
        deleteVBOId(rendRecord, vboid);
    }

    @Override
    public void clearVBOCache() {
        _vboMap.clear();
    }

    @Override
    public Integer removeFromVBOCache(final Buffer buffer) {
        return _vboMap.remove(buffer);

    }

    @Override
    public void applyStates(final EnumMap<StateType, RenderState> states) {
        if (Debug.stats) {
            StatCollector.startStat(StatType.STAT_STATES_TIMER);
        }

        final RenderContext context = ContextManager.getCurrentContext();

        RenderState tempState = null;
        for (final StateType type : StateType.values()) {
            // first look up in enforced states
            tempState = context.getEnforcedState(type);

            // Not there? Look in the states we receive
            if (tempState == null) {
                tempState = states.get(type);
            }

            // Still missing? Use our default states.
            if (tempState == null) {
                tempState = defaultStateList.get(type);
            }

            if (!RenderState._quickCompare.contains(type) || tempState.needsRefresh()
                    || tempState != context.getCurrentState(type)) {
                applyState(tempState);
                tempState.setNeedsRefresh(false);
            }
        }

        if (Debug.stats) {
            StatCollector.endStat(StatType.STAT_STATES_TIMER);
        }
    }

    @Override
    public void updateTextureSubImage(final Texture dstTexture, final int dstX, final int dstY, final Image srcImage,
            final int srcX, final int srcY, final int width, final int height) throws Ardor3dException {
        final GL gl = GLU.getCurrentGL();

        // Check that the texture type is supported.
        if (dstTexture.getType() != Texture.Type.TwoDimensional) {
            throw new UnsupportedOperationException("Unsupported Texture Type: " + dstTexture.getType());
        }

        // Determine the original texture configuration, so that this method can
        // restore the texture configuration to its original state.
        final int origTexBinding[] = new int[1];
        gl.glGetIntegerv(GL.GL_TEXTURE_BINDING_2D, origTexBinding, 0);
        final int origAlignment[] = new int[1];
        gl.glGetIntegerv(GL.GL_UNPACK_ALIGNMENT, origAlignment, 0);
        final int origRowLength = 0;
        final int origSkipPixels = 0;
        final int origSkipRows = 0;

        final int alignment = 1;
        int rowLength;
        if (srcImage.getWidth() == width) {
            // When the row length is zero, then the width parameter is used.
            // We use zero in these cases in the hope that we can avoid two
            // unnecessary calls to glPixelStorei.
            rowLength = 0;
        } else {
            // The number of pixels in a row is different than the number of
            // pixels in the region to be uploaded to the texture.
            rowLength = srcImage.getWidth();
        }
        // Consider moving these conversion methods.
        final int pixelFormat = JoglTextureUtil.getGLPixelFormat(srcImage.getFormat());
        final ByteBuffer data = srcImage.getData(0);
        data.rewind();

        // Update the texture configuration (when necessary).
        if (origTexBinding[0] != dstTexture.getTextureId()) {
            gl.glBindTexture(GL.GL_TEXTURE_2D, dstTexture.getTextureId());
        }
        if (origAlignment[0] != alignment) {
            gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, alignment);
        }
        if (origRowLength != rowLength) {
            gl.glPixelStorei(GL.GL_UNPACK_ROW_LENGTH, rowLength);
        }
        if (origSkipPixels != srcX) {
            gl.glPixelStorei(GL.GL_UNPACK_SKIP_PIXELS, srcX);
        }
        if (origSkipRows != srcY) {
            gl.glPixelStorei(GL.GL_UNPACK_SKIP_ROWS, srcY);
        }

        // Upload the image region into the texture.
        gl.glTexSubImage2D(GL.GL_TEXTURE_2D, 0, dstX, dstY, width, height, pixelFormat, GL.GL_UNSIGNED_BYTE, data);

        // Restore the texture configuration (when necessary).
        // Restore the texture binding.
        if (origTexBinding[0] != dstTexture.getTextureId()) {
            gl.glBindTexture(GL.GL_TEXTURE_2D, origTexBinding[0]);
        }
        // Restore alignment.
        if (origAlignment[0] != alignment) {
            gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, origAlignment[0]);
        }
        // Restore row length.
        if (origRowLength != rowLength) {
            gl.glPixelStorei(GL.GL_UNPACK_ROW_LENGTH, origRowLength);
        }
        // Restore skip pixels.
        if (origSkipPixels != srcX) {
            gl.glPixelStorei(GL.GL_UNPACK_SKIP_PIXELS, origSkipPixels);
        }
        // Restore skip rows.
        if (origSkipRows != srcY) {
            gl.glPixelStorei(GL.GL_UNPACK_SKIP_ROWS, origSkipRows);
        }
    }

    @Override
    public void checkCardError() throws Ardor3dException {
        final GL gl = GLU.getCurrentGL();
        final GLU glu = new GLU();

        try {
            final int errorCode = gl.glGetError();
            if (errorCode != GL.GL_NO_ERROR) {
                throw new GLException(glu.gluErrorString(errorCode));
            }
        } catch (final GLException exception) {
            throw new Ardor3dException("Error in opengl: " + exception.getMessage(), exception);
        }
    }

    @Override
    public void cleanup() {
        // clear vbos
        final RendererRecord rendRecord = ContextManager.getCurrentContext().getRendererRecord();
        cleanupVBOs(rendRecord);
        if (_font != null) {
            _font.deleteFont();
            _font = null;
        }
    }

    @Override
    public void draw(final Renderable renderable) {
        renderable.render(this);
    }

    @Override
    public void setupVertexData(final FloatBuffer vertexBuffer, final VBOInfo vbo) {
        final GL gl = GLU.getCurrentGL();

        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord rendRecord = context.getRendererRecord();
        final ContextCapabilities caps = context.getCapabilities();

        if (vertexBuffer != null && caps.isVBOSupported() && vbo != null && vbo.isVBOVertexEnabled()
                && vbo.getVBOVertexID() <= 0) {
            Object vboid;
            if ((vboid = _vboMap.get(vertexBuffer)) != null) {
                vbo.setVBOVertexID(((Integer) vboid).intValue());
            } else {
                vertexBuffer.rewind();
                final int vboID = makeVBOId(rendRecord);
                vbo.setVBOVertexID(vboID);
                _vboMap.put(vertexBuffer, vboID);

                rendRecord.invalidateVBO();
                JoglRendererUtil.setBoundVBO(rendRecord, vbo.getVBOVertexID());
                gl.glBindBufferARB(GL.GL_ARRAY_BUFFER_ARB, vbo.getVBOVertexID());
                gl.glBufferDataARB(GL.GL_ARRAY_BUFFER_ARB, vertexBuffer.limit() * 4, vertexBuffer,
                        GL.GL_STATIC_DRAW_ARB); // TODO Check <sizeInBytes>
            }
        }

        if (caps.isVBOSupported() && vbo != null && vbo.getVBOVertexID() > 0) {
            gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
            JoglRendererUtil.setBoundVBO(rendRecord, vbo.getVBOVertexID());
            gl.glVertexPointer(3, GL.GL_FLOAT, 0, 0);
        } else if (vertexBuffer == null) {
            gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
        } else if (_oldVertexBuffer != vertexBuffer) {
            gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
            if (caps.isVBOSupported()) {
                JoglRendererUtil.setBoundVBO(rendRecord, 0);
            }
            vertexBuffer.rewind();
            gl.glVertexPointer(3, GL.GL_FLOAT, 0, vertexBuffer);
        }
        _oldVertexBuffer = vertexBuffer;
    }

    @Override
    public void setupNormalData(final FloatBuffer normalBuffer, final NormalsMode normalMode,
            final Transform worldTransform, final VBOInfo vbo) {
        final GL gl = GLU.getCurrentGL();

        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord rendRecord = context.getRendererRecord();
        final ContextCapabilities caps = context.getCapabilities();

        if (normalMode != NormalsMode.Off) {
            applyNormalMode(normalMode, worldTransform);

            if ((caps.isVBOSupported() && vbo != null && vbo.getVBONormalID() > 0)) {
                gl.glEnableClientState(GL.GL_NORMAL_ARRAY);
                JoglRendererUtil.setBoundVBO(rendRecord, vbo.getVBONormalID());
                gl.glNormalPointer(GL.GL_FLOAT, 0, 0);
            } else if (normalBuffer == null) {
                gl.glDisableClientState(GL.GL_NORMAL_ARRAY);
            } else if (_oldNormalBuffer != normalBuffer) {
                gl.glEnableClientState(GL.GL_NORMAL_ARRAY);
                if (caps.isVBOSupported()) {
                    JoglRendererUtil.setBoundVBO(rendRecord, 0);
                }
                normalBuffer.rewind();
                gl.glNormalPointer(GL.GL_FLOAT, 0, normalBuffer); // TODO Check assumed <type> GL_FLOAT
            }
            _oldNormalBuffer = normalBuffer;
        } else {
            if (_prevNormMode == GL.GL_RESCALE_NORMAL) {
                gl.glDisable(GL.GL_RESCALE_NORMAL);
                _prevNormMode = GL.GL_ZERO;
            } else if (_prevNormMode == GL.GL_NORMALIZE) {
                gl.glDisable(GL.GL_NORMALIZE);
                _prevNormMode = GL.GL_ZERO;
            }
            gl.glDisableClientState(GL.GL_NORMAL_ARRAY);
            _oldNormalBuffer = null;
        }
    }

    @Override
    public void setupColorData(final FloatBuffer colorBuffer, final VBOInfo vbo, final ColorRGBA defaultColor) {
        final GL gl = GLU.getCurrentGL();

        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord rendRecord = context.getRendererRecord();
        final ContextCapabilities caps = context.getCapabilities();

        if ((caps.isVBOSupported() && vbo != null && vbo.getVBOColorID() > 0)) {
            gl.glEnableClientState(GL.GL_COLOR_ARRAY);
            JoglRendererUtil.setBoundVBO(rendRecord, vbo.getVBOColorID());
            gl.glColorPointer(4, GL.GL_FLOAT, 0, 0);
        } else if (colorBuffer == null) {
            gl.glDisableClientState(GL.GL_COLOR_ARRAY);

            if (defaultColor != null) {
                gl.glColor4f(defaultColor.getRed(), defaultColor.getGreen(), defaultColor.getBlue(), defaultColor
                        .getAlpha());
            } else {
                gl.glColor4f(1, 1, 1, 1);
            }
        } else if (_oldColorBuffer != colorBuffer) {
            gl.glEnableClientState(GL.GL_COLOR_ARRAY);
            if (caps.isVBOSupported()) {
                JoglRendererUtil.setBoundVBO(rendRecord, 0);
            }
            colorBuffer.rewind();
            gl.glColorPointer(4, GL.GL_FLOAT, 0, colorBuffer); // TODO Check assumed <type> GL_FLOAT
        }

        _oldColorBuffer = colorBuffer;
    }

    @Override
    public void setupInterleavedData(final FloatBuffer interleavedBuffer, final InterleavedFormat format,
            final VBOInfo vbo) {
        final GL gl = GLU.getCurrentGL();

        if (_oldInterleavedBuffer != interleavedBuffer) {
            interleavedBuffer.rewind();

            final int glFormat = getGLInterleavedFormat(format);

            gl.glInterleavedArrays(glFormat, 0, interleavedBuffer);
        }
        _oldInterleavedBuffer = interleavedBuffer;
    }

    @Override
    public void setupFogData(final FloatBuffer fogBuffer, final VBOInfo vbo) {
    // final RenderContext context = ContextManager.getCurrentContext();
    // final RendererRecord rendRecord = (RendererRecord) context.getRendererRecord();

    // if (supportsFogCoords) {
    // oldLimit = -1;
    // final FloatBuffer fogCoords = g.getFogBuffer();
    // if (fogCoords != null) {
    // oldLimit = fogCoords.limit();
    // // make sure only the necessary verts are sent through on old cards.
    // fogCoords.limit(g.getVertexQuantity());
    // }
    // if ((caps.isVBOSupported() && vbo != null && vbo.getVBOVertexID() > 0)) { // use
    // // VBO
    // GL.glEnableClientState(EXTFogCoord.GL_FOG_COORDINATE_ARRAY_EXT);
    // rendRecord.setBoundVBO(vbo.getVBOVertexID());
    // EXTFogCoord.glFogCoordPointerEXT(GL.GL_FLOAT, 0, 0);
    // } else if (fogCoords == null) {
    // GL.glDisableClientState(EXTFogCoord.GL_FOG_COORDINATE_ARRAY_EXT);
    // } else if (_oldFogBuffer != fogCoords) {
    // // fog coords have changed
    // GL.glEnableClientState(EXTFogCoord.GL_FOG_COORDINATE_ARRAY_EXT);
    // // ensure no VBO is bound
    // if (caps.isVBOSupported()) {
    // rendRecord.setBoundVBO(0);
    // }
    // fogCoords.rewind();
    // EXTFogCoord.glFogCoordPointerEXT(0, g.getFogBuffer());
    // }
    // if (oldLimit != -1) {
    // fogCoords.limit(oldLimit);
    // }
    // _oldFogBuffer = fogCoords;
    // }
    }

    @Override
    public void setupTextureData(final List<TexCoords> textureCoords, final VBOInfo vbo) {
        final GL gl = GLU.getCurrentGL();

        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord rendRecord = context.getRendererRecord();
        final ContextCapabilities caps = context.getCapabilities();

        final TextureState ts = (TextureState) context.getCurrentState(RenderState.StateType.Texture);
        int offset = 0;
        if (ts != null) {
            offset = ts.getTextureCoordinateOffset();

            for (int i = 0; i < ts.getNumberOfSetTextures() && i < caps.getNumberOfFragmentTexCoordUnits(); i++) {
                if (caps.isMultitextureSupported()) {
                    gl.glClientActiveTexture(GL.GL_TEXTURE0 + i);
                }

                if (i >= textureCoords.size()) {
                    gl.glDisableClientState(GL.GL_TEXTURE_COORD_ARRAY);
                    continue;
                }

                final TexCoords texC = textureCoords.get(i + offset);
                if ((caps.isVBOSupported() && vbo != null && vbo.getVBOTextureID(i) > 0)) {
                    gl.glEnableClientState(GL.GL_TEXTURE_COORD_ARRAY);
                    JoglRendererUtil.setBoundVBO(rendRecord, vbo.getVBOTextureID(i));
                    gl.glTexCoordPointer(texC._perVert, GL.GL_FLOAT, 0, 0);
                } else if (texC == null) {
                    gl.glDisableClientState(GL.GL_TEXTURE_COORD_ARRAY);
                } else if (_oldTextureBuffers[i] != texC._coords) {
                    gl.glEnableClientState(GL.GL_TEXTURE_COORD_ARRAY);
                    if (caps.isVBOSupported()) {
                        JoglRendererUtil.setBoundVBO(rendRecord, 0);
                    }
                    texC._coords.rewind();
                    gl.glTexCoordPointer(texC._perVert, GL.GL_FLOAT, 0, texC._coords);
                } else {
                    gl.glEnableClientState(GL.GL_TEXTURE_COORD_ARRAY);
                }
                _oldTextureBuffers[i] = texC != null ? texC._coords : null;
            }

            if (ts.getNumberOfSetTextures() < _prevTextureNumber) {
                for (int i = ts.getNumberOfSetTextures(); i < _prevTextureNumber; i++) {
                    if (caps.isMultitextureSupported()) {
                        gl.glClientActiveTexture(GL.GL_TEXTURE0 + i);
                    }
                    gl.glDisableClientState(GL.GL_TEXTURE_COORD_ARRAY);
                }
            }

            _prevTextureNumber = ts.getNumberOfSetTextures() < caps.getNumberOfFixedTextureUnits() ? ts
                    .getNumberOfSetTextures() : caps.getNumberOfFixedTextureUnits();
        }
    }

    @Override
    public boolean doTransforms(final Transform transform) {
        final GL gl = GLU.getCurrentGL();

        // set world matrix
        if (!transform.isIdentity()) {
            synchronized (_transformMatrix) {
                transform.getGLApplyMatrix(_transformBuffer);

                final RendererRecord matRecord = ContextManager.getCurrentContext().getRendererRecord();
                JoglRendererUtil.switchMode(matRecord, GL.GL_MODELVIEW);
                gl.glPushMatrix();
                gl.glMultMatrixd(_transformBuffer);
                return true;
            }
        }
        return false;
    }

    @Override
    public void undoTransforms(final Transform transform) {
        final GL gl = GLU.getCurrentGL();

        final RendererRecord matRecord = ContextManager.getCurrentContext().getRendererRecord();
        JoglRendererUtil.switchMode(matRecord, GL.GL_MODELVIEW);
        gl.glPopMatrix();
    }

    @Override
    public void drawElements(final IntBuffer indices, final VBOInfo vbo, final int[] indexLengths,
            final IndexMode[] indexModes) {
        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord rendRecord = context.getRendererRecord();
        final ContextCapabilities caps = context.getCapabilities();

        final GL gl = GLU.getCurrentGL();

        boolean useIndicesVBO = false;
        if ((caps.isVBOSupported() && vbo != null && vbo.getVBOIndexID() > 0)) {
            useIndicesVBO = true;
            JoglRendererUtil.setBoundElementVBO(rendRecord, vbo.getVBOIndexID());
        } else if (caps.isVBOSupported()) {
            JoglRendererUtil.setBoundElementVBO(rendRecord, 0);
        }

        if (indexLengths == null) {
            final int glIndexMode = getGLIndexMode(indexModes[0]);

            if (!useIndicesVBO) {
                indices.position(0);
                gl.glDrawElements(glIndexMode, indices.limit(), GL.GL_UNSIGNED_INT, indices);
            } else {
                gl.glDrawElements(glIndexMode, indices.limit(), GL.GL_UNSIGNED_INT, 0);
            }
        } else {
            int offset = 0;
            int indexModeCounter = 0;
            for (int i = 0; i < indexLengths.length; i++) {
                final int count = indexLengths[i];

                final int glIndexMode = getGLIndexMode(indexModes[indexModeCounter]);

                if (!useIndicesVBO) {
                    indices.position(offset);
                    indices.limit(offset + count);
                    gl.glDrawElements(glIndexMode, count, GL.GL_UNSIGNED_INT, indices);
                } else {
                    gl.glDrawElements(glIndexMode, count, GL.GL_UNSIGNED_INT, offset);
                }

                offset += count;

                if (indexModeCounter < indexModes.length - 1) {
                    indexModeCounter++;
                }
            }
        }
    }

    @Override
    public void drawArrays(final FloatBuffer vertexBuffer, final int[] indexLengths, final IndexMode[] indexModes) {
        final GL gl = GLU.getCurrentGL();

        if (indexLengths == null) {
            final int glIndexMode = getGLIndexMode(indexModes[0]);

            gl.glDrawArrays(glIndexMode, 0, vertexBuffer.limit() / 3);
        } else {
            int offset = 0;
            int indexModeCounter = 0;
            for (int i = 0; i < indexLengths.length; i++) {
                final int count = indexLengths[i];

                final int glIndexMode = getGLIndexMode(indexModes[indexModeCounter]);

                gl.glDrawArrays(glIndexMode, offset, count);

                offset += count;

                if (indexModeCounter < indexModes.length - 1) {
                    indexModeCounter++;
                }
            }
        }
    }

    public int makeVBOId(final RendererRecord rendRecord) {
        final GL gl = GLU.getCurrentGL();

        _idBuff.rewind();
        gl.glGenBuffersARB(_idBuff.limit(), _idBuff); // TODO Check <size>
        final int vboID = _idBuff.get(0);
        rendRecord.getVboCleanupCache().add(vboID);
        return vboID;
    }

    public void deleteVBOId(final RendererRecord rendRecord, final int id) {
        final GL gl = GLU.getCurrentGL();

        _idBuff.rewind();
        _idBuff.put(id).flip();
        gl.glDeleteBuffersARB(_idBuff.limit(), _idBuff); // TODO Check <size>
        rendRecord.getVboCleanupCache().remove(Integer.valueOf(id));
    }

    public void cleanupVBOs(final RendererRecord rendRecord) {
        final List<Integer> vboCleanupCache = rendRecord.getVboCleanupCache();
        for (int x = vboCleanupCache.size(); --x >= 0;) {
            deleteVBOId(rendRecord, vboCleanupCache.get(x));
        }
        vboCleanupCache.clear();
    }

    @Override
    public void renderDisplayList(final int displayListID) {
        final GL gl = GLU.getCurrentGL();

        gl.glCallList(displayListID);

        // invalidate "current arrays"
        reset();
    }

    private int getGLIndexMode(final IndexMode indexMode) {
        int glMode = GL.GL_TRIANGLES;
        switch (indexMode) {
            case Triangles:
                glMode = GL.GL_TRIANGLES;
                break;
            case TriangleStrip:
                glMode = GL.GL_TRIANGLE_STRIP;
                break;
            case TriangleFan:
                glMode = GL.GL_TRIANGLE_FAN;
                break;
            case Quads:
                glMode = GL.GL_QUADS;
                break;
            case QuadStrip:
                glMode = GL.GL_QUAD_STRIP;
                break;
            case Lines:
                glMode = GL.GL_LINES;
                break;
            case LineStrip:
                glMode = GL.GL_LINE_STRIP;
                break;
            case LineLoop:
                glMode = GL.GL_LINE_LOOP;
                break;
            case Points:
                glMode = GL.GL_POINTS;
                break;
            case Polygon:
                glMode = GL.GL_POLYGON;
                break;
        }
        return glMode;
    }

    private int getGLInterleavedFormat(final InterleavedFormat format) {
        int glInterleavedFormat = GL.GL_V3F;
        switch (format) {
            case GL_V2F:
                glInterleavedFormat = GL.GL_V2F;
                break;
            case GL_V3F:
                glInterleavedFormat = GL.GL_V3F;
                break;
            case GL_C3F_V3F:
                glInterleavedFormat = GL.GL_C3F_V3F;
                break;
            case GL_C4F_N3F_V3F:
                glInterleavedFormat = GL.GL_C4F_N3F_V3F;
                break;
            case GL_C4UB_V2F:
                glInterleavedFormat = GL.GL_C4UB_V2F;
                break;
            case GL_C4UB_V3F:
                glInterleavedFormat = GL.GL_C4UB_V3F;
                break;
            case GL_N3F_V3F:
                glInterleavedFormat = GL.GL_N3F_V3F;
                break;
            case GL_T2F_C3F_V3F:
                glInterleavedFormat = GL.GL_T2F_C3F_V3F;
                break;
            case GL_T2F_C4F_N3F_V3F:
                glInterleavedFormat = GL.GL_T2F_C4F_N3F_V3F;
                break;
            case GL_T2F_C4UB_V3F:
                glInterleavedFormat = GL.GL_T2F_C4UB_V3F;
                break;
            case GL_T2F_N3F_V3F:
                glInterleavedFormat = GL.GL_T2F_N3F_V3F;
                break;
            case GL_T2F_V3F:
                glInterleavedFormat = GL.GL_T2F_V3F;
                break;
            case GL_T4F_C4F_N3F_V4F:
                glInterleavedFormat = GL.GL_T4F_C4F_N3F_V4F;
                break;
            case GL_T4F_V4F:
                glInterleavedFormat = GL.GL_T4F_V4F;
                break;
        }
        return glInterleavedFormat;
    }

    @Override
    public void setModelViewMatrix(final DoubleBuffer matrix) {
        final RendererRecord matRecord = ContextManager.getCurrentContext().getRendererRecord();
        JoglRendererUtil.switchMode(matRecord, GL.GL_MODELVIEW);

        GLU.getCurrentGL().glLoadMatrixd(matrix);
    }

    @Override
    public void setProjectionMatrix(final DoubleBuffer matrix) {
        final RendererRecord matRecord = ContextManager.getCurrentContext().getRendererRecord();
        JoglRendererUtil.switchMode(matRecord, GL.GL_PROJECTION);

        GLU.getCurrentGL().glLoadMatrixd(matrix);
    }

    @Override
    public void setViewport(final int x, final int y, final int width, final int height) {
        GLU.getCurrentGL().glViewport(x, y, width, height);
    }

    @Override
    public void setDepthRange(final double depthRangeNear, final double depthRangeFar) {
        GLU.getCurrentGL().glDepthRange(depthRangeNear, depthRangeFar);
    }

    @Override
    public void setupLineParameters(final float lineWidth, final int stippleFactor, final short stipplePattern,
            final boolean antialiased) {
        final GL gl = GLU.getCurrentGL();

        final LineRecord lineRecord = ContextManager.getCurrentContext().getLineRecord();

        if (!lineRecord.isValid() || lineRecord.width != lineWidth) {
            gl.glLineWidth(lineWidth);
            lineRecord.width = lineWidth;
        }

        if (stipplePattern != (short) 0xFFFF) {
            if (!lineRecord.isValid() || !lineRecord.stippled) {
                gl.glEnable(GL.GL_LINE_STIPPLE);
                lineRecord.stippled = true;
            }

            if (!lineRecord.isValid() || stippleFactor != lineRecord.stippleFactor
                    || stipplePattern != lineRecord.stipplePattern) {
                gl.glLineStipple(stippleFactor, stipplePattern);
                lineRecord.stippleFactor = stippleFactor;
                lineRecord.stipplePattern = stipplePattern;
            }
        } else if (!lineRecord.isValid() || lineRecord.stippled) {
            gl.glDisable(GL.GL_LINE_STIPPLE);
            lineRecord.stippled = false;
        }

        if (antialiased) {
            if (!lineRecord.isValid() || !lineRecord.smoothed) {
                gl.glEnable(GL.GL_LINE_SMOOTH);
                lineRecord.smoothed = true;
            }
            if (!lineRecord.isValid() || lineRecord.smoothHint != GL.GL_NICEST) {
                gl.glHint(GL.GL_LINE_SMOOTH_HINT, GL.GL_NICEST);
                lineRecord.smoothHint = GL.GL_NICEST;
            }
        } else if (!lineRecord.isValid() || lineRecord.smoothed) {
            gl.glDisable(GL.GL_LINE_SMOOTH);
            lineRecord.smoothed = false;
        }

        if (!lineRecord.isValid()) {
            lineRecord.validate();
        }
    }

    @Override
    public void setupPointParameters(final float pointSize, final boolean antialiased) {
        final GL gl = GLU.getCurrentGL();

        // TODO: make this into a pointrecord call
        gl.glPointSize(pointSize);
        if (antialiased) {
            gl.glEnable(GL.GL_POINT_SMOOTH);
            gl.glHint(GL.GL_POINT_SMOOTH_HINT, GL.GL_NICEST);
        }
    }

    @Override
    public void applyState(final RenderState state) {
        if (state == null) {
            logger.warning("tried to apply a null state.");
            return;
        }
        switch (state.getType()) {
            case Texture:
                JoglTextureStateUtil.apply(this, (TextureState) state);
                return;
            case Light:
                JoglLightStateUtil.apply(this, (LightState) state);
                return;
            case Blend:
                JoglBlendStateUtil.apply(this, (BlendState) state);
                return;
            case Clip:
                JoglClipStateUtil.apply(this, (ClipState) state);
                return;
            case ColorMask:
                JoglColorMaskStateUtil.apply(this, (ColorMaskState) state);
                return;
            case Cull:
                JoglCullStateUtil.apply(this, (CullState) state);
                return;
            case Fog:
                JoglFogStateUtil.apply(this, (FogState) state);
                return;
            case FragmentProgram:
                JoglFragmentProgramStateUtil.apply(this, (FragmentProgramState) state);
                return;
            case GLSLShader:
                JoglShaderObjectsStateUtil.apply(this, (GLSLShaderObjectsState) state);
                return;
            case Material:
                JoglMaterialStateUtil.apply(this, (MaterialState) state);
                return;
            case Offset:
                JoglOffsetStateUtil.apply(this, (OffsetState) state);
                return;
            case Shading:
                JoglShadingStateUtil.apply(this, (ShadingState) state);
                return;
            case Stencil:
                JoglStencilStateUtil.apply(this, (StencilState) state);
                return;
            case VertexProgram:
                JoglVertexProgramStateUtil.apply(this, (VertexProgramState) state);
                return;
            case Wireframe:
                JoglWireframeStateUtil.apply(this, (WireframeState) state);
                return;
            case ZBuffer:
                JoglZBufferStateUtil.apply(this, (ZBufferState) state);
                return;
        }
        throw new IllegalArgumentException("Unknown state: " + state);
    }

    @Override
    public void deleteTextureId(final int textureId) {
        JoglTextureStateUtil.deleteTextureId(textureId);
    }

    @Override
    public void loadTexture(final Texture texture, final int unit) {
        JoglTextureStateUtil.load(texture, unit);
    }
}
