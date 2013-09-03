package com.wheelphone.ros;

import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Subscriber;

public class PhoneListeners extends AbstractNodeMain {
	
	public byte flagStatus=0;
	private byte desiredVel [] = new byte[2];
	WheelphoneROS mainAct;
	
	  public GraphName getDefaultNodeName() {
	    return new GraphName("pubsub/listeners");
	  }

	  public void onStart(ConnectedNode connectedNode) {
    
	    Subscriber<std_msgs.Int8> subscribProximity = connectedNode.newSubscriber("flagStatus", std_msgs.Int8._TYPE);
	    subscribProximity.addMessageListener(new MessageListener<std_msgs.Int8>() {
	    	public void onNewMessage(std_msgs.Int8 message) {
	    		flagStatus = message.getData();
	    		mainAct.updateFlagStatus(flagStatus);
	      }
	    });
	    
	    Subscriber<std_msgs.Int8MultiArray> subscribMotorSpeed = connectedNode.newSubscriber("motorSpeed", std_msgs.Int8MultiArray._TYPE);
	    subscribMotorSpeed.addMessageListener(new MessageListener<std_msgs.Int8MultiArray>() {
	    	public void onNewMessage(std_msgs.Int8MultiArray message) {
	    		//desiredVel = message.getData();
	    		desiredVel[0] = message.getData()[0];
	    		desiredVel[1] = message.getData()[1];
	    		mainAct.updateMotorSpeed(desiredVel);
	      }
	    });	    
	    
	  }
	  
	  void setMainActivity(WheelphoneROS ma) {
		  mainAct = ma;
	  }
	  
	  
}
