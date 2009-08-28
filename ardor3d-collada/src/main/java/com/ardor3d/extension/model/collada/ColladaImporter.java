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

import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IUnmarshallingContext;

import com.ardor3d.animations.reference.Animatable;
import com.ardor3d.animations.reference.Skeleton;
import com.ardor3d.animations.runtime.AnimationRegistry;
import com.ardor3d.extension.model.collada.binding.core.Collada;
import com.ardor3d.extension.model.collada.util.ColladaNodeUtils;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.resource.RelativeResourceLocator;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.ResourceSource;

public class ColladaImporter {

    /**
     * Reads a Collada scene from the given resource and returns it as an Ardor3D Node.
     * 
     * @param resource
     *            the name of the resource to find. ResourceLocatorTool will be used with TYPE_MODEL to find the
     *            resource.
     * @return a Node containing the Collada scene.
     */
    public static Node readColladaScene(final String resource) {
        return readColladaScene(resource, null);
    }

    public static Node readColladaScene(final String resource, final AnimationRegistry animationRegistry) {
        final ResourceSource source = ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_MODEL, resource);

        if (source == null) {
            throw new Error("Unable to locate '" + resource + "'");
        }

        return readColladaScene(source, animationRegistry);
    }

    /**
     * Reads a Collada scene from the given resource and returns it as an Ardor3D Node.
     * 
     * @param resource
     *            the name of the resource to find.
     * @return a Node containing the Collada scene.
     */
    public static Node readColladaScene(final ResourceSource resource) {
        return readColladaScene(resource, null);

    }

    public static Node readColladaScene(final ResourceSource resource, final AnimationRegistry animationRegistry) {

        // Collada may or may not have a scene.
        // FIXME: We should eventually return the libraries too since you could have those without a scene.
        final Collada collada = readCollada(resource);

        Node scene = null;
        try {
            final RelativeResourceLocator loc = new RelativeResourceLocator(resource);
            ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_TEXTURE, loc);

            if (collada.getScene() != null) {
                // It's possible to have a scene that is all physics information...
                if (collada.getScene().getInstanceVisualScene() != null) {
                    final String id = collada.getScene().getInstanceVisualScene().getUrl().substring(1);
                    scene = ColladaNodeUtils.getVisualScene(id, collada, animationRegistry);
                }
            }

            ResourceLocatorTool.removeResourceLocator(ResourceLocatorTool.TYPE_TEXTURE, loc);
        } catch (final Exception e) {
            throw new RuntimeException("Unable to load collada resource from URL: " + resource, e);
        }

        return scene;
    }

    /**
     * Reads the whole Collada object from the given resource and returns it. Exceptions may be thrown by underlying
     * tools; these will be wrapped in a RuntimeException and rethrown.
     * 
     * @param resource
     *            the ResourceSource to read the resource from
     * @return the Collada tree
     */
    public static Collada readCollada(final ResourceSource resource) {
        try {
            final IBindingFactory bindingFactory = BindingDirectory.getFactory(Collada.class);

            final IUnmarshallingContext context = bindingFactory.createUnmarshallingContext();

            return (Collada) context.unmarshalDocument(new InputStreamReader(resource.openStream()));
        } catch (final Exception e) {
            throw new RuntimeException("Unable to load collada resource from source: " + resource, e);
        }
    }

    // just a simple way to test things out without bringing up OpenGL, etc.
    // XXX: Maybe we should move this to a test or something.

    static int indent = 0;

    public static void main(final String[] args) throws Exception {
        final AnimationRegistry animationRegistry = new AnimationRegistry();

        final Spatial scene = readColladaScene("/HC_Medium_Char_Skin.dae", animationRegistry);

        printScene(scene);

        for (final Animatable animatable : animationRegistry.getAnimatables()) {
            System.out.println(animatable);
        }

        for (final Skeleton skeleton : animationRegistry.getSkeletons().values()) {
            System.out.println(skeleton);
        }
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
