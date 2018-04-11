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

package org.cablelabs.impl.manager.ixc;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.tv.xlet.XletContext;
import javax.microedition.xlet.ixc.IxcRegistry;
import javax.microedition.xlet.ixc.StubException;

import org.ocap.application.OcapIxcPermission;

import org.cablelabs.impl.manager.application.XletAppContext;
import org.cablelabs.impl.manager.ApplicationManager;
import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.IxcManager;
import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.manager.ManagerManager;

import org.cablelabs.impl.util.SecurityUtil;

public class IxcManagerImpl extends IxcRegistryFactory implements IxcManager
{

    private IxcManagerImpl()
    {
        registry = new Hashtable();
        ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

        // Initialize the PBP1.1 JVM's IXC registry implementation
        IxcRegistryFactory.setFactory(this);
    }

    /**
     * Returns the singleton instance of the IxcManagerImpl.
     */
    public static Manager getInstance()
    {
        return new IxcManagerImpl();
    }

    /**
     * Implements <code>IxcRegistry.lookup()</code>.
     * 
     * @see IxcRegistry#lookup
     */
    public Remote lookup(XletContext xc, String path) throws NotBoundException, RemoteException
    {
        try
        {
            return internalLookup(xc, path);
        }
        catch (SecurityException e)
        {
            // OCAP 1.0.1 Section 14.2.2.9
            throw new NotBoundException("Failed security check for IXC lookup! Name = " + path);
        }
    }

    /**
     * Implements <code>IxcRegistry.lookup()</code> for both J2ME and DVB. From
     * DVB APIs, a failed security check causes a <code>NotBoundException</code>
     * to be thrown. In J2ME APIs, a <code>SecurityException</code> is thrown.
     * This method always throws the <code>SecurityException</code> and lets the
     * calling code decide what to do with it
     * 
     * @param xc
     *            the caller's <code>XletContext</code>
     * @param path
     *            the remote object path name
     * @return the remote object
     * @throws NotBoundException
     *             if the remote object could not be found in the registry under
     *             the given name
     * @throws RemoteException
     *             if a remote instance to the object could not be created
     */
    private Remote internalLookup(XletContext xc, String name) throws NotBoundException, RemoteException
    {
        if (name == null)
        {
            throw new NullPointerException("null path");
        }

        // check that the path is correctly formatted
        // should be of the form /organisation_id/application_id/name
        if ((name = verifyPath(name)) == null)
        {
            throw new IllegalArgumentException("path not formatted correctly");
        }

        // lookup the path in the registry
        Entry value = (Entry) registry.get(name);

        // If we did not find an entry, let the caller know
        if (value == null) throw new NotBoundException();

        // Check that this app has permission to lookup the remote object
        // REMEMBER -- registry entries are only keyed by their unique lookup
        // path (/orgID/appID/bindname), but the entry also stores information
        // about the serviceID and signed/unsigned state of that application
        // that bound it. We use this extra scoping information in the entry
        // to check the permission
        SecurityUtil.checkPermission(new OcapIxcPermission(value.getScope() + name, "lookup"));

        // get the "client" classloader
        ApplicationManager appManager = (ApplicationManager) ManagerManager.getInstance(ApplicationManager.class);
        ClassLoader classLoader = appManager.getAppClassLoader(ccm.getCurrentContext());
        return createRemoteReference(value.getRemoteObject(), value.getCallerContext(), classLoader);
    }

    public Remote createRemoteReference(final Remote object, final CallerContext source,
                                        final ClassLoader targetLoader)
            throws RemoteException
    {
        try
        {
            return (Remote)AccessController.doPrivileged(new PrivilegedExceptionAction() {
                public Object run()
                    throws Exception
                {
                    ProxyHandler handler = new ProxyHandler(object, source);
            
                    // get all the Remote interfaces from the bound object
                    Class classArray[] = getRemoteClasses(object.getClass());
            
                    Class clientClasses[] = new Class[classArray.length];
                    // load all the classes in the correct ClassLoader
                    for (int i = 0; i < clientClasses.length; i++)
                    {
                        try
                        {
                            clientClasses[i] = targetLoader.loadClass(classArray[i].getName());
                        }
                        catch (ClassNotFoundException e)
                        {
                            throw new RemoteException("class not found");
                        }
                    }
                    // create the proxy instance
                    return Proxy.newProxyInstance(targetLoader, clientClasses, handler);
                }
            });
        }
        catch (PrivilegedActionException e1)
        {
            Throwable e = e1.getCause();
            if (e instanceof RemoteException)
                throw (RemoteException)e;
        }
        
        return null;
    }

