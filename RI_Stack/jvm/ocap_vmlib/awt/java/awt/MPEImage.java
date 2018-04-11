/*
 * Copyright  1990-2008 Sun Microsystems, Inc. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License version
 * 2 only, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License version 2 for more details (a copy is
 * included at /legal/license.txt).
 *
 * You should have received a copy of the GNU General Public License
 * version 2 along with this work; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa
 * Clara, CA 95054 or visit www.sun.com if you need additional
 * information or have any questions.
 *
 */

package java.awt;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageConsumer;
import java.awt.image.ImageFilter;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;
import java.awt.image.IndexColorModel;
import java.awt.image.RasterFormatException;
import java.awt.image.ReplicateScaleFilter;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.Vector;

/**
 * MPE implementation of Image.
 *
 * @author Peter Stewart (MicroWindows version)
 * @author Aaron Kamienski (MPE PBP)
 *
 * Note: This is a renamed (and possibly modified) version of a phoneme file (MWImage.java)
 */
class MPEImage extends java.awt.Image implements ImageConsumer, sun.awt.image.BufferedImagePeer
{

    static final int REDRAW_COUNT = 20;

    /** MPE surface for this image. */
    MPESurface surface;

    MPEGraphicsConfiguration gc;

    int width, height;

    int status;

    private Hashtable properties;

    private Vector observers = new Vector();

    /**
     * The producer actually used and returned by getProducer. This may be a
     * scaled image producer if the image was prepared with a specified width
     * and height or it may be the original image producer if -1 was used for
     * the width and height to prepareImage.
     */
    ImageProducer producer;

    boolean started;

    private int scanlineCount;

    /**
     * Added for debugging Bug #902.
     */
    private int prevWidth, prevHeight, nFlush, nSetDim;

    static native void pDrawImage(int context, int surface, int x, int y, int bg);

    static private native void initIDs();

    static
    {
        Toolkit.getDefaultToolkit();

        initIDs();
    }

    /**
     * Creates an offscreen image of the specified width and height. This
     * constructor exists for the MPEOffscreenImage class which is a sub class
     * and should not be called directly.
     */
    MPEImage(java.awt.Component component, int width, int height, MPEGraphicsConfiguration gc)
    {
        this.width = width;
        this.height = height;
        status = ImageObserver.ALLBITS | ImageObserver.WIDTH | ImageObserver.HEIGHT;

        if (width > 0 && height > 0)
        {
            Color color;
            if (component == null || (color = component.getBackground()) == null) color = Color.black;

            this.gc = gc;
            surface = new MPESurface(width, height, gc, color);
        }
    }

    /**
     * Creates an image from the supplied image producer.
     */
    MPEImage(ImageProducer imageProd)
    {
        width = -1;
        height = -1;
        producer = imageProd;
        started = false;

        gc = (MPEGraphicsConfiguration) GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration();
    }

    /**
     * Creates a new MPEImage from an existing one. This constructor only exists
     * for MPESubImage class and should not be used for any other purpose.
     */
    MPEImage(MPEImage image)
    {
        surface = image.surface;
        gc = image.gc;
        status = ImageObserver.ALLBITS | ImageObserver.WIDTH | ImageObserver.HEIGHT;
        width = image.width;
        height = image.height;
    }

    protected void finalize()
    {
        // Simply forget the surface
        surface = null;
    }

    public Graphics getGraphics()
    {
        throw new UnsupportedOperationException(
                "Graphics can only be created for images created with Component.createImage(width, height)");
    }

    public int getWidth()
    {
        return width;
    }

    public int getWidth(ImageObserver observer)
    {
        if (width == -1)
        {
            addObserver(observer);
            startProduction();
        }

        return width;
    }

    public int getHeight()
    {
        return height;
    }

    public int getHeight(ImageObserver observer)
    {
        if (height == -1)
        {
            addObserver(observer);
            startProduction();
        }
        return height;
    }

    public Object getProperty(String name)
    {
        return getProperty(name, null);
    }

    public Object getProperty(String name, ImageObserver observer)
    {
        /*
         * 467980 getProperty checks if he properties hashtable exists. If so,
         * the prop is assigned the value from properties that relates to the
         * name specified. If no match is found, then the UndefinedProperty is
         * returned.
         *
         * addObserver is called if prop is null and the observer is null.
         */

        Object prop = null;

        if (properties != null)
        {
            prop = properties.get(name);
            if (prop == null)
            {
                prop = UndefinedProperty;
            }
        }

        if ((prop == null) && (observer != null))
        {
            addObserver(observer);
        }

        return prop;
    }

