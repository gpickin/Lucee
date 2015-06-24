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
package lucee.runtime.type.scope;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import lucee.commons.collection.MapFactory;
import lucee.commons.io.IOUtil;
import lucee.commons.io.res.Resource;
import lucee.commons.lang.ByteNameValuePair;
import lucee.commons.lang.StringUtil;
import lucee.commons.net.URLItem;
import lucee.runtime.PageContext;
import lucee.runtime.PageContextImpl;
import lucee.runtime.config.ConfigImpl;
import lucee.runtime.exp.PageException;
import lucee.runtime.listener.ApplicationContext;
import lucee.runtime.net.http.ServletInputStreamDummy;
import lucee.runtime.op.Caster;
import lucee.runtime.type.Array;
import lucee.runtime.type.util.ArrayUtil;
import lucee.runtime.type.util.KeyConstants;
import lucee.runtime.type.util.ListUtil;

import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.servlet.ServletRequestContext;


/**
 * Form Scope
 */
public final class FormImpl extends ScopeSupport implements Form,ScriptProtected {
	

	private byte EQL=61;
	private byte NL=10;
	private byte AMP=38;
	
	
	private Map<String,Item> fileItems=MapFactory.<String,Item>getConcurrentMap();
	private Exception initException=null;

    private String encoding=null;
    private int scriptProtected=ScriptProtected.UNDEFINED;
    private static final URLItem[] empty=new URLItem[0];
	//private static final ResourceFilter FILTER = new ExtensionResourceFilter(".upload",false);
    private URLItem[] raw=empty;
    private static int count=1;

    private static final int HEADER_TEXT_PLAIN=0;
    private static final int HEADER_MULTIPART_FORM_DATA=1;
    private static final int HEADER_APP_URL_ENC=2;
	private int headerType=-1;
    
	/**
	 * standart class Constructor
	 */
	public FormImpl() {
		super(true,"form",SCOPE_FORM);
	}

    @Override
    public String getEncoding() {
        return encoding;
    }
    
    @Override
    public void setEncoding(ApplicationContext ac,String encoding) throws UnsupportedEncodingException {
        encoding=encoding.trim().toUpperCase();
        if(encoding.equals(this.encoding)) return;
        this.encoding = encoding;
        if(!isInitalized()) return;
        fillDecoded(raw,encoding,isScriptProtected(),ac.getSameFieldAsArray(Scope.SCOPE_FORM));
        setFieldNames();
    }

	@Override
	public void initialize(PageContext pc) {
		if(encoding==null)encoding=((PageContextImpl)pc).getWebCharset().name();
		
		if(scriptProtected==ScriptProtected.UNDEFINED) {
			scriptProtected=((pc.getApplicationContext().getScriptProtect()&ApplicationContext.SCRIPT_PROTECT_FORM)>0)?
					ScriptProtected.YES:ScriptProtected.NO;
		}
        super.initialize(pc);
		
        String contentType=pc. getHttpServletRequest().getContentType();
        
		if(contentType==null) return;
		contentType=StringUtil.toLowerCase(contentType);
		if(contentType.startsWith("multipart/form-data")) {
			headerType=HEADER_MULTIPART_FORM_DATA;
			initializeMultiPart(pc,isScriptProtected());
		}
		else if(contentType.startsWith("text/plain")) {
			headerType=HEADER_TEXT_PLAIN;
			initializeUrlEncodedOrTextPlain(pc,'\n',isScriptProtected());
		}
		else if(contentType.startsWith("application/x-www-form-urlencoded")) {
			headerType=HEADER_APP_URL_ENC;
			initializeUrlEncodedOrTextPlain(pc,'&',isScriptProtected());
		}
		setFieldNames();
	}

	@Override
	public void reinitialize(ApplicationContext ac) {
		if(isInitalized()) {
			
			if(scriptProtected==ScriptProtected.UNDEFINED) {
				scriptProtected=((ac.getScriptProtect()&ApplicationContext.SCRIPT_PROTECT_FORM)>0)?
						ScriptProtected.YES:ScriptProtected.NO;
			}	
			fillDecodedEL(raw,encoding,isScriptProtected(),ac.getSameFieldAsArray(SCOPE_FORM));
			setFieldNames();
		}
	}

