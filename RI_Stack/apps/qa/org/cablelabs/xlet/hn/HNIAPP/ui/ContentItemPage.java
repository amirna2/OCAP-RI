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
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;
import java.awt.image.ReplicateScaleFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.tv.service.Service;
import javax.tv.service.selection.ServiceContentHandler;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextException;
import javax.tv.service.selection.ServiceContextFactory;
import javax.tv.service.selection.ServiceMediaHandler;

import org.cablelabs.xlet.hn.HNIAPP.controller.HomeNetController;
import org.cablelabs.xlet.hn.HNIAPP.model.ContentItemRequestBean;
import org.cablelabs.xlet.hn.HNIAPP.model.DevicePageBean;
import org.cablelabs.xlet.hn.HNIAPP.util.HNConstants;
import org.cablelabs.xlet.hn.HNIAPP.util.HTextImpl;
import org.cablelabs.xlet.hn.HNIAPP.util.ImageConstants;
import org.cablelabs.xlet.hn.HNIAPP.util.ImageUtil;
import org.cablelabs.xlet.hn.HNIAPP.util.homenetworking.HNUtil;
import org.cablelabs.xlet.hn.HNIAPP.util.logging.HomeNetLogging;
import org.cablelabs.xlet.hn.HNIAPP.util.player.ChannelItemPlayer;
import org.cablelabs.xlet.hn.HNIAPP.util.player.JMFPlayer;
import org.dvb.event.UserEvent;
import org.havi.ui.HState;
import org.havi.ui.HStaticIcon;
import org.havi.ui.HStaticText;
import org.havi.ui.HVisible;
import org.ocap.hn.content.ChannelContentItem;
import org.ocap.hn.content.ContentContainer;
import org.ocap.hn.content.ContentItem;
import org.ocap.ui.event.OCRcEvent;

/**
 * @author Parthiban Balasubramanian, Cognizant Technology Solutions
 * 
 */
public class ContentItemPage extends Page
{
    private HomeNetLogging hnLogger = HomeNetLogging.getInstance();

    private static ContentItemPage contentItemPage = null;

    private static final String BACKGRND_IMG = "/org/cablelabs/xlet/hn/HNIAPP/etc/images/backgrnd.jpg";

    private static final String DEVICE_IMG = "/org/cablelabs/xlet/hn/HNIAPP/etc/images/folder_blue.png";

    private static final String CONTENTLISTICON_IMG = "/org/cablelabs/xlet/hn/HNIAPP/etc/images/ContentListIcon.jpg";

    private static final String TITLE_IMG = "/org/cablelabs/xlet/hn/HNIAPP/etc/images/bannerBackgrnd.png";

    private static final String BROKEN_IMAGE_URL = "/org/cablelabs/xlet/hn/HNIAPP/etc/images/NRImage-thumb.jpg";

    private final Image MPEG_VIDEO_IMG = ImageUtil.getInstance().loadFileImage(ImageConstants.MPEG_VIDEO_ICON, this);

    private final Image MPEG_VIDEO_SMALL_IMG = ImageUtil.getInstance().loadFileImage(
            ImageConstants.MPEG_VIDEO_SMALL_ICON, this);

    private final Image CHANNEL_ICON = ImageUtil.getInstance().loadFileImage(ImageConstants.CHANNEL_ICON, this);

    private boolean noContentItemAlert = false;

    private boolean fullScreen = false;

    private boolean reloadScreen = false;

    private boolean samePage = false;

    private int highButtonIndex = 0;

    private ContentItemRequestBean cip = null;

    private int rowNumber = 0;

    HTextImpl[] ContentTextLabel = null;

    HStaticIcon[] ContentListIcon = null;

    HStaticIcon fullScreenImageIcon = new HStaticIcon();

    HStaticIcon fullScreenTransImageIcon = new HStaticIcon(ImageUtil.getInstance().loadFileImage(
            "/org/cablelabs/xlet/hn/HNIAPP/etc/images/transparent.png", this));

    HStaticIcon headingIcon = new HStaticIcon(ImageUtil.getInstance().loadFileImage(TITLE_IMG, this));

    HStaticText headingTextLabel = new HStaticText();

    // Thumbnail Icon
    HStaticIcon thumbnailImageIcon = null;

    HTextImpl thumbnailImageText = null;

    private final Image contentListIcon = ImageUtil.getInstance().loadFileImage(CONTENTLISTICON_IMG, this);

    Image[] newThumbnailIcon = null;

    // To store content items(Video, Images, Channels) and content containers in
    // a seperate array.
    // Also items that are not recognized are marked as broken images
    private static ArrayList contentItemArray = new ArrayList();

    private static ArrayList contentContainerArray = new ArrayList();

    private static ArrayList contentItemNRArray = new ArrayList();

    private static ArrayList contentItemVideoArray = new ArrayList();

    private static ArrayList contentChannelItemArray = new ArrayList();