    public String[] getPropertyNames()
    {
        if (properties == null) return null;

        Object[] names = properties.keySet().toArray();
        String[] newNames = new String[names.length];

        System.arraycopy(names, 0, newNames, 0, newNames.length);

        return newNames;
    }

    int getStatus(ImageObserver observer)
    {

        if (observer != null)
        {
            if (observer.imageUpdate(this, status, 0, 0, width, height)) addObserver(observer);
        }

        return status;
    }

    public BufferedImage getSubimage(int x, int y, int w, int h)
    {
        if (x < 0) throw new RasterFormatException("x is outside image");
        if (y < 0) throw new RasterFormatException("y is outside image");
        if (x + w > width) throw new RasterFormatException("(x + width) is outside image");
        if (y + h > height) throw new RasterFormatException("(y + height) is outside image");

        return gc.createBufferedImageObject(new MPESubimage(this, x, y, w, h));

    }

    synchronized boolean prepareImage(int width, int height, ImageObserver observer)
    {
        if (width == 0 || height == 0)
        {
            if ((observer != null) && observer.imageUpdate(this, ImageObserver.ALLBITS, 0, 0, 0, 0))
                addObserver(observer);
            return true;
        }

        if (hasError())
        {
            if ((observer != null)
                    && observer.imageUpdate(this, ImageObserver.ERROR | ImageObserver.ABORT, -1, -1, -1, -1))
                addObserver(observer);
            return false;
        }

        if (started)
        {

            if ((observer != null) && observer.imageUpdate(this, status, 0, 0, width, height)) addObserver(observer);

            return ((status & ImageObserver.ALLBITS) != 0);
        }
        else
        {
            addObserver(observer);
            startProduction();
        }

        // Some producers deliver image data synchronously
        return ((status & ImageObserver.ALLBITS) != 0);

    }

    synchronized void addObserver(ImageObserver observer)
    {
        if (isComplete())
        {
            if (observer != null)
            {
                observer.imageUpdate(this, status, 0, 0, width, height);
            }
            return;
        }

        if (observer != null && !isObserver(observer))
        {
            observers.addElement(observer);
        }
    }

    private boolean isObserver(ImageObserver observer)
    {
        return (observer != null && observers.contains(observer));
    }

    private synchronized void removeObserver(ImageObserver observer)
    {
        if (observer != null)
        {
            observers.removeElement(observer);
        }
    }

    private synchronized void notifyObservers(final Image img, final int info, final int x, final int y, final int w,
            final int h)
    {
        Enumeration e = observers.elements();
        Vector uninterested = null;
        while (e.hasMoreElements())
        {
            ImageObserver observer;
            try
            {
                observer = (ImageObserver) e.nextElement();
            }
            catch (NoSuchElementException ex)
            {
                break;
            }

            if (!observer.imageUpdate(img, info, x, y, w, h))
            {
                if (uninterested == null)
                {
                    uninterested = new Vector();
                }
                uninterested.addElement(observer);
            }
        }
        if (uninterested != null)
        {
            e = uninterested.elements();
            while (e.hasMoreElements())
            {
                ImageObserver observer = (ImageObserver) e.nextElement();
                removeObserver(observer);
            }
        }
    }

    synchronized void startProduction()
    {
        if (producer != null && !started)
        {

            if (!producer.isConsumer(this))
            {
                producer.addConsumer(this);
            }
            started = true;
            producer.startProduction(this);
        }
    }

    public void flush()
    {
        // Bug 902: debug info
        ++nFlush;
        prevWidth = width;
        prevHeight = height;

        MPEToolkit.clearCache(this);
        producer.removeConsumer(this);

        synchronized (this)
        {
            status = 0;
            started = false;

            // forget the current surface, let finalization clean it up
            surface = null;

            width = -1;
            height = -1;
        }

        if (producer instanceof sun.awt.image.InputStreamImageSource)
        {
            ((sun.awt.image.InputStreamImageSource) producer).flush();
        }
    }

    public ImageProducer getSource()
    {
        return producer;
    }

    void drawImage(int context, int x, int y, Color bg)
    {
        pDrawImage(context, surface.getPeer(), x, y, (bg == null) ? 0 : bg.getRGB());
    }

    private static native void pDrawImageScaled(int context, int dx1, int dy1, int dx2, int dy2, int surface, int sx1,
            int sy1, int sx2, int sy2, int bg);

    void drawImage(int context, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, Color bg)
    {
        pDrawImageScaled(context, dx1, dy1, dx2, dy2, surface.getPeer(), sx1, sy1, sx2, sy2, (bg == null) ? 0
                : bg.getRGB());
    }

    boolean isComplete()
    {
        return ((status & (ImageObserver.ALLBITS | ImageObserver.ERROR | ImageObserver.ABORT)) != 0);
    }

