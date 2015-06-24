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
package lucee.runtime.type.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import lucee.commons.lang.StringUtil;
import lucee.runtime.Component;
import lucee.runtime.PageContext;
import lucee.runtime.dump.DumpData;
import lucee.runtime.dump.DumpProperties;
import lucee.runtime.dump.DumpRow;
import lucee.runtime.dump.DumpTable;
import lucee.runtime.dump.SimpleDumpData;
import lucee.runtime.exp.ApplicationException;
import lucee.runtime.exp.ExpressionException;
import lucee.runtime.exp.PageException;
import lucee.runtime.exp.PageExceptionImpl;
import lucee.runtime.op.Caster;
import lucee.runtime.type.Collection;
import lucee.runtime.type.Collection.Key;
import lucee.runtime.type.FunctionArgument;
import lucee.runtime.type.KeyImpl;
import lucee.runtime.type.Struct;
import lucee.runtime.type.UDF;
import lucee.runtime.type.UDFGSProperty;
import lucee.runtime.type.scope.Argument;
import lucee.runtime.type.scope.ArgumentIntKey;
import lucee.transformer.library.function.FunctionLibFunction;
import lucee.transformer.library.function.FunctionLibFunctionArg;

public class UDFUtil {

	public static final short TYPE_UDF=1;
	public static final short TYPE_BIF=2;
	public static final short TYPE_CLOSURE=4;
	public static final short TYPE_LAMBDA=8;

	private static final char CACHE_DEL = ';';
	private static final char CACHE_DEL2 = ':';

	private static final FunctionArgument[] EMPTY = new FunctionArgument[0];
	
	/**
	 * add detailed function documentation to the exception
	 * @param pe
	 * @param flf
	 */
	public static void addFunctionDoc(PageExceptionImpl pe,FunctionLibFunction flf) {
		ArrayList<FunctionLibFunctionArg> args=flf.getArg();
		Iterator<FunctionLibFunctionArg> it = args.iterator();
		
		// Pattern
		StringBuilder pattern=new StringBuilder(flf.getName());
		StringBuilder end=new StringBuilder();
		pattern.append("(");
		FunctionLibFunctionArg arg;
		int c=0;
		while(it.hasNext()){
			arg = it.next();
			if(!arg.isRequired()) {
				pattern.append(" [");
				end.append("]");
			}
			if(c++>0)pattern.append(", ");
			pattern.append(arg.getName());
			pattern.append(":");
			pattern.append(arg.getTypeAsString());
			
		}
		pattern.append(end);
		pattern.append("):");
		pattern.append(flf.getReturnTypeAsString());
		
		pe.setAdditional(KeyConstants._Pattern, pattern);
		
		// Documentation
		StringBuilder doc=new StringBuilder(flf.getDescription());
		StringBuilder req=new StringBuilder();
		StringBuilder opt=new StringBuilder();
		StringBuilder tmp;
		doc.append("\n");
		
		it = args.iterator();
		while(it.hasNext()){
			arg = it.next();
			tmp=arg.isRequired()?req:opt;
			
			tmp.append("- ");
			tmp.append(arg.getName());
			tmp.append(" (");
			tmp.append(arg.getTypeAsString());
			tmp.append("): ");
			tmp.append(arg.getDescription());
			tmp.append("\n");
		}

		if(req.length()>0)doc.append("\nRequired:\n").append(req);
		if(opt.length()>0)doc.append("\nOptional:\n").append(opt);
		
		
		pe.setAdditional(KeyImpl.init("Documentation"), doc);
		
	}

	public static void argumentCollection(Struct values) {
		argumentCollection(values,EMPTY);
	}

