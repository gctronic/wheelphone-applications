package com.wheelphone.ros;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.ros.node.AbstractNodeMain;
import org.ros.concurrent.CancellableLoop;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;

import java.nio.ByteOrder;

public class ProximityPublisher extends AbstractNodeMain {
	
	private byte proxValues [] = new byte[4];
	private byte refreshCounter=0;
	private byte msgCounter=0;
	private std_msgs.UInt8MultiArray value;
	
	  //@Override
	  public GraphName getDefaultNodeName() {
	    return GraphName.of("pubsub/proximity");
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
	    	  value = publisher.newMessage();
	      }

	      @Override
	      protected void loop() throws InterruptedException {
//	        proxValues[1] = msgCounter;
//	        if(msgCounter < 255) {
//	        	msgCounter++;
//	        } else {
//	        	msgCounter=0;
//	        }
            ChannelBuffer buff = ChannelBuffers.copiedBuffer(ByteOrder.LITTLE_ENDIAN, proxValues);
	        value.setData(buff);
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
