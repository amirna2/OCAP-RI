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

//lint -e593

/*
 * RIFrontPanel.cpp
 *
 *  Created on: Feb 19, 2009
 *      Author: Mark Millard
 */

#include <list>

// Include wxWidgets header files.
#include <wx/wx.h>

// Include RI Emulator header files.
#include "ri_log.h"
#include "ri_config.h"
#include "ui_window.h"
#include "ui_frontpanel.h"
#include "RIFrontPanel.h"
#include "ImageMapManager.h"
#include "QuadTreeNode.h"
#include "HotSpotActiveRegion.h"
#include "PointActiveRegion.h"
#include "LedEventObject.h"

// Mouse button states.
#define NO_CURRENT_KEY_PRESSED -1
#define MOUSE_BUTTON_UP true
#define MOUSE_BUTTON_DOWN false

// Event table for RIFrontPanel.
BEGIN_EVENT_TABLE(RIFrontPanel, wxGLCanvas)
EVT_KEY_UP(RIFrontPanel::OnKeyUp)
EVT_KEY_DOWN(RIFrontPanel::OnKeyDown)
EVT_ENTER_WINDOW(RIFrontPanel::OnEnterWindow)
EVT_LEFT_DOWN(RIFrontPanel::OnMouseButtonDown)
EVT_LEFT_UP(RIFrontPanel::OnMouseButtonUp)
EVT_MOTION(RIFrontPanel::OnMouseMotion)
EVT_PAINT(RIFrontPanel::OnPaint)
EVT_IDLE(RIFrontPanel::OnIdle)
END_EVENT_TABLE()

// Logging category
#define RILOG_CATEGORY g_uiFrontPanelCat
log4c_category_t* g_uiFrontPanelCat;

// The default image map for the front panel.
const char *RIFrontPanel::DEFAULT_IMAGE_MAP = "./frontpanel.im";

/*lint -sem(RIFrontPanel::Init,initializer)*/
RIFrontPanel::RIFrontPanel(wxWindow *parent, const wxString &name, long style,
        const wxSize size) :
    RIGLCanvas(parent, name, style, size), m_imageMap(NULL),
            m_activeRegionRoot(NULL)
{
    // Initialize logging for FrontPanel functionality.
    g_uiFrontPanelCat = log4c_category_get("RI.UI.FrontPanel");

    Init();
}

RIFrontPanel::~RIFrontPanel()
{
    frontpanel_enable_render( FALSE);

    BitLedMap::iterator bitLedIter;
    for (bitLedIter = m_bitLedMap.begin(); bitLedIter != m_bitLedMap.end(); ++bitLedIter)
    {
        BitLed *bitLed = bitLedIter->second;
        if (bitLed != NULL)
            delete bitLed;
    }
    (void) m_bitLedMap.empty();

    TextLedMap::iterator textLedIter;
    for (textLedIter = m_textLedMap.begin(); textLedIter != m_textLedMap.end(); ++textLedIter)
    {
        TextLed *textLed = textLedIter->second;
        if (textLed != NULL)
        {
            textLed->StopDisplay();
            delete textLed;
        }
    }
    (void) m_textLedMap.empty();

    // clear highlight map
    RemoveHighlights();

    if (m_imageMap)
        delete m_imageMap;
    if (m_activeRegionRoot)
        delete m_activeRegionRoot; // GORP: delete child object mem here
}

void RIFrontPanel::OnPaint(wxPaintEvent &event)
{
    RILOG_DEBUG("Handling wxPaintEvent.\n");

    // Create a device context. This needs to be done even if we don't plan to
    // render anything; otherwise, Windows will keep sending paint events
    // ad infinitum.
    wxPaintDC dc(this);

    PaintMe();
}

