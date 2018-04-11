/*
 * @(#)CxFactory.java
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
 * Helper abstract class that aids in the setting of a unique and
 * distinguishable name for a test case's factory.
 * <P>
 * As of 08-Dec-2002, the original constructor will NOT add the owning
 * class's name to the factory <tt>toString()</tt> output.  Since the
 * Ant JUnit report is setup such that the concrete class's tests are
 * organized under it, this is redundant information, and clutters the
 * report.
 *
 * @author    Matt Albrecht <a href="mailto:groboclown@users.sourceforge.net">groboclown@users.sourceforge.net</a>
 * @version   $Date: 2002/12/09 04:43:23 $
 * @since     October 30, 2002
 */
public abstract class CxFactory implements ICxFactory
{
    private String id;
    
    /**
     * Specify a unique identifier for this specific factory within the
     * context of the concrete test that is providing the factory.  The
     * <tt>CxFactory</tt> constructor will not add the test's class name to
     * this string to create a distinguishable string to help debug the
     * source of any errors caused by a factory's particular setup.  If you
     * want the owning class's name to appear in the id, use the alternate
     * constructor.
     *
     * @param name the unique identifier within the context of the
     *      factory's owning test class.  This cannot be <tt>null</tt>.
     * @exception IllegalArgumentException thrown if <tt>name</tt>
     *      is <tt>null</tt>.
     */
    public CxFactory( String name )
    {
        this( name, false );
    }
    
    
    /**
     * Specify a unique identifier for this specific factory within the
     * context of the concrete test that is providing the factory.  The
     * <tt>CxFactory</tt> constructor may add the test's class name to
     * this string to create a distinguishable string to help debug the
     * source of any errors caused by a factory's particular setup,
     * depending on the value of <tt>addClassName</tt>.
     *
     * @param name the unique identifier within the context of the
     *      factory's owning test class.  This cannot be <tt>null</tt>.
     * @param addClassName <tt>true</tt> if the owning class's name should
     *      be added to the id, and <tt>false</tt> if the name should be
     *      the ID itself.
     * @exception IllegalArgumentException thrown if <tt>name</tt>
     *      is <tt>null</tt>.
     * @since 07-Dec-2002
     */
    public CxFactory( String name, boolean addClassName )
    {
        if (name == null)
        {
            throw new IllegalArgumentException(
                "factory name cannot be null." );
        }
        
        // If this factory is within another class (i.e. an inner class
        // or an anonymous class), then the owning class's name will be
        // used instead, which is normally the test class's name.
        if (addClassName)
        {
            Class c = this.getClass();
            String className = getOwningClassName( c );
            this.id = className + "-" + name;
        }
        else
        {
            this.id = name;
        }
        
        // post condition
        if (this.id == null)
        {
            throw new IllegalStateException(
                "internal coding error: generated ID was null." );
        }
    }
    
    
    /**
     * Override the Java-default toString, and provide our distinguishable
     * name.
     *
     * @return the generated distinguishable name.
     */
    public String toString()
    {
        // Pre-condition
        if (this.id == null)
        {
            throw new IllegalStateException(
                "The internal id is null.  "+
                "An internal API coding mistake was made." );
        }
        
        return this.id;
    }
    
    
    /**
     * Most factories have no need for a tearDown method, so a default
     * (do-nothing) implementation has been provided here.
     */
    public void tearDown( Object implObject ) throws Exception
    {
        // do nothing
    }
    
    
    // inherited from ImplFactory
    public abstract Object createImplObject() throws Exception;
    
    
    /**
     * Find the owning class name for the given class.  For inner classes,
     * this will return the parent class.
     *
     * @param c the class to find the owner
     * @return the owning class' name.
     */
    private String getOwningClassName( Class c )
    {
        if (c == null)
        {
            throw new IllegalArgumentException( "No null args." );
        }
        
        Class lastOwner = c;
        /*
        This part does not work as expected: see the corresponding enemy
        unit test for reasons.
        Class owner = c.getDeclaringClass();
        while (owner != null)
        {
            lastOwner = owner;
            owner = owner.getDeclaringClass();
        }
        */
        String className = lastOwner.getName();
        
        int pos = className.lastIndexOf( '$' );
        if (pos > 0)
        {
            className = className.substring( 0, pos );
        }
        pos = className.lastIndexOf( '.' );
        if (pos > 0)
        {
            className = className.substring( pos+1 );
        }
        return className;
    }
}

