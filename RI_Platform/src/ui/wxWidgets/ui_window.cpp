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

#include <wx/wx.h>
#ifdef __WXMSW__
// Header particular for the GDI (Graphics Device Interface).
#include <wx/gdicmn.h>
#endif /* __WXMSW__ */
#ifdef __WXGTK__
// Header particular for the GTK and underlying GDK.
//#include <gdk/gdkx.h>
#endif /* __WXGDK__ */

// Include RI Emulator header files.
#include "ri_log.h"
#include "ri_config.h"
#include "ui_window_common.h"
#include "ui_opengl_common.h"
#include "ui_window.h"
#include "RIEmulatorApp.h"
#include "RIEmulatorFrame.h"
#include "RITvScreen.h"

// Win32 method forward declaration
static int window_message_loop(UIInfo* uiInfo);
static GHashTable *key_event_mapping = NULL;
static void (*key_event_callback)(ri_event_type type, ri_event_code code) = NULL;
static void
        (*key_event_callback_mfg)(ri_event_type type, ri_event_code code) = NULL;
static void window_initialize_key_events();

#define CONTROL_KEY_CODE 308
#define ONE_KEY_CODE 49
static void (*display_event_callback)(int keyCode) = NULL;
static gboolean controlKeyDown = false;

// Logging category.
#define RILOG_CATEGORY g_uiCat
log4c_category_t* g_uiCat = NULL;

// UI configuration list.

#ifdef __WXMSW__
HINSTANCE hInstance = 0;
extern "C" BOOL WINAPI
DllMain (HANDLE hModule, DWORD fdwReason, LPVOID lpReserved)
{
    hInstance = (HINSTANCE)hModule;
    return TRUE;
}
#endif /* __WXMSW__ */

/**
 * Initialize the Win32 window manager.
 *
 * @param uiInfo The display information.
 *
 * @return The window's identifier should be returned if providing an
 * external Window handle. Return <b>0</b> if the RI Platform should
 * create the window dynamically.
 */
uint32_t window_init(UIInfo* uiInfo)
{
    uint32_t windowId = 0; // The window identifier.
    gboolean result = FALSE;

    // Initialize logging for general UI functionality.
    if (g_uiCat == NULL)
        g_uiCat = log4c_category_get("RI.UI");

    // Load and parse the UI configuration file. Check for the $TWB_TOOLROOT/twb.cfg file
    char *uiConfigFile;
    uiConfigFile = ricfg_getValue("RIPlatform",
                    (char *) "RI.Emulator.TWB.config") ;
    RILOG_WARN("window_init called with twbconfig value %s \n" , uiConfigFile);
    if (uiConfigFile)
    {
        if (ricfg_parseConfigFile("twb_config", uiConfigFile)
                == RICONFIG_SUCCESS)
        {
            result = TRUE;
        }
        g_free( uiConfigFile);
    }

    if (result == FALSE)
    {
        RILOG_WARN(
                "Unable to parse UI configuration file. Using platform config file and then default configuration.\n");
    }

    RILOG_INFO("window_init called\n");

    if (uiInfo == NULL)
        return 0;

    // Make sure that the Window specific data is initialized.
    if (NULL == uiInfo->pWindowInfo->pWindowOSInfo)
    {
        // Allocate and populate the pWindowOSInfo data structure.
        window_init_screen(uiInfo->pWindowInfo);
    }

    // Set the application instance.
    RIEmulatorApp *theApp = new RIEmulatorApp(uiInfo);
    wxApp::SetInstance(theApp);

#ifdef __WXMSW__
    // Might need to manually retrieve module handle because no
    // one knows when DllMain is called (it is indeterminate).
    if (hInstance == NULL)
    hInstance = GetModuleHandle(NULL);
#endif /* __WXMSW__ */

    // Get the window device parameters from a configuration file.
    char *configValue;
    gint width = RITvScreen::DEFAULT_DEVICE_WIDTH;
    if ((configValue = ricfg_getValue("twb_config",
            (char *) "RI.Emulator.TvScreen.width")) != NULL || (configValue
            = ricfg_getValue("RIPlatform",
                    (char *) "RI.Emulator.TvScreen.width")) != NULL)
    {
        if ((width = atoi(configValue)) == 0)
            width = RITvScreen::DEFAULT_DEVICE_WIDTH;
    }
    gint height = RITvScreen::DEFAULT_DEVICE_HEIGHT;
    if ((configValue = ricfg_getValue("twb_config",
            (char *) "RI.Emulator.TvScreen.height")) != NULL || (configValue
            = ricfg_getValue("RIPlatform",
                    (char *) "RI.Emulator.TvScreen.height")) != NULL)
    {
        if ((height = atoi(configValue)) == 0)
            height = RITvScreen::DEFAULT_DEVICE_HEIGHT;
    }
    RILOG_INFO("Device screen resolution: %d x %d\n", width, height);

    uiInfo->pWindowInfo->pWindowOSInfo->m_initialized = FALSE;

    // Create a thread to perform windows message loop processing.
    GError* err;
    uiInfo->pWindowInfo->width = width;
    uiInfo->pWindowInfo->height = height;
    uiInfo->event_thread = g_thread_create((GThreadFunc) window_message_loop,
            uiInfo, FALSE, &err);

    // Wait for a signal from the application indicating that the window
    // identifier's available.
    g_mutex_lock(uiInfo->pWindowInfo->pWindowOSInfo->m_initMutex);
    while (!uiInfo->pWindowInfo->pWindowOSInfo->m_initialized)
        g_cond_wait(uiInfo->pWindowInfo->pWindowOSInfo->m_initCond,
                uiInfo->pWindowInfo->pWindowOSInfo->m_initMutex);
    g_mutex_unlock(uiInfo->pWindowInfo->pWindowOSInfo->m_initMutex);

    // We can now retrieve the window identifier that has been created.
    windowId = theApp->GetTvScreen()->GetId();

    // Enable the rendering loop.
    theApp->GetTvScreen()->ActivateRenderLoop(true);

    RILOG_INFO("window_init returning Window ID: %d\n", windowId);

    return windowId;
}

