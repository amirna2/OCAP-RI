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

import java.util.Date;
import java.util.StringTokenizer;
import java.util.NoSuchElementException;
import org.dvb.application.AppID;

/**
 * AppPattern is an element that constitutes an {@link AppFilter}. An AppPattern
 * has the following attributes:
 * 
 * <ul>
 * 
 * <li><em>idPattern</em> - a group of AppIDs.
 * 
 * <li><em>action</em> - an action (ALLOW, DENY, or ASK) for matching
 * applications.
 * 
 * <li><em>priority</em> - a priority that determines the search order position
 * in an AppFilter. The highest priority is 255, the lowest is 0.
 * 
 * <li><em>expirationTime</em> - An expiration time. Optional.
 * 
 * <li><em>info</em> - an MSO-private data. Optional. Could be a String.
 * {@link AppFilterHandler} may use it for making a decision.
 * 
 * </ul>
 * 
 * <p>
 * <code>idPattern</code> specifies an AppID group with a String: a pair of
 * ranges for Organization IDs and Application IDs. The syntax is:
 * 
 * <blockquote><code>"oid1[-oid2][:aid1[-aid2]]"</code></blockquote>
 * 
 * <ul>
 * 
 * <li><code>oid1</code> and <code>oid2</code> specify a range of Organization
 * IDs inclusive. Each of them must be a 32-bit value.
 * 
 * <li><code>aid1</code> and <code>aid2</code> specify a range of Application
 * IDs inclusive. Each of them must be a 16-bit value.
 * 
 * <li><code>oid2</code> and <code>aid2</code> must be greater than
 * <code>oid1</code> and <code>aid1</code>, respectively.
 * 
 * <li>The encoding of these IDs follows <em>14.5 Text encoding of
 * application identifiers</em> of <em>DVB-MHP 1.0.2 [11]</em>; hexadecimal,
 * lower case, no leading zeros.
 * 
 * <li>Symbols in brackets are optional.
 * 
 * <li>When <code>oid2</code> is omitted, only <code>oid1</code> is in the
 * range.
 * 
 * <li>When <code>aid2</code> is omitted, only <code>aid1</code> is in the
 * range.
 * 
 * <li>When both <code>aid1</code> and <code>aid2</code> are omitted, all
 * Application IDs are in the range.
 * 
 * </ul>
 * 
 * <p>
 * See {@link AppFilter} for the examples.
 * 
 * @see AppFilter
 * @see AppFilterHandler
 */
public class AppPattern
{

    /**
     * When <code>AppFilter.accept</code> finds a matching
     * <code>AppPattern</code> with this action, it returns <code>true</code>.
     * 
     * @see AppFilter#accept
     */
    public static final int ALLOW = 1;

    /**
     * When <code>AppFilter.accept</code> finds a matching
     * <code>AppPattern</code> with this action, it returns <code>false</code>.
     * 
     * @see AppFilter#accept
     */
    public static final int DENY = 2;

    /**
     * When <code>AppFilter.accept</code> finds a matching
     * <code>AppPattern</code> with this action, it asks
     * <code>AppFilterHandler.accept</code> for the decision.
     * 
     * @see AppFilter#accept
     * @see AppFilterHandler#accept
     */
    public static final int ASK = 3;

    /**
     * Returned by {@link #match} when no match is made.
     * 
     * Package private constant used by <code>AppFilter</code> implementation.
     */
    static final int NO_MATCH = 0;

    /**
     * Returned by {@link #match} when no match is made because the pattern
     * expired.
     * 
     * Package private constant used by <code>AppFilter</code> implementation.
     */
    static final int NO_MATCH_EXPIRED = -1;

    /**
     * Constructs a new AppPattern with no expiration.
     * 
     * @param idPattern
     *            a String to specify an AppID group.
     * 
     * @param action
     *            an action.
     * 
     * @param priority
     *            a search order priority.
     * 
     * @throws IllegalArgumentException
     *             <code>idPattern</code> has a bad format, <code>action</code>
     *             or <code>priority</code> is out of range.
     */
    public AppPattern(String idPattern, int action, int priority)
    {
        this(idPattern, action, priority, null, null);
    }

