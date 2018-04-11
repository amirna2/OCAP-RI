
#
# GNU MP configure arguments
#
GMP_CONFIGURE = ./configure \
	CC=$(RI_CC) CXX=$(RI_CXX) \
	--build=$(RI_BUILD) --host=$(RI_HOST) \
	--prefix=$(PLATFORM_INST_DIR) \
	--disable-static --enable-shared \
	$(GMP_CONF_FLAGS)

