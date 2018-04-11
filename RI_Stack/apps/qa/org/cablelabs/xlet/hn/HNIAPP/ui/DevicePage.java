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
import org.havi.ui.HVisible;
import org.ocap.ui.event.OCRcEvent;

/**
 * @author Parthiban Balasubramanian, Cognizant Technology Solutions
 * 
 */
public class DevicePage extends Page
{
    private HomeNetLogging hnLogger = HomeNetLogging.getInstance();

    static DevicePage s_HomePage;

    private static final String BACKGRND_IMG = "/org/cablelabs/xlet/hn/HNIAPP/etc/images/backgrnd.jpg";

    private static final String DEVICE_IMG = "/org/cablelabs/xlet/hn/HNIAPP/etc/images/deviceDisplayIcon.png";

    private static final String TITLE_IMG = "/org/cablelabs/xlet/hn/HNIAPP/etc/images/bannerBackgrnd.png";

    private int highButtonIndex = 0;

    private int rowNumber = 0;

    private boolean screenRefresh = false;

    private boolean noDeviceAlert = false;

    private HStaticIcon loadingImageIcon = new HStaticIcon();

    private HStaticIcon headingIcon = new HStaticIcon(ImageUtil.getInstance().loadFileImage(TITLE_IMG, this));

    private HTextImpl[] DeviceTextLabel = null;

    private HStaticIcon[] buttonListIcon = null;

    private HStaticText headingTextLabel = new HStaticText();

    private String deviceType = new String();

    private ArrayList deviceList = null;

    private int deviceListSize = 0;

    private int numDeviceAllowedPerPage = 24;

    private int startSize = 0;

    private int endIndex = 0;

    private int pageNum = 0;

    private Image loadingImg = null;

    private final Image backgrndImage = ImageUtil.getInstance().loadFileImage(BACKGRND_IMG, this);

    private DevicePage()
    {
    }

    public static DevicePage getInstance()
    {
        if (s_HomePage == null)
        {
            s_HomePage = new DevicePage();
        }
        return s_HomePage;
    }

    public void processUserEvent(UserEvent event)
    {
        switch (event.getCode())
        {
            case OCRcEvent.VK_UP:
                hnLogger.homeNetLogger("Inside Up.....");
                if ((rowNumber - 1) >= 0)
                {
                    highlightUp();
                }
                break;
            case OCRcEvent.VK_DOWN:
                hnLogger.homeNetLogger("Inside down.....");
                if ((rowNumber + 1) < (endIndex / 6))
                {
                    highlightDown();
                }
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
                hnLogger.homeNetLogger("The media server that is selected is :" + highButtonIndex);
                if (deviceType.equalsIgnoreCase(HNConstants.DEVICE_TYPE_MEDIASERVERS))
                {
                    loadingImageIcon.setVisible(true);
                    HomeNetController.getInstance().displayNewScreen(HNConstants.CONTENTITEMSPAGE_NAME,
                            new ContentItemRequestBean((MediaServer) deviceList.get(highButtonIndex), null, 0));
                    loadingImageIcon.setVisible(false);
                }
                else
                {
                    // as of now do not do anything
                }

                break;
            case OCRcEvent.VK_COLORED_KEY_1:
                highButtonIndex = 0;
                loadingImg = ImageUtil.getInstance().loadFileImage(
                        "/org/cablelabs/xlet/hn/HNIAPP/etc/images/ajax-loader.gif", this);
                screenRefresh = true;
                deviceList = null;
                removeAll();
                init(new DevicePageBean(1, deviceType));
                screenRefresh = false;
                break;
            case OCRcEvent.VK_PAGE_DOWN:
                int tempPageNum = 0;
                if (deviceListSize % 24 != 0)
                {
                    tempPageNum = 1;
                }
                if (++pageNum <= (deviceListSize / 24) + tempPageNum)
                {
                    highButtonIndex = 0;
                    loadingImg = ImageUtil.getInstance().loadFileImage(
                            "/org/cablelabs/xlet/hn/HNIAPP/etc/images/ajax-loader.gif", this);
                    screenRefresh = true;
                    removeAll();
                    init(new DevicePageBean(pageNum, deviceType));
                    screenRefresh = false;
                }
                else
                {
                    pageNum = (deviceListSize / 24) + tempPageNum;
                }
                break;
            case OCRcEvent.VK_PAGE_UP:
                if (!(--pageNum < 1))
                {
                    highButtonIndex = 0;
                    loadingImg = ImageUtil.getInstance().loadFileImage(
                            "/org/cablelabs/xlet/hn/HNIAPP/etc/images/ajax-loader.gif", this);
                    screenRefresh = true;
                    removeAll();
                    init(new DevicePageBean(pageNum, deviceType));
                    screenRefresh = false;
                }
                else
                {
                    pageNum = 1;
                }

                break;
            case OCRcEvent.VK_BACK_SPACE:
                HomeNetController.getInstance().displayNewScreen(HNConstants.HOMEPAGE_NAME, null);
                break;
        }

    }

