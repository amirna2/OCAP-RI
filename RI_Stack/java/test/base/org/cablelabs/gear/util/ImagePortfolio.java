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

package org.cablelabs.gear.util;

import org.cablelabs.gear.data.GraphicData;
import org.cablelabs.gear.data.Locator;
import org.cablelabs.gear.data.PortfolioGraphic;
import org.cablelabs.impl.util.MPEEnv;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.MediaTracker;
import java.awt.Component;
import java.awt.image.ImageObserver;
import java.awt.Dimension;
import java.net.URL;
import java.util.Properties;
import java.util.Hashtable;
import java.util.Enumeration;

/**
 * The <code>ImagePortfolio</code> utility class simplifies the mundane tasks of
 * loading and caching images. It is built around a {@link MediaTracker} and a
 * {@link Hashtable}. It gives one the ability to load and cache images in a
 * central location.
 * <p>
 * Images can be added to the <code>ImagePortfolio</code> in a number of
 * different ways. This includes reading image information from a
 * {@link Properties} object.
 * 
 * @see org.cablelabs.gear.util.ImageScrapbook
 * @see java.awt.MediaTracker
 * @see java.util.Hashtable
 * 
 * @author Aaron Kamienski
 * @version $Revision: 1.18 $, $Date: 2002/06/03 21:31:06 $
 */
public class ImagePortfolio implements PicturePortfolio
{
    /********************* Constructors *********************/

    /**
     * Creates a <code>ImagePortfolio</code> with its own
     * <code>MediaTracker</code> and <code>ImageObserver</code>.
     */
    public ImagePortfolio()
    {
        iniz(null, new Component()
        {
        });
    }

    /**
     * Creates a <code>ImagePortfolio</code> with its own
     * <code>MediaTracker</code> and the given <code>ImageObserver</code>.
     * 
     * @param io
     *            the {@link ImageObserver} to associate with this
     *            <code>ImagePortfolio</code>.
     */
    public ImagePortfolio(Component io)
    {
        iniz(null, io);
    }

    /**
     * Creates a <code>ImagePortfolio</code> with its own
     * <code>ImageObserver</code> and the given <code>MediaTracker</code>.
     * 
     * @param mt
     *            the <code>MediaTracker</code> to use for this portfolio
     */
    public ImagePortfolio(MediaTracker mt)
    {
        iniz(mt, null);
    }

    /**
     * Common initialization across constructors.
     * 
     * @param mt
     *            the <code>MediaTracker</code> to use for this portfolio
     * @param io
     *            the {@link ImageObserver} to associate with this
     *            <code>ImagePortfolio</code>.
     */
    private void iniz(MediaTracker mt, Component io)
    {
        if (mt == null) mt = new MediaTracker(io);
        this.mt = mt;
        images = new Hashtable();
    }

    /******************* Add methods *******************/

