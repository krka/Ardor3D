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

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Logger;

import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.Savable;
import com.ardor3d.util.export.binary.modules.BinaryCameraModule;

public class BinaryClassLoader {

    // list of modules maintained in the loader
    private static HashMap<String, BinaryLoaderModule> modules = new HashMap<String, BinaryLoaderModule>();

    {
        BinaryClassLoader.registerModule(new BinaryCameraModule());
    }

    /**
     * registrrModule adds a module to the loader for handling special case class names.
     * 
     * @param m
     *            the module to register with this loader.
     */
    public static void registerModule(final BinaryLoaderModule m) {
        modules.put(m.getKey(), m);
    }

    /**
     * unregisterModule removes a module from the loader, no longer using it to handle special case class names.
     * 
     * @param m
     *            the module to remove from the loader.
     */
    public static void unregisterModule(final BinaryLoaderModule m) {
        modules.remove(m.getKey());
    }

    /**
     * fromName creates a new Savable from the provided class name. First registered modules are checked to handle
     * special cases, if the modules do not handle the class name, the class is instantiated directly.
     * 
     * @param className
     *            the class name to create.
     * @param inputCapsule
     *            the InputCapsule that will be used for loading the Savable (to look up ctor parameters)
     * @return the Savable instance of the class.
     * @throws InstantiationException
     *             thrown if the class does not have an empty constructor.
     * @throws IllegalAccessException
     *             thrown if the class is not accessable.
     * @throws ClassNotFoundException
     *             thrown if the class name is not in the classpath.
     * @throws IOException
     *             when loading ctor parameters fails
     */
    public static Savable fromName(final String className, final InputCapsule inputCapsule)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException {

        final BinaryLoaderModule m = modules.get(className);
        if (m != null) {
            return m.load(inputCapsule);
        }

        try {
            return (Savable) Class.forName(className).newInstance();
        } catch (final InstantiationException e) {
            Logger
                    .getLogger(BinaryClassLoader.class.getName())
                    .severe(
                            "Could not access constructor of class '"
                                    + className
                                    + "'! \n"
                                    + "Some types need to have the BinaryImporter set up in a special way. Please doublecheck the setup.");
            throw e;
        } catch (final IllegalAccessException e) {
            Logger
                    .getLogger(BinaryClassLoader.class.getName())
                    .severe(
                            e.getMessage()
                                    + " \n"
                                    + "Some types need to have the BinaryImporter set up in a special way. Please doublecheck the setup.");
            throw e;
        }
    }

}
