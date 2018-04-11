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
 * RIRemote.cpp
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
#include "RIRemote.h"
#include "ImageMapManager.h"
#include "QuadTreeNode.h"
#include "HotSpotActiveRegion.h"
#include "PointActiveRegion.h"

// Mouse button states.
#define NO_CURRENT_KEY_PRESSED -1
#define MOUSE_BUTTON_UP true
#define MOUSE_BUTTON_DOWN false

// Event table for RIRemote.
BEGIN_EVENT_TABLE(RIRemote, wxGLCanvas)
EVT_KEY_UP(RIRemote::OnKeyUp)
EVT_KEY_DOWN(RIRemote::OnKeyDown)
EVT_ENTER_WINDOW(RIRemote::OnEnterWindow)
EVT_LEFT_DOWN(RIRemote::OnMouseButtonDown)
EVT_LEFT_UP(RIRemote::OnMouseButtonUp)
EVT_MOTION(RIRemote::OnMouseMotion)
EVT_PAINT(RIRemote::OnPaint)
END_EVENT_TABLE()

// Logging category
#define RILOG_CATEGORY g_uiRemoteCat
log4c_category_t* g_uiRemoteCat;

const char *RIRemote::DEFAULT_IMAGE_MAP = "./remote.im";

/*lint -sem(RIRemote::Init,initializer)*/
RIRemote::RIRemote(wxWindow *parent, const wxString &name, long style,
        const wxSize size) :
    RIGLCanvas(parent, name, style, size), m_imageMap(NULL),
            m_activeRegionRoot(NULL)
{
    // Initialize logging for Remote functionality.
    g_uiRemoteCat = log4c_category_get("RI.UI.Remote");

    Init();
}

RIRemote::~RIRemote()
{
    if (m_imageMap)
        delete m_imageMap;
    if (m_activeRegionRoot)
        delete m_activeRegionRoot; // GORP: delete child object mem here
}

void RIRemote::OnPaint(wxPaintEvent &event)
{
    RILOG_DEBUG("Handling wxPaintEvent.\n");

    // Create a device context. This needs to be done even if we don't plan to
    // render anything; otherwise, Windows will keep sending paint events
    // ad infinitum.
    wxPaintDC dc(this);

    // Render base image.
    PaintMe();
}

void RIRemote::PaintMe()
{
    // Create a device context.
    wxClientDC dc(this);

    // Make sure we can render the remote.
    if (!m_canRender)
        return;

    // Set this canvas as the current OpenGL context so that OpenGL
    // commands can be directed to this window.
    SetCurrent();

    gboolean status = g_mutex_trylock(m_renderMutex);
    if (!status)
        return;

    // Render base image.
    Render();

    // render highlights
    if (!m_curHighlights.empty())
    {
        HiglightSelectionList::iterator it = m_curHighlights.begin();
        while (it != m_curHighlights.end())
        {
            Highlight *selection = (Highlight *) (*it);

            RenderHighlight(selection);

            it++;
        }
    }

    // render hotspots
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
                HotSpotActiveRegion *hotspotRegion =
                        (HotSpotActiveRegion *) (*it);
                RenderHotSpot(*hotspotRegion);
            }

            it++;
        }
    }

    // Show the OpenGL back buffer on this window.
    SwapBuffers();

    // Unlock the render mutex.
    g_mutex_unlock( m_renderMutex);
}

void RIRemote::OnKeyUp(wxKeyEvent &event)
{
    int code = event.GetKeyCode();
    RILOG_DEBUG("Handling wxKeyEvent Up: %d.\n", code);
    window_handle_key_event(code, true);
}

void RIRemote::OnKeyDown(wxKeyEvent &event)
{
    int code = event.GetKeyCode();
    RILOG_DEBUG("Handling wxKeyEvent Down: %d.\n", code);
    window_handle_key_event(code, false);
}

void RIRemote::OnEnterWindow(wxMouseEvent &event)
{
    // Set focus to the wxGLCanvas; this facilitates
    // key event processing.
    SetFocus();
}

bool RIRemote::inCurrentSelection(HotSpotActiveRegion *region)
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

