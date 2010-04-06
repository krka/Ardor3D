importPackage(Packages.com.ardor3d.extension.animation.skeletal);
importPackage(Packages.com.ardor3d.extension.animation.skeletal.blendtree);
importPackage(Packages.com.ardor3d.extension.animation.skeletal.clip);
importPackage(Packages.com.ardor3d.extension.animation.skeletal.layer);
importPackage(Packages.com.ardor3d.extension.animation.skeletal.state);

/**
 * Parse a SteadyState Java object from the given json data structure.
 * 
 * @param json
 *            the json data structure
 * @return the new SteadyState
 */
function _steadyState(json) {
	var state = new SteadyState(json.name);

	// check if we are simple and just have a clip
	if (json.clip) {
		// convert clip to source
		state.sourceTree = new ClipSource(INPUTSTORE.clips.get(json.clip),
				MANAGER);
	}
	// else we should have a tree
	else if (json.tree) {
		state.sourceTree = _treeSource(json.tree);
	}

	if (json.endTransition) {
		// parse end transition
		state.endTransition = _transitionState(json.endTransition);
	}

	// look for a set of transitions
	if (json.transitions) {
		for ( var key in json.transitions) {
			// parse and add transition
			state.addTransition(key, _transitionState(json.transitions[key]));
		}
	}

	return state;
}

/**
 * Parse an AbstractTransitionState Java object from the given json data array.
 * 
 * @param args
 *            the json data array
 * @return the new AbstractTransitionState
 */
function _transitionState(args) {
	var type = args[2];
	var transition;

	// based on our "type", create our transition state...
	switch (type) {
	case 'fade':
		transition = new FadeTransitionState(args[3], args[4],
				AbstractTwoStateLerpTransition.BlendType.valueOf(args[5]));
		break;
	case 'syncfade':
		transition = new SyncFadeTransitionState(args[3], args[4],
				AbstractTwoStateLerpTransition.BlendType.valueOf(args[5]));
		break;
	case 'frozen':
		transition = new FrozenTransitionState(args[3], args[4],
				AbstractTwoStateLerpTransition.BlendType.valueOf(args[5]));
		break;
	case 'immediate':
		transition = new ImmediateTransitionState(args[3]);
		break;
	default:
		return null;
	}

	// pull a start window, if set
	if (args[0] != '-') {
		transition.startWindow = args[0];
	}

	// pull an end window, if set
	if (args[1] != '-') {
		transition.endWindow = args[1];
	}

	return transition;
}

/**
 * Parse a BlendTreeSource Java object from the given json data structure.
 * 
 * @param json
 *            the json data structure
 * @return the new BlendTreeSource
 */