/**
 * Creates (opens) a window that is set up for use with OpenGL calls.
 *
 * @param uiInfo The display information.
 * @param width The width of the new window.
 * @param height The height of the new window.
 *
 * @return The newly created window id is returned; otherwise, <code>0</code>
 * is returned if the window was not created.
 */
uint32_t window_open(UIInfo* uiInfo, gint width, gint height)
{
    g_print("window_open called\n");

    // Set the application instance.
    wxApp::SetInstance(new RIEmulatorApp(uiInfo));

#ifdef __WXMSW__
    // Might need to manually retrieve module handle because no
    // one knows when DllMain is called (it is indeterminate).
    if (hInstance == NULL)
    hInstance = GetModuleHandle(NULL);
#endif /* __WXMSW__ */

    uiInfo->pWindowInfo->pWindowOSInfo->m_initialized = FALSE;

    // Create a thread to perform windows message loop processing.
    GError* err;
    uiInfo->pWindowInfo->width = width;
    uiInfo->pWindowInfo->height = height;
    uiInfo->event_thread = g_thread_create((GThreadFunc) window_message_loop,
            uiInfo, FALSE, &err);

    // Wait for a signal from the application indicating that the window
    // identifier's available.
    g_mutex_lock(uiInfo->pWindowInfo->pWindowOSInfo->m_initMutex);
    while (!uiInfo->pWindowInfo->pWindowOSInfo->m_initialized)
        g_cond_wait(uiInfo->pWindowInfo->pWindowOSInfo->m_initCond,
                uiInfo->pWindowInfo->pWindowOSInfo->m_initMutex);
    g_mutex_unlock(uiInfo->pWindowInfo->pWindowOSInfo->m_initMutex);

    // Window id isn't used here so just return 0
    return 0;
}

/**
 * Initialize wxWidgets and begin main loop execution.
 *
 * @param uiInfo The display information
 */
static int window_message_loop(UIInfo* uiInfo)
{
    int rc = 0;

#ifdef __WXMSW__
    // Initialize wxWidgets. Begins main loop of execution, dispatching
    // events through wxTheApp.
    rc = wxEntry(hInstance/*GetModuleHandle(NULL)*/, NULL, NULL, SW_SHOW);
#endif /* __WXMSW__ */
#if defined(__WXX11__) || defined(__WXGTK__)
    // Initialize wxWidgets
    int argc = 0;
    wxChar **argv = NULL;
    rc = wxEntry(argc, argv);
#endif /* __WXX11__ */

    // XXX - not sure if this is ever reached.
    return rc;
}

void window_handle_close_event(UIInfo* uiInfo)
{
    g_print("Handling close event.\n");
    if (uiInfo)
        window_close(uiInfo->pWindowInfo);
}

