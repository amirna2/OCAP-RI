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

import java.util.StringTokenizer;

import org.apache.log4j.Logger;


/**
 * This is a wrapper class for a string representation of an OID and is designed
 * to support {@link OIDMap}. {@link OIDMap} requires both the key and map be
 * derived from {@link Object}. Critical is the {@link Comparable} interface
 * needed to correctly sort the OIDs in {@link OIDMap}.
 * <p>
 * {@code OID} supports {@link OIDMap} in other ways. {@link OID} supplies
 * methods such as {@link #getLowestChildAsOid()} which are used by
 * {@link OIDMap} to bound searches for submaps.
 * <p>
 * Key methods are:
 * <ol>
 * <li> {@link OID#compareTo(Object)}
 * <li>	{@link OID#getRelationship(OID)}
 * <li> {@link OID#getLowestChildAsOid()}
 * <li> {@link OID#getNextLowestSibAsOid()}
 * </ol>
 * <p>
 * 
 * @author acossitt
 * 
 */
public class OID implements Comparable
{
    private static final Logger log = Logger.getLogger(OID.class);

    public static final int cEqual = 0;

    public static final int cParent = 1; // includes grandparents, etc.

    public static final int cChild = 2;

    public static final int cGreaterSibling = 3;

    public static final int cLesserSibling = 4;

    public static final int cLesserBranch = 5;

    public static final int cGreaterBranch = 6;

    /**
     * The root of all OID trees. i.e., OID("") is the root of OID("0"),
     * OID("1"), etc.
     */
    public static final OID cRoot = new OID("");

    /**
     * Delimiter between parts of an OID string representation: 1.2.3.3.4.0 etc.
     */
    public static final String cOidDelim = ".";

    /**
     * All leaf (scalar) OIDs end with {@code .0}
     */
    public static final String cLeafOidEnd = ".0";

    private final String oidS;

    private final int[] tree;

    /**
     * Special case. Say we had an OID 1.2.3.4 and wanted the lowest possible
     * child of that OID. By that I mean that this is OID is the lower than an
     * other child of the OID (except for another lowest child, which would be
     * equal). The representation would actually be the 1.2.3.4.0.0.0...(to
     * infinity) but infinity and computers don't match so I emulate that by
     * turning on the lowestChild flag which (virtually) repeats the last .0 out
     * to infinity. This is necessary so that methods such as
     * {@link OIDMap#getChildren(OID)} and
     * {@link OIDMap#getParentAndChildren(OID)} work correctly.
     * <p>
     * <code>
     * 1.2.3.4.0 (lowestChild) is lowest child of 1.2.3.4
     * 1.2.3.4.0 (lowestChild) is child of 1.2.3.4.0
     * 1.2.3.4.0 (lowestChild) is child of 1.2.3.4.0.0.0
     * </code> etc.
     */
    private boolean lowestChild = false;

    protected OID(int[] tree)
    {
        this.tree = tree;
        this.oidS = genString(tree);

    }

    public OID(String oid)
    {
        this.oidS = oid;
        this.tree = genTree(oid);
    }

    public OID(String oid, boolean lowestChild)
    {
        this.oidS = oid;
        this.tree = genTree(oid);
        this.lowestChild = lowestChild;
    }

    /**
     * Override of {@link Object#toString()}
     * 
     * @return a human readable representation of the OID including whether this
     *         a lowestChild
     */
    public String getString()
    {
        return (lowestChild ? oidS + " (lowestChild)" : oidS);
    }

    public int[] getTreeArray()
    {
        return tree;
    }

    public int[] getTreeArray(int toDepth)
    {
        int[] copy = new int[toDepth];
        System.arraycopy(tree, 0, copy, 0, toDepth);

        return copy;
    }

    public int getRoot()
    {
        return tree[0];
    }

    // public int getRootOfNextLowestBranch()
    // {
    // int lowerRoot = tree[0] -1;
    // return lowerRoot; // can be < 0
    // }

    /**
     * Get a OID that represents the root of the branch of this OID
     * 
     * @return
     */
    public OID getRootAsOid()
    {
        String oid = Integer.toString(getRoot());
        return new OID(oid);
    }

