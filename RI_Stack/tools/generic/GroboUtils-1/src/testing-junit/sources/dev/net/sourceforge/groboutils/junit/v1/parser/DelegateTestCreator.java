/*
 * @(#)DelegateTestCreator.java
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

import org.apache.log4j.Category;


/**
 * Allows for an ordered set of TestCreator instances to be queried for
 * generating instances.
 *
 * @author     Matt Albrecht <a href="mailto:groboclown@users.sourceforge.net">groboclown@users.sourceforge.net</a>
 * @version    $Date: 2002/11/05 00:49:31 $
 * @since      November 4, 2002
 */
public class DelegateTestCreator implements ITestCreator
{
    private static final Category LOG = Category.getInstance(
        DelegateTestCreator.class.getName() );
    
    private ITestCreator[] creators;
    
    
    /**
     * Create the delegation with an ordered array of creators.  The
     * creators are searched from index 0 to the last index for a valid
     * creator.
     */
    public DelegateTestCreator( ITestCreator[] tc )
    {
        if (tc == null || tc.length <= 0)
        {
            throw new IllegalArgumentException("no null args");
        }
        
        int len = tc.length;
        this.creators = new ITestCreator[ len ];
        for (int i = len; --i >= 0;)
        {
            if (tc[i] == null)
            {
                throw new IllegalArgumentException("no null args");
            }
            this.creators[i] = tc[i];
        }
    }
    
    
    /**
     * Checks if the creator can be used on the given class.
     *
     * @param theClass the class to check if parsing is acceptable.
     * @return whether the creator can generate a test based on
     *      <tt>theClass</tt>.
     */
    public boolean canCreate( Class theClass )
    {
        // order doesn't matter at this point
        for (int i = this.creators.length; --i >= 0;)
        {
            if (this.creators[i].canCreate( theClass ))
            {
                return true;
            }
        }
        return false;
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
        // order matters here.
        for (int i = 0; i < this.creators.length; ++i)
        {
            ITestCreator tc = this.creators[i];
            try
            {
                if (tc.canCreate( theClass ))
                {
                    Test t = tc.createTest( theClass, method );
                    if (t != null)
                    {
                        return t;
                    }
                }
            }
            catch (InstantiationException e)
            {
                LOG.info( "Failed to create test with creator "+tc+".", e );
            }
            catch (NoSuchMethodException e)
            {
                LOG.info( "Failed to create test with creator "+tc+".", e );
            }
            catch (InvocationTargetException e)
            {
                LOG.info( "Failed to create test with creator "+tc+".", e );
            }
            catch (IllegalAccessException e)
            {
                LOG.info( "Failed to create test with creator "+tc+".", e );
            }
            catch (ClassCastException e)
            {
                LOG.info( "Failed to create test with creator "+tc+".", e );
            }
        }
        
        // did not find a valid test creator.
        return null;
    }
}

