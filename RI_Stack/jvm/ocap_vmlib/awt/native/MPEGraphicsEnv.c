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

#include "awt.h"
#include "common.h"

#include <mpe_gfx.h>
#include <mpe_types.h>
#include <mpe_error.h>
#include <mpe_os.h>
#include <mpe_disp.h>

#include <jni.h>
#include <java_awt_MPEGraphicsEnvironment.h>
#include <java_awt_MPEGraphicsDevice.h>

/* #include "org_cablelabs_impl_awt_MPEGraphicsConfig.h" */

#include <java_awt_AWTEvent.h>
#include <java_awt_event_InputEvent.h>
#include <java_awt_event_MouseEvent.h>
#include <java_awt_event_KeyEvent.h>

/* local functions */

/** Contains cached JNI IDs. */
struct CachedIDs MPECachedIDs;

JNIEXPORT void JNICALL Java_java_awt_MPEGraphicsEnvironment_init
(JNIEnv * env, jclass cls)
{
    GET_CLASS (AWTError, "java/awt/AWTError");

    if ((*env)->GetVersion (env) == 0x10001)
    {
        (*env)->ThrowNew (env, MPECachedIDs.AWTError, "Requires at least version 1.2 JNI");
        return;
    }

    /* Cache commonly used JNI IDs. */
    GET_CLASS (OutOfMemoryError, "java/lang/OutOfMemoryError");
    GET_CLASS (NullPointerException, "java/lang/NullPointerException");

    GET_CLASS (Thread, "java/lang/Thread");
    GET_STATIC_METHOD_ID (java_lang_Thread_interruptedMID, "interrupted", "()Z");

    GET_CLASS (DirectColorModel, "java/awt/image/DirectColorModel");
    GET_METHOD_ID (DirectColorModel_constructor, "<init>", "(IIIII)V");

    GET_CLASS (IndexColorModel, "java/awt/image/IndexColorModel");
    GET_METHOD_ID (IndexColorModel_constructor, "<init>", "(II[B[B[B)V");

    GET_CLASS (java_awt_image_BufferedImage, "java/awt/image/BufferedImage");
    GET_METHOD_ID (java_awt_image_BufferedImage_constructor, "<init>", "(Lsun/awt/image/BufferedImagePeer;)V");
    GET_FIELD_ID (java_awt_image_BufferedImage_peerFID, "peer", "Lsun/awt/image/BufferedImagePeer;");

    GET_CLASS (MPEToolkit, "java/awt/MPEToolkit");
    GET_STATIC_METHOD_ID (MPEToolkit_postKeyEventMID, "postKeyEvent", "(Ljava/awt/Component;IIIC)V");
}

JNIEXPORT void JNICALL Java_java_awt_MPEGraphicsEnvironment_destroy
(JNIEnv *env, jobject this)
{
    /*
     * !!!! We don't have such a call in MPE GFX...
     * but we could add one...?
     */
    UNUSED(env);
    UNUSED(this);
}

/* The single window that will be the target of all events. */
static jobject eventWindow = NULL;

/**
 * Called when a window has been attatched to a GraphicsDevice.
 * This window will then receive events sent to that device.
 * As MPE only supports one screen device at the moment we store
 * these in global variables.
 * In a multi screen environment these would be more appropriately stored
 * in a hashtable of some kind.
 */
JNIEXPORT void JNICALL Java_java_awt_MPEGraphicsEnvironment_pSetWindow
(JNIEnv * env, jobject this, jint screen, jobject window)
{
    UNUSED(this);
    UNUSED(screen); /* only one screen... */
    eventWindow = (*env)->NewGlobalRef (env, window);
}

JNIEXPORT jint JNICALL Java_java_awt_MPEGraphicsEnvironment_pGetScreenCount
(JNIEnv *env, jobject this)
{
    uint32_t nScreens;
    mpe_Error err;
    UNUSED(this);

    err = mpe_dispGetScreenCount(&nScreens);
    if (err != MPE_SUCCESS)
    {
        (*env)->ThrowNew(env, MPECachedIDs.AWTError, "Error encountered getting number of screens!");
        return nScreens;
    }
    return nScreens;
}

JNIEXPORT void JNICALL Java_java_awt_MPEGraphicsEnvironment_pGetScreens
(JNIEnv *env, jobject this, jintArray scrns)
{
    mpe_Error err;
    jint *scrptr; /* pointer to screens */
    UNUSED(this);

    if ( NULL == (scrptr = (*env)->GetIntArrayElements(env, scrns, NULL)) )
    return; // exception thrown

    err = mpe_dispGetScreens((mpe_DispScreen*)scrptr);

    /* copyback copy if necessary */
    (*env)->ReleaseIntArrayElements(env, scrns, scrptr, 0);

    if (err != MPE_SUCCESS)
    {
        (*env)->ThrowNew(env, MPECachedIDs.AWTError, "Error encountered getting screens!");
    }
}

