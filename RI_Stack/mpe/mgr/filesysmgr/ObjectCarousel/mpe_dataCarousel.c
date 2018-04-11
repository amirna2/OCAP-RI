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

#include <string.h>

#include "mpe_dataCarousel.h"
#include "mpe_objectCarouselSectionParser.h"
#include "mpe_objectCarouselUtils.h"
#include "mpe_cacheManager.h"

#include "mpe_os.h"
#include "mpe_si.h"
#include "mpe_dbg.h"
#include "mpe_error.h"
#include "mpe_file.h"
#include "mpe_os.h"
#include "mpe_types.h"
#include "mpe_filter.h"
#include "mpe_filterevents.h"
#include "mpe_media.h"
#include "mpe_pod.h"

#include "../mgr/include/podmgr.h"

#include "mpeos_media.h"

#include "zlib.h"

/*
 * Constants
 */

// Size of the hash array.
#define     DC_HASH_PRIME                   (32)

// Size of the stack for the Async events thread
#define     DC_THREAD_STACK_SIZE            (64 * 1024)

// Memory handle priority
#define     DC_MEM_PRIORITY                 ((MPE_MEM_PRIOR_HIGH +  MPE_MEM_PRIOR_LOW) / 2)

// Default configuration parameters
#define     DC_PREFETCH_START               (8)
#define     DC_PREFETCH_DISTANCE            (16)
#define     DC_CHECK_VERSION                (1)
#define     DC_MAX_PREFETCH                 (8)
#define     DC_DEFAULT_TIMEOUT              (120 * 1000)
#define     DC_DECOMPRESSION_SIZE           (1024)
#define     DC_PRE_DECOMPRESS_SIZE          (0)
#define     DC_PREFETCH_MODULES             (0)
#define     DC_PREFETCH_MOUNTSIZE           (0)
#define     DC_PREFETCH_MODULESIZE          (0)
#define     DC_PREFETCH_MOPUP               (0)
#define     DC_CHECK_VERSION_OOB            (0)
#define     DC_PREFETCH_OOB                 (1)
#define     DC_POLL_DII_OOB                 (1)
#define     DC_DII_POLLING_INTERVAL         (30 * 1000)
#define     DC_DII_CHECK_DURATION           (10 * 1000)
#define     DC_DII_CHECK_DURATION_OOB       (0)

#define     DC_CHECK_CONSECUTIVE            (0)

#define     DC_CHECK_VERSION_NAME           "OC.CHECK.VERSION"
#define     DC_PREFETCH_DISTANCE_NAME       "OC.PREFETCH.DISTANCE"
#define     DC_PREFETCH_START_NAME          "OC.PREFETCH.START"
#define     DC_MAX_PREFETCH_NAME            "OC.MAX.PREFETCH"
#define     DC_DEFAULT_TIMEOUT_NAME         "OC.DEFAULT.TIMEOUT"
#define     DC_DECOMPRESSION_SIZE_NAME      "OC.DECOMPRESSION.SIZE"
#define     DC_PRE_DECOMPRESS_SIZE_NAME     "OC.PRE.DECOMPRESS.SIZE"
#define     DC_PREFETCH_MODULES_NAME        "OC.PREFETCH.MODULES"
#define     DC_PREFETCH_MOUNTSIZE_NAME      "OC.PREFETCH.MOUNTSIZE"
#define     DC_PREFETCH_MODULESIZE_NAME     "OC.PREFETCH.MODULESIZE"
#define     DC_PREFETCH_MOPUP_NAME          "OC.PREFETCH.MOPUP"
#define     DC_PREFETCH_OOB_NAME            "OC.PREFETCH.OOB"
#define     DC_CHECK_VERSION_OOB_NAME       "OC.CHECK.VERSION.OOB"
#define     DC_POLL_DII_OOB_NAME            "OC.POLL.DII.OOB"
#define     DC_DII_POLLING_INTERVAL_NAME    "OC.DII.POLLING.INTERVAL"
#define     DC_DII_CHECK_DURATION_NAME      "OC.DII.CHECK.DURATION"
#define     DC_DII_CHECK_DURATION_OOB_NAME  "OC.DII.CHECK.DURATION.OOB"

#define     DC_CHECK_CONSECUTIVE_NAME       "OC.CHECK.CONSECUTIVE"

// Event to use to check the timeout
#define     DC_CHECK_TIMEOUT                (0x5000)
#define     DC_MOPUP_MODULE                 (0x5001)

// Caching transparency level times
#define     DC_TRANSPARENT_CACHING_INTERVAL     (500)
#define     DC_SEMITRANSPARENT_CACHING_INTERVAL (30000)

// If the timeout is set to all one's, use the default
#define     DC_USE_DEFAULT_TIMEOUT          (0xffffffff)

#define     DC_IS_OOB(dc)                   ((dc)->params.transport == MPE_OC_OOB)
#define     DC_IS_DSG(dc)                   ((dc)->params.transport == MPE_OC_DSG)
#define     DC_IS_IB(dc)                    ((dc)->params.transport == MPE_OC_IB)

// Macros to determine characteristics of events
#define     EVENT_IS_CANCEL(ev)             ((ev != MPE_SF_EVENT_SECTION_FOUND) && (ev != MPE_SF_EVENT_LAST_SECTION_FOUND))
#define     EVENT_IS_TERMINAL(ev)           (ev != MPE_SF_EVENT_SECTION_FOUND)
#define     EVENT_IS_SECTION(ev)            ((ev == MPE_SF_EVENT_SECTION_FOUND) || (ev == MPE_SF_EVENT_LAST_SECTION_FOUND))

#define     DC_INVALID_TUNER                (0xffffffff)
#define     DC_INVALID_FREQUENCY            (0xffffffff)
#define     DC_INVALID_DDB                  (0xffffffff)
#define     DC_INVALID_PID                  (0xffffffff)

// Cast for dcZAlloc
#define     ZALLOC_FUNCPTR                  void * (*)(void *, unsigned int, unsigned int)

/*
 * Local type declarations
 */
typedef enum
{
    DII_UPDATE, DDB_DEMANDFETCH, DDB_PREFETCH, MODULE_PREFETCH, DII_POLL
} DcRequestType;
typedef struct DcRequestRecord DcRequestRecord;

struct DcRequestRecord
{
    uint32_t uniqueifier; // The uniqueifier in the request
    mpe_FilterSpec *filter; // The filter for this request
    DcRequestType reqType; // The request type, DDB preftech or DII version change
    mpe_DataCarousel *dc; // The outstanding request
    mpe_DcModule *module; // The module this occurred to (for prefetch)
    uint32_t ddb; // The DDB to update (for prefetch)
    mpe_TimeMillis timeout; // The time at which this should timeout.  0 means no timeout.
    DcRequestRecord *next; // Linked list of elements.
};

typedef struct
{
    mpe_Bool compressedProxy;
    mpe_DcModule *module;
    mpe_Bool inited;
    uint32_t uncompOffset;
    uint32_t compOffset;
    uint8_t *inBuffer;
    uint8_t *outBuffer;
    z_stream zStream;
} DcCompressedModule;

// This structure represents a wrapper around a DDB section

typedef struct mpe_DcDDB
{
    mpe_DccCacheObject *sectionHandle; // Handle to the cache block for this DDB
    uint32_t dataOffset; // The offset to the first byte of data in the section
    mpe_Bool prefetched; // True if a prefetch is in flight for this DDB
} mpe_DcDDB;

struct mpe_DcModule
{
    mpe_Bool compressedProxy; // Is this a proxy for a compressed

    mpe_DataCarousel *dc; // Pointer back to the data carousel containing this module

    // Copied information, from the DII
    uint16_t moduleId; // ID number of this module
    uint32_t moduleSize; // Number of bytes in the module
    uint32_t moduleVersion; // The current version of the module
    uint32_t downloadID; // The Download ID specified in the DII
    uint16_t assocTag;
    uint32_t blockSize; // Number of bytes per block
    uint32_t ddbTimeout; // Timeout for a DDB.

    // Descriptor information
    char *label; // Label, if any.
    mpe_Bool linkedModule; // Is there a link to the next module?
    uint8_t position; // Is this the first link in the chain?
    uint16_t nextModuleId; // The next module, if there's a link.
    mpe_Bool compressedModule; // Is module compressed?
    uint32_t originalSize; // Size of uncompressed module.
    uint8_t cachingPriority;
    uint8_t cachingTransparency;

    // Where to find this module
    uint32_t pid; // What pid was this last seen on?

    // Info on locked DDB's
    uint32_t lockedDDB;

    // Computed information
    uint32_t numDDBs; // Number of DDB's in the module

    mpe_Bool loaded; // True if the module is loaded, and all blocks are locked in memory.
    mpe_Bool prefetched; // True if we've issued a prefetch for the whole module
    mpe_DcDDB *ddbs; // The DDB's which make up this module
    int referenceCount; // Number of outstanding holds on this module
    mpe_Bool valid; // Is this module still valid?  Or is a new version available

    uint8_t *decompressedData; // Buffer of data that will be decompressed before accessing

    // User data field
    void *userData; // Undefined user data pointer.
    void (*userDataDeleteFn)(void *); // Callback function when user data should be deleted.
};

// This structure represents a mounted data carousel.
struct mpe_DataCarousel
{
    mpe_SiServiceHandle siHandle; // Handle for SI information about the main program.
    mpe_OcTuningParams params; // Where to find the carousel
    mpe_PODDecryptSessionHandle decryptSessionHandle; // CA session handle
    uint16_t associationTag; // The Association Tag of this carousel.
    uint32_t pid; // The PID where the DII was last seen.
    uint32_t selector;
    mpe_Bool pollDII; // Are we polling for DII updates, or constantly listening.
#ifdef PTV
    uint32_t carouselID; // CarouselID of this DC.  Only on PowerTV OOB.
#endif
    // mpe_EdEventInfo      *edHandle;            // Register for SI change events (added on 9/16/05)
    uint32_t totalSize; // total size of the carousel

    // mpe_EventQueue          eventQ;            // Private queue used to get notified of demand fetched
    // section completions by the section filter

    char name[32]; // String holding a "name" of the carousel.  For debugging.

    uint32_t blocksize; // Size of each DDB.
    uint32_t numModules;
    uint32_t diiTimeOut; // Time out while waiting for a DII or DSI, in milliseconds
    uint32_t ddbTimeOut; // Time out period for waiting for a DDB, in milliseconds

    uint32_t diiVersion;
    uint32_t diiTransactionID;

    mpe_TimeMillis lastDiiCheck; // Last time the DII was checked.

    mpe_Bool mounted; // Is this carousel still mounted?
    mpe_Bool connected; // Is this carousel currently connected (tuned to)?
    mpe_Bool monitored; // Is this carousel currently being monitored for DII changes?
    mpe_Bool authorized; // Is this carousel CA-authorized?

    mpe_PODDecryptSessionHandle decryptHandle;

    DcRequestRecord *diiRequest; // Request for DII updates

    uint32_t referenceCount; // Reference count for this carousel.

    mpe_EventQueue updateQueue; // Queue to send a message to when the DII is updated
    mpe_Bool updateQueueRegistered;// Has this queue been registered
    void *updateData; // Data to send to the queue above when DII is updated

    mpe_DcModule **modules; // The modules themselves
};

/*
 * Useful macros
 */

#define DC_REQUEST_TYPE_NAME(req)   \
    ((req == DDB_PREFETCH) ? "Prefetch" : \
     ((req == DDB_DEMANDFETCH) ? "Demand fetch" : \
      ((req == MODULE_PREFETCH) ? "Module Prefetch" : \
       ((req == DII_UPDATE) ? "DII Update" : "DII Poll"))))

#define DC_TRANSPORT_NAME(dc) \
    ((dc->params.transport == MPE_OC_IB) ? "IB" : \
        ((dc->params.transport == MPE_OC_OOB) ? "OOB" : "DSG"))

/*
 * Forward declarations.
 */
static void dcFreeDC(mpe_DataCarousel *);
static void dcFreeModule(mpe_DcModule *);
static void dcReleaseDC(mpe_DataCarousel *);

static mpe_Error dcReadRawModuleBytes(mpe_DcModule *, uint32_t, uint32_t,
        uint32_t, uint8_t *, uint32_t *);

static mpe_Error dcLoadDII(mpe_DataCarousel *, uint32_t);
static mpe_Error dcFetchDDB(mpe_DcModule *, uint32_t, DcRequestType,
        mpe_EventQueue, mpe_TimeMillis);

static mpe_Error dcRegisterDC(mpe_DataCarousel *);
static void dcUnregisterDC(mpe_DataCarousel *);

static void dcThreadAsync(void *);
static void dcThreadMopup(void *);

static void dcInsertAsyncRequest(DcRequestRecord *);
static mpe_Error dcStartAsyncRequest(mpe_FilterSource *, mpe_FilterSpec *,
        DcRequestType, mpe_DataCarousel *, mpe_DcModule *, uint32_t,
        mpe_EventQueue, mpe_TimeMillis, uint32_t, DcRequestRecord **);

static mpe_Error dcSetUpdateWatcher(mpe_DataCarousel *, uint32_t);
static void dcStartPrefetch(mpe_DcModule *, uint32_t);
static void dcFreeRequestRecord(DcRequestRecord *);

static mpe_DcModule *dcFindModule(mpe_DataCarousel *, uint16_t);
static void dcInvalidateModule(mpe_DcModule *);

static void dcCancelDCRequests(mpe_DataCarousel *);
static void dcCancelModulePrefetches(mpe_DcModule *);
//static void dcReStartDCRequests(mpe_DataCarousel *dc);

static void dcHandleAsyncSection(mpe_Event, uint32_t);
static void dcHandleCancel(uint32_t);
static void dcHandleSectionEvent(mpe_Event, uint32_t);

static void dcReconnectDC(mpe_DataCarousel *, uint32_t);
static void dcRestartPrefetch(mpe_DataCarousel *);
static void dcCancelFilterRequests(mpe_DataCarousel *);
static void dcRestartUpdateWatcher(mpe_DataCarousel *);

static mpe_Error dcNewCompressedModule(mpe_DcModule *, DcCompressedModule **);
static mpe_Error dcInitCompressedModule(DcCompressedModule *);
static void dcFreeCompressedModule(DcCompressedModule *);
static mpe_Error dcSeekCompressedModule(DcCompressedModule *, uint32_t);
static mpe_Error dcReadCompressedBytes(DcCompressedModule *, uint32_t,
        uint32_t, uint8_t *, uint32_t *);

static mpe_Error dcPreDecompressModule(mpe_DcModule *);

static mpe_Error dcSetModulePid(mpe_DcModule *);

static mpe_TimeMillis dcCalculateTimeout(uint32_t);

static mpe_Error dcHandleCaEvent( mpe_Event,
                                  mpe_PODDecryptSessionHandle,
                                  uint32_t,uint32_t );
/*
 * Private variable declarations.
 */
static mpe_Mutex dcGlobalMutex = 0;

static mpe_ThreadId dcWatcherThreadID = 0;
static mpe_ThreadId dcMopupThreadID = 0;

static mpe_EventQueue dcGlobalEventQueue;
static mpe_EventQueue dcGlobalMopupQueue;

static mpe_DataCarousel *dcDataCarousels[DC_MAX_DATACAROUSELS] =
{ NULL };
static uint32_t dcRegisteredCarousels = 0;
static DcRequestRecord *dcAsyncRequests[DC_HASH_PRIME] =
{ NULL };

static uint32_t dcOutstandingPrefetches = 0;

static mpe_TimeMillis dcEarliestTimeout = 0;
static DcRequestRecord *dcEarliestTimeoutReq = NULL;

/*
 * Configuration parameters
 */
static uint32_t dcCheckVersion = DC_CHECK_VERSION;
static uint32_t dcPrefetchDistance = DC_PREFETCH_DISTANCE;
static uint32_t dcPrefetchStart = DC_PREFETCH_START;
static uint32_t dcMaxPrefetch = DC_MAX_PREFETCH;
static uint32_t dcDefaultTimeout = DC_DEFAULT_TIMEOUT;
static uint32_t dcDecompressionSize = DC_DECOMPRESSION_SIZE;
static uint32_t dcPreDecompressSize = DC_PRE_DECOMPRESS_SIZE;
static uint32_t dcPrefetchModules = DC_PREFETCH_MODULES;
static uint32_t dcPrefetchMountSize = DC_PREFETCH_MOUNTSIZE;
static uint32_t dcPrefetchModuleSize = DC_PREFETCH_MODULESIZE;
static uint32_t dcPrefetchMopup = DC_PREFETCH_MOPUP;

static uint32_t dcCheckVersionOOB = DC_CHECK_VERSION_OOB;
static uint32_t dcPrefetchOOB = DC_PREFETCH_OOB;
static uint32_t dcPollDIIOOB = DC_POLL_DII_OOB;
static uint32_t dcDIIPollingInterval = DC_DII_POLLING_INTERVAL;
static uint32_t dcDIICheckDuration = DC_DII_CHECK_DURATION;
static uint32_t dcDIICheckDurationOOB = DC_DII_CHECK_DURATION_OOB;

static uint32_t dcMountCount = 0;

/**
 * Initialize the data carousel module.  Start the thread to watch for changes.
 *
 * @return MPE_SUCCESS if the routine succeeds, failure otherwise.
 */
mpe_Error mpe_dcInit(void)
{
    mpe_Error retCode;
    int i;

    mpe_siInit();

    if (dcGlobalMutex != 0)
    {
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILESYS, "DC: Already initialized\n");
        return MPE_FS_ERROR_INVALID_STATE;
    }

    // Create the global things.
    retCode = mpe_mutexNew(&dcGlobalMutex);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "DC: Could not initialize global lock: Error code %04x\n",
                retCode);
        return retCode;
    }

    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(dcGlobalMutex);

    // Clear out the data carousels pointer
    for (i = 0; i < DC_MAX_DATACAROUSELS; i++)
    {
        dcDataCarousels[i] = NULL;
    }

    // Clear out the async request pointers
    for (i = 0; i < DC_HASH_PRIME; i++)
    {
        dcAsyncRequests[i] = NULL;
    }

    // Read the environment variables

    // First, enable version checking
    dcCheckVersion = ocuGetEnv(DC_CHECK_VERSION_NAME, DC_CHECK_VERSION);

    // Next, prefetch distance
    dcPrefetchDistance = ocuGetEnv(DC_PREFETCH_DISTANCE_NAME,
            DC_PREFETCH_DISTANCE);

    // Next, number of prefetches to start
    dcPrefetchStart = ocuGetEnv(DC_PREFETCH_START_NAME, DC_PREFETCH_START);

    // Next, number of prefetches to start
    dcMaxPrefetch = ocuGetEnv(DC_MAX_PREFETCH_NAME, DC_MAX_PREFETCH);

    // Next, the default timeout, in millisecnods
    dcDefaultTimeout = ocuGetEnv(DC_DEFAULT_TIMEOUT_NAME, DC_DEFAULT_TIMEOUT);

    // Next, Whether to use the prefetch entire modules
    dcPrefetchModules
            = ocuGetEnv(DC_PREFETCH_MODULES_NAME, DC_PREFETCH_MODULES);

    // Next, Whether to prefetch all modules when they're new
    dcPrefetchMountSize = ocuGetEnv(DC_PREFETCH_MOUNTSIZE_NAME,
            DC_PREFETCH_MOUNTSIZE);

    // Next, Whether to prefetch some modules when they're new
    dcPrefetchModuleSize = ocuGetEnv(DC_PREFETCH_MODULESIZE_NAME,
            DC_PREFETCH_MODULESIZE);

    // Next, Whether to aggressively try to get any DDBs we missed on the multimatch
    dcPrefetchMopup = ocuGetEnv(DC_PREFETCH_MOPUP_NAME, DC_PREFETCH_MOPUP);

    // Enable prefetching in OOB carousels
    dcPrefetchOOB = ocuGetEnv(DC_PREFETCH_OOB_NAME, DC_PREFETCH_OOB);

    // Enable version checking for OOB
    dcCheckVersionOOB = ocuGetEnv(DC_CHECK_VERSION_OOB_NAME,
            DC_CHECK_VERSION_OOB);

    // DII Polling parameters
    dcPollDIIOOB = ocuGetEnv(DC_POLL_DII_OOB_NAME, DC_POLL_DII_OOB);
    dcDIIPollingInterval = ocuGetEnv(DC_DII_POLLING_INTERVAL_NAME,
            DC_DII_POLLING_INTERVAL);
    dcDIICheckDuration = ocuGetEnv(DC_DII_CHECK_DURATION_NAME,
            DC_DII_CHECK_DURATION);
    dcDIICheckDurationOOB = ocuGetEnv(DC_DII_CHECK_DURATION_OOB_NAME,
            DC_DII_CHECK_DURATION_OOB);

    // The decompression block size
    dcDecompressionSize = ocuGetEnv(DC_DECOMPRESSION_SIZE_NAME,
            DC_DECOMPRESSION_SIZE);

    // Should we decompress the entire block?
    dcPreDecompressSize = ocuGetEnv(DC_PRE_DECOMPRESS_SIZE_NAME,
            DC_PRE_DECOMPRESS_SIZE);

    // Create the queue we'll use to wait for update events
    retCode = mpe_eventQueueNew(&dcGlobalEventQueue, "MpeDcEvent");
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "DC: Could not create global event queue: Error code %04x\n",
                retCode);
        return retCode;
    }
    retCode = mpe_eventQueueNew(&dcGlobalMopupQueue, "MpeDcMopup");
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "DC: Could not create global mopup queue: Error code %04x\n",
                retCode);
    }

    if (dcPrefetchMopup)
    {
        retCode = mpe_threadCreate(dcThreadMopup, NULL, MPE_THREAD_PRIOR_MIN,
                DC_THREAD_STACK_SIZE, &dcMopupThreadID, "mpeDCMopupThread");
        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_FILESYS,
                    "DC: Could not create version change watcher thread: Error code %04x\n",
                    retCode);
            return retCode;
        }
    }

    // Start the Async Response Thread
    retCode = mpe_threadCreate(dcThreadAsync, NULL,
            MPE_THREAD_PRIOR_SYSTEM_MED, DC_THREAD_STACK_SIZE,
            &dcWatcherThreadID, "mpeDCAsyncRequestThread");
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_FILESYS,
                "DC: Could not create version change watcher thread: Error code %04x\n",
                retCode);
    }

    mpe_mutexRelease(dcGlobalMutex);
    // END CRITICAL SECTION

    mpe_dccInit();

    return retCode;
}

