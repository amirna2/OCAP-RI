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

package org.cablelabs.impl.manager.application;

/**
 * This is wrapper to class to disguise the fact that we support both
 * javax.tv.xlet.Xlet and javax.microedition.xlet.Xlet Xlet classes.
 * 
 * @author Greg Rutz
 */
abstract class Xlet
{
    /**
     * Create a wrapper Xlet class based on the given JavaTV or JavaME xlet
     * object
     * 
     * @param xlet
     *            the JavaTV or JavaME xlet object
     * @return the wrapper Xlet class
     * @throws IllegalArgumentException
     *             if the given xlet argument is not an instance of either
     *             <code>javax.tv.xlet.Xlet</code> or
     *             <code>javax.microedition.xlet.Xlet</code>
     */
    static public Xlet createInstance(Object xlet)
    {
        if (xlet instanceof javax.tv.xlet.Xlet)
            return new JavaTVXlet((javax.tv.xlet.Xlet) xlet);
        else if (xlet instanceof javax.microedition.xlet.Xlet)
            return new JavaMEXlet((javax.microedition.xlet.Xlet) xlet);
        else
            throw new IllegalArgumentException(
                    "Xlet class is not of type javax.tv.xlet.Xlet or javax.microedition.xlet.Xlet");
    }

    /**
     * initXlet
     * 
     * @param ctx
     * @throws XletStateChangeException
     */
    abstract void initXlet(XletAppContext ctx) throws XletStateChangeException;

    /**
     * startXlet
     * 
     * @throws XletStateChangeException
     */
    abstract void startXlet() throws XletStateChangeException;

    /**
     * pauseXlet
     */
    abstract void pauseXlet();

    /**
     * destroyXlet
     * 
     * @param unconditional
     * @throws XletStateChangeException
     */
    abstract void destroyXlet(boolean unconditional) throws XletStateChangeException;
    
    /**
     * Returns the actual xlet object being wrapped
     * 
     * @return the xlet object
     */
    abstract Object getXlet();
}

/**
 * Concrete instance of our Xlet wrapper class to wrap a JavaTV Xlet
 * 
 * @author Greg Rutz
 */
class JavaTVXlet extends Xlet
{
    JavaTVXlet(javax.tv.xlet.Xlet xlet)
    {
        this.xlet = xlet;
    }

    void initXlet(XletAppContext ctx) throws XletStateChangeException
    {
        try
        {
            xlet.initXlet(ctx);
        }
        catch (javax.tv.xlet.XletStateChangeException e)
        {
            throw new XletStateChangeException(e);
        }
    }

    void startXlet() throws XletStateChangeException
    {
        try
        {
            xlet.startXlet();
        }
        catch (javax.tv.xlet.XletStateChangeException e)
        {
            throw new XletStateChangeException(e);
        }
    }

    void pauseXlet()
    {
        xlet.pauseXlet();
    }

    void destroyXlet(boolean unconditional) throws XletStateChangeException
    {
        try
        {
            xlet.destroyXlet(unconditional);
        }
        catch (javax.tv.xlet.XletStateChangeException e)
        {
            throw new XletStateChangeException(e);
        }
    }

    Object getXlet()
    {
        return xlet;
    }

    javax.tv.xlet.Xlet xlet;
}

/**
 * Concrete instance of our Xlet wrapper class to wrap a JavaME Xlet
 * 
 * @author Greg Rutz
 */
class JavaMEXlet extends Xlet
{
    /**
     * 
     * @param xlet
     */
    JavaMEXlet(javax.microedition.xlet.Xlet xlet)
    {
        this.xlet = xlet;
    }

    void initXlet(XletAppContext ctx) throws XletStateChangeException
    {
        try
        {
            xlet.initXlet(ctx);
        }
        catch (javax.microedition.xlet.XletStateChangeException e)
        {
            throw new XletStateChangeException(e);
        }
    }

    void startXlet() throws XletStateChangeException
    {
        try
        {
            xlet.startXlet();
        }
        catch (javax.microedition.xlet.XletStateChangeException e)
        {
            throw new XletStateChangeException(e);
        }
    }

    void pauseXlet()
    {
        xlet.pauseXlet();
    }

    void destroyXlet(boolean unconditional) throws XletStateChangeException
    {
        try
        {
            xlet.destroyXlet(unconditional);
        }
        catch (javax.microedition.xlet.XletStateChangeException e)
        {
            throw new XletStateChangeException(e);
        }
    }

    Object getXlet()
    {
        return xlet;
    }

    javax.microedition.xlet.Xlet xlet;
}
