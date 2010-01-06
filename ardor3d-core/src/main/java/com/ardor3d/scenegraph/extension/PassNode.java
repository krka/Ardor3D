/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scenegraph.extension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

public class PassNode extends Node {
    private static final long serialVersionUID = 1L;

    private List<PassNodeState> _passNodeStates = new ArrayList<PassNodeState>();

    public PassNode(final String name) {
        super(name);
    }

    public PassNode() {
        super();
    }

    @Override
    public void draw(final Renderer r) {
        if (_children == null) {
            return;
        }

        final RenderContext context = ContextManager.getCurrentContext();
        r.getQueue().pushBuckets();
        for (final PassNodeState pass : _passNodeStates) {
            if (!pass.isEnabled()) {
                continue;
            }

            pass.applyPassNodeStates(context);

            Spatial child;
            for (int i = 0, cSize = _children.size(); i < cSize; i++) {
                child = _children.get(i);
                if (child != null) {
                    child.onDraw(r);
                }
            }
            r.renderBuckets();

            context.popEnforcedStates();
        }
        r.getQueue().popBuckets();
    }

    public void addPass(final PassNodeState toAdd) {
        _passNodeStates.add(toAdd);
    }

    public void insertPass(final PassNodeState toAdd, final int index) {
        _passNodeStates.add(index, toAdd);
    }

    public boolean containsPass(final PassNodeState s) {
        return _passNodeStates.contains(s);
    }

    public boolean removePass(final PassNodeState toRemove) {
        return _passNodeStates.remove(toRemove);
    }

    public PassNodeState getPass(final int index) {
        return _passNodeStates.get(index);
    }

    public int nrPasses() {
        return _passNodeStates.size();
    }

    public void clearAll() {
        _passNodeStates.clear();
    }

    @Override
    public void write(final Ardor3DExporter e) throws IOException {
        super.write(e);
        final OutputCapsule capsule = e.getCapsule(this);
        capsule.writeSavableList(_passNodeStates, "passNodeStates", null);
    }

    @Override
    public void read(final Ardor3DImporter e) throws IOException {
        super.read(e);
        final InputCapsule capsule = e.getCapsule(this);
        _passNodeStates = capsule.readSavableList("passNodeStates", null);
    }
}
