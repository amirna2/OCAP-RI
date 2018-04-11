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

#include "mpe_types.h"
#include "mpe_os.h"
#include "mpe_si.h"
#include "mpe_dbg.h"
#include "mpe_file.h"

#include "mpe_objectCarousel.h"
#include "mpe_objectCarouselUtils.h"
#include "mpe_dataCarousel.h"
#include "mpe_objectCarouselSectionParser.h"
#include "mpe_objectCarouselBIOPParser.h"
#include "mpe_si.h"
#include "mpe_pod.h"

#include <string.h> //memset
#define OC_ENABLE_BIOP_CACHE_NAME       ("OC.ENABLE.BIOP.CACHE")
#define OC_ENABLE_BIOP_CACHE_DEFAULT    1
#define OC_USE_DVB_NSAP_NAME            ("OC.USE.DVB.NSAP")
#define OC_USE_DVB_NSAP_DEFAULT         0

#define OC_DSI_TIMEOUT              (60 * 1000)
#define MAX_OBJECTCAROUSELS             (16)
#define DEFAULT_CACHE_SIZE              (32)
#define MAX_LINKS                       (32)

#define MAX_OC_NAMELEN                  (1024+256)

#define OC_THREAD_STACK_SIZE            (8 * 1024)

#define INVALID_CAROUSEL_ID             (-1)

#define PATH_SEPARATOR                  ('/')

#define READER_FUNCPTR                  mpe_Error (*)(void *, uint32_t, uint32_t, uint32_t, uint8_t *, uint32_t *)

// Macros to figure out the timeouts.
// Timeouts in the OC are specified in microseconds, timeouts in MPE are in milliseconds.
// Always specify the default values in milliseconds.
#define OC_USE_DEF_TIMEOUT              (0xffffffff)
#define OC_TIMEOUT(spec, def)           ((spec != OC_USE_DEF_TIMEOUT) ? (spec / 1000) : def)

#ifndef __GNUC__
#define inline
#endif

// Local type declarations

// A cache for parsed BIOP Objects
typedef struct
{
    ocpBIOPObject **cache; // Array of cached elements
    uint32_t numElements; // Number of elements in the cache.
    uint32_t allocatedElements; // Number of elements we have space for

    uint32_t nextOffset; // How far into the module have we parsed so far?
} ocBIOPObjectCache;

// A Data Source for the parser.
// Buffers data to prevent excessive reads to the underlying module.
//
#define             MAX_BUFFER_SIZE     (128)

typedef struct
{
    mpe_DcModule *module;
    uint32_t offset;
    uint32_t avail;
    uint8_t buffer[MAX_BUFFER_SIZE];
} ocReaderBuffer;

// Forward declarations
static void ocFreeOC(mpe_ObjectCarousel *);
static void ocFreeFile(mpe_OcFile *);
static mpe_Error ocGetDataCarousel(mpe_ObjectCarousel *, ocpTAP *,
        mpe_DataCarousel **);
static mpe_Error ocRegisterOC(mpe_ObjectCarousel *);
static void ocUnregisterOC(mpe_ObjectCarousel *);
static mpe_Error ocLoadDirectoryEntries(mpe_ObjectCarousel *, ocpBIOPObject *,
        mpe_DcModule *, uint32_t);

static mpe_Error ocFindObjectInModule(mpe_DcModule *, uint32_t, uint32_t,
        ocpBIOPObject **);
static mpe_Error ocFindDirectoryEntry(mpe_ObjectCarousel *, ocpBIOPObject *,
        char *, mpe_DcModule *, uint32_t, ocpDirEntry **);

static mpe_Error ocWalkPath(mpe_ObjectCarousel *, char *, ocpTAP *,
        ocpBIOPObjectLocation *, uint32_t, ocpTAP **, ocpBIOPObjectLocation **,
        mpe_DcModule **, ocpBIOPObject **, ocpDirEntry **, char **);
static mpe_Error ocWalkPathResolvingLinks(mpe_ObjectCarousel *, char *,
        ocpTAP *, ocpBIOPObjectLocation *, uint32_t, mpe_ObjectCarousel **,
        ocpTAP **, ocpBIOPObjectLocation **, mpe_DcModule **, ocpBIOPObject **,
        ocpDirEntry **);
static mpe_Error ocResolveLiteOptionsName(ocpDirEntry *, mpe_Bool, char *,
        char **);

static mpe_Error ocMakeDirEntry(ocpDirEntry *, mpe_ObjectCarousel *,
        mpe_OcDirEntry **);

static inline char * ocNSAPString(uint8_t *);
static mpe_Error ocCalculateNSAP(mpe_ObjectCarousel *);

static inline mpe_Bool ocCompareTAPs(ocpTAP *, ocpTAP *);

static inline mpe_Error ocInsertBIOPCacheObject(ocBIOPObjectCache *,
        ocpBIOPObject *, uint32_t, uint32_t);
static inline mpe_Error ocLookupBIOPCache(ocBIOPObjectCache *, uint32_t,
        ocpBIOPObject **, uint32_t *);

static inline mpe_Error ocGetModuleCache(mpe_DcModule *, ocBIOPObjectCache **);

static inline mpe_Bool ocCompareFileNames(char *, char *);

static mpe_Error ocGetObject(mpe_ObjectCarousel *, ocpTAP *,
        ocpBIOPObjectLocation *, mpe_DcModule **, ocpBIOPObject **);

static mpe_Error ocReadBufferedBytes(void *, uint32_t, uint32_t, uint32_t,
        uint8_t *, uint32_t *);

// Private variable declarations
static mpe_Mutex ocGlobalMutex = NULL;
static mpe_ObjectCarousel *ocObjectCarousels[MAX_OBJECTCAROUSELS] =
{ NULL };
static uint32_t ocRegisteredCarousels = 0;

// Configurable parameters
static uint32_t ocEnableBIOPCache = OC_ENABLE_BIOP_CACHE_DEFAULT;
static uint32_t ocUseDVBNSAP = OC_USE_DVB_NSAP_DEFAULT;

#define RELEASE_BIOP_OBJECT(x)      if (!ocEnableBIOPCache && (x != NULL)) { ocpFreeBIOPObject(x); }

/**
 *
 */
mpe_Error mpe_ocInit(void)
{
    mpe_Error retCode = MPE_SUCCESS;
    uint32_t i;

    if (ocGlobalMutex != NULL)
    {
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILESYS, "OC: Already initialized\n");
        return MPE_FS_ERROR_FAILURE;
    }
    // Create the global things.
    retCode = mpe_mutexNew(&ocGlobalMutex);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OC: Could not initialize global lock: Error code %04x\n",
                retCode);
        return retCode;
    }
    // Initialize the BIOP parser
    retCode = ocpInit();
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OC: Could not initialize BIOP parser\n");
        return retCode;
    }

    // Set local boolean 'enabled' type of variables based on environment variables
    ocEnableBIOPCache = ocuGetEnv(OC_ENABLE_BIOP_CACHE_NAME,
            OC_ENABLE_BIOP_CACHE_DEFAULT);
    ocUseDVBNSAP = ocuGetEnv(OC_USE_DVB_NSAP_NAME, OC_USE_DVB_NSAP_DEFAULT);

    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(ocGlobalMutex);

    // Clear out the data carousels pointer
    for (i = 0; i < MAX_OBJECTCAROUSELS; i++)
    {
        ocObjectCarousels[i] = NULL;
    }

    mpe_mutexRelease(ocGlobalMutex);
    // END CRITICAL SECTION

    return retCode;
}

/**
 * This function mounts an object carousel, loads the service gateway, and prepares the
 * carousel to have file opened.
 *
 * @param siHandle   A Handle to the SI DB to fetch the carousel ID containing the DSI
 * @param carouselID The carousel ID of the carousel to mount.
 * @param oc         Return pointer to fill in with the object carousel object after it has been mounted.
 *
 * @returns MPE_SUCCESS if the carousel is mounted correctly, error codes otherwise.
 */
mpe_Error mpe_ocMount(mpe_SiServiceHandle siHandle, uint32_t carouselID,
        mpe_ObjectCarousel **retOc)
{
    mpe_Error retCode = MPE_SUCCESS;
    mpe_ObjectCarousel *oc = NULL;
    mpe_FilterSpec *dsiFilter = NULL;
    mpe_OcpDSI *dsi = NULL;
    mpe_FilterSectionHandle dsiSection;
    ocpBIOPObject *gateway;
    ocpGatewayInfo gatewayLoc;
    mpe_DcModule *module;
    mpe_PODDecryptSessionHandle caHandle = 0;
    mpe_EventQueue caQueue = 0;
    mpe_PodDecryptSessionEvent lastCaEvent;
    int i;

    // Let's clear out the returned object.  This simplifies error checks below.  We'll assign
    // it at the end.
    *retOc = NULL;

    MPE_LOG(MPE_LOG_INFO, MPE_MOD_FILESYS,
            "OC: Mounting object carousel: %04x\n", carouselID);

    // Allocate up the data carousel structure
    retCode = mpeos_memAllocP(MPE_MEM_FILE_CAROUSEL,
            sizeof(mpe_ObjectCarousel), (void **) &oc);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OC: Cannot allocate Object Carousel structure\n");
        return retCode;
    }

    // Clear it out of any crap.  Just for safety's sake
    memset(oc, 0, sizeof(mpe_ObjectCarousel));

    for (i = 0; i < DC_MAX_DATACAROUSELS; i++)
    {
        oc->dataCarousels[i].tap.transactionID = MPE_DC_INVALID_TRANSID;
        oc->dataCarousels[i].tap.assocTag = 0;
    }

    // Other default variables
    oc->referenceCount = 0;
    oc->mounted = TRUE;
    oc->iso88591 = TRUE;
    oc->updateQueueRegistered = FALSE;

    // Save the carousel ID
    oc->carouselID = carouselID;

    // Setup the SI Handle
    retCode = mpe_ocSetSIHandle(oc, siHandle);
    if (retCode != MPE_SUCCESS)
    {
        goto CleanUp;
    }

    //
    // Get ready to mount the sucker
    //
    MPE_LOG(MPE_LOG_INFO, MPE_MOD_FILESYS,
            "OC: Mounting object carousel: PID: %x IB: %s\n", oc->rootTS.pid,
            TRUEFALSE(oc->rootTS.sourceType == MPE_FILTER_SOURCE_INB));

    if (oc->rootTS.sourceType == MPE_FILTER_SOURCE_INB)
    {
        // Attempt to setup a CA session
        retCode = mpe_eventQueueNew(&caQueue, "MpeOcCaQueue");
        if (retCode != MPE_SUCCESS)
        {
            goto CleanUp;
        }

        retCode = ocuStartCASession( oc->rootParams.t.ib.tunerId, siHandle,
                                     oc->rootTS.pid, caQueue, &caHandle );
        // caHandle will come back '0'/MPE_SUCCESS if no session is required
        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                    "OC: Error starting CA session\n");
            goto CleanUp;
        }

        if (caHandle != 0)
        {
            // The stream has a CA descriptor, so a CA session is required
            MPE_LOG(MPE_LOG_INFO, MPE_MOD_FILESYS,
                    "OC: Started CA decrypt session: handle: %p\n", caHandle);
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                    "OC: Waiting for CA session startup...\n");

            // This could wait a while if MMI or such is required
            retCode = ocuWaitForCASessionInit(caHandle, caQueue, &lastCaEvent);
            if (retCode != MPE_SUCCESS)
            {
                if (retCode == MPE_ETIMEOUT)
                {
                    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                            "OC: Timed out waiting for CA session startup\n");
                }
                else
                {
                    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                            "OC: Error %d waiting for CA session startup\n",
                            retCode );
                }
                goto CleanUp;
            }

            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                    "OC: CA session started with CA code 0x%x\n", lastCaEvent);

            if (lastCaEvent != MPE_POD_DECRYPT_EVENT_FULLY_AUTHORIZED)
            { // If authorization fails, don't continue the mounting process
                MPE_LOG(MPE_LOG_INFO, MPE_MOD_FILESYS,
                        "OC: Authorization failed (0x%x)\n", lastCaEvent);
                goto CleanUp;
            }
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                    "OC: CA is fully authorized  - continuing...\n");
        }
        else
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                    "OC: No authorization required - continuing...\n");
        }

        // Assert: CA is authorized or is not required
    } // END if (oc->rootTS.sourceType == MPE_FILTER_SOURCE_INB)

    // Get the DSI from this stream.
    retCode = ocuMakeDSIFilter(&dsiFilter);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS, "OC: Cannot make DSI Filter\n");
        goto CleanUp;
    }
    retCode = ocuGetSection(&(oc->rootTS), dsiFilter, OC_DSI_TIMEOUT,
            &dsiSection);
    mpe_filterDestroyFilterSpec(dsiFilter);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OC: Cannot get DSI: Error code %04x\n", retCode);
        goto CleanUp;
    }

    // Parse out the DSI structure, and find the
    retCode = mpe_ocpParseDSI(dsiSection, &dsi);
    mpe_filterSectionRelease(dsiSection);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS, "OC: Cannot parse DSI\n");
        goto CleanUp;
    }

    // Parse the gateway location out of the block
    retCode = ocpGetServiceGatewayLocation(dsi->privateDataBytes,
            dsi->numPrivateDataBytes, &gatewayLoc);
    // Release the DSI structure, we're done with it now.
    mpe_ocpFreeDSI(dsi);

    // Make sure the search above worked.
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OC: Cannot find ServiceGateway Location in DSI\n");
        goto CleanUp;
    }

    // Extract the relevant data from the data structures
    // NOTE: These will need to be changed if we start putting arrays into the object location and/or TAP structures.
    memcpy(&(oc->gatewayLoc),
            &(gatewayLoc.ior.profile.body.biopProfile.objectLoc),
            sizeof(ocpBIOPObjectLocation));
    memcpy(&(oc->gatewayTAP), &(gatewayLoc.ior.profile.body.biopProfile.tap),
            sizeof(ocpTAP));

    // Is a gateway timeout specified?
    // BUG: TODO: Get a better timeout value
    oc->gatewayTimeout
            = OC_TIMEOUT(gatewayLoc.ior.profile.body.biopProfile.tap.timeOut, OC_DSI_TIMEOUT);

    // Calculate the NSAP address of the carousel
    (void) ocCalculateNSAP(oc);

    // Register this object carousel so that other OC's can find it via the LiteOptionsProfile
    // form of a BIOP Directory Entry
    retCode = ocRegisterOC(oc);
    if (retCode != MPE_SUCCESS)
    {
        goto CleanUp;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "OC: Retrieving Service Gateway\n");

    // Get the carousel containing the service gateway
    retCode = ocGetDataCarousel(oc, &(oc->gatewayTAP), &(oc->gatewayDC));
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OC: Cannot get Data Carousel for ServiceGateway\n");
        goto CleanUp;
    }

    // Get the module containing the Service Gateway containing the
    retCode = mpe_dcGetModule(oc->gatewayDC, oc->gatewayLoc.moduleID, &module);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OC: Cannot get module for ServiceGateway\n");
        goto CleanUp;
    }

    //ocFindObjectInModule(mpe_DcModule *, uint32_t, uint32_t, ocpBIOPObject **);
    retCode = ocFindObjectInModule(module, oc->gatewayLoc.objectKey,
            oc->gatewayTimeout, &gateway);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OC: Could not load service gateway\n");
        mpe_dcReleaseModule(module);
        goto CleanUp;
    }
    // Make sure it's a service gateway.
    if (gateway->objectType != BIOP_ServiceGateway)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OC: Specified object is not a ServiceGateway\n");
        RELEASE_BIOP_OBJECT(gateway);
        mpe_dcReleaseModule(module);
        retCode = MPE_FS_ERROR_INVALID_DATA;
        goto CleanUp;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
            "OC: Checking %d Service Contexts\n", gateway->numServiceContexts);

    // Now, check to see if we've got any GIOP Code Set contexts.
    // BUG:
    if (gateway->numServiceContexts != 0)
    {
        for (i = 0; i < gateway->numServiceContexts; i++)
        {
            if (gateway->serviceContexts[i].contextTag == TAG_GIOP_CODESET)
            {
                if (gateway->serviceContexts[i].sc.codeSet.charData
                        == TAG_CODESET_ISO88591)
                {
                    MPE_LOG(MPE_LOG_INFO, MPE_MOD_FILESYS,
                            "OC: Setting code set to ISO 8859-1 (Latin 1)\n");
                    oc->iso88591 = TRUE;
                }
                else if (gateway->serviceContexts[i].sc.codeSet.charData
                        == TAG_CODESET_UTF8)
                {
                    MPE_LOG(MPE_LOG_INFO, MPE_MOD_FILESYS,
                            "OC: Setting code set to UTF-8\n");
                    oc->iso88591 = FALSE;
                }
                else
                {
                    MPE_LOG(
                            MPE_LOG_WARN,
                            MPE_MOD_FILESYS,
                            "OC: Unrecognized codeset descriptor: %08x.  Assuming ISO 8859-1\n",
                            gateway->serviceContexts[i].sc.codeSet.charData);

                }
            }
        }
    }

    // Free up the gateway.  We don't keep it around, instead parsing it each time
    RELEASE_BIOP_OBJECT(gateway);
    mpe_dcReleaseModule(module);

    // Close the OC CA session. The DC should now have setup its own session
    ocuCloseCASession(caHandle, caQueue);
    mpe_eventQueueDelete(caQueue);

    // Return the object carousel
    *retOc = oc;
    return retCode;

    CleanUp:
    ocuCloseCASession(caHandle, caQueue);
    if (caQueue)
    {
        mpe_eventQueueDelete(caQueue);
    }
    (void) mpe_ocUnmount(oc);
    return retCode;
}

