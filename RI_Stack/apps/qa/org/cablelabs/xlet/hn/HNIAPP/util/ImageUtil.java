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
package org.cablelabs.xlet.hn.HNIAPP.util;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;
import java.awt.image.ReplicateScaleFilter;
import java.net.URL;

import org.cablelabs.xlet.hn.HNIAPP.util.logging.HomeNetLogging;

/**
 * @author Parthiban Balasubramanian, Cognizant Technology Solutions
 * 
 */
public class ImageUtil
{
    private static HomeNetLogging hnLogger = HomeNetLogging.getInstance();

	private static ImageUtil utilInstance = null;

    public static ImageUtil getInstance()
    {
        if (utilInstance == null)
        {
			utilInstance = new ImageUtil();
        }
        else
        {
			return utilInstance;
		}
		return utilInstance;
	}

    public Image loadFileImage(String filePath, Component comp)
    {

		URL imgURL = getClass().getResource(filePath);
		Image image = null;
        try
        {
			image = java.awt.Toolkit.getDefaultToolkit().createImage(imgURL);
			ImageUtil.loadImage(image, comp);
        }
        catch (Exception e)
        {
            hnLogger.homeNetLogger(e);
		}
		return image;

	}

    public Image loadImageFromUrl(URL httpURL, Component comp)
    {

        hnLogger.homeNetLogger("Image URL generated :" + httpURL.toString());
		Image image = null;
        try
        {
			image = java.awt.Toolkit.getDefaultToolkit().getImage(httpURL);
			MediaTracker track = new MediaTracker(comp);
			track.addImage(image, 0);

            try
            {
				track.waitForAll();
			}
            catch (InterruptedException e)
            {
            }
        }
        catch (Exception e)
        {
            hnLogger.homeNetLogger(e);
            hnLogger.homeNetLogger("Unable to load image: " + httpURL.toString());
		}
		return image;
	}

    public Image scaleImage(Image inputImage, Dimension size, Component component)
    {
        ReplicateScaleFilter filterScale = new ReplicateScaleFilter(size.width, size.height);
        ImageProducer imageProducer = new FilteredImageSource(inputImage.getSource(), filterScale);
		return component.createImage(imageProducer);

	}

    public static void loadImage(Image img, Component c)
    {
		MediaTracker track = new MediaTracker(c);
        try
        {
			track.addImage(img, 0);
			track.waitForID(0);
			track.removeImage(img);
        }
        catch (InterruptedException e)
        {
            hnLogger.homeNetLogger(e);
		}
	}
}