    /**
     * Constructs a new AppPattern with an expiration time and MSO private
     * information.
     * 
     * @param idPattern
     *            a String to specify an AppID group.
     * 
     * @param action
     *            an action.
     * 
     * @param priority
     *            a search order priority.
     * 
     * @param expirationTime
     *            time for this AppPattern to expire. <code>null</code> it never
     *            expires.
     * 
     * @param info
     *            MSO specific information. Can be <code>null</code>.
     * 
     * @throws IllegalArgumentException
     *             <code>idPattern</code> has a bad format, <code>action</code>
     *             or <code>priority</code> is out of range.
     */
    public AppPattern(String idPattern, int action, int priority, Date expirationTime, Object info)
    {
        // Check action
        switch (action)
        {
            default:
                throw new IllegalArgumentException("invalid action");
            case ALLOW:
            case DENY:
            case ASK:
                this.action = action;
                break;
        }

        if (priority < 0 || priority > 255) throw new IllegalArgumentException("out-of-range priority");
        this.priority = priority;

        // Parse/validate pattern
        parse(idPattern);
        this.pattern = idPattern;

        this.expiration = expirationTime;
        this.info = info;
    }

    /**
     * Returns the pattern string that specifies a group of AppIDs.
     * 
     * @return the pattern string.
     */
    public String getAppIDPattern()
    {
        return pattern;
    }

    /**
     * Returns the action associated with this AppPattern.
     * 
     * @return the action.
     */
    public int getAction()
    {
        return action;
    }

    /**
     * Returns the search order priority of this AppPattern.
     * 
     * @return the search order priority.
     */
    public int getPriority()
    {
        return priority;
    }

    /**
     * Returns the time for this AppPattern to expire or <code>null</code> if it
     * never expires.
     * 
     * @return the expiration time or <code>null</code>.
     */
    public Date getExpirationTime()
    {
        return expiration;
    }

