package com.wheelphone.ros;

import org.ros.concurrent.CancellableLoop;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;
import org.ros.rosjava.tf.pubsub.TransformBroadcaster;


public class OdometryPublisher extends AbstractNodeMain {
	
	private double theta = 0;
	private double xPos=0, yPos=0;
	private TransformBroadcaster tf;
	
	  //@Override
	  public GraphName getDefaultNodeName() {
	    return new GraphName("pubsub/odom");
	  }

	  @Override
	  public void onStart(final ConnectedNode connectedNode) {
	    final Publisher<nav_msgs.Odometry> publisher =
	        connectedNode.newPublisher("odom", nav_msgs.Odometry._TYPE);   
	    
	    // This CancellableLoop will be canceled automatically when the node shuts
	    // down.
	    connectedNode.executeCancellableLoop(new CancellableLoop() {
	    	
	      @Override
	      protected void setup() {
	      }

	      
	      @Override
	      protected void loop() throws InterruptedException {

	    	  // new message creation
	    	  nav_msgs.Odometry value = publisher.newMessage();
	    	
	    	  // header info
	    	  std_msgs.Header h = connectedNode.getTopicMessageFactory().newFromType(std_msgs.Header._TYPE);
	    	  h.setFrameId("odom");
	    	  h.setStamp(connectedNode.getCurrentTime());
	    	  value.setHeader(h);

	    	  // quaternion info => orientation
	    	  // refer to "btQuaternion.h" to know how the x,y,z,w values are computed having only the yaw angle ("setRPY" function)			
	    	  geometry_msgs.Quaternion odom_quat = connectedNode.getTopicMessageFactory().newFromType(geometry_msgs.Quaternion._TYPE);			
	    	  odom_quat.setX(0);
	    	  odom_quat.setY(0);
	    	  odom_quat.setZ(Math.sin(theta/2.0));
	    	  odom_quat.setW(Math.cos(theta/2.0));
			
	    	  // position info
	    	  geometry_msgs.Point odom_pos = connectedNode.getTopicMessageFactory().newFromType(geometry_msgs.Point._TYPE);
	    	  odom_pos.setX(xPos);
	    	  odom_pos.setY(yPos);
	    	  odom_pos.setZ(0);
	    	  
	    	  // Pose message construction => quaternion + position
	    	  geometry_msgs.Pose pose = connectedNode.getTopicMessageFactory().newFromType(geometry_msgs.Pose._TYPE);
	    	  pose.setOrientation(odom_quat);
	    	  pose.setPosition(odom_pos);
	    	  
	    	  // PoseWithCovariance "level"
	    	  geometry_msgs.PoseWithCovariance pose_cov = connectedNode.getTopicMessageFactory().newFromType(geometry_msgs.PoseWithCovariance._TYPE);	    	  
	    	  pose_cov.setPose(pose);
	    	  
	    	  value.setPose(pose_cov);
	    	  
	    	  value.setChildFrameId("base_link");	 
			 
	    	  tf.sendTransform("odom", "base_link", h.getStamp().totalNsecs(), xPos, yPos, 0.0, odom_quat.getX(), odom_quat.getY(), odom_quat.getZ(), odom_quat.getW());	    	  
	    	  
			  publisher.publish(value);
			  Thread.sleep(100);
	      }
	    });
	  }
	  
	  public void updateData(double x, double y, double t) {
		  xPos = x/1000.0;	// in meters
		  yPos = y/1000.0;	// in meters
		  theta = t;		// radians
	  }	  

	  public void setTransformBroadcaster(TransformBroadcaster t) {
		  tf = t;
	  }

	  public void resetValues() {
		  theta = 0;
		  xPos=0;
		  yPos=0;		  
	  }
	  
}
