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

package org.cablelabs.impl.manager.system;

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.GraphicsManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.PODManager;
import org.cablelabs.impl.manager.pod.PODListener;
import org.cablelabs.impl.manager.system.mmi.CableCardUrlGetter;
import org.cablelabs.impl.manager.system.mmi.MmiSystemModule;
import org.cablelabs.impl.manager.system.mmi.ResidentMmiHandlerImpl;
import org.cablelabs.impl.manager.system.ui.DisplayManager;
import org.cablelabs.impl.manager.system.ui.DisplayManagerImpl;
import org.cablelabs.impl.pod.mpe.PODEvent;
import org.cablelabs.impl.util.SecurityUtil;
import org.ocap.hardware.pod.POD;
import org.ocap.system.MonitorAppPermission;
import org.ocap.system.SystemModuleRegistrar;
import org.ocap.system.SystemModuleHandler;

import java.awt.Container;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.math.BigInteger;
import java.util.Enumeration;
import java.util.Hashtable;
import org.apache.log4j.Logger;

/**
 * Implements the OCAP SystemModuleRegistrar, as of ECR OCAP1.0-R-03.0531-1.
 * This class is also responsible implementing a number of the implicit POD
 * related functional requirements of OCAP. Those responsibilities include: 1)
 * Maintaining the knowledge of any resident device applications that register
 * SAS handlers such that if they are replaced by application handlers they will
 * be restored as the handler for a session once the application handler
 * unregisters for whatever reason. 2) The default MMI handler is installed at
 * instantiation time and later if a registered application MMI handler
 * unregisters for whatever reason, the default system MMI handler is restored
 * to process any further MMI activity.
 */
public class SystemModuleRegistrarImpl extends SystemModuleRegistrar implements PODListener
{
    PODManager m_podman;
    private boolean podResetReceived = false;

    /**
     * Private constructor. Instances should be created via {@link #getInstance}
     * .
     */
    protected SystemModuleRegistrarImpl()
    {
        // Instantiate the default resident MMI handler to make sure it's
        // available for processing
        // any POD initiaited MMI activity prior registration of an application
        // handler.
        // MMISystemModuleHandler calls registerMMIHandler from its constructor,
        // so there's no need to save the reference here.

        // handler from within the system context
        ccm.getSystemContext().runInContext(new Runnable()
        {
            public void run()
            {
                createResidentMmiHandler();
            }
        });

        // TODO: instantiate default MMI handler class based on property?
        // defaultMMIHandler = new
        // getClassForName(getProperty("DefaultMMIHandlerClass"));
        
        /*
         * Create instance of CableCardUrlGetter to handle System transactions.
         */
        urlGetter = CableCardUrlGetter.getSession();
        
        MmiSystemModule urlSystemModule = new MmiSystemModule(urlGetter);
        urlSystemModule.connect();
        
        urlGetter.ready(urlSystemModule);
        
        /*
         * Create DisplayManager.
         */
        displayManager = createDisplayManager();
        
        GraphicsManager gfx = (GraphicsManager) ManagerManager.getInstance(GraphicsManager.class);
        final Container mmiPlane = gfx.getMMIDialogPlane();
        
        displayManager.setVisible(false);
        mmiPlane.add(displayManager);

        // TODO: handle resizing (CR 44504)
        displayManager.setBounds(mmiPlane.getBounds());

        displayManager.addComponentListener(new ComponentAdapter()
        {
            public void componentHidden(ComponentEvent e)
            {
                mmiPlane.setVisible(false);
            }

            public void componentShown(ComponentEvent e)
            {
                mmiPlane.setVisible(true);
            }
        });
        
        // register to receive POD events
        m_podman = (PODManager) ManagerManager.getInstance(PODManager.class);
        m_podman.addPODListener(this);
    }

    public void notify(PODEvent event)
    {
        int eventId = event.getEvent();

        if (log.isDebugEnabled())
        {
            log.debug("SystemModuleRegistrarImpl PODEvent received: 0X" + Integer.toHexString(eventId));
        }

        if (eventId == PODEvent.EventID.POD_EVENT_POD_READY && podResetReceived)
        {
            podResetReceived = false;
            synchronized (hostAppID_SMHW_Map)
            {
                // Search for associated handler in hostAppId to
                // SystemModuleHandlerWrapper hashtable.
                Enumeration eh = hostAppID_SMHW_Map.elements();
                while (eh.hasMoreElements())
                {
                    final SystemModuleHandlerWrapper smhw = (SystemModuleHandlerWrapper) eh.nextElement();
                    if (smhw != null)
                    {
                        smhw.getContext().runInContext(new Runnable()
                        {
                            public void run()
                            {                              
                                // Notify the handler that the CableCARD reset
                                // Do we unregister the handler and remove it from
                                // list or wait for the app to unregister??
                                smhw.getHandler().ready(null);
                            }
                        });
                    }
                }
             }
        }
        else if (eventId == PODEvent.EventID.POD_EVENT_RESET_PENDING)
        {
            podResetReceived = true;
        }
    }
    
