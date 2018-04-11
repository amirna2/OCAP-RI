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

package org.ocap.application;

import java.security.BasicPermission;
import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;

// OCAP 1.1 only:
import javax.microedition.xlet.ixc.IxcPermission;

import org.apache.log4j.Logger;
import org.dvb.application.AppID;
import org.dvb.io.ixc.IxcRegistry;


/**
 * This class represents access to the inter-xlet communication registry. An
 * OcapIxcPermission consists of a name specification and a action specifying
 * what can be done with those names.
 * <p>
 * The name specification is a superset of the name passed into the
 * {@link IxcRegistry} methods such as {@link IxcRegistry#bind} and
 * {@link IxcRegistry#lookup}. Valid names are composed of fields delimited by
 * "/" characters, with each field specifying a particular value (e.g., OID).
 * The following grammar defines the name format:
 * 
 * <pre>
 * <i>NAME</i>      = "*" | "/" <i>SCOPE</i> "/" <i>SIGNED</i> "/" <i>OID</i> "/" <i>AID</i> "/" <i>BINDNAME</i>
 * <i>SCOPE</i>     = "*" | "global" | "ixc" | "service-" <i>CONTEXT</i>
 * <i>CONTEXT</i>   = "*" | <i>context-id</i>
 * <i>SIGNED</i>    = "*" | "signed" | "unsigned"
 * <i>OID</i>       = "*" | <i>oid</i>
 * <i>AID</i>       = "*" | <i>aid</i>
 * <i>BINDNAME</i>  = "*" | <i>bindname</i> | <i>bindname</i> "*"
 * </pre>
 * 
 * Where <code>"*"</code> specifies a wildcard character. Where
 * <i>context-id</i> is a platform-specific unique identifier for a service
 * context; <i>oid</i> and <i>aid</i> are the {@link AppID#getOID()
 * organization} and {@link AppID#getAID() application} identifiers of the
 * binding application as converted by {@link Integer#toHexString}; and
 * <i>bindname</i> is the application-defined name given at bind-time.
 * <p>
 * <ul>
 * <li>"*" as the entire name string will match any other name
 * <li>"/&#42;/&#42;/&#42;/&#42;/* is equivalent to "*"
 * <li>"/&#42;/&#42;/1a/4abc/*" will match names in any scope, published by an
 * application with an OID of <code>1a</code> and AID of <code>4abc</code>.
 * <li>"/&#42;/signed/&#42;/VODApi" will match any object bound by a signed
 * application with an ixcname of "VODApi".
 * </ul>
 * 
 * The actions specification is comprised of a single action specified by one of
 * two keywords: "bind" or "lookup". These correspond to the <code>bind</code>
 * and <code>lookup</code> methods of <code>IxcRegistry</code>. The actions
 * string is converted to lowercase before processing.
 * <p>
 * 
 * @author Aaron Kamienski
 */
public final class OcapIxcPermission extends BasicPermission
{
    
    /**
     * Determines if a de-serialized file is compatible with this class.
     *
     * Maintainers must change this value if and only if the new version
     * of this class is not compatible with old versions.See spec available
     * at http://docs.oracle.com/javase/1.4.2/docs/guide/serialization/ for
     * details
     */
    private static final long serialVersionUID = -8705432476008476392L;

    private static final String DVB_PREFIX = "dvb:/";

    private static final Logger log = Logger.getLogger(OcapIxcPermission.class);

    private static final int BASE_16_RADIX = 16;

