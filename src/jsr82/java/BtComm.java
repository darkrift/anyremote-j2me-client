//
// anyRemote java client
// a bluetooth remote for your PC.
//
// Copyright (C) 2006-2010 Mikhail Fedotov <anyremote@mail.ru>
// 
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA. 
//

import java.io.IOException;
import java.util.Enumeration;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DataElement;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;

public class BtComm implements DiscoveryListener {

 	//private static final String AR_UUID="40BA0016474D40718359F4B94FA1CAD7";	 // "1101" is used for SPP

	private int            foundDevicesCount;
	private RemoteDevice[] foundDevices;
	Controller             controller;
        
	public BtComm() {
 		foundDevices = new RemoteDevice[7];
 	}
        
        public void setController(Controller ctl) {
		controller = ctl;
	}
        
	//
        // JSR-82 stuff
        //

        // Fix issues for some buggy SE phones
        // http://developer.sonyericsson.com/thread.jspa?messageID=73104
        private String fixNullConnectionURL(ServiceRecord record) {
         
		DataElement protocolDescriptorList = record.getAttributeValue(0x0004);
		Enumeration e = (Enumeration) protocolDescriptorList.getValue(); // DATSEQ | DATALT

		e.nextElement(); 					// L2CAP (ignored)
		DataElement protocolDescriptorRFCOMM = (DataElement) e.nextElement();
		e = (Enumeration) protocolDescriptorRFCOMM.getValue();	// DATSEQ
		e.nextElement(); 					// UUID (ignored)
		DataElement channelRFCOMM = (DataElement) e.nextElement();
		long channel = channelRFCOMM.getLong(); 		// U_INT_1
            
		StringBuffer nameBuffer = new StringBuffer(69); 	//5+3+12+1+2+19+14+13);
		nameBuffer.append("btspp://");
		nameBuffer.append(record.getHostDevice().getBluetoothAddress());
		nameBuffer.append(":");
		nameBuffer.append(channel);
		nameBuffer.append(";authenticate=false");
		nameBuffer.append(";encrypt=false");
		nameBuffer.append(";master=false");
            
		return nameBuffer.toString();

	}

	public void startSearch() {

		controller.searchForm.removeCommand(controller.searchCommand);
		foundDevicesCount = 0;
		
                controller.searchForm.setTitle("Searching ...");
		controller.handleSettings("\n\n\n",false);  // add used devices to the list
                
		try {
			DiscoveryAgent agent = LocalDevice.getLocalDevice().getDiscoveryAgent();
			agent.startInquiry(DiscoveryAgent.GIAC, this);
		} catch (BluetoothStateException e) {
			e.printStackTrace();
			controller.searchForm.setTitle("Error - stopped");
			controller.searchForm.addCommand(controller.searchCommand);
		} catch (Exception e) {
			controller.searchForm.setTitle("Error - Exception");
		}
	}

	public void deviceDiscovered(RemoteDevice dev, DeviceClass dummy) {
		if (foundDevicesCount < foundDevices.length) {
			foundDevices[foundDevicesCount] = dev;
			foundDevicesCount++;
			controller.searchForm.setTitle("Found "+foundDevicesCount);
		}
	}

	public void servicesDiscovered(int transID, ServiceRecord[] services) {
		for (int n=0; n<services.length; ++n) {
			String url = services[n].getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
                        
			if (url == null) {
                        	controller.searchForm.setTitle("Error: URL is null");
                        	url = fixNullConnectionURL(services[n]);
                        } 
                        
                        String name;
                        
			String devName = null;
			try {
				String tmpDevName = services[n].getHostDevice().getFriendlyName(false);
				if (tmpDevName != null && tmpDevName.length() > 0) {
					devName = tmpDevName;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}			
			int serviceNameOffset = 0x0000;
			int primaryLanguageBase = 0x0100;
			DataElement de = services[n].getAttributeValue(primaryLanguageBase + serviceNameOffset);
			String srvName = null;
			if (de != null && de.getDataType() == DataElement.STRING) {
				srvName = (String)de.getValue();
			}
			if (devName != null && srvName != null) {
				name = devName + " - "+srvName;
			}
			else if (devName != null) {
				name = devName;
			}
			else if (srvName != null) {
				name = services[n].getHostDevice().getBluetoothAddress() + "/" + srvName;
			}
			else {
				name = "???";
			}
			controller.addInfo(name+"\n"+url+"\n");
			controller.searchForm.setTitle("Server found");
		}
	}

	public void serviceSearchCompleted(int arg0, int arg1) {
		searchNextDeviceForServices();
	}

	void searchNextDeviceForServices() {
		if (foundDevicesCount > 0) {
			foundDevicesCount--;
			RemoteDevice dev = foundDevices[foundDevicesCount];
			foundDevices[foundDevicesCount] = null;
			try {
				DiscoveryAgent agent = LocalDevice.getLocalDevice().getDiscoveryAgent();
				UUID[] serialUUID = new UUID[1]; //2];
				serialUUID[0] = new UUID("1101", true);		// btspp
				//serialUUID[1] = new UUID("110E", true);         // AVRCP TG

				int attrSet[] = new int[1]; //2];
				attrSet[0] = 0x0100; 	// service name (primary language)
				//attrSet[1] = 0x0103; 	
				controller.searchForm.setTitle("Retrieving services "+foundDevicesCount+" ...");
				agent.searchServices(attrSet, serialUUID, dev, this);
			} catch (BluetoothStateException e) {
				//e.printStackTrace();
				controller.searchForm.setTitle("Error "+dev.getBluetoothAddress()+" "+e);
                                searchNextDeviceForServices();
			}
		} else {
			controller.addCommPorts();
                        controller.searchForm.setTitle("Done.");
			controller.searchForm.addCommand(controller.searchCommand);
		}
	}

	public void inquiryCompleted(int arg0) {
		//controller.searchForm.setTitle("Done.");
		searchNextDeviceForServices();
	}
	/* crash midlet with jsr-82 v1.0
	public boolean isBtOn() {
		try {
			return LocalDevice.getLocalDevice().isPowerOn();
		} catch (Exception e) {
		} catch (Error e) {}
		return false;
	}*/
}
