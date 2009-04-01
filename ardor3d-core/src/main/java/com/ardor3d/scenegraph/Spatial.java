/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scenegraph;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.ardor3d.bounding.BoundingVolume;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Transform;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyMatrix3;
import com.ardor3d.math.type.ReadOnlyQuaternion;
import com.ardor3d.math.type.ReadOnlyTransform;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.NormalsMode;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.scenegraph.event.DirtyEventListener;
import com.ardor3d.scenegraph.event.DirtyType;
import com.ardor3d.scenegraph.visitor.Visitor;
import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;
import com.google.common.collect.Maps;

public abstract class Spatial implements Cloneable, Savable, Externalizable {

    /** This spatial's name. */
    protected String _name;

    /** Spatial's transform relative to its parent. */
    protected final Transform _localTransform;

    /** Spatial's absolute transform. */
    protected final Transform _worldTransform;

    /** Spatial's world bounding volume */
    protected BoundingVolume _worldBound;

    /** Spatial's parent, or null if it has none. */
    protected Node _parent;

    /** ArrayList of controllers for this spatial. */
    protected List<Controller> _controllers;

    /** The render states of this spatial. */
    protected final EnumMap<RenderState.StateType, RenderState> _renderStateList = new EnumMap<RenderState.StateType, RenderState>(
            RenderState.StateType.class);

    /** Listener for dirty events. */
    protected DirtyEventListener _listener;

    /** Field for accumulating dirty marks. */
    protected EnumSet<DirtyType> _dirtyMark = EnumSet.allOf(DirtyType.class);

    /** Field for user data. Note: If this object is not explicitly of type Savable, it will be ignored during save. */
    protected Object _userData = null;

    // TODO: maybe pack these below into some visualxxx class

    /**
     * A flag indicating how normals should be treated by the renderer.
     */
    protected NormalsMode _normalsMode = NormalsMode.Inherit;

    /**
     * A flag indicating if scene culling should be done on this object by inheritance, dynamically, never, or always.
     */
    protected CullHint _cullHint = CullHint.Inherit;

    /**
     * Flag signaling how lights are combined for this node. By default set to INHERIT.
     */
    protected LightCombineMode _lightCombineMode = LightCombineMode.Inherit;

    /**
     * Flag signaling how textures are combined for this node. By default set to INHERIT.
     */
    protected TextureCombineMode _textureCombineMode = TextureCombineMode.Inherit;

    /** Keeps track of the current frustum intersection state of this Spatial. */
    protected Camera.FrustumIntersect _frustumIntersects = Camera.FrustumIntersect.Intersects;

    /** RenderBucketType for this spatial */
    protected RenderBucketType _renderBucketType = RenderBucketType.Inherit;

    /** TODO: this should be done some other way */
    protected int _zOrder = 0;

    /** Field for setting whether the Spatial is enabled for Picking or Collision. Both are enabled by default. */
    protected EnumSet<PickingHint> _pickingHints = EnumSet.allOf(PickingHint.class);

    /**
     * Constructs a new Spatial. Initializes the transform fields.
     */
    public Spatial() {
        _localTransform = new Transform();
        _worldTransform = new Transform();
    }

    /**
     * Constructs a new <code>Spatial</code> with a given name.
     * 
     * @param name
     *            the name of the spatial. This is required for identification purposes.
     */
    public Spatial(final String name) {
        this();
        _name = name;
    }

    /**
     * Returns the name of this spatial.
     * 
     * @return This spatial's name.
     */
    public String getName() {
        return _name;
    }

    /**
     * Sets the name of this Spatial.
     * 
     * @param name
     *            new name
     */
    public void setName(final String name) {
        _name = name;
    }

    /**
     * <code>getParent</code> retrieve's this node's parent. If the parent is null this is the root node.
     * 
     * @return the parent of this node.
     */
    public Node getParent() {
        return _parent;
    }

    /**
     * Called by {@link Node#attachChild(Spatial)} and {@link Node#detachChild(Spatial)} - don't call directly.
     * <code>setParent</code> sets the parent of this node.
     * 
     * @param parent
     *            the parent of this node.
     */
    protected void setParent(final Node parent) {
        _parent = parent;
    }

    /**
     * <code>removeFromParent</code> removes this Spatial from it's parent.
     * 
     * @return true if it has a parent and performed the remove.
     */
    public boolean removeFromParent() {
        if (_parent != null) {
            _parent.detachChild(this);
            return true;
        }
        return false;
    }

    /**
     * determines if the provided Node is the parent, or parent's parent, etc. of this Spatial.
     * 
     * @param ancestor
     *            the ancestor object to look for.
     * @return true if the ancestor is found, false otherwise.
     */
    public boolean hasAncestor(final Node ancestor) {
        if (_parent == null) {
            return false;
        } else if (_parent.equals(ancestor)) {
            return true;
        } else {
            return _parent.hasAncestor(ancestor);
        }
    }

