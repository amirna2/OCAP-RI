#
# Defs for Windows target using Cygwin GCC with MinGW libraries
#

HOST_CC := gcc-3
HOST_CCC := g++-3

LIB_PREFIX = lib

LIB_POSTFIX = .dll

#
# platform specific architecture flags
#
ASM_ARCH_FLAGS		= -mno-cygwin -march=i686 -DPENTIUM_4
CC_ARCH_FLAGS   	= -mno-cygwin -march=i686

# Fix gcc fp bug on x86 and avoid cvm problems
CC_ARCH_FLAGS_FDLIB	= -ffloat-store

# Don't inline the interpreter loop helper functions. This helps
# reduce register pressure on x86 and improve generated code.
CC_ARCH_FLAGS_LOOP 	= -fno-inline
LINK_ARCH_FLAGS		= -mwindows -mno-cygwin -Wl,--kill-at
LINK_ARCH_LIBS  	= -lm

# This will stop the generation of symbol names in the DLL ending with @<ordinal>
#SO_LINKFLAGS += -Wl,--kill-at

CPPFLAGS += -DASM_PREPEND_UNDERSCORE \
			-DWIN32_LEAN_AND_MEAN \
            -DMPE_LITTLE_ENDIAN \
			-DJNI_LIB_PREFIX="\"lib\"" \
			-DJNI_LIB_SUFFIX="\".dll\"" \
			-DJNIEXPORT="__declspec(dllexport)" \
			-DJNIIMPORT="__declspec(dllimport)" \
			-DJNICALL="" \
			-DPATH_SEPARATOR_CHAR="\";\"" \
			-DHAVE_STDINT_H \
			-DMPE_TARGET_OS_WINDOWS


CVM_INCLUDE_DIRS += \
    $(MPE_ROOT)/os/RI_Win32/include \
    $(MPE_ROOT)/os/RI_Common/include

CVM_COMPILER_INCOMPATIBLE=false

CVM_TARGETOBJS_SPACE += tchar.o