    private Map resultMap = new HashMap();

    private Image backgrndImg = ImageUtil.getInstance().loadFileImage(BACKGRND_IMG, this);

    private Image loadingImg = ImageUtil.getInstance().loadFileImage(
            "/org/cablelabs/xlet/hn/HNIAPP/etc/images/ajax-loader.gif", this);

    // This is for all the image resources
    private static ArrayList contentItemImageURL = null;

    private static ArrayList contentItemTitle = null;

    private static ArrayList contentItemDetails = null;

    // This is for all the video resources
    private static ArrayList contentItemVideoURL = null;

    private static ArrayList contentItemVideoTitle = null;

    private static ArrayList contentItemVideoDetails = null;

    // This is all for the channel items streamed
    private static ArrayList channelItemVideoURL = null;

    private static ArrayList channelItemTitle = null;

    private static ArrayList channelItemService = null;

    private static ArrayList channelItemDetails = null;

    // To hold size related information
    private int m_containerArraySize = 0;

    private int m_contentItemSize = 0;

    private int m_contentItemNRSize = 0;

    private int m_contentItemVideoSize = 0;

    private int m_channelContentItemSize = 0;

    // This is to hold the traversal path inside containers
    private ArrayList nodeNames = new ArrayList();

    public static ContentItemPage getInstance()
    {
        if (contentItemPage == null)
            contentItemPage = new ContentItemPage();
        return contentItemPage;
    }

    public void init(Object parameters)
    {

        setBounds(20, 20, 620, 460);
        hnLogger.homeNetLogger("inside content list page...");

        fullScreenTransImageIcon.setHorizontalAlignment(HStaticIcon.HALIGN_CENTER);
        fullScreenTransImageIcon.setVerticalAlignment(HStaticIcon.VALIGN_CENTER);
        fullScreenTransImageIcon.setBounds(getX(), getY(), getWidth() - 50, getHeight() - 60);
        fullScreenTransImageIcon.setBackgroundMode(HVisible.BACKGROUND_FILL);
        fullScreenTransImageIcon.setVisible(false);

        add(fullScreenImageIcon);
        add(fullScreenTransImageIcon);

        // To hold all content related internal items.
        contentItemImageURL = new ArrayList();
        contentItemTitle = new ArrayList();
        contentItemDetails = new ArrayList();

        // To hold all video related internal details.
        contentItemVideoURL = new ArrayList();
        contentItemVideoTitle = new ArrayList();
        contentItemVideoDetails = new ArrayList();

        // To hold all channel related internal details.
        channelItemVideoURL = new ArrayList();
        channelItemService = new ArrayList();
        channelItemDetails = new ArrayList();
        channelItemTitle = new ArrayList();

        cip = (ContentItemRequestBean) parameters;
        // count to be based on the number of slots to be set.
        resultMap = HNUtil.getInstance().getAllContentEntriesFromAlbum(cip.getCurrentMediaServer(),
                cip.getCurrentContentContainer(), cip.getStartIndex(), 12);
        if (!samePage)
        {
            nodeNames.add(cip.getCurrentContentContainer());
        }
        contentItemArray = (ArrayList) resultMap.get("ContentList");
        contentContainerArray = (ArrayList) resultMap.get("ContentContainer");
        contentItemNRArray = (ArrayList) resultMap.get("NRContenItems");
        contentItemVideoArray = (ArrayList) resultMap.get("ContentListVideo");
        contentChannelItemArray = (ArrayList) resultMap.get("StreamedChannels");

        if (contentItemArray.size() == 0 && contentContainerArray.size() == 0 && contentItemNRArray.size() == 0
                && contentItemVideoArray.size() == 0 && contentChannelItemArray.size() == 0)
        {
            noContentItemAlert = true;
        }
        else
        {
            m_containerArraySize = contentContainerArray.size();
            m_contentItemSize = contentItemArray.size();
            m_contentItemNRSize = contentItemNRArray.size();
            m_contentItemVideoSize = contentItemVideoArray.size();
            m_channelContentItemSize = contentChannelItemArray.size();

            ContentListIcon = new HStaticIcon[m_containerArraySize + m_contentItemSize + m_contentItemNRSize
                    + m_contentItemVideoSize + m_channelContentItemSize];
            ContentTextLabel = new HTextImpl[m_containerArraySize + m_contentItemSize + m_contentItemNRSize
                    + m_contentItemVideoSize + m_channelContentItemSize];
            newThumbnailIcon = new Image[m_containerArraySize + m_contentItemSize + m_contentItemNRSize
                    + m_contentItemVideoSize + m_channelContentItemSize];
            for (int i = 0; i < m_contentItemSize; i++)
            {
                // Get the title and the URL for the resource
                ContentItem tempItem = (ContentItem) contentItemArray.get(i);
                contentItemTitle.add(tempItem.getTitle());
                contentItemImageURL.add(HNUtil.getInstance().getImageUrl(tempItem));
                contentItemDetails.add("Size is " + tempItem.getContentSize() + " bytes & Created on "
                        + tempItem.getCreationDate());
            }

            // To iterate through all the ChannelContentItems and extract
            // internal details
            for (int i = 0; i < m_channelContentItemSize; i++)
            {
                ChannelContentItem l_channelItem = (ChannelContentItem) contentChannelItemArray.get(i);
                channelItemService.add(l_channelItem.getItemService());
                channelItemTitle.add(l_channelItem.getChannelTitle());
                channelItemVideoURL.add(l_channelItem.getChannelLocator());
                channelItemDetails.add("Channel Server:" + l_channelItem.getServer());
            }

            // This counter to display a count for the recordings that do not
            // have any name in their title.
            // This applies only to OCAP RI recorded items.
            int recordingItemCount = 0;
            for (int i = 0; i < m_contentItemVideoSize; i++)
            {
                // Get the title and the URL for the resource
                ContentItem tempItem = (ContentItem) contentItemVideoArray.get(i);
                if (tempItem.getTitle() == null || tempItem.getTitle().trim().length() == 0)
                {
                    contentItemVideoTitle.add(cip.getCurrentMediaServer().getServerName() + "_" + recordingItemCount
                            + 1);
                }
                else
                {
                    contentItemVideoTitle.add(tempItem.getTitle());
                }
                contentItemVideoURL.add(HNUtil.getInstance().getVideoURL(tempItem));
                contentItemVideoDetails.add("Size is " + tempItem.getContentSize() + " bytes & Created on "
                        + tempItem.getCreationDate());
            }
            highButtonIndex = 0;
            initializeScreenComponents(contentItemArray, contentContainerArray, contentItemNRArray,
                    contentItemVideoArray, contentChannelItemArray);
        }
    }

