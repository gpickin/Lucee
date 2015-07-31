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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import lucee.commons.io.IOUtil;
import lucee.commons.io.res.Resource;
import lucee.commons.io.res.filter.ExtensionResourceFilter;
import lucee.commons.io.res.filter.ResourceFilter;
import lucee.commons.io.res.util.ResourceUtil;
import lucee.commons.lang.SystemOut;
import lucee.commons.lang.types.RefBoolean;
import lucee.runtime.CFMLFactoryImpl;
import lucee.runtime.Mapping;
import lucee.runtime.MappingImpl;
import lucee.runtime.PageSource;
import lucee.runtime.PageSourcePool;
import lucee.runtime.config.ConfigImpl;
import lucee.runtime.config.ConfigServer;
import lucee.runtime.config.ConfigWeb;
import lucee.runtime.config.DeployHandler;
import lucee.runtime.config.XMLConfigAdmin;
import lucee.runtime.lock.LockManagerImpl;
import lucee.runtime.net.smtp.SMTPConnectionPool;
import lucee.runtime.op.Caster;
import lucee.runtime.type.scope.ScopeContext;
import lucee.runtime.type.scope.storage.StorageScopeFile;
import lucee.runtime.type.util.ArrayUtil;

/**
 * own thread how check the main thread and his data 
 */
public final class Controler extends Thread {

	private int interval;
	private long lastMinuteInterval=System.currentTimeMillis();
	private long lastHourInterval=System.currentTimeMillis();
	
    private final Map contextes;
    private final RefBoolean run;
	//private ScheduleThread scheduleThread;
	private final ConfigServer configServer;

	/**
	 * @param contextes
	 * @param interval
	 * @param run 
	 */
	public Controler(ConfigServer configServer,Map contextes,int interval, RefBoolean run) {		
        this.contextes=contextes;
		this.interval=interval;
		this.run=run;
        this.configServer=configServer;
        
        Runtime.getRuntime().addShutdownHook(new ShutdownHook(configServer));
        
        // Register Memory Notification Listener
        //MemoryControler.init(configServer);
        
	}
	
	@Override
	public void run() {
		//scheduleThread.start();
		boolean firstRun=true;
		
		CFMLFactoryImpl factories[]=null;
		while(run.toBooleanValue()) {
	        try {
				sleep(interval);
			} 
            catch (InterruptedException e) {
				e.printStackTrace();
			}
            long now = System.currentTimeMillis();
            //print.out("now:"+new Date(now));
            boolean doMinute=lastMinuteInterval+60000<now;
            if(doMinute)lastMinuteInterval=now;
            boolean doHour=(lastHourInterval+(1000*60*60))<now;
            if(doHour)lastHourInterval=now;
            
            // broadcast cluster scope
            factories=toFactories(factories,contextes);
            try {
				ScopeContext.getClusterScope(configServer,true).broadcast();
			} 
            catch (Throwable t) {
				t.printStackTrace();
			}
            
            //try{Deploy Handler.deploy(configServer);}catch(Throwable t){t.printStackTrace();}
            try{XMLConfigAdmin.checkForChangesInConfigFile(configServer);}catch(Throwable t){}

            try{DeployHandler.deploy(configServer);}catch(Throwable t){t.printStackTrace();}
            
            
            // every minute
            if(doMinute) {
            	// deploy extensions, archives ...
				//try{DeployHandler.deploy(configServer);}catch(Throwable t){t.printStackTrace();}
                try{XMLConfigAdmin.checkForChangesInConfigFile(configServer);}catch(Throwable t){}
            }
            // every hour
            if(doHour) {
            	try{configServer.checkPermGenSpace(true);}catch(Throwable t){}
            }
            
            for(int i=0;i<factories.length;i++) {
	            run(factories[i], doMinute, doHour,firstRun);
	        }
	        if(factories.length>0)
				firstRun=false;
	    }    
	}

	private CFMLFactoryImpl[] toFactories(CFMLFactoryImpl[] factories,Map contextes) {
		if(factories==null || factories.length!=contextes.size())
			factories=(CFMLFactoryImpl[]) contextes.values().toArray(new CFMLFactoryImpl[contextes.size()]);
		
		return factories;
	}

