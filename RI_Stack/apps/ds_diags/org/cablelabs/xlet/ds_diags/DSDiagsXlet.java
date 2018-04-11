// COPYRIGHT_BEGIN
//  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
//  
//  Copyright (C) 2008-2013, Cable Television Laboratories, Inc. 
//  
//  This software is available under multiple licenses: 
//  
//  (1) BSD 2-clause 
//   Redistribution and use in source and binary forms, with or without modification, are
//   permitted provided that the following conditions are met:
//        ·Redistributions of source code must retain the above copyright notice, this list 
//             of conditions and the following disclaimer.
//        ·Redistributions in binary form must reproduce the above copyright notice, this list of conditions 
//             and the following disclaimer in the documentation and/or other materials provided with the 
//             distribution.
//   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
//   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED 
//   TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A 
//   PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
//   HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
//   SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
//   LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
//   DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
//   THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
//   (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF 
//   THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//  
//  (2) GPL Version 2
//   This program is free software; you can redistribute it and/or modify
//   it under the terms of the GNU General Public License as published by
//   the Free Software Foundation, version 2. This program is distributed
//   in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
//   even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
//   PURPOSE. See the GNU General Public License for more details.
//  
//   You should have received a copy of the GNU General Public License along
//   with this program.If not, see<http:www.gnu.org/licenses/>.
//  
//  (3)CableLabs License
//   If you or the company you represent has a separate agreement with CableLabs
//   concerning the use of this code, your rights and obligations with respect
//   to this code shall be as set forth therein. No license is granted hereunder
//   for any other purpose.
//  
//   Please contact CableLabs if you need additional information or 
//   have any questions.
//  
//       CableLabs
//       858 Coal Creek Cir
//       Louisville, CO 80027-9750
//       303 661-9100
// COPYRIGHT_END

// Declare package.
package org.cablelabs.xlet.ds_diags;


import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Enumeration;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.dvb.media.VideoFormatControl;
import org.havi.ui.HBackgroundConfiguration;
import org.havi.ui.HGraphicsConfiguration;
import org.havi.ui.HBackgroundDevice;
import org.havi.ui.HGraphicsDevice;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HSceneTemplate;
import org.havi.ui.HScreen;
import org.havi.ui.HVideoConfiguration;
import org.havi.ui.HVideoDevice;
import org.havi.ui.event.HRcEvent;

import org.ocap.ui.event.OCRcEvent;

import org.ocap.hardware.Host;
import org.ocap.hardware.VideoOutputPort;
import org.ocap.hardware.device.FeatureNotSupportedException;
import org.ocap.hardware.device.HostSettings;
import org.ocap.hardware.device.VideoResolution;
import org.ocap.hardware.device.VideoOutputSettings;
import org.ocap.hardware.device.VideoOutputConfiguration;
import org.ocap.hardware.device.FixedVideoOutputConfiguration;
import org.ocap.hardware.device.DynamicVideoOutputConfiguration;

import java.util.*;


/*
 *  class DSDiags
 *
 * To interrigate Device settings system
 *
 */
//=========================================================================
  public class DSDiagsXlet extends Component implements Xlet, KeyListener
