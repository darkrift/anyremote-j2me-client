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

import java.util.Vector;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

public class ListForm extends CanvasConsumer  {

	Controller  controller;
        String      caption;
	CanvasPanel panel;
        Command	    b;
        
        boolean     useIconsStream;  // used only for streamed loading
        String      bufferedItem;
        
	public ListForm(Controller ctl) {
		controller = ctl;
                
                bufferedItem = "";
                
                panel = new CanvasPanel(controller, 0,0,0,255,255,255,0,0,0, Font.SIZE_MEDIUM, CanvasPanel.LIST);
		caption = "";
				
		b = new Command("Back", Command.SCREEN, 4);
 	}

	public void processData(Vector vR, boolean useIcons, int stage) {	// add|replace|close|clear|show[,Title,item1,...itemN]
		                                                          	// or menu,item1,...itemN
                //controller.showAlert("processData() " +stage);  
        	String oper  = (String) vR.elementAt(1); 
                                        
                if (oper.equals("clear")) {
		
                	cleanUp();
             	
                } else if (oper.equals("close")) {
                
                	if (vR.size() > 2 && ((String) vR.elementAt(2)).equals("clear")) {
                		cleanUp();	// stop ticker inside
                        } else {
				controller.cScreen.stopTicker();
			}
			controller.showScr(Controller.CONTROL_FORM);
                        return;
                                
                } else if (oper.equals("fg")) {
 		
                	panel.setColor(CanvasScreen.FG,(String) vR.elementAt(2),(String) vR.elementAt(3),(String) vR.elementAt(4));
                
                } else if (oper.equals("bg")) {
 		
                	panel.setColor(CanvasScreen.BG,(String) vR.elementAt(2),(String) vR.elementAt(3),(String) vR.elementAt(4));
                
                } else if (oper.equals("font")) {
                        
 			panel.setFont(2, vR);
                        
                } else if (oper.equals("select")) {
			
			try { 
                        	int i = Integer.parseInt((String) vR.elementAt(2))-1;
                        	if (i<0 && i>=panel.data.size()) {
                        		return;
                        	}
                        	panel.idxSelect = i;
                        	panel.idxStart = (i>2) ? i-2 : i;
			} catch(Exception z) { }
                
             	} else if (oper.equals("add") || oper.equals("replace")) {

        		String title = (String) vR.elementAt(2);
                        
                	if (oper.equals("replace")) {
                        	cleanUp();
                        }
               		if (!title.equals("SAME")) {
				caption = title;
             		}
                        
                        boolean addVisible = addToList(vR, 3, (stage == CanvasConsumer.FULL), useIcons);
                       
                    	if (controller.cScreen.currentScreen == this && !addVisible){
 				return;	
                        }
                } else if (!oper.equals("show")) {

	          	return;	// Seems command was improperly formed
  		
                }
                controller.showScr(Controller.LIST_FORM);
 	}
        
	public void setData(Vector vR, int stage) {
         	//controller.showAlert("setData() " +stage);  
        	if (stage == CanvasConsumer.FULL || stage == CanvasConsumer.FIRST) {
                
                	//System.out.println("ListForm.setData FULL or FIRST "+stage);
                	useIconsStream = false;
                	if (((Integer) vR.elementAt(0)).intValue() == ARProtocol.CMD_ICONLIST) {
                		useIconsStream = true;
                	}
                         
                        processData(vR, useIconsStream, stage);
                        
                } else  if (stage == CanvasConsumer.INTERMED || stage == CanvasConsumer.LAST) {
                
        		//System.out.println("ListForm.setData INTERMED or LAST");
               		boolean addVisible = addToList(vR, 0, (stage == CanvasConsumer.LAST), useIconsStream);
        
                	if (controller.cScreen.currentScreen == this && !addVisible){
	                	return;         	

                	}
        
                	controller.showScr(Controller.LIST_FORM);
                }
        }

	public boolean addToList(Vector vR, int start, boolean fullCmd, boolean useIcons) {
        
                //controller.showAlert("addToList() " +start);  
        	boolean addVisible = (panel.data.size() < 2 || panel.data.size() <= panel.idxEnd+1);
                
                int end = vR.size();
                if (!fullCmd) {
                	end -= 1;
                }
                
                for (int idx=start;idx<end;idx++) {
                	
                	String item = (String) vR.elementAt(idx);
                        if (start == 0 && idx == 0) {
                        	item = bufferedItem + item;
                        	//System.out.println("ListForm.addToList restore from buffer " + item);
                                bufferedItem = "";
                        }
                	if (!item.equals("") && ! (item.length() == 1 && item.charAt(0) == '\n')) {
                        	if (useIcons) {
                                	panel.addWithIcon(item); 
                                } else {
                			panel.add(item); 
                                }        	
                	}
                }
                
                if (!fullCmd) {
                        bufferedItem = (String) vR.elementAt(end);
                }
		
		if (useIcons && fullCmd) {
			runIconCaching();
                }

                return addVisible;
	}

