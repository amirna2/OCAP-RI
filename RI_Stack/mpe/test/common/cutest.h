#ifndef CU_TEST_H
#define CU_TEST_H

#include <sys/types.h>          /* Necessary for PowerTV */
#include <setjmp.h>
#include <stdarg.h>

/* CuString */

char* CuStrAlloc(int size);
char* CuStrCopy(const char* old);

#define CU_ALLOC(TYPE)		((TYPE*) malloc(sizeof(TYPE)))

/*  Test outcomes. These are possible values for the "result" field  */

#define CU_RSLT_NOTRUN         1
#define CU_RSLT_PASS           2
#define CU_RSLT_FAIL           4
#define CU_RSLT_TESTERROR      8
#define CU_RSLT_MANUAL         16 
#define CU_RSLT_TIMEOUT        32 
#define CU_RSLT_CONDPASS       64
/* for vCert */
#define CU_RSLT_UNRESOLVED     128
#define CU_RSLT_NOT_IN_USE     129
#define CU_RSLT_UNSUPPORTED	   130
#define CU_RSLT_UNTESTED       131
#define CU_RSLT_UNINITIATED    132
#define CU_RSLT_NO_RESULT      133
#define CU_RSLT_UNKNOWN        134
#define CU_RSLT_INSPECT        135
#define CU_RSLT_DATA           136

/*
 * Problems with setjmp and longjmp when you jump from one thread stack to
 * another. To disable fail on assert include the env variable CUTEST_NOFAIL.
 */
/* #define CUTEST_NOFAIL 1 */
#ifdef CUTEST_NOFAIL
# define CU_SETJMP(x)    CuNoopJmp(x)
# define CU_LONGJMP(x,y) CuNoopJmp(x)
#else
# define CU_SETJMP(x)    setjmp(x)
# define CU_LONGJMP(x,y) longjmp(x,y)
#endif

/*
 * Define "setup" macro for pre-test setup to support post-test cleanup:
 * 
 * Note: "setup" can be a statement, a function call, a code block, etc.
 */
#define CuTestSetup(setup) \
        setup;
/*
 #define CuTestSetup(setup) \
	{ \
		jmp_buf buf; \
		jmp_buf *save = tc->jumpBuf; \
		tc->jumpBuf = &buf; \
	  \
		setup; \
	  \
		if (setjmp(buf) == 0) \
		{ 
 */

/*
 * Define "cleanup" macro for post-test cleanup:
 */
#define CuTestCleanup(cleanup) \
         cleanup;
/*
 #define CuTestCleanup(cleanup) \
			cleanup; \
			tc->jumpBuf = save; \
		} \
		else \
		{ \
			cleanup; \
			tc->jumpBuf = save; \
			CU_LONGJMP(*(tc->jumpBuf), 0); \
		} \
	} 
 */

#define HUGE_STRING_LEN	8192
#define STRING_MAX		256
#define STRING_INC		256

typedef struct
{
    int length;
    int size;
    char* buffer;
} CuString;

int CuNoopJmp(jmp_buf buf);
void CuStringInit(CuString* str);
CuString* CuStringNew(void);
void CuStringRead(CuString* str, const char* path);
void CuStringAppend(CuString* str, const char* text);
void CuStringAppendChar(CuString* str, char ch);
void CuStringAppendFormat(CuString* str, const char* format, ...);
void CuStringInsert(CuString* str, const char* text, int pos);
void CuStringResize(CuString* str, int newSize);

/* CuTest */

typedef struct CuTest CuTest;

typedef void (*TestFunction)(CuTest *);

// The way this works is that on setting up the test suites, the failcontinue
// flag in the lists CuTest * entry is set if that test is a stop-test (control
// flow stops after running that test to allow user input). Then, if the user
// inputs an n or N, the control flow will stop after the next test. If the
// input is an s or S, the control flow will stop after the next stop-test.
// If the input is an r or R, the control flow will not stop until this suite
// is completed. If the input is an x or X, the control flow will not stop ever
// again and all the tests will run through to completion. Note that if the
// input is an l or L, the current test will be looped on (run again) regardless
// of whether it passes or fails. Note that the failure count is adjusted to
// reflect reality as if this test is only run once. This can be used to run a
// single test over night for example.
// static unsigned long flowcontrol;	// used to control test flow
// defines here are for each of the flow control bits.
// Note that FLOW_STOP and FLOW_GO are for the failcontinue flag in CuTest. The
// remainder are for use in the flowcontrol bitmap variable.
#define FLOW_STOP	1	/* Stops after a test is run */
#define FLOW_GO		0	/* Doesn't stop after a test is run */
#define FLOW_GOTO_END	2	/* Indicates that tests never stop until done */
#define FLOW_GOTO_NEXT	4	/* Indicates tests stop after every test */
#define FLOW_GOTO_STOP 	8	/* Indicates tests stop after stop-test only */
#define FLOW_GOTO_SUITE 0x10	/* Indicates tests stop after this suite */
#define FLOW_GOTO_LOOP	0x20	/* Indicates that current test will repeat forever */

