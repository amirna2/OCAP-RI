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
import java.net.InetSocketAddress;

import org.cablelabs.xlet.hn.HNIAPP.controller.HomeNetController;
import org.cablelabs.xlet.hn.HNIAPP.model.upnp.UPnPEntity;
import org.cablelabs.xlet.hn.HNIAPP.util.HNConstants;
import org.cablelabs.xlet.hn.HNIAPP.util.ImageConstants;
import org.cablelabs.xlet.hn.HNIAPP.util.ImageUtil;
import org.cablelabs.xlet.hn.HNIAPP.util.MessageHandlerDelegate;
import org.cablelabs.xlet.hn.HNIAPP.util.MessageIdentification;
import org.cablelabs.xlet.hn.HNIAPP.util.logging.HomeNetLogging;
import org.dvb.event.UserEvent;
import org.dvb.ui.DVBTextLayoutManager;
import org.havi.ui.HListElement;
import org.havi.ui.HListGroup;
import org.havi.ui.HState;
import org.havi.ui.HStaticIcon;
import org.havi.ui.HStaticText;
import org.havi.ui.HVisible;
import org.ocap.hn.upnp.common.UPnPIncomingMessageHandler;
import org.ocap.hn.upnp.common.UPnPMessage;
import org.ocap.hn.upnp.common.UPnPOutgoingMessageHandler;
import org.ocap.ui.event.OCRcEvent;

/**
 * @author Parthiban Balasubramanian, Cognizant Technology Solutions
 * 
 */
public class CreateMessage extends Page
{
    private HomeNetLogging hnLogger = HomeNetLogging.getInstance();

    public static CreateMessage crtMsgPage = null;

    private final Image backgrnd_Img = ImageUtil.getInstance().loadFileImage(ImageConstants.BACKGRND_IMG, this);

     public final Image loadingImg = ImageUtil.getInstance().loadFileImage(ImageConstants.LOADING_IMAGE, this);

    HStaticIcon loadingImageIcon = new HStaticIcon();

    HStaticText[] buttonListText = null;

    HStaticIcon[] buttonListIcon = null;

    HStaticText headingTextLabel = new HStaticText();
    
    HStaticText statusTextLabel = new HStaticText();
    private String statusMessageHeading ="Status:\n";
    private StringBuffer statusMessageContent =new StringBuffer();

    HStaticIcon headingIcon = new HStaticIcon(ImageUtil.getInstance().loadFileImage(ImageConstants.TITLE_IMG, this));

    private int buttonIndex = 0;

    private UPnPEntity entityReceived = null;
    private HStaticText[] msgTextLabel = null;
    private String[] msgTextList =null; 
    private int endIndex = 0;
    private final Image img_MSG = ImageUtil.getInstance().loadFileImage(ImageConstants.MSG_IMG, this);
    private final Image img_high_MSG = ImageUtil.getInstance().loadFileImage(ImageConstants.MSG_HIGH_IMG, this);
    
    HListElement[] listItems = new HListElement[10];
    HListGroup hListGroup;
    
    public static CreateMessage getInstance()
    {
        if (crtMsgPage == null)
            crtMsgPage = new CreateMessage();
        return crtMsgPage;
    }

    public void init(Object parameters)
    {
        setBounds(20, 20, 620, 460);
        entityReceived = (UPnPEntity)parameters;
        // Retrieves the list of messages based on the category chosen
        msgTextList = MessageIdentification.messageList(entityReceived);
        if (msgTextList != null)
        {
            buttonListIcon = new HStaticIcon[msgTextList.length];
            buttonListText = new HStaticText[msgTextList.length];
            msgTextLabel = new HStaticText[msgTextList.length];
            endIndex = msgTextList.length;
            hnLogger.homeNetLogger("The received List of text :" + msgTextList.length);
        }
        hnLogger.homeNetLogger("inside CreateMessage...");
        initializeScreenComponents();
    }