    void setFieldNames() {
    	if(size()>0) {
    		setEL(KeyConstants._fieldnames,ListUtil.arrayToList(keys(), ","));
        }
    }


    private void initializeMultiPart(PageContext pc, boolean scriptProteced) {
    	// get temp directory
    	Resource tempDir = ((ConfigImpl)pc.getConfig()).getTempDirectory();
    	Resource tempFile;
    	
    	// Create a new file upload handler
    	final String encoding=getEncoding();
    	FileItemFactory factory = tempDir instanceof File? 
    			new DiskFileItemFactory(DiskFileItemFactory.DEFAULT_SIZE_THRESHOLD,(File)tempDir):
    				new DiskFileItemFactory();
    	
    	ServletFileUpload upload = new ServletFileUpload(factory);
    	upload.setHeaderEncoding(encoding);
    	//ServletRequestContext c = new ServletRequestContext(pc.getHttpServletRequest());
    	
    	
    	HttpServletRequest req = pc.getHttpServletRequest();
    	ServletRequestContext context = new ServletRequestContext(req) {
    		@Override
			public String getCharacterEncoding() {
    			return encoding;
    		}
    	};
    	
    	// Parse the request
    	try {
    		FileItemIterator iter = upload.getItemIterator(context);
        	//byte[] value;
        	InputStream is;
        	ArrayList<URLItem> list=new ArrayList<URLItem>();
			while (iter.hasNext()) {
			    FileItemStream item = iter.next();

			    is=IOUtil.toBufferedInputStream(item.openStream());
			    if (item.getContentType()==null || StringUtil.isEmpty(item.getName())) {
			    	list.add(new URLItem(item.getFieldName(),new String(IOUtil.toBytes(is),encoding),false));	     
			    } 
			    else {
			    	tempFile=tempDir.getRealResource(getFileName());
			    	fileItems.put(item.getFieldName().toLowerCase(),
			    			new Item(tempFile,item.getContentType(),item.getName(),item.getFieldName()));
					String value=tempFile.toString();
			    	IOUtil.copy(is, tempFile,true);
			    	
				    list.add(new URLItem(item.getFieldName(),value,false));	     
			    }       
			}
			
			raw= list.toArray(new URLItem[list.size()]);
			fillDecoded(raw,encoding,scriptProteced,pc.getApplicationContext().getSameFieldAsArray(SCOPE_FORM));
		} 
    	catch (Exception e) {
			
        	//throw new PageRuntimeException(Caster.toPageException(e));
        	fillDecodedEL(new URLItem[0],encoding,scriptProteced,pc.getApplicationContext().getSameFieldAsArray(SCOPE_FORM));
			initException=e;
		}
	}
    
    private static String getFileName() { 
		return "tmp-"+(count++)+".upload";
	}

