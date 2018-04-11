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

package org.dvb.media;

import java.awt.Component;
import javax.media.Time;
import java.io.IOException;
import javax.media.Control;
import org.cablelabs.impl.util.SecurityUtil;
import org.cablelabs.impl.media.player.DripFeedPlayer;
import org.cablelabs.impl.media.player.Player;
import org.cablelabs.impl.media.player.PlayerAssociationControl;
import org.apache.log4j.Logger;

/**
 * This class allows to create a source for a JMF player to be able to feed the
 * decoder progressively with parts of a clip (e.g. I or P MPEG-2 frame)
 * according to the drip-fed mode format defined in the MHP content format
 * clause.
 * <P>
 * To start using the drip-feed mode, the application needs to instantiate a
 * player representing a MPEG-2 video decoder and have its source be a
 * DripFeedDataSource instance.
 * <P>
 * A DripFeedDataSource instance can be obtained by calling the default
 * constructor of the class.
 * <P>
 * A player that will be bound to a MPEG-2 video decoder (when realized) can be
 * created with a locator of the following text representation: "dripfeed://".
 * <P>
 * After having the DripFeedDataSource connected to a Player representing a
 * MPEG-2 video decoder, the following rules applies: <BR>
 * - If the feed method is called when the player is in the "prefetched" state
 * the image will be stored so that when the player goes in the "started" state
 * it will be automatically displayed. <BR>
 * - If the feed method is called when the player is in the "started" mode, the
 * frame shall be displayed immediately. In particular it is not required to
 * feed a second frame to the decoder to display the first frame. <BR>
 * - If the feed method is called when the player is in any other state (or if
 * the DripFeedDataSource is not connected to a player), it will be ignored by
 * the platform implementation. <BR>
 */

public class DripFeedDataSource extends javax.media.protocol.DataSource
{
    /* logging */
    private static final Logger log = Logger.getLogger(DripFeedDataSource.class);

    /**
     * Constructor. A call to the constructor will throw a security exception if
     * the application is not granted the right to use the drip feed mode.
     * The constructor shall automatically set the MediaLocator for this
     * DataSource to the only allowed value: dripfeed://. There is no need for
     * applications to later call setLocator.
     */
    public DripFeedDataSource()
    {
        // Make sure the caller has permission to use a drip feed
        // The security manager will throw an exception if permission is not
        // granted
        SecurityUtil.checkPermission(new DripFeedPermission(""));
    }

    // This player reference is filled in by a DripFeedPlayer when its setSource
    // method is invoked. The DripFeedPlayer passes a reference to itself to
    // this
    // DripFeedDataSource using the PlayerAssocition control interface
    private DripFeedPlayer dripPlayer = null;

    /**
     * This method allows an application to feed the decoder progressively with
     * parts of a clip (e.g. I or P MPEG-2 frame) according to the drip-fed mode
     * format defined in the MHP content format clause.<BR>
     * The feed method shall not be called more often than every 500ms. If this
     * rule is not respected, display is not guaranteed.
     * <p>
     * While in the prefetch state the drip feed data source is only required to
     * correctly process a single invocation of this method where the data
     * consists only of a single I frame. Possible additional invocations while
     * in the prefetch state shall have implementation specific results.
     *
     * @param drip
     *            Chunk of bytes compliant with the drip-fed mode format defined
     *            in the MHP content format clause (i.e. one MPEG-2 frame with
     *            optional synctactic MPEG-2 elements).
     *
     */
    public void feed(byte[] dripFrame)
    {
        if (log.isDebugEnabled())
        {
            log.debug("DripFeedDataSource.feed - dripPlayer = " + dripPlayer);
        }

        // if no player, then there is nothing to do
        // otherwise, give the drip data to the player for rendering
        if (dripPlayer != null)
        {
            dripPlayer.feed(dripFrame);
        }
    }

    /**
     * This method shall return the content type for mpeg-2 video "drips"
     *
     * @return the content type for MPEG-2 video drips
     */
    public java.lang.String getContentType()
    {
        return "video/dvb.mpeg.drip";
    }

