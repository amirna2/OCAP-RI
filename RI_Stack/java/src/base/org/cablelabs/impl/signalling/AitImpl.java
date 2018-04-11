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

package org.cablelabs.impl.signalling;

import org.apache.log4j.Logger;
import org.cablelabs.impl.signalling.Ait;
import org.cablelabs.impl.signalling.AppEntry;
import org.dvb.application.AppID;

import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.Stack;
import java.util.Vector;

import java.security.AccessController;
import sun.security.action.GetPropertyAction;

/**
 * Addressable X/AIT handling. This class provides data structures and methods
 * for performing evaluation of addressing descriptors in X/AIT. See OCAP
 * Section 11.2.2.5.
 * 
 * X/AIT data parsing classes extend this class and populate its data
 * structures. The SignallingMgr can then perform evaluation on this data to
 * determine which apps are valid for this host.
 * 
 * @author Greg Rutz
 */
public class AitImpl implements Ait
{
    public void initialize(int version, Vector externalAuth, Vector allApplications,
                           Hashtable attributeMap, Hashtable addrGroups)
    {
        this.attributeMap = attributeMap;
        this.addrGroups = addrGroups;
        
        /**
         * Update the list of security properties referenced by all addressing
         * descriptors and validate each address descriptor.  OCAP-1.0 11.2.2.5.1.2 
         * indicates that addressing descriptor comparison expressions that contain
         * an attribute ID that does not appear in the attribute map, should cause
         * their entire containing addressing_descriptor to be discarded.
         */
        Set allSecurityAttributes = new HashSet();
        
        // Search for invalid addressing descriptors and update our list of
        // security properties
        for (Enumeration e = addrGroups.keys(); e.hasMoreElements();)
        {
            Object key = e.nextElement();
            TreeSet group = (TreeSet) addrGroups.get(key);
            for (Iterator i = group.iterator(); i.hasNext();)
            {
                AddressingDescriptor ad = (AddressingDescriptor) i.next();

                // Validate AddressingDescriptor
                if (!ad.validate())
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("AddressingDescriptor [" + ad + "] is invalid");
                    }
                    i.remove();
                    continue;
                }
                
                // Update our security properties
                for (Iterator j = ad.securityAttributeIDs.iterator(); j.hasNext();)
                {
                    Integer id = (Integer)j.next();
                    allSecurityAttributes.add(attributeMap.get(id));
                }
            }

