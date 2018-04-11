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

#include "ui_window_common.h"
#include "ui_opengl_common.h"
#include "ui_window.h"
#include <ri_log.h>

// Logging category
#define RILOG_CATEGORY g_uiCat
static log4c_category_t* g_uiCat = NULL;
static char* LOG_CAT = "RI.UI.Window";

static gboolean g_isRunning = FALSE;

// Win32 method forward declaration
//
static int window_message_loop(UIInfo* uiInfo);
static GHashTable *key_event_mapping = NULL;
static void (*key_event_callback)(ri_event_type type, ri_event_code code) = NULL;
static void
        (*key_event_callback_mfg)(ri_event_type type, ri_event_code code) = NULL;
static void (*display_event_callback)(int keyCode) = NULL;
static void window_initialize_key_events();
LRESULT CALLBACK WndProc(HWND hWnd, UINT message, WPARAM wParam, LPARAM lParam);
static void window_destroy(WindowInfo* windowInfo);

/**
 * Initialize the Win32 window manager.
 *
 * @param uiInfo display information
 *
 * @return The window's identifier should be returned if providing an
 * external Window handle. Return <b>0</b> if the RI Platform should
 * create the window dynamically.
 */
uint32_t window_init(UIInfo* uiInfo)
{
    if (NULL == g_uiCat)
        g_uiCat = log4c_category_get(LOG_CAT);

    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    gulong win = window_open(uiInfo, uiInfo->pWindowInfo->width,
            uiInfo->pWindowInfo->height);

    // Set flag to indicate window was created externally
    uiInfo->pWindowInfo->is_created_internally = FALSE;

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    // Return the value which was set when window was created
    return win;
}

/**
 * Creates (opens) an window that is set up for use with OpenGL calls.
 *
 * @param uiInfo    display information
 * @param width width of the new window
 * @param height height of the new window
 *
 * @return the newly created window id, <code>0</code> if the window was not
 * created.
 */
uint32_t window_open(UIInfo* uiInfo, gint width, gint height)
{
    if (NULL == g_uiCat)
        g_uiCat = log4c_category_get(LOG_CAT);

    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    if (NULL == uiInfo->pWindowInfo->pWindowOSInfo)
    {
        window_init_screen(uiInfo->pWindowInfo);
    }

    uiInfo->pWindowInfo->pWindowOSInfo->initialized = FALSE;

    // Create a thread to perform windows message loop processing
    GError* err;
    uiInfo->pWindowInfo->width = width;
    uiInfo->pWindowInfo->height = height;
    uiInfo->event_thread = g_thread_create((GThreadFunc) window_message_loop,
            uiInfo, FALSE, &err);

    // Wait for a signal from the application indicating that the window
    // identifier's available.
    g_mutex_lock(uiInfo->pWindowInfo->pWindowOSInfo->initMutex);
    while (!uiInfo->pWindowInfo->pWindowOSInfo->initialized)
        g_cond_wait(uiInfo->pWindowInfo->pWindowOSInfo->initCond,
                uiInfo->pWindowInfo->pWindowOSInfo->initMutex);
    g_mutex_unlock(uiInfo->pWindowInfo->pWindowOSInfo->initMutex);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return uiInfo->pWindowInfo->pWindowOSInfo->winRetVal;
}