    /**
     * Implements <code>IxcRegistry.bind()</code>.
     * 
     * @see IxcRegistry#bind
     */
    public void bind(XletContext xc, String name, Remote obj) throws AlreadyBoundException
    {
        try
        {
            internalBind(xc, name, obj, null, false);
        }
        catch (SecurityException e)
        {
            // OCAP 1.0.1 Section 14.2.2.9
            return;
        }
    }

    /**
     * Implements <code>IxcRegistry.unbind()</code>.
     * 
     * @see IxcRegistry#unbind
     */
    public void unbind(XletContext xc, String name) throws NotBoundException
    {
        if (xc == null) throw new NullPointerException("XletContext is null");

        if (name == null) throw new NullPointerException("name is null");

        // Create a registry key (lookup name) based on the orgID/appID of the
        // given XletContext and the given name.
        String lookupName = SEPARATOR_CHAR + (String) xc.getXletProperty("dvb.org.id") + SEPARATOR_CHAR
                + (String) xc.getXletProperty("dvb.app.id") + SEPARATOR_CHAR + name;

        if ((lookupName = verifyPath(lookupName)) == null)
        {
            throw new NotBoundException();
        }

        // try to remove the key and object from the hashtable. If
        // null is returned, then the key had no mapping.
        if (registry.remove(lookupName) == null)
        {
            throw new NotBoundException();
        }
    }

    /**
     * Implements <code>IxcRegistry.rebind()</code>.
     * 
     * @see IxcRegistry#rebind
     */
    public void rebind(XletContext xc, String name, Remote obj)
    {
        try
        {
            internalBind(xc, name, obj, null, true);
        }
        catch (AlreadyBoundException e)
        { /* Will never be thrown */
        }
        catch (SecurityException e)
        {
            // OCAP 1.0.1 Section 14.2.2.9
            return;
        }
    }

    /**
     * All calls to bind() and rebind() are implemented by this method. Based on
     * its boolean parameter (allowRebind), it simply determines whether or not
     * to throw an exception if a remote object is already bound under the given
     * name.
     * 
     * @param xc
     *            the XletContext that will provide the AppID and OrgID under
     *            which to bind the remote object
     * @param name
     *            the name under which to bind the remote object
     * @param obj
     *            the remote object to be boung
     * @param scope
     *            if not null, this bind is being called from J2ME registry APIs
     *            and the scoping prefix is provided separate from the name. If
     *            null this bind is being called from the DVB registry APIs and
     *            we must construct the scoping prefix from information in the
     *            calling xlet's context
     * @param allowRebind
     *            false if an exception should be thrown if a remote object is
     *            already bound under the given name, true if the given remote
     *            object may be rebound under an already existing name
     * @throws AlreadyBoundException
     *             if <i>allowRebind</i> is true and a remote object is already
     *             bound under the given name
     * @throws SecurityException
     *             if the caller does not have permission to bind the given name
     */
    private void internalBind(XletContext xc, String name, Remote obj, String scope, boolean allowRebind)
            throws AlreadyBoundException
    {
        if (xc == null) throw new NullPointerException("XletContext is null");

        if (name == null) throw new NullPointerException("name is null");

        if (obj == null) throw new NullPointerException("Remote object is null");

        // Create an Entry for this object
        CallerContext context = ccm.getCurrentContext();
        Entry entry;
        if (scope != null)
            entry = new Entry(obj, scope, context);
        else
            entry = new Entry(obj, xc, context);

        // Create a registry key (lookup name) based on the orgID/appID of the
        // given XletContext and the given bind name.
        String key = SEPARATOR_CHAR + (String) xc.getXletProperty("dvb.org.id") + SEPARATOR_CHAR
                + (String) xc.getXletProperty("dvb.app.id") + SEPARATOR_CHAR + name;

        // Do we have permission to bind this object
        SecurityUtil.checkPermission(new OcapIxcPermission(entry.getScope() + key, "bind"));

        synchronized (registry)
        {
            // does the key already exist?
            if (!allowRebind && registry.containsKey(key)) throw new AlreadyBoundException();

            // add the key/value pair
            registry.put(key, entry);
            context.addCallbackData(new Callback(), this);
        }
    }

