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
import java.util.List;
import java.util.logging.Logger;

import javax.media.opengl.GL;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLException;
import javax.media.opengl.glu.GLU;

import com.ardor3d.image.Image;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Image.Format;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Matrix4;
import com.ardor3d.math.Transform;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.AbstractRenderer;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.DrawBufferTarget;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.InterleavedFormat;
import com.ardor3d.renderer.NormalsMode;
import com.ardor3d.renderer.RenderContext;
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
import com.ardor3d.scenegraph.FloatBufferData;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Renderable;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.VBOInfo;
import com.ardor3d.util.Ardor3dException;
import com.ardor3d.util.Constants;
import com.ardor3d.util.WeakIdentityCache;
import com.ardor3d.util.geom.BufferUtils;
import com.ardor3d.util.stat.StatCollector;
import com.ardor3d.util.stat.StatType;

/**
 * <code>JoglRenderer</code> provides an implementation of the <code>Renderer</code> interface using the JOGL API.
 * 
 * @see com.ardor3d.renderer.Renderer
 */
public class JoglRenderer extends AbstractRenderer {
    private static final Logger logger = Logger.getLogger(JoglRenderer.class.getName());

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

    private boolean glTexSubImage2DSupported = true;

    /**
     * Constructor instantiates a new <code>JoglRenderer</code> object.
     */
    public JoglRenderer() {
        logger.fine("JoglRenderer created.");

        _queue = new RenderQueue(this);

        _oldTextureBuffers = new FloatBuffer[TextureState.MAX_TEXTURES];
    }

    public void setBackgroundColor(final ReadOnlyColorRGBA c) {
        final GL gl = GLU.getCurrentGL();

        _backgroundColor.set(c);
        gl.glClearColor(_backgroundColor.getRed(), _backgroundColor.getGreen(), _backgroundColor.getBlue(),
                _backgroundColor.getAlpha());
    }

    /**
     * render queue if needed
     */
    public void renderBuckets() {
        _processingQueue = true;
        _queue.renderBuckets();
        _processingQueue = false;
    }

    /**
     * clear the render queue
     */
    public void clearQueue() {
        _queue.clearBuckets();
    }

    public void clearZBuffer() {
        final GL gl = GLU.getCurrentGL();

        if (defaultStateList.containsKey(RenderState.StateType.ZBuffer)) {
            doApplyState(defaultStateList.get(RenderState.StateType.ZBuffer));
        }
        gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
    }

    public void clearColorBuffer() {
        final GL gl = GLU.getCurrentGL();

        gl.glClear(GL.GL_COLOR_BUFFER_BIT);
    }

    public void clearStencilBuffer() {
        final GL gl = GLU.getCurrentGL();

        // grab our camera to get width and height info.
        final Camera cam = Camera.getCurrentCamera();

        // Clear the stencil buffer
        gl.glClearStencil(0);
        gl.glStencilMask(~0);
        gl.glDisable(GL.GL_DITHER);
        gl.glEnable(GL.GL_SCISSOR_TEST);
        gl.glScissor(0, 0, cam.getWidth(), cam.getHeight());
        gl.glClear(GL.GL_STENCIL_BUFFER_BIT);
        gl.glDisable(GL.GL_SCISSOR_TEST);
    }

    public void clearBuffers() {
        final GL gl = GLU.getCurrentGL();

        // make sure no funny business is going on in the z before clearing.
        if (defaultStateList.containsKey(RenderState.StateType.ZBuffer)) {
            defaultStateList.get(RenderState.StateType.ZBuffer).setNeedsRefresh(true);
            doApplyState(defaultStateList.get(RenderState.StateType.ZBuffer));
        }
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
    }

