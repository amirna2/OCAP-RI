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

#include <org_ocap_media_ClosedCaptioningAttribute.h>
#include <mgrdef.h>
#include <mpe_caption.h>
#include <mpe_dbg.h>
#include <mpe_gfx.h>
#include <jvmmgr.h>
#include <jni_util.h>

#include <string.h>

#define MPE_MEM_DEFAULT MPE_MEM_CC

// We statically allocate the memory used to hold our capability
// returns so that we don't have to allocate/reallocate memory
static mpe_CcColor* colorCaps[MPE_CC_COLOR_MAX];
static uint32_t* opacityCaps[MPE_CC_OPACITY_MAX];
static mpe_CcFontStyle* fontStyleCaps[MPE_CC_FONT_STYLE_MAX];
static uint32_t* fontSizeCaps[MPE_CC_FONT_SIZE_MAX];
static uint32_t* textStyleCaps[2];
static uint32_t* borderTypeCaps[MPE_CC_BORDER_TYPE_MAX];
static mpe_Mutex capsMutex;

/*
 * Class:     org_ocap_media_ClosedCaptioningAttribute
 * Method:    init
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_ocap_media_ClosedCaptioningAttribute_init
(JNIEnv *env, jclass clazz)
{
    int i;
    jclass cls = NULL;
    
    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(clazz);

    GET_CLASS(ClosedCaptioningInteger, "java/lang/Integer");
    GET_METHOD_ID(ClosedCaptioningInteger_init, "<init>", "(I)V");
    GET_CLASS(ClosedCaptioningMpeColor, "org/ocap/media/ClosedCaptioningAttribute$MpeColor");
    GET_METHOD_ID(ClosedCaptioningMpeColor_init,
                  "<init>", "(Lorg/ocap/media/ClosedCaptioningAttribute;ILjava/lang/String;)V");

    // Color caps
    for (i = 0; i < MPE_CC_COLOR_MAX; i++)
    {
        if (mpe_memAlloc(sizeof(mpe_CcColor), (void**)&colorCaps[i]) != MPE_SUCCESS)
        {
            goto nomem;
        }
    }

    // Opacity caps
    for (i = 0; i < MPE_CC_OPACITY_MAX; i++)
    {
        if (mpe_memAlloc(sizeof(uint32_t), (void**)&opacityCaps[i]) != MPE_SUCCESS)
        {
            goto nomem;
        }
    }

    // FontStyle caps
    for (i = 0; i < MPE_CC_FONT_STYLE_MAX; i++)
    {
        if (mpe_memAlloc(sizeof(mpe_CcFontStyle), (void**)&fontStyleCaps[i]) != MPE_SUCCESS)
        {
            goto nomem;
        }
    }

    // FontSize caps
    for (i = 0; i < MPE_CC_FONT_SIZE_MAX; i++)
    {
        if (mpe_memAlloc(sizeof(uint32_t), (void**)&fontSizeCaps[i]) != MPE_SUCCESS)
        {
            goto nomem;
        }
    }

    // TextStyle caps
    for (i = 0; i < 2; i++)
    {
        if (mpe_memAlloc(sizeof(uint32_t), (void**)&textStyleCaps[i]) != MPE_SUCCESS)
        {
            goto nomem;
        }
    }

    // BorderType caps
    for (i = 0; i < MPE_CC_BORDER_TYPE_MAX; i++)
    {
        if (mpe_memAlloc(sizeof(uint32_t), (void**)&borderTypeCaps[i]) != MPE_SUCCESS)
        {
            goto nomem;
        }
    }

    if (mpe_mutexNew(&capsMutex) != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_FATAL, MPE_MOD_JNI, "%s: Could not allocate mutex\n", __FUNCTION__);
    }

    return;
    
 nomem:
    MPE_LOG(MPE_LOG_FATAL, MPE_MOD_JNI, "%s: Could not allocate caps memory\n", __FUNCTION__);
}

//translate routines from mpe to JAVA
static mpe_CcOpacity translateOpacityToJava(mpe_CcOpacity  mpeOpacity)
{
    int java_opacity = MPE_CC_OPACITY_SOLID;
    switch (mpeOpacity)
    {
    case MPE_CC_OPACITY_SOLID:
        java_opacity = org_ocap_media_ClosedCaptioningAttribute_CC_OPACITY_SOLID;
        break;
    case MPE_CC_OPACITY_FLASHING:
        java_opacity = org_ocap_media_ClosedCaptioningAttribute_CC_OPACITY_FLASH;
        break;
    case MPE_CC_OPACITY_TRANSPARENT:
        java_opacity = org_ocap_media_ClosedCaptioningAttribute_CC_OPACITY_TRANSPARENT;
        break;
    case MPE_CC_OPACITY_TRANSLUCENT:
        java_opacity = org_ocap_media_ClosedCaptioningAttribute_CC_OPACITY_TRANSLUCENT;
        break;
    default:
        break;
    }
    return java_opacity;
}

static mpe_CcType toNativeCCType(jint ccType)
{
    return ccType == org_ocap_media_ClosedCaptioningAttribute_CC_TYPE_ANALOG ?
        MPE_CC_TYPE_ANALOG : MPE_CC_TYPE_DIGITAL;
}

static mpe_CcAttribType toNativeAttribute(jint attrib)
{
    mpe_CcAttribType type = MPE_CC_ATTRIB_FONT_OPACITY;

    switch (attrib)
    {
    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_PEN_FG_OPACITY:
        type = MPE_CC_ATTRIB_FONT_OPACITY;
        break;
    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_PEN_BG_OPACITY:
        type = MPE_CC_ATTRIB_BACKGROUND_OPACITY;
        break;
    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_WINDOW_FILL_OPACITY:
        type = MPE_CC_ATTRIB_WIN_OPACITY;
        break;
    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_FONT_ITALICIZED:
        type = MPE_CC_ATTRIB_FONT_ITALIC;
        break;
    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_FONT_UNDERLINE:
        type = MPE_CC_ATTRIB_FONT_UNDERLINE;
        break;
    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_PEN_SIZE:
        type = MPE_CC_ATTRIB_FONT_SIZE;
        break;
    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_WINDOW_BORDER_TYPE:
        type = MPE_CC_ATTRIB_BORDER_TYPE;
        break;
    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_PEN_FG_COLOR:
        type = MPE_CC_ATTRIB_FONT_COLOR;
        break;
    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_PEN_BG_COLOR:
        type = MPE_CC_ATTRIB_BACKGROUND_COLOR;
        break;
    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_WINDOW_FILL_COLOR:
        type = MPE_CC_ATTRIB_WIN_COLOR;
        break;
    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_WINDOW_BORDER_COLOR:
        type = MPE_CC_ATTRIB_BORDER_COLOR;
        break;
    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_FONT_STYLE:
        type = MPE_CC_ATTRIB_FONT_STYLE;
        break;
    }
    return type;
}

static mpe_CcOpacity translateOpacity(JNIEnv *env, jint opacity)
{
    mpe_CcOpacity mpe_opacity = MPE_CC_OPACITY_SOLID;

    MPE_UNUSED_PARAM(env);

    switch (opacity)
    {
    case org_ocap_media_ClosedCaptioningAttribute_CC_OPACITY_SOLID:
        mpe_opacity = MPE_CC_OPACITY_SOLID;
        break;
    case org_ocap_media_ClosedCaptioningAttribute_CC_OPACITY_FLASH:
        mpe_opacity = MPE_CC_OPACITY_FLASHING;
        break;
    case org_ocap_media_ClosedCaptioningAttribute_CC_OPACITY_TRANSPARENT:
        mpe_opacity = MPE_CC_OPACITY_TRANSPARENT;
        break;
    case org_ocap_media_ClosedCaptioningAttribute_CC_OPACITY_TRANSLUCENT:
        mpe_opacity = MPE_CC_OPACITY_TRANSLUCENT;
        break;
    default:
        break;
    }

    return mpe_opacity;
}

static mpe_CcFontSize translateSize(JNIEnv *env, jint size)
{
    mpe_CcOpacity mpe_size = MPE_CC_FONT_SIZE_STANDARD;

    MPE_UNUSED_PARAM(env);

    switch (size)
    {
    case org_ocap_media_ClosedCaptioningAttribute_CC_PEN_SIZE_SMALL:
        mpe_size = MPE_CC_FONT_SIZE_SMALL;
        break;
    case org_ocap_media_ClosedCaptioningAttribute_CC_PEN_SIZE_STANDARD:
        mpe_size = MPE_CC_FONT_SIZE_STANDARD;
        break;
    case org_ocap_media_ClosedCaptioningAttribute_CC_PEN_SIZE_LARGE:
        mpe_size = MPE_CC_FONT_SIZE_LARGE;
        break;
    default:
        break;
    }

    return mpe_size;
}

static mpe_CcBorderType translateBorderType(JNIEnv *env, jint border)
{
    mpe_CcBorderType mpe_border = MPE_CC_BORDER_TYPE_NONE;

    MPE_UNUSED_PARAM(env);

    switch (border)
    {
    case org_ocap_media_ClosedCaptioningAttribute_CC_BORDER_NONE:
        mpe_border = MPE_CC_BORDER_TYPE_NONE;
        break;
    case org_ocap_media_ClosedCaptioningAttribute_CC_BORDER_RAISED:
        mpe_border = MPE_CC_BORDER_TYPE_RAISED;
        break;
    case org_ocap_media_ClosedCaptioningAttribute_CC_BORDER_DEPRESSED:
        mpe_border = MPE_CC_BORDER_TYPE_DEPRESSED;
        break;
    case org_ocap_media_ClosedCaptioningAttribute_CC_BORDER_UNIFORM:
        mpe_border = MPE_CC_BORDER_TYPE_UNIFORM;
        break;
    case org_ocap_media_ClosedCaptioningAttribute_CC_BORDER_SHADOW_LEFT:
        mpe_border = MPE_CC_BORDER_TYPE_SHADOW_LEFT;
        break;
    case org_ocap_media_ClosedCaptioningAttribute_CC_BORDER_SHADOW_RIGHT:
        mpe_border = MPE_CC_BORDER_TYPE_SHADOW_RIGHT;
        break;
    default:
        break;
    }

    return mpe_border;
}

/*
 * Class:     org_ocap_media_ClosedCaptioningAttribute
 * Method:    nativeSetCCEmbeddedValue
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_org_ocap_media_ClosedCaptioningAttribute_nativeSetCCEmbeddedValue
(JNIEnv *env, jobject obj, jint attrib, jint ccType)
{
    mpe_CcAttributes ccAttrib;

    MPE_UNUSED_PARAM(obj);
    MPE_UNUSED_PARAM(env);

    switch (attrib)
    {
    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_PEN_FG_COLOR:
        ccAttrib.charFgColor.rgb = MPE_CC_EMBEDDED_COLOR;
        break;

    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_PEN_BG_COLOR:
        ccAttrib.charBgColor.rgb = MPE_CC_EMBEDDED_COLOR;
        break;

    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_WINDOW_FILL_COLOR:
        ccAttrib.winColor.rgb = MPE_CC_EMBEDDED_COLOR;
        break;

    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_WINDOW_BORDER_COLOR:
        ccAttrib.borderColor.rgb = MPE_CC_EMBEDDED_COLOR;
        break;

    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_PEN_FG_OPACITY:
        ccAttrib.charFgOpacity = MPE_CC_OPACITY_EMBEDDED;
        break;

    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_PEN_BG_OPACITY:
        ccAttrib.charBgOpacity = MPE_CC_OPACITY_EMBEDDED;
        break;

    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_WINDOW_FILL_OPACITY:
        ccAttrib.winOpacity = MPE_CC_OPACITY_EMBEDDED;
        break;

    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_FONT_ITALICIZED:
        ccAttrib.fontItalic = MPE_CC_TEXT_STYLE_EMBEDDED_TEXT;
        break;

    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_FONT_UNDERLINE:
        ccAttrib.fontUnderline = MPE_CC_TEXT_STYLE_EMBEDDED_TEXT;
        break;

    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_FONT_STYLE:
        strncpy(ccAttrib.fontStyle, MPE_CC_FONT_STYLE_EMBEDDED,
                MPE_CC_MAX_FONT_NAME_LENGTH);
        break;

    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_PEN_SIZE:
        ccAttrib.fontSize = MPE_CC_FONT_SIZE_EMBEDDED;
        break;

    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_WINDOW_BORDER_TYPE:
        ccAttrib.borderType = MPE_CC_BORDER_TYPE_EMBEDDED;
        break;

    default:
        return;
    }

    (void)mpe_ccSetAttributes(&ccAttrib, toNativeAttribute(attrib), toNativeCCType(ccType));
}

/*
 * Class:     org_ocap_media_ClosedCaptioningAttribute
 * Method:    nativeSetCCIntValue
 * Signature: (III)V
 */