    protected void createResidentMmiHandler()
    {
        new ResidentMmiHandlerImpl();
    }

    protected DisplayManager createDisplayManager()
    {
        return new DisplayManagerImpl();
    }

    /**
     * This method returns a sole instance of the SystemModuleRegistrar class.
     * The SystemModuleRegistrar instance is either a singleton for each OCAP
     * application or a singleton for an entire OCAP implementation.
     * 
     * @return a singleton SystemModuleRegistrar instance.
     * @throws SecurityException
     *             if the caller does not have
     *             MonitorAppPermission("podApplication").
     */
    public static synchronized SystemModuleRegistrar getInstance() throws SecurityException
    {
        // Throws SecurityException if the caller does not have podApplication
        // permission
        checkPermission("podApplication");

        if(singleton == null)
        {
            singleton = new SystemModuleRegistrarImpl();
        }        
        return singleton;
    }

    /**
     * <p>
     * This method registeres the specified SystemModuleHandler instance for the
     * specified privateHostAppID. The Private Host Application is a logical
     * entity defined in the OpenCable CableCARD Interface Specification.
     * </p>
     * <p>
     * If there is a current Private Host Application that has a matching
     * Private Host Application ID as the privateHostAppID parameter, it shall
     * be unregistered first, i.e., corresponding SystemModule and
     * SystemModuleHandler shall be unregistered. The
     * {@link SystemModuleHandler#notifyUnregister} method of the
     * SystemModuleHandler to be unregistered shall be called to notify its
     * unregistration and give a chance to do a termination procedure. Note that
     * the OCAP implementation shall call the notifyUnregister() method in a new
     * thread to avoid blocking.
     * </p>
     * <p>
     * After the SystemModuleHandler.notifyUnregister() method returns, the OCAP
     * implementation selects an appropriate session number for sending and
     * receiving APDU. Then the OCAP implementation shall send the
     * sas_connect_rqst APDU with the session automatically. After establishing
     * the SAS connection, the OCAP implementation shall call the
     * SystemModuleHandler.ready() method with a new SystemModule instance.
     * <p>
     * </p>
     * After ready() method is called, all APDUs shall be handled by the
     * assuming OCAP-J application instead of the OCAP implementation.
     * <p>
     * </p>
     * If a native resident Private Host Application is implemented on the Host,
     * it shall has Java interface and be registered in a same manner as the
     * OCAP-J application. Only when no SystemModuleHandler that has a matching
     * Private Host Application ID is registered by the Monitor Application, the
     * OCAP implementation shall register such a native resident Private Host
     * Application. </p>
     * 
     * @param handler
     *            a SystemModuleHandler instance to receive an APDU from the
     *            CableCARD. If the handler has already been registered to the
     *            SystemModuleRegistrar, the method does nothing and throws
     *            IllegalArgumentException. Multiple call of this method with
     *            different SystemModuleHandler instance registers all of them.
     * 
     * @param privateHostAppID
     *            a Private Host Application ID for the specified handler. This
     *            value is defined as an unsigned 64-bit value in the OpenCable
     *            CableCARD Interface specification. The specified byte array
     *            shall be big endian. This value is specified as the
     *            private_host_application_id field in the sas_connect_rqst
     *            APDU.
     * 
     * @throws SecurityException
     *             if the caller does not have
     *             MonitorAppPermission("podApplication").
     * 
     * @throws IllegalStateException
     *             if the CableCARD is not ready.
     * 
     * @throws IllegalArgumentException
     *             if the specified handler already exists, or the specified
     *             parameter is out of range.
     * 
     */
    public void registerSASHandler(final SystemModuleHandler handler, final byte[] privateHostAppID)
    {
        final CallerContext newCtx = ccm.getCurrentContext();

        if (log.isInfoEnabled())
        {
            log.info("registerSASHandler()");
        }

        // Throws a SecurityException if caller does not have "podApplication"
        // permission
        checkPermission("podApplication");

        // Throw an IllegalArgumentException if the user doesn't pass in a
        // proper handler
        if (handler == null) throw new IllegalArgumentException("invalid handler input parameter");

        // Throw an IllegalArgumentException if the user doesn't pass in a
        // proper privateHostAppID
        if ((privateHostAppID == null) || (privateHostAppID.length != 8))
            throw new IllegalArgumentException("invalid privateHostAppID input parameter");

        // Throw an IllegalStateException is the CableCARD is not ready
        if (isCableCARDReady() == false) throw new IllegalStateException("CableCARD is not ready");

        // Wait for exclusive access to registration process.
        synchronized (hostAppID_SMHW_Map)
        {
            while (regInProgress == true)
            {
                try
                {
                    hostAppID_SMHW_Map.wait();
                }
                catch (InterruptedException e)
                {
                }
            }
            regInProgress = true;
        }

        // Search the installed handlers to see it this one is already
        // installed.
        Enumeration e = hostAppID_SMHW_Map.elements();
        while (e.hasMoreElements())
        {
            SystemModuleHandlerWrapper smhw = (SystemModuleHandlerWrapper) e.nextElement();

            // Throw an IllegalArgumentException if the handler already exists
            if (smhw.getHandler() == handler)
            {
                synchronized (hostAppID_SMHW_Map)
                {
                    regInProgress = false;
                    hostAppID_SMHW_Map.notifyAll();
                }
                throw new IllegalArgumentException("Handler already exists");
            }
        }

        // Convert the private host application identifier to a BigInteger
        BigInteger key = new BigInteger(privateHostAppID);

        // "If there is a current Private Host Application that has a
        // matching Private Host Application ID as the privateHostAppID
        // parameter, it shall be unregistered first"
        if (hostAppID_SMHW_Map.containsKey(key))
        {
            // Use the private host application Id to retrieve the handler
            // wrapper
            SystemModuleHandlerWrapper smhw = (SystemModuleHandlerWrapper) hostAppID_SMHW_Map.get(key);

            // Run the unregisterSASHandler() method in the old handler's
            // original context
            smhw.getContext().runInContext(new Runnable()
            {
                public void run()
                {
                    // Replace existing handler with this new one
                    unregisterSASHandlerImpl(privateHostAppID, handler, newCtx);

                    // Release hold on registration process.
                    synchronized (hostAppID_SMHW_Map)
                    {
                        regInProgress = false;
                        hostAppID_SMHW_Map.notifyAll();
                    }
                }
            });
        }
        else
        {
            // Else call the register method in the current context
            // so registerSASHandler() can return without blocking.
            newCtx.runInContext(new Runnable()
            {
                public void run()
                {
                    // Perform connect & notification.
                    // (-1) for the session Id implies open a new session
                    // connection...
                    registerSASHandlerImpl(-1, privateHostAppID, handler);

                    // Release hold on registration process.
                    synchronized (hostAppID_SMHW_Map)
                    {
                        regInProgress = false;
                        hostAppID_SMHW_Map.notifyAll();
                    }
                }
            });
        }
    }