void RIFrontPanel::PaintMe()
{
    // Create a device context.
    wxClientDC dc(this);

    // RILOG_INFO("PaintMe\n");

    // Make sure there is something to render.
    if (m_imageMap == NULL)
        return;

    // Make sure we can render the front panel.
    if (!m_canRender)
        return;

    // Set this canvas as the current OpenGL context so that OpenGL
    // commands can be directed to this window.
    SetCurrent();

    // Attempt to lock the render mutex. If we can't obtain the lock,
    // then assume we can't render the next frame.
    gboolean status = g_mutex_trylock(m_renderMutex);
    if (!status)
        return;

    // Render base image.
    Render(dc);

    // render highlights
    HiglightSelectionList::iterator hslIt = m_curHighlights.begin();
    while (hslIt != m_curHighlights.end())
    {
        Highlight *selection = (Highlight *) (*hslIt);

        RenderHighlight(selection);

        hslIt++;
    }

    // render text strings
    TextLedMap::iterator textLedIter;
    for (textLedIter = m_textLedMap.begin(); textLedIter != m_textLedMap.end(); ++textLedIter)
    {
        TextLed *textLed = textLedIter->second;
        if (textLed != NULL)
        {
            textLed->Redisplay();
        }
    }

    // render active regions
    if (!m_curSelection.empty())
    {
        ActiveRegionSelection::iterator arsIt = m_curSelection.begin();
        while (arsIt != m_curSelection.end())
        {
            ActiveRegionType objType;
            long result = (*arsIt)->GetActiveRegionType(&objType);
            assert(result == 0);

            if (objType == ACTIVE_REGION_HOTSPOT)
            {
                HotSpotActiveRegion *hotspotRegion =
                        (HotSpotActiveRegion *) (*arsIt);
                RenderHotSpot(*hotspotRegion, &dc);
            }

            arsIt++;
        }
    }

    // Show the OpenGL back buffer on this window.
    SwapBuffers();

    // Unlock the render mutex.
    g_mutex_unlock( m_renderMutex);
}

void RIFrontPanel::OnKeyUp(wxKeyEvent &event)
{
    int code = event.GetKeyCode();
    RILOG_DEBUG("Handling wxKeyEvent Up: %d.\n", code);
    window_handle_key_event(code, true);
}

void RIFrontPanel::OnKeyDown(wxKeyEvent &event)
{
    int code = event.GetKeyCode();
    RILOG_DEBUG("Handling wxKeyEvent Down: %d.\n", code);
    window_handle_key_event(code, false);
}

void RIFrontPanel::OnEnterWindow(wxMouseEvent &event)
{
    // Set focus to the wxGLCanvas; this facilitates
    // key event processing.
    SetFocus();
}

bool RIFrontPanel::inCurrentSelection(HotSpotActiveRegion *region)
{
    bool retVal = false;

    ActiveRegionSelection::iterator it = m_curSelection.begin();
    while (it != m_curSelection.end())
    {
        // Note: only Hotspot Active Regions are kept in the current selection.
        if (region == (HotSpotActiveRegion *) (*it))
        {
            retVal = true;
            break;
        }
        it++;
    }
    return retVal;
}

HotSpotActiveRegion * RIFrontPanel::inCurrentSelection(wxPoint pos)
{
    HotSpotActiveRegion *retVal = NULL;

    if (!m_curSelection.empty())
    {
        // Determine if position is still in previous active region set.
        ActiveRegionSelection::iterator it = m_curSelection.begin();
        while (it != m_curSelection.end())
        {
            ActiveRegionType objType;
            long result = (*it)->GetActiveRegionType(&objType);
            assert(result == 0);

            if (objType == ACTIVE_REGION_HOTSPOT)
            {
                double x, y, width, height;
                HotSpotActiveRegion *hotspotRegion =
                        (HotSpotActiveRegion *) (*it);
                (void) hotspotRegion->GetPosition(&x, &y);
                (void) hotspotRegion->GetExtent(&width, &height);
                if ((pos.x >= x) && (pos.x <= (x + width)) && (pos.y >= y)
                        && (pos.y <= (y + height)))
                {
                    retVal = hotspotRegion;
                    break;
                }
            }

            it++;
        }
    }

    return retVal;
}

