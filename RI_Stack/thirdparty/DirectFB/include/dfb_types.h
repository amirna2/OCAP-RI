#ifndef __DFB_TYPES_H__
#define __DFB_TYPES_H__

#include <ctype.h>
#include <errno.h>
#include <limits.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define __u8  unsigned char
#define __u16 unsigned short
#define __u32 unsigned long
#define __s16 signed short
#ifdef WIN32
#define longlong __int64
#else
#define longlong long long
#endif

#define bool int
#undef false 	/* use our version of false */
#define false 0
#undef true	/* use our version of true */
#define true  !false

#ifdef WIN32
#define snprintf _snprintf
#endif

#define DFB_UNUSED_PARAM(x) (x=x)

#endif
