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

/* Header Files */

#ifndef __RIP_DISPLAY_H__
#define __RIP_DISPLAY_H__

#ifdef __cplusplus
extern "C"
{
#endif

#define RIPD_MAX_BYTESPERPIXEL 32
#define RIPD_MAX_WIDTH 1920
#define RIPD_MAX_HEIGHT 1080

/** This type identifies the buffering mode supported in the surface.
 */
typedef enum
{
    RIPD_SINGLEBUFFER = 1, RIPD_DOUBLEBUFFER = 2
} RIPD_BufferMode;

/** This type identifies a frame buffer within surface, when
 double buffering is enabled.
 */
typedef enum
{
    RIPD_PRIMARY = 1, RIPD_SECONDARY = 2
} RIPD_BufferType;

typedef enum
{
    RIPD_BUF_DISABLED = 1, RIPD_BUF_WRITING, RIPD_BUF_PAINTING, RIPD_BUF_READY
} RIPD_BufferState;

/** Result codes.
 *
 * *TODO* - come back and clean these up
 */
typedef enum
{
    RIPD_SUCCESS = 0, RIPD_NOTIMPLEMENTED = 1, RIPD_FAILED_SID_ALLOCATION, /* Failed call to AllocateAndInitializeSid() */
    RIPD_FAILED_ACL_SET, /* Failed call to SetEntriesInAcl() */
    RIPD_FAILED_LOCAL_ALLOC, /* Failed LocalAlloc() */
    RIPD_FAILED_INIT, /* Failed InitializeSecurityDescriptor() */
    RIPD_FAILED_SD_DACL, /* Failed SetSecurityDescriptorDacl() */
    RIPD_FAILED_SHMEM_ALLOCATION, /* Failed CreateFileMapping() */
    RIPD_FAILED_SHMEM_OPEN, /* Failed OpenFileMapping() */
    RIPD_FAILED_SHMEM_MAPPING, /* Failed MapViewOfFile() */
    RIPD_FAILED_EVENT_CREATION, /* Failed CreateEvent() */
    RIPD_FAILED_EVENT_OPEN, /* Failed EventOpen() */
    RIPD_FAILED_EVENT_SIGNAL, /* Failed SetEvent() */
    RIPD_LAST_RESULT
/* Marker */
} RIPD_Result;

/** This structure defines the elements required to access the surface framebuffer.
 *
 *   *TODO* - does this really still apply???
 Since a heap is not used in the Win32 memory-mapped file, an instantiation
 of this structure should start at the address returned by the Win32
 MapViewOfFile() function.
 */
typedef struct RIPDisplay
{
    unsigned int width; /** Width in pixels (column count) */
    unsigned int height; /** Height in pixels (lines/row count). */
    unsigned short bytesPerPixel; /** Bytes-per-pixel. Must be 4 currently. */
    unsigned short bitsPerPixel; /** bits-per-pixel. Must be 32 currently. */
    unsigned int bytesPerLine; /** Length of horizontal line in bytes. */
    RIPD_BufferMode bufMode; /** Indicates whether double-buffering is enabled. */
    RIPD_BufferState primaryBufState; /** State of the primary buffer */
    RIPD_BufferState secondaryBufState; /** State of the secondary buffer */
    unsigned char* primaryBuf; /** This will be allocated as variable-length structure.
     Actual size will be height * bytesPerLine bytes */
    /** secondaryBuf will start at
     primaryBuf + height * bytesPerLine bytes if
     bufMode is RIPD_DOUBLEBUFFER */
} RIPDisplay;

/** This structure defines the session-related variables for the framebuffer.
 */
typedef struct RIPDisplaySession
{
    RIPDisplay * ripDisplay;
    unsigned char * primaryBuf; /** Shortcut pointer to the primary framebuffer */
    unsigned char * secondaryBuf; /** Shortcut pointer to the secondary frambuffer */
} RIPDisplaySession;

/**
 * Initialize the display with the selected coherent configuration
 */
RIPD_Result rip_InitDisplay(unsigned int graphicsWidth,
        unsigned int graphicsHeight, unsigned int graphicsPARx,
        unsigned int graphicsPARy, unsigned int videoWidth,
        unsigned int videoHeight, unsigned int videoPARx,
        unsigned int videPARy, unsigned int backgroundWidth,
        unsigned int backgroundHeight, unsigned int backgroundPARx,
        unsigned int backgroundPARy);

/** Initialize surface control elements. This should only be called once,
 prior to any other functions.
 */
RIPD_Result InitSurface(void);

/** Allocate a framebuffer and associated control elements in RI Platform and
 return a pointer to the control structure.
 Returns 0 on success and non-zero on failure.
 */
void GetCurrentPrimaryBuffer(void** ppPrimaryBuf);

RIPD_Result AllocateSurface(unsigned int width, unsigned int height,
        unsigned int bitsPerPixel, unsigned int bytesPerPixel,
        RIPD_BufferMode bufMode, RIPDisplaySession ** ppShmemFBSession);

/** Signal that a framebuffer is ready to be displayed on-screen.
 If double-buffering is enabled, bufToPaint designated which buffer to
 display on-screen from the designated Framebuffer.
 Returns 0 on success and non-zero on failure.
 */
void RefreshSurface(RIPDisplaySession * pDisplaySession,
        RIPD_BufferType bufToPaint);

#ifdef __cplusplus
} // END extern "C"
#endif

#endif /* __RIP_DISPLAY_H__ */