    public void clearStrictBuffers() {
        final GL gl = GLU.getCurrentGL();

        // grab our camera to get width and height info.
        final Camera cam = Camera.getCurrentCamera();

        gl.glDisable(GL.GL_DITHER);
        gl.glEnable(GL.GL_SCISSOR_TEST);
        gl.glScissor(0, 0, cam.getWidth(), cam.getHeight());
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        gl.glDisable(GL.GL_SCISSOR_TEST);
        gl.glEnable(GL.GL_DITHER);
    }

    public void flushFrame(final boolean doSwap) {
        final GL gl = GLU.getCurrentGL();

        renderBuckets();

        reset();

        gl.glFlush();
        if (doSwap) {

            doApplyState(defaultStateList.get(RenderState.StateType.ColorMask));

            if (Constants.stats) {
                StatCollector.startStat(StatType.STAT_DISPLAYSWAP_TIMER);
            }

            GLContext.getCurrent().getGLDrawable().swapBuffers();
            if (Constants.stats) {
                StatCollector.endStat(StatType.STAT_DISPLAYSWAP_TIMER);
            }
        }

        _vboMap.expunge();

        if (Constants.stats) {
            StatCollector.addStat(StatType.STAT_FRAMES, 1);
        }
    }

    // XXX: look more at this
    public void reset() {
        _oldColorBuffer = _oldNormalBuffer = _oldVertexBuffer = null;
        Arrays.fill(_oldTextureBuffers, null);
    }

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
        final Camera camera = Camera.getCurrentCamera();
        final double viewportWidth = camera.getWidth() * (camera.getViewPortRight() - camera.getViewPortLeft());
        final double viewportHeight = camera.getHeight() * (camera.getViewPortTop() - camera.getViewPortBottom());
        gl.glOrtho(0, viewportWidth, 0, viewportHeight, -1, 1);
        JoglRendererUtil.switchMode(matRecord, GL.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        _inOrthoMode = true;
    }

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

    public void grabScreenContents(final ByteBuffer buff, final Image.Format format, final int x, final int y,
            final int w, final int h) {
        final GL gl = GLU.getCurrentGL();

        final int pixFormat = JoglTextureUtil.getGLPixelFormat(format);
        gl.glReadPixels(x, y, w, h, pixFormat, GL.GL_UNSIGNED_BYTE, buff);
    }

    public void draw(final Spatial s) {
        if (s != null) {
            s.onDraw(this);
        }
    }

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

    public void flushGraphics() {
        final GL gl = GLU.getCurrentGL();

        gl.glFlush();
    }

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

    public void deleteVBO(final Buffer buffer) {
        final Integer i = removeFromVBOCache(buffer);
        if (i != null) {
            deleteVBO(i.intValue());
        }
    }

    public void deleteVBO(final int vboid) {
        if (vboid < 1) {
            return;
        }
        final RendererRecord rendRecord = ContextManager.getCurrentContext().getRendererRecord();
        deleteVBOId(rendRecord, vboid);
    }

    public void clearVBOCache() {
        _vboMap.clear();
    }

    public Integer removeFromVBOCache(final Buffer buffer) {
        return _vboMap.remove(buffer);

    }

    public void updateTextureSubImage(final Texture dstTexture, final Image srcImage, final int srcX, final int srcY,
            final int dstX, final int dstY, final int dstWidth, final int dstHeight) throws Ardor3dException,
            UnsupportedOperationException {
        final ByteBuffer data = srcImage.getData(0);
        data.rewind();
        updateTextureSubImage(dstTexture, data, srcX, srcY, srcImage.getWidth(), srcImage.getHeight(), dstX, dstY,
                dstWidth, dstHeight, srcImage.getFormat());
    }

