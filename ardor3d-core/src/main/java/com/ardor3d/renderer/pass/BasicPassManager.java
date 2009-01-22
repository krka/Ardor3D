/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.pass;

import java.util.ArrayList;
import java.util.List;

import com.ardor3d.renderer.Renderer;

/**
 * <code>BasicPassManager</code> controls a set of passes and sends through calls to render and update.
 */
public class BasicPassManager {

    protected List<Pass> passes = new ArrayList<Pass>();

    public void add(final Pass toAdd) {
        if (toAdd != null) {
            passes.add(toAdd);
        }
    }

    public void insert(final Pass toAdd, final int index) {
        passes.add(index, toAdd);
    }

    public boolean contains(final Pass s) {
        return passes.contains(s);
    }

    public boolean remove(final Pass toRemove) {
        return passes.remove(toRemove);
    }

    public Pass get(final int index) {
        return passes.get(index);
    }

    public int passes() {
        return passes.size();
    }

    public void clearAll() {
        cleanUp();
        passes.clear();
    }

    public void cleanUp() {
        for (int i = 0, sSize = passes.size(); i < sSize; i++) {
            final Pass p = passes.get(i);
            p.cleanUp();
        }
    }

    public void renderPasses(final Renderer r) {
        for (int i = 0, sSize = passes.size(); i < sSize; i++) {
            final Pass p = passes.get(i);
            p.renderPass(r);
        }
    }

    public void updatePasses(final double tpf) {
        for (int i = 0, sSize = passes.size(); i < sSize; i++) {
            final Pass p = passes.get(i);
            p.updatePass(tpf);
        }
    }

}
