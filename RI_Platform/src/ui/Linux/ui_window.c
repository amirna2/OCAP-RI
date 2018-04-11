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
#include <X11/keysym.h>

// Logging category
#define RILOG_CATEGORY g_uiCat
static log4c_category_t* g_uiCat = NULL;
static char* LOG_CAT = "RI.UI";

typedef struct
{
    unsigned long flags;
    unsigned long functions;
    unsigned long decorations;
    long input_mode;
    unsigned long status;
} MotifWmHints, MwmHints;

#define MWM_HINTS_DECORATIONS   (1L << 1)

// used to indicate that an error has been caught
static gboolean g_bErrorCaught = FALSE;
static gboolean g_isRunning = FALSE;

// someplace to store the original error handler when we replace it with our own
int (*pOriginalErrorHandler)(Display* display, XErrorEvent* xevent);

// Forward Declarations
static gboolean decorate_window(WindowOSInfo* pWindowOSInfo, uint32_t win);
void window_set_xsynchronize(WindowInfo* windowInfo, gint synchronous);
static int window_handle_xerror(Display* display, XErrorEvent* xevent);
static GHashTable *key_event_mapping = NULL;
static void (*key_event_callback)(ri_event_type type, ri_event_code code) = NULL;
static void
        (*key_event_callback_mfg)(ri_event_type type, ri_event_code code) = NULL;
static void (*display_event_callback)(int keyCode) = NULL;
static void window_initialize_key_events();
static void window_handle_xevents(UIInfo* uiInfo);
static int window_message_loop(UIInfo* uiInfo);
static gulong window_create_linux(UIInfo* uiInfo, gint width, gint height);
static int window_paint_loop(UIInfo* uiInfo);
static void window_handle_paint_events(UIInfo* uiInfo);
static void window_destroy(WindowInfo* windowInfo);

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

    // Create a thread to create window & perform painting
    // This thread is not joinable since this thread is doing the shutdown
    GError* err;
    uiInfo->pWindowInfo->width = width;
    uiInfo->pWindowInfo->height = height;
    uiInfo->pWindowInfo->pWindowOSInfo->paint_thread = g_thread_create(
            (GThreadFunc) window_paint_loop, uiInfo, FALSE, &err);

    // Wait for a signal from the application indicating that the window
    // identifier's available.
    g_mutex_lock(uiInfo->pWindowInfo->pWindowOSInfo->initMutex);
    while (!uiInfo->pWindowInfo->pWindowOSInfo->initialized)
        g_cond_wait(uiInfo->pWindowInfo->pWindowOSInfo->initCond,
                uiInfo->pWindowInfo->pWindowOSInfo->initMutex);
    g_mutex_unlock(uiInfo->pWindowInfo->pWindowOSInfo->initMutex);

    // Create a thread to handle window event processing
    // This thread is joinable since the paint thread will make sure this thread
    // is terminated when doing shutdown
    uiInfo->event_thread = g_thread_create((GThreadFunc) window_message_loop,
            uiInfo, TRUE, &err);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);

    return uiInfo->pWindowInfo->pWindowOSInfo->win;
}

/**
 * Method which is the X event processing loop which handles
 * key events.  Terminates when it sees running flag set to false.
 *
 * @param uiInfo    display information
 * @return  always returns 0
 */
