/*
 * ------------------------------------------------------------------------
 *        Copyright (c) 2000 University of Coimbra, Portugal
 *
 *                     All Rights Reserved
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose and without fee is hereby granted,
 * provided that the above copyright notice appear in all copies and that
 * both that copyright notice and this permission notice appear in
 * supporting documentation, and that the name of the University of Coimbra
 * not be used in advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 *
 * University of Coimbra distributes this software in the hope that it will
 * be useful but DISCLAIMS ALL WARRANTIES WITH REGARD TO IT, including all
 * implied warranties of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. In no event shall University of Coimbra be liable for any
 * special, indirect or consequential damages (or any damages whatsoever)
 * resulting from loss of use, data or profits, whether in an action of
 * contract, negligence or other tortious action, arising out of or in
 * connection with the use or performance of this software.
 * ------------------------------------------------------------------------
 */

package org.cablelabs.impl.snmp.agentx.protocol;

/**
 * The <code>AgentXProtocolException</code> class implements an AgentX specific Exception.
 * <p>
 * @author Pedro Pereira
 * @version 1.0, 2000/03/01
 */
public class AgentXProtocolException extends Exception
{
    /**
     * Auto generated serial version UID value.
     */    
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a <code>AgentXProtocolException</code> object from the parameters specified.
     * <p>
     * 
     * @param msg the message associated with this <code>AgentXProtocolException</code> object.
     */
    public AgentXProtocolException(AgentXProtocolExceptionReason reason)
    {
        super(reason.toString());
    }
    
    public AgentXProtocolException(Exception reason)
    {
        super(reason);
    }
        
    /**
     * Returns a String containing the description of the <code>AgentXProtocolException</code>.
     * 
     * @return the description message for this <code>AgentXProtocolException</code> object.
     */
    public String toString()
    {
        return "AgentXProtocolException: " + getMessage();
    }

}
