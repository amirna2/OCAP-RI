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

import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.image.PixelGrabber;
import java.awt.image.MemoryImageSource;
import java.awt.image.ImageObserver;
import java.awt.Graphics;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.net.URL;
import java.io.InputStream;
import java.io.IOException;
import org.cablelabs.gear.data.ImageGraphic;

/**
 * The <code>ImageScrapbook</code> class allows one to easily use image scraps
 * combined into a single Image object. Each of these image scraps are specified
 * by a <code>String</code>/<code>Rectangle</code> pair which defines the
 * mapping from ID to an area on the {@link Image} that defines the scrap. The
 * scraps can then be drawn to a graphics context without having to extract a
 * separate image.
 * <p>
 * The <code>ImageScrapbook</code> provides a method for extracting an
 * individual scrap as a separate <code>Image</code> if necessary.
 * <p>
 * When creating an <code>ImageScrapbook</code> one should provide a set of
 * mappings from ID to scrap area. This can be done many different ways.
 * However, the most flexible is to use a property file. For example, given the
 * file "scraps.ini":
 * 
 * <pre>
 * pic1=0,0,50,50
 * pic2=0,50,50,50
 * pic3=50,0,50,50
 * pic4=50,50,50,50
 * </pre>
 * 
 * We define four scraps which carve up a (at least) <i>100x100</i>
 * <code>Image</code>. The <code>ImageScrapbook</code> could be defined and used
 * like so:
 * 
 * <pre>
 *   Properties props = new Properties();
 *   props.load(new FileInputStream(new File("scraps.properties")));
 * 
 *   ImageScrapbook scraps = new ImageScrapbook(props);
 *   ...
 *   scraps.draw(component.getGraphics(), "pic1", x, y, component);
 * </pre>
 * 
 * @author Aaron Kamienski
 * @version $Revision: 1.11 $, $Date: 2002/08/30 18:34:06 $
 */
public class ImageScrapbook implements PicturePortfolio
{
    /**
     * Default constructor.
     * 
     * @see #setImage(Image)
     */
    public ImageScrapbook()
    {
    }

    /**
     * Creates an ImageScrapbook using the given source image. No scraps are
     * defined.
     * 
     * @param image
     *            the source <code>Image</code>.
     */
    public ImageScrapbook(Image image)
    {
        this.image = image;
    }

    /**
     * Creates an ImageScrapbook using the given source image and scrap
     * ids/rectangles.
     * 
     * @param image
     *            the source <code>Image</code>.
     * @param ids
     *            the array of scrap ids to use in populating this
     *            <code>ImageScrapbook</code>.
     * @param scraps
     *            the array of scrap rectangle's to use in populating this
     *            <code>ImageScrapbook</code>.
     * 
     * @throws ArrayIndexOutOfBoundsException
     *             if <code>ids.length != scraps.length</code>.
     * 
     * @see #addScrap(String[],Rectangle[])
     */
    public ImageScrapbook(Image image, String ids[], Rectangle scraps[])
    {
        this(image);
        addScrap(ids, scraps);
    }

    /**
     * Creates an ImageScrapbook using the given source image and
     * <code>Properties</code> object specifying scrap id to rectangle mappings.
     * 
     * @param image
     *            the source <code>Image</code>.
     * @param props
     *            the <code>Properties</code> object specifying scrap id to
     *            rectangle mappings.
     * 
     * @see #addScrap(Properties)
     */
    public ImageScrapbook(Image image, Properties props)
    {
        this(image);
        addScrap(props);
    }

    /**
     * Sets the <code>Image</code> upon which all <i>scraps</i> are based. This
     * should generally not be needed unless the {@link #ImageScrapbook()
     * default} constructor is used.
     * 
     * @param image
     *            the <code>Image</code> upon which all <i>scraps</i> are based
     * @see #ImageScrapbook()
     */
    public void setImage(Image image)
    {
        this.image = image;
    }

    /**
     * Adds the scraps specified by the given <code>Properties</code> object.
     * The <code>Properties</code> object should map scrap ids to rectangles.
     * <p>
     * The property values should be specified as a comma-delimited set of 4
     * integers specifying x-, and y-coordinates, width, and height. For
     * example: <code>20,30,100,5</code> specifies a scrap at x/y-coordinates
     * <i>(20,30)</i> with width/height of <i>100x5</i>.
     * 
     * @param props
     *            the <code>Properties</code> object specifying scrap id to
     *            rectangle mappings.
     */
    public void addScrap(Properties props)
    {
        Enumeration e = props.keys();
        while (e.hasMoreElements())
        {
            String key = (String) e.nextElement();
            String value = props.getProperty(key);

            if (value != null)
            {
                StringTokenizer tok = new StringTokenizer(value, ",");

                if (tok.countTokens() == 4)
                {
                    try
                    {
                        int x = Integer.parseInt(tok.nextToken());
                        int y = Integer.parseInt(tok.nextToken());
                        int width = Integer.parseInt(tok.nextToken());
                        int height = Integer.parseInt(tok.nextToken());

                        addScrap(key, new Rectangle(x, y, width, height));
                    }
                    catch (NumberFormatException ignored)
                    {
                    }
                }
            }
        }
    }