	public static void argumentCollection(Struct values, FunctionArgument[] funcArgs) {
		Object value=values.removeEL(KeyConstants._argumentCollection);
		if(value !=null) {
			value=Caster.unwrap(value,value);
			
			if(value instanceof Argument) {
				Argument argColl=(Argument) value;
				Iterator<Key> it = argColl.keyIterator();
				Key k;
				int i=-1;
			    while(it.hasNext()) {
			    	i++;
			    	k = it.next();
			    	if(funcArgs.length>i && k instanceof ArgumentIntKey) {
	            		if(!values.containsKey(funcArgs[i].getName()))
	            			values.setEL(funcArgs[i].getName(),argColl.get(k,Argument.NULL));
	            		else 
	            			values.setEL(k,argColl.get(k,Argument.NULL));
			    	}
	            	else if(!values.containsKey(k)){
	            		values.setEL(k,argColl.get(k,Argument.NULL));
	            	}
	            }
		    }
			else if(value instanceof Collection) {
		        Collection argColl=(Collection) value;
			    //Collection.Key[] keys = argColl.keys();
				Iterator<Key> it = argColl.keyIterator();
				Key k;
				while(it.hasNext()) {
			    	k = it.next();
			    	if(!values.containsKey(k)){
	            		values.setEL(k,argColl.get(k,Argument.NULL));
	            	}
	            }
		    }
			else if(value instanceof Map) {
				Map map=(Map) value;
			    Iterator it = map.entrySet().iterator();
			    Map.Entry entry;
			    Key key;
			    while(it.hasNext()) {
			    	entry=(Entry) it.next();
			    	key = Caster.toKey(entry.getKey(),null);
			    	if(!values.containsKey(key)){
	            		values.setEL(key,entry.getValue());
	            	}
	            }
		    }
			else if(value instanceof java.util.List) {
				java.util.List list=(java.util.List) value;
			    Iterator it = list.iterator();
			    Object v;
			    int index=0;
			    Key k;
			    while(it.hasNext()) {
			    	v= it.next();
			    	k=ArgumentIntKey.init(++index);
			    	if(!values.containsKey(k)){
	            		values.setEL(k,v);
	            	}
	            }
		    }
		    else {
		        values.setEL(KeyConstants._argumentCollection,value);
		    }
		} 
	}
	
	public static String toReturnFormat(int returnFormat,String defaultValue) {
		if(UDF.RETURN_FORMAT_WDDX==returnFormat)		return "wddx";
		else if(UDF.RETURN_FORMAT_JSON==returnFormat)	return "json";
		else if(UDF.RETURN_FORMAT_PLAIN==returnFormat)	return "plain";
		else if(UDF.RETURN_FORMAT_SERIALIZE==returnFormat)	return "cfml";
		else if(UDF.RETURN_FORMAT_JAVA==returnFormat)	return "java";
		// NO XML else if(UDFPlus.RETURN_FORMAT_XML==returnFormat)	return "xml";
		else return defaultValue;
	}
	

	public static boolean isValidReturnFormat(int returnFormat) {
		return toReturnFormat(returnFormat,null)!=null;
	}
	

	public static int toReturnFormat(String[] returnFormats, int defaultValue) {
		if(ArrayUtil.isEmpty(returnFormats)) return defaultValue;
		int rf;
		for(int i=0;i<returnFormats.length;i++){
			rf=toReturnFormat(returnFormats[i].trim(), -1);
			if(rf!=-1) return rf;
		}
		return defaultValue;
	}
	public static int toReturnFormat(String returnFormat, int defaultValue) {
		if(StringUtil.isEmpty(returnFormat,true)) return defaultValue;
		
		returnFormat=returnFormat.trim().toLowerCase();
		if("wddx".equals(returnFormat))				return UDF.RETURN_FORMAT_WDDX;
		else if("json".equals(returnFormat))		return UDF.RETURN_FORMAT_JSON;
		else if("plain".equals(returnFormat))		return UDF.RETURN_FORMAT_PLAIN;
		else if("text".equals(returnFormat))		return UDF.RETURN_FORMAT_PLAIN;
		else if("serialize".equals(returnFormat))	return UDF.RETURN_FORMAT_SERIALIZE;
		else if("cfml".equals(returnFormat))	return UDF.RETURN_FORMAT_SERIALIZE;
		else if("cfm".equals(returnFormat))	return UDF.RETURN_FORMAT_SERIALIZE;
		else if("xml".equals(returnFormat))	return UDF.RETURN_FORMAT_XML;
		else if("java".equals(returnFormat))	return UDF.RETURN_FORMAT_JAVA;
		return defaultValue;
	}
	

	public static int toReturnFormat(String returnFormat) throws ExpressionException {
		int rf = toReturnFormat(returnFormat,-1);
		if(rf!=-1) return rf;
		throw new ExpressionException("invalid returnFormat definition ["+returnFormat+"], valid values are [wddx,plain,json,cfml]");
	}

	public static String toReturnFormat(int returnFormat) throws ExpressionException {
		if(UDF.RETURN_FORMAT_WDDX==returnFormat)		return "wddx";
		else if(UDF.RETURN_FORMAT_JSON==returnFormat)	return "json";
		else if(UDF.RETURN_FORMAT_PLAIN==returnFormat)	return "plain";
		else if(UDF.RETURN_FORMAT_SERIALIZE==returnFormat)	return "cfml";
		else if(UDF.RETURN_FORMAT_JAVA==returnFormat)	return "java";
		else throw new ExpressionException("invalid returnFormat definition, valid values are [wddx,plain,json,cfml]");
	}
	