    /**
     * Returns the listener for dirty events on this node, if set.
     * 
     * @return the listener
     */
    public DirtyEventListener getListener() {
        return _listener;
    }

    /**
     * Sets the listener for dirty events on this node.
     * 
     * @param listener
     *            listener to use.
     */
    public void setListener(final DirtyEventListener listener) {
        _listener = listener;
    }

    /**
     * Mark this node as dirty. Can be marked as Transform, Bounding, Attached, Detached, Destroyed or RenderState
     * 
     * @param dirtyType
     */
    public void markDirty(final DirtyType dirtyType) {
        markDirty(this, dirtyType);
    }

    /**
     * Mark this node as dirty. Can be marked as Transform, Bounding, Attached, Detached, Destroyed or RenderState
     * 
     * @param caller
     * @param dirtyType
     */
    protected void markDirty(final Spatial caller, final DirtyType dirtyType) {
        switch (dirtyType) {
            case Transform:
                // XXX: Can we pass multiples at one time?
                propagateDirtyUp(DirtyType.Transform);
                propagateDirtyDown(DirtyType.Transform);
                propagateDirtyUp(DirtyType.Bounding);
                propagateDirtyDown(DirtyType.Bounding);
                break;
            case RenderState:
                propagateDirtyUp(DirtyType.RenderState);
                propagateDirtyDown(DirtyType.RenderState);
                break;
            case Bounding:
                propagateDirtyUp(DirtyType.Bounding);
                break;
            case Attached:
                propagateDirtyDown(DirtyType.Transform);
                propagateDirtyDown(DirtyType.RenderState);
                propagateDirtyUp(DirtyType.Bounding);
                propagateDirtyDown(DirtyType.Bounding);
                break;
            case Detached:
            case Destroyed:
                propagateDirtyUp(DirtyType.Bounding);
                break;
            default:
                break;
        }

        propageEventUp(caller, dirtyType);
    }

    /**
     * Test if this spatial is marked as dirty in respect to the supplied DirtyType
     * 
     * @param dirtyType
     *            dirty type to test against
     * @return true if spatial marked dirty against the supplied dirty type
     */
    public boolean isDirty(final DirtyType dirtyType) {
        return _dirtyMark.contains(dirtyType);
    }

    /**
     * Clears the dirty flag set at this spatial for the supplied dirty type.
     * 
     * @param dirtyType
     *            dirty type to clear flag for
     */
    public void clearDirty(final DirtyType dirtyType) {
        _dirtyMark.remove(dirtyType);
    }

    /**
     * Propagate the dirty mark up the tree hierarchy
     * 
     * @param dirtyType
     */
    protected void propagateDirtyUp(final DirtyType dirtyType) {
        _dirtyMark.add(dirtyType);

        if (_parent != null) {
            _parent.propagateDirtyUp(dirtyType);
        }
    }

    /**
     * Propagate the dirty mark down the tree hierarchy
     * 
     * @param dirtyType
     */
    protected void propagateDirtyDown(final DirtyType dirtyType) {
        _dirtyMark.add(dirtyType);
    }

    /**
     * Propagate the dirty event up the hierarchy. If a listener is found on the spatial the event is fired and the
     * propagation is stopped.
     * 
     * @param spatial
     * @param dirtyType
     */
    protected void propageEventUp(final Spatial spatial, final DirtyType dirtyType) {
        boolean consumed = false;
        if (_listener != null) {
            consumed = _listener.spatialDirty(spatial, dirtyType);
        }

        if (!consumed && _parent != null) {
            _parent.propageEventUp(spatial, dirtyType);
        }
    }

    public ReadOnlyMatrix3 getRotation() {
        return _localTransform.getMatrix();
    }

    public ReadOnlyVector3 getScale() {
        return _localTransform.getScale();
    }

    public ReadOnlyVector3 getTranslation() {
        return _localTransform.getTranslation();
    }

    public ReadOnlyTransform getTransform() {
        return _localTransform;
    }

    public void setTransform(final ReadOnlyTransform transform) {
        _localTransform.set(transform);
        markDirty(DirtyType.Transform);
    }

    public void setWorldRotation(final ReadOnlyMatrix3 rotation) {
        _worldTransform.setRotation(rotation);
    }

    public void setWorldScale(final ReadOnlyVector3 scale) {
        _worldTransform.setScale(scale);
    }

    public void setWorldScale(final double x, final double y, final double z) {
        _worldTransform.setScale(x, y, z);
    }

    public void setWorldScale(final double scale) {
        _worldTransform.setScale(scale);
    }

    public void setWorldTranslation(final ReadOnlyVector3 translation) {
        _worldTransform.setTranslation(translation);
    }

    public void setWorldTranslation(final double x, final double y, final double z) {
        _worldTransform.setTranslation(x, y, z);
    }

