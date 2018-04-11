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

import java.awt.image.ColorModel;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;
import java.awt.event.InvocationEvent;
import java.awt.event.KeyEvent;

import java.net.URL;

import java.util.EmptyStackException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import java.io.File;

import sun.awt.image.ByteArrayImageSource;
import sun.awt.image.FileImageSource;
import sun.awt.image.URLImageSource;

import org.cablelabs.impl.awt.GraphicsAdaptable;
import org.cablelabs.impl.awt.GraphicsFactory;
import org.cablelabs.impl.awt.EventDispatchable;
import org.cablelabs.impl.awt.EventDispatcher;
import org.cablelabs.impl.dvb.ui.FontFactoryPeerFactory;

/**
 * The toolkit used by this AWT implementation based on the MPE library.
 *
 * @author Nicholas Allen (MicroWindows)
 * @author Peter Stewart (MicroWindows)
 * @author Aaron Kamienski (MPE)
 *
 * Note: This is a renamed (and possibly modified) version of a phoneme file (MWToolkit.java)
 */
class MPEToolkit extends Toolkit implements GraphicsAdaptable, EventDispatchable
{
    private EventQueue eventQueue = new EventQueue();

    private MPEGraphicsEnvironment localEnv = new MPEGraphicsEnvironment();

    private MPEGraphicsConfiguration defaultGC = (MPEGraphicsConfiguration) localEnv.getDefaultScreenDevice()
            .getDefaultConfiguration();

    private static Hashtable cachedImages = new Hashtable();

    private GraphicsFactory gf = new NullFactory();

    private EventDispatcher dispatcher = null;

    public MPEToolkit()
    {
        // Setup our DVB font factory peer
        FontFactoryPeerFactory.setFontFactoryPeer("org.cablelabs.impl.dvb.ui.MPEFontFactoryPeer");
    }

    /**
     * Gets the size of the screen.
     *
     * @return the size of this toolkit's screen, in pixels.
     * @since JDK1.0
     */
    public Dimension getScreenSize()
    {
        Rectangle dims = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration()
                .getBounds();

        return new Dimension(dims.width, dims.height);
    }

    /**
     * This method is invoked from the Toolkit.getGraphics(Window) native
     * method.
     */
    Graphics getGraphics(Window window)
    {
        return wrapGraphics(new MPEGraphics(window));
    }

    /**
     * This method is invoked from the Toolkit.getLocalGraphicsEnvironment
     * native method.
     */
    GraphicsEnvironment getLocalGraphicsEnvironment()
    {
        return localEnv;
    }

    /**
     * Returns the screen resolution in dots-per-inch.
     *
     * @return this toolkit's screen resolution, in dots-per-inch.
     * @since JDK1.0
     */
    public int getScreenResolution()
    {
        return 72;
    }

    /**
     * Determines the color model of this toolkit's screen.
     * <p>
     * <code>ColorModel</code> is an class that encapsulates the ability to
     * translate between the pixel values of an image and its red, green, blue,
     * and alpha components.
     * <p>
     * This toolkit method is called by the <code>getColorModel</code> method of
     * the <code>Component</code> class.
     *
     * @return the color model of this toolkit's screen.
     * @see java.awt.image.ColorModel
     * @see java.awt.Component#getColorModel
     * @since JDK1.0
     */
    public ColorModel getColorModel()
    {
        return GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration()
                .getColorModel();
    }

    /**
     * Returns the names of the available fonts in this toolkit.
     * <p>
     * For 1.1, the following font names are deprecated (the replacement name
     * follows):
     * <ul>
     * <li>TimesRoman (use Serif)
     * <li>Helvetica (use SansSerif)
     * <li>Courier (use Monospaced)
     * </ul>
     * <p>
     * The ZapfDingbats font is also deprecated in 1.1, but only as a separate
     * fontname. Unicode defines the ZapfDingbat characters starting at \u2700,
     * and as of 1.1 Java supports those characters.
     *
     * @return the names of the available fonts in this toolkit.
     * @since JDK1.0
     */
    public String[] getFontList()
    {
        return MPEFontMetrics.getFontList();
    }

    /**
     * Gets the screen metrics of the font.
     *
     * @param font
     *            a font.
     * @return the screen metrics of the specified font in this toolkit.
     * @since JDK1.0
     */
    public FontMetrics getFontMetrics(Font font)
    {
        return MPEFontMetrics.getFontMetrics(font);
    }