    /**
     * <p>
     * Method that actually calls into the POD to establish an SAS session and
     * ID, stores the SystemModuleHandler/privateHostAppID/Context, creates a
     * new SystemModule, and calls the handler's ready() method.
     * </p>
     * 
     * @param existingSessionId
     *            (-1) implies make a new session connection, otherwise it's the
     *            session identifier for the pre-existing session that a new
     *            handler is taking over.
     * @param privateHostAppID
     *            Handler's Private Host App ID
     * @param handler
     *            SystemModuleHandler object
     */
    private void registerSASHandlerImpl(int existingSessionId, byte[] privateHostAppID,
                                        SystemModuleHandler handler)
    {
        // Instantiate a new SAS SystemModule (with potentially already exising
        // session).
        SASSystemModule module = new SASSystemModule(existingSessionId, handler);

        if (log.isInfoEnabled())
        {
            log.info("registerSASHandlerImpl()");
        }

        try
        {
            synchronized (hostAppID_SMHW_Map)
            {
                int sessionId;

                // Make the SAS connection to the POD application if one does
                // not already exist.
                if ((sessionId = existingSessionId) == (-1)) sessionId = module.connect(privateHostAppID);

                // Instantiate a new SAS handler.
                SystemModuleHandlerWrapper smhw = new SystemModuleHandlerWrapper(handler, ccm.getCurrentContext(),
                        sessionId, privateHostAppID);
                // Convert the private host app identifier to a key value.
                BigInteger key = new BigInteger(privateHostAppID);

                // Save the key-to-SysteModuleHandlerWrapper mapping.
                hostAppID_SMHW_Map.put(key, smhw);
            }

            // Notify the handler that the new SystemModule is ready for use.
            handler.ready(module);
        }
        catch (Exception e)
        {
            // If the session could not be established for any reason, then
            // call ready with a null SystemModule.
            handler.ready(null);
        }
    }