    /**
     * Adds the {@link Image}s specified by the given <code>Properties</code>
     * object. The <code>Properties</code> object should map ids to image
     * sources (URLs, filenames, or resource names).
     * 
     * <p>
     * 
     * The properties should be specified as ...
     * 
     * <pre>
     * <i>image-name</i>=<i>url</i>
     * <i>image-name</i>=<i>filename</i>
     * <i>image-name</i>=<i>class</i>:<i>resource</i>
     * </pre>
     * 
     * The final specification (<i>class</i>:<i>resource</i>) is distinguished
     * from <i>url</i> and <i>filename</i> based on whether <i>class</i> is a
     * valid classname or the entire string is or is not a valid
     * <code>URL</code> or filename.
     * 
     * Image priorities can be specified using properties of the following
     * format:
     * 
     * <pre>
     * priority.<i>image-name</i>=<i>integer-priority</i>
     * </pre>
     * 
     * As a result, <code>priority.*</code> is not a valid image id. If no
     * priority is specified, then the given priority is used.
     * 
     * @param props
     *            the <code>Properties</code> object specifying id to image
     *            source mappings.
     * @param load
     *            the <code>ClassLoader</code> to use in loading the classes
     *            that might be specified in the properties; <code>null</code>
     *            if the default class loader should be used
     */
    public void addImage(Properties props, ClassLoader load, int priority)
    {
        Enumeration e = props.keys();
        while (e.hasMoreElements())
        {
            String key = (String) e.nextElement();
            if (key.startsWith("priority.")) continue;

            String value = props.getProperty(key);
            if (value != null)
            {
                int i;

                // Get priority for this image
                int currPriority = priority;
                String priorityValue = props.getProperty("priority." + key);
                if (priorityValue != null)
                {
                    try
                    {
                        currPriority = Integer.parseInt(priorityValue);
                    }
                    catch (NumberFormatException ignore)
                    {
                        // Not set correctly!
                    }
                }

                // Parse the string and perform the necessary addition
                // Figure out if it is URL?
                // Figure out if it is a class:resource?
                if ((i = value.indexOf(":")) > 0)
                {
                    // Try the URL first
                    try
                    {
                        URL url = new URL(value);
                        addImage(key, url, currPriority);
                        continue;
                    }
                    catch (Exception ignored)
                    {
                    }

                    // Try the class:resource next
                    try
                    {
                        String name = value.substring(0, i);
                        Class cl = (load == null) ? Class.forName(name) : load.loadClass(name);
                        addImage(key, cl.getResource(value.substring(i + 1)), currPriority);
                        continue;
                    }
                    catch (Exception ignored)
                    {
                    }
                }

                // Figure out if it is a filename (last resort?)
                addImage(key, value, currPriority);
            }
        }
    }

    /**
     * Adds the images specified by the given arrays. A priority of 0 is used
     * for each image.
     * 
     * @param ids
     *            the array of image ids to use in populating this
     *            <code>ImagePortfolio</code>.
     * @param images
     *            the array of Image's to use in populating this
     *            <code>ImagePortfolio</code>.
     * 
     * @throws ArrayIndexOutOfBoundsException
     *             if <code>ids.length != images.length</code>.
     * 
     * @see #addImage(String[], Image[], int)
     * @see #addImage(String, Image, int)
     */
    public void addImage(String ids[], Image images[])
    {
        addImage(ids, images, 0);
    }

    /**
     * Adds the images specified by the given arrays.
     * 
     * @param ids
     *            the array of image ids to use in populating this
     *            <code>ImagePortfolio</code>.
     * @param images
     *            the array of Image's to use in populating this
     *            <code>ImagePortfolio</code>.
     * @param priority
     *            the priority at which the given images should be loaded
     * 
     * @throws ArrayIndexOutOfBoundsException
     *             if <code>ids.length != images.length</code>.
     * 
     * @see #addImage(String, Image, int)
     */
    public void addImage(String ids[], Image images[], int priority)
    {
        if (ids.length != images.length) throw new ArrayIndexOutOfBoundsException("ids.length != images.length");

        for (int i = 0; i < ids.length; ++i)
            addImage(ids[i], images[i], priority);
    }

    /**
     * Adds the images specified by the given arrays. A priority of 0 is used
     * for each image.
     * 
     * @param ids
     *            the array of image ids to use in populating this
     *            <code>ImagePortfolio</code>.
     * @param images
     *            the array of <code>String</code>s which specify {@link Image}
     *            resource to use in populating this <code>ImagePortfolio</code>
     * @param base
     *            the <code>Class</code> object used as a base for loading
     *            resources
     * 
     * @throws ArrayIndexOutOfBoundsException
     *             if <code>ids.length != images.length</code>.
     * @throws NullPointerException
     *             if no such resource could be found
     * 
     * @see #addImage(String[], String[], Class, int)
     */
    public void addImage(String ids[], String images[], Class base)
    {
        addImage(ids, images, base, 0);
    }