    private void highlightDown()
    {
        if (rowNumber != 3)
        {
            if (!((highButtonIndex + 6) >= numDeviceAllowedPerPage))
                highButtonIndex = highButtonIndex + 6;
            else
                highButtonIndex = highButtonIndex + (numDeviceAllowedPerPage - highButtonIndex - 1);
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
                highButtonIndex = highButtonIndex - 6;
            }
            else
            {
                highButtonIndex = 0;
            }

            rowNumber--;
        }
        repaint();
    }

    private void highlightNext()
    {
        if ((highButtonIndex + 1) < endIndex)
        {
            highButtonIndex++;
            hnLogger.homeNetLogger("highButtonIndex/6 :" + highButtonIndex / 6);
            rowNumber = (highButtonIndex / 6);
            hnLogger.homeNetLogger("The row number is " + (rowNumber));
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
            hnLogger.homeNetLogger("highButtonIndex/6 :" + highButtonIndex / 6);
            rowNumber = (highButtonIndex / 6);
            hnLogger.homeNetLogger("The row number is " + (rowNumber));
            repaint();
        }
        else
        {
            hnLogger.homeNetLogger("In else part of the prev");
        }
    }

    public void init(Object parameters)
    {
        setBounds(20, 20, 620, 460);
        deviceList = new ArrayList();
        // get the page number to display the devices accordingly
        pageNum = ((DevicePageBean) parameters).getPagenum();
        deviceType = ((DevicePageBean) parameters).getDeviceType();

        if (deviceType.equalsIgnoreCase(HNConstants.DEVICE_TYPE_MEDIASERVERS))
        {
            deviceList = HNUtil.getInstance().getMediaServers();
        }
        else if (deviceType.equalsIgnoreCase(HNConstants.DEVICE_TYPE_ALL))
        {
            deviceList = HNUtil.getInstance().getAllDevices();
        }
        deviceListSize = deviceList.size();
        hnLogger.homeNetLogger("The device list size is " + deviceListSize);
        if (deviceListSize == 0)
        {
            noDeviceAlert = true;
        }
        else if (deviceListSize <= numDeviceAllowedPerPage)
        {
            startSize = 0;
            endIndex = deviceListSize;
            buttonListIcon = new HStaticIcon[deviceListSize];
            DeviceTextLabel = new HTextImpl[deviceListSize];
        }
        else if (numDeviceAllowedPerPage * pageNum <= deviceListSize)
        {
            if (pageNum == 1)
            {
                startSize = 0;
                endIndex = numDeviceAllowedPerPage;
            }
            else
            {
                startSize += numDeviceAllowedPerPage;
                endIndex = numDeviceAllowedPerPage;
            }
            buttonListIcon = new HStaticIcon[numDeviceAllowedPerPage];
            DeviceTextLabel = new HTextImpl[numDeviceAllowedPerPage];
        }
        else
        {
            startSize += numDeviceAllowedPerPage;
            endIndex = deviceListSize - numDeviceAllowedPerPage;
            buttonListIcon = new HStaticIcon[deviceListSize - numDeviceAllowedPerPage];
            DeviceTextLabel = new HTextImpl[deviceListSize - numDeviceAllowedPerPage];
        }
        initializeComponents();
    }

    private void initializeComponents()
    {
        loadingImageIcon.setGraphicContent(
                ImageUtil.getInstance().loadFileImage("/org/cablelabs/xlet/hn/HNIAPP/etc/images/ajax-loader.gif", this),
                HState.NORMAL_STATE);
        loadingImageIcon.setHorizontalAlignment(HStaticIcon.HALIGN_CENTER);
        loadingImageIcon.setVerticalAlignment(HStaticIcon.VALIGN_CENTER);
        loadingImageIcon.setBounds(getX(), getY(), getWidth() - 50, getHeight() - 60);
        loadingImageIcon.setBackgroundMode(HVisible.BACKGROUND_FILL);
        loadingImageIcon.setVisible(false);
        add(loadingImageIcon);
        hnLogger.homeNetLogger("inside device");
        // init the x,y , width and height and position all the device icons in
        // the same row
        int deviceStartingX = getX() + 35;
        int tempX = deviceStartingX;
        int deviceStartingY = getY() + 80;
        int tempY = deviceStartingY;
        int deviceStartingW = 60;
        int deviceStartingH = 36;
        // have to check for device list
        for (int i = 0; i < endIndex; i++)
        {
            hnLogger.homeNetLogger("TempX before iteration " + (i + startSize) + ":" + tempX);
            hnLogger.homeNetLogger("TempY before iteration " + (i + startSize) + ":" + tempY);
            buttonListIcon[i] = new HStaticIcon();
            buttonListIcon[i].setGraphicContent(ImageUtil.getInstance().loadFileImage(DEVICE_IMG, this),
                    HState.NORMAL_STATE);
            buttonListIcon[i].setHorizontalAlignment(HStaticIcon.HALIGN_CENTER);
            buttonListIcon[i].setVerticalAlignment(HStaticIcon.VALIGN_CENTER);
            buttonListIcon[i].setBordersEnabled(true);
            buttonListIcon[i].setForeground(Color.white);
            buttonListIcon[i].setBounds(tempX, tempY, deviceStartingW, deviceStartingH);
            add(buttonListIcon[i]);
            // TO be changed after testing
            if (deviceType.equalsIgnoreCase(HNConstants.DEVICE_TYPE_MEDIASERVERS))
            {
                DeviceTextLabel[i] = new HTextImpl(
                // "Device"+(i+startSize),
                        ((MediaServer) deviceList.get(i + startSize)).getServerName(), new Rectangle(tempX - 5, tempY
                                + deviceStartingH, deviceStartingW, 40), Color.white, new Font("tiresias", Font.PLAIN,
                                10), this);
            }
            else if (deviceType.equalsIgnoreCase(HNConstants.DEVICE_TYPE_ALL))
            {
                DeviceTextLabel[i] = new HTextImpl(((String) deviceList.get(i + startSize)), new Rectangle(tempX - 5,
                        tempY + deviceStartingH, deviceStartingW + 10, 45), Color.white, new Font("tiresias",
                        Font.PLAIN, 10), this);
            }

            DeviceTextLabel[i].setVisible(true);
            add(DeviceTextLabel[i]);
            if ((tempX + (2 * deviceStartingW) + 10) < 600)
            {
                tempX += deviceStartingW + 25;
                hnLogger.homeNetLogger("New TempX at end of iteration " + i + ":" + tempX);
            }
            else
            {
                // total height with text and icon+10
                tempX = deviceStartingX;
                tempY += 86;
                hnLogger.homeNetLogger("New TempY at end of iteration " + i + ":" + tempY);
            }
        }

        headingIcon.setHorizontalAlignment(HStaticIcon.HALIGN_CENTER);
        headingIcon.setVerticalAlignment(HStaticIcon.VALIGN_CENTER);
        headingIcon.setBordersEnabled(false);
        headingIcon.setBounds((getWidth() / 2) - 125, getY() + 14, 250, 35);

        headingTextLabel = new HStaticText("Devices discovered in network");
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

    public void paint(Graphics g)
    {
        g.drawImage(backgrndImage, getX(), getY(), getWidth() - 50, getHeight() - 60, this);
        g.setColor(Color.yellow);
        g.drawRect(getX(), getY(), getWidth() - 50, getHeight() - 60);
        if (!screenRefresh)
        {
            if (!noDeviceAlert)
                g.drawRect(buttonListIcon[highButtonIndex].getX(), buttonListIcon[highButtonIndex].getY(),
                        buttonListIcon[highButtonIndex].getWidth(), buttonListIcon[highButtonIndex].getHeight());
            else
            {
                g.setColor(Color.red);
                g.setFont(new Font("tiresias", Font.BOLD, 25));
                g.drawString("No device available to be shown", getX() + 100, getHeight() / 2 - 20);
            }
        }
        else
        {
            g.drawImage(loadingImg, 280, 180, 50, 50, this);
        }
        super.paint(g);
    }

    // This should be called when inside the destroy method if something has to
    // be done before destroying the page.
    public void reset()
    {
        highButtonIndex = 0;
        rowNumber = 0;
        screenRefresh = false;
        noDeviceAlert = false;
    }

    // All the destroy logic should be placed inside this as the Controller will
    // call this before switching the page.
    public void destroy()
    {
        reset();
        removeAll();
    }
}
