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

package org.havi.ui;

import junit.framework.*;
import java.awt.*;
import java.awt.event.*;

/**
 * The HaviTestToolkit is an interface for supporting parameterized tests based
 * on the target platform.
 */
public abstract class HaviTestToolkit extends Assert
{
    /** Not publicly instantiable. */
    protected HaviTestToolkit()
    {
    }

    /**
     * Returns the singleton instance of the HaviTestToolkit. The
     * {@link TestSupport#getProperty(String) TestSupport} <code>toolkit</code>
     * property is queried for the name of the required toolkit class. If this
     * is not set, an instance of the default toolkit is returned. If the
     * specified toolkit class cannot be created, null is returned.
     */
    public synchronized static HaviTestToolkit getToolkit()
    {
        if (tk == null)
        {
            String str;
            if ((str = TestSupport.getProperty(PROP)) != null)
            {
                try
                {
                    Class tkClass = Class.forName(str);
                    tk = (HaviTestToolkit) tkClass.newInstance();
                }
                catch (Exception e)
                {
                    tk = null;
                }
            }
            else
            {
                tk = new HaviTestToolkit()
                {
                };
            }
        }
        return tk;
    }

    /**
     * Creates and returns events that are suitable for translation to an
     * HFocusEvent on the appropriate platform.
     */
    public AWTEvent[] createFocusEvent(HComponent component, boolean gained)
    {
        AWTEvent[] array = new AWTEvent[1 /* gained?2:1 */];

        array[0] = new FocusEvent(component, gained ? FocusEvent.FOCUS_GAINED : FocusEvent.FOCUS_LOST);
        if (false /* gained */)
            array[1] = new MouseEvent(component, MouseEvent.MOUSE_ENTERED, System.currentTimeMillis(), 0, 0, 0, 1,
                    false);
        return array;
    }

    /**
     * Creates and returns events that are suitable for translation to an
     * HFocusEvent FOCUS_TRANSFER on the appropriate platform.
     */
    public AWTEvent[] createFocusTransferEvent(HComponent component)
    {
        return new AWTEvent[] {
                new KeyEvent(component, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_UP,
                        KeyEvent.CHAR_UNDEFINED),
                new KeyEvent(component, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_DOWN,
                        KeyEvent.CHAR_UNDEFINED),
                new KeyEvent(component, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_LEFT,
                        KeyEvent.CHAR_UNDEFINED),
                new KeyEvent(component, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_RIGHT,
                        KeyEvent.CHAR_UNDEFINED), };
    }

    /**
     * Creates and returns events that are suitable for translation to an
     * HActionEvent on the appropriate platform.
     */
    public AWTEvent[] createActionEvent(HComponent component)
    {
        return new AWTEvent[] {
                new KeyEvent(component, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ENTER, '\n'),
                new MouseEvent(component, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
                        MouseEvent.BUTTON1_MASK, 0, 0, 1, false) };
    }

    /**
     * Creates and returns events that are suitable for translation to an
     * HKeyEvent on the appropriate platform.
     */
    public AWTEvent[] createKeyEvent(HComponent component)
    {
        return new AWTEvent[] {
                new KeyEvent(component, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ENTER, '\n'),
                new KeyEvent(component, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_A, 'a'),
                new KeyEvent(component, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), KeyEvent.SHIFT_MASK,
                        KeyEvent.VK_A, 'A'),
                new KeyEvent(component, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_TAB, '\t'),
                new KeyEvent(component, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_COMMA, ','),
                new KeyEvent(component, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), KeyEvent.SHIFT_MASK,
                        KeyEvent.VK_UNDEFINED, '<'), };
    }

    /**
     * Creates and returns events that are suitable for translation to an
     * HItemEvent on the appropriate platform.
     */
    public AWTEvent[] createItemEvent(HComponent component)
    {
        return new AWTEvent[] {
                new KeyEvent(component, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_UP,
                        KeyEvent.CHAR_UNDEFINED),
                new KeyEvent(component, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_DOWN,
                        KeyEvent.CHAR_UNDEFINED),
                new KeyEvent(component, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_LEFT,
                        KeyEvent.CHAR_UNDEFINED),
                new KeyEvent(component, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_RIGHT,
                        KeyEvent.CHAR_UNDEFINED),
                new KeyEvent(component, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ENTER, '\n'),
                new KeyEvent(component, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_INSERT,
                        KeyEvent.CHAR_UNDEFINED),
                new KeyEvent(component, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ESCAPE,
                        KeyEvent.CHAR_UNDEFINED), };
    }

    /**
     * Creates and returns events that are suitable for translation to an
     * HAdjustmentEvent on the appropriate platform.
     */
    public AWTEvent[] createAdjustmentEvent(HComponent component)
    {
        return new AWTEvent[] {
                new KeyEvent(component, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_UP,
                        KeyEvent.CHAR_UNDEFINED),
                new KeyEvent(component, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_DOWN,
                        KeyEvent.CHAR_UNDEFINED),
                new KeyEvent(component, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_LEFT,
                        KeyEvent.CHAR_UNDEFINED),
                new KeyEvent(component, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_RIGHT,
                        KeyEvent.CHAR_UNDEFINED),
                new KeyEvent(component, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_INSERT,
                        KeyEvent.CHAR_UNDEFINED),
                new KeyEvent(component, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ESCAPE,
                        KeyEvent.CHAR_UNDEFINED), };
    }

    /**
     * Creates and returns events that are suitable for translation to an
     * HTextEvent on the appropriate platform.
     */
    public AWTEvent[] createTextEvent(HComponent component)
    {
        return new AWTEvent[] {
                new KeyEvent(component, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_UP,
                        KeyEvent.CHAR_UNDEFINED),
                new KeyEvent(component, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_DOWN,
                        KeyEvent.CHAR_UNDEFINED),
                new KeyEvent(component, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_LEFT,
                        KeyEvent.CHAR_UNDEFINED),
                new KeyEvent(component, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_RIGHT,
                        KeyEvent.CHAR_UNDEFINED),
                new KeyEvent(component, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_PAGE_DOWN,
                        KeyEvent.CHAR_UNDEFINED),
                new KeyEvent(component, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_PAGE_UP,
                        KeyEvent.CHAR_UNDEFINED),
                new KeyEvent(component, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_INSERT,
                        KeyEvent.CHAR_UNDEFINED),
                new KeyEvent(component, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ESCAPE,
                        KeyEvent.CHAR_UNDEFINED), };
    }

    /**
     * Provides a platform specific default value for max content size.
     */
    public Dimension getContentMaxSize(HVisible hvisible)
    {
        return new Dimension(2, 2);
    }

    /**
     * Provides a platform specific default value for min content size.
     */
    public Dimension getContentMinSize(HVisible hvisible)
    {
        return new Dimension(2, 2);
    }

    /** Singleton toolkit. */
    private static HaviTestToolkit tk;

    /** The property to lookup. */
    private static final String PROP = "toolkit";

}
