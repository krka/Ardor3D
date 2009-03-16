/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */
 
uniform sampler2DShadow shadowMap0;
uniform sampler2DShadow shadowMap1;
uniform sampler2DShadow shadowMap2;

varying float zDist; 
uniform vec3 sampleDist;

void main()
{  
	float shade = 0.0;
	if (zDist < sampleDist.x) {
		shade = shadow2DProj(shadowMap0, gl_TexCoord[0]).a;
	} else if (zDist < sampleDist.y)  {
    	shade = shadow2DProj(shadowMap1, gl_TexCoord[1]).a;
    } else if (zDist < sampleDist.z)  {
    	shade = shadow2DProj(shadowMap2, gl_TexCoord[2]).a;
    }

    gl_FragColor = vec4(0.0, 0.0, 0.0, shade*0.5);
}