/**
 * Set a new service handle into the carousel.
 *
 * @param   oc      The carousel to set the handle in.
 * @param   handle  The new handle.
 *
 * @returns MPE_SUCCESS if the SI handle is set.  Errors otherwise.
 */
mpe_Error mpe_ocSetSIHandle(mpe_ObjectCarousel *oc,
        mpe_SiServiceHandle siHandle)
{
    int i;
    mpe_Error retCode = MPE_SUCCESS;

    if (oc == NULL)
    {
        return MPE_EINVAL;
    }

    // Make sure PSI is being acquired for this service.  Will initiate a DSG tunnel
    // if needed.
    retCode = mpe_siRegisterForPSIAcquisition(siHandle);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OC: Could not register for PSI Acquisition: %04x\n", retCode);
        return retCode;
    }

    // Get the PID of the DSI from the carousel.
    retCode = ocuTranslateCarouselID(siHandle, oc->carouselID,
            &(oc->rootTS.pid));
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OC: Could not translate carouselID %04x: Error code: %04x\n",
                oc->carouselID, retCode);
        mpe_siUnRegisterForPSIAcquisition(siHandle);
        return retCode;
    }

    // Set up the tuning parameters.
    retCode = ocuSetTuningParams(siHandle, &(oc->rootParams));
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_FILESYS,
                "OC: Could not get Tuning Params for carousel: %04x: Error code: %04x\n",
                oc->carouselID, retCode);
        mpe_siUnRegisterForPSIAcquisition(siHandle);
        return retCode;
    }

    // Set the tuner.
    retCode = ocuSetTuner(&(oc->rootParams));
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_FILESYS,
                "OC: Could not find tuner on frequency %d for CarouselID: %04x  Error Code: %04x\n",
                oc->rootParams.t.ib.frequency, oc->carouselID, retCode);
        mpe_siUnRegisterForPSIAcquisition(siHandle);
        return retCode;
    }

    // Create the filter source
    ocuSetStreamParams(&(oc->rootParams), &(oc->rootTS));

    // Clear out previous SI Handle.
    if (oc->siHandle != MPE_SI_INVALID_HANDLE)
    {
        mpe_siUnRegisterForPSIAcquisition(oc->siHandle);
    }

    // Save the SI Handle
    oc->siHandle = siHandle;

    for (i = 0; i < DC_MAX_DATACAROUSELS; i++)
    {
        if (oc->dataCarousels[i].dc != NULL)
        {
            (void) dcSetSiHandle(oc->dataCarousels[i].dc, oc->siHandle);
        }
    }

    return retCode;
}

/**
 * Get the SI Handle from a file.
 *
 * @param   file    The file to get the SI Handle from.
 * @param   handle  Pointer to a location to put the SI Handle in.
 *
 * @returns MPE_SUCCESS if the SI handle is retrieved.  Errors otherwise.
 */
mpe_Error mpe_ocGetSIHandle(mpe_OcFile *file, mpe_SiServiceHandle *siHandle)
{
    if (file == NULL || siHandle == NULL)
    {
        return MPE_EINVAL;
    }
    *siHandle = file->oc->siHandle;
    return MPE_SUCCESS;
}

/**
 * This function unmounts an object carousel, and releases all it's memory.  It does no
 * error checking to determine if any files are open in the object carousel,
 * so the caller must make sure that the carousel is ready to be unmounted.
 * Further attempts to access the carousel can result in random memory accesses.
 * Unmounts the underyling data carousel as well.
 *
 * @param oc The object carousel to unmount.
 *
 * @returns MPE_SUCCESS if the object carousel is unmounted correctly.
 */
mpe_Error mpe_ocUnmount(mpe_ObjectCarousel *oc)
{
    if (oc == NULL || oc->mounted == FALSE)
    {
        return MPE_EINVAL;
    }

    MPE_LOG(MPE_LOG_INFO, MPE_MOD_FILESYS, "OC: Unmounting carousel %p\n", oc);

    // Mark it unmounted
    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(ocGlobalMutex);
    oc->mounted = FALSE;
    mpe_mutexRelease(ocGlobalMutex);
    // END CRITICAL SECTION

    // Unregister it so that other guys can proceed
    ocUnregisterOC(oc);

    // Stop PSI Acquisition
    if (oc->siHandle != MPE_SI_INVALID_HANDLE)
    {
        mpe_siUnRegisterForPSIAcquisition(oc->siHandle);
    }

    // Not a problem checking if we're outside the critical section.  Nobody should be incrementing
    // the counter once mounted == false
    if (oc->referenceCount == 0)
    {
        ocFreeOC(oc);
    }

    return MPE_SUCCESS;
}

/**
 * Is the carousel connected?
 *
 * @param dc            The object carousel to query on.
 * @param connected     [out] Pointer to where to make the connection.
 *
 * @return MPE_SUCCESS
 */
mpe_Error mpe_ocIsConnected(mpe_ObjectCarousel *oc, mpe_Bool *connected)
{
    mpe_Bool lConnected = true;
    int i;

    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(ocGlobalMutex);
    if (oc == NULL || connected == NULL)
    {
        return MPE_EINVAL;
    }

    // Walk the carousels
    // Quit if we find one that isn't connected.
    for (i = 0; lConnected && (i < DC_MAX_DATACAROUSELS); i++)
    {
        if (oc->dataCarousels[i].dc != NULL)
        {
            (void) mpe_dcIsConnected(oc->dataCarousels[i].dc, &lConnected);
        }
    }

    // Done, return our value
    *connected = lConnected;

    mpe_mutexRelease(ocGlobalMutex);
    // END CRITICAL SECTION

    return MPE_SUCCESS;
}

/**
 * Walk a path name and return a: enough information to find the file, and b: the directory and
 * entry which referred to this file.
 *
 * @param oc        The object carousel to look in.
 * @param filename  The filename to look for.
 * @param tap       TAP of the object in which we'll start looking.  Normally the service gateway.
 * @param loc       ObjectLocation of the object we'll start looking in.  Normally the service gateway.
 * @param timeout   Timeout for reads in the object we'll start looking in, in milliseconds.
 * @param targetTap [out] Where to put the tap of the target we're looking for.  Only valid on success.
 * @param targetLoc [out] Where to put the ObjectLocation of the target we're looking for.  Only valid on success.
 * @param dirModule [out] Where to put the module of the directory entry for the file we're looking for.
 *                        Valid on success or service transfer.  Contains the dirObject, dirModule, targetTap, and targetLoc.
 *                        Caller is responsible for dereferencing this module later.
 * @param dirObject [out] where to put the BIOP Object that's the directory containing the directory entry for the
 *                        target object.  Caller is responsible for releasing this object.
 *                        Note: This arg can be removed if we switch to ONLY using the BIOP cache.
 * @param dirEntry  [out] Where to put the Directory entry for the target file.
 * @param residual  [out] Pointer to where to put residual portion of the filename if we encounter a lite options body
 *                        profile object.  Only set on ServiceXfer calls.
 * Note: The dirModule, dirObject, and dirEntry values are only set if a directory entry is found for the object.  If the object
 *       is the root of the search tree, these are NULL.
 * @returns MPE_SUCCESS              if an object with the name is found.
 *          MPE_FS_ERROR_SERVICEXFER if a Lite Options Profile Body object is found in the path.
 *          Various error codes otherwise.
 */
static mpe_Error ocWalkPath(mpe_ObjectCarousel *oc, char *filename,
        ocpTAP *tap, ocpBIOPObjectLocation *loc, uint32_t timeout,
        ocpTAP **targetTap, ocpBIOPObjectLocation **targetLoc,
        mpe_DcModule **dirModule, ocpBIOPObject **dirObject,
        ocpDirEntry **dirEntry, char **residual)
{
    mpe_Error retCode;
    ocpBIOPObject *newObject;
    mpe_DcModule *newModule;
    char *f = filename;

    // Just in case
    *dirObject = NULL;
    *dirModule = NULL;
    *dirEntry = NULL;
    *residual = NULL;
    *targetTap = NULL;
    *targetLoc = NULL;

    while (true)
    {
        // Skip any leading slashes
        while (*f != '\0' && *f == PATH_SEPARATOR)
        {
            f++;
        }

        // If we're at the end of the string, this must be it.
        if (*f == '\0')
        {
            // Fill in the return values;
            *targetTap = tap;
            *targetLoc = loc;
            return MPE_SUCCESS;
        }

        // Find this object in the directory
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                "OC: Total Name: %s Now searching for: %s\n", filename, f);

        // Ok, get this directory object
        retCode = ocGetObject(oc, tap, loc, &newModule, &newObject);
        // Free up the previous object
        // Can't delete it until here, as it might contain the tap and stuff.
        if (*dirObject != NULL || *dirModule != NULL)
        {
            RELEASE_BIOP_OBJECT(*dirObject);
            mpe_dcReleaseModule(*dirModule);
            *dirModule = NULL;
        }
        // Now, check the result of the search
        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                    "OC: Get object failed in walkpath\n");
            // Clear out the return pointers
            *dirObject = NULL;
            *dirModule = NULL;
            *dirEntry = NULL;
            return retCode;
        }
        *dirObject = newObject;
        *dirModule = newModule;

        // Now, look up the directory entry
        retCode = ocFindDirectoryEntry(oc, *dirObject, f, *dirModule, timeout,
                dirEntry);

        // If we got an error, just return it
        // Basically, file not found someplace along the way
        if (retCode != MPE_SUCCESS)
        {
            // Release the module
            mpe_dcReleaseModule(*dirModule);
            // Clear out the return pointers
            RELEASE_BIOP_OBJECT(*dirObject);
            *dirObject = NULL;
            *dirModule = NULL;
            *dirEntry = NULL;
            return retCode;
        }

        // Skip the filename
        while (*f != '\0' && *f != PATH_SEPARATOR)
        {
            f++;
        }

        // Must have a valid directory entry.  Let's look in it.
        if ((*dirEntry)->ior.profile.profileTag == TAG_BIOP)
        {
            loc = &((*dirEntry)->ior.profile.body.biopProfile.objectLoc);
            tap = &((*dirEntry)->ior.profile.body.biopProfile.tap);
            // Convert the timeout on the gateway from microseconds to milliseconds
            // BUG: TODO: Get a better default timeout.
            timeout
                    = OC_TIMEOUT((*dirEntry)->ior.profile.body.biopProfile.tap.timeOut, OC_DSI_TIMEOUT);
        }
        else if ((*dirEntry)->ior.profile.profileTag == TAG_LITE_OPTIONS)
        {
            // Save away the rest of the name
            *residual = f;
            return MPE_FS_ERROR_SERVICEXFER;
        }
        else
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                    "OC: Unexpected type found in directory entry: %x\n",
                    ((*dirEntry)->ior.profile.profileTag));
            // Release the module
            mpe_dcReleaseModule(*dirModule);
            // Clear out the return pointers
            RELEASE_BIOP_OBJECT(*dirObject);
            *dirObject = NULL;
            *dirModule = NULL;
            *dirEntry = NULL;
            return MPE_FS_ERROR_FAILURE;
        }

        // And do it all again
    }
}