void window_handle_paint_event(UIInfo* uiInfo)
{
    //g_print("Handling paint event.\n");

#ifdef __WXMSW__
    RIEmulatorApp & theApp = wxGetApp();
    RITvScreen *tvScreen = theApp.GetTvScreen();
    HWND hWnd = (HWND)tvScreen->GetHandle();

    // Tell windows that the window has been repainted
    // and to quit sending paint events.
    (void) ValidateRect(hWnd, NULL);
#endif /* __WXMSW__ */
    // TODO - need to determine what to do for Linux.

    // Need to acquire the window lock here while accessing window.
    g_mutex_lock(uiInfo->window_lock);

    // Call OpenGL rendering for RI Platform.
    opengl_render_display(uiInfo);

    g_mutex_unlock(uiInfo->window_lock);
}

void window_handle_key_event(int keyCode, bool keyUp)
{
    //g_print("Handling key event: %d\n", keyCode);

    gboolean lookup_succeeded = FALSE;
    ri_event_code code = RI_OCRC_LAST;

    //use ctrl+integer to fake video output port connect/disconnect...
    if(keyCode != CONTROL_KEY_CODE && !keyUp && controlKeyDown && display_event_callback != NULL)
    {
    	(*display_event_callback)(keyCode - ONE_KEY_CODE);
        return;
    }
    if(keyCode == CONTROL_KEY_CODE)
    {
    	if(keyUp)
    	{
    		controlKeyDown = false;
    	}
    	else
    	{
    		controlKeyDown = true;
    	}
    	return;
    }

    // The following doesn't work with the g++ compiler; it appears that
    // the C++ signature for this routine is different than the C version.
    //lookup_succeeded = g_hash_table_lookup_extended(key_event_mapping,
    //                                                (gconstpointer) key,
    //                                                NULL,
    //                                                (gpointer) &code);

    unsigned int key = keyCode;
    ri_event_code *value = g_new0(ri_event_code, 1);
    lookup_succeeded = g_hash_table_lookup_extended(key_event_mapping,
            (gconstpointer) key, NULL, (void **) (&(*value)));
    code = *value;



    if (lookup_succeeded && key_event_callback != NULL)
    {
        if (keyUp) // Process key up event.
        {
            (*key_event_callback)(RI_EVENT_TYPE_RELEASED, code);
        }
        else // Process key down event.
        {
            (*key_event_callback)(RI_EVENT_TYPE_PRESSED, code);
        }
    }
    if (lookup_succeeded && key_event_callback_mfg != NULL)
    {
        if (keyUp) // Process key up event.
        {
            printf("processed key up event  for mfg %d \n", code);

            (*key_event_callback_mfg)(RI_EVENT_TYPE_RELEASED, code);

        }
        else // Process key down event.
        {
            printf("processed key down event for mfg %d \n", code);

            (*key_event_callback_mfg)(RI_EVENT_TYPE_PRESSED, code);

        }
    }
    g_free(value);
}

void window_handle_remote_event(ri_event_code remoteCode, bool buttonUp)
{
    //g_print("Handling remote event: %d\n", keyCode);

    // Note that there isn't any translation for the Remote key code
    // since we should be using the RI virtual key values directly.

    if (key_event_callback != NULL)
    {
        if (buttonUp) // Process remote key up event.
        {
            (*key_event_callback)(RI_EVENT_TYPE_RELEASED, remoteCode);
        }
        else // Process remote key down event.
        {
            (*key_event_callback)(RI_EVENT_TYPE_PRESSED, remoteCode);
        }
    }
    if (key_event_callback_mfg != NULL)
    {
        if (buttonUp) // Process remote key up event.
        {
            (*key_event_callback_mfg)(RI_EVENT_TYPE_RELEASED, remoteCode);
        }
        else // Process remote key down event.
        {
            (*key_event_callback_mfg)(RI_EVENT_TYPE_PRESSED, remoteCode);
        }
    }
}

/**
 * Creates the wxWidgets window.
 * <p>
 * Currently not used by implementation.
 * </p>
 *
 * @param width The width in pixels of window to create.
 * @param height The height in pixels of window to create.
 * @param is_fixed Flag indicating if a window without borders
 * (inmoveable window) is to be created.
 *
 * @return The id of the created window is returned; otherwise,
 * <b>0</b> is returned if window creation fails.
 */
gulong window_create(guint32 width, guint32 height, gboolean is_fixed)
{
    return 0;
}

