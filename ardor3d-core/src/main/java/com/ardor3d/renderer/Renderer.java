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

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.EnumMap;
import java.util.List;

import com.ardor3d.image.Image;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Image.Format;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Transform;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.queue.RenderQueue;
import com.ardor3d.renderer.state.LightState;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.scenegraph.Renderable;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.TexCoords;
import com.ardor3d.scenegraph.VBOInfo;
import com.ardor3d.util.Ardor3dException;

/**
 * <code>Renderer</code> defines an interface for classes that handle displaying of graphics data to a render context.
 * 
 * All rendering state and tasks should be handled through this class.
 */
public interface Renderer {

    /**
     * <code>setBackgroundColor</code> sets the color of window. This color will be shown for any pixel that is not set
     * via typical rendering operations.
     * 
     * @param c
     *            the color to set the background to.
     */
    void setBackgroundColor(ReadOnlyColorRGBA c);

    /**
     * <code>getBackgroundColor</code> retrieves the clear color of the current OpenGL context.
     * 
     * @return the current clear color.
     */
    ReadOnlyColorRGBA getBackgroundColor();

    /**
     * <code>clearZBuffer</code> clears the depth buffer of the renderer. The Z buffer allows sorting of pixels by depth
     * or distance from the view port. Clearing this buffer prepares it for the next frame.
     * 
     */
    void clearZBuffer();

    /**
     * <code>clearBackBuffer</code> clears the back buffer of the renderer. The backbuffer is the buffer being rendered
     * to before it is displayed to the screen. Clearing this buffer frees it for rendering the next frame.
     * 
     */
    void clearColorBuffer();

    /**
     * <code>clearStencilBuffer</code> clears the stencil buffer of the renderer.
     */
    void clearStencilBuffer();

    /**
     * <code>clearBuffers</code> clears both the depth buffer and the back buffer.
     * 
     */
    void clearBuffers();

    /**
     * <code>clearStrictBuffers</code> clears both the depth buffer and the back buffer restricting the clear to the
     * rectangle defined by the width and height of the renderer.
     * 
     */
    void clearStrictBuffers();

    /**
     * <code>flushFrame</code> handles rendering any items still remaining in the render buckets and optionally swaps
     * the back buffer with the currently displayed buffer.
     * 
     * @param doSwap
     *            if true, will ask the underlying implementation to blit the back buffer contents to the display
     *            buffer. Usually this will be true, unless you are in a windowing toolkit that handles doing this for
     *            you.
     */
    void flushFrame(boolean doSwap);

    /**
     * 
     * <code>setOrtho</code> sets the display system to be in orthographic mode. If the system has already been set to
     * orthographic mode a <code>Ardor3dException</code> is thrown. The origin (0,0) is the bottom left of the screen.
     * 
     */
    void setOrtho();

    /**
     * 
     * <code>unsetOrhto</code> unsets the display system from orthographic mode back into regular projection mode. If
     * the system is not in orthographic mode a <code>Ardor3dException</code> is thrown.
     * 
     * 
     */
    void unsetOrtho();

    /**
     * @return true if the renderer is currently in ortho mode.
     */
    boolean isInOrthoMode();

    /**
     * render queue if needed
     */
    void renderBuckets();

    /**
     * clear the render queue
     */
    void clearQueue();

    /**
     * <code>grabScreenContents</code> reads a block of data as bytes from the current framebuffer. The format
     * determines how many bytes per pixel are read and thus how big the buffer must be that you pass in.
     * 
     * @param buff
     *            a buffer to store contents in.
     * @param format
     *            the format to read in bytes for.
     * @param x
     *            - x starting point of block
     * @param y
     *            - y starting point of block
     * @param w
     *            - width of block
     * @param h
     *            - height of block
     */
    void grabScreenContents(ByteBuffer buff, Image.Format format, int x, int y, int w, int h);

    /**
     * <code>draw</code> renders a scene. As it receives a base class of <code>Spatial</code> the renderer hands off
     * management of the scene to spatial for it to determine when a <code>Geometry</code> leaf is reached.
     * 
     * @param s
     *            the scene to render.
     */
    void draw(Spatial s);

