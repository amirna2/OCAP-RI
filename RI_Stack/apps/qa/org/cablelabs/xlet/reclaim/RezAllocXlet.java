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

package org.cablelabs.xlet.reclaim;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Enumeration;
import java.util.Vector;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.dvb.ui.DVBBufferedImage;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HScreen;
import org.havi.ui.HText;
import org.havi.ui.HTextButton;
import org.havi.ui.event.HActionListener;
import org.havi.ui.event.HRcEvent;
import org.ocap.system.event.SystemEvent;
import org.ocap.system.event.SystemEventListener;
import org.ocap.system.event.SystemEventManager;

/**
 * @author Aaron Kamienski
 */
public class RezAllocXlet extends Component implements Xlet
{
    private XletContext xc;

    private HScene scene;

    private boolean started;

    private MemUsageLabel memory;

    private Component first;

    private Vector purgeable;

    public void initXlet(XletContext ctx) throws XletStateChangeException
    {
        xc = ctx;
    }

    public void startXlet() throws XletStateChangeException
    {
        if (!started)
        {
            HSceneFactory hsf = HSceneFactory.getInstance();
            scene = hsf.getFullScreenScene(HScreen.getDefaultHScreen().getDefaultHGraphicsDevice());
            if (scene == null) throw new XletStateChangeException("Could not create scene");

            scene.setForeground(Color.white.darker());
            scene.setBackground(Color.blue.darker().darker());
            scene.setFont(new Font("sansserif", Font.PLAIN, 14));

            scene.setLayout(new BorderLayout());

            // Safe area border
            scene.add(new Filler(50), BorderLayout.NORTH);
            scene.add(new Filler(50), BorderLayout.EAST);
            scene.add(new Filler(50), BorderLayout.SOUTH);
            scene.add(new Filler(50), BorderLayout.WEST);

            Container root = new Container()
            { /* empty */
            };
            root.setLayout(new BorderLayout());
            scene.add(root);

            memory = new MemUsageLabel();
            root.add(memory, BorderLayout.SOUTH);

            Container main = new Container()
            { /* empty */
            };
            main.setLayout(new FlowLayout());
            root.add(main);

            main.add(first = new MemGarbageButton());
            main.add(new ImageGarbageButton());
            main.add(new MemLeakLabel());
            main.add(new ObjectLeakLabel());
            main.add(new ImageLeakLabel());
            main.add(new ThreadLeakLabel());
            main.add(new EatMemThreadButton());
            main.add(new GCButton());
            main.add(new FinalButton());
            main.add(new MonAppButton());

            scene.validate();

            scene.list();

            // Remember Purgeable items
            Component[] c = main.getComponents();
            purgeable = new Vector();
            for (int i = 0; i < c.length; ++i)
            {
                if (c[i] instanceof Purgeable) purgeable.addElement(c[i]);
            }

            started = true;
        }
        scene.show();
        first.requestFocus();

        SetupTraversals trav = new SetupTraversals();
        trav.setWrap(true);
        trav.setSlopeRatio(3, 1);
        trav.setFocusTraversal(first.getParent().getComponents());
    }

    public void pauseXlet()
    {
        HScene tmp = scene;
        if (tmp != null) tmp.setVisible(false);
    }

    public void destroyXlet(boolean unconditional) throws XletStateChangeException
    {
        HScene tmp = scene;
        scene = null;
        if (tmp != null) tmp.dispose();
    }

    private void purgeAll()
    {
        for (Enumeration e = purgeable.elements(); e.hasMoreElements();)
        {
            ((Purgeable) e.nextElement()).purge();
        }
    }

    class Filler extends Component
    {
        public Filler(int space)
        {
            this.space = space;
        }

        public Dimension getPreferredSize()
        {
            return new Dimension(space, space);
        }

        public Dimension getMaximumSize()
        {
            return getPreferredSize();
        }

        public Dimension getMinimumSize()
        {
            return getPreferredSize();
        }

        private final int space;
    }

    abstract class Button extends HTextButton implements HActionListener
    {
        Button(String txt)
        {
            super(txt);
            setBackgroundMode(BACKGROUND_FILL);
            addHActionListener(this);
        }
    }

