
#
# iconv configure arguments
#
ICONV_CONFIGURE = ./configure \
	CC=$(RI_CC) CXX=$(RI_CXX) \
	--build=$(RI_BUILD) --host=$(RI_HOST) \
	--prefix=$(PLATFORM_INST_DIR) \
	--without-libiconv-prefix \
	--without-libintl-prefix