void RIFrontPanel::OnMouseMotion(wxMouseEvent &event)
{
    wxPoint pos = event.GetPosition();

    // Check to see if we ever created an Image Map (if frontpanel.im was not found).
    if (m_imageMap == NULL)
        return;

    //RILOG_DEBUG("Handling wxMouseEvent: (%d, %d)\n", pos.x, pos.y);

    bool bRepaint = false;

    // Clean-up current selection if necessary.
    if (!m_curSelection.empty())
    {
        // Determine if mouse position is still in previous active region set.
        ActiveRegionSelection::iterator it = m_curSelection.begin();
        while (it != m_curSelection.end())
        {
            ActiveRegionType objType;
            long result = (*it)->GetActiveRegionType(&objType);
            assert(result == 0);

            if (objType == ACTIVE_REGION_HOTSPOT)
            {
                double x, y, width, height;
                HotSpotActiveRegion *hotspotRegion =
                        (HotSpotActiveRegion *) (*it);
                (void) hotspotRegion->GetPosition(&x, &y);
                (void) hotspotRegion->GetExtent(&width, &height);
                if ((pos.x < x) || (pos.x > (x + width)) || (pos.y < y)
                        || (pos.y > (y + height)))
                {
                    RILOG_DEBUG("REMOVING REGION FROM SELECTION\n");

                    // remove region from the current selection.
                    it = m_curSelection.erase(it);

                    // If a key is pressed, release it...
                    if (m_currentKeyDown != NO_CURRENT_KEY_PRESSED)
                    {
                        // Unhighlight any previous selections.
                        RemoveHighlights();

                        RILOG_DEBUG(
                                "Mouse Button Up (due to mouse motion): Key Code: %d\n",
                                m_currentKeyDown);
                        window_handle_remote_event(
                                (ri_event_code) m_currentKeyDown,
                                MOUSE_BUTTON_UP);
                        m_currentKeyDown = NO_CURRENT_KEY_PRESSED;
                    }

                    bRepaint = true;

                    continue;
                }
            }
            it++;
        }
    }

    // Process new active regions.
    ActiveRegionSet collidables;
    PointActiveRegion region(pos.x, pos.y);
    (void) m_activeRegionRoot->GetCollidableObjects(&region, false,
            ACTIVE_REGION_NULL, collidables);
    ActiveRegionSet::iterator it = collidables.begin();
    while (it != collidables.end())
    {
        ActiveRegionType objType;
        long result = (*it)->GetActiveRegionType(&objType);
        assert(result == 0);

        if (objType == ACTIVE_REGION_HOTSPOT)
        {
            double x, y, width, height;
            HotSpotActiveRegion *hotspotRegion = (HotSpotActiveRegion *) (*it);
            (void) hotspotRegion->GetPosition(&x, &y);
            (void) hotspotRegion->GetExtent(&width, &height);
            if ((pos.x >= x) && (pos.x <= (x + width)) && (pos.y >= y)
                    && (pos.y <= (y + height)))
            {
                RILOG_DEBUG("Rendering HotSpot\n");

                if (inCurrentSelection(hotspotRegion))
                {
                    // Already selected - skip.
                    it++;
                    continue;
                }

                RILOG_DEBUG("NEW REGION IN SELECTION");

                // Mouse is in a new active region.
                m_curSelection.push_back(*it);
                bRepaint = true;
            }
        }
        it++;
    }

    if (bRepaint)
    {
        RILOG_DEBUG("MouseMove hander calling PaintMe()");
        PaintMe();
    }
}

void RIFrontPanel::OnMouseButtonDown(wxMouseEvent &event)
{
    wxPoint pos = event.GetPosition();

    // XXX - Should probably search all selections which overlap
    // the specified mouse position. For now, just process the first
    // one found.

    HotSpotActiveRegion *region = inCurrentSelection(pos);
    if (region != NULL)
    {
        HotSpot *hotspot;
        region->GetHotSpot(&hotspot);

        // Highlight the selected regions.
        HighlightSelection(*region);

        PaintMe();

        // Send the key code event to the platform.
        int code = hotspot->m_event;
        RILOG_DEBUG("Mouse Button Down: (%d, %d,); Key Code: %d\n", pos.x,
                pos.y, code);
        window_handle_remote_event((ri_event_code) code, MOUSE_BUTTON_DOWN);
        m_currentKeyDown = code;
    }
}