/**
 * Mount an object carousel and prepare it for loading.  Reads the DII(s) and
 * DSI (if necessary), and returns.
 * Question: Do we need to indicate a timeout?
 *
 * @param siHandle          Handle in SIDB to indicate the service containing this carousel.
 * @param associationTag    Tag of the stream in which to find the DII of this carousel.
 * @param selector          Selector bits for finding the DII.
 *                          Needs to include all the selector bits, but only bits 1-15 will
 *                          be use.  Bits 16-31 should be 0's, per MHP.
 * @param timeout           Time, in Milliseconds, to wait for the DII to be found.
 * @param dc                [out] Output parameter, returns a pointer to the data carousel object.

 * The next parameter is a hack to support PowerTV's nonstandard filtering for OOB DII's.
 * @param carouselID            For OOB, what is the carouselID of this DII.
 *
 * @returns MPE_SUCCESS if the carousel is correctly mounted, error codes if not.
 */
mpe_Error mpe_dcMount(mpe_SiServiceHandle siHandle, uint16_t associationTag,
        uint32_t selector, uint32_t timeout,
#ifdef PTV
        uint32_t carouselID,
#endif
        mpe_DataCarousel **retDc)
{
    mpe_Error retCode;
    mpe_DataCarousel *dc = NULL; // Just keep around for grins.  Don't
    // have to doubly redirect through
    // retDc all the time.

    // Let's clear out the returned object.  This simplifies error checks below.  We'll assign
    // it at the end.
    *retDc = NULL;

    // Allocate up the data carousel structure
    retCode = mpeos_memAllocP(MPE_MEM_FILE_CAROUSEL, sizeof(mpe_DataCarousel),
            (void **) &dc);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "DC: Cannot allocate data carosuel structure\n");
        return retCode;
    }

    // Clear it out of any crap.  Just for safety's sake
    memset(dc, 0, sizeof(mpe_DataCarousel));

    // Generate a name for debugging
    mpe_mutexAcquire(dcGlobalMutex);
    sprintf(dc->name, "C%u(ID:%u)", dcMountCount++, associationTag);
    mpe_mutexRelease(dcGlobalMutex);

    // Copy in the association tag and the siHandle
    dc->siHandle = siHandle;
    dc->associationTag = associationTag;

    // Mark that it's valid, and that we have a handle on it (reference count of 1)
    dc->mounted = TRUE;
    dc->connected = TRUE;
    dc->authorized = TRUE; // OC will ensure authorization prior to DC mount
    dc->referenceCount = 1;

    // Clear out other parameters
    dc->numModules = 0;
    dc->updateQueueRegistered = FALSE;
    dc->updateData = NULL;
    dc->totalSize = 0;
    dc->pollDII = FALSE;
    dc->monitored = FALSE;

    // Set the timeout for the DII
    if (timeout == DC_USE_DEFAULT_TIMEOUT)
    {
        dc->diiTimeOut = dcDefaultTimeout;
    }
    else
    {
        dc->diiTimeOut = timeout;
    }

    // Figure out the frequency that this carousel lives on.
    retCode = ocuSetTuningParams(siHandle, &(dc->params));
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_FILESYS,
                "DC: %s: Couldn't get frequency/program number for carousel from SI Handle: %04x\n",
                dc->name, dc->siHandle);
        goto CleanUp;
    }

    // Set the PID
    retCode = ocuTranslateAssociationTag(siHandle, associationTag, &(dc->pid));
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "DC: %s: Couldn't Translate association tag: %04x\n", dc->name,
                dc->associationTag);
        goto CleanUp;
    }

    // Mark if this DC is IB or OOB
    if (DC_IS_OOB(dc) && dcPollDIIOOB)
    {
        dc->pollDII = true;
    }

#ifdef PTV
    dc->carouselID = carouselID;
#endif

    // Figure out where this carousel resides
    // BUG: FIXME: TODO: Should this really come from above someplace?
    if (DC_IS_IB(dc))
    {
        retCode = ocuSetTuner(&(dc->params));
        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                    "DC: %s: Could not find tuner with frequency %d\n",
                    dc->name, dc->params.t.ib.frequency);
            goto CleanUp;
        }
    }

    // Register the data carousel
    retCode = dcRegisterDC(dc);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "DC: %s: Couldn't register Data Carousel : %d\n", dc->name,
                retCode);
        goto CleanUp;
    }

    if (DC_IS_IB(dc))
    {
        // Setup the CA session
        retCode = ocuStartCASession( dc->params.t.ib.tunerId, dc->siHandle,
                                     dc->pid, dcGlobalEventQueue, &(dc->decryptSessionHandle) );
        if (retCode != MPE_SUCCESS)
        {
            // If we couldn't allocate it, get out.
            MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILESYS,
                    "DC: ocuStartCASession returned error %d\n", retCode);
            goto CleanUp;
        }
        // Assert: dc->authorized is true (see above)
    }

    MPE_LOG(MPE_LOG_INFO, MPE_MOD_FILESYS,
            "DC: ocuStartCASession returned session 0x%p\n", dc->decryptSessionHandle);

    MPE_LOG(MPE_LOG_INFO, MPE_MOD_FILESYS,
            "DC: %s: Mounting %s stream on tuner %d:\n", dc->name,
            DC_TRANSPORT_NAME(dc), (DC_IS_IB(dc) ? dc->params.t.ib.tunerId : 0));

    retCode = dcLoadDII(dc, selector);

    // An error occured in loading the DII.  Free up the data carousel.
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "DC: %s: Unable to load DII: %d\n", dc->name, retCode);
        goto CleanUp;
    }

    // Start watching for the version changes
    // Note, this is the very last thing we do, so we don't have to stop the filter
    // in cleanup.  If this doesn't succeed, there is no filter to cleanup.
    if (dcCheckVersion)
    {
        retCode = dcSetUpdateWatcher(dc, (DC_IS_OOB(dc) ? dcDIICheckDurationOOB
                : dcDIICheckDuration));
        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_FILESYS,
                    "DC: %s: Unable to start watching for version change events\n",
                    dc->name);
            goto CleanUp;
        }
    }

    // Got this far, must be ready to go.
    *retDc = dc;
    return retCode;

    CleanUp:

    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS, "DC: %s: Mount Failed\n",
            dc->name);

    // Mark it unmounted so release will delete it
    dc->mounted = false;
    // Cancel any prefetches which started
    dcCancelDCRequests(dc);
    // Free up the DC
    dcReleaseDC(dc);

    return retCode;
} /* End mpe_dcMount */

/**
 * Unmount an object carousel, and free the underlying data structures.
 *
 * @param dc The data carousel we wish to unmount.
 *
 * @returns MPE_SUCCESS if the carousel is correctly unmounted, error codes if not.
 */
mpe_Error mpe_dcUnmount(mpe_DataCarousel * dc)
{
    mpe_Error retCode = MPE_SUCCESS;
    mpe_Bool freeDC = false;

    MPE_LOG(MPE_LOG_INFO, MPE_MOD_FILESYS, "DC: %s: Unmounting carousel\n",
            dc->name);
    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(dcGlobalMutex);

    // Make sure the carousel is not already unmounted.
    if (!dc->mounted)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "DC: %s: Attempting to unmount an unmounted carousel\n",
                dc->name);
        retCode = MPE_FS_ERROR_INVALID_STATE;
    }
    else
    {
        // Not mounted now
        dc->mounted = FALSE;

        // Cancel any outstanding requests
        dcCancelDCRequests(dc);

        // Decrement the reference count
        dc->referenceCount--;

        mpe_podStopDecrypt(dc->decryptSessionHandle);
        dc->authorized = FALSE;
        dc->decryptSessionHandle = 0;

        // Now, check to see if we've finished with this carousel completely.
        if (dc->referenceCount == 0)
        {
            // Nobody has a reference, blow it away
            freeDC = true;
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                    "DC: %s: Reference count is 0.  Unmounting immediately\n",
                    dc->name);
        }
        else
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                    "DC: %s: Reference count is %d.  Delaying unmount\n",
                    dc->name, dc->referenceCount);
        }
    }
    mpe_mutexRelease(dcGlobalMutex);
    // END CRITICAL SECTION

    // If we want to blow it away, do it.
    if (freeDC)
    {
        dcFreeDC(dc);
    }

    return retCode;
}

/**
 * Return a handle to a module within the carousel.  This does not necessarily cause
 * any data to be read from the carousel.  Increments a reference count in the module to make
 * sure it's not deallocated when any data (files) within it are being used.  Modules MUST
 * be released when they're no longer need to allow them to be deallocated. @see{mpe_dcReleaseModule}.
 *
 * @param dc The data carousel to use.
 * @param moduleId The module ID to get.
 * @param module Output parameter, will contain the module object.
 *
 * @returns MPE_SUCCESS if the module object is retrieved, MPE_FS_ERROR_INVALID_DATA if the module ID doesn't exist.
 */
mpe_Error mpe_dcGetModule(mpe_DataCarousel *dc, uint16_t moduleId,
        mpe_DcModule ** const retModule)
{
    mpe_Error retCode = MPE_FS_ERROR_INVALID_DATA; // Assume we didn't find it.
    mpe_DcModule *module;
    DcCompressedModule *compMod = NULL;
    mpe_Bool current;

    *retModule = NULL; // Error condition

    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(dcGlobalMutex);

    // Get the module
    module = dcFindModule(dc, moduleId);
    if (module != NULL)
    {
        // Now, check to see if what we've got is up to date.
        retCode = mpe_dcCheckModuleCurrent(module, &current);
        if (retCode != MPE_SUCCESS)
        {
            // Ugly, set the module to NULL, and we'll fall out.
            module = NULL;
        }
        else if (!current)
        {
            // Get the module again.  CheckModuleCurrent
            // will have updated the DII for us.
            module = dcFindModule(dc, moduleId);
        }
    }

    if (module != NULL)
    {
        // Increase the reference count
        module->referenceCount++;
    }
    mpe_mutexRelease(dcGlobalMutex);
    // END CRITICAL SECTION

    if (module == NULL)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "DC: %s: Could not find module %04x\n", dc->name, moduleId);
        return MPE_FS_ERROR_INVALID_DATA;
    }

    // If it's a compressed module,
    if (module->compressedModule && module->originalSize > dcPreDecompressSize)
    {
        retCode = dcNewCompressedModule(module, &compMod);
        if (retCode != MPE_SUCCESS)
        {
            // Couldn't allocate
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                    "DC: %s: Unable to create compressed module %04x\n",
                    dc->name, moduleId);
            mpe_dcReleaseModule(module);
            return retCode;
        }
        else
        {
            *retModule = (mpe_DcModule *) compMod;
        }
    }
    else
    {
        // Return a pointer to the module
        *retModule = module;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
            "DC: %s: Got module %04x.  Reference count is %d\n", dc->name,
            moduleId, module->referenceCount);

    return MPE_SUCCESS;
}

/**
 * Return a handle to a module within the carousel, based on the name.  Searches based on the names in
 * the descriptors.  This does not necessarily cause any data to be read from the carousel.
 * Note: If the carousel changes between invocations of this function, the N'th instance may change.
 *
 * @param dc        The data carousel to use.
 * @param name      The name of the module to search for.
 * @param instance  The instance (starting at 0) of modules which have this name.
 * @param module    [out] Pointer to where to put a handle to the module object.
 *
 * @returns MPE_SUCCESS if the module object is retrievedd, MPE_FS_ERROR_INVALID_DATA if the module doesn't exist
 */
mpe_Error mpe_dcGetModuleByName(mpe_DataCarousel *dc, const char *name,
        uint32_t instance, mpe_DcModule ** const retModule)
{
    mpe_Error retCode = MPE_FS_ERROR_INVALID_DATA; // Assume we didn't find it.
    mpe_DcModule *module = NULL;
    DcCompressedModule *compMod = NULL;
    uint32_t found = 0;
    uint32_t i;

    *retModule = NULL; // Error condition

    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(dcGlobalMutex);

    for (i = 0; i < dc->numModules; i++)
    {
        // If this module has a name, and it's name matches, check it out
        if (dc->modules[i]->label != NULL
                && strcmp(name, dc->modules[i]->label) == 0)
        {
            // If this is the N'th instance of this name, return this
            // module.
            if (found == instance)
            {
                module = dc->modules[i];
                break;
            }
            found++;
        }
    }

    if (module != NULL)
    {
        // Increase the refcount
        module->referenceCount++;

        // If it's a compressed module,
        if (module->compressedModule && module->originalSize
                > dcPreDecompressSize)
        {
            retCode = dcNewCompressedModule(module, &compMod);
            if (retCode != MPE_SUCCESS)
            {
                // Couldn't allocate
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                        "DC: %s: Unable to create compressed module %04x\n",
                        dc->name, module->moduleId);
                mpe_dcReleaseModule(module);
            }
            *retModule = (mpe_DcModule *) compMod;
        }
        else
        {
            *retModule = module;
        }
    }

    mpe_mutexRelease(dcGlobalMutex);
    // END CRITICAL SECTION

    return retCode;
}

/**
 * Release a module and decrement it's reference count.  If it's reference count
 * goes to zero, and the module is no longer current, allow it to be deallocated.
 *
 * @param module The module to deallocate.
 *
 * @return MPE_SUCCESS
 */
mpe_Error mpe_dcReleaseModule(mpe_DcModule *module)
{
    DcCompressedModule *compMod;
    mpe_Error retCode;

    // Make sure there's a module to free
    if (module == NULL)
    {
        return MPE_EINVAL;
    }

    // If we're a compressed module, release the enclosed module
    // and then free up the compressed module
    if (module->compressedProxy)
    {
        compMod = (DcCompressedModule *) module;
        retCode = mpe_dcReleaseModule(compMod->module);
        dcFreeCompressedModule(compMod);
        return retCode;
    }

    // Otherwise, just release the module
    if (module != NULL)
    {
        // BEGIN CRITICAL SECTION
        mpe_mutexAcquire(dcGlobalMutex);

        module->referenceCount--;

        // If we're at zero, let's get rid of any locked DDB's we've got
        if (module->referenceCount == 0 && module->lockedDDB != DC_INVALID_DDB)
        {
            MPE_LOG(MPE_LOG_TRACE8, MPE_MOD_FILESYS,
                    "DC: %s Unlocking module %04x[%d]\n", module->dc->name,
                    module->moduleId, module->lockedDDB);
            // Unlock the buffer
            (void) mpe_dccUnlockCacheObject(
                    module->ddbs[module->lockedDDB].sectionHandle);
            module->lockedDDB = DC_INVALID_DDB;
        }

        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                "DC: %s: Releasing module %04x.  Reference count now %d\n",
                module->dc->name, module->moduleId, module->referenceCount);

        mpe_mutexRelease(dcGlobalMutex);
        // END CRITICAL SECTION

        // If the module is unreferenced, and invalid, blow it away
        if (!module->valid && (module->referenceCount == 0))
        {
            (void) mpe_dcUnloadModule(module);
            dcFreeModule(module);
        }
    }
    return MPE_SUCCESS;
}

/**
 * This function unloads a module, and frees all the buffers within it.  Buffers (may) need
 * to be read in again from the carousel after this call.  This function can be called on
 * any module, even ones which have not been loaded, marking all the memory as eligible for
 * deallocation.
 *
 * @param module The module to unload.
 *
 * @returns MPE_SUCCESS if successful, error codes otherwise.
 */
mpe_Error mpe_dcUnloadModule(mpe_DcModule *module)
{
    uint32_t i;
    DcCompressedModule *compMod;

    // If it's a compressed module, load the underlying module
    if (module->compressedProxy)
    {
        compMod = (DcCompressedModule *) module;
        return mpe_dcUnloadModule(compMod->module);
    }

    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(dcGlobalMutex);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "DC: %s: Unloading module %04x\n",
            module->dc->name, module->moduleId);
    for (i = 0; i < module->numDDBs; i++)
    {
        // Free up any null sections
        if (module->ddbs[i].sectionHandle != NULL)
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                    "DC: %s: Unloading module %04x DDB: %d\n",
                    module->dc->name, module->moduleId, i);
            mpe_dccReleaseCacheObject(module->ddbs[i].sectionHandle);
            module->ddbs[i].sectionHandle = NULL; // It's gone, live with it.
            module->ddbs[i].dataOffset = 0; // Just in case.
            module->ddbs[i].prefetched = FALSE; // Not in the pipe, so must be false
        }
    }

    module->loaded = FALSE;

    mpe_mutexRelease(dcGlobalMutex);
    // END CRITICAL SECTION

    return MPE_SUCCESS;
}

/**
 * This function reads data from a module or linked set of modules.
 *
 * @param module The module to read the bytes from.
 * @param start The starting location at which we wish to read.
 * @param numBytes The number of bytes to read from this location
 * @param timeout Time, in Milliseconds, to wait for any particular DDB to be found.
 * @param bytes A pointer to a byte array to fill with the bytes read.  This array must be at least numBytes long.
 *
 * @return MPE_SUCCESSS if the function succeeded, an appropriate error code if the function failed.
 */
mpe_Error mpe_dcReadModuleBytes(mpe_DcModule *module, uint32_t start,
        uint32_t numBytes, uint32_t timeout, uint8_t *bytes,
        uint32_t *totalBytesRead)
{
    DcCompressedModule *compMod;
    mpe_Error retCode = MPE_SUCCESS;

    // Check to see if we're a compressed module.
    if (module->compressedProxy)
    {
        compMod = (DcCompressedModule *) module;
        return dcReadCompressedBytes(compMod, start, numBytes, bytes,
                totalBytesRead);
    }

    // If we're here, and we're a compressed module, getModule didn't allocate a compressed proxy, so we want to
    // use a predecompressed module
    if (module->compressedModule)
    {
        // Check the args.
        if (numBytes == 0 || start + numBytes > module->originalSize)
        {
            return MPE_EINVAL;
        }

        // If it doesn't yet exist, predecompress it now.
        if (module->decompressedData == NULL)
        {
            retCode = dcPreDecompressModule(module);
            if (retCode != MPE_SUCCESS)
            {
                MPE_LOG(
                        MPE_LOG_ERROR,
                        MPE_MOD_FILESYS,
                        "DC: %s: Module %04x:  Unable to predecompress module\n",
                        module->dc->name, module->moduleId);
                return retCode;
            }
        }

        MPE_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_FILESYS,
                "DC: %s: Module %04x: Copying %d predecompressed bytes at %d\n",
                module->dc->name, module->moduleId, numBytes, start);
        memcpy(bytes, &(module->decompressedData[start]), numBytes);
        *totalBytesRead = numBytes;

        return MPE_SUCCESS;
    }

    return dcReadRawModuleBytes(module, start, numBytes, timeout, bytes,
            totalBytesRead);
}

/**
 * Prefetch a module based on it's handle.
 *
 * @param module    The module to prefetch.
 *
 * @returns MPE_SUCCESS if the prefetch started (or the module is already prefetched).
 *          Error codes otherwise.
 */
mpe_Error mpe_dcPrefetchModule(mpe_DcModule *module)
{
    mpe_Error retCode = MPE_SUCCESS;
    mpe_FilterSource stream;
    mpe_FilterSpec *ddbFilter; // DDB Filter
    mpe_Bool done = false;
    DcCompressedModule *compMod;

    // If we're a proxy, prefetch the underlying module.
    if (module->compressedProxy)
    {
        compMod = (DcCompressedModule *) module;
        return mpe_dcPrefetchModule(compMod->module);
    }

    // If it's an OOB carousel, and we don't want to prefetch OOB, just return
    if (!dcPrefetchOOB && DC_IS_OOB(module->dc))
    {
        return MPE_SUCCESS;
    }

    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(dcGlobalMutex);

    if (!module->valid)
    {
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILESYS,
                "DC: %s: Unable to prefetch invalid module %04x\n",
                module->dc->name, module->moduleId);
        retCode = MPE_FS_ERROR_INVALID_STATE;
        done = true;
    }
    else if (module->prefetched)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                "DC: %s: Module %04x already prefetched\n", module->dc->name,
                module->moduleId);
        done = true;
    }
    else
    {
        // Note, we only increment the ref count by one.
        // This will be released when the last section is found.
        module->referenceCount++;
        module->prefetched = true;
    }
    mpe_mutexRelease(dcGlobalMutex);
    // END CRITICAL SECTION

    // Check the retcode from the section
    if (done)
    {
        return retCode;
    }

    // Set up for filtering on the appropriate place.
    stream.pid = module->pid;
    ocuSetStreamParams(&(module->dc->params), &stream);

    // Create the filter to get a DDB from the stream.
    retCode = ocuMakeDDBFilter(module->moduleId, false, 0,
            module->moduleVersion, module->downloadID, &ddbFilter);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "DC: %s: Could not make filter for all ddb's in Module %04x\n",
                module->dc->name, module->moduleId);
        mpe_dcReleaseModule(module);
        module->prefetched = false;
        return retCode;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
            "DC: %s: Prefetching module %04x, %d sections\n", module->dc->name,
            module->moduleId, module->numDDBs);

    // Actually start the request
    retCode = dcStartAsyncRequest(&stream, ddbFilter, MODULE_PREFETCH,
            module->dc, module, 0, dcGlobalEventQueue, dcCalculateTimeout(
                    module->ddbTimeout), module->numDDBs, NULL);

    // Check to make sure it went well.
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_FILESYS,
                "DC: %s: Unable to start async request for Module %04x Prefetch\n",
                module->dc->name, module->moduleId);
        mpe_filterDestroyFilterSpec(ddbFilter);
        mpe_dcReleaseModule(module);
        module->prefetched = false;
    }

    // Outta here
    return retCode;
}