    private void highlightNext()
    {
        if (!fullScreen)
        {
            if ((highButtonIndex + 1) < (m_containerArraySize + m_contentItemSize + m_contentItemNRSize + m_contentItemVideoSize))
            {
                highButtonIndex++;
                repaint();
            }
            else
            {
                hnLogger.homeNetLogger("In else part of the next");
            }
        }
        else
        {
            if ((highButtonIndex + 1) < (m_containerArraySize + m_contentItemSize + m_contentItemNRSize + m_contentItemVideoSize))
            {
                highButtonIndex++;
                displayFullScreenImage();
            }

        }
    }

    public void displayFullScreenImage()
    {
        fullScreen = true;
        hnLogger.homeNetLogger("The fullscreen highlight that is selected is :" + highButtonIndex);
        Image tempImg = null;
        try
        {
            tempImg = ImageUtil.getInstance().loadImageFromUrl(
                    new URL((String) contentItemImageURL.get(highButtonIndex)), this);
        }
        catch (MalformedURLException e)
        {
            hnLogger.homeNetLogger(e);
        }
        int m_h = tempImg.getHeight(this);
        int m_w = tempImg.getWidth(this);
        hnLogger.homeNetLogger("Height of the image" + tempImg.getHeight(this));
        hnLogger.homeNetLogger("Width of the image" + tempImg.getWidth(this));
        hnLogger.homeNetLogger("The width of the fullscreenIcon " + (getWidth() - 50));
        hnLogger.homeNetLogger("The height of the fullscreenIcon " + (getHeight() - 65));
        // This piece of code is to avoid the scaling if the pic fits in the
        // slot for fullscreen
        if (m_w > (getWidth() - 50) || m_h > (getHeight() - 60))
        {
            ReplicateScaleFilter filterScale = new ReplicateScaleFilter(getWidth() - 50, getHeight() - 60);
            ImageProducer imageProducer = new FilteredImageSource(tempImg.getSource(), filterScale);
            fullScreenImageIcon.setGraphicContent(createImage(imageProducer), HState.NORMAL_STATE);
        }
        else
        {
            fullScreenImageIcon.setGraphicContent(tempImg, HState.NORMAL_STATE);
        }
        fullScreenImageIcon.setHorizontalAlignment(HStaticIcon.HALIGN_CENTER);
        fullScreenImageIcon.setVerticalAlignment(HStaticIcon.VALIGN_CENTER);
        fullScreenImageIcon.setBounds(getX(), getY(), getWidth() - 50, getHeight() - 60);
        fullScreenImageIcon.setBackgroundMode(HVisible.BACKGROUND_FILL);
        fullScreenTransImageIcon.setVisible(true);
        fullScreenImageIcon.setVisible(true);
        repaint();
    }

    private void highlightPrev()
    {
        if (!fullScreen)
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
        else
        {
            if ((highButtonIndex - 1) >= 0)
            {
                highButtonIndex--;
                displayFullScreenImage();
            }
        }
    }

