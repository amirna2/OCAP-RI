
INC_DIRS = ../../src/include \
     $(OCAPROOT)/mpe/os/include \
     $(OCAPROOT)/mpe/os/RI_Common/include \
     $(OCAPROOT)/mpe/include 

MOCKDLL_SRC_DIR    = ../src
MOCKDLL_LIBNAME    = libmockdll.a

COPT = -c -efi2 \
       -D_ARCH_IA32 -DUPNP_MICRO_STACK -DMICROSTACK_NO_STDAFX -DMPE_LITTLE_ENDIAN -O0 -g \
       $(addprefix -I, $(INC_DIRS))
CSRC := $(wildcard $(MOCKDLL_SRC_DIR)/*.c)


ifeq ($(HOSTOS),Cygwin)
  MOCKDLL_LIB_DIR = ../lib/win32
  MOCKDLL_OBJ_DIR = ../obj/win32
  MOCKDLL_OBJ_SUBDIR = ../obj/win32/win32
  INC_DIRS += $(OCAPROOT)/mpe/os/RI_Win32/include
  CC = gcc-3 -mno-cygwin
  COPT += -DWINSOCK2
  INC_DIRS += ../src/win32
  CSRCTEMP := $(wildcard $(MOCKDLL_SRC_DIR)/win32/*.c)
  COBJ := $(patsubst %.c,$(MOCKDLL_OBJ_DIR)/%.o,$(notdir $(CSRC)))
  CSRC += $CSRCTEMP;  
  COBJ += $(patsubst %.c,$(MOCKDLL_OBJ_DIR)/win32/%.o,$(notdir $(CSRCTEMP)))
else ifeq ($(HOSTOS),GNU/Linux)
  MOCKDLL_LIB_DIR = ../lib/linux
  MOCKDLL_OBJ_DIR = ../obj/linux
  MOCKDLL_OBJ_SUBDIR = ../obj/linux/linux
  INC_DIRS += $(OCAPROOT)/mpe/os/RI_Linux/include
  CC = gcc
  COPT += -Dunix -Dlinux -Dlinux32 -D_POSIX
  INC_DIRS += ../src/linux
  CSRCTEMP := $(wildcard $(MOCKDLL_SRC_DIR)/linux/*.c)
  COBJ := $(patsubst %.c,$(MOCKDLL_OBJ_DIR)/%.o,$(notdir $(CSRC)))
  CSRC += $CSRCTEMP;  
  COBJ += $(patsubst %.c,$(MOCKDLL_OBJ_DIR)/linux/%.o,$(notdir $(CSRCTEMP)))
else
  $(error Unsupported OS.)
endif


build: clean_lib prepare $(MOCKDLL_LIBNAME)

$(MOCKDLL_LIBNAME) : $(COBJ)
	/usr/bin/ar q $(MOCKDLL_LIB_DIR)/$@ $(COBJ)
	
$(COBJ) : $(MOCKDLL_OBJ_DIR)/%.o : $(MOCKDLL_SRC_DIR)/%.c $(wildcard $(addsuffix /*.h, $(INC_DIRS)))
	$(CC) $(COPT) -o $@ $<

clean: clean_lib
	rm -fR $(MOCKDLL_OBJ_DIR)

clean_lib:
	rm -fR $(MOCKDLL_LIB_DIR)/$(MOCKDLL_LIBNAME)

prepare:
	mkdir -p $(MOCKDLL_OBJ_DIR)
	mkdir -p $(MOCKDLL_OBJ_SUBDIR)
	mkdir -p $(MOCKDLL_LIB_DIR)