    /**
     * Sets the rotation of this spatial. This marks the spatial as DirtyType.Transform.
     * 
     * @param rotation
     *            the new rotation of this spatial
     * @see Transform#setRotation(Matrix3)
     */
    public void setRotation(final ReadOnlyMatrix3 rotation) {
        _localTransform.setRotation(rotation);
        markDirty(DirtyType.Transform);
    }

    /**
     * Sets the rotation of this spatial. This marks the spatial as DirtyType.Transform.
     * 
     * @param rotation
     *            the new rotation of this spatial
     * @see Transform#setRotation(Quaternion)
     */
    public void setRotation(final ReadOnlyQuaternion rotation) {
        _localTransform.setRotation(rotation);
        markDirty(DirtyType.Transform);
    }

    /**
     * Sets the rotation and potentially scale of this spatial. This marks the spatial as DirtyType.Transform.
     * 
     * @param rotation
     *            the new rotation of this spatial
     * @see Transform#setMatrix(Matrix3)
     */
    public void setMatrix(final ReadOnlyMatrix3 matrix) {
        _localTransform.setMatrix(matrix);
        markDirty(DirtyType.Transform);
    }

    /**
     * <code>setScale</code> sets the scale of this spatial. This marks the spatial as DirtyType.Transform.
     * 
     * @param scale
     *            the new scale of this spatial
     */
    public void setScale(final ReadOnlyVector3 scale) {
        _localTransform.setScale(scale);
        markDirty(DirtyType.Transform);
    }

    /**
     * <code>setScale</code> sets the scale of this spatial. This marks the spatial as DirtyType.Transform.
     * 
     * @param scale
     *            the new scale of this spatial
     */
    public void setScale(final double scale) {
        _localTransform.setScale(scale);
        markDirty(DirtyType.Transform);
    }

    /**
     * sets the scale of this spatial. This marks the spatial as DirtyType.Transform.
     * 
     * @param x
     * @param y
     * @param z
     */
    public void setScale(final double x, final double y, final double z) {
        _localTransform.setScale(x, y, z);
        markDirty(DirtyType.Transform);
    }

    /**
     * <code>setTranslation</code> sets the translation of this spatial. This marks the spatial as DirtyType.Transform.
     * 
     * @param rotation
     *            the new translation of this spatial
     */
    public void setTranslation(final ReadOnlyVector3 translation) {
        _localTransform.setTranslation(translation);
        markDirty(DirtyType.Transform);
    }

    /**
     * sets the translation of this spatial. This marks the spatial as DirtyType.Transform.
     * 
     * @param x
     * @param y
     * @param z
     */
    public void setTranslation(final double x, final double y, final double z) {
        _localTransform.setTranslation(x, y, z);
        markDirty(DirtyType.Transform);
    }

    public ReadOnlyMatrix3 getWorldRotation() {
        return _worldTransform.getMatrix();
    }

    public ReadOnlyVector3 getWorldScale() {
        return _worldTransform.getScale();
    }

    public ReadOnlyVector3 getWorldTranslation() {
        return _worldTransform.getTranslation();
    }

    public ReadOnlyTransform getWorldTransform() {
        return _worldTransform;
    }

    /**
     * <code>getWorldBound</code> retrieves the world bound at this level.
     * 
     * @return the world bound at this level.
     */
    public BoundingVolume getWorldBound() {
        return _worldBound;
    }

    /**
     * @param zOrder
     */
    public void setZOrder(final int zOrder) {
        _zOrder = zOrder;
    }

    /**
     * @return
     */
    public int getZOrder() {
        return _zOrder;
    }

    /**
     * <code>onDraw</code> checks the spatial with the camera to see if it should be culled, if not, the node's draw
     * method is called.
     * <p>
     * This method is called by the renderer. Usually it should not be called directly.
     * 
     * @param r
     *            the renderer used for display.
     */
    public void onDraw(final Renderer r) {
        final CullHint cm = getCullHint();
        if (cm == Spatial.CullHint.Always) {
            setLastFrustumIntersection(Camera.FrustumIntersect.Outside);
            return;
        } else if (cm == Spatial.CullHint.Never) {
            setLastFrustumIntersection(Camera.FrustumIntersect.Intersects);
            draw(r);
            return;
        }

        final Camera camera = Camera.getCurrentCamera();
        final int state = camera.getPlaneState();

        // check to see if we can cull this node
        _frustumIntersects = (_parent != null ? _parent._frustumIntersects : Camera.FrustumIntersect.Intersects);

        if (cm == Spatial.CullHint.Dynamic && _frustumIntersects == Camera.FrustumIntersect.Intersects) {
            _frustumIntersects = camera.contains(_worldBound);
        }

        if (_frustumIntersects != Camera.FrustumIntersect.Outside) {
            draw(r);
        }
        camera.setPlaneState(state);
    }