HotSpotActiveRegion * RIRemote::inCurrentSelection(wxPoint pos)
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
        }
    }

    return retVal;
}

void RIRemote::OnMouseMotion(wxMouseEvent &event)
{
    wxPoint pos = event.GetPosition();

    // Check to see if we ever created an Image Map (if remote.im was not found).
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
                double x = 0;
                double y = 0;
                double width = 0;
                double height = 0;
                HotSpotActiveRegion *hotspotRegion =
                        (HotSpotActiveRegion *) (*it);
                (void) hotspotRegion->GetPosition(&x, &y);
                (void) hotspotRegion->GetExtent(&width, &height);
                if ((pos.x < x) || (pos.x > (x + width)) || (pos.y < y)
                        || (pos.y > (y + height)))
                {
                    // Unhighlight previous display.
                    bRepaint = true;

                    // And remove it from the current selection.
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
            double x = 0;
            double y = 0;
            double width = 0;
            double height = 0;
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

                bRepaint = true;

                // Mouse is in a new active region.
                //RILOG_ERROR("Right before code that crashes, x = %f\n", x);
                m_curSelection.push_back(*it);
            }
        }
        it++;
    }

    if (bRepaint)
    {
        PaintMe();
    }
}

void RIRemote::OnMouseButtonDown(wxMouseEvent &event)
{
    wxPoint pos = event.GetPosition();

    // Check to see if we ever created an Image Map (if remote.im was not found).
    if (m_imageMap == NULL)
        return;

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

void RIRemote::OnMouseButtonUp(wxMouseEvent &event)
{
    (void) event.GetPosition();

    // Check to see if we ever created an Image Map (if remote.im was not found).
    if (m_imageMap == NULL)
        return;

    // Unhighlight any previous selections.
    RemoveHighlights();

    PaintMe();

    // Release the previous key, regardless of where the pointer currently is...
    if (m_currentKeyDown != NO_CURRENT_KEY_PRESSED)
    {
        RILOG_DEBUG("Mouse Button Up: Key Code: %d\n", m_currentKeyDown);
        window_handle_remote_event((ri_event_code) m_currentKeyDown,
                MOUSE_BUTTON_UP);
        m_currentKeyDown = NO_CURRENT_KEY_PRESSED;
    }
}

void RIRemote::Init()
{
    int i;

    // Initialize the Image Map.
    // Retrieve value from the configuration file.
    const char *configValue;
    const char *imageMapFile = RIRemote::DEFAULT_IMAGE_MAP;
    if ((configValue = ricfg_getValue("twb_config",
            (char *) "RI.Emulator.Remote.imageMap")) != NULL || (configValue
            = ricfg_getValue("RIPlatform",
                    (char *) "RI.Emulator.Remote.imageMap")) != NULL)
    {
        imageMapFile = configValue;
    }

    ImageMapManager *theManager = ImageMapManager::getInstance();
    m_imageMap = theManager->LoadImageMap(imageMapFile);
    if (!m_imageMap)
    {
        RILOG_ERROR("Unable to load Remote Image Map %s.\n", imageMapFile);
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
}

void RIRemote::InitOpenGL()
{
    wxSize size;
    size = GetClientSize();

    // Initialize common OpenGL state.
    RIGLCanvas::InitOpenGL(size.GetWidth(), size.GetHeight());

    m_blendSupported = RIGLCanvas::IsExtensionSupported(
            "GL_EXT_blend_equation_separate");
}

void RIRemote::Render()
{
    GLubyte *image = NULL;
    GLuint width, height;

    // Make sure there is an image map to render.
    if (m_imageMap == NULL)
        return;

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

void RIRemote::RenderHotSpot(HotSpotActiveRegion &region)
{
    double x_1, y_1, x_2, y_2;
    double width, height;

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
    //RILOG_ERROR("About to draw line x_1 %f, y_1 %f, x_2 %f, y_2 %f\n",
    //      x_1, y_1, x_2, y_2);
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

void RIRemote::HighlightSelection(HotSpotActiveRegion &region)
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

void RIRemote::RemoveHighlights()
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

void RIRemote::RenderHighlight(Highlight *highlight)
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