    /**
     * <p>
     * This private method performs the actual unregistration work of the public
     * SystemModuleRegistrar.unregisterSASHandler methods.
     * 
     * Unregister the specified SystemModuleHandler and the corresponding
     * SystemModule instance, and revives an original resident Private Host
     * Application.
     * </p>
     * <p>
     * In this method call, the {@link SystemModuleHandler#notifyUnregister}
     * method of the specified SystemModuleHandler shall be called to notify its
     * unregistration and give a chance to do a termination procedure. The
     * SystemModuleHandler and the corresponding SystemModule shall be removed
     * from the SystemModuleRegistrar after returning of the notifyUnregister()
     * method. Note that the OCAP implementation shall call the
     * notifyUnregister() method in a new thread to avoid blocking.
     * </p>
     * <p>
     * The OCAP implementation shall re-register a native resident Private Host
     * Application automatically (i.e., revive it), when no SystemModuleHandler
     * that has a matching Private Host Application ID is registered.
     * </p>
     * 
     * @param privateHostAppID
     *            is the private host application identifier for the associated
     *            SAS connection
     * @param handler
     *            is the previous handler instance if this unregister is part of
     *            a register operation that is resulting in the unregistration
     *            of a previous handler
     * @param newCtx
     *            is the caller context of the newly registering handler in case
     *            where a register is replacing an existing handler
     * 
     * @throws java.lang.SecurityException
     *             if the caller does not have
     *             MonitorAppPermission("podApplication")
     * 
     * @throws IllegalArgumentException
     *             if the specified handler has not been registered.
     * 
     */
    private void unregisterSASHandlerImpl(final byte[] privateHostAppID, final SystemModuleHandler handler,
            CallerContext newCtx)
    {
        synchronized (hostAppID_SMHW_Map)
        {
            final BigInteger key = new BigInteger(privateHostAppID);

            // Look up this privateHostAppID in the Id to
            // SystemModuleHandlerWrapper map
            if (hostAppID_SMHW_Map.containsKey(key) == false)
                throw new IllegalArgumentException("Host App ID has not been registered");

            // Call notifyUnregister() in the "handler to be unregistered"'s
            // context.
            final SystemModuleHandlerWrapper smhw = (SystemModuleHandlerWrapper) hostAppID_SMHW_Map.get(key);
            final int sessionId = smhw.getSessionId();

            smhw.getContext().runInContext(new Runnable()
            {
                public void run()
                {
                    synchronized (hostAppID_SMHW_Map)
                    {
                        // Notify the application that it is about to be
                        // unregistered
                        // Must be done in the handler's caller context
                        smhw.notifyUnregister();
                    }
                }
            });

            // If the handler is being replaced by another handler, and the
            // previous handler
            // is associated a resident developer application, maintain its
            // reference so it
            // can be automatically restored if the assuming handler goes away
            // later.
            // See "restoreResidentSASHandler()" for details.
            if ((handler != null) && isResidentDevApp() && (residentDevApps.containsKey(key) == false))
                residentDevApps.put(key, smhw);

            // Remove the module handler mapping from hostAppID_SMHW_Map
            // after notifyUnregister returns
            hostAppID_SMHW_Map.remove(key);

            // Check for the case where this unregistration operation is the
            // result of some
            // application performing a handler registration, which is replacing
            // this handler.
            // In this case the final registration of the new handler must be
            // performed after
            // the unregistration of the previous handler is complete.
            if (handler != null)
            {
                // There is a new system module handler to register
                // (unregisterSASHandler was called to remove
                // an existing handler by registerSASHandler)

                // Run in the new handler's context
                newCtx.runInContext(new Runnable()
                {
                    public void run()
                    {
                        // Re-use the pre-existing session.
                        registerSASHandlerImpl(sessionId, privateHostAppID, handler);
                    }
                });
            }
            else
            {
                // Check for previous resident dev app that has to have its
                // handler reinstalled...
                restoreResidentSASHandler(sessionId, privateHostAppID); // Re-use
                                                                        // the
                                                                        // same
                                                                        // session.
            }
        }
    }

    /**
     * <p>
     * This method unregisters the specified SystemModuleHandler and the
     * corresponding SystemModule instance, and revives an original resident
     * Private Host Application.
     * </p>
     * <p>
     * In this method call, the {@link SystemModuleHandler#notifyUnregister}
     * method of the specified SystemModuleHandler shall be called to notify its
     * unregistration and give a chance to do a termination procedure. The
     * SystemModuleHandler and the corresponding SystemModule shall be removed
     * from the SystemModuleRegistrar after returning of the notifyUnregister()
     * method. Note that the OCAP implementation shall call the
     * notifyUnregister() method in a new thread to avoid blocking.
     * </p>
     * <p>
     * The OCAP implementation shall re-register a native resident Private Host
     * Application automatically (i.e., revive it), when no SystemModuleHandler
     * that has a matching Private Host Application ID is registered.
     * </p>
     * 
     * @param handler
     *            a SystemModuleHandler instance (the Private Host Application)
     *            to be unregistered. If the specified handler has not been
     *            registered to the SystemModuleRegistrar, the method call does
     *            nothing and throws IllegalArgumentException.
     * 
     * @throws java.lang.SecurityException
     *             if the caller does not have
     *             MonitorAppPermission("podApplication")
     * 
     * @throws IllegalArgumentException
     *             if the specified handler has not been registered.
     * 
     */
    public void unregisterSASHandler(SystemModuleHandler handler)
    {
        SystemModuleHandlerWrapper smhw = null;
        byte[] hostAppId = null;

        if (log.isInfoEnabled())
        {
            log.info("unregisterSASHandler(SystemModuleHandler)");
        }

        // Throws SecurityException if the caller does not have podApplication
        // permission
        checkPermission("podApplication");

        synchronized (hostAppID_SMHW_Map)
        {
            // Search for associated handler in hostAppId to
            // SystemModuleHandlerWrapper hashtable.
            Enumeration eh = hostAppID_SMHW_Map.elements();
            while (eh.hasMoreElements())
            {
                smhw = (SystemModuleHandlerWrapper) eh.nextElement();
                if (smhw.getHandler() == handler) break; // Handler wrapper
                                                         // found.
            }

            // Verify handler was installed.
            if ((null == smhw) || (smhw.getHandler() != handler))
                throw new IllegalArgumentException("Handler has not been registered");

            // Now complete the unregistration process.
            if ((hostAppId = smhw.getHostAppID()) != null) unregisterSASHandlerImpl(hostAppId, null, null);
        }
    }

