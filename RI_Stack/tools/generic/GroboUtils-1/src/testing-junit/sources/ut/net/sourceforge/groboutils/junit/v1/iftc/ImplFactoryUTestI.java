/*
 * @(#)ImplFactoryUTestI.java
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

//import net.sourceforge.groboutils.testing.junitlog.v1.*;
import org.easymock.EasyMock;
import org.easymock.MockControl;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


/**
 * Tests the ImplFactory interface.
 *
 * @author    Matt Albrecht <a href="mailto:groboclown@users.sourceforge.net">groboclown@users.sourceforge.net</a>
 * @since     March 1, 2002
 * @version   $Date: 2002/10/31 05:54:23 $
 */
public class ImplFactoryUTestI extends InterfaceTestCase
{
    //-------------------------------------------------------------------------
    // Standard JUnit Class-specific declarations
    
    private static final Class THIS_CLASS = ImplFactoryUTestI.class;
//    private static final IJUnitDocumentor LOG = (new JUnitLog(THIS_CLASS)).getDocumentor();
    
    public ImplFactoryUTestI( String name, ImplFactory f )
    {
        super( name, ImplFactory.class, f );
    }

    
    protected ImplFactory createImplFactory()
    {
        return (ImplFactory)createImplObject();
    }


    //-------------------------------------------------------------------------
    // Tests
    
    
    public void testCreateImplObject1()
    {
        ImplFactory implf = createImplFactory();
        assertNotNull(
            "Returned null factory.",
            implf );
    }
    
    
    //-------------------------------------------------------------------------
    // Standard JUnit declarations
    
    
    public static InterfaceTestSuite suite()
    {
        InterfaceTestSuite suite = new InterfaceTestSuite( THIS_CLASS );
        
        return suite;
    }
    
    public static void main( String[] args )
    {
        String[] name = { THIS_CLASS.getName() };
        
        // junit.textui.TestRunner.main( name );
        // junit.swingui.TestRunner.main( name );
        
        junit.textui.TestRunner.main( name );
    }
    
    
    /**
     * 
     * @exception Exception thrown under any exceptional condition.
     */
    protected void setUp() throws Exception
    {
        super.setUp();
        
        // set ourself up
    }
    
    
    /**
     * 
     * @exception Exception thrown under any exceptional condition.
     */
    protected void tearDown() throws Exception
    {
        // tear ourself down
        
        
        super.tearDown();
    }
}

