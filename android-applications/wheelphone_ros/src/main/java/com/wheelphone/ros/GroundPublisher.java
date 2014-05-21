package com.wheelphone.ros;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.ros.concurrent.CancellableLoop;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;

import java.nio.ByteOrder;

public class GroundPublisher extends AbstractNodeMain {
	
	private byte groundValues [] = new byte[4];
	private std_msgs.UInt8MultiArray value;
    //private ChannelBuffer buff;

    //@Override
	  public GraphName getDefaultNodeName() {
	    return GraphName.of("pubsub/ground");
	  }

	  @Override
	  public void onStart(final ConnectedNode connectedNode) {

        //buff = ChannelBuffers.buffer(ByteOrder.LITTLE_ENDIAN, 4);

	    final Publisher<std_msgs.UInt8MultiArray> publisher =
	        connectedNode.newPublisher("ground", std_msgs.UInt8MultiArray._TYPE);
	    // This CancellableLoop will be canceled automatically when the node shuts
	    // down.
	    connectedNode.executeCancellableLoop(new CancellableLoop() {
	    	
	      @Override
	      protected void setup() {
	    	  value = publisher.newMessage();
	      }

	      @Override
	      protected void loop() throws InterruptedException {
            ChannelBuffer buff = ChannelBuffers.copiedBuffer(ByteOrder.LITTLE_ENDIAN, groundValues);
            //buff.setBytes(0, groundValues);
	        value.setData(buff);
	        publisher.publish(value);
	        Thread.sleep(100);
	      }
	    });
	  }
	  
	  public void updateData(byte v[]) {
		  groundValues[0] = v[0];
		  groundValues[1] = v[1];
		  groundValues[2] = v[2];
		  groundValues[3] = v[3];
	  }
	  
}
