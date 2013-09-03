package com.wheelphone.ros;

import org.ros.concurrent.CancellableLoop;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;

public class GroundPublisher extends AbstractNodeMain {
	
	private short groundValues [] = new short[4];
	
	  //@Override
	  public GraphName getDefaultNodeName() {
	    return new GraphName("pubsub/ground");
	  }

	  @Override
	  public void onStart(final ConnectedNode connectedNode) {
	    final Publisher<std_msgs.Int16MultiArray> publisher =
	        connectedNode.newPublisher("ground", std_msgs.Int16MultiArray._TYPE);
	    // This CancellableLoop will be canceled automatically when the node shuts
	    // down.
	    connectedNode.executeCancellableLoop(new CancellableLoop() {
	    	
	      @Override
	      protected void setup() {
	      }

	      @Override
	      protected void loop() throws InterruptedException {
	        std_msgs.Int16MultiArray value = publisher.newMessage();
	        value.setData(groundValues);
	        publisher.publish(value);
	        Thread.sleep(100);
	      }
	    });
	  }
	  
	  public void updateData(short v[]) {
		  groundValues[0] = v[0];
		  groundValues[1] = v[1];
		  groundValues[2] = v[2];
		  groundValues[3] = v[3];
	  }
	  
}