/**
 * Prefetch a module (or modules) based on a name.
 *
 * @param module    The module to prefetch.
 * @param label     The name of the module to look for in the label descriptors.
 *
 * @returns MPE_SUCCESS if the prefetch started (or the module is already prefetched).
 *          MPE_NODATA if the name doesn't exist.
 */
mpe_Error mpe_dcPrefetchModuleByName(mpe_DataCarousel *dc, const char *label)
{
    mpe_Error retCode = MPE_ENODATA;
    uint32_t i;

    if (label == NULL || dc == NULL)
    {
        return MPE_EINVAL;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
            "DC: %s: Prefetching module by name %s\n", dc->name, label);

    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(dcGlobalMutex);
    for (i = 0; i < dc->numModules; i++)
    {
        if (dc->modules[i]->label != NULL && (strcmp(dc->modules[i]->label,
                label) == 0))
        {
            retCode = mpe_dcPrefetchModule(dc->modules[i]);
            if (retCode != MPE_SUCCESS)
            {
                break;
            }
        }
    }

    mpe_mutexRelease(dcGlobalMutex);
    // END CRITICAL SECTION

    return retCode;
}

/**
 * Is the carousel connected?
 *
 * @param dc            The data carousel to query on.
 * @param connected     [out] Pointer to where to make the connection.
 *
 * @return MPE_SUCCESS
 */
mpe_Error mpe_dcIsConnected(mpe_DataCarousel *dc, mpe_Bool *connected)
{
    if (dc == NULL || connected == NULL)
    {
        return MPE_EINVAL;
    }
    *connected = dc->connected;
    return MPE_SUCCESS;
}

/**
 * Is the carousel out of band (oob)?
 *
 * @param dc            The data carousel to query on.
 * @param connected     [out] Pointer to oob boolean.
 *
 * @return TRUE if the the data carousel is OOB.
 */
mpe_Bool dc_IsOOB(mpe_DataCarousel *dc)
{
    return ((dc == NULL) ? FALSE : DC_IS_OOB(dc));
}

/**
 * Return the size of a module.
 *
 * @param module    The module in question.
 * @param size      [out] Pointer to where to return the size.
 */
mpe_Error mpe_dcGetModuleSize(mpe_DcModule *mod, uint32_t *size)
{
    DcCompressedModule *compMod;

    if (mod == NULL || size == NULL)
    {
        return MPE_EINVAL;
    }
    if (mod->compressedProxy)
    {
        compMod = (DcCompressedModule *) mod;
        return mpe_dcGetModuleSize(compMod->module, size);
    }
    if (mod->compressedModule)
    {
        *size = mod->originalSize;
    }
    else
    {
        *size = mod->moduleSize;
    }
    return MPE_SUCCESS;
}

/**
 * Return the version of a module.
 *
 * @param module    The module in question.
 * @param version   [out] Pointer to where to return the size.
 */
mpe_Error mpe_dcGetModuleVersion(mpe_DcModule *mod, uint32_t *version)
{
    DcCompressedModule *compMod;

    if (mod == NULL || version == NULL)
    {
        return MPE_EINVAL;
    }

    if (mod->compressedProxy)
    {
        compMod = (DcCompressedModule *) mod;
        return mpe_dcGetModuleVersion(compMod->module, version);
    }
    *version = mod->moduleVersion;
    return MPE_SUCCESS;
}

/**
 * Return the loaded status of a module.
 *
 * @param module    The module in question.
 * @param isLoaded  [out] Pointer to where to return the loaded status.
 */
mpe_Error mpe_dcGetModuleLoadedStatus(mpe_DcModule *mod, mpe_Bool *isLoaded)
{
    DcCompressedModule *compMod;

    if (mod == NULL || isLoaded == NULL)
    {
        return MPE_EINVAL;
    }

    if (mod->compressedProxy)
    {
        compMod = (DcCompressedModule *) mod;
        return mpe_dcGetModuleLoadedStatus(compMod->module, isLoaded);
    }
    *isLoaded = mod->loaded;
    return MPE_SUCCESS;
}

/**
 * Register an event to be sent when the a module gets invalidated.
 *
 * @param module The module to watch.
 * @param queue  The event queue to send the event too.
 * @param data   The data to use to inform caller with.
 *
 * @return MPE_SUCCESS if the event is successfully registered, error codes otherwise.
 */
mpe_Error mpe_dcRegisterChangeNotification(mpe_DataCarousel *dc,
        mpe_EventQueue queue, void *data)
{
    if (dc == NULL)
    {
        return MPE_EINVAL;
    }

    MPE_LOG(MPE_LOG_INFO, MPE_MOD_FILESYS,
            "DC: %s: Setting version change notification\n",
            dc->name);

    // Make sure we have data if there's an event queue
    // TODO: Should this be an error?
    if (data == NULL)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                "DC: %s: Setting update event queue with NULL event.\n",
                dc->name);
    }

    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(dcGlobalMutex);

    dc->updateQueue = queue;
    dc->updateQueueRegistered = TRUE;
    dc->updateData = data;

    // If we're already monitored, set the timeout on the monitor to be infinite.
    if (dc->monitored)
    {
        MPE_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_FILESYS,
                "DC: %s: Setting timeout on DII Change Listener %x to 0 (was %u)\n",
                dc->name, dc->diiRequest->uniqueifier,
                (uint32_t) dc->diiRequest->timeout);

        // Set the DII timeout to forever (0).
        dc->diiRequest->timeout = 0;
        // No real need to hit the asynch thread.
    }
    else if (dcCheckVersion)
    {
        (void) dcSetUpdateWatcher(dc, 0);
    }

    mpe_mutexRelease(dcGlobalMutex);
    // END CRITICAL SECTION

    return MPE_SUCCESS;
}

/**
 * Unregister an previously registered notification queue
 *
 * @param module The module to watch.
 * @param queue  The event queue to unregister.  If this doesn't match the currently
 *               registered queue, no action is taken and an error is returned
 *
 * @return MPE_SUCCESS if the queue is successfully unregistered, error codes otherwise.
 */
mpe_Error mpe_dcUnregisterChangeNotification(mpe_DataCarousel *dc,
        mpe_EventQueue queue)
{
    mpe_TimeMillis timeoutTime;
    if (dc == NULL)
    {
        return MPE_EINVAL;
    }

    MPE_LOG(MPE_LOG_INFO, MPE_MOD_FILESYS,
            "DC: %s: Removing version change notification\n", dc->name);

    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(dcGlobalMutex);

    if (dc->updateQueue != queue)
    {
        mpe_mutexRelease(dcGlobalMutex);
        return MPE_EINVAL;
    }

    dc->updateQueueRegistered = FALSE;
    dc->updateData = NULL;

    // If we're removing the queue, we're unregistering for events.
    // Shutdown the
    if (dc->monitored)
    {
        timeoutTime = dcCalculateTimeout((DC_IS_OOB(dc) ? dcDIICheckDurationOOB
                : dcDIICheckDuration));
        MPE_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_FILESYS,
                "DC: %s: Setting timeout on DII Change Listener %x to %u (was %u)\n",
                dc->name, dc->diiRequest->uniqueifier, (uint32_t) timeoutTime,
                (uint32_t) dc->diiRequest->timeout);
        // Set the timeout on this request to be a regular duration
        dc->diiRequest->timeout = timeoutTime;
        // And let the async thread know
        mpe_eventQueueSend(dcGlobalEventQueue, DC_CHECK_TIMEOUT, 0, NULL, 0);
    }

    mpe_mutexRelease(dcGlobalMutex);
    // END CRITICAL SECTION

    return MPE_SUCCESS;
}

/**
 * Add a block of user data to the structure.
 *
 * @param module        The module to add the user data block to.
 * @param data          A pointer to the data block to add.
 * @param delFunc       A function pointer which will be called when this block
 *                      should be deleted.
 *
 * @return MPE_SUCCESS if added successfully, various error codes otherwise.
 */
mpe_Error mpe_dcAddModuleUserData(mpe_DcModule *module, void *data,
        void(*function)(void *))
{
    DcCompressedModule *compMod;

    // Make sure the args make sense.
    if ((module == NULL) || (data != NULL && function == NULL))
    {
        return MPE_EINVAL;
    }

    // If it's a compressed proxy module, go to the underlying module
    if (module->compressedProxy)
    {
        compMod = (DcCompressedModule *) module;
        return mpe_dcAddModuleUserData(compMod->module, data, function);
    }

    // Error if there's something there already.
    if (data != NULL && module->userData != NULL)
    {
        return MPE_EBUSY;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
            "DC: %s: Adding user data to module %04x: %p %p\n",
            module->dc->name, module->moduleId, data, function);

    module->userData = data;
    module->userDataDeleteFn = function;

    return MPE_SUCCESS;
}

/**
 * Return a block of memory which the user previously allocated.
 *
 * @param module        The module to get the block from.
 * @param data          [out] The data block.
 */
mpe_Error mpe_dcGetModuleUserData(mpe_DcModule *module, void **data)
{
    DcCompressedModule *compMod;

    // Make sure the args make sense.
    if (module == NULL || data == NULL)
    {
        return MPE_EINVAL;
    }
    if (module->compressedProxy)
    {
        compMod = (DcCompressedModule *) module;
        return mpe_dcGetModuleUserData(compMod->module, data);
    }
    *data = module->userData;
    return MPE_SUCCESS;
}

/**
 * Assign a new SI Handle to this data carousel.
 *
 * @param   dc          The data carousel.
 * @param   siHandle    The new SI Handle.
 */
mpe_Error dcSetSiHandle(mpe_DataCarousel *dc, mpe_SiServiceHandle siHandle)
{
    mpe_Error retCode;
    uint32_t tunerID = 0;
    uint32_t i;

    // Set the SI Handle
    dc->siHandle = siHandle;

    // Figure out the frequency that this carousel lives on.
    retCode = ocuSetTuningParams(siHandle, &(dc->params));
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_FILESYS,
                "DC: %s: Couldn't get frequency/program number for carousel%04x\n",
                dc->name, dc->associationTag);
        return retCode;
    }

    // Set the new PID's for the DII and modules
    retCode = ocuTranslateAssociationTag(siHandle, dc->associationTag,
            &(dc->pid));
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "DC: %s: Couldn't Translate association tag: %04x\n", dc->name,
                dc->associationTag);
        dc->pid = DC_INVALID_PID;
    }
    for (i = 0; i < dc->numModules; i++)
    {
        retCode = dcSetModulePid(dc->modules[i]);
        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                    "DC: %s: Couldn't Translate association tag: %04x\n",
                    dc->name, dc->modules[i]->assocTag);
            dc->modules[i]->pid = DC_INVALID_PID;
        }
    }

    // Figure out where this carousel resides
    // BUG: FIXME: TODO: DSG????
    if (DC_IS_IB(dc))
    {
        retCode = mpeos_mediaFrequencyToTuner(dc->params.t.ib.frequency,
                &tunerID);
        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                    "DC: %s: Could not find tuner with frequency %d\n",
                    dc->name, dc->params.t.ib.frequency);
            return retCode;
        }
    }

    dcReconnectDC(dc, tunerID);

    // And we're done.
    return MPE_SUCCESS;
}

/***********************************************************************************************
 * Internal (static) functions.
 ***********************************************************************************************/

/**
 * This function reads data from a module.  It performs all the necessary underlying operations, including
 * reading data in from the carousel (if necessary), and copying data out of multiple DDB's into a single
 * data block for output.  Since this operation can cause data to be read in from the carousel, it is potentially
 * very long running.  Blocking.
 *
 * @param module The module to read the bytes from.
 * @param start The starting location at which we wish to read.
 * @param numBytes The number of bytes to read from this location
 * @param timeout Time, in Milliseconds, to wait for any particular DDB to be found.
 * @param bytes A pointer to a byte array to fill with the bytes read.  This array must be at least numBytes long.
 *
 * @return MPE_SUCCESSS if the function succeeded, an appropriate error code if the function failed.
 */
static mpe_Error dcReadRawModuleBytes(mpe_DcModule *module, uint32_t start,
        uint32_t numBytes, uint32_t timeout, uint8_t *bytes,
        uint32_t *totalBytesRead)
{
    mpe_Error retCode = MPE_SUCCESS; // Return code, set to success by default.
    uint32_t firstDDB; // The first DDB we want to read
    uint32_t lastDDB; // The last DDB we'll want to read
    uint32_t startOffset;
    uint32_t destOffset = 0;
    uint32_t i;
    uint32_t copyBytes;
    uint32_t blockSize;
    mpe_DccCacheObject *ddbBuffer;
    mpe_EventQueue lQueue = 0;
    mpe_Bool lQueueCreated = FALSE;
    mpe_Event eventID;
    void *eventData;
    mpe_Bool firstTry;
    mpe_TimeMillis timeoutTime = 0;
    mpe_TimeMillis now;

    MPE_UNUSED_PARAM(timeout); /* TODO: implement use of the 'timeout' parameter */

    MPE_LOG(MPE_LOG_TRACE8, MPE_MOD_FILESYS,
            "DC: %s Reading module %04x Bytes: %d offset %d\n",
            module->dc->name, module->moduleId, numBytes, start);

    // Error condition setup
    *totalBytesRead = 0;

    // If the end is past the number of bytes, return error
    // Note, if start > module->size, start + numBytes > module->size, by definition
    // Or if numBytes == 0.  Gotta read something, right?
    if (numBytes == 0 || start + numBytes > module->moduleSize)
    {
        return MPE_EINVAL;
    }

    // Compute the first DDB and last that we'll be caring about
    // Do it like this, just in case we later want to
    blockSize = module->blockSize;
    firstDDB = start / blockSize;
    lastDDB = (start + numBytes - 1) / blockSize;
    startOffset = start % blockSize;

    /*
     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "DC: %s Reading %d DDB's, %d to %d\n", module->dc->name, (lastDDB - firstDDB) + 1, firstDDB, lastDDB);

     for (i = firstDDB; i <= lastDDB; i++)
     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "DC: %s: Module %04x[%d]: %08x\n", module->dc->name, module->moduleId, i, module->ddbs[i].sectionHandle);
     */

    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(dcGlobalMutex);

    // Walk the DDBs in the module
    for (i = firstDDB; i <= lastDDB; i++)
    {
        // Set that this is the first time attempting to read this block.
        firstTry = true;

        RetryRead:

        // If the DDB isn't here, go get it.
        if (!(mpe_dccBlockPresent(module->ddbs[i].sectionHandle)))
        {
            // Release the mutex, we'll let this routine proceed on it's
            // own.  It will get the mutex to put the module in.
            mpe_mutexRelease(dcGlobalMutex);

            // If we're the first time around, figure out what the total
            // timeout should be.
            if (firstTry)
            {
                timeoutTime = dcCalculateTimeout(module->ddbTimeout);
                firstTry = false; // no longer the first time around.
            }

            // Get an event queue
            if (!lQueueCreated)
            {
                retCode = mpe_eventQueueNew(&lQueue, "MpeDcLQueue");
                if (retCode != MPE_SUCCESS)
                {
                    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                            "DC: %s: Unable to allocate event queue\n",
                            module->dc->name);
                    return retCode;
                }
                lQueueCreated = TRUE;
            }

            // Demand fetch the DDB
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                    "DC: %s: Demand Fetching Module %04x[%d]\n",
                    module->dc->name, module->moduleId, i);
            retCode = dcFetchDDB(module, i, DDB_DEMANDFETCH, lQueue,
                    timeoutTime);
            // Make sure we were successful
            if (retCode != MPE_SUCCESS)
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                        "DC: %s: Fetch of module %04x[%d] failed: %d\n",
                        module->dc->name, module->moduleId, i, retCode);
                mpe_eventQueueDelete(lQueue);
                lQueueCreated = FALSE;
                goto CleanUp;
            }

            // Check to prefetch ahead.
            // First, check to see if we've got the full prefetch on.
            if (dcPrefetchModules && !(module->prefetched))
            {
                (void) mpe_dcPrefetchModule(module);
            }
            else if (i < (module->numDDBs - 1))
            {
                dcStartPrefetch(module, i + 1);
            }

            // Wait for the event
            retCode = mpe_eventQueueWaitNext(lQueue, &eventID, &eventData,
                    NULL, NULL, 0);
            // How do we cleanup?
            dcHandleSectionEvent(eventID, (uint32_t) eventData);

            // Fix for bug #1381 (added on 9/16/05)
            // If a filter is explicitly cancelled, it can only
            // mean that PMT has changed and the data carousel itself
            // has initiated the cancel. In this case just go back to
            // top and get ready to receive events for newly set-up
            // requests
            if (eventID == MPE_SF_EVENT_FILTER_CANCELLED)
            {
                // FIXME: There should be a better way of handling this.
                // We should probably note the actual cause in the async request
                // record, and get that from up above.
                mpe_timeGetMillis(&now);
                if (timeoutTime > now)
                {
                    // Not yet.  This implies that we were cancelled for
                    // some other reason.  At present, this can only be a
                    // SI change.  Start the read again on the new
                    // SI version
                    MPE_LOG(
                            MPE_LOG_DEBUG,
                            MPE_MOD_FILESYS,
                            "DC: %s: Restarting read of %04x[%d] due to SI change\n",
                            module->dc->name, module->moduleId, i);
                    // HACK: WARNING: DANGER:
                    // We are about to branch from this location, which is outside the critical section
                    // to the point where we restart the read, which is inside the critical
                    // section.
                    // Reaquire the mutex to restart the critical section
                    mpe_mutexAcquire(dcGlobalMutex);
                    // END HACK:
                    goto RetryRead;
                }
                MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILESYS,
                        "DC: %s: Timing out read of %04x[%d]\n",
                        module->dc->name, module->moduleId, i);
                retCode = MPE_FS_ERROR_INVALID_STATE;
                goto CleanUp;
            }

            // Get the lock back again
            // We'll leave the temporary non-critical section
            mpe_mutexAcquire(dcGlobalMutex);
        }

        // Either copy from this location until the end of the section, or the number
        // of bytes we still want, whichever is less
        copyBytes = MIN(blockSize - startOffset, numBytes);

        // Lock the buffer in memory.  If it's not here, go back and get it
        // again.  Assumes mpe_memLockH returns an error code if sectionHandle is NULL
        if (module->lockedDDB == i)
        {
            MPE_LOG(MPE_LOG_TRACE8, MPE_MOD_FILESYS,
                    "DC: %s: Using previously locked module %08x[%d]\n",
                    module->dc->name, module->moduleId, i);
            ddbBuffer = module->ddbs[i].sectionHandle;
        }
        else
        {
            if (module->lockedDDB != DC_INVALID_DDB)
            {
                MPE_LOG(MPE_LOG_TRACE8, MPE_MOD_FILESYS,
                        "DC: %s: Unlocking module %04x[%d]\n",
                        module->dc->name, module->moduleId, module->lockedDDB);
                // Unlock the buffer
                (void) mpe_dccUnlockCacheObject(
                        module->ddbs[module->lockedDDB].sectionHandle);
            }
            MPE_LOG(MPE_LOG_TRACE8, MPE_MOD_FILESYS,
                    "DC: %s: Locking module %04x[%d]\n", module->dc->name,
                    module->moduleId, i);
            retCode = mpe_dccLockCacheObject(module->ddbs[i].sectionHandle);

            // Note that we've locked this.
            module->lockedDDB = i;
            ddbBuffer = module->ddbs[i].sectionHandle;
        }
        // Section should now be locked.  If not, need to redo.
        if (retCode != MPE_SUCCESS || ddbBuffer == NULL)
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                    "DC: %s: Module %04x[%d] Purged.  Refetching\n",
                    module->dc->name, module->moduleId, i);
            mpe_dccReleaseCacheObject(module->ddbs[i].sectionHandle);
            module->ddbs[i].sectionHandle = NULL;

            // Note that nothing is currently locked
            module->lockedDDB = DC_INVALID_DDB;

            // Try the read again
            goto RetryRead;
        }

        // Debugging message
        MPE_LOG(MPE_LOG_TRACE8, MPE_MOD_FILESYS,
                "DC: %s: Reading %d bytes from DDB %d at offset %d (%d)\n",
                module->dc->name, copyBytes, i, startOffset, startOffset
                        + module->ddbs[i].dataOffset);

        // Copy the bytes to the buffer.
        // readFromSection returns number of bytes read, not an error code.
        // memcpy(&bytes[destOffset], &ddbBuffer[startOffset + module->ddbs[i].dataOffset], copyBytes);
        retCode = mpe_dccReadCacheObject(ddbBuffer, startOffset
                + module->ddbs[i].dataOffset, copyBytes, &bytes[destOffset]);
        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_FILESYS,
                    "DC: %s: Error reading data in Module %04x[%d]: Error code: %x\n",
                    module->dc->name, module->moduleId, i, retCode);
            mpe_mutexRelease(dcGlobalMutex);
            goto CleanUp;
        }

        // Indicate that we read these bytes to the caller
        *totalBytesRead += copyBytes;

        // Figure out where to start copying the next block from.
        destOffset += copyBytes;
        startOffset = 0; // Next block starts at the start of the next buffer

        numBytes -= copyBytes; // Bytes left to copy
    }

    mpe_mutexRelease(dcGlobalMutex);
    // END CRITICAL SECTION

    CleanUp:
    // Free up the event queue
    if (lQueueCreated)
    {
        mpe_eventQueueDelete(lQueue);
    }

    return retCode;
}