    /**
     * <code>flush</code> tells the graphics hardware to send through all currently waiting commands in the buffer.
     */
    void flushGraphics();

    /**
     * <code>finish</code> is similar to flush, however it blocks until all waiting hardware graphics commands have been
     * finished.
     */
    void finishGraphics();

    /**
     * Get the render queue associated with this Renderer.
     * 
     * @return RenderQueue
     */
    RenderQueue getQueue();

    /**
     * Return true if this renderer is in the middle of processing its RenderQueue.
     * 
     * @return boolean
     */
    boolean isProcessingQueue();

    /**
     * Check a given Spatial to see if it should be queued. return true if it was queued.
     * 
     * @param s
     *            Spatial to check
     * @return true if it was queued.
     */
    boolean checkAndAdd(Spatial s);

    /**
     * Checks the VBO cache to see if this Buffer is mapped to a VBO-id. If it does the mapping will be removed from the
     * cache and the VBO with the VBO-id found will be deleted.
     * 
     * If no mapped VBO-id is found, this method does not do anything else.
     * 
     * @param buffer
     *            The Buffer who's associated VBO should be deleted.
     */
    void deleteVBO(Buffer buffer);

    /**
     * Attempts to delete the VBO with this VBO id. Ignores ids < 1.
     * 
     * @param vboid
     */
    void deleteVBO(int vboid);

    /**
     * Clears all entries from the VBO cache. Does not actually delete any VBO buffer, only all mappings between Buffers
     * and VBO-ids.
     * 
     */
    void clearVBOCache();

    /**
     * Removes the mapping between this Buffer and it's VBO-id. Does not actually delete the VBO. <br>
     * This method is useful if you want to use the same Buffer to create several VBOs. After a VBO is created for this
     * Buffer, update the Buffer and remove it from the VBO cache. You can now reuse the same buffer with another Mesh
     * object. <br>
     * If no association is found, this method does nothing.
     * 
     * @param buffer
     *            The nio Buffer whose associated VBO should be deleted.
     * @return An int wrapped in an Integer object that's the VBO-id of the VBO previously mapped to this Buffer, or
     *         null is no mapping existed.
     */
    Integer removeFromVBOCache(Buffer buffer);

    /**
     * Updates a region of the content area of the provided texture using the specified region of the given data.
     * 
     * @param dstTexture
     *            the texture to be updated
     * @param dstX
     *            the x offset relative to the lower-left corner of this texture where the update will be applied
     * @param dstY
     *            the y offset relative to the lower-left corner of this texture where the update will be applied
     * @param srcImage
     *            the image data to be uploaded to the texture
     * @param srcX
     *            the x offset relative to the lower-left corner of the supplied buffer from which to fetch the update
     *            rectangle
     * @param srcY
     *            the y offset relative to the lower-left corner of the supplied buffer from which to fetch the update
     *            rectangle
     * @param width
     *            the width of the region to be updated
     * @param height
     *            the height of the region to be updated
     * @throws Ardor3dException
     *             if unable to update the texture
     * @throws UnsupportedOperationException
     *             if updating for the provided texture type is unsupported
     * @see com.sun.opengl.util.texture.Texture#updateSubImage(com.sun.opengl.util.texture.TextureData, int, int, int,
     *      int, int, int, int)
     * @since 2.0
     */
    void updateTextureSubImage(final Texture dstTexture, final Image srcImage, final int srcX, final int srcY,
            final int dstX, final int dstY, final int dstWidth, final int dstHeight) throws Ardor3dException,
            UnsupportedOperationException;

    void updateTextureSubImage(final Texture dstTexture, final ByteBuffer data, final int srcX, final int srcY,
            final int srcWidth, final int srcHeight, final int dstX, final int dstY, final int dstWidth,
            final int dstHeight, final Format format) throws Ardor3dException, UnsupportedOperationException;

