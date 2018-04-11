#include <windows.h>
#include "wfb.h"

//
// Globals
//
__declspec(dllexport) HWND hWnd;
static WFB_Screen *screen = NULL;

//
// Initialize the WFB API
//
WFB_Result WFB_Init(void)
{
     return WFB_SUCCESS;
}

//
// Terminate the WFB API
//
WFB_Result WFB_Term(void)
{
     return WFB_SUCCESS;
}

//
// Set video mode
//
WFB_Result WFB_SetVideoMode(unsigned int width, unsigned int height, unsigned int Bpp,
                            unsigned int bpp, WFB_Screen **ret_screen)
{
     RECT rect;

     // If we already have a screen with the proper dimensions, then just
     // return it.
     if (screen)
          if ( (screen->width  == width)  &&
               (screen->height == height) &&
               (screen->Bpp    == Bpp)    &&
               (screen->bpp    == bpp) )
          {
               *ret_screen = screen;
               return WFB_SUCCESS;
          }

     // Allocate screen structure
     if ((screen = (WFB_Screen *)calloc(1, sizeof(WFB_Screen))) == NULL)
          return errno;
     screen->width = width;
     screen->height = height;
     screen->Bpp = Bpp;
     screen->bpp = bpp;

     // Throw away old pixelmap if there is one
     if (screen->pixels)
          free(screen->pixels);

     // Allocate pixelmap
     screen->pitch = (unsigned short)(Bpp * width);
     if ((screen->pixels = calloc(1, screen->pitch * height)) == NULL)
          return errno;

     // Ensure that the size of the client window matches the request video mode
     rect.left = rect.top = 1;
     rect.right = width;
     rect.bottom = height;
     if (AdjustWindowRect(&rect, WS_OVERLAPPEDWINDOW, TRUE) == 0)
          return GetLastError();
     if (SetWindowPos(hWnd, HWND_TOP, 0, 0,
          rect.right-rect.left+1, rect.bottom-rect.top+1, SWP_NOMOVE) == 0)
          return GetLastError();

     // Return a pointer to the screen
     *ret_screen = screen;
     return WFB_SUCCESS;
}

//
// Flip buffers
//
WFB_Result WFB_Flip(void)
{
     // We only maintain one buffer within DirectFB. This is an off-screen
     // buffer that must be copied to the display each time this function
     // is called
     HBITMAP hbitmap;
     HDC bitmapDC, hdc;

     // Create a bitmap to hold the pixelmap
     hbitmap = CreateBitmap(screen->width, screen->height, 1, screen->Bpp * 8, screen->pixels);
     if (hbitmap == NULL)
          return GetLastError();

     // Create a drawing context for the bitmap
     bitmapDC = CreateCompatibleDC(NULL);
     if (bitmapDC == NULL)
          return GetLastError();

     // Select the bitmap into the source DC
     if (SelectObject(bitmapDC, hbitmap) == NULL)
          return GDI_ERROR;

     // Blit the source bitmap to the display
     hdc = GetDC(hWnd);
     if (BitBlt(hdc, 0, 0, screen->width, screen->height, bitmapDC, 0, 0, SRCCOPY) == 0)
          return GetLastError();
     ReleaseDC(hWnd, hdc);

     // Delete source objects
     DeleteDC(bitmapDC);
     DeleteObject(hbitmap);

     return WFB_SUCCESS;
}

//
// Refresh the display. Usually called when an area of the display which
// was hidden is exposed.
//
WFB_Result WFB_Refresh(void)
{
     // Do a flip since it just paints the display
     if (screen)
          return WFB_Flip();
     else
          return WFB_SUCCESS;
}