JNIEXPORT jint JNICALL Java_java_awt_MPEGraphicsEnvironment_pGetDeviceCount
(JNIEnv *env, jobject this, jint screen)
{
    uint32_t nDevices;
    mpe_Error err;
    UNUSED(this);

    /* Only one screen and only graphics devices */
    err = mpe_dispGetDeviceCount((mpe_DispScreen)screen,
            MPE_DISPLAY_GRAPHICS_DEVICE, &nDevices);
    if (err != MPE_SUCCESS)
    {
        (*env)->ThrowNew(env, MPECachedIDs.AWTError, "Error encountered getting Device Count!");
        return(jint)nDevices;
    }
    return nDevices;
}

/* Get devices for particular screen */
JNIEXPORT void JNICALL Java_java_awt_MPEGraphicsEnvironment_pGetDevices
(JNIEnv *env, jobject this, jint screen, jintArray devices)
{
    mpe_Error err;
    jint *devptr; /* int pointers */
    UNUSED(this);

    /* get actual array or a copyback copy*/
    devptr = (*env)->GetIntArrayElements(env, devices, NULL);

    err = mpe_dispGetDevices((mpe_DispScreen)screen,
            MPE_DISPLAY_GRAPHICS_DEVICE,
            (mpe_DispDevice*)devptr);

    /* copyback the copy if necessary */
    (*env)->ReleaseIntArrayElements(env, devices, devptr, 0);

    if (err != MPE_SUCCESS)
    {
        (*env)->ThrowNew(env, MPECachedIDs.AWTError, "Error getting devices!");
    }
}

/** Shifted versions of VK_0-VK_9. */
static const jint shifted_numbers[] =
{ ')', '!', '@', '#', '$', '%', '^', '&', '*', '(', };
/** Shifted versions of VK_OPEN_BRACKET-VK_CLOSE_BRACKET. */
static const jint shifted_bracket[] =
{ '{', '|', '}', };
/** Shifted versions of VK_COMMA-VK_SLASH. */
static const jint shifted_comma[] =
{ '<', '_', '>', '?', };
/** shifted versions of VK_NUMPAD0-VK_NUMPAD9. */
static const jint numpad[] =
{ '*', '+', java_awt_event_KeyEvent_CHAR_UNDEFINED, '-', '.', '/', };
/** Shifted versions of VK_SEMICOLON-VK_EQUALS. (Also includes '<'/'>'.) */
static const jint shifted_semicolon[] =
{ ':', '<', '+', '>', };

/**
 * Maps the given virtual keycode to the resulting character.
 *
 * @param eventCode the virtual key code (e.g., <code>VK_A</code>)
 * @param eventMod the event modifiers mask (e.g., <code>SHIFT_MASK|ALT_MASK</code>)
 * @param capsLock the caps lock mask (either <code>0</code> or <code>SHIFT_MASK</code>)
 *
 * @return the keyChar for the given inputs or <code>CHAR_UNDEFINED</code>
 */