/**
 * Walk a path name and return a: enough information to find the file, and b: the directory and
 * entry which referred to this file.  Will traverse any lite options profile body's it finds.
 *
 * @param oc        The object carousel to look in.
 * @param filename  The filename to look for.
 * @param tap       TAP of the object in which we'll start looking.  Normally the service gateway.
 * @param loc       ObjectLocation of the object we'll start looking in.  Normally the service gateway.
 * @param timeout   Timeout for reads in the object we'll start looking in.
 * @param targetOC  [out] Where to put the OC that the target file is in.  If set, the caller is responsible for
 *                        defererencing this OC.
 * @param targetTap [out] Where to put the tap of the target we're looking for.  Only valid on success.
 * @param targetLoc [out] Where to put the ObjectLocation of the target we're looking for.  Only valid on success.
 * @param dirModule [out] Where to put the module of the directory entry for the file we're looking for.
 *                        Valid on success or service transfer.  Contains the dirObject, dirModule, targetTap, and targetLoc.
 *                        Caller is responsible for dereferencing this module later.
 * @param dirObject [out] where to put the BIOP Object that's the directory containing the directory entry for the
 *                        target object.  Caller is responsible for releasing this object.
 *                        Note: This arg can be removed if we switch to ONLY using the BIOP cache.
 * @param dirEntry  [out] Where to put the Directory entry for the target file.
 * Note: The dirModule, dirObject, and dirEntry values are only set if a directory entry is found for the object.  If the object
 *       is the root of the search tree, these are NULL.
 * @returns MPE_SUCCESS               if an object with the name is found.
 *          MPE_FS_ERROR_TOOMANYLINKS if too many links are traversed, indicating a possible loop.
 *          Various error codes otherwise.
 */
static mpe_Error ocWalkPathResolvingLinks(mpe_ObjectCarousel *oc,
        char *filename, ocpTAP *tap, ocpBIOPObjectLocation *loc,
        uint32_t timeout, mpe_ObjectCarousel **targetOc, ocpTAP **targetTap,
        ocpBIOPObjectLocation **targetLoc, mpe_DcModule **dirModule,
        ocpBIOPObject **dirObject, ocpDirEntry **dirEntry)
{
    mpe_Error retCode;
    mpe_ObjectCarousel *newOC;
    uint32_t numLinks = 0;
    char *newName = NULL;
    char *oldName = NULL; // This will only be non-null on the second loop around, so that
    // we know to delete it.
    char *residualName;

    while (true)
    {
        *targetOc = oc;
        retCode = ocWalkPath(oc, filename, tap, loc, timeout, targetTap,
                targetLoc, dirModule, dirObject, dirEntry, &residualName);
        // If the error code is something that's not a link, just return
        if (retCode != MPE_FS_ERROR_SERVICEXFER)
        {
            if (oldName != NULL)
            {
                mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, oldName);
            }
            return retCode;
        }
        else
        {
            numLinks++;
            if (numLinks >= MAX_LINKS)
            {
                RELEASE_BIOP_OBJECT(*dirObject);
                mpe_dcReleaseModule(*dirModule);
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                        "OC: Too many links in path\n");
                return MPE_FS_ERROR_TOOMANYLINKS;
            }

            // Find the remote carousel amongst our mounted carousels
            retCode
                    = mpe_ocFindCarouselByNSAPAddress(
                            (*dirEntry)->ior.profile.body.liteOptionsProfile.addressNSAP,
                            &newOC);
            if (retCode != MPE_SUCCESS)
            {
                MPE_LOG(
                        MPE_LOG_ERROR,
                        MPE_MOD_FILESYS,
                        "OC: Couldn't find mounted OC corresponding to NSAP %s in LiteOptionsBodyProfile\n",
                        ocNSAPString(
                                (uint8_t *) ((*dirEntry)->ior.profile.body.liteOptionsProfile.addressNSAP)));
                // Now we're done with the BIOP object, toss it.
                RELEASE_BIOP_OBJECT(*dirObject);
                mpe_dcReleaseModule(*dirModule);
                return retCode;
            }

            // Find the revert the path
            retCode = ocResolveLiteOptionsName(*dirEntry, oc->iso88591,
                    residualName, &newName);
            if (retCode != MPE_SUCCESS)
            {
                MPE_LOG(
                        MPE_LOG_ERROR,
                        MPE_MOD_FILESYS,
                        "OC: Couldn't resolve LiteOptions file name: %s.  Error code: %4x\n",
                        residualName, retCode);
                mpe_ocDereferenceOC(newOC);
                if (oldName != NULL)
                {
                    mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, oldName);
                }
                return retCode;
            }
            if (oldName != NULL)
            {
                mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, oldName);
            }

            MPE_LOG(
                    MPE_LOG_DEBUG,
                    MPE_MOD_FILESYS,
                    "OC: Filename in target %s : %s\n",
                    ocNSAPString(
                            (uint8_t *) ((*dirEntry)->ior.profile.body.liteOptionsProfile.addressNSAP)),
                    newName);

            // Free up the things I'm not using anymore.
            // These will be replaced on the next loop
            RELEASE_BIOP_OBJECT(*dirObject);
            mpe_dcReleaseModule(*dirModule);
            mpe_ocDereferenceOC(oc);

            // Set up for the next loop.
            oldName = newName;
            filename = newName;
            oc = newOC;
            tap = &(oc->gatewayTAP);
            loc = &(oc->gatewayLoc);
            timeout = oc->gatewayTimeout;

        }
    }
}

/**
 * Open a file for reading.
 *
 * @param oc The object carousel the file exists within.
 * @param filename The full pathname of the file to open, relative to the object carousel root.
 * @param file Output pointer to fill in with the filename.
 *
 * @returns MPE_SUCCESS if the file is opened, error codes otherwise.
 */
mpe_Error mpe_ocOpenFile(mpe_ObjectCarousel *oc, char *filename,
        mpe_Bool followLinks, mpe_DcCacheMode cacheMode, mpe_OcFile **retFile)
{
    mpe_Error retCode = MPE_SUCCESS;
    ocpBIOPObject *dirObject = NULL;
    mpe_DcModule *dirModule = NULL;
    ocpDirEntry *dirEntry = NULL;
    mpe_OcFile *file = NULL;
    ocpTAP *targetTap = NULL;
    ocpBIOPObjectLocation *targetLoc = NULL;
    char *sink;

    MPE_UNUSED_PARAM(cacheMode); /* TODO: if this param is not used, should it be removed from this function? */

    // Verify that the parameters are at least set
    if (oc == NULL || filename == NULL || retFile == NULL)
    {
        return MPE_EINVAL;
    }

    // Clear out the response, just in case.
    *retFile = NULL;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "OC: Attempting to open file %s\n",
            filename);

    // Increment the reference count
    if (!mpe_ocReferenceOC(oc))
    {
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILESYS,
                "OC: Cannot open file %s in an unmounted carousel\n", filename);
        return MPE_EINVAL;
    }

    // Cons up the ocfile object
    retCode = mpeos_memAllocP(MPE_MEM_FILE_CAROUSEL, sizeof(mpe_OcFile),
            (void **) &file);
    if (retCode != MPE_SUCCESS)
    {
        // If we couldn't allocate it, get out.
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILESYS,
                "OC: Couldn't allocate file object %s\n", filename);
        mpe_ocDereferenceOC(oc);
        // Return here, as we don't want to go through the whole cleanup loop yet.
        return retCode;
    }
    memset(file, 0, sizeof(mpe_OcFile));

    // Find the object by name
    // This only gives us the directory entry, and the "pointer" to the target object.
    // When we get back, oc could be dereferenced, so we won't use it anymore.
    // If it hasn't been, it's been copied to file->oc anyhow.
    if (followLinks)
    {
        retCode = ocWalkPathResolvingLinks(oc, filename, &(oc->gatewayTAP),
                &(oc->gatewayLoc), oc->gatewayTimeout, &(file->oc), &targetTap,
                &targetLoc, &dirModule, &dirObject, &dirEntry);
    }
    else
    {
        file->oc = oc;
        retCode = ocWalkPath(oc, filename, &(oc->gatewayTAP),
                &(oc->gatewayLoc), oc->gatewayTimeout, &targetTap, &targetLoc,
                &dirModule, &dirObject, &dirEntry, &sink);
    }
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILESYS,
                "OC: Couldn't find file %s: %04x\n", filename, retCode);
        if (dirModule != NULL)
        {
            RELEASE_BIOP_OBJECT(dirObject);
            mpe_dcReleaseModule(dirModule);
        }
        goto CleanUp;
    }

    // Now, actually acquire the object we want
    retCode = ocGetObject(file->oc, targetTap, targetLoc, &(file->module),
            &(file->biopObject));
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILESYS,
                "OC: Couldn't open file %s: %04x\n", filename, retCode);
        goto CleanUp;
    }
#ifdef OCP_DUMP_OBJECTS
    ocpDumpBIOPObject(file->biopObject);
#endif

    // MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "OC: Setup file structure\n");
    // Put the important information into the object
    if (dirEntry != NULL)
    {
        // If we're here, we MUST have a tagged profile, so we don't check
        // BUG: TODO: Get a better timeout
        file->timeout
                = OC_TIMEOUT(dirEntry->ior.profile.body.biopProfile.tap.timeOut, OC_DSI_TIMEOUT);
    }
    else
    {
        file->timeout = oc->gatewayTimeout;
    }

    switch (file->biopObject->objectType)
    {
    case BIOP_Directory:
    case BIOP_ServiceGateway:
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "OC: Setting type Directory\n");
        file->fileType = Type_Directory;
        file->length = file->biopObject->payload.dir.numDirEntries;
        break;
    case BIOP_File:
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "OC: Setting type File\n");
        file->fileType = Type_File;
        // Copy the file and data offset values up to here.
        file->dataOffset = file->biopObject->payload.file.dataOffset;
        file->length = file->biopObject->payload.file.dataLength;
        break;
    case BIOP_Stream:
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "OC: Setting type Stream\n");
        file->fileType = Type_Stream;
        file->length = 0;
        break;
    case BIOP_StreamEvent:
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                "OC: Setting type StreamEvent\n");
        file->fileType = Type_StreamEvent;
        file->length = file->biopObject->payload.streamEvent.eventCount;
        break;
    default:
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILESYS,
                "OC: Unknown File Type for %s: %04x\n", filename,
                file->biopObject->objectType);
        retCode = MPE_EINVAL;
        goto CleanUp;
    }
    RELEASE_BIOP_OBJECT(dirObject);
    mpe_dcReleaseModule(dirModule);
    *retFile = file;
    return retCode;
    CleanUp: MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILESYS,
            "OC: Cleaning file %s open failure: Dir: %p %p: File: %p %p %p\n",
            filename, dirObject, dirModule, file, file->biopObject,
            file->module);
    RELEASE_BIOP_OBJECT(dirObject);
    mpe_dcReleaseModule(dirModule);
    RELEASE_BIOP_OBJECT(file->biopObject);
    mpe_dcReleaseModule(file->module);
    mpe_ocDereferenceOC(file->oc);
    ocFreeFile(file);
    return retCode;
}

/**
 * This function returns the directory entry for a given filename.  Will walk LiteOptionsProfileBody's
 * to other object carousels.
 *
 * @param oc        The object carousel containing the file.
 * @param filename  The name of the file to search for.
 * @param retEntry  [out] Pointer to where to put a pointer to the read directory entry.
 */
mpe_Error mpe_ocGetDirectoryEntry(mpe_ObjectCarousel *oc, char *filename,
        mpe_OcDirEntry **retEntry)
{
    mpe_Error retCode;
    ocpBIOPObject *dirObject = NULL;
    mpe_DcModule *dirModule = NULL;
    ocpDirEntry *dirEntry = NULL;
    ocpTAP *targetTap = NULL;
    ocpBIOPObjectLocation *targetLoc = NULL;

    // Verify that the parameters are at least set
    if (oc == NULL || filename == NULL || retEntry == NULL)
    {
        return MPE_EINVAL;
    }

    // Clear out the response, just in case.
    *retEntry = NULL;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
            "OC: Attempting to get directory entry for %s\n", filename);

    // Increment the reference count
    if (!mpe_ocReferenceOC(oc))
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OC: Cannot open file %s in an unmounted carousel\n", filename);
        return MPE_EINVAL;
    }

    // Find the object by name
    // This only gives us the directory entry, and the "pointer" to the target object.
    // When we get back, oc could be dereferenced, so we won't use it anymore.
    // If it hasn't been, it's been copied to file->oc anyhow.
    retCode = ocWalkPathResolvingLinks(oc, filename, &(oc->gatewayTAP),
            &(oc->gatewayLoc), oc->gatewayTimeout, &oc, &targetTap, &targetLoc,
            &dirModule, &dirObject, &dirEntry);
    if (retCode != MPE_SUCCESS)
    {
        if (dirModule != NULL)
        {
            RELEASE_BIOP_OBJECT(dirObject);
            mpe_dcReleaseModule(dirModule);
        }
        // goto CleanUp;
        return retCode;
    }
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
            "OC: Got Dir Entry for %s.  Building return entry\n", filename);

    retCode = ocMakeDirEntry(dirEntry, oc, retEntry);

    // Release the OC and the directory module.
    RELEASE_BIOP_OBJECT(dirObject);
    mpe_dcReleaseModule(dirModule);
    mpe_ocDereferenceOC(oc);

    return retCode;
}

/**
 * Close a file and delete all the data structures corresponding to it.
 *
 * @param file The file pointer to close.
 *
 * @returns MPE_SUCCESS.
 */
mpe_Error mpe_ocCloseFile(mpe_OcFile *file)
{
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "OC: Closing file: %p\n", file);
    if (file != NULL)
    {
        mpe_dcReleaseModule(file->module);
        mpe_ocDereferenceOC(file->oc);
        ocFreeFile(file);
    }
    return MPE_SUCCESS;
}

/**
 * Prefetch a file based on the name.  Grads the entire module containing the file.
 *
 * @param oc            The object carousel to add the tap to.
 * @param fileName      The file name to prefetch.
 *
 * @returns MPE_SUCCESS if the tap can be added, or is already there.  Error codes if it can't be added.
 */