    // public OID getRootOfNextLowestBranchAsOid()
    // {
    // int nextLowestBranch = getRootOfNextLowestBranch();
    // if(nextLowestBranch < 0) return null; // no such branch exists
    //        
    // String oid = Integer.toString(nextLowestBranch);
    // return new OID(oid);
    // }

    public int[] getParentTree()
    {
        int[] parentTree = new int[tree.length - 1];
        System.arraycopy(tree, 0, parentTree, 0, tree.length - 1);

        return parentTree;
    }

    /**
     * Get a OID that represents the parent of this OID
     * 
     * @return
     */
    public OID getParentAsOid()
    {
        return new OID(getParentTree());
    }

    /**
     * @see lowestChild
     * @return an OID that is the lowest child of this oid (as calculated by
     *         {@link getRelationship})
     */
    public OID getLowestChildAsOid()
    {
        int[] childTree = new int[this.tree.length + 1];
        System.arraycopy(this.tree, 0, childTree, 0, this.tree.length);
        // not needed: childTree[childTree.length-1] = 0;
        OID lowestChild = new OID(childTree);
        lowestChild.setLowestChild();
        return lowestChild;
    }

    private void setLowestChild()
    {
        this.lowestChild = true;

    }

    public OID getNextLowestSibAsOid()
    {
        int[] nextSibTree = new int[this.tree.length];
        System.arraycopy(this.tree, 0, nextSibTree, 0, this.tree.length);
        nextSibTree[this.tree.length - 1]++;
        OID lowestSib = new OID(nextSibTree);
        lowestSib.setLowestChild(); // lowest child in the next lowest sib tree
        return lowestSib;
    }

    private String genString(int[] tree)
    {
        String strRep = "";
        for (int i = 0; i <= tree.length - 2; i++)
        {
            strRep += tree[i] + cOidDelim;
        }
        strRep += tree[tree.length - 1]; // no ending cOidDelim

        return strRep;
    }

    protected int[] genTree(String oid)
    {
        StringTokenizer st = new StringTokenizer(oid, cOidDelim);

        int arraySize = st.countTokens();

        int[] treeRep = new int[arraySize];

        for (int arrayIdx = 0; st.hasMoreElements(); arrayIdx++)
        {
            String element = (String) st.nextElement();
            treeRep[arrayIdx] = Integer.parseInt(element);
        }

        return treeRep;

    }

    public boolean equals(Object obj)
    {
        if (obj == this) return true;

        if (!(obj instanceof OID)) return false;

        OID o = (OID) obj;

        if (getRelationship(o) == cEqual)
        {
            return true;
        }
        return false;

    }

    public int hashCode()
    {
        return getString().hashCode();
    }

    public String toString()
    {
        return getString();
    }

    public boolean isTree(String oid)
    {
        // TODO, TODO_SNMP: how do I determine this?
        return false;
    }

    public static boolean isLeaf(String oid)
    {
        return determineLeafness(oid);
    }

    /**
     * Get a value in the {@link #tree} array. This method as returns tree
     * values for {@link #OID}s that use {@link #lowestChild}.
     * <p>
     * When {@link #lowestChild} is {@code true} and the index is past the limit
     * of the physical {@link #tree} this routine will return {@code 0}.
     * 
     * @param idx
     *            index into the {@link #tree} array.
     * 
     * @return the physical tree value at {@code idx}, or if
     *         {@link #lowestChild} is {@code true} and {@code idx} is past the
     *         limit of the physical {@link #tree} return {@code 0}.
     */
    private int getTreeValue(int idx)
    {
        if (idx > this.tree.length - 1)
        {
            if (this.lowestChild)
                return 0;
            else
                throw new IndexOutOfBoundsException();
        }
        else
            return this.tree[idx];
    }

    /**
     * Get the length of the virtual or real tree
     * 
     * @param compareOid
     * @return
     */
    private int getTreeLength(OID compareOid)
    {
        if (!this.lowestChild)
        {
            return tree.length;
        }
        else
        {
            return getMaxRealTreeLength(this, compareOid) + 1; // an OID that is
                                                               // a lowestChild
                                                               // can never be a
                                                               // sibling, so
                                                               // add one to the
                                                               // length to make
                                                               // this
                                                               // determination
                                                               // easier.
        }
    }

