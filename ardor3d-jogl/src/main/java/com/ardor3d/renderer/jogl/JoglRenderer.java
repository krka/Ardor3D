/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
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
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import javax.media.opengl.GL;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLException;
import javax.media.opengl.glu.GLU;

import com.ardor3d.image.Image;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture1D;
import com.ardor3d.image.Texture2D;
import com.ardor3d.image.Texture3D;
import com.ardor3d.image.TextureCubeMap;
import com.ardor3d.image.TextureCubeMap.Face;
import com.ardor3d.math.Matrix4;
import com.ardor3d.math.Rectangle2;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.math.type.ReadOnlyTransform;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.AbstractRenderer;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.DrawBufferTarget;
import com.ardor3d.renderer.IndexMode;
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
import com.ardor3d.scenegraph.AbstractBufferData;
import com.ardor3d.scenegraph.FloatBufferData;
import com.ardor3d.scenegraph.IntBufferData;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Renderable;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.AbstractBufferData.VBOAccessMode;
import com.ardor3d.scenegraph.hint.NormalsMode;
import com.ardor3d.util.Ardor3dException;
import com.ardor3d.util.Constants;
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

    private FloatBuffer _oldFogBuffer;

    private final FloatBuffer[] _oldTextureBuffers;

    private final DoubleBuffer _transformBuffer = BufferUtils.createDoubleBuffer(16);
    {
        _transformBuffer.position(15);
        _transformBuffer.put(1.0);
    }

    private final Matrix4 _transformMatrix = new Matrix4();

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

    public void clearBuffers(final int buffers) {
        clearBuffers(buffers, false);
    }

    public void clearBuffers(final int buffers, final boolean strict) {
        final GL gl = GLU.getCurrentGL();

        int clear = 0;

        if ((buffers & Renderer.BUFFER_COLOR) != 0) {
            clear |= GL.GL_COLOR_BUFFER_BIT;
        }

        if ((buffers & Renderer.BUFFER_DEPTH) != 0) {
            clear |= GL.GL_DEPTH_BUFFER_BIT;

            // make sure no funny business is going on in the z before clearing.
            if (defaultStateList.containsKey(RenderState.StateType.ZBuffer)) {
                defaultStateList.get(RenderState.StateType.ZBuffer).setNeedsRefresh(true);
                doApplyState(defaultStateList.get(RenderState.StateType.ZBuffer));
            }
        }

        if ((buffers & Renderer.BUFFER_STENCIL) != 0) {
            clear |= GL.GL_STENCIL_BUFFER_BIT;

            gl.glClearStencil(_stencilClearValue);
            gl.glStencilMask(~0);
            gl.glClear(GL.GL_STENCIL_BUFFER_BIT);
        }

        if ((buffers & Renderer.BUFFER_ACCUMULATION) != 0) {
            clear |= GL.GL_ACCUM_BUFFER_BIT;
        }

        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord record = context.getRendererRecord();

        if (strict) {
            // grab our camera to get width and height info.
            final Camera cam = Camera.getCurrentCamera();

            gl.glEnable(GL.GL_SCISSOR_TEST);
            gl.glScissor(0, 0, cam.getWidth(), cam.getHeight());
            record.setClippingTestEnabled(true);
        }

        gl.glClear(clear);

        if (strict) {
            // put us back.
            JoglRendererUtil.applyScissors(record);
        }
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

            checkCardError();
            GLContext.getCurrent().getGLDrawable().swapBuffers();
            if (Constants.stats) {
                StatCollector.endStat(StatType.STAT_DISPLAYSWAP_TIMER);
            }
        }

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
        final RenderBucketType rqMode = s.getSceneHints().getRenderBucketType();
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

    public void applyNormalsMode(final NormalsMode normalsMode, final ReadOnlyTransform worldTransform) {
        final GL gl = GLU.getCurrentGL();
        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord rendRecord = context.getRendererRecord();
        if (normalsMode != NormalsMode.Off) {
            final ContextCapabilities caps = context.getCapabilities();
            switch (normalsMode) {
                case NormalizeIfScaled:
                    if (worldTransform.isRotationMatrix()) {
                        final ReadOnlyVector3 scale = worldTransform.getScale();
                        if (!(scale.getX() == 1.0 && scale.getY() == 1.0 && scale.getZ() == 1.0)) {
                            if (scale.getX() == scale.getY() && scale.getY() == scale.getZ()
                                    && caps.isOpenGL1_2Supported()
                                    && rendRecord.getNormalMode() != GL.GL_RESCALE_NORMAL) {
                                if (rendRecord.getNormalMode() == GL.GL_NORMALIZE) {
                                    gl.glDisable(GL.GL_NORMALIZE);
                                }
                                gl.glEnable(GL.GL_RESCALE_NORMAL);
                                rendRecord.setNormalMode(GL.GL_RESCALE_NORMAL);
                            } else if (rendRecord.getNormalMode() != GL.GL_NORMALIZE) {
                                if (rendRecord.getNormalMode() == GL.GL_RESCALE_NORMAL) {
                                    gl.glDisable(GL.GL_RESCALE_NORMAL);
                                }
                                gl.glEnable(GL.GL_NORMALIZE);
                                rendRecord.setNormalMode(GL.GL_NORMALIZE);
                            }
                        } else {
                            if (rendRecord.getNormalMode() == GL.GL_RESCALE_NORMAL) {
                                gl.glDisable(GL.GL_RESCALE_NORMAL);
                            } else if (rendRecord.getNormalMode() == GL.GL_NORMALIZE) {
                                gl.glDisable(GL.GL_NORMALIZE);
                            }
                            rendRecord.setNormalMode(GL.GL_ZERO);
                        }
                    } else {
                        if (!worldTransform.getMatrix().isIdentity()) {
                            // *might* be scaled...
                            if (rendRecord.getNormalMode() != GL.GL_NORMALIZE) {
                                if (rendRecord.getNormalMode() == GL.GL_RESCALE_NORMAL) {
                                    gl.glDisable(GL.GL_RESCALE_NORMAL);
                                }
                                gl.glEnable(GL.GL_NORMALIZE);
                                rendRecord.setNormalMode(GL.GL_NORMALIZE);
                            }
                        } else {
                            // not scaled
                            if (rendRecord.getNormalMode() == GL.GL_RESCALE_NORMAL) {
                                gl.glDisable(GL.GL_RESCALE_NORMAL);
                            } else if (rendRecord.getNormalMode() == GL.GL_NORMALIZE) {
                                gl.glDisable(GL.GL_NORMALIZE);
                            }
                            rendRecord.setNormalMode(GL.GL_ZERO);
                        }
                    }
                    break;
                case AlwaysNormalize:
                    if (rendRecord.getNormalMode() != GL.GL_NORMALIZE) {
                        if (rendRecord.getNormalMode() == GL.GL_RESCALE_NORMAL) {
                            gl.glDisable(GL.GL_RESCALE_NORMAL);
                        }
                        gl.glEnable(GL.GL_NORMALIZE);
                        rendRecord.setNormalMode(GL.GL_NORMALIZE);
                    }
                    break;
                case UseProvided:
                default:
                    if (rendRecord.getNormalMode() == GL.GL_RESCALE_NORMAL) {
                        gl.glDisable(GL.GL_RESCALE_NORMAL);
                    } else if (rendRecord.getNormalMode() == GL.GL_NORMALIZE) {
                        gl.glDisable(GL.GL_NORMALIZE);
                    }
                    rendRecord.setNormalMode(GL.GL_ZERO);
                    break;
            }
        } else {
            if (rendRecord.getNormalMode() == GL.GL_RESCALE_NORMAL) {
                gl.glDisable(GL.GL_RESCALE_NORMAL);
            } else if (rendRecord.getNormalMode() == GL.GL_NORMALIZE) {
                gl.glDisable(GL.GL_NORMALIZE);
            }
            rendRecord.setNormalMode(GL.GL_ZERO);
        }
    }

    public void applyDefaultColor(final ReadOnlyColorRGBA defaultColor) {
        final GL gl = GLU.getCurrentGL();
        if (defaultColor != null) {
            gl.glColor4f(defaultColor.getRed(), defaultColor.getGreen(), defaultColor.getBlue(), defaultColor
                    .getAlpha());
        } else {
            gl.glColor4f(1, 1, 1, 1);
        }
    }

    public void deleteVBOs(final Collection<Integer> ids) {
        final GL gl = GLU.getCurrentGL();
        final IntBuffer idBuffer = BufferUtils.createIntBuffer(ids.size());
        idBuffer.clear();
        for (final Integer i : ids) {
            if (i != null && i > 0) {
                idBuffer.put(i);
            }
        }
        idBuffer.flip();
        if (idBuffer.remaining() > 0) {
            gl.glDeleteBuffers(idBuffer.remaining(), idBuffer);
        }
    }

    public void deleteDisplayLists(final Collection<Integer> ids) {
        final GL gl = GLU.getCurrentGL();
        for (final Integer i : ids) {
            if (i != null && i > 0) {
                gl.glDeleteLists(i, 1);
            }
        }
    }

    public void deleteVBOs(final AbstractBufferData<?> buffer) {
        if (buffer == null) {
            return;
        }

        final GL gl = GLU.getCurrentGL();

        // ask for the current state record
        final RenderContext context = ContextManager.getCurrentContext();

        final int id = buffer.getVBOID(context.getGlContextRep());
        if (id == 0) {
            // Not on card... return.
            return;
        }

        buffer.removeVBOID(context.getGlContextRep());

        final IntBuffer idBuff = BufferUtils.createIntBuffer(1);
        idBuff.put(id);
        idBuff.flip();
        gl.glDeleteBuffers(1, idBuff);
    }

    public void updateTexture1DSubImage(final Texture1D destination, final int dstOffsetX, final int dstWidth,
            final ByteBuffer source, final int srcOffsetX) {
        updateTexSubImage(destination, dstOffsetX, 0, 0, dstWidth, 0, 0, source, srcOffsetX, 0, 0, 0, 0, null);
    }

    public void updateTexture2DSubImage(final Texture2D destination, final int dstOffsetX, final int dstOffsetY,
            final int dstWidth, final int dstHeight, final ByteBuffer source, final int srcOffsetX,
            final int srcOffsetY, final int srcTotalWidth) {
        updateTexSubImage(destination, dstOffsetX, dstOffsetY, 0, dstWidth, dstHeight, 0, source, srcOffsetX,
                srcOffsetY, 0, srcTotalWidth, 0, null);
    }

    public void updateTexture3DSubImage(final Texture3D destination, final int dstOffsetX, final int dstOffsetY,
            final int dstOffsetZ, final int dstWidth, final int dstHeight, final int dstDepth, final ByteBuffer source,
            final int srcOffsetX, final int srcOffsetY, final int srcOffsetZ, final int srcTotalWidth,
            final int srcTotalHeight) {
        updateTexSubImage(destination, dstOffsetX, dstOffsetY, dstOffsetZ, dstWidth, dstHeight, dstDepth, source,
                srcOffsetX, srcOffsetY, srcOffsetZ, srcTotalWidth, srcTotalHeight, null);
    }

    public void updateTextureCubeMapSubImage(final TextureCubeMap destination, final TextureCubeMap.Face dstFace,
            final int dstOffsetX, final int dstOffsetY, final int dstWidth, final int dstHeight,
            final ByteBuffer source, final int srcOffsetX, final int srcOffsetY, final int srcTotalWidth) {
        updateTexSubImage(destination, dstOffsetX, dstOffsetY, 0, dstWidth, dstHeight, 0, source, srcOffsetX,
                srcOffsetY, 0, srcTotalWidth, 0, dstFace);
    }

    private void updateTexSubImage(final Texture destination, final int dstOffsetX, final int dstOffsetY,
            final int dstOffsetZ, final int dstWidth, final int dstHeight, final int dstDepth, final ByteBuffer source,
            final int srcOffsetX, final int srcOffsetY, final int srcOffsetZ, final int srcTotalWidth,
            final int srcTotalHeight, final Face dstFace) {

        final GL gl = GLU.getCurrentGL();

        // Ignore textures that do not have an id set
        if (destination.getTextureIdForContext(ContextManager.getCurrentContext().getGlContextRep()) == 0) {
            logger.warning("Attempting to update a texture that is not currently on the card.");
            return;
        }

        // Determine the original texture configuration, so that this method can
        // restore the texture configuration to its original state.
        final int origAlignment[] = new int[1];
        gl.glGetIntegerv(GL.GL_UNPACK_ALIGNMENT, origAlignment, 0);
        final int origRowLength = 0;
        final int origImageHeight = 0;
        final int origSkipPixels = 0;
        final int origSkipRows = 0;
        final int origSkipImages = 0;

        final int alignment = 1;

        int rowLength;
        if (srcTotalWidth == dstWidth) {
            // When the row length is zero, then the width parameter is used.
            // We use zero in these cases in the hope that we can avoid two
            // unnecessary calls to glPixelStorei.
            rowLength = 0;
        } else {
            // The number of pixels in a row is different than the number of
            // pixels in the region to be uploaded to the texture.
            rowLength = srcTotalWidth;
        }

        int imageHeight;
        if (srcTotalHeight == dstHeight) {
            // When the image height is zero, then the height parameter is used.
            // We use zero in these cases in the hope that we can avoid two
            // unnecessary calls to glPixelStorei.
            imageHeight = 0;
        } else {
            // The number of pixels in a row is different than the number of
            // pixels in the region to be uploaded to the texture.
            imageHeight = srcTotalHeight;
        }

        // Grab pixel format
        final int pixelFormat = JoglTextureUtil.getGLPixelFormat(destination.getImage().getFormat());

        // bind...
        JoglTextureStateUtil.doTextureBind(destination, 0, false);

        // Update the texture configuration (when necessary).

        if (origAlignment[0] != alignment) {
            gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, alignment);
        }
        if (origRowLength != rowLength) {
            gl.glPixelStorei(GL.GL_UNPACK_ROW_LENGTH, rowLength);
        }
        if (origSkipPixels != srcOffsetX) {
            gl.glPixelStorei(GL.GL_UNPACK_SKIP_PIXELS, srcOffsetX);
        }
        // NOTE: The below will be skipped for texture types that don't support them because we are passing in 0's.
        if (origSkipRows != srcOffsetY) {
            gl.glPixelStorei(GL.GL_UNPACK_SKIP_ROWS, srcOffsetY);
        }
        if (origImageHeight != imageHeight) {
            gl.glPixelStorei(GL.GL_UNPACK_IMAGE_HEIGHT, imageHeight);
        }
        if (origSkipImages != srcOffsetZ) {
            gl.glPixelStorei(GL.GL_UNPACK_SKIP_IMAGES, srcOffsetZ);
        }

        // Upload the image region into the texture.
        try {
            switch (destination.getType()) {
                case TwoDimensional:
                    gl.glTexSubImage2D(GL.GL_TEXTURE_2D, 0, dstOffsetX, dstOffsetY, dstWidth, dstHeight, pixelFormat,
                            GL.GL_UNSIGNED_BYTE, source);
                    break;
                case OneDimensional:
                    gl.glTexSubImage1D(GL.GL_TEXTURE_1D, 0, dstOffsetX, dstWidth, pixelFormat, GL.GL_UNSIGNED_BYTE,
                            source);
                    break;
                case ThreeDimensional:
                    gl.glTexSubImage3D(GL.GL_TEXTURE_3D, 0, dstOffsetX, dstOffsetY, dstOffsetZ, dstWidth, dstHeight,
                            dstDepth, pixelFormat, GL.GL_UNSIGNED_BYTE, source);
                    break;
                case CubeMap:
                    gl.glTexSubImage2D(JoglTextureStateUtil.getGLCubeMapFace(dstFace), 0, dstOffsetX, dstOffsetY,
                            dstWidth, dstHeight, pixelFormat, GL.GL_UNSIGNED_BYTE, source);
                    break;
                default:
                    throw new Ardor3dException("Unsupported type for updateTextureSubImage: " + destination.getType());
            }
        } finally {
            // Restore the texture configuration (when necessary)...
            // Restore alignment.
            if (origAlignment[0] != alignment) {
                gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, origAlignment[0]);
            }
            // Restore row length.
            if (origRowLength != rowLength) {
                gl.glPixelStorei(GL.GL_UNPACK_ROW_LENGTH, origRowLength);
            }
            // Restore skip pixels.
            if (origSkipPixels != srcOffsetX) {
                gl.glPixelStorei(GL.GL_UNPACK_SKIP_PIXELS, origSkipPixels);
            }
            // Restore skip rows.
            if (origSkipRows != srcOffsetY) {
                gl.glPixelStorei(GL.GL_UNPACK_SKIP_ROWS, origSkipRows);
            }
            // Restore image height.
            if (origImageHeight != imageHeight) {
                gl.glPixelStorei(GL.GL_UNPACK_IMAGE_HEIGHT, origImageHeight);
            }
            // Restore skip images.
            if (origSkipImages != srcOffsetZ) {
                gl.glPixelStorei(GL.GL_UNPACK_SKIP_IMAGES, origSkipImages);
            }
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

    public void draw(final Renderable renderable) {
        renderable.render(this);
    }

    public boolean doTransforms(final ReadOnlyTransform transform) {
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

    public void undoTransforms(final ReadOnlyTransform transform) {
        final GL gl = GLU.getCurrentGL();

        final RendererRecord matRecord = ContextManager.getCurrentContext().getRendererRecord();
        JoglRendererUtil.switchMode(matRecord, GL.GL_MODELVIEW);
        gl.glPopMatrix();
    }

    public void setupVertexData(final FloatBufferData vertexBufferData) {
        final GL gl = GLU.getCurrentGL();

        final FloatBuffer vertexBuffer = vertexBufferData != null ? vertexBufferData.getBuffer() : null;

        if (vertexBuffer == null) {
            gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
        } else if (_oldVertexBuffer != vertexBuffer) {
            gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
            vertexBuffer.rewind();
            gl.glVertexPointer(vertexBufferData.getValuesPerTuple(), GL.GL_FLOAT, 0, vertexBuffer);
        }

        _oldVertexBuffer = vertexBuffer;
    }

    public void setupNormalData(final FloatBufferData normalBufferData) {
        final GL gl = GLU.getCurrentGL();

        final FloatBuffer normalBuffer = normalBufferData != null ? normalBufferData.getBuffer() : null;

        if (normalBuffer == null) {
            gl.glDisableClientState(GL.GL_NORMAL_ARRAY);
        } else if (_oldNormalBuffer != normalBuffer) {
            gl.glEnableClientState(GL.GL_NORMAL_ARRAY);
            normalBuffer.rewind();
            gl.glNormalPointer(GL.GL_FLOAT, 0, normalBuffer);
        }

        _oldNormalBuffer = normalBuffer;
    }

    public void setupColorData(final FloatBufferData colorBufferData) {
        final GL gl = GLU.getCurrentGL();

        final FloatBuffer colorBuffer = colorBufferData != null ? colorBufferData.getBuffer() : null;

        if (colorBuffer == null) {
            gl.glDisableClientState(GL.GL_COLOR_ARRAY);
        } else if (_oldColorBuffer != colorBuffer) {
            gl.glEnableClientState(GL.GL_COLOR_ARRAY);
            colorBuffer.rewind();
            gl.glColorPointer(colorBufferData.getValuesPerTuple(), GL.GL_FLOAT, 0, colorBuffer);
        }

        _oldColorBuffer = colorBuffer;
    }

    public void setupFogData(final FloatBufferData fogBufferData) {
        final GL gl = GLU.getCurrentGL();

        final FloatBuffer fogBuffer = fogBufferData != null ? fogBufferData.getBuffer() : null;

        if (fogBuffer == null) {
            gl.glDisableClientState(GL.GL_FOG_COORDINATE_ARRAY);
        } else if (_oldFogBuffer != fogBuffer) {
            gl.glEnableClientState(GL.GL_FOG_COORDINATE_ARRAY);
            fogBuffer.rewind();
            gl.glFogCoordPointer(GL.GL_FLOAT, 0, fogBuffer);
        }

        _oldFogBuffer = fogBuffer;
    }

    public void setupTextureData(final List<FloatBufferData> textureCoords) {
        final GL gl = GLU.getCurrentGL();

        final RenderContext context = ContextManager.getCurrentContext();
        final ContextCapabilities caps = context.getCapabilities();
        final RendererRecord rendRecord = context.getRendererRecord();

        final TextureState ts = (TextureState) context.getCurrentState(RenderState.StateType.Texture);
        int enabledTextures = rendRecord.getEnabledTextures();
        final boolean valid = rendRecord.isTexturesValid();
        boolean isOn, wasOn;
        if (ts != null) {
            final int max = caps.isMultitextureSupported() ? Math.min(caps.getNumberOfFragmentTexCoordUnits(),
                    TextureState.MAX_TEXTURES) : 1;
            for (int i = 0; i < max; i++) {
                wasOn = (enabledTextures & (2 << i)) != 0;
                isOn = textureCoords != null && i < textureCoords.size() && textureCoords.get(i) != null
                        && textureCoords.get(i).getBuffer() != null;

                if (!isOn) {
                    if (valid && !wasOn) {
                        continue;
                    } else {
                        if (caps.isMultitextureSupported()) {
                            gl.glClientActiveTexture(GL.GL_TEXTURE0 + i);
                        }

                        // disable bit in tracking int
                        enabledTextures &= ~(2 << i);

                        // disable state
                        gl.glDisableClientState(GL.GL_TEXTURE_COORD_ARRAY);

                        // discard old comparison buffer
                        _oldTextureBuffers[i] = null;

                        continue;
                    }
                } else {
                    if (caps.isMultitextureSupported()) {
                        gl.glClientActiveTexture(GL.GL_TEXTURE0 + i);
                    }

                    if (!valid || !wasOn) {
                        // enable state
                        gl.glEnableClientState(GL.GL_TEXTURE_COORD_ARRAY);

                        // enable bit in tracking int
                        enabledTextures |= (2 << i);
                    }

                    final FloatBufferData textureBufferData = textureCoords.get(i);
                    final FloatBuffer textureBuffer = textureBufferData != null ? textureBufferData.getBuffer() : null;

                    if (_oldTextureBuffers[i] != textureBuffer) {
                        gl.glEnableClientState(GL.GL_TEXTURE_COORD_ARRAY);
                        textureBuffer.rewind();
                        gl.glTexCoordPointer(textureBufferData.getValuesPerTuple(), GL.GL_FLOAT, 0, textureBuffer);
                    }

                    _oldTextureBuffers[i] = textureBuffer;
                }
            }
        }

        rendRecord.setEnabledTextures(enabledTextures);
        rendRecord.setTexturesValid(true);
    }

    public void drawElements(final IntBufferData indices, final int[] indexLengths, final IndexMode[] indexModes) {
        if (indices == null || indices.getBuffer() == null) {
            logger.severe("Missing indices for drawElements call without VBO");
            return;
        }

        final GL gl = GLU.getCurrentGL();

        if (indexLengths == null) {
            final int glIndexMode = getGLIndexMode(indexModes[0]);

            indices.getBuffer().position(0);
            gl.glDrawElements(glIndexMode, indices.getBufferLimit(), GL.GL_UNSIGNED_INT, indices.getBuffer());

            if (Constants.stats) {
                addStats(indexModes[0], indices.getBufferLimit());
            }
        } else {
            int offset = 0;
            int indexModeCounter = 0;
            for (int i = 0; i < indexLengths.length; i++) {
                final int count = indexLengths[i];

                final int glIndexMode = getGLIndexMode(indexModes[indexModeCounter]);

                indices.getBuffer().position(offset);
                indices.getBuffer().limit(offset + count);
                gl.glDrawElements(glIndexMode, count, GL.GL_UNSIGNED_INT, indices.getBuffer());

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

    private int setupVBO(final FloatBufferData data, final RenderContext context, final RendererRecord rendRecord) {
        if (data == null) {
            return -1;
        }

        final GL gl = GLU.getCurrentGL();

        int vboID = data.getVBOID(context.getGlContextRep());
        if (vboID > 0) {
            updateVBO(data, rendRecord, vboID, 0);

            return vboID;
        }

        final FloatBuffer dataBuffer = data.getBuffer();
        if (dataBuffer != null) {
            // XXX: should we be rewinding? Maybe make that the programmer's responsibility.
            dataBuffer.rewind();
            vboID = makeVBOId(rendRecord);
            data.setVBOID(context.getGlContextRep(), vboID);

            rendRecord.invalidateVBO();
            JoglRendererUtil.setBoundVBO(rendRecord, vboID);
            gl.glBufferDataARB(GL.GL_ARRAY_BUFFER_ARB, dataBuffer.limit() * 4, dataBuffer, getGLVBOAccessMode(data
                    .getVboAccessMode()));
        } else {
            throw new Ardor3dException("Attempting to create a vbo id for a FloatBufferData with no Buffer value.");
        }
        return vboID;
    }

    private void updateVBO(final FloatBufferData data, final RendererRecord rendRecord, final int vboID,
            final int offset) {
        if (data.isNeedsRefresh()) {
            final GL gl = GLU.getCurrentGL();
            final FloatBuffer dataBuffer = data.getBuffer();
            dataBuffer.rewind();
            JoglRendererUtil.setBoundVBO(rendRecord, vboID);
            gl.glBufferSubDataARB(GL.GL_ARRAY_BUFFER_ARB, offset, dataBuffer.limit() * 4, dataBuffer);
            data.setNeedsRefresh(false);
        }
    }

    private int setupVBO(final IntBufferData data, final RenderContext context, final RendererRecord rendRecord) {
        if (data == null) {
            return -1;
        }

        final GL gl = GLU.getCurrentGL();

        int vboID = data.getVBOID(context.getGlContextRep());
        if (vboID > 0) {
            if (data.isNeedsRefresh()) {
                final IntBuffer dataBuffer = data.getBuffer();
                dataBuffer.rewind();
                JoglRendererUtil.setBoundElementVBO(rendRecord, vboID);
                gl.glBufferSubDataARB(GL.GL_ELEMENT_ARRAY_BUFFER_ARB, 0, dataBuffer.limit() * 4, dataBuffer);
                data.setNeedsRefresh(false);
            }

            return vboID;
        }

        final IntBuffer dataBuffer = data.getBuffer();
        if (dataBuffer != null) {
            // XXX: should we be rewinding? Maybe make that the programmer's responsibility.
            dataBuffer.rewind();
            vboID = makeVBOId(rendRecord);
            data.setVBOID(context.getGlContextRep(), vboID);

            rendRecord.invalidateVBO();
            JoglRendererUtil.setBoundElementVBO(rendRecord, vboID);
            gl.glBufferDataARB(GL.GL_ELEMENT_ARRAY_BUFFER_ARB, dataBuffer.limit() * 4, dataBuffer,
                    getGLVBOAccessMode(data.getVboAccessMode()));
        } else {
            throw new Ardor3dException("Attempting to create a vbo id for a FloatBufferData with no Buffer value.");
        }
        return vboID;
    }

    public void setupVertexDataVBO(final FloatBufferData data) {
        final GL gl = GLU.getCurrentGL();

        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord rendRecord = context.getRendererRecord();

        final int vboID = setupVBO(data, context, rendRecord);

        if (vboID > 0) {
            gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
            JoglRendererUtil.setBoundVBO(rendRecord, vboID);
            gl.glVertexPointer(data.getValuesPerTuple(), GL.GL_FLOAT, 0, 0);
        } else {
            gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
            JoglRendererUtil.setBoundVBO(rendRecord, 0);
        }
    }

    public void setupNormalDataVBO(final FloatBufferData data) {
        final GL gl = GLU.getCurrentGL();

        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord rendRecord = context.getRendererRecord();

        final int vboID = setupVBO(data, context, rendRecord);

        if (vboID > 0) {
            gl.glEnableClientState(GL.GL_NORMAL_ARRAY);
            JoglRendererUtil.setBoundVBO(rendRecord, vboID);
            gl.glNormalPointer(GL.GL_FLOAT, 0, 0);
        } else {
            gl.glDisableClientState(GL.GL_NORMAL_ARRAY);
            JoglRendererUtil.setBoundVBO(rendRecord, 0);
        }
    }

    public void setupColorDataVBO(final FloatBufferData data) {
        final GL gl = GLU.getCurrentGL();

        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord rendRecord = context.getRendererRecord();

        final int vboID = setupVBO(data, context, rendRecord);

        if (vboID > 0) {
            gl.glEnableClientState(GL.GL_COLOR_ARRAY);
            JoglRendererUtil.setBoundVBO(rendRecord, vboID);
            gl.glColorPointer(data.getValuesPerTuple(), GL.GL_FLOAT, 0, 0);
        } else {
            gl.glDisableClientState(GL.GL_COLOR_ARRAY);
            JoglRendererUtil.setBoundVBO(rendRecord, 0);
        }
    }

    public void setupFogDataVBO(final FloatBufferData data) {
        final GL gl = GLU.getCurrentGL();

        final RenderContext context = ContextManager.getCurrentContext();
        final ContextCapabilities caps = context.getCapabilities();

        if (!caps.isFogCoordinatesSupported()) {
            return;
        }

        final RendererRecord rendRecord = context.getRendererRecord();
        final int vboID = setupVBO(data, context, rendRecord);

        if (vboID > 0) {
            gl.glEnableClientState(GL.GL_FOG_COORDINATE_ARRAY_EXT);
            JoglRendererUtil.setBoundVBO(rendRecord, vboID);
            gl.glFogCoordPointerEXT(GL.GL_FLOAT, 0, 0);
        } else {
            gl.glDisableClientState(GL.GL_FOG_COORDINATE_ARRAY_EXT);
            JoglRendererUtil.setBoundVBO(rendRecord, 0);
        }
    }

    public void setupTextureDataVBO(final List<FloatBufferData> textureCoords) {
        final GL gl = GLU.getCurrentGL();

        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord rendRecord = context.getRendererRecord();
        final ContextCapabilities caps = context.getCapabilities();

        final TextureState ts = (TextureState) context.getCurrentState(RenderState.StateType.Texture);
        int enabledTextures = rendRecord.getEnabledTextures();
        final boolean valid = rendRecord.isTexturesValid();
        boolean exists, wasOn;
        if (ts != null) {
            final int max = caps.isMultitextureSupported() ? Math.min(caps.getNumberOfFragmentTexCoordUnits(),
                    TextureState.MAX_TEXTURES) : 1;
            for (int i = 0; i < max; i++) {
                wasOn = (enabledTextures & (2 << i)) != 0;
                exists = textureCoords != null && i < textureCoords.size();

                if (!exists) {
                    if (valid && !wasOn) {
                        continue;
                    } else {
                        if (caps.isMultitextureSupported()) {
                            gl.glClientActiveTexture(GL.GL_TEXTURE0 + i);
                        }

                        // disable bit in tracking int
                        enabledTextures &= ~(2 << i);

                        // disable state
                        gl.glDisableClientState(GL.GL_TEXTURE_COORD_ARRAY);

                        // discard old comparison buffer
                        _oldTextureBuffers[i] = null;

                        continue;
                    }
                } else {

                    if (caps.isMultitextureSupported()) {
                        gl.glClientActiveTexture(GL.GL_TEXTURE0 + i);
                    }

                    // grab a vboID and make sure it exists and is up to date.
                    final FloatBufferData data = textureCoords.get(i);
                    final int vboID = setupVBO(data, context, rendRecord);

                    // Found good vbo
                    if (vboID > 0) {
                        if (!valid || !wasOn) {
                            // enable bit in tracking int
                            enabledTextures |= (2 << i);

                            // enable state
                            gl.glEnableClientState(GL.GL_TEXTURE_COORD_ARRAY);
                        }

                        // set our active vbo
                        JoglRendererUtil.setBoundVBO(rendRecord, vboID);

                        // send data
                        gl.glTexCoordPointer(data.getValuesPerTuple(), GL.GL_FLOAT, 0, 0);
                    }
                    // Not a good vbo, disable it.
                    else {
                        if (!valid || wasOn) {
                            // disable bit in tracking int
                            enabledTextures &= ~(2 << i);

                            // disable state
                            gl.glDisableClientState(GL.GL_TEXTURE_COORD_ARRAY);
                        }

                        // set our active vbo to 0
                        JoglRendererUtil.setBoundVBO(rendRecord, 0);
                    }
                }
            }
        }

        rendRecord.setEnabledTextures(enabledTextures);
        rendRecord.setTexturesValid(true);
    }

    public void setupInterleavedDataVBO(final FloatBufferData interleaved, final FloatBufferData vertexCoords,
            final FloatBufferData normalCoords, final FloatBufferData colorCoords,
            final List<FloatBufferData> textureCoords) {
        final GL gl = GLU.getCurrentGL();

        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord rendRecord = context.getRendererRecord();
        final ContextCapabilities caps = context.getCapabilities();

        if (interleaved.getVBOID(context.getGlContextRep()) <= 0 || interleaved.isNeedsRefresh()) {
            initializeInterleavedVBO(context, interleaved, vertexCoords, normalCoords, colorCoords, textureCoords);
        }

        final int vboID = interleaved.getVBOID(context.getGlContextRep());
        JoglRendererUtil.setBoundVBO(rendRecord, vboID);

        int offset = 0;

        if (normalCoords != null) {
            updateVBO(normalCoords, rendRecord, vboID, offset);
            gl.glNormalPointer(GL.GL_FLOAT, 0, offset);
            gl.glEnableClientState(GL.GL_NORMAL_ARRAY);
            offset += normalCoords.getBufferLimit() * 4;
        } else {
            gl.glDisableClientState(GL.GL_NORMAL_ARRAY);
        }

        if (colorCoords != null) {
            updateVBO(colorCoords, rendRecord, vboID, offset);
            gl.glColorPointer(colorCoords.getValuesPerTuple(), GL.GL_FLOAT, 0, offset);
            gl.glEnableClientState(GL.GL_COLOR_ARRAY);
            offset += colorCoords.getBufferLimit() * 4;
        } else {
            gl.glDisableClientState(GL.GL_COLOR_ARRAY);
        }

        if (textureCoords != null) {
            final TextureState ts = (TextureState) context.getCurrentState(RenderState.StateType.Texture);
            int enabledTextures = rendRecord.getEnabledTextures();
            final boolean valid = rendRecord.isTexturesValid();
            boolean exists, wasOn;
            if (ts != null) {
                final int max = caps.isMultitextureSupported() ? Math.min(caps.getNumberOfFragmentTexCoordUnits(),
                        TextureState.MAX_TEXTURES) : 1;
                for (int i = 0; i < max; i++) {
                    wasOn = (enabledTextures & (2 << i)) != 0;
                    exists = textureCoords != null && i < textureCoords.size() && textureCoords.get(i) != null;

                    if (!exists) {
                        if (valid && !wasOn) {
                            continue;
                        } else {
                            if (caps.isMultitextureSupported()) {
                                gl.glClientActiveTexture(GL.GL_TEXTURE0 + i);
                            }

                            // disable bit in tracking int
                            enabledTextures &= ~(2 << i);

                            // disable state
                            gl.glDisableClientState(GL.GL_TEXTURE_COORD_ARRAY);

                            // discard old comparison buffer
                            _oldTextureBuffers[i] = null;

                            continue;
                        }

                    } else {

                        if (caps.isMultitextureSupported()) {
                            gl.glClientActiveTexture(GL.GL_TEXTURE0 + i);
                        }

                        // grab a vboID and make sure it exists and is up to date.
                        final FloatBufferData textureBufferData = textureCoords.get(i);
                        updateVBO(textureBufferData, rendRecord, vboID, offset);

                        if (!valid || !wasOn) {
                            // enable bit in tracking int
                            enabledTextures |= (2 << i);

                            // enable state
                            gl.glEnableClientState(GL.GL_TEXTURE_COORD_ARRAY);
                        }

                        // send data
                        gl.glTexCoordPointer(textureBufferData.getValuesPerTuple(), GL.GL_FLOAT, 0, offset);
                        offset += textureBufferData.getBufferLimit() * 4;
                    }
                }
            }

            rendRecord.setEnabledTextures(enabledTextures);
            rendRecord.setTexturesValid(true);
        }

        if (vertexCoords != null) {
            updateVBO(vertexCoords, rendRecord, vboID, offset);
            gl.glVertexPointer(vertexCoords.getValuesPerTuple(), GL.GL_FLOAT, 0, offset);
            gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
            offset += vertexCoords.getBufferLimit() * 4;
        } else {
            gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
        }

        JoglRendererUtil.setBoundVBO(rendRecord, 0);
    }

    private void initializeInterleavedVBO(final RenderContext context, final FloatBufferData interleaved,
            final FloatBufferData vertexCoords, final FloatBufferData normalCoords, final FloatBufferData colorCoords,
            final List<FloatBufferData> textureCoords) {
        final GL gl = GLU.getCurrentGL();

        final RendererRecord rendRecord = context.getRendererRecord();
        final ContextCapabilities caps = context.getCapabilities();

        final int vboID = makeVBOId(rendRecord);
        interleaved.setVBOID(context.getGlContextRep(), vboID);

        rendRecord.invalidateVBO();
        JoglRendererUtil.setBoundVBO(rendRecord, vboID);
        final int bufferSize = getTotalInterleavedSize(context, vertexCoords, normalCoords, colorCoords, textureCoords);
        gl
                .glBufferDataARB(GL.GL_ARRAY_BUFFER_ARB, bufferSize, null, getGLVBOAccessMode(interleaved
                        .getVboAccessMode()));

        int offset = 0;
        if (normalCoords != null) {
            normalCoords.getBuffer().rewind();
            gl.glBufferSubDataARB(GL.GL_ARRAY_BUFFER_ARB, offset, normalCoords.getBufferLimit() * 4, normalCoords
                    .getBuffer());
            offset += normalCoords.getBufferLimit() * 4;
        }
        if (colorCoords != null) {
            colorCoords.getBuffer().rewind();
            gl.glBufferSubDataARB(GL.GL_ARRAY_BUFFER_ARB, offset, colorCoords.getBufferLimit() * 4, colorCoords
                    .getBuffer());
            offset += colorCoords.getBufferLimit() * 4;
        }
        if (textureCoords != null) {
            final TextureState ts = (TextureState) context.getCurrentState(RenderState.StateType.Texture);
            if (ts != null) {
                for (int i = 0; i <= ts.getMaxTextureIndexUsed() && i < caps.getNumberOfFragmentTexCoordUnits(); i++) {
                    if (textureCoords == null || i >= textureCoords.size()) {
                        continue;
                    }

                    final FloatBufferData textureBufferData = textureCoords.get(i);
                    final FloatBuffer textureBuffer = textureBufferData != null ? textureBufferData.getBuffer() : null;
                    if (textureBuffer != null) {
                        textureBuffer.rewind();
                        gl.glBufferSubDataARB(GL.GL_ARRAY_BUFFER_ARB, offset, textureBufferData.getBufferLimit() * 4,
                                textureBuffer);
                        offset += textureBufferData.getBufferLimit() * 4;
                    }
                }
            }
        }
        if (vertexCoords != null) {
            vertexCoords.getBuffer().rewind();
            gl.glBufferSubDataARB(GL.GL_ARRAY_BUFFER_ARB, offset, vertexCoords.getBufferLimit() * 4, vertexCoords
                    .getBuffer());
            offset += vertexCoords.getBufferLimit() * 4;
        }

        interleaved.setNeedsRefresh(false);
    }

    public void drawElementsVBO(final IntBufferData indices, final int[] indexLengths, final IndexMode[] indexModes) {
        final GL gl = GLU.getCurrentGL();

        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord rendRecord = context.getRendererRecord();

        final int vboID = setupVBO(indices, context, rendRecord);

        JoglRendererUtil.setBoundElementVBO(rendRecord, vboID);

        if (indexLengths == null) {
            final int glIndexMode = getGLIndexMode(indexModes[0]);

            gl.glDrawElements(glIndexMode, indices.getBufferLimit(), GL.GL_UNSIGNED_INT, 0);
            if (Constants.stats) {
                addStats(indexModes[0], indices.getBufferLimit());
            }
        } else {
            int offset = 0;
            int indexModeCounter = 0;
            for (int i = 0; i < indexLengths.length; i++) {
                final int count = indexLengths[i];

                final int glIndexMode = getGLIndexMode(indexModes[indexModeCounter]);

                // offset in this call is done in bytes.
                gl.glDrawElements(glIndexMode, count, GL.GL_UNSIGNED_INT, offset * 4);
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

    public void drawArrays(final FloatBufferData vertexBuffer, final int[] indexLengths, final IndexMode[] indexModes) {
        final GL gl = GLU.getCurrentGL();

        if (indexLengths == null) {
            final int glIndexMode = getGLIndexMode(indexModes[0]);

            gl.glDrawArrays(glIndexMode, 0, vertexBuffer.getTupleCount());

            if (Constants.stats) {
                addStats(indexModes[0], vertexBuffer.getTupleCount());
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

    public int makeVBOId(final RendererRecord rendRecord) {
        final GL gl = GLU.getCurrentGL();

        final IntBuffer idBuff = BufferUtils.createIntBuffer(1);
        gl.glGenBuffersARB(1, idBuff);
        return idBuff.get(0);
    }

    public void unbindVBO() {
        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord rendRecord = context.getRendererRecord();
        JoglRendererUtil.setBoundVBO(rendRecord, 0);
        JoglRendererUtil.setBoundElementVBO(rendRecord, 0);
    }

    private int getGLVBOAccessMode(final VBOAccessMode vboAccessMode) {
        int glMode = GL.GL_STATIC_DRAW_ARB;
        switch (vboAccessMode) {
            case StaticDraw:
                glMode = GL.GL_STATIC_DRAW_ARB;
                break;
            case StaticRead:
                glMode = GL.GL_STATIC_READ_ARB;
                break;
            case StaticCopy:
                glMode = GL.GL_STATIC_COPY_ARB;
                break;
            case DynamicDraw:
                glMode = GL.GL_DYNAMIC_DRAW_ARB;
                break;
            case DynamicRead:
                glMode = GL.GL_DYNAMIC_READ_ARB;
                break;
            case DynamicCopy:
                glMode = GL.GL_DYNAMIC_COPY_ARB;
                break;
            case StreamDraw:
                glMode = GL.GL_STREAM_DRAW_ARB;
                break;
            case StreamRead:
                glMode = GL.GL_STREAM_READ_ARB;
                break;
            case StreamCopy:
                glMode = GL.GL_STREAM_COPY_ARB;
                break;
        }
        return glMode;
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

    public void deleteTexture(final Texture texture) {
        JoglTextureStateUtil.deleteTexture(texture);
    }

    public void loadTexture(final Texture texture, final int unit) {
        JoglTextureStateUtil.load(texture, unit);
    }

    public void deleteTextureIds(final Collection<Integer> ids) {
        JoglTextureStateUtil.deleteTextureIds(ids);
    }

    /**
     * Start a new display list. All further renderer commands that can be stored in a display list are part of this new
     * list until {@link #endDisplayList()} is called.
     * 
     * @return id of new display list
     */
    public int startDisplayList() {
        final GL gl = GLU.getCurrentGL();

        final int id = gl.glGenLists(1);

        gl.glNewList(id, GL.GL_COMPILE);

        return id;
    }

    /**
     * Ends a display list. Will likely cause an OpenGL exception is a display list is not currently being generated.
     */
    public void endDisplayList() {
        GLU.getCurrentGL().glEndList();
    }

    /**
     * Draw the given display list.
     */
    public void renderDisplayList(final int displayListID) {
        final GL gl = GLU.getCurrentGL();

        gl.glCallList(displayListID);

        // invalidate "current arrays"
        reset();
    }

    public void clearClips() {
        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord record = context.getRendererRecord();
        record.getScissorClips().clear();

        JoglRendererUtil.applyScissors(record);
    }

    public void popClip() {
        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord record = context.getRendererRecord();
        record.getScissorClips().pop();

        JoglRendererUtil.applyScissors(record);
    }

    public void pushClip(final int x, final int y, final int width, final int height) {
        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord record = context.getRendererRecord();
        record.getScissorClips().push(new Rectangle2(x, y, width, height));

        JoglRendererUtil.applyScissors(record);
    }

    public void pushEmptyClip() {
        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord record = context.getRendererRecord();
        record.getScissorClips().push(null);

        JoglRendererUtil.applyScissors(record);
    }

    public void setClipTestEnabled(final boolean enabled) {
        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord record = context.getRendererRecord();

        JoglRendererUtil.setClippingEnabled(record, enabled);
    }
}