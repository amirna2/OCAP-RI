package org.cablelabs.xlet.hn.UPnPDiagnostics;

    /*
     * Copyright (c) 2009, Cable Television Laboratories, Inc.
     * All rights reserved.
     *
     * Redistribution and use in source and binary forms, with or without
     * modification, are permitted provided that the following conditions are met:
     *
     *   1. Redistributions of source code must retain the above copyright notice,
     *      this list of conditions and the following disclaimer.
     *   2. Redistributions in binary form must reproduce the above copyright
     *      notice, this list of conditions and the following disclaimer in the
     *      documentation and/or other materials provided with the distribution.
     *   3. Neither the name of CableLabs, Inc. nor the names of its contributors
     *      may be used to endorse or promote products derived from this software
     *      without specific prior written permission.
     *
     * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
     * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
     * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
     * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
     * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
     * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
     * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
     * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
     * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
     * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
     * POSSIBILITY OF SUCH DAMAGE.
     */

    // Declare package.

    import javax.tv.xlet.XletStateChangeException;
    import org.ocap.hn.upnp.client.UPnPControlPoint;
    import org.ocap.hn.upnp.common.UPnPMessage;
    import org.ocap.hn.upnp.common.UPnPIncomingMessageHandler;
    import java.net.InetSocketAddress;

    public class UPnPMessageControlXlet implements javax.tv.xlet.Xlet,
    UPnPIncomingMessageHandler{
    /**
     * The class presents a monitor app or similar application
     * that intercepts UPnP messages.
     *
     * @author Cable Television Laboratories, Inc.
     */


        // A flag indicating that the Xlet has been started.
        private boolean m_started = false;

        /**
         * Initializes the OCAP Xlet.
         * <p>
         * A reference to the context is stored for further need.
         * This is the place where any initialisation should be done,
         * unless it takes a lot of time or resources.
         * </p>
         *
         * @param The context for this Xlet is passed in.
         *
         * @throws XletStateChangeException If something goes wrong, then an
         * XletStateChangeException is sent so that the runtime system knows that
         * the Xlet can't be initialised.
         */
        public void initXlet(javax.tv.xlet.XletContext ctx) throws javax.tv.xlet.XletStateChangeException
        {
            try
            {
                maLog ("In initXlet");
            } catch (Exception ex)
            {
                ex.printStackTrace();
                throw new javax.tv.xlet.XletStateChangeException(ex.getMessage());
            }
        }

        /**
         * Starts the OCAP Xlet.
         *
         * @throws XletStateChangeException If something goes wrong, then an
         * XletStateChangeException is sent so that the runtime system knows that
         * the Xlet can't be started.
         */
        public void startXlet() throws javax.tv.xlet.XletStateChangeException
        {
            try
            {
                if (!m_started)
                {
                    m_started = true;
                    // XXX - Do something here, but only things that need to occur once
                    // in the lifecycle of the application.
                }
                maLog ("In startXlet");

            } catch (Exception ex)
            {
                ex.printStackTrace();
                throw new javax.tv.xlet.XletStateChangeException(ex.getMessage());
            }
            maLog ("Establishing UPnPControlPoint");
            UPnPControlPoint deviceController = UPnPControlPoint.getInstance();
            try {
                deviceController.setIncomingMessageHandler(this);
            } catch (Exception e) {
                maLog ("Exception in setIncomingMessageHandler");
                e.printStackTrace();
                return;
            }
        }

        /**
         * Pauses the OCAP Xlet.
         */
        public void pauseXlet()
        {
            if (m_started)
            {
                maLog ("In pauseXlet");
            }
        }

        /**
         * Destroys the OCAP Xlet.
         *
         * @throws XletStateChangeException If something goes wrong, then an
         * XletStateChangeException is sent so that the runtime system knows that
         * the Xlet can't be destroyed.
         */
        public void destroyXlet(boolean forced) throws javax.tv.xlet.XletStateChangeException
        {
            try
            {
                if (m_started)
                {
                    maLog ("In destroyXlet");
                    m_started = false;
                }
            } catch (Exception ex)
            {
                ex.printStackTrace();
                throw new javax.tv.xlet.XletStateChangeException(ex.getMessage());
            }
        }
        private void maLog (String message) {
            System.out.println ("*MC *CP*: " + message );
        }
        public UPnPMessage handleIncomingMessage(InetSocketAddress address,
                byte[] content, UPnPIncomingMessageHandler defaultHandler) {
                maLog ("in handleIncomingMessage");
                UPnPMessage um = defaultHandler.handleIncomingMessage(address,content,defaultHandler);
                if (um.getXML()!= null){
                    maLog( um.getXML().toString());
                }
                return um;
            }
    }