// There is one of these structs for every test in a list array in each CuSuite.
// The failcontinue flag is currently not used so we are assigning it the value
// whereby a test is being told to stop after running based on flow control
// flags
struct CuTest
{
    const char* name;
    TestFunction function;
    int result;
    int failed;
    int ran;
    int failcontinue; // Use this flag to indicate stop after test
    const char* message;
    jmp_buf *jumpBuf;
};

void CuTestInit(CuTest* t, const char* name, TestFunction function);
CuTest* CuTestNew(const char* name, TestFunction function, int flagstop);
void CuTestRun(CuTest* tc);

/* Internal versions of assert functions -- use the public versions */
void CuFail_Line(CuTest* tc, const char* file, int line, const char* message2,
        const char* message);
void CuAssert_Line(CuTest* tc, const char* file, int line, const char* message,
        int condition);
void CuAssertStrEquals_LineMsg(CuTest* tc, const char* file, int line,
        const char* message, const char* expected, const char* actual);
void CuAssertIntEquals_LineMsg(CuTest* tc, const char* file, int line,
        const char* message, int expected, int actual);
void CuAssertDblEquals_LineMsg(CuTest* tc, const char* file, int line,
        const char* message, double expected, double actual, double delta);
void CuAssertPtrEquals_LineMsg(CuTest* tc, const char* file, int line,
        const char* message, void* expected, void* actual);
void CuAssertIntEqualsCallback_LineMsg(CuTest* tc, void(*callback)(void),
        const char* file, int line, const char* msg, int exp, int act);

/* public assert functions */

#define CuFail(tc, ms)                        CuFail_Line(  (tc), __FILE__, __LINE__, NULL, (ms))
#define CuAssert(tc, ms, cond)                CuAssert_Line((tc), __FILE__, __LINE__, (ms), (cond))
#define CuAssertTrue(tc, cond)                CuAssert_Line((tc), __FILE__, __LINE__, "assert failed", (cond))

#define CuAssertStrEquals(tc,ex,ac)           CuAssertStrEquals_LineMsg((tc),__FILE__,__LINE__,NULL,(ex),(ac))
#define CuAssertStrEquals_Msg(tc,ms,ex,ac)    CuAssertStrEquals_LineMsg((tc),__FILE__,__LINE__,(ms),(ex),(ac))
#define CuAssertIntEquals(tc,ex,ac)           CuAssertIntEquals_LineMsg((tc),__FILE__,__LINE__,NULL,(ex),(ac))
#define CuAssertIntEquals_Msg(tc,ms,ex,ac)    CuAssertIntEquals_LineMsg((tc),__FILE__,__LINE__,(ms),(ex),(ac))
#define CuAssertDblEquals(tc,ex,ac,dl)        CuAssertDblEquals_LineMsg((tc),__FILE__,__LINE__,NULL,(ex),(ac),(dl))
#define CuAssertDblEquals_Msg(tc,ms,ex,ac,dl) CuAssertDblEquals_LineMsg((tc),__FILE__,__LINE__,(ms),(ex),(ac),(dl))
#define CuAssertPtrEquals(tc,ex,ac)           CuAssertPtrEquals_LineMsg((tc),__FILE__,__LINE__,NULL,(ex),(ac))
#define CuAssertPtrEquals_Msg(tc,ms,ex,ac)    CuAssertPtrEquals_LineMsg((tc),__FILE__,__LINE__,(ms),(ex),(ac))
#define CuAssertIntEqualsCB(tc,fp,ex,ac)      CuAssertIntEqualsCallback_LineMsg((tc),(fp),__FILE__,__LINE__,NULL,(ex),(ac))
#define CuAssertIntEqualsCB_Msg(tc,fp,ms,ex,ac) CuAssertIntEqualsCallback_LineMsg((tc),(fp),__FILE__,__LINE__,(ms),(ex),(ac))

#define CuAssertPtrNotNull(tc,p)        CuAssert_Line((tc),__FILE__,__LINE__,"null pointer unexpected",(p != NULL))
#define CuAssertPtrNotNullMsg(tc,msg,p) CuAssert_Line((tc),__FILE__,__LINE__,(msg),(p != NULL))

/* CuSuite */

#define MAX_TEST_CASES	1024

#define SUITE_ADD_TEST(SUITE,TEST)	CuSuiteAdd(SUITE, CuTestNew(#TEST, TEST, 0))

// Same as SUITE_ADD_TEST except makes this test a stop-test.
#define SUITE_STOP_TEST(SUITE,TEST)	CuSuiteAdd(SUITE, CuTestNew(#TEST, TEST, 1))

typedef struct
{
    int count;
    CuTest* list[MAX_TEST_CASES];
    int failCount;

} CuSuite;

void CuSuiteInit(CuSuite* testSuite);
CuSuite* CuSuiteNew(void);
void CuSuiteAdd(CuSuite* testSuite, CuTest *testCase);
void CuSuiteAddSuite(CuSuite* testSuite, CuSuite* testSuite2);
int CuSuiteUserInput(CuTest *testCase);
void CuSuiteRun(CuSuite* testSuite);
void CuSuiteSummary(CuSuite* testSuite, CuString* summary);
void CuSuiteDetails(CuSuite* testSuite, CuString* details);

#endif /* CU_TEST_H */