    /**
     * Synchronizes this toolkit's graphics state. Some window systems may do
     * buffering of graphics events.
     * <p>
     * This method ensures that the display is up-to-date. It is useful for
     * animation.
     *
     * @since JDK1.0
     */
    public void sync()
    {
        localEnv.sync();
    }

    static void clearCache(MPEImage image)
    {
        synchronized (cachedImages)
        {
            Iterator i = cachedImages.entrySet().iterator();

            while (i.hasNext())
            {
                Map.Entry entry = (Map.Entry) i.next();

                if (entry.getValue() == image)
                {
                    i.remove();
                    return;
                }
            }
        }
    }

    /**
     * Returns an image which gets pixel data from the specified file. The
     * underlying toolkit attempts to resolve multiple requests with the same
     * filename to the same returned Image. Since the mechanism required to
     * facilitate this sharing of Image objects may continue to hold onto images
     * that are no longer of use for an indefinite period of time, developers
     * are encouraged to implement their own caching of images by using the
     * createImage variant wherever available. <h3>Compatibility</h3>
     * PersonalJava does not require support of the PNG image file format.
     *
     * @param filename
     *            Filename must reference an image format that is recognized by
     *            this toolkit. The toolkit must be able to create images from
     *            the following image file formats: GIF, JPEG(JFIF), XBM, and
     *            PNG.
     * @return an image which gets its pixel data from the specified file.
     * @see java.awt.Image
     * @see java.awt.Toolkit#createImage(java.lang.String)
     */
    public Image getImage(String filename)
    {
        String path = resolveFile(filename);

        if (cachedImages.containsKey(path)) return (Image) cachedImages.get(path);

        Image newImage = createImageImpl(path);

        if (newImage != null) cachedImages.put(path, newImage);

        return newImage;
    }

    /**
     * Returns an image which gets pixel data from the specified URL. The
     * underlying toolkit attempts to resolve multiple requests with the same
     * URL to the same returned Image. Since the mechanism required to
     * facilitate this sharing of Image objects may continue to hold onto images
     * that are no longer of use for an indefinite period of time, developers
     * are encouraged to implement their own caching of images by using the
     * createImage variant wherever available. <h3>Compatibility</h3>
     * PersonalJava does not require support of the PNG image file format.
     *
     * @param url
     *            URL must reference an image format that is recognized by this
     *            toolkit. The toolkit must be able to create images from the
     *            following image file formats: GIF, JPEG(JFIF), XBM, and PNG.
     * @return an image which gets its pixel data from the specified URL.
     * @see java.awt.Image
     * @see java.awt.Toolkit#createImage(java.net.URL)
     */
    public Image getImage(URL url)
    {
        if (cachedImages.containsKey(url)) return (Image) cachedImages.get(url);

        Image newImage = createImage(url);

        if (newImage != null) cachedImages.put(url, newImage);

        return newImage;
    }

    /**
     * Returns an image which gets pixel data from the specified file. The
     * returned Image is a new object which will not be shared with any other
     * caller of this method or its getImage variant.
     *
     * @param filename
     *            the name of a file containing pixel data in a recognized file
     *            format.
     * @return an image which gets its pixel data from the specified file.
     * @see java.awt.Toolkit#getImage(java.lang.String)
     */

    public Image createImage(String filename)
    {
        return createImageImpl(resolveFile(filename));
    }

    /**
     * Implements <code>createImage()</code>.
     *
     * @param path
     *            a canonicalized absolute path to the image
     * @return an image which gets its path from the specified file
     * @see #createImage(String)
     * @see #getImage(String)
     */
    private Image createImageImpl(String path)
    {
        ImageProducer ip = new FileImageSource(path);
        Image newImage = createImage(ip);

        return newImage;

    }

    /**
     * Resolve the given filename, returning the absolute/canonical path.
     *
     * @param filename
     * @return absolute/canonical path
     */
    private String resolveFile(String filename)
    {
        try
        {
            File f = new File(filename);
            return f.getCanonicalFile().getAbsolutePath();
        }
        catch (java.io.IOException e)
        {
            return filename;
        }
    }