    /**
     * Adds the scraps specified by the given arrays.
     * 
     * @param ids
     *            the array of scrap ids to use in populating this
     *            <code>ImageScrapbook</code>.
     * @param scraps
     *            the array of scrap rectangle's to use in populating this
     *            <code>ImageScrapbook</code>.
     * 
     * @throws ArrayIndexOutOfBoundsException
     *             if <code>ids.length != scraps.length</code>.
     */
    public void addScrap(String ids[], Rectangle scraps[])
    {
        if (ids.length != scraps.length) throw new ArrayIndexOutOfBoundsException("ids.length != scraps.length");

        for (int i = 0; i < ids.length; ++i)
            addScrap(ids[i], scraps[i]);
    }

    /**
     * Adds a single scrap specified by the given id to rectangle mapping.
     * 
     * @param id
     *            the scrap id
     * @param scrap
     *            the scrap <code>Rectangle</code>
     */
    public void addScrap(String id, Rectangle scrap)
    {
        scrapMap.put(id, scrap);
    }

    /**
     * Removes a the scrap specified by the given id.
     * 
     * @param id
     *            the scrap id
     */
    public void removeScrap(String id)
    {
        scrapMap.remove(id);
    }

    /**
     * Removes all scraps from the scrapbook.
     */
    public void removeAll()
    {
        scrapMap.clear();
    }

    /**
     * Calls <code>Image.flush()</code> on the source image.
     */
    public void flush()
    {
        image.flush();
    }

    /**
     * Maps a scrap id to the scrap rectangle. Used internally, but may be
     * called externally if desired.
     * <p>
     * <b>Note:</b> this method could be overridden in a subclass to dynamically
     * map a scrap id to a scrap <code>Rectangle</code>. E.g., to hard-code
     * mappings and not require one to go through the <i>hassle</i> of calling
     * <code>addScrap()</code>.
     * 
     * @param the
     *            scrap id to map
     * @return the scrap <code>Rectangle</code> for the given scrap id;
     *         <code>null</code> if no such scrap exists.
     */
    public Rectangle getScrap(String id)
    {
        return (Rectangle) scrapMap.get(id);
    }

    // Description copied from PicturePortfolio
    public GraphicData getPicture(String id)
    {
        return new PortfolioGraphic(this, id);
    }

    // Description copied from PicturePortfolio
    public Dimension getSize(String id)
    {
        Rectangle r = getScrap(id);

        return (r == null) ? null : r.getSize();
    }

    // Description copied from PicturePortfolio
    public void draw(Graphics g, String id, int x, int y, ImageObserver io)
    {
        Rectangle r = getScrap(id);

        if (r != null && r.width > 0 && r.height > 0)
        {
            // Src/dest width/height the same -> no scaling
            g.drawImage(image, x, y, x + r.width, y + r.height, // dest rect
                    r.x, r.y, r.x + r.width, r.y + r.height, // src rect
                    io);
        }
    }

    // Description copied from PicturePortfolio
    public void draw(Graphics g, String id, int x, int y, int width, int height, ImageObserver io)
    {
        Rectangle r = getScrap(id);

        if (r != null && r.width > 0 && r.height > 0)
        {
            g.drawImage(image, x, y, x + width, y + height, // dest rectangle
                    r.x, r.y, r.x + r.width, r.y + r.height, // src rectangle
                    io);
        }
    }

    /**
     * Returns the base <code>Image</code> associated with this
     * <code>ImageScrapbook</code>.
     * 
     * @return the base <code>Image</code> associated with this
     *         <code>ImageScrapbook</code>.
     */
    public Image getImage()
    {
        return image;
    }

    /**
     * Creates a new <code>Image</code> object from the specified scrap.
     * 
     * @param id
     *            the id of the image scrap to generate an <code>Image</code>
     *            for.
     * @return an <code>Image</code> created from a scrap of the
     *         <code>ImageScrapbook</code> main <code>Image</code>;
     *         <code>null</code> if no such scrap exists.
     */
    public Image getImage(String id) throws InterruptedException, ImageException
    {
        Image img = null;

        Rectangle r = getScrap(id);
        if (r == null) return null;

        // First, extract the pixels from the source Image
        int pixels[] = new int[r.width * r.height];
        PixelGrabber pg = new PixelGrabber(image, r.x, r.y, r.width, r.height, pixels, 0, r.width);

        if (!pg.grabPixels())
            throw new ImageException("Could not grab Image pixels", pg.getStatus());
        else
        {
            // Then, create and return a new Image
            Toolkit tk = Toolkit.getDefaultToolkit();
            img = tk.createImage(new MemoryImageSource(r.width, r.height, pixels, 0, r.width));
        }

        return (img);
    }