    /**
     * Check the underlying rendering system (opengl, etc.) for exceptions.
     * 
     * @throws Ardor3dException
     *             if an error is found.
     */
    void checkCardError() throws Ardor3dException;

    /**
     * Perform any necessary cleanup operations such as deleting VBOs, etc.
     */
    void cleanup();

    /**
     * <code>draw</code> renders the renderable to the back buffer.
     * 
     * @param renderable
     *            the text object to be rendered.
     */
    void draw(final Renderable renderable);

    /**
     * @param states
     *            states to apply. Any LightState will be ignored.
     */
    void applyNonLightStates(final EnumMap<StateType, RenderState> states);

    /**
     * @param state
     *            the LightState to apply.
     */
    void applyLightState(LightState state);

    /**
     * <code>doTransforms</code> sets the current transform.
     * 
     * @param transform
     *            transform to apply.
     */
    boolean doTransforms(final Transform transform);

    /**
     * <code>undoTransforms</code> reverts the latest transform.
     * 
     * @param transform
     *            transform to revert.
     */
    void undoTransforms(final Transform transform);

    /**
     * <code>setupVertexData</code> sets up the buffers for vertex data.
     * 
     * @param vertices
     *            transform to apply.
     */
    void setupVertexData(final FloatBuffer vertexBuffer, final VBOInfo vbo);

    void setupNormalData(final FloatBuffer normalBuffer, final NormalsMode normalMode, final Transform worldTransform,
            final VBOInfo vbo);

    void setupColorData(final FloatBuffer colorBuffer, final VBOInfo vbo, final ColorRGBA defaultColor);

    void setupFogData(final FloatBuffer fogBuffer, final VBOInfo vbo);

    void setupTextureData(final List<TexCoords> textureCoords, final VBOInfo vbo);

    void setupInterleavedData(final FloatBuffer interleavedBuffer, InterleavedFormat format, final VBOInfo vbo);

    void drawElements(final IntBuffer indices, final VBOInfo vbo, final int[] indexLengths, final IndexMode[] indexModes);

    void drawArrays(final FloatBuffer vertexBuffer, final int[] indexLengths, final IndexMode[] indexModes);

    void renderDisplayList(final int displayListID);

    void setProjectionMatrix(Buffer matrix);

    /**
     * Gets the current projection matrix in row major order
     * 
     * @param store
     *            The buffer to store in. Must be a FloatBuffer or DoubleBuffer or null. If null or remaining is < 16, a
     *            new Buffer (DoubleBuffer for null) will be created and returned.
     * @return
     */
    Buffer getProjectionMatrix(Buffer store);

    void setModelViewMatrix(Buffer matrix);

    /**
     * Gets the current modelview matrix in row major order
     * 
     * @param store
     *            The buffer to store in. Must be a FloatBuffer or DoubleBuffer or null. If null or remaining is < 16, a
     *            new Buffer (DoubleBuffer for null) will be created and returned.
     * @return
     */
    Buffer getModelViewMatrix(Buffer store);

    void setViewport(int x, int y, int width, int height);

    void setDepthRange(double depthRangeNear, double depthRangeFar);

    /**
     * Specify which color buffers are to be drawn into.
     * 
     * @param target
     */
    void setDrawBuffer(DrawBufferTarget target);

    /**
     * This is a workaround until we make shared Record classes, or open up lower level opengl calls abstracted from
     * lwjgl/jogl.
     * 
     * @param lineWidth
     * @param stippleFactor
     * @param stipplePattern
     * @param antialiased
     */
    void setupLineParameters(float lineWidth, int stippleFactor, short stipplePattern, boolean antialiased);

    /**
     * This is a workaround until we make shared Record classes, or open up lower level opengl calls abstracted from
     * lwjgl/jogl.
     * 
     * @param pointSize
     * @param antialiased
     */
    void setupPointParameters(float pointSize, boolean antialiased);

    /**
     * Apply the given state to the current RenderContext using this Renderer.
     * 
     * @param state
     */
    void applyState(RenderState state);

    void loadTexture(Texture texture, int unit);

    void deleteTextureId(int textureId);
}