    private void highlightNext()
    {
        hnLogger.homeNetLogger("Button index in next prev :" + buttonIndex);
        if (buttonIndex < (msgTextList.length - 1))
        {
            buttonListIcon[buttonIndex].setGraphicContent(img_MSG, HState.NORMAL_STATE);
            buttonIndex = buttonIndex + 1;
            hnLogger.homeNetLogger("button high " + buttonIndex);
            buttonListIcon[buttonIndex].setGraphicContent(img_high_MSG, HState.NORMAL_STATE);
            hnLogger.homeNetLogger("Button index in next after :" + buttonIndex);
        }
    }

    private void highlightPrev()
    {
        if (buttonIndex > 0)
        {
            buttonListIcon[buttonIndex].setGraphicContent(img_MSG, HState.NORMAL_STATE);
            buttonIndex = buttonIndex - 1;
            hnLogger.homeNetLogger("button high in prev" + buttonIndex);
            buttonListIcon[buttonIndex].setGraphicContent(img_high_MSG, HState.NORMAL_STATE);
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

        headingTextLabel = new HStaticText(entityReceived.getMessageCatSelected()+" messages");
        headingTextLabel.setFont(new Font("tiresias", Font.BOLD, 14));
        headingTextLabel.setForeground(Color.black);
        headingTextLabel.setHorizontalAlignment(HStaticText.HALIGN_CENTER);
        headingTextLabel.setVerticalAlignment(HStaticText.VALIGN_CENTER);
        headingTextLabel.setBounds((getWidth() / 2) - 100, getY() + 10, 200, 35);
        headingTextLabel.setVisible(true);

        statusTextLabel = new HStaticText(statusMessageHeading);
        statusTextLabel.setFont(new Font("tiresias", Font.BOLD, 14));
        statusTextLabel.setForeground(Color.black);
        statusTextLabel.setHorizontalAlignment(HStaticText.HALIGN_CENTER);
        statusTextLabel.setVerticalAlignment(HStaticText.VALIGN_CENTER);
        statusTextLabel.setBounds(300, 250, 250, 140);
        statusTextLabel.setTextLayoutManager(new DVBTextLayoutManager());
        statusTextLabel.setVisible(true);
        
        add(headingTextLabel);
        add(statusTextLabel);
        add(headingIcon);
        
        int deviceStartingX = getX()+35;
        int tempX = deviceStartingX;
        int deviceStartingY = getY()+80;
        int tempY = deviceStartingY;
        int deviceStartingW = 180;
        int deviceStartingH = 35;
        
        for (int i = 0; i < endIndex; i++)
        {
            hnLogger.homeNetLogger("TempX before iteration " + (i) + ":" + tempX);
            hnLogger.homeNetLogger("TempY before iteration " + (i) + ":" + tempY);
            
            // TO be changed after testing
            msgTextLabel[i] = new HStaticText(msgTextList[i]);
            msgTextLabel[i].setFont(new Font("tiresias", Font.PLAIN,13));
            msgTextLabel[i].setForeground(Color.white);
            msgTextLabel[i].setHorizontalAlignment(HStaticText.HALIGN_CENTER);
            msgTextLabel[i].setVerticalAlignment(HStaticText.VALIGN_CENTER);
            msgTextLabel[i].setBounds(tempX,tempY,deviceStartingW,deviceStartingH);
            msgTextLabel[i].setVisible(true);
            add(msgTextLabel[i]);
            
            buttonListIcon[i] = new HStaticIcon();
            if (i == 0)
            {
                buttonListIcon[i].setGraphicContent(img_high_MSG, HState.NORMAL_STATE);
            }
            else
            {
                buttonListIcon[i].setGraphicContent(img_MSG, HState.NORMAL_STATE);
            }
            
            buttonListIcon[i].setHorizontalAlignment(HStaticIcon.HALIGN_CENTER);
            buttonListIcon[i].setVerticalAlignment(HStaticIcon.VALIGN_CENTER);
            buttonListIcon[i].setBordersEnabled(true);
            buttonListIcon[i].setForeground(Color.white);
            buttonListIcon[i].setBounds(tempX,tempY,deviceStartingW,deviceStartingH);
            add(buttonListIcon[i]);

            if (tempY + 40 > 400)
            {
                tempX += deviceStartingX+deviceStartingW+10;
                tempY = deviceStartingY;
            }
            else
            {
                tempY += 40;
            }
            hnLogger.homeNetLogger("New TempY at end of iteration " + i + ":" + tempY);
        }
        repaint();
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
                statusMessageContent.delete(0,statusMessageContent.length());
                statusMessageContent.append(new MessageHandlerDelegate(entityReceived, msgTextList[buttonIndex],
                        new ServerMessageInterceptor(), new ClientMessageInterceptor()).handleMessage());
                statusTextLabel.setTextContent(statusMessageHeading + statusMessageContent.toString(),
                        HState.NORMAL_STATE);
                repaint();
                break;
            case OCRcEvent.VK_BACK_SPACE:
                HomeNetController.getInstance().displayNewScreen(HNConstants.MESSAGECATEGORYLIST_NAME, entityReceived);
                break;
            default:
                hnLogger.homeNetLogger("Default case is not supported.");
                break;
        }

    }

