/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.collada.binding;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.jibx.runtime.IUnmarshallingContext;

import com.ardor3d.extension.model.collada.binding.core.Collada;

public abstract class DaeTreeNode {
    // generic Collada attributes - may not be used by all node types
    private String id;
    private String sid;
    private String name;

    // tree navigation members that don't actually relate to Collada
    private DaeTreeNode parent;
    private final LinkedList<DaeTreeNode> children;

    public DaeTreeNode() {
        children = new LinkedList<DaeTreeNode>();
        parent = null;
    }

    protected void postset(final Object parent) {
        registerParent(parent);

        if (id != null && id.length() > 0) {
            getRootNode().mapId(id, this);
        }
    }

    protected void postsetSpecifiedInAbstract(final IUnmarshallingContext ctx) {
        if (ctx.getStackDepth() > 1) {
            postset(ctx.getStackObject(1));
        }
    }

    protected void registerParent(final Object parent) {
        // parent == null should only happen for the root COLLADA node
        if (parent == null) {
            if (!(this instanceof Collada)) {
                throw new ColladaException("ERROR: null parent for object of class: " + getClass(), this);
            }
        } else {
            try {
                final DaeTreeNode parentNode = (DaeTreeNode) parent;
                parentNode.addChild(this);

                this.parent = parentNode;
            } catch (final ClassCastException e) {
                throw new ColladaException("ERROR: unable to cast parent of class: " + parent.getClass()
                        + " to ColladaTreeNode - child class is: " + getClass(), this);
            }
        }
    }

    public Collada getRootNode() {
        if (parent == null) {
            try {
                return (Collada) this;
            } catch (final ClassCastException e) {
                throw new ColladaException("SHOULD NOT BE POSSIBLE: unable to cast root node of class " + getClass()
                        + " to Collada class", this, e);
            }
        } else {
            return parent.getRootNode();
        }
    }

    protected void addChild(final DaeTreeNode child) {
        if (child == this) {
            throw new ColladaException("ERROR: trying to add child to itself", this);
        }

        children.add(child);
    }

    public List<DaeTreeNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public DaeTreeNode findSubNode(final String targetPart) {
        // target part as per Collada spec - NOTE that this is only a tiny partial implementation!

        if (sid != null && sid.equals(targetPart)) {
            return this;
        }

        for (final DaeTreeNode child : children) {
            final DaeTreeNode result = child.findSubNode(targetPart);

            if (result != null) {
                return result;
            }
        }

        return null;
    }

    public String getId() {
        return id;
    }

    protected void setId(final String id) {
        this.id = id;
    }

    public String getSid() {
        return sid;
    }

    protected void setSid(final String sid) {
        this.sid = sid;
    }

    protected String idToString() {
        final StringBuffer result = new StringBuffer();

        if (id != null) {
            result.append("id: ").append(id);
        }

        if (sid != null) {
            if (result.length() != 0) {
                result.append(" ");
            }
            result.append("sid: ").append(sid);
        }

        if (result.length() == 0) {
            return "null";
        }

        return result.toString();
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + idOrName();
    }

    protected String idOrName() {
        return (name == null ? "unnamed" : name) + " - " + idToString();
    }
}
