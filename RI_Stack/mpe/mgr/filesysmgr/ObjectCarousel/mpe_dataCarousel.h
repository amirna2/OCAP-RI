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

#ifndef MPE_DATACAROUSEL_H
#define MPE_DATACAROUSEL_H

#include "mpe_filter.h"
#include "mpe_os.h"
#include "mpe_si.h"

#ifdef __cplusplus
"C"
{
#endif

    // Constants
#define DC_MAX_DATACAROUSELS        (32)

    // Event values
#define MPE_DC_UPDATE_EVENT         (0x4242)

#define MPE_DC_INVALID_MODULEID     (0xffffffff)
#define MPE_DC_INVALID_TRANSID      (0xffffffff)

    typedef enum
    {
        Cache_StreamOrCache,
        Cache_StreamOnly,
        Cache_CacheOnly
    }mpe_DcCacheMode;

    typedef struct mpe_DataCarousel mpe_DataCarousel;

    typedef struct mpe_DcModule mpe_DcModule;

    /** Public APIs **/

    /**
     * Initialize the data carousel module.  Start the thread to watch for changes.
     *
     * @return MPE_SUCCESS if the routine succeeds, failure otherwise.
     */
    mpe_Error mpe_dcInit(void);

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
     *
     * The next parameter is a hack to support PowerTV's nonstandard filtering for OOB DII's.
     * @param carouselID        For OOB, what is the carouselID of this DII.
     *
     * @returns MPE_SUCCESS if the carousel is correctly mounted, error codes if not.
     */
    mpe_Error mpe_dcMount(mpe_SiServiceHandle siHandle,
            uint16_t associationTag,
            uint32_t selector,
            uint32_t timeout,
#ifdef PTV
            uint32_t carouselID,
#endif
            mpe_DataCarousel **retDc);

    /**
     * Unmount an object carousel, and free the underlying data structures.
     * Note:  This does no reference counting, so the caller must be careful to make
     * sure that nobody else is using the carousel when it's unmounted.  All modules will
     * be unloaded and deallocated, and all global structures deallocated.
     *
     * @param dc The data carousel we wish to unmount.
     *
     * @returns MPE_SUCCESS if the carousel is correctly unmounted, error codes if not.
     */
    mpe_Error mpe_dcUnmount(mpe_DataCarousel * dc);

    /**
     * Return a handle to a module within the carousel, and increment it's
     * reference count.  This does not necessarily cause any data to be read from
     * the carousel.
     *
     * @param dc The data carousel to use.
     * @param moduleId The module ID to get.
     * @param module [out] Output parameter, will contain the module object.
     *
     * @returns MPE_SUCCESS if the module object is retrieved, MPE_EINVAL if the module ID
     *          doesn't exist.
     */
    mpe_Error mpe_dcGetModule(mpe_DataCarousel *dc, uint16_t moduleId, mpe_DcModule ** const module);

    /**
     * Return a handle to a module within the carousel, based on the name.  Searches based on the names in
     * the descriptors.  This does not necessarily cause any data to be read from the carousel.
     *
     * @param dc        The data carousel to use.
     * @param name      The name of the module to search for.
     * @param instance  The instance (starting at 0) of modules which have this name.
     * @param module    [out] Pointer to where to put a handle to the module object.
     *
     * @returns MPE_SUCCESS if the module object is retrievedd, MPE_EINVALI if the module
     * name and instance doesn't exist.
     */
    mpe_Error mpe_dcGetModuleByName(mpe_DataCarousel *dc, const char *name, uint32_t instance, mpe_DcModule ** const module);

    /**
     * Prefetch a module based on it's handle.
     *
     * @param module    The module to prefetch.
     *
     * @returns MPE_SUCCESS if the prefetch started (or the module is already prefetched).
     *          Error codes otherwise.
     */
    mpe_Error mpe_dcPrefetchModule(mpe_DcModule *module);

    /**
     * Prefetch a module (or modules) based on a name.
     *
     * @param module    The module to prefetch.
     * @param label     The name of the module to look for in the label descriptors.
     *
     * @returns MPE_SUCCESS if the prefetch started (or the module is already prefetched).
     *          MPE_NODATA if the name doesn't exist.
     */
    mpe_Error mpe_dcPrefetchModuleByName(mpe_DataCarousel *dc, const char *label);

    /**
     * Release a module and decrement it's reference count.  If it's reference count
     * goes to zero, and the module is no longer current, allow it to be deallocated.
     *
     * @param module The module to deallocate.
     *
     * @return MPE_SUCCESS
     */
    mpe_Error mpe_dcReleaseModule(mpe_DcModule *module);

    /**
     * Is the carousel connected?
     *
     * @param dc            The data carousel to query on.
     * @param connected     [out] Pointer to where to make the connection.
     *
     * @return MPE_SUCCESS
     */
    mpe_Error mpe_dcIsConnected(mpe_DataCarousel *dc, mpe_Bool *connected);

    /**
     * Return the size of a module.
     *
     * @param module    The module in question.
     * @param size      [out] Pointer to where to return the size.
     */

    mpe_Error mpe_dcGetModuleSize(mpe_DcModule *mod, uint32_t *size);

    /**
     * Return the version of a module.
     *
     * @param module    The module in question.
     * @param version   [out] Pointer to where to return the version.
     */
    mpe_Error mpe_dcGetModuleVersion(mpe_DcModule *mod, uint32_t *version);

    /**
     * Return the loaded status of a module.
     *
     * @param module    The module in question.
     * @param isLoaded  [out] Pointer to where to return the loaded status.
     */
    mpe_Error mpe_dcGetModuleLoadedStatus(mpe_DcModule *mod, mpe_Bool *isLoaded);

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
    mpe_Error mpe_dcUnloadModule(mpe_DcModule *module);

    /**
     * This function reads data from a module.  It performs all the necessary underlying
     * operations, including reading data in from the carousel (if necessary), and copying
     * data out of multiple DDB's into a single data block for output.  Since this operation
     * can cause data to be read in from the carousel, it is potentially very long running.
     * Blocking.
     *
     * @param module The module to read the bytes from.
     * @param start The starting location at which we wish to read.
     * @param numBytes The number of bytes to read from this location
     * @param timeout Time, in Milliseconds, to wait for any particular DDB to be found.
     * @param bytes A pointer to a byte array to fill with the bytes read.
     *              This array must be at least numBytes long.
     * @param bytesRead [out] Pointer to a variable where the number of bytes read will be written.
     *
     * @return MPE_SUCCESSS if the function succeeded, an appropriate error code if the
     *        function failed.
     */
    mpe_Error mpe_dcReadModuleBytes(mpe_DcModule *module,
            uint32_t start,
            uint32_t numBytes,
            uint32_t timeout,
            uint8_t *bytes,
            uint32_t *bytesRead);

    /**
     * Register an event queue to be notified when the carousel is updated.
     * Only the most recent set will be allowed.
     *
     * @param module The module to watch.
     * @param queue  The event queue to send the event too.
     * @param data   The data to use to inform caller with.
     *
     * @return MPE_SUCCESS if the event is successfully registered, error codes otherwise.
     */
    mpe_Error mpe_dcRegisterChangeNotification(mpe_DataCarousel *dc,
            mpe_EventQueue queue,
            void *data);

    /**
     * Unregister an previously registered notification queue
     *
     * @param module The module to watch.
     * @param queue  The event queue to unregister.  If this doesn't match the currently
     *               registered queue, no action is taken and an error is returned
     *
     * @return MPE_SUCCESS if the queue is successfully unregistered, error codes otherwise.
     */
    mpe_Error
    mpe_dcUnregisterChangeNotification(mpe_DataCarousel *dc,
            mpe_EventQueue queue);

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
    mpe_Error mpe_dcAddModuleUserData(mpe_DcModule *module, void *data, void (*function)(void *));

    /**
     * Return a block of memory which the user previously allocated.
     *
     * @param module        The module to get the block from.
     * @param data          [out] The data block.
     */
    mpe_Error mpe_dcGetModuleUserData(mpe_DcModule *module,
            void **data);

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
    mpe_Error mpe_dcCheckModuleCurrent(mpe_DcModule *module, mpe_Bool *current);

    /** Non-Public APIs **/

    /**
     * Is the carousel out of band (oob)?
     *
     * @param dc            The data carousel to query on.
     * @param connected     [out] Pointer to oob boolean.
     *
     * @return TRUE if the the data carousel is OOB.
     */
    mpe_Bool dc_IsOOB(mpe_DataCarousel *dc);

    /**
     * Assign a new SI Handle to this data carousel.
     *
     * @param   dc          The data carousel.
     * @param   siHandle    The new SI Handle.
     */
    mpe_Error dcSetSiHandle(mpe_DataCarousel *dc, mpe_SiServiceHandle siHandle);

#ifdef __cplusplus
}
#endif

#endif /* MPE_DATACAROUSEL_H */