	public static DumpData toDumpData(PageContext pageContext, int maxlevel, DumpProperties dp,UDF udf, short type) {
	
		if(!dp.getShowUDFs()) {
			if(TYPE_UDF==type) return new SimpleDumpData("<UDF>");
			if(TYPE_BIF==type) return new SimpleDumpData("<BIF>");
			if(TYPE_CLOSURE==type) return new SimpleDumpData("<Closure>");
			if(TYPE_LAMBDA==type) return new SimpleDumpData("<Lambda>");
		}
		// arguments
		FunctionArgument[] args = udf.getFunctionArguments();
        
        DumpTable atts;
        if(TYPE_UDF==type) 			atts= new DumpTable("udf","#ca5095","#e9accc","#000000");
        else if(TYPE_CLOSURE==type) atts= new DumpTable("udf","#9cb770","#c7e1ba","#000000");
        else if(TYPE_BIF==type) 	atts= new DumpTable("udf","#e1c039","#f1e2a3","#000000");
        else						atts= new DumpTable("udf","#f3d5bd","#f6e4cc","#000000");
        
        
		atts.appendRow(new DumpRow(63,new DumpData[]{new SimpleDumpData("label"),new SimpleDumpData("name"),new SimpleDumpData("required"),new SimpleDumpData("type"),new SimpleDumpData("default"),new SimpleDumpData("hint")}));
		for(int i=0;i<args.length;i++) {
			FunctionArgument arg=args[i];
			DumpData def;
			try {
				Object oa = udf.getDefaultValue(pageContext, i,null);
                if(oa==null)oa="null";
				def=new SimpleDumpData(Caster.toString(oa));
			}
			catch (PageException e) {
				def=new SimpleDumpData("");
			}
			atts.appendRow(new DumpRow(0,new DumpData[]{
					new SimpleDumpData(arg.getDisplayName()),
					new SimpleDumpData(arg.getName().getString()),
					new SimpleDumpData(arg.isRequired()),
					new SimpleDumpData(arg.getTypeAsString()),
					def,
					new SimpleDumpData(arg.getHint())}));
			//atts.setRow(0,arg.getHint());
			
		}
		DumpTable func;
		String label = udf.getDisplayName();
		if(TYPE_CLOSURE==type) {
			func=new DumpTable("#9cb770","#c7e1ba","#000000");
			func.setTitle(StringUtil.isEmpty(label)?"Closure":"Closure "+label);
		}
		else if(TYPE_UDF==type) {
			func=new DumpTable("#ca5095","#e9accc","#000000");
			String f="Function ";
			try {
				f=StringUtil.ucFirst(ComponentUtil.toStringAccess(udf.getAccess()).toLowerCase())+" "+f;
			} 
			catch (ApplicationException e) {}
			f+=udf.getFunctionName();
			if(udf instanceof UDFGSProperty) f+=" (generated)";
			func.setTitle(f);
		}
		else if(TYPE_BIF==type) {
			String f="Build in Function "+(!StringUtil.isEmpty(label)?label: udf.getFunctionName());
			func=new DumpTable("#e1c039","#f1e2a3","#000000");
			func.setTitle(f);
		}
		else {
			func=new DumpTable("#f3d5bd","#f6e4cc","#000000");
			func.setTitle(StringUtil.isEmpty(label)?"Lambda":"Lambda "+label);
		}

		// Source
		String src = udf.getSource();
		if(!StringUtil.isEmpty(src)) func.setComment("source:"+src);
		

		String hint=udf.getHint();
		String desc=udf.getDescription();
		if(!StringUtil.isEmpty(desc))addComment(func,desc);
		if(!StringUtil.isEmpty(hint))addComment(func,hint);
		if(Component.MODIFIER_NONE!=udf.getModifier())
			func.appendRow(1,new SimpleDumpData("modifier"),new SimpleDumpData(ComponentUtil.toModifier(udf.getModifier(),"")));
		func.appendRow(1,new SimpleDumpData("arguments"),atts);
		func.appendRow(1,new SimpleDumpData("return type"),new SimpleDumpData(udf.getReturnTypeAsString()));
		
		return func;
	}
	
	private static void addComment(DumpTable dt,String comment) {
    	if(StringUtil.isEmpty(dt.getComment()) || dt.getComment().indexOf(comment)!=-1) dt.setComment(comment);
    	else dt.setComment(dt.getComment()+"\n"+comment);
    }


}