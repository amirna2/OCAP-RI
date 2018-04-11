/*
 * @(#)CxFactorySample.java
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
 * Sample CxFactory used in CxFactoryUTest.
 *
 * @author    Matt Albrecht <a href="mailto:groboclown@users.sourceforge.net">groboclown@users.sourceforge.net</a>
 * @since     October 30, 2002
 * @version   $Date: 2002/12/08 07:40:09 $
 */
public class CxFactorySample extends CxFactory
{
    public CxFactorySample( String s )
    {
        super( s );
    }
    
    public CxFactorySample( String s, boolean b )
    {
        super( s, b );
    }
    
    public Object createImplObject()
    {
        return "";
    }
}