/**
 * Most applications contain a single thread that creates windows. A typical
 * application registers the window class for its main window, creates and
 * shows the main window, and then starts its message loop � all in the
 * WinMain function.  For the GstDisplay Plugin, a thread is launched to
 * call this method which will create the window and run the message loop instead
 * of doing this from the WinMain function.
 *
 * The system automatically creates a message queue for each thread. If the thread
 * creates one or more windows, a message loop must be provided; this message loop
 * retrieves messages from the thread's message queue and dispatches them to the
 * appropriate window procedures.  Because the system directs messages to individual
 * windows in an application, a thread must create at least one window before
 * starting its message loop.
 *
 * Windows is an event-driven operating system, each time an event occurs,
 * a message will be generated by the operating system. The message contains
 * all the information needed to process the event, including a handle to its
 * destination window. Some of those messages (usually the ones generated by
 * user interacting with a window) will be sent to the message queue of the
 * program that owns that window. The message queue is created by the operating
 * system once a program begins to run. Each running program has a chunk of code
 * called message loop that checks its message queue and dispatches available
 * messages to their destination windows (a program may own multiple windows).
 * Each window a program created has a function called window procedure that
 * handles those messages. In addition to the messages that go through the message
 * queue, the operating system can also send messages directly (namely bypassing
 * the message queue) to the relevant window procedure.
 *
 * @param uiInfo    display information
 */
static int window_message_loop(UIInfo* uiInfo)
{
    if (NULL == g_uiCat)
        g_uiCat = log4c_category_get(LOG_CAT);

    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    int rc = 0;

    // Need to create the window here in this thread so that
    // the message loop can reside here and receive events from
    // the operating system since Win32 delivers the events to
    // the thread which creates the window
    //
    gulong winRetVal = window_create(uiInfo->pWindowInfo->width,
            uiInfo->pWindowInfo->height, uiInfo->pWindowInfo->is_fixed);
    if (0 == winRetVal)
    {
        // Don't bother starting loop because window creation failed
        RILOG_ERROR("%s -- window creation failed\n", __FUNCTION__);

        RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
        return rc;
    }

    opengl_init_environment(uiInfo, winRetVal);

    // Signal that initialization has completed and window is ready
    uiInfo->pWindowInfo->pWindowOSInfo->winRetVal = winRetVal;

    g_mutex_lock(uiInfo->pWindowInfo->pWindowOSInfo->initMutex);
    uiInfo->pWindowInfo->pWindowOSInfo->initialized = TRUE;
    g_cond_signal(uiInfo->pWindowInfo->pWindowOSInfo->initCond);
    g_mutex_unlock(uiInfo->pWindowInfo->pWindowOSInfo->initMutex);

    // The PeekMessage function, unlike its GetMessage cousin, will NOT wait when
    // the message queue is empty, which makes the while-loop a busy loop that
    // consumes lots of CPU cycles. In most OpenGL programs such as video games,
    // however, fast rendering is the key and the program is usually not run
    // in parallel with other major programs, therefore we do have a large fraction
    // of the CPU cycles and we don't want to waste time sending WM_PAINT messages
    // to the callback function for the handler to do the rendering each time.
    // In that case, it is a lot more efficient to render graphics directly in
    // the else-branch of the new (busy) message loop.  That's when PeekMessage()
    // should be used instead of GetMessage()

    MSG msg;
    BOOL quit = FALSE;
    g_isRunning = TRUE;

    while (!quit)
    {
        (void) GetMessage(&msg, (HWND) winRetVal, 0, 0);

        // Process the close message here because we have the uiInfo plugin
        // and we need to terminate this loop and thread
        switch (msg.message)
        {
        case WM_CLOSE:
        {
            RILOG_DEBUG("%s -- message queue received close message\n",
                    __FUNCTION__);

            g_mutex_lock(uiInfo->window_lock);
            window_destroy(uiInfo->pWindowInfo);
            g_mutex_unlock(uiInfo->window_lock);

            quit = TRUE;
            break;
        }
            // Initiate a refresh of the display when a paint event is received
        case WM_PAINT:
        {
            // Tell windows that the window has been repainted
            // and to quit sending paint events
            // Need to acquire the window lock here while accessing window
            g_mutex_lock(uiInfo->window_lock);

            if ((NULL != uiInfo->pWindowInfo)
                    && (0 != uiInfo->pWindowInfo->win) && (g_isRunning))
            {
                (void) ValidateRect((HWND) winRetVal, NULL);

                opengl_render_display(uiInfo);
            }
            g_mutex_unlock(uiInfo->window_lock);

            break;
        }
        case WM_KEYUP:
        case WM_KEYDOWN:
        {
            gboolean lookup_succeeded = FALSE;
            ri_event_code code = RI_OCRC_LAST;

            lookup_succeeded = g_hash_table_lookup_extended(key_event_mapping,
                    (gconstpointer) msg.wParam, NULL, (gpointer) & code);

            if (lookup_succeeded && key_event_callback != NULL)
            {
                if (msg.message == WM_KEYUP)
                {
                    (*key_event_callback)(RI_EVENT_TYPE_RELEASED, code);
                }
                else // WM_KEYDOWN
                {
                    (*key_event_callback)(RI_EVENT_TYPE_PRESSED, code);
                }
            }
            if (lookup_succeeded && key_event_callback_mfg != NULL)
            {
                if (msg.message == WM_KEYUP)
                {

                    (*key_event_callback_mfg)(RI_EVENT_TYPE_RELEASED, code);
                }
                else // WM_KEYDOWN
                {

                    (*key_event_callback_mfg)(RI_EVENT_TYPE_PRESSED, code);
                }
            }
            break;
        }
        default:
        {
            // If application must obtain character input from the user,
            // include the TranslateMessage function in the loop. TranslateMessage
            // translates virtual-key messages into character messages.
            //TranslateMessage(&msg);

            // Forward other messages on to WndProc for default processing
            (void) DispatchMessage(&msg);
        }
        }
    }

    rc = msg.wParam;

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return rc;
}

