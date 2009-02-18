/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.collada;

import java.io.InputStreamReader;
import java.net.URL;

import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IUnmarshallingContext;

import com.ardor3d.extension.model.collada.binding.core.Collada;
import com.ardor3d.extension.model.collada.util.ColladaNodeUtils;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.SimpleResourceLocator;

public class ColladaImporter {
    /**
     * Reads a Collada scene from the given resource and returns it as an Ardor3D Node.
     * 
     * @param resource
     *            the name of the resource to find. ResourceLocatorTool will be used with TYPE_MODEL to find the
     *            resource.
     * @return a Node containing the Collada scene.
     * @throws Exception
     */
    public static Node readColladaScene(final String resource) throws Exception {
        final URL url = ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_MODEL, resource);

        if (url == null) {
            throw new Error("Unable to locate '" + resource + "' on the classpath");
        }

        final Node scene = readColladaScene(url);

        return scene;
    }

    /**
     * Reads a Collada scene from the given resource and returns it as an Ardor3D Node.
     * 
     * @param resource
     *            the name of the resource to find.
     * @return a Node containing the Collada scene.
     * @throws Exception
     */
    public static Node readColladaScene(final URL resource) throws Exception {
        final IBindingFactory bindingFactory = BindingDirectory.getFactory(Collada.class);

        final IUnmarshallingContext context = bindingFactory.createUnmarshallingContext();

        final SimpleResourceLocator srl = new SimpleResourceLocator(resource);
        ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_TEXTURE, srl);

        final Collada collada = (Collada) context.unmarshalDocument(new InputStreamReader(resource.openStream()));

        // Collada may or may not have a scene.
        // FIXME: We should eventually return the libraries too since you could have those without a scene.

        Node scene = null;
        if (collada.getScene() != null) {
            // It's possible to have a scene that is all physics information...
            if (collada.getScene().getInstanceVisualScene() != null) {
                final String id = collada.getScene().getInstanceVisualScene().getUrl().substring(1);
                scene = ColladaNodeUtils.getVisualScene(id, collada);
            }
        }

        ResourceLocatorTool.removeResourceLocator(ResourceLocatorTool.TYPE_TEXTURE, srl);

        return scene;
    }

    // just a simple way to test things out without bringing up OpenGL, etc.
    // XXX: Maybe we should move this to a test or something.

    static int indent = 0;

    public static void main(final String[] args) throws Exception {
        final Spatial scene = readColladaScene("/duck/duck.dae");

        printScene(scene);
    }

    private static void printScene(final Spatial spat) {
        for (int i = 0; i < indent; i++) {
            System.out.print("    ");
        }

        if (spat instanceof Mesh) {
            System.out.println(spat + " v:" + ((Mesh) spat).getMeshData().getVertexCount() + " p:"
                    + ((Mesh) spat).getMeshData().getTotalPrimitiveCount());
        } else {
            System.out.println(spat);
        }

        if (spat instanceof Node) {
            final Node n = (Node) spat;
            if (n.getNumberOfChildren() > 0) {
                indent++;
                for (final Spatial child : n.getChildren()) {
                    printScene(child);
                }
                indent--;
            }
        }
    }

}
