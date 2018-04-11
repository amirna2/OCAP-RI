###
#
# Makefile for Mock DTCP library customized for OCAP RI.
#
###

HOSTOS := $(shell uname -o)
export HOSTOS

ifeq ($(HOSTOS),Cygwin)
  CC = gcc-3 -mno-cygwin
  DLL_SUFFIX = dll
  MODULES_LINK_EXT = -lws2_32 -lwinmm
  LINK_PATHS = -Llib/win32
else ifeq ($(HOSTOS),GNU/Linux)
  CC = gcc
  DLL_SUFFIX = so
  LINK_PATHS = -Llib/linux
else
  $(error Unsupported OS.)
endif


DTCPIP_DLL ?= dtcpip_mock.$(DLL_SUFFIX)

MODULE_MOCKDLL = mockdll

MODULE_LINK_PFX = $(addprefix -l,mockdll)
MODULE_LINK = $(addprefix -l,$(MODULE_MOCKDLL))


default: all

$(MODULE_MOCKDLL):
	$(MAKE) -C _build build

all: $(MODULE_MOCKDLL)

build: $(MODULE_MOCKDLL)

shlib: build $(MODULE_MOCKDLL)
	$(CC) -shared -o $(DTCPIP_DLL) -Wl,--whole-archive -lmockdll $(LINK_PATHS) -Wl,--no-whole-archive $(MODULE_LINK_EXT)

clean:
	$(MAKE) -C _build clean 

purge:
	rm -f $(DTCPIP_DLL)

