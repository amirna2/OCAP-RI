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
import org.cablelabs.xlet.hn.HNIAPP.model.upnp.UPnPEntity;
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
public class MessageCategoryList extends Page
{
    private HomeNetLogging hnLogger = HomeNetLogging.getInstance();

    public static MessageCategoryList msgCatListPage = null;

    private final Image backgrnd_Img = ImageUtil.getInstance().loadFileImage(ImageConstants.BACKGRND_IMG, this);

    public String[] buttonNormalImagePath = { ImageConstants.BUTTONNORMAL_IMG, ImageConstants.BUTTONNORMAL_IMG,
                                              ImageConstants.BUTTONNORMAL_IMG, ImageConstants.BUTTONNORMAL_IMG };

    public String[] buttonHighImagePath = { ImageConstants.BUTTONHIGHLIGHTED_IMG, ImageConstants.BUTTONHIGHLIGHTED_IMG,
                                            ImageConstants.BUTTONHIGHLIGHTED_IMG, ImageConstants.BUTTONHIGHLIGHTED_IMG };

    public String[] buttonTextList = { HNConstants.DISCOVER_MSG, HNConstants.DESCRIPTION_MSG, HNConstants.CONTROL_MSG,
            HNConstants.EVENTING_MSG };

    private final Image img_BTN1 = ImageUtil.getInstance().loadFileImage(ImageConstants.BUTTONNORMAL_IMG, this);

    public final Image img_BTN2 = ImageUtil.getInstance().loadFileImage(ImageConstants.BUTTONHIGHLIGHTED_IMG, this);
    
    public final Image loadingImg = ImageUtil.getInstance().loadFileImage(ImageConstants.LOADING_IMAGE, this);

    public Image[] image_Instances = { img_BTN1, img_BTN1,img_BTN1, img_BTN1 };

    public Image[] image_Highlighted_Instances = { img_BTN2, img_BTN2,img_BTN2, img_BTN2 };

    HStaticIcon loadingImageIcon = new HStaticIcon();

    HStaticText[] buttonListText = new HStaticText[5];

    HStaticIcon[] buttonListIcon = new HStaticIcon[5];

    HStaticText headingTextLabel = new HStaticText();

    HStaticIcon headingIcon = new HStaticIcon(ImageUtil.getInstance().loadFileImage(ImageConstants.TITLE_IMG, this));

    private int buttonIndex = 0;

    private int buttonStartYAxis = 110;

    private int yAxisIncrement = 70;

    private int iconStartYAxis = 110;

    private Object upnpEntity = null; 
    
    public static MessageCategoryList getInstance()
    {
        if (msgCatListPage == null)
            msgCatListPage = new MessageCategoryList();
        return msgCatListPage;
    }

    public void init(Object parameters)
    {
        setBounds(20, 20, 620, 460);
        hnLogger.homeNetLogger("inside ontent list page...");
        upnpEntity = parameters;
        initializeScreenComponents();
    }

    private void highlightNext()
    {
        hnLogger.homeNetLogger("Button index in next prev :" + buttonIndex);
        if (buttonIndex < (buttonTextList.length-1))
        {
            buttonListIcon[buttonIndex].setGraphicContent(image_Instances[buttonIndex], HState.NORMAL_STATE);
            buttonIndex = buttonIndex + 1;
            hnLogger.homeNetLogger("button high " + buttonHighImagePath[buttonIndex]);
            buttonListIcon[buttonIndex].setGraphicContent(image_Highlighted_Instances[buttonIndex], HState.NORMAL_STATE);
            hnLogger.homeNetLogger("Button index in next after :" + buttonIndex);
        }
    }

    private void highlightPrev()
    {
        if (buttonIndex > 0)
        {
            buttonListIcon[buttonIndex].setGraphicContent(image_Instances[buttonIndex], HState.NORMAL_STATE);
            buttonIndex = buttonIndex - 1;
            hnLogger.homeNetLogger("button high in prev" + buttonHighImagePath[buttonIndex]);
            buttonListIcon[buttonIndex].setGraphicContent(image_Highlighted_Instances[buttonIndex], HState.NORMAL_STATE);
            hnLogger.homeNetLogger("Button index in prev after :" + buttonIndex);
        }
    }

