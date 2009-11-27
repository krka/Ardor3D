/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * a description of a class for the RunExamples (and maybe other tools)
 */
@Target( { TYPE })
@Retention(RUNTIME)
public @interface Purpose {

    /**
     * @return a free form description of the example, it may contain HTML tags
     */
    String htmlDescription();

    /**
     * @return the resource path to a screenshot thumbnail, e.g. /com/ardor3d/example/thumbnails/boxexample.png
     */
    String thumbnailPath();

    /**
     * @return the value to use for max heap (in MB).
     */
    int maxHeapMemory();
}
