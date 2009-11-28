/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input.logical;

import com.ardor3d.input.ControllerEvent;
import com.ardor3d.input.ControllerState;
import com.ardor3d.input.ControllerWrapper;
import com.google.common.collect.PeekingIterator;

public class DummyControllerWrapper implements ControllerWrapper {

    public ControllerState getBlankState() {
        return new ControllerState();
    }

    public PeekingIterator<ControllerEvent> getEvents() {
        return new PeekingIterator<ControllerEvent>() {

            public ControllerEvent next() {
                throw new UnsupportedOperationException();
            }

            public ControllerEvent peek() {
                throw new UnsupportedOperationException();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }

            public boolean hasNext() {
                return false;
            }
        };
    }

    public void init() {}

}
