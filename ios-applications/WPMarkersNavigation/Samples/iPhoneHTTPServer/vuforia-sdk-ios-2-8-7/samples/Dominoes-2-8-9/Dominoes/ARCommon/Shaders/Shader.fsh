/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 Confidential and Proprietary - Qualcomm Connected Experiences, Inc.
 ==============================================================================*/

const char* fragmentShader = MAKESTRING(
precision mediump float;
varying vec2 texCoord;

uniform sampler2D texSampler2D;

void main()
{
    gl_FragColor = texture2D(texSampler2D, texCoord);
}
);