            // If the group is now empty, remove it from the table
            if (group.isEmpty())
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Addressing group [" + key + "] is now empty.  Removing...");
                }
                addrGroups.remove(key);
            }
        }
        
        this.version = version;
        this.externalAuth = externalAuth;
        this.securityProperties.addAll(allSecurityAttributes);
        this.allApplications = allApplications;
    }
    
    /**
     * Implements Ait.getApps(). Returns the list of apps that are valid for
     * this host as determined by addressable properties.
     * 
     * @return the list of apps valid for this host. Returns an empty list if
     *         filterApps() has not yet been called.
     */
    public synchronized AppEntry[] getApps()
    {
        AppEntry[] apps = new AppEntry[validApps.size()];
        validApps.copyInto(apps);
        return apps;
    }

    /**
     * Returns a list of security property names referenced by all addressing
     * descriptors included in this X/AIT. Used to manage the polling
     * requirements for security attributes.
     * 
     * @return the properties identified as security properties and their last
     *         known value (computed from a call to filterApps()).
     * 
     * @see �OCAP-1.0: 11.2.2.5.5 Addressable Attribute Management�
     */
    public Vector getSecurityProps()
    {
        return securityProperties;
    }

    /**
     * Implements the logic to filter the full set of applications signaled in
     * this X/AIT according to addressable rules. This creates the list of apps
     * that will be returned by subsequent calls to getApps().
     * 
     * @param securityProps
     *            the set of recently-retrieved properties from the CableCARD
     *            that should be used to resolve all security attribute queries
     * @param registeredProps
     *            the set of properties registered by privileged applications
     * @return true if the list of valid apps has changed since the last filter
     *         call. Always returns true the first time.
     */
    public synchronized boolean filterApps(Properties securityProps, Properties registeredProps)
    {
        // Generate a list of all valid address labels according to the
        // rules described in OCAP-1.0 11.2.2.5
        Vector validLabels = new Vector();

        // Iterate through each addressing group
        for (Enumeration e = addrGroups.keys(); e.hasMoreElements();)
        {
            Integer key = (Integer)e.nextElement();
            TreeSet group = (TreeSet)addrGroups.get(key);
            
            if (log.isDebugEnabled())
            {
                log.debug("filterApps: checking addressing group " + key);
            }

            // Each group has its member addressing descriptors sorted by
            // priority from highest to lowest
            for (Iterator i = group.iterator(); i.hasNext();)
            {
                AddressingDescriptor ad = (AddressingDescriptor) i.next();
                if (ad.evaluate(securityProps, registeredProps))
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("filterApps: " + ad.addressLabel + " is a valid address label");
                    }
                    validLabels.add(new Integer(ad.addressLabel));
                    break; // Only the first match from each group
                }
            }
        }

        Vector newValidApps = new Vector();
        Vector addressableApps = new Vector();
        Vector nonAddressableApps = new Vector();

        // Determine if there were any addressable applications anywhere in this
        // X/AIT.
        // i.e. apps containing the addressable_application_descriptor()
        for (Enumeration e = allApplications.elements(); e.hasMoreElements();)
        {
            AppEntry app = (AppEntry) e.nextElement();
            if (app.addressLabels != null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("filterApps: " + app.id + " is addressable app");
                }
                addressableApps.add(app);
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("filterApps: " + app.id + " is non-addressable app");
                }
                nonAddressableApps.add(app);
            }
        }

        // OCAP-1.0 11.2.2.5.4 -- Backwards compatibility
        // - When no addressable_application_descriptor(s) are signaled in an
        // AIT or XAIT, any application_descriptor(s) that are present SHALL be
        // processed without consideration of application addressing.
        // - When addressing_descriptor(s) are present in an AIT or XAIT and
        // none of them evaluate to true, any application_descriptor(s) that
        // are present SHALL be processed without consideration of application
        // addressing
        if (addrGroups.isEmpty() || validLabels.isEmpty())
        {
            if (log.isDebugEnabled())
            {
                log.debug("filterApps: no addressable_application_descriptors.  only non-addressable apps are valid.");
            }
            newValidApps.addAll(nonAddressableApps);
        }
        else
        {
            // At this point, we know that we must apply addressing rules and
            // only allow applications that have at least one label from our
            // validLabels list.
            for (Enumeration e = addressableApps.elements(); e.hasMoreElements();)
            {
                AppEntry app = (AppEntry) e.nextElement();

                // Determine if this app has an address label from our matched
                // list
                for (int i = 0; i < app.addressLabels.length; ++i)
                {
                    // We have a match, so add to our new list and check to see
                    // if this does not appear in the previous list to determine
                    // if the list has changed
                    if (validLabels.contains(new Integer(app.addressLabels[i])))
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("filterApps: " + app.id + " contains a valid address label");
                        }
                        newValidApps.add(app);
                        break;
                    }
                }
            }

            // OCAP-1.0 11.2.2.5.4 -- Backwards compatibility
            // - When an addressing_descriptor is present in an AIT or XAIT and
            // evaluates to true and at least one addressable_application_descriptor
            // references it, the implementation SHALL NOT process any
            // application_descriptor that MAY be present in the same AIT or
            // XAIT
            if (newValidApps.isEmpty())
            {
                newValidApps.addAll(nonAddressableApps);
            }
        }

        // First time called?
        if (validApps == null)
        {
            validApps = newValidApps;
            return true;
        }

        // Check for changes from the previous list
        boolean listChanged = false;
        for (Enumeration e = newValidApps.elements(); e.hasMoreElements();)
        {
            AppEntry ae = (AppEntry) e.nextElement();
            if (!containsApp(validApps, ae.id))
            {
                listChanged = true;
                break;
            }
        }
        if (!listChanged)
        {
            for (Enumeration e = validApps.elements(); e.hasMoreElements();)
            {
                AppEntry ae = (AppEntry) e.nextElement();
                if (!containsApp(newValidApps, ae.id))
                {
                    listChanged = true;
                    break;
                }
            }
        }

        // Update our list
        validApps = newValidApps;
        
        // Updates the list of applications to remove AppID duplicates
        checkAppIDDuplicates();
        
        return listChanged;
    }
    
    protected void checkAppIDDuplicates()
    {
        // Sort by AppID, then priority
        Collections.sort(validApps, new AppEntry.AppIDPrioCompare());
        
        // First, remove any duplicate AppIDs with the same application version
        // based on application priority
        AppEntry[] copy = new AppEntry[validApps.size()];
        validApps.copyInto(copy);
        for (int i = 0; i < (copy.length-1); i++)
        {
            AppEntry test1 = copy[i];
            AppEntry test2 = copy[i+1];
            if (test1.id.equals(test2.id))
            {
                // Since we are sorted by priority (ascending), we can assume
                // that index i is lower or equal priority
                validApps.remove(test1);
                if (log.isWarnEnabled())
                {
                    log.warn("Removing duplicate AppID (same or lower priority) -- " + test1);
                }
            }
        }
    }

    /**
     * Determines if the given AppID is present in the given Vector of AppEntry
     * objects
     * 
     * @param apps the list of AppEntry objects to check
     * @param aID the AppID to check for
     * @return true if the given list contains an AppEntry with the given AppID,
     *         false otherwise
     */
    private boolean containsApp(Vector apps, AppID aID)
    {
        for (Iterator i = apps.iterator(); i.hasNext();)
        {
            AppEntry ae = (AppEntry) i.next();
            if (ae.id.equals(aID)) return true;
        }
        return false;
    }

    /**
     * Base expression class for all expressions found in addressing descriptors
     * 
     * @author Greg Rutz
     */
    public abstract class Expression
    {
        /**
         * Validate this expression
         * 
         * @return true if the expression is valid, false otherwise
         */
        public abstract boolean validate();
        
        /**
         * Construct a new expression with the given op-code
         * 
         * @param opCode the opcode
         */
        protected Expression(int opCode)
        {
            this.opCode = opCode;
        }

        public int getOpCode()
        {
            return opCode;
        }

        private int opCode;
    }

    /**
     * Represents the logical operations found in the <i>address_expression</i>
     * byte-field of addressing descriptors.
     */
    public class LogicalOp extends Expression
    {
        // Logical operation op-codes
        public static final int AND = 0x31;

        public static final int OR = 0x32;

        public static final int NOT = 0x33;

        public static final int TRUE = 0x34;

        public LogicalOp(int opCode)
        {
            super(opCode);
        }

        /**
         * Validate that this logical expression contains a valid op-code
         */
        public boolean validate()
        {
            return getOpCode() >= AND && getOpCode() <= TRUE;
        }
        
        public String toString()
        {
            switch (getOpCode())
            {
            case AND:  return "AND";
            case OR:   return "OR";
            case NOT:  return "NOT";
            case TRUE: return "TRUE";
            default:   return "INVALID_OPCODE";
            }
        }
    }

    /**
     * Represents the comparison operations found in the
     * <i>address_expression</i> byte-field of addressing descriptors. A
     * <code>Comparison</code> is capable of evaluating itself by querying the
     * host for its associated property values and comparing that value against
     * the test value provided in the <i>address_expression</i>.
     */
    public class Comparison extends Expression
    {
        // Comparison op-codes
        public static final int LT = 0x11;

        public static final int LTE = 0x12;

        public static final int EQ = 0x13;

        public static final int GTE = 0x14;

        public static final int GT = 0x15;

        // Attribute types
        private static final int ATTR_TYPE_LONG = 0x01; // Handles short, int,
                                                        // and long attribute
                                                        // types

        private static final int ATTR_TYPE_BOOL = 0x02;

        private static final int ATTR_TYPE_STRING = 0x03;

        public Comparison(int opCode, boolean isSecurity, int attributeID, String attributeValue)
        {
            super(opCode);
            this.attributeID = attributeID;
            this.isSecurityAttribute = isSecurity;
            this.attributeValue = attributeValue;
        }
        
        /**
         * Validates this <code>Comparison</code>. This validation cannot take
         * place during construction due to the fact that not all required AIT
         * information has been parsed at construction-time. Performs the
         * following operations:
         * 
         * <ol>
         * <li>Ensures that the attribute ID is present in an AITs attribute
         * map</li>
         * <li>Computes this comparison's attribute type</li>
         * </ol>
         * 
         * @return true if this <code>Comparison</code> is valid, false
         *         otherwise
         */
        public boolean validate()
        {
            // Validate our op-code
            if (getOpCode() < LT || getOpCode() > GT)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Expression has invalid opcode -- " + getOpCode());
                }
                return false;
            }

            // Retrieve the attribute name from the list of attribute mapping
            // descriptors. Validate that the attribute ID was found in the map
            attributeName = (String)attributeMap.get(new Integer(attributeID));
            if (attributeName == null)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Expression has invalid attribute ID -- " + attributeID);
                }
                return false;
            }
            
            // Determine the attribute type. For Java system properties, the
            // type is specified in OCAP-1.0 13.3.12.3. All other attributes
            // are assumed to be of String type
            if (attributeName.startsWith("ocap.cablecard.") ||
                attributeName.startsWith("ocap.hardware.v") ||
                attributeName.startsWith("ocap.memory.") ||
                attributeName.startsWith("mhp.eb.") ||
                attributeName.startsWith("mhp.ib.") ||
                attributeName.startsWith("mhp.ia.") ||
                attributeName.startsWith("dvb.returnchannel.timeout"))
            {
                attributeType = ATTR_TYPE_LONG;
            }
            else if (attributeName.equals("ocap.system.highdef"))
            {
                attributeType = ATTR_TYPE_BOOL;
            }
            else
            {
                attributeType = ATTR_TYPE_STRING;
            }

            return true;
        }

        /**
         * Evaluate this comparison and determine whether or not it is true.
         * Involves the querying of registered, security, and/or Java system
         * properties to determine the host's property value.
         * 
         * @param securityProps
         *            a set of recently-retrieved properties from the CableCARD
         *            that should be used to resolve all security attribute
         *            queries
         * @param registeredProps
         *            the set of properties registered by privileged
         *            applications
         * @return true if the two properties are of comparable types and the
         *         comparison is true, false otherwise
         */
        public boolean evaluate(Properties securityProps, Properties registeredProps)
        {
            // Retrieve the actual property value
            String hostValue;
            if (isSecurityAttribute)
                hostValue = securityProps.getProperty(attributeName);
            else
            {
                // If this is not a security value, first check registered
                // properties, then Java system properties
                hostValue = registeredProps.getProperty(attributeName);

                if (hostValue == null)
                {
                    // Java system property
                    hostValue = (String) AccessController.doPrivileged(new GetPropertyAction(attributeName));
                }
            }

            // Validate host value
            if (hostValue == null)
            {
                return false;
            }

            // Perform the comparison based on the attribute type. Only the "EQ"
            // comparison opcode is valid for String and Boolean types. Numeric
            // types can use all opcodes.
            switch (attributeType)
            {
                case ATTR_TYPE_LONG:
                {
                    try
                    {
                        // Perform comparison
                        long hostVal = Long.parseLong(hostValue);
                        long compVal = Long.parseLong(attributeValue);
                        switch (getOpCode())
                        {
                            case EQ:
                                return hostVal == compVal;
                            case LT:
                                return hostVal < compVal;
                            case LTE:
                                return hostVal <= compVal;
                            case GT:
                                return hostVal > compVal;
                            case GTE:
                                return hostVal >= compVal;
                        }
                    }
                    catch (NumberFormatException e)
                    {
                        // Attribute strings that cannot be parsed into a Long
                        // cause
                        // this comparison to return false
                        return false;
                    }
                    break;
                }

                case ATTR_TYPE_BOOL:
                    Boolean hostVal = Boolean.valueOf(hostValue);
                    Boolean compVal = Boolean.valueOf(attributeValue);
                    if (getOpCode() == EQ)
                    {
                        return hostVal.booleanValue() == compVal.booleanValue();
                    }
                    return false;

                case ATTR_TYPE_STRING:
                    if (getOpCode() == EQ)
                    {
                        return hostValue.equals(attributeValue);
                    }
                    return false;
            }

            return false;
        }

        public String toString()
        {
            String op;
            switch (getOpCode())
            {
            case LT:  op = "LT"; break;
            case LTE: op = "LTE"; break;
            case EQ:  op = "EQ"; break;
            case GTE: op = "GTE"; break;
            case GT:  op = "GT"; break;
            default:  op = "INVALID_OPCODE"; break;
            }
            return "attrID(" + attributeID + ((isSecurityAttribute) ? ",security) " : ") ") + op + " " + attributeValue;
        }

        private boolean isSecurityAttribute;
        private int attributeID;
        private String attributeName;
        private String attributeValue;
        private int attributeType;
    }

    /**
     * Class for holding data from the <i>addressing_descriptor</i> found in the
     * common loop of X/AIT. The containing X/AIT object maintains a
     * <code>TreeSet</code> of <code>AddressingDescriptor</code>s for each group
     * that are sorted in descending order by priority. Note that the
     * <code>AddressingDescComparator</code> class used to sort the
     * <code>TreeSet</code> does not provide the same <i>equals</i> semantics as
     * the <code>AddressingDescriptor</code>, which utilizes only the default
     * equals() method. This allows our <code>TreeSet</code> to behave like a
     * sorted multiset (allowing multiple objects with the same priority to be
     * in the set).
     * 
     * @see �OCAP-1.0: 11.2.2.5.1 Addressing Descriptor�
     */
    public class AddressingDescriptor
    {
        /**
         * Validate this addressing descriptor. Simply validates each
         * expression.
         * 
         * @return
         */
        public boolean validate()
        {
            // Validate all expressions.
            for (Iterator j = expressions.iterator(); j.hasNext();)
            {
                Expression expr = (Expression) j.next();
                if (!expr.validate()) return false;
            }

            // This descriptor is valid only if all expressions are valid
            return true;
        }

        /**
         * Evaluates this addressing descriptor to determine if its address
         * label is valid for this host. Involves retrieval of all property
         * values (security and non-security) and evaluating the expression that
         * was delivered in the <i>addressing_descriptor</i>.
         * 
         * @param securityProps
         *            a set of recently-retrieved properties from the CableCARD
         *            that should be used to resolve all security attribute
         *            queries
         * @param registeredProps
         *            the set of properties registered by privileged
         *            applications
         * @return true if this addressing descriptor�s address label is valid
         *         for this host
         */
        public boolean evaluate(Properties securityProps, Properties registeredProps)
        {
            // Evaluate -- examine each expression/logicalOp in order and
            // add result to the evaluation stack
            Stack evalStack = new Stack();
            for (Enumeration e = expressions.elements(); e.hasMoreElements();)
            {
                Expression expr = (Expression) e.nextElement();
                if (expr instanceof Comparison)
                {
                    // Evaluate the comparison and push its result onto the
                    // stack
                    Comparison comp = (Comparison) expr;
                    evalStack.push(new Boolean(comp.evaluate(securityProps, registeredProps)));
                    if (log.isDebugEnabled())
                    {
                        log.debug("Pushing addressing comparison [" + comp + "]: result is " + evalStack.peek());
                    }
                }
                else if (expr instanceof LogicalOp)
                {
                    Boolean op1, op2;
                    LogicalOp logical = (LogicalOp) expr;
                    switch (logical.getOpCode())
                    {
                        // AND -- Pop the top 2 results off the stack and push
                        // the logical AND result of those 2 ops
                        case LogicalOp.AND:
                            if (evalStack.size() < 2) // OCAP-1.0 11.2.2.5.1
                                return false;
                            op1 = (Boolean) evalStack.pop();
                            op2 = (Boolean) evalStack.pop();
                            if (log.isDebugEnabled())
                            {
                                log.debug("Logical Op (" + logical + ")" +
                                          " popping 2 operands from stack (" + op1 + "," + op2 + ")");
                            }
                            evalStack.push(new Boolean(op1.booleanValue() && op2.booleanValue()));
                            break;

                        // OR -- Pop the top 2 results off the stack and push
                        // the logical OR result of those 2 ops
                        case LogicalOp.OR:
                            if (evalStack.size() < 2) // OCAP-1.0 11.2.2.5.1
                                return false;
                            op1 = (Boolean) evalStack.pop();
                            op2 = (Boolean) evalStack.pop();
                            if (log.isDebugEnabled())
                            {
                                log.debug("Logical Op (" + logical + ")" +
                                          " popping 2 operands from stack (" + op1 + "," + op2 + ")");
                            }
                            evalStack.push(new Boolean(op1.booleanValue() || op2.booleanValue()));
                            break;

                        // NOT -- Pop the top result off the stack and push the
                        // logical negation of that result
                        case LogicalOp.NOT:
                            if (evalStack.size() < 1) // OCAP-1.0 11.2.2.5.1
                                return false;
                            op1 = (Boolean) evalStack.pop();
                            if (log.isDebugEnabled())
                            {
                                log.debug("Logical Op (" + logical + ")" +
                                          " popping 1 operand from stack (" + op1 + ")");
                            }
                            evalStack.push(new Boolean(!op1.booleanValue()));
                            break;

                        // TRUE -- Push a TRUE result onto the stack
                        case LogicalOp.TRUE:
                            if (log.isDebugEnabled())
                            {
                                log.debug("Logical Op (" + logical + ")" + " pushing operand");
                            }
                            evalStack.push(new Boolean(true));
                            break;

                        default:
                            return false;
                    }
                    if (log.isDebugEnabled())
                    {
                        log.debug("Addressing operation result is " + evalStack.peek());
                    }
                }
            }

            // Ensure that we only have a single result left on the stack and
            // return it
            if (evalStack.size() != 1) return false;

            return ((Boolean) evalStack.pop()).booleanValue();
        }
        
        public String toString()
        {
            StringBuffer sb = new StringBuffer();
            sb.append("AddressingDescriptor - priority = " + priority +
                      ", label = " + addressLabel + "\n");
            for (int i = 0; i < expressions.size(); i++)
            {
                sb.append("\t" + expressions.elementAt(i).toString());
            }
            return sb.toString();
        }

        public int priority;
        public int addressLabel;
        public Set securityAttributeIDs;

        /* Contains combination of LogicalOp and Comparison objects */
        public Vector expressions = new Vector();
    }
    
    /**
     * This is a utility method used by the parsers to add an addressing descriptor
     * to hashtable of addressing groups.  Each slot in the hashtable (keyed by GroupID)
     * contains a <code>Set</code> of <code>AddressingDescriptor</code>s sorted by
     * priority
     * 
     * @param adGroups the table of addressing groups to modify
     * @param groupID the group ID associated with the addressing descriptor
     * @param ad the addressing descriptor to add
     */
    public static void addAddressingDescriptor(Hashtable adGroups, int groupID, AddressingDescriptor ad)
    {
        Integer group = new Integer(groupID);

        // Find the set of addressing descriptors for this group ID
        TreeSet addrs = (TreeSet)adGroups.get(group);
        
        // No descriptors for this group yet, so create the set
        if (addrs == null)
        {
            /**
             * The comparator is used by the TreeSet in our addrGroups table to keep
             * each set of addressing descriptors in a group sorted by priority.
             * Standard comparison operators are reversed so that the descriptors will
             * be sorted in <i>descending</i> order. This allows us to iterate over
             * addressing descriptors from highest-to-lowest priority.
             */
            addrs = new TreeSet(new Comparator() {
                public int compare(Object o1, Object o2)
                {
                    AddressingDescriptor ad1 = (AddressingDescriptor) o1;
                    AddressingDescriptor ad2 = (AddressingDescriptor) o2;
        
                    // Direction intentionally reversed to sort from highest
                    // to lowest priority
                    if (ad1.priority > ad2.priority) return -1;
                    if (ad1.priority < ad2.priority) return 1;
                    return 0;
                }
            });
            
            // Add this group to the table of groups
            adGroups.put(group, addrs);
        }

        // Add our most recently parsed addressing descriptor to the set
        addrs.add(ad);
    }
    
    // Description copied from Ait
    public int getVersion()
    {
        return version;
    }
    
    // Description copied from Ait
    public ExternalAuthorization[] getExternalAuthorization()
    {
        ExternalAuthorization[] auth = new ExternalAuthorization[externalAuth.size()];
        externalAuth.copyInto(auth);
        return auth;
    }
    
    /**
     * The table version
     */
    private int version;

    /** The set of external authorizations. */
    private Vector externalAuth = new Vector();

    /**
     * Contains list of all applications (AppEntry) found in the table
     * regardless of whether or not addressing properties indicate them as valid
     * for this host
     */
    private Vector allApplications = new Vector();

    /**
     * Contains the list of apps that are valid for this host.  This list has
     * been filtered using addressable attributes and duplicate AppID checks
     * 
     * @return a list of apps that are valid for this host as determined by
     *         addressing rules and tests for remoing duplicate AppIDs 
     *         as a result of a call to filterApps().
     */
    protected Vector validApps = new Vector();

    /**
     * Key=Integer, Value=String. All attribute mappings found in the common
     * loop of this table.
     * 
     * @see �OCAP-1.0: 11.2.2.5.2 Attribute Mapping Descriptor�
     */
    private Hashtable attributeMap = new Hashtable();

    /**
     * Key=Integer, Value=TreeSet&lt;AddrDesc&gt;. All parsed addressing groups
     * from the common loop of this table, keyed by Group ID. The TreeSet
     * maintains a list of all addressing descriptors associated with the group
     * sorted by addressing descriptor priority.
     */
    private Hashtable addrGroups = new Hashtable();

    /**
     * The set of security properties names referenced by all addressing
     * descriptors within this X/AIT
     */
    private Vector securityProperties = new Vector();
    
    private static final Logger log = Logger.getLogger(AitImpl.class.getName());
}