JNIEXPORT void JNICALL Java_org_ocap_media_ClosedCaptioningAttribute_nativeSetCCIntValue
(JNIEnv *env, jobject obj, jint attrib, jint value, jint ccType)
{
    mpe_CcAttributes ccAttrib;

    MPE_UNUSED_PARAM(obj);
    MPE_UNUSED_PARAM(env);

    switch (attrib)
    {
    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_PEN_FG_OPACITY:
        ccAttrib.charFgOpacity = translateOpacity(env,value);
        break;

    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_PEN_BG_OPACITY:
        ccAttrib.charBgOpacity = translateOpacity(env,value);
        break;

    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_WINDOW_FILL_OPACITY:
        ccAttrib.winOpacity = translateOpacity(env,value);
        break;

    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_FONT_ITALICIZED:
        ccAttrib.fontItalic = (mpe_Bool)value;
        break;

    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_FONT_UNDERLINE:
        ccAttrib.fontUnderline = (mpe_Bool)value;
        break;

    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_PEN_SIZE:
        ccAttrib.fontSize = translateSize(env,value);
        break;

    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_WINDOW_BORDER_TYPE:
        ccAttrib.borderType = translateBorderType(env,value);
        break;

    default:
        return;
    }

    (void)mpe_ccSetAttributes(&ccAttrib, toNativeAttribute(attrib), toNativeCCType(ccType));
}