    class GCButton extends Button
    {
        GCButton()
        {
            super("Garbage Collect...");
        }

        public void actionPerformed(ActionEvent e)
        {
            System.gc();
            memory.update();
        }
    }

    class FinalButton extends Button
    {
        FinalButton()
        {
            super("Run Finalization...");
        }

        public void actionPerformed(ActionEvent e)
        {
            System.runFinalization();
            memory.update();
        }
    }

    class MonAppButton extends Button implements KeyListener, SystemEventListener
    {
        MonAppButton()
        {
            super("");
            addKeyListener(this);
            update();
        }

        private void update()
        {
            if (switched)
                setTextContent("Remove Handler\nResourceDepletionEvent\nBlock " + block + " seconds", ALL_STATES);
            else
                setTextContent("Install Handler\nResourceDepletionEvent\nBlock " + block + " seconds", ALL_STATES);
        }

        public void actionPerformed(ActionEvent e)
        {
            switched = !switched;

            SystemEventManager sem = SystemEventManager.getInstance();

            sem.setEventListener(SystemEventManager.RESOURCE_DEPLETION_EVENT_LISTENER, switched ? this : null);
            update();
        }

        public void keyPressed(KeyEvent e)
        {
            switch (e.getKeyCode())
            {
                case HRcEvent.VK_CHANNEL_UP:
                    if (block == 0)
                        block = 1;
                    else
                        block = block * 2;
                    break;
                case HRcEvent.VK_CHANNEL_DOWN:
                    block = block / 2;
                    break;
            }
            update();
        }

        public void keyReleased(KeyEvent e)
        {
            // empty
        }

        public void keyTyped(KeyEvent e)
        {
            // empty
        }