    /**
     * Create a scope string based on the calling context and the given
     * XletContext. String is formatted like:
     * "/service-<i>service-id</i>/[unsigned|signed]"
     * 
     * @param xc
     *            the XletContext that provides signed/unsigned state
     * @return the scope string
     */
    private String createScope(XletContext xc)
    {
        String scope = "";

        // Service ID
        CallerContext cc = ccm.getCurrentContext();
        Long scId = (Long) cc.get(CallerContext.SERVICE_CONTEXT_ID);
        scope = "/service-" + scId.longValue();

        // Signed/Unsigned
        int appId = Integer.parseInt((String) xc.getXletProperty("dvb.app.id"), 16);
        if (appId < 0x4000)
            scope = scope + "/unsigned";
        else
            scope = scope + "/signed";

        return scope;
    }

    /**
     * Implements <code>IxcRegistry.list()</code>.
     * 
     * @see IxcRegistry#list
     */
    public String[] list(XletContext xc)
    {
        Vector validKeys = new Vector();

        // get the list of keys and return them as an array of strings
        synchronized (registry)
        {
            Set keys = registry.keySet();
            for (Iterator it = keys.iterator(); it.hasNext();)
            {
                String key = (String) it.next();
                Entry e = (Entry) registry.get(key);

                // Remove any keys that this application does not have
                // permission to lookup OR throw an exception
                if (SecurityUtil.hasPermission(new OcapIxcPermission(e.getScope() + key, "lookup")))
                {
                    validKeys.add(key);
                }
            }
        }

        // Create the array of remote object lookup names
        String[] result = (String[]) validKeys.toArray(new String[] {});
        return result;
    }

    public void destroy()
    {
        registry.clear();
    }

    /**
     * remove any registry entries made by the exiting application
     * 
     * @param context
     *            the application that is exiting.
     */
    public void cleanup(CallerContext context)
    {
        // given the CallerContext, remove all registry entries bound by the
        // app.
        for (Enumeration e = registry.keys(); e.hasMoreElements(); /* blank */)
        {
            String key = (String) e.nextElement();
            Entry entry = (Entry) registry.get(key);
            if (entry.callerContext == context)
            {
                registry.remove(key);
            }
        }
    }

    /**
     * Verifies that the return type, method parameters, and the throws clause
     * of each method in the <code>Class</code> object parameter is compliant
     * with MHP section 11.7.3.1.1.
     * 
     * <ul>
     * <li>Each method must declare java.rmi.RemoteException in its throws
     * clause, in addition to any application-specific exceptions.
     * <li>A remote object passed by remote reference as an argument or return
     * value must be declared as an interface that extends java.rmi.Remote, and
     * not as an application class that implements this remote interface.
     * <li>The type of each method argument must either be a remote interface, a
     * class or interface that implements java.io.Serializable, or a primitive
     * type.
     * <li>Each return value must either be a remote interface, a class or
     * interface that implements java.io.Serializable, a primitive type, or
     * void.
     * </ul>
     * 
     * @param c
     *            the Class object to verify.
     * @throws RemoteException
     *             if any of the rules in MHP 11.7.3.1.1 is broken
     */
    private static void verifyMethods(Class c) throws RemoteException
    {
        Method methods[] = c.getMethods();
        for (int i = 0; i < methods.length; i++)
        {
            Class exceptionTypes[] = methods[i].getExceptionTypes();
            Class returnType = methods[i].getReturnType();
            Class parameterTypes[] = methods[i].getParameterTypes();
            boolean found = false;
            int j;

            // check the return type of the method
            if (!((returnType.isInterface() && Remote.class.isAssignableFrom(returnType))
                    || Serializable.class.isAssignableFrom(returnType) || returnType.isPrimitive() || returnType.equals(Void.TYPE)))
            {
                throw new RemoteException("return type is not Remote, Serializable, or primitive for method "
                        + methods[i]);
            }

            // check that RemoteException is in the throws clause
            for (j = 0; j < exceptionTypes.length; j++)
            {
                if (RemoteException.class.equals(exceptionTypes[j]))
                {
                    found = true;
                    break;
                }
            }

            if (!found)
            {
                throw new RemoteException("method " + methods[i] + " not declared to throw a RemoteException");
            }

            // check the paramter types
            for (j = 0; j < parameterTypes.length; j++)
            {
                if (!((parameterTypes[j].isInterface() && Remote.class.isAssignableFrom(parameterTypes[j]))
                        || Serializable.class.isAssignableFrom(parameterTypes[j]) || parameterTypes[j].isPrimitive()))
                {
                    throw new RemoteException("parameter " + (j + 1) + " for method " + methods[i]
                            + " is not Remote, Serializable, or primitive.");
                }
            }
        }
    }