	public void runIconCaching() {         	
		// Run new thread which will load icons to cache, overwise repaint speed is too slow 
		// (retrieving an icon from RMS take a lot of time)
		//System.out.println("ListForm.runIconCaching");
		
		controller.display.callSerially(new Runnable() { 
			public void run() { 
				//System.out.println("ListForm.runIconCaching.run()");
				try { 
					int sz = panel.icons.size();
					for (int i=0;i<sz;i++) {
					        //System.out.println("ListForm.runIconCaching.run() "+(String) panel.icons.elementAt(i));
						controller.cScreen.loadCachedImage((String) panel.icons.elementAt(i), 16, true);
					}
				} catch(Exception z) { }
			}
		});
		
        }
        
	public void updateMenu() {         	
                //controller.menuCommands.addElement(s);
                controller.menuCommands.addElement(b);
        }

	public void cleanUp() {
        	//System.out.println("cleanUp");
        	if (panel != null) {
        		panel.removeAll();
                }
	}
 	
	public void commandAction(Command cmd, Displayable display) {
        	if (panel.data.size() > 0) {
			controller.protocol.queueCommand(cmd.getLabel() + 
                                                        "(" + String.valueOf(panel.idxSelect+1) + 
                                                        "," + panel.data.elementAt(panel.idxSelect) + ")");
                } else if (cmd.getLabel().equals("Back")) {
			controller.protocol.queueCommand(cmd.getLabel() + "(0,)");
		}
	}
        
	public void keyPressed(int keyCode) {
        	//System.out.println("ListForm.keyPressed");
		int gAction;
                try {
 			gAction = controller.cScreen.getGameAction(keyCode);
  	    	} catch (Exception e) { 
			return;
           	}
                
 		boolean redraw = false;
                if (gAction == Canvas.UP) {
                	redraw = panel.stepUp(true);
                } else if (gAction == Canvas.DOWN) {
                 	redraw = panel.stepDown(true);
                } else if ((gAction == Canvas.FIRE || 
                            keyCode == CanvasScreen.KEY_SEND) && 
                           controller.menuCommands.size() > 1) {
                 	controller.protocol.queueCommand(((Command)controller.menuCommands.elementAt(1)).getLabel() + 
                                                        "(" + String.valueOf(panel.idxSelect+1) + 
                                                        "," + (String) panel.data.elementAt(panel.idxSelect) + ")");
                }
                
                if (keyCode == Canvas.KEY_NUM0) {  // force to stop ticker
                	panel.switchUseTicker();
                        redraw = true;
                } else if (keyCode == Canvas.KEY_NUM1) {
                	panel.goTo(CanvasPanel.START);
                        redraw = true;
                } else if (keyCode == Canvas.KEY_NUM7) {
                	panel.goTo(CanvasPanel.END);
                        redraw = true;
                } else if (keyCode == Canvas.KEY_NUM3) {
                	panel.goTo(CanvasPanel.PGUP);
                        redraw = true;
                } else if (keyCode == Canvas.KEY_NUM9) {
                	panel.goTo(CanvasPanel.PGDN);
                        redraw = true;
		}
                
                if (redraw) {
			drawScreen();
                }
  	}

	public void keyReleased(int keyCode) { 
        	panel.keyReleased();
        }
        
	public void pointerPressed(int x, int y) { 
                panel.goToPointer(y);
                drawScreen();
        }
        
	public void pointerReleased(int x, int y) { }
        
	public void pointerDragged(int x, int y) { 
                if (panel.dragPointer(y)) {
			drawScreen();
                }
        }
        
	public void drawScreen() {
 		//controller.showAlert("drawScreen");		
		synchronized (controller.cScreen.drawMutex) {
			// draw bg
        		controller.cScreen.gr.setClip(0, 0, controller.cScreen.CW, controller.cScreen.CH);
        		controller.cScreen.gr.setColor(panel.bg[0], panel.bg[1], panel.bg[2]);
			controller.cScreen.gr.fillRect(0, 0, controller.cScreen.CW, controller.cScreen.CH);
		        
			if (controller.cScreen.isFullscreen) {
                        	int FH = panel.cpFont.getHeight();
                        	
                		// draw caption
        			controller.cScreen.gr.setFont(panel.cpFont);
	        		controller.cScreen.gr.setColor(panel.fg[0], panel.fg[1], panel.fg[2]);
	        		controller.cScreen.gr.drawString(caption, controller.cScreen.CW>>1, 1, Graphics.TOP|Graphics.HCENTER);
                		controller.cScreen.gr.drawLine(0,FH+2,controller.cScreen.CW,FH+2);
			}
                	panel.draw();
                	controller.cScreen.flushGraphics(); 
		}
		//controller.cScreen.resumeTicker();
        }      
            
	public void fullscreenBkgr() {
        	controller.cScreen.flushFullScreen(panel.bg[0], panel.bg[1], panel.bg[2]);
	}


        public void showScreen() {
		//System.out.println("ListForm.showScreen");
                
                int FH = panel.cpFont.getHeight();
                
                controller.cScreen.setTVisuals(panel.cpFont, 80, panel.bg[0], panel.bg[1], panel.bg[2], panel.fg[0], panel.fg[1], panel.fg[2], Graphics.LEFT, true);
		
		int xStart = (controller.cScreen.isFullscreen ? FH+3 : 0);
                panel.setSize(2,controller.cScreen.CW-4,xStart,controller.cScreen.CH-xStart);
		
		drawScreen();
	}
	
        public void hideScreen() {}
}
