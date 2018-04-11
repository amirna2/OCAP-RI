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

#include <errno.h>
#include <strings.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>

#include "jvm.h"
#include "jni_util.h"
#include "net_util.h"

/************************************************************************
 * NetworkInterface
 */

#include "java_net_NetworkInterface.h"

/************************************************************************
 * NetworkInterface
 */
jclass ni_class;
jfieldID ni_nameID;
jfieldID ni_indexID;
jfieldID ni_descID;
jfieldID ni_addrsID;
jmethodID ni_ctrID;

static jclass ni_iacls;
static jclass ni_ia4cls;
static jclass ni_ia6cls;
static jmethodID ni_ia4ctrID;
static jmethodID ni_ia6ctrID;
static jfieldID ni_iaaddressID;
static jfieldID ni_iafamilyID;
static jfieldID ni_ia6ipaddressID;

static jobject createNetworkInterface(JNIEnv *env, mpe_SocketNetIfList *ifs);

static mpe_SocketNetIfList *enumInterfaces(JNIEnv *env);

/*
 * Class:     java_net_NetworkInterface
 * Method:    init
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_java_net_NetworkInterface_init(JNIEnv *env, jclass cls)
{
    init_IPv6Available(env);
    ni_class = (*env)->FindClass(env,"java/net/NetworkInterface");
    ni_class = (*env)->NewGlobalRef(env, ni_class);
    ni_nameID = (*env)->GetFieldID(env, ni_class,"name", "Ljava/lang/String;");
    ni_indexID = (*env)->GetFieldID(env, ni_class, "index", "I");
    ni_addrsID = (*env)->GetFieldID(env, ni_class, "addrs", "[Ljava/net/InetAddress;");
    ni_descID = (*env)->GetFieldID(env, ni_class, "displayName", "Ljava/lang/String;");
    ni_ctrID = (*env)->GetMethodID(env, ni_class, "<init>", "()V");

    ni_iacls = (*env)->FindClass(env, "java/net/InetAddress");
    ni_iacls = (*env)->NewGlobalRef(env, ni_iacls);
    ni_ia4cls = (*env)->FindClass(env, "java/net/Inet4Address");
    ni_ia4cls = (*env)->NewGlobalRef(env, ni_ia4cls);
    ni_ia6cls = (*env)->FindClass(env, "java/net/Inet6Address");
    ni_ia6cls = (*env)->NewGlobalRef(env, ni_ia6cls);
    ni_ia4ctrID = (*env)->GetMethodID(env, ni_ia4cls, "<init>", "()V");
    ni_ia6ctrID = (*env)->GetMethodID(env, ni_ia6cls, "<init>", "()V");
    ni_iaaddressID = (*env)->GetFieldID(env, ni_iacls, "address", "I");
    ni_iafamilyID = (*env)->GetFieldID(env, ni_iacls, "family", "I");
    ni_ia6ipaddressID = (*env)->GetFieldID(env, ni_ia6cls, "ipaddress", "[B");
}

/*
 * Class:     java_net_NetworkInterface
 * Method:    getByName0
 * Signature: (Ljava/lang/String;)Ljava/net/NetworkInterface;
 */
JNIEXPORT jobject JNICALL
Java_java_net_NetworkInterface_getByName0(JNIEnv *env, jclass cls, jstring name)
{
    mpe_SocketNetIfList *ifs, *curr;
    jboolean isCopy;
    const char* name_utf = (*env)->GetStringUTFChars(env, name, &isCopy);
    jobject obj = NULL;

    if ( (ifs = enumInterfaces(env)) == NULL )
    return NULL;

    /*
     * Search the list of interface based on name
     */
    for ( curr = ifs; curr != NULL; curr = curr->if_next)
    {
        if (strcmp(name_utf, curr->if_name) == 0)
        break;
    }

    /* if found create a NetworkInterface */
    if (curr != NULL)
    obj = createNetworkInterface(env, curr);

    /* release the UTF string and interface list */
    (*env)->ReleaseStringUTFChars(env, name, name_utf);

    mpe_socketFreeInterfaces(ifs);
    return obj;
}

