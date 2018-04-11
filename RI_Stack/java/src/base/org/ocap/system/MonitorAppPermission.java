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

package org.ocap.system;

import org.dvb.user.UserPreferencePermission;

/**
 * The MonitorAppPermission class represents permission to execute privileged
 * operations only Monitor Application should be granted.
 * <p>
 * A MonitorAppPermission consists of a permission name, representing a single
 * privileged operation. The name given in the constructor may end in ".*" to
 * represent all permissions beginning with the given string, such as
 * <code>"*"</code> to allow all MonitorAppPermission operations, or
 * <code>"handler.*"</code> to only allow setting any handler.
 * <p>
 * The following table lists all MonitorAppPermission permission names.
 * <table border=1 cellpadding=5>
 * <tr>
 * <th>Permission Name</th>
 * <th>What the Permission Allows</th>
 * <th>Description</th>
 * </tr>
 *
 * <tr>
 * <td>registrar</td>
 * <td>Provides access to the Application Database by way of the AppRegistrar</td>
 * <td>This permission allows the caller to add or remove applications from the
 * Application Database.</td>
 * </tr>
 *
 * <tr>
 * <td>service</td>
 * <td>Allows creation of an AbstractService</td>
 * <td>Applications with this permission can create and manage their own service
 * contexts and the services running in those service contexts.</td>
 * </tr>
 *
 * <tr>
 * <td>servicemanager</td>
 * <td>Allows management of all services</td>
 * <td>Applications with this permission can create their own service contexts
 * and manage both their own and other service contexts and services.</td>
 *
 * <tr>
 * <td>security</td>
 * <td>Allows setting the {@link org.ocap.application.SecurityPolicyHandler}
 * used by the AppManager</td>
 * <td>This permission allows the application to register a
 * SecurityPolicyHandler with the AppManager to determine the
 * PermissionCollection granted to applications before they are run.</td>
 * </tr>
 *
 * <tr>
 * <td>reboot</td>
 * <td>Initiates a system to reboot itself</td>
 * <td>This permission allows the caller to request for a system reboot.</td>
 * </tr>
 *
 * <tr>
 * <td>systemevent</td>
 * <td>Allows setting the error, resource depletion, or reboot handlers</td>
 * <td>This permission allows the Monitor Application to be alerted upon system
 * reboot, resource depletion, and error events.</td>
 * </tr>
 *
 * <tr>
 * <td>handler.appFilter</td>
 * <td>Set a new black and white list to the system</td>
 * <td>This permission allows the application to set a new black and white list,
 * which the system uses to determine whether to accept or reject broadcasted
 * applications on the receiver. Such control should only be granted to a
 * monitor application.</td>
 * </tr>
 *
 * <tr>
 * <td>handler.resource</td>
 * <td>Set a Resource Contention Handler</td>
 * <td>Set a handler to resolve resource contention between two or more apps see
 * {@link org.ocap.resource.ResourceContentionManager#setResourceContentionHandler}
 * .</td>
 * </tr>
 *
 * <tr>
 * <td>handler.closedCaptioning</td>
 * <td>Set closed-captioning preferences and control captioning.</td>
 * <td>Allows monitor application to get a
 * {@link org.ocap.media.ClosedCaptioningAttribute} and call methods in a
 * {@link org.ocap.media.ClosedCaptioningControl}.</td>
 * </tr>
 *
 * <tr>
 * <td>filterUserEvents</td>
 * <td>Filter user events</td>
 * <td>This permission allows the user to filter user events.</td>
 * </tr>
 *
 * <tr>
 * <td>handler.eas</td>
 * <td>Set preferences of Emergency Alert System (EAS) message representation.</td>
 * <td>Allows monitor application to set preferences of EAS message
 * representation and add a new EAShandler by calling
 * {@link org.ocap.system.EASModuleRegistrar#registerEASHandler}.</td>
 *
 * </tr>
 *
 * <tr>
 * <td>setVideoPort</td>
 * <td>Allows enabling and disabling video port</td>
 * <td>Allows monitor to call org.ocap.hardware.VideoOutputPort.enable() and
 * org.ocap.hardware.VideoOutputPort.disable().</td>
 * </tr>
 *
 * <tr>
 * <td>podApplication</td>
 * <td>Allows access to Specific Application Support Resource</td>
 * <td>Allows Monitor Application to call org.ocap.system.SystemModuleRegistrar.
 * </td>
 * </tr>
 *
 * <tr>
 * <td>signal.configured</td>
 * <td>Allows monitor to signal implementation to resume boot processing after
 * handlers have been set</td>
 * <td>Allows monitor to call org.ocap.OcapSystem.monitorConfiguredSignal().</td>
 * </tr>
 *
 * <tr>
 * <td>storage</td>
 * <td>Provides control of persistent storage devices and content stored therein
 * </td>
 * <td>Allows monitor to delete volumes it does not own, initialize
 * {@link org.ocap.storage.StorageProxy} associated devices, make detachable
 * devices ready for detaching or ready for use, and set file access permissions
 * for any application accessible file or directory.</td>
 * </tr>
 *
 * <tr>
 * <td>properties</td>
 * <td>Allows monitor to access ocap system properties</td>
 * <td>Allows monitor to call read ocap properties that require monitor
 * application permission.</td>
 * </tr>
 *
 * <tr>
 * <td>registeredapi.manager</td>
 * <td>Provides access to network specific APIs</td>
 * <td>Gives monitor ability to register an API, remove a registered API, or
 *     access a registered API.</td>
 * </tr>
 *
 * <tr>
 * <td>vbifiltering</td>
 * <td>Allows monitor application to filter VBI data. </td>
 * <td>Allows monitor application to call a VBIFilterGroup constructor.</td>
 * </tr>
 *
 * <tr>
 *   <td>codeDownload</td>
 *   <td>Allows monitor application to initiate a download, generally following
 *   a CVT signaling a deferred download </td>
 *   <td>Allow monitor application to call Host.codeDownload method.</td>
 * </tr>
 *
 * <tr>
 *   <td>mediaAccess</td>
 *   <td>Allows monitor application to register MediaAccessHandler.</td>
 *   <td>Allows monitor application to call a
 *       MediaAccessHandlerRegistrar.setExternalTriggers(). Allows monitor
 *       application to call a
 *       MediaAccessConditionControl.conditionHasChanged().</td>
 * </tr>
 *
 *<tr>
 * <td>powerMode</td>
 * <td>Allows an application to set the power mode.</td>
 * <td>Applications with this permission can programmatically control the power
 * mode of the device.</td>
 * </tr>
 *
 *
 *<tr>
 * <td>environment.selection</td>
 * <td>Allows monitor application to request the cable environment to become
 * selected or deselected</td>
 * <td>Allows monitor application to request the cable environment
 * to become selected or deselected by calling Environment.select or
 * Environment.deselect</td>
 *</tr>
 *
 * <tr>
 *   <td>logger.config</td>
 *   <td>Allows an application to configure the logger.</td>
 *   <td>Applications with this permission can programmatically control the
 *       configuration of the logger using the log4j API.</td>
 * </tr>
 *
 * <tr>
 * <td>testApplications</td>
 * <td>Allows monitor application to control how applications with
 * &quot;test_application_flag&quot; set are handled</td>
 * <td>Allows monitor application to control whether applications with
 * &quot;test_application_flag&quot; are ignored (as normal) or whether this
 * flag is ignored. This is controlled by the method
 * AppManagerProxy.enableTestApplications().
 *</tr>
 *</table>
 *
 * Other permissions may be added as necessary.
 *
 * @ext FP The Front Panel extension adds the following:
 *      <table border=1 cellpadding=5>
 *      <tr>
 *      <td>frontpanel</td> <td>Allows use of the front panel API.</td> <td>
 *      Allows an application to get the front panel manager singleton and use
 *      the front panel API to modify the front panel display.</td>
 *      </tr>
 *      </table>
 *
 * @ext DVR The DVR extension adds the following:
 *      <table border=1 cellpadding=5>
 *      <tr>
 *      <td>recording</td> <td>Allows management of system wide recording
 *      operations.</td> <td>Applications with this permission can delay the
 *      start of scheduled recordings and disable system wide buffering.</td>
 *      </tr>
 *      <tr>
 *      <td>handler.recording</td> <td>Allows management of recording
 *      prioritization and resolution.</td> <td>Applications with this
 *      permission can register as request resolution handler, register as space
 *      allocation handler and manipulate prioritization of scheduled
 *      recordings.</td>
 *      </tr>
 *      </table>
 */