function _treeSource(json) {

	// look for the source type
	var source;
	var root;
	// ClipSource
	if (json.clip) {
		root = json.clip;
		var clip = INPUTSTORE.clips.get(root.name);
		// create source
		source = new ClipSource(clip, MANAGER);
		_populateClipSource(source, clip, root);
		return source;
	}
	// InclusiveClipSource
	else if (json.inclusiveClip) {
		root = json.inclusiveClip;
		var clip = INPUTSTORE.clips.get(root.name);
		// create source
		source = new InclusiveClipSource(clip, MANAGER);
		_populateClipSource(source, clip, root);
		// add channels/joints
		if (root.channels)
			source.addEnabledChannels(root.channels);
		if (root.joints)
			source.addEnabledJoints(root.joints);
		return source;
	}
	// ExclusiveClipSource
	else if (json.exclusiveClip) {
		root = json.exclusiveClip;
		var clip = INPUTSTORE.clips.get(root.name);
		// create source
		source = new ExclusiveClipSource(clip, MANAGER);
		_populateClipSource(source, clip, root);
		// add channels/joints
		if (root.channels)
			source.addDisabledChannels(root.channels);
		if (root.joints)
			source.addDisabledJoints(root.joints);
		return source;
	}
	// BinaryLERPSource
	else if (json.lerp) {
		root = json.lerp;
		// get child source A
		var sourceA = _treeSource(root.childA);
		// get child source B
		var sourceB = _treeSource(root.childB);
		// create source
		source = new BinaryLERPSource(sourceA, sourceB);
		// pull weight info
		if (root.blendKey) {
			source.setBlendKey(root.blendKey);
			var weight = (root.blendWeight) ? root.blendWeight : 0;
			MANAGER.valuesStore.put(source.blendKey, weight);
		}
		return source;
	}
	// ManagedTransformSource
	else if (json.managed) {
		root = json.managed;
		// create source
		source = new ManagedTransformSource();
		// if we are asked to, init joint positions from initial position of a
		// clip
		if (root.initFromClip) {
			// get clip
			var clip = INPUTSTORE.clips.get(root.initFromClip.clip);
			if (root.initFromClip.jointNames) {
				source.initJointsByName(MANAGER.getSkeletonPose(0), clip,
						root.initFromClip.jointNames);
			}
			if (root.initFromClip.jointIds) {
				source.initJointsById(clip, root.initFromClip.jointIds);
			}
		}
		return source;
	}
	// FrozenTreeSource
	else if (json.frozen) {
		root = json.frozen;
		// get child source
		var childSource = _treeSource(root.child);
		// read time
		var time = (root.time) ? root.time : 0;
		// create source
		source = new FrozenTreeSource(childSource, time);
		return source;
	}

	return null;
}

/**
 * Populate derivatives of ClipSource with instance values such as time scale.
 * 
 * @param source
 *            our ClipSource-based class.
 * @param clip
 *            our AnimationClip
 * @param root
 *            our json root data object.
 * @return void
 */
function _populateClipSource(source, clip, root) {
	// clip instance params...
	// add time scaling, if present
	if (root.timeScale) {
		MANAGER.getClipInstance(clip).setTimeScale(root.timeScale);
	}
	// add loop count
	if (root.loopCount) {
		MANAGER.getClipInstance(clip).setLoopCount(root.loopCount);
	}
	// add active flag
	if (root.active) {
		MANAGER.getClipInstance(clip).setActive(root.timeScale);
	}

	return;
}

/**
 * Parse an AnimationLayer Java object from the given json data structure.
 * 
 * @param json
 *            the json data structure
 * @return the new AnimationLayer
 */
function _animationLayer(json) {
	var layer = new AnimationLayer(json.name);

	if (json.blendType) {
		var blender;
		// grab based on blend type...
		switch (json.blendType) {
		case 'lerp':
			blender = new LayerLERPBlender();
			layer.setLayerBlender(blender);

			// pull weight
			if (json.blendKey) {
				blender.setBlendKey(json.blendKey);
				var weight = (json.blendWeight) ? json.blendWeight : 0;
				MANAGER.valuesStore.put(blender.blendKey, weight);
			}
			break;
		}
	}

	return layer;
}

/**
 * Create an attachment stub.
 * 
 * @param attachName
 *            a name to call this attachment point, used for finding it later in
 *            the code.
 * @param jointName
 *            the name of the joint to attach to.
 * @param poseIndex
 *            the index of the pose in our Manager to use. Usually 0.
 * @param transform
 *            a joint offset as a com.ardor3d.math.Transform object. If null,
 *            identity is used.
 */
function _addAttachment(attachName, jointName, poseIndex, transform) {
	var attach = new AttachmentPoint(attachName);
	var pose = MANAGER.getSkeletonPose(poseIndex);
	var jointIndex = pose.getSkeleton().findJointByName(jointName);
	attach.setJointIndex(jointIndex);
	attach.setOffset(transform != null ? transform
			: com.ardor3d.math.Transform.IDENTITY);
	pose.addPoseListener(attach);
	OUTPUTSTORE.addAttachmentPoint(attach);

	return;
}