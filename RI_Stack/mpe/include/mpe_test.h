#ifndef _MPE_TEST_BINDINGS_H_
#define _MPE_TEST_BINDINGS_H_

#include "mpe_sys.h"
#include "../mgr/testmgr/testmgr.h"

#define mpe_test_ftable ((mpe_test_ftable_t*)(FTABLE[MPE_MGR_TYPE_TEST]))

#define mpe_testInit  (*(mpe_test_ftable->mpe_test_init_ptr))
#define mpe_testFunc1 (*(mpe_test_ftable->mpe_test_func1_ptr))
#define mpe_testFunc2 (*(mpe_test_ftable->mpe_test_func2_ptr))

#endif

