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

import lucee.commons.io.cache.Cache;
import lucee.commons.lang.StringUtil;
import lucee.runtime.PageContext;
import lucee.runtime.config.Config;
import lucee.runtime.exp.FunctionException;
import lucee.runtime.exp.PageException;
import lucee.runtime.ext.function.Function;
import lucee.runtime.op.Caster;

/**
 * 
 */
public final class CacheGet implements Function {

	private static final long serialVersionUID = -7164470356423036571L;

	public static Object call(PageContext pc, String key) throws PageException {
		try {
			return _call(pc, key, false, Util.getDefault(pc, Config.CACHE_TYPE_OBJECT));
		} 
		catch (IOException e) {
			throw Caster.toPageException(e);
		}
	}

	public static Object call(PageContext pc, String key, Object objThrowWhenNotExist) throws PageException {
		// default behavior, second parameter is a boolean
		Boolean throwWhenNotExist=Caster.toBoolean(objThrowWhenNotExist,null);
		if(throwWhenNotExist!=null) {
			try {
				return _call(pc, key, throwWhenNotExist.booleanValue(), Util.getDefault(pc, Config.CACHE_TYPE_OBJECT));
			} 
			catch (IOException e) {
				throw Caster.toPageException(e);
			}
		}
		
		// compatibility behavior, second parameter is a cacheName 
		if(objThrowWhenNotExist instanceof String) {
			String cacheName=(String)objThrowWhenNotExist;
			if(!StringUtil.isEmpty(cacheName)) {
				try {
					Cache cache = Util.getCache(pc.getConfig(),cacheName,null);
					
					if(cache!=null) 
						return _call(pc, key, false, cache);
				} 
				catch (IOException e) {
					throw Caster.toPageException(e);
				}
			}
		}
		
		// not a boolean or cacheName
		throw new FunctionException(pc, "cacheGet", 2, "ThrowWhenNotExist", "arguments needs to be a boolean value, but also a valid cacheName is supported for compatibility reasons to other engines");
	}
	
	public static Object call(PageContext pc, String key, Object objThrowWhenNotExist,String cacheName) throws PageException {
		
		
		Boolean throwWhenNotExist=Caster.toBoolean(objThrowWhenNotExist,null);
		if(throwWhenNotExist==null)throw new FunctionException(pc, "cacheGet", 2, "ThrowWhenNotExist", "arguments needs to be a boolean value");
		
		try {
			Cache cache = Util.getCache(pc,cacheName,Config.CACHE_TYPE_OBJECT);
			return _call(pc, key, throwWhenNotExist.booleanValue(), cache);
		} catch (IOException e) {
			throw Caster.toPageException(e);
		}
	}

	private static Object _call(PageContext pc, String key, boolean throwWhenNotExist,Cache cache) throws IOException {
		return throwWhenNotExist?cache.getValue(Util.key(key)):cache.getValue(Util.key(key),null);
	}
}