void RIFrontPanel::OnMouseButtonUp(wxMouseEvent &event)
{
    (void) event.GetPosition();

    // Unhighlight any previouse selections.
    RemoveHighlights();

    PaintMe();

    // XXX - Should probably search all selections which overlap
    // the specified mouse position. For now, just process the first
    // one found.

    // Release the previous key, regardless of where the pointer currently is...
    if (m_currentKeyDown != NO_CURRENT_KEY_PRESSED)
    {
        RILOG_DEBUG("Mouse Button Up: Key Code: %d\n", m_currentKeyDown);
        window_handle_remote_event((ri_event_code) m_currentKeyDown,
                MOUSE_BUTTON_UP);
        m_currentKeyDown = NO_CURRENT_KEY_PRESSED;
    }
}

void RIFrontPanel::Init()
{
    int i;

    // Initialize the Image Map.
    // Retrieve value from the configuration file.
    const char *configValue;
    const char *imageMapFile = RIFrontPanel::DEFAULT_IMAGE_MAP;
    if ((configValue = ricfg_getValue("twb_config",
            (char *) "RI.Emulator.FrontPanel.imageMap")) != NULL
            || (configValue = ricfg_getValue("RIPlatform",
                    (char *) "RI.Emulator.FrontPanel.imageMap")) != NULL)
    {
        imageMapFile = configValue;
    }

    ImageMapManager *theManager = ImageMapManager::getInstance();
    m_imageMap = theManager->LoadImageMap(imageMapFile);
    if (!m_imageMap)
    {
        RILOG_ERROR("Unable to load FrontPanel Image Map %s.\n", imageMapFile);
        return;
    }

    // Reset the size of the client area based on image.
    unsigned int width, height;
    wxSize size;
    m_imageMap->GetBaseImageSize(&width, &height);
    size.SetWidth(width);
    size.SetHeight(height);
    SetClientSize(size);

    // Initialize the active hotspot regions.
    m_activeRegionRoot = new QuadTreeNode(0, 0, width, height);
    int numSubdivisions = 5; // TODO - Should probably be based on number of hotspots in Image Map.
    for (i = 0; i < numSubdivisions; i++)
    {
        // Create 2**numSubdivisions quadrants (i.e. 2**5 = 32 quadrants).
        (void) m_activeRegionRoot->SubDivide();
    }

    HotSpot *hotspots = m_imageMap->GetHotspots();
    for (i = 0; i < m_imageMap->GetNumHotspots(); i++)
    {
        //double x = hotspots[i].m_x + (hotspots[i].m_width / 2);
        //double y = hotspots[i].m_y + (hotspots[i].m_height / 2);
        double x = hotspots[i].m_x;
        double y = hotspots[i].m_y;
        HotSpotActiveRegion *region = new HotSpotActiveRegion(x, y,
                &hotspots[i]);
        (void) m_activeRegionRoot->AddObject(region);
    }

    // Initialize current key pressed state.
    m_currentKeyDown = NO_CURRENT_KEY_PRESSED;

    // Enable Indicator and Text Display LED rendering.
    frontpanel_enable_render( TRUE);
}

void RIFrontPanel::InitOpenGL()
{
    wxSize size;
    size = GetClientSize();

    // Initialize common OpenGL state.
    RIGLCanvas::InitOpenGL(size.GetWidth(), size.GetHeight());

    m_blendSupported = RIGLCanvas::IsExtensionSupported(
            "GL_EXT_blend_equation_separate");
}