    public void updateTextureSubImage(final Texture dstTexture, final ByteBuffer data, final int srcX, final int srcY,
            final int srcWidth, final int srcHeight, final int dstX, final int dstY, final int dstWidth,
            final int dstHeight, final Format format) throws Ardor3dException, UnsupportedOperationException {
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
        if (srcWidth == dstWidth) {
            // When the row length is zero, then the width parameter is used.
            // We use zero in these cases in the hope that we can avoid two
            // unnecessary calls to glPixelStorei.
            rowLength = 0;
        } else {
            // The number of pixels in a row is different than the number of
            // pixels in the region to be uploaded to the texture.
            rowLength = srcWidth;
        }
        final int pixelFormat = JoglTextureUtil.getGLPixelFormat(format);

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
        if (glTexSubImage2DSupported) {
            gl.glTexSubImage2D(GL.GL_TEXTURE_2D, 0, dstX, dstY, dstWidth, dstHeight, pixelFormat, GL.GL_UNSIGNED_BYTE,
                    data);

            final int errorCode = gl.glGetError();
            if (errorCode != GL.GL_NO_ERROR) {
                glTexSubImage2DSupported = false;
                updateTextureSubImage(dstTexture, data, srcX, srcY, srcWidth, srcHeight, dstX, dstY, dstWidth,
                        dstHeight, format);
            }
        } else {
            final int internalFormat = JoglTextureUtil.getGLInternalFormat(format);
            gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, internalFormat, dstWidth, dstHeight, 0, pixelFormat,
                    GL.GL_UNSIGNED_BYTE, data);
        }

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

    public void cleanup() {
        // clear vbos
        final RendererRecord rendRecord = ContextManager.getCurrentContext().getRendererRecord();
        cleanupVBOs(rendRecord);
    }

    public void draw(final Renderable renderable) {
        renderable.render(this);
    }