    boolean hasError()
    {
        return ((status & ImageObserver.ERROR) != 0);
    }

    /***** --- Consumer Stuff --- *****/

    public void imageComplete(int stat)
    {
        switch (stat)
        {
            case STATICIMAGEDONE:
                status = ImageObserver.ALLBITS;
                break;

            case SINGLEFRAMEDONE:
                status = ImageObserver.FRAMEBITS;
                break;

            case IMAGEERROR:
                status = ImageObserver.ERROR;
                break;

            case IMAGEABORTED:
                status = ImageObserver.ABORT;
                break;
        }

        if (status != 0) notifyObservers(this, status, 0, 0, width, height);
        if (isComplete()) producer.removeConsumer(this);
    }

    public void setColorModel(ColorModel cm)
    {
    }

    public synchronized void setDimensions(int width, int height)
    {
        if ((width > 0) && (height > 0))
        {
            // Bug 902 - to determine if setDimensions was previously called
            ++nSetDim;
            prevWidth = this.width;
            prevHeight = this.height;

            this.width = width;
            this.height = height;

            // forget the current surface
            // replace with a new one
            surface = new MPESurface(width, height, gc);
            status = ImageObserver.WIDTH | ImageObserver.HEIGHT;
            notifyObservers(this, status, 0, 0, width, height);
        }
        else
        {
            // Added for Bug 902 to see if called with width or height of 0
            throw new java.awt.AWTError("setDimensions(" + width + "," + height + ") " + this);
        }
    }

    public void setProperties(Hashtable props)
    {
        properties = props;
    }

    public void setHints(int hints)
    {
        /*
         * System.out.println("ImageHints:"); if((hints&RANDOMPIXELORDER) != 0)
         * System.out.println("Hints: random order");
         * if((hints&TOPDOWNLEFTRIGHT) != 0)
         * System.out.println("Hints: top down"); if((hints&COMPLETESCANLINES)
         * != 0) System.out.println("Hints: complete scan lines");
         * if((hints&SINGLEPASS) != 0) System.out.println("Hints: single pass");
         * if((hints&SINGLEFRAME) != 0)
         * System.out.println("Hints: single frame");
         */
    }

    /**
     * Unaccelerated native function for setting pixels in the image from any
     * kind of ColorModel.
     */
    private static native void pSetColorModelBytePixels(int surface, int x, int y, int w, int h, ColorModel cm,
            byte[] pixels, int offset, int scansize);

    /**
     * Unaccelerated native function for setting pixels in the image from any
     * kind of ColorModel.
     */
    private static native void pSetColorModelIntPixels(int surface, int x, int y, int w, int h, ColorModel cm,
            int[] pixels, int offset, int scansize);

    /**
     * Accelerated native function for setting pixels in the image when the
     * ColorModel is an IndexColorModel.
     */
    private static native void pSetIndexColorModelBytePixels(int surface, int x, int y, int w, int h, ColorModel cm,
            byte[] pixels, int offset, int scansize);

    /**
     * Accelerated native function for setting pixels in the image when the
     * ColorModel is an IndexColorModel.
     */
    private static native void pSetIndexColorModelIntPixels(int surface, int x, int y, int w, int h, ColorModel cm,
            int[] pixels, int offset, int scansize);

    /**
     * Accelerated native function for setting pixels in the image when the
     * ColorModel is a DirectColorModel.
     */
    private static native void pSetDirectColorModelPixels(int surface, int x, int y, int w, int h, ColorModel cm,
            int[] pixels, int offset, int scansize);

    /** Gets the ARGB color value at the supplied location. */
    private static native int pGetRGB(int surface, int x, int y);

    /* Gets an area of ARGB values and stores them in the array. */
    private native void pGetRGBArray(int surface, int x, int y, int w, int h, int[] pixels, int off, int scansize);

    /* Sets the pixel at the supplied location to an ARGB value. */
    private static native void pSetRGB(int surface, int x, int y, int rgb);

    public void setPixels(int x, int y, int w, int h, ColorModel cm, byte[] pixels, int off, int scansize)
    {
        checkBounds(x, y, w, h, pixels.length, off, scansize);

        // Use accelerated set pixels routine if possible

        int peer = surface.getPeer();
        if (cm instanceof IndexColorModel)
            pSetIndexColorModelBytePixels(peer, x, y, w, h, cm, pixels, off, scansize);
        else
            pSetColorModelBytePixels(peer, x, y, w, h, cm, pixels, off, scansize);

        scanlineCount++;

        status = ImageObserver.SOMEBITS;

        if (scanlineCount % REDRAW_COUNT == 0)
        {
            notifyObservers(this, ImageObserver.SOMEBITS, x, y, w, h);
        }

        // System.out.println("SetPixelsByte " + new Rectangle(x, y, w, h));

    }