mpe_Error mpe_ocPrefetchFile(mpe_ObjectCarousel *oc, char *fileName)
{
    mpe_Error retCode;
    ocpBIOPObject *dirObject = NULL;
    mpe_DcModule *dirModule = NULL;
    ocpDirEntry *dirEntry = NULL;
    ocpTAP *targetTap = NULL;
    ocpBIOPObjectLocation *targetLoc = NULL;
    mpe_DataCarousel *dc = NULL;
    mpe_DcModule *module = NULL;

    // Reference the OC so it doesn't go away.
    if (!mpe_ocReferenceOC(oc))
    {
        return MPE_EINVAL;
    }
    MPE_LOG(MPE_LOG_INFO, MPE_MOD_FILESYS, "OC: Prefetching %s\n", fileName);

    // Find the object by name
    // This only gives us the directory entry, and the "pointer" to the target object.
    // When we get back, oc could be dereferenced, so we won't use it anymore.
    // If it hasn't been, it's been copied to file->oc anyhow.
    retCode = ocWalkPathResolvingLinks(oc, fileName, &(oc->gatewayTAP),
            &(oc->gatewayLoc), oc->gatewayTimeout, &oc, &targetTap, &targetLoc,
            &dirModule, &dirObject, &dirEntry);
    if (retCode != MPE_SUCCESS)
    {
        goto CleanUp;
    }

    // Get the data carousel
    retCode = ocGetDataCarousel(oc, targetTap, &dc);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OC: Could not get data carousel");
        goto CleanUp;
    }

    // Get the module in the data carousel
    retCode = mpe_dcGetModule(dc, targetLoc->moduleID, &module);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OC: Could not get module %04x\n", targetLoc->moduleID);
        goto CleanUp;
    }

    // Start the prefetch
    (void) mpe_dcPrefetchModule(module);
    // And we're done
    mpe_dcReleaseModule(module);

    CleanUp:
    // Release the OC and the directory module.
    RELEASE_BIOP_OBJECT(dirObject);
    mpe_dcReleaseModule(dirModule);
    mpe_ocDereferenceOC(oc);
    // See 'ya
    return retCode;
}

/**
 * Read data from an object carousel file.  This will only work if the file type
 * is a file.  It will not work if the file is a directory or a stream.  Directories
 * should be read via the mpe_ocReadDirectoryEntry() function.
 *
 * @param file The file to read the data from.
 * @param start The location at which to start reading.
 * @param length The number of bytes to read.
 * @param output An array with which to fill with data. [output]
 * @param bytesRead Output parameter to fill with number of bytes read.
 *
 * @returns MPE_SUCCESS if the read completes, error codes otherwise.
 */
mpe_Error mpe_ocReadFile(mpe_OcFile *file, uint32_t start, uint32_t length,
        uint8_t *output, uint32_t *bytesRead)
{
    mpe_Error retCode;

    // Make sure it's a file
    if (file->fileType != Type_File)
    {
        return MPE_EINVAL;
    }

    // Make sure we're reading in the file itself
    if (start > file->length)
    {
        *bytesRead = 0;
        return MPE_EINVAL;
    }

    // Make sure we don't read off of the end of the file
    if (start + length > file->length)
    {
        length = file->length - start;
    }

    // actually read the data
    retCode = mpe_dcReadModuleBytes(file->module, start + file->dataOffset,
            length, file->timeout, output, bytesRead);

    return retCode;
}

/**
 * Get the file type of a file.
 *
 * @param oc The object carousel to look in.
 * @param file The file to get the path of.
 * @param fileType Output parameter of the file to be typed.  Return value is invalid if
 *                 not successful.
 *
 * @returns MPE_SUCCESS if the file exists and can be typed, error codes otherwise.
 */
mpe_Error mpe_ocGetFileType(mpe_OcFile *file, mpe_OcFileType *fileType)
{
    *fileType = file->fileType;
    return MPE_SUCCESS;
}

/**
 * This functions returns an element in the directory of an object carousel.
 *
 * @param dir The directory to read the directory out of.
 *
 * @returns MPE_SUCCESS if successful, MPE_EINVAL if the file is not a directory,
 *          error codes otherwise.
 */
mpe_Error mpe_ocReadDirectoryEntry(mpe_OcFile *dir, uint32_t entry,
        mpe_OcDirEntry **retEntry)
{
    mpe_Error retCode = MPE_SUCCESS;

    // Clear out return value for the error case.
    *retEntry = NULL;

    // Make sure it's a directory object
    if (dir->fileType != Type_Directory)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS, "OC: Not a directory\n");
        return MPE_EINVAL;
    }

    // Make sure the entry number isn't too big
    if (entry >= dir->biopObject->payload.dir.numDirEntries)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_FILESYS,
                "OC: Entry %d is too big, directory only contains %d entries\n",
                entry, dir->biopObject->payload.dir.numDirEntries);
        return MPE_FS_ERROR_EOF;
    }

    // Load the directory entries.  We'll be wanting them
    if (dir->biopObject->payload.dir.dirEntries == NULL)
    {
        retCode = ocLoadDirectoryEntries(dir->oc, dir->biopObject, dir->module,
                dir->timeout);
        if (retCode != MPE_SUCCESS)
        {
            return retCode;
        }
    }

    // Fill in the return structure
    return ocMakeDirEntry(&(dir->biopObject->payload.dir.dirEntries[entry]),
            dir->oc, retEntry);
}

/**
 * This functions returns an event from the StreamEvent.
 *
 * @param stream The StreamEvent to read the entry out of.
 * @param entry  The entry to read.
 * @param retEntry  [out] Pointer to where to put a pointer to the returned StreamEvent entry.
 *                  Caller is responsible for freeing this structure.
 *
 * @returns MPE_SUCCESS if successful, MPE_EINVAL if the file is not a stream,
 *          error codes otherwise.
 */
mpe_Error mpe_ocReadStreamEventEntry(mpe_OcFile *stream, uint32_t entry,
        mpe_OcStreamEventEntry **retEntry)
{
    mpe_Error retCode = MPE_SUCCESS;
    mpe_OcStreamEventEntry *event;
    uint8_t *name;
    uint8_t utf8Buffer[2 * MAX_OC_NAMELEN + 1];

    // Clear out return value for the error case.
    *retEntry = NULL;

    // Make sure it's a StreamEvent object
    if (stream->fileType != Type_StreamEvent)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS, "OC: Not a stream event\n");
        return MPE_EINVAL;
    }

    // Make sure the entry number isn't too big
    if (entry >= stream->biopObject->payload.streamEvent.eventCount)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_FILESYS,
                "OC: Entry %d is too big, StreamEvent only contains %d events\n",
                entry, stream->biopObject->payload.streamEvent.eventCount);
        return MPE_FS_ERROR_EOF;
    }

    if (stream->oc->iso88591)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                "OC: Converting ISO name %s to UTF-8\n",
                stream->biopObject->payload.streamEvent.eventNames[entry]);
        ocuIso88591ToUtf8(
                (uint8_t *) stream->biopObject->payload.streamEvent.eventNames[entry],
                utf8Buffer);
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                "OC: Name conversion done: %s Characters: %d\n", utf8Buffer,
                strlen((char *) utf8Buffer));
        name = (uint8_t*) utf8Buffer;
    }
    else
    {
        name
                = (uint8_t*) &(stream->biopObject->payload.streamEvent.eventNames[entry]);
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                "OC: Keeping name the same: %s\n", name);
    }

    // Malloc up the structure.
    // Include space at the end to free up things
    retCode = mpeos_memAllocP(MPE_MEM_FILE_CAROUSEL,
            (sizeof(mpe_OcStreamEventEntry) + strlen((char *) name) + 1),
            (void **) &event);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OC: Unable to allocate StreamEntry structure\n");
        return retCode;
    }

    // Copy in the name
    strcpy(event->name, (char *) name);
    // And the size
    event->eventID = stream->biopObject->payload.streamEvent.eventIDs[entry];

    *retEntry = event;
    return retCode;
}

/**
 * This function causes a file to be unloaded, it's memory marked as eligible
 * for reallocation.  Can be applied to any file or directory, even if it wasn't
 * previously loaded.
 *
 * @param file The file to unload.
 *
 * @returns MPE_SUCCESS
 */
mpe_Error mpe_ocUnloadFile(mpe_OcFile *file)
{
    // BUG: FIXME: TODO: If more than one file in a module is loaded, then
    // unloading one file would unload the others.  Probably need to reference
    // count the load requests in the DC.
    return mpe_dcUnloadModule(file->module);
}

/**
 * Return the size of a file.  Only works with file objects.
 *
 * @param file The file to get the size of.
 * @param size Pointer to a variable to return the size in.
 *
 * @returns MPE_SUCCESS if successful, error codes otherwise.
 */
mpe_Error mpe_ocGetFileSize(mpe_OcFile *file, uint32_t *size)
{
    *size = file->length;
    return MPE_SUCCESS;
}

/**
 * Is the file current (ie, up-to-date).
 *
 * @param file      The file to check.
 * @param isCurrent [out] Output parameter, where to put the result.
 *
 * @Returns MPE_SUCCESS if the check could be made.  Errors otherwise.
 */
mpe_Error mpe_ocIsCurrent(mpe_OcFile *file, mpe_Bool *isCurrent)
{
    return mpe_dcCheckModuleCurrent(file->module, isCurrent);
}

/**
 * Get the version of a file in the object carousel.  Returns the version
 * number of the module containing file object.
 *
 * @param file      The file to get the version of.
 * @param version   [out] Pointer to where to indicate the version number.
 *
 * @returns MPE_SUCCESS if the version is found, error codes otherwise.
 */
mpe_Error mpe_ocGetVersion(mpe_OcFile *file, uint32_t *version)
{
    if (file == NULL || version == NULL)
    {
        return MPE_EINVAL;
    }

    return mpe_dcGetModuleVersion(file->module, version);
}

/**
 * Return whether a file is in a loaded module.
 *
 * @param file          The file to get the status of.
 * @param isLoaded      [out] Pointer to where to put the status.
 */
mpe_Error mpe_ocGetFileLoadedStatus(mpe_OcFile *file, mpe_Bool *isLoaded)
{
    if (file == NULL || isLoaded == NULL)
    {
        return MPE_EINVAL;
    }
    return mpe_dcGetModuleLoadedStatus(file->module, isLoaded);
}

/**
 * Return whether this stream has a certain type of stream.  Audio, Video, or MPEG Program.
 *
 * @param file          The Stream "file" to check for this type.
 * @param isType        The type to determine if this stream is
 * @param result        [out] Boolean, does this stream have the right type
 *
 * @returns MPE_SUCCESS if it could be determined if this type is there.
 */
mpe_Error mpe_ocIsStreamType(mpe_OcFile *file, mpe_OcStreamType isType,
        mpe_Bool *result)
{
    ocpBIOPObject *biopObject; // The BIOP object embedded in the file

    // Make sure it's a stream
    if (((file->fileType != Type_Stream)
            && (file->fileType != Type_StreamEvent)) || result == NULL)
    {
        return MPE_EINVAL;
    }

    // Let's be a bit quicker, fewer deref's
    biopObject = file->biopObject;

    // Ugly.  We could actually do this without having one for each file
    // type, but this is correct.  Inheritance would be nice.
    if (file->fileType == Type_Stream)
    {
        switch (isType)
        {
        case StreamType_Audio:
            *result = (biopObject->payload.stream.sInfo.audio != 0);
            break;
        case StreamType_Video:
            *result = (biopObject->payload.stream.sInfo.video != 0);
            break;
        case StreamType_Data:
            *result = (biopObject->payload.stream.sInfo.data != 0);
            break;
        }
    }
    else
    {
        switch (isType)
        {
        case StreamType_Audio:
            *result = (biopObject->payload.streamEvent.sInfo.audio != 0);
            break;
        case StreamType_Video:
            *result = (biopObject->payload.streamEvent.sInfo.video != 0);
            break;
        case StreamType_Data:
            *result = (biopObject->payload.streamEvent.sInfo.data != 0);
            break;
        }

    }

    return MPE_SUCCESS;
}

/**
 * Get the duration of the stream in milliseconds.  Converts it from the seconds + microSeconds
 * that the stream uses internally.
 *
 * @param file      The stream to use.
 * @param duration  [out] Where to put the duration.
 *
 * @returns MPE_SUCCESS if it successfully calculated the duration, MPE_EINVAL if the arguments are wrong.
 */
mpe_Error mpe_ocGetStreamDuration(mpe_OcFile *file, uint64_t *duration)
{
    ocpBIOPObject *biopObject; // The BIOP object embedded in the file

    // Make sure it's a stream
    if (((file->fileType != Type_Stream)
            && (file->fileType != Type_StreamEvent)) || duration == NULL)
    {
        return MPE_EINVAL;
    }
    biopObject = file->biopObject;
    *duration = ((biopObject->payload.stream.sInfo.durationSeconds
            * (uint64_t) 1000)
            + (biopObject->payload.stream.sInfo.durationMicroSeconds
                    / (uint64_t) 1000));

    return MPE_SUCCESS;
}

/**
 * Return info about where to find the carousel.
 *
 * @param file          The stream we're working with.
 * @param freq          [out] Pointer to where to fill in frequency from the TAP.
 * @param prog          [out] Pointer to where to fill in program from the TAP.
 * @param qam           [out] Pointer to where to fill in QAM mode from the TAP.
 * @param sourceID      [out] Pointer to where to fill in the sourceID, if it exists.  -1 if not.
 *
 * @returns MPE_SUCCESS
 */
mpe_Error mpe_ocGetTuningInfo(mpe_OcFile *file, uint32_t *freq, uint32_t *prog,
        mpe_SiModulationMode *qam, uint32_t *sourceId)
{
    mpe_Error retCode;

    // Make sure it's a stream
    if (file == NULL || freq == NULL || prog == NULL || qam == NULL || sourceId
            == NULL)
    {
        return MPE_EINVAL;
    }

    // Found it, translate the association tag.
    retCode = mpe_siLockForRead();
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OCU: Could not get access to SI DB Handle \n");
        return retCode;
    }
    retCode = mpe_siGetFrequencyForServiceHandle(file->oc->siHandle, freq);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS, "OC: Could not get Frequency\n");
        goto GetOut;
    }
    retCode = mpe_siGetProgramNumberForServiceHandle(file->oc->siHandle, prog);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OC: Could not get Program Number\n");
        goto GetOut;
    }
    retCode
            = mpe_siGetModulationFormatForServiceHandle(file->oc->siHandle, qam);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS, "OC: Could not get QAM Mode\n");
        goto GetOut;
    }
    retCode = mpe_siGetSourceIdForServiceHandle(file->oc->siHandle, sourceId);
    if (retCode != MPE_SUCCESS)
    {
        // No source ID, dynamic service.  Set it to 0.
        *sourceId = 0;
    }

    GetOut: mpe_siUnLock();
    return retCode;
}

/**
 * Return the number of taps of a given type.
 *
 * @param file          The stream we're working with.
 * @param tapType       The tap type to look for.  MPE_FS_OC_ALL_TAPS to get all the taps.
 * @param numTaps       [out] Pointer to where to put the
 *
 * @returns MPE_SUCCESS if returning a valid number, error codes otherwise.
 */
