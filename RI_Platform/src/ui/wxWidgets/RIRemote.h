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

/*
 * RIRemote.h
 *
 *  Created on: Feb 19, 2009
 *      Author: Mark Millard
 */

#ifndef _RI_REMOTE_H_
#define _RI_REMOTE_H_

// Include RI Emulator header files.
#include "RIGLCanvas.h"
#include "ImageMap.h"
#include "ActiveRegion.h"

// Declare external classes.
class QuadTreeNode;
class HotSpotActiveRegion;

/**
 * The RI Emulator Remote window.
 */
class RIRemote: public RIGLCanvas
{
public:

    typedef struct _Highlight
    {
        int m_x; // The x coordinate location for the selection.
        int m_y; // The y coordinate location for the selection.
        int m_width; // The width of the selection.
        int m_height; // The height of the selection.
        void *m_pixels; // The selection region.
    } Highlight;

    /** The default Image Map, if one isn't provided via a configuration file. */
    static const char *DEFAULT_IMAGE_MAP;

    /**
     * A constructor that creates a window for the RI Emulator remote.
     *
     * @param parent The window's parent.
     * @param name The window's name.
     * @param style The style template.
     * @param size The width and height of the window to create.
     */
    RIRemote(wxWindow *parent, const wxString &name, long style,
            const wxSize size);

    /**
     * The destructor.
     */
    virtual ~RIRemote();

    // Event handlers.

    /**
     * The paint event handler.
     *
     * @param event The paint event.
     */
    void OnPaint(wxPaintEvent &event);

    /**
     * The key release event handler.
     *
     * @param event The key event.
     */
    void OnKeyUp(wxKeyEvent &event);

    /**
     * The key pressed event handler.
     *
     * @param event The key event.
     */
    void OnKeyDown(wxKeyEvent &event);

    /**
     * The mouse entered window event handler.
     *
     * @param event The mouse event.
     */
    void OnEnterWindow(wxMouseEvent &event);

    /**
     * The mouse button down event handler.
     *
     * @param event The mouse event.
     */
    void OnMouseButtonDown(wxMouseEvent &event);

    /**
     * The mouse button up event handler.
     *
     * @param event The mouse event.
     */
    void OnMouseButtonUp(wxMouseEvent &event);

    /**
     * The mouse moved event handler.
     *
     * @param event The mouse event.
     */
    void OnMouseMotion(wxMouseEvent &event);

    /**
     * Initialize OpenGL.
     */
    void InitOpenGL();

protected:

    /**
     * Initialize the Remote interface.
     * <p>
     * This method is responsible for initializing OpenGL and the Image Map.
     * </p>
     */
    virtual void Init();

    /**
     * Display the image.
     */
    virtual void Render();

    /**
     * Display the HotSpot region.
     *
     * @param region A reference to the active HotSpot region.
     */
    virtual void RenderHotSpot(HotSpotActiveRegion &region);

    virtual void HighlightSelection(HotSpotActiveRegion &region);

    virtual void RemoveHighlights();

    virtual void RenderHighlight(Highlight *highlight);

    virtual void PaintMe();

private:

    bool inCurrentSelection(HotSpotActiveRegion *region);
    HotSpotActiveRegion *inCurrentSelection(wxPoint pos);

    // The Image Map.
    ImageMap *m_imageMap;

    // The hotspot active regions.
    QuadTreeNode *m_activeRegionRoot;

    // Record previous keypress.
    int m_currentKeyDown;

    // Indicates if Opengl extension is supported for hotspot outline
    bool m_blendSupported;

    // A list of currently selected active regions.
    typedef std::list<ActiveRegion *> ActiveRegionSelection;
    ActiveRegionSelection m_curSelection;

    // A list of currently selected highlights.
    typedef std::list<Highlight *> HiglightSelectionList;
    HiglightSelectionList m_curHighlights;

    // This class handles events.
    /*lint -e(1516)*/
DECLARE_EVENT_TABLE()
};
#endif /* _RI_REMOTE_H_ */