    /**
     * <p>
     * This method unregisters the SystemModuleHandler and the the SystemModule
     * instance correspoinding to the specified privateHostAppID, and revives an
     * original resident Private Host Application.
     * </p>
     * <p>
     * In this method call, the {@link SystemModuleHandler#notifyUnregister}
     * method corresponding to the specified privateHostAppID shall be called to
     * notify its unregistration and give a chance to do a termination
     * procedure. The SystemModuleHandler and the corresponding SystemModule
     * shall be removed from the SystemModuleRegistrar after returning of the
     * notifyUnregister() method. Note that the OCAP implementation shall call
     * the notifyUnregister() method in a new thread to avoid blocking.
     * </p>
     * <p>
     * The OCAP implementation shall re-register a native resident Private Host
     * Application automatically (i.e., revive it), when no SystemModuleHandler
     * that has a matching Private Host Application ID is registered.
     * </p>
     * 
     * @param privateHostAppID
     *            a Private Host Application ID of the Private Host Application
     *            (i.e., SystemModuleHandler) to be unregistered. This value is
     *            defined as an unsigned 64-bit value in the OpenCable Host-POD
     *            Interface specification. The specified byte array shall be a
     *            big endian. If the specified privateHostAppID has not been
     *            registered to the SystemModuleRegistrar, this method does
     *            nothing and throws IllegalArgumentException.
     * 
     * @throws SecurityException
     *             if the caller does not have
     *             MonitorAppPermission("podApplication").
     * 
     * @throws IllegalArgumentException
     *             if the specified privateHostAppID has not been registered.
     * 
     */
    public void unregisterSASHandler(byte[] privateHostAppID)
    {
        if (log.isInfoEnabled())
        {
            log.info("unregisterSASHandler(byte[)");
        }

        // Throws SecurityException if the caller does not have podApplication
        // permission
        checkPermission("podApplication");

        // Call common routine to finish the unregistration.
        unregisterSASHandlerImpl(privateHostAppID, null, null);
    }

    /**
     * restoreResidentSASHandler()
     * 
     * This method will check to see if there was a resident developer
     * application that had a previously installed handler for this private host
     * application identifier. If there was, then its handler will be
     * re-installed and its "ready" method invoked again.
     * 
     * @param sessionId
     *            is the session identifier association with the unregistered
     *            SAS connection.
     * @param privateHostAppID
     *            is the private host application identifier for the SAS
     *            connection.
     */
    public void restoreResidentSASHandler(int sessionId, byte[] privateHostAppID)
    {
        final BigInteger key = new BigInteger(privateHostAppID);

        if (log.isInfoEnabled())
        {
            log.info("restoreResidentSASHandler(byte[)");
        }

        // See there is was a resident developer app previously installed.
        if (residentDevApps.containsKey(key))
        {
            final SystemModuleHandlerWrapper smhw = (SystemModuleHandlerWrapper) residentDevApps.get(key);
            final SASSystemModule module = new SASSystemModule(sessionId, smhw.getHandler());

            // Re-install the resident app's handler within its application
            // context.
            // Run in the new handler's context
            smhw.getContext().runInContext(new Runnable()
            {
                public void run()
                {
                    // Re-register the handler.
                    hostAppID_SMHW_Map.put(key, smhw);

                    // Notify the handler that the new SystemModule is ready for
                    // use
                    smhw.ready(module);
                }
            });
        }
        else
        {
            // No resident application to take over, so perform any native
            // unregistration.
            podSASClose(sessionId);
        }

    }

