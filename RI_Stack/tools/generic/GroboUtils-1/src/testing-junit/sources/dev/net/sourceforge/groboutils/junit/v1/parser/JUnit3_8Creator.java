/*
 * @(#)JUnit3_8Creator.java
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

import junit.framework.Test;
import junit.framework.TestCase;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Constructor;


/**
 * Emulates the JUnit 3.8+ construction mechanism.
 *
 * @author     Matt Albrecht <a href="mailto:groboclown@users.sourceforge.net">groboclown@users.sourceforge.net</a>
 * @version    $Date: 2002/11/05 00:49:31 $
 * @since      November 3, 2002
 */
public class JUnit3_8Creator implements ITestCreator
{
    /**
     * Checks if the creator can be used on the given class.
     *
     * @param theClass the class to check if parsing is acceptable.
     */
    public boolean canCreate( Class theClass )
    {
        if (!TestCase.class.isAssignableFrom( theClass ))
        {
            return false;
        }
        
        try
        {
            Constructor c = theClass.getConstructor( new Class[0] );
            return (c != null);
        }
        catch (Exception ex)
        {
            return false;
        }
    }
    
    
    /**
     * Creates a new test, based on the given class and method of the
     * class.
     *
     * @param theClass the class to parse for testing.
     * @param m the method that will be tested with the new class instance.
     * @exception InstantiationException if there was a problem creating
     *      the class.
     * @exception NoSuchMethodException if the method does not exist in the
     *      class.
     */
    public Test createTest( Class theClass, Method method )
            throws InstantiationException, NoSuchMethodException,
            InvocationTargetException, IllegalAccessException,
            ClassCastException
    {
        TestCase tc;
        try
        {
            tc = (TestCase)theClass.newInstance();
        }
        catch (IllegalArgumentException iae)
        {
            StringBuffer args = new StringBuffer(
                "Arguments didn't match for default constructor in class "
                );
            args.append( theClass.getName() ).append( "." );
            throw new InstantiationException( args.toString() );
        }
        tc.setName( method.getName() );
        return tc;
    }
}