void RIFrontPanel::Render(wxDC &dc)
{
    GLubyte *image = NULL;
    GLuint width, height;

    // Clear the window with current clearing color (background color
    // set in parent class, RIGLCanvas).
    glClear( GL_COLOR_BUFFER_BIT);

    // The image data is 1 byte aligned. This should have been set up by the parent
    // class, RIGLCanvas::Init().

    // Get a pointer to the image data from the Image Map.
    image = (GLubyte *) m_imageMap->GetBaseImageAddr();

    // Get the image data size.
    m_imageMap->GetBaseImageSize(&width, &height);

    // Set raster position. The image is inverted, so we need to flip it before
    // displaying.
    glPixelZoom(1.0f, -1.0f);
    glRasterPos2i(0, height);

    // Draw the image.
    if (image != NULL)
        glDrawPixels(width, height, GL_RGBA, GL_UNSIGNED_BYTE, image);
    GLenum error = glGetError();
    if (error != GL_NO_ERROR)
    {
        //const GLubyte *errString = gluErrorString(error);
        //RILOG_ERROR("OpenGL Error: %s\n", errString);
        RILOG_ERROR("OpenGL Error: %d\n", error);
    }
}

#define drawLine(x1, y1, x2, y2) \
    glBegin(GL_LINES); \
    glVertex2f((x1),(y1)); \
    glVertex2f((x2),(y2)); \
    glEnd();

void RIFrontPanel::RenderHotSpot(HotSpotActiveRegion &region, wxClientDC *dc)
{
    double x_1, y_1, x_2, y_2;
    double width, height;

    RILOG_DEBUG("RIFrontPanel::RenderHotSpot\n");

    // Set up drawing parameters for highlighting; using XOR technique.
    glLineWidth(2.0f);
    if (m_blendSupported)
    {
        glEnable( GL_COLOR_LOGIC_OP);
        glLogicOp( GL_XOR);
        glColor3f(1.0f, 1.0f, 1.0f); // white
    }
    else
    {
        glColor3f(0.1f, 0.1f, 0.1f); // black
    }

    (void) region.GetPosition(&x_1, &y_1);
    (void) region.GetExtent(&width, &height);

    // Draw current region.
    unsigned int imageWidth, imageHeight;
    m_imageMap->GetBaseImageSize(&imageWidth, &imageHeight);

    x_2 = x_1 + width;
    y_1 = imageHeight - y_1;
    y_2 = y_1 - height;
    drawLine(x_1, y_1, x_2, y_1);
    drawLine(x_2, y_1, x_2, y_2);
    drawLine(x_2, y_2, x_1, y_2);
    drawLine(x_1, y_2, x_1, y_1);

    if (m_blendSupported)
    {
        // Reset drawing mode.
        glDisable( GL_COLOR_LOGIC_OP);
    }
}

void RIFrontPanel::HighlightSelection(HotSpotActiveRegion &region)
{
    double x_1, y_1, x_2, y_2;
    double width, height;
    unsigned int imageWidth, imageHeight;
    Highlight *selection;

    // Adjust the extent we are highlighting. Compensate for the rollover
    // highlight which has a pixel width of 2.
    (void) region.GetPosition(&x_1, &y_1);
    (void) region.GetExtent(&width, &height);
    m_imageMap->GetBaseImageSize(&imageWidth, &imageHeight);
    x_2 = x_1 + width - 1;
    x_1 = x_1 + 1;
    y_2 = imageHeight - y_1 - height + 1;
    y_1 = imageHeight - y_1 - 1;

    // Remember the region we are highlighting so we can recover during
    // the unhighlight stage.
    width = x_2 - x_1;
    height = y_1 - y_2;
    void *raster = g_try_malloc((size_t)(width * height * 4 * sizeof(GLbyte)));
    glReadPixels((GLint) x_1, (GLint)(y_1 - height), (GLsizei) width,
            (GLsizei) height, GL_RGBA, GL_UNSIGNED_BYTE, raster);
    selection = (Highlight *) g_try_malloc(sizeof(Highlight));
    if (selection)
    {
        selection->m_x = (int) x_1;
        selection->m_y = (int) y_1;
        selection->m_width = (int) width;
        selection->m_height = (int) height;
        selection->m_pixels = raster;
    }

    m_curHighlights.push_back(selection);
}

