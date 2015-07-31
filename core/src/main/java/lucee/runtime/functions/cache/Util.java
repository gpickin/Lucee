/**
 *
 * Copyright (c) 2014, the Railo Company Ltd. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either 
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public 
 * License along with this library.  If not, see <http://www.gnu.org/licenses/>.
 * 
 **/
package lucee.runtime.functions.cache;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;

import lucee.commons.io.cache.Cache;
import lucee.commons.io.cache.CacheEntryFilter;
import lucee.commons.io.cache.exp.CacheException;
import lucee.commons.lang.StringUtil;
import lucee.loader.engine.CFMLEngine;
import lucee.runtime.PageContext;
import lucee.runtime.cache.CacheConnection;
import lucee.runtime.config.Config;
import lucee.runtime.config.ConfigImpl;
import lucee.runtime.config.ConfigWeb;
import lucee.runtime.config.Constants;
import lucee.runtime.config.Password;
import lucee.runtime.config.PasswordImpl;
import lucee.runtime.engine.ThreadLocalPageContext;
import lucee.runtime.listener.ApplicationContext;
import lucee.runtime.listener.ModernApplicationContext;
import lucee.runtime.op.Caster;
import lucee.runtime.type.util.KeyConstants;

public class Util {
	
	/**
	 * get the default cache for a certain type, also check definitions in application context (application . cfc/cfapplication)
	 * @param pc current PageContext
	 * @param type default type -> Config.CACHE_DEFAULT_...
	 * @param defaultValue value returned when there is no default cache for this type
	 * @return matching cache
	 */
	public static Cache getDefault(PageContext pc, int type,Cache defaultValue) {
		// get default from application conetx
		String name=null;
		if(pc!=null && pc.getApplicationContext()!=null)
			name=pc.getApplicationContext().getDefaultCacheName(type);
		Config config=ThreadLocalPageContext.getConfig(pc);
		if(!StringUtil.isEmpty(name)){
			Cache cc = getCache(config, name, null);
			if(cc!=null) return cc;
		}
		
		// get default from config
		CacheConnection cc= ((ConfigImpl)config).getCacheDefaultConnection(type);
		if(cc==null) return defaultValue;
		try {
			return cc.getInstance(config);
		} catch (Throwable t) {
			return defaultValue;
		}
	}
	
	/**
	 * get the default cache for a certain type, also check definitions in application context (application . cfc/cfapplication)
	 * @param pc current PageContext
	 * @param type default type -> Config.CACHE_DEFAULT_...
	 * @return matching cache
	 * @throws IOException 
	 */
	public static Cache getDefault(PageContext pc, int type) throws IOException {
		// get default from application conetx
		String name=pc!=null?pc.getApplicationContext().getDefaultCacheName(type):null;
		if(!StringUtil.isEmpty(name)){
			Cache cc = getCache(pc.getConfig(), name, null);
			if(cc!=null) return cc;
		}
		
		// get default from config
		Config config = ThreadLocalPageContext.getConfig(pc);
		CacheConnection cc= ((ConfigImpl)config).getCacheDefaultConnection(type);
		if(cc==null) throw new CacheException("there is no default "+toStringType(type,"")+" cache defined, you need to define this default cache in the Lucee Administrator");
		return cc.getInstance(config);
		
		
	}

	public static Cache getCache(PageContext pc,String cacheName, int type) throws IOException {
		if(StringUtil.isEmpty(cacheName)){
			return getDefault(pc, type);
		}
		return getCache(ThreadLocalPageContext.getConfig(pc), cacheName);
	}

	public static Cache getCache(PageContext pc,String cacheName, int type, Cache defaultValue)  {
		if(StringUtil.isEmpty(cacheName)){
			return getDefault(pc, type,defaultValue);
		}
		return getCache(ThreadLocalPageContext.getConfig(pc), cacheName,defaultValue);
	}
	
	
	public static Cache getCache(Config config,String cacheName) throws IOException {
		CacheConnection cc=  config.getCacheConnections().get(cacheName.toLowerCase().trim());
		if(cc==null) throw noCache(config,cacheName);
		return cc.getInstance(config);	
	}
	
	public static Cache getCache(Config config,String cacheName, Cache defaultValue) {
		CacheConnection cc= config.getCacheConnections().get(cacheName.toLowerCase().trim());
		if(cc==null) return defaultValue;
		try {
			return cc.getInstance(config);
		} catch (Throwable t) {
			return defaultValue;
		}	
	}
	public static CacheConnection getCacheConnection(Config config,String cacheName) throws IOException {
		CacheConnection cc= config.getCacheConnections().get(cacheName.toLowerCase().trim());
		if(cc==null) throw noCache(config,cacheName);
		return cc;	
	}