	private void run(CFMLFactoryImpl cfmlFactory, boolean doMinute, boolean doHour, boolean firstRun) {
		try {
				boolean isRunning=cfmlFactory.getUsedPageContextLength()>0;   
			    if(isRunning) {
					cfmlFactory.checkTimeout();
			    }
				ConfigWeb config = null;
				
				if(firstRun) {
					config = cfmlFactory.getConfig();
					ThreadLocalConfig.register(config);
					
					config.reloadTimeServerOffset();
					checkOldClientFile(config);
					
					//try{checkStorageScopeFile(config,Session.SCOPE_CLIENT);}catch(Throwable t){}
					//try{checkStorageScopeFile(config,Session.SCOPE_SESSION);}catch(Throwable t){}
					try{config.reloadTimeServerOffset();}catch(Throwable t){}
					try{checkTempDirectorySize(config);}catch(Throwable t){}
					try{checkCacheFileSize(config);}catch(Throwable t){}
					try{cfmlFactory.getScopeContext().clearUnused();}catch(Throwable t){}
				}
				
				if(config==null) {
					config = cfmlFactory.getConfig();
					ThreadLocalConfig.register(config);
				}
				try{doCheckMappings(config);}catch(Throwable t){}
				
				
				//every Minute
				if(doMinute) {
					if(config==null) {
						config = cfmlFactory.getConfig();
						ThreadLocalConfig.register(config);
					}
					
					// deploy extensions, archives ...
					try{DeployHandler.deploy(config);}catch(Throwable t){t.printStackTrace();}
					
					// clear unused DB Connections
					try{((ConfigImpl)config).getDatasourceConnectionPool().clear();}catch(Throwable t){}
					// clear all unused scopes
					try{cfmlFactory.getScopeContext().clearUnused();}catch(Throwable t){}
					// Memory usage
					// clear Query Cache
					
					
					// contract Page Pool
					//try{doClearPagePools((ConfigWebImpl) config);}catch(Throwable t){}
					//try{checkPermGenSpace((ConfigWebImpl) config);}catch(Throwable t){}
					try{doCheckMappings(config);}catch(Throwable t){}
					try{doClearMailConnections();}catch(Throwable t){}
					// clean LockManager
					if(cfmlFactory.getUsedPageContextLength()==0)try{((LockManagerImpl)config.getLockManager()).clean();}catch(Throwable t){}
					
					try{XMLConfigAdmin.checkForChangesInConfigFile(config);}catch(Throwable t){}
	            	
				}
				// every hour
				if(doHour) {
					if(config==null) {
						config = cfmlFactory.getConfig();
						ThreadLocalConfig.register(config);
					}
					// time server offset
					try{config.reloadTimeServerOffset();}catch(Throwable t){}
					// check file based client/session scope
					//try{checkStorageScopeFile(config,Session.SCOPE_CLIENT);}catch(Throwable t){}
					//try{checkStorageScopeFile(config,Session.SCOPE_SESSION);}catch(Throwable t){}
					// check temp directory
					try{checkTempDirectorySize(config);}catch(Throwable t){}
					// check cache directory
					try{checkCacheFileSize(config);}catch(Throwable t){}
				}
			}
			catch(Throwable t){
				
			}
			finally{
				ThreadLocalConfig.release();
			}
	}

	private void doClearMailConnections() {
		SMTPConnectionPool.closeSessions();
	}

	private void checkOldClientFile(ConfigWeb config) {
		ExtensionResourceFilter filter = new ExtensionResourceFilter(".script",false);
		
		// move old structured file in new structure
		try {
			Resource dir = config.getClientScopeDir(),trgres;
			Resource[] children = dir.listResources(filter);
			String src,trg;
			int index;
			for(int i=0;i<children.length;i++) {
				src=children[i].getName();
				index=src.indexOf('-');

				trg=StorageScopeFile.getFolderName(src.substring(0,index), src.substring(index+1),false);
				trgres=dir.getRealResource(trg);
				if(!trgres.exists()){
					trgres.createFile(true);
					ResourceUtil.copy(children[i],trgres);
				}
					//children[i].moveTo(trgres);
				children[i].delete();
				
			}
		} catch (Throwable t) {}
	}

	private void checkCacheFileSize(ConfigWeb config) {
		checkSize(config,config.getCacheDir(),config.getCacheDirSize(),new ExtensionResourceFilter(".cache"));
	}
	