mpe_Error mpe_ocGetNumTaps(mpe_OcFile *file, uint16_t tapType,
        uint32_t *numTaps)
{
    uint32_t count = 0;
    uint32_t i;
    uint32_t limit;

    if (file == NULL || numTaps == NULL)
    {
        return MPE_EINVAL;
    }
    // Make sure it's a stream
    if (file->fileType != Type_Stream && file->fileType != Type_StreamEvent)
    {
        return MPE_EINVAL;
    }
    // How many to walk.  Cache it in a register for speed.
    limit = file->biopObject->payload.stream.tapsCount;

    // If all taps, just return it.
    if (tapType == MPE_FS_OC_ALL_TAPS)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                "OC: TapType == All Taps.  Returning %d\n", limit);
        *numTaps = limit;
        return MPE_SUCCESS;
    }

    // Otherwise, count the number of taps of this type.
    for (i = 0; i < limit; i++)
    {
        if (file->biopObject->payload.stream.taps[i].use == tapType)
        {
            count++;
        }
    }

    *numTaps = count;
    return MPE_SUCCESS;
}

/**
 * Return the tag value of a tap of a given type.
 * @param file          The stream we're working with.
 * @param tapType       Type of the TAP we're looking for.  Only finds the first TAP for any type.
 * @param tapInstance   Occurrance of the TAP we're looking for.
 * @param tapTag        [out] Pointer to where to fill in the tag from the TAP.
 * @param tapId         [out] Pointer to where to fill in the ID from the TAP.
 *
 * @returns MPE_SUCCESS if found, MPE_NODATA if not, other error codes as appropriate.
 */
mpe_Error mpe_ocGetTap(mpe_OcFile *file, uint16_t tapType,
        uint32_t tapInstance, uint16_t *tapTag, uint16_t *tapId)
{
    ocpBIOPObject *biopObject; // The BIOP object embedded in the file
    int i;

    if (file == NULL || tapTag == NULL)
    {
        return MPE_EINVAL;
    }
    // Make sure it's a stream
    if (file->fileType != Type_Stream && file->fileType != Type_StreamEvent)
    {
        return MPE_EINVAL;
    }

    // Let's be a bit quicker, fewer deref's
    biopObject = file->biopObject;

    // Walk the list of taps.
    // Check each one to see if it's the N'th of this type
    for (i = 0; i < biopObject->payload.stream.tapsCount; i++)
    {
        if (tapType == MPE_FS_OC_ALL_TAPS
                || biopObject->payload.stream.taps[i].use == tapType)
        {
            if (tapInstance == 0)
            {
                // Fill in the output values
                *tapTag = biopObject->payload.stream.taps[i].assocTag;
                *tapId = biopObject->payload.stream.taps[i].id;
                return MPE_SUCCESS;
            }
            else
            {
                tapInstance--;
            }
        }
    }

    // Found nothing, return No Data
    return MPE_ENODATA;
}

/**
 * Retrieve the content type from a file.
 *
 * @param file      The File object to get the content type from.
 * @param size      Max number of bytes to copy (size of the buffer)
 * @param buffer    A buffer of at least size bytes.  To be filled.
 *
 * @return MPE_SUCCESS if a value was retrieved, even if it was null.  MPE_EINVAL if anything is broken.
 */
mpe_Error mpe_ocGetContentType(mpe_OcFile *file, uint32_t size, char *buffer)
{
    if (file == NULL || buffer == NULL || file->biopObject == NULL)
    {
        return MPE_EINVAL;
    }
    buffer[0] = '\0'; // Null out the output string.
    if (file->biopObject->payload.file.contentType != NULL)
    {
        strncpy(buffer, (char *) file->biopObject->payload.file.contentType,
                size);
        buffer[size] = '\0';
    }
    return MPE_SUCCESS;
}

/**
 * Resolve a pathname that contains a LiteOptionsProfileBody object.
 *
 * @param oc            The object carousel to start in.
 * @param fileName      The pathname to resolve.
 * @param maxLength     Number of bytes in the target name buffer.
 * @param targetName    [out] Buffer to be filled with the name.  At least maxLength bytes long.
 * @param nsap          [out] Buffer to fill with the NSAP address.  Must be 20 bytes long.
 */
mpe_Error mpe_ocResolveFilename(mpe_ObjectCarousel *oc, char *fileName,
        uint32_t maxLength, char *targetName, uint8_t *targetNSAP)
{
    mpe_Error retCode;
    char *residualName;
    char *newName;
    ocpTAP *targetTap;
    ocpBIOPObjectLocation *targetLoc;
    mpe_DcModule *dirModule;
    ocpBIOPObject *dirObject;
    ocpDirEntry *dirEntry;

    // Check the args a bit.
    if (oc == NULL || fileName == NULL || targetName == NULL || targetNSAP
            == NULL || maxLength == 0)
    {
        return MPE_EINVAL;
    }

    retCode = ocWalkPath(oc, fileName, &(oc->gatewayTAP), &(oc->gatewayLoc),
            oc->gatewayTimeout, &targetTap, &targetLoc, &dirModule, &dirObject,
            &dirEntry, &residualName);

    if (retCode == MPE_FS_ERROR_SERVICEXFER)
    {
        // Find the revert the path
        retCode = ocResolveLiteOptionsName(dirEntry, oc->iso88591,
                residualName, &newName);
        MPE_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_FILESYS,
                "OC: Name %s (%s) is %s in %s\n",
                fileName,
                residualName,
                newName,
                ocNSAPString(
                        dirEntry->ior.profile.body.liteOptionsProfile.addressNSAP));

        if (retCode == MPE_SUCCESS)
        {
            // Copy the name to the target
            strncpy(targetName, newName, maxLength);
            targetName[maxLength] = '\0';
            // And the NSAP address
            memcpy(targetNSAP,
                    dirEntry->ior.profile.body.liteOptionsProfile.addressNSAP,
                    OCP_DSI_SERVERID_LENGTH);
        }
        else
        {
            MPE_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_FILESYS,
                    "OC: Couldn't resolve LiteOptions file name: %s.  Error code: %4x\n",
                    residualName, retCode);
        }
        // Free up the things we don't need
        mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, newName);
        RELEASE_BIOP_OBJECT(dirObject);
        mpe_dcReleaseModule(dirModule);
    }
    else if (retCode == MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OC: Found path %s, but not LiteOptions, no service XFER\n",
                fileName);
        // Free up the things we don't need
        RELEASE_BIOP_OBJECT(dirObject);
        mpe_dcReleaseModule(dirModule);

        retCode = MPE_FS_ERROR_FAILURE;
    }
    else
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OC: Error walking path %s: %d\n", fileName, retCode);
    }
    return retCode;
}

/**
 * Find an object carousel by the NSAP address.  When found, the carousel is referenced, and must
 * be dereferenced later.
 *
 * @param NSAP The address to search for.
 * @param retOc [out] Pointer to where to return the object carousel, if found.
 *
 * @returns MPE_SUCCESS if found, MPE_FS_ERROR_SERVICEXFER if not.
 */
mpe_Error mpe_ocFindCarouselByNSAPAddress(uint8_t *nsap,
        mpe_ObjectCarousel **retOC)
{
    mpe_Error retCode = MPE_FS_ERROR_SERVICEXFER;
    int i;

    *retOC = NULL;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
            "OC: Searching for Carousel with NSAP %s\n", ocNSAPString(nsap));
    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(ocGlobalMutex);

    for (i = 0; i < MAX_OBJECTCAROUSELS; i++)
    {
        if (ocObjectCarousels[i] != NULL)
        {
            // Debugging statement.
            // Hopefully dead code eliminator will eliminate it in production.
            if (ocObjectCarousels[i]->mounted)
            {
                MPE_LOG(
                        MPE_LOG_DEBUG,
                        MPE_MOD_FILESYS,
                        "OC: Checking position %d: NSAP %s\n",
                        i,
                        ocNSAPString(
                                (uint8_t *) (ocObjectCarousels[i]->nsapAddress)));
            }
            if (ocObjectCarousels[i]->mounted && memcmp(
                    ocObjectCarousels[i]->nsapAddress, nsap,
                    OCP_DSI_SERVERID_LENGTH) == 0)
            {
                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                        "OC: Found in position %d\n", i);
                // Increment the reference count for the target OC
                ocObjectCarousels[i]->referenceCount++;
                *retOC = ocObjectCarousels[i];
                retCode = MPE_SUCCESS;
                break;
            }
        }
    }

    mpe_mutexRelease(ocGlobalMutex);
    // END CRITICAL SECTION
    return retCode;
}

/**
 * Set an event queue to respond to when anything in the object carousel changes.
 *
 * @param oc        The object carousel to set the change on.
 * @param queue     The queue to assign.
 * @param data      The data to send back with the version updated event.
 *
 * @returns MPE_SUCCESS if the version change is set correctly.
 */
mpe_Error mpe_ocSetChangeNotification(mpe_ObjectCarousel *oc,
        mpe_EventQueue queue, void *data)
{
    uint32_t i;
    if (oc == NULL)
    {
        return MPE_EINVAL;
    }

    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(ocGlobalMutex);

    oc->updateQueue = queue;
    oc->updateData = data;

    // Now, set this into the object carousels.
    for (i = 0; i < DC_MAX_DATACAROUSELS; i++)
    {
        if (oc->dataCarousels[i].dc != NULL)
        {
            (void) mpe_dcRegisterChangeNotification(oc->dataCarousels[i].dc,
                    queue, data);
        }
    }

    mpe_mutexRelease(ocGlobalMutex);
    // END  CRITICAL SECTION

    return MPE_SUCCESS;
}

/**
 * Unregister any event queue currently registered with this obejct carousel
 *
 * @param oc        The object carousel to set the change on.
 *
 * @returns MPE_SUCCESS if the registered event queue is unregistered
 */
mpe_Error mpe_ocUnsetChangeNotification(mpe_ObjectCarousel *oc)
{
    uint32_t i;
    if (oc == NULL)
    {
        return MPE_EINVAL;
    }

    if (oc->updateQueueRegistered)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                "OC: Unsetting version change notification\n");
        // BEGIN CRITICAL SECTION
        mpe_mutexAcquire(ocGlobalMutex);

        // Now, set this into the object carousels.
        for (i = 0; i < DC_MAX_DATACAROUSELS; i++)
        {
            if (oc->dataCarousels[i].dc != NULL)
            {
                (void) mpe_dcUnregisterChangeNotification(
                        oc->dataCarousels[i].dc, oc->updateQueue);
            }
        }

        oc->updateQueueRegistered = FALSE;

        mpe_mutexRelease(ocGlobalMutex);
        // END  CRITICAL SECTION
    }

    return MPE_SUCCESS;
}

/**
 * Add user data to a carousel.  Overwrites whatever was there before.  Use with caution.
 *
 * @param oc  The carousel to add data to.
 * @param data The data to add.
 */
void mpe_ocSetUserData(mpe_ObjectCarousel *oc, void *data)
{
    if (oc != NULL)
    {
        oc->userData = data;
    }
}

/**
 * Add user data to a carousel.  Overwrites whatever was there before.  Use with caution.
 *
 * @param oc  The carousel to add data to.
 * @param data The data to add.
 */
void *
mpe_ocGetUserData(mpe_ObjectCarousel *oc)
{
    if (oc != NULL)
    {
        return oc->userData;
    }
    else
    {
        return NULL;
    }
}

/**
 * Add a DII (data carousel), as specified by the TAP, into the object carousel.
 *
 * @param oc        The object carousel to add the tap to.
 *
 * @returns MPE_SUCCESS if the tap can be added, or is already there.  Error codes if it can't be added.
 */
mpe_Error mpe_ocAddDII(mpe_ObjectCarousel *oc, uint16_t diiId,
        uint16_t assocTag)
{
    mpe_DataCarousel *sink; // Won't be used.
    ocpTAP tap; // Tap we create for the getCarousel call

    if (oc == NULL)
    {
        return MPE_EINVAL;
    }

    MPE_LOG(MPE_LOG_INFO, MPE_MOD_FILESYS,
            "OC: Adding DII %04x on Association Tag %d\n", diiId, assocTag);
    // Create an artificial TAP to mount with
    tap.assocTag = assocTag;
    // shift the DII ID into the right bits.
    // We don't know version, so we'll just leave it blank
    tap.transactionID = ((uint32_t) diiId) << 1;

    // Get the data carousel, performing a mount if need be
    // Ignore the returned carousel
    return ocGetDataCarousel(oc, &tap, &sink);
}

/**
 * Prefetch a module (or modules) based on the name.
 *
 * @param oc            The object carousel to add the tap to.
 * @param moduleName    The module name to prefetch.
 *
 * @returns MPE_SUCCESS the module is prefeteched, MPE_ENODATA if no such module
 *          exists in any mounted carousel, or error codes otherwise.
 */
mpe_Error mpe_ocPrefetchModule(mpe_ObjectCarousel *oc, const char *moduleName)
{
    int i;
    mpe_Error retCode = MPE_SUCCESS;

    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(ocGlobalMutex);

    // Search for this carouselID
    for (i = 0; i < DC_MAX_DATACAROUSELS; i++)
    {
        // Found a matching ID
        if (oc->dataCarousels[i].dc != NULL)
        {
            retCode = mpe_dcPrefetchModuleByName(oc->dataCarousels[i].dc,
                    moduleName);
            // If the error code isn't MPE_ENODATA, the module must have been found,
            // and we tried prefetching, so we'll quit, as the name is only allowed to exist
            // in one carousel.
            if (retCode != MPE_ENODATA)
            {
                break;
            }
        }
    }
    mpe_mutexRelease(ocGlobalMutex);
    // END CRITICAL SECTION
    return retCode;
}

/**
 * This function frees a directory entry.  Callers should use this instead of freeing it themselves.
 *
 * @param dirEntry  The directory entry to free.
 */

void mpe_ocFreeDirectoryEntry(mpe_OcDirEntry *dirEntry)
{
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
            "OC: Freeing directory entry: %p\n", dirEntry);
    if (dirEntry != NULL)
    {
        if (dirEntry->mimeType != NULL)
        {
            mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, dirEntry->mimeType);
        }

        mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, dirEntry);
    }
}

// Static functions here.

/**
 * Load the actual directory entries for a directory.
 */