static int window_message_loop(UIInfo* uiInfo)
{
    if (NULL == g_uiCat)
        g_uiCat = log4c_category_get(LOG_CAT);

    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    int rc = 0;

    while (g_isRunning)
    {
        g_mutex_lock(uiInfo->window_lock);
        if ((NULL != uiInfo->pWindowInfo) && (0 != uiInfo->pWindowInfo->win))
        {
            window_handle_xevents(uiInfo);
        }
        g_mutex_unlock(uiInfo->window_lock);

        // This sleep is longer than paint loop sleep since key events don't normally
        // come in that fast
        g_usleep(100000);
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return rc;
}

/**
 * Method which is the X expose event processing loop which handles
 * paint requests.  This thread creates the window and also destroys it.
 * Terminates when it sees running flag set to false.
 *
 * @param uiInfo    display information
 * @return  always returns 0
 */
static int window_paint_loop(UIInfo* uiInfo)
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
    gulong winRetVal = window_create_linux(uiInfo, uiInfo->pWindowInfo->width,
            uiInfo->pWindowInfo->height);
    if (0 == winRetVal)
    {
        // Don't bother starting loop because window creation failed
        RILOG_ERROR("%s -- window creation failed\n", __FUNCTION__);
        return rc;
    }

    // Signal that initialization has completed and window is ready
    uiInfo->pWindowInfo->pWindowOSInfo->win = winRetVal;

    g_mutex_lock(uiInfo->pWindowInfo->pWindowOSInfo->initMutex);
    uiInfo->pWindowInfo->pWindowOSInfo->initialized = TRUE;
    g_cond_signal(uiInfo->pWindowInfo->pWindowOSInfo->initCond);
    g_mutex_unlock(uiInfo->pWindowInfo->pWindowOSInfo->initMutex);

    g_isRunning = TRUE;

    while (g_isRunning)
    {
        g_mutex_lock(uiInfo->window_lock);
        if ((NULL != uiInfo->pWindowInfo) && (0 != uiInfo->pWindowInfo->win))
        {
            window_handle_paint_events(uiInfo);
        }
        g_mutex_unlock(uiInfo->window_lock);

        //g_usleep (100000);
        g_usleep(100);
    }

    // Wait until event thread exits
    g_thread_join(uiInfo->event_thread);

    // Close down window
    g_mutex_lock(uiInfo->window_lock);

    window_destroy(uiInfo->pWindowInfo);

    // Window has now been destroyed and all events shutdown
    // so release lock and get out of here
    g_mutex_unlock(uiInfo->window_lock);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return rc;
}

/**
 * Initializes X Display and creates the window.
 *
 * @param   uiInfo  display information
 * @param   width       width in pixels of window to create
 * @param   height  height in pixels of window to create
 * @return  id of window created
 */