/*
 * Class:     org_ocap_media_ClosedCaptioningAttribute
 * Method:    nativeSetCCColorValue
 * Signature: (IIIII)V
 */
JNIEXPORT void JNICALL Java_org_ocap_media_ClosedCaptioningAttribute_nativeSetCCColorValue
(JNIEnv *env, jobject obj, jint attrib, jint red, jint green, jint blue, jint ccType)
{
    mpe_CcAttributes ccAttrib;

    MPE_UNUSED_PARAM(obj);
    MPE_UNUSED_PARAM(env);

    switch (attrib)
    {
    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_PEN_FG_COLOR:
        ccAttrib.charFgColor.rgb = MPE_CC_COLOR(red,green,blue);
        break;

    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_PEN_BG_COLOR:
        ccAttrib.charBgColor.rgb = MPE_CC_COLOR(red,green,blue);
        break;

    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_WINDOW_FILL_COLOR:
        ccAttrib.winColor.rgb = MPE_CC_COLOR(red,green,blue);
        break;

    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_WINDOW_BORDER_COLOR:
        ccAttrib.borderColor.rgb = MPE_CC_COLOR(red,green,blue);
        break;

    default:
        return;
    }

    (void)mpe_ccSetAttributes(&ccAttrib, toNativeAttribute(attrib), toNativeCCType(ccType));
}