static mpe_Error ocLoadDirectoryEntries(mpe_ObjectCarousel *oc,
        ocpBIOPObject *dir, mpe_DcModule *module, uint32_t timeout)
{
    ocpDataSource dSource;
    uint32_t offset;
    ocReaderBuffer buffer =
    { 0, 0, 0 };
    mpe_Error retCode;

    MPE_UNUSED_PARAM(oc); /* TODO: if this param is not used, should it be removed from this function? */

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
            "OC: Loading directory entries in object %08x\n", dir->objectKey);

    if (dir->objectType != BIOP_Directory && dir->objectType
            != BIOP_ServiceGateway)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OC: File type is not a directory: %d\n", dir->objectType);
        return MPE_EINVAL;
    }
    if (dir->payload.dir.dirEntries != NULL)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                "OC: Directory already loaded\n");
        return MPE_SUCCESS;
    }
    // MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "OC: Loading directory for dir object in module %04x\n", module->moduleId);

    // Create a dSource object for the parser
    dSource.dataSource = &buffer;
    dSource.reader = (READER_FUNCPTR) ocReadBufferedBytes;
    dSource.timeout = timeout;
    buffer.module = module;

    // Set the offset where to look.
    offset = dir->payload.dir.dataOffset;

    // Parse the data
    retCode = ocpParseDirEntries(&dSource, &offset, &(dir->payload.dir));

    return retCode;
}

/**
 * Make a directory entry structure.
 * @param   dirEntry        The internal BIOP directory entry.  NULL if this for the service gateway.
 * @param   iso88591        Are these file names in ISO88591?
 * @param   retEntry        Where to allocate and put the returned entry.
 */
static mpe_Error ocMakeDirEntry(ocpDirEntry *dirEntry, mpe_ObjectCarousel *oc,
        mpe_OcDirEntry **retEntry)
{
    mpe_Error retCode;
    uint8_t utf8Buffer[2 * MAX_OC_NAMELEN + 1];
    char *name;
    mpe_OcDirEntry *result;
    uint32_t version = 0;
    ocpTAP *tap;
    mpe_DataCarousel *dc;
    mpe_DcModule *module;
    ocpBIOPObjectLocation *loc;

    // If we're the service gateway, set everything to point to the service gateway.
    if (dirEntry == NULL)
    {
        name = "";
        tap = &(oc->gatewayTAP);
        loc = &(oc->gatewayLoc);
    }
    else
    {
        if (dirEntry->ior.profile.profileTag != TAG_BIOP)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                    "OC: ocMakeDirEntry called with symbolic link directory entry\n");
            return MPE_EINVAL;
        }

        loc = &(dirEntry->ior.profile.body.biopProfile.objectLoc);
        tap = &(dirEntry->ior.profile.body.biopProfile.tap);

        // Convert the name.
        // If ISO 8859-1 encodings, translate it back to UTF-8.
        // If already in UTF-8, don't worry about it.
        if (oc->iso88591)
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                    "OC: Converting ISO name %s to UTF-8\n", dirEntry->name);
            ocuIso88591ToUtf8(dirEntry->name, utf8Buffer);
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                    "OC: Name conversion done: %s Characters: %d\n",
                    utf8Buffer, strlen((char*) utf8Buffer));
            name = (char*) utf8Buffer;
        }
        else
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                    "OC: Keeping name the same: %s\n", dirEntry->name);
            name = (char*) dirEntry->name;
        }
    }

    retCode = ocGetDataCarousel(oc, tap, &dc);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OC: Could not get data carousel\n");
        return retCode;
    }
    retCode = mpe_dcGetModule(dc, loc->moduleID, &module);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OC: Could not get module %04x\n", loc->moduleID);
        mpe_dcReleaseModule(module);
        return retCode;
    }
    retCode = mpe_dcGetModuleVersion(module, &(version));
    mpe_dcReleaseModule(module);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OC: Could not get module %04x version\n", loc->moduleID);
        return retCode;
    }

    // Malloc up the structure.
    // Include space at the end to free up things
    retCode = mpeos_memAllocP(MPE_MEM_FILE_CAROUSEL, (sizeof(mpe_OcDirEntry)
            + strlen(name) + 1), (void **) &result);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OC: Unable to allocate dirEntry structure\n");
        return retCode;
    }

    // Copy in the name
    strcpy(result->name, name);
    result->version = version;

    // And the size
    if (dirEntry == NULL)
    {
        result->size = 0;
        result->fileType = Type_Directory;
    }
    else
    {
        result->size = (uint32_t) dirEntry->fileSize; // cast retrieved 64-bit value into local 32-bit value
        // Set the type
        switch (dirEntry->objectType)
        {
        case BIOP_File:
            result->fileType = Type_File;
            break;
        case BIOP_Directory:
        case BIOP_ServiceGateway:
            result->fileType = Type_Directory;
            break;
        case BIOP_Stream:
            result->fileType = Type_Stream;
            break;
        case BIOP_StreamEvent:
            result->fileType = Type_StreamEvent;
            break;
        default:
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                    "OC: Internal error -- unknown type %d\n",
                    dirEntry->objectType);
            break;
        }
    }

    // Fill in the MIME Type
    // Copy from the BIOP object, and translate.  Translation is probably unnecessary, as MIME types probably
    // are always in ASCII, but just for fun.

    result->mimeType = NULL; // Just in case.

    if (dirEntry != NULL && dirEntry->contentType != NULL)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                "OC: Copying content type: %s\n", dirEntry->contentType);
        if (oc->iso88591)
        {
            // TODO: FIXME: Allocate the correct amount of space.
            retCode = mpeos_memAllocP(MPE_MEM_FILE_CAROUSEL, strlen(
                    (char *) dirEntry->contentType) * 2 + 1,
                    (void **) &(result->mimeType));
            if (retCode == MPE_SUCCESS)
            {
                ocuIso88591ToUtf8(dirEntry->contentType,
                        (uint8_t *) result->mimeType);
            }
        }
        else
        {
            retCode = mpeos_memAllocP(MPE_MEM_FILE_CAROUSEL, strlen(
                    (char *) dirEntry->contentType) + 1,
                    (void **) &(result->mimeType));
            if (retCode == MPE_SUCCESS)
            {
                strcpy((char *) result->mimeType,
                        (char *) dirEntry->contentType);
            }
        }
        if (retCode != MPE_SUCCESS)
        {
            mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, result);
            result = NULL;
        }
    }

    *retEntry = result;
    return retCode;
}

/**
 * Find an object matching a key in a data carousel module.
 *
 * @param module    The module to look in.
 * @param key       The key we're looking for.
 * @param timeout   The Timeout for this module.
 * @param retOffset [out] Return variable containing the offset into the module
 *                  where the object lives.
 * @param retObject [out] Return variable containing a pointer to the object we're looking
 *                  for.
 * @returns MPE_SUCCESS if the object is found, error codes otherwise.
 */
static mpe_Error ocFindObjectInModule(mpe_DcModule *module, uint32_t key,
        uint32_t timeout, ocpBIOPObject **retObject)
{
    mpe_Error retCode = MPE_SUCCESS;
    uint32_t offset = 0; // Where the current object started, after parse
    uint32_t nextOffset = 0; // Where the next object is
    uint32_t moduleSize;
    ocpBIOPObject *object;
    ocpDataSource dSource;
    ocReaderBuffer buffer =
    { 0, 0, 0 };
    ocBIOPObjectCache *modCache = NULL;

    // Clear out error conditions
    *retObject = NULL;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "OC: Searching for key %08x\n", key);

    // If we're using a BIOP object cache, lookup the object here.
    if (ocEnableBIOPCache)
    {
        // Get the cache for this module
        retCode = ocGetModuleCache(module, &modCache);
        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                    "OC: Unable to allocate cache\n");
            return retCode;
        }

        // Lookup the object key
        retCode = ocLookupBIOPCache(modCache, key, retObject, &offset);
        // If we found it, let's get out here.
        if (retCode == MPE_SUCCESS)
        {
            return MPE_SUCCESS;
        }
        // Oops, not there.  Let's start searching again
        // Skip ahead to the unparsed parts of the cache
        nextOffset = modCache->nextOffset;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
            "OC: Parsing in module starting at offset %d\n", nextOffset);
    // Get the size, just for speed
    retCode = mpe_dcGetModuleSize(module, &moduleSize);
    if (retCode != MPE_SUCCESS)
    {
        return retCode;
    }

    // Malloc up the object
    retCode = ocpAllocBIOPObject(&object);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS, "OC: Cannot allocate object\n");
        return retCode;
    }

    // Create a dSource object for the parser
    dSource.dataSource = &buffer;
    dSource.reader = (READER_FUNCPTR) ocReadBufferedBytes;
    dSource.timeout = timeout;
    buffer.module = module;

    // Now, check all the objects
    while (nextOffset < moduleSize)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "OC: Searching at offset %d\n",
                nextOffset);

        // Record where this object started
        offset = nextOffset;

        // Parse the object, getting the next location along the way.
        retCode = ocpParseBIOPObject(&dSource, &nextOffset, object);
        // If retCode isn't MPE_SUCCESS, the OC is invalid.
        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                    "OC: Unable to parse BIOP object at offset %d in module\n",
                    offset);
            ocpFreeBIOPObject(object);
            return retCode;
        }
#ifdef OCP_DUMP_OBJECTS
        ocpDumpBIOPObject(object);
#endif

        // Put the object in the cache
        if (ocEnableBIOPCache)
        {
            retCode = ocInsertBIOPCacheObject(modCache, object, offset,
                    nextOffset);
            if (retCode != MPE_SUCCESS)
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                        "OC: Unable to insert object into BIOP Object Cache\n");
                ocpFreeBIOPObject(object);
                return retCode;
            }
        }

        // Compare the keys.  If it's a match, we're golden.
        if (object->objectKey == key)
        {
            *retObject = object;
            return MPE_SUCCESS;
        }

        if (ocEnableBIOPCache)
        {
            // Malloc up a new object.  We don't want to rehash that one.
            retCode = ocpAllocBIOPObject(&object);
            if (retCode != MPE_SUCCESS)
            {
                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                        "OC: Cannot allocate object\n");
                return retCode;
            }
        }
        else
        {
            // Purge out any allocated data in the object
            ocpPurgeBIOPObject(object);
        }
    }

    // Oops, in a bad place.  Object not found.  Free the BIOP Object
    ocpFreeBIOPObject(object);
    return MPE_ENODATA;
}

/**
 * Overarching routine to get an object.
 *
 */
static mpe_Error ocGetObject(mpe_ObjectCarousel *oc, ocpTAP *tap,
        ocpBIOPObjectLocation *loc, mpe_DcModule **module,
        ocpBIOPObject **object)
{
    mpe_Error retCode;
    mpe_DataCarousel *dc = NULL;

    *module = NULL;
    *object = NULL;

    // First, get the data carousel
    retCode = ocGetDataCarousel(oc, tap, &dc);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OC: Could not get data carousel");
        return retCode;
    }

    // Next, get the module in the carousel
    retCode = mpe_dcGetModule(dc, loc->moduleID, module);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OC: Could not get module %04x\n", loc->moduleID);
        return retCode;
    }

    // Next, load the service gateway from this module
    retCode = ocFindObjectInModule(*module, loc->objectKey,
            OC_TIMEOUT(tap->timeOut, OC_DSI_TIMEOUT), object);
    if (retCode != MPE_SUCCESS)
    {
        mpe_dcReleaseModule(*module);
        *module = NULL;
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OC: Could not locate object %08x in module %04x\n",
                loc->objectKey, loc->moduleID);
    }
    return retCode;
}
/**
 * Find the directory entry for an object in a Directory object.
 * @param dir       The directory we're searching in.
 * @param name      The filename to search for.
 * @param retEntry  [out] Return pointer to the directory entry for the name, if found.
 * @return MPE_SUCCESS if the file is found, error codes otherwise.
 */
static mpe_Error ocFindDirectoryEntry(mpe_ObjectCarousel *oc,
        ocpBIOPObject *dir, char *name, mpe_DcModule *module, uint32_t timeout,
        ocpDirEntry **retEntry)
{
    mpe_Error retCode;
    uint32_t i;
    char *compName; // The string we'll use to compare with
    char isoBuffer[MAX_OC_NAMELEN + 1]; // Max ISO 8859-1 characters.  1 byte each.

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
            "OC: Searching for object %s in object %08x\n", name,
            dir->objectKey);

    // Haven't found anything
    *retEntry = NULL;

    if (dir->objectType != BIOP_Directory && dir->objectType
            != BIOP_ServiceGateway)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OC: Can't search in an object which is not a directory\n");
        return MPE_FS_ERROR_INVALID_DATA;
    }

    // Load the directory entries if we haven't already
    if (dir->payload.dir.dirEntries == NULL)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                "OC: Loading directory entries\n");
        retCode = ocLoadDirectoryEntries(oc, dir, module, timeout);
        if (retCode != MPE_SUCCESS)
        {
            return retCode;
        }
    }

    // If the name is in ISO 8859-1, convert the filename here.
    if (oc->iso88591)
    {
        retCode = ocuUtf8ToIso88591((uint8_t*) name, (uint8_t*) isoBuffer);
        if (retCode != MPE_SUCCESS)
        {
            // Could not translate name.
            MPE_LOG(
                    MPE_LOG_WARN,
                    MPE_MOD_FILESYS,
                    "OC: Could not find file %s.  Non-ISO 8859-1 characters specified in ISO 8859-1 carousel.\n",
                    name);
            return MPE_FS_ERROR_NOT_FOUND;
        }
        compName = isoBuffer;
    }
    else
    {
        compName = name;
    }

    // Search the names, just simple string compares
    for (i = 0; i < dir->payload.dir.numDirEntries; i++)
    {
        /*
         MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
         "OC: Searching for name %s in location %d: Name %s\n",
         name, i, dir->payload.dir.dirEntries[i].name);
         */
        if (ocCompareFileNames(compName,
                (char *) dir->payload.dir.dirEntries[i].name))
        {
            // Found it.  Set up the return value
            *retEntry = &(dir->payload.dir.dirEntries[i]);
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                    "OC: Found name %s in location %d\n", name, i);
            return MPE_SUCCESS;
        }
    }
    return MPE_FS_ERROR_NOT_FOUND;
}

/*
 * Convert a local name into a name in the destination space
 */
