#
# Defs for Windows target using Cygwin GCC with MinGW libraries
#

CC = gcc
CCC = g++

LIB_PREFIX = lib

LIB_POSTFIX = .so

#
# platform specific architecture flags
#
ASM_ARCH_FLAGS		= -march=i686 -DPENTIUM_4
CC_ARCH_FLAGS   	= -march=i686

# Fix gcc fp bug on x86 and avoid cvm problems
CC_ARCH_FLAGS_FDLIB	= -ffloat-store

# Don't inline the interpreter loop helper functions. This helps
# reduce register pressure on x86 and improve generated code.
CC_ARCH_FLAGS_LOOP 	= -fno-inline
LINK_ARCH_FLAGS		= 
LINK_ARCH_LIBS  	= -lm

# Libraries that are required to link libcvm.so
LINKCVM_LIBS += -lm

CVM_JIT_USE_FP_HARDWARE = false
CVM_JIT_REGISTER_LOCALS ?= false

CPPFLAGS += -DMPE_LITTLE_ENDIAN \
			-DJNI_LIB_PREFIX="\"lib\"" \
			-DJNI_LIB_SUFFIX="\".so\"" \
			-DJNICALL="" \
			-DJNIIMPORT="" \
			-DJNIEXPORT="" \
			-DPATH_SEPARATOR_CHAR="\":\"" \
			-DMPE_TARGET_OS_LINUX \
			

CVM_INCLUDE_DIRS += \
    $(MPE_ROOT)/os/RI_Common/include \
    $(MPE_ROOT)/os/RI_Linux/include

CVM_COMPILER_INCOMPATIBLE=false

