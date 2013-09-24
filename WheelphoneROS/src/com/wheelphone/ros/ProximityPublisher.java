package com.wheelphone.ros;

import org.ros.node.AbstractNodeMain;
import org.ros.concurrent.CancellableLoop;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;

public class ProximityPublisher extends AbstractNodeMain {
	
	private byte proxValues [] = new byte[4];
	private byte refreshCounter=0;
	private byte msgCounter=0;
	
	  //@Override
	  public GraphName getDefaultNodeName() {
	    return new GraphName("pubsub/proximity");
	  }

	  @Override
	  public void onStart(final ConnectedNode connectedNode) {
	    final Publisher<std_msgs.UInt8MultiArray> publisher =
	        connectedNode.newPublisher("proximity", std_msgs.UInt8MultiArray._TYPE);
	    // This CancellableLoop will be canceled automatically when the node shuts
	    // down.
	    connectedNode.executeCancellableLoop(new CancellableLoop() {
	    	
	      @Override
	      protected void setup() {
	      }

	      @Override
	      protected void loop() throws InterruptedException {
	        std_msgs.UInt8MultiArray value = publisher.newMessage();
//	        proxValues[1] = msgCounter;
//	        if(msgCounter < 255) {
//	        	msgCounter++;
//	        } else {
//	        	msgCounter=0;
//	        }	        
	        value.setData(proxValues);
	        publisher.publish(value);
	        Thread.sleep(100);
	      }
	    });
	  }
	  
	  public void updateData(byte v[]) {
//		  proxValues[0] = refreshCounter; //v[0];
//		  if(refreshCounter < 255) {
//			  refreshCounter++;
//		  } else {
//			  refreshCounter=0;
//		  }
		  proxValues[0] = v[0];
		  proxValues[1] = v[1];
		  proxValues[2] = v[2];
		  proxValues[3] = v[3];
	  }
}