/**
 * Insert a DDB into a module.  Allocates up the handle structure to
 * hold the module.
 */
static mpe_Error dcInsertDDB(mpe_DcModule *module,
        mpe_FilterSectionHandle section)
{
    mpe_Error retCode;
    mpe_DccCacheObject *ddbHandle;
    uint32_t dataOffset;
    uint32_t ddb;

    // Make sure the section makes sense
    retCode = mpe_ocpParseDDBHeader(section, &ddb, &dataOffset);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "DC: %s: Unable to parse DDB section for module %04x\n",
                module->dc->name, module->moduleId);
        mpe_filterSectionRelease(section);
        return retCode;
    }
    else
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                "DC: %s: Got DDB %d in module %04x: Data offset: %d\n",
                module->dc->name, ddb, module->moduleId, dataOffset);
    }

    // Shortcut the rest of this before creating stuff and entering the critical section
    // TODO: Is this correct?  What happens if this changes before the actual read
    // occurs?  Worst case is we skip this check, but that's expensive.
    if (mpe_dccBlockPresent(module->ddbs[ddb].sectionHandle))
    {
        MPE_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_FILESYS,
                "DC: %s: Module %04x DDB %d is already loaded.  Disposing additional copy\n",
                module->dc->name, module->moduleId, ddb);
        mpe_filterSectionRelease(section);
        return MPE_SUCCESS;
    }

    // Create the cache object
    retCode = mpe_dccNewCacheObject(section, &ddbHandle);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_FILESYS,
                "DC: %s: Could not create Cache Object for Module %04x[%d]: Error code: %04x\n",
                module->dc->name, module->moduleId, ddb, retCode);
        // Release the section.
        // Normally this will be handled by the cache on success, but
        // not here.
        mpe_filterSectionRelease(section);
        return retCode;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "DC: %s: Inserting DDB %04x[%d]\n",
            module->dc->name, module->moduleId, ddb);

    // BEGIN CRITICAL SECTION
    // Lock the module
    mpe_mutexAcquire(dcGlobalMutex);

    // Make sure the section isn't there
    if (!mpe_dccBlockPresent(module->ddbs[ddb].sectionHandle))
    {
        // If the section handle isn't NULL, and we're here, the block must have been flushed.
        // Toss out the old version
        if (module->ddbs[ddb].sectionHandle != NULL)
        {
            mpe_dccReleaseCacheObject(module->ddbs[ddb].sectionHandle);
        }
        module->ddbs[ddb].sectionHandle = ddbHandle;
        module->ddbs[ddb].dataOffset = dataOffset;
    }
    else
    {
        MPE_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_FILESYS,
                "DC: %s: Module %04x[%d] is already present.  Disposing additional copy\n",
                module->dc->name, module->moduleId, ddb);
        // Release the cache object, which will release the section as well.
        // No need to release it explicitly.
        mpe_dccReleaseCacheObject(ddbHandle);
    }

    mpe_mutexRelease(dcGlobalMutex);
    // END CRITICAL SECTION

    return retCode;
}

/**
 * Start a prefetch of a DDB from the stream.
 */
static mpe_Error dcFetchDDB(mpe_DcModule *module, uint32_t ddb,
        DcRequestType reqType, mpe_EventQueue queue, mpe_TimeMillis timeoutTime)
{
    mpe_Error retCode = MPE_SUCCESS;
    mpe_FilterSource stream;
    mpe_FilterSpec *ddbFilter; // DDB Filter

    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(dcGlobalMutex);

    if (!module->valid)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "DC: %s Unable to load DDB %d from invalid module %04x\n",
                module->dc->name, ddb, module->moduleId);
        retCode = MPE_FS_ERROR_INVALID_STATE;
    }
    else
    {
        // Increment the reference count (essentially a mpe_dcGetModule() call without the overhead
        // so that the module doesn't get deleted until the prefetch is complete.
        // I don't worry about this module disappearing before here, as the surrounding call
        // should be holding the module as it got it via a mpe_dcGetModule() call.  The reference
        // count should ALWAYS be above zero here.
        // This just makes sure that the module doesn't get deleted before the prefetch completes
        module->referenceCount++;

        // Mark that we're going.
        module->ddbs[ddb].prefetched = TRUE;
    }

    mpe_mutexRelease(dcGlobalMutex);
    // END CRITICAL SECTION

    if (retCode != MPE_SUCCESS)
    {
        return retCode;
    }

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_FILESYS,
            "DC: %s: %sing DDB %04x[%d] Reference Count: %d Outstanding Prefetches: %d\n",
            module->dc->name, DC_REQUEST_TYPE_NAME(reqType), module->moduleId,
            ddb, module->referenceCount, dcOutstandingPrefetches);

    // Set up for filtering on the appropriate location
    stream.pid = module->pid;
    ocuSetStreamParams(&(module->dc->params), &stream);

    // Create the filter to get a DDB from the stream.
    retCode = ocuMakeDDBFilter(module->moduleId, true, ddb,
            module->moduleVersion, module->downloadID, &ddbFilter);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "DC: %s: Could not make filter for Module %04x DDB %d\n",
                module->dc->name, module->moduleId, ddb);
        mpe_dcReleaseModule(module);
        return retCode;
    }

    // Actually start the request
    retCode = dcStartAsyncRequest(&stream, ddbFilter, reqType, module->dc,
            module, ddb, queue, timeoutTime, 1, NULL);

    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_FILESYS,
                "DC: %s Unable to start Async request %08x for DDB Prefetch %04x[%d]\n",
                module->dc->name, module->moduleId, ddb, ddb);
        mpe_filterDestroyFilterSpec(ddbFilter);
        mpe_dcReleaseModule(module);
    }

    // Outta here
    return retCode;
}

/**
 * Check to make sure that a module is current.
 * If the DII is currently being monitored, or the module is marked with a static caching
 * level, it's good to go.
 * If the module is transparent or semitransparently cached, we need to have checked for a DII
 * within the last .5 or 30 seconds, respectively.
 *
 * @param   module      Pointer to the module to be checked.  Will point to the correct
 *                      target module if the module is updated.
 * @param   current     [out]  If the event is current or not.
 */
mpe_Error mpe_dcCheckModuleCurrent(mpe_DcModule *module, mpe_Bool *out)
{
    mpe_TimeMillis now;
    mpe_TimeMillis then;
    mpe_Error retCode;
    mpe_DataCarousel *dc;
    DcCompressedModule *compMod;

    // If we're a compressed module, check the underyling module.
    if (module->compressedProxy)
    {
        compMod = (DcCompressedModule *) module;
        return mpe_dcCheckModuleCurrent(compMod->module, out);
    }

    // Fastest.  Check to make sure module isn't marked invalid.
    if (!module->valid)
    {
        *out = false;
        return MPE_SUCCESS;
    }
    // Grab the carousel, just for good measure.
    dc = module->dc;
    // If we're actively monitoring, or the caching level is static, we're good.  No need to check
    if (dc->monitored || (module->cachingTransparency
            == OCP_CACHING_LEVEL_STATIC))
    {
        // If DII update request, and it has a timeout, update that timeout to stretch further out.
        if (dc->diiRequest->timeout != 0)
        {
            then = dcCalculateTimeout((DC_IS_OOB(dc) ? dcDIICheckDurationOOB
                    : dcDIICheckDuration));
            // MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "DC: %s: Moving DII change request %x timeout to %u (was %u)\n",
            //        dc->name, dc->diiRequest->uniqueifier, (uint32_t) then, (uint32_t) dc->diiRequest->timeout);
            dc->diiRequest->timeout = then;
            // Note, we don't tickle the other queue.  Reason being it will actually
            // already be waiting to kill this guy, and will automatically look for a new update
            // time when it rolls around.
        }
        *out = true;
        return MPE_SUCCESS;
    }
    // Figure out when the earliest we should have checked is.
    mpe_timeGetMillis(&now);
    switch (module->cachingTransparency)
    {
    case OCP_CACHING_LEVEL_TRANSPARENT:
        then = now - DC_TRANSPARENT_CACHING_INTERVAL;
        break;
    case OCP_CACHING_LEVEL_SEMITRANSPARENT:
        then = now - DC_SEMITRANSPARENT_CACHING_INTERVAL;
        break;
    case OCP_CACHING_LEVEL_STATIC:
    default:
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "DC: %s: Shouldn't be here in check module current\n", module->dc->name);
        return MPE_EINVAL;
    }

    // Was the DII checked more recently?
    if (dc->lastDiiCheck > then)
    {
        *out = true;
        return MPE_SUCCESS;
    }

    // If we're not actively monitoring, start
    if (!dc->monitored && dcCheckVersion)
    {
        (void) dcSetUpdateWatcher(dc, (DC_IS_OOB(dc) ? dcDIICheckDurationOOB
                : dcDIICheckDuration));
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
            "DC: %s: Checking %s DII version on tuner %d\n", dc->name,
            DC_TRANSPORT_NAME(dc), (DC_IS_IB(dc) ? dc->params.t.ib.tunerId : 0));

    retCode = dcLoadDII(dc, dc->diiTransactionID);
    if (retCode != MPE_SUCCESS)
    {
        return retCode;
    }
    *out = module->valid;
    return MPE_SUCCESS;
}

/**
 * Look ahead in the carousel, and start prefetches of blocks that aren't in.
 * Implements a *VERY* simple algorithm, looks ahead up to DC_PREFETCH_DISTANCE
 * DDB's, and will start up to DC_PREFETCH_START requests.
 */
static
void dcStartPrefetch(mpe_DcModule *module, uint32_t firstDDB)
{
    uint32_t i;
    uint32_t limitDDB = MIN(module->numDDBs, firstDDB + dcPrefetchDistance);
    uint32_t numPfs = 0;

    // Make sure we're still working with this module
    if (!(module->dc->mounted) || !(module->valid))
    {
        return;
    }
    // Just return if OOB Prefetching is disabled
    if (!dcPrefetchOOB && DC_IS_OOB(module->dc))
    {
        return;
    }

    // Start looking ahead for modules to prefetch.
    // Only prefetch up to DC_PREFETCH_START modules
    // and only within the DC_PREFETCH_DISTANCE
    // and no more than DC_MAX_PREFETCHES total
    // Note, the check on MAX_PREFETCHES is not checked inside a mutex.  Technically, we can climb above
    // the max, but this will generally limit it.
    // dcOutstandingPrefetches get's incemented in the startAsync routine, so that we can limit
    // the number of mutex acquire's we must do.
    for (i = firstDDB; (i < limitDDB) && (numPfs < dcPrefetchStart)
            && (dcOutstandingPrefetches < dcMaxPrefetch); i++)
    {
        if (!module->ddbs[i].prefetched && !(mpe_dccBlockPresent(
                module->ddbs[i].sectionHandle)))
        {
            numPfs++;
            (void) dcFetchDDB(module, i, DDB_PREFETCH, dcGlobalEventQueue,
                    dcCalculateTimeout(module->ddbTimeout));
        }
    }
}

/**
 * Create a new module based on the data in the DII.
 *
 * @param dc        The data carousel containing the module.
 * @param dii       The DII describing the module.
 * @param modNum    The module within the carousel.
 * @param retModule [out] Where the module will be returned.
 *
 * @returns MPE_SUCCESS if the module is correctly created,
 */
static mpe_Error dcNewModule(mpe_DataCarousel *dc, mpe_OcpDII *dii,
        uint32_t modNum, mpe_DcModule **retModule)
{
    uint32_t i;
    mpe_DcModule *module;
    mpe_Error retCode;

    // Just in case.
    *retModule = NULL;

    if (modNum > dii->numModules)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_FILESYS,
                "DC: %s: Attempting to add module %d to DC with only %d modules\n",
                dc->name, modNum, dii->numModules);
        return MPE_EINVAL;
    }

    // Allocate up the module structure
    retCode = mpeos_memAllocP(MPE_MEM_FILE_CAROUSEL, sizeof(mpe_DcModule),
            (void **) &module);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "DC: %s: Unable to allocate module structure for module %d\n",
                dc->name, modNum);
        return retCode;
    }

    // Set a pointer back to the data carousel.  Allows us to to work
    // with the module without having to pass a DC pointer around.
    module->dc = dc;

    // Clear out the DDB pointer, in case of error
    module->ddbs = NULL;

    // Make sure it's a real module, not a proxy
    module->compressedProxy = false;

    // Mark the module as valid.
    module->valid = TRUE;

    // Other useful information
    module->linkedModule = false;
    module->compressedModule = false;
    module->loaded = false;
    module->prefetched = false;

    // No user data.
    module->userData = NULL;
    module->userDataDeleteFn = NULL;

    // No uncompressed data
    module->decompressedData = NULL;

    // Default descriptor values
    module->cachingTransparency = OCP_CACHING_LEVEL_TRANSPARENT;
    module->label = NULL;

    // Copy stuff from the dii to the mpe_DcModuleInfo structure.
    module->moduleId = dii->modules[modNum].moduleID;
    module->moduleSize = dii->modules[modNum].moduleSize;
    module->moduleVersion = dii->modules[modNum].moduleVersion;
    module->ddbTimeout = dii->modules[modNum].blockTimeout;
    module->assocTag = dii->modules[modNum].associationTag;

    // Copy from the DII global area
    module->downloadID = dii->downloadID;
    module->blockSize = dii->blockSize;

    // Make sure the DDB Timeout is reasonable
    if (module->ddbTimeout == DC_USE_DEFAULT_TIMEOUT)
    {
        module->ddbTimeout = dcDefaultTimeout;
    }

    // No locked buffers.
    module->lockedDDB = DC_INVALID_DDB;

    // Translate the PID.
    retCode = dcSetModulePid(module);
    if (retCode != MPE_SUCCESS)
    {
        goto CleanUp;
    }

    // Grab the descriptor information.
    for (i = 0; i < dii->modules[modNum].numDescriptors; i++)
    {
        switch (dii->modules[modNum].descriptors[i].descType)
        {
        case OCP_MODULE_LINK_DESCRIPTOR_TAG:
            module->linkedModule = true;
            module->position
                    = dii->modules[modNum].descriptors[i].desc.moduleLink.position;
            module->nextModuleId
                    = dii->modules[modNum].descriptors[i].desc.moduleLink.nextModule;
            break;
        case OCP_CACHING_PRIORITY_DESCRIPTOR_TAG:
            module->cachingPriority
                    = dii->modules[modNum].descriptors[i].desc.cachingPriority.priorityValue;
            module->cachingTransparency
                    = dii->modules[modNum].descriptors[i].desc.cachingPriority.transparencyLevel;
            break;
        case OCP_COMPRESSED_MODULE_DESCRIPTOR_TAG:
            switch ((dii->modules[modNum].descriptors[i].desc.compressedModule.compressionMethod)
                    & OCP_COMPRESSION_MASK)
            {
            case OCP_COMPRESSION_ZLIB:
                module->compressedModule = true;
                break;
            case OCP_COMPRESSION_NONE:
                module->compressedModule = false;
                break;
            default:
                MPE_LOG(
                        MPE_LOG_WARN,
                        MPE_MOD_FILESYS,
                        "DC: %s: Module %04x: Compression method %02x not supported\n",
                        dc->name,
                        module->moduleId,
                        dii->modules[modNum].descriptors[i].desc.compressedModule.compressionMethod);
                retCode = MPE_FS_ERROR_INVALID_DATA;
                goto CleanUp;
            }
            module->originalSize
                    = dii->modules[modNum].descriptors[i].desc.compressedModule.originalSize;
            break;
        case OCP_LABEL_DESCRIPTOR_TAG:
            // Ok, we do a little magic here.
            // We grab the descriptor directly out of the descriptor, and set the label in the descriptor to NULL.
            // When we free the DII, it won't free this block.
            module->label
                    = dii->modules[modNum].descriptors[i].desc.label.label;
            dii->modules[modNum].descriptors[i].desc.label.label = NULL;

            // This is the alternate code, but would do another memory allocation and copy.
            // retCode = mpeos_memAllocP(MPE_MEM_FILE_CAROUSEL, (strlen(dii->modules[modNum].descriptors[i].desc.label.label) + 1) * sizeof(char),
            //                        (void **)&module->label);
            // if (retCode == MPE_SUCCESS)
            // {
            //     strcpy(module->label, dii->modules[modNum].descriptors[i].desc.label.label);
            // }
            break;
        default:
            // Ignore it.
            break;
        }
    }

    // Compute the number of DDB's we'll need.
    // standard trick of (n + size - 1) / size == needed
    module->numDDBs = (dii->modules[modNum].moduleSize + dii->blockSize - 1)
            / dii->blockSize;

    // Create an array of DDB pointers for each module.
    retCode = mpeos_memAllocP(MPE_MEM_FILE_CAROUSEL, module->numDDBs
            * sizeof(mpe_DcDDB), (void **) &module->ddbs);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "DC: %s: Unable to allocate %d DDBs for module %d\n", dc->name,
                module->numDDBs, modNum);
        goto CleanUp;
    }

    // Clear out the section indicators
    for (i = 0; i < module->numDDBs; i++)
    {
        module->ddbs[i].sectionHandle = NULL;
        module->ddbs[i].prefetched = FALSE;
    }

    // No references
    module->referenceCount = 0;

    // If the total size of the carousel is less than the threshold for
    // prefetching carousels, prefetch this module
    // Or, if this is smaller than the module prefetch size for very large carousels.
    if (dc->totalSize < dcPrefetchMountSize || module->moduleSize
            < dcPrefetchModuleSize)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                "DC: %s: Prefetching New Module %04x\n", dc->name,
                module->moduleId);
        (void) mpe_dcPrefetchModule(module);
    }

    // And we're outta here.
    *retModule = module;
    return MPE_SUCCESS;

    CleanUp: dcFreeModule(module);
    return retCode;
}

/**
 * Walk the DII record and add the modules to the module list.
 * This does not check for any duplicates, merely assumes that each
 * list can be appended to one another.
 */
static mpe_Error dcUpdateModules(mpe_DataCarousel *dc, mpe_OcpDII *dii)
{
    mpe_Error retCode = MPE_SUCCESS;
    mpe_DcModule **modules;
    mpe_DcModule *module = NULL;
    mpe_DcModule **oldModules;
    uint32_t oldNumModules;
    uint32_t i;
    uint32_t diiSize = 0;
    uint32_t diiDDBs = 0;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
            "DC: %s: Allocating space for %d modules\n", dc->name,
            dii->numModules);
    retCode = mpeos_memAllocP(MPE_MEM_FILE_CAROUSEL, dii->numModules
            * sizeof(mpe_DcModule *), (void **) &(modules));
    // Make sure we got the space.
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "DC: %s: Unable to allocate array for %d modules", dc->name,
                dii->numModules);
        return retCode;
    }

    // Calculate the new size of the DII
    for (i = 0; i < dii->numModules; i++)
    {
        diiSize += dii->modules[i].moduleSize;
    }
    dc->totalSize = diiSize;

    // For each module, create module object
    for (i = 0; i < dii->numModules; i++)
    {
        module = NULL; // Make sure we don't use the same module twice.

        // Attempt to find this module in the old list
        if (dc->modules != NULL)
        {
            module = dcFindModule(dc, dii->modules[i].moduleID);
            // If we found the old module, check it's version
            if (module != NULL)
            {
                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                        "DC: %s: Found previous module %04x, Version %d\n",
                        dc->name, module->moduleId, module->moduleVersion);
            }
        }
        // No module to use already, create a new one
        if (module == NULL || module->moduleVersion
                != dii->modules[i].moduleVersion)
        {
            retCode = dcNewModule(dc, dii, i, &(module));
            if (retCode != MPE_SUCCESS)
            {
                MPE_LOG(
                        MPE_LOG_ERROR,
                        MPE_MOD_FILESYS,
                        "DC: %s: Unable to create module structure for module %d (%04x)\n",
                        dc->name, i, dii->modules[i].moduleID);
                // FIXME: BUG: TODO: Free up the module structures that we've allocated here too.......
                // How, exactly, do I do that?
                mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, modules);
                return retCode;
            }
        }

        // Insert it into the new list
        modules[i] = module;
        MPE_LOG(
                MPE_LOG_INFO,
                MPE_MOD_FILESYS,
                "DC: %s: Module info %d: ID: %04x Size: %6d (%6d) Version: %d BlockSize: %d NumDDBs: %d Timeout: %d %s %s %s\n",
                dc->name, i, module->moduleId, module->moduleSize,
                (module->compressedModule ? module->originalSize
                        : module->moduleSize), module->moduleVersion,
                module->blockSize, module->numDDBs, ((module->ddbTimeout
                        == DC_USE_DEFAULT_TIMEOUT) ? module->ddbTimeout
                        : dcDefaultTimeout), ((module->ddbTimeout
                        == DC_USE_DEFAULT_TIMEOUT) ? "(default)" : ""),
                (module->compressedModule ? "Compressed" : ""), ((module->label
                        != NULL) ? module->label : ""));
        // Track the total DDBs
        diiDDBs += modules[i]->numDDBs;
    }
    MPE_LOG(MPE_LOG_INFO, MPE_MOD_FILESYS,
            "DC: %s: Total size: %d Modules: %d DDBs: %d\n",
            dc->name, dc->totalSize, dii->numModules, diiDDBs);

    // Copy the old data away
    oldModules = dc->modules;
    oldNumModules = dc->numModules;

    // Copy over the new data into the DC
    dc->numModules = dii->numModules;
    dc->modules = modules;

    // Now, walk the Old list of modules, checking for ones which are not in the new list
    // If an element is in the old list, but not the new list, it must be invalidated.
    // Note, if an element is in the new list, either it
    for (i = 0; i < oldNumModules; i++)
    {
        if (oldModules[i] != NULL && oldModules[i]->valid && dcFindModule(dc,
                oldModules[i]->moduleId) != oldModules[i])
        {
            // In the old list, but not the new.
            // Invalidate the sucker.
            MPE_LOG(
                    MPE_LOG_DEBUG,
                    MPE_MOD_FILESYS,
                    "DC: %s: Old module %04x is not in new list.  Invalidating version %d\n",
                    dc->name, oldModules[i]->moduleId,
                    oldModules[i]->moduleVersion);
            dcInvalidateModule(oldModules[i]);
        }
    }
    // Now, all the modules should have been either invalidated, or copied.
    // Blow away the old array.
    mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, oldModules);

    return retCode;
}