    /**
     * method that returns an array of <code>Class</code> objects that extend
     * <code>java.rmi.Remote</code>.
     * 
     * @param object
     *            object that implements <code>Remote</code> interfaces
     * @return array of <code>Remote</code> interfaces
     * @throws RemoteException
     */
    static Class[] getRemoteClasses(Class object) throws RemoteException
    {
        Vector list = new Vector();
        Class clss = object;

        while (clss != null)
        {
            if (Remote.class.isAssignableFrom(clss))
            {
                if (clss.isInterface())
                {
                    // will throw a RemoteException if anything isn't correct
                    verifyMethods(clss);
                    // add this interface to a list of classes
                    if (!list.contains(clss)) list.add(clss);
                }
                else
                {
                    Class intf[] = clss.getInterfaces();
                    for (int i = 0; i < intf.length; i++)
                    {
                        // does the interface extend java.rmi.Remote?
                        if (Remote.class.isAssignableFrom(intf[i]))
                        {
                            // will throw a RemoteException if anything isn't
                            // correct
                            verifyMethods(intf[i]);
                            // add this interface to the list of classes
                            if (!list.contains(intf[i])) list.add(intf[i]);
                        }
                    }
                }
            }
            clss = clss.getSuperclass();
        }
        if (list.isEmpty())
        {
            throw new RemoteException("No classes extend Remote");
        }

        return (Class[]) list.toArray(new Class[0]);
    }

    /*
     * [ETSI TS 101 812 V1.3.1 (2003-06)] (MHP 1.0.3) Annex Y - IxcRegistry
     * Description
     * 
     * The identification of a remote object is given using a syntax indicating
     * the organisation ID and application ID:
     * /organisation_id/application_id/name organisation_id = the organisation
     * ID of the Xlet, as signalled in the application_identifier record,
     * defined in the MHP specification. application_id = the application ID of
     * the Xlet, as signalled in the application_identifier record, defined in
     * the MHP specification. name = the name under which the remote object was
     * exported.
     */
    private String verifyPath(String path)
    {
        // path must begin with '/'
        if (path.charAt(0) != SEPARATOR_CHAR)
        {
            return null;
        }

        StringTokenizer tokenizer = new StringTokenizer(path, SEPARATOR_STRING, false);

        // check that the path has the minimum number of tokens (orgid, appid, &
        // name)
        if (tokenizer.countTokens() < 3)
        {
            return null;
        }

        String str;
        StringBuffer buffer = new StringBuffer();

        // validate & cannonicalize the /organisation_id/application_id/ portion
        // of the path
        for (int i = 0; i < 2; i++)
        {
            str = tokenizer.nextToken();

            /*
             * [ETSI TS 101 812 V1.3.1 (2003-06)] (MHP 1.0.3) Annex Y -
             * IxcRegistry Description
             * 
             * The organisation ID and the application ID shall each be encoded
             * as a hexadecimal string, as would be accepted by
             * java.lang.Integer.parseInt(String s, 16).
             */
            try
            {
                /*
                 * This cannonicalizes the orgid & appid to correctly match the
                 * Hashtable keys (see createKey implementation).
                 */
                str = Integer.toHexString(Integer.parseInt(str, 16));
            }
            catch (NumberFormatException e)
            {
                return null;
            }

            buffer.append(SEPARATOR_STRING + str);
        }

        /*
         * [ETSI TS 101 812 V1.3.1 (2003-06)] (MHP 1.0.3) Annex Y -
         * bind(XletContext, String, Remote)
         * 
         * The name can be any valid nonnull String. No hierarchical namespace
         * exists, e.g. the names "foo" and "bar/../foo" are distinct.
         */
        str = tokenizer.nextToken(""); // get the remaining chars, starting with
                                       // the '/' after application_id
        buffer.append(str);

        return buffer.toString();
    }

    /**
     * Hashtable to keep track of <key, Remote> pairs
     */
    private Hashtable registry;

    /**
     * reference to the caller context manager
     */
    private CallerContextManager ccm;