	private void checkTempDirectorySize(ConfigWeb config) {
		checkSize(config,config.getTempDirectory(),1024*1024*1024,null);
	}
	
	private void checkSize(ConfigWeb config,Resource dir,long maxSize, ResourceFilter filter) {
		if(!dir.exists()) return;
		Resource res=null;
		int count=ArrayUtil.size(filter==null?dir.list():dir.list(filter));
		long size=ResourceUtil.getRealSize(dir,filter);
		PrintWriter out = config.getOutWriter();
		SystemOut.printDate(out,"check size of directory ["+dir+"]");
		SystemOut.printDate(out,"- current size	["+size+"]");
		SystemOut.printDate(out,"- max size 	["+maxSize+"]");
		int len=-1;
		while(count>100000 || size>maxSize) {
			Resource[] files = filter==null?dir.listResources():dir.listResources(filter);
			if(len==files.length) break;// protect from inifinti loop
			len=files.length;
			for(int i=0;i<files.length;i++) {
				if(res==null || res.lastModified()>files[i].lastModified()) {
					res=files[i];
				}
			}
			if(res!=null) {
				size-=res.length();
				try {
					res.remove(true);
					count--;
				} catch (IOException e) {
					SystemOut.printDate(out,"cannot remove resource "+res.getAbsolutePath());
					break;
				}
			}
			res=null;
		}
		
	}

	private void doCheckMappings(ConfigWeb config) {
        Mapping[] mappings = config.getMappings();
        for(int i=0;i<mappings.length;i++) {
            Mapping mapping = mappings[i];
            mapping.check();
        }
    }

	private PageSourcePool[] getPageSourcePools(ConfigWeb config) {
		return getPageSourcePools(config.getMappings());
	}

	private PageSourcePool[] getPageSourcePools(Mapping... mappings) {
        PageSourcePool[] pools=new PageSourcePool[mappings.length];
        //int size=0;
        
        for(int i=0;i<mappings.length;i++) {
            pools[i]=((MappingImpl)mappings[i]).getPageSourcePool();
            //size+=pools[i].size();
        }
        return pools;
    }
    private int getPageSourcePoolSize(PageSourcePool[] pools) {
        int size=0;
        for(int i=0;i<pools.length;i++)size+=pools[i].size();
        return size;
    }
    private void removeOldest(PageSourcePool[] pools) {
        PageSourcePool pool=null;
        Object key=null;
        PageSource ps=null;
        
        long date=-1;
        for(int i=0;i<pools.length;i++) {
        	try {
	            Object[] keys=pools[i].keys();
	            for(int y=0;y<keys.length;y++) {
	                ps = pools[i].getPageSource(keys[y],false);
	                if(date==-1 || date>ps.getLastAccessTime()) {
	                    pool=pools[i];
	                    key=keys[y];
	                    date=ps.getLastAccessTime();
	                }
	            }
        	}
        	catch(Throwable t) {
        		pools[i].clear();
        	}
        	
        }
        if(pool!=null)pool.remove(key);
    }
    private void clear(PageSourcePool[] pools) {
        for(int i=0;i<pools.length;i++) {
        	pools[i].clear();
        }
    }

    /*private void doLogMemoryUsage(ConfigWeb config) {
		if(config.logMemoryUsage()&& config.getMemoryLogger()!=null)
			config.getMemoryLogger().write();
	}*/
    
    
    static class ExpiresFilter implements ResourceFilter {

		private long time;
		private boolean allowDir;

		public ExpiresFilter(long time, boolean allowDir) {
			this.allowDir=allowDir;
			this.time=time;
		}

		@Override
		public boolean accept(Resource res) {

			if(res.isDirectory()) return allowDir;
			
			// load content
			String str=null;
			try {
				str = IOUtil.toString(res,"UTF-8");
			} 
			catch (IOException e) {
				return false;
			}
			
			int index=str.indexOf(':');
			if(index!=-1){
				long expires=Caster.toLongValue(str.substring(0,index),-1L);
				// check is for backward compatibility, old files have no expires date inside. they do ot expire
				if(expires!=-1) {
					if(expires<System.currentTimeMillis()){
						return true;
					}
					str=str.substring(index+1);
					return false;
				}
			}
			// old files not having a timestamp inside
			else if(res.lastModified()<=time) {
				return true;
				
			}
			return false;
		}
    	
    }
}