/**
 * Find and a parse a DII in a data carousel, setting up the module list as we do.
 */
static mpe_Error dcLoadDII(mpe_DataCarousel *dc, uint32_t transactionID)
{
    mpe_Error retCode = MPE_SUCCESS;
    mpe_FilterSectionHandle diiSection;
    mpe_FilterSpec *diiFilter;
    mpe_OcpDII *dii = NULL;
    mpe_FilterSource stream;

    // Get the pid and other good info
    stream.pid = dc->pid;

    // set the params for loading the DII
    ocuSetStreamParams(&(dc->params), &stream);

    // Get the section from the stream.
    retCode = ocuMakeDIIFilter(transactionID,
#ifdef PTV
            DC_IS_OOB(dc),
            dc->carouselID,
#endif
            &diiFilter);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "DC: %s: Could not create DII filter\n", dc->name);
        return retCode;
    }
    retCode = ocuGetSection(&stream, diiFilter, dc->diiTimeOut, &diiSection);
    mpe_filterDestroyFilterSpec(diiFilter);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS, "DC: %s Could not get DII\n",
                dc->name);
        return retCode;
    }

    // Parse the DII here.
    retCode = mpe_ocpParseDII(diiSection, &dii);
    if (retCode == MPE_SUCCESS)
    {
        // add the modules from this DII to the list of modules in the carousel.
        retCode = dcUpdateModules(dc, dii);
        if (retCode == MPE_SUCCESS)
        {
            // Indicate that we checked the DII
            mpe_timeGetMillis(&(dc->lastDiiCheck));

            // Grab the version stuffs
            dc->diiVersion = dii->header.version;
            dc->diiTransactionID = dii->header.transactionID;
        }
        else
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                    "DC: %s: Parsing DII Failed\n", dc->name);
        }
    }
    else
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS, "DC: %s: Parsing DII Failed\n",
                dc->name);
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
            "DC: %s: Done parsing DII -- cleaning up\n", dc->name);
    // clean up
    mpe_filterSectionRelease(diiSection);
    mpe_ocpFreeDII(dii);

    return retCode;
}

/**
 * Free a module.  Deallocate all the DDB's, and then blow away the arrays
 * and the structure.  Assumes everybody else is done with this module
 */
static
void dcFreeModule(mpe_DcModule *module)
{
    if (module != NULL)
    {
        if (module->referenceCount > 0)
        {
            MPE_LOG(
                    MPE_LOG_WARN,
                    MPE_MOD_FILESYS,
                    "DC: %s: Freeing module %04x with non-zero reference count: %d\n",
                    module->dc->name, module->moduleId, module->referenceCount);
        }
        // Free up any nested structures.
        if (module->ddbs != NULL)
        {
            mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, module->ddbs);
        }
        if (module->label != NULL)
        {
            mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, module->label);
        }
        if (module->decompressedData != NULL)
        {
            mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, module->decompressedData);
        }
        // If there's a user data structure, call back the user
        if (module->userData != NULL)
        {
            MPE_LOG(
                    MPE_LOG_DEBUG,
                    MPE_MOD_FILESYS,
                    "DC: %s: Calling User memory deletion callback for module %04x\n",
                    module->dc->name, module->moduleId);
            (module->userDataDeleteFn)(module->userData);
        }
        mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, module);
    }
}

/**
 * Decrement the reference count on a DC, and if it's 0 and unmounted, blow it away.
 *
 * @param dc The Carousel to release
 *
 * @returns TRUE if the carousel is deleted, FALSE if not.
 */
static
void dcReleaseDC(mpe_DataCarousel *dc)
{
    mpe_Bool unMountMe = false;

    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(dcGlobalMutex);
    dc->referenceCount--;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
            "DC: %s: Releasing carousel.  Reference count now: %d\n", dc->name,
            dc->referenceCount);

    if (!dc->mounted && (dc->referenceCount == 0))
    {
        unMountMe = true;
    }

    mpe_mutexRelease(dcGlobalMutex);
    // END CRITICAL SECTION

    // If we want to unmount it, do it
    if (unMountMe)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                "DC: %s: Reference count reached 0.  Unmounting\n", dc->name);
        dcFreeDC(dc);
    }
}

/**
 * Free up all the memory associated with a data carousel.
 * Checks to see if each component is there, so it can be used in error
 * checks during allocation.
 */
static
void dcFreeDC(mpe_DataCarousel *dc)
{
    uint32_t i;

    if (dc != NULL)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "DC: %s: Freeing DC\n",
                dc->name);

        // Unregister the data carousel
        dcUnregisterDC(dc);

        // Unregister for SI change events (Fix for bug #1381)
        /*
         if(dc->edHandle != NULL)
         {
         (void)mpe_siUnRegisterForSIEvents(dc->edHandle);
         }
         */

        // Free the modules, if they're there
        if (dc->modules != NULL)
        {
            for (i = 0; i < dc->numModules; i++)
            {
                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                        "DC: %s: Freeing DC modules %d\n", dc->name, i);
                (void) mpe_dcUnloadModule(dc->modules[i]);
                dcFreeModule(dc->modules[i]);
            }
            // blast the parent
            mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, dc->modules);
        }

        // blast the whole darned carousel.  Tired of the silly horses anyway.
        mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, dc);

        // Now, if there are no more carousels registered, let's kick the
        // watcher thread so that he can see about going away.
        if (dcRegisteredCarousels == 0)
        {
            mpe_eventQueueSend(dcGlobalEventQueue, DC_CHECK_TIMEOUT, 0, NULL, 0);
        }
    }
}

/**
 * Disconnect a data carousel.
 * Note: This does not do any synchronization, assumed to be called from within a synchronized block.
 */
static
void dcDisconnectDC(mpe_DataCarousel *dc)
{
    mpe_Error retCode;

    // Do not disconnect OOB or DSG carousels.  They're always connected.
    if (!DC_IS_IB(dc))
        return;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "DC: %s: Disconnecting carousel\n",
            dc->name);
    dc->connected = false;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "DC: %s: Shutting down CA session\n",
            dc->name);

    retCode = mpe_podStopDecrypt(dc->decryptSessionHandle);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG( MPE_LOG_WARN, MPE_MOD_FILESYS, "DC: %s: Error stopping CA session (%d)\n",
                 dc->name, retCode );
    }
    dc->authorized = false;

    // If we were monitoring, let's mark now as the last time we checked.
    if (dc->monitored)
    {
        mpe_timeGetMillis(&dc->lastDiiCheck);
    }

    dc->monitored = false;
    dc->params.t.ib.tunerId = DC_INVALID_TUNER;
}

/**
 * Disconnect all carousels.
 */
static
void dcDisconnectCarousels(uint32_t tunerId)
{
    int i;

    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(dcGlobalMutex);

    // BUG: Needs to be reworked for multi-tuner.
    for (i = 0; i < DC_MAX_DATACAROUSELS; i++)
    {
        if (dcDataCarousels[i] != NULL && DC_IS_IB(dcDataCarousels[i])
                && dcDataCarousels[i]->connected
                && dcDataCarousels[i]->params.t.ib.tunerId == tunerId)
        {
            dcDisconnectDC(dcDataCarousels[i]);
        }
    }

    mpe_mutexRelease(dcGlobalMutex);
    // END CRITICAL SECTION
}

/**
 * Reconnect a carousel and start a version watcher.
 */
static
void dcReconnectDC(mpe_DataCarousel *dc, uint32_t tunerId)
{
    int i;
    mpe_Error retCode;
    // Only handle IB.  DSG and OOB can't disconnect, at least not at this point.
    // TODO: DSG???
    if (dc->connected || !DC_IS_IB(dc))
    {
        return;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "DC: %s: Reconnecting carousel\n",
            dc->name);
    dc->connected = true;
    dc->params.t.ib.tunerId = tunerId;

    // Re-start module prefetches to avoid falling back into
    // slow, demand-fetch mode. This happens when the carousel
    // gets disconnected while not being fully prefetched.

    if (!dc->authorized)
    {
        if (dc->decryptSessionHandle == NULL)
        {
            // Setup the CA session

            // Assert: dc->authorized == false (set in disconnect)
            retCode = ocuStartCASession( dc->params.t.ib.tunerId, dc->siHandle,
                                         dc->pid, dcGlobalEventQueue, &(dc->decryptSessionHandle) );
            if (retCode != MPE_SUCCESS)
            {
                MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILESYS,
                        "DC: ocuStartCASession returned error %d\n", retCode);
                return;
            }

            if (dc->decryptSessionHandle != NULL)
            {
                MPE_LOG(
                        MPE_LOG_INFO,
                        MPE_MOD_FILESYS,
                        "DC: %s: CA not yet established - deferring reconnect...\n",
                        dc->name );
                // We'll be called again when authorization has been established
                return;
            }
            // Assert: No CA required (dc->decryptSessionHandle == NULL)
            dc->authorized = true;
        }
        else
        { // Not authorized with an active CA session - so we can't set filters
            MPE_LOG(
                    MPE_LOG_INFO,
                    MPE_MOD_FILESYS,
                    "DC: %s: CA not authorized - not reconnecting\n",
                    dc->name );
            // We'll be called again if authorization changes
            return;
        }
    } // END if (!dc->authorized)

    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(dcGlobalMutex);
    for (i = 0; i < dc->numModules; i++)
    {
        if (dc->modules[i]->prefetched == false)
        {
            if (dc->totalSize < dcPrefetchMountSize
                    || dc->modules[i]->moduleSize < dcPrefetchModuleSize)
            {
                retCode = mpe_dcPrefetchModule(dc->modules[i]);
                if (retCode != MPE_SUCCESS)
                {
                    MPE_LOG(
                            MPE_LOG_WARN,
                            MPE_MOD_FILESYS,
                            "DC: %s: Could not restart prefetch for module %04x\n",
                            dc->name, dc->modules[i]->moduleId);
                }
            }
        }
    }
    mpe_mutexRelease(dcGlobalMutex);
    // END CRITICAL SECTION

    // Only set the version watcher if the update queue is non-null, if, somebody's
    // looking for DII version changes.
    if (dcCheckVersion && dc->updateQueueRegistered)
    {
        retCode = dcSetUpdateWatcher(dc, 0);
        //
        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILESYS,
                    "DC: %s: Could not reconnect carousel\n", dc->name);
            dc->connected = false;
            return;
        }
    }
}

/**
 * Reconnnect carousels.
 */
static
void dcReconnectCarousels(uint32_t tunerId)
{
    int i;
    mpe_Error retCode;
    mpe_MediaTuneParams tunerInfo;

    retCode = mpeos_mediaGetTunerInfo(tunerId, &tunerInfo);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILESYS,
                "DC: Could not get frequency for tuner %d\n", tunerId);
        return;
    }

    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(dcGlobalMutex);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
            "DC: Checking for carousels on frequency %d\n", tunerInfo.frequency);

    for (i = 0; i < DC_MAX_DATACAROUSELS; i++)
    {
        if (dcDataCarousels[i] != NULL && DC_IS_IB(dcDataCarousels[i])
                && !(dcDataCarousels[i]->connected)
                && (dcDataCarousels[i]->params.t.ib.frequency
                        == tunerInfo.frequency))
        {
            dcReconnectDC(dcDataCarousels[i], tunerId);
        }
    }

    mpe_mutexRelease(dcGlobalMutex);
    // END CRITICAL SECTION
}

/**
 * Find a module in the data carousel.
 * @param dc        The data carousel to search.
 * @param moduleID  The moduleID to search for.
 *
 * @returns The module if found, NULL otherwise.
 */
static mpe_DcModule *
dcFindModule(mpe_DataCarousel *dc, uint16_t moduleID)
{
    uint32_t i;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
            "DC: %s: Searching for module %04x.  Checking in %d locations\n",
            dc->name, moduleID, dc->numModules);

    // Walk the list of modules until we find this
    for (i = 0; i < dc->numModules; i++)
    {
        if (dc->modules[i]->moduleId == moduleID)
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                    "DC: %s: dcFindModule %04x found module at location %d\n",
                    dc->name, moduleID, i);
            return (dc->modules[i]);
        }
    }

    // Didn't find it.
    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
            "DC: %s: Couldn't find module ID %04x\n", dc->name, moduleID);

    return NULL;
}

/**
 * Mark a module as invalid.  Once marked invalid, no one will attempt to insert data into
 * it, or fetch more data.
 * Modules are marked invalid when a: the carousel is unmounted, or b: the module version
 * changes and the module is replaced.
 */
static
void dcInvalidateModule(mpe_DcModule *module)
{
    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(dcGlobalMutex);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
            "DC: %s: Invalidating module %04x.  Reference count: %d\n",
            module->dc->name, module->moduleId, module->referenceCount);
    module->valid = FALSE;

    // Cancel any outstanding prefetches for this module.
    dcCancelModulePrefetches(module);

    // Is there a race condition here at all?
    // FIXME: TODO:
    // What if a get module comes in?  I think the lock on the DC between
    // the invalidate thread and this covers it
    if (module->referenceCount == 0)
    {
        (void) mpe_dcUnloadModule(module);
        dcFreeModule(module);
    }

    mpe_mutexRelease(dcGlobalMutex);
    // END CRITICAL SECTION
}

/**
 * Register the data carousel into the global struture
 * so that it can be found at later dates.
 */
static mpe_Error dcRegisterDC(mpe_DataCarousel *dc)
{
    mpe_Error retCode = MPE_FS_ERROR_FAILURE;
    int i;

    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(dcGlobalMutex);

    if (dcRegisteredCarousels >= DC_MAX_DATACAROUSELS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "DC: %s: Max data carousels already mounted: %d\n", dc->name,
                DC_MAX_DATACAROUSELS);
        retCode = MPE_EINVAL;
    }
    else
    {
        // Count that we're registered
        dcRegisteredCarousels++;

        // Find the first empty slot
        for (i = 0; i < DC_MAX_DATACAROUSELS; i++)
        {
            // If this slot is empty
            if (dcDataCarousels[i] == NULL)
            {
                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                        "DC: %s: Registering in slot %d\n", dc->name, i);
                // put the carousel here
                dcDataCarousels[i] = dc;
                retCode = MPE_SUCCESS;
                // And get out of the loop.
                break;
            }
        }
    }

    mpe_mutexRelease(dcGlobalMutex);
    // END CRITICAL SECTION
    return retCode;
}

/**
 * Remove the data carousel from the global list of carousels.
 */
static
void dcUnregisterDC(mpe_DataCarousel *dc)
{
    int i;

    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(dcGlobalMutex);

    // Find the carousel
    for (i = 0; i < DC_MAX_DATACAROUSELS; i++)
    {
        // If this slot contains this carousel
        if (dcDataCarousels[i] == dc)
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                    "DC: %s: UnRegistering from slot %d\n", dc->name, i);
            // Remove it
            dcDataCarousels[i] = NULL;

            // Count that we're unregistered
            dcRegisteredCarousels--;

            // And get out of the loop.
            break;
        }
    }

    mpe_mutexRelease(dcGlobalMutex);
    // END CRITICAL SECTION

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
            "DC: %s: %d carousels remain registered\n", dc->name,
            dcRegisteredCarousels);
}

/**
 * Calculate the time of the next timeout in the queue.
 * Walks the list of timeouts, and returns one which has the lowest
 * non-zero timeout time.  Requests with 0 timeouts are expected to never
 * timeout (such as DII update requests).
 */
static
void dcSetNextTimeout(void)
{
    int i;
    DcRequestRecord *earliest = NULL;
    DcRequestRecord *curr;

    // Search the structure
    for (i = 0; i < DC_HASH_PRIME; i++)
    {
        // Start each list
        curr = dcAsyncRequests[i];
        // Walk the list
        while (curr)
        {
            // If it hase a timeout
            if (curr->timeout != 0)
            {
                // If there is an earliest on already
                if (earliest)
                {
                    // And we're before it
                    if (curr->timeout < earliest->timeout)
                    {
                        earliest = curr;
                    }
                }
                else
                {
                    // There's not an earliest, and this has a timeout,
                    // so it must be the earliest
                    earliest = curr;
                }
            }
            curr = curr->next;
        }

    }
    if (earliest != NULL)
    {
        dcEarliestTimeoutReq = earliest;
        dcEarliestTimeout = earliest->timeout;
    }
    else
    {
        dcEarliestTimeoutReq = NULL;
        dcEarliestTimeout = 0;
    }
}

/**
 * Calculate the timeout time.
 *
 * @param interval      Number of milliseconds that a timeout should take.
 *
 * @return 0 if interval is 0, the current time + interval milliseconds otherwise.
 */
static mpe_TimeMillis dcCalculateTimeout(uint32_t interval)
{
    mpe_TimeMillis timeout;
    if (interval == 0)
    {
        return ((mpe_TimeMillis) 0);
    }
    mpe_timeGetMillis(&timeout);
    timeout += (mpe_TimeMillis) interval;
    return timeout;
}

/**
 * Find a request record corresponding to a uniqueifier/requestID.
 * Removes it from the list.
 *
 * @param uniqueifier   The Request ID to look for.
 * @param delete        Should we delete this request from the queue?
 *
 * @returns The request record if the uniqueifier is found, NULL otherwise.
 */
static
DcRequestRecord *
dcFindUniqueifier(uint32_t uniqueifier, mpe_Bool delete)
{
    DcRequestRecord *curr = NULL;
    DcRequestRecord *prev = NULL;
    uint32_t index;

    // Figure out where in the hash this is.
    index = uniqueifier % DC_HASH_PRIME;

    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(dcGlobalMutex);

    curr = dcAsyncRequests[index];
    while (curr != NULL)
    {
        // Check to see if we found the Uniqueifier
        if (curr->uniqueifier == uniqueifier)
        {
            if (delete)
            {
                // Found it, remove it from the list
                if (prev == NULL)
                {
                    // First in the list, set list to second item
                    dcAsyncRequests[index] = curr->next;
                }
                else
                {
                    // Not first, remove it from the previous pointer
                    prev->next = curr->next;
                }

                curr->next = NULL;
            }

            // we're done, break out of the loop, see 'ya
            break;
        }
        prev = curr;
        curr = curr->next;
    }

    // If it's a prefetch, decrement the count of outstanding prefetches.
    // and we're done with it.
    if (curr != NULL && curr->reqType == DDB_PREFETCH && delete)
    {
        dcOutstandingPrefetches--;
    }

    // Check if this is the earliest timeout
    if (dcEarliestTimeoutReq == curr)
    {
        dcSetNextTimeout();
    }

    mpe_mutexRelease(dcGlobalMutex);
    // END CRITICAL SECTION

    return curr;
}

/**
 * This request has timed out.  Cancel it, and set it's timeout to 0.
 * When it's timeout is 0, the dcSetNextTimeout() routine will not identify
 * it as a request which needs to timeout.  We leave it in the queue so that
 * dcHandleSectionEvent can clean it up when the FilterCancelled request
 * comes in.
 *
 * @param req     The request to timeout.
 */