    /**
     * <p>
     * This method registers a SystemModuleHandler instance for the assuming MMI
     * and Application Information Resource. The OCAP implementation shall call
     * the {@link org.ocap.system.SystemModuleHandler#ready} method with a new
     * SystemModule instance to send an APDU to the CableCARD.
     * </p>
     * <p>
     * The resident MMI and Application Information Resources don't terminate
     * but shall pass APDU to the SystemModuleHandler. The OCAP-J application
     * can send and receive APDUs via the
     * {@link org.ocap.system.SystemModule#sendAPDU} and the
     * {@link org.ocap.system.SystemModuleHandler#receiveAPDU} method instead of
     * the resident Resources. The sessions established by the resident MMI and
     * Application Information Resource is used to send and receive the APDU.
     * See also the description of the {@link org.ocap.system.SystemModule} and
     * the {@link org.ocap.system.SystemModuleHandler}.
     * </p>
     * <p>
     * After successful registration, the resident MMI Resource shall not
     * represent the MMI dialog on the screen. The Host shall close all resident
     * MMI dialog and finalize all transaction related to the MMI dialog. The
     * Host shall send the close_mmi_cnf APDU to the CableCARD to notify MMI
     * dialog closing. If the unregisterMMIHandler() is called or the OCAP-J
     * application that called this method changes its state to Destroyed, the
     * resident MMI Resource can represent the MMI dialog again.
     * </p>
     * 
     * @param handler
     *            a SystemModuleHandler instance to receive an APDU from
     *            CableCARD. Only one SystemModuleHandler can be registered. If
     *            the second SystemModuleHandler is to be registered, this
     *            method throws IllegalArgumentException.
     * 
     * @throws SecurityException
     *             if the caller does not have
     *             MonitorAppPermission("podApplication").
     * 
     * @throws IllegalStateException
     *             if the CableCARD is not ready.
     * 
     * @throws IllegalArgumentException
     *             if the second SystemModuleHandler is to be registered.
     * 
     */
    public void registerMMIHandler(final SystemModuleHandler handler)
    {
        if (log.isInfoEnabled())
        {
            log.info("registerMMIHandler() handler: " + handler);
        }

        // Throws SecurityException if the caller does not have podApplication
        // permission
        checkPermission("podApplication");

        if (handler == null)
        {
            throw new IllegalArgumentException("MMI handler is null...");
        }

        if (log.isInfoEnabled())
        {
            log.info("registerMMIHandler() mmiHandler: " + mmiHandler);
        }

        if (mmiHandler != null)
        {
            if (log.isInfoEnabled())
            {
                log.info("registerMMIHandler() mmiHandler.getHandler(): " + mmiHandler.getHandler());
            }

            // Return immediately (as "successful") if we've already registered
            // this MMI handler
            if (mmiHandler.getHandler() == handler)
            {
                if (log.isInfoEnabled())
                {
                    log.info("registerMMIHandler() - this handler is already registered");
                }
                return;
            }
            
            // Added for findbugs issues fix - using cached copy of residentMMIHandler
            SystemModuleHandlerWrapper l_residentMMIHandler;
            synchronized(lock)
            {
                l_residentMMIHandler = residentMMIHandler;
            }
            if (l_residentMMIHandler != null)
            {
                if (log.isInfoEnabled())
                {
                    log.info("registerMMIHandler() residentMMIHandler: " + l_residentMMIHandler
                            + "residentMMIHandler.getHandler(): " + l_residentMMIHandler.getHandler());
                }
            }

            // handler
            if ((mmiHandler.getHandler() != null) && (l_residentMMIHandler != null) && (l_residentMMIHandler.getHandler() != mmiHandler.getHandler()))
            {
                if (log.isInfoEnabled())
                {
                    log.info("registerMMIHandler() - trying to register a second MMI Handler..");
                }
                // If a handler is already registered throw exception
                // (fix for CTP test: org.ocap.system.SystemModuleRegistrar-70)
                throw new IllegalArgumentException("An MMI handler is already registered");
            }
        }

        // Throw an IllegalStateException is the CableCARD is not ready
        if (isCableCARDReady() == false) throw new IllegalStateException("The CableCARD is not ready");

        synchronized (lock)
        {
            // Install the new MMI handler
            ccm.getCurrentContext().runInContext(new Runnable()
            {
                public void run()
                {
                    registerMMIHandlerImpl(handler);
                }
            });
        }
    }

    /**
     * <p>
     * Private method that implements the inner workings of registerMMIHandler
     * </p>
     * 
     * @param handler
     *            New MMI SystemModuleHandler to be registered
     */
    protected void registerMMIHandlerImpl(SystemModuleHandler handler)
    {
        MmiSystemModule module;

        if (log.isInfoEnabled())
        {
            log.info("registerMMIHandlerImpl()");
        }

        try
        {
            synchronized (lock)
            {
                // Instantiate an MMI SystemModule for the associated MMI
                // handler.
                module = new MmiSystemModule(handler);

                // Make the connection (if necessary).
                module.connect();

                // Instantiate a SystemModuleHandlerWrapper for the MMI handler.
                mmiHandler = new SystemModuleHandlerWrapper(handler, ccm.getCurrentContext());

                // Check for initial installation of the default resident MMI
                // handler (1st one).
                if (residentMMIHandler == null)
                    residentMMIHandler = mmiHandler;
                else if (residentMMIHandler.getHandler() != handler) // Application
                                                                     // handler?
                    removeResidentMMIHandler(); // Remove the resident MMI
                                                // handler (close dialogues).
            }

            // Notify the handler that the new MMI SystemModule is ready for use
            handler.ready(module);
        }
        catch (Exception e)
        {
            // If the session could not be established for any reason, then
            // call ready with a null SystemModule.
            handler.ready(null);
        }
    }

