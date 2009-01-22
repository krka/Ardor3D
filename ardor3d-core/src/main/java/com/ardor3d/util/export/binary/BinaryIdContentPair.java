/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.export.binary;

public class BinaryIdContentPair {
    private int id;
    private BinaryOutputCapsule content;

    public BinaryIdContentPair(final int id, final BinaryOutputCapsule content) {
        this.id = id;
        this.content = content;
    }

    public BinaryOutputCapsule getContent() {
        return content;
    }

    public void setContent(final BinaryOutputCapsule content) {
        this.content = content;
    }

    public int getId() {
        return id;
    }

    public void setId(final int id) {
        this.id = id;
    }
}