    /**
     * <code>draw</code> abstract method that handles drawing data to the renderer if it is geometry and passing the
     * call to it's children if it is a node.
     * 
     * @param r
     *            the renderer used for display.
     */
    public abstract void draw(Renderer r);

    public void updateGeometricState(final double time) {
        updateGeometricState(time, true);
    }

    /**
     * <code>updateGeometricState</code> updates all the geometry information for the node.
     * 
     * @param time
     *            the frame time.
     * @param initiator
     *            true if this node started the update process.
     */
    public void updateGeometricState(final double time, final boolean initiator) {
        updateControllers(time);

        if (isDirty(DirtyType.Transform)) {
            updateWorldTransform(false);
        }

        if (isDirty(DirtyType.RenderState)) {
            updateWorldRenderStates(false);
            clearDirty(DirtyType.RenderState);
        }

        updateChildren(time);

        if (isDirty(DirtyType.Bounding)) {
            updateWorldBound(false);
            if (initiator) {
                propagateBoundToRoot();
            }
        }
    }

    /**
     * TODO: This is a hack to allow objects like Node to traverse it's children.
     */
    protected void updateChildren(final double time) {

    }

    public void updateControllers(final double time) {
        if (_controllers != null) {
            for (int i = 0, gSize = _controllers.size(); i < gSize; i++) {
                try {
                    final Controller controller = _controllers.get(i);
                    if (controller != null) {
                        if (controller.isActive()) {
                            controller.update(time, this);
                        }
                    }
                } catch (final IndexOutOfBoundsException e) {
                    // a controller was removed in Controller.update (note: this
                    // may skip one controller)
                    break;
                }
            }
        }
    }

    /**
     * Updates the worldTransform
     * 
     * @param recurse
     *            usually false when updating the tree. Set to true when you just want to update the world transforms
     *            for a branch without updating geometric state.
     */
    public void updateWorldTransform(final boolean recurse) {
        if (_parent != null) {
            _parent._worldTransform.multiply(_localTransform, _worldTransform);
        } else {
            _worldTransform.set(_localTransform);
        }
        clearDirty(DirtyType.Transform);
    }

    /**
     * Convert a vector (in) from this spatial's local coordinate space to world coordinate space.
     * 
     * @param in
     *            vector to read from
     * @param store
     *            where to write the result (null to create a new vector, may be same as in)
     * @return the result (store)
     */
    public Vector3 localToWorld(final ReadOnlyVector3 in, Vector3 store) {
        if (store == null) {
            store = new Vector3();
        }

        return _worldTransform.applyForward(in, store);
    }

    /**
     * Convert a vector (in) from world coordinate space to this spatial's local coordinate space.
     * 
     * @param in
     *            vector to read from
     * @param store
     *            where to write the result (null to create a new vector, may be same as in)
     * @return the result (store)
     */
    public Vector3 worldToLocal(final ReadOnlyVector3 in, Vector3 store) {
        if (store == null) {
            store = new Vector3();
        }

        return _worldTransform.applyInverse(in, store);
    }

    /**
     * Updates the render state values of this Spatial and and children it has. Should be called whenever render states
     * change.
     */
    public void updateWorldRenderStates(final boolean recurse) {
        updateWorldRenderStates(recurse, null);
    }

    /**
     * Called internally. Updates the render states of this Spatial. The stack contains parent render states.
     * 
     * @param stateStacks
     *            The parent render states, or null if we are starting at this point in the scenegraph.
     */
    protected void updateWorldRenderStates(final boolean recurse,
            final Map<RenderState.StateType, Stack<RenderState>> stateStacks) {
        Map<RenderState.StateType, Stack<RenderState>> stacks = stateStacks;

        final boolean initiator = (stacks == null);

        // first we need to get all the states from parent to us.
        if (initiator) {
            // grab all states from root to here.
            stacks = Maps.newHashMap();
            propagateStatesFromRoot(stacks);
        } else {
            Stack<RenderState> stack;
            for (final RenderState state : _renderStateList.values()) {
                stack = stacks.get(state.getType());
                if (stack == null) {
                    stack = new Stack<RenderState>();
                    stacks.put(state.getType(), stack);
                }
                stack.push(state);
            }
        }

        applyWorldRenderStates(recurse, stacks);

        // restore previous if we are not the initiator
        if (!initiator) {
            for (final RenderState state : _renderStateList.values()) {
                stacks.get(state.getType()).pop();
            }
        }
    }

    /**
     * The method actually implements how the render states are applied to this spatial and (if recurse is true) any
     * children it may have. By default, this function does nothing.
     * 
     * @param recurse
     * @param states
     *            An array of stacks for each state.
     */
    protected void applyWorldRenderStates(final boolean recurse,
            final Map<RenderState.StateType, Stack<RenderState>> states) {}

    /**
     * Sort the ligts on this spatial.
     */
    public void sortLights() {}