/**
 * Callback function which handles Win32 window related event messages.
 * A pointer to this function is used when creating the window.
 *
 * NOTE: This method handles WM_CLOSE, so a WM_DESTROY message will
 * never be received by this method.
 *
 * @param hWnd    handle to associated window
 * @param message window related event information
 * @param wParam  additional msg info dependent on msg type
 * @param lParam  additional msg info dependent on msg type
 * @return returns 0 for all defined cases, otherwise
 * default window processing value
 */
LRESULT CALLBACK WndProc(HWND hWnd, UINT message, WPARAM wParam, LPARAM lParam)
{
    if (NULL == g_uiCat)
        g_uiCat = log4c_category_get(LOG_CAT);

    // Handle only the message types that needs a manual handling,
    // and pass everything else to the default handler -
    // DefWindowProc - provided by the Win32 API.
    switch (message)
    {
    // User has clicked close button on window
    case WM_CLOSE:
        RILOG_DEBUG("%s -- callback close message received\n", __FUNCTION__);

        // Doesn't seem to work with this window's message queue which
        // uses PeekMessage() to support OpenGL
        //PostQuitMessage(0);

        // The close message is not received by the message loop
        // but is by this callback method.  Need to forward this
        // message to message loop so loop will perform shutdown
        (void) PostMessage(hWnd, message, wParam, lParam);
        break;

    case WM_KEYDOWN:
        switch (wParam)
        {
        case VK_ESCAPE:
            RILOG_DEBUG("%s -- escape key received called\n", __FUNCTION__);

            // Doesn't seem to work with this window's message queue which
            // uses PeekMessage() to support OpenGL
            PostQuitMessage(0);

            // Post WM_CLOSE message to message loop so loop will perform shutdown
            //PostMessage(hWnd, WM_CLOSE, wParam, lParam);
            break;
        }
        break;

        // No special processing for this message, let windows
        // perform default processing
    default:
        return DefWindowProc(hWnd, message, wParam, lParam);
    }
    return 0;
}

/**
 * Creates the Win32 window
 *
 * @param width      width in pixels of window to create
 * @param height     height in pixels of window to create
 * @param is_fixed   indicates if a window without borders
 *                   (inmoveable window) is to be created.
 *
 * @return id of created window, 0 if window creation fails.
 */
