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

#ifndef MPE_OBJECTCAROUSELUTILS_H
#define MPE_OBJECTCAROUSELUTILS_H

#include "mpe_os.h"
#include "mpe_dbg.h"
#include "mpe_si.h"
#include "mpe_filter.h"
#include "mpe_dataCarousel.h"
#include "mpe_pod.h"

#ifdef __cplusplus
extern "C"
{
#endif

// Constants
// Table and message descriminators for DSM-CC tables in the MPEG-Stream
// Defined in IEC 13818-6

#define DII_TABLE_ID     0x3b
#define DSI_TABLE_ID     0x3b
#define DDB_TABLE_ID     0x3c

// Last 4 bytes of the mask.
#define DII_MESSAGE_DESC   {0x11, 0x03, 0x10, 0x02}
#define DSI_MESSAGE_DESC   {0x11, 0x03, 0x10, 0x06}
#define DDB_MESSAGE_DESC   {0x11, 0x03, 0x10, 0x03}

#define DSMCC_MESSAGE_DESC_POS      (8)
#define DSMCC_MESSAGE_DESC_LEN      (4)

// Grab the bits in a transactionID to represent the carousel ID and the version
#define OCU_DII_CAROUSEL_ID(x)      (x & 0x0000fffe)
#define OCU_DII_CAROUSEL_VERSION(x) ((x & 0x3fff0000) >> 16)

// Useful debugging macros
#define TRUEFALSE(x) ((x) ? "True" : "False")

#ifndef MIN
#define MIN(a,b) (((a) < (b)) ? (a) : (b))
#endif

#ifndef MAX
#define MAX(a,b) (((a) > (b)) ? (a) : (b))
#endif

// Recognized transport types
typedef enum
{
    MPE_OC_IB, MPE_OC_OOB, MPE_OC_DSG
} mpe_OcTransportType;

typedef struct
{
    uint32_t frequency;
    uint32_t prog;
    uint32_t tunerId;
} mpe_OcIbParams;

typedef struct
{
    uint32_t prog;
} mpe_OcOobParams;

typedef struct
{
    uint32_t appId;
    uint32_t prog;
} mpe_OcDsgAppTunnelParams;

typedef struct
{
    mpe_OcTransportType transport;
    union
    {
        mpe_OcIbParams ib;
        mpe_OcOobParams oob;
        mpe_OcDsgAppTunnelParams dsg;
    } t;
} mpe_OcTuningParams;

/*
 * Forward declarations.
 */

/**
 * Get a section that matches a filter from the MPEG stream.
 *
 * @param stream    The MPEG stream to fetch the section from.
 * @param filter    A filter to apply to get the desired section.
 * @param timeout   How long to wait, in milliseconds, before giving up.
 *                  0 means wait forever.
 * @param section   [out] A Pointer to where to put the section handle for the
 *                  retrieved section.
 *
 * @return MPE_SUCCESS if the section is retrieved, MPE_ETIMEOUT if the fetch
 *         times out, other error codes as appropriate if failure.
 */
extern mpe_Error ocuGetSection(mpe_FilterSource *source,
        mpe_FilterSpec *filter, uint32_t timeout,
        mpe_FilterSectionHandle *section);

/**
 * Create a filter to grab a DDB out of a transport stream.
 *
 * @param module            The module from which we want the DDB.
 * @param matchDDB          Do we want to match against a specific DDB?
 * @param ddb               The DDB within this filter (starting at 0).
 * @param version           Which version of this DDB are we looking for?
 * @param downloadID
 * @param retFilter [out]   The filter we'll be returning. [output]
 *
 * @return MPE_SUCCESS if we create the filter, error codes otherwise.
 */
extern mpe_Error
ocuMakeDDBFilter(uint32_t moduleId, mpe_Bool matchDDB, uint32_t ddb,
        uint32_t version, uint32_t downloadID, mpe_FilterSpec **retFilter);
/**
 * Create a filter to find a DSI.
 *
 * @param retFilter             [out] Pointer to where to put the created filter.
 *
 * @return MPE_SUCCESS if the filter is successfully created, error codes otherwise.
 */
extern mpe_Error ocuMakeDSIFilter(mpe_FilterSpec **retFilter);

/**
 * Create a filter to find a particular DII.
 *
 * @param selector              The selector to check for.
 * @param retFilter             [out] Pointer to where to put the created filter.
 *
 * The next two parameters are hacks to support PowerTV's nonstandard filtering for DII's.
 * @param oob                   Is this an OOB filter?
 * @param carouselID            For OOB, what is the carouselID of this DII.
 *
 * @return MPE_SUCCESS if the filter is successfully created, error codes otherwise.
 */
extern mpe_Error
ocuMakeDIIFilter(uint32_t selector,
#ifdef PTV
        mpe_Bool oob,
        uint32_t carouselID,
#endif
        mpe_FilterSpec **retFilter);

/**
 * Create a filter to when a DII changes.
 *
 * @param transactionID     The current transactions ID, will change when
 *                          the version is updated.
 * @param retFilter         [out] Pointer to where to put the created filter.
 *
 * The next two parameters are hacks to support PowerTV's nonstandard filtering for DII's.
 * @param oob                   Is this an OOB filter?
 * @param carouselID            For OOB, what is the carouselID of this DII.
 *
 * @return MPE_SUCCESS if the filter is successfully created, error codes otherwise.
 */
extern mpe_Error
ocuMakeDIIChangeFilter(uint32_t transactionID,
#ifdef PTV
        mpe_Bool oob,
        uint32_t carouselID,
#endif
        mpe_FilterSpec **retFilter);

/**
 * Set up a mpe_FilterSource appropriately for a filtering.
 * Note: Does not set the PID.  That's handled separately.
 *
 * @param transport The tuning params for the carousel we're looking at.
 * @param stream    The mpe_FilterSource we wish to fill in.
 */
extern
void
ocuSetStreamParams(mpe_OcTuningParams *transport, mpe_FilterSource *stream);

/**
 * Convert a carousel ID into the PID which can be used to set filters on the stream.
 *
 * @param siHandle          The SI DB handle
 *                          to the current service.
 * @param carouselID        The carousel ID to translate.
 * @param pid               [out] Location where to put the PID
 *
 * @returns MPE_SUCCESS if the translation succeeds, errors otherwise.
 */
extern mpe_Error
ocuTranslateCarouselID(mpe_SiServiceHandle siHandle, uint32_t carouselID,
        uint32_t *pid);

/**
 * Convert a association tag into the PID which can be used to set filters on the stream.
 *
 * @param siHandle          The SI DB handle
 *                          to the current service.
 * @param assocTag          The tag to translate.
 * @param pid               [out] Pointer to an object to fill.  Note, must be preallocated.
 *
 * @returns MPE_SUCCESS if the translation succeeds, errors otherwise.
 */
extern mpe_Error
ocuTranslateAssociationTag(mpe_SiServiceHandle siHandle, uint16_t assocTag,
        uint32_t *pid);

/**
 * Translate an association tag into a program number via a deferred association tag descriptor.
 * This is used to translate a BIOP_PROGRAM_USE TAP into a program number.  Will only return success
 * if a deferred association tag exists with the appropriate tag.  Does not confirm that the target
 * program exists.
 *
 * @param siHandle          The SI DB handle
 *                          to the current service.
 * @param assocTag          The tag to translate.
 * @param program           [out] The program number of the target program.
 *
 * @returns MPE_SUCCESS if a deferred association tag is found and translated, error codes otherwise.
 *
 */
extern mpe_Error
ocuGetProgramFromAssociationTag(mpe_SiServiceHandle siHandle,
        uint16_t assocTag, uint32_t *program);

/**
 * Get the frequency and program number for a given si handle.
 *
 * @param siHandle          The SI DB handle.
 * @param frequency         [out] Pointer to where to put the frequency.
 * @param program           [out] Pointer to where to put the program number.
 *
 * @returns MPE_SUCCESS if the translation succeeds, errors otherwise.
 */
extern mpe_Error
ocuSetTuningParams(mpe_SiServiceHandle siHandle, mpe_OcTuningParams *params);

/**
 * Set the tuner ID.  Only valid for Inband streams, nop otherwise.
 * @param transport     The Tuning Parameters block to set the tuner in.
 *
 * @returns MPE_SUCCESS if the tuner could be set (or not IB), false if the frequency was
 *          not found.
 */
extern mpe_Error
ocuSetTuner(mpe_OcTuningParams *transport);

/**
 * Get the Source ID for a given si handle.
 *
 * @param siHandle          The SI DB handle.
 * @param sourceID          [out] Pointer to where to put the Source ID.
 *
 * @returns MPE_SUCCESS if the translation succeeds, errors otherwise.
 */
extern mpe_Error
ocuGetSourceID(mpe_SiServiceHandle siHandle, uint32_t *sourceID);

/**
 * Get the AppID for a given si handle.
 *
 * @param siHandle          The SI DB handle.
 * @param appID          [out] Pointer to where to put the appID.
 *
 * @returns MPE_SUCCESS if the translation succeeds, errors otherwise.
 */
extern mpe_Error
ocuGetAppID(mpe_SiServiceHandle siHandle, uint32_t *appID);

/**
 * Convert a string from UTF-8 encoding to ISO 8859-1 (standard Latin-1).
 *
 * @param in    The input string.
 * @param out   [out] The output string to fill.  Must be at long enough to hold the entire string.
 *
 * @return MPE_SUCCESS if the string is sucessfully translated, MPE_EINVAL if it contains characters
 *                     which are not in ISO 8859-1.
 */
extern mpe_Error
ocuUtf8ToIso88591(uint8_t *in, uint8_t *out);

/**
 * Translate a string from ISO 8859-1 to UTF-8.
 *
 * @param in        The input string to translate.
 * @param out       [out] Where to put the translated string.
 *
 * @return MPE_SUCCESS
 */
extern mpe_Error
ocuIso88591ToUtf8(uint8_t *in, uint8_t *out);

/**
 * Get an environment variable, if it exists, otherwise return the default.
 *
 * @param name          The name of the variable to read.
 * @param defValue      The default value to use if the variable is not
 *                      in the environment.
 *
 * @return The value, either from the environment, or from the default
 * provided.
 */
extern uint32_t
ocuGetEnv(char *name, uint32_t defValue);

/**
 * Start a CA decrypt session. When MPE_SUCCESS is returned, caHandle will
 * be 0 if CA authorization is not required. When non-zero, changes in the
 * CA session will be signaled via the mpe_EventQueue.
 *
 * @param tunerId       The tuner to be used for mounting the carousel
 * @param siHandle      The SI handle for the associated Service
 * @param pid           The PID carrying the carousel
 * @param queue         Queue to register for CA session updates
 * @param caHandle      Pointer to store the created CA session handle
 *
 * @return MPE_SUCCESS when the session is successfully created or no
 *         session is required or an appopriate error if the session
 *         cannot be created
 */
extern mpe_Error
ocuStartCASession(uint32_t tunerId, mpe_SiServiceHandle siHandle, uint32_t pid, mpe_EventQueue queue, mpe_PODDecryptSessionHandle * caHandle);

/**
 * Wait on the designated queue for a terminal CA condition (fully
 * authorized or unauthorized) or timeout.
 *
 * @param caHandle      Handle to the CA decrypt session
 * @param queue         The queue associated with the decrypt session
 * @param status        Pointer to store the terminal CA condition/event
 *                      (only set when MPE_SUCCESS is returned)
 *
 * @return MPE_SUCCESS if a terminal event is received in the timeout
 *         period or MPE_ETIMEOUT if no terminal event is received in the
 *         timeout period
 */
extern mpe_Error
ocuWaitForCASessionInit( mpe_PODDecryptSessionHandle caHandle,
                         mpe_EventQueue queue,
                         mpe_PodDecryptSessionEvent * status );

/**
 * Issue a close on the CA session and wait for the shutdown acknowledgment
 * or timeout.
 *
 * @param caHandle      Handle to the CA decrypt session
 * @param queue         The queue associated with the decrypt session
 *
 * @return MPE_SUCCESS if the shutdown event is received in the timeout
 *         period or MPE_ETIMEOUT if no terminal event is received in the
 *         timeout period
 */
extern mpe_Error
ocuCloseCASession(mpe_PODDecryptSessionHandle caHandle, mpe_EventQueue queue);

#ifdef __cplusplus
}
#endif
#endif /* ifndef MPE_OBJECTCAROUSELUTILS_H */