    /**
     * Returns MSO-private information of this AppPattern.
     * 
     * @return the MSO private information.
     */
    public Object getPrivateInfo()
    {
        return info;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * 
     * <p>
     * This method does not factor in <code>expirationTime</code> or
     * <code>info</code> attributes, but does compare <code>idPattern</code>,
     * <code>action</code>, and <code>priority</code> attributes.
     */
    public boolean equals(Object that)
    {
        AppPattern other;
        return (that instanceof AppPattern) && ((other = (AppPattern) that) != null) && action == other.action
                && priority == other.priority && pattern.equals(other.pattern);
    }

    public int hashCode()
    {
        return pattern.hashCode() ^ action ^ priority;
    }

    public String toString()
    {
        return super.toString() + "[" + "pattern=" + pattern + "," + "action=" + action + "," + "priority=" + priority
                + "," + "expiration=" + ((expiration == null) ? "never" : expiration.toString()) + "," + "private="
                + info + "]";
    }

    /**
     * Parses the given pattern, filling in the privately-held range values for
     * the pattern.
     * 
     * @param idPattern
     *            a String to specify an AppID group.
     * 
     * @throws IllegalArgumentException
     *             <code>idPattern</code> has a bad format
     */
    private void parse(String idPattern) throws IllegalArgumentException
    {
        if (idPattern == null) throw new IllegalArgumentException("null idPattern is not accepted");

        try
        {
            StringTokenizer strtok = new StringTokenizer(idPattern, ":-", true);
            String tok;

            // Initialize app with full range
            appStart = 0;
            appEnd = 0xFFFF;

            // org
            tok = strtok.nextToken();
            orgStart = parseHex(tok, false) & 0xFFFFFFFFL;
            orgEnd = orgStart;
            if (!strtok.hasMoreTokens()) return;

            // - or :
            tok = strtok.nextToken();
            if ("-".equals(tok))
            {
                tok = strtok.nextToken();
                orgEnd = parseHex(tok, false) & 0xFFFFFFFFL;
                if (orgEnd < orgStart)
                    throw new IllegalArgumentException("end of OID range is less than start: " + orgEnd + " < "
                            + orgStart);
                if (!strtok.hasMoreTokens()) return;

                tok = strtok.nextToken();
            }

            if (!":".equals(tok)) throw new IllegalArgumentException("Expected ':'");

            // app
            tok = strtok.nextToken();
            appStart = parseHex(tok, true);
            appEnd = appStart;
            if (appEnd < appStart) throw new IllegalArgumentException("end of AID range is less than start");
            if (!strtok.hasMoreTokens()) return;

            // -
            tok = strtok.nextToken();
            if (!"-".equals(tok)) throw new IllegalArgumentException("Expected '-'");
            tok = strtok.nextToken();
            appEnd = parseHex(tok, true);

            if (strtok.hasMoreTokens()) throw new IllegalArgumentException("Unexpected tokens after parsing");
        }
        catch (NoSuchElementException e)
        {
            throw new IllegalArgumentException("Ran out of tokens during parsing");
        }
    }

    /**
     * Parses a string in hexadecimal format. No leading "0x" is expected; the
     * presence of one will generate an IllegalArgumentException. Performs the
     * appropriate range checking for OID/AID values, according to <i>app</i>.
     * 
     * @param str
     *            the string to parse
     * @param app
     *            if <code>true</code>, consider this an AID value; else
     *            consider it an OID value
     * @throws IllegalArgumentException
     *             <code>idPattern</code> has a bad format
     */
    private int parseHex(String str, boolean app) throws IllegalArgumentException
    {
        if (str.startsWith("0x")) throw new IllegalArgumentException("No leading \"0x\" prefix is expected");

        int value;
        try
        {
            value = (int) (Long.parseLong(str, 16) & 0xFFFFFFFFL);
        }
        catch (NumberFormatException e)
        {
            throw new IllegalArgumentException("Could not parse \"" + str + "\" as hex value");
        }

        if (app && (value & ~0xFFFF) != 0)
            throw new IllegalArgumentException("Out of range for AID: " + str);
        else if (!app && (value == 0)) throw new IllegalArgumentException("Invalid OID: " + str);

        return value;
    }

    /**
     * Returns whether this <code>AppPattern</code> has expired or not.
     * 
     * @return <code>true</code> if this pattern has expired; <code>false</code>
     *         otherwise
     */
    boolean isExpired()
    {
        return expiration != null && expiration.before(new Date());
    }

    /**
     * Returns whether this AppPattern matches the given <code>AppID</code>.
     * This is a package private method, called by the <code>AppFilter</code>
     * implementation.
     * <p>
     * Another possible implementation would be for the internally-held range
     * values to be made package private so that the filter could access them
     * directly. This solution would enable the <code>AppFilter</code>
     * implementation to be optimized based on the values that it holds.
     * 
     * @param appID
     *            an AppID to test.
     * 
     * @return the action (ALLOW, DENY, ASK) associated this pattern if the
     *         given pattern matches; if there is no match then
     */
    int match(AppID id)
    {
        long org;
        int app;

        // If this pattern has expired, then we don't have a match
        if (isExpired()) return NO_MATCH_EXPIRED;

        org = id.getOID() & 0xFFFFFFFFL;
        if (org < orgStart || org > orgEnd) return NO_MATCH;
        app = id.getAID();
        if (app < appStart || app > appEnd) return NO_MATCH;

        // We have a match - return the action
        return action;
    }

    /**
     * This pattern's specified action.
     */
    private int action;

    /**
     * This pattern's specified priority.
     */
    private int priority;

    /**
     * This pattern's specified pattern (as a string).
     */
    private String pattern;

    /**
     * This pattern's specified pattern (as integer ranges).
     * 
     * <pre>
     * &quot;&lt;i&gt;orgStart&lt;/i&gt;[-&lt;i&gt;orgEnd&lt;/i&gt;][:&lt;i&gt;appStart&lt;/i&gt;[-&lt;i&gt;appEnd&lt;/i&gt;]]&quot;
     * </pre>
     */
    private long orgStart, orgEnd;

    /**
     * This pattern's specified pattern (as integer ranges).
     * 
     * <pre>
     * &quot;&lt;i&gt;orgStart&lt;/i&gt;[-&lt;i&gt;orgEnd&lt;/i&gt;][:&lt;i&gt;appStart&lt;/i&gt;[-&lt;i&gt;appEnd&lt;/i&gt;]]&quot;
     * </pre>
     */
    private int appStart, appEnd;

    /**
     * This pattern's expiration time/date.
     */
    private Date expiration;

    /**
     * This pattern's private data.
     */
    private Object info;
}
