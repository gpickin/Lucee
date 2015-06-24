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
package lucee.runtime.db;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TimeZone;

import lucee.commons.io.log.Log;
import lucee.commons.lang.ClassException;
import lucee.commons.lang.StringUtil;
import lucee.runtime.config.ConfigWebUtil;
import lucee.runtime.op.Caster;
import lucee.runtime.type.Collection.Key;
import lucee.runtime.type.KeyImpl;
import lucee.runtime.type.Struct;
import lucee.runtime.type.util.CollectionUtil;

import org.osgi.framework.BundleException;


/**
 * 
 */
public final class DataSourceImpl  extends DataSourceSupport {

    private String connStr;
    private String host;
    private String database;
    private int port;
    private String connStrTranslated;
    private Struct custom;
	private boolean validate;
	private String dbdriver;
    
	/**
	 * constructor of the class
	 * @param name 
	 * @param className
	 * @param host 
	 * @param dsn
	 * @param database 
	 * @param port 
	 * @param username
	 * @param password
	 * @param connectionLimit 
	 * @param connectionTimeout 
	 * @param blob 
	 * @param clob 
	 * @param allow 
	 * @param custom 
	 * @param readOnly 
	 * @throws ClassNotFoundException 
	 * @throws ClassException 
	 * @throws BundleException 
	 * @throws SQLException 
	 */
   

	public DataSourceImpl(JDBCDriver driver,String name, ClassDefinition cd, String host, String connStr, String database, int port, String username, String password,
            int connectionLimit, int connectionTimeout, long metaCacheTimeout, boolean blob, boolean clob, int allow, Struct custom, boolean readOnly,
            boolean validate, boolean storage, TimeZone timezone, String dbdriver,Log log) throws BundleException, ClassException, SQLException {

		super(driver,name, cd,username,ConfigWebUtil.decrypt(password),blob,clob,connectionLimit, connectionTimeout, metaCacheTimeout, timezone, allow<0?ALLOW_ALL:allow, storage, readOnly,log);
			
        this.host=host;
        this.database=database;
        this.connStr=connStr; 
        this.port=port;

        this.custom=custom;
        this.validate=validate;
        
        this.connStrTranslated=connStr; 
        translateConnStr();

		this.dbdriver = dbdriver;
        
        //	throw new DatabaseException("can't find class ["+classname+"] for jdbc driver, check if driver (jar file) is inside lib folder",e.getMessage(),null,null,null);
        
	}
    private void translateConnStr() {
        connStrTranslated=replace(connStrTranslated,"host",host,false);
        connStrTranslated=replace(connStrTranslated,"database",database,false);
        connStrTranslated=replace(connStrTranslated,"port",Caster.toString(port),false);
        connStrTranslated=replace(connStrTranslated,"username",getUsername(),false);
        connStrTranslated=replace(connStrTranslated,"password",getPassword(),false);
        
        //Collection.Key[] keys = custom==null?new Collection.Key[0]:custom.keys();
        if(custom!=null) {
        	Iterator<Entry<Key, Object>> it = custom.entryIterator();
        	Entry<Key, Object> e;
            while(it.hasNext()) {
	        	e = it.next();
	            connStrTranslated=replace(connStrTranslated,e.getKey().getString(),Caster.toString(e.getValue(),""),true);
	        }
        }
    }

    private String replace(String src, String name, String value,boolean doQueryString) {
        if(StringUtil.indexOfIgnoreCase(src,"{"+name+"}")!=-1) {
            return StringUtil.replace(connStrTranslated,"{"+name+"}",value,false);
        }
        if(!doQueryString) return src;
        if(getClassDefinition().getClassName().indexOf("microsoft")!=-1 || getClassDefinition().getClassName().indexOf("jtds")!=-1)
        	return src+=';'+name+'='+value;
        return src+=((src.indexOf('?')!=-1)?'&':'?')+name+'='+value;
    }

    @Override
    public String getDsnOriginal() {
        return getConnectionString();
    }

    @Override
    public String getConnectionString() {
        return connStr;
    }
    
    @Override
    public String getDsnTranslated() {
        return getConnectionStringTranslated();
    }
    
    @Override
    public String getConnectionStringTranslated() {
        return connStrTranslated;
    }

    @Override
    public String getDatabase() {
        return database;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public String getHost() {
        return host;
    }
    
    @Override
    public Object clone() {
        return _clone(isReadOnly());
    }

    @Override
    public DataSource cloneReadOnly() {
        return _clone(true);
    }
    
    public DataSource _clone(boolean readOnly) {
    	try {
            return new DataSourceImpl(jdbc,getName(),getClassDefinition(), host, connStr, database, port, getUsername(), getPassword(), getConnectionLimit(), getConnectionTimeout(),getMetaCacheTimeout(), isBlob(), isClob(), allow, custom, readOnly, validate, isStorage(),getTimeZone(), dbdriver,getLog());
		} catch (RuntimeException re) {
			throw re; // this should never happens, because the class was already loaded in this object
		} catch (Exception e) {
			throw new RuntimeException(e); // this should never happens, because the class was already loaded in this object
		}
    }

    @Override
    public String getCustomValue(String key) {
        return Caster.toString(custom.get(KeyImpl.init(key),null),"");
    }
    
    @Override
    public String[] getCustomNames() {
        return CollectionUtil.keysAsString(custom);
    }
    
    @Override
    public Struct getCustoms() {
        return (Struct)custom.clone();
    }

    @Override
    public boolean validate() {
		return validate;
	}

	public String getDbDriver() {
		return dbdriver;
	}
}