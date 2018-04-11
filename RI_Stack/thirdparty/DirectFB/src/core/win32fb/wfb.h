#ifdef __cplusplus
extern "C"
{
#endif

//
// Screen structure
//
typedef struct
{
     unsigned int width;      // Width in pixels
     unsigned int height;     // Height in pixels
     unsigned int Bpp;        // Bytes-per-pixel
     unsigned int bpp;        // bits-per-pixel
     unsigned short pitch;    // Length of scanline in bytes
     void *pixels;            // Pointer to actual pixel data
} WFB_Screen;

//
// Result value returned by all functions
//
typedef int WFB_Result;
#define WFB_SUCCESS 0

//
// Function prototypes
//
WFB_Result WFB_Init(void);
WFB_Result WFB_Term(void);
WFB_Result WFB_SetVideoMode(unsigned int width, unsigned int height, unsigned int Bpp,
                            unsigned int bpp, WFB_Screen **screen);
WFB_Result WFB_Flip(void);
WFB_Result WFB_Refresh(void);

#ifdef __cplusplus
}
#endif