public final class MonitorAppPermission extends java.security.BasicPermission
{

    /**
     * Determines if a de-serialized file is compatible with this class.
     *
     * Maintainers must change this value if and only if the new version
     * of this class is not compatible with old versions.See spec available
     * at http://docs.oracle.com/javase/1.4.2/docs/guide/serialization/ for
     * details
     */
    private static final long serialVersionUID = -8998202296815337458L;

    /**
     * Constructor for the MonitorAppPermission
     *
     * @param name
     *            the name of this permission (see table in class description)
     */
    public MonitorAppPermission(String name)
    {
        super(name);
    }

    /**
     * Checks if the specified permission is "implied" by this object.
     * <p>
     * �deviceController� permission implies �powerMode� permission. ECN 1297
     * <p>
     *
     * @param p
     *            the permission to check against.
     * @return true if the passed permission is equal to or implied by this
     *         permission, false otherwise.
     */
    public boolean implies(java.security.Permission p)
    {

        /*
         * replaced by code modeling fp and dvr with DeviceSettingPermissions
         * and xmlMgrImpl //powerMode is implied by deviceController
         * if(this.getName().equals("deviceController")&&
         * (p.getName().equals("powerMode"))) return true;
         */
        // ECN 1316 User preference write and read permissions are implied by
        // MonApp properties perm
        /* else */if ((this.getName().equals("properties")) && (
        // Check if isImpliedPerm is a UserPref perm.
                p.equals(new UserPreferencePermission("write")) || p.equals(new UserPreferencePermission("read"))))
            return true;
        else
            return super.implies(p);
    }
}
