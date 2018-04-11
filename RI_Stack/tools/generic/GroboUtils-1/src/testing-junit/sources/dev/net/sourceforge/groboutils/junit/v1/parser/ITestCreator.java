/*
 * @(#)ITestCreator.java
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

package net.sourceforge.groboutils.junit.v1.parser;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import junit.framework.Test;


/**
 * Interface that can create test objects based on a class and a method from
 * within that class, using a specific method.  Also provides means to check
 * the class object to see if the implementation can instantiate the test
 * class.
 *
 * @author     Matt Albrecht <a href="mailto:groboclown@users.sourceforge.net">groboclown@users.sourceforge.net</a>
 * @version    $Date: 2002/11/05 00:52:07 $
 * @since      November 3, 2002
 */
public interface ITestCreator
{
    /**
     * Creates a new test, based on the given class and method of the
     * class.
     *
     * @param theClass the class to parse for testing.
     * @param m the method that will be tested with the new class instance.
     * @return the generated test, or <tt>null</tt> if the test could not
     *      be created.
     * @exception InstantiationException if there was a problem creating
     *      the class.
     * @exception NoSuchMethodException if the method does not exist in the
     *      class.
     */
    public Test createTest( Class theClass, Method method )
            throws InstantiationException, NoSuchMethodException,
            InvocationTargetException, IllegalAccessException,
            ClassCastException;
    
    /**
     * Checks if the creator can be used on the given class.
     *
     * @param theClass the class to check if parsing is acceptable.
     * @return whether the creator can generate a test based on
     *      <tt>theClass</tt>.
     */
    public boolean canCreate( Class theClass );
}

