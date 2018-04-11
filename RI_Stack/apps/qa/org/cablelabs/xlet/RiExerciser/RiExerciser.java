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

package org.cablelabs.xlet.RiExerciser;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.apache.log4j.Logger;


public class RiExerciser implements Xlet
{
    /**
     * Added to silence the compiler
     */
    private static final long serialVersionUID = -3599127111275002970L;

    private static final Logger log = Logger.getLogger(RiExerciser.class);

    // The OCAP Xlet context.
    private XletContext m_ctx;
    
    // A reference to the Controller
    private RiExerciserController m_rxController = null;

    protected static String m_persistentDirStr = null;

    private boolean m_dvrExtEnabled = false;
   
    private boolean m_hnExtEnabled = false;

    /**
     * The default constructor.
     */
    public RiExerciser()
    {

        if (log.isInfoEnabled())
        {
            log.info("RiExerciser()");
        }
    }

    /**
     * Initializes the OCAP Xlet.
     * 
     * @param ctx
     *            the context for this Xlet A reference to the context is stored
     *            for further need. This is the place where any initialization
     *            should be done, unless it takes a lot of time or resources.
     * 
     * @throws XletStateChangeException
     *             If something goes wrong, then an XletStateChangeException is
     *             sent so that the runtime system knows that the Xlet can't be
     *             initialized.
     */
    public void initXlet(javax.tv.xlet.XletContext ctx) throws
                         javax.tv.xlet.XletStateChangeException
    {   
        //PropertyConfigurator.configure("log4j.properties");
        m_rxController = RiExerciserController.getInstance();
        m_rxController.init(ctx);
        if (log.isInfoEnabled())
        {
            log.info("RiExerciser.initXlet()");
        }

        // save the xlet context
        m_ctx = ctx;

        // Determine if DVR extension is enabled
        m_dvrExtEnabled = true;
        if (System.getProperty("ocap.api.option.dvr") == null)
        {
            m_dvrExtEnabled = false;            
        }
        
        // Determine if HN Extension is enabled
        m_hnExtEnabled = true;
        if (System.getProperty("ocap.api.option.hn") == null)
        {
            m_hnExtEnabled = false;            
        }

        // Formulate the persistent storage dir path
        String persistentRoot = System.getProperty("dvb.persistent.root");
        String oid = (String) m_ctx.getXletProperty("dvb.org.id");
        String aid = (String) m_ctx.getXletProperty("dvb.app.id");
        m_persistentDirStr = persistentRoot + "/" + oid + "/" + aid + "/";
    }


    /**
     * Starts the OCAP Xlet.
     * 
     * @throws XletStateChangeException
     *             If something goes wrong, then an XletStateChangeException is
     *             sent so that the runtime system knows that the Xlet can't be
     *             started.
     */
    public void startXlet() throws javax.tv.xlet.XletStateChangeException
    {
        if (log.isInfoEnabled())
        {
            log.info("RiExerciser.startXlet()");
        }
    }

    /**
     * Pauses the OCAP Xlet.
     */
    public void pauseXlet()
    {
        if (log.isInfoEnabled())
        {
            log.info("RiExerciser.pauseXlet()");
        }
    }

    /**
     * Destroys the OCAP Xlet.
     * 
     * @throws XletStateChangeException
     *             If something goes wrong, then an XletStateChangeException is
     *             sent so that the runtime system knows that the Xlet can't be
     *             destroyed.
     */
    public void destroyXlet(boolean forced) throws
                       javax.tv.xlet.XletStateChangeException
    {
        if (log.isInfoEnabled())
        {
            log.info("RiExerciser.destroyXlet()");
        }
    }
    
    /**
     * Determines if DVR extension is enabled.
     * 
     * @return  true if DVR is enabled, false otherwise
     */
    public boolean isDvrExtEnabled()
    {
        return m_dvrExtEnabled;
    }

    /**
     * Determines if Home Networking extension is enabled.
     * 
     * @return  true if HN is enabled, false otherwise
     */
    public boolean isHnExtEnabled()
    {
        return m_hnExtEnabled;
    }
}