void RIFrontPanel::RenderHighlight(Highlight *highlight)
{
    RILOG_DEBUG("RIFrontPanel::RenderHighlight\n");

    double x_1, y_1, x_2, y_2;

    x_1 = highlight->m_x;
    y_1 = highlight->m_y;
    x_2 = x_1 + highlight->m_width;
    y_2 = y_1 - highlight->m_height;

    // Set up drawing parameters for highlighting
    glColor4f(1.0f, 1.0f, 1.0f, 0.5f);

    // Draw highlight.
    glRectd(x_1, y_1, x_2, y_2);
}

void RIFrontPanel::RemoveHighlights()
{
    if (!m_curHighlights.empty())
    {
        HiglightSelectionList::iterator it = m_curHighlights.begin();
        while (it != m_curHighlights.end())
        {
            Highlight *selection = (Highlight *) (*it);

            // Clean up.
            g_free(selection->m_pixels);
            g_free(selection);
            it = m_curHighlights.erase(it);
        }
    }
}

void RIFrontPanel::OnIdle(wxIdleEvent &event)
{
    // Create a device context.
    wxClientDC dc(this);

    // Validate that the event is coming from our own.
    wxObject *evObj = event.GetEventObject();
    if (evObj == NULL)
    {
        //RILOG_INFO("RIFrontPanel::OnIdle invalid LED event.\n");
        return;
    }
    wxClassInfo *info = evObj->GetClassInfo();
    if (info == NULL)
    {
        //RILOG_INFO("RIFrontPanel::OnIdle invalid LED event.\n");
        return;
    }
    const wxChar *className = info->GetClassName();
    wxString *classStr = new wxString(className);
    if (!classStr->IsSameAs("LedEventObject"))
    {
        //RILOG_INFO("RIFrontPanel::OnIdle invalid LED event.\n");
        delete classStr;
        return;
    }
    delete classStr;
    LedEventObject *ledev = (LedEventObject *) evObj;

    if (ledev->IsIndicator())
    {
        ri_led_t *led = ledev->GetIndicator();
        BitLed *bitLed = m_bitLedMap[led->m_name];
        if (bitLed != NULL)
            bitLed->Redisplay();
    }
    else if (ledev->IsTextDisplay())
    {
        ri_textled_t *led = ledev->GetTextDisplay();
        int mode = led->m_textMode;

        if (mode == RI_FP_MODE_STRING)
        {
            TextLed *textLed = m_textLedMap[led->m_base.m_name];
            if (textLed != NULL)
                textLed->DisplayLEDString(led->m_text);
        }
        else if ((mode == RI_FP_MODE_12H_CLOCK) || (mode
                == RI_FP_MODE_12H_CLOCK))
        {
            TextLed *textLed = m_textLedMap[led->m_base.m_name];
            if (textLed != NULL)
            {
                gboolean status = textLed->DisplayLEDClock(led->m_textMode);
                if (!status)
                    // May  have failed because time didn't change.
                    textLed->Redisplay();
            }
        }
    }

    PaintMe();

    // No longer need the LED event object;
    delete ledev;

}

void RIFrontPanel::CreateIndicatorDisplay(ri_led_t *led)
{
    RILOG_INFO("Creating the Indicator display: %s.\n", led->m_name);

    // Make sure that the image map was really initialized.
    if (m_imageMap == NULL)
        return;

    // Add the LED to the collection of indicator displays.
    BitLed *bitLed = new BitLed(led, m_imageMap);
    m_bitLedMap[led->m_name] = bitLed;

    // And flag it ready for rendering.
    bitLed->CanRender(true);
}

void RIFrontPanel::DestroyIndicatorDisplay(ri_led_t *led)
{
    RILOG_INFO("Destroying the Indicator display: %s.\n", led->m_name);

    // Retrieve the corresponding led.
    BitLed *bitLed = m_bitLedMap[led->m_name];
    if (bitLed == NULL)
        return;

    // Delete the associated LED.
    delete bitLed;

    // Remove the LED from the collection of indicator displays.
    (void) m_bitLedMap.erase(led->m_name);
}

