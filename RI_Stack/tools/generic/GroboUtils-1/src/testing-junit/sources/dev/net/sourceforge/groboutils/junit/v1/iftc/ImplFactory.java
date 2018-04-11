/*
 * @(#)ImplFactory.java
 *
 * Copyright (C) 2002 Matt Albrecht
 * groboclown@users.sourceforge.net
 * http://groboutils.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package net.sourceforge.groboutils.junit.v1.iftc;



/**
 * Allows for tests to be written on interfaces or abstract classes, by creating
 * a specific instance of the interface or abstract class.  Test classes will
 * invoke this method to retrieve the specific instance for their tests.
 * <P>
 * Since October 21, 2002, the <tt>createImplObject()</tt> method can now
 * throw any exception.  Some construction implementations throw all kinds
 * of errors, such as <tt>IOException</tt> or <tt>SQLException</tt>.  This
 * makes the task of creating factories a bit easier, since we no longer
 * need to worry about proper try/catch blocks.
 *
 * @author    Matt Albrecht <a href="mailto:groboclown@users.sourceforge.net">groboclown@users.sourceforge.net</a>
 * @version   $Date: 2002/12/10 02:53:18 $
 * @since     March 2, 2002
 */
public interface ImplFactory
{
    /**
     * Create a new instance of the interface type for testing through
     * an InterfaceTest.
     * <P>
     * As of 21-Oct-2002, this method can raise any exception, and it will be
     * correctly caught and reported as a failure by the
     * <tt>InterfaceTestCase.createImplObject()</tt> method, so that the
     * creation method can simplify its logic, and add any kind of
     * initialization without having to worry about the correct way to
     * handle exceptions.
     *
     * @return a new instance of the expected type that the corresponding
     *      <tt>InterfaceTestCase</tt>(s) cover.
     * @exception Exception thrown under any unexpected condition that
     *      results in the failure to properly create the instance.
     * @since October 21, 2002: Since this date, this method can now throw
     *      exceptions to make creation a bit easier on us.
     */
    public Object createImplObject() throws Exception;
    
    
    /**
     * All ImplFactory instances should specify a distinguishable name
     * to help in debugging failed tests due to a particular factory's
     * instance setup.
     *
     * @return a distinguishable name for the factory.
     * @see CxFactory CxFactory: a helper that simplifies this task for us.
     */
    public String toString();
}

