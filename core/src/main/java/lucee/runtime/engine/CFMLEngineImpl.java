/**
 * Copyright (c) 2014, the Railo Company Ltd.
 * Copyright (c) 2015, Lucee Assosication Switzerland
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
 */
package lucee.runtime.engine;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.instrument.Instrumentation;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TimeZone;

import javax.script.ScriptEngineFactory;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;

import lucee.Info;
import lucee.cli.servlet.HTTPServletImpl;
import lucee.commons.collection.MapFactory;
import lucee.commons.io.FileUtil;
import lucee.commons.io.IOUtil;
import lucee.commons.io.SystemUtil;
import lucee.commons.io.compress.CompressUtil;
import lucee.commons.io.log.Log;
import lucee.commons.io.res.Resource;
import lucee.commons.io.res.ResourceProvider;
import lucee.commons.io.res.ResourcesImpl;
import lucee.commons.io.res.util.ResourceUtil;
import lucee.commons.io.res.util.ResourceUtilImpl;
import lucee.commons.lang.Pair;
import lucee.commons.lang.StringUtil;
import lucee.commons.lang.SystemOut;
import lucee.commons.lang.types.RefBoolean;
import lucee.commons.lang.types.RefBooleanImpl;
import lucee.commons.net.HTTPUtil;
import lucee.intergral.fusiondebug.server.FDControllerImpl;
import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.loader.engine.CFMLEngineFactorySupport;
import lucee.loader.engine.CFMLEngineWrapper;
import lucee.loader.osgi.BundleCollection;
import lucee.loader.util.Util;
import lucee.runtime.CFMLFactory;
import lucee.runtime.CFMLFactoryImpl;
import lucee.runtime.PageContext;
import lucee.runtime.PageSource;
import lucee.runtime.config.Config;
import lucee.runtime.config.ConfigServer;
import lucee.runtime.config.ConfigServerImpl;
import lucee.runtime.config.ConfigWeb;
import lucee.runtime.config.ConfigWebImpl;
import lucee.runtime.config.DeployHandler;
import lucee.runtime.config.Identification;
import lucee.runtime.config.Password;
import lucee.runtime.config.XMLConfigServerFactory;
import lucee.runtime.config.XMLConfigWebFactory;
import lucee.runtime.exp.ApplicationException;
import lucee.runtime.exp.PageException;
import lucee.runtime.exp.PageServletException;
import lucee.runtime.instrumentation.InstrumentationFactory;
import lucee.runtime.jsr223.ScriptEngineFactoryImpl;
import lucee.runtime.net.amf.AMFEngine;
import lucee.runtime.net.http.HTTPServletRequestWrap;
import lucee.runtime.net.http.HttpServletRequestDummy;
import lucee.runtime.net.http.HttpServletResponseDummy;
import lucee.runtime.net.http.ReqRspUtil;
import lucee.runtime.op.CastImpl;
import lucee.runtime.op.Caster;
import lucee.runtime.op.CreationImpl;
import lucee.runtime.op.DecisionImpl;
import lucee.runtime.op.ExceptonImpl;
import lucee.runtime.op.IOImpl;
import lucee.runtime.op.OperationImpl;
import lucee.runtime.op.StringsImpl;
import lucee.runtime.type.StructImpl;
import lucee.runtime.util.Cast;
import lucee.runtime.util.ClassUtil;
import lucee.runtime.util.ClassUtilImpl;
import lucee.runtime.util.Creation;
import lucee.runtime.util.DBUtil;
import lucee.runtime.util.DBUtilImpl;
import lucee.runtime.util.Decision;
import lucee.runtime.util.Excepton;
import lucee.runtime.util.HTMLUtil;
import lucee.runtime.util.HTMLUtilImpl;
import lucee.runtime.util.HTTPUtilImpl;
import lucee.runtime.util.IO;
import lucee.runtime.util.ListUtil;
import lucee.runtime.util.ListUtilImpl;
import lucee.runtime.util.ORMUtil;
import lucee.runtime.util.ORMUtilImpl;
import lucee.runtime.util.Operation;
import lucee.runtime.util.PageContextUtil;
import lucee.runtime.util.Strings;
import lucee.runtime.util.SystemUtilImpl;
import lucee.runtime.util.TemplateUtil;
import lucee.runtime.util.TemplateUtilImpl;
import lucee.runtime.util.XMLUtil;
import lucee.runtime.util.XMLUtilImpl;
import lucee.runtime.util.ZipUtil;
import lucee.runtime.util.ZipUtilImpl;
import lucee.runtime.video.VideoUtil;
import lucee.runtime.video.VideoUtilImpl;

import org.apache.felix.framework.Felix;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

//import com.intergral.fusiondebug.server.FDControllerFactory;

/**
 * The CFMl Engine
 */
public final class CFMLEngineImpl implements CFMLEngine {
	
	
	private static Map<String,CFMLFactory> initContextes=MapFactory.<String,CFMLFactory>getConcurrentMap();
    private static Map<String,CFMLFactory> contextes=MapFactory.<String,CFMLFactory>getConcurrentMap();
    private ConfigServerImpl configServer=null;
    private static CFMLEngineImpl engine=null;
    //private ServletConfig config;
    private CFMLEngineFactory factory;
    private AMFEngine amfEngine=null;
    private final RefBoolean controlerState=new RefBooleanImpl(true);
	private boolean allowRequestTimeout=true;
	private Monitor monitor;
	private List<ServletConfig> servletConfigs=new ArrayList<ServletConfig>();
	private long uptime;
	private InfoImpl info;
	
	private BundleCollection bundleCollection;
	
	private ScriptEngineFactory cfmlScriptEngine;
	private ScriptEngineFactory cfmlTagEngine;
	private ScriptEngineFactory luceeScriptEngine;
	private ScriptEngineFactory luceeTagEngine;
    