    private void highlightDown()
    {
        if (!fullScreen)
        {
            if (rowNumber != 3)
            {
                if (!((highButtonIndex + 3) >= (m_containerArraySize + m_contentItemSize + m_contentItemNRSize + m_contentItemVideoSize)))
                    highButtonIndex = highButtonIndex + 3;
                else
                    highButtonIndex = highButtonIndex
                            + ((m_containerArraySize + m_contentItemSize + m_contentItemNRSize + m_contentItemVideoSize)
                                    - highButtonIndex - 1);
                rowNumber++;
            }
            repaint();
        }
    }

    private void highlightUp()
    {

        if (!fullScreen)
        {
            if (rowNumber != 0)
            {
                if (!((highButtonIndex - 3) < 0))
                {
                    highButtonIndex = highButtonIndex - 3;
                }
                else
                {
                    highButtonIndex = 0;
                }

                rowNumber--;
            }
            repaint();
        }
    }

    private void initializeScreenComponents(ArrayList contentItemArray, ArrayList contentContainerArray,
            ArrayList contentItemNRArray, ArrayList contentItemVideo, ArrayList contentChannelItems)
    {
        hnLogger.homeNetLogger("inside content page initialize");
        // init the x,y , width and height and position all the device icons in
        // the same row
        // int deviceStartingX = getX()+35;
        int deviceStartingX = getX() + 295;
        int tempX = deviceStartingX;
        int deviceStartingY = getY() + 80;
        int tempY = deviceStartingY;
        int deviceStartingW = 60;
        int deviceStartingH = 36;
        for (int i = 0; i < m_containerArraySize; i++)
        {
            hnLogger.homeNetLogger("The content container is ..name . "
                    + ((ContentContainer) contentContainerArray.get(i)).getName());
            hnLogger.homeNetLogger("TempX before iteration " + i + ":" + tempX);
            hnLogger.homeNetLogger("TempY before iteration " + i + ":" + tempY);
            ContentListIcon[i] = new HStaticIcon();
            Image tempImage = ImageUtil.getInstance().loadFileImage(DEVICE_IMG, this);
            ContentListIcon[i].setGraphicContent(tempImage, HState.NORMAL_STATE);
            ContentListIcon[i].setHorizontalAlignment(HStaticIcon.HALIGN_CENTER);
            ContentListIcon[i].setVerticalAlignment(HStaticIcon.VALIGN_CENTER);
            ContentListIcon[i].setBordersEnabled(true);
            ContentListIcon[i].setForeground(Color.white);
            ContentListIcon[i].setBounds(tempX, tempY, deviceStartingW, deviceStartingH);
            newThumbnailIcon[i] = contentListIcon;
            add(ContentListIcon[i]);

            ContentTextLabel[i] = new HTextImpl(((ContentContainer) contentContainerArray.get(i)).getName(),
                    new Rectangle(tempX, tempY + deviceStartingH, deviceStartingW, 40), Color.white, new Font(
                            "tiresias", Font.PLAIN, 10), this);
            ContentTextLabel[i].setVisible(true);
            add(ContentTextLabel[i]);
            if ((tempX + (2 * deviceStartingW) + 10) < 590)
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

        for (int i = 0; i < m_contentItemSize; i++)
        {
            hnLogger.homeNetLogger("The content Item is ..name . " + contentItemTitle.get(i));
            hnLogger.homeNetLogger("TempX before iteration " + i + ":" + tempX);
            hnLogger.homeNetLogger("TempY before iteration " + i + ":" + tempY);
            // get the image url
            // Scale the image to fit the slot
            int newIndex = m_containerArraySize + i;
            try
            {
                Image originalImage;
                hnLogger.homeNetLogger((String) contentItemImageURL.get(i));
                if (((String) contentItemImageURL.get(i)).equalsIgnoreCase(""))
                {
                    contentItemImageURL.add(i, BROKEN_IMAGE_URL);
                    originalImage = ImageUtil.getInstance().loadFileImage(BROKEN_IMAGE_URL, this);
                }
                else
                {
                    originalImage = loadImageFromURLHelper(new URL((String) contentItemImageURL.get(i)));
                }
                // This is the scaling done for the small icons.

                ImageProducer tempImgProducer = scaleImage(originalImage, deviceStartingW, deviceStartingH);
                Image tempImg = createImage(tempImgProducer);
                // Image tempImg = loadImageFromURLHelper(new
                // URL((String)contentItemImageURL.get(i)));
                int t_h = originalImage.getHeight(this);
                int t_w = originalImage.getWidth(this);
                if (t_w > 245 || t_h > 270)
                {
                    // The scaling is done for the images if the dimensions are
                    // more than the given slot for the thumbnail
                    newThumbnailIcon[newIndex] = createImage(scaleImage(originalImage, 245, 270));
                }
                else
                {
                    newThumbnailIcon[newIndex] = originalImage;
                }
                ContentListIcon[newIndex] = new HStaticIcon(tempImg);
            }
            catch (MalformedURLException e)
            {
                hnLogger.homeNetLogger(e);
            }
            ContentListIcon[newIndex].setHorizontalAlignment(HStaticIcon.HALIGN_CENTER);
            ContentListIcon[newIndex].setVerticalAlignment(HStaticIcon.VALIGN_CENTER);
            ContentListIcon[newIndex].setBordersEnabled(true);
            ContentListIcon[newIndex].setForeground(Color.white);
            ContentListIcon[newIndex].setBounds(tempX, tempY, deviceStartingW, deviceStartingH);
            add(ContentListIcon[newIndex]);

            ContentTextLabel[newIndex] = new HTextImpl((String) contentItemTitle.get(i), new Rectangle(tempX - 5, tempY
                    + deviceStartingH, deviceStartingW, 40), Color.white, new Font("tiresias", Font.PLAIN, 10), this);
            ContentTextLabel[newIndex].setVisible(true);
            add(ContentTextLabel[newIndex]);
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
            repaint();
        }
        // Populate the video items on the screen
        for (int i = 0; i < m_contentItemVideoSize; i++)
        {
            hnLogger.homeNetLogger("The content Item is ..name . " + (String) contentItemVideoTitle.get(i));
            hnLogger.homeNetLogger("TempX before iteration " + i + ":" + tempX);
            hnLogger.homeNetLogger("TempY before iteration " + i + ":" + tempY);
            int newIndex1 = m_containerArraySize + m_contentItemSize + i;
            ContentListIcon[newIndex1] = new HStaticIcon(MPEG_VIDEO_SMALL_IMG);
            ContentListIcon[newIndex1].setHorizontalAlignment(HStaticIcon.HALIGN_CENTER);
            ContentListIcon[newIndex1].setVerticalAlignment(HStaticIcon.VALIGN_CENTER);
            ContentListIcon[newIndex1].setBordersEnabled(true);
            ContentListIcon[newIndex1].setForeground(Color.white);
            ContentListIcon[newIndex1].setBounds(tempX, tempY, deviceStartingW, deviceStartingH);
            newThumbnailIcon[newIndex1] = MPEG_VIDEO_IMG;
            add(ContentListIcon[newIndex1]);
            ContentTextLabel[newIndex1] = new HTextImpl((String) contentItemVideoTitle.get(i), new Rectangle(tempX - 5,
                    tempY + deviceStartingH, deviceStartingW, 40), Color.white, new Font("tiresias", Font.PLAIN, 10),
                    this);
            ContentTextLabel[newIndex1].setVisible(true);
            add(ContentTextLabel[newIndex1]);
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
            repaint();
        }

        for (int i = 0; i < m_channelContentItemSize; i++)
        {
            hnLogger.homeNetLogger("The channel Item title is ..name . " + (String) channelItemTitle.get(i));
            hnLogger.homeNetLogger("TempX before iteration " + i + ":" + tempX);
            hnLogger.homeNetLogger("TempY before iteration " + i + ":" + tempY);
            int newIndex1 = m_containerArraySize + m_contentItemSize + m_contentItemVideoSize + i;
            ContentListIcon[newIndex1] = new HStaticIcon(CHANNEL_ICON);
            ContentListIcon[newIndex1].setHorizontalAlignment(HStaticIcon.HALIGN_CENTER);
            ContentListIcon[newIndex1].setVerticalAlignment(HStaticIcon.VALIGN_CENTER);
            ContentListIcon[newIndex1].setBordersEnabled(true);
            ContentListIcon[newIndex1].setForeground(Color.white);
            ContentListIcon[newIndex1].setBounds(tempX, tempY, deviceStartingW, deviceStartingH);
            newThumbnailIcon[newIndex1] = MPEG_VIDEO_IMG;
            add(ContentListIcon[newIndex1]);
            ContentTextLabel[newIndex1] = new HTextImpl((String) channelItemTitle.get(i), new Rectangle(tempX - 5,
                    tempY + deviceStartingH, deviceStartingW, 40), Color.white, new Font("tiresias", Font.PLAIN, 10),
                    this);
            ContentTextLabel[newIndex1].setVisible(true);
            add(ContentTextLabel[newIndex1]);
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
            repaint();
        }

        // Populate the NR items on the screen
        for (int i = 0; i < m_contentItemNRSize; i++)
        {
            hnLogger.homeNetLogger("The content Item is ..name . " + (String) contentItemNRArray.get(i));
            hnLogger.homeNetLogger("TempX before iteration " + i + ":" + tempX);
            hnLogger.homeNetLogger("TempY before iteration " + i + ":" + tempY);
            int newIndex1 = m_containerArraySize + m_contentItemSize + m_contentItemVideoSize
                    + m_channelContentItemSize + i;
            ContentListIcon[newIndex1] = new HStaticIcon(MPEG_VIDEO_IMG);
            ContentListIcon[newIndex1].setHorizontalAlignment(HStaticIcon.HALIGN_CENTER);
            ContentListIcon[newIndex1].setVerticalAlignment(HStaticIcon.VALIGN_CENTER);
            ContentListIcon[newIndex1].setBordersEnabled(true);
            ContentListIcon[newIndex1].setForeground(Color.white);
            ContentListIcon[newIndex1].setBounds(tempX, tempY, deviceStartingW, deviceStartingH);
            newThumbnailIcon[newIndex1] = MPEG_VIDEO_IMG;
            add(ContentListIcon[newIndex1]);
            ContentTextLabel[newIndex1] = new HTextImpl((String) contentItemNRArray.get(i), new Rectangle(tempX - 5,
                    tempY + deviceStartingH, deviceStartingW, 40), Color.white, new Font("tiresias", Font.PLAIN, 10),
                    this);
            ContentTextLabel[newIndex1].setVisible(true);
            add(ContentTextLabel[newIndex1]);
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
            repaint();
        }
        resetThumbnail(newThumbnailIcon[0]);

        headingIcon.setHorizontalAlignment(HStaticIcon.HALIGN_CENTER);
        headingIcon.setVerticalAlignment(HStaticIcon.VALIGN_CENTER);
        headingIcon.setBordersEnabled(false);
        headingIcon.setBounds((getWidth() / 2) - 125, getY() + 14, 250, 35);

        // for keeping the parent container names
        if (nodeNames.get(nodeNames.size() - 1) == null)
        {
            headingTextLabel = new HStaticText("RootContainer");
        }
        else
        {
            headingTextLabel = new HStaticText(((ContentContainer) nodeNames.get(nodeNames.size() - 1)).getName());
        }

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

            case OCRcEvent.VK_PAGE_DOWN:
                hnLogger.homeNetLogger("Inside page down....");
                ContentItemRequestBean tempcipDown = null;
                if (!((m_containerArraySize + m_contentItemSize + m_contentItemNRSize + m_contentItemVideoSize) < 12))
                {
                    reloadScreen = true;
                    cip.setPreviousStartIndex(cip.getStartIndex());
                    cip.setStartIndex(cip.getStartIndex() + 12);
                    tempcipDown = new ContentItemRequestBean(cip.getCurrentMediaServer(),
                            cip.getCurrentContentContainer(), cip.getStartIndex());
                    reset();
                    removeAll();
                    samePage = true;
                    init(tempcipDown);
                    samePage = false;
                    reloadScreen = false;
                }
                break;

            case OCRcEvent.VK_PAGE_UP:
                hnLogger.homeNetLogger("Inside page up....");
                ContentItemRequestBean tempcipUp = null;
                reloadScreen = true;
                reset();
                if (!((cip.getStartIndex() - 12) < 0))
                {
                    cip.setStartIndex(cip.getStartIndex() - 12);
                    tempcipUp = new ContentItemRequestBean(cip.getCurrentMediaServer(),
                            cip.getCurrentContentContainer(), cip.getStartIndex());
                    removeAll();
                    samePage = true;
                    init(tempcipUp);
                    samePage = false;
                }
                reloadScreen = false;
                break;

            case OCRcEvent.VK_ENTER:
                hnLogger.homeNetLogger("Inside Enter of .....");
                ContentItemRequestBean tempcip = null;
                hnLogger.homeNetLogger("Value of high Index in Enter is :" + highButtonIndex);
                if (m_containerArraySize != 0 && highButtonIndex < m_containerArraySize)
                {
                    reloadScreen = true;
                    cip.setCurrentContentContainer((ContentContainer) contentContainerArray.get(highButtonIndex));
                    tempcip = new ContentItemRequestBean(cip.getCurrentMediaServer(), cip.getCurrentContentContainer(),
                            cip.getStartIndex());
                    reset();
                    removeAll();
                    init(tempcip);
                    reloadScreen = false;
                }
                else if (highButtonIndex < (m_containerArraySize + m_contentItemSize))
                {
                    reloadScreen = true;
                    displayFullScreenImage();
                    reloadScreen = false;
                }
                else if (highButtonIndex < (m_containerArraySize + m_contentItemSize + m_contentItemVideoSize))
                {
                    reloadScreen = true;
                    stopExistingMediaHandlers();
                    JMFPlayer.getInstance().start(
                            (String) contentItemVideoURL.get(highButtonIndex
                                    - (m_containerArraySize + m_contentItemSize)));
                    reloadScreen = false;
                }
                else if (highButtonIndex < (m_containerArraySize + m_contentItemSize + m_contentItemVideoSize + m_channelContentItemSize))
                {
                    reloadScreen = true;
                    System.out.println("Inside playing channel Content Information");
                    stopExistingMediaHandlers();
                    ChannelItemPlayer.getInstance().startLiveChannel(
                            (Service) channelItemService.get(highButtonIndex
                                    - (m_containerArraySize + m_contentItemSize + m_contentItemVideoSize)));
                    reloadScreen = false;
                }
                break;
            case OCRcEvent.VK_BACK_SPACE:
                if (!fullScreen)
                {
                    JMFPlayer.getInstance().destroy();
                    reloadScreen = true;
                    tempcip = null;
                    nodeNames.remove(nodeNames.size() - 1);
                    if (nodeNames.size() != 0)
                    {
                        cip.setCurrentContentContainer((ContentContainer) nodeNames.get(nodeNames.size() - 1));
                        tempcip = new ContentItemRequestBean(cip.getCurrentMediaServer(),
                                cip.getCurrentContentContainer(), 0);
                        // this is removed since in the init method the
                        // container entry is again added to the arraylist
                        nodeNames.remove(nodeNames.size() - 1);
                        reset();
                        removeAll();
                        init(tempcip);
                        reloadScreen = false;
                        repaint();
                    }
                    else
                    {
                        HomeNetController.getInstance().displayNewScreen(HNConstants.DEVICEPAGE_NAME,
                                new DevicePageBean(1, HNConstants.DEVICE_TYPE_MEDIASERVERS));
                        repaint();
                    }
                }
                else
                {
                    fullScreenImageIcon.setVisible(false);
                    fullScreenTransImageIcon.setVisible(false);
                    fullScreen = false;
                    repaint();
                }
                break;

            case OCRcEvent.VK_STOP:
                hnLogger.homeNetLogger("Inside stop");
                if (JMFPlayer.getInstance().isPlayerStarted())
                {
                    JMFPlayer.getInstance().destroy();
                }
                else if (ChannelItemPlayer.getInstance().isServiceSelected())
                {
                    ChannelItemPlayer.getInstance().stopService();
                }

                break;

            case OCRcEvent.VK_FAST_FWD:
                hnLogger.homeNetLogger("Inside Fast forward");
                if (JMFPlayer.getInstance().getPlayerInstance() != null)
                {
                    JMFPlayer.getInstance().changePlayerRate((float) 4.0);
                }
                else if (ChannelItemPlayer.getInstance().isServiceSelected())
                {
                    ChannelItemPlayer.getInstance().setContextPlayerRate((float) 4.0);
                }
                break;
            case OCRcEvent.VK_PAUSE:
                hnLogger.homeNetLogger("Inside pause..");
                if (JMFPlayer.getInstance().getPlayerInstance() != null)
                {
                    JMFPlayer.getInstance().changePlayerRate((float) 0.0);
                }
                else if (ChannelItemPlayer.getInstance().isServiceSelected())
                {
                    ChannelItemPlayer.getInstance().setContextPlayerRate((float) 0.0);
                }
                break;
            case OCRcEvent.VK_REWIND:
                hnLogger.homeNetLogger("Inside rewind..");
                if (JMFPlayer.getInstance().getPlayerInstance() != null)
                {
                    JMFPlayer.getInstance().changePlayerRate((float) -2.0);
                }
                else if (ChannelItemPlayer.getInstance().isServiceSelected())
                {
                    ChannelItemPlayer.getInstance().setContextPlayerRate((float) -2.0);
                }
                break;
            case OCRcEvent.VK_PLAY:
                hnLogger.homeNetLogger("Inside play..");
                if (JMFPlayer.getInstance().getPlayerInstance() != null)
                {
                    JMFPlayer.getInstance().changePlayerRate((float) 1.0);
                }
                else if (ChannelItemPlayer.getInstance().isServiceSelected())
                {
                    ChannelItemPlayer.getInstance().setContextPlayerRate((float) 1.0);
                }
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
            if (!noContentItemAlert)
            {
                if (m_containerArraySize != 0 && highButtonIndex < m_containerArraySize)
                {
                    resetNewThumbnailIcon(newThumbnailIcon[highButtonIndex],
                            ((ContentContainer) contentContainerArray.get(highButtonIndex)).getName());
                }
                else if (highButtonIndex < (m_containerArraySize + m_contentItemSize))
                {
                    resetNewThumbnailIcon(newThumbnailIcon[highButtonIndex],
                            (String) contentItemDetails.get(highButtonIndex - m_containerArraySize));
                }
                else if (highButtonIndex < (m_containerArraySize + m_contentItemSize + m_contentItemVideoSize))
                {
                    resetNewThumbnailIcon(newThumbnailIcon[highButtonIndex],
                            (String) contentItemVideoTitle.get(highButtonIndex
                                    - (m_containerArraySize + m_contentItemSize)));
                }
                else if (highButtonIndex < (m_containerArraySize + m_contentItemSize + m_contentItemVideoSize + m_channelContentItemSize))
                {
                    resetNewThumbnailIcon(newThumbnailIcon[highButtonIndex],
                            (String) channelItemDetails.get(highButtonIndex
                                    - (m_containerArraySize + m_contentItemSize + m_contentItemVideoSize)));
                }
                else if (highButtonIndex < (m_containerArraySize + m_contentItemSize + m_contentItemVideoSize
                        + m_channelContentItemSize + m_contentItemNRSize))
                {
                    resetNewThumbnailIcon(
                            newThumbnailIcon[highButtonIndex],
                            (String) contentItemNRArray.get(highButtonIndex
                                    - (m_containerArraySize + m_contentItemSize + m_contentItemVideoSize + m_channelContentItemSize)));
                }
                g.drawRect(ContentListIcon[highButtonIndex].getX(), ContentListIcon[highButtonIndex].getY(),
                        ContentListIcon[highButtonIndex].getWidth(), ContentListIcon[highButtonIndex].getHeight());
                g.drawRect(thumbnailImageIcon.getX(), thumbnailImageIcon.getY(), thumbnailImageIcon.getWidth(),
                        thumbnailImageIcon.getHeight());
            }
            else
            {
                g.setColor(Color.red);
                g.setFont(new Font("tiresias", Font.BOLD, 25));
                g.drawString("No content Items available to be shown", getX() + 100, getHeight() / 2 - 20);
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
        noContentItemAlert = false;
        rowNumber = 0;

    }

    public void destroy()
    {

        reset();
        reloadScreen = false;
        removeAll();
    }

    // this has been modified to avoid the image to get scaled if the image's
    // height and width is fitting the slots for thumbnail
    public ImageProducer loadImageFromURLHelper(URL imgURL, int x, int y) throws MalformedURLException
    {
        Image tempImg = null;
        tempImg = ImageUtil.getInstance().loadImageFromUrl(imgURL, this);
        if ((tempImg.getHeight(this) > x) && (tempImg.getWidth(this) > y))
        {
            ReplicateScaleFilter filterScale = new ReplicateScaleFilter(x, y);
            ImageProducer imageProducer = new FilteredImageSource(tempImg.getSource(), filterScale);
            return imageProducer;
        }
        else
        {
            return null;
        }
    }

    public ImageProducer scaleImage(Image l_img, int x, int y)
    {
        Image tempImg = l_img;
        ReplicateScaleFilter filterScale = new ReplicateScaleFilter(x, y);
        ImageProducer imageProducer = new FilteredImageSource(tempImg.getSource(), filterScale);
        return imageProducer;
    }

    public Image loadImageFromURLHelper(URL imgURL) throws MalformedURLException
    {
        Image tempImg = null;
        tempImg = ImageUtil.getInstance().loadImageFromUrl(imgURL, this);
        return tempImg;
    }

    public void resetThumbnail(Image imgThumb)
    {
        thumbnailImageIcon = new HStaticIcon(imgThumb);
        thumbnailImageIcon.setHorizontalAlignment(HStaticIcon.HALIGN_CENTER);
        thumbnailImageIcon.setVerticalAlignment(HStaticIcon.VALIGN_CENTER);
        thumbnailImageIcon.setBordersEnabled(true);
        thumbnailImageIcon.setForeground(Color.white);
        thumbnailImageIcon.setBounds(55, 100, 245, 270);
        thumbnailImageIcon.setVisible(false);

        thumbnailImageText = new HTextImpl("", new Rectangle(thumbnailImageIcon.getX(), thumbnailImageIcon.getY()
                + thumbnailImageIcon.getHeight() + 5, thumbnailImageIcon.getWidth(), 40), Color.white, new Font(
                "tiresias", Font.PLAIN, 10), this);
        thumbnailImageText.setVisible(false);
        add(thumbnailImageText);
        add(thumbnailImageIcon);
    }

    public void resetThumbnailIcon()
    {
        thumbnailImageIcon.setGraphicContent(contentListIcon, HState.NORMAL_STATE);
        thumbnailImageIcon.setVisible(true);
        thumbnailImageText.setVisible(true);
    }

    public void resetNewThumbnailIcon(Image imgProd, String imgText)
    {
        thumbnailImageIcon.setGraphicContent(imgProd, HState.NORMAL_STATE);
        thumbnailImageText.setTextContent(imgText);
        thumbnailImageIcon.setVisible(true);
        thumbnailImageText.setVisible(true);
    }

    /**
     * This stops any existing Media Handlers already started and being used.
     * This is to avoid resource contention during player creation.
     */
    private void stopExistingMediaHandlers()
    {
        ServiceContext serviceContext;
        try
        {
            serviceContext = ServiceContextFactory.getInstance().getServiceContext(
                    HomeNetController.getInstance().getXletContext());
            ServiceContentHandler[] handlers = serviceContext.getServiceContentHandlers();
            for (int i = 0; i < handlers.length; i++)
            {
                if (handlers[i] instanceof ServiceMediaHandler)
                {
                    ((ServiceMediaHandler) handlers[i]).stop();
                }
            }
        }
        catch (SecurityException e1)
        {
            e1.printStackTrace();
        }
        catch (ServiceContextException e1)
        {
            e1.printStackTrace();
        }

    }

}