	/*private void initializeMultiPart(PageContext pc, boolean scriptProteced) {

    	File tempDir=FileWrapper.toFile(pc.getConfig().getTempDirectory());
    	
    	// Create a factory for disk-based file items
    	DiskFileItemFactory factory = new DiskFileItemFactory(-1,tempDir);

    	// Create a new file upload handler
    	ServletFileUpload upload = new ServletFileUpload(factory);
    	
    	upload.setHeaderEncoding(getEncoding());
    	
		//FileUpload fileUpload=new FileUpload(new DiskFileItemFactory(0,tempDir));
		java.util.List list;
		try {
			list = upload.parseRequest(pc.getHttpServletRequest());
            raw=new ByteNameValuePair[list.size()];
                        
			for(int i=0;i<raw.length;i++) {
				DiskFileItem val=(DiskFileItem) list.get(i);
				if(val.isFormField()) {
                    raw[i]=new ByteNameValuePair(getBytes(val.getFieldName()),val.get(),false);
                }
				else {
					print.out("-------------------------------");
					print.out("fieldname:"+val.getFieldName());
					print.out("name:"+val.getName());
					print.out("formfield:"+val.isFormField());
					print.out("memory:"+val.isInMemory());
					print.out("exist:"+val.getStoreLocation().getCanonicalFile().exists());
					
					fileItems.put(val.getFieldName().toLowerCase(),val);
					
                    raw[i]=new ByteNameValuePair(getBytes(val.getFieldName()),val.getStoreLocation().getCanonicalFile().toString().getBytes(),false);
                    //raw.put(val.getFieldName(),val.getStoreLocation().getCanonicalFile().toString());
				}
			}
            fillDecoded(raw,encoding,scriptProteced);
		} 
        catch (Exception e) {
        	
        	//throw new PageRuntimeException(Caster.toPageException(e));
        	fillDecodedEL(new ByteNameValuePair[0],encoding,scriptProteced);
			initException=e;
		}
	}*/
    
	private void initializeUrlEncodedOrTextPlain(PageContext pc, char delimiter, boolean scriptProteced) {
		BufferedReader reader=null;
		try {
			reader = pc.getHttpServletRequest().getReader();
			raw=setFrom___(IOUtil.toString(reader,false),delimiter);
			fillDecoded(raw,encoding,scriptProteced,pc.getApplicationContext().getSameFieldAsArray(SCOPE_FORM));
		} 
        catch (Exception e) {
        	
        	fillDecodedEL(new URLItem[0],encoding,scriptProteced,pc.getApplicationContext().getSameFieldAsArray(SCOPE_FORM));
			initException=e;
        }
        finally {
        	IOUtil.closeEL(reader);
        }
	}

	@Override
	public void release(PageContext pc) {
		super.release(pc);
        encoding=null;
        scriptProtected=ScriptProtected.UNDEFINED;
        raw=empty;
		
		if(!fileItems.isEmpty()) {
			Iterator<Entry<String, Item>> it = fileItems.entrySet().iterator();
			Item item;
			while(it.hasNext()) {
				item=it.next().getValue();
				item.getResource().delete();
			}
			fileItems.clear();
		}
		initException=null;
		
	}

	@Override
	public FormItem[] getFileItems() {
		if(fileItems==null || fileItems.isEmpty()) return new FormImpl.Item[0];
		
		Iterator<Entry<String, Item>> it = fileItems.entrySet().iterator();
		Map.Entry<String, Item> entry;
		FormImpl.Item[] rtn=new FormImpl.Item[fileItems.size()];
		int index=0;
		while(it.hasNext()){
			entry=it.next();
			rtn[index++]=entry.getValue();
		}
		return rtn;
	}
	
	
	public DiskFileItem getFileUpload(String key) {
		return null;
	}
	
	@Override
	public FormItem getUploadResource(String key) {
		key=key.trim();
		String lcKey = StringUtil.toLowerCase(key);
		
		// x
		Item item = fileItems.get(lcKey);
		if(item!=null)return item;
		
		// form.x
		if(lcKey.startsWith("form.")) {
			lcKey=lcKey.substring(5).trim();
			item = fileItems.get(lcKey);
			if(item!=null)return item;
		}
		
		// form . x
		try {
			Array array = ListUtil.listToArray(lcKey, '.');
			if(array.size()>1 && array.getE(1).toString().trim().equals("form")) {
				array.removeE(1);
				lcKey=ListUtil.arrayToList(array, ".").trim();
				item = fileItems.get(lcKey);
				if(item!=null)return item;
			}
		} 
		catch (PageException e) {}
		
		// /file.tmp
		Iterator<Entry<String, Item>> it = fileItems.entrySet().iterator();
		while(it.hasNext()) {
			item=it.next().getValue();
			if(item.getResource().getAbsolutePath().equalsIgnoreCase(key))return item;
		}
		
		return null;
	}