/**
 * Get information about the display.
 * <p>
 * Populates the <code>DisplayInfo</code> structure with information about the
 * display screen.
 * </p>
 *
 * @param displayInfo The <code>DisplayInfo</code> structure to be written
 * to (must already exist).
 *
 * @return <code>TRUE</code> is returned if the display information could be obtained,
 * <code>FALSE</code> otherwise.
 */
gboolean window_init_display(UIInfo* uiInfo)
{
    wxSize size;
    wxSize sizemm;
    int depth;

    DisplayInfo* displayInfo = uiInfo->pDisplayInfo;
    // Assume failure
    gboolean bRetVal = FALSE;

    if (g_uiCat == NULL)
        g_uiCat = log4c_category_get("RI.UI");
    RILOG_DEBUG("%s -- called\n", __FUNCTION__);

#if defined(__WXMSW__)
    // On Win32, take advantage of the GDI provided functions.
    size = wxGetDisplaySize();
    sizemm = wxGetDisplaySizeMM();
    depth = wxDisplayDepth();

    // Initialize the display info using device content
    displayInfo->depth = depth;
    displayInfo->width = size.GetWidth();
    displayInfo->height = size.GetHeight();
    displayInfo->widthmm = sizemm.GetWidth();
    displayInfo->heightmm = sizemm.GetHeight();

    // TODO - need to figure out how to get this on Win32 - image byte order
    // hardcoded to little for now.
    displayInfo->endianness = G_LITTLE_ENDIAN;
#endif /* __WXMSW__ */
#if defined(__WXX11__) || defined(__WXGTK__)
    // On Linux, we need to use the DisplayWidth, DisplayHeight, DisplayWidthMM, DisplayHeightMM and
    // DefaultDepthOfScreen routines. The wxGetDisplayxxx calls won't work yet because wxWidgets has
    // not yet been started.
    Display *display = XOpenDisplay(NULL);
    if (display == NULL)
    {
        RILOG_FATAL(-1, "XOpenDisplay returned NULL. Make sure your DISPLAY environment variable is set correctly\n");
    }
    else
    {
        Screen *screen = DefaultScreenOfDisplay(display);
        int screenNum = DefaultScreen(display);

        int width, height, widthmm, heightmm;
        width = DisplayWidth(display, screenNum);
        height = DisplayHeight(display, screenNum);
        widthmm = DisplayWidthMM(display, screenNum);
        heightmm = DisplayHeightMM(display, screenNum);
        depth = DefaultDepthOfScreen(screen);

        // Initialize the display info using device content
        displayInfo->depth = depth;
        displayInfo->width = width;
        displayInfo->height = height;
        displayInfo->widthmm = widthmm;
        displayInfo->heightmm = heightmm;

        displayInfo->endianness = (ImageByteOrder(display) == LSBFirst) ? G_LITTLE_ENDIAN : G_BIG_ENDIAN;

        XCloseDisplay(display);
    }
#endif /* __WXX11__ */

    bRetVal = TRUE;

    // Initialize the key event handling.
    if (key_event_mapping == NULL)
    {
        window_initialize_key_events();
    }

    return bRetVal;
}

/**
 * This function allocates memory for the screen information structure.
 * <p>
 * Note that the caller is responsible for freeing the memory allocated for the
 * returned WindowOSInfo structure (by calling opengl_uninit_window_system()).
 * </p>
 *
 * @return A pointer to the newly allocated instance of WindowOSInfo,
 * <code>NULL</code> if the function failed.
 */
void window_init_screen(WindowInfo* windowInfo)
{
    // Allocate screen information structure
    windowInfo->pWindowOSInfo = g_new0(WindowOSInfo, 1);

    // Create a mutex for signaling completion of UI bring-up.
    windowInfo->pWindowOSInfo->m_initMutex = g_mutex_new();
    // Create a semaphore for signaling completion of UI bring-up;
    windowInfo->pWindowOSInfo->m_initCond = g_cond_new();

#ifdef __WXX11__
    // Use default display.
    windowInfo->pWindowOSInfo->m_display = XOpenDisplay(NULL);
#endif /* __WXX11__ */
#ifdef __WXGTK__
    //pWindowOSInfo->m_display = gdk_x11_get_default_xdisplay();
    windowInfo->pWindowOSInfo->m_display = XOpenDisplay(NULL);
#endif /* __WXGTK__ */
}

/**
 * Closes the window associated with info supplied.
 *
 * @param windowInfo Information about the window
 */
