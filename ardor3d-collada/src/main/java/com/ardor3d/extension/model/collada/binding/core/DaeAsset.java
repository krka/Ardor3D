/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.collada.binding.core;

import java.util.Date;

import org.jibx.runtime.ITrackSource;

import com.ardor3d.extension.model.collada.binding.DaeTreeNode;

public class DaeAsset extends DaeTreeNode {
    private Date created;
    private Date modified;
    private String revision;

    public Date getCreated() {
        return created;
    }

    public void setCreated(final Date created) {
        this.created = created;
    }

    public Date getModified() {
        return modified;
    }

    public void setModified(final Date modified) {
        this.modified = modified;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(final String revision) {
        this.revision = revision;
    }

    @Override
    public String toString() {
        return "cmr: " + created + ", " + modified + ", " + revision;
    }

    public String getSourceInfo() {
        final ITrackSource source = (ITrackSource) this;

        return source.jibx_getDocumentName() + ": " + source.jibx_getLineNumber() + ", "
                + source.jibx_getColumnNumber();
    }
}