    //private static CFMLEngineImpl engine=new CFMLEngineImpl();

    private CFMLEngineImpl(CFMLEngineFactory factory, BundleCollection bc) {
    	this.factory=factory; 
    	this.bundleCollection=bc;
    	
    	// happen when Lucee is loaded directly
    	if(bundleCollection==null) {
    		try{
    			Properties prop = InfoImpl.getDefaultProperties(null);
    				
    			// read the config from default.properties
    			Map<String,Object> config=new HashMap<String, Object>();
    			Iterator<Entry<Object, Object>> it = prop.entrySet().iterator();
    			Entry<Object, Object> e;
    			String k;
    			while(it.hasNext()){
    				e = it.next();
    				k=(String) e.getKey();
    				if(!k.startsWith("org.") && !k.startsWith("felix.")) continue;
    				config.put(k, CFMLEngineFactorySupport.removeQuotes((String)e.getValue(),true));
    			}
    			
    			/*/ TODO no idea what is going on, but this is necessary atm
    			config.put(
    				Constants.FRAMEWORK_SYSTEMPACKAGES,
    				"org.w3c.dom,org.w3c.dom.bootstrap,org.w3c.dom.events,org.w3c.dom.ls,org.xml.sax,org.xml.sax.ext,org.xml.sax.helpers,javax.crypto,javax.crypto.spec");
    			
    			config.put(
	    				Constants.FRAMEWORK_BOOTDELEGATION,
	    				"coldfusion,coldfusion.image,coldfusion.runtime,coldfusion.runtime.java,coldfusion.server,coldfusion.sql,org,org.apache,org.apache.axis,org.apache.axis.encoding,org.apache.axis.encoding.ser,org.apache.taglibs,org.apache.taglibs.datetime,org.jfree,org.jfree.chart,org.jfree.chart.block,org.objectweb,org.objectweb.asm,org.opencfml,org.opencfml.cfx,lucee,lucee.commons,lucee.commons.activation,lucee.commons.cli,lucee.commons.collection,lucee.commons.collection.concurrent,lucee.commons.color,lucee.commons.date,lucee.commons.db,lucee.commons.digest,lucee.commons.i18n,lucee.commons.img,lucee.commons.io,lucee.commons.io.auto,lucee.commons.io.cache,lucee.commons.io.compress,lucee.commons.io.ini,lucee.commons.io.log,lucee.commons.io.log.log4j,lucee.commons.io.log.log4j.appender,lucee.commons.io.log.log4j.appender.task,lucee.commons.io.log.log4j.layout,lucee.commons.io.log.sl4j,lucee.commons.io.reader,lucee.commons.io.res,lucee.commons.io.res.filter,lucee.commons.io.res.type,lucee.commons.io.res.type.cache,lucee.commons.io.res.type.cfml,lucee.commons.io.res.type.compress,lucee.commons.io.res.type.datasource,lucee.commons.io.res.type.datasource.core,lucee.commons.io.res.type.file,lucee.commons.io.res.type.ftp,lucee.commons.io.res.type.http,lucee.commons.io.res.type.ram,lucee.commons.io.res.type.s3,lucee.commons.io.res.type.tar,lucee.commons.io.res.type.tgz,lucee.commons.io.res.type.zip,lucee.commons.io.res.util,lucee.commons.io.retirement,lucee.commons.lang,lucee.commons.lang.font,lucee.commons.lang.lock,lucee.commons.lang.mimetype,lucee.commons.lang.types,lucee.commons.lock,lucee.commons.lock.rw,lucee.commons.management,lucee.commons.math,lucee.commons.net,lucee.commons.net.http,lucee.commons.net.http.httpclient3,lucee.commons.net.http.httpclient3.entity,lucee.commons.net.http.httpclient4,lucee.commons.net.http.httpclient4.entity,lucee.commons.pdf,lucee.commons.res,lucee.commons.res.io,lucee.commons.res.io.filter,lucee.commons.security,lucee.commons.sql,lucee.commons.surveillance,lucee.commons.util,lucee.deployer,lucee.deployer.filter,lucee.intergral,lucee.intergral.fusiondebug,lucee.intergral.fusiondebug.server,lucee.intergral.fusiondebug.server.type,lucee.intergral.fusiondebug.server.type.coll,lucee.intergral.fusiondebug.server.type.nat,lucee.intergral.fusiondebug.server.type.qry,lucee.intergral.fusiondebug.server.type.simple,lucee.intergral.fusiondebug.server.util,lucee.runtime,lucee.runtime.cache,lucee.runtime.cache.eh,lucee.runtime.cache.eh.remote,lucee.runtime.cache.eh.remote.rest,lucee.runtime.cache.eh.remote.rest.sax,lucee.runtime.cache.eh.remote.soap,lucee.runtime.cache.legacy,lucee.runtime.cache.ram,lucee.runtime.cache.tag,lucee.runtime.cache.tag.include,lucee.runtime.cache.tag.query,lucee.runtime.cache.tag.request,lucee.runtime.cache.tag.smart,lucee.runtime.cache.tag.timespan,lucee.runtime.cache.tag.udf,lucee.runtime.cache.util,lucee.runtime.cfx,lucee.runtime.cfx.customtag,lucee.runtime.chart,lucee.runtime.coder,lucee.runtime.com,lucee.runtime.compiler,lucee.runtime.component,lucee.runtime.concurrency,lucee.runtime.config,lucee.runtime.config.ajax,lucee.runtime.config.component,lucee.runtime.converter,lucee.runtime.converter.bin,lucee.runtime.crypt,lucee.runtime.customtag,lucee.runtime.db,lucee.runtime.db.driver,lucee.runtime.db.driver.state,lucee.runtime.debug,lucee.runtime.debug.filter,lucee.runtime.dump,lucee.runtime.engine,lucee.runtime.err,lucee.runtime.exp,lucee.runtime.ext,lucee.runtime.ext.tag,lucee.runtime.extension,lucee.runtime.flash,lucee.runtime.format,lucee.runtime.functions,lucee.runtime.functions.arrays,lucee.runtime.functions.cache,lucee.runtime.functions.closure,lucee.runtime.functions.component,lucee.runtime.functions.conversion,lucee.runtime.functions.csrf,lucee.runtime.functions.dateTime,lucee.runtime.functions.decision,lucee.runtime.functions.displayFormatting,lucee.runtime.functions.dynamicEvaluation,lucee.runtime.functions.file,lucee.runtime.functions.gateway,lucee.runtime.functions.image,lucee.runtime.functions.international,lucee.runtime.functions.list,lucee.runtime.functions.math,lucee.runtime.functions.orm,lucee.runtime.functions.other,lucee.runtime.functions.owasp,lucee.runtime.functions.poi,lucee.runtime.functions.query,lucee.runtime.functions.rest,lucee.runtime.functions.s3,lucee.runtime.functions.string,lucee.runtime.functions.struct,lucee.runtime.functions.system,lucee.runtime.functions.video,lucee.runtime.functions.xml,lucee.runtime.gateway,lucee.runtime.gateway.proxy,lucee.runtime.helpers,lucee.runtime.i18n,lucee.runtime.img,lucee.runtime.img.coder,lucee.runtime.img.composite,lucee.runtime.img.filter,lucee.runtime.img.gif,lucee.runtime.img.interpolation,lucee.runtime.img.math,lucee.runtime.img.vecmath,lucee.runtime.instrumentation,lucee.runtime.interpreter,lucee.runtime.interpreter.ref,lucee.runtime.interpreter.ref.cast,lucee.runtime.interpreter.ref.func,lucee.runtime.interpreter.ref.literal,lucee.runtime.interpreter.ref.op,lucee.runtime.interpreter.ref.util,lucee.runtime.interpreter.ref.var,lucee.runtime.java,lucee.runtime.listener,lucee.runtime.lock,lucee.runtime.monitor,lucee.runtime.net,lucee.runtime.net.amf,lucee.runtime.net.ftp,lucee.runtime.net.http,lucee.runtime.net.imap,lucee.runtime.net.ipsettings,lucee.runtime.net.ldap,lucee.runtime.net.mail,lucee.runtime.net.ntp,lucee.runtime.net.pop,lucee.runtime.net.proxy,lucee.runtime.net.rpc,lucee.runtime.net.rpc.client,lucee.runtime.net.rpc.server,lucee.runtime.net.s3,lucee.runtime.net.smtp,lucee.runtime.op,lucee.runtime.op.date,lucee.runtime.op.validators,lucee.runtime.orm,lucee.runtime.osgi,lucee.runtime.poi,lucee.runtime.query,lucee.runtime.query.caster,lucee.runtime.reflection,lucee.runtime.reflection.pairs,lucee.runtime.reflection.storage,lucee.runtime.regex,lucee.runtime.registry,lucee.runtime.rest,lucee.runtime.rest.path,lucee.runtime.schedule,lucee.runtime.search,lucee.runtime.search.lucene2,lucee.runtime.search.lucene2.analyzer,lucee.runtime.search.lucene2.docs,lucee.runtime.search.lucene2.highlight,lucee.runtime.search.lucene2.html,lucee.runtime.search.lucene2.net,lucee.runtime.search.lucene2.query,lucee.runtime.security,lucee.runtime.services,lucee.runtime.spooler,lucee.runtime.spooler.mail,lucee.runtime.spooler.remote,lucee.runtime.spooler.test,lucee.runtime.sql,lucee.runtime.sql.exp,lucee.runtime.sql.exp.op,lucee.runtime.sql.exp.value,lucee.runtime.sql.old,lucee.runtime.tag,lucee.runtime.tag.util,lucee.runtime.text,lucee.runtime.text.csv,lucee.runtime.text.feed,lucee.runtime.text.pdf,lucee.runtime.text.xml,lucee.runtime.text.xml.storage,lucee.runtime.text.xml.struct,lucee.runtime.thread,lucee.runtime.timer,lucee.runtime.type,lucee.runtime.type.cfc,lucee.runtime.type.comparator,lucee.runtime.type.dt,lucee.runtime.type.it,lucee.runtime.type.query,lucee.runtime.type.ref,lucee.runtime.type.scope,lucee.runtime.type.scope.client,lucee.runtime.type.scope.session,lucee.runtime.type.scope.storage,lucee.runtime.type.scope.storage.clean,lucee.runtime.type.scope.storage.db,lucee.runtime.type.scope.util,lucee.runtime.type.sql,lucee.runtime.type.trace,lucee.runtime.type.util,lucee.runtime.type.wrap,lucee.runtime.user,lucee.runtime.util,lucee.runtime.util.pool,lucee.runtime.video,lucee.runtime.vm,lucee.runtime.writer,lucee.servlet,lucee.servlet.pic,lucee.transformer,lucee.transformer.bytecode,lucee.transformer.bytecode.cast,lucee.transformer.bytecode.expression,lucee.transformer.bytecode.expression.type,lucee.transformer.bytecode.expression.var,lucee.transformer.bytecode.literal,lucee.transformer.bytecode.op,lucee.transformer.bytecode.reflection,lucee.transformer.bytecode.statement,lucee.transformer.bytecode.statement.tag,lucee.transformer.bytecode.statement.udf,lucee.transformer.bytecode.util,lucee.transformer.bytecode.visitor,lucee.transformer.cfml,lucee.transformer.cfml.attributes,lucee.transformer.cfml.attributes.impl,lucee.transformer.cfml.evaluator,lucee.transformer.cfml.evaluator.func,lucee.transformer.cfml.evaluator.func.impl,lucee.transformer.cfml.evaluator.impl,lucee.transformer.cfml.expression,lucee.transformer.cfml.script,lucee.transformer.cfml.tag,lucee.transformer.expression,lucee.transformer.expression.literal,lucee.transformer.expression.var,lucee.transformer.library,lucee.transformer.library.function,lucee.transformer.library.tag,lucee.transformer.util");
    			*/
    			config.put(
	    				Constants.FRAMEWORK_BOOTDELEGATION,
	    				"lucee.*");
    		    			
    			
    			Felix felix = factory.getFelix(factory.getResourceRoot(),config);
    			
    			bundleCollection=new BundleCollection(felix, felix, null);
    			//bundleContext=bundleCollection.getBundleContext();
    		}
    		catch (Throwable t) {
				throw new RuntimeException(t);
			}
    	}
    	
    	
    	
    	this.info=new InfoImpl(bundleCollection==null?null:bundleCollection.core);
    	Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader()); // MUST better location for this
		
    	
    	CFMLEngineFactory.registerInstance((this));// patch, not really good but it works
        ConfigServerImpl cs = getConfigServerImpl();
    	