void window_close(WindowInfo* windowInfo)
{
    // De-select the rendering context; no known way to do this with wxGLCanvas.

    // Release the rendering context; no known way to do this with wxGLCanvas.

    // Release the device context; no corresponding step in wxWidgets.

    // Destroy the window.
    RIEmulatorApp &theApp = wxGetApp();
    RITvScreen *tvScreen = theApp.GetTvScreen();
    delete tvScreen;
    theApp.SetTvScreen(NULL);
}

/**
 * Uninitializes the native window system screen context;
 * <p>
 * In addition to freeing the screen info data structure, it frees the conditional
 * semaphore and mutex used for signaling that the UI application has successfully
 * been initialized.
 * </p>
 *
 * @param pWindowOSInfo A pointer to the screen info structure allocated by a
 * previous call to window_init_screen().
 */
void window_uninit_screen(WindowInfo* windowInfo)
{
    if ((NULL != windowInfo) && (NULL != windowInfo->pWindowOSInfo))
    {
#if defined(__WXX11__) || defined(__WXGTK__)
        // Use default display.
        //XCloseDisplay(pWindowOSInfo->m_display);
#endif /* __WXX11__ */

        g_cond_free(windowInfo->pWindowOSInfo->m_initCond);
        g_mutex_free(windowInfo->pWindowOSInfo->m_initMutex);
        g_free(windowInfo->pWindowOSInfo);
        windowInfo->pWindowOSInfo = NULL;
    }
}

/**
 * Set ID, height & width info about the specified window.  Used
 * when the window geometry is updated.
 *
 * @param uiInfo The display information.
 * @param win The window ID to update information for.
 */
void window_update_info(UIInfo* uiInfo, gulong win)
{
    int width, height;

    wxWindow *window = wxWindow::FindWindowById(win, NULL);
    window->GetClientSize(&width, &height);
    uiInfo->pWindowInfo->win = win;
    uiInfo->pWindowInfo->width = width;
    uiInfo->pWindowInfo->height = height;
}

void window_register_key_event_callback(void(*cb)(ri_event_type type,
        ri_event_code code))
{
    key_event_callback = cb;
}

void window_register_key_event_callback_mfg(void(*cb)(ri_event_type type,
        ri_event_code code))
{
    g_print("window_register_key_event_callback_mfg called\n");
    key_event_callback_mfg = cb;
}

void window_register_display_event_callback(void(*cb)(int keyCode))
{
    g_print("window_register_display_event_callback called\n");
    display_event_callback = cb;
}