/*
 * Class:     org_ocap_media_ClosedCaptioningAttribute
 * Method:    nativeSetCCStringValue
 * Signature: (ILjava/lang/String;I)V
 */
JNIEXPORT void JNICALL Java_org_ocap_media_ClosedCaptioningAttribute_nativeSetCCStringValue
(JNIEnv *env, jobject obj, jint attrib, jstring value, jint ccType)
{
    mpe_CcAttributes ccAttrib;

    MPE_UNUSED_PARAM(obj);
    MPE_UNUSED_PARAM(env);

    switch (attrib)
    {
    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_FONT_STYLE:
        {
            const char* str = (*env)->GetStringUTFChars(env, value, NULL);
            strncpy(ccAttrib.fontStyle, str, MPE_CC_MAX_FONT_NAME_LENGTH);
            (*env)->ReleaseStringUTFChars(env, value, str);
            break;
        }

    default:
        return;
    }

    (void)mpe_ccSetAttributes(&ccAttrib, toNativeAttribute(attrib), toNativeCCType(ccType));
}


/*
 * Class:     org_ocap_media_ClosedCaptioningAttribute
 * Method:    nativeGetCCAttribute
 * Signature: (II)Ljava/lang/String;
 */
JNIEXPORT jobject JNICALL Java_org_ocap_media_ClosedCaptioningAttribute_nativeGetCCAttribute(
        JNIEnv *env, jobject obj, jint attrib, jint ccType)
{
    mpe_Error err;
    mpe_CcAttributes mpe_attrib;

    MPE_UNUSED_PARAM(obj);

    err = mpe_ccGetAttributes(&mpe_attrib, toNativeCCType(ccType));

    if (err != MPE_SUCCESS)
        return NULL;

    switch (attrib)
    {
    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_FONT_STYLE:
        return (*env)->NewStringUTF(env, mpe_attrib.fontStyle);

    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_FONT_ITALICIZED:
        return (*env)->NewObject(env, jniutil_CachedIds.ClosedCaptioningInteger,
                                 jniutil_CachedIds.ClosedCaptioningInteger_init,
                                 mpe_attrib.fontItalic);

    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_FONT_UNDERLINE:
        return (*env)->NewObject(env, jniutil_CachedIds.ClosedCaptioningInteger,
                                 jniutil_CachedIds.ClosedCaptioningInteger_init,
                                 mpe_attrib.fontUnderline);

    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_PEN_FG_COLOR:
        return (*env)->NewObject(env, jniutil_CachedIds.ClosedCaptioningMpeColor,
                                 jniutil_CachedIds.ClosedCaptioningMpeColor_init,
                                 obj, mpe_attrib.charFgColor.rgb,
                                 (*env)->NewStringUTF(env, mpe_attrib.charFgColor.name));

    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_PEN_BG_COLOR:
        return (*env)->NewObject(env, jniutil_CachedIds.ClosedCaptioningMpeColor,
                                 jniutil_CachedIds.ClosedCaptioningMpeColor_init,
                                 obj, mpe_attrib.charBgColor.rgb,
                                 (*env)->NewStringUTF(env, mpe_attrib.charBgColor.name));

    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_WINDOW_FILL_COLOR:
        return (*env)->NewObject(env, jniutil_CachedIds.ClosedCaptioningMpeColor,
                                 jniutil_CachedIds.ClosedCaptioningMpeColor_init,
                                 obj, mpe_attrib.winColor.rgb,
                                 (*env)->NewStringUTF(env, mpe_attrib.winColor.name));

    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_WINDOW_BORDER_COLOR:
        return (*env)->NewObject(env, jniutil_CachedIds.ClosedCaptioningMpeColor,
                                 jniutil_CachedIds.ClosedCaptioningMpeColor_init,
                                 obj, mpe_attrib.borderColor.rgb,
                                 (*env)->NewStringUTF(env, mpe_attrib.borderColor.name));

    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_PEN_FG_OPACITY:
    {
        int javaOpacity = translateOpacityToJava(mpe_attrib.charFgOpacity);
        return (*env)->NewObject(env, jniutil_CachedIds.ClosedCaptioningInteger,
                                 jniutil_CachedIds.ClosedCaptioningInteger_init,
                                javaOpacity);
    }
    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_PEN_BG_OPACITY:
    {
        int javaOpacity = translateOpacityToJava(mpe_attrib.charBgOpacity);
        return (*env)->NewObject(env, jniutil_CachedIds.ClosedCaptioningInteger,
                                 jniutil_CachedIds.ClosedCaptioningInteger_init,
                                javaOpacity);
    }

    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_WINDOW_FILL_OPACITY:
      {
        int javaOpacity = translateOpacityToJava(mpe_attrib.winOpacity);
        return (*env)->NewObject(env, jniutil_CachedIds.ClosedCaptioningInteger,
                                 jniutil_CachedIds.ClosedCaptioningInteger_init,
                                 javaOpacity);
      }

    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_PEN_SIZE:
        return (*env)->NewObject(env, jniutil_CachedIds.ClosedCaptioningInteger,
                                 jniutil_CachedIds.ClosedCaptioningInteger_init,
                                 mpe_attrib.fontSize);

    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_WINDOW_BORDER_TYPE:
        return (*env)->NewObject(env, jniutil_CachedIds.ClosedCaptioningInteger,
                                 jniutil_CachedIds.ClosedCaptioningInteger_init,
                                 mpe_attrib.borderType);
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "ClosedCaptioningAttribute JNI: nativeGetCCAttribute\n");

    return NULL;
}