    /**
     * Get the maximum real length of the two {@link #tree}s.
     * 
     * @param oidA
     * @param oidB
     * @return
     */
    private int getMaxRealTreeLength(OID oidA, OID oidB)
    {
        return (oidA.tree.length >= oidB.tree.length ? oidA.tree.length : oidB.tree.length);
    }

    /**
     * Get the relationship between this OID and the OID passed in. The return
     * value is the relationship relative to this OID. If {@link cParent} is
     * returned, then this OID is a parent (grandparent, great-grandparent,
     * etc.) of the OID passed in, etc.
     * 
     * @see #cChild
     * @see #cEqual
     * @see #cGreaterBranch
     * @see #cGreaterSibling
     * @see #cLesserBranch
     * @see #cLesserSibling
     * @see #cParent
     * 
     * @param o
     * 
     * @return the relationship as defined above in the "See Also" section.
     */
    public int getRelationship(OID o)
    {
        OID inOid = (OID) o;

        int branchTestDepth = -1;
        int thisTreeLen = this.getTreeLength(inOid);
        int inOidTreeLen = inOid.getTreeLength(this);

        if (thisTreeLen == inOidTreeLen)
        {
            // possible that we are siblings or equal, but check for that after
            // checking for
            // branches. This is an optimization so there are not a lot checks
            // inside the commonDepth loop.
            branchTestDepth = thisTreeLen - 1;
        }
        else
        {
            // shorter of two the lengths
            branchTestDepth = (thisTreeLen <= inOidTreeLen ? thisTreeLen : inOidTreeLen);
        }

        // first check for a branch
        for (int i = 0; i < branchTestDepth; i++)
        {
            int thisNode = this.getTreeValue(i);
            int inOidNode = inOid.getTreeValue(i);

            if (thisNode > inOidNode)
            {
                printRelationship(o, cGreaterBranch, null);
                return cGreaterBranch;
            }
            else if (thisNode < inOidNode)
            {
                printRelationship(o, cLesserBranch, null);
                return cLesserBranch;
            }
        }

        // got to the end of branch depth and everything was equal and lengths
        // are equal. Look for siblings or equality
        // for example 1.2.3.4 is a lower sibling of 1.2.3.5 1.2.3.4 == 1.2.3.4
        // etc.
        if (thisTreeLen == inOidTreeLen)
        {
            int myLastValue = this.getTreeValue(thisTreeLen - 1);
            int inOidLastValue = inOid.getTreeValue(inOidTreeLen - 1);

            if (myLastValue < inOidLastValue)
            {
                printRelationship(o, cLesserSibling, null);
                return cLesserSibling; // I'm a sibling of o
            }
            else if (myLastValue > inOidLastValue)
            {
                printRelationship(o, cGreaterSibling, null);
                return cGreaterSibling;
            }
            else
            // ==
            {
                printRelationship(o, cEqual, null);
                return cEqual;
            }
        }
        // got to the end of branch depth and everything was equal and lengths
        // are not equal
        else if (thisTreeLen > inOidTreeLen)
        {
            // so with my greater tree depth I am a child
            printRelationship(o, cChild, "thisTreeLen > inOidTreeLen");
            return cChild;
        }
        else if (thisTreeLen < inOidTreeLen)
        {
            // so with my lesser tree depth I am a parent
            printRelationship(o, cParent, null);
            return cParent;
        }

        // defensive programming.
        throw new IllegalStateException("Failed to find a relationship for this=" + this.toString() + ", inOid="
                + inOid.toString());
    }

    private void printRelationship(OID o, int relationship, String note)
    {
        String s = null;
        switch (relationship)
        {
            case cChild:
                s = "is child of";
                break;
            case cEqual:
                s = "is equal to";
                break;
            case cGreaterBranch:
                s = "is greater branch of";
                break;
            case cGreaterSibling:
                s = "is greater sibling of";
                break;
            case cLesserBranch:
                s = "is lesser branch of";
                break;
            case cLesserSibling:
                s = "is lesser sibling of";
                break;
            case cParent:
                s = "is a parent of";
                break;
            default:
                if (log.isErrorEnabled())
                {
                    log.error("printRelationship - unknown relationship");
                }
                break;
        }

        //guard added for consistency
        if (log.isDebugEnabled())
        {
            log.debug("getRelationship " + this.getString() + " " + s + " " + o.toString() + ", note: " + note);
        }
    }