    /**
     * Retrieves the complete renderstate list.
     * 
     * @return the list of renderstates
     */
    public EnumMap<StateType, RenderState> getLocalRenderStates() {
        return _renderStateList;
    }

    /**
     * <code>setRenderState</code> sets a render state for this node. Note, there can only be one render state per type
     * per node. That is, there can only be a single BlendState a single TextureState, etc. If there is already a render
     * state for a type set the old render state will be returned. Otherwise, null is returned.
     * 
     * @param rs
     *            the render state to add.
     * @return the old render state.
     */
    public RenderState setRenderState(final RenderState rs) {
        if (rs == null) {
            return null;
        }

        final RenderState.StateType type = rs.getType();
        final RenderState oldState = _renderStateList.get(type);
        _renderStateList.put(type, rs);

        markDirty(DirtyType.RenderState);

        return oldState;
    }

    /**
     * Returns the requested RenderState that this Spatial currently has set or null if none is set.
     * 
     * @param type
     *            the state type to retrieve
     * @return a render state at the given position or null
     */
    public RenderState getLocalRenderState(final RenderState.StateType type) {
        return _renderStateList.get(type);
    }

    /**
     * Clears a given render state index by setting it to null.
     * 
     * @param type
     *            The type of RenderState to clear
     */
    public void clearRenderState(final RenderState.StateType type) {
        _renderStateList.remove(type);
    }

    /**
     * Called during updateRenderState(Stack[]), this function goes up the scene graph tree until the parent is null and
     * pushes RenderStates onto the states Stack array.
     * 
     * @param stateStack
     *            The stack to push any parent states onto.
     */
    public void propagateStatesFromRoot(final Map<RenderState.StateType, Stack<RenderState>> stateStack) {
        // traverse to root to allow downward state propagation
        if (_parent != null) {
            _parent.propagateStatesFromRoot(stateStack);
        }

        // push states onto current render state stack
        Stack<RenderState> stack;
        for (final RenderState state : _renderStateList.values()) {
            stack = stateStack.get(state.getType());
            if (stack == null) {
                stack = new Stack<RenderState>();
                stateStack.put(state.getType(), stack);
            }
            stack.push(state);
        }
    }

    /**
     * updates the bounding volume of the world. Abstract, geometry transforms the bound while node merges the
     * children's bound. In most cases, users will want to call updateModelBound() and let this function be called
     * automatically during updateGeometricState().
     */
    public abstract void updateWorldBound(boolean recurse);

    /**
     * passes the new world bound up the tree to the root.
     */
    public void propagateBoundToRoot() {
        if (_parent != null) {
            _parent.updateWorldBound(false);
            _parent.propagateBoundToRoot();
        }
    }

    /**
     * @see #setCullHint(CullHint)
     * @return the cull mode of this spatial, or if set to INHERIT, the cullmode of it's parent.
     */
    public CullHint getCullHint() {
        if (_cullHint != CullHint.Inherit) {
            return _cullHint;
        } else if (_parent != null) {
            return _parent.getCullHint();
        } else {
            return CullHint.Dynamic;
        }
    }

    /**
     * Returns this spatial's texture combine mode. If the mode is set to inherit, then the spatial gets its combine
     * mode from its parent.
     * 
     * @return The spatial's texture current combine mode.
     */
    public TextureCombineMode getTextureCombineMode() {
        if (_textureCombineMode != TextureCombineMode.Inherit) {
            return _textureCombineMode;
        } else if (_parent != null) {
            return _parent.getTextureCombineMode();
        } else {
            return TextureCombineMode.CombineClosest;
        }
    }

    /**
     * Returns this spatial's light combine mode. If the mode is set to inherit, then the spatial gets its combine mode
     * from its parent.
     * 
     * @return The spatial's light current combine mode.
     */
    public LightCombineMode getLightCombineMode() {
        if (_lightCombineMode != LightCombineMode.Inherit) {
            return _lightCombineMode;
        } else if (_parent != null) {
            return _parent.getLightCombineMode();
        } else {
            return LightCombineMode.CombineFirst;
        }
    }

    /**
     * Returns this spatial's normals mode. If the mode is set to inherit, then the spatial gets its normals mode from
     * its parent. If no parent, we'll default to NormalizeIfScaled.
     * 
     * @return The spatial's current normals mode.
     */
    public NormalsMode getNormalsMode() {
        if (_normalsMode != NormalsMode.Inherit) {
            return _normalsMode;
        } else if (_parent != null) {
            return _parent.getNormalsMode();
        } else {
            return NormalsMode.NormalizeIfScaled;
        }
    }

