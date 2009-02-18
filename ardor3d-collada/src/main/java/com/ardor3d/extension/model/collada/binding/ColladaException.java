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

import org.jibx.runtime.ITrackSource;

public class ColladaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ColladaException(final String message, final Object source) {
        super(createMessage(message, source));
    }

    public ColladaException(final String msg, final Object source, final Throwable cause) {
        super(createMessage(msg, source), cause);
    }

    private static String createMessage(final String message, final Object source) {
        if (source instanceof ITrackSource) {
            final ITrackSource trackSource = (ITrackSource) source;

            return "Collada problem on line: " + trackSource.jibx_getLineNumber() + ", column: "
                    + trackSource.jibx_getColumnNumber() + ": " + message;
        } else {
            return "Collada problem for source: " + source.toString() + ": " + message;
        }
    }
}