void RIFrontPanel::CreateTextDisplay(ri_textled_t *led)
{
    RILOG_INFO("Creating the Text display: %s.\n", led->m_base.m_name);

    // Make sure that the image map was really initialized.
    if (m_imageMap == NULL)
        return;

    // Add the LED to the collection of text displays.
    TextLed *textLed = new TextLed(led, m_imageMap);
    m_textLedMap[led->m_base.m_name] = textLed;

    // Start the display (i.e. the clock).
    textLed->StartDisplay(this);
}

void RIFrontPanel::DestroyTextDisplay(ri_textled_t *led)
{
    RILOG_INFO("Destroying the Text display: %s.\n", led->m_base.m_name);

    // Retrieve the corresponding led.
    TextLed *textLed = m_textLedMap[led->m_base.m_name];
    if (textLed == NULL)
        return;

    // Stop the display (i.e. the clock).
    textLed->StopDisplay();

    // Delete the associated Text LED.
    delete textLed;

    // Remove the LED from the collection of indicator displays.
    (void) m_textLedMap.erase(led->m_base.m_name);
}

void RIFrontPanel::UpdateTextDisplayString(ri_textled_t *led)
{
    RILOG_INFO("Updating the Text display string: %s.\n", led->m_base.m_name);

    // Don't post an event if there isn't any image map.
    if (m_imageMap == NULL)
        return;

    // Post an idle event to render text display.
    wxIdleEvent ev;
    ev.SetEventObject(new LedEventObject(led));
    wxPostEvent(this, ev);
    //RILOG_INFO("***** Posted idle event for rendering text display. *****\n");
}

void RIFrontPanel::UpdateTextDisplayClock(ri_textled_t *led)
{
    RILOG_INFO("Updating the Text display clock: %s.\n", led->m_base.m_name);

    // Don't post an event if there isn't any image map.
    if (m_imageMap == NULL)
        return;

    // Post an idle event to render text display.
    wxIdleEvent ev;
    ev.SetEventObject(new LedEventObject(led));
    wxPostEvent(this, ev);
    //RILOG_INFO("***** Posted idle event for rendering text display. *****\n");
}

void RIFrontPanel::UpdateTextDisplayScroll(ri_textled_t *led)
{
    RILOG_INFO("Updating the Text display scroll: %s.\n", led->m_base.m_name);

    TextLed *textLed = m_textLedMap[led->m_base.m_name];
    if (textLed != NULL)
        textLed->UpdateScroll();
}

void RIFrontPanel::RenderIndicatorDisplay(ri_led_t *led)
{
    RILOG_DEBUG("Updating the Indicator display: %s.\n", led->m_name);

    // Don't post an event if there isn't any image map.
    if (m_imageMap == NULL)
        return;

    // Post an idle event to render indicator.
    wxIdleEvent ev;
    ev.SetEventObject(new LedEventObject(led));
    wxPostEvent(this, ev);
    //RILOG_INFO("***** Posted idle event for rendering indicator display. *****\n");
}

void RIFrontPanel::RenderTextDisplay(ri_textled_t *led)
{
    RILOG_DEBUG("Updating the Text display: %s.\n", led->m_base.m_name);

    // Don't post an event if there isn't any image map.
    if (m_imageMap == NULL)
        return;

    // Post an idle event to render text display.
    wxIdleEvent ev;
    ev.SetEventObject(new LedEventObject(led));
    wxPostEvent(this, ev);
    //RILOG_INFO("***** Posted idle event for rendering indicator display. *****\n");
}

void RIFrontPanel::ResetTextDisplay(ri_textled_t *led)
{
    RILOG_INFO("Resetting the Text display: %s.\n", led->m_base.m_name);

    TextLed *textLed = m_textLedMap[led->m_base.m_name];
    if (textLed != NULL)
        textLed->ResetLED();
}