static jint map2KeyChar(int32_t eventCode, int32_t eventMod, int32_t capsLock)
{
    jint eventChar;
    int shifted = (eventMod & java_awt_event_InputEvent_SHIFT_MASK);

    switch (eventCode)
    {
    default:
        eventChar = java_awt_event_KeyEvent_CHAR_UNDEFINED;
        break;
    case java_awt_event_KeyEvent_VK_A:
    case java_awt_event_KeyEvent_VK_B:
    case java_awt_event_KeyEvent_VK_C:
    case java_awt_event_KeyEvent_VK_D:
    case java_awt_event_KeyEvent_VK_E:
    case java_awt_event_KeyEvent_VK_F:
    case java_awt_event_KeyEvent_VK_G:
    case java_awt_event_KeyEvent_VK_H:
    case java_awt_event_KeyEvent_VK_I:
    case java_awt_event_KeyEvent_VK_J:
    case java_awt_event_KeyEvent_VK_K:
    case java_awt_event_KeyEvent_VK_L:
    case java_awt_event_KeyEvent_VK_M:
    case java_awt_event_KeyEvent_VK_N:
    case java_awt_event_KeyEvent_VK_O:
    case java_awt_event_KeyEvent_VK_P:
    case java_awt_event_KeyEvent_VK_Q:
    case java_awt_event_KeyEvent_VK_R:
    case java_awt_event_KeyEvent_VK_S:
    case java_awt_event_KeyEvent_VK_T:
    case java_awt_event_KeyEvent_VK_U:
    case java_awt_event_KeyEvent_VK_V:
    case java_awt_event_KeyEvent_VK_W:
    case java_awt_event_KeyEvent_VK_X:
    case java_awt_event_KeyEvent_VK_Y:
    case java_awt_event_KeyEvent_VK_Z:
        eventChar = (shifted ^ capsLock) ? eventCode : tolower(eventCode);
        break;
    case java_awt_event_KeyEvent_VK_0:
    case java_awt_event_KeyEvent_VK_1:
    case java_awt_event_KeyEvent_VK_2:
    case java_awt_event_KeyEvent_VK_3:
    case java_awt_event_KeyEvent_VK_4:
    case java_awt_event_KeyEvent_VK_5:
    case java_awt_event_KeyEvent_VK_6:
    case java_awt_event_KeyEvent_VK_7:
    case java_awt_event_KeyEvent_VK_8:
    case java_awt_event_KeyEvent_VK_9:
        eventChar = shifted ? shifted_numbers[eventCode
                - java_awt_event_KeyEvent_VK_0] : eventCode;
        break;
    case java_awt_event_KeyEvent_VK_OPEN_BRACKET:
    case java_awt_event_KeyEvent_VK_BACK_SLASH:
    case java_awt_event_KeyEvent_VK_CLOSE_BRACKET:
        eventChar = shifted ? shifted_bracket[eventCode
                - java_awt_event_KeyEvent_VK_OPEN_BRACKET] : eventCode;
        break;
    case java_awt_event_KeyEvent_VK_NUMPAD0:
    case java_awt_event_KeyEvent_VK_NUMPAD1:
    case java_awt_event_KeyEvent_VK_NUMPAD2:
    case java_awt_event_KeyEvent_VK_NUMPAD3:
    case java_awt_event_KeyEvent_VK_NUMPAD4:
    case java_awt_event_KeyEvent_VK_NUMPAD5:
    case java_awt_event_KeyEvent_VK_NUMPAD6:
    case java_awt_event_KeyEvent_VK_NUMPAD7:
    case java_awt_event_KeyEvent_VK_NUMPAD8:
    case java_awt_event_KeyEvent_VK_NUMPAD9:
        eventChar = (eventCode - java_awt_event_KeyEvent_VK_NUMPAD0)
                + java_awt_event_KeyEvent_VK_0;
        break;
    case java_awt_event_KeyEvent_VK_MULTIPLY:
    case java_awt_event_KeyEvent_VK_ADD:
    case java_awt_event_KeyEvent_VK_SUBTRACT:
    case java_awt_event_KeyEvent_VK_DECIMAL:
    case java_awt_event_KeyEvent_VK_DIVIDE:
        eventChar = numpad[eventCode - java_awt_event_KeyEvent_VK_MULTIPLY];
        break;
    case java_awt_event_KeyEvent_VK_BACK_SPACE:
    case java_awt_event_KeyEvent_VK_TAB:
    case java_awt_event_KeyEvent_VK_ENTER:
    case java_awt_event_KeyEvent_VK_SPACE:
        eventChar = eventCode;
        break;
    case java_awt_event_KeyEvent_VK_COMMA:
    case java_awt_event_KeyEvent_VK_MINUS: // Note: not in JDK 1.1.8
    case java_awt_event_KeyEvent_VK_PERIOD:
    case java_awt_event_KeyEvent_VK_SLASH:
        eventChar = shifted ? shifted_comma[eventCode
                - java_awt_event_KeyEvent_VK_COMMA] : eventCode;
        break;
    case java_awt_event_KeyEvent_VK_SEMICOLON:
    case java_awt_event_KeyEvent_VK_EQUALS:
        eventChar = shifted ? shifted_semicolon[eventCode
                - java_awt_event_KeyEvent_VK_SEMICOLON] : eventCode;
        break;
    case java_awt_event_KeyEvent_VK_QUOTE:
        eventChar = shifted ? '"' : '\'';
        break;
    case java_awt_event_KeyEvent_VK_BACK_QUOTE:
        eventChar = shifted ? '~' : '`';
        break;

    }
    return eventChar;
}

