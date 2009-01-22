/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.screen;

import java.nio.ByteBuffer;

import com.ardor3d.image.Image;
import com.ardor3d.image.Image.Format;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.util.geom.BufferUtils;

public class ScreenExporter {

    static ByteBuffer scratch = BufferUtils.createByteBuffer(1);

    public synchronized static void exportCurrentScreen(final Renderer renderer, final ScreenExportable exportable) {
        final Format format = exportable.getFormat();
        final Camera camera = ContextManager.getCurrentContext().getCurrentCamera();
        final int width = camera.getWidth(), height = camera.getHeight();

        // prepare our data buffer
        final int size = width * height * Image.getEstimatedByteSize(format);
        if (scratch.capacity() < size) {
            scratch = BufferUtils.createByteBuffer(size);
        } else {
            scratch.limit(size);
            scratch.rewind();
        }

        // Ask the renderer for the current scene to be stored in the buffer
        renderer.grabScreenContents(scratch, format, 0, 0, width, height);

        // send the buffer to the exportable object for processing.
        exportable.export(scratch, width, height);
    }
}