/*
 * Class:     java_net_NetworkInterface
 * Method:    getByIndex
 * Signature: (Ljava/lang/String;)Ljava/net/NetworkInterface;
 */
JNIEXPORT jobject JNICALL
Java_java_net_NetworkInterface_getByIndex(JNIEnv *env, jclass cls, jint index)
{
    mpe_SocketNetIfList *ifs, *curr;
    jobject obj = NULL;

    if (index <= 0)
    return NULL;

    if ( (ifs = enumInterfaces(env)) == NULL )
    return NULL;

    /*
     * Search the list of interface based on index
     */
    for ( curr = ifs; curr != NULL; curr = curr->if_next )
    {
        if (index == curr->if_index)
        break;
    }

    /* if found create a NetworkInterface */
    if (curr != NULL)
    obj = createNetworkInterface(env, curr);

    mpe_socketFreeInterfaces(ifs);
    return obj;
}

/*
 * Class:     java_net_NetworkInterface
 * Method:    getByInetAddress0
 * Signature: (Ljava/net/InetAddress;)Ljava/net/NetworkInterface;
 */
JNIEXPORT jobject JNICALL
Java_java_net_NetworkInterface_getByInetAddress0(JNIEnv *env, jclass cls, jobject iaObj)
{
    mpe_SocketNetIfList *ifs, *curr;
    int family = (*env)->GetIntField(env, iaObj, ni_iafamilyID) == IPv6 ?
                                     MPE_SOCKET_AF_INET6 : MPE_SOCKET_AF_INET4;
    jobject obj = NULL;
    jboolean match = JNI_FALSE;

    if ( (ifs = enumInterfaces(env)) == NULL )
    return NULL;

    for ( curr = ifs; (curr != NULL) && (match == JNI_FALSE); curr = curr->if_next )
    {
        mpe_SocketNetAddr *addrP;

        /*
         * Iterate through each address on the interface
         */
        for ( addrP = curr->if_addresses; (addrP != NULL) && (match == JNI_FALSE); addrP = addrP->if_next)
        {
            if (family == addrP->if_family)
            {
                if (family == MPE_SOCKET_AF_INET4)
                {
                    int address1 = mpe_socketHtoNL(((mpe_SocketIPv4SockAddr*)addrP->if_addr)->sin_addr.s_addr);
                    int address2 = (*env)->GetIntField(env, iaObj, ni_iaaddressID);

                    if (address1 == address2)
                    {
                        match = JNI_TRUE;
                        break;
                    }
                }

                if (family == MPE_SOCKET_AF_INET6)
                {
                    jbyte *bytes = (jbyte *)&(((mpe_SocketIPv6SockAddr*)addrP->if_addr)->sin6_addr);
                    jbyteArray ipaddress = (*env)->GetObjectField(env, iaObj, ni_ia6ipaddressID);
                    jbyte caddr[16];
                    int i;

                    (*env)->GetByteArrayRegion(env, ipaddress, 0, 16, caddr);
                    i = 0;
                    while (i < 16)
                    {
                        if (caddr[i] != bytes[i])
                        break;
                        i++;
                    }
                    if (i >= 16)
                    {
                        match = JNI_TRUE;
                        break;
                    }
                }
            }
        }
        if (match)
        break;
    }

    /* if found create a NetworkInterface */
    if (match)
    obj = createNetworkInterface(env, curr);

    mpe_socketFreeInterfaces(ifs);
    return obj;
}

/*
 * Class:     java_net_NetworkInterface
 * Method:    getAll
 * Signature: ()[Ljava/net/NetworkInterface;
 */
JNIEXPORT jobjectArray JNICALL
Java_java_net_NetworkInterface_getAll(JNIEnv *env, jclass cls)
{
    mpe_SocketNetIfList *ifs, *curr;
    jobjectArray netIFArr;
    jint arr_index, ifCount;

    if ( (ifs = enumInterfaces(env)) == NULL )
    return NULL;

    /* count the interface */
    ifCount = 0;
    for ( curr = ifs; curr != NULL; curr = curr->if_next )
    ifCount++;

    /* allocate a NetworkInterface array */
    if ( (netIFArr = (*env)->NewObjectArray(env, ifCount, cls, NULL)) == NULL )
    {
        mpe_socketFreeInterfaces(ifs);
        return NULL;
    }

    /*
     * Iterate through the interfaces, create a NetworkInterface instance
     * for each array element and populate the object.
     */
    arr_index = 0;
    for ( curr = ifs; curr != NULL; curr = curr->if_next )
    {
        jobject netifObj;

        if ( (netifObj = createNetworkInterface(env, curr)) == NULL )
        {
            mpe_socketFreeInterfaces(ifs);
            return NULL;
        }

        /* put the NetworkInterface into the array */
        (*env)->SetObjectArrayElement(env, netIFArr, arr_index++, netifObj);
    }

    mpe_socketFreeInterfaces(ifs);
    return netIFArr;
}

