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

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.lwjgl.opengl.ARBBufferObject;
import org.lwjgl.opengl.ARBMultitexture;
import org.lwjgl.opengl.ARBVertexBufferObject;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.OpenGLException;
import org.lwjgl.opengl.Util;

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
import com.ardor3d.scene.state.lwjgl.LwjglBlendStateUtil;
import com.ardor3d.scene.state.lwjgl.LwjglClipStateUtil;
import com.ardor3d.scene.state.lwjgl.LwjglColorMaskStateUtil;
import com.ardor3d.scene.state.lwjgl.LwjglCullStateUtil;
import com.ardor3d.scene.state.lwjgl.LwjglFogStateUtil;
import com.ardor3d.scene.state.lwjgl.LwjglFragmentProgramStateUtil;
import com.ardor3d.scene.state.lwjgl.LwjglLightStateUtil;
import com.ardor3d.scene.state.lwjgl.LwjglMaterialStateUtil;
import com.ardor3d.scene.state.lwjgl.LwjglOffsetStateUtil;
import com.ardor3d.scene.state.lwjgl.LwjglShaderObjectsStateUtil;
import com.ardor3d.scene.state.lwjgl.LwjglShadingStateUtil;
import com.ardor3d.scene.state.lwjgl.LwjglStencilStateUtil;
import com.ardor3d.scene.state.lwjgl.LwjglTextureStateUtil;
import com.ardor3d.scene.state.lwjgl.LwjglVertexProgramStateUtil;
import com.ardor3d.scene.state.lwjgl.LwjglWireframeStateUtil;
import com.ardor3d.scene.state.lwjgl.LwjglZBufferStateUtil;
import com.ardor3d.scene.state.lwjgl.util.LwjglRendererUtil;
import com.ardor3d.scene.state.lwjgl.util.LwjglTextureUtil;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Renderable;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.TexCoords;
import com.ardor3d.scenegraph.VBOInfo;
import com.ardor3d.util.Ardor3dException;
import com.ardor3d.util.Constants;
import com.ardor3d.util.WeakIdentityCache;
import com.ardor3d.util.geom.BufferUtils;
import com.ardor3d.util.stat.StatCollector;
import com.ardor3d.util.stat.StatType;

/**
 * <code>LwjglRenderer</code> provides an implementation of the <code>Renderer</code> interface using the LWJGL API.
 * 
 * @see com.ardor3d.renderer.Renderer
 */
public class LwjglRenderer extends AbstractRenderer {
    private static final Logger logger = Logger.getLogger(LwjglRenderer.class.getName());

    private boolean _inOrthoMode;

    private FloatBuffer _oldVertexBuffer;

    private FloatBuffer _oldNormalBuffer;

    private FloatBuffer _oldColorBuffer;

    private FloatBuffer _oldInterleavedBuffer;

    private final FloatBuffer[] _oldTextureBuffers;

    private int _prevNormMode = GL11.GL_ZERO;

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
     * Constructor instantiates a new <code>LwjglRenderer</code> object.
     */
    public LwjglRenderer() {

        logger.fine("LwjglRenderer created.");

        _queue = new RenderQueue(this);

        _oldTextureBuffers = new FloatBuffer[TextureState.MAX_TEXTURES];
    }

