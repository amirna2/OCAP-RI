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
package org.cablelabs.xlet.hn.HNIAPP.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;

import org.cablelabs.xlet.hn.HNIAPP.controller.HomeNetController;
import org.cablelabs.xlet.hn.HNIAPP.model.DevicePageBean;
import org.cablelabs.xlet.hn.HNIAPP.util.HNConstants;
import org.cablelabs.xlet.hn.HNIAPP.util.ImageConstants;
import org.cablelabs.xlet.hn.HNIAPP.util.ImageUtil;
import org.cablelabs.xlet.hn.HNIAPP.util.logging.HomeNetLogging;
import org.dvb.event.UserEvent;
import org.havi.ui.HState;
import org.havi.ui.HStaticIcon;
import org.havi.ui.HStaticText;
import org.havi.ui.HVisible;
import org.ocap.ui.event.OCRcEvent;
/**
 * @author Parthiban Balasubramanian, Cognizant Technology Solutions
 * 
 */
public class Homepage extends Page
{
    private HomeNetLogging hnLogger = HomeNetLogging.getInstance();

	static Homepage s_HomePage;
	public int buttonIndex = 0;
	
	HStaticIcon loadingImageIcon = new HStaticIcon();
	
    public String[] buttonNormalImagePath = { ImageConstants.DEVICEBUTTON_IMG, ImageConstants.FINDINGBUTTON_IMG,
            ImageConstants.FINDINGBUTTON_IMG };

    public String[] buttonHighImagePath = { ImageConstants.DEVICEBUTTONHIGH_IMG, ImageConstants.FINDINGBUTTONHIGH_IMG,
            ImageConstants.FINDINGBUTTONHIGH_IMG };

    public String[] buttonTextList = { "Find all network MediaServers", "Generate UPnP Messages",
            "Display all devices in network" };

	HStaticText[] buttonListText = new HStaticText[5];
	HStaticIcon[] buttonListIcon = new HStaticIcon[5];

	int buttonStartYAxis = 110;
	int yAxisIncrement = 70;
	int iconStartYAxis = 110;
	
	HStaticText headingTextLabel = new HStaticText();
	HStaticIcon headingIcon = new HStaticIcon(ImageUtil.getInstance().loadFileImage(ImageConstants.TITLE_IMG, this));
	
	private Image backgrndImage = ImageUtil.getInstance().loadFileImage(ImageConstants.BACKGRND_IMG, this);

    private Homepage()
    {

	}

    public static Homepage getInstance()
    {
        if (s_HomePage == null)
        {
			s_HomePage = new Homepage();
		}
		return s_HomePage;
	}

    public HomeNetController getController()
    {
		return HomeNetController.getInstance();
	}
	
    public void processUserEvent(UserEvent event)
    {
        switch (event.getCode())
        {
		case OCRcEvent.VK_UP:
                hnLogger.homeNetLogger("Inside Up");
			highlightPrev();
			break;
		case OCRcEvent.VK_DOWN:
                hnLogger.homeNetLogger("Inside down");
			highlightNext();
			break;
		case OCRcEvent.VK_RIGHT:
                hnLogger.homeNetLogger("Inside right");
			break;
		case OCRcEvent.VK_LEFT:
                hnLogger.homeNetLogger("Inside left");
			break;
		case OCRcEvent.VK_ENTER:
                hnLogger.homeNetLogger("Inside Enter");
			loadingImageIcon.setVisible(true);
			if(buttonIndex==0)
			{
                    getController().displayNewScreen(HNConstants.DEVICEPAGE_NAME,
                            new DevicePageBean(1, HNConstants.DEVICE_TYPE_MEDIASERVERS));
                }
                else if (buttonIndex == 1)
                {
				getController().displayNewScreen(HNConstants.MESSAGETYPELIST_NAME, null);
                }
                else if (buttonIndex == 2)
                {
                    getController().displayNewScreen(HNConstants.DEVICEPAGE_NAME,
                            new DevicePageBean(1, HNConstants.DEVICE_TYPE_ALL));
			}
			loadingImageIcon.setVisible(false);
			break;
		}
	}

    private void highlightPrev()
    {
        hnLogger.homeNetLogger("Button index in prev before :" + buttonIndex);
        if (buttonIndex > 0)
        {
            buttonListIcon[buttonIndex].setGraphicContent(ImageUtil.getInstance().loadFileImage(
                    buttonNormalImagePath[buttonIndex], this), HState.NORMAL_STATE);
			buttonIndex = buttonIndex -1;
            hnLogger.homeNetLogger("button high in prev" + buttonHighImagePath[buttonIndex]);
            buttonListIcon[buttonIndex].setGraphicContent(ImageUtil.getInstance().loadFileImage(
                    buttonHighImagePath[buttonIndex], this), HState.NORMAL_STATE);
            hnLogger.homeNetLogger("Button index in prev after :" + buttonIndex);
		}
	}

