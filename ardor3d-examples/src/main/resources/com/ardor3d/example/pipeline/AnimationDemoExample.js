/**
 * Our base Layer
 */
var baseLayer = MANAGER.baseAnimationLayer;

// set up walk/run as our base layer as separate states.
var walkState = {
	"name" : "walk_anim",
	"clip" : "skeleton.walk",
	"transitions" : {
	    "run" : [
             "-", "-", // start/end window
	         "syncfade", // type
	         "run_anim", // target
	         0.5, // fade time
	         "Linear" // type
	         ]
	},
	"endTransition" : [
         "-", "-", // start/end window
         "immediate", // type
         "walk_anim" // target
         ]
};

// add to layer
baseLayer.addSteadyState(_steadyState(walkState));

var runState = {
	"name" : "run_anim",
	"clip" : "skeleton.run",
	"transitions" : {
	    "walk" : [
             "-", "-", // start/end window
	         "syncfade", // type
	         "walk_anim", // target
	         0.75, // fade time
	         "Linear" // type
	         ]
	},
	"endTransition" : [
             "-", "-", // start/end window
	         "immediate", // type
	         "run_anim" // target
	         ]
};

// add to layer
baseLayer.addSteadyState(_steadyState(runState));

/**
 * Our Punch Layer
 */
var punchLayerInfo = {
	"name" : "punch",
	"blendType" : "lerp",
	"blendWeight" : 1.0,
	"blendKey" : "punch_blend"
};
	
var punchLayer = _animationLayer(punchLayerInfo);
MANAGER.addAnimationLayer(punchLayer);

// set up punching state. Will not always be playing.
var punchState = {
	"name" : "punch_right",
	"tree" : {
		"inclusiveClip" : {
			"name" : "skeleton.punch",
			joints : [11, 12, 13, 14, 15],
			channels : ["punch_fire"],
			"active" : false
		}
	}
};

// add to layer
punchLayer.addSteadyState(_steadyState(punchState));

/**
 * Our Head Layer
 */
var headLayerInfo = {
	"name" : "head",
	"blendType" : "lerp",
	"blendWeight" : 1.0,
	"blendKey" : "head_blend"
};
	
var headLayer = _animationLayer(headLayerInfo);
MANAGER.addAnimationLayer(headLayer);

// set up head turning state. Will not always be playing.
var headState = {
	"name" : "head_rotate",
	"tree" : {
		"managed" : {
			"initFromClip" : {
				"clip" : "skeleton.walk",
				"jointNames" : ["Bip01_Head"]
			}
		}
	},
};

// add to layer
headLayer.addSteadyState(_steadyState(headState));

// create attachment points
_addAttachment("right_weapon", "Bip01_R_Finger0", 0, null);
_addAttachment("left_weapon", "Bip01_L_Finger0", 0, null);