/*
 * Create a NetworkInterface object, populate the name and index, and
 * populate the InetAddress array based on the IP addresses for this
 * interface.
 */
jobject createNetworkInterface(JNIEnv *env, mpe_SocketNetIfList *ifs)
{
    jobject netifObj;
    jobject name;
    jobjectArray addrArr;
    jint addr_index, addr_count;
    mpe_SocketNetAddr *addrP;

    /*
     * Create a NetworkInterface object and populate it
     */
    netifObj = (*env)->NewObject(env, ni_class, ni_ctrID);
    name = (*env)->NewStringUTF(env, ifs->if_name);
    if (netifObj == NULL || name == NULL)
        return NULL;

    (*env)->SetObjectField(env, netifObj, ni_nameID, name);
    (*env)->SetObjectField(env, netifObj, ni_descID, name);
    (*env)->SetIntField(env, netifObj, ni_indexID, ifs->if_index);

    /* Count the number of address on this interface */
    addr_count = 0;
    for (addrP = ifs->if_addresses; addrP != NULL; addrP = addrP->if_next)
        addr_count++;

    /* Create the array of InetAddresses */
    if ((addrArr = (*env)->NewObjectArray(env, addr_count, ni_iacls, NULL))
            == NULL)
        return NULL;

    addr_index = 0;
    for (addrP = ifs->if_addresses; addrP != NULL; addrP = addrP->if_next)
    {
        jobject iaObj = NULL;

        if (addrP->if_family == MPE_SOCKET_AF_INET4)
        {
            iaObj = (*env)->NewObject(env, ni_ia4cls, ni_ia4ctrID);
            if (iaObj)
            {
                (*env)->SetIntField(
                        env,
                        iaObj,
                        ni_iaaddressID,
                        mpe_socketHtoNL(
                                ((mpe_SocketIPv4SockAddr*) addrP->if_addr)->sin_addr.s_addr));
            }
        }

        if (addrP->if_family == MPE_SOCKET_AF_INET6)
        {
            iaObj = (*env)->NewObject(env, ni_ia6cls, ni_ia6ctrID);
            if (iaObj)
            {
                jbyteArray ipaddress = (*env)->NewByteArray(env, 16);
                if (ipaddress == NULL)
                return NULL;

                (*env)->SetByteArrayRegion(env, ipaddress, 0, 16,
                        (jbyte *)&(((mpe_SocketIPv6SockAddr*)addrP->if_addr)->sin6_addr));
                (*env)->SetObjectField(env, iaObj, ni_ia6ipaddressID, ipaddress);
            }
        }

        if (iaObj == NULL)
            return NULL;

        (*env)->SetObjectArrayElement(env, addrArr, addr_index++, iaObj);
    }

    (*env)->SetObjectField(env, netifObj, ni_addrsID, addrArr);

    /* return the NetworkInterface */
    return netifObj;
}

/* 
 * Enumerates all interfaces
 */
static mpe_SocketNetIfList *enumInterfaces(JNIEnv *env)
{
    mpe_SocketNetIfList *interfaces;
    mpe_Error ec;

    /* Acquire the interface list. */
    if ((ec = mpe_socketGetInterfaces(&interfaces)) != MPE_SUCCESS)
    {
        if (ec == MPE_ENOMEM)
        {
            JNU_ThrowOutOfMemoryError(env, "heap allocation failed");
        }
        else
        {
            NET_ThrowNew(env, 0, "interface acquisition failed");
        }
        return NULL;
    }

    return interfaces;
}
