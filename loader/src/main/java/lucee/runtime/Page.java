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
package lucee.runtime;

import java.io.Serializable;
import java.lang.ref.SoftReference;

import lucee.runtime.component.ImportDefintion;
import lucee.runtime.type.Struct;
import lucee.runtime.type.UDF;
import lucee.runtime.type.UDFProperties;

/**
 * abstract Method for all generated Page Object
 */
public abstract class Page implements Serializable {

	private static final long serialVersionUID = 7844636300784565040L;

	private static final ImportDefintion[] NO_IMPORTS = new ImportDefintion[0];
	private static final CIPage[] NO_SUB_PAGES = new CIPage[0];

	public static boolean FALSE = false;
	public static boolean TRUE = true;

	private PageSource pageSource;
	private byte loadType;

	/**
	 * return version definition of the page
	 * 
	 * @return version
	 */
	public long getVersion() {
		return -1;
	}

	/**
	 * method to invoke a page
	 * 
	 * @param pc PageContext
	 * @throws Throwable
	 */
	public Object call(PageContext pc) throws Throwable {
		return null;
	}

	/**
	 * return when the source file last time was modified
	 * 
	 * @return last modification of source file
	 */
	public long getSourceLastModified() {
		return 0;
	}

	/**
	 * return the time when the file was compiled
	 */
	public long getCompileTime() {
		return 0;
	}

	/**
	 * @param pageSource
	 */
	public void setPageSource(PageSource pageSource) {
		this.pageSource = pageSource;
	}

	/**
	 * @return Returns the pageResource.
	 */
	public PageSource getPageSource() {
		return pageSource;
	}

	/**
	 * @return gets the load type
	 */
	public byte getLoadType() {
		return loadType;
	}

	/**
	 * @param loadType sets the load type
	 */
	public void setLoadType(byte loadType) {
		this.loadType = loadType;
	}

	public Object udfCall(PageContext pageContext, UDF udf, int functionIndex)
			throws Throwable {
		return null;
	}

	public void threadCall(PageContext pageContext, int threadIndex)
			throws Throwable {
	}

	public Object udfDefaultValue(PageContext pc, int functionIndex,
			int argumentIndex, Object defaultValue) {
		return null;
	}

	public ImportDefintion[] getImportDefintions() {
		return NO_IMPORTS;
	}

	public CIPage[] getSubPages() {
		return NO_SUB_PAGES;
	}

	public SoftReference<Struct> metaData;

	public UDFProperties[] udfs;
}