static
void dcTimeoutRequest(DcRequestRecord *req)
{
    MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILESYS,
            "DC: %s: Timing out asynchronous %s request %04x\n", req->dc->name,
            DC_REQUEST_TYPE_NAME(req->reqType), req->uniqueifier);
    req->timeout = 0;
    // If this was a DII Update request, mark the DC as no longer monitored.
    if (req->reqType == DII_UPDATE)
    {
        // Mark the carousel as not monitored.
        req->dc->monitored = false;
        // Indicate the we checked the DII.
        mpe_timeGetMillis(&(req->dc->lastDiiCheck));
    }
    mpe_filterCancelFilter(req->uniqueifier);
}

/**
 * Insert a record into the async request structure.
 * @param request       The request to insert.
 */
static
void dcInsertAsyncRequest(DcRequestRecord *request)
{
    uint32_t index;
    mpe_Bool checkTimeout = false;

    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(dcGlobalMutex);

    // Reference count this call
    request->dc->referenceCount++;

    // figure out where in the arry to put it.
    index = request->uniqueifier % DC_HASH_PRIME;

    // Insert it into the queue at index
    request->next = dcAsyncRequests[index];
    dcAsyncRequests[index] = request;
    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_FILESYS,
            "DC: %s: Inserted Uniqueifier %04x for %s request at position %d\n",
            request->dc->name, request->uniqueifier,
            DC_REQUEST_TYPE_NAME(request->reqType), index);

    // If it's a prefetch, increment the number of outstanding prefetches.
    if (request->reqType == DDB_PREFETCH)
    {
        dcOutstandingPrefetches++;
    }

    // Determine if we need to check the timeout time.
    // Do this if we have a timeout, and we're earlier than any other timeout, or there isn't any
    // other timeout
    if ((request->timeout != 0) && (dcEarliestTimeout == 0 || request->timeout
            < dcEarliestTimeout))
    {
        dcEarliestTimeout = request->timeout;
        dcEarliestTimeoutReq = request;
        checkTimeout = TRUE;
    }

    mpe_mutexRelease(dcGlobalMutex);
    // END CRITICAL SECTION

    // Indicate that there's a new timeout time, if there is
    if (checkTimeout)
    {
        mpe_eventQueueSend(dcGlobalEventQueue, DC_CHECK_TIMEOUT, 0, NULL, 0);
    }
}

/**
 * Insert a uniqueifier into the request record hash.
 *
 * @param stream      The stream/PID to get the data from.
 * @param filter      The filter which created this request.
 * @param reqType     The type fo request that this is, namely a prefetch or version change.
 * @param dc          The data carousel which this request deals with.
 * @param module      The module this is a prefetch for (NULL for version changes)
 * @param ddb         The DDB this request is a prefetch for (ignored for version changes)
 * @param queue       Deliver section filtering events to this queue.
 * @param timeout     Time to stop waiting for the section.  0 == wait forever.
 *
 * @returns MPE_SUCCESS if successfully inserted, MPE_NOMEM if unable to create the record.
 */
static mpe_Error dcStartAsyncRequest(mpe_FilterSource *stream,
        mpe_FilterSpec *filter, DcRequestType reqType, mpe_DataCarousel *dc,
        mpe_DcModule *module, uint32_t ddb, mpe_EventQueue queue,
        mpe_TimeMillis timeoutTime, uint32_t expectedSections,
        DcRequestRecord **reqOut)
{
    uint32_t uniqueifier = 0; // Annoying compiler.
    mpe_Error retCode;
    DcRequestRecord *request = NULL;

    // Check the parameters
    // Make sure there's a DC, and make sure we have a DDB's that's less than 256
    if (!dc)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "DC: ERROR: StartAsyncRequest: DC is NULL\n");
        return MPE_EINVAL;
    }

    retCode = mpeos_memAllocP(MPE_MEM_FILE_CAROUSEL, sizeof(DcRequestRecord),
            (void **) &request);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "DC: Unable to allocate request record memory\n");
        return retCode;
    }

    // Fill in the record
    request->filter = filter;
    request->reqType = reqType;
    request->dc = dc;
    request->module = module;
    request->ddb = ddb;
    request->timeout = timeoutTime;

    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(dcGlobalMutex);

    // Now, make sure the carousel is still connected & authorized
    if (dc->connected && dc->mounted && dc->authorized)
    {
        // It is, let's get the section
        retCode = mpe_filterSetFilter(stream, filter, queue, NULL,
                MPE_SF_FILTER_PRIORITY_OC, expectedSections, 0, &uniqueifier);
    }
    else
    {
        MPE_LOG(
                MPE_LOG_WARN,
                MPE_MOD_FILESYS,
                "DC: %s: Unable to start asynch request for unconnected/unmounted/unauthorized carousel\n",
                dc->name);

        // BUG: TODO: FIXME: Wrong return code?
        // We need to return a code which indicates that the carousel is unconnected
        // so we can signal the right error.
        retCode = MPE_FS_ERROR_DISCONNECTED;
    }

    // Make sure we got a valid request
    // Will fail if we're disconnected.
    if (retCode == MPE_SUCCESS)
    {
        // Save the uniqueifier
        request->uniqueifier = uniqueifier;

        // Insert the request
        dcInsertAsyncRequest(request);
        // Return thue value
        if (reqOut != NULL)
        {
            *reqOut = request;
        }
    }
    else
    {
        if (retCode != MPE_FS_ERROR_DISCONNECTED)
        {
            MPE_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_FILESYS,
                    "DC: %s: Unable to set filter for %s request: Error code: %04x\n",
                    dc->name, DC_REQUEST_TYPE_NAME(reqType), retCode);
        }
        mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, request);
    }

    mpe_mutexRelease(dcGlobalMutex);
    // END CRITICAL SECTION

    return retCode;
}

/**
 * Cancel all outstanding uniqueifier requests for a given DC
 */
static
void dcCancelDCRequests(mpe_DataCarousel *dc)
{
    uint32_t i;
    DcRequestRecord *curr = NULL;

    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(dcGlobalMutex);

    // First, if this is a polling request, blow away the uniqueifier
    if (dc->pollDII)
    {
        curr = dcFindUniqueifier(MPE_SF_INVALID_UNIQUE_ID, true);
        if (curr != NULL)
        {
            dcFreeRequestRecord(curr);
        }
    }
    // Now, walk all the other entries, searching for this one
    for (i = 0; i < DC_HASH_PRIME; i++)
    {
        curr = dcAsyncRequests[i];
        while (curr != NULL)
        {
            // Check to see if we found the data carousel
            if (curr->dc == dc)
            {
                // Found it.  Delete the sucker
                MPE_LOG(
                        MPE_LOG_DEBUG,
                        MPE_MOD_FILESYS,
                        "DC: %s: Cancelling %s RequestID %04x: Module %04x[%d]\n",
                        dc->name, DC_REQUEST_TYPE_NAME(curr->reqType),
                        curr->uniqueifier,
                        ((curr->module) ? curr->module->moduleId : 0),
                        ((curr->module) ? curr->ddb : 0));
                mpe_filterCancelFilter(curr->uniqueifier);
                if (curr->reqType == DII_UPDATE)
                {
                    // It was an update request, so clear everything out
                    curr->dc->monitored = false;
                    curr->dc->diiRequest = NULL;
                }
            }
            curr = curr->next;
        }
    }

    mpe_mutexRelease(dcGlobalMutex);
    // END CRITICAL SECTION
}

#if 0
/**
 * Cancel all outstanding uniqueifier requests for a given DC (SI Changes)
 * Fix for bug #1381 (Look for SI Change events)
 */
static
void dcReStartDCRequests(mpe_DataCarousel *dc)
{
    uint32_t i;
    DcRequestRecord *curr = NULL;
    mpe_Error retCode = MPE_SUCCESS;

    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(dcGlobalMutex);

    for (i = 0; i < DC_HASH_PRIME; i++)
    {
        curr = dcAsyncRequests[i];
        while (curr != NULL)
        {
            // Check to see if we found the data carousel
            if (curr->dc == dc)
            {
                // Found it.  Delete the sucker
                MPE_LOG(
                        MPE_LOG_DEBUG,
                        MPE_MOD_FILESYS,
                        "DC: %s: Cancelling %s RequestID %04x: Module %04x[%d]\n",
                        dc->name, DC_REQUEST_TYPE_NAME(curr->reqType),
                        curr->uniqueifier,
                        ((curr->module) ? curr->module->moduleId : 0),
                        ((curr->module) ? curr->ddb : 0));

                mpe_filterCancelFilter(curr->uniqueifier);

                // Fix for bug #1381
                // Check if this was a pre-fetch (Module or DDB) and
                // re-issue those requests
                if (curr->reqType == MODULE_PREFETCH)
                {
                    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                            "\nDC: %s: Restarting pre-fetch for Module %04x\n",
                            dc->name, curr->module);
                    if (dcPrefetchModules && !(curr->module->prefetched))
                    {
                        retCode = mpe_dcPrefetchModule(curr->module);
                        if (retCode != MPE_SUCCESS)
                        {
                            MPE_LOG(
                                    MPE_LOG_DEBUG,
                                    MPE_MOD_FILESYS,
                                    "DC: %s: Could not start pre-fetch for Module %04x\n",
                                    dc->name, curr->module);
                        }
                    }
                }
                else if (curr->reqType == DDB_PREFETCH)
                {
                    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                            "DC: %s: Restarting pre-fetch for DDB %04x\n",
                            dc->name, curr->ddb);

                    retCode = dcFetchDDB(curr->module, curr->ddb, DDB_PREFETCH,
                            dcGlobalEventQueue, dcCalculateTimeout(
                                    curr->module->ddbTimeout));
                    if (retCode != MPE_SUCCESS)
                    {
                        MPE_LOG(
                                MPE_LOG_DEBUG,
                                MPE_MOD_FILESYS,
                                "DC: %s: Could not start pre-fetch for DDB %04x\n",
                                dc->name, curr->ddb);
                    }
                }
            }
            curr = curr->next;
        }
    }

    mpe_mutexRelease(dcGlobalMutex);
    // END CRITICAL SECTION
}
#endif

/**
 * Cancel all outstanding filter requests for a given DC (SI Changes)
 */
static
void dcCancelFilterRequests(mpe_DataCarousel *dc)
{
    uint32_t i;
    DcRequestRecord *curr = NULL;

    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(dcGlobalMutex);

    for (i = 0; i < DC_HASH_PRIME; i++)
    {
        curr = dcAsyncRequests[i];
        while (curr != NULL)
        {
            // Check to see if we found the data carousel
            if (curr->dc == dc)
            {
                // Found it.  Delete the sucker
                MPE_LOG(
                        MPE_LOG_DEBUG,
                        MPE_MOD_FILESYS,
                        "DC: %s: Cancelling %s RequestID %04x: Module %04x[%d]\n",
                        dc->name, DC_REQUEST_TYPE_NAME(curr->reqType),
                        curr->uniqueifier,
                        ((curr->module) ? curr->module->moduleId : 0),
                        ((curr->module) ? curr->ddb : 0));

                mpe_filterCancelFilter(curr->uniqueifier);
            }
            curr = curr->next;
        }
    }

    mpe_mutexRelease(dcGlobalMutex);
    // END CRITICAL SECTION
} // END dcCancelFilterRequests

/**
 * Restart pre-fetches
 */
static
void dcRestartPrefetch(mpe_DataCarousel *dc)
{
    uint32_t i;
    DcRequestRecord *curr = NULL;
    mpe_Error retCode = MPE_SUCCESS;

    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(dcGlobalMutex);

    for (i = 0; i < DC_HASH_PRIME; i++)
    {
        curr = dcAsyncRequests[i];
        while (curr != NULL)
        {
            // Check to see if we found the data carousel
            if (curr->dc == dc)
            {
                if (curr->reqType == MODULE_PREFETCH)
                {
                    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                            "\nDC: %s: Restarting pre-fetch for Module %p\n",
                            dc->name, curr->module);
                    if (dcPrefetchModules && !(curr->module->prefetched))
                    {
                        retCode = mpe_dcPrefetchModule(curr->module);
                        if (retCode != MPE_SUCCESS)
                        {
                            MPE_LOG(
                                    MPE_LOG_DEBUG,
                                    MPE_MOD_FILESYS,
                                    "DC: %s: Could not start pre-fetch for Module %p\n",
                                    dc->name, curr->module);
                        }
                    }
                }
                else if (curr->reqType == DDB_PREFETCH)
                {
                    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                            "DC: %s: Restarting pre-fetch for DDB %04x\n",
                            dc->name, curr->ddb);

                    retCode = dcFetchDDB(curr->module, curr->ddb, DDB_PREFETCH,
                            dcGlobalEventQueue, dcCalculateTimeout(
                                    curr->module->ddbTimeout));
                    if (retCode != MPE_SUCCESS)
                    {
                        MPE_LOG(
                                MPE_LOG_DEBUG,
                                MPE_MOD_FILESYS,
                                "DC: %s: Could not start pre-fetch for DDB %04x\n",
                                dc->name, curr->ddb);
                    }
                }
            }
            curr = curr->next;
        }
    }

    mpe_mutexRelease(dcGlobalMutex);
    // END CRITICAL SECTION
} // END dcRestartPrefetch

/**
 * Start update watcher for the dc
 */
static
void dcRestartUpdateWatcher(mpe_DataCarousel *dc)
{
    mpe_Error retCode;

    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(dcGlobalMutex);

	// Reset the update watcher IFF we've got an updateQueue.
	// Otherwise, for simplicity, we'll bite the bullet and just
	// check the next time we try to "get" a module.
	// This should be rare, so we don't care.
	if (dcCheckVersion && dc->updateQueueRegistered)
	{
		retCode = dcSetUpdateWatcher(dc, 0);
		if (retCode != MPE_SUCCESS)
		{
			MPE_LOG(
					MPE_LOG_ERROR,
					MPE_MOD_FILESYS,
					"DC: %s: Unable to start watching for version change events\n",
					dc->name);
			dc->mounted = false;
			// Cancel any prefetches which started
			dcCancelDCRequests(dc);
			// Free up the DC
			dcReleaseDC(dc);
		}
	}

    mpe_mutexRelease(dcGlobalMutex);
    // END CRITICAL SECTION
}

/**
 * Cancel all outstanding uniqueifier requests for a given module
 */
static
void dcCancelModulePrefetches(mpe_DcModule *module)
{
    uint32_t i;
    DcRequestRecord *curr = NULL;

    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(dcGlobalMutex);

    for (i = 0; i < DC_HASH_PRIME; i++)
    {
        curr = dcAsyncRequests[i];
        while (curr != NULL)
        {
            // Check to see if we found the data carousel
            if (curr->module == module)
            {
                // Found it.  Delete the sucker
                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                        "DC: %s: Cancelling Prefetch request %04x\n",
                        module->dc->name, curr->uniqueifier);
                mpe_filterCancelFilter(curr->uniqueifier);
            }
            curr = curr->next;
        }
    }

    mpe_mutexRelease(dcGlobalMutex);
    // END CRITICAL SECTION
}

/**
 * Set a DII polling request.
 */
static mpe_Error dcSetPollRequest(mpe_DataCarousel *dc)
{
    mpe_TimeMillis later;
    mpe_Error retCode;
    DcRequestRecord *request;

    // Allocate up a request record
    retCode = mpeos_memAllocP(MPE_MEM_FILE_CAROUSEL, sizeof(DcRequestRecord),
            (void **) &request);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "DC: Unable to allocate request record memory\n");
        return retCode;
    }

    // Calculate the polling time
    later = dcCalculateTimeout(dcDIIPollingInterval);

    // Add the time
    // Fill in the record
    request->reqType = DII_POLL;
    request->dc = dc;
    // BUG: TODO: FIXME: There can be only one filter request with this value.
    // Only 1 carousel should be mounted with a polling UID.
    request->uniqueifier = MPE_SF_INVALID_UNIQUE_ID;
    request->timeout = later;
    request->module = NULL;
    request->filter = NULL;
    request->ddb = 0;

    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(dcGlobalMutex);
    if (dc->mounted)
    {
        MPE_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_FILESYS,
                "DC: %s: Installing DII Polling request record.  First time: %d\n",
                dc->name, (uint32_t) later);

        dcInsertAsyncRequest(request);
    }
    else
    {
        mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, request);
    }
    mpe_mutexRelease(dcGlobalMutex);
    // END CRITICAL SECTION

    return MPE_SUCCESS;
}

/**
 * Create a filter to watch for changes in the DII.
 *
 * @param dc The Data Carousel to look for changes in.
 * @param timeout
 *
 * @returns MPE_SUCCESS if the version change filter was set correctly, error codes otherwise.
 */
static mpe_Error dcSetUpdateWatcher(mpe_DataCarousel *dc, uint32_t timeout)
{
    mpe_FilterSpec *filter;
    mpe_Error retCode = MPE_SUCCESS;
    mpe_FilterSource stream;

    if (dc->mounted && dc->pollDII)
    {
        return dcSetPollRequest(dc);
    }

    // Skip this if we're not enabling version checking for the OOB
    // HACK
    if (DC_IS_OOB(dc) && !dcCheckVersionOOB)
    {
        return MPE_SUCCESS;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
            "DC: %s: Setting DII Watch filter with timeout %d\n", dc->name,
            timeout);

    // Get the stream for the data
    stream.pid = dc->pid;

    // Set up the stream.
    ocuSetStreamParams(&(dc->params), &stream);

    if (dc->mounted)
    {
        // Make the filter
        retCode = ocuMakeDIIChangeFilter(dc->diiTransactionID,
#ifdef PTV
                DC_IS_OOB(dc),
                dc->carouselID,
#endif
                &filter);
        if (retCode != MPE_SUCCESS)
        {
            dc->monitored = false;
            MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILESYS,
                    "DC: %s: Unable to create version change filter\n",
                    dc->name);
        }
        else
        {
            dc->monitored = true;
            // Start the request
            retCode = dcStartAsyncRequest(&stream, filter, DII_UPDATE, dc,
                    NULL, 0, dcGlobalEventQueue, dcCalculateTimeout(timeout),
                    1, &(dc->diiRequest));

            if (retCode != MPE_SUCCESS)
            {
                dc->monitored = false;
                MPE_LOG(
                        MPE_LOG_ERROR,
                        MPE_MOD_FILESYS,
                        "DC: %s: Unable to start DII Version checking request: Error code: %04x\n",
                        dc->name, retCode);
                mpe_filterDestroyFilterSpec(filter);
            }
        }
    }

    return retCode;
}

/**
 * Update the DII of a DC when a version change is detected.
 *
 * @param section The section handle to use to update the DC.
 * @param dc      The data carousel containing the DII we wish to update.
 */
static
void dcUpdateDII(mpe_FilterSectionHandle section, mpe_DataCarousel *dc)
{
    mpe_Error retCode;
    mpe_OcpDII *dii;

    // Parse the DII here.
    retCode = mpe_ocpParseDII(section, &dii);

    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(dcGlobalMutex);

    // If we parsed the DII correctly, update the modules
    if (retCode == MPE_SUCCESS)
    {
        MPE_LOG(
                MPE_LOG_INFO,
                MPE_MOD_FILESYS,
                "DC: %s: Updating DII version.  New Version: %d  Old Version: %d  Transaction ID: %08x\n",
                dc->name, dii->header.version, dc->diiVersion,
                dii->header.transactionID);

        // Do the update, assuming the version is different
        if (dii->header.version != dc->diiVersion)
        {
            retCode = dcUpdateModules(dc, dii);

            if (retCode != MPE_SUCCESS)
            {
                MPE_LOG(
                        MPE_LOG_ERROR,
                        MPE_MOD_FILESYS,
                        "DC: %s: ERROR -- POTENTIAL MEMORY LEAK: Unable to update modules on an invalidate\n",
                        dc->name);
            }
            if (dc->updateQueueRegistered)
            {
                MPE_LOG(
                        MPE_LOG_DEBUG,
                        MPE_MOD_FILESYS,
                        "DC: %s: Sending carousel change event to event queue\n",
                        dc->name);
                mpe_eventQueueSend(dc->updateQueue, MPE_DC_UPDATE_EVENT,
                        dc->updateData, NULL, 0);
            }
        }
        else
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                    "DC: %s: Version in new DII has not changed.  Ignoring\n",
                    dc->name);
        }
        // Indicate the we checked the DII.
        mpe_timeGetMillis(&(dc->lastDiiCheck));

        // Grab the version stuffs
        dc->diiVersion = dii->header.version;
        dc->diiTransactionID = dii->header.transactionID;

    }
    else
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "DC: %s: Unable to parse DII record\n", dc->name);
    }

    // Set a new watcher.  We do this whether we parsed the DII or not.  We'll just pick
    // up the next one if this one was bogus.
    // NOTE: This assumes the next one will be valid.  If the DII's are corrupted coming from
    // the headend, we're just plain hosed
    // Note: No need to check this on dcCheckVersion, as if that wasn't set, we're not here.
    if (dcCheckVersion)
    {
        (void) dcSetUpdateWatcher(dc, (DC_IS_OOB(dc) ? dcDIICheckDurationOOB
                : dcDIICheckDuration));
    }

    mpe_mutexRelease(dcGlobalMutex);
    // END CRITICAL SECTION

    mpe_ocpFreeDII(dii);
    mpe_filterSectionRelease(section);
}

