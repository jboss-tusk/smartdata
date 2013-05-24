package org.jboss.tusk.smartdata.data;

import org.jboss.logging.Logger;

public class CachedItemHelperFactory {
	
	private final static Logger LOG = Logger.getLogger(CachedItemHelperFactory.class);
	
	public static final String CACHED_ITEM_HELPER_PROP = "cacheditemhelper";
	public static final String DEFAULT_CACHED_HELPER_IMPL = "org.jboss.tusk.smartdata.data.impl.SampleHelper";
	
	public static CachedItemHelper getInstance() {
		return getInstance(System.getProperty(CACHED_ITEM_HELPER_PROP, DEFAULT_CACHED_HELPER_IMPL));
	}
	
	public static CachedItemHelper getInstance(String helperImpl) {
		LOG.info("Getting CachedItemHelper of type " + helperImpl);
		try {
			Class clazz = Class.forName(helperImpl);
			CachedItemHelper helper = (CachedItemHelper)clazz.newInstance();
			return helper;
		} catch (ClassNotFoundException ex) {
			throw new IllegalArgumentException("ClassNotFoundException getting CachedItemHelper " +
					"for class " + helperImpl + ": " + ex.getMessage());
		} catch (IllegalAccessException ex) {
			throw new IllegalArgumentException("IllegalAccessException getting CachedItemHelper " +
					"for class " + helperImpl + ": " + ex.getMessage());
		} catch (InstantiationException ex) {
			throw new IllegalArgumentException("IllegalAccessException getting CachedItemHelper " +
					"for class " + helperImpl + ": " + ex.getMessage());
		}
	}

}
