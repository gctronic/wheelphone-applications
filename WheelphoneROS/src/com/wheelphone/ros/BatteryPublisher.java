package com.wheelphone.ros;

import org.ros.node.AbstractNodeMain;
import org.ros.concurrent.CancellableLoop;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;

public class BatteryPublisher extends AbstractNodeMain {

	private byte batteryValue;
	
	  //@Override
	  public GraphName getDefaultNodeName() {
	    return new GraphName("pubsub/battery");
	  }

	  @Override
	  public void onStart(final ConnectedNode connectedNode) {
	    final Publisher<std_msgs.UInt8> publisher =
	        connectedNode.newPublisher("battery", std_msgs.UInt8._TYPE);
	    // This CancellableLoop will be canceled automatically when the node shuts
	    // down.
	    connectedNode.executeCancellableLoop(new CancellableLoop() {
	    	
	      @Override
	      protected void setup() {
	      }

	      @Override
	      protected void loop() throws InterruptedException {
	        std_msgs.UInt8 value = publisher.newMessage();
	        value.setData(batteryValue);
	        publisher.publish(value);
	        Thread.sleep(10000);
	      }
	    });
	  }
	  
	  public void updateData(byte v) {
		  batteryValue = v;
	  }	
	
}