    /**
     * Creates a new OcapIxcPermission object with the specified name and
     * actions. The name specification is a superset of the name passed into the
     * {@link IxcRegistry} methods such as {@link IxcRegistry#bind} and
     * {@link IxcRegistry#lookup}. See the {@link OcapIxcPermission class
     * description} for the specification of the name string.
     * <p>
     * The actions specification is comprised of a single action specified by
     * one of two keywords: "bind" or "lookup". These correspond to the
     * <code>bind</code> and <code>lookup</code> methods of
     * <code>IxcRegistry</code>. The actions string is converted to lowercase
     * before processing.
     * 
     * @param name
     *            The name specification for exported/imported objects
     * @param actions
     *            The action string
     */
    public OcapIxcPermission(String name, String actions)
    {
        super(name, actions);
        // superclass does not store the action, store it here
        // Action must be either "bind" or "lookup"
        action = actions.toLowerCase();
        if (!action.equals("bind") && !action.equals("lookup"))
        {
            throw new IllegalArgumentException("OcapIxcPermission -- invalid action: " + action);
        }

        // First, handle the case where the name is just equal to "*"
        if (name.equals(WILDCARD))
        {
            scope = WILDCARD;
            signed = WILDCARD;
            orgID = WILDCARD;
            appID = WILDCARD;
            bindName = WILDCARD;
            bindnameHasWildcard = true;
        }
        else
        {
            //
            // Parse the name string
            //

            // Check for leading "/"
            if (name.charAt(0) != SEPARATOR)
            {
                throw new IllegalArgumentException("OcapIxcPermission -- name must begin with \"/\"");
            }

            StringTokenizer tokens = new StringTokenizer(name, SEPARATOR_STRING, false);

            try
            {
                // //////////////////////////////////////////////
                // Scope description.
                scope = tokens.nextToken().toLowerCase();

                // Check for valid forms of scope description
                if (scope.equals(WILDCARD))
                {
                    // allow wildcard
                }
                else if (scope.equals(SCOPE_SERVICE_WILDCARD))
                {
                    scope = SCOPE_OCAP_SERVICE;
                    serviceHasWildcard = true;
                }
                else if (!scope.startsWith(SCOPE_OCAP_SERVICE) && !scope.equals(SCOPE_GLOBAL)
                        && !scope.equals(SCOPE_IXC))
                {
                    throw new IllegalArgumentException("OcapIxcPermission -- invalid service description: " + scope);
                }

                // //////////////////////////////////////////////
                // Signed or unsigned
                signed = tokens.nextToken().toLowerCase();

                // Must be either "*" or "signed" or "unsigned"
                if (!signed.equals(WILDCARD) && !signed.equals(SIGNED) && !signed.equals(UNSIGNED))
                {
                    throw new IllegalArgumentException("OcapIxcPermission -- invalid signed/unsigned description: "
                            + signed);
                }

                // //////////////////////////////////////////////
                // Org ID
                orgID = tokens.nextToken().toLowerCase();

                // Must be a valid orgID
                if (!orgID.equals(WILDCARD))
                {
                    try
                    {
                        Long.parseLong(orgID, BASE_16_RADIX);
                    }
                    catch (NumberFormatException e)
                    {
                        throw new IllegalArgumentException("OcapIxcPermission -- invalid orgID: " + orgID);
                    }
                }

                // //////////////////////////////////////////////
                // App ID
                appID = tokens.nextToken().toLowerCase();

                // Must be a valid appID
                if (!appID.equals(WILDCARD))
                {
                    try
                    {
                        Integer.parseInt(appID, BASE_16_RADIX);
                    }
                    catch (NumberFormatException e)
                    {
                        throw new IllegalArgumentException("OcapIxcPermission -- invalid appID: " + appID);
                    }
                    appID = appID.toLowerCase();
                }

                // //////////////////////////////////////////////
                // Bind Name -- ensure that wildcard is only at the end
                bindName = tokens.nextToken();
                if (bindName.indexOf(WILDCARD) != -1)
                {
                    if (bindName.endsWith(WILDCARD))
                    {
                        bindName = bindName.substring(0, bindName.length() - 1);
                        bindnameHasWildcard = true;
                    }
                    else
                    {
                        throw new IllegalArgumentException(
                                "OcapIxcPermission -- bind name wildcard can only appear at the end:" + bindName);
                    }
                }
            }
            catch (NoSuchElementException e)
            {
                throw new IllegalArgumentException("OcapIxcPermission -- invalid name: " + name);
            }
        }
    }

