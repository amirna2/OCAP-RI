/*
 * @(#)AgentX_SubAgent.java									v1.0	2000/03/01
 *
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
package org.cablelabs.impl.snmp.agentx;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;

import org.cablelabs.impl.snmp.MIBDelegate;
import org.cablelabs.impl.snmp.MIBTransaction;
import org.cablelabs.impl.snmp.MIBTransactionManager;
import org.cablelabs.impl.snmp.SNMPBadValueException;

import org.cablelabs.impl.snmp.agentx.protocol.AgentXHeader;
import org.cablelabs.impl.snmp.agentx.protocol.AgentXProtocolException;
import org.cablelabs.impl.snmp.agentx.protocol.AgentXResponseError;
import org.cablelabs.impl.snmp.agentx.protocol.ProtocolDataUnitFactory;
import org.cablelabs.impl.snmp.agentx.protocol.ProtocolDataUnitHandler;
import org.cablelabs.impl.snmp.agentx.protocol.ProtocolDataUnitHandlerFactory;
import org.cablelabs.impl.snmp.agentx.protocol.ProtocolDataUnitType;
import org.cablelabs.impl.snmp.agentx.protocol.admin.AgentXClosePDU;
import org.cablelabs.impl.snmp.agentx.protocol.admin.AgentXOpenPDU;
import org.cablelabs.impl.snmp.agentx.protocol.admin.AgentXRegisterPDU;
import org.cablelabs.impl.snmp.agentx.protocol.admin.AgentXUnregisterPDU;
import org.cablelabs.impl.snmp.agentx.protocol.notifications.AgentXNotifyPDU;
import org.cablelabs.impl.snmp.agentx.protocol.notifications.AgentXResponsePDU;
import org.cablelabs.impl.snmp.agentx.types.util.AgentXVarbindList;
import org.cablelabs.impl.snmp.agentx.util.AgentXTransactionManager;
import org.cablelabs.impl.util.MPEEnv;

import org.ocap.diagnostics.MIBObject;

/**
 * This class provides a higher level session abstraction for the communications
 * with the master agent.
 * 
 * @author Kevin Hendry
 */
public class AgentXSubAgent
{
    private static final Logger log = Logger.getLogger(AgentXSubAgent.class);

    private static final String SESSION_NOT_STARTED_MSG = "The session has not been started.";
    private static final String PARSE_ERROR_DEFAULT_MSG = "The master could not parse the request. "
                                                            + "Or the subagent could not parse the response.";
    
    private static final int DEFAULT_COMMS_TIMEOUT = 60000;
    private static final int UNKOWN_ERROR = -1;

    private long mySessionId = -1;
    private long packetId = 0;
    private boolean isSessionActive = false;

    private AgentXTransactionManager myTransactionManager;
    private AgentXResponsePDU myLastResponse;
    private Object lock = new Object();

    private Socket myConnection;
    private InputStream in;
    private OutputStream out;

    private ProtocolDataUnitFactory myPDUFactory;
    private ProtocolDataUnitHandlerFactory myHandlerFactory;
    private MIBDelegate myMibDelegate;
    private AgentXSubAgentListener myListener;
    private MIBTransactionManager myMIBTransactionManager;

