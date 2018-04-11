/*
  ,---------------------------------------------------------------------------,
  |                                                                           |
  |                       Copyright 2004 OCAP Development LLC                 |
  |                              All rights reserved                          |
  |                            Reproduced Under License                       |
  |                                                                           |
  |  This source code is the proprietary confidential property of             |
  |  OCAP Development LLC and is provided to recipient for documentation and  |
  |  educational purposes only. Reproduction, publication, or distribution in |
  |  any form to any party other than the recipient is strictly prohibited.   |
  |                                                                           |
  `---------------------------------------------------------------------------'
*/

#ifndef __DFB2EXTERNAL_H__
#define __DFB2EXTERNAL_H__

#include <stdlib.h>
#include <dfb_types.h>

#ifdef _WINDOWS
#define LIBEXPORT __declspec(dllexport)
#else
#define LIBEXPORT
#endif

typedef struct DFB2Ex_Mutex_tag *DFB2Ex_Mutex;

typedef struct 
{

	/*
	 * Memory allocation routines: malloc, realloc, and free.
	 */
	void* (*malloc)(size_t);
	void* (*realloc)(void*, size_t);
	void  (*free)(void*);

	/*
	 * Mutual exclusion routines: init, lock, try_lock, unlock, and destroy.
	 */
	int (*mutex_init)(DFB2Ex_Mutex *mutex, void* mutexaddr);
	int (*mutex_lock)(DFB2Ex_Mutex *mutex);
	int (*mutex_trylock)(DFB2Ex_Mutex *mutex);
	int (*mutex_unlock)(DFB2Ex_Mutex *mutex);
	int (*mutex_destroy)(DFB2Ex_Mutex *mutex);

	/*
	 * Debug tracing: printf (no return value).
	 */
	void (*printf)(const char*, ...);

} DirectFB2External;

extern DirectFB2External* g_dfb2ext;

#define dfb_malloc(x)     g_dfb2ext->malloc(x)
#define dfb_realloc(x, y) g_dfb2ext->realloc(x, y)
#define dfb_free(x)       g_dfb2ext->free(x)

#define pthread_mutex_init(x, y)     g_dfb2ext->mutex_init(x, y)
#define pthread_mutex_lock(x)        g_dfb2ext->mutex_lock(x)
#define pthread_mutex_trylock(x)     g_dfb2ext->mutex_trylock(x)
#define pthread_mutex_unlock(x)      g_dfb2ext->mutex_unlock(x)
#define pthread_mutex_destroy(x)     g_dfb2ext->mutex_destroy(x)

#define dfb_printf  g_dfb2ext->printf


/*
 * Map pthread mutex variables to external layer
 */
#ifdef  PTHREAD_MUTEX_INITIALIZER
#undef  PTHREAD_MUTEX_INITIALIZER
#endif
#define PTHREAD_MUTEX_INITIALIZER ((DFB2Ex_Mutex)0)
#define PTHREAD_MUTEX_FAST (void*)10
#define PTHREAD_MUTEX_RECURSIVE (void*)11
#define pthread_mutex_t DFB2Ex_Mutex


#endif /* __DFB2EXTERNAL_H__ */

