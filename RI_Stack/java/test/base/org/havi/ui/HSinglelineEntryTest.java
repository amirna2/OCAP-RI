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

import org.cablelabs.test.*;
import java.awt.*;
import org.havi.ui.event.*;

/**
 * Tests {@link #HSinglelineEntry}.
 * 
 * @author Aaron Kamienski
 * @author Tom Henriksen
 * @version $Revision: 1.10 $, $Date: 2002/11/07 21:14:09 $
 */
public class HSinglelineEntryTest extends HVisibleTest
{
    /**
     * Standard constructor.
     */
    public HSinglelineEntryTest(String str)
    {
        super(str);
    }

    /**
     * Standalone runner.
     */
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(HSinglelineEntryTest.class);
    }

    /**
     * Creates a look of the appropriate type. Should be overridden by
     * subclasses.
     */
    protected HLook createLook()
    {
        return new HSinglelineEntryLook()
        {
        };
    }

    /**
     * The tested component.
     */
    protected HSinglelineEntry hsinglelineentry;

    /**
     * Should be overridden to create subclass of HSinglelineEntry.
     * 
     * @return the instance of HSinglelineEntry to test
     */
    protected HSinglelineEntry createHSinglelineEntry()
    {
        return new HSinglelineEntry();
    }

    /**
     * Overridden to create an HSinglelineEntry.
     * 
     * @return the instance of HVisible to test
     */
    protected HVisible createHVisible()
    {
        return (hsinglelineentry = createHSinglelineEntry());
    }

    /**
     * Tests for correct ancestry.
     * <ul>
     * <li>extends HVisible
     * <li>implements HTextValue
     * </ul>
     */
    public void testAncestry()
    {
        checkClass(HSinglelineEntryTest.class);

        TestUtils.testExtends(HSinglelineEntry.class, HVisible.class);
        HTextValueTest.testAncestry(HSinglelineEntry.class);
    }

    /**
     * Test the 5 constructors (and verify defaults and non-defaults):
     * <ul>
     * <li>HSinglelineEntry()
     * <li>HSinglelineEntry(int maxChars)
     * <li>HSinglelineEntry(int x, int y, int width, int height, int maxChars)
     * <li>HSinglelineEntry(String text, int maxChars, Font f, Color c)
     * <li>HSinglelineEntry(String text, int x, int y, int width, int height,
     * int maxChars, Font f, Color c)
     * </ul>
     */
    public void testConstructors()
    {
        Color fg = Color.blue;
        Font f = new Font("Dialog", Font.BOLD, 22);
        checkConstructor("HSinglelineEntry()", new HSinglelineEntry(), null, 0, 0, 0, 0, 16, null, null);
        checkConstructor("HSinglelineEntry(int maxChars)", new HSinglelineEntry(20), null, 0, 0, 0, 0, 20, null, null);
        checkConstructor("HSinglelineEntry(int x, int y, int w, int h, int maxChars)", new HSinglelineEntry(1, 2, 3, 4,
                20), null, 1, 2, 3, 4, 20, null, null);
        checkConstructor("HSinglelineEntry(String txt, int maxChars, Font f, Color fg)", new HSinglelineEntry("Hello",
                30, f, fg), "Hello", 0, 0, 0, 0, 30, f, fg);
        checkConstructor("HSinglelineEntry(String txt, int x, int y, int w, int h,"
                + " int maxChars, Font f, Color fg)", new HSinglelineEntry("Hello", 1, 2, 3, 4, 40, f, fg), "Hello", 1,
                2, 3, 4, 40, f, fg);
    }

    /**
     * Check for proper initialization of constructor values.
     */
    private void checkConstructor(String msg, HSinglelineEntry entry, String txt, int x, int y, int w, int h,
            int maxChars, Font f, Color fg)
    {
        // Check variables exposed in constructors
        assertNotNull(msg + " not allocated", entry);
        assertEquals(msg + " x-coordinated not initialized correctly", x, entry.getLocation().x);
        assertEquals(msg + " y-coordinated not initialized correctly", y, entry.getLocation().y);
        assertEquals(msg + " width not initialized correctly", w, entry.getSize().width);
        assertEquals(msg + " height not initialized correctly", h, entry.getSize().height);
        assertEquals(msg + " maxChars not initialized correctly", maxChars, entry.getMaxChars());
        if (txt != null)
            assertEquals(msg + " text not initialized correctly", txt, entry.getTextContent(HState.NORMAL_STATE));
        else
            assertNull(msg + " text not initialized to null", entry.getTextContent(HState.NORMAL_STATE));
        assertSame(msg + " font not initialized correctly", f, entry.getFont());
        assertSame(msg + " fg color not initialized correctly", fg, entry.getForeground());

        // Check variables NOT exposed in constructors
        assertEquals(msg + " type should be set to accept any input", HKeyboardInputPreferred.INPUT_ANY,
                entry.getType());
        assertSame(msg + " default look not used", HSinglelineEntry.getDefaultLook(), entry.getLook());
        assertNull(msg + " gain focus sound incorrectly initialized", entry.getGainFocusSound());
        assertNull(msg + " lose focus sound incorrectly initialized", entry.getLoseFocusSound());
        assertEquals(msg + " caret position incorrectly initialized", (txt != null) ? txt.length() : 0,
                entry.getCaretCharPosition());
        assertEquals(msg + " echo character incorrectly initialized", '\0', entry.getEchoChar());
        assertEquals(msg + " edit mode incorrectly initialized", false, entry.getEditMode());
        assertEquals(msg + " border mode not initialized correctly", true, entry.getBordersEnabled());
        assertNull(msg + " custom input chars should not be assigned", entry.getValidInput());
    }

    /**
     * Tests for unexpected added fields.
     */
    public void testFields()
    {
        HKeyboardInputPreferredTest.testFields();
        TestUtils.testNoAddedFields(getTestedClass(), null);
    }

    /**
     * Test setLook().
     * <ul>
     * <li>Only HSinglelineEntryLook should be accepted.
     * <li>The set look should be the retrieved look.
     * </ul>
     */
    public void testLook() throws Exception
    {
        HSinglelineEntryLook newLook = new HSinglelineEntryLook();
        try
        {
            hsinglelineentry.setLook(newLook);
        }
        catch (HInvalidLookException e)
        {
        }
        assertEquals("Set look should equal retrieved look", newLook, hsinglelineentry.getLook());
    }

    /**
     * Test {set|get}Move/setFocusTraversal
     * <ul>
     * <li>The set move should be the retreived move
     * <li>Setting a move to null should remove the traversal
     * <li>setFocusTraversal should set the correct keys
     * </ul>
     */
    public void testMove()
    {
        HNavigableTest.testMove(hsinglelineentry);
    }

    /**
     * Test isSelected
     * <ul>
     * <li>Should be getInteractionState()==FOCUSED_STATE
     * </ul>
     */
    public void testSelected()
    {
        HNavigableTest.testSelected(hsinglelineentry);
    }

    /**
     * Test {get|set}{Lose|Gain}FocusSound.
     * <ul>
     * <li>Ensure that the set sound is the retreived sound
     * <li>Tests set{Lose|Gain}Sound(null)
     * <li>Test that the sound is played when the component gains|loses focus
     * </ul>
     */
    public void testFocusSound()
    {
        HNavigableTest.testFocusSound(hsinglelineentry);
    }

    /**
     * Tests getNavigationKeys().
     */
    public void testNavigationKeys()
    {
        HNavigableTest.testNavigationKeys(hsinglelineentry);
    }

    /**
     * Tests add/removeHFocusListener().
     */
    public void testFocusListener()
    {
        HNavigableTest.testFocusListener(hsinglelineentry);
    }

    /**
     * Tests proper state traversal as a result of focus events.
     */
    public void testProcessHFocusEvent()
    {
        HNavigableTest.testProcessHFocusEvent(hsinglelineentry);
    }

    /**
     * Test setDefaultLook/getDefaultLook.
     * <ul>
     * <li>Only HSinglelineEntryLook should be accepted.
     * <li>The set look should be the retrieved look.
     * <li>newly created HSinglelineEntrys should use the new default look
     * </ul>
     */
    public void testDefaultLook() throws HInvalidLookException
    {
        HSinglelineEntryLook save = HSinglelineEntry.getDefaultLook();
        try
        {
            HSinglelineEntryLook newLook = new HSinglelineEntryLook();
            hsinglelineentry.setLook(newLook);
            assertEquals("Set look should equal retrieved look", newLook, hsinglelineentry.getLook());

            HSinglelineEntry entryTest = new HSinglelineEntry();
            assertEquals("A newly created HSinglelineEntry should use default look", entryTest.getDefaultLook(),
                    entryTest.getLook());
        }
        finally
        {
            // reset
            HSinglelineEntry.setDefaultLook(save);
        }
    }

    /**
     * Tests {set|get}TextContent().
     * <ul>
     * <li>Only one piece of content is supported for ALL states
     * <li>The string is truncated according to the current maxChars setting
     * <li>The caret should be set to the end of the string
     * <li>Using null as the text content should erase the current content
     * <li>The set content should be the retrieved content (unless truncated)
     * <li>The state parameter should be ignored and assumed to be ALL_STATES
     * </ul>
     */
    public void testTextContent()
    {
        final String[] strings = { "", "x", "This is a test", "This is a longer test", "$@%^&!", "", };

        for (int i = 0; i < strings.length; ++i)
        {
            hsinglelineentry.setTextContent(strings[i], HState.DISABLED_STATE);

            int maxChars = hsinglelineentry.getMaxChars();
            final String truncated = (strings[i].length() <= maxChars) ? strings[i] : strings[i].substring(0, maxChars);
            foreachState(new Callback()
            {
                public void callback(int state)
                {
                    assertEquals(stateToString(state) + " unexpected text content set", truncated,
                            hsinglelineentry.getTextContent(state));
                }
            });

            assertEquals("Caret position should be at end of text content", truncated.length(),
                    hsinglelineentry.getCaretCharPosition());
        }
    }

    /**
     * Tests {get|set}CaretCharPosition().
     * <ul>
     * <li>Valid values range from [0,text.length] inclusive
     * <li>The set position should be the returned position
     * <li>Test invalid settings
     * <li>Test proper operation
     * </ul>
     */
    public void testCaretCharPosition()
    {
        hsinglelineentry.setTextContent("This is a test", 0);

        hsinglelineentry.setCaretCharPosition(2);
        assertEquals("Set caret position should be the retrieved caret position", 2,
                hsinglelineentry.getCaretCharPosition());

        hsinglelineentry.setCaretCharPosition(-1);
        assertEquals("Negative caret position should be the changed to zero", 0,
                hsinglelineentry.getCaretCharPosition());

        hsinglelineentry.setCaretCharPosition(32767);
        assertEquals("Caret position out of range should be the set to last caret position",
                hsinglelineentry.getTextContent(0).length(), hsinglelineentry.getCaretCharPosition());
    }

    private void reInitComponent()
    {
        hsinglelineentry.setTextContent("", 0);
        hsinglelineentry.setCaretCharPosition(0);
        hsinglelineentry.setMaxChars(200);
    }

    private static final int types[] = new int[] { HKeyboardInputPreferred.INPUT_ALPHA,
            HKeyboardInputPreferred.INPUT_NUMERIC, HKeyboardInputPreferred.INPUT_ANY,
            HKeyboardInputPreferred.INPUT_CUSTOMIZED, };

    /**
     * Tests {set|get}Type().
     * <ul>
     * <li>Valid values are ANY, ALPHANUMERIC, or NUMERIC
     * <li>The set value should be the returned value
     * <li>Test proper operation
     * </ul>
     */
    public void testType()
    {
        reInitComponent();
        hsinglelineentry.setType(HKeyboardInputPreferred.INPUT_NUMERIC);
        assertEquals("Set input type should be retrieved input type (INPUT_NUMERIC)",
                HKeyboardInputPreferred.INPUT_NUMERIC, hsinglelineentry.getType());
        for (int x = '0'; x <= 'z'; x++)
        {
            char insert = (char) x;
            if (Character.isDigit(insert))
                assertTrue("Type is INPUT_NUMERIC:  Trying to insert any numeric character should work",
                        hsinglelineentry.insertChar(insert));
            else
                assertTrue("Type is INPUT_NUMERIC:  Trying to insert any non-numeric character should fail",
                        !hsinglelineentry.insertChar(insert));
        }

        reInitComponent();
        hsinglelineentry.setType(HKeyboardInputPreferred.INPUT_ALPHA | HKeyboardInputPreferred.INPUT_NUMERIC);
        assertEquals("Set input type should be retrieved input type (INPUT_ALPHA|INPUT_NUMERIC)",
                HKeyboardInputPreferred.INPUT_ALPHA | HKeyboardInputPreferred.INPUT_NUMERIC, hsinglelineentry.getType());
        for (int x = '0'; x <= 'z'; x++)
        {
            char insert = (char) x;
            if (Character.isLetterOrDigit(insert))
                assertTrue("Type is INPUT_ALPHANUMERIC:  Trying to insert any alphanumeric character should work",
                        hsinglelineentry.insertChar(insert));
            else
                assertTrue("Type is INPUT_ALPHANUMERIC:  Trying to insert any non-alphanumeric character should fail",
                        !hsinglelineentry.insertChar(insert));
        }

        reInitComponent();
        hsinglelineentry.setType(HKeyboardInputPreferred.INPUT_ALPHA);
        assertEquals("Set input type should be retrieved input type (INPUT_ALPHA)",
                HKeyboardInputPreferred.INPUT_ALPHA, hsinglelineentry.getType());
        for (int x = '0'; x <= 'z'; x++)
        {
            char insert = (char) x;
            if (Character.isLetter(insert))
                assertTrue("Type is INPUT_ALPHA:  Trying to insert any alpha character should work",
                        hsinglelineentry.insertChar(insert));
            else
                assertTrue("Type is INPUT_ALPHA:  Trying to insert any non-alpha character should fail",
                        !hsinglelineentry.insertChar(insert));
        }

        reInitComponent();
        hsinglelineentry.setType(HKeyboardInputPreferred.INPUT_CUSTOMIZED);
        assertEquals("Set input type should be retrieved input type (INPUT_CUSTOMIZED)",
                HKeyboardInputPreferred.INPUT_CUSTOMIZED, hsinglelineentry.getType());
        hsinglelineentry.setValidInput(new char[] { 'A', 'B' });
        for (int x = '0'; x <= 'z'; x++)
        {
            char insert = (char) x;

            if (insert == 'A' || insert == 'B')
                assertTrue("Type is INPUT_CUSTOMIZED:  Trying to insert 'A' or 'B' should work",
                        hsinglelineentry.insertChar(insert));
            else
                assertTrue(
                        "Type is INPUT_CUSTOMIZED:  Trying to insert any character other that 'A' or 'B' should fail",
                        !hsinglelineentry.insertChar(insert));
        }

        reInitComponent();
        hsinglelineentry.setType(HKeyboardInputPreferred.INPUT_ANY);
        assertEquals("Set input type should be retrieved input type (INPUT_ANY)", HKeyboardInputPreferred.INPUT_ANY,
                hsinglelineentry.getType());
        for (int x = '0'; x <= 'z'; x++)
        {
            assertTrue("Type is INPUT_ANY:  Trying to insert any of these characters should work",
                    hsinglelineentry.insertChar((char) x));
        }

        /* Test type combinations (beyond ALPHA/NUMERIC). */
        assertEquals("unexpected INPUT_NUMERIC constant", 1, HSinglelineEntry.INPUT_NUMERIC);
        assertEquals("unexpected INPUT_ALPHA constant", 2, HSinglelineEntry.INPUT_ALPHA);
        assertEquals("unexpected INPUT_ANY constant", 4, HSinglelineEntry.INPUT_ANY);
        assertEquals("unexpected INPUT_CUSTOMIZED constant", 8, HSinglelineEntry.INPUT_CUSTOMIZED);

        for (int type = 0; type < 16; ++type)
        {
            hsinglelineentry.setTextContent("", HState.ALL_STATES);
            hsinglelineentry.setType(type);
            hsinglelineentry.setValidInput(new char[0]);
            hsinglelineentry.setInteractionState(HState.FOCUSED_STATE);
            hsinglelineentry.setEditMode(true);

            // Alpha
            doTestType(hsinglelineentry, type, 'a');
            doTestType(hsinglelineentry, type, 'z');
            doTestType(hsinglelineentry, type, 'A');
            doTestType(hsinglelineentry, type, 'Z');

            // Numeric
            doTestType(hsinglelineentry, type, '0');
            doTestType(hsinglelineentry, type, '9');

            // Other
            doTestType(hsinglelineentry, type, ',');
            doTestType(hsinglelineentry, type, '<');
            doTestType(hsinglelineentry, type, '`');
            doTestType(hsinglelineentry, type, '~');
            doTestType(hsinglelineentry, type, '!');
            doTestType(hsinglelineentry, type, '\0');
            doTestType(hsinglelineentry, type, '\u1234');
            doTestType(hsinglelineentry, type, ' ');

            // Customized
            hsinglelineentry.setValidInput(new char[] { 'b', '*', '7', '\n' });
            doTestType(hsinglelineentry, type, 'b');
            doTestType(hsinglelineentry, type, 'B');
            doTestType(hsinglelineentry, type, '*');
            doTestType(hsinglelineentry, type, '8');
            doTestType(hsinglelineentry, type, '7');
            doTestType(hsinglelineentry, type, '&');
            doTestType(hsinglelineentry, type, '\n');
        }
    }

    /**
     * Searches an array for a character.
     */
    private boolean isInArray(char[] array, char c)
    {
        if (array == null) return false;
        for (int i = 0; i < array.length; ++i)
        {
            if (c == array[i]) return true;
        }
        return false;
    }

    /**
     * Performs testType() for a type/char combination.
     */
    private void doTestType(HSinglelineEntry sle, int type, char c)
    {
        boolean inserted = sle.insertChar(c);
        boolean expected = false;

        assertEquals("Content should reflect the inserted nature (" + type + ",'" + c + "')", sle.getTextContent(
                HState.NORMAL_STATE).length() == 1, inserted);

        expected = ((0 != (type & HKeyboardInputPreferred.INPUT_ANY) && Character.isDefined(c))
                || (0 != (type & HKeyboardInputPreferred.INPUT_NUMERIC) && Character.isDigit(c))
                || (0 != (type & HKeyboardInputPreferred.INPUT_ALPHA) && Character.isLetter(c)) || (0 != (type & HKeyboardInputPreferred.INPUT_CUSTOMIZED) && isInArray(
                sle.getValidInput(), c)));

        String shouldve = expected ? "should've" : "should not have";
        assertEquals("The textValue type of " + type + " " + shouldve + " allowed insertion of '" + c + "'", expected,
                inserted);

        sle.deletePreviousChar();
    }

    /**
     * Tests insertChar().
     * <ul>
     * <li>Inserts a char at the current caret position, subject to the maxChars
     * limit
     * </ul>
     */
    public void testInsertChar()
    {
        char insert = 'B';

        hsinglelineentry.setCaretCharPosition(4);
        assertTrue("Character should be inserted in HSinglelineEntry", hsinglelineentry.insertChar(insert));
    }

    /**
     * Tests delete{Next|Previous}Char().
     * <ul>
     * <ul>
     * <li><b>previous</b>
     * <li>Delete the char behind the current caret position
     * <li>returns true if a char was deleted (i.e., there was one to delete)
     * </ul>
     * <ul>
     * <li><b>next</b>
     * <li>Delete the char forward of the current caret position
     * <li>returns true if a char was deleted (i.e., caret not at eol)
     * </ul>
     * </ul>
     */
    public void testCaretDelete()
    {
        // Set some text content
        hsinglelineentry.setTextContent("This is a test", HState.NORMAL_STATE);

        hsinglelineentry.setCaretCharPosition(1);
        assertTrue("Previous character should've been deleted", hsinglelineentry.deletePreviousChar());

        hsinglelineentry.setCaretCharPosition(0);
        assertTrue("Next character should've been deleted", hsinglelineentry.deleteNextChar());

        hsinglelineentry.setCaretCharPosition(0);
        assertTrue("Previous character couldn't be deleted", !hsinglelineentry.deletePreviousChar());

        hsinglelineentry.setCaretCharPosition(Integer.MAX_VALUE);
        assertTrue("Next character couldn't be deleted", !hsinglelineentry.deleteNextChar());
    }

    /**
     * Tests caret{Next|Previous}Character().
     * <ul>
     * <li>advances the caret to the next/previous char
     * </ul>
     */
    public void testCaretMove()
    {
        // Set some text content
        hsinglelineentry.setTextContent("This is a test", HState.NORMAL_STATE);

        hsinglelineentry.setCaretCharPosition(0);
        hsinglelineentry.caretNextCharacter();
        assertEquals("Next character position of 0 should be 1", 1, hsinglelineentry.getCaretCharPosition());

        hsinglelineentry.setCaretCharPosition(1);
        hsinglelineentry.caretPreviousCharacter();
        assertEquals("Previous character position of 1 should be 0", 0, hsinglelineentry.getCaretCharPosition());

        int len = hsinglelineentry.getTextContent(HState.NORMAL_STATE).length();
        hsinglelineentry.setCaretCharPosition(len);
        hsinglelineentry.caretNextCharacter();
        assertEquals("Next character position of last position should stay last position", len,
                hsinglelineentry.getCaretCharPosition());

        hsinglelineentry.setCaretCharPosition(0);
        hsinglelineentry.caretPreviousCharacter();
        assertEquals("Previous character position of 0 should stay 0", 0, hsinglelineentry.getCaretCharPosition());
    }

    /**
     * Tests {set|get}EditMode().
     * <ul>
     * <li>The set mode should be the retrieved mode.
     * <li>
     * </ul>
     */
    public void testEditMode()
    {
        assertTrue("HSinglelineEntry should NOT be in edit mode", !hsinglelineentry.getEditMode());
        hsinglelineentry.setEditMode(true);
        assertTrue("HSinglelineEntry SHOULD be in edit mode", hsinglelineentry.getEditMode());
        hsinglelineentry.setEditMode(false);
        assertTrue("HSinglelineEntry should NOT be in edit mode", !hsinglelineentry.getEditMode());
    }

    /**
     * Tests {set|get}MaxChars().
     * <ul>
     * <li>The set maxChars should be the retrieved maxChars
     * <li>Truncation to the set maxChars is tested in
     * {@link #testTextContent()}.
     * </ul>
     */
    public void testMaxChars()
    {
        int newMax = 25;

        hsinglelineentry.setMaxChars(newMax);
        assertEquals("Set max chars per line should be the retrieved max chars per line", newMax,
                hsinglelineentry.getMaxChars());

        hsinglelineentry.setMaxChars(newMax + 5);
        assertEquals("Set max chars per line should be the retrieved max chars per line", newMax + 5,
                hsinglelineentry.getMaxChars());

        hsinglelineentry.setMaxChars(newMax - 5);
        assertEquals("Set max chars per line should be the retrieved max chars per line", newMax - 5,
                hsinglelineentry.getMaxChars());
    }

    /**
     * Tests getEchoChar()/setEchoChar()/echoCharIsSet().
     * <ul>
     * <li>echoCharIsSet() == (getEchoChar() == '\0')
     * <li>The set echo echo is the retrieved echo char
     * </ul>
     */
    public void testEchoChar()
    {
        assertEquals("Echo character should default to '\\0\'", '\0', hsinglelineentry.getEchoChar());

        hsinglelineentry.setEchoChar('X');
        assertEquals("Set echo character should equal retrieved echo character", 'X', hsinglelineentry.getEchoChar());

        hsinglelineentry.setEchoChar('\0');
    }

    /**
     * Tests going into edit mode via HUIEvent.VK_START_CHANGE and non-edit mode
     * via HUIEvent.VK_END_CHANGE.
     * <ul>
     * <li>When in edit mode, all keyboard events will be received by the single
     * line entry
     * </ul>
     */
    public void testEdit()
    {
        assertTrue("HSinglelineEntry should NOT be in edit mode", !hsinglelineentry.getEditMode());

        hsinglelineentry.processHTextEvent(new HTextEvent(hsinglelineentry, HTextEvent.TEXT_START_CHANGE));

        assertTrue("HSinglelineEntry SHOULD be in edit mode", hsinglelineentry.getEditMode());

        hsinglelineentry.processHTextEvent(new HTextEvent(hsinglelineentry, HTextEvent.TEXT_END_CHANGE));

        assertTrue("HSinglelineEntry should NOT be in edit mode", !hsinglelineentry.getEditMode());
    }

    private void compareValidInputArrays(String title, char[] retrieved, char[] set)
    {
        boolean found;

        assertEquals(title + " Set Valid input array length should equal retrieved valid input array length",
                set.length, retrieved.length);

        for (int x = 0; x < retrieved.length; x++)
        {
            found = false;

            for (int y = 0; (y < set.length) && !found; y++)
            {
                if (retrieved[x] == set[y]) found = true;
            }
            assertTrue(title + "  The retrieved character \'" + retrieved[x]
                    + "\' was not found in the valid input array", found);
        }
    }

    /**
     * Tests {set|get}ValidInput().
     * <ul>
     * <li>Tests whether the set valid input characters are the retrieved valid
     * input characters.
     * </ul>
     */
    public void testValidInput()
    {
        char[] test, validInput = new char[] { 'B', '5', '%', '&', '>', '+', 'L' };
        char[] validInput2 = new char[] { 'A', ']', 'm', 'n', '.', 'k', 'd', 'P', '0', '^' };
        char[] validInput3 = new char[] { 'y', 'Q', 'z', 'X', '?', '|', ')', 'H', 'J', '<', '1', 'F', 'B', 'c', 'v',
                '*', '$', '@', 'U' };

        hsinglelineentry.setValidInput(validInput);
        hsinglelineentry.setType(HKeyboardInputPreferred.INPUT_CUSTOMIZED);
        test = hsinglelineentry.getValidInput();
        compareValidInputArrays("Test 1:", test, validInput);

        hsinglelineentry.setValidInput(validInput2);
        test = hsinglelineentry.getValidInput();
        compareValidInputArrays("Test 2:", test, validInput2);

        hsinglelineentry.setValidInput(validInput3);
        test = hsinglelineentry.getValidInput();
        compareValidInputArrays("Test 3:", test, validInput3);
    }

    /**
     * Tests add/removeHKeyListener().
     * <ul>
     * <li>Test that listener gets called
     * <li>Ensure that it doesn't after being removed.
     * </ul>
     */
    public void testKeyListener()
    {
        HTextValueTest.testKeyListener(hsinglelineentry);
    }

    /**
     * Tests add/removeHTextListener().
     * <ul>
     * <li>Test that listener gets called
     * <li>Ensure that it doesn't after being removed.
     * </ul>
     */
    public void testTextListener()
    {
        HTextValueTest.testTextListener(hsinglelineentry);
    }

    /**
     * Create an HComponent of the appropriate class type that, in response to
     * HAVi Events, will set the generated[0] element to true.
     * <p>
     * The special component should (where appropriate) override:
     * <ul>
     * <li>processHFocusEvent
     * <li>processHTextEvent
     * <li>processHKeyEvent
     * </ul>
     * <p>
     * This is necessary because HNavigable and HTextValue components are not
     * required to support HFocusListeners.
     * 
     * @param ev
     *            a helper object used to test the event generation
     * @see #testProcessEvent
     */
    protected HComponent createSpecialComponent(final EventCheck ev)
    {
        checkClass(HSinglelineEntryTest.class);

        return new HSinglelineEntry()
        {
            public void processHFocusEvent(org.havi.ui.event.HFocusEvent e)
            {
                ev.validate(e);
            }

            public void processHTextEvent(org.havi.ui.event.HTextEvent e)
            {
                ev.validate(e);
            }

            public void processHKeyEvent(org.havi.ui.event.HKeyEvent e)
            {
                ev.validate(e);
            }
        };
    }
}
