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

#include <mpeos_mem.h>
#include <mpeos_dbg.h>
#include <mpeos_dll.h>
#include <mpeos_util.h>

#include "os_gfx.h"         /* resolve os specific types */
#include "platform_gfxdfb.h"

#include <gfx/mpeos_screen.h>
#include <gfx/mpeos_surface.h>

/* DirectFB includes */
#include <misc/conf.h>
#include <core/gfxcard.h>
#include <core/layers.h>
#include <core/system.h>
#include <core/core_system.h>
#include <core/surfaces.h>

mpeos_GfxScreen _screen; /* unique instance of screen */

extern DFBResult (*dfb_layers_register_ptr)(GraphicsDevice *device,
        void *driver_data, DisplayLayerFuncs *funcs);

/* External function */
void GetGraphicsDriverFuncs(GraphicsDriverFuncs *graphics_driver_funcs);
void cs_funcs_populate(CoreSystemFuncs *cs_ptr);

/* Extra symbol prototypes */

/* First Structs */
DirectFB2External *port_dfb2ext;
DFBConfig *port_dfbConfig;

/* Then Functions */
DFBSurfacePixelFormat (*dfb_pixelformat_for_depth_ptr)(int depth);
CoreSurface *(*dfb_layer_surface_ptr)(const DisplayLayer *layer);
void (*dfb_surface_flip_buffers_ptr)(CoreSurface *surface);

DFBResult
        (*dfb_surface_create_preallocated_ptr)(int width, int height,
                DFBSurfacePixelFormat format, CoreSurfacePolicy policy,
                DFBSurfaceCapabilities caps, CorePalette *palette,
                void *front_data, void *back_data, int front_pitch,
                int back_pitch, CoreSurface **surface);
DFBResult (*dfb_surface_create_preallocated_video_ptr)(int width, int height,
        DFBSurfacePixelFormat format, CoreSurfacePolicy policy,
        DFBSurfaceCapabilities caps, CorePalette *palette, void *front_data,
        void *back_data, int front_pitch, int back_pitch,
        unsigned long native_surface_context, CoreSurface **surface);
DFBResult (*dfb_palette_create_ptr)(unsigned int size,
        CorePalette **ret_palette);
void (*dfb_palette_generate_rgb332_map_ptr)(CorePalette *palette);
DFBResult (*dfb_surface_set_palette_ptr)(CoreSurface *surface,
        CorePalette *palette);
FusionResult (*fusion_object_unref_ptr)(FusionObject *object);
// #define fusion_object_unref(x) ((*fusion_object_unref_ptr)(x))

static mpe_Error gfxDfbInit(void);

/**
 * <i>layerCallback()</i>
 * Interface to DirectFB : Callback to retrieve directfb layer information
 * The callback is executed for each layer present in the system. We only care about the
 * one labeled "PrimaryLayer"
 *
 * @param layer_id      the directfb layer id
 * @param desc          the layer description
 * @param dfb           the super interface - reset to null if there is an error
 */
static void layerCallback(DFBDisplayLayerID layer_id,
        DFBDisplayLayerDescription desc, IDirectFB *dfb)
{
    IDirectFBDisplayLayer *inf;
    DFBDisplayLayerConfig config;

    if (!strcmp(desc.name, "PrimaryLayer"))
    {
        if (dfb->GetDisplayLayer(dfb, layer_id, &inf) != DFB_OK)
        {
            return;
        }

        if (inf->GetConfiguration(inf, &config) != DFB_OK)
        {
            return;
        }

        /* update screen information */

        switch (config.pixelformat)
        {
        case DSPF_ARGB1555:
            _screen.colorFormat = MPE_GFX_ARGB1555;
            _screen.bitdepth = MPE_GFX_16BPP;
            break;
        case DSPF_RGB16:
            _screen.colorFormat = MPE_GFX_RGB565;
            _screen.bitdepth = MPE_GFX_16BPP;
            break;
        case DSPF_RGB24:
            _screen.colorFormat = MPE_GFX_RGB888;
            _screen.bitdepth = MPE_GFX_24BPP;
            break;
        case DSPF_RGB32:
            _screen.colorFormat = MPE_GFX_UNDEFINED;
            _screen.bitdepth = MPE_GFX_32BPP;
            break;
        case DSPF_ARGB:
            _screen.colorFormat = MPE_GFX_ARGB8888;
            _screen.bitdepth = MPE_GFX_32BPP;
            break;
        default: /* unsupported format */
            _screen.colorFormat = MPE_GFX_UNDEFINED;
        }
    }
}