    /**
     * Adds the images specified by the given arrays.
     * 
     * @param ids
     *            the array of image ids to use in populating this
     *            <code>ImagePortfolio</code>.
     * @param images
     *            the array of <code>String</code>s which specify {@link Image}
     *            resource to use in populating this <code>ImagePortfolio</code>
     * @param base
     *            the <code>Class</code> object used as a base for loading
     *            resources
     * @param priority
     *            the priority at which the given images should be loaded
     * 
     * @throws ArrayIndexOutOfBoundsException
     *             if <code>ids.length != images.length</code>.
     * @throws NullPointerException
     *             if no such resource could be found
     * 
     * @see #addImage(String, URL, int)
     */
    public void addImage(String ids[], String images[], Class base, int priority)
    {
        if (ids.length != images.length) throw new ArrayIndexOutOfBoundsException("ids.length != images.length");

        for (int i = 0; i < ids.length; ++i)
            addImage(ids[i], base.getResource(images[i]), priority);
    }

    /**
     * Adds the images specified by the given arrays. A priority of 0 is used
     * for each image.
     * 
     * @param ids
     *            the array of image ids to use in populating this
     *            <code>ImagePortfolio</code>.
     * @param images
     *            the array of <code>String</code>s which specify {@link Image}
     *            filenames to use in populating this
     *            <code>ImagePortfolio</code>
     * 
     * @throws ArrayIndexOutOfBoundsException
     *             if <code>ids.length != images.length</code>.
     * 
     * @see #addImage(String, String, int)
     */
    public void addImage(String ids[], String images[])
    {
        addImage(ids, images, 0);
    }

    /**
     * Adds the images specified by the given arrays.
     * 
     * @param ids
     *            the array of image ids to use in populating this
     *            <code>ImagePortfolio</code>.
     * @param images
     *            the array of <code>String</code>s which specify {@link Image}
     *            filenames to use in populating this
     *            <code>ImagePortfolio</code>
     * @param priority
     *            the priority at which the given images should be loaded
     * 
     * @throws ArrayIndexOutOfBoundsException
     *             if <code>ids.length != images.length</code>.
     * 
     * @see #addImage(String, String, int)
     */
    public void addImage(String ids[], String images[], int priority)
    {
        if (ids.length != images.length) throw new ArrayIndexOutOfBoundsException("ids.length != images.length");

        for (int i = 0; i < ids.length; ++i)
            addImage(ids[i], images[i], priority);
    }

    /**
     * Adds the images specified by the given arrays. A priority of 0 is used
     * for each image.
     * 
     * @param ids
     *            the array of image ids to use in populating this
     *            <code>ImagePortfolio</code>.
     * @param images
     *            the array of <code>URL</code>s which reference the
     *            {@link Image}s to use in populating this
     *            <code>ImagePortfolio</code>.
     * 
     * @throws ArrayIndexOutOfBoundsException
     *             if <code>ids.length != images.length</code>.
     * 
     * @see #addImage(String, URL, int)
     */
    public void addImage(String ids[], URL images[])
    {
        addImage(ids, images, 0);
    }

    /**
     * Adds the images specified by the given arrays.
     * 
     * @param ids
     *            the array of image ids to use in populating this
     *            <code>ImagePortfolio</code>.
     * @param images
     *            the array of <code>URL</code>s which reference the
     *            {@link Image}s to use in populating this
     *            <code>ImagePortfolio</code>.
     * @param priority
     *            the priority at which the given images should be loaded
     * 
     * @throws ArrayIndexOutOfBoundsException
     *             if <code>ids.length != images.length</code>.
     * 
     * @see #addImage(String, URL, int)
     */
    public void addImage(String ids[], URL images[], int priority)
    {
        if (ids.length != images.length) throw new ArrayIndexOutOfBoundsException("ids.length != images.length");

        for (int i = 0; i < ids.length; ++i)
            addImage(ids[i], images[i], priority);
    }

