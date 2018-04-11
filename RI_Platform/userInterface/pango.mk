
#
# pango configure arguments
#
PANGO_CONFIGURE = ./configure \
	PKG_CONFIG_PATH=$(PLATFORM_PKGCFG) \
	CC=$(RI_CC) CXX=$(RI_CXX) \
	CPPFLAGS='-I$(PLATFORM_INST_DIR)/include' \
	LDFLAGS='-L$(PLATFORM_INST_DIR)/lib' \
	--build=$(RI_BUILD) --host=$(RI_HOST) \
	--prefix=$(PLATFORM_INST_DIR)