    /**
     * <p>
     * This method unregisters the SystemModuleHandler and SystemModule instance
     * of the assuming MMI and Application Information Resource and revives the
     * resident MMI and Application Information Resource.
     * </p>
     * <p>
     * In this method call, the {@link SystemModuleHandler#notifyUnregister}
     * method of the SystemModuleHandler registered by the registerMMIHandler()
     * method shall be called to notify its unregistration and give a chance to
     * do a a termination procedure. At least, all MMI dialog shall be closed
     * and all of the transaction related to the MMI and Application Information
     * Resource shall be terminated. The OCAP-J application shall send the
     * close_mmi_cnf APDU to the CableCARD to notify MMI dialog closing. The
     * SystemModuleHandler and the corresponding SystemModule shall be removed
     * from the SystemModuleRegistrar after returning of the notifyUnregister()
     * method. I.e., after returning of the notifyUnregister() method, no APDU
     * can be sent. Note that the OCAP implementation shall call the
     * notifyUnregister() method in a new thread to avoid blocking.
     * </p>
     * <p>
     * After this method is called, the resident MMI and Application Information
     * Resource handles all APDUs and the resident MMI can represent the MMI
     * dialog again.
     * </p>
     * 
     * @throws SecurityException
     *             if the caller does not have
     *             MonitorAppPermission("podApplication")
     * 
     */
    public void unregisterMMIHandler()
    {
        if (log.isInfoEnabled())
        {
            log.info("unregisterMMIHandler()");
        }

        // Throws SecurityException if the caller does not have podApplication
        // permission
        checkPermission("podApplication");

        // Check no handler installed or the default resident handler installed
        // (don't remove)
        // Added for findbugs issues fix - moved condition inside synchronized block
        synchronized (lock)
        {
            if ((null == mmiHandler) || (residentMMIHandler == mmiHandler)) return;
            
            // Make a local copy of the currently installed MMI handler.
            final SystemModuleHandlerWrapper mmi = mmiHandler;

            // Call notifyUnregister() in the caller's context
            mmi.getContext().runInContext(new Runnable()
            {
                public void run()
                {
                    synchronized (lock)
                    {
                        // Notify the application that it is about to be
                        // unregistered
                        // Must be done in the handler's caller context
                        mmi.notifyUnregister();
                    }
                }
            });
            
            // After notifyUnregister returns, we must restore the
            // resident system MMI handler.
            restoreResidentMMIHandler();
        }
    }

    /**
     * <p>
     * Private method that restores the resident MMI SystemModuleHandler
     * </p>
     * 
     */
    // Added for findbugs issues fix
    // Added synchronization only to the required block
    public void restoreResidentMMIHandler()
    {
        if (log.isInfoEnabled())
        {
            log.info("restoreResidentMMIHandler()");
        }

        // Restore the resident MMI handler as the current MMI handler (if there
        // is one).
        synchronized(lock)
        {
            if (residentMMIHandler != null) registerMMIHandlerImpl(residentMMIHandler.getHandler());
        }
    }

    /**
     * <p>
     * Removes the resident MMI SystemModuleHandler in preparation for a
     * non-resident MMI handler
     * </p>
     * 
     */
    // Added for findbugs issues fix
    // Added synchronization only to the required block
    private void removeResidentMMIHandler()
    {
        if (log.isInfoEnabled())
        {
            log.info("removeResidentMMIHandler()");
        }

        // Call the resident MMI handler's unregistration notification method,
        // but don't remove the reference. It may need to be re-installed later.
        synchronized(lock)
        {
            if (residentMMIHandler != null) residentMMIHandler.notifyUnregister();
        }
    }

    /**
     * Dispatch a received APDU to the application that is currently connected
     * to the session.
     * 
     * @param sessionID
     *            session identifier
     * @param apdu
     *            the APDU received from the POD
     */
    public void dispatchReceivedAPDU(final PodAPDU apdu)
    {
        SystemModuleHandlerWrapper smhw = null;
        
        byte[] apdu_data = apdu.getAPDU();
        
        // Make sure this looks like an APDU.
        if (APDU_TAG != apdu_data[0]) return;

        // Check for SAS APDU type.
        if (APDU_TAG_SAS == apdu_data[1])
        {
            // Locate the handler for the target session.
            synchronized (hostAppID_SMHW_Map)
            {
                Enumeration ek = hostAppID_SMHW_Map.keys();

                // Iterate through handlers and compare session Ids.
                while (ek.hasMoreElements())
                {
                    BigInteger key = (BigInteger) ek.nextElement();
                    smhw = (SystemModuleHandlerWrapper) hostAppID_SMHW_Map.get(key);

                    if (smhw.getSessionId() == apdu.getSessionID())
                        break; // Handler found.
                    else
                        smhw = null; // Handler not found yet.
                }
            }

        }
        // MMI or Application Info APDU tag?
        else if (APDU_TAG_MMI == apdu_data[1] || APDU_TAG_APPINFO == apdu_data[1])
        {
            // Call MMI handler for MMI packets.
            smhw = mmiHandler;
            
            // If this is application_info_cnf() APDU, pass it on to
            // the PODManager so we can cache the data
            if (APDU_TAG_APPINFO_CNF == apdu_data[2])
            {
                pm.processApplicationInfoCnfAPDU(apdu.getData(), MmiSystemModule.getAppInfoResourceVersion());
            }
        }
        else
            return; // Ignore all other APDUs.

        // Call handler if there is one...
        if (smhw != null)
        {
            final SystemModuleHandler handler = smhw.getHandler();

            // Call the target handler in it's associated caller context.
            smhw.getContext().runInContext(new Runnable()
            {
                public void run()
                {
                    // Call installed handler with separate fields: apduTag,
                    // length and APDU.
                    handler.receiveAPDU(apdu.getTag(), apdu.getLength(), apdu.getData());
                }
            });
            
            if(APDU_TAG_APPINFO == apdu_data[1])
            {
                urlGetter.receiveAPDU(apdu.getTag(), apdu.getLength(), apdu.getData());
            }
        }

        return;
    }