gulong window_create(guint32 width, guint32 height, gboolean is_fixed)
{
    // Register window class
    WNDCLASS wc;
    wc.style = CS_OWNDC;
    wc.lpfnWndProc = WndProc;
    wc.cbClsExtra = 0;
    wc.cbWndExtra = 0;
    wc.hInstance = GetModuleHandle(NULL);
    wc.hIcon = LoadIcon(NULL, IDI_APPLICATION);
    wc.hCursor = LoadCursor(NULL, IDC_ARROW);
    wc.hbrBackground = (HBRUSH) GetStockObject(BLACK_BRUSH);
    wc.lpszMenuName = NULL;
    wc.lpszClassName = "GstDisplayWin";
    (void) RegisterClass(&wc);

    // Open visible window with border and title bar
    DWORD style = WS_CAPTION | WS_POPUPWINDOW | WS_VISIBLE | WS_CLIPCHILDREN
            | WS_CLIPSIBLINGS | CS_OWNDC;
    if (is_fixed)
    {
        // Open a visible fixed window with no title bar
        style = WS_POPUP | WS_VISIBLE;
    }

    // Create main window
    gulong win = (gulong) CreateWindow("GstDisplayWin",
            "GstDisplay OpenGL Window", style, 0, 0, width, height, NULL, NULL,
            wc.hInstance, NULL);

    return win;
}

/**
 * Get information about the display.
 * Populates the displayInfo structure with information about the
 * display screen.  This function create temporary invisible window to
 * obtain device context in order to get display information.
 *
 * @param displayInfo the DisplayInfo structure to be written to (must already exist).
 *
 * @return <code>TRUE</code> if the display information could be obtained,
 * <code>FALSE</code> otherwise.
 */