    /**
     * Separator string and character used in IXC pathnames
     */
    final private String SEPARATOR_STRING = "/";

    final private char SEPARATOR_CHAR = SEPARATOR_STRING.charAt(0);

    /**
     * Instances of this class represent an entry in the IXCRegistry. Maintains
     * references to the bound <code>Remote</code> object, to the current
     * <code>XletContext</code> and to the current <code>CallerContext</code>.
     */
    private class Entry
    {
        /**
         * Create a new registry entry, computing the objects scope from the
         * XletContext
         * 
         * @param obj
         *            the remote object to be stored in the registry
         * @param xc
         *            the XletContext that will be used to compute this object's
         *            scope
         * @param cc
         *            the CallerContext to which this entry should be
         *            associated. The death of this CallerContext will signal
         *            the removal of this entry
         */
        Entry(Remote obj, XletContext xc, CallerContext cc)
        {
            real = obj;
            callerContext = cc;
            scope = createScope(xc);
        }

        /**
         * Create a new registry entry, computing the objects scope from the
         * XletContext
         * 
         * @param obj
         *            the remote object to be stored in the registry
         * @param scope
         *            the scoping prefix to be assigned to this object
         * @param cc
         *            the CallerContext to which this entry should be
         *            associated. The death of this CallerContext will signal
         *            the removal of this entry
         */
        Entry(Remote obj, String scope, CallerContext cc)
        {
            real = obj;
            callerContext = cc;
            this.scope = scope;
        }

        /**
         * Retrieve the <code>CallerContext</code> associated with this registry
         * entry.
         * 
         * @return the CallerContext object when the bind() call was made.
         */
        CallerContext getCallerContext()
        {
            return callerContext;
        }

        /**
         * Retrieve the <code>Remote</code> object associated with the registry
         * entry.
         * 
         * @return the Remote object that was passed to the bind() call.
         */
        Remote getRemoteObject()
        {
            return real;
        }

        /**
         * Returns a string representing the the IXC scope of this bound object.
         * The scope string is formatted like this:
         * <p>
         * "/service-{service-id}/[unsigned|signed]"
         * <p>
         * "/ixc/[unsigned|signed]"
         * <p>
         * "/global/service-{service-id}/[unsigned|signed]"
         * 
         * @return the IXC scope formatted as a string
         */
        String getScope()
        {
            return scope;
        }

        private Remote real;

        private CallerContext callerContext;

        private String scope;
    }

    /**
     * clears the reference to the Remote object when the exporting context is
     * destroyed.
     */
    private class Callback implements CallbackData
    {
        public void destroy(CallerContext cc)
        {
            cleanup(cc);
        }

        public void active(CallerContext cc)
        {
        }

        public void pause(CallerContext cc)
        {
        }

    }

    /**
     * Our IxcRegistryFactory implementation. Returns new instances of
     * JMEIxcRegistry based on the given XletContext
     * 
     * @param xc
     *            the XletContext
     * @return a new IxcRegistry based on the given XletContext
     */
    public IxcRegistry getIxcRegistry(javax.microedition.xlet.XletContext xc)
    {
        return new JMEIxcRegistry(xc);
    }

    /**
     * Convert a fully-qualified JME object URL to its OCAP (MHP) counterpart,
     * but split into two pieces: its scoping prefix and its name.
     * <p>
     * Scoping prefix is:
     * "/[ixc|global|service-<i>service-id</i>]/[signed|unsigned]"
     * <p>
     * Name is: "/<i>oid</i>/<i>aid</i>/bindname
     * 
     * This method assumes that a legal JME scope is passed in
     * 
     * @param jmeName
     *            the JME URL to be converted
     * @return the fully-qualified OCAP object name with the scoping prefix at
     *         index 0, and the name at index 1
     */
    String[] convertJMEURLToOCAPName(String jmeURL)
    {
        String[] retVal = new String[2];
        StringBuffer ocapScope = new StringBuffer();
        StringTokenizer tokens = new StringTokenizer(jmeURL, SEPARATOR_STRING, false);

        // Skip "dvb:" token
        String token = tokens.nextToken();
        token = tokens.nextToken();

        // IXC access
        if (token.equals("ixc"))
        {
            ocapScope.append("/ixc");
        }
        // Service access
        else if (token.equals("service"))
        {
            // Service ID
            token = tokens.nextToken();
            ocapScope.append("/service-" + token);

            // Signed/Unsigned
            token = tokens.nextToken();
            ocapScope.append(token);
        }
        // Global access
        else
        {
            token = tokens.nextToken();
            ocapScope.append("/global/" + token);
        }
        retVal[0] = ocapScope.toString();

        // Re-construct the remaining name
        StringBuffer name = new StringBuffer();
        while (tokens.hasMoreElements())
        {
            token = tokens.nextToken();
            name.append("/" + token);
        }

        return retVal;
    }

