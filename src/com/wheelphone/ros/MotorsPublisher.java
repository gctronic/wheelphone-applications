package com.wheelphone.ros;

import org.ros.node.AbstractNodeMain;
import org.ros.concurrent.CancellableLoop;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;

public class MotorsPublisher extends AbstractNodeMain {
	
	private byte motorsVel [] = new byte[2];
	
	  //@Override
	  public GraphName getDefaultNodeName() {
	    return new GraphName("pubsub/motors");
	  }

	  @Override
	  public void onStart(final ConnectedNode connectedNode) {
	    final Publisher<std_msgs.Int8MultiArray> publisher =
	        connectedNode.newPublisher("motors", std_msgs.Int8MultiArray._TYPE);
	    // This CancellableLoop will be canceled automatically when the node shuts
	    // down.
	    connectedNode.executeCancellableLoop(new CancellableLoop() {
	    	
	      @Override
	      protected void setup() {
	      }

	      @Override
	      protected void loop() throws InterruptedException {
	        std_msgs.Int8MultiArray value = publisher.newMessage();
	        value.setData(motorsVel);
	        publisher.publish(value);
	        Thread.sleep(100);
	      }
	    });
	  }
	  
	  public void updateData(byte v[]) {
		  motorsVel[0] = v[0];	// left
		  motorsVel[1] = v[1];	// right
	  }
	  
}
