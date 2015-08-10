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
package lucee.runtime.util;

import java.io.File;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lucee.cli.servlet.HTTPServletImpl;
import lucee.commons.lang.StringUtil;
import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.CFMLFactory;
import lucee.runtime.MappingImpl;
import lucee.runtime.PageContext;
import lucee.runtime.PageSource;
import lucee.runtime.config.Config;
import lucee.runtime.config.ConfigWeb;
import lucee.runtime.engine.ThreadLocalPageContext;
import lucee.runtime.listener.ApplicationListener;
import lucee.runtime.op.Caster;
import lucee.runtime.op.CreationImpl;
import lucee.runtime.type.dt.TimeSpan;
import lucee.runtime.type.dt.TimeSpanImpl;
import lucee.runtime.type.util.KeyConstants;
import lucee.runtime.type.util.ListUtil;

public class PageContextUtil {

	public static ApplicationListener getApplicationListener(PageContext pc) {
		PageSource ps = pc.getBasePageSource();
		if(ps!=null) {
			MappingImpl mapp=(MappingImpl) ps.getMapping();
			if(mapp!=null) return mapp.getApplicationListener();
		}
		return pc.getConfig().getApplicationListener();
	}


	public static String getCookieDomain(PageContext pc) {
		if(!pc.getApplicationContext().isSetDomainCookies()) return null;

		String result = Caster.toString(pc.cgiScope().get(KeyConstants._server_name, null),null);

		if(!StringUtil.isEmpty(result)) {

			String listLast = ListUtil.last(result, '.');
			if ( !lucee.runtime.op.Decision.isNumber(listLast) ) {    // if it's numeric then must be IP address
				int numparts = 2;
				int listLen = ListUtil.len( result, '.', true );

				if ( listLen > 2 ) {
					if ( listLast.length() == 2 || !StringUtil.isAscii(listLast) ) {      // country TLD

						int tldMinus1 = ListUtil.getAt( result, '.', listLen - 1, true, "" ).length();

						if ( tldMinus1 == 2 || tldMinus1 == 3 )                             // domain is in country like, example.co.uk or example.org.il
							numparts++;
					}
				}

				if ( listLen > numparts )
					result = result.substring( result.indexOf( '.' ) );
				else if ( listLen == numparts )
					result = "." + result;
			}
		}

		return result;
	}

	public static PageContext getPageContext(File contextRoot, String host, String scriptName, String queryString
			, Cookie[] cookies,Map<String, Object> headers,Map<String, String> parameters, 
			Map<String, Object> attributes, OutputStream os, boolean register, long timeout, boolean ignoreScopes) throws ServletException {
		
		if(contextRoot==null)contextRoot=new File(".");
		
		// Engine
		CFMLEngine engine=null;
		try{
			engine = CFMLEngineFactory.getInstance();
		}
		catch(Throwable t){}
		if(engine==null) throw new ServletException("there is no ServletContext");
		
		
		
		// Request
		HttpServletRequest req = CreationImpl.getInstance(engine).createHttpServletRequest(contextRoot, host, scriptName, queryString,
				cookies, new HashMap<String, Object>(),
				new HashMap<String, String>(), new HashMap<String, Object>(),
				null); 
		
		// Response
		HttpServletResponse rsp = CreationImpl.getInstance(engine).createHttpServletResponse(os);
		
		Config config = ThreadLocalPageContext.getConfig();
		
		CFMLFactory factory=null;
		HttpServlet servlet;
		if(config instanceof ConfigWeb) {
			ConfigWeb cw=(ConfigWeb)config;
			factory=cw.getFactory();
			servlet=factory.getServlet();
		}
		else {
			ServletConfig[] configs = engine.getServletConfigs();
			ServletConfig servletConfig=configs[0];
			factory = engine.getCFMLFactory(servletConfig, req);
			servlet=new HTTPServletImpl(servletConfig, servletConfig.getServletContext(), servletConfig.getServletName());
		}
		
		return factory.getLuceePageContext(servlet,req,rsp,null,false,-1,false,register,timeout,false,ignoreScopes);
	}
	
	
	public static void releasePageContext(PageContext pc, boolean register) {
		if(pc!=null)pc.getConfig().getFactory().releaseLuceePageContext(pc,register);
		ThreadLocalPageContext.register(null);
	}

	public static TimeSpan remainingTime(PageContext pc) {
		long ms = pc.getRequestTimeout()-(System.currentTimeMillis()-pc.getStartTime());
		if(ms>0) return TimeSpanImpl.fromMillis(ms);
		return TimeSpanImpl.fromMillis(0);
	}
}