/**
 * <i>dfb_gfxCreatePrimarySurface(mpe_GfxDimensions initialSize)</i>
 *
 * @param intialSize   Initial size of the single primary screen surface
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed,
 *         or MPE_GFX_ERROR_NOERR otherwise.
 */
// TODO: Something should be added here to associate the surface with
//       the display manager graphics device - for multi-screen support
mpe_Error dfb_gfxCreatePrimarySurface(mpe_GfxDimensions initialSize)
{
    mpe_Error err;
    mpe_GfxSurfaceInfo desc;

    MPEOS_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_GFX,
            "<<GFX>> dfb_gfxCreatePrimarySurface() - Setting up primary surface - %dx%d\n",
            initialSize.width, initialSize.height);

    _screen.x = 0;
    _screen.y = 0;
    _screen.width = initialSize.width;
    _screen.height = initialSize.height;

    err = gfxDfbInit();

    if (err != MPE_GFX_ERROR_NOERR)
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "<<GFX>> dfb_gfxCreatePrimarySurface() - ERROR - Failed to Init DirectFB\n");
        return err;
    }

    desc.bpp = _screen.bitdepth;
    desc.format = _screen.colorFormat;
    desc.pixeldata = NULL;
    desc.widthbytes = (_screen.width * _screen.bitdepth) / 8;
    desc.dim.height = _screen.height;
    desc.dim.width = _screen.width;

    _screen.surf = mpeos_gfxSurface(&desc, true);

    if (_screen.surf == NULL)
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "<<GFX>> dfb_gfxCreatePrimarySurface() - ERROR - mpeos_gfxSurface() failed\n");
        return MPE_GFX_ERROR_OSERR;
    }

    return MPE_GFX_ERROR_NOERR;
} // END dfb_gfxCreatePrimarySurface()

/**
 * <i>dfb_gfxResizePrimarySurface( mpe_GfxDimensions newSize )</i>
 * Resize the default screen surface to new dimensions.
 *
 * @param newSize   new dimensions for the primary graphics surface
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed,
 *         or MPE_GFX_ERROR_NOERR otherwise.
 */
// TODO: Something should be added here to identify the surface/device
//       we're operating upon
mpe_Error dfb_gfxResizePrimarySurface(mpe_GfxDimensions newSize)
{
    mpeos_GfxSurface *pSurface;
    IDirectFB *dfb = NULL;

    if (_screen.height == newSize.height && _screen.width == newSize.width)
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_GFX,
                "<<GFX>> dfb_gfxResizePrimarySurface() - Maintaining graphics surface size\n");
        return MPE_GFX_ERROR_NOERR;
    }

    MPEOS_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_GFX,
            "<<GFX>> dfb_gfxResizePrimarySurface() - Resizing primary surface to %dx%d\n",
            newSize.width, newSize.height);
    // first, update the current video mode in DirectFB which will trigger an
    // update to the simulator display
    dfb = _screen.osScr.dfb;
    if (dfb)
    {
        dfb->SetVideoMode(dfb, newSize.width, newSize.height, _screen.bitdepth);
    }

    // resize the graphics surface (DirectFB will trigger a primary surface update)
    pSurface = gfxSurfaceResize(_screen.surf, newSize.width, newSize.height);

    if (pSurface == NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_GFX,
                "<<GFX>> dfb_gfxResizePrimarySurface() - ERROR - Could not resize surface\n");
        return MPE_GFX_ERROR_OSERR;
    }

    // Success, update the relevant screen attributes
    _screen.height = newSize.height;
    _screen.width = newSize.width;
    _screen.widthbytes = (_screen.width * _screen.bitdepth) / 8;

    return MPE_GFX_ERROR_NOERR;
} // END dfb_gfxResizePrimarySurface()

/**
 * <i>mpeos_gfxGetScreen()</i>
 * Returns the address where the screen information is stored
 *
 * @return a pointer to <code>mpeos_GfxScreen</code>
 *
 */
