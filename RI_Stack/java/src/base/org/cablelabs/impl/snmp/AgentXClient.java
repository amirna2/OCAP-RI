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

package org.cablelabs.impl.snmp;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.cablelabs.impl.snmp.agentx.AgentXContextNotSupportedException;
import org.cablelabs.impl.snmp.agentx.AgentXDuplicateRegistrationException;
import org.cablelabs.impl.snmp.agentx.AgentXNotOpenException;
import org.cablelabs.impl.snmp.agentx.AgentXOpenFailedException;
import org.cablelabs.impl.snmp.agentx.AgentXParseErrorException;
import org.cablelabs.impl.snmp.agentx.AgentXProcessingErrorException;
import org.cablelabs.impl.snmp.agentx.AgentXRequestDeniedException;
import org.cablelabs.impl.snmp.agentx.AgentXSubAgent;
import org.cablelabs.impl.snmp.agentx.AgentXSubAgentEvent;
import org.cablelabs.impl.snmp.agentx.AgentXSubAgentListener;
import org.cablelabs.impl.snmp.agentx.AgentXUnknownRegistrationException;
import org.ocap.diagnostics.MIBObject;

public class AgentXClient implements AgentXSubAgentListener
{
    private static final Logger log = Logger.getLogger(AgentXClient.class);
    private static final String AGENTX_SESSION_NAME = "OCAP Tru2way Java Stack";
    private static final int AGENTX_MAX_RETRIES = 1;

    private AgentXSubAgent agentX;

    public AgentXClient(MIBDelegate delegator)
    {
        agentX = new AgentXSubAgent(delegator, this);
        startAgentXSession(AGENTX_SESSION_NAME, 0);
    }

    /**
     * Send an SNMP trap/notification.
     *
     * @param oid the Object ID for the trap being delivered.
     * @param data the set of OID/variables being sent with this trap.
     *
     * @throws SNMPTrapException if the trap cannot be sent for any reason.
     */
    public void sendTrap(String oid, MIBObject[] data) throws SNMPTrapException
    {
        try
        {
            agentX.notify(oid, data);
        }
        catch (AgentXParseErrorException e)
        {
            // Wrap the exception and throw it up the chain for someone else to
            // handle
            throw new SNMPTrapException(e);
        }
        catch (AgentXNotOpenException e)
        {
            // Wrap the exception and throw it up the chain for someone else to
            // handle
            throw new SNMPTrapException(e);
        }
        catch (AgentXContextNotSupportedException e)
        {
            // Wrap the exception and throw it up the chain for someone else to
            // handle
            throw new SNMPTrapException(e);
        }
        catch (AgentXProcessingErrorException e) {
            // Wrap the exception and throw it up the chain for someone else to
            // handle
            throw new SNMPTrapException(e);
        }

    }

    public void notify(AgentXSubAgentEvent event)
    {
        /*
         * We may want to make this more intelligent, but for now if the
         * connection closes for any reason then start a new session
         */
        if(event.getCode() == AgentXSubAgentEvent.AGENTX_EVENT_CONNECTION_CLOSED_CODE
                || event.getCode() == AgentXSubAgentEvent.AGENTX_EVENT_CONNECTION_ERROR_CODE)
        {           
            startAgentXSession(AGENTX_SESSION_NAME, 0);             
        }
    }

    /*
     * Private method to hide all the error handling required when talking to
     * the AgentXSubAgent
     */
    private boolean startAgentXSession(String sessionName, int attemptNumber)
    {
        boolean sessionStarted = false;
        int myAttempt = attemptNumber + 1;
        try
        {
            agentX.startSession(AGENTX_SESSION_NAME);
            sessionStarted = true;
        }
        catch (AgentXOpenFailedException e)
        {
            /*
             * Since we don't know why the Open failed, we'll try again. We
             * don't want to have to deal with all the errors so we'll use a
             * method to wrap the error for us.
             */
            if (myAttempt == AGENTX_MAX_RETRIES)
            {
                if (log.isErrorEnabled())
                {
                    log.error("Unable to start an AgentX session with the master for an unknown reason, attempted to retry "
                                + AGENTX_MAX_RETRIES + " times but still failed.");
                }
            }
            else
            {
                sessionStarted = startAgentXSession(sessionName, myAttempt);
            }
        }
        catch (AgentXParseErrorException e)
        {
            /*
             * Since we don't know why the Open failed, we'll try again. We
             * don't want to have to deal with all the errors so we'll use a
             * method to wrap the error for us.
             */
            if (myAttempt == AGENTX_MAX_RETRIES)
            {
                if (log.isErrorEnabled())
                {
                    log.error("Unable parse the response from the master, or the master was unable to parse our message, attempted to retry "
                                + AGENTX_MAX_RETRIES + " times but still failed.");
                }
            }
            else
            {
                sessionStarted = startAgentXSession(sessionName, myAttempt);
            }
        }
        catch (AgentXContextNotSupportedException e)
        {
            /*
             * The master doesn't like our context, and we have no way to change
             * it, so log this exception and give up.
             */
            if (log.isErrorEnabled())
            {
                log.error("Unable to start an AgentX session with the master - Bad context.");
            }
        }
        catch (IOException e)
        {
            /*
             * Since we don't know why IOException occurred, we'll try again. We
             * don't want to have to deal with all the errors so we'll use a
             * method to wrap the error for us.
             */
            if (myAttempt == AGENTX_MAX_RETRIES)
            {
                if (log.isErrorEnabled())
                {
                    log.error("Unable to start an AgentX session with the master due to an IOException, attempted to retry "
                                + AGENTX_MAX_RETRIES + " times but still failed.");
                }
            }
            else
            {
                sessionStarted = startAgentXSession(sessionName, myAttempt);
            }
        }
        return sessionStarted;
    }

