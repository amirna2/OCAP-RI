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

package org.cablelabs.xlet.hsampler;

import org.havi.ui.*;
import org.havi.ui.event.*;
import org.dvb.ui.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

/*
 * ToDo:
 *
 * - Provide toggle-buttons/groups or lists to select H/V alignment.
 *   Apply to all components.  I.e., change list operation to controlling
 *   vertical alignment, and change toggle button operation to controlling 
 *   horizontal alignment.
 */

/**
 * A version of HSampler rewritten to use graphics.
 */
public class HSampler2 extends HContainer
{
    public HSampler2(String args[])
    {
        // parseArgs(args);

        setLayout(new BorderLayout());
        setSize(640, 480);

        // Create list of components
        // (To be filled in in setup)
        compVector = new Vector();

        // Create HScene
        scene = HSceneFactory.getInstance().getDefaultHScene();
        scene.setSize(640, 480); // if not already
        scene.add(this);
        scene.addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
                // System.exit(0);
            }
        });

        setup();

        showIt();

        // new test.util.TraversalBrowser(this).show();
    }

    /**
     * Show the scene.
     */
    void showIt()
    {
        scene.show();
        scene.repaint();
        ((Component) compVector.elementAt(0)).requestFocus();
    }

    /**
     * Hide the scene.
     */
    void hideIt()
    {
        scene.setVisible(false);
    }

    /**
     * Hide and dispose the scene.
     */
    void disposeIt()
    {
        hideIt();
        HScene tmp = scene;
        scene = null;
        HSceneFactory.getInstance().dispose(tmp);
    }

    /**
     * Parse arguments and set instance variables using reflection.
     */
    private void parseArgs(String[] args)
    {
        Class cl = getClass();
        for (int i = 0; i < args.length; ++i)
        {
            try
            {
                if (DEBUG) out.println("Accessing field: " + args[i]);
                java.lang.reflect.Field fl = cl.getField(args[i]);
                boolean value = fl.getBoolean(this);
                fl.setBoolean(this, !value);
            }
            catch (Exception e)
            {
                e.printStackTrace(out);
            }
        }
    }

    /**
     * Performs extended initialization, including HAVi component creation,
     * layout, and focus traversals.
     */
    private void setup()
    {
        setForeground(textColor);
        setBackground(bgColor);

        setupImages();
        setupFonts();
        setupPanels();
        validate();
        scene.show();
        setupTraversals();
        if (DEBUG) dump(this, "+");
    }

    /**
     * Debug method used to "dump" a component hierarchy.
     */
    private void dump(Container me, String pad)
    {
        Component c[] = me.getComponents();
        out.println(pad + me);

        pad = pad + "--";
        for (int i = 0; i < c.length; ++i)
        {
            if (c[i] instanceof Container)
                dump((Container) c[i], pad);
            else
                out.println(pad + c[i]);
        }
        out.flush();
    }

    /**
     * Root background image.
     */
    private Image bg;

    /**
     * Sets up globally accessible images. Currently only the main background
     * image is loaded here. The rest are loaded in setupPanels().
     */
    private void setupImages()
    {
        bg = loadImage("haviBg.jpg");
        waitImages(new Image[] { bg });
    }

    /**
     * Creates fonts in the desired sizes.
     */
    private void setupFonts()
    {
        fonts = new Font[fontSizes.length];
        for (int i = 0; i < fontSizes.length; ++i)
            fonts[i] = new Font("SansSerif", 0, fontSizes[i]);

        setFont(fonts[0]);
    }

    /**
     * Instance variables set by parseArgs() via reflection.
     */
    public boolean YES_LINES = false, NO_STATUS = false, NO_TITLE = false, NO_MLE = false, NO_MLE_TITLE = false,
            NO_MLE_BUTTONS = false, NO_SLE = false, NO_SLE_TITLE = false, NO_SLE_BUTTONS = false, NO_LISTGROUP = false,
            NO_LIST_TITLE = false, NO_LISTINFO = false, NO_TOGGLE1 = false, NO_TOGGLE2 = false,
            NO_TOGGLE1_TITLE = false, NO_TOGGLE2_TITLE = false, MULTICOLOR = false;

    private static final Color orange = new Color(233, 165, 104); // orange

    private static final Color lightBlue = new Color(140, 185, 210); // light
                                                                     // blue

    private static final Color medBlue = new Color(78, 103, 160); // med blue

    private static final Color white = new Color(240, 240, 240); // white

    private static final Color statusColor = lightBlue;

    private static final Color textColor = white;

    private static final Color downColor = lightBlue;

    private static final Color bgColor = medBlue;

    private static final Color titleColor = orange;

    /**
     * Creates HAVi component hierarchy.
     */
    private void setupPanels()
    {
        Dimension size = getSize();
        /*
         * 
         * HSampler2 +-- root (border) +-- Status +-- MLE +-- MLE Buttons* +--
         * SLE +-- SLE Buttons* +-- ListGroup +-- List Label +-- Single Select
         * Buttons* +-- Multi Select Buttons*
         */

        setLayout(null);

        Container root = new MyContainer("root");
        root.setLayout(new BorderLayout());
        add(root);
        root.setBounds(0, 0, size.width, size.height);

        // Add status bar
        statusBar = new StaticText("HSampler2 started...", "StatusBar");
        statusBar.setFont(fonts[0]);
        root.add(statusBar, BorderLayout.SOUTH);

        // Focus Status
        HFocusListener focus = new FocusHandler(statusBar);

        // TextLook
        HTextLook textLook = new TextSliceLook(bg);

        // MultilineEntry Box
        HMultilineEntry mle = new MultilineEntry("Multi-line text entry", "Multiline Entry", focus);
        add(mle);
        mle.setBounds(4, 66, 300, 240);
        HActionListener mleHandler = new ButtonHandler(mle);
        HTextButton mleLower = new TextButton("Lowercase", "MLE Lower", focus, textLook, mleHandler);
        HTextButton mleClear = new TextButton("Clear", "MLE Clear", focus, textLook, mleHandler);
        HTextButton mleUpper = new TextButton("Uppercase", "MLE Upper", focus, textLook, mleHandler);
        add(mleLower);
        add(mleClear);
        add(mleUpper);
        mleLower.setBounds(39, 309, 90, 35);
        mleClear.setBounds(133, 309, 48, 35);
        mleUpper.setBounds(179, 309, 90, 35);

        // SinglelineEntry Box
        HSinglelineEntry sle = new SinglelineEntry("Single-line text entry", "Singleline Entry", focus);
        add(sle);
        sle.setBounds(4, 380, 300, 22);
        HActionListener sleHandler = new ButtonHandler(sle);
        HText sleLower = new TextButton("Lowercase", "SLE Lower", focus, textLook, sleHandler);
        HText sleClear = new TextButton("Clear", "SLE Clear", focus, textLook, sleHandler);
        HText sleUpper = new TextButton("Uppercase", "SLE Upper", focus, textLook, sleHandler);
        add(sleLower);
        add(sleClear);
        add(sleUpper);
        sleLower.setBounds(39, 409, 90, 35);
        sleClear.setBounds(133, 409, 48, 35);
        sleUpper.setBounds(179, 409, 90, 35);

        // List Box
        HStaticText listInfo = new StaticText("\n\n\n", "listInfo");
        listInfo.setFont(fonts[0]);
        String listStrings[] = { "Sleepy", "Sneezy", "Grumpy", "Dopey", "Doc", "Happy", "Bashful" };
        Image listIcon = loadImage("haviUIGroupListHL.jpg");
        waitImages(new Image[] { listIcon });
        HListGroup list = new ListGroup2("ListGroup", focus, listStrings, listIcon);
        list.setMultiSelection(true);
        list.addItemListener(new ItemHandler(listInfo));
        add(list);
        list.setBounds(400, 69, 130, 225);
        add(listInfo);
        listInfo.setBounds(330, 308, 290, 48);

        // Toggle Box
        Image tImages[] = new Image[] { loadImage("haviUIRadioOff.jpg"), loadImage("haviUIRadioOff.jpg"),
                loadImage("haviUIRadioON.jpg"), loadImage("haviUIRadioON.jpg"), };
        waitImages(tImages);

        int x = 481;
        for (int i = 0; i < 3; ++i)
        {
            HToggleButton t = new ToggleButton(tImages[0], tImages[1], tImages[2], tImages[3], focus);
            t.setForeground(lightBlue);
            t.setBackground(white);
            add(t);
            t.setBounds(x, 376, tImages[0].getWidth(null), tImages[0].getHeight(null));
            x += 25;
        }

        HToggleGroup group = new HToggleGroup();
        group.setForcedSelection(true);
        x = 484;
        for (int i = 0; i < 3; ++i)
        {
            HToggleButton t = new ToggleButton(tImages[0], tImages[1], tImages[2], tImages[3], group, focus);
            add(t);
            t.setForeground(lightBlue);
            t.setBackground(white);
            t.setBounds(x, 413, tImages[0].getWidth(null), tImages[0].getHeight(null));
            x += 25;
        }
    }

    /**
     * Loads a image resource.
     */
    private Image loadImage(String res)
    {
        java.net.URL url = getClass().getResource(res);
        if (DEBUG)
        {
            out.println("loadImage(" + res + ") -> " + url);
            out.flush();
        }
        Image i = Toolkit.getDefaultToolkit().getImage(url);
        if (DEBUG)
        {
            out.println(url + " -> " + i);
            out.flush();
        }
        return i;
    }

    /**
     * Waits on a set of images.
     */
    private void waitImages(Image[] images)
    {
        MediaTracker mt = new MediaTracker(this);
        for (int i = 0; i < images.length; ++i)
        {
            mt.addImage(images[i], 1);
        }
        try
        {
            mt.waitForAll();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace(out);
        }
    }

    /**
     * Sets up traversals for all components that have been registered upon
     * creation via registerComponent().
     */
    private void setupTraversals()
    {
        SetupTraversals setup = new SetupTraversals();

        Component[] components = new Component[compVector.size()];
        compVector.copyInto(components);
        setup.setFocusTraversal(components);
    }

    /**
     * Draws this container as the root.
     */
    public void paint(Graphics g)
    {
        if (DEBUG)
        {
            out.println("DRAWING BACKGROUND " + bg);
            out.println("Graphics = " + g);
        }

        // Paint background
        if (g instanceof DVBGraphics)
        {
            try
            {
                ((DVBGraphics) g).setDVBComposite(DVBAlphaComposite.Src);
                if (DEBUG) out.println("Using SRC mode");
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        g.drawImage(bg, 0, 0, this);

        super.paint(g);
    }

    /**
     * Used to setup a "tee" pipe of output to both System.out and a file. (Not
     * exactly OCAP-compliant...)
     */
    protected static void setupOutput()
    {
        try
        {
            /*
             * FileOutputStream fos = new FileOutputStream("hsampler.txt");
             * TeeOutputStream tos = new TeeOutputStream(fos, System.out); out =
             * new PrintWriter(new OutputStreamWriter(tos));
             */
            out = System.out;
            out.println("!!!!!!! HSampler2 !!!!!!");
            out.flush();
        }
        catch (Exception e)
        {
            out = null;
        }
    }

    /**
     * Registers a component to have it's focus traversals set up.
     */
    private void registerComponent(Component c)
    {
        // c.addNotify();
        compVector.addElement(c);
    }

    /**
     * Handles Multi- and Single-line entry button actions.
     */
    class ButtonHandler implements HActionListener
    {
        private HVisible v;

        public ButtonHandler(HVisible v)
        {
            this.v = v;
        }

        public void actionPerformed(ActionEvent e)
        {
            String cmd = ((HActionable) e.getSource()).getActionCommand();
            String content = v.getTextContent(HState.NORMAL_STATE);
            if ("Uppercase".equals(cmd))
                content = content.toUpperCase();
            else if ("Lowercase".equals(cmd))
                content = content.toLowerCase();
            else if ("Clear".equals(cmd)) content = "";
            v.setTextContent(content, HState.NORMAL_STATE);

            // Sleep
            try
            {
                Thread.sleep(50);
            }
            catch (InterruptedException ex)
            {
            }
        }
    }

    /**
     * Tracks focus traversals and records them in the status bar.
     */
    class FocusHandler extends FocusAdapter implements HFocusListener
    {
        private HVisible status;

        public FocusHandler(HVisible status)
        {
            this.status = status;
        }

        public void focusGained(FocusEvent e)
        {
            Component src = (Component) e.getSource();
            String name = src.getName();

            if (name == null) name = "<unknown>";
            status.setTextContent("focused: " + name, HState.ALL_STATES);
        }
    }

    /**
     * Tracks list selection and records them in the given HVisible.
     */
    class ItemHandler implements HItemListener
    {
        private HVisible v;

        public ItemHandler(HVisible v)
        {
            this.v = v;
        }

        public void currentItemChanged(HItemEvent e)
        {
        }

        public void selectionChanged(HItemEvent e)
        {
            HListGroup list = (HListGroup) e.getSource();
            HListElement[] items = list.getSelection();

            String info;
            if (items == null || items.length == 0)
                info = "";
            else
            {
                info = items[0].getLabel();
                for (int i = 1; i < items.length; ++i)
                {
                    info = info + " " + items[i].getLabel();
                }
            }
            v.setTextContent(info, HState.NORMAL_STATE);
        }
    }

    class StaticText extends HStaticText
    {
        public StaticText(String str, String name)
        {
            super(str);
            setBackgroundMode(NO_BACKGROUND_FILL);
            setName(name);
            setFont(fonts[1]);
        }
    }

    class TextButton extends HTextButton
    {
        public TextButton(String str, String name, HFocusListener fl)
        {
            this(str, name, fl, null, null);
        }

        public TextButton(String str, String name, HFocusListener fl, HTextLook look, HActionListener al)
        {
            super(str);
            setBackgroundMode(NO_BACKGROUND_FILL);
            registerComponent(this);
            setName(name);
            setActionCommand(str);
            if (look != null) try
            {
                setLook(look);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            if (al != null) addHActionListener(al);
            if (fl != null) addHFocusListener(fl);
        }

        public void paint(Graphics g)
        {
            int state = getInteractionState();
            switch (state)
            {
                case ACTIONED_FOCUSED_STATE:
                    setForeground(downColor);
                    break;
                default:
                    setForeground(textColor);
                    break;
            }
            super.paint(g);
        }
    }

    class ListGroup extends HListGroup
    {
        public ListGroup(String name, HFocusListener fl)
        {
            setBackgroundMode(NO_BACKGROUND_FILL);
            registerComponent(this);
            setName(name);
            if (fl != null) addHFocusListener(fl);
        }
    }

    /**
     * ListGroup that changes the elements as they are selected/unselected.
     * Selected elements have an icon, unselected ones do not. HAVi HListElement
     * icons aren't meant to signify any sense of state, but just additional
     * information. Hence why this code is so... twisted.
     */
    class ListGroup2 extends ListGroup
    {
        HListElement[] selected;

        HListElement[] unselected;

        public ListGroup2(String name, HFocusListener fl, String[] label, Image icon)
        {
            super(name, fl);
            try
            {
                setLook(new HListGroupLook()
                {
                    public Insets getInsets(HVisible v)
                    {
                        return new Insets(3, 3, 3, 3);
                    }
                });
            }
            catch (Exception e)
            {
            }

            selected = new HListElement[label.length];
            unselected = new HListElement[label.length];
            for (int i = 0; i < label.length; ++i)
            {
                selected[i] = new HListElement(icon, label[i]);
                unselected[i] = new HListElement(label[i]);
            }
            addItems(unselected, ADD_INDEX_END);
            if (icon != null && icon.getWidth(null) > 0 && icon.getHeight(null) > 0)
            {
                setIconSize(new Dimension(icon.getWidth(null), icon.getHeight(null)));
            }
            /*
             * This is REALLY ugly! Unfortunately, I think it's all we can do...
             * Because an element cannot change after being added to a list
             * group. So, we must remove and re-add!!!!
             */
            if (icon != null) addItemListener(new HItemListener()
            {
                boolean busy;

                public void currentItemChanged(HItemEvent e)
                {
                }

                public void selectionChanged(HItemEvent e)
                {
                    // If selection changes as a result of OUR ops, ignore...
                    if (busy == true) return;
                    busy = true;

                    // Remember current index
                    int current = getCurrentIndex();
                    int[] indices = getSelectionIndices();

                    // Clear all items
                    removeAllItems();

                    // Add them back as unselected
                    addItems(unselected, ADD_INDEX_END);

                    // Replace with selected items
                    if (indices != null)
                    {
                        // remove and re-add
                        for (int i = 0; i < indices.length; ++i)
                        {
                            removeItem(indices[i]);
                            addItem(selected[indices[i]], indices[i]);
                        }
                        // set selected
                        for (int i = 0; i < indices.length; ++i)
                            setItemSelected(indices[i], true);
                    }

                    // restore current...
                    setCurrentItem(current);
                    busy = false;
                }
            });
        }
    }

    private static int toggleI = 0;

    class ToggleButton extends HToggleButton
    {
        public ToggleButton(Image i1, Image i2, Image i3, Image i4, HFocusListener fl)
        {
            this(i1, i2, i3, i4, null, fl);
        }

        public ToggleButton(Image i1, Image i2, Image i3, Image i4, HToggleGroup group, HFocusListener fl)
        {
            super(i1, i2, i3, i4, false, group);
            setBackgroundMode(NO_BACKGROUND_FILL);
            registerComponent(this);
            if (fl != null) addHFocusListener(fl);
            setName("Toggle #" + (toggleI++));
            try
            {
                setLook(new HGraphicLook()
                {
                    public Insets getInsets(HVisible v)
                    {
                        return new Insets(3, 3, 3, 3);
                    }
                });
            }
            catch (Exception e)
            {
            }
        }
    }

    class MultilineEntry extends HMultilineEntry
    {
        public MultilineEntry(String text, String name, HFocusListener fl)
        {
            setTextContent(text, ALL_STATES);
            registerComponent(this);
            setName(name);
            setMaxChars(200);
            if (fl != null) addHFocusListener(fl);
            try
            {
                setLook(new HMultilineEntryLook()
                {
                    public Insets getInsets(HVisible v)
                    {
                        return new Insets(3, 3, 3, 3);
                    }
                });
            }
            catch (Exception e)
            {
            }
        }
    }

    class SinglelineEntry extends HSinglelineEntry
    {
        public SinglelineEntry(String text, String name, HFocusListener fl)
        {
            setTextContent(text, ALL_STATES);
            registerComponent(this);
            setName(name);
            setMaxChars(20);
            if (fl != null) addHFocusListener(fl);
            try
            {
                setLook(new HSinglelineEntryLook()
                {
                    public Insets getInsets(HVisible v)
                    {
                        return new Insets(3, 3, 3, 3);
                    }
                });
            }
            catch (Exception e)
            {
            }
        }
    }

    class MyContainer extends HContainer
    {
        public MyContainer(String name)
        {
            setName(name);
        }
    }

    /**
     * HTextLook that draws an image background, sliced from the component's
     * corresponding location on the given background image.
     */
    static class TextSliceLook extends HTextLook
    {
        private Image image;

        private Point offset;

        public TextSliceLook(Image image)
        {
            this(image, new Point(0, 0));
        }

        public TextSliceLook(Image image, Point offset)
        {
            this.image = image;
            this.offset = offset;
        }

        public Insets getInsets(HVisible v)
        {
            return new Insets(3, 3, 3, 3);
        }

        public void showLook(Graphics g, HVisible v, int state)
        {
            int w, h;
            if (v.getBackgroundMode() == v.NO_BACKGROUND_FILL && (w = image.getWidth(v)) > 0
                    && (h = image.getHeight(v)) > 0)
            {
                Rectangle bounds = v.getBounds();

                int dx2 = bounds.width - 1;
                int dy2 = bounds.height - 1;
                int sx1 = bounds.x + offset.x;
                int sy1 = bounds.y + offset.y;
                int sx2 = sx1 + dx2;
                int sy2 = sy1 + dy2;

                g.drawImage(image, 0, 0, dx2, dy2, sx1, sy1, sx2, sy2, v);
            }
            super.showLook(g, v, state);
        }
    }

    /**
     * A "main" interface for the HSampler2 app.
     */
    public static void main(String args[])
    {
        setupOutput();
        try
        {
            HSampler2 app = new HSampler2(args);
        }
        catch (Throwable e)
        {
            out.println("Exception!");
            e.printStackTrace(out);
            out.flush();
        }
    }

    /**
     * An Xlet interface for the HSampler2 app.
     */
    public static class Xlet implements javax.tv.xlet.Xlet
    {
        private boolean started = false;

        private javax.tv.xlet.XletContext ctx;

        private HSampler2 app;

        public void initXlet(javax.tv.xlet.XletContext ctx)
        {
            this.ctx = ctx;
        }

        public void startXlet()
        {
            if (!started)
            {
                started = true;

                String[] args = getArgs();
                setupOutput();
                try
                {
                    app = new HSampler2(args);
                }
                catch (Throwable e)
                {
                    out.println("Exception!");
                    e.printStackTrace(out);
                    out.flush();
                }
            }
            else if (app != null)
            {
                app.showIt();
            }
        }

        public void pauseXlet()
        {
            if (app != null) app.hideIt();
        }

        public void destroyXlet(boolean forced) throws javax.tv.xlet.XletStateChangeException
        {
            if (app != null) app.disposeIt();
        }

        private String[] getArgs()
        {
            String[] params;

            params = (String[]) ctx.getXletProperty("dvb.caller.parameters");
            if (params == null) params = (String[]) ctx.getXletProperty(javax.tv.xlet.XletContext.ARGS);
            if (params == null) params = new String[0];
            return params;
        }
    }

    static PrintStream out;

    private HStaticText statusBar;

    private HScene scene;

    private Vector compVector;

    private Font[] fonts;

    private int[] fontSizes = { 14, 18, 24, 26 };

    private static final boolean DEBUG = false;
}