    private void initializeScreenComponents()
    {
        loadingImageIcon.setGraphicContent(loadingImg,HState.NORMAL_STATE);
        loadingImageIcon.setHorizontalAlignment(HStaticIcon.HALIGN_CENTER);
        loadingImageIcon.setVerticalAlignment(HStaticIcon.VALIGN_CENTER);
        loadingImageIcon.setBounds(getX(), getY(), getWidth() - 50, getHeight() - 60);
        loadingImageIcon.setBackgroundMode(HVisible.BACKGROUND_FILL);
        loadingImageIcon.setVisible(false);

        add(loadingImageIcon);
        headingIcon.setHorizontalAlignment(HStaticIcon.HALIGN_CENTER);
        headingIcon.setVerticalAlignment(HStaticIcon.VALIGN_CENTER);
        headingIcon.setBordersEnabled(false);
        headingIcon.setBounds((getWidth() / 2) - 125, getY() + 14, 250, 35);

        headingTextLabel = new HStaticText("Choose the Message category");
        headingTextLabel.setFont(new Font("tiresias", Font.BOLD, 14));
        headingTextLabel.setForeground(Color.black);
        headingTextLabel.setHorizontalAlignment(HStaticText.HALIGN_CENTER);
        headingTextLabel.setVerticalAlignment(HStaticText.VALIGN_CENTER);
        headingTextLabel.setBounds((getWidth() / 2) - 100, getY() + 10, 200, 35);
        headingTextLabel.setVisible(true);

        loadButtonImages();
        add(headingTextLabel);
        add(headingIcon);

    }

    private void loadButtonImages()
    {
        for (int i = 0; i < buttonTextList.length; i++)
        {
            buttonListText[i] = new HStaticText(buttonTextList[i]);
            buttonListText[i].setFont(new Font("tiresias", Font.BOLD, 16));
            buttonListText[i].setForeground(Color.white);
            buttonListText[i].setHorizontalAlignment(HStaticText.HALIGN_CENTER);
            buttonListText[i].setVerticalAlignment(HStaticText.VALIGN_CENTER);
            buttonListText[i].setBounds((getWidth() / 2) - 80, getY() + buttonStartYAxis, 150, 40);
            buttonListText[i].setVisible(true);
            buttonStartYAxis = buttonStartYAxis + yAxisIncrement;
            add(buttonListText[i]);
        }

        for (int i = 0; i < buttonTextList.length; i++)
        {
            buttonListIcon[i] = new HStaticIcon();
            if (i == 0)
            {
                hnLogger.homeNetLogger("Inside the first highlighted image" + i + "" + image_Highlighted_Instances[i]);
                buttonListIcon[i].setGraphicContent(image_Highlighted_Instances[i], HState.NORMAL_STATE);
            }  
            else
            {
                buttonListIcon[i].setGraphicContent(image_Instances[i], HState.NORMAL_STATE);
            }
            
            buttonListIcon[i].setHorizontalAlignment(HStaticIcon.HALIGN_JUSTIFY);
            buttonListIcon[i].setVerticalAlignment(HStaticIcon.VALIGN_JUSTIFY);
            buttonListIcon[i].setBordersEnabled(false);
            buttonListIcon[i].setBounds((getWidth() / 2) - 125, getY() + iconStartYAxis, 232, 47);
            iconStartYAxis = iconStartYAxis + yAxisIncrement;
            add(buttonListIcon[i]);
        }
    }

    public void processUserEvent(UserEvent event)
    {
        switch (event.getCode())
        {
            case OCRcEvent.VK_UP:
                hnLogger.homeNetLogger("Inside Up.....");
                highlightPrev();
                break;
            case OCRcEvent.VK_DOWN:
                hnLogger.homeNetLogger("Inside down.....");
                highlightNext();
                break;
            case OCRcEvent.VK_RIGHT:
                hnLogger.homeNetLogger("Inside right.....");

                break;
            case OCRcEvent.VK_LEFT:
                hnLogger.homeNetLogger("Inside left....");

                break;
            case OCRcEvent.VK_ENTER:
                setMessageSelected(buttonTextList[buttonIndex]);
                if(buttonIndex==0 || buttonIndex==1 || buttonIndex==2 || buttonIndex==3)
                {
                    HomeNetController.getInstance().displayNewScreen(HNConstants.CREATEMESSAGE_NAME,upnpEntity);
                }
                loadingImageIcon.setVisible(false);
                break;
            case OCRcEvent.VK_BACK_SPACE:
                HomeNetController.getInstance().displayNewScreen(HNConstants.MESSAGETYPELIST_NAME, null);
                break;
        }

    }

    private void setMessageSelected(String message)
    {
        hnLogger.homeNetLogger("The message type set is" + message);
        ((UPnPEntity)upnpEntity).setMessageCatSelected(message);
        hnLogger.homeNetLogger("The message type set is" + ((UPnPEntity) upnpEntity).getMessageCatSelected());
        
    }

    public void paint(Graphics g)
    {
        g.drawImage(backgrnd_Img, getX(), getY(), getWidth() - 50, getHeight() - 60, this);
        g.drawRect(getX(), getY(), getWidth() - 50, getHeight() - 60);
        super.paint(g);
    }

    public void reset()
    {
        buttonIndex = 0;
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