        // start the controler
        SystemOut.printDate(SystemUtil.getPrintWriter(SystemUtil.OUT),"Start CFML Controller");
        Controler controler = new Controler(cs,initContextes,5*1000,controlerState);
        controler.setDaemon(true);
        controler.setPriority(Thread.MIN_PRIORITY);
        controler.start();
        
        // install extension defined
        String extensionIds=System.getProperty("lucee-extensions");
        if(!StringUtil.isEmpty(extensionIds,true)) {
        	Log log = cs.getLog("deploy", true);
        	String[] ids = lucee.runtime.type.util.ListUtil.listToStringArray(extensionIds, ';');
        	String id;
        	for(int i=0;i<ids.length;i++){
        		id=ids[i].trim();
        		if(StringUtil.isEmpty(id,true)) continue;
        		DeployHandler.deployExtension(cs, id,log);
        	}
        }
        
        //print.e(System.getProperties());
        

        touchMonitor(cs);  
        this.uptime=System.currentTimeMillis();
        //this.config=config; 
    }

	public void touchMonitor(ConfigServerImpl cs) {
		if(monitor!=null && monitor.isAlive()) return; 
		monitor = new Monitor(cs,controlerState); 
        monitor.setDaemon(true);
        monitor.setPriority(Thread.MIN_PRIORITY);
        monitor.start(); 
	}

    /**
     * get singelton instance of the CFML Engine
     * @param factory
     * @return CFMLEngine
     */
    public static synchronized CFMLEngine getInstance(CFMLEngineFactory factory,BundleCollection bc) {
    	if(engine==null) {
    		engine=new CFMLEngineImpl(factory,bc);
        }
        return engine;
    }
    
    /**
     * get singelton instance of the CFML Engine, throwsexception when not already init
     * @param factory
     * @return CFMLEngine
     */
    public static synchronized CFMLEngine getInstance() throws ServletException {
    	if(engine!=null) return engine;
    	throw new ServletException("CFML Engine is not loaded");
    }
    
    @Override
    public void addServletConfig(ServletConfig config) throws ServletException {
    	servletConfigs.add(config);
    	String real=ReqRspUtil.getRootPath(config.getServletContext());
    	if(!initContextes.containsKey(real)) {
        	CFMLFactory jspFactory = loadJSPFactory(getConfigServerImpl(),config,initContextes.size());
            initContextes.put(real,jspFactory);
        }        
    }
    
    @Override
    public ConfigServer getConfigServer(Password password) throws PageException {
    	getConfigServerImpl().checkAccess(password);
    	return configServer;
    }

    @Override
    public ConfigServer getConfigServer(String key, long timeNonce) throws PageException {
    	getConfigServerImpl().checkAccess(key,timeNonce);
    	return configServer;
    }
    
    public void setConfigServerImpl(ConfigServerImpl cs) {
    	this.configServer=cs;
    }

    private ConfigServerImpl getConfigServerImpl() {
    	if(configServer==null) {
            try {
            	ResourceProvider frp = ResourcesImpl.getFileResourceProvider();
            	Resource context = frp.getResource(factory.getResourceRoot().getAbsolutePath()).getRealResource("context");
            	//CFMLEngineFactory.registerInstance(this);// patch, not really good but it works
                configServer=XMLConfigServerFactory.newInstance(
                        this,
                        initContextes,
                        contextes,
                        context);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return configServer;
    }
    
    private  CFMLFactoryImpl loadJSPFactory(ConfigServerImpl configServer, ServletConfig sg, int countExistingContextes) throws ServletException {
    	try {
            // Load Config
    		RefBoolean isCustomSetting=new RefBooleanImpl();
            Resource configDir=getConfigDirectory(sg,configServer,countExistingContextes,isCustomSetting);
            
            CFMLFactoryImpl factory=new CFMLFactoryImpl(this,sg);
            ConfigWebImpl config=XMLConfigWebFactory.newInstance(factory,configServer,configDir,isCustomSetting.toBooleanValue(),sg);
            factory.setConfig(config);
            return factory;
        }
        catch (Exception e) {
            ServletException se= new ServletException(e.getMessage());
            se.setStackTrace(e.getStackTrace());
            throw se;
        } 
        
    }   

    /**
     * loads Configuration File from System, from init Parameter from web.xml
     * @param sg
     * @param configServer 
     * @param countExistingContextes 
     * @return return path to directory
     */
    private Resource getConfigDirectory(ServletConfig sg, ConfigServerImpl configServer, int countExistingContextes, RefBoolean isCustomSetting) throws PageServletException {
    	isCustomSetting.setValue(true);
    	ServletContext sc=sg.getServletContext();
        String strConfig=sg.getInitParameter("configuration");
        if(StringUtil.isEmpty(strConfig))strConfig=sg.getInitParameter("lucee-web-directory");
        if(StringUtil.isEmpty(strConfig))strConfig=System.getProperty("lucee.web.dir");
        
        if(StringUtil.isEmpty(strConfig)) {
        	isCustomSetting.setValue(false);
        	strConfig="{web-root-directory}/WEB-INF/lucee/";
        }
        // only for backward compatibility
        else if(strConfig.startsWith("/WEB-INF/lucee/"))strConfig="{web-root-directory}"+strConfig;
        
        
        strConfig=StringUtil.removeQuotes(strConfig,true);
        
        
        
        // static path is not allowed
        if(countExistingContextes>1 && strConfig!=null && strConfig.indexOf('{')==-1){
        	String text="static path ["+strConfig+"] for servlet init param [lucee-web-directory] is not allowed, path must use a web-context specific placeholder.";
        	System.err.println(text);
        	throw new PageServletException(new ApplicationException(text));
        }
        strConfig=SystemUtil.parsePlaceHolder(strConfig,sc,configServer.getLabels());
        
        
        
        ResourceProvider frp = ResourcesImpl.getFileResourceProvider();
        Resource root = frp.getResource(ReqRspUtil.getRootPath(sc));
        Resource configDir=ResourceUtil.createResource(root.getRealResource(strConfig), FileUtil.LEVEL_PARENT_FILE,FileUtil.TYPE_DIR);
        
        if(configDir==null) {
            configDir=ResourceUtil.createResource(frp.getResource(strConfig), FileUtil.LEVEL_GRAND_PARENT_FILE,FileUtil.TYPE_DIR);
        }
        if(configDir==null) throw new PageServletException(new ApplicationException("path ["+strConfig+"] is invalid"));
        
        if(!configDir.exists() || ResourceUtil.isEmptyDirectory(configDir, null)){
        	Resource railoRoot;
        	// there is a railo directory
        	if(configDir.getName().equals("lucee") && (railoRoot=configDir.getParentResource().getRealResource("railo")).isDirectory()) {
        		try {
					copyRecursiveAndRename(railoRoot,configDir);
				}
				catch (IOException e) {
					try {
	    				configDir.createDirectory(true);
	    			} 
	            	catch (IOException ioe) {}
					return configDir;
				}
				// zip the railo-server di and delete it (optional)
				try {
					Resource p=railoRoot.getParentResource();
					CompressUtil.compress(CompressUtil.FORMAT_ZIP, railoRoot, p.getRealResource("railo-web-context-old.zip"), false, -1);
					ResourceUtil.removeEL(railoRoot, true);
				}
				catch(Throwable t){t.printStackTrace();}
        	}
        	else {
            	try {
    				configDir.createDirectory(true);
    			} 
            	catch (IOException e) {}	
        	}
        }
        return configDir;
    }
    
    private File getDirectoryByProp(String name) {
		String value=System.getProperty(name);
		if(Util.isEmpty(value,true)) return null;
		
		File dir=new File(value);
		dir.mkdirs();
		if (dir.isDirectory()) return dir;
		return null;
	}
    
    private static void copyRecursiveAndRename(Resource src,Resource trg) throws IOException {
	 	if(!src.exists()) return ;
		if(src.isDirectory()) {
			if(!trg.exists())trg.mkdirs();
			
			Resource[] files = src.listResources();
				for(int i=0;i<files.length;i++) {
					copyRecursiveAndRename(files[i],trg.getRealResource(files[i].getName()));
				}
		}
		else if(src.isFile()) {
			if(trg.getName().endsWith(".rc") || trg.getName().startsWith(".")) {
				return;
			}
					
			if(trg.getName().equals("railo-web.xml.cfm")) {
				trg=trg.getParentResource().getRealResource("lucee-web.xml.cfm");
				// cfLuceeConfiguration
				InputStream is = src.getInputStream();
				OutputStream os = trg.getOutputStream();
					try{
						String str=Util.toString(is);
						str=str.replace("<cfRailoConfiguration", "<!-- copy from Railo context --><cfLuceeConfiguration");
						str=str.replace("</cfRailoConfiguration", "</cfLuceeConfiguration");
						str=str.replace("<railo-configuration", "<lucee-configuration");
						str=str.replace("</railo-configuration", "</lucee-configuration");
						str=str.replace("{railo-config}", "{lucee-config}");
						str=str.replace("{railo-server}", "{lucee-server}");
						str=str.replace("{railo-web}", "{lucee-web}");
						str=str.replace("\"railo.commons.", "\"lucee.commons.");
						str=str.replace("\"railo.runtime.", "\"lucee.runtime.");
						str=str.replace("\"railo.cfx.", "\"lucee.cfx.");
						str=str.replace("/railo-context.ra", "/lucee-context.lar");
						str=str.replace("/railo-context", "/lucee");
						str=str.replace("railo-server-context", "lucee-server");
						str=str.replace("http://www.getrailo.org", "http://stable.lucee.org");
						str=str.replace("http://www.getrailo.com", "http://stable.lucee.org");
						
						
						ByteArrayInputStream bais = new ByteArrayInputStream(str.getBytes());
						
						try {
					 		Util.copy(bais, os);
					 		bais.close();
					 	}
					 	finally {
					 		Util.closeEL(is, os);
					 	}
					}
					finally {
						Util.closeEL(is,os);
					}
				return;
			}

			InputStream is = src.getInputStream();
			OutputStream os = trg.getOutputStream();
			try{
				Util.copy(is, os);
			}
			finally {
				Util.closeEL(is, os);
			}
		}
	 }
    
    @Override
    public CFMLFactory getCFMLFactory(ServletConfig srvConfig,HttpServletRequest req) throws ServletException {
    	ServletContext srvContext = srvConfig.getServletContext();
    	
    	String real=ReqRspUtil.getRootPath(srvContext);
        ConfigServerImpl cs = getConfigServerImpl();
    	
        // Load JspFactory
        
        CFMLFactory factory=contextes.get(real);
        if(factory==null) {
        	factory=initContextes.get(real);
            if(factory==null) {
                factory=loadJSPFactory(cs,srvConfig,initContextes.size());
                initContextes.put(real,factory);
            }
            contextes.put(real,factory);
            
            try {
            	String cp = req.getContextPath();
            	if(cp==null)cp="";
				((CFMLFactoryImpl)factory).setURL(new URL(req.getScheme(),req.getServerName(),req.getServerPort(),cp));
			} 
            catch (MalformedURLException e) {
				e.printStackTrace();
			}
        }
        return factory;
    }
    
    @Override
    public void service(HttpServlet servlet, HttpServletRequest req, HttpServletResponse rsp) throws ServletException, IOException {
    	CFMLFactory factory=getCFMLFactory(servlet.getServletConfig(), req);
    	
        PageContext pc = factory.getLuceePageContext(servlet,req,rsp,null,false,-1,false,true,-1,true,false);
        ThreadQueue queue = factory.getConfig().getThreadQueue();
        queue.enter(pc);
        try {
        	pc.execute(pc.getHttpServletRequest().getServletPath(),false,true);
        } 
        catch (PageException pe) {
			throw new PageServletException(pe);
		}
        finally {
        	queue.exit(pc);
            factory.releaseLuceePageContext(pc,true);
            //FDControllerFactory.notifyPageComplete();
        }
    }
    
    @Override
    public void serviceCFML(HttpServlet servlet, HttpServletRequest req, HttpServletResponse rsp) throws ServletException, IOException {
    	CFMLFactory factory=getCFMLFactory(servlet.getServletConfig(), req);
    	
        PageContext pc = factory.getLuceePageContext(servlet,req,rsp,null,false,-1,false,true,-1,true,false);
        ThreadQueue queue = factory.getConfig().getThreadQueue();
        queue.enter(pc);
        try {
        	/*print.out("INCLUDE");
        	print.out("servlet_path:"+req.getAttribute("javax.servlet.include.servlet_path"));
        	print.out("request_uri:"+req.getAttribute("javax.servlet.include.request_uri"));
        	print.out("context_path:"+req.getAttribute("javax.servlet.include.context_path"));
        	print.out("path_info:"+req.getAttribute("javax.servlet.include.path_info"));
        	print.out("query_string:"+req.getAttribute("javax.servlet.include.query_string"));
        	print.out("FORWARD");
        	print.out("servlet_path:"+req.getAttribute("javax.servlet.forward.servlet_path"));
        	print.out("request_uri:"+req.getAttribute("javax.servlet.forward.request_uri"));
        	print.out("context_path:"+req.getAttribute("javax.servlet.forward.context_path"));
        	print.out("path_info:"+req.getAttribute("javax.servlet.forward.path_info"));
        	print.out("query_string:"+req.getAttribute("javax.servlet.forward.query_string"));
        	print.out("---");
        	print.out(req.getServletPath());
        	print.out(pc.getHttpServletRequest().getServletPath());
        	*/
        	
        	pc.executeCFML(pc.getHttpServletRequest().getServletPath(),false,true);
        } 
        catch (PageException pe) {
			throw new PageServletException(pe);
		}
        finally {
        	queue.exit(pc);
            factory.releaseLuceePageContext(pc,true);
            //FDControllerFactory.notifyPageComplete();
        }
    }

	@Override
	public void serviceFile(HttpServlet servlet, HttpServletRequest req, HttpServletResponse rsp) throws ServletException, IOException {
		req=new HTTPServletRequestWrap(req);
		CFMLFactory factory=getCFMLFactory( servlet.getServletConfig(), req);
        ConfigWeb config = factory.getConfig();
        PageSource ps = config.getPageSourceExisting(null, null, req.getServletPath(), false, true, true, false);
        //Resource res = ((ConfigWebImpl)config).getPhysicalResourceExistingX(null, null, req.getServletPath(), false, true, true); 
        
		if(ps==null) {
    		rsp.sendError(404);
    	}
    	else {
    		Resource res = ps.getResource();
    		if(res==null) {
    			rsp.sendError(404);
    		}
    		else {
	    		ReqRspUtil.setContentLength(rsp,res.length());
	    		String mt = servlet.getServletContext().getMimeType(req.getServletPath());
	    		if(!StringUtil.isEmpty(mt))ReqRspUtil.setContentType(rsp,mt);
	    		IOUtil.copy(res, rsp.getOutputStream(), true);
    		}
    	}
	}
	

	@Override
	public void serviceRest(HttpServlet servlet, HttpServletRequest req, HttpServletResponse rsp) throws ServletException, IOException {
		req=new HTTPServletRequestWrap(req);
		CFMLFactory factory=getCFMLFactory(servlet.getServletConfig(), req);
        
		PageContext pc = factory.getLuceePageContext(servlet,req,rsp,null,false,-1,false,true,-1,true,false);
        ThreadQueue queue = factory.getConfig().getThreadQueue();
        queue.enter(pc);
        try {
        	pc.executeRest(pc.getHttpServletRequest().getServletPath(),false);
        } 
        catch (PageException pe) {
			throw new PageServletException(pe);
		}
        finally {
        	queue.exit(pc);
            factory.releaseLuceePageContext(pc,true);
            //FDControllerFactory.notifyPageComplete();
        }
		
		
	}
    

    /*private String getContextList() {
        return List.arrayToList((String[])contextes.keySet().toArray(new String[contextes.size()]),", ");
    }*/

    @Override
    public String getVersion() {
        return info.getVersion().toString();
    }
    
    @Override
	public Info getInfo() {
        return info;
    }

    @Override
    public String getUpdateType() {
        return getConfigServerImpl().getUpdateType();
    }

    @Override
    public URL getUpdateLocation() {
        return getConfigServerImpl().getUpdateLocation();
    }

    @Override
    public Identification getIdentification() {
        return getConfigServerImpl().getIdentification();
    }

    @Override
    public boolean can(int type, Password password) {
        return getConfigServerImpl().passwordEqual(password);
    }

    @Override
	public CFMLEngineFactory getCFMLEngineFactory() {
        return factory;
    }

    @Override
	public void serviceAMF(HttpServlet servlet, HttpServletRequest req, HttpServletResponse rsp) throws ServletException, IOException {
    	req=new HTTPServletRequestWrap(req);
    	if(amfEngine==null) amfEngine=new AMFEngine();
		amfEngine.service(servlet,req,rsp);
    }

    @Override
    public void reset() {
    	reset(null);
    }
    
    @Override
    public void reset(String configId) {
        
        CFMLFactoryImpl cfmlFactory;
        //ScopeContext scopeContext;
        try {
	        Iterator<String> it = contextes.keySet().iterator();
	        while(it.hasNext()) {
	        	try {
		            cfmlFactory=(CFMLFactoryImpl) contextes.get(it.next());
		            if(configId!=null && !configId.equals(cfmlFactory.getConfigWebImpl().getIdentification().getId())) continue;
		            	
		            // scopes
		            try{cfmlFactory.getScopeContext().clear();}catch(Throwable t){t.printStackTrace();}
		            
		            // PageContext
		            try{cfmlFactory.resetPageContext();}catch(Throwable t){t.printStackTrace();}
		            
		            // Query Cache
		            try{ 
		            	PageContext pc = ThreadLocalPageContext.get();
		            	if(pc!=null) {
		            		pc.getConfig().getCacheHandlerCollection(Config.CACHE_TYPE_QUERY,null).clear(pc);
		            		pc.getConfig().getCacheHandlerCollection(Config.CACHE_TYPE_FUNCTION,null).clear(pc);
		            		pc.getConfig().getCacheHandlerCollection(Config.CACHE_TYPE_INCLUDE,null).clear(pc);
		            	}
		            	//cfmlFactory.getDefaultQueryCache().clear(null);
		            }catch(Throwable t){t.printStackTrace();}
		            
		            
		            
		            // Gateway
		            try{ cfmlFactory.getConfigWebImpl().getGatewayEngine().reset();}catch(Throwable t){t.printStackTrace();}
		            
	        	}
	        	catch(Throwable t){
	        		t.printStackTrace();
	        	}
	        }
        }
    	finally {
            // Controller
            controlerState.setValue(false);
    	}
    }
    
    @Override
    public Cast getCastUtil() {
        return CastImpl.getInstance();
    }

    @Override
    public Operation getOperatonUtil() {
        return OperationImpl.getInstance();
    }

    @Override
    public Decision getDecisionUtil() {
        return DecisionImpl.getInstance();
    }

    @Override
    public Excepton getExceptionUtil() {
        return ExceptonImpl.getInstance();
    }

    @Override
    public Creation getCreationUtil() {
    	return CreationImpl.getInstance(this);
    }

    @Override
    public IO getIOUtil() {
        return IOImpl.getInstance();
    }

    @Override
    public Strings getStringUtil() {
        return StringsImpl.getInstance();
    }

	@Override
	public Object getFDController() {
		engine.allowRequestTimeout(false);
		
		return new FDControllerImpl(engine,engine.getConfigServerImpl().getSerialNumber());
	}

	public Map<String,CFMLFactory> getCFMLFactories() {
		return initContextes;
	}

	@Override
	public lucee.runtime.util.ResourceUtil getResourceUtil() {
		return ResourceUtilImpl.getInstance();
	}

	@Override
	public lucee.runtime.util.HTTPUtil getHTTPUtil() {
		return HTTPUtilImpl.getInstance();
	}

	@Override
	public PageContext getThreadPageContext() {
		return ThreadLocalPageContext.get();
	}

	@Override
	public Config getThreadConfig() {
		return ThreadLocalPageContext.getConfig();
	}

	@Override
	public void registerThreadPageContext(PageContext pc) {
		ThreadLocalPageContext.register(pc);
	}

	@Override
	public VideoUtil getVideoUtil() {
		return VideoUtilImpl.getInstance();
	}

	@Override
	public ZipUtil getZipUtil() {
		return ZipUtilImpl.getInstance();
	}

	@Override
	public String getState() {
		return info.getStateAsString();
	}

	public void allowRequestTimeout(boolean allowRequestTimeout) {
		this.allowRequestTimeout=allowRequestTimeout;
	}

	public boolean allowRequestTimeout() {
		return allowRequestTimeout;
	}
	
	public boolean isRunning() {
		try{
			CFMLEngine other = CFMLEngineFactory.getInstance();
			// FUTURE patch, do better impl when changing loader
			if(other!=this && controlerState.toBooleanValue() &&  !(other instanceof CFMLEngineWrapper)) {
				SystemOut.printDate("CFMLEngine is still set to true but no longer valid, "+lucee.runtime.config.Constants.NAME+" disable this CFMLEngine.");
				controlerState.setValue(false);
				reset();
				return false;
			}
		}
		catch(Throwable t){}
		return controlerState.toBooleanValue();
	}

	@Override
	public void cli(Map<String, String> config, ServletConfig servletConfig) throws IOException,JspException,ServletException {
		ServletContext servletContext = servletConfig.getServletContext();
		HTTPServletImpl servlet=new HTTPServletImpl(servletConfig, servletContext, servletConfig.getServletName());

		// webroot
		String strWebroot=config.get("webroot");
		if(StringUtil.isEmpty(strWebroot,true)) throw new IOException("missing webroot configuration");
		Resource root=ResourcesImpl.getFileResourceProvider().getResource(strWebroot);
		root.mkdirs();
		
		// serverName
		String serverName=config.get("server-name");
		if(StringUtil.isEmpty(serverName,true))serverName="localhost";
		
		// uri
		String strUri=config.get("uri");
		if(StringUtil.isEmpty(strUri,true)) throw new IOException("missing uri configuration");
		URI uri;
		try {
			uri = lucee.commons.net.HTTPUtil.toURI(strUri);
		} catch (URISyntaxException e) {
			throw Caster.toPageException(e);
		}
		
		// cookie
		Cookie[] cookies;
		String strCookie=config.get("cookie");
		if(StringUtil.isEmpty(strCookie,true)) cookies=new Cookie[0];
		else {
			Map<String,String> mapCookies=HTTPUtil.parseParameterList(strCookie,false,null);
			int index=0;
			cookies=new Cookie[mapCookies.size()];
			Entry<String, String> entry;
			Iterator<Entry<String, String>> it = mapCookies.entrySet().iterator();
			Cookie c;
			while(it.hasNext()){
				entry = it.next();
				c=ReqRspUtil.toCookie(entry.getKey(),entry.getValue(),null);
				if(c!=null)cookies[index++]=c;
				else throw new IOException("cookie name ["+entry.getKey()+"] is invalid");
			}
		}
		

		// header
		Pair[] headers=new Pair[0];
		
		// parameters
		Pair[] parameters=new Pair[0];
		
		// attributes
		StructImpl attributes = new StructImpl();
		ByteArrayOutputStream os=new ByteArrayOutputStream();
		
		
		
		
		HttpServletRequestDummy req=new HttpServletRequestDummy(
				root,serverName,uri.getPath(),uri.getQuery(),cookies,headers,parameters,attributes,null);
		req.setProtocol("CLI/1.0");
		HttpServletResponse rsp=new HttpServletResponseDummy(os);
		
		serviceCFML(servlet, req, rsp);
		String res = os.toString(ReqRspUtil.getCharacterEncoding(null,rsp).name());
		System.out.println(res);
	}
	
	@Override
	public ServletConfig[] getServletConfigs(){
		return servletConfigs.toArray(new ServletConfig[servletConfigs.size()]);
	}

	@Override
	public long uptime() {
		return uptime;
	}

	/*public Bundle getCoreBundle() {
		return bundle;
	}*/

	@Override
	public BundleCollection getBundleCollection() {
		return bundleCollection;
	}
	
	@Override
	public BundleContext getBundleContext() {
		return bundleCollection.getBundleContext();
	}

	@Override
	public ClassUtil getClassUtil() {
		return new ClassUtilImpl();
	}

	@Override
	public XMLUtil getXMLUtil() {
		return new XMLUtilImpl();
	}

	@Override
	public ListUtil getListUtil() {
		return new ListUtilImpl();
	}

	@Override
	public DBUtil getDBUtil() {
		return new DBUtilImpl();
	}

	@Override
	public ORMUtil getORMUtil() {
		return new ORMUtilImpl();
	}

	@Override
	public TemplateUtil getTemplateUtil() {
		return new TemplateUtilImpl();
	}

	@Override
	public HTMLUtil getHTMLUtil() {
		return new HTMLUtilImpl();
	}

	@Override
	public ScriptEngineFactory getScriptEngineFactory(int dialect) {
		
		if(dialect==CFMLEngine.DIALECT_CFML) {
			if(cfmlScriptEngine==null) cfmlScriptEngine=new ScriptEngineFactoryImpl(this,false,dialect);
			return cfmlScriptEngine;
		}
		
		if(luceeScriptEngine==null) luceeScriptEngine=new ScriptEngineFactoryImpl(this,false,dialect);
		return luceeScriptEngine;
	}

	@Override
	public ScriptEngineFactory getTagEngineFactory(int dialect) {
		
		if(dialect==CFMLEngine.DIALECT_CFML) {
			if(cfmlTagEngine==null) cfmlTagEngine=new ScriptEngineFactoryImpl(this,true,dialect);
			return cfmlTagEngine;
		}
		
		if(luceeTagEngine==null) luceeTagEngine=new ScriptEngineFactoryImpl(this,true,dialect);
		return luceeTagEngine;
	}

	@Override
	public PageContext createPageContext(File contextRoot, String host, String scriptName, String queryString
			, Cookie[] cookies,Map<String, Object> headers,Map<String, String> parameters, 
			Map<String, Object> attributes, OutputStream os, long timeout, boolean register) throws ServletException {
		return PageContextUtil.getPageContext(contextRoot,host, scriptName, queryString, cookies, headers, parameters, attributes, os,register,timeout,false);
	}
	
	@Override
	public ConfigWeb createConfig(File contextRoot,String host, String scriptName) throws ServletException {
		// TODO do a mored rect approach
		PageContext pc = null;
		try{
			pc = PageContextUtil.getPageContext(contextRoot,host,scriptName, null, null, null, null, null, null,false,-1,false);
			return pc.getConfig();
		}
		finally{
			pc.getConfig().getFactory().releaseLuceePageContext(pc, false);
		}
		
	}

	@Override
	public void releasePageContext(PageContext pc, boolean unregister) {
		PageContextUtil.releasePageContext(pc,unregister);
	}

	@Override
	public lucee.runtime.util.SystemUtil getSystemUtil() {
		return new SystemUtilImpl();
	}

	@Override
	public TimeZone getThreadTimeZone() {
		return ThreadLocalPageContext.getTimeZone();
	}

	@Override
	public Instrumentation getInstrumentation() {
		return InstrumentationFactory.getInstrumentation(ThreadLocalPageContext.getConfig());
	}

}