static
void dcPollDII(mpe_DataCarousel *dc)
{
    mpe_FilterSpec *filter;
    mpe_Error retCode = MPE_SUCCESS;
    mpe_FilterSource stream;
    DcRequestRecord *request;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
            "DC: %s: Polling for DII changes\n", dc->name);

    // First, remove the request record.
    // A new one will get set when we go through the UpdateDII call.
    request = dcFindUniqueifier(MPE_SF_INVALID_UNIQUE_ID, true);
    dcFreeRequestRecord(request);

    // Get the stream for the data
    stream.pid = dc->pid;

    // Set up the stream.
    ocuSetStreamParams(&(dc->params), &stream);

    if (dc->mounted)
    {
        // Make the filter
        retCode = ocuMakeDIIFilter(dc->diiTransactionID,
#ifdef PTV
                DC_IS_OOB(dc),
                dc->carouselID,
#endif
                &filter);
        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILESYS,
                    "DC: %s: Unable to create version change filter\n",
                    dc->name);
        }
        else
        {
            retCode = dcStartAsyncRequest(&stream, filter, DII_UPDATE, dc,
                    NULL, 0, dcGlobalEventQueue, 0, 1, NULL);

            if (retCode != MPE_SUCCESS)
            {
                MPE_LOG(
                        MPE_LOG_ERROR,
                        MPE_MOD_FILESYS,
                        "DC: %s: Unable to start DII Version checking request: Error code: %04x\n",
                        dc->name, retCode);
                mpe_filterDestroyFilterSpec(filter);
            }
        }
    }
}

/**
 * Routine to handle the asynchronous notification of having found section.
 * This routine fetches the uinqueifier record, and hands the event
 *
 * @param uniqueifier The ID of the section to processes, and the corresponding
 *                    request record.
 */
static
void dcHandleAsyncSection(mpe_Event eventID, uint32_t uniqueifier)
{
    DcRequestRecord *request;
    mpe_Error retCode;
    mpe_FilterSectionHandle section;

    // Get the request
    // Remove it from the queue if this is a "terminal" event
    request = dcFindUniqueifier(uniqueifier, EVENT_IS_TERMINAL(eventID));

    // No request found.  Free up the section, and get out
    if (request == NULL)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "DC: Could not find uniqueifier: %04x\n", uniqueifier);
        mpe_filterCancelFilter(uniqueifier);
        return;
    }

    // Get the section handle.
    retCode = mpe_filterGetSectionHandle(uniqueifier, 0, &section);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_FILESYS,
                "DC: %s: Could not get section corresponding to uniqueifier: %04x\n",
                request->dc->name, uniqueifier);
        // Get rid of the filters
        mpe_filterCancelFilter(uniqueifier);

        goto CleanUp;
    }

    // Now, we have the section.  Process it.
    switch (request->reqType)
    {
    case DII_UPDATE:
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_FILESYS,
                "DC: %s: Version Change message: %04x\n", request->dc->name,
                uniqueifier);
        dcUpdateDII(section, request->dc);
        break;
    case DDB_DEMANDFETCH:
    case DDB_PREFETCH:
    case MODULE_PREFETCH:
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                "DC: %s: %s %s: Module: %04x RequestID: %04x\n",
                request->dc->name, DC_REQUEST_TYPE_NAME(request->reqType),
                (EVENT_IS_TERMINAL(eventID) ? "complete" : "section found"),
                request->module->moduleId, uniqueifier);

        retCode = dcInsertDDB(request->module, section);
        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_FILESYS,
                    "DC: %s Insert %s DDB Failed: Module: %04x Request: %04x Uniqueifier: %u\n",
                    request->dc->name, DC_REQUEST_TYPE_NAME(request->reqType),
                    request->module->moduleId, request->ddb, uniqueifier);
        }
        // If this was a prefetch or demand fetch, mark that the block in question is no longer
        // in the process of being fetched.
        if (request->reqType != MODULE_PREFETCH)
        {
            request->module->ddbs[request->ddb].prefetched = FALSE;
        }
        // If this was a module prefetch, and we just hit the last section, let's kick this over
        // to mopup thread to see if there's anything else to prefetch
        if (dcPrefetchMopup && request->reqType == MODULE_PREFETCH && eventID
                == MPE_SF_EVENT_LAST_SECTION_FOUND)
        {
            // BEGIN CRITICAL SECTION
            mpe_mutexAcquire(dcGlobalMutex);

            // Reference the DC and the module so they don't go away
            request->module->referenceCount++;
            request->module->dc->referenceCount++;

            mpe_mutexRelease(dcGlobalMutex);
            // END CRITICAL SECTION

            mpe_eventQueueSend(dcGlobalMopupQueue, DC_MOPUP_MODULE,
                    (void *) request->module, NULL, 0);
        }

        break;
    default:
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILESYS,
                "DC: Unrecognized asynchronous event %04x: %08x\n",
                uniqueifier, request->reqType);
        // Toss the section, as nobody consumed it.
        mpe_filterSectionRelease(section);
        break;
    }

    // Release the filter structures
    // mpe_filterSectionRelease(section);

    CleanUp:
    // Free the request record if we don't expect any more responses
    if (EVENT_IS_TERMINAL(eventID))
    {
        dcFreeRequestRecord(request);
    }
}

/**
 * Free up a request record.  Does nothing with the section data, assumes
 * that is correctly handled before calling this routine.
 */
static
void dcFreeRequestRecord(DcRequestRecord *request)
{
    // If there's a module specified, release it
    if (request->module != NULL)
    {
        mpe_dcReleaseModule(request->module);
    }

    // Release the DC, we're not holding onto it anymore
    dcReleaseDC(request->dc);

    // Release the filter
    if (request->uniqueifier != MPE_SF_INVALID_UNIQUE_ID)
    {
        mpe_filterRelease(request->uniqueifier);

        // Release the filter
        mpe_filterDestroyFilterSpec(request->filter);
    }

    // Free the memory
    mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, request);
}

/**
 * Handle a Cancel request.
 * Just get the request, and delete it.
 */
static
void dcHandleCancel(uint32_t uniqueifier)
{
    DcRequestRecord *request;

    request = dcFindUniqueifier(uniqueifier, true);
    if (request != NULL)
    {
        mpe_DcModule *module = request->module;
        if (module != NULL && module->prefetched == true)
        {
            module->prefetched = false;
        }
        dcFreeRequestRecord(request);
    }
    else
    {
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILESYS,
                "DC: Could not find cancelled uniqueifier: %04x\n", uniqueifier);
    }
}

/**
 * Handle a section event that's received from the queue.  Either section found, cancelled, whatnot.
 * @param eventID       The Event we need to handle.
 * @param uniqueifier   The event data, ie, the section uniqueifier, for this event.
 *
 * @return TRUE if we could process this event.  False otherwise.
 */
static
void dcHandleSectionEvent(mpe_Event eventID, uint32_t uniqueifier)
{
    switch (eventID)
    {
    case MPE_SF_EVENT_SECTION_FOUND:
    case MPE_SF_EVENT_LAST_SECTION_FOUND:
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "DC: Got message: %s: %04x\n",
                ((eventID == MPE_SF_EVENT_SECTION_FOUND) ? "SECTION FOUND"
                        : "LAST SECTION FOUND"), uniqueifier);
        dcHandleAsyncSection(eventID, uniqueifier);
        break;
    case MPE_SF_EVENT_FILTER_PREEMPTED:
    case MPE_SF_EVENT_FILTER_CANCELLED:
    case MPE_SF_EVENT_SOURCE_CLOSED:
        MPE_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_FILESYS,
                "DC: Got message: FILTER CANCEL: %s : %04x\n",
                ((eventID == MPE_SF_EVENT_FILTER_CANCELLED) ? "CANCELLED"
                        : ((eventID == MPE_SF_EVENT_FILTER_PREEMPTED) ? "PREEMPTED"
                                : "TUNE AWAY")), uniqueifier);
        dcHandleCancel(uniqueifier);
        break;
    default:
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILESYS,
                "DC: Got unexpected message: %04x: %04x\n", eventID,
                uniqueifier);
        break;
    }
}

/**
 * When a service changes it's SI information, find all carousels which
 * are on that service, and restart their outstanding requests, as they may be
 * on a new PID.
 *
 * @param siHandle       The SI Handle to change.
 */
static
void dcUpdateSIHandle(mpe_SiServiceHandle siHandle)
{
    int i;
    uint32_t j;
    mpe_Error retCode;
    mpe_DataCarousel *dc;

    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(dcGlobalMutex);

    // Walk through list of DCs (dcDataCarousels)
    // and find the ones matching the SI handle
    for (i = 0; i < DC_MAX_DATACAROUSELS; i++)
    {
        dc = dcDataCarousels[i];

        // If this slot contains this carousel
        if (dc != NULL && dc->connected && dc->authorized)
        {
            if (dc->mounted)
            {
                MPE_LOG(
                        MPE_LOG_DEBUG,
                        MPE_MOD_FILESYS,
                        "DC: Found a mounted carousel dcDataCarousels[%d]: 0x%p\n",
                        i, dc);
            }

            if (dc->siHandle == siHandle)
            {
                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                        "DC: %s: SI Change event received for 0x%p\n",
                        dc->name, dc);

                // Set the new PID's for the DII and modules
                retCode = ocuTranslateAssociationTag(siHandle,
                        dc->associationTag, &(dc->pid));
                if (retCode != MPE_SUCCESS)
                {
                    MPE_LOG(
                            MPE_LOG_ERROR,
                            MPE_MOD_FILESYS,
                            "DC: %s: Couldn't Translate association tag: %04x\n",
                            dc->name, dc->associationTag);
                    dc->pid = DC_INVALID_PID;
                }
                for (j = 0; j < dc->numModules; j++)
                {
                    retCode = dcSetModulePid(dc->modules[j]);
                    if (retCode != MPE_SUCCESS)
                    {
                        MPE_LOG(
                                MPE_LOG_ERROR,
                                MPE_MOD_FILESYS,
                                "DC: %s: Couldn't Translate association tag: %04x\n",
                                dc->name, dc->modules[j]->assocTag);
                        dc->modules[j]->pid = DC_INVALID_PID;
                    }
                }

                //dcReStartDCRequests(dc);

                // dcReStartDCRequests does two things
                // 1. Cancel Section filters
                // 2. Start pre-fetching

                // Instead of restarting here do the following
                // 1. Cancel Section filters (call dcCancelSectionFilter())
                // 2. Stop CA session (set 'authorized' to false)
                // 3. Setup new CA session (PMT could have changed)
                // 4. Synchronously if no session is required, start pre-fetching
                // 5. If session is required defer until authorization is granted
                // 6. In HandleCaEnable() check if connected is true and authorized is false
                // 7. If so start pre-fetch (call dcStartPrefetch())

                dcCancelFilterRequests(dc);

                // Stop CA session
                mpe_podStopDecrypt(dc->decryptSessionHandle);
                dc->authorized = FALSE;
                dc->decryptSessionHandle = 0;

                // Re-start CA session
                {
                    retCode = ocuStartCASession( dc->params.t.ib.tunerId, dc->siHandle,
                                                 dc->pid, dcGlobalEventQueue, &(dc->decryptSessionHandle) );
                    if (retCode != MPE_SUCCESS)
                    {
                        MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILESYS,
                                "DC: ocuStartCASession returned error %d\n", retCode);
                        goto UpdateSIHandleCleanup;
                    }

                    if (dc->decryptSessionHandle != NULL)
                    {
                        MPE_LOG(MPE_LOG_INFO,
                                MPE_MOD_FILESYS,
                                "DC: %s: CA not yet established - deferring till CA is authorized...\n",
                                dc->name );
                        // We'll be called again when authorization has been established
                        goto UpdateSIHandleCleanup;
                    }
                    // Assert: No CA required (dc->decryptSessionHandle == NULL)
                    dc->authorized = true;

                    // Re-start the prefetch
                    dcRestartPrefetch(dc);

                    // Re-start update watcher
                    dcRestartUpdateWatcher(dc);
                }
            }
        }
    }

    UpdateSIHandleCleanup:

    mpe_mutexRelease(dcGlobalMutex);
    // END CRITICAL SECTION
}

static mpe_Error dcSetModulePid(mpe_DcModule *module)
{
    mpe_DataCarousel *dc;
    mpe_Error retCode = MPE_SUCCESS;

    dc = module->dc;

    // Get the PID
    if (module->assocTag == dc->associationTag)
    {
        // Most likely it's the same as in the DII, so just grab that if the association tags are
        // the same.
        module->pid = dc->pid;
    }
    else
    {
        retCode = ocuTranslateAssociationTag(module->dc->siHandle,
                module->assocTag, &(module->pid));
        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_FILESYS,
                    "DC: %s: Unable to translate association tag for module %04x.  Tag: %04x\n",
                    module->dc->name, module->moduleId, module->assocTag);
        }
    }
    return retCode;
}

/**
 * Function to process CA-related events from the main DC thread
 */
static mpe_Error dcHandleCaEvent( mpe_Event event,
                                  mpe_PODDecryptSessionHandle caSessionHandle,
                                  uint32_t eventData2,
                                  uint32_t eventData3 )
{
    int i;
    mpe_Error retCode = MPE_SUCCESS;
    mpe_DataCarousel *dc = NULL;

    MPE_LOG( MPE_LOG_DEBUG,
             MPE_MOD_FILESYS,
             "DC: dcHandleCaEvent(event 0x%x. handle 0x%p, data2 0x%08x, data3 0x%08x)\n",
             event, caSessionHandle, eventData2, eventData3 );

    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(dcGlobalMutex);

    // Walk the DCs and find the one corresponding with the associated handle
    for (i = 0; i < DC_MAX_DATACAROUSELS; i++)
    {
        if ( (dcDataCarousels[i] != NULL)
             && (dcDataCarousels[i]->decryptSessionHandle == caSessionHandle) )
        {
            dc = dcDataCarousels[i];
            MPE_LOG( MPE_LOG_DEBUG,
                     MPE_MOD_FILESYS,
                     "DC: %s: Processing CA event 0x%08x\n",
                     dc->name, event );
            break;
        }
    }

    if (dc == NULL)
    {
        MPE_LOG( MPE_LOG_WARN,
                 MPE_MOD_FILESYS,
                 "DC: dcHandleCaEvent: Could not find DC for CA session handle 0x%p\n",
                 caSessionHandle );
        mpe_mutexRelease(dcGlobalMutex);
        return MPE_EINVAL;
    }

    switch (event)
    {
        case MPE_POD_DECRYPT_EVENT_FULLY_AUTHORIZED:
            if (dc->connected)
            {
                MPE_LOG( MPE_LOG_DEBUG,
                         MPE_MOD_FILESYS,
                         "DC: %s: Received FULLY_AUTHORIZED event\n",
                         dc->name );

                if(!dc->authorized)
                {
                	// This is done when PMT change is received
                	dc->authorized = true;
                    // Re-start the prefetch
                    dcRestartPrefetch(dc);

                    // Re-start update watcher
                    dcRestartUpdateWatcher(dc);
                }
                else
                {
                    dc->authorized = true;
                	dcReconnectDC(dc, dc->params.t.ib.tunerId);
                }
            }
            break;
        case MPE_POD_DECRYPT_EVENT_CANNOT_DESCRAMBLE_ENTITLEMENT:
        case MPE_POD_DECRYPT_EVENT_CANNOT_DESCRAMBLE_RESOURCES:
        case MPE_POD_DECRYPT_EVENT_MMI_PURCHASE_DIALOG:
        case MPE_POD_DECRYPT_EVENT_MMI_TECHNICAL_DIALOG:
        case MPE_POD_DECRYPT_EVENT_POD_REMOVED:
        case MPE_POD_DECRYPT_EVENT_RESOURCE_LOST:
        case MPE_POD_DECRYPT_EVENT_SESSION_SHUTDOWN:
            MPE_LOG( MPE_LOG_DEBUG,
                     MPE_MOD_FILESYS,
                     "DC: %s: Received event %s (0x%x) (not authorized)\n",
                     dc->name, mpe_podCAEventString(event), event );
            dc->authorized = false;
            break;
        default:
            MPE_LOG( MPE_LOG_WARN,
                     MPE_MOD_FILESYS,
                     "DC: %s: Received unknown event (0x%x)\n",
                     dc->name, event );
            dc->authorized = false;
            break;
    } // END switch (event)

    mpe_mutexRelease(dcGlobalMutex);
    // END CRITICAL SECTION

    return retCode;
} // END dcHandleCaEvent()


/**
 * Main routine for the asynchronous listener thread.  Loops waiting for events, and
 * hands the events off to the appropriate handlers.
 *
 * @param data Ignored.
 */
static
void dcThreadAsync(void *data)
{
    mpe_Event eventID;
    void *eventData;
    uint32_t eventData2, eventData3;
    mpe_TimeMillis now;
    uint32_t delay;
    mpe_Error retCode;
    DcRequestRecord *timeoutReq;
    mpe_Bool quit = false;
    uint32_t tunerId;
    mpe_SiServiceHandle siH;
    mpe_PODDecryptSessionHandle caSessionHandle;

    MPE_UNUSED_PARAM(data);

    MPE_LOG(MPE_LOG_INFO, MPE_MOD_FILESYS, "DC: Watcher thread started\n");

    // Register to receive tuning events
    retCode = mpeos_mediaRegisterQueueForTuneEvents(dcGlobalEventQueue);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILESYS,
                "DC: Watcher thread could not register for tune events.  Ignoring\n");
    }

    // Register to receive si events
    retCode = mpe_siRegisterQueueForSIEvents(dcGlobalEventQueue);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILESYS,
                "DC: Watcher thread could not register for si events.  Ignoring\n");
    }

    // Do forever
    while (!quit)
    {
        // Figure out what time it is.
        mpe_timeGetMillis(&now);

        // BEGIN CRITICAL SECTION
        mpe_mutexAcquire(dcGlobalMutex);

        // Process any timeout events which have occurred in the past
        while (dcEarliestTimeout != 0 && dcEarliestTimeout < now)
        {
            // If the request we need to timeout is a DII Polling request
            // then trigger the poll
            if (dcEarliestTimeoutReq->reqType == DII_POLL)
            {
                dcPollDII(dcEarliestTimeoutReq->dc);
            }
            else if (dcEarliestTimeoutReq->timeout <= now
                    && dcEarliestTimeoutReq->timeout != 0)
            {
                // If the earliest timeout request is still before now, and not 0, time it out
                dcTimeoutRequest(dcEarliestTimeoutReq);
            }
            // Get the next earliest one.
            dcSetNextTimeout();
        }

        timeoutReq = dcEarliestTimeoutReq;

        // Figure out how long to stall
        if (timeoutReq != NULL)
        {
            // Timeout period is the difference between the future, and now
            delay = (uint32_t)(timeoutReq->timeout - now);
        }
        else
        {
            delay = 0;
        }

        mpe_mutexRelease(dcGlobalMutex);
        // END CRITICAL SECTION

        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                "DC: Setting next loop delay to %u milliseconds\n", delay);

        // Clear out the event ID
        eventID = 0;

        // Wait for the next event
        retCode = mpe_eventQueueWaitNext(dcGlobalEventQueue, &eventID,
                &eventData, (void**)&eventData2, &eventData3, delay);

        // Make sure we got an event.  If not, we timed out on something, and we'll go around the
        // loop again, and check to see if anything should be handled
        if (retCode == MPE_SUCCESS)
        {
            while (retCode == MPE_SUCCESS)
            {
                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                        "DC: Got Event from Global Queue: %04x\n", eventID);
                // Now, process the event
                switch (eventID)
                {
                case MPE_TUNE_SYNC:
                    tunerId = (uint32_t) eventData;
                    MPE_LOG(
                            MPE_LOG_DEBUG,
                            MPE_MOD_FILESYS,
                            "DC: Got Tune Sync. Tuner %d. Attempting to reattach carousels\n",
                            tunerId);
                    dcReconnectCarousels(tunerId);
                    break;
                case MPE_TUNE_UNSYNC:
                case MPE_TUNE_STARTED:
                    tunerId = (uint32_t) eventData;
                    MPE_LOG(
                            MPE_LOG_DEBUG,
                            MPE_MOD_FILESYS,
                            "DC: Got Tune Started.  Tuner %d.  Attempting to disconnect carousels\n",
                            tunerId);
                    dcDisconnectCarousels(tunerId);
                    break;
                case MPE_TUNE_ABORT:
                case MPE_TUNE_FAIL:
                case MPE_CONTENT_PRESENTING:
                case MPE_FAILURE_UNKNOWN:
                case MPE_STILL_FRAME_DECODED:
                case MPE_EVENT_SHUTDOWN:
                    // Ignore these events.  We don't process them.
                    MPE_LOG(
                            MPE_LOG_DEBUG,
                            MPE_MOD_FILESYS,
                            "DC: Received superfluous media event 0x%x.  Ignoring\n",
                            eventID);
                    break;
                case MPE_POD_DECRYPT_EVENT_FULLY_AUTHORIZED:
                case MPE_POD_DECRYPT_EVENT_CANNOT_DESCRAMBLE_ENTITLEMENT:
                case MPE_POD_DECRYPT_EVENT_CANNOT_DESCRAMBLE_RESOURCES:
                case MPE_POD_DECRYPT_EVENT_MMI_PURCHASE_DIALOG:
                case MPE_POD_DECRYPT_EVENT_MMI_TECHNICAL_DIALOG:
                case MPE_POD_DECRYPT_EVENT_POD_REMOVED:
                case MPE_POD_DECRYPT_EVENT_RESOURCE_LOST:
                case MPE_POD_DECRYPT_EVENT_SESSION_SHUTDOWN:
                    caSessionHandle = (mpe_PODDecryptSessionHandle) eventData;
                    MPE_LOG(MPE_LOG_DEBUG,
                            MPE_MOD_FILESYS,
                            "DC: Received pod event 0x%x (CA session 0x%p)\n",
                            eventID, caSessionHandle);
                    dcHandleCaEvent( eventID, caSessionHandle,
                                     eventData2, eventData3 );
                    break;
                case DC_CHECK_TIMEOUT:
                    // Do nothing.  Just go around the loop again
                    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                            "DC: Got Check New Timeout event\n");
                    break;
                case MPE_SI_EVENT_IB_PAT_ACQUIRED:
                case MPE_SI_EVENT_IB_PMT_ACQUIRED:
                case MPE_SI_EVENT_OOB_PAT_ACQUIRED:
                case MPE_SI_EVENT_OOB_PMT_ACQUIRED:
                case MPE_SI_EVENT_IB_PAT_UPDATE:
                case MPE_SI_EVENT_OOB_PAT_UPDATE:
                    // Ignore these events.  We don't process them.
                    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                            "DC: Got SI event...  Ignoring\n");
                    break;
                    // Fix for bug #1381 (Listen for SI Change events)
                case MPE_SI_EVENT_OOB_PMT_UPDATE:
                case MPE_SI_EVENT_IB_PMT_UPDATE:
                    // SI handle for which PMT changed is in eventData
                    siH = (mpe_SiServiceHandle) eventData;
                    MPE_LOG(
                            MPE_LOG_DEBUG,
                            MPE_MOD_SI,
                            "DC: Got MPE_SI_EVENT_IB_PMT_UPDATE event for siHandle 0x%x\n",
                            siH);
                    dcUpdateSIHandle(siH);
                    break;
                default:
                    dcHandleSectionEvent(eventID, (uint32_t) eventData);
                    break;
                }
                // Check to see if another event is ready.  If so, we'll grab it and not go through the
                // effort of timing anything out.
                retCode = mpe_eventQueueNext(dcGlobalEventQueue, &eventID,
                        &eventData, NULL, NULL);
            }
        }
        else
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                    "DC: No event received.  Got a timeout.\n");
        }
    }
}

