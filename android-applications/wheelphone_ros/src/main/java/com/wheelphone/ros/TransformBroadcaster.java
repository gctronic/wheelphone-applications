/* 
 * Copyright 2011 Heuristic Labs, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *   
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wheelphone.ros;

import android.util.Log;

import geometry_msgs.TransformStamped;

import java.util.ArrayList;

import org.ros.concurrent.CancellableLoop;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;

import tf2_msgs.TFMessage;

/**
 * @author nick@heuristiclabs.com (Nick Armstrong-Crews)
 * @brief This is a simple class to provide sendTransform() (akin to rospy and roscpp versions); it handles creation of publisher and advertising for you.
 */

public class TransformBroadcaster extends AbstractNodeMain {

	protected Publisher<TFMessage> pub;
	public boolean sendTransform = false;
	
	String parent, child;
	long timeStamp; // in nanoseconds
	double vX, vY, vZ;
	double qX, qY, qZ, qW; // quaternion

    TFMessage tfMsg;
    geometry_msgs.TransformStamped odomToBaseLink;
    std_msgs.Header h;
    geometry_msgs.Vector3 vec;
    geometry_msgs.Quaternion quat;
    geometry_msgs.Transform trans;
    ArrayList<TransformStamped> tsList;

    //@Override
	public GraphName getDefaultNodeName() {
		return GraphName.of("/transform");
	}	
	
	@Override
	public void onStart(final ConnectedNode connectedNode) {	

		this.pub = connectedNode.newPublisher("/tf", TFMessage._TYPE);
		this.pub.setLatchMode(true);
		
		// This CancellableLoop will be canceled automatically when the node shut down.
		connectedNode.executeCancellableLoop(new CancellableLoop() {
		    	
			@Override
			protected void setup() {
                // new message creation
                tfMsg = pub.newMessage();

                odomToBaseLink = connectedNode.getTopicMessageFactory().newFromType(geometry_msgs.TransformStamped._TYPE);

                // header info
                h = connectedNode.getTopicMessageFactory().newFromType(std_msgs.Header._TYPE);

                vec = connectedNode.getTopicMessageFactory().newFromType(geometry_msgs.Vector3._TYPE);

                quat = connectedNode.getTopicMessageFactory().newFromType(geometry_msgs.Quaternion._TYPE);

                trans = connectedNode.getTopicMessageFactory().newFromType(geometry_msgs.Transform._TYPE);

                tsList = new ArrayList<TransformStamped>(1);
                tsList.add(odomToBaseLink);
			}

			@Override
			protected void loop() throws InterruptedException {
				
				if(sendTransform) {
					
					sendTransform = false;

					h.setFrameId(parent);
					h.setStamp(org.ros.message.Time.fromNano(timeStamp));

                    odomToBaseLink.setHeader(h);
                    odomToBaseLink.setChildFrameId(child);

					// TODO: invert transform, if it is not cool (have to add tfTree here, then...)
					vec.setX(vX);
					vec.setY(vY);
					vec.setZ(vZ);

					quat.setX(qX);
					quat.setY(qY);
					quat.setZ(qZ);
					quat.setW(qW);					

					trans.setTranslation(vec);
					trans.setRotation(quat);
                    odomToBaseLink.setTransform(trans);

                    //tsList.clear();
					//tsList.add(transStamped);
                    tsList.set(0, odomToBaseLink);
                    tfMsg.setTransforms(tsList);

					pub.publish(tfMsg);

                    Log.d("TAG", "parent="+parent+", child="+child+", vx="+vX+", vy="+vY+", vz="+vZ+", qx="+qX+", qy="+qY+", qz="+qZ+", qw="+qW+", timestamp="+timeStamp);

				}
		    	
				Thread.sleep(100);
				
		      }
		    });		
	}

	public void sendTransform(
									String parentFrame, String childFrame,
									long t, // in nanoseconds
									double v_x, double v_y, double v_z,
									double q_x, double q_y, double q_z, double q_w // quaternion
									) {	
		
		// WARN if quaternion not normalized, and normalize it
		// WARN if time is in the future, or otherwise looks funky (negative? more than a year old?)
		//Preconditions.checkNotNull(node);
		
		parent = parentFrame;
		child = childFrame;
		timeStamp = t;
		vX = v_x;
		vY = v_y;
		vZ = v_z;
		qX = q_x;
		qY = q_y;
		qZ = q_z;
		qW = q_w;
		sendTransform = true;

	}

}