    private void checkBounds(int x, int y, int w, int h, int arrayLength, int offset, int scansize)
    {
        // Check image bounds
        if ((x < 0) || (y < 0) || (w < 0) || (h < 0) || ((x + w) > width) || ((y + h) > height))
            throw new ArrayIndexOutOfBoundsException("x=" + x + " y=" + y + " w=" + w + " h=" + h + ": " + this);
        // Check array bounds
        else if (offset < 0 || scansize < 0)
            throw new ArrayIndexOutOfBoundsException("offset=" + offset + " scansize=" + scansize + ": " + this);
        else if (scansize != 0 && ((h - 1) > (arrayLength - (w) - offset) / scansize))
            // else if ((offset + (h-1)*scansize + w-1) > arrayLength)
            throw new ArrayIndexOutOfBoundsException("h=" + h + " offset=" + offset + " length=" + arrayLength
                    + " scansize=" + scansize + ": " + this);
        else if (scansize == 0 && (arrayLength - (w) - offset) < 0)
            throw new ArrayIndexOutOfBoundsException("h=" + h + " offset=" + offset + " length=" + arrayLength + ": "
                    + this);
    }

    public void setPixels(int x, int y, int w, int h, ColorModel cm, int[] pixels, int off, int scansize)
    {
        // Use accelerated set pixels routine if possible
        checkBounds(x, y, w, h, pixels.length, off, scansize);

        int peer = surface.getPeer();
        if (cm instanceof DirectColorModel)
            pSetDirectColorModelPixels(peer, x, y, w, h, cm, pixels, off, scansize);
        else if (cm instanceof IndexColorModel)
            pSetIndexColorModelIntPixels(peer, x, y, w, h, cm, pixels, off, scansize);
        else
            pSetColorModelIntPixels(peer, x, y, w, h, cm, pixels, off, scansize);

        scanlineCount++;

        status = ImageObserver.SOMEBITS;

        if (scanlineCount % REDRAW_COUNT == 0)
        {
            notifyObservers(this, ImageObserver.SOMEBITS, x, y, w, h);
        }

        // System.out.println("SetPixelsInt " + new Rectangle(x, y, w, h));
    }

    public int getType()
    {
        return gc.getCompatibleImageType();
    }

    public ColorModel getColorModel()
    {
        return gc.getColorModel();
    }

    public synchronized void setRGB(int x, int y, int rgb)
    {
        // Check image bounds
        if ((x < 0) || (y < 0) || (x >= width) || (y >= height))
            throw new java.lang.ArrayIndexOutOfBoundsException(x + y * width + ": " + this);

        pSetRGB(surface.getPeer(), x, y, rgb);
    }

    public void setRGB(int x, int y, int w, int h, int[] rgbArray, int offset, int scansize)
    {
        checkBounds(x, y, w, h, rgbArray.length, offset, scansize);

        pSetDirectColorModelPixels(surface.getPeer(), x, y, w, h, ColorModel.getRGBdefault(), rgbArray, offset,
                scansize);
    }

    public int getRGB(int x, int y)
    {
        if ((x < 0) || (y < 0) || (x >= width) || (y >= height))
            throw new java.lang.ArrayIndexOutOfBoundsException(x + y * width + ": " + this);

        return pGetRGB(surface.getPeer(), x, y);
    }

    public int[] getRGB(int x, int y, int w, int h, int[] rgbArray, int offset, int scansize)
    {
        int yoff = offset;
        int off;

        if ((x < 0) || (y < 0) || ((x + w) > width) || ((y + h) > height))
            throw new java.lang.ArrayIndexOutOfBoundsException(x + y * width + ": " + this);
        else if (offset < 0) throw new java.lang.ArrayIndexOutOfBoundsException("offset=" + offset + ": " + this);

        if (rgbArray == null)
            rgbArray = new int[offset + h * scansize];
        else if (rgbArray.length < offset + h * scansize)
            throw new IllegalArgumentException("rgbArray is not large enough to store all the values: " + this);

        pGetRGBArray(surface.getPeer(), x, y, w, h, rgbArray, offset, scansize);

        return rgbArray;
    }

    public String toString()
    {
        return "[surface=" + surface + ",width=" + width + ",height=" + height + ",status=" + status + ",producer="
                + producer + ",started=" + started + ",flushes=" + nFlush + ",setDims=" + nSetDim + ",old=("
                + prevWidth + "x" + prevHeight + ")" + "]";
    }
}
