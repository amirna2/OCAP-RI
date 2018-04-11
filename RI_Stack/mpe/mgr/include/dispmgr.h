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

#ifndef _DISPMGR_H_
#define _DISPMGR_H_

#ifdef __cplusplus
extern "C"
{
#endif

#include <mpeos_gfx.h>
#include <mpeos_disp.h>

/***************************************************************
 * DISPLAY Manager function table definition
 ***************************************************************/

void mpe_dispSetup(void);
void mpe_dispInit(void);

typedef struct
{
    void (*mpe_disp_init_ptr)(void);

    /* UIEvent */
    mpe_Error
            (*mpe_gfxWaitNextEvent_ptr)(mpe_GfxEvent *event, uint32_t timeout);
    /* Context */
    mpe_Error (*mpe_gfxContextNew_ptr)(mpe_GfxSurface surface,
            mpe_GfxContext* ctx);
    mpe_Error (*mpe_gfxContextCreate_ptr)(mpe_GfxContext base,
            mpe_GfxContext* ctx);
    mpe_Error (*mpe_gfxContextDelete_ptr)(mpe_GfxContext ctx);
    mpe_Error (*mpe_gfxGetSurface_ptr)(mpe_GfxContext ctx,
            mpe_GfxSurface* surface);
    mpe_Error (*mpe_gfxGetColor_ptr)(mpe_GfxContext ctx, mpe_GfxColor* color);
    mpe_Error (*mpe_gfxSetColor_ptr)(mpe_GfxContext ctx, mpe_GfxColor color);
    mpe_Error (*mpe_gfxGetFont_ptr)(mpe_GfxContext ctx, mpe_GfxFont* font);
    mpe_Error (*mpe_gfxSetFont_ptr)(mpe_GfxContext ctx, mpe_GfxFont font);
    mpe_Error (*mpe_gfxGetPaintMode_ptr)(mpe_GfxContext ctx,
            mpe_GfxPaintMode* mode, uint32_t* data);
    mpe_Error (*mpe_gfxSetPaintMode_ptr)(mpe_GfxContext ctx,
            mpe_GfxPaintMode mode, uint32_t data);
    mpe_Error (*mpe_gfxGetOrigin_ptr)(mpe_GfxContext ctx, mpe_GfxPoint* point);
    mpe_Error (*mpe_gfxSetOrigin_ptr)(mpe_GfxContext ctx, int32_t x, int32_t y);
    mpe_Error (*mpe_gfxGetClipRect_ptr)(mpe_GfxContext ctx,
            mpe_GfxRectangle* rect);
    mpe_Error (*mpe_gfxSetClipRect_ptr)(mpe_GfxContext ctx,
            mpe_GfxRectangle* rect);
    /* Draw */
    mpe_Error (*mpe_gfxDrawLine_ptr)(mpe_GfxContext ctx, int32_t x1,
            int32_t y1, int32_t x2, int32_t y2);
    mpe_Error
            (*mpe_gfxDrawRect_ptr)(mpe_GfxContext ctx, mpe_GfxRectangle *rect);
    mpe_Error
            (*mpe_gfxFillRect_ptr)(mpe_GfxContext ctx, mpe_GfxRectangle *rect);
    mpe_Error (*mpe_gfxDrawEllipse_ptr)(mpe_GfxContext ctx,
            mpe_GfxRectangle *bounds);
    mpe_Error (*mpe_gfxFillEllipse_ptr)(mpe_GfxContext ctx,
            mpe_GfxRectangle *bounds);
    mpe_Error (*mpe_gfxDrawRoundRect_ptr)(mpe_GfxContext ctx,
            mpe_GfxRectangle *rect, int32_t arcWidth, int32_t arcHeight);
    mpe_Error (*mpe_gfxFillRoundRect_ptr)(mpe_GfxContext ctx,
            mpe_GfxRectangle *rect, int32_t arcWidth, int32_t arcHeight);
    mpe_Error (*mpe_gfxDrawArc_ptr)(mpe_GfxContext ctx,
            mpe_GfxRectangle *bounds, int32_t startAngle, int32_t endAngle);
    mpe_Error (*mpe_gfxFillArc_ptr)(mpe_GfxContext ctx,
            mpe_GfxRectangle *bounds, int32_t startAngle, int32_t endAngle);
    mpe_Error (*mpe_gfxDrawPolyline_ptr)(mpe_GfxContext context,
            int32_t* xCoords, int32_t* yCoords, int32_t nCoords);
    mpe_Error (*mpe_gfxDrawPolygon_ptr)(mpe_GfxContext, int32_t* xCoords,
            int32_t* yCoords, int32_t nCoords);
    mpe_Error (*mpe_gfxFillPolygon_ptr)(mpe_GfxContext, int32_t* xCoords,
            int32_t* yCoords, int32_t nCoords);
    mpe_Error (*mpe_gfxBitBlt_ptr)(mpe_GfxContext dest, mpe_GfxSurface source,
            int32_t dx, int32_t dy, mpe_GfxRectangle *srect);
    mpe_Error (*mpe_gfxStretchBlt_ptr)(mpe_GfxContext dest,
            mpe_GfxSurface source, mpe_GfxRectangle *drect,
            mpe_GfxRectangle *srect);
    mpe_Error (*mpe_gfxDrawString_ptr)(mpe_GfxContext ctx, int32_t x,
            int32_t y, const char* buf, int32_t len);
    mpe_Error (*mpe_gfxDrawString16_ptr)(mpe_GfxContext ctx, int32_t x,
            int32_t y, const mpe_GfxWchar* buf, int32_t len);
    /* Surface */
    mpe_Error (*mpe_gfxSurfaceNew_ptr)(mpe_GfxSurfaceInfo *desc,
            mpe_GfxSurface* surface);
    mpe_Error (*mpe_gfxSurfaceCreate_ptr)(mpe_GfxSurface base,
            mpe_GfxSurface* surface);
    mpe_Error (*mpe_gfxSurfaceDelete_ptr)(mpe_GfxSurface surface);
    mpe_Error (*mpe_gfxSurfaceGetInfo_ptr)(mpe_GfxSurface surface,
            mpe_GfxSurfaceInfo *info);
    /* Font */
    mpe_Error (*mpe_gfxFontNew_ptr)(mpe_GfxFontFactory ff,
            const mpe_GfxWchar* name, const uint32_t namelength,
            mpe_GfxFontStyle style, int32_t size, mpe_GfxFont* font);
    mpe_Error (*mpe_gfxFontDelete_ptr)(mpe_GfxFont font);
    mpe_Error (*mpe_gfxGetFontMetrics_ptr)(mpe_GfxFont font,
            mpe_GfxFontMetrics* metrics);
    mpe_Error (*mpe_gfxGetStringWidth_ptr)(mpe_GfxFont font, const char* str,
            int32_t len, int32_t* width);
    mpe_Error (*mpe_gfxGetString16Width_ptr)(mpe_GfxFont font,
            const mpe_GfxWchar* str, int32_t len, int32_t* width);

    mpe_Error (*mpe_gfxGetCharWidth_ptr)(mpe_GfxFont font, mpe_GfxWchar ch,
            int32_t* width);

    mpe_Error (*mpe_gfxFontHasCode_ptr)(mpe_GfxFont font, mpe_GfxWchar code);

    /* Font Factory */
    mpe_Error (*mpe_gfxFontFactoryNew_ptr)(mpe_GfxFontFactory *ff);
    mpe_Error (*mpe_gfxFontFactoryDelete_ptr)(mpe_GfxFontFactory ff);
    mpe_Error (*mpe_gfxFontFactoryAdd_ptr)(mpe_GfxFontFactory ff,
            mpe_GfxFontDesc* desc);

    mpe_Error (*mpe_gfxFontGetList_ptr)(mpe_GfxFontDesc** desc);

    /* Additional Draw */
    mpe_Error (*mpe_gfxClearRect_ptr)(mpe_GfxContext ctx,
            mpe_GfxRectangle *rect, mpe_GfxColor color);

    /* Display Discovery/Configuration */
    mpe_Error (*mpe_dispGetScreenCount_ptr)(uint32_t *nScreens);
    mpe_Error (*mpe_dispGetScreens_ptr)(mpe_DispScreen* screens);
    mpe_Error (*mpe_dispGetScreenInfo_ptr)(mpe_DispScreen screen,
            mpe_DispScreenInfo* info);
    mpe_Error (*mpe_dispGetDeviceCount_ptr)(mpe_DispScreen screen,
            mpe_DispDeviceType type, uint32_t *nDevices);
    mpe_Error (*mpe_dispGetDevices_ptr)(mpe_DispScreen screen,
            mpe_DispDeviceType type, mpe_DispDevice* devices);
    mpe_Error (*mpe_dispGetDeviceInfo_ptr)(mpe_DispDevice device,
            mpe_DispDeviceInfo* info);
    mpe_Error (*mpe_dispGetConfigCount_ptr)(mpe_DispDevice device,
            uint32_t* nConfigs);
    mpe_Error (*mpe_dispGetConfigs_ptr)(mpe_DispDevice device,
            mpe_DispDeviceConfig* configs);
    mpe_Error (*mpe_dispGetCurrConfig_ptr)(mpe_DispDevice device,
            mpe_DispDeviceConfig* config);
    mpe_Error (*mpe_dispSetCurrConfig_ptr)(mpe_DispDevice device,
            mpe_DispDeviceConfig config);
    mpe_Error (*mpe_dispWouldImpact_ptr)(mpe_DispDevice device,
            mpe_DispDeviceConfig config, mpe_DispDevice device2,
            mpe_DispDeviceConfig config2, mpe_Bool *impact);
    mpe_Error (*mpe_dispGetConfigInfo_ptr)(mpe_DispDeviceConfig config,
            mpe_DispDeviceConfigInfo* info);
    mpe_Error (*mpe_dispGetCoherentConfigCount_ptr)(mpe_DispScreen screen,
            uint32_t* nSets);
    mpe_Error (*mpe_dispGetCoherentConfigs_ptr)(mpe_DispScreen screen,
            mpe_DispCoherentConfig* set);
    mpe_Error (*mpe_dispSetCoherentConfig_ptr)(mpe_DispScreen screen,
            mpe_DispCoherentConfig set);
    mpe_Error (*mpe_dispGetConfigSetCount_ptr)(mpe_DispCoherentConfig set,
            uint32_t* nConfigs);
    mpe_Error (*mpe_dispGetConfigSet_ptr)(mpe_DispCoherentConfig set,
            mpe_DispDeviceConfig* configs);
    mpe_Error (*mpe_dispSetBGColor_ptr)(mpe_DispDevice device,
            mpe_GfxColor color);
    mpe_Error (*mpe_dispGetBGColor_ptr)(mpe_DispDevice device,
            mpe_GfxColor* color);
    mpe_Error (*mpe_dispDisplayBGImage_ptr)(mpe_DispDevice device,
            mpe_DispBGImage image, mpe_GfxRectangle* size);
    mpe_Error (*mpe_dispBGImageGetSize_ptr)(mpe_DispBGImage image,
            mpe_GfxDimensions* size);

    mpe_Error (*mpe_dispGetOutputPortCount_ptr)(uint32_t *nPorts);
    mpe_Error (*mpe_dispGetOutputPorts_ptr)(mpe_DispOutputPort *ports);
    mpe_Error (*mpe_dispEnableOutputPort_ptr)(mpe_DispOutputPort port,
            mpe_Bool enable);
    mpe_Error (*mpe_dispGetOutputPortInfo_ptr)(mpe_DispOutputPort port,
            mpe_DispOutputPortInfo *info);

    mpe_Error (*mpe_gfxPaletteNew_ptr)(int nColors, mpe_GfxPalette* palette);
    mpe_Error (*mpe_gfxPaletteDelete_ptr)(mpe_GfxPalette palette);
    mpe_Error (*mpe_gfxPaletteSet_ptr)(mpe_GfxPalette palette,
            mpe_GfxColor *colors, int nColors, int offset);
    mpe_Error (*mpe_gfxPaletteGet_ptr)(mpe_GfxPalette palette, int nColors,
            int offset, mpe_GfxColor *colors);
    mpe_Error (*mpe_gfxPaletteMatch_ptr)(mpe_GfxPalette palette,
            mpe_GfxColor color, int *index);

    mpe_Error (*mpe_dispBGImageNew_ptr)(uint8_t* buffer, size_t length,
            mpe_DispBGImage* image);
    mpe_Error (*mpe_dispBGImageDelete_ptr)(mpe_DispBGImage image);

    mpe_Error (*mpe_dispGetRFBypassState_ptr)(mpe_Bool *state);
    mpe_Error (*mpe_dispSetRFBypassState_ptr)(mpe_Bool enable);
    mpe_Error (*mpe_dispGetRFChannel_ptr)(uint32_t *channel);
    mpe_Error (*mpe_dispSetRFChannel_ptr)(uint32_t channel);

    mpe_Error (*mpe_dispGetDFC_ptr)(mpe_DispDevice decoder,
            mpe_DispDfcAction *action, mpe_Bool *isPlatformMode);
    mpe_Error (*mpe_dispCheckDFC_ptr)(mpe_DispDevice decoder,
            mpe_DispDfcAction action);
    mpe_Error (*mpe_dispSetDFC_ptr)(mpe_DispDevice decoder,
            mpe_DispDfcAction action);

    mpe_Error (*mpe_dispGetVideoOutputPortOption_ptr)(mpe_DispOutputPort port,
            mpe_DispOutputPortOption *opt);
    mpe_Error (*mpe_dispSetVideoOutputPortOption_ptr)(mpe_DispOutputPort port,
            mpe_DispOutputPortOption *opt);

    /* Gfx Device Surface APIs */
    mpe_Error (*mpe_dispGetGfxSurface_ptr)(mpe_DispDevice device,
            mpe_GfxSurface *surface);
    mpe_Error (*mpe_dispFlushGfxSurface_ptr)(mpe_DispDevice device);

    /* DSExt api */
    mpe_Error (*mpe_dispSetMainVideoOutputPort_ptr)(mpe_DispScreen screen,
            mpe_DispOutputPort port);
    mpe_Error (*mpe_dispGetDisplayAttributes_ptr)(mpe_DispOutputPort port,
            mpe_DispVideoDisplayAttrInfo* info);
    mpe_Error (*mpe_dispIsDisplayConnected_ptr)(mpe_DispOutputPort port,
            mpe_Bool* connected);
    mpe_Error (*mpe_dispIsContentProtected_ptr)(mpe_DispOutputPort port,
            mpe_Bool* encrypted);
    //mpe_Error (*mpe_dispIsDynamicConfigurationSupported_ptr)(mpe_DispOutputPort port, mpe_Bool* supported);
    mpe_Error (*mpe_dispGetSupportedFixedVideoOutputConfigurations_ptr)(
            mpe_DispOutputPort port,
            mpe_DispFixedVideoOutputConfigInfo** ptrToArray);
    mpe_Error (*mpe_dispGetSupportedDynamicVideoOutputConfigurations_ptr)(
            mpe_DispOutputPort port,
            mpe_DispDynamicVideoOutputConfigInfo** ptrArray);
    mpe_Error (*mpe_dispGetCurVideoOutputConfiguration_ptr)(
            mpe_DispOutputPort port, mpe_DispVideoConfig* handle);
    mpe_Error (*mpe_dispSetCurVideoOutputConfiguration_ptr)(
            mpe_DispOutputPort port, mpe_DispVideoConfig handle);
    mpe_Error (*mpe_dispRegister_ptr)(mpe_EventQueue queueId, void *handle);
    mpe_Error (*mpe_dispUnregister_ptr)(mpe_EventQueue queueId, void *handle);
    mpe_Error (*mpe_dispGetDeviceDest_ptr)(mpe_DispDevice device,
            mpe_DispDeviceDest* dest);
    mpe_Error (*mpe_dispGetSupportedDFCs_ptr)(mpe_DispDevice decoder,
            mpe_DispDfcAction** array);
    mpe_Error (*mpe_dispGetSupportedDFCCount_ptr)(mpe_DispDevice decoder,
            uint32_t* count);
    mpe_Error (*mpe_dispSetDefaultPlatformDFC_ptr)(mpe_DispDevice decoder,
            mpe_DispDfcAction action);
    mpe_Error (*mpe_dispGetSupportedFixedVideoOutputConfigurationCount_ptr)(
            mpe_DispOutputPort port, uint32_t* count);
    mpe_Error (*mpe_dispGetSupportedDynamicVideoOutputConfigurationCount_ptr)(
            mpe_DispOutputPort port, uint32_t* count);
    mpe_Error
            (*mpe_gfxGeneratePlatformKeyEvent_ptr)(int32_t type, int32_t code);

} mpe_disp_ftable_t;

#ifdef __cplusplus
}
;
#endif

#endif /* _DISPMGR_H_ */