    /**
     * 
     * 
     * Algorithm definition:
     * <ul>
     * <li>parents greater than children (1.2.3.4 > 1.2.3.4.10)
     * <li>for different branches: 1.3.* > 1.2.* ; 2.0 > 1.20000
     * <li>different OID objects with identical string representations are equal
     * (oid1#1.2.3.4 == oid2#1.2.3.4)
     * </ul>
     * 
     * @param o
     * @return
     */
    public int compareTo(Object o)
    {
        OID inOid = (OID) o;

        int compValue = getRelationship(inOid);

        switch (compValue)
        {
            case cLesserBranch:
            case cLesserSibling:
            case cChild:
                if (log.isDebugEnabled())
                {
                    log.debug("compareTo returns -1");
                }
                return -1; // I'm lesser
            case cEqual:
                if (log.isDebugEnabled())
                {
                    log.debug("compareTo returns  0");
                }
                return 0;
            case cParent:
            case cGreaterSibling:
            case cGreaterBranch:
                if (log.isDebugEnabled())
                {
                    log.debug("compareTo returns  1");
                }
                return 1; // I'm greater
            default:
                throw new IllegalArgumentException();
        }
    }

    public static boolean isWellFormed(String oid)
    {
        // "" is valid (root of all OIDs)
        if (oid.equals(OID.cRoot.oidS)) return true;

        // 01234567 index (length == 8)
        // 1.2.3.4. is invalid
        if (oid.lastIndexOf(".") == oid.length() - 1)
        {
            return false;
        }

        // not doing more through checks (looking for characters other then
        // periods and numbers, etc.)

        return true;
    }

    private static boolean determineLeafness(String oidStr)
    {
        int idx = oidStr.lastIndexOf(cLeafOidEnd);

        // "1.2.3.4.0" has lastIndexof(cLeafOidEnd) == 7
        // "1.2.3.4.0" has length == 9
        // ".0" has length == 2
        // 9-2=7

        if (idx == (oidStr.length() - cLeafOidEnd.length()))
        {
            return true;
        }
        return false;
    }

    /**
     * Compares two OID strings to determine if one is out of scope of the other <br>
     * @param oid1 defines the scope
     * @param oid2 tested for being out of scope of oid1
     * @return true if oid2 is NOT a child of, or identical to oid1
     * <br><br>
     */
    public static boolean outOfScope(String oid1, String oid2)
    {
        boolean outOfScope;
        int oid1Depth = getOIDDepth(oid1);
        int oid2Depth = getOIDDepth(oid2);

        if (oid2Depth > oid1Depth)
        {
            if (oid1.compareTo(trimOID(oid2, oid1Depth)) == 0)
            {
                outOfScope = false;
            }
            else
            {
                outOfScope = true;
            }
        }
        else if (oid2Depth == oid1Depth)
        {
            if (oid1.compareTo(oid2) == 0)
            {
                outOfScope = false;
            }
            else
            {
                outOfScope = true;
            }
        }
        else
        {
            // oid1 is deeper so oid2 cannot be equal to, or a child of oid1
            outOfScope = true;
        }

        return outOfScope;
    }

    /**
     * trims the OID string to the required depth
     * @param oid OID string to be trimmed
     * @param depth token depth to trim the supplied OID to
     * @return
     */
    private static String trimOID(String oid, int depth)
    {
        StringBuffer sb = new StringBuffer();
        StringTokenizer st = new StringTokenizer(oid, ".");

        while ((depth > 1) && (st.hasMoreElements()))
        {
            sb.append(st.nextToken());
            if (st.hasMoreElements())
            {
                sb.append(".");
                depth --;
            }
        }

        // Add the last one as the 0th element should always be returned.
        if (st.hasMoreElements())
        {
            sb.append(st.nextToken());
        }

        return sb.toString();
    }

    /**
     * @param oid OID string to be measured
     * @return number of tokens separated by '.'s present the OID string
     */
    private static int getOIDDepth(String oid)
    {
        StringTokenizer st = new StringTokenizer(oid, ".");
        return st.countTokens();
    }
}