mpeos_GfxScreen* mpeos_gfxGetScreen(void)
{
    return &_screen;
}

/**
 * Replacement malloc() for DirectFB.
 */
static void *dfbMalloc(size_t bytes)
{
    void *ptr;
    if ((errno = mpeos_memAllocP(MPE_MEM_GFX_LL, bytes, &ptr)) == MPE_SUCCESS)
        return ptr;
    else
        return NULL;
}

/**
 * Replacement realloc() for DirectFB.
 */
static void *dfbRealloc(void *mem, size_t bytes)
{
    if (mem == NULL)
        return dfbMalloc(bytes);
    if ((errno = mpeos_memReallocP(MPE_MEM_GFX_LL, bytes, &mem)) == MPE_SUCCESS)
        return mem;
    else
        return NULL;
}

/**
 * Replacement free() for DirectFB.
 */
static void dfbFree(void* mem)
{
    if (mem != NULL)
        mpeos_memFreeP(MPE_MEM_GFX_LL, mem);
}

/**
 * Replacement pthread_mutex_init() for DirectFB.
 * Initialize the mutex. The mutex kind is ignored since MPEOS does not allow
 * it to be specified. In MPEOS the kind is always recursive.
 */
static int dfbMutexInit(DFB2Ex_Mutex *mutex, void* mutexattr)
{
    MPE_UNUSED_PARAM(mutexattr);
    mpeos_mutexNew((mpe_Mutex*) mutex);
    return 0;
}

/**
 * Replacement pthread_mutex_lock() for DirectFB.
 * Lock the mutex
 */
static int dfbMutexLock(DFB2Ex_Mutex *mutex)
{
    if (*(mpe_Mutex*) mutex == NULL)
    {
        int result = mpeos_mutexNew((mpe_Mutex*) mutex);
        if (result != MPE_SUCCESS)
            return result;
    }
    return mpeos_mutexAcquire(*(mpe_Mutex*) mutex);
}

/**
 * Replacement pthread_mutex_trylock() for DirectFB.
 * Try to lock the mutex. Return an error instead of blocking if the mutex
 * cannot be aquired.
 */
static int dfbMutexTryLock(DFB2Ex_Mutex *mutex)
{
    if (*mutex == NULL)
    {
        int result = mpeos_mutexNew((mpe_Mutex*) mutex);
        if (result != MPE_SUCCESS)
            return result;
    }
    return mpeos_mutexAcquireTry(*(mpe_Mutex*) mutex);
}

/**
 * Replacement pthread_mutex_unlock() for DirectFB.
 * Unlock the mutex
 */
static int dfbMutexUnlock(DFB2Ex_Mutex *mutex)
{
    return mpeos_mutexRelease(*(mpe_Mutex*) mutex);
}

/**
 * Replacement pthread_mutex_destroy() for DirectFB.
 * Destroy the mutex
 */
static int dfbMutexDestroy(DFB2Ex_Mutex *mutex)
{
    return mpeos_mutexDelete(*(mpe_Mutex*) mutex);
}

/**
 * Wrapper for dbg_msgRaw().
 */
#define MAX_MESSAGE_LENGTH 512
static void dfbDebugPrintf(const char *format, ...)
{
    va_list args = NULL;
    char msg[MAX_MESSAGE_LENGTH];

    va_start(args, format);
    /*lint -e(418)*/
    vsnprintf(msg, MAX_MESSAGE_LENGTH-1, format, args);
    va_end(args);

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_GFX, msg, NULL);
}

/**
 * Initializes the DirectFB subsystem and returns a pointer to
 * the created <i>IDirectFB</i> interface object.
 *
 * @return the created <i>IDirectFB</i> interface object or <code>NULL</code>
 */