    /**
     * Construct a newly allocated <code>AgentXSubagent</code> with its
     * components set to the defaults values.
     * 
     * @param mibDelegate a delegate implementation provided to the AgentXSubAgent to provide a simple 
     *                    interface to interact with the MIB database.
     * @param listener a listener used to detect supported AgentX subagent events.
     */
    public AgentXSubAgent(MIBDelegate mibDelegate, AgentXSubAgentListener listener)
    {
        myPDUFactory = new ProtocolDataUnitFactory();
        myHandlerFactory = new ProtocolDataUnitHandlerFactory();
        myTransactionManager = new AgentXTransactionManager();
        
        myMibDelegate = mibDelegate;
        myMIBTransactionManager = mibDelegate.getTransactionManager();

        if (listener != null)
        {
            myListener = listener;
        }
        else
        {
            /*
             * A null listener implementation. Just to avoid null checks later
             * on.
             */
            myListener = new AgentXSubAgentListener()
            {
                public void notify(AgentXSubAgentEvent e)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("AgentXSubAgentEvent: " + e.getCode());
                    }
                }
            };
        }
    }

    /**
     * This constructor is intended to be used only by a unit test. DO NOT USE THIS!
     * 
     * @param mibDelegate a Mock delegate implementation for testing.
     * @param transMgr a Mock transaction manager for testing.
     * @param pduFactory a Mock PDU factory for testing.
     * @param handlerFactory a Mock PDU handler factor for testing.
     */
    protected AgentXSubAgent(MIBDelegate mibDelegate, 
                             AgentXTransactionManager transMgr,
                             ProtocolDataUnitFactory pduFactory, 
                             ProtocolDataUnitHandlerFactory handlerFactory,
                             AgentXSubAgentListener listener)
    {
        myPDUFactory = pduFactory;
        myHandlerFactory = handlerFactory;
        myMibDelegate = mibDelegate;
        myTransactionManager = transMgr;
        myListener = listener;
    }

    /**
     * Starts a session with master agent.
     * 
     * @param description a description of this <code>AgentXSubagent</code>.
     * 
     * @throws AgentXOpenFailedException
     *             if the request to open the session with the master has
     *             failed.
     * @throws AgentXParseErrorException
     *             if there was an error in parsing the data stream either sent
     *             or received.
     * @throws AgentXContextNotSupportedException
     *             if an invalid context is reported by the master.
     * @throws IOException
     *             if there was a problem creating the socket connection with
     *             the master agent.
     */
    public synchronized void startSession(String description) throws AgentXOpenFailedException,
                                                                     AgentXParseErrorException, 
                                                                     AgentXContextNotSupportedException, 
                                                                     IOException
    {
        /*
         * We are only supporting one session at a time for now.
         */
        if (isSessionActive)
        {
            throw new IllegalStateException("Session is already started.");
        }

        if (setupConnection())
        {
            /*
             * Start a session thread waiting listening for data from the master
             * agent.
             */
            new Thread(new Runnable() 
            {
                public void run()
                {
                    sessionThreadEntry();
                }        
            }).start();
                        
            final long currentPacketId = getNextPacketId();
            final AgentXOpenPDU open = myPDUFactory.createOpenPDU(currentPacketId, description);

            /*
             * Manage the response PDU from the master.
             */
            switch (sendPDU(currentPacketId, open.encode()))
            {
                case AgentXResponseError.NOAGENTXERROR:
                    mySessionId = myLastResponse.getSessionId();
                    isSessionActive = true;
                    break;
                case AgentXResponseError.OPENFAILED:
                    throw new AgentXOpenFailedException();
                case AgentXResponseError.UNSUPPORTEDCONTEXT:
                    throw new AgentXContextNotSupportedException();
                case AgentXResponseError.PARSEERROR:
                default:
                    throw new AgentXParseErrorException(PARSE_ERROR_DEFAULT_MSG);
            }
        }

        if (log.isDebugEnabled())
        {
            log.debug("Done. Session number = " + mySessionId);
        }
    }

    /**
     * Register an OID range according to the given parameters.
     * 
     * @param subTreeOid the starting OID for this registration.
     * 
     * @throws AgentXRequestDeniedException
     *             if the master doesn't wish to permit this registration for
     *             implementation specific reasons.
     * @throws AgentXDuplicateRegistrationException
     *             if the registration would result in the registration of
     *             duplicate subtrees.
     * @throws AgentXParseErrorException
     *             if there was an error in parsing the data stream either sent
     *             or received.
     * @throws AgentXContextNotSupportedException
     *             if an invalid context is reported by the master.
     * @throws AgentXNotOpenException
     *             if the master believes that the session is not open.
     */
    public synchronized void register(String subTreeOid) throws AgentXRequestDeniedException,
                                                         AgentXDuplicateRegistrationException, 
                                                         AgentXParseErrorException, 
                                                         AgentXContextNotSupportedException,
                                                         AgentXNotOpenException
    {
        if (isSessionActive)
        {
            try
            {
                final long currentPacketId = getNextPacketId();
                final byte range = 0;
                final int upperBound = 0;
                final AgentXRegisterPDU register = myPDUFactory.createRegisterPDU(mySessionId, 
                                                                                  currentPacketId, 
                                                                                  subTreeOid, 
                                                                                  range,
                                                                                  upperBound);

                if (log.isDebugEnabled())
                {
                    log.debug("Registering...");
                }

                switch (sendPDU(currentPacketId, register.encode()))
                {
                    case AgentXResponseError.NOAGENTXERROR:
                        /*
                         * If there is no error, we don't need to do anything.
                         */
                        if (log.isDebugEnabled())
                        {
                            log.debug("Registered OID: " + subTreeOid);
                        }
                        break;
                    case AgentXResponseError.REQUESTDENIED:
                        throw new AgentXRequestDeniedException();
                    case AgentXResponseError.DUPLICATEREGISTRATION:
                        throw new AgentXDuplicateRegistrationException();
                    case AgentXResponseError.UNSUPPORTEDCONTEXT:
                        throw new AgentXContextNotSupportedException();
                    case AgentXResponseError.NOTOPEN:
                        throw new AgentXNotOpenException();
                    default:
                        throw new AgentXParseErrorException(PARSE_ERROR_DEFAULT_MSG);
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
                throw new AgentXNotOpenException();
            }
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("Not connected to Master");
            }
            throw new IllegalStateException(SESSION_NOT_STARTED_MSG);
        }
    }

    /**
     * Register an OID range according to the given parameters.
     * 
     * @param oid the starting OID for this un-registration.
     * 
     * @throws AgentXUnknownRegistrationException
     *             if an matching registration cannot be found by the master.
     * @throws AgentXParseErrorException
     *             if there was an error in parsing the sent or received data on
     *             the wire.
     * @throws AgentXContextNotSupportedException
     *             if an invalid context is reported by the master.
     * @throws AgentXNotOpenException
     *             if the master believes that the session is not open.
     */
    public synchronized void unregister(String oid) throws AgentXUnknownRegistrationException,
                                                           AgentXParseErrorException, 
                                                           AgentXContextNotSupportedException, 
                                                           AgentXNotOpenException
    {
        if (isSessionActive)
        {
            try
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Unregistering...");
                }

                final byte rangeSubId = 0;
                final int upperBound = 0;
                final long currentPacketId = getNextPacketId();

                final AgentXUnregisterPDU unregister = myPDUFactory.createUnregisterPDU(mySessionId, 
                                                                                        currentPacketId, 
                                                                                        oid,
                                                                                        rangeSubId, 
                                                                                        upperBound);

                switch (sendPDU(currentPacketId, unregister.encode()))
                {
                    case AgentXResponseError.NOAGENTXERROR:
                        /*
                         * If there is no error, we don't need to do anything.
                         */
                        break;
    
                    case AgentXResponseError.UNKNOWNREGISTRATION:
                        throw new AgentXUnknownRegistrationException();
                    case AgentXResponseError.UNSUPPORTEDCONTEXT:
                        throw new AgentXContextNotSupportedException();
                    case AgentXResponseError.NOTOPEN:
                        throw new AgentXNotOpenException();
                    default:
                        throw new AgentXParseErrorException(PARSE_ERROR_DEFAULT_MSG);
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
                throw new AgentXNotOpenException();
            }
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("Not connected to Master");
            }

            throw new IllegalStateException(SESSION_NOT_STARTED_MSG);
        }
    }

    /**
     * Close the session with the master.
     * 
     * @param reason the closing session reason.
     * 
     * @throws AgentXNotOpenException
     *             if the master believes that the session is not open.
     * @throws AgentXContextNotSupportedException
     *             if an invalid context is reported by the master.
     * @throws AgentXParseErrorException
     *             if there was an error in parsing the sent or received data on
     *             the wire.
     */
    public synchronized void close(AgentXCloseReason reason) throws AgentXNotOpenException,
                                                                    AgentXContextNotSupportedException, 
                                                                    AgentXParseErrorException
    {
        if (isSessionActive)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Closing...");
            }

            try
            {
                final long currentPacketId = getNextPacketId();
                final AgentXClosePDU close = myPDUFactory.createClosePDU(mySessionId, currentPacketId, reason);

                switch (sendPDU(currentPacketId, close.encode()))
                {
                    case AgentXResponseError.NOAGENTXERROR:
                        /*
                         * Clean up our internal state and close the socket
                         * connection with the master.
                         */
                        myConnection.close();
                        mySessionId = -1;
                        isSessionActive = false;
                        break;
                    case AgentXResponseError.NOTOPEN:
                        throw new AgentXNotOpenException();
                    case AgentXResponseError.UNSUPPORTEDCONTEXT:
                        throw new AgentXContextNotSupportedException();
                    default:
                        throw new AgentXParseErrorException(PARSE_ERROR_DEFAULT_MSG);
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
                throw new AgentXNotOpenException();
            }
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("Not connected to Master");
            }
            throw new IllegalStateException(SESSION_NOT_STARTED_MSG);
        }
    }

    /**
     * Notify the master of a trap event to be delivered by the master in an
     * implementation specific manner..
     * 
     * @param snmpTrapOid the OID for the trap.
     * @param varBindList the set of OIDs and data associated with the trap.
     * 
     * @throws AgentXParseErrorException
     *             if there was an error in parsing the sent or received data on
     *             the wire.
     * @throws AgentXNotOpenException
     *             if the master believes that the session is not open.
     * @throws AgentXContextNotSupportedException
     *             if an invalid context is reported by the master.
     * @throws AgentXProcessingErrorException 
     *             if there is an error in processing the PDU in the master.
     */
    public void notify(String snmpTrapOid, MIBObject[] varBindList) throws AgentXParseErrorException,
                                                                           AgentXNotOpenException, 
                                                                           AgentXContextNotSupportedException, 
                                                                           AgentXProcessingErrorException
    {
        if (isSessionActive)
        {
            try
            {
                final long currentPacketId = getNextPacketId();
                final AgentXVarbindList list = new AgentXVarbindList(varBindList);                
                final AgentXNotifyPDU notify = myPDUFactory.createNotifyPDU(mySessionId, 
                                                                            currentPacketId, 
                                                                            snmpTrapOid, 
                                                                            list);

                switch (sendPDU(currentPacketId, notify.encode()))
                {
                    case AgentXResponseError.NOAGENTXERROR:
                        /*
                         * If there is no error, we don't need to do anything.
                         */
                        break;
                    case AgentXResponseError.UNSUPPORTEDCONTEXT:
                        throw new AgentXContextNotSupportedException();
                    case AgentXResponseError.NOTOPEN:
                        throw new AgentXNotOpenException();
                    case AgentXResponseError.PROCESSINGERROR:
                        throw new AgentXProcessingErrorException();
                    default:
                        throw new AgentXParseErrorException(PARSE_ERROR_DEFAULT_MSG);
                }
            }
            catch (SNMPBadValueException e)
            {
                e.printStackTrace();
                throw new IllegalArgumentException("Invalid data in the Varbind List, or an invalid OID.");
            }
            catch (IOException e)
            {
                e.printStackTrace();
                throw new AgentXNotOpenException();
            }
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("Not connected to Master");
            }

            throw new IllegalStateException(SESSION_NOT_STARTED_MSG);
        }
    }

    /*
     * Create a connection with the master agent.
     */
    private boolean setupConnection()
    {
        String masterAddress = MPEEnv.getEnv("OCAP.snmp.agentX.masterAddress","127.0.0.1");
        int masterPort = MPEEnv.getEnv("OCAP.snmp.agentX.masterPort", 10705);
        try
        {
            myConnection = new Socket(masterAddress, masterPort);

            in = myConnection.getInputStream();
            out = myConnection.getOutputStream();
            return true;
        }
        catch (UnknownHostException uhe)
        {
            uhe.printStackTrace();
            return false;
        }
        catch (IOException ioe)
        {
            ioe.printStackTrace();
            return false;
        }
    }

    /*
     * Increment the packet ID used when communicating with the master agent.
     */
    private long getNextPacketId()
    {
        return ++packetId;
    }

    /*
     * Update the internal state of the subagent once a response has been
     * detected.
     */
    private void wakeUpWithResponse(AgentXResponsePDU receivedResponse)
    {
        synchronized (lock)
        {
            myLastResponse = receivedResponse;
            lock.notifyAll();
        }
    }

    /*
     * Block the current thread of execution until a response has been delivered
     * or the timeout period expires.
     */
    private void waitForResponse()
    {
        synchronized (lock)
        {
            for (;;)
            {
                try
                {
                    if (myLastResponse == null)
                    {
                        lock.wait(DEFAULT_COMMS_TIMEOUT);
                    }
                    break;
                }
                catch (InterruptedException e)
                {
                    /*
                     * There is nothing for it Mr Frodo...for the Shire!
                     */
                    e.printStackTrace();
                }
            }
        }
    }

    /*
     * Send a PDU to the master and block until a response has been retrieved,
     * or a timeout. Then provide an error code in response for the caller to
     * decide what to do next.
     */
    private int sendPDU(long packetId, byte[] pdu) throws IOException
    {
        myLastResponse = null;
        out.write(pdu);
        waitForResponse();

        return ((myLastResponse != null) && (myLastResponse.getPacketId() == packetId)) 
                    ? myLastResponse.getErrorCode() : UNKOWN_ERROR;
    }

    /*
     * The main thread of execution for a session. Listens for data on the wire
     * from the master agent.
     */
    private void sessionThreadEntry()
    {
        for (;;)
        {
            try
            {
                /*
                 * Block until some data is read from the master.
                 */
                final byte[] pdu = readProtocolDataUnit();
                handlePDUData(pdu);
            }
            catch (AgentXProtocolException axpe)
            {
                /*
                 * In this case a parse error happened and wasn't
                 * handled...we'll just eat it and try again.
                 */
                axpe.printStackTrace();
                continue;
            }
            catch (SocketException se)
            {
                /*
                 * This should be thrown when the socket has been closed.
                 */
                se.printStackTrace();
                isSessionActive = false;
                myListener.notify(AgentXSubAgentEvent.CONNECTION_CLOSED_EVENT);
                break;
            }
            catch (IOException ioe)
            {
                /*
                 * There was an error in reading/writing on the socket. The
                 * thread should exit.
                 */
                ioe.printStackTrace();
                isSessionActive = false;
                myListener.notify(AgentXSubAgentEvent.CONNECTION_ERROR_EVENT);
                break;
            }
        } /* for */
    }

    /*
     * A private class implementing the MIBTransaction interface in order to 
     * ensure that the operations done on the MIB via the delegate are atomic. 
     */
    private class PDUHandlerTransaction implements MIBTransaction
    {
        private AgentXResponsePDU response;
        private ProtocolDataUnitHandler myHandler;

        public PDUHandlerTransaction(ProtocolDataUnitHandler handler)
        {
            myHandler = handler;
        }
        
        public void executeTransaction()
        {
            response = myHandler.handlePDURequest(myPDUFactory, myMibDelegate, myTransactionManager);
        }
        
        public AgentXResponsePDU getResponse()
        {
            return response;
        }
    }
    
    /*
     * Process the PDU data read off the wire.
     */
    private void handlePDUData(byte[] pdu) throws AgentXProtocolException, IOException
    {
        final int TYPE_INDEX = 1;
        final byte type = pdu[TYPE_INDEX];

        if (type != ProtocolDataUnitType.RESPONSE_PDU)
        {
            final ProtocolDataUnitHandler handler = myHandlerFactory.getRequestPDUHandler(type, pdu);
            final PDUHandlerTransaction transaction = new PDUHandlerTransaction(handler);            
            
            myMIBTransactionManager.performTransaction(transaction);
            
            if (transaction.getResponse() != null)
            {
                out.write(transaction.getResponse().encode());
            }
        }
        else if (type == ProtocolDataUnitType.RESPONSE_PDU)
        {
            AgentXResponsePDU response;
            try
            {
                response = AgentXResponsePDU.decode(pdu);
            }
            catch (AgentXParseErrorException e)
            {
                response = null;
            }
            
            wakeUpWithResponse(response);
        }
    }

    /*
     * Read a PDU sent from the master over the connection.
     */
    private byte[] readProtocolDataUnit() throws IOException, AgentXProtocolException
    {        
        try
        {
            final byte[] rawHeader = readHeader();
            final AgentXHeader header = AgentXHeader.decode(rawHeader);
            final byte[] rawPayload = readPayload(header.getPayloadLength());
            
            return concatenate(rawHeader, rawPayload);
        }
        catch (AgentXParseErrorException e)
        {
            throw new AgentXProtocolException(e);
        }
    }

    /*
     * Concatenate the header and the PDU payload into a complete array.
     */
    private byte[] concatenate(byte[] header, byte[] payload)
    {
        final byte[] pdu = new byte[header.length + payload.length];

        System.arraycopy(header, 0, pdu, 0, header.length);
        System.arraycopy(payload, 0, pdu, header.length, payload.length);
        
        return pdu;
    }

    /*
     * Read the number of bytes in a PDU header. This should be at the start of
     * any read.
     */
    private byte[] readHeader() throws IOException
    {
        return readData(AgentXHeader.HEADER_LENGTH);
    }

    /*
     * Read the number of bytes in the payload as indicated by the proper field
     * in the header.
     */
    private byte[] readPayload(int payloadLength) throws IOException
    {        
        return readData(payloadLength);
    }

    private byte[] readData(int length) throws IOException
    {
        final byte[] data = new byte[length];
        final int count = in.read(data);
        if (count < length)
        {
            throw new IOException("Stream ended unexpectedly.");
        }
        return data;
    }
    
    /*
     * NOTE: TEST ONLY!
     */
    protected void setSessionActive(boolean isActive)
    {
        isSessionActive = isActive;
    }

    /*
     * NOTE: TEST ONLY!
     */
    protected void setSessionId(int sessionId)
    {
        mySessionId = sessionId;
    }

    /*
     * NOTE: TEST ONLY!
     */
    protected void setConnection(Socket conn) throws IOException
    {
        myConnection = conn;
        in = myConnection.getInputStream();
        out = myConnection.getOutputStream();        
    }
}