    public void setupVertexData(final FloatBufferData vertexBufferData, final VBOInfo vbo) {
        final GL gl = GLU.getCurrentGL();

        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord rendRecord = context.getRendererRecord();
        final ContextCapabilities caps = context.getCapabilities();

        final FloatBuffer vertexBuffer = vertexBufferData != null ? vertexBufferData.getBuffer() : null;

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
            gl.glVertexPointer(vertexBufferData.getCoordsPerVertex(), GL.GL_FLOAT, 0, 0);
        } else if (vertexBuffer == null) {
            gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
        } else if (_oldVertexBuffer != vertexBuffer) {
            gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
            if (caps.isVBOSupported()) {
                JoglRendererUtil.setBoundVBO(rendRecord, 0);
            }
            vertexBuffer.rewind();
            gl.glVertexPointer(vertexBufferData.getCoordsPerVertex(), GL.GL_FLOAT, 0, vertexBuffer);
        }
        _oldVertexBuffer = vertexBuffer;
    }

    public void setupNormalData(final FloatBufferData normalBufferData, final NormalsMode normalMode,
            final Transform worldTransform, final VBOInfo vbo) {
        final GL gl = GLU.getCurrentGL();

        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord rendRecord = context.getRendererRecord();
        final ContextCapabilities caps = context.getCapabilities();

        final FloatBuffer normalBuffer = normalBufferData != null ? normalBufferData.getBuffer() : null;

        if (normalBuffer != null && caps.isVBOSupported() && vbo != null && vbo.isVBONormalEnabled()
                && vbo.getVBONormalID() <= 0) {
            Object vboid;
            if ((vboid = _vboMap.get(normalBuffer)) != null) {
                vbo.setVBONormalID(((Integer) vboid).intValue());
            } else {
                normalBuffer.rewind();
                final int vboID = makeVBOId(rendRecord);
                vbo.setVBONormalID(vboID);
                _vboMap.put(normalBuffer, vboID);

                rendRecord.invalidateVBO();
                JoglRendererUtil.setBoundVBO(rendRecord, vbo.getVBONormalID());
                gl.glBindBufferARB(GL.GL_ARRAY_BUFFER_ARB, vbo.getVBONormalID());
                gl.glBufferDataARB(GL.GL_ARRAY_BUFFER_ARB, normalBuffer.limit() * 4, normalBuffer,
                        GL.GL_STATIC_DRAW_ARB); // TODO Check <sizeInBytes>
            }
        }

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

    public void setupColorData(final FloatBufferData colorBufferData, final VBOInfo vbo, final ColorRGBA defaultColor) {
        final GL gl = GLU.getCurrentGL();

        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord rendRecord = context.getRendererRecord();
        final ContextCapabilities caps = context.getCapabilities();

        final FloatBuffer colorBuffer = colorBufferData != null ? colorBufferData.getBuffer() : null;

        if (colorBuffer != null && caps.isVBOSupported() && vbo != null && vbo.isVBOColorEnabled()
                && vbo.getVBOColorID() <= 0) {
            Object vboid;
            if ((vboid = _vboMap.get(colorBuffer)) != null) {
                vbo.setVBOColorID(((Integer) vboid).intValue());
            } else {
                colorBuffer.rewind();
                final int vboID = makeVBOId(rendRecord);
                vbo.setVBOColorID(vboID);
                _vboMap.put(colorBuffer, vboID);

                rendRecord.invalidateVBO();
                JoglRendererUtil.setBoundVBO(rendRecord, vbo.getVBOColorID());
                gl.glBindBufferARB(GL.GL_ARRAY_BUFFER_ARB, vbo.getVBOColorID());
                gl.glBufferDataARB(GL.GL_ARRAY_BUFFER_ARB, colorBuffer.limit() * 4, colorBuffer, GL.GL_STATIC_DRAW_ARB); // TODO
                // Check
                // <sizeInBytes>
            }
        }

        if ((caps.isVBOSupported() && vbo != null && vbo.getVBOColorID() > 0)) {
            gl.glEnableClientState(GL.GL_COLOR_ARRAY);
            JoglRendererUtil.setBoundVBO(rendRecord, vbo.getVBOColorID());
            gl.glColorPointer(colorBufferData.getCoordsPerVertex(), GL.GL_FLOAT, 0, 0);
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
            gl.glColorPointer(colorBufferData.getCoordsPerVertex(), GL.GL_FLOAT, 0, colorBuffer); // TODO Check assumed
            // <type> GL_FLOAT
        }

        _oldColorBuffer = colorBuffer;
    }

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

    public void setupFogData(final FloatBufferData fogBufferData, final VBOInfo vbo) {
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

    public void setupTextureData(final List<FloatBufferData> textureCoords, final VBOInfo vbo) {
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

                if (textureCoords == null || i >= textureCoords.size()) {
                    gl.glDisableClientState(GL.GL_TEXTURE_COORD_ARRAY);
                    continue;
                }

                final FloatBufferData textureBufferData = textureCoords.get(i + offset);
                final FloatBuffer textureBuffer = textureBufferData != null ? textureBufferData.getBuffer() : null;

                if (textureBuffer != null && caps.isVBOSupported() && vbo != null && vbo.isVBOTextureEnabled()
                        && vbo.getVBOTextureID(i) <= 0) {
                    Object vboid;
                    if ((vboid = _vboMap.get(textureBuffer)) != null) {
                        vbo.setVBOTextureID(i, ((Integer) vboid).intValue());
                    } else {
                        textureBuffer.rewind();
                        final int vboID = makeVBOId(rendRecord);
                        vbo.setVBOTextureID(i, vboID);
                        _vboMap.put(textureBuffer, vboID);

                        rendRecord.invalidateVBO();
                        JoglRendererUtil.setBoundVBO(rendRecord, vbo.getVBOTextureID(i));
                        gl.glBindBufferARB(GL.GL_ARRAY_BUFFER_ARB, vbo.getVBOTextureID(i));
                        gl.glBufferDataARB(GL.GL_ARRAY_BUFFER_ARB, textureBuffer.limit() * 4, textureBuffer,
                                GL.GL_STATIC_DRAW_ARB); // TODO Check <sizeInBytes>
                    }
                }

                if ((caps.isVBOSupported() && vbo != null && vbo.getVBOTextureID(i) > 0)) {
                    gl.glEnableClientState(GL.GL_TEXTURE_COORD_ARRAY);
                    JoglRendererUtil.setBoundVBO(rendRecord, vbo.getVBOTextureID(i));
                    gl.glTexCoordPointer(textureBufferData.getCoordsPerVertex(), GL.GL_FLOAT, 0, 0);
                } else if (textureBufferData == null) {
                    gl.glDisableClientState(GL.GL_TEXTURE_COORD_ARRAY);
                } else if (_oldTextureBuffers[i] != textureBuffer) {
                    gl.glEnableClientState(GL.GL_TEXTURE_COORD_ARRAY);
                    if (caps.isVBOSupported()) {
                        JoglRendererUtil.setBoundVBO(rendRecord, 0);
                    }
                    textureBuffer.rewind();
                    gl.glTexCoordPointer(textureBufferData.getCoordsPerVertex(), GL.GL_FLOAT, 0, textureBuffer);
                } else {
                    gl.glEnableClientState(GL.GL_TEXTURE_COORD_ARRAY);
                }
                _oldTextureBuffers[i] = textureBuffer;
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

    public void undoTransforms(final Transform transform) {
        final GL gl = GLU.getCurrentGL();

        final RendererRecord matRecord = ContextManager.getCurrentContext().getRendererRecord();
        JoglRendererUtil.switchMode(matRecord, GL.GL_MODELVIEW);
        gl.glPopMatrix();
    }

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
            if (Constants.stats) {
                addStats(indexModes[0], indices.limit());
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
                if (Constants.stats) {
                    addStats(indexModes[indexModeCounter], count);
                }

                offset += count;

                if (indexModeCounter < indexModes.length - 1) {
                    indexModeCounter++;
                }
            }
        }
    }

    public void drawArrays(final FloatBuffer vertexBuffer, final int[] indexLengths, final IndexMode[] indexModes) {
        final GL gl = GLU.getCurrentGL();

        if (indexLengths == null) {
            final int glIndexMode = getGLIndexMode(indexModes[0]);

            gl.glDrawArrays(glIndexMode, 0, vertexBuffer.limit() / 3);

            if (Constants.stats) {
                addStats(indexModes[0], vertexBuffer.limit() / 3);
            }
        } else {
            int offset = 0;
            int indexModeCounter = 0;
            for (int i = 0; i < indexLengths.length; i++) {
                final int count = indexLengths[i];

                final int glIndexMode = getGLIndexMode(indexModes[indexModeCounter]);

                gl.glDrawArrays(glIndexMode, offset, count);

                if (Constants.stats) {
                    addStats(indexModes[indexModeCounter], count);
                }

                offset += count;

                if (indexModeCounter < indexModes.length - 1) {
                    indexModeCounter++;
                }
            }
        }
    }

    private void addStats(final IndexMode indexMode, final int count) {
        switch (indexMode) {
            case Triangles:
            case TriangleFan:
            case TriangleStrip:
                StatCollector.addStat(StatType.STAT_TRIANGLE_COUNT, count);
                break;
            case Lines:
            case LineLoop:
            case LineStrip:
                StatCollector.addStat(StatType.STAT_LINE_COUNT, count);
                break;
            case Points:
                StatCollector.addStat(StatType.STAT_POINT_COUNT, count);
                break;
            case Quads:
            case QuadStrip:
                StatCollector.addStat(StatType.STAT_QUAD_COUNT, count);
                break;
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

    public void setModelViewMatrix(final Buffer matrix) {
        final RendererRecord matRecord = ContextManager.getCurrentContext().getRendererRecord();
        JoglRendererUtil.switchMode(matRecord, GL.GL_MODELVIEW);

        loadMatrix(matrix);
    }

    public void setProjectionMatrix(final Buffer matrix) {
        final RendererRecord matRecord = ContextManager.getCurrentContext().getRendererRecord();
        JoglRendererUtil.switchMode(matRecord, GL.GL_PROJECTION);

        loadMatrix(matrix);
    }

    private void loadMatrix(final Buffer matrix) {
        if (matrix instanceof DoubleBuffer) {
            GLU.getCurrentGL().glLoadMatrixd((DoubleBuffer) matrix);
        } else if (matrix instanceof FloatBuffer) {
            GLU.getCurrentGL().glLoadMatrixf((FloatBuffer) matrix);
        }
    }

    public Buffer getModelViewMatrix(final Buffer store) {
        if (store == null || store instanceof DoubleBuffer) {
            return getMatrix(GL.GL_MODELVIEW_MATRIX, (DoubleBuffer) store);
        } else if (store instanceof FloatBuffer) {
            return getMatrix(GL.GL_MODELVIEW_MATRIX, (FloatBuffer) store);
        } else {
            return null;
        }
    }

    public Buffer getProjectionMatrix(final Buffer store) {
        if (store == null || store instanceof DoubleBuffer) {
            return getMatrix(GL.GL_PROJECTION_MATRIX, (DoubleBuffer) store);
        } else if (store instanceof FloatBuffer) {
            return getMatrix(GL.GL_PROJECTION_MATRIX, (FloatBuffer) store);
        } else {
            return null;
        }
    }

    private DoubleBuffer getMatrix(final int matrixType, final DoubleBuffer store) {
        DoubleBuffer result = store;
        if (result == null || result.remaining() < 16) {
            result = BufferUtils.createDoubleBuffer(16);
        }
        GLU.getCurrentGL().glGetDoublev(matrixType, store);
        return result;
    }

    private FloatBuffer getMatrix(final int matrixType, final FloatBuffer store) {
        FloatBuffer result = store;
        if (result.remaining() < 16) {
            result = BufferUtils.createFloatBuffer(16);
        }
        GLU.getCurrentGL().glGetFloatv(matrixType, store);
        return result;
    }

    public void setViewport(final int x, final int y, final int width, final int height) {
        GLU.getCurrentGL().glViewport(x, y, width, height);
    }

    public void setDepthRange(final double depthRangeNear, final double depthRangeFar) {
        GLU.getCurrentGL().glDepthRange(depthRangeNear, depthRangeFar);
    }

    public void setDrawBuffer(final DrawBufferTarget target) {
        final RendererRecord record = ContextManager.getCurrentContext().getRendererRecord();
        if (record.getDrawBufferTarget() != target) {
            int buffer = GL.GL_BACK;
            switch (target) {
                case Back:
                    break;
                case Front:
                    buffer = GL.GL_FRONT;
                    break;
                case BackLeft:
                    buffer = GL.GL_BACK_LEFT;
                    break;
                case BackRight:
                    buffer = GL.GL_BACK_RIGHT;
                    break;
                case FrontLeft:
                    buffer = GL.GL_FRONT_LEFT;
                    break;
                case FrontRight:
                    buffer = GL.GL_FRONT_RIGHT;
                    break;
                case FrontAndBack:
                    buffer = GL.GL_FRONT_AND_BACK;
                    break;
                case Left:
                    buffer = GL.GL_LEFT;
                    break;
                case Right:
                    buffer = GL.GL_RIGHT;
                    break;
                case Aux0:
                    buffer = GL.GL_AUX0;
                    break;
                case Aux1:
                    buffer = GL.GL_AUX1;
                    break;
                case Aux2:
                    buffer = GL.GL_AUX2;
                    break;
                case Aux3:
                    buffer = GL.GL_AUX3;
                    break;
            }

            GLU.getCurrentGL().glDrawBuffer(buffer);
            record.setDrawBufferTarget(target);
        }
    }

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
    protected void doApplyState(final RenderState state) {
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

    public void deleteTextureId(final int textureId) {
        JoglTextureStateUtil.deleteTextureId(textureId);
    }

    public void loadTexture(final Texture texture, final int unit) {
        JoglTextureStateUtil.load(texture, unit);
    }
}
