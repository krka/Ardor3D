/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.framework;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ardor3d.annotation.GuardedBy;
import com.ardor3d.annotation.MainThread;
import com.ardor3d.util.Timer;
import com.google.inject.Inject;

/**
 * Does the work needed in a given frame.
 */
public final class FrameHandler {
    private static final Logger logger = Logger.getLogger(FrameHandler.class.toString());

    /**
     * Thread synchronization of the updaters list is delegated to the CopyOnWriteArrayList.
     */
    private final List<Updater> _updaters;

    /**
     * Canvases is both protected by an intrinsic lock and by the fact that it is a CopyOnWriteArrayList. This is
     * because it is necessary to check the size of the list and then fetch an iterator that is guaranteed to iterate
     * over that number of elements. See {@link #updateFrame()} for the code that does this.
     */
    @GuardedBy("this")
    private final List<Canvas> _canvases;
    private final Timer _timer;

    @Inject
    public FrameHandler(final Timer timer) {
        _timer = timer;
        _updaters = new CopyOnWriteArrayList<Updater>();
        _canvases = new CopyOnWriteArrayList<Canvas>();
    }

    @MainThread
    public void updateFrame() {
        // calculate tpf
        // update updaters
        // draw canvases

        _timer.update();

        final double tpf = _timer.getTimePerFrame();

        // using the CopyOnWriteArrayList synchronization here, since that means
        // that we don't have to hold any locks while calling Updater.update(double),
        // and also makes the code simple. An updater that is registered after the below
        // loop has started will be updated at the next call to updateFrame().
        for (final Updater updater : _updaters) {
            updater.update(tpf);
        }

        int numCanvases;
        Iterator<Canvas> iterator;

        // make sure that there is no race condition with registerCanvas - getting the iterator and
        // the number of canvases currently in the list in a synchronized section, and ensuring that
        // the registerCanvas() method is also synchronized on this, means that they will
        // both remain valid outside the section later, when we call the probably long-running, alien
        // draw() methods. Since 'canvases' is a CopyOnWriteArrayList, the iterator is guaranteed to
        // be valid outside the synchronized section, and getting them both inside the synchronized section
        // means that the number of canvases read will be the same as the number of elements the iterator
        // will iterate over.
        synchronized (this) {
            numCanvases = _canvases.size();
            iterator = _canvases.iterator();
        }

        final CountDownLatch latch = new CountDownLatch(numCanvases);

        while (iterator.hasNext()) {
            iterator.next().draw(latch);
        }

        try {
            // wait for all canvases to be drawn - the reason for using the latch is that
            // in some cases (AWT, for instance), the thread that calls canvas.draw() is not the
            // one that holds the OpenGL context, which means that drawing is simply queued.
            // When the actual OpenGL rendering has been done, the OpenGL thread will countdown
            // on the latch, and once all the canvases have finished rendering, this method
            // will return.
            final boolean success = latch.await(5, TimeUnit.SECONDS);

            if (!success) {
                logger.logp(Level.SEVERE, FrameHandler.class.toString(), "updateFrame",
                        "Timeout while waiting for renderers");
                // FIXME: should probably reset update flag in canvases?
            }
        } catch (final InterruptedException e) {
            // restore updated status
            Thread.currentThread().interrupt();
        }
    }

    public void registerUpdater(final Updater updater) {
        _updaters.add(updater);
    }

    public boolean removeUpdater(final Updater updater) {
        return _updaters.remove(updater);
    }

    public synchronized void registerCanvas(final Canvas canvas) {
        _canvases.add(canvas);
    }

    public synchronized boolean removeCanvas(final Canvas canvas) {
        return _canvases.remove(canvas);
    }

    public void init() {
        // TODO: this can lead to problems with canvases and updaters added after init() has been called once...
        for (final Canvas canvas : _canvases) {
            canvas.init();
        }

        for (final Updater updater : _updaters) {
            updater.init();
        }

    }
}