    /**
     * Convert a fully-qualified OCAP object name to its J2ME URL counterpart,
     * but split into two pieces: its scoping prefix and its name.
     * <p>
     * Scoping prefix is: "dvb:/ixc" OR "dvb:/[signed|unsigned]" OR
     * "dvb:/service/<i>service-id</i>/[signed|unsigned]"
     * <p>
     * Name is: "/<i>oid</i>/<i>aid</i>/bindname
     * 
     * This method assumes that a legal OCAP name is passed in
     * 
     * @param ocapName
     *            the OCAP object name to be converted
     * @return the fully-qualified JME URL with the scoping prefix at index 0,
     *         and the name at index 1
     */
    String[] convertOCAPNameToJMEURL(String ocapName)
    {
        String[] retVal = new String[2];
        StringBuffer jmeScope = new StringBuffer("dvb:");
        StringTokenizer tokens = new StringTokenizer(ocapName, SEPARATOR_STRING, false);

        String token = tokens.nextToken();

        // IXC access
        if (token.equals("ixc"))
        {
            jmeScope.append("/ixc");
        }
        // Service access
        else if (token.startsWith("service-"))
        {
            // Service context ID
            int dashIndex = token.indexOf('-');
            jmeScope.append("/" + token.substring(0, dashIndex));
            jmeScope.append("/" + token.substring(dashIndex + 1));

            // Signed
            token = tokens.nextToken();
            jmeScope.append("/" + token);
        }
        // Global access
        else if (token.equals("global"))
        {
            // Signed
            token = tokens.nextToken();
            jmeScope.append("/" + token);
        }
        retVal[0] = jmeScope.toString();

        // Re-construct the remaining name
        StringBuffer name = new StringBuffer();
        while (tokens.hasMoreElements())
        {
            token = tokens.nextToken();
            name.append("/" + token);
        }

        return retVal;
    }

    /**
     * Implementation of the JME IxcRegistry. Instances of this class are
     * created by our IxcRegistryFactory that we install into the JVM
     * 
     * @author Greg Rutz
     */
    private class JMEIxcRegistry extends javax.microedition.xlet.ixc.IxcRegistry
    {
        /**
         * Construct a new IxcRegistry based on the given XletContext
         * 
         * @param xc
         *            the XletContext
         */
        public JMEIxcRegistry(javax.microedition.xlet.XletContext xc)
        {
            this.xc = xc;
        }

        // From javax.microedition.xlet.ixc.IxcRegistry
        public void bind(String name, Remote obj) throws StubException, AlreadyBoundException
        {
            String parsedName = parseJMEURL(name);
            if (parsedName == null) return;

            String[] ocapName = convertJMEURLToOCAPName(parsedName);
            internalBind((XletAppContext) xc, ocapName[1], obj, ocapName[0], false);
        }

        // From javax.microedition.xlet.ixc.IxcRegistry
        public String[] list()
        {
            String[] list = org.dvb.io.ixc.IxcRegistry.list((XletAppContext) xc);
            String[] retList = new String[list.length];

            // Convert OCAP names to J2ME names
            for (int i = 0; i < list.length; ++i)
            {
                Entry e = (Entry) registry.get(list[i]);

                // Combine the scoping prefix and name and convert to
                // JME URL
                String[] url = convertOCAPNameToJMEURL(e.getScope() + list[i]);
                retList[i] = url[0] + url[1];
            }

            return retList;
        }

        // From javax.microedition.xlet.ixc.IxcRegistry
        public Remote lookup(String name) throws StubException, NotBoundException
        {
            String parsedName = parseJMEURL(name);
            if (parsedName == null) throw new NotBoundException("Invalid J2ME remote object URL");

            String[] ocapName = convertJMEURLToOCAPName(parsedName);
            try
            {
                return internalLookup((XletAppContext) xc, ocapName[1]);
            }
            catch (RemoteException e)
            {
                throw new StubException(e.getMessage());
            }
        }