static IDirectFB* initDirectFB(void)
{
    static DirectFB2External funcs =
    { dfbMalloc, dfbRealloc, dfbFree, dfbMutexInit, dfbMutexLock,
            dfbMutexTryLock, dfbMutexUnlock, dfbMutexDestroy, dfbDebugPrintf };
    IDirectFB* dfb = NULL;

#define USE_DIRECTFB_THRU_DLL
#ifdef USE_DIRECTFB_THRU_DLL
    const char *dfbDllName;
    mpe_Dlmod dll = (mpe_Dlmod) NULL;
    /*lint -e(578)*/
    DFBResult (*DirectFBInit)(int *argc, char **argv[], DirectFB2External *ext);
    /*lint -e(578)*/
    DFBResult (*DirectFBCreate)(IDirectFB**);

    typedef void (*funcptr1)(GraphicsDriverFuncs *gd_funcs);
    funcptr1 f1ptr;
    void (*SetupDirectFB)(funcptr1 fptr, CoreSystemFuncs **cs_funcs);
    void (*dfb_get_structs_ptr)(void **sptr1, void **sptr2);
    CoreSystemFuncs *cs_funcsptr;

    f1ptr = GetGraphicsDriverFuncs;

    if (((dfbDllName = mpeos_envGet("DFBDLLPATH")) == NULL) || MPE_SUCCESS
            != mpeos_dlmodOpen(dfbDllName, &dll) || MPE_SUCCESS
            != mpeos_dlmodGetSymbol(dll, "DirectFBInit", (void*) &DirectFBInit)
            || MPE_SUCCESS != mpeos_dlmodGetSymbol(dll, "DirectFBCreate",
                    (void*) &DirectFBCreate))
    {
        MPEOS_LOG(
                MPE_LOG_WARN,
                MPE_MOD_GFX,
                "<<GFX>> Problems accessing DirectFB DLL, DirectFBInit or DirectFBCreate symbols!\n");
        return NULL;
    }
    /*
     * This following section of code is what allows the DirectFB port driver
     * to run while residing as part of the mpe tree.
     * There are 3 sections. The first call gets the function pointer to
     * GetGraphicsDriverFuncs() from MPE and stores it in DirectFB.
     * The next call gets the function pointer to dfb_layers_register()
     * from DirectFB and stores it under MPE. Finally, core system funcs need
     * to be dynamically set up in the static structure core_system_funcs
     * in core_system.h. These are basically all the system_ calls in the
     * driver.
     */
    if (MPE_SUCCESS != mpeos_dlmodGetSymbol(dll, "SetupDirectFB",
            (void *) &SetupDirectFB))
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "<<GFX>> Problem getting SetupDirectFB symbol from DirectFB!\n");
        return NULL;
    }
    if (MPE_SUCCESS != mpeos_dlmodGetSymbol(dll, "dfb_layers_register",
            (void *) &dfb_layers_register_ptr))
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "<<GFX>> Problem getting dfb_layers_register symbol from DirectFB!\n");
        return NULL;
    }
    /**
     * Finally get the extra struct pointers and function pointers that
     * the drivers use. Note that in future ports, more symbols may have to be
     * added here. Note that all symbols here are also in the directfb/exports
     * file. Note that this method only works for functions. To get structures,
     * a function has to be created that passes it back.
     */
    if (MPE_SUCCESS != mpeos_dlmodGetSymbol(dll, "dfb_get_structs",
            (void *) &dfb_get_structs_ptr) || MPE_SUCCESS
            != mpeos_dlmodGetSymbol(dll, "dfb_pixelformat_for_depth",
                    (void *) &dfb_pixelformat_for_depth_ptr) || MPE_SUCCESS
            != mpeos_dlmodGetSymbol(dll, "dfb_layer_surface",
                    (void *) &dfb_layer_surface_ptr) || MPE_SUCCESS
            != mpeos_dlmodGetSymbol(dll, "dfb_surface_flip_buffers",
                    (void *) &dfb_surface_flip_buffers_ptr) || MPE_SUCCESS
            != mpeos_dlmodGetSymbol(dll, "dfb_surface_create_preallocated",
                    (void *) &dfb_surface_create_preallocated_ptr)
            || MPE_SUCCESS != mpeos_dlmodGetSymbol(dll,
                    "dfb_surface_create_preallocated_video",
                    (void *) &dfb_surface_create_preallocated_video_ptr)
            || MPE_SUCCESS != mpeos_dlmodGetSymbol(dll, "dfb_palette_create",
                    (void *) &dfb_palette_create_ptr) || MPE_SUCCESS
            != mpeos_dlmodGetSymbol(dll, "dfb_palette_generate_rgb332_map",
                    (void *) &dfb_palette_generate_rgb332_map_ptr)
            || MPE_SUCCESS != mpeos_dlmodGetSymbol(dll,
                    "dfb_surface_set_palette",
                    (void *) &dfb_surface_set_palette_ptr) || MPE_SUCCESS
            != mpeos_dlmodGetSymbol(dll, "fusion_object_unref",
                    (void *) &fusion_object_unref_ptr))
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "<<GFX>> Problem getting extra symbols for DFB driver!\n");
        return NULL;
    }
