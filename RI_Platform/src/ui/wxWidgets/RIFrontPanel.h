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
 * RIFrontPanel.h
 *
 *  Created on: June 5, 2009
 *      Author: Mark Millard
 */

#ifndef _RI_FRONTPANEL_UI_H_
#define _RI_FRONTPANEL_UI_H_

#include <map>
#include <string>
using namespace std;

// Include RI Platform header files.
#include <ri_frontpanel.h>

// Include RI Emulator header files.
#include "RIGLCanvas.h"
#include "ImageMap.h"
#include "BitLed.h"
#include "TextLed.h"
#include "ActiveRegion.h"

// Declare external classes.
class QuadTreeNode;
class HotSpotActiveRegion;

/**
 * The RI Emulator Front Panel window.
 */
class RIFrontPanel: public RIGLCanvas
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
     * A constructor that creates a window for the RI Emulator front panel.
     *
     * @param parent The window's parent.
     * @param name The window's name.
     * @param style The style template.
     * @param size The width and height of the window to create.
     */
    RIFrontPanel(wxWindow *parent, const wxString &name, long style,
            const wxSize size);

    /**
     * The destructor.
     */
    virtual ~RIFrontPanel();

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
     * The message loop idle event handler.
     *
     * @param event The idle event.
     */
    void OnIdle(wxIdleEvent &event);

    /**
     * Initialize OpenGL.
     */
    void InitOpenGL();

    /**
     * Create an LED Indicator.
     *
     * @param led A pointer to the RI Platform LED to create a display
     * for.
     */
    void CreateIndicatorDisplay(ri_led_t *led);

    /**
     * Destroy the specified LED Indicator.
     *
     * @param led A pointer to the RI Platform LED to desroy.
     */
    void DestroyIndicatorDisplay(ri_led_t *led);

    /**
     * Create a Text Display.
     *
     * @param led A pointer to the RI Platform Text LED to create
     * a display for.
     */
    void CreateTextDisplay(ri_textled_t *led);

    /**
     * Destroy the specified Text LED.
     *
     * @param led A pointer to the RI Platform Text LED to destroy.
     */
    void DestroyTextDisplay(ri_textled_t *led);

    /**
     * Update the Text LED Display.
     * <p>
     * The associated string, as referenced by the <code>led</code>
     * will be refreshed.
     * </p>
     *
     * @param led A pointer to the RI Platform Text LED to update.
     */
    void UpdateTextDisplayString(ri_textled_t *led);

    /**
     * Update the Text LED Display.
     * <p>
     * The current time will be refreshed.
     * </p>
     *
     * @param led A pointer to the RI Platform Text LED to update.
     */
    void UpdateTextDisplayClock(ri_textled_t *led);

    /**
     * Update the Text LED Display scroll.
     *
     * @param led A pointer to the RI Platform Text LED to update.
     */
    void UpdateTextDisplayScroll(ri_textled_t *led);

    /**
     * Reset the Text LED Display.
     *
     * @param led A pointer to the RI Platform Text LED to reset.
     */
    void ResetTextDisplay(ri_textled_t *led);

    /**
     * Update the indicator LED.
     *
     * @param led The indicator display to update.
     */
    void RenderIndicatorDisplay(ri_led_t *led);

    /**
     * Update the text LED.
     *
     * @param led The text display to update.
     */
    void RenderTextDisplay(ri_textled_t *led);

protected:

    /**
     * Initialize the Front Panel interface.
     * <p>
     * This method is responsible for initializing OpenGL and the Image Map.
     * </p>
     */
    virtual void Init();

    /**
     * Display the image.
     */
    virtual void Render(wxDC &dc);

    /**
     * Display the HotSpot region.
     *
     * @param region A reference to the active HotSpot region.
     */
    virtual void RenderHotSpot(HotSpotActiveRegion &region, wxClientDC *dc);

    /**
     * Highlight the specified selection.
     *
     * @param region The active HotSpot region to highlight.
     */
    virtual void HighlightSelection(HotSpotActiveRegion &region);
    virtual void RenderHighlight(Highlight *highlight);

    /**
     * Unhighlight the current selection.
     */
    virtual void RemoveHighlights();

    virtual void PaintMe();

private:

    bool inCurrentSelection(HotSpotActiveRegion *region);
    HotSpotActiveRegion * inCurrentSelection(wxPoint pos);

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

    // The BitLed map.
    typedef map<string, BitLed*> BitLedMap;
    BitLedMap m_bitLedMap;

    // The TextLed map.
    typedef map<string, TextLed*> TextLedMap;
    TextLedMap m_textLedMap;

    // A list of currently selected highlights.
    typedef std::list<Highlight *> HiglightSelectionList;
    HiglightSelectionList m_curHighlights;

    // This class handles events.
    /*lint -e(1516)*/
DECLARE_EVENT_TABLE()
};
#endif /* _RI_FRONTPANEL_UI_H_ */