    /**
     * This method shall not be used and has no effect. This source is
     * considered as always connected.
     *
     * @throws IOException
     *             never thrown in this sub-class
     */
    public void connect() throws IOException
    {
    }

    /**
     * This method shall not be used and has no effect. This source is
     * considered as always connected.
     */
    public void disconnect()
    {
    }

    /**
     * This method shall not be used and has no effect. This source is
     * considered as always started.
     *
     * @throws IOException
     *             never thrown in this sub-class
     */
    public void start() throws IOException
    {
    }

    /**
     * This method shall not be used and has no effect. This source is
     * considered as always started.
     *
     * @throws IOException
     *             never thrown in this sub-class
     */
    public void stop() throws IOException
    {
    }

    /**
     * This method shall not be used and has no effect.
     *
     * @return DURATION_UNKNOWN.
     */
    public Time getDuration()
    {
        return DURATION_UNKNOWN;
    }

    // The dripFeedAssociationControl allows players to associate with this
    // DripFeedDataSource
    private DripFeedPlayerAssociationImpl dripFeedAssociationCtrl = new DripFeedPlayerAssociationImpl();

    // array of controls for a DripFeedDataSource
    private Control[] controls = {};

    /**
     * Obtain the collection of objects that control this object. If no controls
     * are supported, a zero length array is returned.
     *
     * @return the collection of object controls
     */
    public Object[] getControls()
    {
        return controls;
    }

    /**
     * Obtain the object that implements the specified Class or Interface. The
     * full class or interface name must be used. If the control is not
     * supported then null is returned.
     *
     * @param controlType
     *            the full class or interface name of the requested control
     * @return the object that implements the control, or null.
     */
    public Object getControl(String controlType)
    {
        if (log.isDebugEnabled())
        {
            log.debug("DripFeedDataSource getControl( " + controlType + ")");
        }

        if (controlType == null)
            throw new NullPointerException("null Control name");

        // Lookup the Class object for 'forName'. If not found, return null.
        Class ctrlClass = null;
        try
        {
            ctrlClass = Class.forName(controlType);
        }
        catch (ClassNotFoundException e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("DripFeedDataSource caught exception - control class not found");
            }
            return null;
        }

        // Handle special case for this control class.
        if (ctrlClass.isInstance(dripFeedAssociationCtrl))
        {
            return dripFeedAssociationCtrl;
        }

        // Iterate through the controls, looking for one that is an instanceof
        // the 'ctrlClass'.
        for (int i = 0; i < controls.length; ++i)
        {
            Control ctrl = controls[i];
            if (ctrlClass.isInstance(ctrl))
                return ctrl;
        }

        if (log.isDebugEnabled())
        {
            log.debug("DripFeedDataSource control " + controlType + " not supported");
        }

        return null;
    }

    /**
     * PlayerAssociationControl class provides the implementation for the
     * {@link PlayerAssociationControl} interface. This interface is used by a
     * player to associate itself with the object that exposes the
     * {@link PlayerAssociationControl} interface. The example usage is for a
     * {@link DripFeedDataSource} to support {@link PlayerAssociationControl} to
     * allow a {@link DripFeedPlayer} to associate with a
     * {@link DripFeedDataSource}.
     *
     * @author scottb
     */
    protected class DripFeedPlayerAssociationImpl implements PlayerAssociationControl
    {
        protected DripFeedPlayerAssociationImpl()
        {
        }

        public void setPlayer(Player player)
        {
            if (log.isDebugEnabled())
            {
                log.debug("DripFeedPlayerAssociationImpl.setPlayer - player = " + player);
            }

            if (!(player instanceof DripFeedPlayer))
            {
                ;
                // TODO: Define an exception for incompatible player and throw
                // it here?
                // Where should IncompatiblePlayerException be defined?
                // org.cablelabs.impl.media.player?
                // throw IncompatiblePlayerException;
            }

            dripPlayer = (DripFeedPlayer) player;
        }

        // This implementation does not implement visual control components.
        public Component getControlComponent()
        {
            return null;
        }
    }

    static
    {
        org.cablelabs.impl.ocap.OcapMain.loadLibrary();
    }
}