        public void notifyEvent(SystemEvent event)
        {
            // First, block the given number of seconds
            long time = block * 1000L;
            if (time > 0)
            {
                System.out.println("**** Blocking " + block + " seconds");
                try
                {
                    Thread.sleep(time);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
            // Purge!
            System.out.println("**** Purging Leaked Data ******");
            purgeAll();
        }

        private boolean switched;

        private int block = 0;
    }

    interface Purgeable
    {
        public void purge();
    }

    class MemUsageLabel extends HText
    {
        MemUsageLabel()
        {
            setBackgroundMode(BACKGROUND_FILL);
            update();
        }

        void update()
        {
            Runtime runtime = Runtime.getRuntime();
            long free = runtime.freeMemory();
            long total = runtime.totalMemory();
            long used = total - free;
            setTextContent("Free=" + free + " Used=" + used + " Total=" + total, ALL_STATES);
        }
    }

    class ImageLeakLabel extends ThreadLeakLabel
    {
        private static final int W = 320;

        private static final int H = 240;

        protected Object create()
        {
            return new DVBBufferedImage(320, 240);
        }

        protected void delete(Object obj)
        {
            ((Image) obj).flush();
        }

        protected void update()
        {
            setTextContent(SPACE + "Images (" + W + "x" + H + "): " + objects.size() + SPACE + EOL, ALL_STATES);
        }
    }

    class MemLeakLabel extends ThreadLeakLabel
    {
        private static final int SIZE = 256 * 1024;

        protected Object create()
        {
            return new byte[SIZE];
        }

        protected void delete(Object obj)
        {
            // Does nothing really
        }

        protected void update()
        {
            setTextContent(SPACE + "Memory [" + SIZE + "]: " + objects.size() + SPACE + EOL, ALL_STATES);
        }
    }

    class ObjectLeakLabel extends ThreadLeakLabel
    {
        private static final int SIZE = 64 * 1024;

        protected Object create()
        {
            Object a[] = new Object[SIZE];
            for (int i = 0; i < a.length; ++i)
                a[i] = new Object();
            return a;
        }

        protected void delete(Object obj)
        {
            // Does nothing really
        }

        protected void update()
        {
            setTextContent(SPACE + "Object [" + SIZE + "]: " + objects.size() + SPACE + EOL, ALL_STATES);
        }
    }

    class ThreadLeakLabel extends HText implements KeyListener, Runnable, Purgeable
    {
        ThreadLeakLabel()
        {
            setBackgroundMode(BACKGROUND_FILL);
            update();
            addKeyListener(this);
        }

        protected Object create()
        {
            Thread thread = new Thread(this);
            thread.start();
            return thread;
        }

        protected void delete(Object obj)
        {
            Thread thread = (Thread) obj;
            thread.interrupt();
            while (thread.isAlive())
            {
                try
                {
                    thread.join();
                }
                catch (InterruptedException ex)
                {
                    ex.printStackTrace();
                }
            }
        }

        protected void addObject(Object obj)
        {
            objects.addElement(obj);
        }

        public void purge()
        {
            Vector old = objects;
            objects = new Vector();
            for (Enumeration e = old.elements(); e.hasMoreElements();)
            {
                delete(e.nextElement());
            }
            update();
        }

        public void keyPressed(KeyEvent e)
        {
            switch (e.getKeyCode())
            {
                case HRcEvent.VK_CHANNEL_UP:
                    // This is what this would normally look like.
                    // objects.addElement(create());
                    // However, the generated bytecode results in objects being
                    // referenced event after clearing.
                    addObject(create());
                    break;
                case HRcEvent.VK_CHANNEL_DOWN:
                    if (objects.size() > 0)
                    {
                        Object obj = objects.elementAt(0);
                        delete(obj);
                        objects.removeElementAt(0);
                    }
                    break;
            }
            update();
            memory.update();
        }

        public void keyReleased(KeyEvent e)
        {
            // empty
        }

        public void keyTyped(KeyEvent e)
        {
            // empty
        }

        public void run()
        {
            while (!Thread.interrupted())
            {
                try
                {
                    synchronized (this)
                    {
                        wait();
                    }
                }
                catch (InterruptedException e)
                {
                    break;
                }
            }
        }

        protected void update()
        {
            setTextContent(SPACE + "Threads: " + objects.size() + SPACE + EOL, ALL_STATES);
        }

        protected static final String EOL = "\nCH+/CH- to change";

        protected Vector objects = new Vector();
    }

    class ImageGarbageButton extends MemGarbageButton
    {
        ImageGarbageButton()
        {
            bytes = 1024;
        }

        public void actionPerformed(ActionEvent e)
        {
            DVBBufferedImage garbage = new DVBBufferedImage(bytes, 10);
            memory.update();
        }

        protected void update()
        {
            setTextContent(SPACE + "Garbage: " + bytes + "x10 image" + SPACE + EOL, ALL_STATES);
        }
    }

    class MemGarbageButton extends Button implements KeyListener
    {
        MemGarbageButton()
        {
            super("");
            addKeyListener(this);
            bytes = 1024 * 1024;
            update();
        }

        public void actionPerformed(ActionEvent e)
        {
            byte[] garbage = new byte[bytes];
            memory.update();
        }

        public void keyPressed(KeyEvent e)
        {
            switch (e.getKeyCode())
            {
                case HRcEvent.VK_CHANNEL_UP:
                    bytes = bytes * 2;
                    break;
                case HRcEvent.VK_CHANNEL_DOWN:
                    if (bytes > 1) bytes = bytes / 2;
                    break;
            }
            update();
        }

        public void keyReleased(KeyEvent e)
        {
            // empty
        }

        public void keyTyped(KeyEvent e)
        {
            // empty
        }

        protected void update()
        {
            setTextContent(SPACE + "Garbage: " + bytes + " bytes" + SPACE + EOL, ALL_STATES);
        }

        protected static final String EOL = "\nCH+/CH-: to change amount\nSELECT: to alloc";

        protected int bytes;
    }

    class EatMemThreadButton extends Button
    {
        EatMemThreadButton()
        {
            super("Eat Memory Thread");
        }

        public void actionPerformed(ActionEvent e)
        {
            Thread thread = new Thread(new Runnable()
            {
                public void run()
                {
                    System.gc();
                    System.runFinalization();
                    System.gc();
                    memory.update();

                    Vector v = new Vector();
                    while (true)
                    {
                        v.addElement(new byte[256 * 1024]);

                        memory.update();
                    }
                }
            }, "MemEater");

            thread.start();
        }
    }

    private static final String SPACE = "     ";
}