        // From javax.microedition.xlet.ixc.IxcRegistry
        public void rebind(String name, Remote obj) throws StubException, AccessException
        {
            String parsedName = parseJMEURL(name);
            if (parsedName == null) return;

            String[] ocapName = convertJMEURLToOCAPName(parsedName);
            try
            {
                internalBind((XletAppContext) xc, ocapName[1], obj, ocapName[0], true);
            }
            catch (AlreadyBoundException e)
            { /* Will never be thrown */
            }
        }

        // From javax.microedition.xlet.ixc.IxcRegistry
        public void unbind(String name) throws NotBoundException, AccessException
        {
            String parsedName = parseJMEURL(name);
            if (parsedName == null) return;

            String[] ocapName = convertJMEURLToOCAPName(parsedName);
            org.dvb.io.ixc.IxcRegistry.unbind((XletAppContext) xc, ocapName[1]);
        }

        // From javax.microedition.xlet.ixc.IxcRegistry
        public void unbindAll()
        {
            cleanup(ccm.getCurrentContext());
        }

        /**
         * Parses a JME IxcRegistry URL that would be passed into either bind(),
         * rebind(), or lookup(), and returns a validated, conforming name in
         * its place. The following grammar defines a valid name format:
         * 
         * <pre>
         * <i>NAME</i>          = "dvb:/" <i>SCOPE_PREFIX</i> "/" <i>OID</i> "/" <i>AID</i> "/" <i>BINDNAME</i>
         * <i>SCOPE_PREFIX</i>  = "ixc" | <i>SIGNED</i> | <i>SERVICE</i> "/" <i>SIGNED</i>
         * <i>SIGNED</i>        = "signed" | "unsigned"
         * <i>SERVICE</i>       = "service" "/" <i>service-context-id</i>
         * <i>OID</i>           = <i>oid</i>
         * <i>AID</i>           = <i>aid</i>
         * <i>BINDNAME</i>      = <i>bindname</i>
         * </pre>
         * 
         * @param url
         *            the JME URL
         * @return a validated, conforming J2ME remoet object URL. Returns null
         *         if the url did not represent a valid name as described by the
         *         grammar.
         */
        private String parseJMEURL(String url)
        {
            // Must start with "dvb:/"
            if (!url.startsWith("dvb:/")) return null;

            StringBuffer retVal = new StringBuffer("dvb:/");

            // Strip the "dvb:/" and tokenize the remaining string
            String strippedURL = url.substring("dvb:/".length());
            StringTokenizer tokens = new StringTokenizer(strippedURL, SEPARATOR_STRING, false);

            // Parse the scoping prefix
            try
            {
                String token = tokens.nextToken();

                // Global access (signed)
                if (token.equals("signed"))
                {
                    retVal.append(token);
                    retVal.append("/");
                }
                // Global access (unsigned)
                else if (token.equals("unsigned"))
                {
                    retVal.append(token);
                    retVal.append("/");
                }
                // Service access
                else if (token.equals("service"))
                {
                    retVal.append(token);
                    retVal.append("/");

                    // Grab service ID
                    token = tokens.nextToken();
                    retVal.append(token);
                    retVal.append("/");

                    // Grab signed/unsigned token
                    token = tokens.nextToken();
                    if (token.equals("signed"))
                    {
                        retVal.append(token);
                    }
                    else if (token.equals("unsigned"))
                    {
                        retVal.append(token);
                    }
                    else
                    {
                        return null;
                    }
                }
                // IXC Access
                else if (token.equals("ixc"))
                {
                    retVal.append(token);
                }
                else
                {
                    return null;
                }
            }
            catch (NoSuchElementException e)
            {
                return null;
            }

            // Re-construct the remaining name and then verify it for
            // correctness
            StringBuffer name = new StringBuffer("/");
            while (tokens.hasMoreElements())
            {
                String token = tokens.nextToken();
                name.append(token);
                name.append("/");
            }

            // Strip the final "/" character and verify that the remaining name
            // is a valid OID/AID/bindname name
            String remainingName = name.toString();
            String verifiedName = verifyPath(remainingName.substring(0, remainingName.length() - 1));
            if (verifiedName == null) return null;

            retVal.append(verifiedName);
            return retVal.toString();
        }

        private javax.microedition.xlet.XletContext xc;
    }
}