    /**
     * Returns an image which gets pixel data from the specified URL. The
     * returned Image is a new object which will not be shared with any other
     * caller of this method or its getImage variant.
     *
     * @param url
     *            the URL to use in fetching the pixel data.
     * @return an image which gets its pixel data from the specified URL.
     * @see java.awt.Toolkit#getImage(java.net.URL)
     */

    public Image createImage(URL url)
    {
        ImageProducer ip = new URLImageSource(url);
        Image newImage = createImage(ip);

        return newImage;
    }

    /**
     * Prepares an image for rendering.
     * <p>
     * If the values of the width and height arguments are both <code>-1</code>,
     * this method prepares the image for rendering on the default screen;
     * otherwise, this method prepares an image for rendering on the default
     * screen at the specified width and height.
     * <p>
     * The image data is downloaded asynchronously in another thread, and an
     * appropriately scaled screen representation of the image is generated.
     * <p>
     * This method is called by components <code>prepareImage</code> methods.
     * <p>
     * Information on the flags returned by this method can be found with the
     * definition of the <code>ImageObserver</code> interface.
     *
     * @param image
     *            the image for which to prepare a screen representation.
     * @param width
     *            the width of the desired screen representation, or
     *            <code>-1</code>.
     * @param height
     *            the height of the desired screen representation, or
     *            <code>-1</code>.
     * @param observer
     *            the <code>ImageObserver</code> object to be notified as the
     *            image is being prepared.
     * @return <code>true</code> if the image has already been fully prepared;
     *         <code>false</code> otherwise.
     * @see java.awt.Component#prepareImage(java.awt.Image,
     *      java.awt.image.ImageObserver)
     * @see java.awt.Component#prepareImage(java.awt.Image, int, int,
     *      java.awt.image.ImageObserver)
     * @see java.awt.image.ImageObserver
     * @since JDK1.0
     */
    public boolean prepareImage(Image image, int width, int height, ImageObserver observer)
    {

        if (!(image instanceof MPEImage))
        {
            return true;
        }

        MPEImage mpeimg = (MPEImage) image;

        return mpeimg.prepareImage(width, height, observer);
    }

    /**
     * Indicates the construction status of a specified image that is being
     * prepared for display.
     * <p>
     * If the values of the width and height arguments are both <code>-1</code>,
     * this method returns the construction status of a screen representation of
     * the specified image in this toolkit. Otherwise, this method returns the
     * construction status of a scaled representation of the image at the
     * specified width and height.
     * <p>
     * This method does not cause the image to begin loading. An application
     * must call <code>prepareImage</code> to force the loading of an image.
     * <p>
     * This method is called by the component's <code>checkImage</code> methods.
     * <p>
     * Information on the flags returned by this method can be found with the
     * definition of the <code>ImageObserver</code> interface.
     *
     * @param image
     *            the image whose status is being checked.
     * @param width
     *            the width of the scaled version whose status is being checked,
     *            or <code>-1</code>.
     * @param height
     *            the height of the scaled version whose status is being
     *            checked, or <code>-1</code>.
     * @param observer
     *            the <code>ImageObserver</code> object to be notified as the
     *            image is being prepared.
     * @return the bitwise inclusive <strong>OR</strong> of the
     *         <code>ImageObserver</code> flags for the image data that is
     *         currently available.
     * @see java.awt.Toolkit#prepareImage(java.awt.Image, int, int,
     *      java.awt.image.ImageObserver)
     * @see java.awt.Component#checkImage(java.awt.Image,
     *      java.awt.image.ImageObserver)
     * @see java.awt.Component#checkImage(java.awt.Image, int, int,
     *      java.awt.image.ImageObserver)
     * @see java.awt.image.ImageObserver
     * @since JDK1.0
     */
    public int checkImage(Image image, int width, int height, ImageObserver observer)
    {

        if (!(image instanceof MPEImage))
        {
            return ImageObserver.ALLBITS;
        }

        MPEImage mpeimg = (MPEImage) image;

        return mpeimg.getStatus(observer);
    }

    /**
     * Creates an image with the specified image producer.
     *
     * @param producer
     *            the image producer to be used.
     * @return an image with the specified image producer.
     * @see java.awt.Image
     * @see java.awt.image.ImageProducer
     * @see java.awt.Component#createImage(java.awt.image.ImageProducer)
     * @since JDK1.0
     */
    public Image createImage(ImageProducer producer)
    {
        return new MPEImage(producer);
    }