static
void dcThreadMopup(void *data)
{
    mpe_Error retCode;
    mpe_Event eventID;
    void *eventData;
    mpe_DcModule *module;
    uint32_t i;

    MPE_UNUSED_PARAM(data);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "DC: Mopup thread starting\n");
    // Loop forever
    while (true)
    {
        // Wait for the next event
        retCode = mpe_eventQueueWaitNext(dcGlobalMopupQueue, &eventID,
                &eventData, NULL, NULL, 0);
        // Some error checks
        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILESYS,
                    "DC: Error when retrieving event from mopup queue: %04x\n",
                    retCode);
            continue;
        }
        if (eventID != DC_MOPUP_MODULE)
        {
            MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILESYS,
                    "DC: Unknown event encountered on mopup thread: %04x\n",
                    eventID);
            continue;
        }
        if (eventData == NULL)
        {
            MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILESYS,
                    "DC: No module specified to mopup thread\n");
            continue;
        }
        // Ok, let's do the actual mopup now.
        module = (mpe_DcModule *) eventData;
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                "DC: %s: Mopup Thread processing module %04x\n",
                module->dc->name, module->moduleId);

        if (module->valid && module->dc->mounted)
        {
            for (i = 0; i < module->numDDBs; i++)
            {
                if (module->ddbs[i].sectionHandle == NULL)
                {
                    MPE_LOG(
                            MPE_LOG_DEBUG,
                            MPE_MOD_FILESYS,
                            "DC: %s: Mopup Thread prefetching Module %04x[%d]\n",
                            module->dc->name, module->moduleId, i);

                    (void) dcFetchDDB(module, i, DDB_PREFETCH,
                            dcGlobalEventQueue, dcCalculateTimeout(
                                    module->ddbTimeout));
                }
            }
        }
        // Now I'm done with the module.
        // Have to release both the DC and the module
        dcReleaseDC(module->dc);
        mpe_dcReleaseModule(module);
    }
}

/*-----------------------------------------------------------------------------------------------*
 * Here follows code to deal with ZLib.  This should probably be broken out into a separate file,
 * but it uses too much static stuff in the data carousel.
 *-----------------------------------------------------------------------------------------------*/

/**
 * Helper functions for ZLib.
 */
static
void *
dcZAlloc(void *opaque, uint32_t items, uint32_t size)
{
    mpe_Error retCode;
    void *data;

    MPE_UNUSED_PARAM(opaque);

    retCode = mpeos_memAllocP(MPE_MEM_FILE_CAROUSEL, (items * size), &data);

    if (retCode != MPE_SUCCESS)
    {
        return (void *) Z_NULL;
    }
    else
    {
        return data;
    }
}

static
void dcZFree(void *opaque, void *data)
{
    MPE_UNUSED_PARAM(opaque);

    mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, data);
}

#define BYTES_AVAIL(x)  (dcDecompressionSize - x->zStream.avail_out)

static mpe_Error dcUncompressBytes(DcCompressedModule *compMod)
{
    uint32_t bytesRead;
    uint32_t bytesToRead;
    mpe_Error retCode;
    int zRetCode;

    // First, see if we need to read a new data buffer
    if (compMod->zStream.avail_in == 0)
    {
        MPE_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_FILESYS,
                "DC: %s: Module %04x: Reading compressed data block at offset %d\n",
                compMod->module->dc->name, compMod->module->moduleId,
                compMod->compOffset);
        // Nothing available, read in the next chunk
        bytesToRead = MIN(dcDecompressionSize, compMod->module->moduleSize
                - compMod->compOffset);
        retCode = dcReadRawModuleBytes(compMod->module, compMod->compOffset,
                bytesToRead, compMod->module->ddbTimeout, compMod->inBuffer,
                &bytesRead);
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                "DC: %s: Read complete: %d bytes read\n",
                compMod->module->dc->name, bytesRead);
        // Make sure we read things right
        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(
                    MPE_LOG_WARN,
                    MPE_MOD_FILESYS,
                    "DC: %s: Module %04x: Unable to read uncompressed bytes at offset %d\n",
                    compMod->module->dc->name, compMod->module->moduleId,
                    compMod->compOffset);
            return retCode;
        }

        // We're good.
        // Reset the read pointer to the start of the buffer
        compMod->zStream.avail_in = bytesRead;
        compMod->zStream.next_in = compMod->inBuffer;
        // Move the compressed offset
        compMod->compOffset += bytesRead;
    }

    // Next, let's see if we need to reset the output buffer
    if (compMod->zStream.avail_out == 0)
    {
        // Reset the output buffer
        compMod->zStream.next_out = compMod->outBuffer;
        compMod->zStream.avail_out = dcDecompressionSize;

        // Move the uncompressed offset.  Always moves it dcDecompressionSize
        // bounds.
        compMod->uncompOffset += dcDecompressionSize;

        MPE_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_FILESYS,
                "DC: %s: Module %04x: Set new uncompressed data block at offset %d\n",
                compMod->module->dc->name, compMod->module->moduleId,
                compMod->uncompOffset);
    }

    // Uncompress it
    zRetCode = inflate(&(compMod->zStream), Z_SYNC_FLUSH);

    // Make sure it uncompressed reasonably
    if (zRetCode != Z_OK && zRetCode != Z_STREAM_END)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "DC: %s: Module %04x: Zlib Inflate Failed: Error code: %d\n",
                compMod->module->dc->name, compMod->module->moduleId, zRetCode);
        return MPE_FS_ERROR_FAILURE;
    }
    else
    {
        MPE_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_FILESYS,
                "DC: %s: Module %04x: Zlib Inflate succeeded.  Offset %d (%d).  Available: %d\n",
                compMod->module->dc->name, compMod->module->moduleId,
                compMod->uncompOffset, compMod->compOffset,
                BYTES_AVAIL(compMod));
    }
    return MPE_SUCCESS;
}

static mpe_Error dcSeekCompressedModule(DcCompressedModule *compMod,
        uint32_t offset)
{
    mpe_Error retCode;
    int zRetCode;

    // Check to if we've allocated the underlying structure
    if (!compMod->inited)
    {
        retCode = dcInitCompressedModule(compMod);
        if (retCode != MPE_SUCCESS)
        {
            return retCode;
        }
    }

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_FILESYS,
            "DC: %s: Module %04x: Seeking to uncompressed location %d (%d -- %d)\n",
            compMod->module->dc->name, compMod->module->moduleId, offset,
            compMod->uncompOffset, compMod->compOffset);

    if (offset >= compMod->module->originalSize)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_FILESYS,
                "DC: %s: Module %04x: Unable to seek to %d in compressed module.  Original data sizes %d\n",
                compMod->module->dc->name, compMod->module->moduleId, offset,
                compMod->module->originalSize);
        return MPE_EINVAL;
    }

    // Check to see if we're seeking something earlier than where
    // we're currently uncompressing
    if (offset < compMod->uncompOffset)
    {
        // Reset the read pointer to the beginning and
        // start decompressing again.
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                "DC: %s: Module %04x: Resetting compressed stream\n",
                compMod->module->dc->name, compMod->module->moduleId);

        compMod->uncompOffset = 0;
        compMod->compOffset = 0;
        compMod->zStream.avail_out = dcDecompressionSize;
        compMod->zStream.next_out = compMod->outBuffer;
        compMod->zStream.avail_in = 0;
        compMod->zStream.next_in = compMod->inBuffer;
        zRetCode = inflateReset(&(compMod->zStream));
        // Make sure it happened.
        if (zRetCode != Z_OK)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                    "DC: %s: Module %04x: ZLib inflateReset failed\n",
                    compMod->module->dc->name, compMod->module->moduleId);
            return MPE_FS_ERROR_FAILURE;
        }
    }

    // Now, uncompress data until the end of the buffer is after the
    // offset at which we wish to read.
    while ((compMod->uncompOffset + BYTES_AVAIL(compMod)) < offset)
    {
        retCode = dcUncompressBytes(compMod);
        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_FILESYS,
                    "DC: %s: Module %04x: Decompression failed during seek near offset %d %d\n",
                    compMod->module->dc->name, compMod->module->moduleId,
                    compMod->compOffset, compMod->uncompOffset);
            return retCode;
        }
    }

    // Must be good.
    //
    return MPE_SUCCESS;
}

static mpe_Error dcReadCompressedBytes(DcCompressedModule *compMod,
        uint32_t start, uint32_t numBytes, uint8_t *bytes,
        uint32_t *totalBytesRead)
{
    mpe_Error retCode;
    uint32_t destOffset;
    uint32_t startOffset;
    uint32_t copyBytes;

    *totalBytesRead = 0;

    // Check to if we've allocated the underlying structure
    if (!compMod->inited)
    {
        retCode = dcInitCompressedModule(compMod);
        if (retCode != MPE_SUCCESS)
        {
            return retCode;
        }
    }

    /*
     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "DC: %s: Read %d bytes at offset %d in compressed module %04x\n",
     compMod->module->dc->name,
     numBytes,
     start,
     compMod->module->moduleId);
     */

    // See if we should be reading in this buffer
    if (start < compMod->uncompOffset || start > (compMod->uncompOffset
            + BYTES_AVAIL(compMod)))
    {
        // Nope, better seek to the right place.
        retCode = dcSeekCompressedModule(compMod, start);
        if (retCode != MPE_SUCCESS)
        {
            return retCode;
        }
    }

    // assert(start >= compMod->uncompOffset && start < (compMod->uncompOffset + BYTES_AVAIL(compMod)))

    destOffset = 0;

    while (numBytes > 0)
    {
        startOffset = start - compMod->uncompOffset;
        if (startOffset > BYTES_AVAIL(compMod))
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                    "DC: %s: Module %04x: Pointer in wrong place %d %d\n",
                    compMod->module->dc->name, compMod->module->moduleId,
                    compMod->uncompOffset, BYTES_AVAIL(compMod));
            return MPE_EINVAL;
        }

        // Number of bytes to copy this time
        copyBytes = MIN(numBytes, BYTES_AVAIL(compMod) - startOffset);
        /*
         MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "DC: %s: Module %04x: Copying %d bytes at offset %d\n",
         compMod->module->dc->name,
         compMod->module->moduleId,
         copyBytes,
         startOffset);
         */

        // Copy from the uncompressed buffer
        memcpy(&bytes[destOffset], &compMod->outBuffer[startOffset], copyBytes);

        // Move the pointer
        numBytes -= copyBytes;
        *totalBytesRead += copyBytes;
        start += copyBytes;
        destOffset += copyBytes;

        //
        if (numBytes > 0)
        {
            retCode = dcUncompressBytes(compMod);
            if (retCode != MPE_SUCCESS)
            {
                MPE_LOG(
                        MPE_LOG_ERROR,
                        MPE_MOD_FILESYS,
                        "DC: %s: Module %04x: Decompression failed during read near offset %d %d\n",
                        compMod->module->dc->name, compMod->module->moduleId,
                        compMod->compOffset, compMod->uncompOffset);
                return retCode;
            }
        }
    }
    return MPE_SUCCESS;
}

static mpe_Error dcNewCompressedModule(mpe_DcModule *module,
        DcCompressedModule **out)
{
    mpe_Error retCode;
    DcCompressedModule *compMod;

    // Clear out the return value.  Just in case.
    *out = NULL;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
            "DC: %s: Creating compressed module structure for %04x\n",
            module->dc->name, module->moduleId);

    // Allocate up the buffer.
    retCode = mpeos_memAllocP(MPE_MEM_FILE_CAROUSEL,
            sizeof(DcCompressedModule), (void **) &compMod);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_FILESYS,
                "DC: %s: Could not allocate compressed module structure %04x\n",
                module->dc->name, module->moduleId);
        return retCode;
    }
    // Not yet initialized
    compMod->inited = false;

    // Clear out the pointers, temporarily.
    compMod->inBuffer = NULL;
    compMod->outBuffer = NULL;

    // Point to the underlying module
    compMod->module = module;

    // Mark it as a compressed module
    compMod->compressedProxy = true;

    // Set the output value
    *out = compMod;
    // And we're outta here
    return MPE_SUCCESS;
}

static mpe_Error dcInitCompressedModule(DcCompressedModule *compMod)
{
    mpe_Error retCode;
    int zRetCode;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
            "DC: %s: Initializing compressed module structure for %04x\n",
            compMod->module->dc->name, compMod->module->moduleId);

    // Allocate the input and output buffers.
    retCode = mpeos_memAllocP(MPE_MEM_FILE_CAROUSEL, dcDecompressionSize,
            (void **) &(compMod->inBuffer));
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_FILESYS,
                "DC: %s: Could not allocate compressed module %d byte input buffer for module %04x\n",
                compMod->module->dc->name, dcDecompressionSize,
                compMod->module->moduleId);
        goto CleanUp;
    }
    retCode = mpeos_memAllocP(MPE_MEM_FILE_CAROUSEL, dcDecompressionSize,
            (void **) &(compMod->outBuffer));
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_FILESYS,
                "DC: %s: Could not allocate compressed module %d byte output buffer for module %04x\n",
                compMod->module->dc->name, dcDecompressionSize,
                compMod->module->moduleId);
        goto CleanUp;
    }

    // Fill in the zStream structure
    compMod->zStream.zalloc = (ZALLOC_FUNCPTR) dcZAlloc;
    compMod->zStream.zfree = dcZFree;

    compMod->zStream.next_in = compMod->inBuffer;
    compMod->zStream.next_out = compMod->outBuffer;

    compMod->zStream.avail_in = 0;
    compMod->zStream.avail_out = dcDecompressionSize;

    // fill in compressed module fields
    compMod->uncompOffset = 0;
    compMod->compOffset = 0;

    // Initialize the zLib
    zRetCode = inflateInit(&(compMod->zStream));

    if (zRetCode != Z_OK)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "DC: %s: ZLib InflateInit failed (%d) for Module %04x\n",
                compMod->module->dc->name, zRetCode, compMod->module->moduleId);
        mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, compMod);
        return MPE_FS_ERROR_FAILURE;
    }

    // And we're good
    compMod->inited = true;

    // Return the module
    return MPE_SUCCESS;

    CleanUp: mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, compMod->inBuffer);
    mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, compMod->outBuffer);
    mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, compMod);
    return retCode;
}

static
void dcFreeCompressedModule(DcCompressedModule *compMod)
{
    int zRetCode;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
            "DC: %s: Freeing compressed module %04x.  Decompressor %s\n",
            compMod->module->dc->name, compMod->module->moduleId,
            (compMod->inited) ? "initialized" : "not initialized");

    // Check to see if the internal structures have been inited
    if (compMod->inited)
    {
        // Shut down the compressor
        zRetCode = inflateEnd(&(compMod->zStream));
        if (zRetCode != Z_OK)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                    "DC: %s: ZLib InflateEnd failed (%d) for Module %04x\n",
                    compMod->module->dc->name, zRetCode,
                    compMod->module->moduleId);
        }
        mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, compMod->inBuffer);
        mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, compMod->outBuffer);
    }
    mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, compMod);
}

/**
 * Precompress a module.
 */
static mpe_Error dcPreDecompressModule(mpe_DcModule *module)
{
    uint8_t *outBuffer = NULL;
    uint8_t *inBuffer = NULL;
    uint32_t bytesLeft;
    uint32_t offset = 0;
    uint32_t bytesRead;
    z_stream zStream;
    int zRetCode;
    mpe_Bool inflateInited = FALSE;
    mpe_Error retCode = MPE_SUCCESS;

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_FILESYS,
            "DC: %s: Creating predecompressed buffer for module %04x, %d bytes\n",
            module->dc->name, module->moduleId, module->originalSize);

    // Allocate up the input and output buffers
    retCode = mpeos_memAllocP(MPE_MEM_FILE_CAROUSEL, module->originalSize,
            (void **) &outBuffer);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_FILESYS,
                "DC: %s: Unable to allocate %d byte predecompressed buffer for %04x\n",
                module->dc->name, module->originalSize, module->moduleId);
        goto CleanUp;
    }

    retCode = mpeos_memAllocP(MPE_MEM_FILE_CAROUSEL, dcDecompressionSize,
            (void **) &inBuffer);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "DC: %s: Unable to allocate %d byte input buffer for %04x\n",
                module->dc->name, dcDecompressionSize, module->moduleId);
        goto CleanUp;
    }

    // Fill in the zStream structure
    zStream.zalloc = (ZALLOC_FUNCPTR) dcZAlloc;
    zStream.zfree = dcZFree;

    zStream.next_in = inBuffer;
    zStream.next_out = outBuffer;

    zStream.avail_in = 0;
    zStream.avail_out = module->originalSize;

    // Initialize the zLib
    zRetCode = inflateInit(&zStream);

    if (zRetCode != Z_OK)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "DC: %s: ZLib InflateInit failed (%d) for Module %04x\n",
                module->dc->name, zRetCode, module->moduleId);
        retCode = MPE_FS_ERROR_FAILURE;
        goto CleanUp;
    }
    inflateInited = TRUE;

    bytesLeft = module->moduleSize;
    // Loop over the bytes, reading them, and decompressing them into the buffer
    while (bytesLeft > 0)
    {
        MPE_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_FILESYS,
                "DC: %s: Module %04x: Predecompressing %d bytes at offset %d\n",
                module->dc->name, module->moduleId, MIN(bytesLeft,
                        dcDecompressionSize), offset);

        retCode = dcReadRawModuleBytes(module, offset, MIN(bytesLeft,
                dcDecompressionSize), module->ddbTimeout, inBuffer, &bytesRead);
        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_FILESYS,
                    "DC: %s: Module %04x: Unable to read %d bytes at offset %d\n",
                    module->dc->name, module->moduleId, MIN(bytesLeft,
                            dcDecompressionSize), offset);
            goto CleanUp;
        }

        zStream.next_in = inBuffer;
        zStream.avail_in = bytesRead;
        zRetCode = inflate(&zStream, Z_SYNC_FLUSH);

        // Make sure it uncompressed reasonably
        if (zRetCode != Z_OK && zRetCode != Z_STREAM_END)
        {
            MPE_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_FILESYS,
                    "DC: %s: Module %04x: Zlib Inflate Failed: Error code: %d\n",
                    module->dc->name, module->moduleId, zRetCode);
            goto CleanUp;
        }

        // Move the pointers
        offset += bytesRead;
        bytesLeft -= bytesRead;
    }

    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(dcGlobalMutex);

    if (module->decompressedData == NULL)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                "DC: %s: Module %04x: Inserting predecompressed buffer\n",
                module->dc->name, module->moduleId);
        module->decompressedData = outBuffer;
        outBuffer = NULL; // Mark as null so we don't delete it.
    }
    else
    {
        MPE_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_FILESYS,
                "DC: %s: Module %04x: Duplicate decompression.  Not inserting\n",
                module->dc->name, module->moduleId);
    }

    mpe_mutexRelease(dcGlobalMutex);
    // END CRITICAL SECTION

    CleanUp:
    // Shut down the compressor
    if (inflateInited)
    {
        zRetCode = inflateEnd(&zStream);
        if (zRetCode != Z_OK)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                    "DC: %s: ZLib InflateEnd failed (%d) for Module %04x\n",
                    module->dc->name, zRetCode, module->moduleId);
        }
    }
    // Free up structures
    if (inBuffer != NULL)
    {
        mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, inBuffer);
    }
    if (outBuffer != NULL)
    {
        mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, outBuffer);
    }
    return retCode;
}