	public static CacheConnection getCacheConnection(Config config,String cacheName, CacheConnection defaultValue) {
		CacheConnection cc= config.getCacheConnections().get(cacheName.toLowerCase().trim());
		if(cc==null) return defaultValue;
		return cc;	
	}
	
	
	
	
	
	
	private static CacheException noCache(Config config, String cacheName) {
		StringBuilder sb=new StringBuilder("there is no cache defined with name [").append(cacheName).append("], available caches are [");
		Iterator<String> it = ((ConfigImpl)config).getCacheConnections().keySet().iterator();
		if(it.hasNext()){
			sb.append(it.next());
		}
		while(it.hasNext()){
			sb.append(", ").append(it.next());
		}
		sb.append("]");
		
		return new CacheException(sb.toString());
	}

	private static String toStringType(int type, String defaultValue) {
		if(type==Config.CACHE_TYPE_OBJECT) return "object";
		if(type==Config.CACHE_TYPE_TEMPLATE) return "template";
		if(type==Config.CACHE_TYPE_QUERY) return "query";
		if(type==Config.CACHE_TYPE_RESOURCE) return "resource";
		if(type==Config.CACHE_TYPE_FUNCTION) return "function";
		if(type==Config.CACHE_TYPE_INCLUDE) return "include";
		if(type==Config.CACHE_TYPE_HTTP) return "http";
		if(type==Config.CACHE_TYPE_FILE) return "file";
		if(type==Config.CACHE_TYPE_WEBSERVICE) return "webservice";
		return defaultValue;
	}

	public static String key(String key) {
		return key.toUpperCase().trim();
	}


	public static boolean removeEL(ConfigWeb config, CacheConnection cc)  {
		try {
			remove(config,cc);
			return true;
		} catch (Throwable e) {
			return false;
		}
	}
	public static void remove(ConfigWeb config, CacheConnection cc) throws Throwable  {
		Cache c = cc.getInstance(config);
		// FUTURE no reflection needed
		
		
		Method remove=null;
		try{
			remove = c.getClass().getMethod("remove", new Class[0]);
			
		}
		catch(Exception ioe){
			c.remove((CacheEntryFilter)null);
			return;
		}
		
		try {
			remove.invoke(c, new Object[0]);
		}
		catch (InvocationTargetException e) {
			throw e.getTargetException();
		}
	}

	public static int toType(String type, int defaultValue) {
		type=type.trim().toLowerCase();
		if("object".equals(type)) return Config.CACHE_TYPE_OBJECT;
		if("query".equals(type)) return Config.CACHE_TYPE_QUERY;
		if("resource".equals(type)) return Config.CACHE_TYPE_RESOURCE;
		if("template".equals(type)) return Config.CACHE_TYPE_TEMPLATE;
		if("function".equals(type)) return Config.CACHE_TYPE_FUNCTION;
		if("include".equals(type)) return Config.CACHE_TYPE_INCLUDE;
		if("http".equals(type)) return Config.CACHE_TYPE_HTTP;
		if("file".equals(type)) return Config.CACHE_TYPE_FILE;
		if("webservice".equals(type)) return Config.CACHE_TYPE_WEBSERVICE;
		return defaultValue;
	}

	public static String toType(int type, String defaultValue) {
		if(Config.CACHE_TYPE_OBJECT==type) return "object";
		if(Config.CACHE_TYPE_QUERY==type) return "query";
		if(Config.CACHE_TYPE_RESOURCE==type) return "resource";
		if(Config.CACHE_TYPE_TEMPLATE==type) return "template";
		if(Config.CACHE_TYPE_FUNCTION==type) return "function";
		if(Config.CACHE_TYPE_INCLUDE==type) return "include";
		if(Config.CACHE_TYPE_HTTP==type) return "http";
		if(Config.CACHE_TYPE_FILE==type) return "file";
		if(Config.CACHE_TYPE_WEBSERVICE==type) return "webservice";
		return defaultValue;
	}


    /**
     * returns true if the webAdminPassword matches the passed password if one is passed, or a password defined
     * in Application . cfc as this.webAdminPassword if null or empty-string is passed for password
     *
     * @param pc
     * @param password
     * @return
     * @throws lucee.runtime.exp.SecurityException
     */
    public static Password getPassword( PageContext pc, String password , boolean server) throws lucee.runtime.exp.SecurityException {   // TODO: move this to a utility class in a more generic package?
    	// no password passed
        if (StringUtil.isEmpty(password,true)) {
            ApplicationContext appContext = pc.getApplicationContext();
            if ( appContext instanceof ModernApplicationContext)
                password = Caster.toString( ( (ModernApplicationContext)appContext ).getCustom( KeyConstants._webAdminPassword ), "" );
        }
        else password=password.trim();
        
        if (StringUtil.isEmpty(password, true))
            throw new lucee.runtime.exp.SecurityException( "A Web Admin Password is required to manipulate Cache connections. " +
                    "You can either pass the password as an argument to this function, or set it in "
            		+(pc.getRequestDialect()==CFMLEngine.DIALECT_CFML?Constants.CFML_APPLICATION_EVENT_HANDLER:Constants.LUCEE_APPLICATION_EVENT_HANDLER)
            		+" with the variable [this.webAdminPassword]." );
        
        return PasswordImpl.passwordToCompare(pc.getConfig(), server, password);
    }
}