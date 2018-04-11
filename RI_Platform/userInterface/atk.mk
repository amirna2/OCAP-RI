
#
# ATK configure arguments
#
ATK_CONFIGURE = ./configure \
	PKG_CONFIG_PATH=$(PLATFORM_PKGCFG) \
	CC=$(RI_CC) CXX=$(RI_CXX) \
	CPPFLAGS='$(GTKPLUS_CPPFLAGS) -I$(PLATFORM_INST_DIR)/include' \
	LDFLAGS='$(GTKPLUS_LDFLAGS) -L$(PLATFORM_INST_DIR)/lib' \
	PATH='$(PLATFORM_PATH):$(PATH)' \
	--enable-debug=$(GTKPLUS_DEBUG) --enable-debug-gdb \
	--build=$(RI_BUILD) --host=$(RI_HOST) \
	$(GTKPLUS_CONFIG_FLAGS) \
	--prefix=$(PLATFORM_INST_DIR)


