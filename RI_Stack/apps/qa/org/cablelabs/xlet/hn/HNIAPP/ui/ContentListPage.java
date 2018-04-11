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
import java.awt.Rectangle;
import java.util.ArrayList;

import org.cablelabs.xlet.hn.HNIAPP.controller.HomeNetController;
import org.cablelabs.xlet.hn.HNIAPP.model.ContentItemRequestBean;
import org.cablelabs.xlet.hn.HNIAPP.model.DevicePageBean;
import org.cablelabs.xlet.hn.HNIAPP.util.HNConstants;
import org.cablelabs.xlet.hn.HNIAPP.util.HTextImpl;
import org.cablelabs.xlet.hn.HNIAPP.util.ImageUtil;
import org.cablelabs.xlet.hn.HNIAPP.util.homenetworking.HNUtil;
import org.cablelabs.xlet.hn.HNIAPP.util.homenetworking.MediaServer;
import org.cablelabs.xlet.hn.HNIAPP.util.logging.HomeNetLogging;
import org.dvb.event.UserEvent;
import org.havi.ui.HState;
import org.havi.ui.HStaticIcon;
import org.havi.ui.HStaticText;
import org.ocap.hn.content.ContentContainer;
import org.ocap.ui.event.OCRcEvent;
/**
 * @author Parthiban Balasubramanian, Cognizant Technology Solutions
 * 
 */
public class ContentListPage extends Page
{
    private HomeNetLogging hnLogger = HomeNetLogging.getInstance();

	public static ContentListPage contentPage= null;
	public static final String BACKGRND_IMG = "/org/cablelabs/xlet/hn/HNIAPP/etc/images/backgrnd.jpg";
	public static final String DEVICE_IMG = "/org/cablelabs/xlet/hn/HNIAPP/etc/images/folder_blue.png";
	public static final String TITLE_IMG = "/org/cablelabs/xlet/hn/HNIAPP/etc/images/bannerBackgrnd.png";
	public int highButtonIndex = 0;
	public boolean noContentAlert = false;
	public int rowNumber =0;
	private int startIndex = 0;
	private boolean endOfContent = false;
	
	public MediaServer currentMediaServer = null;
	public boolean reloadScreen = false;

    private final Image loadingImg = ImageUtil.getInstance().loadFileImage(
            "/org/cablelabs/xlet/hn/HNIAPP/etc/images/ajax-loader.gif", this);

	private final Image backgrndImg = ImageUtil.getInstance().loadFileImage(BACKGRND_IMG, this);
	private final Image titleImg = ImageUtil.getInstance().loadFileImage(TITLE_IMG, this);
	private final Image deviceImg = ImageUtil.getInstance().loadFileImage(DEVICE_IMG, this);
	HStaticIcon headingIcon = new HStaticIcon(titleImg);
	HStaticText headingTextLabel = new HStaticText();
	
	HTextImpl[] ContentTextLabel = null;
	HStaticIcon[] ContentListIcon = null;
	public static ArrayList contentListArray = new ArrayList(); 
	
    public static ContentListPage getInstance()
    {
	if(contentPage==null)
		contentPage = new ContentListPage();
		return contentPage;
	} 
	
    public void init(Object parameters)
    {

		setBounds(20, 20, 620, 460);
        hnLogger.homeNetLogger("inside content list page...");
		currentMediaServer = (MediaServer)parameters;
		// Retrieving first 24 albums per page.
		contentListArray = HNUtil.getInstance().retrieveAlbums(currentMediaServer,startIndex,24);
        if (contentListArray != null)
        {
            hnLogger.homeNetLogger("Device list :" + contentListArray);
            if (contentListArray.size() == 0)
            {
				endOfContent = true;
				noContentAlert = true;
            }
            else
            {
                if (contentListArray.size() != 24)
                {
					endOfContent = true;
				}
				ContentListIcon = new HStaticIcon[contentListArray.size()];
				ContentTextLabel = new HTextImpl[contentListArray.size()];
				initializeScreenComponents(contentListArray);
			}
		}
	}