#endif

    /* Pass in GetGraphicsDriverFuncs and get core_system_funcs back */
    (*SetupDirectFB)(f1ptr, &cs_funcsptr);
    /* Populate both mpe and dfb core_system_funcs structures */
    cs_funcs_populate(cs_funcsptr);

    /* Kind of have to know this...port_dfb2ext is really &funcs.
     * Set it up here and retrieve it as a test of the data passing
     * ability between the DLLs. Needed in DirectFBCreate.
     */
    port_dfb2ext = &funcs;

    if (DFB_OK != DirectFBInit(NULL, NULL, &funcs))
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "<<GFX>> Problem initializing DirectFB!\n");
        return NULL;
    }

    /* Get extra structs needed from DirectFB. Note that if more structs beyond
     * these are needed, then this function has to have more arguments put in it
     * and has to pass these structs for all ports. Note that this call has to
     * be after DirectFBInit and before DirectFBCreate.
     */
    (*dfb_get_structs_ptr)((void **) &port_dfb2ext, (void **) &port_dfbConfig);

    port_dfbConfig->mode.width = _screen.width;
    port_dfbConfig->mode.height = _screen.height;

    if (DFB_OK != DirectFBCreate(&dfb))
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "<<GFX>> Problems with DirectFBCreate!\n");
        return NULL;
    }

    if ((void *) port_dfb2ext != (void *) &funcs)
    {
        printf(
                "<<GFX>> Error, port_dfb2ext = %08lx, funcs = %08lx supposed to be equal!\n",
                (unsigned long) port_dfb2ext, (unsigned long) &funcs);
        return NULL;
    }

    return dfb;
}

/**
 * <i>cs_funcs_populate(CoreSystemFuncs *ptr)</i>
 * Populates both the local MPE and dfb (passed in) core_system_funcs variables
 * with valid functions.
 */
void cs_funcs_populate(CoreSystemFuncs *cs_ptr)
{
    /**
     * populate remote dfb core_system_funcs variable
     */
    cs_ptr->GetSystemInfo = system_get_info;
    cs_ptr->Initialize = system_initialize;
    cs_ptr->Join = system_join;
    cs_ptr->Shutdown = system_shutdown;
    cs_ptr->Leave = system_leave;
    cs_ptr->Suspend = system_suspend;
    cs_ptr->Resume = system_resume;
    cs_ptr->GetModes = system_get_modes;
    cs_ptr->GetCurrentMode = system_get_current_mode;
    cs_ptr->MapMMIO = system_map_mmio;
    cs_ptr->UnmapMMIO = system_unmap_mmio;
    cs_ptr->GetAccelerator = system_get_accelerator;
    cs_ptr->VideoMemoryPhysical = system_video_memory_physical;
    cs_ptr->VideoMemoryVirtual = system_video_memory_virtual;
    cs_ptr->VideoRamLength = system_videoram_length;

} /* cs_funcs_populate() */

/**
 * <i>gfxDfbInit()</i>
 * Performs directfb initializations and updates screen information
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed,
 *         or MPE_GFX_ERROR_NOERR otherwise.
 */
static mpe_Error gfxDfbInit(void)
{
    IDirectFB *dfb = NULL; /* The super interface */

    /* Initialize DirectFB and create main interface */
    if (NULL == (dfb = initDirectFB()))
    {
        return MPE_GFX_ERROR_OSERR;
    }
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_GFX, "DirectFB Initialized - OK\n");

    _screen.osScr.dfb = dfb;

    /* get the layer information so we can adjust the surface description */
    dfb->EnumDisplayLayers(dfb, (DFBDisplayLayerCallback) layerCallback,
            _screen.osScr.dfb);

    if (_screen.osScr.dfb == NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_GFX,
                "gfxDfbInit() - ERROR - unable to retrieve layer information\n");
        return MPE_GFX_ERROR_OSERR;
    }

    return MPE_GFX_ERROR_NOERR;
}