    public void paint(Graphics g)
    {
        g.drawImage(backgrnd_Img, getX(), getY(), getWidth() - 50, getHeight() - 60, this);
        g.setColor(Color.yellow);
        g.drawRect(getX(), getY(), getWidth() - 50, getHeight() - 60);
        g.drawRect(300,250,250,140);
        super.paint(g);
    }

    public void reset()
    {
        buttonIndex = 0;
    }

    public void destroy()
    {
        reset();
        removeAll();
    }

    class ServerMessageInterceptor implements UPnPOutgoingMessageHandler, UPnPIncomingMessageHandler
    {
    public byte[] handleOutgoingMessage(InetSocketAddress isa, UPnPMessage um, UPnPOutgoingMessageHandler uomh)
    {
            hnLogger.homeNetLogger("::::: Start of Server OutGoing Message ::::" + um.getStartLine());
            hnLogger.homeNetLogger("::::: Listing all the headers :::::");
            for (int i = 0; i < um.getHeaders().length; i++)
            {
                hnLogger.homeNetLogger(um.getHeaders()[i]);
       }
            hnLogger.homeNetLogger("::::: OutGoing Message XML:::::" + um.getXML());
            hnLogger.homeNetLogger("::::: End of Server OutGoing Message ::::");
       return uomh.handleOutgoingMessage(isa, um, uomh);
    }

    public UPnPMessage handleIncomingMessage(InetSocketAddress isa, byte[] im, UPnPIncomingMessageHandler uimh)
    {
            hnLogger.homeNetLogger(":::::Start of Server Incoming Message :::::\n" + new String(im)
                    + "\n:::::End of Server Incoming Message :::::");
        return uimh.handleIncomingMessage(isa, im, uimh);
        }
    }

    class ClientMessageInterceptor implements UPnPOutgoingMessageHandler, UPnPIncomingMessageHandler
    {
        public byte[] handleOutgoingMessage(InetSocketAddress isa, UPnPMessage um, UPnPOutgoingMessageHandler uomh)
        {
            hnLogger.homeNetLogger("::::: Start of Client OutGoing Message ::::" + um.getStartLine());
            hnLogger.homeNetLogger("::::: Listing all the headers :::::");
            for (int i = 0; i < um.getHeaders().length; i++)
            {
                hnLogger.homeNetLogger(um.getHeaders()[i]);
            }
            hnLogger.homeNetLogger("::::: OutGoing Message XML:::::" + um.getXML());
            hnLogger.homeNetLogger("::::: End of Client OutGoing Message ::::");
            return uomh.handleOutgoingMessage(isa, um, uomh);
        }

        public UPnPMessage handleIncomingMessage(InetSocketAddress isa, byte[] im, UPnPIncomingMessageHandler uimh)
        {
            hnLogger.homeNetLogger(":::::Start of Client Incoming Message :::::\n" + new String(im)
                    + "\n:::::End of Client Incoming Message :::::");
            return uimh.handleIncomingMessage(isa, im, uimh);
        }
    }
}