static void processKeyEvent(JNIEnv *env, jobject window, mpe_GfxEvent *event)
{
    jint id;
    jint eventChar;
    static jint eventModifiers = 0;
    static jint capsLock = 0;

    MPEAWT_TIME_INIT();

    if (window == NULL)
    {
        MPE_LOG(MPEAWT_LOGEVENT, MPEAWT_LOG_MOD,
                "MPEGraphicsEnv::processKeyEvent() - DROPPED\n");
        return;
    }

    /* Update event modifiers */
    if (event->eventId == OCAP_KEY_PRESSED)
    {
        if (event->eventCode == java_awt_event_KeyEvent_VK_SHIFT)
            eventModifiers |= java_awt_event_InputEvent_SHIFT_MASK;

        else if (event->eventCode == java_awt_event_KeyEvent_VK_CONTROL)
            eventModifiers |= java_awt_event_InputEvent_CTRL_MASK;

        else if (event->eventCode == java_awt_event_KeyEvent_VK_ALT)
            eventModifiers |= java_awt_event_InputEvent_ALT_MASK;
    }
    else
    {
        if (event->eventCode == java_awt_event_KeyEvent_VK_SHIFT)
            eventModifiers &= ~java_awt_event_InputEvent_SHIFT_MASK;

        else if (event->eventCode == java_awt_event_KeyEvent_VK_CONTROL)
            eventModifiers &= ~java_awt_event_InputEvent_CTRL_MASK;

        else if (event->eventCode == java_awt_event_KeyEvent_VK_ALT)
            eventModifiers &= ~java_awt_event_InputEvent_ALT_MASK;

        else if (event->eventCode == java_awt_event_KeyEvent_VK_CAPS_LOCK)
            capsLock ^= java_awt_event_InputEvent_SHIFT_MASK;
    }

    id
            = (event->eventId == OCAP_KEY_RELEASED) ? java_awt_event_KeyEvent_KEY_RELEASED
                    : java_awt_event_KeyEvent_KEY_PRESSED;

    /* Generate eventChar if MPE doesn't do it. */
    eventChar = event->eventChar;
    if (eventChar == OCAP_CHAR_UNKNOWN)
        eventChar = map2KeyChar(event->eventCode, eventModifiers, capsLock);
    else if (eventChar == OCAP_CHAR_UNDEFINED)
        eventChar = java_awt_event_KeyEvent_CHAR_UNDEFINED;

    /* Post key typed event to Java if this was a printable character and the key was released. */
    if (event->eventId == OCAP_KEY_RELEASED && eventChar
            != java_awt_event_KeyEvent_CHAR_UNDEFINED && eventChar != 0)
    {
        MPE_LOG(MPEAWT_LOGEVENT, MPEAWT_LOG_MOD,
                "MPEGraphicsEnv::processKeyEvent(%d,%d,%04x)\n",
                java_awt_event_KeyEvent_KEY_TYPED, event->eventCode,
                java_awt_event_KeyEvent_VK_UNDEFINED);
        MPEAWT_TIME_START();
        (*env)->CallStaticVoidMethod(env, MPECachedIDs.MPEToolkit,
                MPECachedIDs.MPEToolkit_postKeyEventMID, window,
                java_awt_event_KeyEvent_KEY_TYPED, eventModifiers,
                java_awt_event_KeyEvent_VK_UNDEFINED, eventChar);
        MPEAWT_TIME_END();
#if MPEAWT_DBGTIME
        MPE_LOG( MPEAWT_LOGTIME, MPEAWT_LOG_MOD, "MPEGraphicsEnv::processKeyEvent(TYPED) elapsed %d ms\n", time_elapsed );
#endif
    }

    /* Post key pressed or released event to Java. */
    MPE_LOG(MPEAWT_LOGEVENT, MPEAWT_LOG_MOD,
            "MPEGraphicsEnv::processKeyEvent(%d,%d,%04x)\n", id,
            event->eventCode, eventChar);
    MPEAWT_TIME_START();
    (*env)->CallStaticVoidMethod(env, MPECachedIDs.MPEToolkit,
            MPECachedIDs.MPEToolkit_postKeyEventMID, window, id,
            eventModifiers, event->eventCode, eventChar);
    MPEAWT_TIME_END();
#if MPEAWT_DBGTIME
    MPE_LOG( MPEAWT_LOGTIME, MPEAWT_LOG_MOD, "MPEGraphicsEnv::processKeyEvent() elapsed %d ms\n", time_elapsed );
#endif
}

JNIEXPORT void JNICALL Java_java_awt_MPEGraphicsEnvironment_run(JNIEnv * env, jobject this)
{
#if 1
#define TIMEOUT 5000*10 /* Allow timeout and check for interrupted */
#else
#define TIMEOUT MPE_GFX_WAIT_INFINITE
#endif
    while (JNI_FALSE
            == (*env)->CallStaticBooleanMethod(env,
                    MPECachedIDs.Thread,
                    MPECachedIDs.java_lang_Thread_interruptedMID))
    {
        mpe_GfxEvent event;
        mpe_Error err;

        event.eventChar = OCAP_CHAR_UNKNOWN; // in case MPE doesn't fill it in
        if ( MPE_SUCCESS == (err = mpe_gfxWaitNextEvent(&event, TIMEOUT)) )
        {
            processKeyEvent(env, eventWindow, &event);
        }
        else if (err != MPE_ETIMEOUT)
        {
            (*env)->ThrowNew (env, MPECachedIDs.AWTError, "Error encountered receiving events!");
        }
    }
}