static gulong window_create_linux(UIInfo* uiInfo, gint width, gint height)
{
    if (NULL == g_uiCat)
        g_uiCat = log4c_category_get(LOG_CAT);

    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    // Initialize X for multiple thread use, this call needs to be the first
    // X lib call made
    if (0 == XInitThreads())
    {
        RILOG_ERROR("%s -- unable to initialize X for concurrent thread use\n",
                __FUNCTION__);
    }

    // Get the display to use
    uiInfo->pWindowInfo->pWindowOSInfo->disp = XOpenDisplay(NULL);
    if (NULL == uiInfo->pWindowInfo->pWindowOSInfo->disp)
    {
        RILOG_ERROR("%s -- unable to open X display\n", __FUNCTION__);
    }

    // get screen information
    uiInfo->pWindowInfo->pWindowOSInfo->screen = DefaultScreenOfDisplay(
            uiInfo->pWindowInfo->pWindowOSInfo->disp);
    uiInfo->pWindowInfo->pWindowOSInfo->screen_num = DefaultScreen(
            uiInfo->pWindowInfo->pWindowOSInfo->disp);
    uiInfo->pWindowInfo->pWindowOSInfo->root = DefaultRootWindow(
            uiInfo->pWindowInfo->pWindowOSInfo->disp);

    gint piAttributeList[] =
    { GLX_RGBA, GLX_RED_SIZE, 8, GLX_GREEN_SIZE, 8, GLX_BLUE_SIZE, 8,
            GLX_DOUBLEBUFFER, 0 // terminator
            };

    uiInfo->pWindowInfo->pWindowOSInfo->visual = glXChooseVisual(
            uiInfo->pWindowInfo->pWindowOSInfo->disp, // dpy
            uiInfo->pWindowInfo->pWindowOSInfo->screen_num, // screen
            piAttributeList); // AttributeList

    uiInfo->pWindowInfo->width = width;
    uiInfo->pWindowInfo->height = height;

    uiInfo->pWindowInfo->pWindowOSInfo->width = width;
    uiInfo->pWindowInfo->pWindowOSInfo->height = height;

    // assume failure
    Window winRetVal = 0;

    XLockDisplay(uiInfo->pWindowInfo->pWindowOSInfo->disp);

    // This is also false since XServer is not remote
    gint synchronous = 0;
    window_set_xsynchronize(uiInfo->pWindowInfo, synchronous);

    // ...create a window
    winRetVal = XCreateSimpleWindow(uiInfo->pWindowInfo->pWindowOSInfo->disp, // display
            uiInfo->pWindowInfo->pWindowOSInfo->root, // parent
            0, // x
            0, // y
            uiInfo->pWindowInfo->width, // width
            uiInfo->pWindowInfo->height, // height
            0, // border_width
            0, // border
            0); // background (black)

    // if we got a window...
    if (0 != winRetVal)
    {
        // Setup to receive events from the window
        XSelectInput(uiInfo->pWindowInfo->pWindowOSInfo->disp, winRetVal,
                ExposureMask | StructureNotifyMask | PointerMotionMask
                        | KeyPressMask | KeyReleaseMask | ButtonPressMask
                        | ButtonReleaseMask);

        // Setup the window to handle close client messages rather than
        // using default OS handling
        //
        Atom wm_delete = XInternAtom(uiInfo->pWindowInfo->pWindowOSInfo->disp,
                "WM_DELETE_WINDOW", False);
        (void) XSetWMProtocols(uiInfo->pWindowInfo->pWindowOSInfo->disp,
                winRetVal, &wm_delete, 1);

        // ...request a handle to a GLX rendering context
        uiInfo->pWindowInfo->pWindowOSInfo->glxContext = glXCreateContext(
                uiInfo->pWindowInfo->pWindowOSInfo->disp, // dpy
                uiInfo->pWindowInfo->pWindowOSInfo->visual, // Visual
                0, // ShareList
                TRUE); // Direct

        // raise window so that it is visible
        XMapRaised(uiInfo->pWindowInfo->pWindowOSInfo->disp, winRetVal);

        XSync(uiInfo->pWindowInfo->pWindowOSInfo->disp, FALSE);

        // connect the context to the window
        glXMakeCurrent(uiInfo->pWindowInfo->pWindowOSInfo->disp, winRetVal,
                uiInfo->pWindowInfo->pWindowOSInfo->glxContext);

        // decorate the window appropriately
        decorate_window(uiInfo->pWindowInfo->pWindowOSInfo, winRetVal);

        // Initialize the openGL environment
        opengl_init_environment(uiInfo, winRetVal);
    }
    else
    {
        RILOG_ERROR("%s -- XCreateSimpleWindow() failed.\n", __FUNCTION__);
    }

    XUnlockDisplay(uiInfo->pWindowInfo->pWindowOSInfo->disp);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return winRetVal;
}

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

    RILOG_INFO("%s -- Entry\n", __FUNCTION__);

    if (NULL == uiInfo->pWindowInfo->pWindowOSInfo)
    {
        window_init_screen(uiInfo->pWindowInfo);
    }

    // set a handler to catch X events, saving the original error handler
    pOriginalErrorHandler = XSetErrorHandler(window_handle_xerror);

    // Reset error handler
    g_bErrorCaught = FALSE;

    // use the default display
    uiInfo->pWindowInfo->pWindowOSInfo->disp = XOpenDisplay(NULL);
    if (NULL == uiInfo->pWindowInfo->pWindowOSInfo->disp)
    {
        RILOG_ERROR("%s -- unable to open X display\n", __FUNCTION__);
        return FALSE;
    }

    // get screen information
    uiInfo->pWindowInfo->pWindowOSInfo->screen = DefaultScreenOfDisplay(
            uiInfo->pWindowInfo->pWindowOSInfo->disp);
    uiInfo->pWindowInfo->pWindowOSInfo->screen_num = DefaultScreen(
            uiInfo->pWindowInfo->pWindowOSInfo->disp);
    uiInfo->pWindowInfo->pWindowOSInfo->root = DefaultRootWindow(
            uiInfo->pWindowInfo->pWindowOSInfo->disp);

    gint piAttributeList[] =
    { GLX_RGBA, GLX_RED_SIZE, 8, GLX_GREEN_SIZE, 8, GLX_BLUE_SIZE, 8,
            GLX_DOUBLEBUFFER, 0 // terminator
            };
    uiInfo->pWindowInfo->pWindowOSInfo->visual = glXChooseVisual(
            uiInfo->pWindowInfo->pWindowOSInfo->disp, // dpy
            uiInfo->pWindowInfo->pWindowOSInfo->screen_num, // screen
            piAttributeList); // AttributeList

    if (NULL == uiInfo->pWindowInfo->pWindowOSInfo->visual)
    {
        RILOG_ERROR("%s -- unable to choose visual\n", __FUNCTION__);

        RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
        return FALSE;
    }

    // Allocate display if NULL
    if (NULL == uiInfo->pDisplayInfo)
    {
        uiInfo->pDisplayInfo = g_try_new0(DisplayInfo, 1);

        if (NULL == uiInfo->pDisplayInfo)
        {
            RILOG_ERROR("%s -- unable to allocate display structure\n",
                    __FUNCTION__);

            RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
            return FALSE;
        }
    }

    // ...update the displayInfo structure with it
    uiInfo->pDisplayInfo->depth = DefaultDepthOfScreen(
            uiInfo->pWindowInfo->pWindowOSInfo->screen);

    uiInfo->pDisplayInfo->width = DisplayWidth(
            uiInfo->pWindowInfo->pWindowOSInfo->disp,
            uiInfo->pWindowInfo->pWindowOSInfo->screen_num);
    uiInfo->pDisplayInfo->height = DisplayHeight(
            uiInfo->pWindowInfo->pWindowOSInfo->disp,
            uiInfo->pWindowInfo->pWindowOSInfo->screen_num);
    uiInfo->pDisplayInfo->widthmm = DisplayWidthMM(
            uiInfo->pWindowInfo->pWindowOSInfo->disp,
            uiInfo->pWindowInfo->pWindowOSInfo->screen_num);
    uiInfo->pDisplayInfo->heightmm = DisplayHeightMM(
            uiInfo->pWindowInfo->pWindowOSInfo->disp,
            uiInfo->pWindowInfo->pWindowOSInfo->screen_num);

    uiInfo->pDisplayInfo->endianness
            = (ImageByteOrder(uiInfo->pWindowInfo->pWindowOSInfo->disp)
                    == LSBFirst) ? G_LITTLE_ENDIAN : G_BIG_ENDIAN;

    // Initialize the key event handling
    if (key_event_mapping == NULL)
    {
        window_initialize_key_events();
    }

    XSetErrorHandler(pOriginalErrorHandler);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return TRUE;
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
 * Closes the wyindow associated with info supplied
 *
 * @param windowInfo    information about the window
 */