    /**
     * Create an OcapIxcPermission equivalent to the given IxcPermission
     * 
     * @param ixcPerm
     *            the IxcPermission
     */
    private OcapIxcPermission(IxcPermission ixcPerm)
    {
        super(ixcPerm.getName(), ixcPerm.getActions());
        // superclass does not store the action, store it here
        // Action must be either "bind" or "lookup"
        action = ixcPerm.getActions().toLowerCase();
        if (!action.equals("bind") && !action.equals("lookup"))
        {
            throw new IllegalArgumentException("OcapIxcPermission -- invalid action: " + action);
        }

        if (!ixcPerm.getName().equals(WILDCARD) && !(ixcPerm.getName().startsWith(DVB_PREFIX)))
        {
            throw new IllegalArgumentException("IxcPermission must be * or start with " + DVB_PREFIX);
        }

        if (log.isDebugEnabled())
        {
            log.debug("constructing new ocapixcpermission from ixcpermission: " + getName() + ".." + getActions());
        }

        // By default initialize all fields to wildcard. If we run out of tokens
        // in the IxcPermission name, remaining fields are implicitly wildcards
        scope = WILDCARD;
        signed = WILDCARD;
        orgID = WILDCARD;
        appID = WILDCARD;
        bindName = WILDCARD;
        bindnameHasWildcard = true;

        if (ixcPerm.getName().startsWith(DVB_PREFIX))
        {
            StringTokenizer tokens = new StringTokenizer(ixcPerm.getName().substring(DVB_PREFIX.length()),
                    SEPARATOR_STRING, false);

            try
            {
                // Validate the scope
                scope = tokens.nextToken();
                if (scope.equals(WILDCARD))
                {
                    // allow wildcard
                }
                else if (scope.equals(SCOPE_J2ME_SERVICE))
                {
                    // Add service ID
                    scope = scope + "-" + tokens.nextToken();

                    // Signed
                    signed = tokens.nextToken();
                    if (signed.equals(WILDCARD))
                    {
                        // allow wildcard
                    }
                    else if (!signed.equals(SIGNED) && !signed.equals(UNSIGNED))
                    {
                        throw new IllegalArgumentException("invalid signed field: " + signed);
                    }
                }
                else if (scope.equals(SIGNED) || scope.equals(UNSIGNED))
                {
                    // Add GLOBAL scope and save signed state
                    signed = scope;
                    scope = SCOPE_GLOBAL;
                }
                else if (!scope.equals(SCOPE_IXC))
                {
                    throw new IllegalArgumentException("OcapIxcPermission -- invalid service description: " + scope);
                }

                // Must be a valid orgID
                orgID = tokens.nextToken();
                if (!orgID.equals(WILDCARD))
                {
                    try
                    {
                        Integer.parseInt(orgID, BASE_16_RADIX);
                    }
                    catch (NumberFormatException e)
                    {
                        throw new IllegalArgumentException();
                    }
                }

                // Must be a valid appID
                appID = tokens.nextToken();
                if (!appID.equals(WILDCARD))
                {
                    try
                    {
                        Integer.parseInt(appID, BASE_16_RADIX);
                    }
                    catch (NumberFormatException e)
                    {
                        throw new IllegalArgumentException();
                    }
                }

                // Bindname
                bindName = tokens.nextToken();
                if (bindName.endsWith(WILDCARD))
                {
                    bindName = bindName.substring(0, bindName.length() - 1);
                }
                else
                {
                    bindnameHasWildcard = false;
                }
            }
            catch (NoSuchElementException e)
            {
                // ignore
            }
            // Validate IxcPermission SERVICE scope. Ensure that a service ID
            // was specified
            if (scope.equals(SCOPE_J2ME_SERVICE))
            {
                throw new IllegalArgumentException("Invalid service description - not ID provided");
            }
            if (log.isDebugEnabled())
            {
                log.debug("constructed OcapIxcPermission for: " + getName() + " with action: " + action + "..scope:"
                        + scope + "..signed:" + signed + "..orgID:" + orgID + "..appID:" + appID + "..bindName:"
                        + bindName);
            }
        }
    }

