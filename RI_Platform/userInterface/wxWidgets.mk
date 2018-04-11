
#
# wxWidgets configure arguments
#
WXWIDGETS_CONFIGURE = ./configure \
	PKG_CONFIG_PATH=$(PLATFORM_PKGCFG) \
	CC=$(RI_CC) CXX=$(RI_CXX) \
	CPPFLAGS='$(WXWIDGETS_CPPFLAGS) -I$(PLATFORM_INST_DIR)/include' \
	LDFLAGS='$(WXWIDGETS_LDFLAGS) -L$(PLATFORM_INST_DIR)/lib' \
	PATH='$(WXWIDGETS_TOOLS_PATH):$(PLATFORM_PATH):$(PATH)' \
	--enable-debug=$(WXWIDGETS_DEBUG) --enable-debug-gdb \
	--build=$(RI_BUILD) --host=$(RI_HOST) \
	$(WXWIDGETS_CONFIG_FLAGS) \
	--prefix=$(PLATFORM_INST_DIR)


