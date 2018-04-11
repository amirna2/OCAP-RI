/*
 * @(#)AgentXGetNextPDU.java									v1.0	2000/03/01
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
package org.cablelabs.impl.snmp.agentx.protocol.requests;

import org.cablelabs.impl.snmp.MIBDelegate;
import org.cablelabs.impl.snmp.SNMPBadValueException;
import org.cablelabs.impl.snmp.agentx.AgentXParseErrorException;
import org.cablelabs.impl.snmp.agentx.protocol.AgentXHeader;
import org.cablelabs.impl.snmp.agentx.protocol.AgentXProtocolException;
import org.cablelabs.impl.snmp.agentx.protocol.AgentXResponseError;
import org.cablelabs.impl.snmp.agentx.protocol.ProtocolDataUnitFactory;
import org.cablelabs.impl.snmp.agentx.protocol.ProtocolDataUnitHandler;
import org.cablelabs.impl.snmp.agentx.protocol.notifications.AgentXResponsePDU;
import org.cablelabs.impl.snmp.agentx.types.AgentXOctetString;
import org.cablelabs.impl.snmp.agentx.types.AgentXOid;
import org.cablelabs.impl.snmp.agentx.types.AgentXSearchRange;
import org.cablelabs.impl.snmp.agentx.types.AgentXVarbind;
import org.cablelabs.impl.snmp.agentx.types.util.AgentXSearchRangeList;
import org.cablelabs.impl.snmp.agentx.types.util.AgentXVarbindList;
import org.cablelabs.impl.snmp.agentx.util.AgentXTransactionManager;
import org.ocap.diagnostics.MIBObject;

/**
 * The <code>AgentXGetNextPDU</code> class implements an AgentX GetNext PDU.
 * The structure of a <code>AgentXGetNextPDU</code> object is as follows: <br>
 * <blockquote>
 *      <li><code>Header</code>: AgentX Pdu Header. <br></li>
 *      <li><code>Context</code>: An optional non-default context. <br></li>
 *      <li><code>SearchRangeList</code>: A SearchRangeList containing the requested variables for this session.</li>
 * </blockquote>
 *
 * @author Pedro Pereira
 * @version 1.0, 2000/03/01
 */
public class AgentXGetNextPDU extends AgentXGetPDU
{
    protected final ProtocolDataUnitHandler getNextRequestHandler = new ProtocolDataUnitHandler()
    {
        /*
         * (non-Javadoc)
         *
         * @seeorg.cablelabs.impl.snmp.agentx.protocol.requests.AgentXGetPDU#handlePDURequest
         * (org.cablelabs.impl.snmp.agentx.protocol.ProtocolDataUnitFactory,
         * org.cablelabs.impl.snmp.agentx.AgentXMIBDelegate,
         * org.cablelabs.impl.snmp.agentx.util.AgentXTransactionManager)
         */
        public AgentXResponsePDU handlePDURequest(ProtocolDataUnitFactory pduFactory,
                                                  MIBDelegate mib,
                                                  AgentXTransactionManager transactionMgr)
        {
            short errorOccurred = AgentXResponseError.NOAGENTXERROR;
            short errorIndex = 0;
            AgentXVarbindList varbindList = null;

            try
            {
                varbindList = new AgentXVarbindList();
                final AgentXSearchRangeList searchRangeList = getSearchRangeList();

                for (int index = 0; index < searchRangeList.size(); index++)
                {
                    final AgentXSearchRange searchRange = searchRangeList.getValueAt(index);
                    varbindList.add(getNext(mib, searchRange));
                }
            }
            catch (SNMPBadValueException e)
            {
                /*
                 * If processing should fail for any reason not described below,
                 * res.error is set to `genErr',
                 *
                 * @see RFC2741 section 7.2.3.2. Subagent Processing of the
                 * agentx-GetNext-PDU
                 */
                errorOccurred = AgentXResponseError.GENERR;
            }
            return pduFactory.createResponsePDU(myHeader, errorOccurred, errorIndex, varbindList);
        }
    };