    // Description copied from PicturePortfolio
    public int getSize()
    {
        return scrapMap.size();
    }

    // Description copied from PicturePortfolio
    public Enumeration getPictures()
    {
        return new DataEnumeration(scrapMap.keys());
    }

    // Description copied from PicturePortfolio
    public Enumeration getIDs()
    {
        return scrapMap.keys();
    }

    /**
     * Exception potentially thrown by <code>getImage(String)</code> if pixels
     * could not be extracted for the given <code>Image</code> scrap.
     */
    public static class ImageException extends Exception
    {
        public ImageException(String msg, int status)
        {
            super(msg);
            this.status = status;
        }

        /**
         * Returns the <code>ImageObserver</code> flags that caused the problem.
         */
        public int getStatus()
        {
            return status;
        }

        private int status;
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
            return new PortfolioGraphic(ImageScrapbook.this, id);
        }
    }

    // ********* Properties Exposed For Use Within An IDE **********

    /**
     * This method provides a simple way load a set of id/scrap mappings at once
     * using an external file. The file specified by {@link Locator locator} is
     * loaded and parsed to retrieve all of the id/scrap mappings to be used in
     * the <code>ImageScrapbook</code>.
     * 
     * The file is assumed to be in a format that can be parsed by a
     * <code>Properties</code> object.
     * 
     * Note: This will override any previously set id/scrap mappings.
     * 
     * @param locator
     *            the <code>Locator</code> specifying the location of the
     *            properties file.
     * 
     * @see #getPropertiesFile()
     */
    public void setPropertiesFile(Locator locator)
    {
        this.locator = locator;

        if (locator != null)
        {

            try
            {
                URL url = locator.getLocation();

                if (url != null)
                {
                    InputStream in = url.openStream();

                    if (in != null)
                    {
                        Properties properties = new Properties();

                        // load properties file
                        properties.load(in);

                        // if the load was successful, remove all previously
                        // registered scraps
                        removeAll();

                        // add new scraps defined in properties file.
                        addScrap(properties);
                    }
                }
            }
            catch (IOException e)
            {
            }
        }
    }

    /**
     * Retrieves the previously set {@link Locator} which specifies the file
     * containing the id/scrap mappings used in the <code>ImageScrapbook</code>.
     * 
     * @return the <code>Locator</code> specifying the location of the
     *         properties file.
     * 
     * @see #setPropertiesFile(Locator)
     */
    public Locator getPropertiesFile()
    {
        return (locator);
    }

    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    // This is unfortunate: it is incongruous.
    // Perhaps instead of being based on an Image... we should have
    // a GraphicScrapbook based on a GraphicData?
    // One problem is that GraphicData does not expose the
    // srcRect/destRect method needed to scale a scrap.
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    /**
     * Sets the <code>Image</code> retrieved from the supplied
     * <code>GraphicData</code> object. The <code>GraphiData</code> is saved in
     * case of further editing by a visual development environment.
     * 
     * @param data
     *            the <code>GraphicData</code> object to retrieve the
     *            <code>Image</code> from
     */
    public void setGraphicData(GraphicData data)
    {
        graphic = data;

        if (data instanceof ImageGraphic)
        {
            setImage(((ImageGraphic) data).getImage());
        }
        else if (data != null)
        {
            // create a dummy component
            Component component = new Component()
            {
            };

            Image temp = component.createImage(data.getWidth(), data.getHeight());
            Graphics g = temp.getGraphics();
            data.draw(g, 0, 0, component);
            setImage(temp);
        }
        else
            setImage(null);
    }

    /**
     * Retrieves the previously set <code>GraphicData</code> object
     * 
     * @return the previously set <code>GraphicData</code>
     */
    public GraphicData getGraphicData()
    {
        if (graphic == null && image != null) graphic = new ImageGraphic(image);
        return graphic;
    }

    /** The source image from which all scraps will be taken. */
    private Image image;

    /** The mapping of scrap ids to rectangles. */
    private Hashtable scrapMap = new Hashtable();

    /**
     * A <code>GraphicData</code> object which specifies the main image for this
     * scrapbook
     */
    private GraphicData graphic;

    /**
     * A <code>Locator</code> specifying the location of the scraps file used to
     * initialize this scrapbook.
     */
    private Locator locator;
}