    private boolean isFullNameWildCardEquivalent(String name)
    {
        if (WILDCARD.equals(name))
        {
            return true;
        }
        Enumeration tokens = new StringTokenizer(name, SEPARATOR_STRING, false);
        while (tokens.hasMoreElements())
        {
            String thisToken = tokens.nextElement().toString();
            if (!WILDCARD.equals(thisToken))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks two OcapIxcPermission objects for equality. Check that other is an
     * OcapIxcPermission, and has the same name and actions as this object.
     * 
     * @param obj
     *            the object we are testing for equality with this object
     * @return true if obj is an OcapIxcPermission, and has the same name and
     *         actions as this OcapIxcPermission object.
     */
    public boolean equals(Object obj)
    {
        if (obj != null && obj instanceof OcapIxcPermission)
        {
            OcapIxcPermission other = (OcapIxcPermission) obj;

            if (isFullNameWildCardEquivalent(getName()) && isFullNameWildCardEquivalent(other.getName()))
            {
                return action.equals(other.action);
            }
            return super.equals(obj) && action.equals(other.action);
        }

        return false;
    }

    /**
     * Returns the "canonical string representation" of the actions. That is,
     * this method always returns present actions in the following order: bind,
     * lookup. For example, if this OcapIxcPermission object allows both bind
     * and lookup actions, a call to getActions will return the string
     * "bind,lookup".
     * 
     * @return the canonical string representation of the actions
     */
    public String getActions()
    {
        return action;
    }

    /**
     * Returns the hash code value for this object.
     * 
     * @return a hash code value for this object.
     */
    public int hashCode()
    {
        return super.hashCode() ^ action.hashCode();
    }

    /**
     * Checks if this OcapIxcPermission "implies" the specified permission.
     * <p>
     * More specifically, this method returns true if:
     * <ul>
     * <li>p is an instanceof OcapIxcPermission
     * <li>p's actions are a proper subset of this object's actions, and
     * <li>p's name is implied by this object's name.
     * </ul>
     * <p>
     * The rules for determining if this object's name implies p's name are as
     * follows:
     * <ul>
     * <li>Where p's name is exactly the same as this object's name, then it is
     * implied.
     * <li>The name <code>"*"</code> and
     * <code>"/&#42;/&#42;/&#42;/&#42;/*"</code> both imply all possible names.
     * <li>Where this object's name includes a wildcard for a field (
     * <code>"*"</code>), then all possible values for that field are implied.
     * <li>Where this object's name includes a field that ends in a wildcard
     * (e.g., <code>service-*</code>) then all possible values for that field
     * starting with the non-wildcard portion are implied.
     * </ul>
     * <p>
     * For example, <code>"/service-&#42;/signed/abc/4001/*"</code> implies
     * <code>"/service-1234/signed/abc/4001/VODObject"</code>.
     * 
     * <p>
     * An <code>OcapIxcPermission</code> may also imply an {@link IxcPermission}
     * . That is, this method will also return true if:
     * <ul>
     * <li>p is an instanceof IxcPermission
     * <li>p's actions are a proper subset of this object's actions, and
     * <li>p's name is implied by this object's name.
     * </ul>
     * <p>
     * The rules for determining if this object's name implies an
     * <code>IxcPermission</code> name are the same as detailed above except
     * that a translation of the <code>IxcPermission</code> name to the
     * <code>OcapIxcPermission</code> is applied first. The following table
     * shows how such a mapping SHALL be applied:
     * <p>
     * <table border>
     * <tr>
     * <th>IxcPermission name</th>
     * <th>OcapIxcPermission name</th>
     * </tr>
     * <tr>
     * <td>"*"</td>
     * <td>"*"</td>
     * </tr>
     * <tr>
     * <td>"dvb:/*"</td>
     * <td>"*"</td>
     * </tr>
     * <tr>
     * <td>"dvb:/signed/*"</td>
     * <td>"/global/signed/&#42;/&#42;/*</td>
     * </tr>
     * <tr>
     * <td>"dvb:/service/<i>id</i>/signed/*"</td>
     * <td>"/service-<i>id</i>/signed/&#42;/&#42;/*</td>
     * </tr>
     * <tr>
     * <td>"dvb:/ixc/*"</td>
     * <td>"/ixc/&#42;/&#42;/&#42;/*</td>
     * </tr>
     * <tr>
     * <td>"dvb:/signed/<i>OID</i>/*"</td>
     * <td>"/global/signed/<i>OID</i>/&#42;/*</td>
     * </tr>
     * <tr>
     * <td>"dvb:/service/<i>id</i>/signed/<i>OID</i>/*"</td>
     * <td>"/service-<i>id</i>/signed/<i>OID</i>/&#42;/*</td>
     * </tr>
     * <tr>
     * <td>"dvb:/ixc/<i>OID</i>/<i>AID</i>/*"</td>
     * <td>"/ixc/&#42;/<i>OID</i>/&#42;/*</td>
     * </tr>
     * <tr>
     * <td>"dvb:/signed/<i>OID</i>/<i>AID</i>/*"</td>
     * <td>"/global/signed/<i>OID</i>/<i>AID</i>/*</td>
     * </tr>
     * <tr>
     * <td>"dvb:/service/<i>id</i>/signed/<i>OID</i>/<i>AID</i>/*"</td>
     * <td>"/service-<i>id</i>/signed/<i>OID</i>/<i>AID</i>/*</td>
     * </tr>
     * <tr>
     * <td>"dvb:/ixc/<i>OID</i>/<i>AID</i>/*"</td>
     * <td>"/ixc/&#42;/<i>OID</i>/<i>AID</i>/*</td>
     * </tr>
     * <tr>
     * <td>"dvb:/signed/<i>OID</i>/<i>AID</i>/<i>name</i>*"</td>
     * <td>"/global/signed/<i>OID</i>/<i>AID</i>/<i>name</i>*</td>
     * </tr>
     * <tr>
     * <td>"dvb:/service/<i>id</i>/signed/<i>OID</i>/<i>AID</i>/<i>name</i>*"</td>
     * <td>"/service-<i>id</i>/signed/<i>OID</i>/<i>AID</i>/<i>name</i>*</td>
     * </tr>
     * <tr>
     * <td>"dvb:/ixc/<i>OID</i>/<i>AID</i>/<i>name</i>*"</td>
     * <td>"/ixc/&#42;/<i>OID</i>/<i>AID</i>/<i>name</i>*</td>
     * </tr>
     * </table>
     * <p>
     * Any <code>IxcPermission</code> name that cannot be mapped cannot be
     * implied.
     * 
     * @param p
     *            the permission to check against
     * @return true if the specified permission is implied by this object, false
     *         if not.
     */
    public boolean implies(Permission p)
    {
        if (this == p)
        {
            return true;
        }

        if (p == null)
        {
            return false;
        }

        // If p is IxcPermission, create new OcapIxcPermission based on p
        if (p.getClass() == IxcPermission.class)
        {
            try
            {
                return implies(new OcapIxcPermission((IxcPermission) p));
            }
            catch (IllegalArgumentException e)
            {
                if (log.isInfoEnabled())
                {
                    log.info("unable to construct OcapIxcPermission from: " + p, e);
                }
                return false;
            }
        }

        // If p is not OcapIxcPermission, this does not imply p
        if (p.getClass() != OcapIxcPermission.class)
        {
            return false;
        }

        OcapIxcPermission other = (OcapIxcPermission) p;
        // Actions must match (wildcards not supported)
        if (!action.equals(other.action))
        {
            return false;
        }

        // These parts of the permission name can only be the wildcard
        // character ('*') or a complete value
        if (!isSimpleGeneralization(orgID, other.orgID))
        {
            return false;
        }

        if (!isSimpleGeneralization(appID, other.appID))
        {
            return false;
        }

        if (!isSimpleGeneralization(signed, other.signed))
        {
            return false;
        }

        // Check scope
        if (scope.equals(WILDCARD))
        {
            // allow wildcard
        }
        else if (scope.startsWith(SCOPE_OCAP_SERVICE))
        {
            if (!isComplexGeneralization(scope, serviceHasWildcard, other.scope, other.serviceHasWildcard))
            {
                return false;
            }
        }
        else if (scope.equals(SCOPE_GLOBAL))
        {
            if (!other.scope.equals(SCOPE_GLOBAL))
            {
                return false;
            }
        }
        else if (scope.equals(SCOPE_IXC))
        {
            if (!other.scope.equals(SCOPE_IXC))
            {
                return false;
            }
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("unsupported scope: " + scope);
            }
            return false;
        }

        boolean bindNameAcceptable;
        if (bindName.equals(WILDCARD))
        {
            // allow wildcard
            bindNameAcceptable = true;
        }
        else
        {
            bindNameAcceptable = isComplexGeneralization(bindName, bindnameHasWildcard, other.bindName,
                    other.bindnameHasWildcard);
        }

        return bindNameAcceptable;
    }

    /**
     * Returns a new PermissionCollection object for storing IxcPermission
     * objects.
     * <p>
     * IxcPermission objects must be stored in a manner that allows them to be
     * inserted into the collection in any order, but that also enables the
     * PermissionCollection implies method to be implemented in an efficient
     * (and consistent) manner.
     * 
     * @return a new PermissionCollection object suitable for storing
     *         IxcPermissions.
     */
    public PermissionCollection newPermissionCollection()
    {
        return new OcapIxcPermissionCollection();
    }

    /**
     * This method compares 2 strings and determines if the first string (s1) is
     * a "simple" generalization of the second string (s2). If the first string
     * is the wildcard character ("*"), it is considered a generalization of the
     * second string. The first string is also considered a generalization of
     * the second if both strings are identical
     * 
     * @param s1
     *            the first string
     * @param s2
     *            the second string
     * @return true if the first string is a "simple" generalization of the
     *         second string; false otherwise
     */
    private boolean isSimpleGeneralization(String s1, String s2)
    {
        if (s1.equals(WILDCARD) || s1.equals(s2))
        {
            return true;
        }

        return false;
    }

    /**
     * This method compares 2 strings and determines if the first string (s1) is
     * a "complex" generalization of the second string (s2). If the first string
     * (of length N) originally ended with the wildcard character, it is
     * considered a generalization of the second string if and only if the
     * characters of the second string up to index N-1 are identical to the
     * first string. The first string is also considered a generalization of the
     * second if both strings are identical. Finally, if the first string does
     * not end with a wildcard, but the second string does, then the first is
     * not a generalization of the second.
     * 
     * @param s1
     *            the first string with ending wildcard removed
     * @param s1Wildcard
     *            true if s1 originally ended with a wildcard
     * @param s2
     *            the second string
     * @param s2Wildcard
     *            true if s2 originally ended with a wildcard
     * @return true if the first string is a "complex" generalization of the
     *         second string; false otherwise
     */
    private boolean isComplexGeneralization(String s1, boolean s1Wildcard, String s2, boolean s2Wildcard)
    {
        if (s2Wildcard && !s1Wildcard)
        {
            return false;
        }

        if (s1Wildcard && s2.startsWith(s1))
        {
            return true;
        }
        else if (s1.equals(s2))
        {
            return true;
        }

        return false;
    }

    private static final char SEPARATOR = '/';

    private static final String SEPARATOR_STRING = "/";

    private static final String WILDCARD = "*";

    private static final String SIGNED = "signed";

    private static final String UNSIGNED = "unsigned";

    private static final String SCOPE_J2ME_SERVICE = "service";

    private static final String SCOPE_OCAP_SERVICE = "service-";

    private static final String SCOPE_SERVICE_WILDCARD = "service-*";

    private static final String SCOPE_GLOBAL = "global";

    private static final String SCOPE_IXC = "ixc";

    // These members represent each individual field of the
    // OcapIxcPermission name.
    private String scope = null;

    private String signed = null;

    private String appID = null;

    private String orgID = null;

    private String bindName = null;

    // For service and bindname, we will keep track of whether or not they
    // originally contained a wildcard
    boolean serviceHasWildcard = false;

    boolean bindnameHasWildcard = false;

    // Permission action
    private String action = null;

    private class OcapIxcPermissionCollection extends PermissionCollection
    {
        /**
         * Determines if a de-serialized file is compatible with this class.
         *
         * Maintainers must change this value if and only if the new version
         * of this class is not compatible with old versions.See spec available
         * at http://docs.oracle.com/javase/1.4.2/docs/guide/serialization/ for
         * details
         */
        private static final long serialVersionUID = -7846535764072219652L;

        // Smartly add a new permission to this collection. If the permission
        // to be added is simply a more general version of a permission already
        // in the collection, the original permission will be overwritten by the
        // new one
        public void add(Permission perm)
        {
            boolean needToAdd = true;

            for (Iterator iter = permissions.iterator(); iter.hasNext();)
            {
                OcapIxcPermission nextPerm = (OcapIxcPermission) iter.next();

                // The new perm is already covered by a perm in the collection
                if (nextPerm.implies(perm))
                {
                    needToAdd = false;
                    break;
                }
                // The new perm covers a perm already in the collection, so
                // remove
                // this one
                else if (perm.implies(nextPerm))
                {
                    iter.remove();
                }
            }

            if (needToAdd)
            {
                permissions.addElement(perm);
            }
        }

        public Enumeration elements()
        {
            return permissions.elements();
        }

        public boolean implies(Permission perm)
        {
            // only allow valid OcapIxcPermission objects
            if (perm == null || perm.getClass() != OcapIxcPermission.class)
            {
                return false;
            }

            // walk the collection and see if there is a permssion that implies
            // the provided permission
            Enumeration e = permissions.elements();
            while (e.hasMoreElements())
            {
                OcapIxcPermission nextPerm = (OcapIxcPermission) e.nextElement();
                if (nextPerm.implies(perm))
                {
                    return true;
                }
            }

            return false;
        }

        private Vector permissions = new Vector();
    }
}
