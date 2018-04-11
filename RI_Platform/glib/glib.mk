
#
# glib configure arguments
#
ifeq ($(GLIB_CONF_CACHE_FILE),)
	CACHE_FILE_ARG =
else
	CACHE_FILE_ARG = --cache-file=$(GLIB_CONF_CACHE_FILE)
endif
GLIB_CONFIGURE = PATH='$(GLIB_TOOLS_PATH):$(PLATFORM_PATH):$(PATH)' ./configure \
	$(CACHE_FILE_ARG) \
	--prefix=$(PLATFORM_INST_DIR) \
	--build=$(RI_BUILD) \
	--host=$(RI_HOST) \
	--enable-option-checking \
	--enable-maintainer-mode \
	--disable-dependency-tracking \
	--enable-debug=$(GLIB_DEBUG) \
	--disable-gc-friendly \
	--disable-mem-pools \
	--enable-threads \
	--disable-rebuilds \
	--disable-largefile \
	--enable-iconv-cache=no \
	--disable-static \
	--enable-shared \
	--enable-fast-install \
	--enable-libtool-lock \
	--enable-included-printf \
	--disable-selinux \
	--disable-fam \
	--disable-dtrace \
	--disable-xattr \
	--enable-regex \
	--disable-gtk-doc \
	--disable-gtk-doc-pdf \
	--disable-gtk-doc-html \
	--disable-man \
	$(GLIB_LIBICONV) \
	--without-gnu-ld \
	--without-pic \
	$(GLIB_THREADS) \
	--with-pcre=internal \
	CC=$(RI_CC) \
	LDFLAGS='$(GLIB_LDFLAGS) -L$(PLATFORM_INST_DIR)/lib' \
	CPPFLAGS='$(GLIB_CPPFLAGS) -I$(PLATFORM_INST_DIR)/include' \
	CXX=$(RI_CXX)