static void window_initialize_key_events()
{
    int i = 0;
    gint windows_key_event_codes[RI_OCRC_LAST] =
    { WXK_RETURN, /* RI_VK_ENTER */
    WXK_BACK, /* RI_VK_BACK_SPACE */
    WXK_TAB, /* RI_VK_TAB */
    WXK_UP, /* RI_VK_UP */
    WXK_DOWN, /* RI_VK_DOWN */
    WXK_LEFT, /* RI_VK_LEFT */
    WXK_RIGHT, /* RI_VK_RIGHT */
    WXK_HOME, /* RI_VK_HOME */
    WXK_END, /* RI_VK_END */
    WXK_PAGEDOWN, /* RI_VK_PAGE_DOWN */
    WXK_PAGEUP, /* RI_VK_PAGE_UP */
    WXK_F1, /* RI_VK_COLORED_KEY_0 */
    WXK_F2, /* RI_VK_COLORED_KEY_1 */
    WXK_F3, /* RI_VK_COLORED_KEY_2 */
    WXK_F4, /* RI_VK_COLORED_KEY_3 */
    WXK_F5, /* RI_VK_GUIDE */
    WXK_F6, /* RI_VK_MENU */
    WXK_F7, /* RI_VK_INFO */
    0x30, /* RI_VK_0 */
    0x31, /* RI_VK_1 */
    0x32, /* RI_VK_2 */
    0x33, /* RI_VK_3 */
    0x34, /* RI_VK_4 */
    0x35, /* RI_VK_5 */
    0x36, /* RI_VK_6 */
    0x37, /* RI_VK_7 */
    0x38, /* RI_VK_8 */
    0x39, /* RI_VK_9 */
    0x41, /* RI_VK_A */
    0x42, /* RI_VK_B */
    0x43, /* RI_VK_C */
    0x44, /* RI_VK_D */
    0x45, /* RI_VK_E */
    0x46, /* RI_VK_F */
    0x47, /* RI_VK_G */
    0x48, /* RI_VK_H */
    0x49, /* RI_VK_I */
    0x4A, /* RI_VK_J */
    0x4B, /* RI_VK_K */
    0x4C, /* RI_VK_L */
    0x4D, /* RI_VK_M */
    0x4E, /* RI_VK_N */
    0x4F, /* RI_VK_O */
    0x50, /* RI_VK_P */
    0x51, /* RI_VK_Q */
    0x52, /* RI_VK_R */
    0x53, /* RI_VK_S */
    0x54, /* RI_VK_T */
    0x55, /* RI_VK_U */
    0x56, /* RI_VK_V */
    0x57, /* RI_VK_W */
    0x58, /* RI_VK_X */
    0x59, /* RI_VK_Y */
    0x5A, /* RI_VK_Z */
    0xAF, /* RI_VK_VOLUME_UP */
    0xAE, /* RI_VK_VOLUME_DOWN */
    0xAD, /* RI_VK_MUTE */
    WXK_NUMLOCK, /* RI_VK_PLAY */
    WXK_SEPARATOR, /* RI_VK_PAUSE */
    WXK_MULTIPLY, /* RI_VK_STOP */
    WXK_SUBTRACT, /* RI_VK_REWIND */
    WXK_NUMPAD7, /* RI_VK_RECORD */
    WXK_NUMPAD8, /* RI_VK_FAST_FWD */
    WXK_F8, /* RI_VK_SETTINGS */
    WXK_F9, /* RI_VK_EXIT */
    WXK_F10, /* RI_VK_CHANNEL_UP */
    WXK_F11, /* RI_VK_CHANNEL_DOWN */
    WXK_F12, /* RI_VK_ON_DEMAND */
    WXK_NUMPAD1, /* RI_VK_RF_BYPASS */
    WXK_NUMPAD2, /* RI_VK_POWER */
    WXK_NUMPAD3, /* RI_VK_LAST */
    WXK_NUMPAD4, /* RI_VK_NEXT_FAVORITE_CHANNEL */
    WXK_NUMPAD5, /* RI_VK_LIVE */
    WXK_NUMPAD6 /* RI_VK_LIST */
    };

    key_event_mapping = g_hash_table_new(NULL, NULL);
    g_assert(key_event_mapping != NULL);

    for (i = 0; i < RI_OCRC_LAST; i++)
    {
        g_hash_table_insert(key_event_mapping,
                (gpointer) windows_key_event_codes[i], ((gpointer)(
                        (ri_event_code) i)));
    }
}

/**
 * Invalidate the window rectangle so a Windows Paint event
 * will be generated and received in the message loop which
 * will cause the openGL to be rendered.
 *
 * @param windowInfo window information
 */
void window_request_repaint(UIInfo* uiInfo)
{
    WindowInfo* windowInfo = uiInfo->pWindowInfo;

    g_mutex_lock(uiInfo->window_lock);

    // Invalidate rect to cause repaint
    if (0 != windowInfo->win)
    {
        RIEmulatorApp & theApp = wxGetApp();
        RITvScreen *tvScreen = theApp.GetTvScreen();

        if (tvScreen->CanRender())
        {
#ifdef __WXMSW__
            HWND hWnd = (HWND)tvScreen->GetHandle();
            (void) InvalidateRect(hWnd, NULL, FALSE);
#endif /* __WXMSW__ */
#if defined(__WXX11__) || defined(__WXGTK__)
            wxIdleEvent ev;
            wxPostEvent(tvScreen, ev);
            //RILOG_INFO("***** Posted idle event. *****\n");
#endif /* __WXGTK__ */
        }
    }

    g_mutex_unlock(uiInfo->window_lock);
}

/**
 * Synchronizes display with the screen; this is a no-op on
 * Microsoft Windows.
 *
 * @param windowOSInfo Native OS window information.
 */
void window_flush_graphics(WindowInfo* windowInfo)
{
#if defined(__WXX11__) || defined(__WXGTK__)
    XSync(windowInfo->pWindowOSInfo->m_display, FALSE);
#endif /* __WXX11__ */
}

/**
 * Exposes a general function for calling XSynchronize; this is a
 * no-op on Microsoft Windows.
 *
 * @param windowOSInfo Native OS window information.
 * @param synchronous Flag indicating whether synchronize should be a
 * synchronous action
 */
void window_set_xsynchronize(WindowInfo* windowInfo, gint synchronous)
{
#if defined(__WXX11__) || defined(__WXGTK__)
    XSynchronize(windowInfo->pWindowOSInfo->m_display, synchronous);
#endif /* __WXX11__ */
}