    private void highlightNext()
    {
        if ((highButtonIndex + 1) < contentListArray.size())
        {
			highButtonIndex++;
			repaint();
        }
        else
        {
            hnLogger.homeNetLogger("In else part of the next");
		}
	}

    private void highlightPrev()
    {
        if ((highButtonIndex - 1) >= 0)
        {
			highButtonIndex--;
			repaint();
        }
        else
        {
            hnLogger.homeNetLogger("In else part of the prev");
		}
	}

    private void highlightDown()
    {
        if (rowNumber != 3)
        {
			if(!((highButtonIndex+6)>= contentListArray.size()))
				highButtonIndex = highButtonIndex+6;
			else
				highButtonIndex = highButtonIndex +(contentListArray.size() - highButtonIndex -1);
			rowNumber++;
	}
		repaint();
	}

    private void highlightUp()
    {

        if (rowNumber != 0)
        {
            if (!((highButtonIndex - 6) < 0))
            {
				highButtonIndex = highButtonIndex-6;
            }
            else
            {
				highButtonIndex = 0;
			}
			rowNumber--;
		}
		repaint();
	}
	
    private void initializeScreenComponents(ArrayList contentListArray)
    {
        hnLogger.homeNetLogger("inside content page initialize");
        // init the x,y , width and height and position all the device icons in
        // the same row
			int deviceStartingX = getX()+35;
			int tempX = deviceStartingX;
			int deviceStartingY = getY()+80;
			int tempY = deviceStartingY;
			int deviceStartingW = 60;
			int deviceStartingH = 36;
			// have to check for device list
        for (int i = 0; i < contentListArray.size(); i++)
        {
            hnLogger.homeNetLogger("THe content list is ..name . "
                    + ((ContentContainer) contentListArray.get(i)).getName());
            hnLogger.homeNetLogger("TempX before iteration " + i + ":" + tempX);
            hnLogger.homeNetLogger("TempY before iteration " + i + ":" + tempY);
			ContentListIcon[i] = new HStaticIcon();
			ContentListIcon[i].setGraphicContent(deviceImg, HState.NORMAL_STATE);
			ContentListIcon[i].setHorizontalAlignment(HStaticIcon.HALIGN_CENTER);
			ContentListIcon[i].setVerticalAlignment(HStaticIcon.VALIGN_CENTER);
			ContentListIcon[i].setBordersEnabled(true);
			ContentListIcon[i].setForeground(Color.white);
			ContentListIcon[i].setBounds(tempX,tempY,deviceStartingW,deviceStartingH);
			add(ContentListIcon[i]);
			
            ContentTextLabel[i] = new HTextImpl(((ContentContainer) contentListArray.get(i)).getName(), new Rectangle(
                    tempX - 5, tempY + deviceStartingH, deviceStartingW, 40), Color.white, new Font("tiresias",
                    Font.PLAIN, 10), this);
			ContentTextLabel[i].setVisible(true);
			add(ContentTextLabel[i]);
            if ((tempX + (2 * deviceStartingW) + 10) < 600)
            {
				tempX+=deviceStartingW+25;
                hnLogger.homeNetLogger("New TempX at end of iteration " + i + ":" + tempX);
            }
            else
            {
				// total height with text and icon+10
				tempX= deviceStartingX;
				tempY += 86;
                hnLogger.homeNetLogger("New TempY at end of iteration " + i + ":" + tempY);
			}
		}
		headingIcon.setHorizontalAlignment(HStaticIcon.HALIGN_CENTER);
		headingIcon.setVerticalAlignment(HStaticIcon.VALIGN_CENTER);
		headingIcon.setBordersEnabled(false);
		headingIcon.setBounds((getWidth() / 2) - 125, getY() + 14, 250, 35);
		
		headingTextLabel = new HStaticText(currentMediaServer.getServerName());
		headingTextLabel.setFont(new Font("tiresias", Font.BOLD, 14));
		headingTextLabel.setForeground(Color.black);
		headingTextLabel.setHorizontalAlignment(HStaticText.HALIGN_CENTER);
		headingTextLabel.setVerticalAlignment(HStaticText.VALIGN_CENTER);
		headingTextLabel.setBounds((getWidth() / 2) - 100, getY() + 10, 200, 35);
		headingTextLabel.setVisible(true);
		add(headingTextLabel);
		add(headingIcon);
		repaint();
	}

