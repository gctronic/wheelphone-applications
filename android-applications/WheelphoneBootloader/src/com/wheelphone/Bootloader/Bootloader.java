package com.wheelphone.Bootloader;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Vector;

import com.wheelphone.BootLoader.R;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/** A class that extends activity. This class handles inter-thread communication
 *  and updating the layout for the web bootloader demo.
 *  
 * @author Microchip Technology Inc.
 *
 */
public class Bootloader extends Activity {
	// Message commands from other threads 
	private static final int USB_EVENT_RECEIVED = 0;
	private static final int XML_PARSING_COMPLETE = 1;
	private static final int USB_PACKET_READY = 2;

	// Commands for USB Packets
	protected static final byte ENTER_BOOT_MODE_REQ = 0;
	protected static final byte START_BOOT_MODE_REQ = 1;
	protected static final byte READ_FILE_REQ = 2;
	protected static final byte LOAD_COMPLETE_RSP = 3;

	// Error codes for LOAD_COMPLETE_RSP
	protected static final byte BOOT_SUCCESS = 0;
	protected static final byte VERIFICATION_FAIL = 1;

	// Constants
	protected static final boolean updateFromInternet = true;
	protected static final String xml_file_name = "versions-rev82.xml";
	protected static final String file_system_dir = "sdcard/Android/data/com.microchip.android.WebBootLoader/";
	protected static final String xml_file_url = "http://wheelphone.gctronic.com/versions.xml"; 
	//protected static final String xml_file_url = "http://ww1.microchip.com/downloads/en/SoftwareLibrary/versions.xml";

	// Private variables for WebBootLoader class
	private USBAccessoryManager accessoryManager;			// low-level USB communication class
	private VersionInfo accessoryInfo;
	private VersionInfo updatedAccessoryInfo;
	private BinaryFileParser binParser;

	// Various
	public String debugStr = null;
	public int firmwareSize = 0;
	public int currentBytesSent = 0;
	public boolean debug = false;
	
	// UI
	TextView view;
	TextView updateStatusView;
	TextView debugView;
	
	// Function Override that is called when the application is started
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// Call super class function 
		super.onCreate(savedInstanceState);
		
		// Set Content view to layout defined in main.xml
		setContentView(R.layout.main);
		