    public void setBackgroundColor(final ReadOnlyColorRGBA color) {
        _backgroundColor.set(color);
        GL11.glClearColor(_backgroundColor.getRed(), _backgroundColor.getGreen(), _backgroundColor.getBlue(),
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
        if (defaultStateList.containsKey(RenderState.StateType.ZBuffer)) {
            applyState(defaultStateList.get(RenderState.StateType.ZBuffer));
        }
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
    }

    public void clearColorBuffer() {
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
    }

    public void clearStencilBuffer() {
        // grab our camera to get width and height info.
        final Camera cam = Camera.getCurrentCamera();

        // Clear the stencil buffer
        GL11.glClearStencil(0);
        GL11.glStencilMask(~0);
        GL11.glDisable(GL11.GL_DITHER);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(0, 0, cam.getWidth(), cam.getHeight());
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    public void clearBuffers() {
        // make sure no funny business is going on in the z before clearing.
        if (defaultStateList.containsKey(RenderState.StateType.ZBuffer)) {
            defaultStateList.get(RenderState.StateType.ZBuffer).setNeedsRefresh(true);
            applyState(defaultStateList.get(RenderState.StateType.ZBuffer));
        }
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    }

    public void clearStrictBuffers() {
        // grab our camera to get width and height info.
        final Camera cam = Camera.getCurrentCamera();

        GL11.glDisable(GL11.GL_DITHER);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(0, 0, cam.getWidth(), cam.getHeight());
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glEnable(GL11.GL_DITHER);
    }

    public void flushFrame(final boolean doSwap) {
        renderBuckets();

        reset();

        GL11.glFlush();
        if (doSwap) {
            applyState(defaultStateList.get(RenderState.StateType.ColorMask));

            if (Constants.stats) {
                StatCollector.startStat(StatType.STAT_DISPLAYSWAP_TIMER);
            }
            Display.update();
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
        _oldColorBuffer = _oldNormalBuffer = _oldVertexBuffer = _oldInterleavedBuffer = null;
        Arrays.fill(_oldTextureBuffers, null);
    }

    public void setOrtho() {
        if (_inOrthoMode) {
            throw new Ardor3dException("Already in Orthographic mode.");
        }
        // set up ortho mode
        final RendererRecord matRecord = ContextManager.getCurrentContext().getRendererRecord();
        LwjglRendererUtil.switchMode(matRecord, GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        final Camera camera = Camera.getCurrentCamera();
        final double viewportWidth = camera.getWidth() * (camera.getViewPortRight() - camera.getViewPortLeft());
        final double viewportHeight = camera.getHeight() * (camera.getViewPortTop() - camera.getViewPortBottom());
        GL11.glOrtho(0, viewportWidth, 0, viewportHeight, -1, 1);
        LwjglRendererUtil.switchMode(matRecord, GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        _inOrthoMode = true;
    }

    public void unsetOrtho() {
        if (!_inOrthoMode) {
            throw new Ardor3dException("Not in Orthographic mode.");
        }
        // remove ortho mode, and go back to original
        // state
        final RendererRecord matRecord = ContextManager.getCurrentContext().getRendererRecord();
        LwjglRendererUtil.switchMode(matRecord, GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        LwjglRendererUtil.switchMode(matRecord, GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        _inOrthoMode = false;
    }

    public void grabScreenContents(final ByteBuffer buff, final Image.Format format, final int x, final int y,
            final int w, final int h) {
        final int pixFormat = LwjglTextureUtil.getGLPixelFormat(format);
        GL11.glReadPixels(x, y, w, h, pixFormat, GL11.GL_UNSIGNED_BYTE, buff);
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
        GL11.glFlush();
    }

    public void finishGraphics() {
        GL11.glFinish();
    }

    private void applyNormalsMode(final NormalsMode normMode, final Transform worldTransform) {
        final RenderContext context = ContextManager.getCurrentContext();
        final ContextCapabilities caps = context.getCapabilities();
        switch (normMode) {
            case NormalizeIfScaled:
                final ReadOnlyVector3 scale = worldTransform.getScale();
                if (!(scale.getX() == 1.0 && scale.getY() == 1.0 && scale.getZ() == 1.0)) {
                    if (scale.getX() == scale.getY() && scale.getY() == scale.getZ() && caps.isOpenGL1_2Supported()
                            && _prevNormMode != GL12.GL_RESCALE_NORMAL) {
                        if (_prevNormMode == GL11.GL_NORMALIZE) {
                            GL11.glDisable(GL11.GL_NORMALIZE);
                        }
                        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
                        _prevNormMode = GL12.GL_RESCALE_NORMAL;
                    } else if (_prevNormMode != GL11.GL_NORMALIZE) {
                        if (_prevNormMode == GL12.GL_RESCALE_NORMAL) {
                            GL11.glDisable(GL12.GL_RESCALE_NORMAL);
                        }
                        GL11.glEnable(GL11.GL_NORMALIZE);
                        _prevNormMode = GL11.GL_NORMALIZE;
                    }
                } else {
                    if (_prevNormMode == GL12.GL_RESCALE_NORMAL) {
                        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
                        _prevNormMode = GL11.GL_ZERO;
                    } else if (_prevNormMode == GL11.GL_NORMALIZE) {
                        GL11.glDisable(GL11.GL_NORMALIZE);
                        _prevNormMode = GL11.GL_ZERO;
                    }
                }
                break;
            case AlwaysNormalize:
                if (_prevNormMode != GL11.GL_NORMALIZE) {
                    if (_prevNormMode == GL12.GL_RESCALE_NORMAL) {
                        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
                    }
                    GL11.glEnable(GL11.GL_NORMALIZE);
                    _prevNormMode = GL11.GL_NORMALIZE;
                }
                break;
            case UseProvided:
            default:
                if (_prevNormMode == GL12.GL_RESCALE_NORMAL) {
                    GL11.glDisable(GL12.GL_RESCALE_NORMAL);
                    _prevNormMode = GL11.GL_ZERO;
                } else if (_prevNormMode == GL11.GL_NORMALIZE) {
                    GL11.glDisable(GL11.GL_NORMALIZE);
                    _prevNormMode = GL11.GL_ZERO;
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
        // Check that the texture type is supported.
        if (dstTexture.getType() != Texture.Type.TwoDimensional) {
            throw new UnsupportedOperationException("Unsupported Texture Type: " + dstTexture.getType());
        }

        // Determine the original texture configuration, so that this method can
        // restore the texture configuration to its original state.
        GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D, _idBuff);
        final int origTexBinding = _idBuff.get(0);
        GL11.glGetInteger(GL11.GL_UNPACK_ALIGNMENT, _idBuff);
        final int origAlignment = _idBuff.get(0);
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
        // Consider moving these conversion methods.
        final int dataFormat = LwjglTextureUtil.getGLDataFormat(format);
        final int pixelFormat = LwjglTextureUtil.getGLPixelFormat(format);

        // Update the texture configuration (when necessary).
        if (origTexBinding != dstTexture.getTextureId()) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, dstTexture.getTextureId());
        }
        if (origAlignment != alignment) {
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, alignment);
        }
        if (origRowLength != rowLength) {
            GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, rowLength);
        }
        if (origSkipPixels != srcX) {
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, srcX);
        }
        if (origSkipRows != srcY) {
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, srcY);
        }

        // Upload the image region into the texture.
        if (glTexSubImage2DSupported) {
            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, dstX, dstY, dstWidth, dstHeight, pixelFormat,
                    GL11.GL_UNSIGNED_BYTE, data);

            try {
                Util.checkGLError();
            } catch (final OpenGLException e) {
                glTexSubImage2DSupported = false;
                updateTextureSubImage(dstTexture, data, srcX, srcY, srcWidth, srcHeight, dstX, dstY, dstWidth,
                        dstHeight, format);
            }
        } else {
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, dataFormat, dstWidth, dstHeight, 0, pixelFormat,
                    GL11.GL_UNSIGNED_BYTE, data);
        }

        // Restore the texture configuration (when necessary).
        // Restore the texture binding.
        if (origTexBinding != dstTexture.getTextureId()) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, origTexBinding);
        }
        // Restore alignment.
        if (origAlignment != alignment) {
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, origAlignment);
        }
        // Restore row length.
        if (origRowLength != rowLength) {
            GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, origRowLength);
        }
        // Restore skip pixels.
        if (origSkipPixels != srcX) {
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, origSkipPixels);
        }
        // Restore skip rows.
        if (origSkipRows != srcY) {
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, origSkipRows);
        }
    }

    public void checkCardError() throws Ardor3dException {
        try {
            org.lwjgl.opengl.Util.checkGLError();
        } catch (final OpenGLException exception) {
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

    public void setupVertexData(final FloatBuffer vertexBuffer, final VBOInfo vbo) {
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
                LwjglRendererUtil.setBoundVBO(rendRecord, vbo.getVBOVertexID());
                ARBBufferObject.glBindBufferARB(ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB, vbo.getVBOVertexID());
                ARBBufferObject.glBufferDataARB(ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB, vertexBuffer,
                        ARBBufferObject.GL_STATIC_DRAW_ARB);
            }
        }

        if (caps.isVBOSupported() && vbo != null && vbo.getVBOVertexID() > 0) {
            GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
            LwjglRendererUtil.setBoundVBO(rendRecord, vbo.getVBOVertexID());
            GL11.glVertexPointer(3, GL11.GL_FLOAT, 0, 0);
        } else if (vertexBuffer == null) {
            GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
        } else if (_oldVertexBuffer != vertexBuffer) {
            GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
            if (caps.isVBOSupported()) {
                LwjglRendererUtil.setBoundVBO(rendRecord, 0);
            }
            vertexBuffer.rewind();
            GL11.glVertexPointer(3, 0, vertexBuffer);
        }

        _oldVertexBuffer = vertexBuffer;
    }

    public void setupNormalData(final FloatBuffer normalBuffer, final NormalsMode normalMode,
            final Transform worldTransform, final VBOInfo vbo) {
        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord rendRecord = context.getRendererRecord();
        final ContextCapabilities caps = context.getCapabilities();

        if (normalMode != NormalsMode.Off) {
            applyNormalsMode(normalMode, worldTransform);

            if ((caps.isVBOSupported() && vbo != null && vbo.getVBONormalID() > 0)) {
                GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
                LwjglRendererUtil.setBoundVBO(rendRecord, vbo.getVBONormalID());
                GL11.glNormalPointer(GL11.GL_FLOAT, 0, 0);
            } else if (normalBuffer == null) {
                GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
            } else if (_oldNormalBuffer != normalBuffer) {
                GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
                if (caps.isVBOSupported()) {
                    LwjglRendererUtil.setBoundVBO(rendRecord, 0);
                }
                normalBuffer.rewind();
                GL11.glNormalPointer(0, normalBuffer);
            }
            _oldNormalBuffer = normalBuffer;
        } else {
            if (_prevNormMode == GL12.GL_RESCALE_NORMAL) {
                GL11.glDisable(GL12.GL_RESCALE_NORMAL);
                _prevNormMode = GL11.GL_ZERO;
            } else if (_prevNormMode == GL11.GL_NORMALIZE) {
                GL11.glDisable(GL11.GL_NORMALIZE);
                _prevNormMode = GL11.GL_ZERO;
            }
            GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
            _oldNormalBuffer = null;
        }
    }

    public void setupColorData(final FloatBuffer colorBuffer, final VBOInfo vbo, final ColorRGBA defaultColor) {
        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord rendRecord = context.getRendererRecord();
        final ContextCapabilities caps = context.getCapabilities();

        if ((caps.isVBOSupported() && vbo != null && vbo.getVBOColorID() > 0)) {
            GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
            LwjglRendererUtil.setBoundVBO(rendRecord, vbo.getVBOColorID());
            GL11.glColorPointer(4, GL11.GL_FLOAT, 0, 0);
        } else if (colorBuffer == null) {
            GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);

            if (defaultColor != null) {
                GL11.glColor4f(defaultColor.getRed(), defaultColor.getGreen(), defaultColor.getBlue(), defaultColor
                        .getAlpha());
            } else {
                GL11.glColor4f(1, 1, 1, 1);
            }
        } else if (_oldColorBuffer != colorBuffer) {
            GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
            if (caps.isVBOSupported()) {
                LwjglRendererUtil.setBoundVBO(rendRecord, 0);
            }
            colorBuffer.rewind();
            GL11.glColorPointer(4, 0, colorBuffer);
        }

        _oldColorBuffer = colorBuffer;
    }

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
    // GL11.glEnableClientState(EXTFogCoord.GL_FOG_COORDINATE_ARRAY_EXT);
    // rendRecord.setBoundVBO(vbo.getVBOVertexID());
    // EXTFogCoord.glFogCoordPointerEXT(GL11.GL_FLOAT, 0, 0);
    // } else if (fogCoords == null) {
    // GL11.glDisableClientState(EXTFogCoord.GL_FOG_COORDINATE_ARRAY_EXT);
    // } else if (_oldFogBuffer != fogCoords) {
    // // fog coords have changed
    // GL11.glEnableClientState(EXTFogCoord.GL_FOG_COORDINATE_ARRAY_EXT);
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

    public void setupTextureData(final List<TexCoords> textureCoords, final VBOInfo vbo) {
        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord rendRecord = context.getRendererRecord();
        final ContextCapabilities caps = context.getCapabilities();

        final TextureState ts = (TextureState) context.getCurrentState(RenderState.StateType.Texture);
        int offset = 0;
        if (ts != null) {
            offset = ts.getTextureCoordinateOffset();

            for (int i = 0; i < ts.getNumberOfSetTextures() && i < caps.getNumberOfFragmentTexCoordUnits(); i++) {
                if (caps.isMultitextureSupported()) {
                    ARBMultitexture.glClientActiveTextureARB(ARBMultitexture.GL_TEXTURE0_ARB + i);
                }

                if (i >= textureCoords.size()) {
                    GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
                    continue;
                }

                final TexCoords texC = textureCoords.get(i + offset);
                if ((caps.isVBOSupported() && vbo != null && vbo.getVBOTextureID(i) > 0)) {
                    GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
                    LwjglRendererUtil.setBoundVBO(rendRecord, vbo.getVBOTextureID(i));
                    GL11.glTexCoordPointer(texC.perVert, GL11.GL_FLOAT, 0, 0);
                } else if (texC == null) {
                    GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
                } else if (_oldTextureBuffers[i] != texC.coords) {
                    GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
                    if (caps.isVBOSupported()) {
                        LwjglRendererUtil.setBoundVBO(rendRecord, 0);
                    }
                    texC.coords.rewind();
                    GL11.glTexCoordPointer(texC.perVert, 0, texC.coords);
                } else {
                    GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
                }
                _oldTextureBuffers[i] = texC != null ? texC.coords : null;
            }

            if (ts.getNumberOfSetTextures() < _prevTextureNumber) {
                for (int i = ts.getNumberOfSetTextures(); i < _prevTextureNumber; i++) {
                    if (caps.isMultitextureSupported()) {
                        ARBMultitexture.glClientActiveTextureARB(ARBMultitexture.GL_TEXTURE0_ARB + i);
                    }
                    GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
                }
            }

            _prevTextureNumber = ts.getNumberOfSetTextures() < caps.getNumberOfFixedTextureUnits() ? ts
                    .getNumberOfSetTextures() : caps.getNumberOfFixedTextureUnits();
        }
    }

    public void setupInterleavedData(final FloatBuffer interleavedBuffer, final InterleavedFormat format,
            final VBOInfo vbo) {
        if (_oldInterleavedBuffer != interleavedBuffer) {
            interleavedBuffer.rewind();

            final int glFormat = getGLInterleavedFormat(format);

            GL11.glInterleavedArrays(glFormat, 0, interleavedBuffer);
        }
        _oldInterleavedBuffer = interleavedBuffer;
    }

    public boolean doTransforms(final Transform transform) {
        // set world matrix
        if (!transform.isIdentity()) {
            synchronized (_transformMatrix) {
                transform.getGLApplyMatrix(_transformBuffer);

                final RendererRecord matRecord = ContextManager.getCurrentContext().getRendererRecord();
                LwjglRendererUtil.switchMode(matRecord, GL11.GL_MODELVIEW);
                GL11.glPushMatrix();
                GL11.glMultMatrix(_transformBuffer);
                return true;
            }
        }
        return false;
    }

    public void undoTransforms(final Transform transform) {
        final RendererRecord matRecord = ContextManager.getCurrentContext().getRendererRecord();
        LwjglRendererUtil.switchMode(matRecord, GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
    }

    public void drawElements(final IntBuffer indices, final VBOInfo vbo, final int[] indexLengths,
            final IndexMode[] indexModes) {
        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord rendRecord = context.getRendererRecord();
        final ContextCapabilities caps = context.getCapabilities();

        boolean useIndicesVBO = false;
        if ((caps.isVBOSupported() && vbo != null && vbo.getVBOIndexID() > 0)) {
            useIndicesVBO = true;
            LwjglRendererUtil.setBoundElementVBO(rendRecord, vbo.getVBOIndexID());
        } else if (caps.isVBOSupported()) {
            LwjglRendererUtil.setBoundElementVBO(rendRecord, 0);
        }

        if (!useIndicesVBO && indices == null) {
            logger.severe("Missing indices for drawElements call without VBO");
            return;
        }

        if (indexLengths == null) {
            final int glIndexMode = getGLIndexMode(indexModes[0]);

            if (!useIndicesVBO) {
                indices.position(0);
                GL11.glDrawElements(glIndexMode, indices);
            } else {
                GL11.glDrawElements(glIndexMode, indices.limit(), GL11.GL_UNSIGNED_INT, 0);
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
                    GL11.glDrawElements(glIndexMode, indices);
                } else {
                    GL11.glDrawElements(glIndexMode, count, GL11.GL_UNSIGNED_INT, offset);
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
        if (indexLengths == null) {
            final int glIndexMode = getGLIndexMode(indexModes[0]);

            GL11.glDrawArrays(glIndexMode, 0, vertexBuffer.limit() / 3);

            if (Constants.stats) {
                addStats(indexModes[0], vertexBuffer.limit() / 3);
            }
        } else {
            int offset = 0;
            int indexModeCounter = 0;
            for (int i = 0; i < indexLengths.length; i++) {
                final int count = indexLengths[i];

                final int glIndexMode = getGLIndexMode(indexModes[indexModeCounter]);

                GL11.glDrawArrays(glIndexMode, offset, count);

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
        _idBuff.rewind();
        ARBBufferObject.glGenBuffersARB(_idBuff);
        final int vboID = _idBuff.get(0);
        rendRecord.getVboCleanupCache().add(vboID);
        return vboID;
    }

    public void deleteVBOId(final RendererRecord rendRecord, final int id) {
        _idBuff.rewind();
        _idBuff.put(id).flip();
        ARBBufferObject.glDeleteBuffersARB(_idBuff);
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
        GL11.glCallList(displayListID);

        // invalidate "current arrays"
        reset();
    }

    private int getGLIndexMode(final IndexMode indexMode) {
        int glMode = GL11.GL_TRIANGLES;
        switch (indexMode) {
            case Triangles:
                glMode = GL11.GL_TRIANGLES;
                break;
            case TriangleStrip:
                glMode = GL11.GL_TRIANGLE_STRIP;
                break;
            case TriangleFan:
                glMode = GL11.GL_TRIANGLE_FAN;
                break;
            case Quads:
                glMode = GL11.GL_QUADS;
                break;
            case QuadStrip:
                glMode = GL11.GL_QUAD_STRIP;
                break;
            case Lines:
                glMode = GL11.GL_LINES;
                break;
            case LineStrip:
                glMode = GL11.GL_LINE_STRIP;
                break;
            case LineLoop:
                glMode = GL11.GL_LINE_LOOP;
                break;
            case Points:
                glMode = GL11.GL_POINTS;
                break;
        }
        return glMode;
    }

    private int getGLInterleavedFormat(final InterleavedFormat format) {
        int glInterleavedFormat = GL11.GL_V3F;
        switch (format) {
            case GL_V2F:
                glInterleavedFormat = GL11.GL_V2F;
                break;
            case GL_V3F:
                glInterleavedFormat = GL11.GL_V3F;
                break;
            case GL_C3F_V3F:
                glInterleavedFormat = GL11.GL_C3F_V3F;
                break;
            case GL_C4F_N3F_V3F:
                glInterleavedFormat = GL11.GL_C4F_N3F_V3F;
                break;
            case GL_C4UB_V2F:
                glInterleavedFormat = GL11.GL_C4UB_V2F;
                break;
            case GL_C4UB_V3F:
                glInterleavedFormat = GL11.GL_C4UB_V3F;
                break;
            case GL_N3F_V3F:
                glInterleavedFormat = GL11.GL_N3F_V3F;
                break;
            case GL_T2F_C3F_V3F:
                glInterleavedFormat = GL11.GL_T2F_C3F_V3F;
                break;
            case GL_T2F_C4F_N3F_V3F:
                glInterleavedFormat = GL11.GL_T2F_C4F_N3F_V3F;
                break;
            case GL_T2F_C4UB_V3F:
                glInterleavedFormat = GL11.GL_T2F_C4UB_V3F;
                break;
            case GL_T2F_N3F_V3F:
                glInterleavedFormat = GL11.GL_T2F_N3F_V3F;
                break;
            case GL_T2F_V3F:
                glInterleavedFormat = GL11.GL_T2F_V3F;
                break;
            case GL_T4F_C4F_N3F_V4F:
                glInterleavedFormat = GL11.GL_T4F_C4F_N3F_V4F;
                break;
            case GL_T4F_V4F:
                glInterleavedFormat = GL11.GL_T4F_V4F;
                break;
        }
        return glInterleavedFormat;
    }

    public void setModelViewMatrix(final Buffer matrix) {
        final RendererRecord matRecord = ContextManager.getCurrentContext().getRendererRecord();
        LwjglRendererUtil.switchMode(matRecord, GL11.GL_MODELVIEW);
        loadMatrix(matrix);
    }

    public void setProjectionMatrix(final Buffer matrix) {
        final RendererRecord matRecord = ContextManager.getCurrentContext().getRendererRecord();
        LwjglRendererUtil.switchMode(matRecord, GL11.GL_PROJECTION);
        loadMatrix(matrix);
    }

    private void loadMatrix(final Buffer matrix) {
        if (matrix instanceof DoubleBuffer) {
            GL11.glLoadMatrix((DoubleBuffer) matrix);
        } else if (matrix instanceof FloatBuffer) {
            GL11.glLoadMatrix((FloatBuffer) matrix);
        }
    }

    public Buffer getModelViewMatrix(final Buffer store) {
        if (store == null || store instanceof DoubleBuffer) {
            return getMatrix(GL11.GL_MODELVIEW_MATRIX, (DoubleBuffer) store);
        } else if (store instanceof FloatBuffer) {
            return getMatrix(GL11.GL_MODELVIEW_MATRIX, (FloatBuffer) store);
        } else {
            return null;
        }
    }

    public Buffer getProjectionMatrix(final Buffer store) {
        if (store == null || store instanceof DoubleBuffer) {
            return getMatrix(GL11.GL_PROJECTION_MATRIX, (DoubleBuffer) store);
        } else if (store instanceof FloatBuffer) {
            return getMatrix(GL11.GL_PROJECTION_MATRIX, (FloatBuffer) store);
        } else {
            return null;
        }
    }

    private DoubleBuffer getMatrix(final int matrixType, final DoubleBuffer store) {
        DoubleBuffer result = store;
        if (result == null || result.remaining() < 16) {
            result = BufferUtils.createDoubleBuffer(16);
        }
        GL11.glGetDouble(matrixType, store);
        return result;
    }

    private FloatBuffer getMatrix(final int matrixType, final FloatBuffer store) {
        FloatBuffer result = store;
        if (result.remaining() < 16) {
            result = BufferUtils.createFloatBuffer(16);
        }
        GL11.glGetFloat(matrixType, store);
        return result;
    }

    public void setViewport(final int x, final int y, final int width, final int height) {
        GL11.glViewport(x, y, width, height);
    }

    public void setDepthRange(final double depthRangeNear, final double depthRangeFar) {
        GL11.glDepthRange(depthRangeNear, depthRangeFar);
    }

    public void setDrawBuffer(final DrawBufferTarget target) {
        final RendererRecord record = ContextManager.getCurrentContext().getRendererRecord();
        if (record.getDrawBufferTarget() != target) {
            int buffer = GL11.GL_BACK;
            switch (target) {
                case Back:
                    break;
                case Front:
                    buffer = GL11.GL_FRONT;
                    break;
                case BackLeft:
                    buffer = GL11.GL_BACK_LEFT;
                    break;
                case BackRight:
                    buffer = GL11.GL_BACK_RIGHT;
                    break;
                case FrontLeft:
                    buffer = GL11.GL_FRONT_LEFT;
                    break;
                case FrontRight:
                    buffer = GL11.GL_FRONT_RIGHT;
                    break;
                case FrontAndBack:
                    buffer = GL11.GL_FRONT_AND_BACK;
                    break;
                case Left:
                    buffer = GL11.GL_LEFT;
                    break;
                case Right:
                    buffer = GL11.GL_RIGHT;
                    break;
                case Aux0:
                    buffer = GL11.GL_AUX0;
                    break;
                case Aux1:
                    buffer = GL11.GL_AUX1;
                    break;
                case Aux2:
                    buffer = GL11.GL_AUX2;
                    break;
                case Aux3:
                    buffer = GL11.GL_AUX3;
                    break;
            }

            GL11.glDrawBuffer(buffer);
            record.setDrawBufferTarget(target);
        }
    }

    public void setupLineParameters(final float lineWidth, final int stippleFactor, final short stipplePattern,
            final boolean antialiased) {
        final LineRecord lineRecord = ContextManager.getCurrentContext().getLineRecord();

        if (!lineRecord.isValid() || lineRecord.width != lineWidth) {
            GL11.glLineWidth(lineWidth);
            lineRecord.width = lineWidth;
        }

        if (stipplePattern != (short) 0xFFFF) {
            if (!lineRecord.isValid() || !lineRecord.stippled) {
                GL11.glEnable(GL11.GL_LINE_STIPPLE);
                lineRecord.stippled = true;
            }

            if (!lineRecord.isValid() || stippleFactor != lineRecord.stippleFactor
                    || stipplePattern != lineRecord.stipplePattern) {
                GL11.glLineStipple(stippleFactor, stipplePattern);
                lineRecord.stippleFactor = stippleFactor;
                lineRecord.stipplePattern = stipplePattern;
            }
        } else if (!lineRecord.isValid() || lineRecord.stippled) {
            GL11.glDisable(GL11.GL_LINE_STIPPLE);
            lineRecord.stippled = false;
        }

        if (antialiased) {
            if (!lineRecord.isValid() || !lineRecord.smoothed) {
                GL11.glEnable(GL11.GL_LINE_SMOOTH);
                lineRecord.smoothed = true;
            }
            if (!lineRecord.isValid() || lineRecord.smoothHint != GL11.GL_NICEST) {
                GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
                lineRecord.smoothHint = GL11.GL_NICEST;
            }
        } else if (!lineRecord.isValid() || lineRecord.smoothed) {
            GL11.glDisable(GL11.GL_LINE_SMOOTH);
            lineRecord.smoothed = false;
        }

        if (!lineRecord.isValid()) {
            lineRecord.validate();
        }
    }

    public void setupPointParameters(final float pointSize, final boolean antialiased) {
        // TODO: make a record for point states
        GL11.glPointSize(pointSize);
        if (antialiased) {
            GL11.glEnable(GL11.GL_POINT_SMOOTH);
            GL11.glHint(GL11.GL_POINT_SMOOTH_HINT, GL11.GL_NICEST);
        }
    }

    public void applyState(final RenderState state) {
        if (state == null) {
            logger.warning("tried to apply a null state.");
            return;
        }
        switch (state.getType()) {
            case Texture:
                LwjglTextureStateUtil.apply(this, (TextureState) state);
                return;
            case Light:
                LwjglLightStateUtil.apply(this, (LightState) state);
                return;
            case Blend:
                LwjglBlendStateUtil.apply(this, (BlendState) state);
                return;
            case Clip:
                LwjglClipStateUtil.apply(this, (ClipState) state);
                return;
            case ColorMask:
                LwjglColorMaskStateUtil.apply(this, (ColorMaskState) state);
                return;
            case Cull:
                LwjglCullStateUtil.apply(this, (CullState) state);
                return;
            case Fog:
                LwjglFogStateUtil.apply(this, (FogState) state);
                return;
            case FragmentProgram:
                LwjglFragmentProgramStateUtil.apply(this, (FragmentProgramState) state);
                return;
            case GLSLShader:
                LwjglShaderObjectsStateUtil.apply(this, (GLSLShaderObjectsState) state);
                return;
            case Material:
                LwjglMaterialStateUtil.apply(this, (MaterialState) state);
                return;
            case Offset:
                LwjglOffsetStateUtil.apply(this, (OffsetState) state);
                return;
            case Shading:
                LwjglShadingStateUtil.apply(this, (ShadingState) state);
                return;
            case Stencil:
                LwjglStencilStateUtil.apply(this, (StencilState) state);
                return;
            case VertexProgram:
                LwjglVertexProgramStateUtil.apply(this, (VertexProgramState) state);
                return;
            case Wireframe:
                LwjglWireframeStateUtil.apply(this, (WireframeState) state);
                return;
            case ZBuffer:
                LwjglZBufferStateUtil.apply(this, (ZBufferState) state);
                return;
        }
        throw new IllegalArgumentException("Unknown state: " + state);
    }

    public void deleteTextureId(final int textureId) {
        LwjglTextureStateUtil.deleteTextureId(textureId);
    }

    public void loadTexture(final Texture texture, final int unit) {
        LwjglTextureStateUtil.load(texture, unit);
    }
}
