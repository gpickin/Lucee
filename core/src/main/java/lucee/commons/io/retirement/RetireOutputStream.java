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
package lucee.commons.io.retirement;

import java.io.IOException;
import java.io.OutputStream;

import lucee.commons.io.res.Resource;

public class RetireOutputStream extends OutputStream {
	
	private Resource res;
	private boolean append;
	private OutputStream os;
	private long lastAccess=0;
	private long retireRange;
	private RetireListener listener;

	/**
	 * 
	 * @param res
	 * @param append
	 * @param retireRange retire the stream after given time in minutes
	 */
	public RetireOutputStream(Resource res, boolean append, int retireRangeInSeconds, RetireListener listener){
		this.res=res;
		this.append=append;
		retireRange = retireRangeInSeconds>0?retireRangeInSeconds*1000:0;
		this.listener=listener;
	}

	private OutputStream getOutputStream() throws IOException {
		
		if(os==null) {
			//print.e("start "+res);
			os=res.getOutputStream(append);
			RetireOutputStreamFactory.list.add(this);
			RetireOutputStreamFactory.startThread(retireRange);
		}
		lastAccess=System.currentTimeMillis();
		return os;
	}
	
	public boolean retire() throws IOException{
		if(os==null || (lastAccess+retireRange)>System.currentTimeMillis()) {
			//print.e("not retire "+res);
			return false;
		}
		//print.e("retire "+res);
		append=true;
		close();
		
		return true;
	}
	private boolean retireNow() throws IOException{
		if(os==null)return false;
		append=true;
		close();
		return true;
	}

	@Override
	public void close() throws IOException {
		if(os!=null){
			if(listener!=null) listener.retire(this);
			try{
				os.close();
			}
			finally{
				RetireOutputStreamFactory.list.remove(this);
				os=null;
			}
		}
	}

	@Override
	public void flush() throws IOException {
		if(os!=null){
			getOutputStream().flush();
			if(retireRange==0) retireNow();
		}
	}
	
	@Override
	public void write(int b) throws IOException {
		//print.e("write:"+((char)b));
		getOutputStream().write(b);
		if(retireRange==0) retireNow();
	}

	@Override
	public void write(byte[] b) throws IOException {
		//print.e("write.barr:"+b.length);
		getOutputStream().write(b);
		if(retireRange==0) retireNow();
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		//print.e("write.barr:"+b.length+":"+off+":"+len);
		getOutputStream().write(b, off, len);
		if(retireRange==0) retireNow();
	}

}