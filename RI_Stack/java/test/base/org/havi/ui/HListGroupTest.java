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

import org.havi.ui.event.*;

import org.cablelabs.test.*;
import java.awt.*;
import java.util.Vector;

/**
 * Tests {@link #HListGroup}.
 * 
 * @author Aaron Kamienski
 * @version $Revision: 1.10 $, $Date: 2002/11/07 21:14:07 $
 */
public class HListGroupTest extends HVisibleTest
{
    /**
     * Standard constructor.
     */
    public HListGroupTest(String str)
    {
        super(str);
    }

    /**
     * Standalone runner.
     */
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(HListGroupTest.class);
    }

    /**
     * Creates a look of the appropriate type. Should be overridden by
     * subclasses.
     */
    protected HLook createLook()
    {
        return new HListGroupLook()
        {
        };
    }

    /**
     * The tested component.
     */
    protected HListGroup hlistgroup;

    /**
     * Should be overridden to create subclass of HListGroup.
     * 
     * @return the instance of HListGroup to test
     */
    protected HListGroup createHListGroup()
    {
        return new HListGroup();
    }

    /**
     * Overridden to create an HListGroup.
     * 
     * @return the instance of HVisible to test
     */
    protected HVisible createHVisible()
    {
        return (hlistgroup = createHListGroup());
    }

    /**
     * Tests for correct ancestry.
     * <ul>
     * <li>extends HVisible
     * <li>implements HItemValue
     * </ul>
     */
    public void testAncestry()
    {
        checkClass(HListGroupTest.class);

        TestUtils.testExtends(HListGroup.class, HVisible.class);
        HItemValueTest.testAncestry(HListGroup.class);
    }

    /**
     * Test the 3 constructors of HListGroup.
     * <ul>
     * <li>HListGroup()
     * <li>HListGroup(HListElement[] items)
     * <li>HListGroup(HListElement[] items, int x, int y, int w, int h)
     * </ul>
     */
    public void testConstructors()
    {
        checkClass(HListGroupTest.class);

        HListElement[] items = null;

        checkConstructor("HListGroup()", new HListGroup(), items, 0, 0, 0, 0, false);
        items = createContent(1);
        checkConstructor("HListGroup(HListElement[])", new HListGroup(items), items, 0, 0, 0, 0, false);
        items = createContent(5);
        checkConstructor("HListGroup(HListElement[], int x, int y, int w, int h)",
                new HListGroup(items, 10, 20, 30, 40), items, 10, 20, 30, 40, true);
    }

    /**
     * Check for proper initialization of constructor values.
     */
    private void checkConstructor(String msg, HListGroup list, HListElement[] items, int x, int y, int w, int h,
            boolean defaultSize)
    {
        // Check variables exposed in constructors
        assertNotNull(msg + " not allocated", list);
        assertEquals(msg + " x-coordinated not initialized correctly", x, list.getLocation().x);
        assertEquals(msg + " y-coordinated not initialized correctly", y, list.getLocation().y);
        assertEquals(msg + " width not initialized correctly", w, list.getSize().width);
        assertEquals(msg + " height not initialized correctly", h, list.getSize().height);
        assertEquals(msg + " numItems not initialized correctly", (items == null) ? 0 : items.length,
                list.getNumItems());
        if (items == null)
            assertNull(msg + " list content not initialized correctly", list.getListContent());
        else
        {
            for (int i = 0; i < items.length; ++i)
            {
                assertSame(msg + " elements not initialized correctly", items[i], list.getItem(i));
            }
        }

        // Check variables NOT exposed in constructors
        assertEquals(msg + " should be NORMAL_STATE", NORMAL_STATE, list.getInteractionState());
        assertNull(msg + " matte should be unassigned", list.getMatte());
        assertNotNull(msg + " text layout mgr should be assigned", list.getTextLayoutManager());
        assertEquals(msg + " bg mode not initialized incorrectly", list.getBackgroundMode(), list.NO_BACKGROUND_FILL);
        if (!defaultSize)
            assertEquals(msg + " default size should not be set", list.NO_DEFAULT_SIZE, list.getDefaultSize());
        // assertNull(msg+" default size should not be set",
        // list.getDefaultSize());
        else
            assertEquals(msg + " default size initialized incorrectly", list.getDefaultSize(), new Dimension(w, h));
        assertEquals(msg + " horiz alignment initialized incorrectly", list.getHorizontalAlignment(),
                list.HALIGN_CENTER);
        assertEquals(msg + " vert alignment initialized incorrectly", list.getVerticalAlignment(), list.VALIGN_CENTER);
        assertEquals(msg + " resize mode initialized incorrectly", list.getResizeMode(), list.RESIZE_NONE);
        assertSame(msg + " default look not used", HListGroup.getDefaultLook(), list.getLook());
        assertNull(msg + " gain focus sound incorrectly initialized", list.getGainFocusSound());
        assertNull(msg + " lose focus sound incorrectly initialized", list.getLoseFocusSound());
        assertNull(msg + " selection sound incorrectly initialized", list.getSelectionSound());
        // while the default is null, getIconSize always returns a Dimension
        assertEquals(msg + " icon size incorrectly initialized", new Dimension(list.DEFAULT_ICON_WIDTH,
                list.DEFAULT_ICON_HEIGHT), list.getIconSize());
        // while the default is null, getLabelSize always returns a Dimension
        assertEquals(msg + " label size incorrectly initialized", new Dimension(list.DEFAULT_LABEL_WIDTH,
                list.DEFAULT_LABEL_HEIGHT), list.getLabelSize());
        assertEquals(msg + " border mode incorrectly initialized", true, list.getBordersEnabled());
        assertEquals(msg + " orientation incorrectly initialized", HOrientable.ORIENT_TOP_TO_BOTTOM,
                list.getOrientation());
        assertEquals(msg + " multiSelection incorrectly initialized", false, list.getMultiSelection());
        assertEquals(msg + " selection mode incorrectly initialized", false, list.getSelectionMode());
        assertEquals(msg + " currentIndex incorrectly initialized", ((items != null && items.length > 0) ? 0
                : HListGroup.ITEM_NOT_FOUND), list.getCurrentIndex());
        assertSame(msg + " currentItem incorrectly initialized",
                ((items != null && items.length > 0) ? items[0] : null), list.getCurrentItem());
        assertEquals(msg + " scrollPosition incorrectly initialized", ((items != null && items.length > 0) ? 0
                : HListGroup.ITEM_NOT_FOUND), list.getScrollPosition());
        assertNull(msg + " selection incorrectly initialized", list.getSelection());
    }

    /**
     * Tests for any exposed non-final fields or added final fields.
     */
    public void testFields()
    {
        TestUtils.testNoAddedFields(getTestedClass(), fields);
    }

    private static final String[] fields = { "ADD_INDEX_END", "DEFAULT_ICON_HEIGHT", "DEFAULT_ICON_WIDTH",
            "DEFAULT_LABEL_HEIGHT", "DEFAULT_LABEL_WIDTH", "ITEM_NOT_FOUND", };

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
        HNavigableTest.testMove(hlistgroup);
    }

    /**
     * Test isSelected
     * <ul>
     * <li>Should be getInteractionState()==FOCUSED_STATE
     * </ul>
     */
    public void testSelected()
    {
        HNavigableTest.testSelected(hlistgroup);
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
        HNavigableTest.testFocusSound(hlistgroup);
    }

    /**
     * Tests getNavigationKeys().
     */
    public void testNavigationKeys()
    {
        HNavigableTest.testNavigationKeys(hlistgroup);
    }

    /**
     * Tests add/removeHFocusListener().
     */
    public void testFocusListener()
    {
        HNavigableTest.testFocusListener(hlistgroup);
    }

    /**
     * Tests proper state traversal as a result of focus events.
     */
    public void testProcessHFocusEvent()
    {
        HNavigableTest.testProcessHFocusEvent(hlistgroup);
    }

    /**
     * Test setDefaultLook/getDefaultLook.
     * <ul>
     * <li>Only HGraphicLook should be accepted.
     * <li>The set look should be the retreived look.
     * <li>newly created HListGroups should use the new default look
     * </ul>
     */
    public void testDefaultLook() throws HInvalidLookException
    {
        // checkClass(HListGroupTest.class);

        assertSame("Default look should be used", HListGroup.getDefaultLook(), (new HListGroup()).getLook());

        HListGroupLook save = HListGroup.getDefaultLook();

        try
        {
            HListGroupLook look;

            HListGroup.setDefaultLook(look = new HListGroupLook());
            assertSame("Incorrect look retrieved", look, HListGroup.getDefaultLook());
            assertSame("Default look should be used", look, (new HListGroup()).getLook());

            /*
             * // Not possible, because signature doesn't allow it try {
             * HListGroup.setDefaultLook(new HVisibleTest.EmptyLook());
             * fail("Invalid look accepted"); } catch(HInvalidLookException
             * ignored) {}
             */
        }
        finally
        {
            // reset
            HListGroup.setDefaultLook(save);
        }
    }

    /**
     * Test (set|get)SelectionMode().
     * <ul>
     * <li>the set mode should be the retrieved mode
     * </ul>
     */
    public void testSelectionMode()
    {
        HItemValueTest.testSelectionMode(hlistgroup);
    }

    /**
     * Test processHItemEvent().
     * 
     * <ul>
     * <li>When in focus, an ITEM_START_CHANGE causes it to enter selection mode
     * <li>When in focus, an ITEM_START_END causes it to leave selection mode
     * <li>When the component has focus and is in selection mode, the current
     * item can be set by sending ITEM_SET_CURRENT, ITEM_SET_PREVIOUS and
     * ITEM_SET_NEXT events to the component
     * <li>When the component has focus and is in selection mode, sending an
     * ITEM_TOGGLE_SELECTED event causes the current item to be toggled between
     * a selected and unselected state
     * <li>Irrespective of focus and selection mode, sending an
     * ITEM_SELECTION_CLEARED event to the component causes the current
     * selection set to be cleared. The position of the current item is
     * unchanged
     * </ul>
     */
    public void testProcessHItemEvent() throws Exception
    {
        HItemValueTest.testProcessHItemEvent(hlistgroup);
    }

    /**
     * Test (add|remove)ItemListener().
     * <ul>
     * <li>Test that the listener gets called
     * <li>Ensure that it doesn't after being removed
     * </ul>
     */
    public void testItemListener()
    {
        HItemValueTest.testItemListener(hlistgroup);
    }

    /**
     * Test (set|get)SelectionSound().
     * <ul>
     * <li>Tests the default value (most likely null)
     * <li>Ensures that the set sound is the retreived sound
     * <li>Tests setSelectionSound(null)
     * <li>Test that the sound is played when the item is selected
     * </ul>
     */
    public void testSelectionSound()
    {
        HItemValueTest.testSelectionSound(hlistgroup);
    }

    /**
     * Tests (set|get)Orientation().
     */
    public void testOrientation() throws Exception
    {
        HItemValueTest.testOrientation(hlistgroup);
    }

    /**
     * Test (set|get)ListContent().
     * <ul>
     * <li>Ensures that what is put in is retreived
     * <li>Ensures that content can be removed with <code>null</code>
     * <li>Setting the content should discard any existing selection
     * <li>(resulting in a selectionChanged event)
     * <li>HChangeData(LIST_CONTENT_CHANGE, oldContent[]);
     * </ul>
     */
    public void testListContent() throws Exception
    {
        HListGroup lg = hlistgroup;
        final String hintName = "LIST_CONTENT_CHANGE";
        final HChangeData[] hcd = new HChangeData[1];
        createWidgetChangeLook(lg, HVisible.LIST_CONTENT_CHANGE, hintName, hcd);

        HListElement[] content = null, oldContent = null;

        resetListener();

        // Initial content should be null
        assertNull("Initial listContent should be null", lg.getListContent());

        // Add initial content and check it
        content = setContent();
        Vector v = new Vector();
        addToVector(content, v, -1);
        checkContent(lg.getListContent(), v);
        assertNull("No events should've been generated", selectionEvent);
        assertNotNull("A current item changed event should've been generated", currentEvent);
        assertSame("The first element should've been made current", content[0], currentEvent.getItem());
        // check HChangeData
        assertNotNull(hintName + " change data expected", hcd[0]);
        assertNull("Old content expected to be null", hcd[0].data);
        oldContent = content;

        // Replace w/ new content
        // Check for clearance of selection
        lg.setItemSelected(0, true);
        content = new HListElement[] { new HListElement("a"), new HListElement("b"), new HListElement("c"), };
        resetListener();
        v.removeAllElements();
        addToVector(content, v, -1);
        lg.setListContent(content);
        // check items
        checkContent(lg.getListContent(), v);
        // check event
        assertNotNull("A selection changed event should've been sent", selectionEvent);
        assertEquals("A SELECTION_CLEARED event should've been sent", HItemEvent.ITEM_SELECTION_CLEARED,
                selectionEvent.getID());
        // check HChangeData
        assertNotNull(hintName + " change data expected", hcd[0]);
        assertArraySame("Old content expected", oldContent, (Object[]) hcd[0].data);
        oldContent = content;

        // Remove content w/ null
        lg.setListContent(null);
        assertNull("All content should be removed", lg.getListContent());
        assertEquals("Current active index should be ITEM_NOT_FOUND", HListGroup.ITEM_NOT_FOUND, lg.getCurrentIndex());
        // check HChangeData
        assertNotNull(hintName + " change data expected", hcd[0]);
        assertArraySame("Old content expected", oldContent, (Object[]) hcd[0].data);
        oldContent = null;

        // Remove content w/ array of length 0
        lg.setListContent(new HListElement[0]);
        assertNull("All content should be removed", lg.getListContent());
        assertEquals("Current active index should be ITEM_NOT_FOUND", HListGroup.ITEM_NOT_FOUND, lg.getCurrentIndex());
        // check HChangeData
        assertNotNull(hintName + " change data expected", hcd[0]);
        assertNull("Old content expected to be null", hcd[0].data);
        oldContent = null;
    }

    /**
     * Test addItem()
     * <ul>
     * <li>The added item should be available at index+1
     * <li>If no content exists, new content is created and the index is ignored
     * <li>If the insertion changes the current active index, an HItemEvent
     * should be sent
     * <li>ADD_INDEX_END means append to the end of the list
     * <li>If the index is invalid, an IndexOutOfBoundsException should be
     * thrown
     * <li>getNumItems() should return an accurate number
     * </ul>
     */
    public void testAddItem() throws Exception
    {
        HListGroup lg = hlistgroup;
        Vector v = new Vector();

        final String hintName = "LIST_CONTENT_CHANGE";
        final HChangeData[] hcd = new HChangeData[1];
        createWidgetChangeLook(lg, HVisible.LIST_CONTENT_CHANGE, hintName, hcd);
        HListElement[] oldContent = null;

        HListElement e = null;

        // Add a single element
        // Index should be ignored if no content
        try
        {
            lg.addItem(e = new HListElement("a"), 5);
        }
        catch (IndexOutOfBoundsException ex)
        {
            fail("Index should be ignored if no content exists");
        }
        addToVector(e, v, 5);
        checkContent(lg, v);
        // check HChangeData
        assertNotNull(hintName + " change data expected", hcd[0]);
        assertNull("Old content expected to be null", hcd[0].data);
        oldContent = lg.getListContent();
        hcd[0] = null;

        // Try to add an element out of range
        try
        {
            lg.addItem(e = new HListElement("z"), lg.getNumItems() + 1);
            fail("Expected an IndexOutOfBoundsException for index > n");
        }
        catch (IndexOutOfBoundsException expected)
        {
        }
        assertNull("No calls to widgetChanged expected", hcd[0]);
        try
        {
            lg.addItem(e = new HListElement("z"), -20);
            fail("Expected an IndexOutOfBoundsException for index > n");
        }
        catch (IndexOutOfBoundsException expected)
        {
        }
        assertNull("No calls to widgetChanged expected", hcd[0]);

        // Add an element at the FRONT of the list
        // (Actually, not possible in 1.01b)
        lg.addItem(e = new HListElement("b"), 0);
        addToVector(e, v, 0);
        checkContent(lg, v);
        // check HChangeData
        assertNotNull(hintName + " change data expected", hcd[0]);
        assertArraySame("Old content expected", oldContent, (Object[]) hcd[0].data);
        oldContent = lg.getListContent();
        hcd[0] = null;

        // Add an element at the END of the list
        lg.addItem(e = new HListElement("c"), HListGroup.ADD_INDEX_END);
        addToVector(e, v, -1);
        checkContent(lg, v);
        // check HChangeData
        assertNotNull(hintName + " change data expected", hcd[0]);
        assertArraySame("Old content expected", oldContent, (Object[]) hcd[0].data);
        oldContent = lg.getListContent();
        hcd[0] = null;

        // An an element in the middle of the list
        lg.addItem(e = new HListElement("d"), 1);
        addToVector(e, v, 1);
        checkContent(lg, v);
        // check HChangeData
        assertNotNull(hintName + " change data expected", hcd[0]);
        assertArraySame("Old content expected", oldContent, (Object[]) hcd[0].data);
        oldContent = lg.getListContent();
        hcd[0] = null;

        // Check for HItemEvent if current index gets changed
        lg.setCurrentItem(lg.getNumItems() - 1);
        resetListener();
        lg.addItem(e = new HListElement("e"), 0);
        assertNotNull("A current item changed event should've been sent", currentEvent);
        assertEquals("An SET_CURRENT event should've been sent", HItemEvent.ITEM_SET_CURRENT, currentEvent.getID());
        assertSame("Unexpected list item specified in SET_CURRENT event", lg.getItem(lg.getNumItems() - 1),
                currentEvent.getItem());
        // check HChangeData
        assertNotNull(hintName + " change data expected", hcd[0]);
        assertArraySame("Old content expected", oldContent, (Object[]) hcd[0].data);
        // oldContent = lg.getListContent();
        // hcd[0] = null;
    }

    /**
     * Test addItems(HListElement[], index)
     * <ul>
     * <li>The added items should be available at index+1 through
     * index+array.length
     * <li>All of the items should be added, in order
     * <li>If no content exists, new content is created and the index is ignored
     * <li>If the insertion changes the current active index, an HItemEvent
     * should be sent
     * <li>ADD_INDEX_END means append to the end of the list
     * <li>If the index is invalid, an IndexOutOfBoundsException should be
     * thrown
     * <li>getNumItems() should return an accurate number
     * </ul>
     */
    public void testAddItems()
    {
        HListGroup lg = hlistgroup;
        Vector v = new Vector();

        HListElement[] e = null;

        // Add elements
        // Index should be ignored if no content
        try
        {
            lg.addItems(e = createContent(2), 5);
        }
        catch (IndexOutOfBoundsException ex)
        {
            fail("Index should be ignored if no content exists");
        }
        addToVector(e, v, 5);
        checkContent(lg, v);

        // To to add elements out of range
        try
        {
            lg.addItems(e = createContent(2), lg.getNumItems() + 1);
            fail("Expected an IndexOutOfBoundsException for index > n");
        }
        catch (IndexOutOfBoundsException expected)
        {
        }
        try
        {
            lg.addItems(e = createContent(2), -20);
            fail("Expected an IndexOutOfBoundsException for index > n");
        }
        catch (IndexOutOfBoundsException expected)
        {
        }

        // Add elements at the FRONT of the list
        // (Actually, not possible in 1.01b)
        lg.addItems(e = createContent(1), 0);
        addToVector(e, v, 0);
        checkContent(lg, v);

        // Add elements at the END of the list
        lg.addItems(e = createContent(1), HListGroup.ADD_INDEX_END);
        addToVector(e, v, -1);
        checkContent(lg, v);

        // Add elements in the middle of the list
        lg.addItems(e = createContent(2), 1);
        addToVector(e, v, 1);
        checkContent(lg, v);

        // Check for HItemEvent if current index gets changed
        lg.setCurrentItem(lg.getNumItems() - 1);
        resetListener();
        lg.addItems(e = createContent(2), 0);
        assertNotNull("A current item changed event should've been sent", currentEvent);
        assertEquals("An SET_CURRENT event should've been sent", HItemEvent.ITEM_SET_CURRENT, currentEvent.getID());
        assertSame("Unexpected list item specified in SET_CURRENT event", lg.getItem(lg.getNumItems() - 1),
                currentEvent.getItem());
    }

    /**
     * Tests getItem(i).
     * <ul>
     * <li>getItem(getIndex(item)) == item
     * <li>the correct item should be retreived
     * <li>if the index is negative throw an IllegalArgumentException
     * <li>if no such element exists, return null
     * </ul>
     */
    public void testGetItem()
    {
        HListGroup g = hlistgroup;
        HListElement[] content = setContent();

        // The correct item should be retrieved
        for (int i = content.length; i-- > 0;)
        {
            assertEquals("getItem(" + i + ") returned incorrect item", content[i], g.getItem(i));
            assertEquals("getItem(getIndex(item)) != item", content[i], g.getItem(g.getIndex(content[i])));
        }

        // i<0 -> IllegalArgumentException
        try
        {
            g.getItem(-1);
            fail("Index <0: expected IllegalArgumentException");
        }
        catch (IllegalArgumentException expected)
        {
        }

        // No such element, should return null
        assertNull("If the index is too large, null should be returned", g.getItem(g.getNumItems()));
        g.removeAllItems();
        assertNull("If the index is too large, null should be returned", g.getItem(0));
    }

    /**
     * Tests getIndex(HListElement).
     * <ul>
     * <li>getIndex(getItem(i)) == i
     * <li>the correct index should be retreived
     * <li>if not found, return ITEM_NOT_FOUND
     * </ul>
     */
    public void testGetIndex()
    {
        HListGroup g = hlistgroup;
        HListElement[] content = setContent();

        // The correct index should be retrieved
        for (int i = content.length; i-- > 0;)
        {
            assertEquals("getIndex(item) return incorrect index", i, g.getIndex(content[i]));
            assertEquals("getIndex(getItem(" + i + ")) != " + i, i, g.getIndex(g.getItem(i)));
        }

        assertEquals("ITEM_NOT_FOUND should be returned for unfound items", HListGroup.ITEM_NOT_FOUND,
                g.getIndex(new HListElement("x")));
    }

    /**
     * Test getNumItems().
     */
    public void testNumItems()
    {
        HListGroup g = hlistgroup;
        Vector v = new Vector();

        assertEquals("Unexpected item count", 0, g.getNumItems());

        addToVector(setContent(), v, -1);
        assertEquals("Unexpected item count", v.size(), g.getNumItems());

        g.removeItem(0);
        v.removeElementAt(0);
        assertEquals("Unexpected item count", v.size(), g.getNumItems());

        HListElement[] content = new HListElement[] { new HListElement("x"), new HListElement("y") };
        g.addItems(content, 1);
        addToVector(content, v, 1);
        assertEquals("Unexpected item count", v.size(), g.getNumItems());

        HListElement elem;
        g.addItem(elem = new HListElement("z"), 0);
        v.insertElementAt(elem, 0);
        assertEquals("Unexpected item count", v.size(), g.getNumItems());

        int n = g.getNumItems();
        g.addItem(elem = new HListElement("zz"), n - 1);
        v.insertElementAt(elem, n - 1);
        assertEquals("Unexpected item count", v.size(), g.getNumItems());

        g.removeAllItems();
        assertEquals("Unexpected item count", 0, g.getNumItems());
    }

    /**
     * Test removeItem(int index).
     * <ul>
     * <li>after removal, getIndex(item) should be null
     * <li>getNumItems() should return an accurate number
     * <li>the item should be removed from the selection, if it is selected
     * <li>if the removal change the current active index, an HItemEvent should
     * be sent
     * <li>If the removal changes the current selection(s)'s index, an
     * HItemEvent should be sent
     * <li>Should return the removed item else null
     * <li>NO EXCEPTION if index is invalid
     * <li>getNumItems() should return an accurate number
     * </ul>
     */
    public void testRemoveItem()
    {
        HListGroup lg = hlistgroup;
        Vector v = new Vector();
        HListElement[] elements = createContent(5);
        HListElement e = null;

        // No exceptions if given invalid index
        try
        {
            assertNull("Null should be returned", lg.removeItem(-1));
            assertNull("Null should be returned", lg.removeItem(0));
            assertNull("Null should be returned", lg.removeItem(1));
            lg.setListContent(elements);
            assertNull("Null should be returned", lg.removeItem(-1));
            assertNull("Null should be returned", lg.removeItem(elements.length));
            assertNull("Null should be returned", lg.removeItem(elements.length + 1));
        }
        catch (Exception ex)
        {
            fail("No exceptions should be thrown");
        }

        // Continually remove from front of list
        // Check removal of currently selected/current items
        lg.setListContent(elements);
        addToVector(elements, v, -1);
        for (int i = 0; i < elements.length; ++i)
        {
            lg.setCurrentItem(0);
            lg.setItemSelected(0, true);
            resetListener();
            e = lg.removeItem(0);
            v.removeElementAt(0);
            assertSame("The removed item should be returned", elements[i], e);
            assertEquals("After removal getIndex(item) should be NOT_FOUND", HListGroup.ITEM_NOT_FOUND, lg.getIndex(e));
            checkContent(lg, v);

            // Check for events
            assertNotNull("A selection changed event should've been sent", selectionEvent);
            assertEquals("An ITEM_CLEARED event should've been sent", HItemEvent.ITEM_CLEARED, selectionEvent.getID());
            assertSame("Unexpected list item specified in ITEM_CLEARED event", elements[i], selectionEvent.getItem());
            // The index doesn't change, so...
            if (v.size() != 0)
                assertNull("No current item change event is expected", currentEvent);
            else
            {
                assertNotNull("A current item changed event should've been sent", currentEvent);
                assertEquals("A SET_CURRENT event should've been sent", HItemEvent.ITEM_SET_CURRENT,
                        currentEvent.getID());
                assertSame("Unexpected list item specified in SET_CURRENT event", null, currentEvent.getItem());
            }
        }

        // Continually remove from end of list
        // Check removal of currently selected/current items
        lg.setListContent(elements);
        addToVector(elements, v, -1);
        for (int i = elements.length; --i >= 0;)
        {
            lg.setCurrentItem(i);
            lg.setItemSelected(i, true);
            resetListener();
            e = lg.removeItem(i);
            v.removeElementAt(i);
            assertSame("The removed item should be returned", elements[i], e);
            assertEquals("After removal getIndex(item) should be NOT_FOUND", HListGroup.ITEM_NOT_FOUND, lg.getIndex(e));
            checkContent(lg, v);

            // Check for events
            assertNotNull("A selection changed event should've been sent", selectionEvent);
            assertEquals("An ITEM_CLEARED event should've been sent", HItemEvent.ITEM_CLEARED, selectionEvent.getID());
            assertNotNull("A current item changed event should've been sent", currentEvent);
            assertEquals("An SET_CURRENT event should've been sent", HItemEvent.ITEM_SET_CURRENT, currentEvent.getID());
            assertSame("Unexpected list item specified in SET_CURRENT event", (v.size() > 0) ? lg.getItem(0) : null,
                    currentEvent.getItem());
        }

        // Check for events (removal alters current indices)
        lg.setListContent(elements);
        lg.setCurrentItem(lg.getNumItems() - 1);
        lg.setItemSelected(lg.getNumItems() - 1, true);
        resetListener();
        lg.removeItem(0);
        // The selection does not change (just the indices)
        assertNull("A selection changed event was unexpected", selectionEvent);
        assertNotNull("A current item changed event should've been sent", currentEvent);
        assertEquals("An SET_CURRENT event should've been sent", HItemEvent.ITEM_SET_CURRENT, currentEvent.getID());
        assertSame("Unexpected list item specified in SET_CURRENT event", lg.getItem(lg.getNumItems() - 1),
                currentEvent.getItem());
    }

    /**
     * Test removeAllItems().
     * <ul>
     * <li>Should remove all content, and empty the selection
     * <li>getNumItems() should return an accurate number
     * <li>if selection is changed, an event should be sent
     * </ul>
     */
    public void testRemoveAllItems()
    {
        HListGroup lg = hlistgroup;

        setContent();
        lg.removeAllItems();
        assertEquals("HListGroup should be empty", 0, lg.getNumItems());
        assertNull("HListGroup should be empty", lg.getListContent());
        assertNull("HListGroup selection should be empty", lg.getSelection());
        assertEquals("HListGroup current item should not be set", HListGroup.ITEM_NOT_FOUND, lg.getCurrentIndex());
        assertNull("HListGroup current item should not be set", lg.getCurrentItem());

        // Check for event
        setContent();
        lg.setItemSelected(0, true);
        resetListener();
        lg.removeAllItems();
        assertNotNull("A selection changed event should've been sent", selectionEvent);
        assertEquals("A SELECTION_CLEARED event should've been sent", HItemEvent.ITEM_SELECTION_CLEARED,
                selectionEvent.getID());
    }

    /**
     * Tests getCurrentIndex()/getCurrentItem()/setCurrentItem(int).
     * <ul>
     * <li>getIndex(getCurrentItem()) == getCurrentIndex()
     * <li>getItem(getCurrentIndex()) == getCurrentItem()
     * <li>the set item, should be the retrieved current index
     * <li>the set item (setCurrentItem(getIndex(item))) should be the retrieved
     * current item
     * <li>if no item is current then ITEM_NOT_FOUND or null is returned (as
     * appropriate)
     * <li>setCurrentItem() should return false for an invalid item (i.e., not
     * in the list)
     * <li>setCurrentItem() should return false if an item is already selected
     * <li>setCurrentItem() should return true on success
     * <li>events should be sent as appropriate
     * </ul>
     */
    public void testCurrent() throws Exception
    {
        HListGroup lg = hlistgroup;
        final String hintName = "UNKNOWN_CHANGE";
        final HChangeData[] hcd = new HChangeData[1];
        createWidgetChangeLook(lg, HVisible.UNKNOWN_CHANGE, hintName, hcd);

        // The initial current item is ITEM_NOT_FOUND
        resetListener();
        assertEquals("No current item should exist", HListGroup.ITEM_NOT_FOUND, lg.getCurrentIndex());
        assertNull("No current item should exist", lg.getCurrentItem());
        assertTrue("setCurrent should return null on failure", !lg.setCurrentItem(0));
        assertNull("No current item should exist", lg.getCurrentItem());
        // No events should be sent if no current item was set
        assertNull("No events should be generated", currentEvent);
        assertNull("No events should be generated", selectionEvent);
        assertNull("No widgetChange calls should've been made", hcd[0]);

        // Add content
        HListElement[] content = setContent();

        // The initial current item is... 0
        assertEquals("No current item should exist", 0, lg.getCurrentIndex());
        assertSame("The first item should be the default current", content[0], lg.getCurrentItem());
        resetListener();
        hcd[0] = null;
        assertTrue("setCurrent should return null on failure", !lg.setCurrentItem(lg.getNumItems()));
        assertSame("The first item should be the default current", content[0], lg.getCurrentItem());
        assertTrue("setCurrent(-1) should fail", !lg.setCurrentItem(-1));
        // No events should be sent if no current item was set
        assertNull("No events should be generated", currentEvent);
        assertNull("No events should be generated", selectionEvent);
        assertNull("No widgetChange calls should've been made", hcd[0]);

        // Set the current item, same should be retrieved
        for (int i = content.length; --i >= 0;)
        {
            resetListener();
            hcd[0] = null;
            assertTrue("setCurrent should succeed", lg.setCurrentItem(i));
            assertEquals("The set current should be the retrieved current", i, lg.getCurrentIndex());
            assertSame("The set current should be the retrieved current", content[i], lg.getCurrentItem());
            assertEquals("getIndex(getCurrentItem()) == getCurrentIndex()", lg.getIndex(lg.getCurrentItem()),
                    lg.getCurrentIndex());
            assertSame("getItem(getCurrentIndex()) == getCurrentItem()", lg.getItem(lg.getCurrentIndex()),
                    lg.getCurrentItem());
            // Check events
            assertNull("No events should be generated", selectionEvent);
            assertNotNull("A current item changed event is expected", currentEvent);
            assertEquals("An SET_CURRENT event should've been sent", HItemEvent.ITEM_SET_CURRENT, currentEvent.getID());
            assertSame("Unexpected list item specified in SET_CURRENT event", content[i], currentEvent.getItem());
            assertTrue("setCurrent should fail if already current", !lg.setCurrentItem(i));
            assertNotNull("A widgetChanged call was expected", hcd[0]);
        }
    }

    /**
     * Tests getSelectionIndices(), getSelection(), clearSelection(),
     * getNumSelected(), setItemSelected(), isItemSelected().
     * <ul>
     * <li>Honor multi-selection/no multi-selection
     * <li>The set selection should be the retrieved selection (with respect to
     * multi-selection)
     * <li>events should be sent as appropriate
     * </ul>
     */
    public void testSelection()
    {
        HListGroup lg = hlistgroup;

        // No selection initially
        assertNull("SelectionIndices should be null when there is none", lg.getSelectionIndices());
        assertNull("Selection should be null when there is none", lg.getSelection());
        assertEquals("numSelected should be 0 when there is none", 0, lg.getNumSelected());
        try
        {
            lg.setItemSelected(0, true);
            fail("Expected IllegalArgumentException for non-existing index");
        }
        catch (IllegalArgumentException expected)
        {
        }
        try
        {
            lg.isItemSelected(0);
            fail("Expected IllegalArgumentException for non-existing index");
        }
        catch (IllegalArgumentException expected)
        {
        }

        // Add items
        HListElement[] content = setContent();

        // Test single-selection
        lg.setMultiSelection(false);
        lg.clearSelection();
        for (int i = 0; i < content.length; ++i)
        {
            resetListener();
            lg.setItemSelected(i, true);
            assertTrue("set selection should be retrieved", lg.isItemSelected(i));
            if (i > 0) assertTrue("other selections should be cleared", !lg.isItemSelected(i - 1));
            assertEquals("numSelected should be <=1 when in single-selection", 1, lg.getNumSelected());
            // Check events
            assertNotNull("A selection event should've been generated", selectionEvent);
            assertEquals("A SELECTED event should've been generated", HItemEvent.ITEM_SELECTED, selectionEvent.getID());
            assertSame("The selected item should be specified in the event", lg.getItem(i), selectionEvent.getItem());
            // Should also check for ITEM_CLEARED if one WAS set...

            HListElement[] selection = lg.getSelection();
            assertNotNull("selection should be non-null", selection);
            assertEquals("selection size should be 1", 1, selection.length);
            assertSame("set selection should be retrieved", content[i], selection[0]);

            int[] indices = lg.getSelectionIndices();
            assertNotNull("selection should be non-null", indices);
            assertEquals("selection size should be 1", 1, indices.length);
            assertEquals("set selection should be retrieved", i, indices[0]);
        }

        // Test multi-selection
        lg.setMultiSelection(true);
        lg.clearSelection();
        for (int i = 0; i < content.length; ++i)
        {
            resetListener();
            lg.setItemSelected(i, true);
            assertTrue("set selection should be retrieved", lg.isItemSelected(i));
            if (i > 0) assertTrue("other selections should still be set", lg.isItemSelected(i - 1));
            assertEquals("numSelected not as expected", i + 1, lg.getNumSelected());
            // Check events
            assertNotNull("A selection event should've been generated", selectionEvent);
            assertEquals("A SELECTED event should've been generated", HItemEvent.ITEM_SELECTED, selectionEvent.getID());
            assertSame("The selected item should be specified in the event", lg.getItem(i), selectionEvent.getItem());

            HListElement[] selection = lg.getSelection();
            assertNotNull("selection should be non-null", selection);
            assertEquals("selection size not as expected", i + 1, selection.length);
            for (int j = 0; j <= i; ++j)
            {
                int idx = lg.getIndex(selection[j]);
                assertTrue("Unexpected selection index " + idx, idx <= i);
            }

            int[] indices = lg.getSelectionIndices();
            assertNotNull("selection should be non-null", indices);
            assertEquals("selection size not as expected", i + 1, indices.length);
            for (int j = 0; j < indices.length; ++j)
                assertTrue("Unexpected selection index " + indices[j], indices[j] <= i);

            // Test that we can unset and reset
            lg.setItemSelected(i, false);
            assertTrue("unset selection should be retrieved", !lg.isItemSelected(i));
            lg.setItemSelected(i, true);
            assertTrue("set selection should be retrieved", lg.isItemSelected(i));
        }
    }

    /**
     * Tests (set|get)MultiSelection().
     * <ul>
     * <li>the set value is the retrieved value
     * <li>test that multi-selection is enforced (on events)!
     * <li>events should be sent as appropriate (when selection OR mode changes)
     * <li>setting multiselection to false should clear the current selection of
     * all but one (first) element
     * <li>HChangeData(LIST_MULTISELECTION_CHANGE, oldFlag);
     * </ul>
     */
    public void testMultiSelection() throws Exception
    {
        HListGroup lg = hlistgroup;
        final String hintName = "LIST_MULTISELECTION_CHANGE";
        final HChangeData[] hcd = new HChangeData[1];
        createWidgetChangeLook(lg, HVisible.LIST_MULTISELECTION_CHANGE, hintName, hcd);

        lg.setMultiSelection(false);
        assertTrue("Set multiSelection mode should be retrieved mode", !lg.getMultiSelection());

        boolean old = false;

        hcd[0] = null;
        lg.setMultiSelection(true);
        assertTrue("Set multiSelection mode should be retrieved mode", lg.getMultiSelection());
        assertNotNull(hintName + " change data expected", hcd[0]);
        assertTrue("Old multiSelection expected to be an Integer", hcd[0].data instanceof Integer);
        assertEquals("Old multiSelection expected in change data", old ? 1 : 0, ((Integer) hcd[0].data).intValue());
        old = true;

        hcd[0] = null;
        lg.setMultiSelection(true);
        assertTrue("Set multiSelection mode should be retrieved mode", lg.getMultiSelection());
        assertNull("Unexpected HChangeData", hcd[0]);

        hcd[0] = null;
        lg.setMultiSelection(false);
        assertTrue("Set multiSelection mode should be retrieved mode", !lg.getMultiSelection());
        assertNotNull(hintName + " change data expected", hcd[0]);
        assertTrue("Old multiSelection expected to be an Integer", hcd[0].data instanceof Integer);
        assertEquals("Old multiSelection expected in change data", old ? 1 : 0, ((Integer) hcd[0].data).intValue());
        old = false;

        // With multiselection==true, add a bunch of elements and select them
        HListElement[] content = setContent();
        lg.setMultiSelection(true);
        for (int i = 1; i < content.length; ++i)
            lg.setItemSelected(i, true);
        for (int i = 1; i < content.length; ++i)
            assertTrue("Elements should be selected", lg.isItemSelected(i));
        // Reset multiselection, ensure that ONLY one is selected
        lg.setMultiSelection(false);
        assertTrue("The first selected element should remain selected", lg.isItemSelected(1));
        for (int i = 0; i < content.length; ++i)
            if (i != 1) assertTrue("All other items should be unselected", !lg.isItemSelected(i));
    }

    /**
     * Tests (get|set)ScrollPosition().
     * <ul>
     * <li>the position is 0 if there is not content
     * <li>The set position should be the retreived position
     * <li>An IllegalArgumentException should be thrown if it is an invalid
     * scroll position
     * <li>HChangeData(LIST_SCROLLPOSITION_CHANGE, oldFlag);
     * </ul>
     */
    public void testScrollPosition() throws Exception
    {
        HListGroup lg = hlistgroup;

        assertEquals("ScrollPosition given no content should be ITEM_NOT_FOUND", HListGroup.ITEM_NOT_FOUND,
                lg.getScrollPosition());

        setContent();
        final String hintName = "LIST_SCROLLPOSITION_CHANGE";
        final HChangeData[] hcd = new HChangeData[1];
        createWidgetChangeLook(lg, HVisible.LIST_SCROLLPOSITION_CHANGE, hintName, hcd);

        lg.setScrollPosition(0);
        assertEquals("Set scrollPosition should be retrieved position", 0, lg.getScrollPosition());

        int old = 0;
        hcd[0] = null;
        lg.setScrollPosition(1);
        assertEquals("Set scrollPosition should be retrieved position", 1, lg.getScrollPosition());
        assertNotNull(hintName + " change data expected", hcd[0]);
        assertEquals("Old scrollPosition expected in change data", old, ((Integer) hcd[0].data).intValue());
        lg.setLook(null); // don't track anymore...

        try
        {
            lg.setScrollPosition(-1);
            fail("Invalid (<0) scrollPosition should cause an exception");
        }
        catch (IllegalArgumentException expected)
        {
        }

        try
        {
            lg.setScrollPosition(lg.getNumItems());
            fail("Invalid (>n-1) scrollPosition should cause an exception");
        }
        catch (IllegalArgumentException expected)
        {
        }

        try
        {
            lg.removeAllItems();
            lg.setScrollPosition(1);
            fail("Invalid (no items) scrollPosition should cause an exception");
        }
        catch (IllegalArgumentException expected)
        {
        }
    }

    /**
     * Tests (get|set)IconSize().
     * <ul>
     * <li>The set size should be the retreived size
     * <li>if unset, should be Dimension(DEFAULT_ICON_WIDTH,
     * DEFAULT_ICON_HEIGHT)
     * <li>When setting to null, should revert to default of
     * Dimension(DEFAULT_ICON_WIDTH, DEFAULT_ICON_HEIGHT)
     * <li>HChangeData(LIST_ICONSIZE_CHANGE, oldDimension);
     * </ul>
     */
    public void testIconSize() throws Exception
    {
        HListGroup lg = hlistgroup;
        Dimension d;
        Dimension defaultSize = new Dimension(HListGroup.DEFAULT_ICON_WIDTH, HListGroup.DEFAULT_ICON_HEIGHT);
        Dimension old = defaultSize;

        final String hintName = "LIST_ICONSIZE_CHANGE";
        final HChangeData[] hcd = new HChangeData[1];
        createWidgetChangeLook(lg, HVisible.LIST_ICONSIZE_CHANGE, hintName, hcd);

        // Set to 20x30
        hcd[0] = null;
        lg.setIconSize(d = new Dimension(20, 30));
        assertEquals("Set icon size should be retrieved size", d, lg.getIconSize());
        assertNotNull(hintName + " change data expected", hcd[0]);
        assertEquals("Old icon size expected in change data", old, hcd[0].data);
        old = d;

        // Set to default
        hcd[0] = null;
        lg.setIconSize(d = defaultSize);
        assertEquals("Set icon size (default) should be retrieved size", d, lg.getIconSize());
        assertNotNull(hintName + " change data expected", hcd[0]);
        assertEquals("Old icon size expected in change data", old, hcd[0].data);
        old = d;

        // Set to 20xdefaultH
        hcd[0] = null;
        lg.setIconSize(d = new Dimension(20, HListGroup.DEFAULT_ICON_HEIGHT));
        assertEquals("Set icon size (default height) should be retrieved size", d, lg.getIconSize());
        assertNotNull(hintName + " change data expected", hcd[0]);
        assertEquals("Old icon size expected in change data", old, hcd[0].data);
        old = d;

        // Set to defaultWx30
        hcd[0] = null;
        lg.setIconSize(d = new Dimension(HListGroup.DEFAULT_ICON_WIDTH, 30));
        assertEquals("Set icon size (default width) should be retrieved size", d, lg.getIconSize());
        assertNotNull(hintName + " change data expected", hcd[0]);
        assertEquals("Old icon size expected in change data", old, hcd[0].data);
        old = d;

        // Reset to default
        hcd[0] = null;
        lg.setIconSize(null);
        assertEquals("Setting icon size to null should revert to default", defaultSize, lg.getIconSize());
        assertNotNull(hintName + " change data expected", hcd[0]);
        assertEquals("Old icon size expected in change data", old, hcd[0].data);
    }

    /**
     * Tests (get|set)LabelSize().
     * <ul>
     * <li>The set size should be the retreived size
     * <li>if unset, should be Dimension(DEFAULT_LABEL_WIDTH,
     * DEFAULT_LABEL_HEIGHT)
     * <li>When setting to null, should revert to default of
     * Dimension(DEFAULT_LABEL_WIDTH, DEFAULT_LABEL_HEIGHT)
     * <li>HChangeData(LIST_LABELSIZE_CHANGE, oldDimension);
     * </ul>
     */
    public void testLabelSize() throws Exception
    {
        HListGroup lg = hlistgroup;
        Dimension d;
        Dimension defaultSize = new Dimension(HListGroup.DEFAULT_LABEL_WIDTH, HListGroup.DEFAULT_LABEL_HEIGHT);
        Dimension old = defaultSize;

        final String hintName = "LIST_LABELSIZE_CHANGE";
        final HChangeData[] hcd = new HChangeData[1];
        createWidgetChangeLook(lg, HVisible.LIST_LABELSIZE_CHANGE, hintName, hcd);

        // Set to 20x30
        hcd[0] = null;
        lg.setLabelSize(d = new Dimension(20, 30));
        assertEquals("Set label size should be retrieved size", d, lg.getLabelSize());
        assertNotNull(hintName + " change data expected", hcd[0]);
        assertEquals("Old label size expected in change data", old, hcd[0].data);
        old = d;

        // Set to default
        hcd[0] = null;
        lg.setLabelSize(d = defaultSize);
        assertEquals("Set label size (default) should be retrieved size", d, lg.getLabelSize());
        assertNotNull(hintName + " change data expected", hcd[0]);
        assertEquals("Old label size expected in change data", old, hcd[0].data);
        old = d;

        // Set to 20xdefaultH
        hcd[0] = null;
        lg.setLabelSize(d = new Dimension(20, HListGroup.DEFAULT_LABEL_HEIGHT));
        assertEquals("Set label size (default height) should be retrieved size", d, lg.getLabelSize());
        assertNotNull(hintName + " change data expected", hcd[0]);
        assertEquals("Old label size expected in change data", old, hcd[0].data);
        old = d;

        // Set to defaultWx30
        hcd[0] = null;
        lg.setLabelSize(d = new Dimension(HListGroup.DEFAULT_LABEL_WIDTH, 30));
        assertEquals("Set label size (default width) should be retrieved size", d, lg.getLabelSize());
        assertNotNull(hintName + " change data expected", hcd[0]);
        assertEquals("Old label size expected in change data", old, hcd[0].data);
        old = d;

        // Reset to default
        hcd[0] = null;
        lg.setLabelSize(null);
        assertEquals("Setting label size to null should revert to default", defaultSize, lg.getLabelSize());
        assertNotNull(hintName + " change data expected", hcd[0]);
        assertEquals("Old label size expected in change data", old, hcd[0].data);
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
        checkClass(HListGroupTest.class);

        return new HListGroup()
        {
            public void processHFocusEvent(org.havi.ui.event.HFocusEvent e)
            {
                ev.validate(e);
            }

            public void processHItemEvent(org.havi.ui.event.HItemEvent e)
            {
                ev.validate(e);
            }
        };
    }

    /**
     * Create an array of elements.
     */
    private HListElement[] createContent(int n)
    {
        HListElement[] array = new HListElement[n];
        for (int i = 0; i < n; ++i)
            array[i] = new HListElement("e" + i);
        return array;
    }

    private HListElement[] content = null;

    /**
     * Sets the list content of hlistgroup.
     */
    private HListElement[] setContent()
    {
        if (content == null) content = createContent(5);
        hlistgroup.setListContent(content);
        return content;
    }

    private int currentCount, selectionCount;

    private HItemEvent currentEvent, selectionEvent;

    HItemListener listener;

    /**
     * Adds an item listener which can be used to track events.
     */
    private void addListener()
    {
        hlistgroup.addItemListener(listener = new HItemListener()
        {
            public void selectionChanged(HItemEvent e)
            {
                ++selectionCount;
                selectionEvent = e;
            }

            public void currentItemChanged(HItemEvent e)
            {
                ++currentCount;
                currentEvent = e;
            }
        });
    }

    /**
     *
     */
    private void resetListener()
    {
        if (listener == null) addListener();
        selectionCount = 0;
        currentCount = 0;
        selectionEvent = null;
        currentEvent = null;
    }

    /**
     * Check the listgroup against the expected vector.
     */
    private void checkContent(HListGroup lg, Vector v)
    {
        assertEquals("Unexpected size of list", lg.getNumItems(), v.size());
        checkContent(lg.getListContent(), v);
    }

    /**
     * Check the array of listelements against the expected vector.
     */
    private void checkContent(HListElement[] content, Vector v)
    {
        if (v.size() == 0)
        {
            assertNull("Unexpected content array", content);
            return;
        }

        assertEquals("Unexpected size of content", v.size(), content.length);
        for (int i = 0; i < content.length; ++i)
        {
            HListElement e = (HListElement) v.elementAt(i);
            if (content[i] != e)
            {
                if (e == null)
                    fail("Expected a null element at index " + i);
                else if (content[i] == null) fail("Did not expecte a null element at index " + i);

                int index = v.indexOf(content[i]);
                if (index == -1)
                    fail("Unexpected element at index " + i);
                else
                    fail("Element at index " + i + " expected at " + index);
            }
        }
    }

    /**
     * Adds the given array to the given vector after the given idx.
     */
    private void addToVector(HListElement[] array, Vector v, int idx)
    {
        boolean ignoreIdx = v.size() == 0;
        for (int i = 0; i < array.length; ++i)
        {
            if (idx == -1 || ignoreIdx)
                v.addElement(array[i]);
            else if (ADDAFTER)
                v.insertElementAt(array[i], ++idx);
            else
                v.insertElementAt(array[i], idx++);
        }
    }

    /**
     * Adds the given element to the given vector after the given idx.
     */
    private void addToVector(HListElement e, Vector v, int idx)
    {
        if (idx == -1 || (idx > v.size() - 1))
            v.addElement(e);
        else if (ADDAFTER)
            v.insertElementAt(e, ++idx);
        else
            v.insertElementAt(e, idx);
    }

    /**
     * Whether to add AFTER the given index (as specified by 1.01b) or add AT
     * the given index (as specified by 1.1).
     */
    private static final boolean ADDAFTER = false;
}