		// Instantiate USB accessory manager with the handler inside WebBootLoader and it's Message command
    	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
    		accessoryManager = new USBAccessoryManagerAndroidLib(handler, USB_EVENT_RECEIVED);
    		if(debug) {
    			debugStr = "AndroidLib\n";
    		}
    	} else {
    		accessoryManager = new USBAccessoryManagerAddOnLib(handler, USB_EVENT_RECEIVED);
    		if(debug) {
    			debugStr = "AddOnLib\n";
    		}
    	}		

    	if(debug) {
    		debugView = (TextView) findViewById(R.id.debugView);		
    		debugView.setVisibility(View.VISIBLE);
    		debugView.setText(debugStr);
    		debugView.setMovementMethod(new ScrollingMovementMethod());
    	}
		
		updateStatusView = (TextView) findViewById(R.id.updateStatus);
		
		// disable strict mode due to error "androidblockguardpolicy. onnetwork strictmode" given 
		// from HONEYCOMB onward
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);
		
	}

	// Function Override that is called after onCreate, a restart of the activity, or resuming from a pause
	@Override
	public void onResume() {
		// Call super class function
		super.onResume();
		
		// Enable the USB thread to start reading USB packets sent from host
		accessoryManager.enable(this);
		
		if(debug) {
			debugStr += "onResume => enable\n";		
			debugView.setText(debugStr);
		}
	}

	// Function Override that is called when another activity comes in front of this one
	@Override
	public void onPause() {
		// Call super class function
		super.onPause();
		
		// Disable the USB thread
		accessoryManager.disable(this);
		
		if(debug) {
			debugStr += "onPause => disable\n";		
			debugView.setText(debugStr);
		}
		
		// Disconnect accessory
		disconnectAccessory();
		
	}

	/** Show disconnect of accessory in layout
	 * 
	 */
	public void disconnectAccessory() {

		// Stop parsing the binary file if that thread has been started
		if (binParser != null) {
			binParser.Cancel();
		}

		if(debug) {
			debugStr += "disconnectAccessory => no device attached\n"; 
			debugView.setText(debugStr);
		}
		
		// Update the layout to show disconnect of layout
		view = (TextView) findViewById(R.id.description);		 
		view.setText("No device attached");

		view = (TextView) findViewById(R.id.model);
		view.setVisibility(View.GONE);

		view = (TextView) findViewById(R.id.version);
		view.setVisibility(View.GONE);

		view = (TextView) findViewById(R.id.updateStatus);
		view.setVisibility(View.GONE);

	}

	/***********************************************************************/
	/** Private section **/
	/***********************************************************************/
    // Create a Handler to accept messages from other threads
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			byte[] commandCode = new byte[1];

			// Which thread sent message?
			switch (msg.what) {
			// If the USB Accessory Manager thread sent message
			case USB_EVENT_RECEIVED:
				// Then check to see what event occurred
				switch (((USBAccessoryManagerMessage) msg.obj).type) {
				// If an accessory has been connected, then do nothing
				case ATTACHED:
					if(debug) {
						debugStr += "Attached\n"; 		
						debugView.setText(debugStr);
					}
					break;
				// If an accessory has been connected, and is ready
				case READY:
					if(debug) {
						debugStr += "Ready\n"; 	
						debugView.setText(debugStr);
					}
					
					// Instantiate a new VersionInfo object to keep track of the accessory's information
					accessoryInfo = new VersionInfo(accessoryManager.getVersion(), accessoryManager.getModel(), accessoryManager.getDescription());

					if(debug) {
						debugStr += "accessory version: " + accessoryManager.getVersion() + "\n";
						debugStr += "accessory model: " + accessoryManager.getModel() + "\n";
						debugStr += "accessory desc: " + accessoryManager.getDescription() + "\n";
						debugView.setText(debugStr);
					}
					
					// Update the view with the model, description, and version of the attached accessory
					view = (TextView) findViewById(R.id.model);
					if(accessoryInfo.getModel().equals("Web Bootloader Accessory Demo")) {
						view.setText("Wheelphone Robot");
					} else {
						view.setText(accessoryInfo.getModel());
					}
					
					view = (TextView) findViewById(R.id.description);
					if(accessoryInfo.getDescription().equals("PIC24FJ64GB004 PIM")) {
						view.setText("");
					} else {
						view.setText(accessoryInfo.getDescription());						
					}												
					view.setVisibility(View.VISIBLE);
					
					//view = (TextView) findViewById(R.id.version);
					//view.setText("v" + accessoryInfo.getVersionString()); 
					//view.setVisibility(View.VISIBLE);

					// If accessory is in Bootloader mode and we've gotten the updated application info
					if ((accessoryInfo.isInBootloadMode()) && (updatedAccessoryInfo != null)) {
						// Then we can start bootloading by sending the START_BOOT_MODE_REQ command to accessory
						byte[] startBootloaderPacket = new byte[1];
						startBootloaderPacket[0] = START_BOOT_MODE_REQ;
						accessoryManager.write(startBootloaderPacket);

						if(debug) {
							debugStr += "WRITE START_BOOT_MODE_REQ\n"; 
							debugStr += "Starting Bootloading...\n";
							debugView.setText(debugStr);
						}
						
						// And update the title showing bootloader is starting
						view = (TextView) findViewById(R.id.updateStatus);
						view.setText("Starting Bootloading..."); 
						view.setVisibility(View.VISIBLE);
					} else {
						
						if(debug) {
							debugStr += "searching for updates\n"; 	
							debugView.setText(debugStr);
						}
						
						// Otherwise update the title showing searching for updates
						view = (TextView) findViewById(R.id.updateStatus);
						view.setText("Searching for updates..."); 
						view.setVisibility(View.VISIBLE);
						
						// If we are updating from the Internet
						if(updateFromInternet)
						{
							if(debug) {
								debugStr += "Get XML file from internet\n"; 		
								debugView.setText(debugStr);
							}
							
							// Check for connectivity
							ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
							NetworkInfo netInfo = cm.getActiveNetworkInfo();
							if (netInfo != null && netInfo.isConnectedOrConnecting()) {
								// If connected, then try to connect to versions.xml on microchip.com website
								try {
									URL xmlURL = new URL(xml_file_url); 
									URLConnection xmlConnection = xmlURL.openConnection();
									
									// If successfully connected, then instantiate a VersionsXMLParser thread to parse versions.xml 
									VersionsXMLParser parser = new VersionsXMLParser(handler, XML_PARSING_COMPLETE);
									parser.ParseThreaded(xmlConnection.getInputStream());
								} catch (MalformedURLException e) {
									// If the URL is incorrect, update title
									if(debug) {
										debugStr += "Unable to connect. Illegal URL.\n"; 	
										debugView.setText(debugStr);
									}
									e.printStackTrace();
									view.setText("Unable to connect. Illegal URL."); 
								} catch (IOException e) {
									// If a file error occurs, update title
									if(debug) {
										debugStr += "Unable to connect. File error.\n"; 	
										debugView.setText(debugStr);
									}
									e.printStackTrace();
									view.setText("Unable to connect. File Error."); 
								}
							} else {
								// If not connected, update title
								if(debug) {
									debugStr += "Internet connection not available\n"; 	
									debugView.setText(debugStr);
								}
								
								view.setText("Internet connection not available."); 
							}
						} else {
							if(debug) {
								debugStr += "Get XML file from file system\n"; 	
								debugView.setText(debugStr);
							}
							
							// Otherwise, get xml file from Android file system, inside WebBootLoader directory.
							File file = new File(file_system_dir + xml_file_name);
							
							// Check to see if file exists
							if(file.exists()) {
								// If so, then instantiate a new VersionXMLParser thread to parse the file
								VersionsXMLParser parser = new VersionsXMLParser(handler, XML_PARSING_COMPLETE);
								parser.ParseThreaded(file);
							} else {
								// If the file doesn't exist, update title
								view.setText("File does not exist."); 
							}
						}
					}
					break;
				// If the accessory was disconnected
				case DETACHED:
					if(debug) {
						debugStr += "detached\n"; 		
						debugView.setText(debugStr);
					}
					// Then update the view
					disconnectAccessory();
					break;
				// If a USB packet was read
				case READ:
					if(debug) {
						debugStr += "Read\n"; 	
						debugView.setText(debugStr);
					}
					
					// Then make sure that the accessory is still connected
					if (accessoryManager.isConnected() == false) {
						return;
					}

					// While data is available to read
					while (accessoryManager.available() != 0) {
						// Read the first byte to see what the command was
						accessoryManager.read(commandCode);
						switch (commandCode[0]) {
						// If READ_FILE_REQ command read
						case READ_FILE_REQ:
							
							if(debug) {
								debugStr += "READ_FILE_REQ\n";
								debugStr += "Updating Accessory...";
								debugView.setText(debugStr);
							}
							
							// Then the accessory wants to be updated with new firmware
							view = (TextView) findViewById(R.id.updateStatus);
							view.setText("Updating Accessory..."); 

							// If we are updating from the Internet
							if(updateFromInternet)
							{
								if(debug) {
									debugStr += "Get binary from internet\n"; 	
									debugView.setText(debugStr);
								}
								
								// Check for connectivity
								ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
								NetworkInfo netInfo = cm.getActiveNetworkInfo();
								if (netInfo != null && netInfo.isConnectedOrConnecting()) {
									// If connected, then try to connect to the associated url for the binary file on microchip.com website.
									try {
										URL binaryURL = new URL(updatedAccessoryInfo.getURL());
										URLConnection binaryConnection = binaryURL.openConnection();
										
										if(debug) {
											debugStr += "Binary size: " + binaryConnection.getContentLength() + "\n"; 	
											debugView.setText(debugStr);
										}
										
										firmwareSize = binaryConnection.getContentLength();										
												
										// If successfully connected, then instantiate a BinaryFileParser thread to parse the binary file 
										binParser = new BinaryFileParser(handler, USB_PACKET_READY);
										binParser.ParseThreaded(binaryConnection.getInputStream());
									} catch (MalformedURLException e) {
										// If the URL is incorrect, update title
										e.printStackTrace();
										view.setText("Unable to connect. Illegal URL."); 
									} catch(IOException e) {
										// If a file error occurs, update title
										view.setText("Unable to connect. File error."); 
										e.printStackTrace();
									}
								} else {
									// If not connected, update title
									view.setText("Internet connection not available."); 
								}
							} else {
								
								if(debug) {
									debugStr += "Get binary from file system\n"; 	
									debugView.setText(debugStr);
								}
								
								// Otherwise, get binary file from Android file system, inside WebBootLoader directory.
								File file = new File(file_system_dir + updatedAccessoryInfo.getFileName()); 
								
								// Check to see if file exists
								if(file.exists()) {
									// If so, then instantiate a new BinaryFileParser thread to parse the file
									binParser = new BinaryFileParser(handler, USB_PACKET_READY);
									binParser.ParseThreaded(file);
								} else {
									// If the file doesn't exist, update title
									view.setText("File does not exist."); 
								}
							}
							break;
						// If LOAD_COMPLETE_RSP command read
						case LOAD_COMPLETE_RSP:
							
							if(debug) {
								debugStr += "LOAD_COMPLETE_RSP\n"; 		
								debugView.setText(debugStr);
							}
							
							byte[] errorCode = new byte[4];
							int numRead;
							
							// Cancel the binary file parser thread and read the next four bytes for the error code
							binParser.Cancel();
							view = (TextView) findViewById(R.id.updateStatus);
							numRead = accessoryManager.read(errorCode);

							// If less than four bytes received  
							if (numRead < 4) {
								if(debug) {
									debugStr += "No error code received\n"; 	
									debugView.setText(debugStr);
								}
								
								// No error code received, Update title
								view.setText("No error code received."); 
							} else {
								// Otherwise Convert errorCode from 4 byte array to int
								ByteBuffer bb = ByteBuffer.wrap(errorCode);
								IntBuffer ib = bb.asIntBuffer();
								int errorCodeInt = ib.get(0);
	
								// Update title based on error code
								switch (errorCodeInt) {
								case BOOT_SUCCESS:
									view.setText("Update Complete."); 
									if(debug) {
										debugStr += "Update complete\n";
									}
									break;
								case VERIFICATION_FAIL:
									view.setText("Verification Failed.");
									if(debug) {
										debugStr += "Verification failed\n";
									}
									break;
								default:
									view.setText("Unknown Error: " + Integer.toString(errorCodeInt));
									if(debug) {
										debugStr += "Unknown error\n";
									}
									break;
								}	
								if(debug) {
									debugView.setText(debugStr);
								}
							}
							break;
						default:
							break;
						}
					}
					break;
				}
				break;
			// If the XML parsing thread sent the message
			case XML_PARSING_COMPLETE:
				// Get the parser object
				VersionsXMLParser parser = (VersionsXMLParser) msg.obj;

				if(debug) {
					debugStr += "XML_PARSING_COMPLETE\n"; 	
					debugView.setText(debugStr);
				}		
				
				// If in bootloader mode
				if(accessoryInfo.isInBootloadMode()) {
					// That means that we started in bootloader mode and don't have an application, so just filter by board name (description)
					parser.filterResults(accessoryInfo.getDescription());
					if(debug) {
						debugStr += "Bootload mode => Filter by description\n"; 	
						debugView.setText(debugStr);
					}
				} else {
					// Otherwise we can filter by both application (model) and board name (description)
					if(debug) {
						debugStr += "Not Bootload Mode => Filter by model and description\n"; 	
						debugView.setText(debugStr);
					}
					parser.filterResults(accessoryInfo.getModel(), accessoryInfo.getDescription());
				}
				
				// Also filter to get the latest version
				parser.filterResultsBestVersionOnly();

				// And save the results
				Vector<VersionInfo> versions = parser.GetResults();

				view = (TextView) findViewById(R.id.revision);
				view.setText("Firmware " + versions.elementAt(0).getURL().split("-")[2]);
				view = (TextView) findViewById(R.id.changelog);
				view.setText("Changelog: " + versions.elementAt(0).getChangeLog());

				view = (TextView) findViewById(R.id.updateStatus);
				
				// If there are any versions available
				if ((versions != null) && (versions.size() != 0)) {
					// We'll just take the first one
					updatedAccessoryInfo = versions.elementAt(0);
					
					// If we're in bootloader mode
					if (accessoryInfo.isInBootloadMode()) {
						// Then we can start bootloading by sending the START_BOOT_MODE_REQ command to accessory
						byte[] startBootloaderPacket = new byte[1];
						startBootloaderPacket[0] = START_BOOT_MODE_REQ;
						accessoryManager.write(startBootloaderPacket);
						
						if(debug) {
							debugStr += "WRITE START_BOOT_MODE_REQ\n"; 	
							debugView.setText(debugStr);
						}

						// And update the title showing bootloader is starting
						view = (TextView) findViewById(R.id.updateStatus);
						view.setText("Starting Bootloading..."); 
						view.setVisibility(View.VISIBLE);
					}
				} else {
					if(debug) {
						debugStr += "No updates found.\n"; 	
						debugView.setText(debugStr);
					}
					
					// Otherwise show there are no updates
					view.setText("No updates found."); 
				}
				break;
			// If the binary file parser thread sent the message
			case USB_PACKET_READY:
				if(debug) {
					debugStr += "USB_PACKET_READY\n"; 	
					debugView.setText(debugStr);
				}
				
				// Then read the packet of bytes that will be sent
				byte[] packet = (byte[]) msg.obj;
				Log.d("WebBootloader", "Binary Message Received, size: " + Integer.toString(packet.length));  
				
				currentBytesSent += packet.length;
				updateStatusView.setText(Integer.toString(currentBytesSent*100/firmwareSize)+"%");
				
				if(debug) {
					debugStr += "Binary Message Received, size: " + Integer.toString(packet.length) + "\n"; 	
					debugView.setText(debugStr);
				}
				
				// And add that packet to the USB accessory manager's output buffer
				accessoryManager.write(packet);
				
				if(debug) {
					debugStr += "WRITE packet\n"; 	
					debugView.setText(debugStr);
				}
				
				break;
			default:
				break;
			}
		}
	};
}