    /**
     * Adds a single Image specified by the given id to Image mapping.
     * 
     * @param id
     *            the image id
     * @param image
     *            the <code>Image</code>
     * @param priority
     *            the priority at which to load the image
     */
    public void addImage(String id, Image image, int priority)
    {
        images.put(id, new Structure(image, priority));
        mt.addImage(image, priority);
    }

    /**
     * Adds a single Image specified by the given id to Image filename mapping.
     * 
     * @param id
     *            the image id
     * @param image
     *            the {@link Image} filename
     * @param priority
     *            the priority at which to load the image
     */
    public void addImage(String id, String image, int priority)
    {
        Image i = createImage(image);
        addImage(id, i, priority);
    }

    /**
     * Adds a single Image specified by the given id to URL mapping.
     * 
     * @param id
     *            the image id
     * @param image
     *            the <code>URL</code> referencing an <code>Image</code>
     * @param priority
     *            the priority at which to load the image
     */
    public void addImage(String id, URL image, int priority)
    {
        Image i = createImage(image);
        addImage(id, i, priority);
    }

    /**
     * Removes the {@link Image} specified by the given id from the
     * <code>ImagePortfolio</code>.
     * 
     * @param id
     *            the image id
     */
    public void removeImage(String id)
    {
        Structure s = (Structure) images.remove(id);
        if (s != null && s.image != null) mt.removeImage(s.image);
    }

    /********************* PicturePortfolio *****************************/

    // Description copied from PicturePortfolio
    public void draw(Graphics g, String id, int x, int y, ImageObserver io)
    {
        Image i = getImageUnchecked(id);

        if (i != null) g.drawImage(i, x, y, io);
    }

    // Description copied from PicturePortfolio
    public void draw(Graphics g, String id, int x, int y, int width, int height, ImageObserver io)
    {
        Image i = getImageUnchecked(id);

        if (i != null) g.drawImage(i, x, y, width, height, io);
    }

    // Description copied from PicturePortfolio
    public GraphicData getPicture(String id)
    {
        return new PortfolioGraphic(this, id);
    }

