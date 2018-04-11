/*
 * @(#)JUnitOrigCreator.java
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

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Constructor;


/**
 * Emulates the construction mechanism for all JUnit versions.
 *
 * @author     Matt Albrecht <a href="mailto:groboclown@users.sourceforge.net">groboclown@users.sourceforge.net</a>
 * @version    $Date: 2002/11/05 00:49:31 $
 * @since      November 3, 2002
 */
public class JUnitOrigCreator implements ITestCreator
{
    /**
     * Creates a new test, based on the given class and method of the class.
     * This calls <tt>createTest( Class, Object[] )</tt> to create the new
     * class, which itself calls <tt>getConstructorArgTypes( Class )</tt> to
     * determine which constructor to get.  Also,
     * <tt>createTestArguments( Class, Method )</tt> is called to generate the
     * constructor's arguments.
     *
     * @param theClass the class to parse for testing.
     * @param m the method that will be tested with the new class instance.
     * @exception InstantiationException if there was a problem creating the
     *      class.
     * @exception NoSuchMethodException if the method does not exist in the
     *      class.
     * @see #createTest( Class, Object[] )
     */
    public Test createTest( Class theClass, Method method )
            throws InstantiationException, NoSuchMethodException,
            InvocationTargetException, IllegalAccessException,
            ClassCastException
    {
        return createTest( theClass,
            createTestArguments( theClass, method ) );
    }
    
    
    /**
     * Checks if the creator can be used on the given class.
     *
     * @param theClass the class to check if parsing is acceptable.
     */
    public boolean canCreate( Class theClass )
    {
        try
        {
            Constructor c = getConstructor( theClass );
            return (c != null);
        }
        catch (Exception ex)
        {
            return false;
        }
    }
    
    
    /**
     * Discovers the constructor for the test class which will be used in
     * the instantiation of a new instance of the class.  This constructor
     * will be discovered through a call to
     * <tt>getConstructorArgTypes</tt>.  The returned constructor must be
     * callable through <tt>createTestArguments</tt>.
     *
     * @param theClass the class to parse for testing.
     * @return the constructor to create a new test instance with.
     * @exception NoSuchMethodException if the class does not have a
     *      constructor with the arguments returned by
     *      <tt>getConstructorArgTypes</tt>.
     * @see #getConstructorArgTypes( Class )
     * @see #createTest( Class, Method )
     * @see #createTestArguments( Class, Method )
     */
    protected Constructor getConstructor( final Class theClass )
            throws NoSuchMethodException
    {
        return theClass.getConstructor(
            getConstructorArgTypes( theClass ) );
    }
    
    
    /**
     * Allows for pluggable constructor types.  The default action is to
     * return <tt>java.lang.String</tt>.
     *
     * @param theClass the class to parse for testing.
     * @return the set of classes which define the constructor to extract.
     */
    protected Class[] getConstructorArgTypes( Class theClass )
    {
        return new Class[] { String.class };
    }
    
    
    /**
     * 
     *
     * @param theClass the class to parse for testing.
     * @param m the method that will be tested with the new class instance.
     */
    protected Object[] createTestArguments( Class theClass, Method method )
    {
        return new Object[] { method.getName() };
    }
    
    
    /**
     * Creates a new test class instance.
     *
     * @param theClass the class to parse for testing.
     * @param constructorArgs arguments for the constructor retrieved through
     *      <tt>getConstructor()</tt>.
     * @return the new Test.
     * @exception InstantiationException if a new instance could not be made
     *      of the test class.
     * @exception NoSuchMethodException if the constructor could not be found.
     * @see #getConstructor( Class )
     */
    protected Test createTest( Class theClass, Object[] constructorArgs )
            throws InstantiationException, NoSuchMethodException,
            InvocationTargetException, IllegalAccessException,
            ClassCastException
    {
        Constructor c = getConstructor( theClass );
        Test t;
        try
        {
            t = (Test)c.newInstance( constructorArgs );
        }
        catch (IllegalArgumentException iae)
        {
            StringBuffer args = new StringBuffer(
                "Arguments didn't match for constructor " );
            args.append( c ).append( " in class " ).append(
                theClass.getName() ).append( ".  Arguments = [" );
            for (int i = 0; i < constructorArgs.length; ++i)
            {
                if (i > 0)
                {
                    args.append( ", " );
                }
                args.append( constructorArgs[i].getClass().getName() ).
                    append( " = '" ).
                    append( constructorArgs[i] ).
                    append( "'" );
            }
            args.append("]: ").append( iae );
            throw new InstantiationException( args.toString() );
        }
        return t;
    }
}