    /**
     * Creates an image which decodes the image stored in the specified byte
     * array, and at the specified offset and length. The data must be in some
     * image format, such as GIF or JPEG, that is supported by this toolkit.
     *
     * @param imagedata
     *            an array of bytes, representing image data in a supported
     *            image format.
     * @param imageoffset
     *            the offset of the beginning of the data in the array.
     * @param imagelength
     *            the length of the data in the array.
     * @return an image.
     * @since JDK1.1
     */
    public Image createImage(byte[] imagedata, int imageoffset, int imagelength)
    {
        ImageProducer ip = new ByteArrayImageSource(imagedata, imageoffset, imagelength);
        Image newImage = createImage(ip);

        return newImage;
    }

    /**
     * This method is invoked from the Toolkit.createImage() private native
     * method.
     */
    Image createImage(Component component, int width, int height)
    {
        float size = (float) width * height;

        if (width <= 0 || height <= 0)
        {
            throw new IllegalArgumentException("Width (" + width + ") and height (" + height + ") must be > 0");
        }
        if (size >= Integer.MAX_VALUE)
        {
            throw new IllegalArgumentException("Dimensions (width=" + width + " height=" + height + ") are too large");
        }

        return new MPEOffscreenImage(component, width, height, defaultGC);
    }

    /**
     * Emits an audio beep.
     *
     * @since JDK1.1
     */
    public void beep()
    {
        // Not implemented
    }

    /*
     * Get the application's or applet's EventQueue instance, without checking
     * access. For security reasons, this can only be called from a Toolkit
     * subclass. Implementations wishing to modify the default EventQueue
     * support should subclass this method.
     */
    protected EventQueue getSystemEventQueueImpl()
    {
        return eventQueue;
    }

    /**
     * Posts a key event to the event queue. This method is called from native
     * code when a key event is received.
     *
     * @param source
     *            the top-level <code>Window</code>
     * @param id
     *            the event id
     * @param modifiers
     *            key event modifiers
     * @param keyCode
     *            the virtual key code
     * @param keyChar
     *            the key character
     */
    private static void postKeyEvent(Component source, int id, int modifiers, int keyCode, char keyChar)
    {
        /*
         * This is a first-pass implementation. Currently, we always post AWT
         * events to the single system queue. And the EventDispatcher can handle
         * additional events.
         *
         * Eventually, we'll want to either: 1) Have getSystemEventQueueImpl
         * return an app-specific queue. 2) Always call the EventDispatcher (and
         * let it dispatch to the event-specific queue)
         *
         * I'm not sure which.
         *
         * Also, I'm not sure where an <i>AppContext</i> needs to be introduced.
         * This would be mapped back to the OCAP CallerContext on some level.
         */

        long when = System.currentTimeMillis();
        final KeyEvent event = new KeyEvent(source, id, when, modifiers, keyCode, keyChar);
        final MPEToolkit tk = (MPEToolkit) Toolkit.getDefaultToolkit();
        EventQueue queue = tk.eventQueue;

        // Post AWT event to event queue
        if (tk.dispatcher == null)
        {
            queue.postEvent(event);
        }
        else
        {
            Runnable run = new Runnable() {
                public void run()
                {
                    tk.dispatcher.dispatch(event);
                }
            };
            queue.postEvent(new InvocationEvent(source, run));
        }
    }

    // Implements EventDispatchable.setEventDispatcher()
    public void setEventDispatcher(EventDispatcher d)
    {
        dispatcher = d;
    }

    // Implements GraphicsAdaptable.setGraphicsFactory()
    public void setGraphicsFactory(GraphicsFactory f)
    {
        if (f == null) this.gf = new NullFactory();
        this.gf = f;
    }

    /**
     * Allows other classes within the AWT implementation to wrap the
     * <code>Graphics</code> that they create appropriately.
     */
    Graphics wrapGraphics(Graphics g)
    {
        return gf.wrap(g);
    }

    /**
     * The default implementation of <code>GraphicsFactory</code> which performs
     * no wrapping of the given <code>Graphics</code> context.
     */
    private static class NullFactory implements GraphicsFactory
    {
        public Graphics wrap(Graphics g)
        {
            return g;
        }
    }
}
