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

public class HSampler extends HContainer
{
    public HSampler(String args[])
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

    private void parseArgs(String[] args)
    {
        Class cl = getClass();
        for (int i = 0; i < args.length; ++i)
        {
            try
            {
                out.println("Accessing field: " + args[i]);
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

    private void setup()
    {
        setForeground(Color.blue.darker());
        setBackground(Color.lightGray.brighter());

        setupFonts();
        setupPanels();
        validate();
        scene.show();
        setupTraversals();
        dump(this, "+");
    }

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

    private void setupFonts()
    {
        fonts = new Font[fontSizes.length];
        for (int i = 0; i < fontSizes.length; ++i)
            fonts[i] = new Font("SansSerif", 0, fontSizes[i]);

        setFont(fonts[0]);
    }

    public boolean YES_LINES = false, NO_STATUS = false, NO_TITLE = false, NO_MLE = false, NO_MLE_TITLE = false,
            NO_MLE_BUTTONS = false, NO_SLE = false, NO_SLE_TITLE = false, NO_SLE_BUTTONS = false, NO_LISTGROUP = false,
            NO_LIST_TITLE = false, NO_LISTINFO = false, NO_TOGGLE1 = false, NO_TOGGLE2 = false,
            NO_TOGGLE1_TITLE = false, NO_TOGGLE2_TITLE = false, NO_ANIM = false, MULTICOLOR = false;

    private static final Color[] multi = { Color.black, Color.blue, Color.cyan, Color.darkGray, Color.gray,
            Color.green, Color.magenta, Color.orange, Color.pink, Color.red, Color.white,
    // Color.yellow,
    // Color.lightGray,
    };

    private static final Color[] gray = { Color.gray, };

    Color[] colors = MULTICOLOR ? multi : gray;

    private int colorIndex = 0;

    private Color nextColor()
    {
        int i = colorIndex++;

        colorIndex = colorIndex % colors.length;
        return colors[i];
    }

    private void setupPanels()
    {
        FlowLayout flow = new FlowLayout();
        GridLayout column = new GridLayout(1, 2);
        GridLayout row = new GridLayout(2, 1);

        // Add status bar
        statusBar = new StaticText("HSampler started...", "StatusBar");
        statusBar.setFont(fonts[0]);
        if (!NO_STATUS) add(statusBar, BorderLayout.SOUTH);

        // Focus Status
        HFocusListener focus = new FocusHandler(statusBar);

        // Add title bar
        HStaticText title = new StaticText("HSampler", "title");
        title.setFont(fonts[3]);
        if (!NO_TITLE) add(title, BorderLayout.NORTH);

        // Split center into 2 columns
        Container columnPanel = new MyContainer("columnPanel", nextColor());
        add(columnPanel, BorderLayout.CENTER);

        // Add left and right columns
        Container leftColumn = new MyContainer("left", nextColor());
        Container rightColumn = new MyContainer("right", nextColor());
        columnPanel.setLayout(column);
        columnPanel.add(leftColumn);
        columnPanel.add(rightColumn);

        // Left Column
        Container mleBox = new MyContainer("mleBox", nextColor());
        Container sleBox = new MyContainer("sleBox", nextColor());
        Container animBox = new MyContainer("animBox", nextColor());
        leftColumn.setLayout(new BorderLayout());
        leftColumn.add(animBox, BorderLayout.NORTH);
        leftColumn.add(mleBox, BorderLayout.CENTER);
        leftColumn.add(sleBox, BorderLayout.SOUTH);

        // Right Column
        Container listBox = new MyContainer("listBox", nextColor());
        Container toggleBox = new MyContainer("toggleBox", nextColor());
        rightColumn.setLayout(new BorderLayout());
        rightColumn.add(listBox, BorderLayout.CENTER);
        rightColumn.add(toggleBox, BorderLayout.SOUTH);

        // Animation Box
        Image aImages[] = new Image[] { loadImage("images/T1.gif"), loadImage("images/T2.gif"),
                loadImage("images/T3.gif"), loadImage("images/T4.gif"), loadImage("images/T5.gif"),
                loadImage("images/T6.gif"), loadImage("images/T7.gif"), loadImage("images/T8.gif"),
                loadImage("images/T9.gif"), loadImage("images/T10.gif"), };
        waitImages(aImages);

        animBox.setLayout(flow);
        HAnimation anim = new Animation("anim", aImages, focus);
        anim.addHFocusListener(new HFocusListener()
        {
            public void focusGained(FocusEvent e)
            {
                HAnimation anim = (HAnimation) e.getSource();
                anim.start();
            }

            public void focusLost(FocusEvent e)
            {
                HAnimation anim = (HAnimation) e.getSource();
                anim.stop();
            }
        });
        if (!NO_ANIM) animBox.add(anim);

        // MultilineEntry Box
        Container mleButtons = new MyContainer("mleButtons", nextColor());
        HMultilineEntry mle = new MultilineEntry("Multi-line text entry", "Multiline Entry", focus);
        HTextButton mleLower = new TextButton("Lowercase", "MLE Lower", focus);
        HTextButton mleClear = new TextButton("Clear", "MLE Clear", focus);
        HTextButton mleUpper = new TextButton("Uppercase", "MLE Upper", focus);
        HActionListener mleHandler = new ButtonHandler(mle);
        mleLower.addHActionListener(mleHandler);
        mleClear.addHActionListener(mleHandler);
        mleUpper.addHActionListener(mleHandler);
        mleButtons.setLayout(flow);
        if (!NO_MLE_BUTTONS)
        {
            mleButtons.add(mleLower);
            mleButtons.add(mleClear);
            mleButtons.add(mleUpper);
        }
        mleBox.setLayout(new BorderLayout());
        if (!NO_MLE_TITLE) mleBox.add(new StaticText("Multiline", "MLE Title"), BorderLayout.NORTH);
        if (!NO_MLE) mleBox.add(mle, BorderLayout.CENTER);
        mleBox.add(mleButtons, BorderLayout.SOUTH);

        // SinglelineEntry Box
        Container sleButtons = new MyContainer("sleButtons", nextColor());
        HSinglelineEntry sle = new SinglelineEntry("Single-line text entry", "Singleline Entry", focus);
        HTextButton sleLower = new TextButton("Lowercase", "SLE Lower", focus);
        HTextButton sleClear = new TextButton("Clear", "SLE Clear", focus);
        HTextButton sleUpper = new TextButton("Uppercase", "SLE Upper", focus);
        HActionListener sleHandler = new ButtonHandler(sle);
        sleLower.addHActionListener(sleHandler);
        sleClear.addHActionListener(sleHandler);
        sleUpper.addHActionListener(sleHandler);
        sleButtons.setLayout(flow);
        if (!NO_SLE_BUTTONS)
        {
            sleButtons.add(sleLower);
            sleButtons.add(sleClear);
            sleButtons.add(sleUpper);
        }
        sleBox.setLayout(new BorderLayout());
        if (!NO_SLE_TITLE) sleBox.add(new StaticText("Singleline", "SLE Title"), BorderLayout.NORTH);
        if (!NO_SLE) sleBox.add(sle, BorderLayout.CENTER);
        sleBox.add(sleButtons, BorderLayout.SOUTH);

        // List Box
        HStaticText listInfo = new StaticText("\n\n\n", "listInfo");
        listInfo.setFont(fonts[0]);
        String listStrings[] = { "Sleepy", "Sneezy", "Grumpy", "Dopey", "Doc", "Happy", "Bashful" };
        HListGroup list = new ListGroup("ListGroup", focus);
        list.setMultiSelection(true);
        for (int i = 0; i < listStrings.length; ++i)
            list.addItem(new HListElement(listStrings[i]), HListGroup.ADD_INDEX_END);
        list.addItemListener(new ItemHandler(listInfo));
        listBox.setLayout(new BorderLayout());
        if (!NO_LIST_TITLE) listBox.add(new StaticText("ListGroup", "List Title"), BorderLayout.NORTH);
        if (!NO_LISTGROUP) listBox.add(list, BorderLayout.CENTER);
        if (!NO_LISTINFO) listBox.add(listInfo, BorderLayout.SOUTH);

        // Toggle Box
        Container tLeft = new MyContainer("Toggle Left", nextColor());
        Container tRight = new MyContainer("Toggle Right", nextColor());
        toggleBox.setLayout(row);
        toggleBox.add(tLeft);
        toggleBox.add(tRight);

        Image tImages[] = new Image[] { loadImage("images/red-ball-small.gif"), loadImage("images/red-ball.gif"),
                loadImage("images/green-ball.gif"), loadImage("images/green-ball-small.gif"), };
        waitImages(tImages);

        tLeft.setLayout(flow);
        title = new StaticText("MultiSelect: ", "MultiSelect");
        title.setFont(fonts[0]);
        if (!NO_TOGGLE1_TITLE) tLeft.add(title);
        if (!NO_TOGGLE1)
        {
            for (int i = 0; i < 3; ++i)
            {
                HToggleButton t = new ToggleButton(tImages[0], tImages[1], tImages[2], tImages[3], focus);
                tLeft.add(t);
            }
        }

        HToggleGroup group = new HToggleGroup();
        group.setForcedSelection(true);
        tRight.setLayout(flow);
        title = new StaticText("SingleSelect: ", "SingleSelect");
        title.setFont(fonts[0]);
        if (!NO_TOGGLE2_TITLE) tRight.add(title);
        if (!NO_TOGGLE2)
        {
            for (int i = 0; i < 3; ++i)
            {
                HToggleButton t = new ToggleButton(tImages[0], tImages[1], tImages[2], tImages[3], group, focus);
                tRight.add(t);
            }
        }
    }

    private Image loadImage(String res)
    {
        java.net.URL url = getClass().getResource(res);
        out.println("loadImage(" + res + ") -> " + url);
        out.flush();
        Image i = Toolkit.getDefaultToolkit().getImage(url);
        out.println(url + " -> " + i);
        out.flush();
        return i;
    }

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

    private void setupTraversals()
    {
        SetupTraversals setup = new SetupTraversals();

        Component[] components = new Component[compVector.size()];
        compVector.copyInto(components);
        setup.setFocusTraversal(components);
    }

    public void paint(Graphics g)
    {
        // Color in background, as for AWT Panel
        Dimension size = getSize();
        g.setColor(getBackground());
        g.fillRect(0, 0, size.width, size.height);
        super.paint(g);
    }

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
            out.println("!!!!!!! HSampler !!!!!!");
            out.flush();
        }
        catch (Exception e)
        {
            out = null;
        }
    }

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
                Thread.sleep(250);
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
            if (items.length == 0)
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
            setBackgroundMode(BACKGROUND_FILL);
            setName(name);
            setFont(fonts[1]);
        }
    }

    class TextButton extends HTextButton
    {
        public TextButton(String str, String name, HFocusListener fl)
        {
            super(str);
            setTextContent(str.toUpperCase(), FOCUSED_STATE);
            setTextContent(str, ACTIONED_FOCUSED_STATE);
            setBackgroundMode(BACKGROUND_FILL);
            registerComponent(this);
            setName(name);
            setActionCommand(str);
            if (fl != null) addHFocusListener(fl);
        }
    }

    class ListGroup extends HListGroup
    {
        public ListGroup(String name, HFocusListener fl)
        {
            setBackgroundMode(BACKGROUND_FILL);
            registerComponent(this);
            setName(name);
            if (fl != null) addHFocusListener(fl);
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
            setBackgroundMode(BACKGROUND_FILL);
            registerComponent(this);
            if (fl != null) addHFocusListener(fl);
            setName("Toggle #" + (toggleI++));
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
        }
    }

    class Animation extends HAnimation
    {
        public Animation(String name, Image[] images, HFocusListener fl)
        {
            setAnimateContent(images, HState.NORMAL_STATE);
            setName(name);
            registerComponent(this);
            if (fl != null) addHFocusListener(fl);
        }
    }

    class MyContainer extends HContainer
    {
        Color oc, ic;

        public MyContainer(String name)
        {
            this(name, Color.darkGray, Color.red);
        }

        public MyContainer(String name, Color c)
        {
            this(name, c, c);
        }

        public MyContainer(String name, Color oc, Color ic)
        {
            setName(name);
            this.oc = oc;
            this.ic = ic;
        }

        public Insets getInsets()
        {
            return new Insets(4, 4, 4, 4);
        }

        public void paint(Graphics g)
        {
            super.paint(g);

            Dimension size = getSize();
            g.setColor(oc);
            g.drawRect(0, 0, size.width - 1, size.height - 1);
            g.setColor(ic);
            g.drawRect(1, 1, size.width - 3, size.height - 3);

            if (YES_LINES)
            {
                g.drawLine(1, 1, size.width - 1, size.height - 1);
                g.drawLine(1, size.height - 1, size.width - 1, 1);
            }
        }
    }

    public static void main(String args[])
    {
        setupOutput();
        try
        {
            HSampler app = new HSampler(args);
        }
        catch (Throwable e)
        {
            out.println("Exception!");
            e.printStackTrace(out);
            out.flush();
        }
    }

    /**
     * An Xlet interface for the HSampler app.
     */
    public static class Xlet implements javax.tv.xlet.Xlet
    {
        private boolean started = false;

        private javax.tv.xlet.XletContext ctx;

        private HSampler app;

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
                    app = new HSampler(args);
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
}