static mpe_Error ocResolveLiteOptionsName(ocpDirEntry *dirEntry,
        mpe_Bool iso88591, char *suffix, char **dest)
{
    mpe_Error retCode;
    uint32_t stringLength;
    uint32_t i;
    uint32_t nameLen;
    char *buffer = NULL;
    char utf8Buffer[MAX_OC_NAMELEN * 2 + 1]; // 2 bytes per ISO 8859-1 character, for back translation.

    if (dirEntry == NULL || dest == NULL)
    {
        return MPE_EINVAL;
    }
    *dest = NULL;
    // Calculate the string length.
    // Note, we do the caluclations as though we were in UTF-8 encoding space.
    // Start with 2, for the leading '/' and the trailing null terminator
    stringLength = 2;

    for (i = 0; i < dirEntry->ior.profile.body.liteOptionsProfile.numNameComps; i++)
    {
        nameLen = strlen(
                dirEntry->ior.profile.body.liteOptionsProfile.nameComps[i]) + 1;
        // If the current encoding is ISO8859-1, double the amount of space necessary,
        // as a ISO 8859-1 character will only be 1 or 2 bytes in UTF-8.
        if (iso88591)
        {
            stringLength += 2 * nameLen;
        }
        else
        {
            stringLength += nameLen;
        }
        // Add space for the trailing '/'.  1 character in UTF-8.
        stringLength++;
    }

    if (suffix != NULL)
    {
        stringLength += strlen(suffix);
    }

    retCode = mpeos_memAllocP(MPE_MEM_FILE_CAROUSEL, stringLength,
            (void **) &buffer);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OC: Could not allocate %d bytes to hold target pathname\n",
                stringLength);
        return retCode;
    }

    buffer[0] = '\0';
    for (i = 0; i < dirEntry->ior.profile.body.liteOptionsProfile.numNameComps; i++)
    {
        strcat(buffer, "/");
        if (iso88591)
        {
            // Translate to UTF-8
            ocuIso88591ToUtf8(
                    (uint8_t*) dirEntry->ior.profile.body.liteOptionsProfile.nameComps[i],
                    (uint8_t*) utf8Buffer);
            strcat(buffer, utf8Buffer);
        }
        else
        {
            strcat(buffer,
                    dirEntry->ior.profile.body.liteOptionsProfile.nameComps[i]);
        }
    }
    strcat(buffer, "/");
    if (suffix != NULL)
    {
        strcat(buffer, suffix);
    }
    *dest = buffer;

    return MPE_SUCCESS;
}

/**
 * Look up a data carousel based on the carouselID.
 * If the data carousel has not yet been mounted, mount it now.
 */
static mpe_Error ocGetDataCarousel(mpe_ObjectCarousel *oc, ocpTAP *tap,
        mpe_DataCarousel **retDC)
{
    mpe_Error retCode = MPE_SUCCESS;
    int i;
    int destLocation = -1;
    mpe_DataCarousel *dc = NULL;

    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(ocGlobalMutex);

    // Search for this carouselID
    for (i = 0; i < DC_MAX_DATACAROUSELS; i++)
    {
        // Found a matching ID
        if (ocCompareTAPs(tap, &(oc->dataCarousels[i].tap)))
        {
            // Make sure there's a carousel mounted there
            if (oc->dataCarousels[i].dc != NULL)
            {
                // Found it.  Return it.
                MPE_LOG(
                        MPE_LOG_DEBUG,
                        MPE_MOD_FILESYS,
                        "OC: Found carousel ID %04x:%08x mounted in location %d\n",
                        tap->assocTag, tap->transactionID, i);
                dc = oc->dataCarousels[i].dc;
                break;
            }
            else
            {
                // Carousel ID is set, but nothing is there yet.
                //
                MPE_LOG(
                        MPE_LOG_DEBUG,
                        MPE_MOD_FILESYS,
                        "OC: Found carousel ID %04x:%08x unmounted in location %d.  Will mount\n",
                        tap->assocTag, tap->transactionID, i);
                destLocation = i;
                break;
            }
        }
        else if (oc->dataCarousels[i].tap.transactionID
                == MPE_DC_INVALID_TRANSID && destLocation == -1)
        {
            destLocation = i;
            break;
        }
    }

    // Didn't find the carousel, but we should have a place to put it.
    // Mount the sucker
    if (dc == NULL)
    {
        if (destLocation == -1)
        {
            MPE_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_FILESYS,
                    "OC: Too many DC's mounted, no place to put carouselID %04x:%08x\n",
                    tap->assocTag, tap->transactionID);
            retCode = MPE_FS_ERROR_FAILURE;
        }
        else
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                    "OC: Mounting %04x:%08x into position %d\n", tap->assocTag,
                    tap->transactionID, destLocation);
            // Copy the tap in
            memcpy(&(oc->dataCarousels[destLocation].tap), tap, sizeof(ocpTAP));

            mpe_mutexRelease(ocGlobalMutex);
            // END CRITICAL SECTION

            // Release the lock for the long latency mount
            retCode = mpe_dcMount(oc->siHandle, tap->assocTag,
                    tap->transactionID, oc->gatewayTimeout,
#ifdef PTV
                    oc->carouselID,
#endif
                    &dc);
            if (retCode != MPE_SUCCESS)
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                        "OC: ERROR: Unable to mount data carousel\n");
                return retCode;
            }
            // BEGIN CRITICAL SECTION
            mpe_mutexAcquire(ocGlobalMutex);
            if (oc->dataCarousels[destLocation].dc == NULL)
            {
                // Still empty.  Put it in the structure.
                oc->dataCarousels[destLocation].dc = dc;
                (void) mpe_dcRegisterChangeNotification(dc, oc->updateQueue,
                        oc->updateData);
            }
            else
            {
                MPE_LOG(
                        MPE_LOG_WARN,
                        MPE_MOD_FILESYS,
                        "OC: Data carousel position %d already filled.  Duplicate mount attempt\n",
                        destLocation);
                (void) mpe_dcUnmount(dc);
                dc = oc->dataCarousels[destLocation].dc;
            }
        }
    }

    mpe_mutexRelease(ocGlobalMutex);
    // END CRITICAL SECTION
    *retDC = dc;

    return retCode;
}

/**
 * Increment the reference count on a carousel.
 * Will prevent it from being unmounted completely until it goes to 0.
 * Can only increment the count on mounted carousels.
 *
 * @param The carousel to increment the ref count on.
 *
 * @returns True if the carousel was mounted.  False if it wasn't.
 */
mpe_Bool mpe_ocReferenceOC(mpe_ObjectCarousel *oc)
{
    mpe_Bool retCode = true;

    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(ocGlobalMutex);

    oc->referenceCount++;

    mpe_mutexRelease(ocGlobalMutex);
    // END CRITICAL SECTION

    return retCode;
}

/**
 * Decrement the reference count on a carousel.  If the carousel is unmounted, and the ref count
 * goes to 0, the carousel will be freed.
 *
 * @param The carousel to decrement the ref count on.
 *
 * @returns True if the carousel was freed as part of this call.  False if it wasn't.
 */
mpe_Bool mpe_ocDereferenceOC(mpe_ObjectCarousel *oc)
{
    mpe_Bool unload = FALSE; // Do we need to unload the carousel?

    if (oc == NULL)
    {
        return false;
    }

    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(ocGlobalMutex);
    oc->referenceCount--;

    if (oc->mounted == false && oc->referenceCount == 0)
    {
        unload = true;
    }
    mpe_mutexRelease(ocGlobalMutex);
    // END CRITICAL SECTION

    if (unload)
    {
        ocFreeOC(oc);
    }
    return unload;
}

/*
 * Free an OC object.
 */
static
void ocFreeOC(mpe_ObjectCarousel *oc)
{
    uint32_t i;

    MPE_LOG(MPE_LOG_INFO, MPE_MOD_FILESYS,
            "OC: Freeing Object Carousel: %p\n", oc);
    if (oc != NULL)
    {
        for (i = 0; i < DC_MAX_DATACAROUSELS; i++)
        {
            if (oc->dataCarousels[i].dc != NULL)
            {
                (void) mpe_dcUnmount(oc->dataCarousels[i].dc);
                oc->dataCarousels[i].tap.transactionID = MPE_DC_INVALID_TRANSID;
            }
        }
        mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, oc);
    }
}

/*
 * Free a file object.
 */
static
void ocFreeFile(mpe_OcFile *file)
{
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "OC: Freeing OC File: %p\n", file);
    if (file)
    {
        RELEASE_BIOP_OBJECT(file->biopObject);
        mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, file);
    }
}

/**
 * Convert a NSAP address to string form.
 * UGLY: Uses a static string buffer to make sure which is, in no way, synchronized.
 * Don't call this simultaneously, or call it again before using the output.
 *
 */
#define HEXCHAR(val) ((char)(((val) & 0x0f) < 0xa) ? ('0' + ((val) & 0x0f)) : ('a' + ((val) & 0x0f)- 10))
static
inline
char *
ocNSAPString(uint8_t *nsap)
{
    int i;
    static char string[2 * OCP_DSI_SERVERID_LENGTH + 1];
    for (i = 0; i < OCP_DSI_SERVERID_LENGTH; i++)
    {
        string[i * 2] = HEXCHAR((nsap[i] >> 4) & 0xf);
        string[i * 2 + 1] = HEXCHAR(nsap[i] & 0xf);
    }
    string[OCP_DSI_SERVERID_LENGTH * 2] = '\0';
    return string;
}

/**
 * Register an OC into the global structure.
 */
static mpe_Error ocRegisterOC(mpe_ObjectCarousel *oc)
{
    mpe_Error retCode = MPE_FS_ERROR_FAILURE;
    int i;

    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(ocGlobalMutex);

    if (ocRegisteredCarousels >= MAX_OBJECTCAROUSELS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OC: Max object carousels already mounted: %d\n",
                MAX_OBJECTCAROUSELS);
    }
    else
    {
        // Count that we're registered
        ocRegisteredCarousels++;

        // Find the first empty slot
        for (i = 0; i < MAX_OBJECTCAROUSELS; i++)
        {
            // If this slot is empty
            if (ocObjectCarousels[i] == NULL)
            {

                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                        "OC: Registering OC %s in slot %d\n", ocNSAPString(
                                (uint8_t *) (oc->nsapAddress)), i);
                // put the carousel here
                ocObjectCarousels[i] = oc;

                retCode = MPE_SUCCESS;
                // And get out of the loop.
                break;
            }
        }
    }

    mpe_mutexRelease(ocGlobalMutex);
    // END CRITICAL SECTION
    return retCode;
}

/**
 * Remove an OC from the global structure.
 */
static
void ocUnregisterOC(mpe_ObjectCarousel *oc)
{
    int i;

    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(ocGlobalMutex);
    // Find the carousel
    for (i = 0; i < MAX_OBJECTCAROUSELS; i++)
    {
        // If this slot contains this carousel
        if (ocObjectCarousels[i] == oc)
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                    "OC: UnRegistering OC %s in slot %d\n", ocNSAPString(
                            (uint8_t *) (oc->nsapAddress)), i);
            // Remove it
            ocObjectCarousels[i] = NULL;
            // Count that we're unregistered
            ocRegisteredCarousels--;

            // And get out of the loop.
            break;
        }
    }

    mpe_mutexRelease(ocGlobalMutex);
    // END CRITICAL SECTION
}

/**
 * Determine if two TAP's are essentially equivalent.  Question: Do we want to compare
 * all the bits in the transactionID/selector, or just the ID bits?
 *
 * @param tap1 First TAP to compare.
 * @param tap2 Second TAP to compare.
 *
 * @returns true if they're the same, false otherwise.
 */
static inline mpe_Bool ocCompareTAPs(ocpTAP *tap1, ocpTAP *tap2)
{
    if (tap1 == NULL || tap2 == NULL)
    {
        return false;
    }
    if (tap1 == tap2)
    {
        return true;
    }
    if ((tap1->assocTag == tap2->assocTag) && (OCU_DII_CAROUSEL_ID(
            tap1->transactionID) == OCU_DII_CAROUSEL_ID(tap2->transactionID)))
    {
        return true;
    }
    return false;
}

/**
 * Free up the BIOP object cache, freeing each object in the cache, and the
 * cache object itself.  Intended to be called from the
 *
 * @param data          A (void *) pointer to the cache.
 */
static
void ocFreeBIOPObjectCache(void *data)
{
    ocBIOPObjectCache *cache = (ocBIOPObjectCache *) data;
    uint32_t i;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
            "OC: Freeing BIOP Object Cache: %p\n", cache);
    if (cache == NULL)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS, "OC: NULL BIOP Object Cache\n");
        return;
    }
    // Free up the BIOP objects
    for (i = 0; i < cache->numElements; i++)
    {
        ocpFreeBIOPObject(cache->cache[i]);
    }
    // free up the dude.
    mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, cache->cache);
    mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, cache);
}

/**
 * Get the cache for this module, and if it doesn't exist, create one.
 * Stores the cache in the user data field of the module.
 *
 * @param module        The module to get the cache from.
 * @param retCache      [out] Pointer to where to put the cache object, if there is one or it's created.
 *
 * @return MPE_SUCCESS if a
 */
static inline mpe_Error ocGetModuleCache(mpe_DcModule *module,
        ocBIOPObjectCache **retCache)
{
    ocBIOPObjectCache *cache = NULL;
    mpe_Error retCode = MPE_SUCCESS;
    uint32_t allocElements = DEFAULT_CACHE_SIZE;
    uint32_t moduleSize;

    // MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "OC: Getting cache object\n");
    if (module == NULL || retCache == NULL)
    {
        return MPE_EINVAL;
    }

    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(ocGlobalMutex);

    // Get the user data object
    (void) mpe_dcGetModuleUserData(module, (void **) &cache);
    if (cache == NULL)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                "OC: No BIOP Cache Object available.  Creating\n");
        // Get the size
        retCode = mpe_dcGetModuleSize(module, &moduleSize);
        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                    "OC: Could not get module size\n");
            goto Done;
        }

        // If it's null, we need to allocate up a new one.
        retCode = mpeos_memAllocP(MPE_MEM_FILE_CAROUSEL,
                sizeof(ocBIOPObjectCache), (void **) &cache);
        if (retCode != MPE_SUCCESS)
        {
            cache = NULL;
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                    "OC: BIOP Cache Object allocation failed\n");
            goto Done;
        }
        // If the module is > 64, it can only contain a single element.
        if (moduleSize >= 64 * 1024)
        {
            allocElements = 1;
        }
        // Create the cache array
        retCode = mpeos_memAllocP(MPE_MEM_FILE_CAROUSEL, allocElements
                * sizeof(ocpBIOPObject *), (void **) &cache->cache);
        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                    "OC: Unable to allocate cache array\n");
            mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, cache);
            cache = NULL;
        }
        else
        {
            // set the appropriate values
            cache->nextOffset = 0;
            cache->numElements = 0;
            cache->allocatedElements = allocElements;
            (void) mpe_dcAddModuleUserData(module, cache, ocFreeBIOPObjectCache);
        }
    }
    Done: mpe_mutexRelease(ocGlobalMutex);
    // END CRITICAL SECTION

    *retCache = cache;
    return retCode;
}

/**
 * Insert an item into the cache.  Handle changing the cache size if it's too small.
 *
 * @param cache         The cache to insert into.
 * @param object        The object to insert.
 * @param offset        The offset where this object was found.
 * @param nextOffset    The offset of the next object.  If nothing is found in the cache
 *                      on a lookup, scanning will resume at this location.
 */