/*
 * Class:     org_ocap_media_ClosedCaptioningAttribute
 * Method:    nativeGetCCCapability
 * Signature: (II)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_org_ocap_media_ClosedCaptioningAttribute_nativeGetCCCapability(
        JNIEnv *env, jobject obj, jint attrib, jint ccType)
{
    jclass clazz = (*env)->FindClass(env, "java/lang/String");
    jobjectArray retVal;
    mpe_Error err;
    uint32_t size = 0;
    int i;

    MPE_UNUSED_PARAM(obj);

    mpe_mutexAcquire(capsMutex);

    // For the desired attribute, retrieve the capabilities from MPEOS into our
    // pre-allocated array.  Then allocate an array of Java objects and initialize
    // them for our return objectarray
    switch (attrib)
    {
    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_FONT_STYLE:
        err = mpe_ccGetCapability(toNativeAttribute(attrib), toNativeCCType(ccType),
                                  (void*)fontStyleCaps, &size);
        retVal = (*env)->NewObjectArray(env, size, clazz, NULL);
        for (i = 0; i < size; i++)
        {
            (*env)->SetObjectArrayElement(env, retVal, i,
                                          (*env)->NewStringUTF(env, *fontStyleCaps[i]));
        }
        break;

    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_FONT_ITALICIZED:
    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_FONT_UNDERLINE:
        err = mpe_ccGetCapability(toNativeAttribute(attrib), toNativeCCType(ccType),
                                  (void**)textStyleCaps, &size);
        retVal = (*env)->NewObjectArray(env, size,
                                        jniutil_CachedIds.ClosedCaptioningInteger, NULL);
        for (i = 0; i < size; i++)
        {
            (*env)->SetObjectArrayElement(env, retVal, i,
                                          (*env)->NewObject(env, jniutil_CachedIds.ClosedCaptioningInteger,
                                                            jniutil_CachedIds.ClosedCaptioningInteger_init,
                                                            *textStyleCaps[i]));
        }
        break;

    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_PEN_FG_COLOR:
    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_PEN_BG_COLOR:
    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_WINDOW_FILL_COLOR:
    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_WINDOW_BORDER_COLOR:
        err = mpe_ccGetCapability(toNativeAttribute(attrib), toNativeCCType(ccType),
                                  (void**)colorCaps, &size);
        retVal = (*env)->NewObjectArray(env, size,
                                        jniutil_CachedIds.ClosedCaptioningMpeColor, NULL);
        for (i = 0; i < size; i++)
        {
            (*env)->SetObjectArrayElement(env, retVal, i,
                                          (*env)->NewObject(env, jniutil_CachedIds.ClosedCaptioningMpeColor,
                                                            jniutil_CachedIds.ClosedCaptioningMpeColor_init,
                                                            obj, colorCaps[i]->rgb,
                                                            (*env)->NewStringUTF(env,colorCaps[i]->name)));
        }
        break;

    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_PEN_FG_OPACITY:
    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_PEN_BG_OPACITY:
    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_WINDOW_FILL_OPACITY:
        err = mpe_ccGetCapability(toNativeAttribute(attrib), toNativeCCType(ccType),
                                  (void**)opacityCaps, &size);
        retVal = (*env)->NewObjectArray(env, size,
                                        jniutil_CachedIds.ClosedCaptioningInteger, NULL);
        for (i = 0; i < size; i++)
        {
            (*env)->SetObjectArrayElement(env, retVal, i,
                                          (*env)->NewObject(env, jniutil_CachedIds.ClosedCaptioningInteger,
                                                            jniutil_CachedIds.ClosedCaptioningInteger_init,
                                                            *opacityCaps[i]));
        }
        break;

    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_PEN_SIZE:
        err = mpe_ccGetCapability(toNativeAttribute(attrib), toNativeCCType(ccType),
                                  (void**)fontSizeCaps, &size);
        retVal = (*env)->NewObjectArray(env, size,
                                        jniutil_CachedIds.ClosedCaptioningInteger, NULL);
        for (i = 0; i < size; i++)
        {
            (*env)->SetObjectArrayElement(env, retVal, i,
                                          (*env)->NewObject(env, jniutil_CachedIds.ClosedCaptioningInteger,
                                                            jniutil_CachedIds.ClosedCaptioningInteger_init,
                                                            *fontSizeCaps[i]));
        }
        break;

    case org_ocap_media_ClosedCaptioningAttribute_CC_ATTRIBUTE_WINDOW_BORDER_TYPE:
        err = mpe_ccGetCapability(toNativeAttribute(attrib), toNativeCCType(ccType),
                                  (void**)borderTypeCaps, &size);
        retVal = (*env)->NewObjectArray(env, size,
                                        jniutil_CachedIds.ClosedCaptioningInteger, NULL);
        for (i = 0; i < size; i++)
        {
            (*env)->SetObjectArrayElement(env, retVal, i,
                                          (*env)->NewObject(env, jniutil_CachedIds.ClosedCaptioningInteger,
                                                            jniutil_CachedIds.ClosedCaptioningInteger_init,
                                                            *borderTypeCaps[i]));
        }
        break;

    default:
        err = MPE_CC_ERROR_INVALID_PARAM;
        retVal = NULL;
        break;
    }

    mpe_mutexRelease(capsMutex);

    if (err != MPE_SUCCESS)
    {
        return NULL;
    }

    return retVal;
}