    /**
     * Constructs a <code>AgentXGetNextPDU</code> object from the parameters specified.
     *
     * @param data an encoded <code>AgentXGetNextPDU</code> object.
     * @throws AgentXProtocolException if the <code>data</code> is not valid encoded <code>AgentXGetNextPDU</code>.
     */
    private AgentXGetNextPDU(byte[] data) throws AgentXProtocolException
    {
        int index = 0;

        try
        {
            myHandler = noHeaderHandler;
            index += decodeHeader(data);
            myHandler = parseErrorHandler;
            index += decodeContext(data, index);
            decodeSearchRangeList(data, index);
            myHandler = getNextRequestHandler;
        }
        catch (AgentXParseErrorException e)
        {
            /*
             * Nothing needs to be done here other than eating the exception.  The noHeaderHandler will
             * ensure that the PDU is handled correctly.
             */
        }
    }

    /**
     * Default empty constructor, used for inheritance only.
     */
    protected AgentXGetNextPDU() { }

    /**
     * Constructor for testing purposes only
     *
     * @param context the context to use in a test
     * @param searchRangeList the list of search ranges to use in a test
     */
    protected AgentXGetNextPDU(AgentXHeader header, AgentXOctetString context, AgentXSearchRangeList searchRangeList)
    {
        super(header, context, searchRangeList);
        myHandler = getNextRequestHandler;
    }

    /**
     * Decodes an byte array containing the <code>AgentXGetNextPDU</code> object.
     *
     * @param stream the encoded <code>AgentXGetNextPDU</code> object.
     * @throws AgentXProtocolException if the stream is not a valid encoding of <code>AgentXGetNextPDU</code>.
     */
    public static AgentXGetPDU decode(byte[] stream) throws AgentXProtocolException
    {
        return new AgentXGetNextPDU(stream);
    }

    /**
     * Find the Next OID in the MIB definition within the bounds of the two OIDs
     * specified by the AgentXSearchRange
     *
     * @param mib the delegate used interface between the AgentX code and the SNMP MIB implementation
     * @param searchRange the upper and lower bounds to search for the next OID
     *
     * @return An AgentXVarbind containing the Next OID and it's Value, or the first OID from the AgentXSearchRange
     *
     * @throws SNMPBadValueException An exception occurred while trying to get the next OID
     */
    protected AgentXVarbind getNext(MIBDelegate mib, AgentXSearchRange searchRange) throws SNMPBadValueException
    {
        final AgentXOid beginning = searchRange.getFirstValue();
        final AgentXOid end = searchRange.getSecondValue();

        AgentXVarbind returnVarbind = null;

        if (beginning.isIncluded())
        {
            /*
             * if the "include" field of the starting OID is 1, the variable's
             * name is either equal to, or the closest lexicographical successor
             * to, the starting OID.
             *
             * @see RFC2741 section 7.2.3.2. Subagent Processing of the
             * agentx-GetNext-PDU
             */
            final MIBObject mibValue = mib.get(beginning.getValue());
            
            if (mibValue.getData().length != 0 
                && mibValue.getData()[0] != (byte) AgentXVarbind.NO_SUCH_OBJECT
                && mibValue.getData()[0] != (byte) AgentXVarbind.NO_SUCH_INSTANCE)
            {
                /*
                 * The beginning OID was matched, return that value.
                 */
                return new AgentXVarbind(mibValue.getOID(), mibValue.getData());
            }
        }

        /*
         * Otherwise We'll need to search for the next value.
         */

        final MIBObject mibValue = mib.getNext(beginning.getValue());
        
        if (mibValue.getData().length != 0 
             &&((end.getValue().equals(""))
             || end.compareTo(new AgentXOid(mibValue.getOID())) > 0))
        {
            /*
             * If the ending OID is not null, the variable's name
             * lexicographically precedes the ending OID.
             *
             * @see RFC2741 section 7.2.3.2. Subagent Processing of the
             * agentx-GetNext-PDU
             */
            returnVarbind = new AgentXVarbind(mibValue.getOID(), mibValue.getData());
        }
        else
        {
            /*
             * If the subagent cannot locate an appropriate variable, v.name is
             * set to the starting OID, and the VarBind is set to
             * `endOfMibView', in the manner described in section 5.4,
             * "Value Representation"
             *
             * @see RFC2741 section 7.2.3.2. Subagent Processing of the agentx-GetNext-PDU
             */
            returnVarbind = new AgentXVarbind(beginning.getValue(), AgentXVarbind.END_OF_MIB_VIEW);
        }

        return returnVarbind;
    }
}