	@Override
	public PageException getInitException() {
		if(initException!=null)
			return Caster.toPageException(initException);
		return null;
	}

	@Override
	public void setScriptProtecting(ApplicationContext ac,boolean scriptProtected) {
		int _scriptProtected = scriptProtected?ScriptProtected.YES:ScriptProtected.NO;
		if(isInitalized() && _scriptProtected!=this.scriptProtected) {
			fillDecodedEL(raw,encoding,scriptProtected,ac.getSameFieldAsArray(SCOPE_FORM));
			setFieldNames();
		}
		this.scriptProtected=_scriptProtected;
	}

	@Override
	public boolean isScriptProtected() {
		return scriptProtected==ScriptProtected.YES ;
	}

	/**
	 * @return the raw
	 */
	public URLItem[] getRaw() {
		return raw;
	}

	public void addRaw(ApplicationContext ac,URLItem[] raw) {
		URLItem[] nr=new URLItem[this.raw.length+raw.length];
		for(int i=0;i<this.raw.length;i++) {
			nr[i]=this.raw[i];
		}
		for(int i=0;i<raw.length;i++) {
			nr[this.raw.length+i]=raw[i];
		}
		this.raw=nr;
		
		if(!isInitalized()) return;
        fillDecodedEL(this.raw,encoding,isScriptProtected(),ac.getSameFieldAsArray(SCOPE_FORM));
        setFieldNames();
	}

	private class Item implements FormItem {
		Resource resource;
		String contentType;
		String name;
		private String fieldName;
		
		public Item(Resource resource, String contentType,String name, String fieldName) {
			this.fieldName = fieldName;
			this.name = name;
			this.resource = resource;
			this.contentType = contentType;
		}
		/**
		 * @return the resource
		 */
		@Override
		public Resource getResource() {
			return resource;
		}
		/**
		 * @return the contentType
		 */
		@Override
		public String getContentType() {
			return contentType;
		}
		/**
		 * @return the name
		 */
		@Override
		public String getName() {
			return name;
		}
		/**
		 * @return the fieldName
		 */
		@Override
		public String getFieldName() {
			return fieldName;
		}
	}

	/**
	 * @return return content as a http header input stream
	 */
	@Override
	public ServletInputStream getInputStream() {
		if(headerType==HEADER_APP_URL_ENC) {
			return new ServletInputStreamDummy(toBarr(raw,AMP));
		}
		else if(headerType==HEADER_TEXT_PLAIN) {
			return new ServletInputStreamDummy(toBarr(raw,NL));
		}
		/*else if(headerType==HEADER_MULTIPART_FORM_DATA) {
			return new FormImplInputStream(this);
			// TODO
		}*/
		return new ServletInputStreamDummy(new byte[]{});
	}

	private byte[] toBarr(URLItem[] items, byte del) {
		
		ByteNameValuePair[] raw=new ByteNameValuePair[items.length];
		for(int i=0;i<raw.length;i++) {
			try {
				raw[i]=new ByteNameValuePair(items[i].getName().getBytes("iso-8859-1"),items[i].getValue().getBytes("iso-8859-1"),items[i].isUrlEncoded());
			} catch (UnsupportedEncodingException e) {}
		}
		
		int size=0;
		if(!ArrayUtil.isEmpty(raw)){
			for(int i=0;i<raw.length;i++) {
				size+=raw[i].getName().length;
				size+=raw[i].getValue().length;
				size+=2;
			}
			size--;
		}
		byte[] barr = new byte[size],bname,bvalue;
		int count=0;
		
		for(int i=0;i<raw.length;i++) {
			bname=raw[i].getName();
			bvalue=raw[i].getValue();
			// name
			for(int y=0;y<bname.length;y++) {
				barr[count++]=bname[y];
			}
			barr[count++]=EQL;
			// value
			for(int y=0;y<bvalue.length;y++) {
				barr[count++]=bvalue[y];
			}
			if(i+1<raw.length)barr[count++]=del;
		}
		return barr;
	}

}