    private void highlightNext()
    {
        hnLogger.homeNetLogger("Button index in next prev :" + buttonIndex);
        if (buttonIndex < (buttonTextList.length - 1))
        {
            buttonListIcon[buttonIndex].setGraphicContent(ImageUtil.getInstance().loadFileImage(
                    buttonNormalImagePath[buttonIndex], this), HState.NORMAL_STATE);
			buttonIndex = buttonIndex +1;
            hnLogger.homeNetLogger("button high " + buttonHighImagePath[buttonIndex]);
            buttonListIcon[buttonIndex].setGraphicContent(ImageUtil.getInstance().loadFileImage(
                    buttonHighImagePath[buttonIndex], this), HState.NORMAL_STATE);
            hnLogger.homeNetLogger("Button index in next after :" + buttonIndex);
		}
		
	}

    public void init(Object parameters)
    {
		setBounds(20, 20, 620, 460);
		initializeComponents();
	}

    private void initializeComponents()
    {
        hnLogger.homeNetLogger("inside initiallasdfasdafds");
        loadingImageIcon.setGraphicContent(ImageUtil.getInstance().loadFileImage(ImageConstants.LOADING_IMAGE, this),
                HState.NORMAL_STATE);
		loadingImageIcon.setHorizontalAlignment(HStaticIcon.HALIGN_CENTER);
		loadingImageIcon.setVerticalAlignment(HStaticIcon.VALIGN_CENTER);
		loadingImageIcon.setBounds(getX(),getY(),getWidth() - 50, getHeight() - 60);
		loadingImageIcon.setBackgroundMode(HVisible.BACKGROUND_FILL);
		loadingImageIcon.setVisible(false);
		add(loadingImageIcon);
		
		headingIcon.setHorizontalAlignment(HStaticIcon.HALIGN_CENTER);
		headingIcon.setVerticalAlignment(HStaticIcon.VALIGN_CENTER);
		headingIcon.setBordersEnabled(false);
		headingIcon.setBounds((getWidth() / 2) - 125, getY() + 14, 250, 35);

		headingTextLabel = new HStaticText("Home Network Insight");
		headingTextLabel.setFont(new Font("tiresias", Font.BOLD, 14));
		headingTextLabel.setForeground(Color.black);
		headingTextLabel.setHorizontalAlignment(HStaticText.HALIGN_CENTER);
		headingTextLabel.setVerticalAlignment(HStaticText.VALIGN_CENTER);
		headingTextLabel.setBounds((getWidth() / 2) - 100, getY() + 10, 200, 35);
		headingTextLabel.setVisible(true);

		loadButtonImages();
		add(headingTextLabel);
		add(headingIcon);

        for (int i = 0; i < buttonTextList.length; i++)
        {
			add(buttonListText[i]);
			add(buttonListIcon[i]);
		}

		repaint();
	}

    void loadButtonImages()
        {
		
        for (int i = 0; i < buttonTextList.length; i++)
        {
			
			buttonListText[i] = new HStaticText(buttonTextList[i]);
			buttonListText[i].setFont(new Font("tiresias", Font.BOLD, 16));
			buttonListText[i].setForeground(Color.white);
			buttonListText[i].setHorizontalAlignment(HStaticText.HALIGN_LEFT);
			buttonListText[i].setVerticalAlignment(HStaticText.VALIGN_CENTER);
			buttonListText[i].setBounds((getWidth() / 2) - 100, getY() + buttonStartYAxis, 290, 50);
			buttonListText[i].setVisible(true);
			buttonStartYAxis = buttonStartYAxis+yAxisIncrement;
		}

        for (int i = 0; i < buttonTextList.length; i++)
        {
			
			buttonListIcon[i] = new HStaticIcon();
			if(i==0)
                buttonListIcon[i].setGraphicContent(
                        ImageUtil.getInstance().loadFileImage(buttonHighImagePath[i], this), HState.NORMAL_STATE);
			else
                buttonListIcon[i].setGraphicContent(ImageUtil.getInstance().loadFileImage(buttonNormalImagePath[i],
                        this), HState.NORMAL_STATE);
			buttonListIcon[i].setHorizontalAlignment(HStaticIcon.HALIGN_CENTER);
			buttonListIcon[i].setVerticalAlignment(HStaticIcon.VALIGN_CENTER);
			buttonListIcon[i].setBordersEnabled(false);
			buttonListIcon[i].setBounds((getWidth() / 2) - 160, getY() + iconStartYAxis, 320,50);
			iconStartYAxis = iconStartYAxis+yAxisIncrement;
		}
		
	}

    public void paint(Graphics g)
    {
        g.drawImage(backgrndImage, getX(), getY(), getWidth() - 50, getHeight() - 60, this);
		g.setColor(Color.yellow);
		g.drawRect(getX(), getY(), getWidth() - 50, getHeight() - 60);
		
		super.paint(g);
	}

    public void reset()
    {
		buttonIndex =0;
		buttonStartYAxis = 110;
		yAxisIncrement = 70;
		iconStartYAxis = 110;
	}

    public void destroy()
                    {
               
        reset();
        removeAll();
    }
}