void window_close(WindowInfo* windowInfo)
{
    RILOG_INFO("%s -- Entry\n", __FUNCTION__);

    // Set the running flag to false to initiate window close
    g_isRunning = FALSE;

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Closes the wyindow associated with info supplied
 *
 * @param windowInfo    information about the window
 */
static void window_destroy(WindowInfo* windowInfo)
{
    RILOG_INFO("%s -- Entry\n", __FUNCTION__);

    // Get local copies and NULL out global values
    Display *disp = windowInfo->pWindowOSInfo->disp;
    windowInfo->pWindowOSInfo->disp = NULL;

    Window win = windowInfo->pWindowOSInfo->win;
    windowInfo->pWindowOSInfo->win = 0;
    windowInfo->win = 0;

    GLXContext glxContext = windowInfo->pWindowOSInfo->glxContext;
    windowInfo->pWindowOSInfo->glxContext = NULL;

    // Get rid of window
    XDestroyWindow(disp, win);

    // Get rid of OpenGL context
    glXDestroyContext(disp, glxContext);

    // Discard all events
    XSync(disp, TRUE);

    // Close the display which will disconnect with XServer
    // and call final sync
    XCloseDisplay(disp);

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
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    if ((NULL != windowInfo) && (NULL != windowInfo->pWindowOSInfo))
    {
        g_cond_free(windowInfo->pWindowOSInfo->initCond);
        g_mutex_free(windowInfo->pWindowOSInfo->initMutex);
        g_free(windowInfo->pWindowOSInfo);
        windowInfo->pWindowOSInfo = NULL;
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
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
    XWindowAttributes attr;

    XGetWindowAttributes(uiInfo->pWindowInfo->pWindowOSInfo->disp, win, &attr);

    uiInfo->pWindowInfo->width = attr.width;
    uiInfo->pWindowInfo->height = attr.height;
}

/**
 * Registers a callback method for X key events
 */
void window_register_key_event_callback(void(*cb)(ri_event_type type,
        ri_event_code code))
{
    if (NULL == g_uiCat)
        g_uiCat = log4c_category_get(LOG_CAT);

    RILOG_DEBUG("%s -- call back method has been registered\n", __FUNCTION__);
    key_event_callback = cb;
}

void window_register_key_event_callback_mfg(void(*cb)(ri_event_type type,
        ri_event_code code))
{
    RILOG_DEBUG("%s -- mfg call back method has been registered\n",
            __FUNCTION__);
    key_event_callback_mfg = cb;
}

void window_register_display_event_callback(void(*cb)(int keyCode))
{
    if (NULL == g_uiCat)
        g_uiCat = log4c_category_get(LOG_CAT);

    RILOG_DEBUG("%s -- call back method has been registered\n", __FUNCTION__);
    display_event_callback = cb;
}

/**
 * Creates the hash table which contains the mapping between OS key codes
 * and key codes for the RI.
 */
static void window_initialize_key_events()
{
    if (NULL == g_uiCat)
        g_uiCat = log4c_category_get(LOG_CAT);

    int i = 0;
    gint windows_key_event_codes[RI_VK_LAST] =
    { XK_Return, /* RI_VK_ENTER */
    XK_BackSpace, /* RI_VK_BACK_SPACE */
    XK_Tab, /* RI_VK_TAB */
    XK_Up, /* RI_VK_UP */
    XK_Down, /* RI_VK_DOWN */
    XK_Left, /* RI_VK_LEFT */
    XK_Right, /* RI_VK_RIGHT */
    XK_Home, /* RI_VK_HOME */
    XK_End, /* RI_VK_END */
    XK_Page_Down, /* RI_VK_PAGE_DOWN */
    XK_Page_Up, /* RI_VK_PAGE_UP */
    XK_F1, /* RI_VK_COLORED_KEY_0 */
    XK_F2, /* RI_VK_COLORED_KEY_1 */
    XK_F3, /* RI_VK_COLORED_KEY_2 */
    XK_F4, /* RI_VK_COLORED_KEY_3 */
    XK_F5, /* RI_VK_GUIDE */
    XK_F6, /* RI_VK_MENU */
    XK_F7, /* RI_VK_INFO */
    XK_0, /* RI_VK_0 */
    XK_1, /* RI_VK_1 */
    XK_2, /* RI_VK_2 */
    XK_3, /* RI_VK_3 */
    XK_4, /* RI_VK_4 */
    XK_5, /* RI_VK_5 */
    XK_6, /* RI_VK_6 */
    XK_7, /* RI_VK_7 */
    XK_8, /* RI_VK_8 */
    XK_9, /* RI_VK_9 */
    XK_A, /* RI_VK_A */
    XK_B, /* RI_VK_B */
    XK_C, /* RI_VK_C */
    XK_D, /* RI_VK_D */
    XK_E, /* RI_VK_E */
    XK_F, /* RI_VK_F */
    XK_G, /* RI_VK_G */
    XK_H, /* RI_VK_H */
    XK_I, /* RI_VK_I */
    XK_J, /* RI_VK_J */
    XK_K, /* RI_VK_K */
    XK_L, /* RI_VK_L */
    XK_M, /* RI_VK_M */
    XK_N, /* RI_VK_N */
    XK_O, /* RI_VK_O */
    XK_P, /* RI_VK_P */
    XK_Q, /* RI_VK_Q */
    XK_R, /* RI_VK_R */
    XK_S, /* RI_VK_S */
    XK_T, /* RI_VK_T */
    XK_U, /* RI_VK_U */
    XK_V, /* RI_VK_V */
    XK_W, /* RI_VK_W */
    XK_X, /* RI_VK_X */
    XK_Y, /* RI_VK_Y */
    XK_Z, /* RI_VK_Z */
    0xAF, /* RI_VK_VOLUME_UP */// *TODO* - could not find an X key equivalent
            0xAE, /* RI_VK_VOLUME_DOWN */// *TODO* - could not find an X key equivalent
            0xAD, /* RI_VK_MUTE */// *TODO* - could not find an X key equivalent
            XK_Num_Lock, /* RI_VK_PLAY */
            XK_space, /* RI_VK_PAUSE */
            XK_asterisk, /* RI_VK_STOP */

    //XK_minus,     /* RI_VK_REWIND */
            //XK_KP_7,      /* RI_VK_RECORD */
            //XK_KP_8,      /* RI_VK_FAST_FWD */
            //XK_F8,        /* RI_VK_SETTINGS */
            //XK_F9,        /* RI_VK_EXIT */
            //XK_F10,       /* RI_VK_CHANNEL_UP *//
            //XK_F11,       /* RI_VK_CHANNEL_DOWN */
            //XK_F12,       /* RI_VK_ON_DEMAND */
            //XK_KP_1,      /* RI_VK_RF_BYPASS */
            //XK_KP_2,      /* RI_VK_POWER */
            //XK_KP_3,      /* RI_VK_LAST */
            //XK_KP_4,      /* RI_VK_NEXT_FAVORITE_CHANNEL */
            //XK_KP_5,      /* RI_VK_LIVE */
            //XK_KP_6       /* RI_VK_LIST */

            };

    key_event_mapping = g_hash_table_new(NULL, NULL);
    g_assert(key_event_mapping != NULL);

    for (i = 0; i < RI_VK_LAST; i++)
    {
        g_hash_table_insert(key_event_mapping, GINT_TO_POINTER(
                windows_key_event_codes[i]),
                (GINT_TO_POINTER((ri_event_code) i)));
    }
}

/**
 * Send an expose event which will be received in the paint loop which
 * will cause the openGL to be rendered.
 *
 * @param uiInfo    display information
 */
void window_request_repaint(UIInfo* uiInfo)
{
    g_mutex_lock(uiInfo->window_lock);

    if ((g_isRunning) && (NULL != uiInfo->pWindowInfo) && (0
            != uiInfo->pWindowInfo->win) && (NULL
            != uiInfo->pWindowInfo->pWindowOSInfo) && (NULL
            != uiInfo->pWindowInfo->pWindowOSInfo->disp))
    {
        //RILOG_ERROR("%s - called\n", __FUNCTION__);
        XLockDisplay(uiInfo->pWindowInfo->pWindowOSInfo->disp);

        // Send an expose event to request a repaint
        XExposeEvent event;
        event.type = Expose;
        event.display = uiInfo->pWindowInfo->pWindowOSInfo->disp;
        event.window = uiInfo->pWindowInfo->pWindowOSInfo->win;
        event.count = 0;

        // Create an expose event & send it which will request a window repaint
        if (0 == XSendEvent(uiInfo->pWindowInfo->pWindowOSInfo->disp,
                uiInfo->pWindowInfo->pWindowOSInfo->win, True, ExposureMask,
                (XEvent*) &event))
        {
            RILOG_ERROR("%s - send expose event failed\n", __FUNCTION__);
        }

        XUnlockDisplay(uiInfo->pWindowInfo->pWindowOSInfo->disp);
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
    XLockDisplay(windowInfo->pWindowOSInfo->disp);
    XSync(windowInfo->pWindowOSInfo->disp, FALSE);
    XUnlockDisplay(windowInfo->pWindowOSInfo->disp);
}

/**
 * Exposes a general function for calling XSynchronize, this is a
 * no-op on windows.
 */
void window_set_xsynchronize(WindowInfo* windowInfo, gint synchronous)
{
    XLockDisplay(windowInfo->pWindowOSInfo->disp);
    XSynchronize(windowInfo->pWindowOSInfo->disp, synchronous);
    XUnlockDisplay(windowInfo->pWindowOSInfo->disp);
}

/**
 * Set window decorations. Gives window border and resize buttons.
 *
 * @param pWindowOSInfo    information such as display.
 * @param win              if of the window to be decorated.
 *
 * @return <code>TRUE</code> if the window was decorated successfully,
 * <code>FALSE</code> otherwise.
 */
static gboolean decorate_window(WindowOSInfo* pWindowOSInfo, uint32_t win)
{
    Atom hints_atom = None;
    MotifWmHints *hints;

    XLockDisplay(pWindowOSInfo->disp);

    // get the 'hints' atom
    hints_atom = XInternAtom(pWindowOSInfo->disp, "_MOTIF_WM_HINTS", 1);
    if (hints_atom == None)
    {
        XUnlockDisplay(pWindowOSInfo->disp);
        return FALSE;
    }

    hints = g_try_malloc0(sizeof(MotifWmHints));

    if (NULL == hints)
    {
        RILOG_FATAL(-1, "line %d of %s, %s memory allocation failure!\n",
                    __LINE__, __FILE__, __func__);
    }


    hints->flags |= MWM_HINTS_DECORATIONS;
    hints->decorations = 1 << 0;

    XChangeProperty(pWindowOSInfo->disp, win, // w (window)
            hints_atom, // property
            hints_atom, // type
            32, // format
            PropModeReplace, // mode
            (guchar *) hints, // data
            sizeof(MotifWmHints) / sizeof(long)); // nelements

    XSync(pWindowOSInfo->disp, FALSE);

    g_free(hints);
    XUnlockDisplay(pWindowOSInfo->disp);
    return TRUE;
}

/**
 * Handles X errors by logging error message.
 *
 * @param   display X display
 * @param   xevent  error event to report
 *
 * @return  always returns 0
 */
static int window_handle_xerror(Display* display, XErrorEvent* xevent)
{
    char error_msg[1024];

    XGetErrorText(display, xevent->error_code, error_msg, 1024);
    RILOG_ERROR("%s -- ui_window triggered an XError. error: %s\n",
            __FUNCTION__, error_msg);
    RILOG_ERROR("%s -- \tresourceid = %ld\n", __FUNCTION__,
            (glong) xevent->resourceid);

    g_bErrorCaught = TRUE;

    return 0;
}

/**
 * Handles X events received in the window.  It will forward
 * keys to registered key callback.
 *
 * @param uiInfo  information about display and window
 */
static void window_handle_xevents(UIInfo* uiInfo)
{
    if (NULL == g_uiCat)
        g_uiCat = log4c_category_get(LOG_CAT);

    XEvent e;
    Display* disp = uiInfo->pWindowInfo->pWindowOSInfo->disp;
    Window win = uiInfo->pWindowInfo->pWindowOSInfo->win;
    KeySym keysym;
    char buf[31];

    XLockDisplay(uiInfo->pWindowInfo->pWindowOSInfo->disp);

    while (XCheckWindowEvent(disp, win, KeyPressMask | KeyReleaseMask, &e))
    {
        switch (e.type)
        {
        case KeyPress:
        case KeyRelease:

            // Get key symbol and string for key event
            (void) XLookupString(&e.xkey, buf, sizeof(buf), &keysym, NULL);
            RILOG_INFO("%s -- got key event %s, code %d, XK_2 %d\n",
                    __FUNCTION__, (char*) &buf, (int) keysym, XK_2);

            gboolean lookup_succeeded = FALSE;

            // Initialize code to last key
            ri_event_code code = RI_VK_LAST;

            // Look up this key in the hash table to find RI code
            lookup_succeeded = g_hash_table_lookup_extended(key_event_mapping,
                    GUINT_TO_POINTER(keysym), NULL, (gpointer)(&code));

            if (lookup_succeeded && key_event_callback != NULL)
            {
                if (e.type == KeyRelease)
                {
                    (*key_event_callback)(RI_EVENT_TYPE_RELEASED, code);
                }
                else // KeyPress
                {
                    (*key_event_callback)(RI_EVENT_TYPE_PRESSED, code);
                }
            }
            if (lookup_succeeded && key_event_callback_mfg != NULL)
            {
                if (e.type == KeyRelease)
                {
                    (*key_event_callback_mfg)(RI_EVENT_TYPE_RELEASED, code);
                }
                else // Process key down event.
                {
                    (*key_event_callback_mfg)(RI_EVENT_TYPE_PRESSED, code);
                }
            }
            break;

        default:
            RILOG_DEBUG("%s -- got unrecognized event type %d\n", __FUNCTION__,
                    e.type);
        }
    }

    // Process display events
    while (XPending(disp))
    {
        XNextEvent(disp, &e);

        switch (e.type)
        {
        case ClientMessage:
        {
            Atom wm_delete;
            wm_delete = XInternAtom(disp, "WM_DELETE_WINDOW", False);

            if (wm_delete == (Atom) e.xclient.data.l[0])
            {
                RILOG_DEBUG("%s - got delete window message\n", __FUNCTION__);

                // Set the flag so paint thread exits
                g_isRunning = FALSE;

                // Unlock the display so paint thread can close down window
                XUnlockDisplay(uiInfo->pWindowInfo->pWindowOSInfo->disp);

                return;
            }
            break;
        }
        default:
            break;
        }
    }

    XUnlockDisplay(uiInfo->pWindowInfo->pWindowOSInfo->disp);
}

/**
 *
 */
static void window_handle_paint_events(UIInfo* uiInfo)
{
    if (NULL == g_uiCat)
        g_uiCat = log4c_category_get(LOG_CAT);

    XEvent e;
    Display* disp = uiInfo->pWindowInfo->pWindowOSInfo->disp;
    Window win = uiInfo->pWindowInfo->pWindowOSInfo->win;

    // Check running flag in case window was closed by other thread
    if (!g_isRunning)
    {
        return;
    }

    // Lock down X display while making X calls
    XLockDisplay(uiInfo->pWindowInfo->pWindowOSInfo->disp);

    // Reset flag which reports if at least one repaint requests has been received
    gboolean exposed = FALSE;

    // Check for all expose events
    while (XCheckWindowEvent(disp, win, ExposureMask, &e))
    {
        switch (e.type)
        {
        case Expose:
            //RILOG_DEBUG("%s - got expose event\n", __FUNCTION__);
            exposed = TRUE;
            break;

        default:
            RILOG_DEBUG("%s -- got unrecognized event type %d\n", __FUNCTION__,
                    e.type);
        }
    }

    // If expose flag is set, need to do a repaint
    if (exposed)
    {
        if ((g_isRunning) && (NULL != uiInfo->pWindowInfo) && (0
                != uiInfo->pWindowInfo->win))
        {
            opengl_render_display(uiInfo);
        }
    }

    // Release X lock
    XUnlockDisplay(uiInfo->pWindowInfo->pWindowOSInfo->disp);
}