//=========================================================================
{
    private static final long serialVersionUID = 1;
    
    private static final Font FONT = new Font("sansserif", Font.PLAIN, 14);
    private HScene m_scene;
    
    // hard code fixed size for now, may try to adapt to attached display in the future
    private static final int m_hc_disp_w = 640;
    private static final int m_hc_disp_h = 480;

    private static final int HOST_TEXT_X = 10;
    private static final int VOP_TEXT_X = m_hc_disp_w/2 + 50;
    private static final int HOST_TEXT_START_Y = 30;
    private static final int VOP_TEXT_START_Y = 30;
    private static final int REPAINT_MSG_START_X = 10;
    private static final int REPAINT_MSG_START_Y = 500;
    private static java.awt.Color m_lastRepaintClr = Color.green;
    
    // for text management
    private int m_line_height = -1;
    private int m_host_txt_cur_x = -1;
    private int m_host_txt_cur_y = -1;
    
    // Stuff on the host that does not change
    Host m_host = null;
    HostSettings m_host_settings = null; // itf implemented by Host
    
    // Array of output port types
    ArrayList m_VOP_type_names = new ArrayList();
    ArrayList m_tabs = new ArrayList();

    // VOP state
    int m_num_VOPS = -1;
    int m_main_VOP_num = -1;
    int m_last_main_VOP_num = -2;
    
  //-------------------------------------------------------------------------
    public void initXlet(XletContext c) throws XletStateChangeException
  //-------------------------------------------------------------------------
    {
        try
        {
            // create the scene
            m_scene = HSceneFactory.getInstance().getBestScene(new HSceneTemplate());

            // We extend component, so add to the scene
            m_scene.add(this);

            // Set component bounds
            setBounds(0, 0, m_hc_disp_w, m_hc_disp_h);

            // Set font used for text
            setFont(FONT);

            // Register a key listener
            m_scene.addKeyListener(this);          

            // Get the host stuff 
            if ( m_host == null )
            {
            	m_host = Host.getInstance();
                m_host_settings = (HostSettings)m_host;
                m_num_VOPS = m_EnumCollectionLen(m_host.getVideoOutputPorts());
                
                m_host.setPowerMode(Host.FULL_POWER);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new XletStateChangeException(e.getMessage());
        }

        // set up the VOP type strings
        m_VOP_type_names.add(VideoOutputPort.AV_OUTPUT_PORT_TYPE_RF,              "RF CH 3/4");
        m_VOP_type_names.add(VideoOutputPort.AV_OUTPUT_PORT_TYPE_BB,              "RCA/BBand");
        m_VOP_type_names.add(VideoOutputPort.AV_OUTPUT_PORT_TYPE_SVIDEO,          "S-Video");
        m_VOP_type_names.add(VideoOutputPort.AV_OUTPUT_PORT_TYPE_1394,            "1394");
        m_VOP_type_names.add(VideoOutputPort.AV_OUTPUT_PORT_TYPE_DVI,             "DVI");
        m_VOP_type_names.add(VideoOutputPort.AV_OUTPUT_PORT_TYPE_COMPONENT_VIDEO, "Component");
        m_VOP_type_names.add(VideoOutputPort.AV_OUTPUT_PORT_TYPE_HDMI,            "HDMI");
        m_VOP_type_names.add(VideoOutputPort.AV_OUTPUT_PORT_TYPE_INTERNAL,        "Internal");

        // set up the tab strings
        m_tabs.add(0, "   ");
        m_tabs.add(1, "     ");
        m_tabs.add(2, "      ");
        m_tabs.add(3, "       ");
        m_tabs.add(4, "        ");
        m_tabs.add(5, "         ");
        m_tabs.add(6, "          ");
        m_tabs.add(7, "           ");
        m_tabs.add(8, "            ");
        m_tabs.add(9, "             ");
        m_tabs.add(10,"              ");
        
        message("----- DSDiagsXlet Started --------------------------------------------");
    }

    /**
     * startXlet
     * 
     * Called by the system when the app is suppose to actually start.
     * 
     */
  //-------------------------------------------------------------------------
    public void startXlet() throws XletStateChangeException
  //-------------------------------------------------------------------------
    {
        try
        {
            // Make the scene visible
            m_scene.setVisible(true);

            // Make this scene gain focus and paint
            m_scene.requestFocus();
            repaint();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new XletStateChangeException(e.getMessage());
        }
    }

    /**
     * pauseXlet
     * 
     * Called by the system when the user has performed an action requiring this
     * application to pause for another ,
     */
  //-------------------------------------------------------------------------
    public void pauseXlet()
  //-------------------------------------------------------------------------
    {
        // Make the scene invisible since we're pausing
        m_scene.setVisible(false);
    }

    /**
     * destroyXlet
     * 
     * Called by the system when the application needs to exit and clean up.
     * 
     */
  //-------------------------------------------------------------------------
    public void destroyXlet(boolean arg0) throws XletStateChangeException
  //-------------------------------------------------------------------------
    {
        try
        {
            // Hide screen
            m_scene.setVisible(false);

            // Remove key listener
            m_scene.removeKeyListener(this);

            // Remove everything else
            m_scene.removeAll();

            // Give up resources
            m_scene.dispose();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new XletStateChangeException(e.getMessage());
        }
    }

  //-------------------------------------------------------------------------
    public void paint(Graphics g)
  //-------------------------------------------------------------------------
    {	                   
        // Initialize text management stuff
        if ( m_line_height == -1 )
        {
            FontMetrics fm = g.getFontMetrics(FONT);
            m_line_height = (int)((double)fm.getHeight() * 1.05);
        }

        m_host_txt_cur_x = HOST_TEXT_X;         	
        m_host_txt_cur_y = HOST_TEXT_START_Y;         	

        // Get HScrene Info
        int numScreens = (HScreen.getHScreens()).length;
        HScreen defaultScreen = HScreen.getDefaultHScreen();       

        // Get info about associated Graphics Device
        int numGraphDev = (defaultScreen.getHGraphicsDevices()).length;
        HGraphicsDevice defaultGraphicsDev = defaultScreen.getDefaultHGraphicsDevice();
        HGraphicsConfiguration defaultGraphicsCfg = defaultGraphicsDev.getCurrentConfiguration();        
        Dimension defaultGraphicsRsln = defaultGraphicsCfg.getPixelResolution();
        
        // Get info about associated Video Device
        int numVidDev = (defaultScreen.getHVideoDevices()).length;
        HVideoDevice defaultVidDev = defaultScreen.getDefaultHVideoDevice();
        HVideoConfiguration defaultVidCfg = defaultVidDev.getCurrentConfiguration();
        Dimension defaultVidRsln = defaultVidCfg.getPixelResolution();
        
        // Get info about associated Background Device
        int numBackDev = (defaultScreen.getHBackgroundDevices()).length;
        HBackgroundDevice defaultBackDev = defaultScreen.getDefaultHBackgroundDevice();       
        HBackgroundConfiguration defaultBackCfg = defaultBackDev.getCurrentConfiguration();
        Dimension defaultBackRsln = defaultBackCfg.getPixelResolution();
        
                       // Print the host ID
        g.setColor(Color.white);
        String hostID = m_host.getID();      
        m_writeLn(g, "HOSTID: " + hostID + "(" + numScreens + " HScrene[s])");

        // print info about Devices
        g.setColor(Color.cyan);
        m_writeLn(g, numBackDev  + " B-Dev(s), Dflt AR="  + defaultBackDev.getScreenAspectRatio().width  + 
                                                     ":"  + defaultBackDev.getScreenAspectRatio().height +
                                                     ", Rsln=("+ defaultBackRsln.width+"x"+defaultBackRsln.height+")");
        
        m_writeLn(g, numGraphDev + " G-Dev(s), Dflt AR=" + defaultGraphicsDev.getScreenAspectRatio().width + 
                                                     ":" + defaultGraphicsDev.getScreenAspectRatio().height +
                                                     ", Rsln=("+ defaultGraphicsRsln.width+"x"+defaultGraphicsRsln.height+")");

        
        m_writeLn(g, numVidDev   + " V-Dev(s), Dflt AR=" + defaultVidDev.getScreenAspectRatio().width + 
                                                     ":" + defaultVidDev.getScreenAspectRatio().height+
                                                     ", Rsln=("+ defaultVidRsln.width+"x"+defaultVidRsln.height+")");
        	
        
        // Get main VOP for this host
        VideoOutputPort mainVOP = m_host_settings.getMainVideoOutputPort(defaultScreen);

        int curVOPNum = 0;
        Enumeration VOPEnum = m_host.getVideoOutputPorts();
        g.setColor(Color.yellow);
		while (VOPEnum.hasMoreElements())
		{
			VideoOutputPort curVOP = (VideoOutputPort) VOPEnum.nextElement();
			curVOPNum += 1;

			// see if we have the MAIN VOP
			if ( curVOP.equals(mainVOP))
			{
				g.setColor(Color.red);
				if ( m_main_VOP_num < 0 )
				{
					m_main_VOP_num = curVOPNum; 
				}
			}
			
		    String[] VOPStrings = m_VOPSummaryStrings("VOP("+curVOPNum+"): ", curVOP);
			m_writeLn(g, "    " + VOPStrings[0]);  
			m_writeLn(g, "    " + VOPStrings[1]);  
	        
			g.setColor(Color.yellow);
		}      
    
		// now time to print out more detailed info about main VOP
		m_dispVOPConfig(g, mainVOP, m_main_VOP_num);
		
		// repaint indicator
		if ( m_lastRepaintClr == Color.green )
			{m_lastRepaintClr =  Color.magenta;}	
		else
			{m_lastRepaintClr =  Color.green;}	
		g.drawString("Repaint", REPAINT_MSG_START_X, REPAINT_MSG_START_Y);
    }

  //-------------------------------------------------------------------------
    private void m_dispVOPConfig(Graphics g, VideoOutputPort VOP, int VOPnum )
  //-------------------------------------------------------------------------
   {
	    if ( VOP == null )
	    	{return;}
    	
	    m_host_txt_cur_x = VOP_TEXT_X;         	
        m_host_txt_cur_y = VOP_TEXT_START_Y;         	

        // erase previous VOP info
	    // TODO
	    
        // Display basic VOP info
        g.setColor(Color.red);
        m_writeLn(g, "Main VOP Info (in red at left)");
    	g.setColor(Color.orange);
	
        String[] VOPStrings = m_VOPSummaryStrings("", VOP);
		m_writeLn(g, VOPStrings[0]);  
        
        // Get VOP settings and config objects
	    VideoOutputSettings VOPS = (VideoOutputSettings)VOP;
	    VideoOutputConfiguration VOPCfgList[] = VOPS.getSupportedConfigurations();
	    VideoOutputConfiguration activeVOPCfg = VOPS.getOutputConfiguration();
	    
	    // write config info
	    m_writeLn(g, VOPCfgList.length+ " Supported Configurations (active white & starred**)");
	    for ( int i = 0; i < VOPCfgList.length; i++)
	    {
	    	String cfgStr = "    ("+i+") " + VOPCfgList[i].getName();
	    	if ( VOPCfgList[i].getName() == activeVOPCfg.getName() )
	    	{
		    	g.setColor(Color.white);
	    		cfgStr += " **";
	    	}
	    	m_writeLn(g, cfgStr);
	    	
	    	// see if this is a dynamic configuration
	    	if ( VOPCfgList[i] instanceof DynamicVideoOutputConfiguration )
	    	{
	    		DynamicVideoOutputConfiguration DCfg = (DynamicVideoOutputConfiguration)VOPCfgList[i];
	    		Enumeration DCfgTable = DCfg.getInputResolutions();
	    		while (DCfgTable.hasMoreElements())
	    		{
	    			VideoResolution curInputRes = (VideoResolution) DCfgTable.nextElement();
	    			FixedVideoOutputConfiguration curOutputCfg = DCfg.getOutputResolution(curInputRes);  
	    	    	m_writeLn(g, "          ["+ curInputRes.getPixelResolution().width+  "x"+ 
	    	    			                    curInputRes.getPixelResolution().height+ "]-> ["+
	    	    			                    curOutputCfg.getVideoResolution().getPixelResolution().width+  "x"+
	    	    			                    curOutputCfg.getVideoResolution().getPixelResolution().height+ "]");
	    		}	    		
	    	}
            else
            {
	    		FixedVideoOutputConfiguration FCfg = (FixedVideoOutputConfiguration)VOPCfgList[i];
                m_writeLn(g, "          stereoscopicmode = " + FCfg.getVideoResolution().getStereoscopicMode());
            }
	    	
	    	g.setColor(Color.orange);
	    }

	    // Display information
    	g.setColor(Color.orange);
	    String dispConStr = "dispCon(n), ";
	    if (VOPS.isDisplayConnected() ) 
	    	{dispConStr="dispCon(y), ";}
	    
	    String dispARStr = "DAR(?:?)";
	    switch (VOPS.getDisplayAspectRatio())
        {
        	case VideoFormatControl.DAR_16_9: dispARStr = "DAR(16:9)"; break;
        	case VideoFormatControl.DAR_4_3: dispARStr = "DAR(4:3)"; break;
        }    
	    m_writeLn(g, "Display Info: " + dispConStr + dispARStr);
	    
	    // Print out the display attributes
        Hashtable dispAttr = VOPS.getDisplayAttributes();
	    if ( dispAttr != null )
	    {
		    Enumeration keys = dispAttr.keys();
		    while( keys.hasMoreElements() ) 
		    {
		        String key = (String)keys.nextElement();
		        Object value = dispAttr.get(key);
			    m_writeLn(g, "    " + key + " : " + value);
		    }
	    }
   }
    
  //-------------------------------------------------------------------------
    private String[] m_VOPSummaryStrings(String sPre, VideoOutputPort VOP)
  //-------------------------------------------------------------------------
    {
    	String[] VOPStrings = new String[2];
    	VOPStrings[0] = "";
    	VOPStrings[1] = "";

    	// Get VideoOutputSettings for this port
    	VideoOutputSettings VOPS = (VideoOutputSettings)VOP; 
    	
    	// Pre string
    	if ( sPre != null )
    	{
    		VOPStrings[0] += sPre;
    		VOPStrings[1] += m_tabs.get(sPre.length());
    	}
    	
    	// Type() 
    	int vopType = VOP.getType();	
		VOPStrings[0] += "T(" + m_VOP_type_names.get(vopType)+ "), ";
		
    	// Rez() 
    	Dimension vopDim = VOP.getResolution();
		VOPStrings[0] += "R("+vopDim.width+"x"+vopDim.height+"), ";
		
		// AspRat
		int aspRat = VOPS.getDisplayAspectRatio();
		if ( aspRat ==  VideoFormatControl.DAR_4_3 )
	    	{ VOPStrings[0] += "DAR(4:3), ";}
		else if ( aspRat ==  VideoFormatControl.DAR_16_9 )
			{ VOPStrings[0] += "DAR(16:9), ";}
		else
			{ VOPStrings[0] += "DAR(?:?), ";}
		
		// Stat()
		if ( VOP.status() )
	    	{ VOPStrings[0] += "S(enabled)";}
		else
    		{ VOPStrings[0] += "S(disabled)";}
		
		// #Conf()
		VOPStrings[1] += "#Cf(" + (VOPS.getSupportedConfigurations()).length+"), ";
		
		// DynCfg()
		if ( VOPS.isDynamicConfigurationSupported() )
    		{ VOPStrings[1] += "DynCf(y), " ; }
		else
			{ VOPStrings[1] += "DynCf(n), "; }
    	
		// Disp()
		if ( VOPS.isDisplayConnected())
			{ VOPStrings[1] += "DispCn(y), " ; }
		else
			{ VOPStrings[1] += "DispCn(n), "; }
		
		// ContProt()
		if ( VOPS.isDisplayConnected())
			{ VOPStrings[1] += "ContProt(y)" ; }
		else
			{ VOPStrings[1] += "ContProt(n)"; }
                
        return VOPStrings;		
    }
    
  //-------------------------------------------------------------------------
    public void keyTyped(KeyEvent e)
  //-------------------------------------------------------------------------
    {
        // Do nothing
    }

  //-------------------------------------------------------------------------
    public void keyReleased(KeyEvent e)
  //-------------------------------------------------------------------------
    {
        // Do nothing
    }

  //-------------------------------------------------------------------------
    public void keyPressed(KeyEvent e)
  //-------------------------------------------------------------------------
    {
        message("~~~~ DSDiagsXlet recieved awt event " + e.getKeyCode());

        // Handle the key event
        int candidateVOPCfgIdx = -1;
        boolean repaint = true;
        switch (e.getKeyCode())
        {
        	// 0-9 indicates which VOP config to make active for current main VOP
        	case OCRcEvent.VK_0: candidateVOPCfgIdx = 0; break;
        	case OCRcEvent.VK_1: candidateVOPCfgIdx = 1; break;
            case OCRcEvent.VK_2: candidateVOPCfgIdx = 2; break;
            case OCRcEvent.VK_3: candidateVOPCfgIdx = 3; break;
            case OCRcEvent.VK_4: candidateVOPCfgIdx = 4; break;
            case OCRcEvent.VK_5: candidateVOPCfgIdx = 5; break;
            case OCRcEvent.VK_6: candidateVOPCfgIdx = 6; break;
            case OCRcEvent.VK_7: candidateVOPCfgIdx = 7; break;
            case OCRcEvent.VK_8: candidateVOPCfgIdx = 8; break;
            case OCRcEvent.VK_9: candidateVOPCfgIdx = 9; break;
        
	        case OCRcEvent.VK_DOWN: 
	            if ( m_main_VOP_num < m_num_VOPS )  
	            {
                    m_main_VOP_num++;
                    System.out.println("DSDIAGS: Changed VOP " + m_main_VOP_num);
                }
	            break;

	        case OCRcEvent.VK_UP: 
	            if ( m_main_VOP_num > 1 )  
		        {
                    m_main_VOP_num--;
                    System.out.println("DSDIAGS: Changed VOP " + m_main_VOP_num);
                }
		        break;
            
	        case HRcEvent.VK_POWER:
	        	// sleep before repaint to make sure that all VOP enable/disable states have been set first
				try { Thread.sleep(750); } 
				catch (InterruptedException e1) {} 
		    	break;
		        
	        case OCRcEvent.VK_ENTER:
                // switch enabled state of port
    	        VideoOutputPort mainVOP = m_getVOPFromNum(m_main_VOP_num);
    	        VideoOutputSettings mainVOPS = (VideoOutputSettings)mainVOP; 
                if (mainVOP.status())
                {
                    System.out.println("DSDIAGS: Disabling video output port");
                    mainVOP.disable();
                }
                else
                {
                    System.out.println("DSDIAGS: Enabling video output port");
                    mainVOP.enable();
                }
		    	break;
		        
            default:
                repaint = true;
                break;
        }       

    	VideoOutputPort mainVOP = m_getVOPFromNum(m_main_VOP_num);
    	VideoOutputSettings mainVOPS = (VideoOutputSettings)mainVOP; 
    	VideoOutputConfiguration mainVOPCfgList[] = mainVOPS.getSupportedConfigurations();

        // set main VOP
        if ( m_main_VOP_num > 0 && m_main_VOP_num != m_last_main_VOP_num)
        {
	        VideoOutputPort VOPToSetToMain = m_getVOPFromNum(m_main_VOP_num);
	        try 
	            { 
                HScreen defaultScreen = HScreen.getDefaultHScreen();       
                m_host_settings.setMainVideoOutputPort(defaultScreen, VOPToSetToMain); 
                m_last_main_VOP_num = m_main_VOP_num;
                }
			catch (FeatureNotSupportedException ee) 
			    { message("***ERROR: HostSettings:setMainVideoOutputPort() not supported"); }
        }	        

    	// VOP Config change
    	if ( candidateVOPCfgIdx >= 0 && candidateVOPCfgIdx < mainVOPCfgList.length )
    	{
    		VideoOutputConfiguration activeVOPCfg = mainVOPCfgList[candidateVOPCfgIdx];            		
            System.out.println("DSDIAGS: Setting OutputConfiguration to " + activeVOPCfg);
	        try 
            	{ mainVOPS.setOutputConfiguration(activeVOPCfg); }
	        catch (FeatureNotSupportedException exp) 
		    	{ message("***ERROR: VideoOutputSettings:setOutputConfiguration() not supported"); }
    	}
        
        // We have to repaint the screen to handle any changes
        if (repaint)
        {
            repaint();
        }
    }

  //-------------------------------------------------------------------------
    protected void message(String s)
  //-------------------------------------------------------------------------
    {
        System.out.println(s);
    }

  //-------------------------------------------------------------------------
    private void m_writeLn(Graphics g, String s)
  //-------------------------------------------------------------------------
    {
    	g.drawString(s, m_host_txt_cur_x, m_host_txt_cur_y);
        m_host_txt_cur_y += m_line_height;
    }
    

  //-------------------------------------------------------------------------
    private VideoOutputPort m_getVOPFromNum(int VOPnum)
  //-------------------------------------------------------------------------
    {
    if ( VOPnum <= 0 )
		{return null;}

	int curVOPNum = 1;
	VideoOutputPort dispVOP = null;
	Enumeration VOPEnum = m_host.getVideoOutputPorts();
	while (VOPEnum.hasMoreElements())
	{
		VideoOutputPort curVOP = (VideoOutputPort) VOPEnum.nextElement();
		if (curVOPNum == VOPnum )
		{
			dispVOP = curVOP;
			break;
		}
	    curVOPNum += 1;
	}
	
	return dispVOP;
    }
    
  //-------------------------------------------------------------------------
    private int m_EnumCollectionLen(Enumeration enumToCheck)
  //-------------------------------------------------------------------------
   {
        int enumCnt = 0;
        while (enumToCheck.hasMoreElements())
        {
    		enumCnt += 1;
    		enumToCheck.nextElement();
    	}
    return enumCnt;
   }
}