    /**
     * Register an OID with the agentX sub-agent
     * @param oid OID to be registered
     * @return true on successful registration
     * @throws IllegalArgumentException if the OID is already registered
     */
    public boolean registerOid(String oid) throws IllegalArgumentException
    {
        return registerOid(oid, 0);
    }

    /*
     * Private method to hide all the error handling required when talking to the AgentXSubAgent.
     * @throw new IllegalArgumentException if the OID is already registered
     */
    private boolean registerOid(String oid, int attemptNumber) throws IllegalArgumentException
    {
        boolean oidRegistered = false;
        int myAttempt = attemptNumber + 1;
        try
        {
            agentX.register(oid);
            oidRegistered = true;
        }
        catch (AgentXRequestDeniedException e)
        {
            /*
             * The master doesn't wish to permit this registration for
             * implementation specific reasons just print to the log and give up
             */
            if (log.isErrorEnabled())
            {
                log.error("Unable to register OID: " + oid + "Host Denied request");
            }
        }
        catch (AgentXDuplicateRegistrationException e)
        {
            /*
             * @see org.ocap.diagnostics.MIBManager#registerOID(java.lang.String, int, boolean, int, org.ocap.diagnostics.MIBListener)
             * We should throw an IllegalArgumentException if the OID is already registered
             */
            throw new IllegalArgumentException("OID is already registered with the native host");
        }
        catch (AgentXParseErrorException e)
        {
            if (myAttempt == AGENTX_MAX_RETRIES)
            {
                if (log.isErrorEnabled())
                {
                    log.error("Unable parse the response from the master, or the master was unable to parse our message, attempted to retry "
                                + AGENTX_MAX_RETRIES + " times but still failed.");
                }
            }
            else
            {
                oidRegistered = registerOid(oid, myAttempt);
            }
        }
        catch (AgentXContextNotSupportedException e)
        {
            /*
             * The master reported that our context is invalid, nothing to do so
             * just print to the log and give up
             */
            if (log.isErrorEnabled())
            {
                log.error("Unable to register OID: " + oid + "Host Denied request");
            }
        }
        catch (AgentXNotOpenException e)
        {
            if ((myAttempt != AGENTX_MAX_RETRIES) && (startAgentXSession(AGENTX_SESSION_NAME, 0)))
            {
                oidRegistered = registerOid(oid, myAttempt);
            }
            else
            {
                if (log.isErrorEnabled())
                {
                    log.error("Unable to start a new session, cannot register oid: " + oid);
                }
            }
        }

        return oidRegistered;
    }

    /*
     * Private method to hide all the error handling required when talking to the AgentXSubAgent.
     * If we receive an AgentXUnknownRegistrationException then we'll treat it as a success
     */
    public boolean unregisterOid(String oid, int attemptNumber)
    {
        boolean oidUnregistered = false;
        int myAttempt = attemptNumber + 1;

        try
        {
            agentX.unregister(oid);
            oidUnregistered = true;
        }
        catch (AgentXUnknownRegistrationException e)
        {
            // The master doesn't know about this OID, treat this as a success
            oidUnregistered = true;
        }
        catch (AgentXParseErrorException e)
        {
            if (myAttempt == AGENTX_MAX_RETRIES)
            {
                if (log.isErrorEnabled())
                {
                    log.error("Unable parse the response from the master, or the master was unable to parse our message, attempted to retry "
                                + AGENTX_MAX_RETRIES + " times but still failed.");
                }
            }
            else
            {
                oidUnregistered = unregisterOid(oid, myAttempt);
            }
        }
        catch (AgentXContextNotSupportedException e)
        {
            /*
             * The master reported that our context is invalid, nothing to do so
             * just print to the log and give up
             */
            if (log.isErrorEnabled())
            {
                log.error("Unable to unregister OID: " + oid + "Host Denied request");
            }
        }
        catch (AgentXNotOpenException e)
        {
            if ((myAttempt != AGENTX_MAX_RETRIES) && (startAgentXSession(AGENTX_SESSION_NAME, 0)))
            {
                oidUnregistered = unregisterOid(oid, myAttempt);
            }
            else
            {
                if (log.isErrorEnabled())
                {
                    log.error("Unable to start a new session, cannot register oid: " + oid);
                }
            }
        }

        return oidUnregistered;
    }
}
