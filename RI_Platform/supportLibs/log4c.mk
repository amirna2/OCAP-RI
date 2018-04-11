
#
# log4c configure arguments
#
LOG4C_CONFIGURE = ./configure \
	CC=$(RI_CC) CXX=$(RI_CXX) \
	--build=$(RI_BUILD) --host=$(RI_HOST) \
	CPPFLAGS=$(LOG4C_CPPFLAGS) \
	LDFLAGS=$(LOG4C_LDFLAGS) \
	$(LOG4C_CONFIG_FLAGS) \
	--enable-shared \
	--disable-static \
	--disable-reread \
	--disable-test \
	--disable-doc \
	--disable-constructors \
	--without-expat \
	--prefix=$(PLATFORM_INST_DIR)