    /**
     * <code>setCullHint</code> sets how scene culling should work on this spatial during drawing. CullHint.Dynamic:
     * Determine via the defined Camera planes whether or not this Spatial should be culled. CullHint.Always: Always
     * throw away this object and any children during draw commands. CullHint.Never: Never throw away this object
     * (always draw it) CullHint.Inherit: Look for a non-inherit parent and use its cull mode. NOTE: You must set this
     * AFTER attaching to a parent or it will be reset with the parent's cullMode value.
     * 
     * @param hint
     *            one of CullHint.Dynamic, CullHint.Always, CullHint.Inherit or CullHint.Never
     */
    public void setCullHint(final CullHint hint) {
        _cullHint = hint;
    }

    /**
     * @return the cullmode set on this Spatial
     */
    public CullHint getLocalCullHint() {
        return _cullHint;
    }

    /**
     * @return
     */
    public NormalsMode getLocalNormalsMode() {
        return _normalsMode;
    }

    /**
     * @param mode
     */
    public void setNormalsMode(final NormalsMode mode) {
        _normalsMode = mode;
    }

    /**
     * Sets how lights from parents should be combined for this spatial.
     * 
     * @param mode
     *            The light combine mode for this spatial
     * @throws IllegalArgumentException
     *             if mode is null
     */
    public void setLightCombineMode(final LightCombineMode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("mode can not be null.");
        }
        _lightCombineMode = mode;
    }

    /**
     * @return the lightCombineMode set on this Spatial
     */
    public LightCombineMode getLocalLightCombineMode() {
        return _lightCombineMode;
    }

    /**
     * Sets how textures from parents should be combined for this Spatial.
     * 
     * @param mode
     *            The new texture combine mode for this spatial.
     * @throws IllegalArgumentException
     *             if mode is null
     */
    public void setTextureCombineMode(final TextureCombineMode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("mode can not be null.");
        }
        _textureCombineMode = mode;
    }

    /**
     * @return the textureCombineMode set on this Spatial
     */
    public TextureCombineMode getLocalTextureCombineMode() {
        return _textureCombineMode;
    }

    /**
     * <code>setRenderQueueMode</code> determines at what phase of the rendering proces this Spatial will rendered.
     * There are 4 different phases: QUEUE_SKIP - The spatial will be drawn as soon as possible, before the other phases
     * of rendering. QUEUE_OPAQUE - The renderer will try to find the optimal order for rendering all objects using this
     * mode. You should use this mode for most normal objects, except transparant ones, as it could give a nice
     * performance boost to your application. QUEUE_TRANSPARENT - This is the mode you should use for object with
     * transparancy in them. It will ensure the objects furthest away are rendered first. That ensures when another
     * transparent object is drawn on top of previously drawn objects, you can see those (and the object drawn using
     * SKIP and OPAQUE) through the tranparant parts of the newly drawn object. QUEUE_ORTHO - This is a special mode,
     * for drawing 2D object without prespective (such as GUI or HUD parts) Lastly, there is a special mode,
     * QUEUE_INHERIT, that will ensure that this spatial uses the same mode as the parent Node does.
     * 
     * @param renderBucketType
     *            The mode to use for this Spatial.
     */
    public void setRenderBucketType(final RenderBucketType renderBucketType) {
        _renderBucketType = renderBucketType;
    }

    /**
     * @return
     */
    public RenderBucketType getLocalRenderBucketType() {
        return _renderBucketType;
    }

    /**
     * Returns this spatial's renderqueue mode. If the mode is set to inherit, then the spatial gets its renderqueue
     * mode from its parent.
     * 
     * @return The spatial's current renderqueue mode.
     */
    public RenderBucketType getRenderBucketType() {
        if (_renderBucketType != RenderBucketType.Inherit) {
            return _renderBucketType;
        } else if (_parent != null) {
            return _parent.getRenderBucketType();
        } else {
            return RenderBucketType.Skip;
        }
    }

    public Object getUserData() {
        return _userData;
    }

    /**
     * @param userData
     *            Some Spatial specific user data. Note: If this object is not explicitly of type Savable, it will be
     *            ignored during read/write.
     */
    public void setUserData(final Object userData) {
        _userData = userData;
    }

    /**
     * Adds a Controller to this Spatial's list of controllers.
     * 
     * @param controller
     *            The Controller to add
     * @see com.ardor3d.scenegraph.Controller
     */
    public void addController(final Controller controller) {
        if (_controllers == null) {
            _controllers = new ArrayList<Controller>(1);
        }
        _controllers.add(controller);
    }

    /**
     * Removes a Controller from this Spatial's list of controllers, if it exist.
     * 
     * @param controller
     *            The Controller to remove
     * @return True if the Controller was in the list to remove.
     * @see com.ardor3d.scenegraph.Controller
     */
    public boolean removeController(final Controller controller) {
        if (_controllers == null) {
            return false;
        }
        return _controllers.remove(controller);
    }

    /**
     * Removes a Controller from this Spatial's list of controllers by index.
     * 
     * @param index
     *            The index of the controller to remove
     * @return The Controller removed or null if nothing was removed.
     * @see com.ardor3d.scenegraph.Controller
     */
    public Controller removeController(final int index) {
        if (_controllers == null) {
            return null;
        }
        return _controllers.remove(index);
    }

    /**
     * Removes all Controllers from this Spatial's list of controllers.
     * 
     * @see com.ardor3d.scenegraph.Controller
     */
    public void clearControllers() {
        if (_controllers != null) {
            _controllers.clear();
        }
    }

    /**
     * Returns the controller in this list of controllers at index i.
     * 
     * @param i
     *            The index to get a controller from.
     * @return The controller at index i.
     * @see com.ardor3d.scenegraph.Controller
     */
    public Controller getController(final int i) {
        if (_controllers == null) {
            _controllers = new ArrayList<Controller>(1);
        }
        return _controllers.get(i);
    }

    /**
     * Returns the ArrayList that contains this spatial's Controllers.
     * 
     * @return This spatial's _controllers.
     */
    public List<Controller> getControllers() {
        if (_controllers == null) {
            _controllers = new ArrayList<Controller>(1);
        }
        return _controllers;
    }

    /**
     * @return the number of controllers set on this Spatial.
     */
    public int getControllerCount() {
        if (_controllers == null) {
            return 0;
        }
        return _controllers.size();
    }

    /**
     * Returns this spatial's last frustum intersection result. This int is set when a check is made to determine if the
     * bounds of the object fall inside a camera's frustum. If a parent is found to fall outside the frustum, the value
     * for this spatial will not be updated.
     * 
     * @return The spatial's last frustum intersection result.
     */
    public Camera.FrustumIntersect getLastFrustumIntersection() {
        return _frustumIntersects;
    }

    /**
     * Overrides the last intersection result. This is useful for operations that want to start rendering at the middle
     * of a scene tree and don't want the parent of that node to influence culling. (See texture renderer code for
     * example.)
     * 
     * @param frustumIntersects
     *            the new value
     */
    public void setLastFrustumIntersection(final Camera.FrustumIntersect frustumIntersects) {
        _frustumIntersects = frustumIntersects;
    }

    /**
     * Execute the given Visitor on this Spatial, and any Spatials managed by this Spatial as appropriate.
     * 
     * @param visitor
     *            the Visitor object to use.
     * @param preexecute
     *            if true, we will visit <i>this</i> Spatial before any Spatials we manage (such as children of a Node.)
     *            If false, we will visit them first, then ourselves.
     */
    public void acceptVisitor(final Visitor visitor, final boolean preexecute) {
        visitor.visit(this);
    }

    /**
     * Enable or disable a picking hint for this Spatial
     * 
     * @param pickingHint
     *            PickingHint to set. Pickable or Collidable
     * @param enabled
     *            Enable or disable
     */
    public void setPickingHint(final PickingHint pickingHint, final boolean enabled) {
        if (enabled) {
            _pickingHints.add(pickingHint);
        } else {
            _pickingHints.remove(pickingHint);
        }
    }

    /**
     * Enable or disable all picking hints for this Spatial
     * 
     * @param enabled
     *            Enable or disable
     */
    public void setAllPickingHints(final boolean enabled) {
        if (enabled) {
            _pickingHints.addAll(EnumSet.allOf(PickingHint.class));
        } else {
            _pickingHints.clear();
        }
    }

    /**
     * Returns whether a certain pick hint is set on this spatial.
     * 
     * @param pickingHint
     *            Pick hint to test for
     * @return Enabled or disabled
     */
    public boolean isPickingHintEnabled(final PickingHint pickingHint) {
        return _pickingHints.contains(pickingHint);
    }

    /**
     * Returns the Spatial's name followed by the class of the spatial <br>
     * Example: "MyNode (com.ardor3d.scene.Spatial)
     * 
     * @return Spatial's name followed by the class of the Spatial
     */
    @Override
    public String toString() {
        return _name + " (" + this.getClass().getName() + ')';
    }

    // /////////////////
    // Method for Cloneable
    // /////////////////

    @Override
    public Spatial clone() {
        try {
            return (Spatial) super.clone();
        } catch (final CloneNotSupportedException e) {
            throw new AssertionError(); // can not happen
        }
    }

    // /////////////////
    // Methods for Savable
    // /////////////////

    public Class<? extends Spatial> getClassTag() {
        return this.getClass();
    }

    public void read(final Ardor3DImporter im) throws IOException {
        final InputCapsule capsule = im.getCapsule(this);
        _name = capsule.readString("name", null);
        // isCollidable = capsule.readBoolean("isCollidable", true);
        _cullHint = capsule.readEnum("cullMode", CullHint.class, CullHint.Inherit);
        _zOrder = capsule.readInt("zOrder", 0);
        _renderBucketType = capsule.readEnum("renderBucketType", RenderBucketType.class, RenderBucketType.Inherit);
        _lightCombineMode = capsule.readEnum("lightCombineMode", LightCombineMode.class, LightCombineMode.Inherit);
        _textureCombineMode = capsule.readEnum("textureCombineMode", TextureCombineMode.class,
                TextureCombineMode.Inherit);

        _normalsMode = capsule.readEnum("normalsMode", NormalsMode.class, NormalsMode.Inherit);

        final Savable[] savs = capsule.readSavableArray("renderStateList", null);
        _renderStateList.clear();
        if (savs != null) {
            for (int x = 0; x < savs.length; x++) {
                final RenderState state = (RenderState) savs[x];
                _renderStateList.put(state.getType(), state);
            }
        }

        _localTransform.set((Transform) capsule.readSavable("localTransform", new Transform(Transform.IDENTITY)));
        _worldTransform.set((Transform) capsule.readSavable("worldTransform", new Transform(Transform.IDENTITY)));

        final Savable userData = capsule.readSavable("userData", null);
        // only override set userdata if we have something in the capsule.
        if (userData != null) {
            _userData = userData;
        }

        _controllers = capsule.readSavableList("controllers", null);
    }

    public void write(final Ardor3DExporter ex) throws IOException {
        final OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(_name, "name", null);
        // capsule.write(isCollidable, "isCollidable", true);
        capsule.write(_cullHint, "cullMode", CullHint.Inherit);
        capsule.write(_renderBucketType, "renderBucketType", RenderBucketType.Inherit);
        capsule.write(_zOrder, "zOrder", 0);
        capsule.write(_lightCombineMode, "lightCombineMode", LightCombineMode.Inherit);
        capsule.write(_textureCombineMode, "textureCombineMode", TextureCombineMode.Inherit);

        capsule.write(_normalsMode, "normalsMode", NormalsMode.Inherit);

        capsule.write(_renderStateList.values().toArray(new RenderState[0]), "renderStateList", null);

        capsule.write(_localTransform, "localTransform", new Transform(Transform.IDENTITY));
        capsule.write(_worldTransform, "worldTransform", new Transform(Transform.IDENTITY));

        if (_userData instanceof Savable) {
            capsule.write((Savable) _userData, "userData", null);
        }

        capsule.writeSavableList(_controllers, "controllers", null);
    }

    // /////////////////
    // Methods for Externalizable
    // /////////////////

    /**
     * Used with serialization. Not to be called manually.
     * 
     * @param in
     *            ObjectInput
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
    // TODO
    }

    /**
     * Used with serialization. Not to be called manually.
     * 
     * @param out
     *            ObjectOutput
     * @throws IOException
     */
    public void writeExternal(final ObjectOutput out) throws IOException {
    // TODO
    }

    /**
     * Describes how to combine textures from ancestor texturestates when an updateRenderState is called on a Spatial.
     */
    public enum TextureCombineMode {
        /** When updating render states, turn off texturing for this spatial. */
        Off,

        /**
         * Combine textures starting from the root node and working towards the given Spatial. Ignore disabled states.
         */
        CombineFirst,

        /**
         * Combine textures starting from the given Spatial and working towards the root. Ignore disabled states.
         * (Default)
         */
        CombineClosest,

        /**
         * Similar to CombineClosest, but if a disabled state is encountered, it will stop combining at that point.
         */
        CombineClosestEnabled,

        /** Inherit mode from parent. */
        Inherit,

        /** Do not combine textures, just use the most recent texture state. */
        Replace;
    }

    /**
     * Describes how to combine lights from ancestor lightstates when an updateRenderState is called on a Spatial.
     */
    public enum LightCombineMode {
        /** When updating render states, turn off lighting for this spatial. */
        Off,

        /**
         * Combine lights starting from the root node and working towards the given Spatial. Ignore disabled states.
         * Stop combining when lights == MAX_LIGHTS_ALLOWED
         */
        CombineFirst,

        /**
         * Combine lights starting from the given Spatial and working up towards the root. Ignore disabled states. Stop
         * combining when lights == MAX_LIGHTS_ALLOWED
         */
        CombineClosest,

        /**
         * Similar to CombineClosest, but if a disabled state is encountered, it will stop combining at that point. Stop
         * combining when lights == MAX_LIGHTS_ALLOWED
         */
        CombineClosestEnabled,

        /** Inherit mode from parent. */
        Inherit,

        /** Do not combine lights, just use the most recent light state. */
        Replace;
    }

    public enum CullHint {
        /** Do whatever our parent does. If no parent, we'll default to dynamic. */
        Inherit,
        /**
         * Do not draw if we are not at least partially within the view frustum of the renderer's camera.
         */
        Dynamic,
        /** Always cull this from view. */
        Always,
        /**
         * Never cull this from view. Note that we will still get culled if our parent is culled.
         */
        Never;
    }

    public enum PickingHint {
        Pickable, Collidable
    }
}