static inline mpe_Error ocInsertBIOPCacheObject(ocBIOPObjectCache *cache,
        ocpBIOPObject *object, uint32_t offset, uint32_t nextOffset)
{
    mpe_Error retCode = MPE_SUCCESS;
    uint32_t newElements;

    if (cache == NULL)
        return MPE_EINVAL;

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_FILESYS,
            "OC: Inserting cache element %08x (%p), at offset %d, next offset %d: %d Current elements, allocated for %d\n",
            object->objectKey, object, offset, nextOffset, cache->numElements,
            cache->allocatedElements);

    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(ocGlobalMutex);

    if (cache->numElements == cache->allocatedElements)
    {
        newElements = 2 * cache->allocatedElements;
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                "OC: Reallocating BIOP Object cache to %d elements\n",
                newElements);

        retCode = mpeos_memReallocP(MPE_MEM_FILE_CAROUSEL, newElements
                * sizeof(ocpBIOPObject *), (void **) &cache->cache);
        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_FILESYS,
                    "OC: Unable to reallocate %d elements in BIOP Object Cache\n",
                    newElements);
            goto Done;
        }
        cache->allocatedElements = newElements;
    }
    cache->cache[cache->numElements] = object;
    cache->numElements++;
    cache->nextOffset = nextOffset;

    Done: // HACK
    mpe_mutexRelease(ocGlobalMutex);
    // END CRITICAL SECTION
    return retCode;
}

/**
 * Lookup up an object in the cache.
 *
 * @param cache         The cache to look in.
 * @param key           The key to look for.
 * @param retObject     [out] Pointer to where to put the object.  Not touched if not found.
 * @param retOffset     [out] Pointer to where to put the offset of this object in the module.
 *
 * @return MPE_SUCCESS if key is found, MPE_NODATA if not found.
 */
static inline mpe_Error ocLookupBIOPCache(ocBIOPObjectCache *cache,
        uint32_t key, ocpBIOPObject **retObject, uint32_t *retOffset)
{
    ocpBIOPObject *curr;
    mpe_Error retCode = MPE_ENODATA;
    uint32_t i;

    if (cache == NULL || retObject == NULL || retOffset == NULL)
    {
        return MPE_EINVAL;
    }

    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(ocGlobalMutex);
    for (i = 0; i < cache->numElements; i++)
    {
        curr = cache->cache[i];
        /*
         MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
         "OC: Checking key %08x against cached object %d key %08x at %08x\n",
         key, i, curr->objectKey, curr);
         */

        if (curr->objectKey == key)
        {
            // Found it.
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                    "OC: Found key %08x in cache position %d\n", key, i);
            // Set the return values
            *retObject = curr;
            *retOffset = curr->moduleOffset;

            // Perf hack.  Let's put this in position 0.   Assuming we might use it again real soon.
            if (i > 0)
            {
                cache->cache[i] = cache->cache[0];
                cache->cache[0] = curr;
            }
            // Signal that we found it
            retCode = MPE_SUCCESS;
            // Break out of the loop
            break;
        }
    }
    mpe_mutexRelease(ocGlobalMutex);
    // END CRITICAL SECTION
    return retCode;
}
// #define HW_SUPPORT_UNALIGNS             // Define if the HW supports doing unaligned accesses (ie, x86 or VAX).  Move to build rules.
/*
 * Perform buffered reading out of the module.  We use this when accessing data from the
 * BIOP object parser, as it makes repeated reads for small numbers of bytes, typically 1-4
 * but occassionally larger blocks.  Going all the way to readModuleBytes() would cause
 * multiple synchronizations, walking multiple pointers, yada, yada, yada.  Instead,
 * we read 128 bytes at a time into a buffer, and then read out of that buffer.
 */
static mpe_Error ocReadBufferedBytes(void *in, uint32_t start, uint32_t length,
        uint32_t timeout, uint8_t *output, uint32_t *bytesRead)
{
    ocReaderBuffer *buf = (ocReaderBuffer *) in;
    mpe_Error retCode = MPE_SUCCESS;
    int32_t startRead, endRead;
    uint32_t modSize;
    uint32_t bytesToRead;

    // Figure out the starting and ending locations.
    // Used signed values so we can determine if we're before or after the end buffer
    startRead = (int32_t) start - (int32_t) buf->offset;
    endRead = startRead + (int32_t) length - 1;

    // MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "OC: Parser Read Request (Start: %d, length: %d)\n", start, length);

    // If the total size of the read is greater than a buffer, or it's larger than half a buffer
    // and not entirely read, let's just read it out of the module.
    if ((length > MAX_BUFFER_SIZE) || ((length > (MAX_BUFFER_SIZE / 2))
            && ((startRead < 0) || (endRead > (int32_t) buf->avail))))
    {
        // MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "OC: Parser falling back on module read for %d bytes\n", length);
        return mpe_dcReadModuleBytes(buf->module, start, length, timeout,
                output, bytesRead);
    }

    // Otherwise, if it's not entirely
    if ((startRead < 0) || (endRead >= (int32_t) buf->avail))
    {
        // Figure out how big the module is, and how many bytes to read.  Necessary, as we don't allow reads past the end.
        retCode = mpe_dcGetModuleSize(buf->module, &modSize);
        if (retCode != MPE_SUCCESS)
        {
            return retCode;
        }
        bytesToRead = MIN(modSize - start, MAX_BUFFER_SIZE);

        // Read 'em
        // MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "OC: Parser Loading Buffer (Start: %d, length: %d)\n", start, bytesToRead);
        retCode = mpe_dcReadModuleBytes(buf->module, start, bytesToRead,
                timeout, buf->buffer, &(buf->avail));
        if (retCode != MPE_SUCCESS)
        {
            return retCode;
        }

        // Reset the buffer and the start and end read locations
        buf->offset = start;
        startRead = start - buf->offset;
        endRead = startRead + length - 1;

        // Check that we have enough bytes.
        // Otherwise, we need to truncate the read.
        if (endRead > (int32_t) buf->avail)
        {
            endRead = buf->avail;
            length = endRead - startRead + 1;
        }
    }

    // Copy the data out of the buffer.
    // If the hardware supports unaligned accesses (like x86 does), try to do the move in a single instruction.
    // otherwise, fall back on using a loop if it's anything but a byte move.
    switch (length)
    {
#ifdef HW_SUPPORT_UNALIGNS
    // Hardware can copy data at arbitrary locatins, without faults
    case sizeof(uint8_t):
    *output = buf->buffer[startRead];
    break;
    case sizeof(uint16_t):
    *((uint16_t *) output) = *(uint16_t *)(&(buf->buffer[startRead]));
    break;
    case sizeof(uint32_t):
    *((uint32_t *) output) = *(uint32_t *)(&(buf->buffer[startRead]));
    break;
    case sizeof(uint64_t):
    *((uint64_t *) output) = *(uint64_t *)(&(buf->buffer[startRead]));
    break;
#else
    // Else, copy byte by byte.
    // Unroll the smaller, more common amounts.  Include extra amounts because the cost is essentially free.
    case 8:
        *(output++) = buf->buffer[startRead++]; //lint -e(616)
    case 7:
        *(output++) = buf->buffer[startRead++]; //lint -e(616)
    case 6:
        *(output++) = buf->buffer[startRead++]; //lint -e(616)
    case 5:
        *(output++) = buf->buffer[startRead++]; //lint -e(616)
    case 4:
        *(output++) = buf->buffer[startRead++]; //lint -e(616)
    case 3:
        *(output++) = buf->buffer[startRead++]; //lint -e(616)
    case 2:
        *(output++) = buf->buffer[startRead++]; //lint -e(616)
    case 1:
        *(output++) = buf->buffer[startRead++];
        break;
#endif
    default:
        while (startRead <= endRead)
        {
            *(output++) = buf->buffer[startRead++];
        }
        break;
    }
    *bytesRead = length;
    return retCode;
}

/**
 * Calculate the NSAP address based on the tuning parameters.
 * NSAP address is defined per OCAP, I15, Table 22-2.
 *
 * @param oc        The object carousel to calculate the NSAP address for.
 *                  Value is set directly in the carousel's NSAP field.
 *
 * @return MPE_SUCCESS if all info could be calculated correctly.
 */

static mpe_Error ocCalculateNSAP(mpe_ObjectCarousel *oc)
{
    mpe_Error retCode = MPE_SUCCESS;
    uint32_t sourceID = 0;
    uint32_t carouselID = 0;
    uint32_t prog = 0;
    uint32_t appId = 0;
    mpe_OcTuningParams params;

    // Get the frequency, program number
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
            "OC: Calculating NSAP address for carousel %04x\n", oc->carouselID);

    retCode = ocuSetTuningParams(oc->siHandle, &params);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OC: Could not get Tuning Parameters\n");
        return retCode;
    }

    if (params.transport == MPE_OC_IB)
    {
        retCode = ocuGetSourceID(oc->siHandle, &sourceID);
        // BUG: What if this is a dynamic service and doesn't have a source ID?
        // Huh, huh, huh?  What about a recorded service.
        if (retCode != MPE_SUCCESS)
        {
            // HACK: FIXME: TODO: Use SourceID == 0.
            sourceID = 0;
        }
    }

    carouselID = oc->carouselID;
    switch (params.transport)
    {
    case MPE_OC_IB:
        prog = 0;
        break;
    case MPE_OC_OOB:
        prog = params.t.oob.prog;
        break;
    case MPE_OC_DSG:
        prog = params.t.dsg.prog;
        appId = params.t.dsg.appId;
        break;
    default:
        return MPE_EINVAL;
    }

    // OCAP NSAP address
    // Defined in OCAP I15, page 188
    // OpenCable OC-SP-OCAP1.0-I15-050415
    oc->nsapAddress[0] = (uint8_t) 0x00; // AFI.  Defined to 0.
    oc->nsapAddress[1] = (uint8_t) 0x00; // Type.  Defined to 0.

    oc->nsapAddress[2] = (uint8_t)((carouselID >> 24) & 0xff); // Carousel ID.  4 bytes
    oc->nsapAddress[3] = (uint8_t)((carouselID >> 16) & 0xff);
    oc->nsapAddress[4] = (uint8_t)((carouselID >> 8) & 0xff);
    oc->nsapAddress[5] = (uint8_t)(carouselID & 0xff);

    oc->nsapAddress[6] = (uint8_t) 0x01; // Specifier Type.  Defined to be 01

    if (ocUseDVBNSAP)
    {
        // Fill in the rest of the NSAP per DVB
        oc->nsapAddress[7] = (uint8_t) 0x00; // Specifier.  0x00015a.  DVB.
        oc->nsapAddress[8] = (uint8_t) 0x01;
        oc->nsapAddress[9] = (uint8_t) 0x5a;

        oc->nsapAddress[10] = (uint8_t) 0x00; // transport stream ID.  Defined to be 0.
        oc->nsapAddress[11] = (uint8_t) 0x00;

        oc->nsapAddress[12] = (uint8_t) 0x00; // orginal network ID.  Defined to be 0.
        oc->nsapAddress[13] = (uint8_t) 0x00;

        oc->nsapAddress[14] = (uint8_t)((prog >> 8) & 0xff);
        oc->nsapAddress[15] = (uint8_t)(prog & 0xff);

        oc->nsapAddress[16] = (uint8_t) 0xff;
    }
    else
    {
        // OCAP. Fill in per OCAP I16
        // DSG. ECN 858
        oc->nsapAddress[7] = (uint8_t) 0x00; // Specifier.  0x001000.  CableLabs Org.
        oc->nsapAddress[8] = (uint8_t) 0x10;
        oc->nsapAddress[9] = (uint8_t) 0x00;

        oc->nsapAddress[10] = (uint8_t) 0x00; // transport stream ID.  Defined to be 0.
        oc->nsapAddress[11] = (uint8_t) 0x00;

        oc->nsapAddress[12] = (uint8_t) 0x00; // orginal network ID.  Defined to be 0.
        oc->nsapAddress[13] = (uint8_t) 0x00;

        oc->nsapAddress[17] = (uint8_t) 0xff;
        oc->nsapAddress[18] = (uint8_t) 0xff; //reserved for IB/OOB
        oc->nsapAddress[19] = (uint8_t) 0xff;

        if (params.transport == MPE_OC_OOB)
        {
            // OOB
            // 14-15 contains the program number in the OOB channel
            oc->nsapAddress[14] = (uint8_t)((prog >> 8) & 0xff);
            oc->nsapAddress[15] = (uint8_t)(prog & 0xff);
            oc->nsapAddress[16] = (uint8_t) 0xff; //first 2 bits are '11' for OOB
        }
        else if (params.transport == MPE_OC_DSG)
        {
            oc->nsapAddress[14] = (uint8_t)((prog >> 8) & 0xff);
            oc->nsapAddress[15] = (uint8_t)(prog & 0xff);
            oc->nsapAddress[16] = (uint8_t) 0xbf; //first 2 bits are '101 for DSG
            oc->nsapAddress[18] = (uint8_t)((appId >> 8) & 0xff);
            oc->nsapAddress[19] = (uint8_t)(appId & 0xff);
        }
        else
        {
            // Inband.
            // 14-15 contains the service ID
            oc->nsapAddress[14] = (uint8_t)((sourceID >> 8) & 0xff);
            oc->nsapAddress[15] = (uint8_t)(sourceID & 0xff);
            oc->nsapAddress[16] = (uint8_t) 0x7f; //first 2 bits are '01' for IB
        }
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "OC: Calculated %s NSAP: %s\n",
            (ocUseDVBNSAP ? "DVB" : "OCAP"), ocNSAPString(oc->nsapAddress));

    return retCode;
}

/**
 * Comparison function for filenames.
 * Like strcmp, but stops on a / or a \0.
 *
 */
static inline mpe_Bool ocCompareFileNames(char *a, char *b)
{
    char A = *a;
    char B = *b;
    // while we're in string characters, and we haven't hit a directory separator, keep going.
    while ((A == B) && (A != '\0') && (B != '\0') && (A != PATH_SEPARATOR)
            && (B != PATH_SEPARATOR))
    {
        ++a;
        ++b;
        A = *a;
        B = *b;
    }
    // if either of the characters we're dealing with is a directory separator, turn it into a NULL
    if (A == PATH_SEPARATOR)
    {
        A = '\0';
    }
    // We'd do the same with B, but B is the name coming from the OC, not the full path, so B will never
    // be a '/'
    // if (B == PATH_SEPARATOR) { B = '\0'; }
    return (A == B);
}