    public DisplayManager getDisplayManager()
    {
        return displayManager;
    }
    
    /**
     * <p>
     * Private helper function that wraps SecurityManager.checkPermission()
     * </p>
     * 
     * @param value
     *            String supplied to checkPermission
     */
    private static void checkPermission(String value)
    {
        SecurityUtil.checkPermission(new MonitorAppPermission(value));
    }

    /**
     * <p>
     * Returns the status of the CableCARD
     * </p>
     * 
     * @return The CableCARD ready status
     */
    private synchronized boolean isCableCARDReady()
    {
        POD pod = POD.getInstance();

        if (pod == null) return false;

        return pod.isReady();
    }

    /**
     * removeHandler()
     * 
     * This method will remove the specified handler from its status as an
     * active APDU handler. Any previously present resident handler will be
     * restored as the active handler.
     * 
     * @param handler
     *            is the target handler wrapper to remove from the list of
     *            handlers.
     */
    public void removeHandler(SystemModuleHandlerWrapper handler)
    {
        // Check for the MMI handler, restore original MMI handler.
        if (handler == mmiHandler)
        {
            restoreResidentMMIHandler();
        }
        else
        {
            // It's an SAS handler being removed.
            synchronized (hostAppID_SMHW_Map)
            {
                // Get the associated private host application ID and convert it
                // to a key.
                BigInteger key = new BigInteger(handler.getHostAppID());
                SystemModuleHandlerWrapper smhw = (SystemModuleHandlerWrapper) hostAppID_SMHW_Map.get(key);

                // Verify it's in the list of handlers.
                if ((hostAppID_SMHW_Map.contains(key) && (smhw.getHandler() == handler)))
                {
                    // Handler found, remove it.
                    hostAppID_SMHW_Map.remove(key);

                    // If there previously was a resident handler, restore it.
                    restoreResidentSASHandler(handler.getSessionId(), handler.getHostAppID());
                }
            }
        }
    }

    /**
     * isResidentDevApp()
     * 
     * This method queries the attributes of the current application from the
     * application manager to determine if the application is a resident
     * developer application. This determination is necessary for keeping track
     * of handlers installed by resident developer applications. These handlers
     * must be re-installed whenever an assuming application's handler is
     * unregistered (i.e. it explicitly unregisters or dies).
     * 
     * @ return true if the current application is a resident developer
     * application.
     */
    private boolean isResidentDevApp()
    {
        // TODO: query application manager for attributes on the current
        // application
        // to determine if it's a resident developer application.
        return false; // Assume not for now.
    }

    /**
     * Native method for performing any native unregistration steps for an MMI
     * connection.
     */
    private native void podMMIClose();

    /**
     * Native method for performing any native unregistration steps for an SAS
     * connection.
     */
    private native void podSASClose(int sessionId);

    // Allowed APDU tags.
    private final byte APDU_TAG = (byte) 0x9F;

    private final byte APDU_TAG_MMI = (byte) 0x88;

    private final byte APDU_TAG_SAS = (byte) 0x9A;

    private final byte APDU_TAG_APPINFO = (byte) 0x80;
    
    private final byte APDU_TAG_APPINFO_CNF = (byte) 0x21;

    // The one and only SystemModuleRegistrarImpl object
    private static SystemModuleRegistrarImpl singleton = null;

    // Map HostAppID byte[] (as a BigInteger) to SystemModuleHandlerWrapper
    // objects, used by SAS.
    // Specify an initial capacity to improve performance (max SAS = 32)
    private Hashtable hostAppID_SMHW_Map = new Hashtable(16);

    // Used to maintain exclusive access to the registration process.
    private boolean regInProgress = false;

    // Hashtable used by SAS to keep track of any resident developer apps that
    // may have to have
    // their handler re-installed if an application that assumed the handler's
    // role unregisters
    // or dies.
    // Specify an initial capacity to improve performance (max SAS = 32)
    private Hashtable residentDevApps = new Hashtable(16);

    // Variables that store the non-resident MMI handler information
    protected SystemModuleHandlerWrapper mmiHandler = null;

    // The resident MMI module/module handler are never destroyed, per OCAP
    // specification
    protected SystemModuleHandlerWrapper residentMMIHandler = null;
    
    // SystemModule which handles SERVER transactions.
    private CableCardUrlGetter urlGetter = null;

    private DisplayManager displayManager = null;
    
    // Lock for synchronizing the connection.
    private Object lock = new Object();

    private static final CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
    private static final PODManager pm = (PODManager)ManagerManager.getInstance(PODManager.class);

    private static final Logger log = Logger.getLogger(SystemModuleRegistrarImpl.class.getName());

    static
    {
        org.cablelabs.impl.ocap.OcapMain.loadLibrary();
    }
}
