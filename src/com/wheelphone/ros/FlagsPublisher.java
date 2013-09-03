package com.wheelphone.ros;

import org.ros.node.AbstractNodeMain;
import org.ros.concurrent.CancellableLoop;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;

public class FlagsPublisher extends AbstractNodeMain {

	private short flagsValues [] = new short[2];
	
	  //@Override
	  public GraphName getDefaultNodeName() {
	    return new GraphName("pubsub/flags");
	  }

	  @Override
	  public void onStart(final ConnectedNode connectedNode) {
	    final Publisher<std_msgs.Int16MultiArray> publisher =
	        connectedNode.newPublisher("flags", std_msgs.Int16MultiArray._TYPE);
	    // This CancellableLoop will be canceled automatically when the node shuts
	    // down.
	    connectedNode.executeCancellableLoop(new CancellableLoop() {
	    	
	      @Override
	      protected void setup() {
	      }

	      @Override
	      protected void loop() throws InterruptedException {
	        std_msgs.Int16MultiArray value = publisher.newMessage();
	        value.setData(flagsValues);
	        publisher.publish(value);
	        Thread.sleep(100);
	      }
	    });
	  }
	  
	  public void updateData(short v[]) {
		  flagsValues[0] = v[0];
		  flagsValues[1] = v[1];
	  }	
	
}