gboolean window_init_display(UIInfo* uiInfo)
{
    if (NULL == g_uiCat)
        g_uiCat = log4c_category_get(LOG_CAT);

    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    DisplayInfo* displayInfo = uiInfo->pDisplayInfo;

    // Assume failure
    gboolean bRetVal = FALSE;

    // On Win32, need a open an invisible window in order to get the device context which supplies
    // the display characteristics which will be used when actual window is opened
    //
    // Open an temporary invisible window which is 25 x 25
    WNDCLASS wc;
    wc.style = CS_OWNDC;
    wc.lpfnWndProc = WndProc;
    wc.cbClsExtra = 0;
    wc.cbWndExtra = 0;
    wc.hIcon = LoadIcon(NULL, IDI_APPLICATION);
    wc.hCursor = LoadCursor(NULL, IDC_ARROW);
    wc.hbrBackground = (HBRUSH) GetStockObject(BLACK_BRUSH);
    wc.lpszMenuName = NULL;
    wc.lpszClassName = "tmp";
    wc.hInstance = GetModuleHandle(NULL);
    (void) RegisterClass(&wc);
    HWND hWnd = CreateWindow("tmp", "tmp", WS_POPUPWINDOW, 0, 0, 25, 25, NULL,
            NULL, wc.hInstance, NULL);

    // Get the device context from temporary window
    if (0 != hWnd)
    {
        HDC hDC = GetDC(hWnd);

        // Initialize the display info using device content
        displayInfo->depth = GetDeviceCaps(hDC, BITSPIXEL);
        displayInfo->width = GetDeviceCaps(hDC, HORZRES);
        displayInfo->height = GetDeviceCaps(hDC, VERTRES);
        displayInfo->widthmm = GetDeviceCaps(hDC, HORZSIZE);
        displayInfo->heightmm = GetDeviceCaps(hDC, VERTSIZE);

        // *TODO* - need to figure out how to get this on Win32 - image byte order
        // Hardcoded to little for now
        displayInfo->endianness = G_LITTLE_ENDIAN;

        // Close the temporary window and delete the device context
        (void) ReleaseDC(hWnd, hDC);
        (void) DestroyWindow(hWnd);

        bRetVal = TRUE;
    }

    if (key_event_mapping == NULL)
    {
        window_initialize_key_events();
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return bRetVal;
}

/**
 * This function allocates memory for the screen information structure.
 *
 * Note that the caller is responsible for freeing the memory allocated for the
 * returned WindowOSInfo structure (by calling opengl_uninit_window_system()).
 *
 * @return pointer to newly allocated instance of WindowOSInfo, <code>NULL</code> if
 * the function failed.
 *
 * This method is called "cl_init_window_system()" in openglonx.c.
 */
void window_init_screen(WindowInfo* windowInfo)
{
    // Allocate screen information structure
    windowInfo->pWindowOSInfo = g_try_new0(WindowOSInfo, 1);

    if (NULL == windowInfo->pWindowOSInfo)
    {
        RILOG_FATAL(-1, "line %d of %s, %s memory allocation failure!\n",
                    __LINE__, __FILE__, __func__);
    }

    // Create a mutex for signaling completion of UI bring-up.
    windowInfo->pWindowOSInfo->initMutex = g_mutex_new();

    // Create a semaphore for signaling completion of UI bring-up;
    windowInfo->pWindowOSInfo->initCond = g_cond_new();
}

/**
 * Sends message to close the window associated with info supplied
 *
 * @param windowInfo    information about the window
 */
void window_close(WindowInfo* windowInfo)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    // Clear the is running flag to eliminate paint requests
    g_isRunning = FALSE;

    // Post a message so window will shutdown
    (void) PostMessage((HWND) windowInfo->win, WM_CLOSE, 0, 0);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Performs the actions necessary to shutdown and destroy
 * the window.
 *
 * @param windowInfo information about window to destroy
 */
static void window_destroy(WindowInfo* windowInfo)
{
    if (NULL == g_uiCat)
        g_uiCat = log4c_category_get(LOG_CAT);

    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    // Set window id to zero to indicate window has been destroyed
    HWND win = (HWND) windowInfo->win;
    windowInfo->win = 0;

    // Use local copies while destroying and NULL globals
    HDC hDC = windowInfo->pWindowOSInfo->hDC;
    windowInfo->pWindowOSInfo->hDC = NULL;

    HGLRC hRC = windowInfo->pWindowOSInfo->hRC;
    windowInfo->pWindowOSInfo->hRC = NULL;

    // De-select the rendering context
    (void) wglMakeCurrent(hDC, NULL);

    // Release the rendering context
    (void) wglDeleteContext(hRC);

    // Release the device context
    (void) ReleaseDC(win, hDC);

    (void) DestroyWindow(win);

    window_uninit_screen(windowInfo);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Uninitializes the native window system which on Win32 just involves
 * freeing the screen info data structure.
 *
 * @param pWindowOSInfo points to screen info structure allocated by a previous call to
 * window_init_screen().
 */
void window_uninit_screen(WindowInfo* windowInfo)
{
    if ((NULL != windowInfo) && (NULL != windowInfo->pWindowOSInfo))
    {
        g_cond_free(windowInfo->pWindowOSInfo->initCond);
        g_mutex_free(windowInfo->pWindowOSInfo->initMutex);
        g_free(windowInfo->pWindowOSInfo);
        windowInfo->pWindowOSInfo = NULL;
    }
}

/**
 * Set ID, height & width info about the specified window.  Used
 * when the window geometry is updated.
 *
 * @param uiInfo  display information
 * @param win     window ID to update information for
 */
void window_update_info(UIInfo* uiInfo, gulong win)
{
    RECT winSize;
    (void) GetWindowRect((HWND) win, &winSize);
    uiInfo->pWindowInfo->width = winSize.right - winSize.left;
    uiInfo->pWindowInfo->height = winSize.bottom - winSize.top;
}

void window_register_key_event_callback(void(*cb)(ri_event_type type,
        ri_event_code code))
{
    key_event_callback = cb;
}

void window_register_key_event_callback_mfg(void(*cb)(ri_event_type type,
        ri_event_code code))
{
    key_event_callback_mfg = cb;
}

void window_register_display_event_callback(void(*cb)(int keyCode))
{
    display_event_callback = cb;
}

static void window_initialize_key_events()
{
    int i = 0;
    gint windows_key_event_codes[RI_OCRC_LAST] =
    { VK_RETURN, /* RI_VK_ENTER */
    VK_BACK, /* RI_VK_BACK_SPACE */
    VK_TAB, /* RI_VK_TAB */
    VK_UP, /* RI_VK_UP */
    VK_DOWN, /* RI_VK_DOWN */
    VK_LEFT, /* RI_VK_LEFT */
    VK_RIGHT, /* RI_VK_RIGHT */
    VK_HOME, /* RI_VK_HOME */
    VK_END, /* RI_VK_END */
    VK_NEXT, /* RI_VK_PAGE_DOWN */
    VK_PRIOR, /* RI_VK_PAGE_UP */
    VK_F1, /* RI_VK_COLORED_KEY_0 */
    VK_F2, /* RI_VK_COLORED_KEY_1 */
    VK_F3, /* RI_VK_COLORED_KEY_2 */
    VK_F4, /* RI_VK_COLORED_KEY_3 */
    VK_F5, /* RI_VK_GUIDE */
    VK_F6, /* RI_VK_MENU */
    VK_F7, /* RI_VK_INFO */
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
    VK_NUMLOCK, /* RI_VK_PLAY */
    VK_SEPARATOR, /* RI_VK_PAUSE */
    VK_MULTIPLY, /* RI_VK_STOP */
    VK_SUBTRACT, /* RI_VK_REWIND */
    VK_NUMPAD7, /* RI_VK_RECORD */
    VK_NUMPAD8, /* RI_VK_FAST_FWD */
    VK_F8, /* RI_VK_SETTINGS */
    VK_F9, /* RI_VK_EXIT */
    VK_F10, /* RI_VK_CHANNEL_UP */
    VK_F11, /* RI_VK_CHANNEL_DOWN */
    VK_F12, /* RI_VK_ON_DEMAND */
    VK_NUMPAD1, /* RI_VK_RF_BYPASS */
    VK_NUMPAD2, /* RI_VK_POWER */
    VK_NUMPAD3, /* RI_VK_LAST */
    VK_NUMPAD4, /* RI_VK_NEXT_FAVORITE_CHANNEL */
    VK_NUMPAD5, /* RI_VK_LIVE */
    VK_NUMPAD6 /* RI_VK_LIST */
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
 * @param windowInfo    window information
 */
void window_request_repaint(UIInfo* uiInfo)
{
    g_mutex_lock(uiInfo->window_lock);

    // Invalidate rect to cause repaint
    if ((NULL != uiInfo->pWindowInfo) && (0 != uiInfo->pWindowInfo->win)
            && (g_isRunning))
    {
        //RILOG_ERROR("%s -- calling\n", __FUNCTION__);
        (void) InvalidateRect((HWND) uiInfo->pWindowInfo->win, NULL, FALSE);
    }

    g_mutex_unlock(uiInfo->window_lock);
}

/**
 * Synchronizes display with the screen, this is a no-op on windows.
 *
 * @param windowOSInfo  native OS window information
 */
void window_flush_graphics(WindowInfo* windowInfo)
{
    //XSync(display->pWindowOSInfo->osScr.disp, FALSE);
}

/**
 * Exposes a general function for calling XSynchronize, this is a
 * no-op on windows.
 */
void window_set_xsynchronize(WindowInfo* windowInfo, gint synchronous)
{
    //XSynchronize(display->pWindowOSInfo->osScr.disp, synchronous);
}
