
#
# xml2 configure arguments
#
XML2_CONFIGURE = ./configure \
	CC=$(RI_CC) CXX=$(RI_CXX) \
	--build=$(RI_BUILD) --host=$(RI_HOST) \
	LDFLAGS=$(XML2_LDFLAGS) \
	--with-python=no \
	$(XML2_CONFIG_FLAGS) \
	--with-iconv=$(PLATFORM_INST_DIR) \
	--with-zlib=$(PLATFORM_INST_DIR) \
	--prefix=$(PLATFORM_INST_DIR)