    public void processUserEvent(UserEvent event)
    {

        switch (event.getCode())
        {
		case OCRcEvent.VK_UP:
                hnLogger.homeNetLogger("Inside Up.....");
			highlightUp();
			break;
		case OCRcEvent.VK_DOWN:
                hnLogger.homeNetLogger("Inside down.....");
			highlightDown();
			break;
		case OCRcEvent.VK_RIGHT:
                hnLogger.homeNetLogger("Inside right.....");
			highlightNext();
			break;
		case OCRcEvent.VK_LEFT:
                hnLogger.homeNetLogger("Inside left....");
			highlightPrev();
			break;
		case OCRcEvent.VK_ENTER:
                hnLogger.homeNetLogger("Inside Enter of .....");
                hnLogger.homeNetLogger("The Content list that is selected is :" + highButtonIndex);
			reloadScreen = true;
			
                HomeNetController.getInstance().displayNewScreen(
                        HNConstants.CONTENTITEMSPAGE_NAME,
                        new ContentItemRequestBean(currentMediaServer,
                                (ContentContainer) contentListArray.get(highButtonIndex), 0));
			reloadScreen = false;
			break;
		case OCRcEvent.VK_PAGE_DOWN:
                hnLogger.homeNetLogger("Inside page down of content list.");
                if (!endOfContent)
                {
				reloadScreen = true;
				reset();
				removeAll();
				startIndex+=24;
				init(currentMediaServer);
				reloadScreen = false;
			}
			break;
		case OCRcEvent.VK_PAGE_UP:
                hnLogger.homeNetLogger("Inside page up of content list.");
                if (!(startIndex - 24 < 0))
                {
				reloadScreen = true;
				reset();
				removeAll();
				startIndex-=24;
				init(currentMediaServer);
				reloadScreen = false;
			}
			break;
		case OCRcEvent.VK_BACK_SPACE:
			reloadScreen = true;
			HomeNetController.getInstance().displayNewScreen(HNConstants.DEVICEPAGE_NAME,new DevicePageBean(1, HNConstants.DEVICE_TYPE_MEDIASERVERS));
			reloadScreen = false;
			repaint();
			break;
		}
		
	}
	
    public void paint(Graphics g)
    {
        g.drawImage(backgrndImg, getX(), getY(), getWidth() - 50, getHeight() - 60, this);
		g.setColor(Color.yellow);
		g.drawRect(getX(), getY(), getWidth() - 50, getHeight() - 60);
        if (!reloadScreen)
        {
			if(!noContentAlert)
                g.drawRect(ContentListIcon[highButtonIndex].getX(), ContentListIcon[highButtonIndex].getY(),
                        ContentListIcon[highButtonIndex].getWidth(), ContentListIcon[highButtonIndex].getHeight());
            else
            {
				g.setColor(Color.red);
				g.setFont(new Font("tiresias", Font.BOLD,25));
				g.drawString("No content list available to be shown", getX()+100, getHeight()/2-20);
			}
        }
        else
        {
			g.drawImage(loadingImg, 280, 180, 50, 50, this);
		}
		super.paint(g);
	}

    public void reset()
    {
		highButtonIndex = 0;
		noContentAlert = false;
		rowNumber =0;
	}

    public void destroy()
    {

		reset();
		removeAll();
	}

}