    // Description copied from PicturePortfolio
    public Dimension getSize(String id)
    {
        try
        {
            Image i = getImage(id);
            return new Dimension(i.getWidth(null), i.getHeight(null));
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /**
     * Returns an <code>Image</code> of the requested <i>picture</i>. The
     * <code>Image</code> is the same as was stored with or created as a result
     * of an <code>addImage</code> call.
     * 
     * @param id
     *            the id of the <i>picture</i> to return an <code>Image</code>
     *            for.
     * @return an <code>Image</code> of the requested <i>picture</i>;
     *         <code>null</code> if no such <i>picture</i> exists
     * 
     * @throws Exception
     *             if it gets mad
     * @see #addImage(String, Image, int)
     * @see #addImage(String, String, int)
     * @see #addImage(String, URL, int)
     */
    public Image getImage(String id) throws Exception
    {
        checkLoad(id);
        return getImageUnchecked(id);
    }

    // Description copied from PicturePortfolio
    public int getSize()
    {
        return images.size();
    }

    // Description copied from PicturePortfolio
    public Enumeration getPictures()
    {
        return new DataEnumeration(images.keys());
    }

    // Description copied from PicturePortfolio
    public Enumeration getIDs()
    {
        return images.keys();
    }

    /******************* Additional methods *******************/

    /**
     * Returns the <code>MediaTracker</code> used by this
     * <code>ImagePortfolio</code>.
     * 
     * @return the <code>MediaTracker</code> used by this
     *         <code>ImagePortfolio</code>.
     */
    public MediaTracker getMediaTracker()
    {
        return mt;
    }

    /**
     * Returns the requested <code>Image</code>, or an alternate if errors were
     * encountered during loading.
     * 
     * @param id
     *            the <code>Image</code> identifier
     * @param alt
     *            the alternate <code>Image</code> identifier
     * @return the requested <code>Image</code> or its alternate
     * 
     * @throws Exception
     *             if it gets mad
     */
    public Image getImage(String id, String alt) throws Exception
    {
        try
        {
            return getImage(id);
        }
        catch (Exception e)
        {
            return getImage(alt);
        }
    }

    /**
     * Clears the <code>ImagePortfolio</code> of all {@link Image}s.
     */
    public void clear()
    {
        // Clears all images
        images.clear();
    }

    /**
     * Flushes all of the images' cache.
     */
    public void flush()
    {
        Enumeration e = images.elements();
        while (e.hasMoreElements())
            ((Structure) e.nextElement()).image.flush();
    }

    /**
     * Returns an <code>Image</code> of the requested <i>picture</i>. The
     * <code>Image</code> is the same as was stored with or created as a result
     * of an <code>addImage</code> call.
     * 
     * @param id
     *            the id of the <i>picture</i> to return an <code>Image</code>
     *            for.
     * @return an <code>Image</code> of the requested <i>picture</i>;
     *         <code>null</code> if no such <i>picture</i> exists
     */
    public Image getImageUnchecked(String id)
    {
        Structure s = (Structure) images.get(id);
        return s != null ? s.image : null;
    }

    /**
     * Checks whether the given image was loaded successfully or not. Blocks
     * until the image is loaded or an error occurs.
     * 
     * @param id
     *            the {@link Image} identifier
     */
    public void checkLoad(String id) throws Exception
    {
        // find image priority
        Structure s = (Structure) images.get(id);

        if (s != null)
        {
            final int priority = s.priority;

            // wait for load
            while (true)
            {
                try
                {
                    mt.waitForID(priority);
                }
                catch (InterruptedException e)
                {
                    continue;
                }
                break;
            }
            if (mt.isErrorID(priority))
            {
                Image img = s.image;
                Object[] errors = mt.getErrorsID(priority);

                // look for given image
                for (int i = 0; i < errors.length; ++i)
                    if (errors[i] == img) throw new Exception("Error loading image");
            }
        }
    }

    /**
     * Checks whether the images with the given priority were loaded
     * successfully or not. Blocks until the image is loaded or an error occurs.
     * 
     * @param id
     *            the priority of the images to check
     */
    public void checkLoad(int priority) throws Exception
    {
        // Wait for load
        while (true)
        {
            try
            {
                mt.waitForID(priority);
            }
            catch (InterruptedException ie)
            {
                continue;
            }
            break;
        }
        if (mt.isErrorID(priority)) throw new Exception("Error loading image priority " + priority);
    }

    /**
     * Checks whether <i>all</i> images currently managed by this this
     * <code>ImagePortfolio</code> have been loaded successfully or not. Blocks
     * until the image is loaded or an error occurs.
     */
    public void checkLoad() throws Exception
    {
        // Wait for load
        while (true)
        {
            try
            {
                mt.waitForAll();
            }
            catch (InterruptedException ie)
            {
                continue;
            }
            break;
        }
        if (mt.isErrorAny()) throw new Exception("Error loading images");
    }

    /********************* Utility methods ********************/

    /**
     * Utility method which creates an <code>Image</code> object given a
     * filename. On systems which support it, the <code>Image</code> is created
     * using the Java2 <code>Toolkit.createImage()</code> method as opposed to
     * the <code>Toolkit.getImage()</code> method.
     * 
     * @param filename
     *            the <code>Image</code> source filename
     */
    public static Image createImage(String filename)
    {
        if (filename == null) throw new NullPointerException("Invalid image filename");
        return tk.createImage(filename);
    }

    /**
     * Utility method which creates an <code>Image</code> object given a
     * <code>URL</code>. On systems which support it, the <code>Image</code> is
     * created using the Java2 <code>Toolkit.createImage()</code> method as
     * opposed to the <code>Toolkit.getImage()</code> method.
     * 
     * @param url
     *            the <code>Image</code> source <code>URL</code>
     */
    public static Image createImage(URL url)
    {
        if (url == null) throw new NullPointerException("Invalid image URL");
        return tk.createImage(url);
    }

    /**
     * Utility method used to fully load an {@link Image} from the given source.
     * The <code>Image</code> is created using the corresponding
     * {@link #createImage(URL) loadImage} method.
     * 
     * @param url
     *            the <code>Image</code> source <code>URL</code>
     */
    public static Image loadImage(URL url)
    {
        return loadFully(createImage(url));
    }

    /**
     * Utility method used to fully load an {@link Image} from the given source.
     * The <code>Image</code> is created using the corresponding
     * {@link #createImage(String) loadImage} method.
     * 
     * @param filename
     *            the <code>Image</code> source filename
     */
    public static Image loadImage(String filename)
    {
        return loadFully(createImage(filename));
    }

    /**
     * Utility method to fully-load an image, using the static MediaTracker.
     * 
     * @param img
     *            the <code>Image</code> to fully load
     * @return the given <code>Image</code> if it could be loaded;
     *         <code>null</code> if the <code>Image</code> could not be fully
     *         loaded.
     */
    private synchronized static Image loadFully(Image img)
    {
        if (img != null)
        {
            MediaTracker mt = getStaticTracker();

            mt.addImage(img, 0);
            try
            {
                mt.waitForID(0);
                if (mt.isErrorID(0)) return null;
            }
            catch (InterruptedException e)
            {
                return null;
            }
            finally
            {
                mt.removeImage(img, 0);
            }
        }

        return img;
    }

    /**
     * Method to allocate a MediaTracker for static uses.
     * 
     * @return a singleton MediaTracker
     */
    private synchronized static MediaTracker getStaticTracker()
    {
        if (staticTracker == null) staticTracker = new MediaTracker(new Component()
        {
        });

        return staticTracker;
    }

    /****************** Inner classes ************************/

    /**
     * Structure used to store image w/ priority in hashtable.
     */
    private class Structure
    {
        Image image;

        int priority;

        int error;

        public Structure(Image image, int priority)
        {
            this.image = image;
            this.priority = priority;
        }
    }

    /**
     * Implementation of an enumeration which gets the image.
     */
    private class DataEnumeration implements Enumeration
    {
        private Enumeration e;

        public DataEnumeration(Enumeration e)
        {
            this.e = e;
        }

        public boolean hasMoreElements()
        {
            return e.hasMoreElements();
        }

        public Object nextElement()
        {
            String id = (String) e.nextElement();
            return new PortfolioGraphic(ImagePortfolio.this, id);
        }
    }

    /**
     * Strategy object which represents how to create an <code>Image</code>.
     */
    private static interface MyToolkit
    {
        public Image createImage(URL url);

        public Image createImage(String filename);
    }

    /**
     * Pre-Java2 <code>Image</code> creation strategy.
     */
    private static class OldToolkit implements MyToolkit
    {
        public Image createImage(URL url)
        {
            return Toolkit.getDefaultToolkit().getImage(url);
        }

        public Image createImage(String filename)
        {
            return Toolkit.getDefaultToolkit().getImage(filename);
        }
    }

    /**
     * Java2 <code>Image</code> creation strategy.
     */
    private static class NewToolkit implements MyToolkit
    {
        public Image createImage(URL url)
        {
            return Toolkit.getDefaultToolkit().createImage(url);
        }

        public Image createImage(String filename)
        {
            return Toolkit.getDefaultToolkit().createImage(filename);
        }
    }

    private static MyToolkit tk;
    static
    {
        String javaVer = MPEEnv.getSystemProperty("java.version");
        if (javaVer.startsWith("1.0") || javaVer.startsWith("3.0") || javaVer.startsWith("1.1"))
            tk = new OldToolkit();
        else
            tk = new NewToolkit();
    }

    /********************* Attributes ************************/

    private Hashtable images;

    private MediaTracker mt;

    private Locator locator;

    static private MediaTracker staticTracker;
}
