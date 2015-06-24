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
package lucee.runtime.functions.xml;

import lucee.commons.lang.StringUtil;
import lucee.runtime.PageContext;
import lucee.runtime.exp.PageException;
import lucee.runtime.ext.function.Function;
import lucee.runtime.op.Caster;
import lucee.runtime.text.xml.XMLUtil;
import lucee.runtime.type.Struct;

import org.xml.sax.InputSource;

/**
 * 
 */
public final class XmlValidate implements Function {

	public static Struct call(PageContext pc, String strXml) throws PageException {
		return call(pc,strXml,null);
	}
	public static Struct call(PageContext pc, String strXml, String strValidator) throws PageException {
		strXml=strXml.trim();
		try {
			InputSource xml = XMLUtil.toInputSource(pc,strXml);
			InputSource validator = StringUtil.isEmpty(strValidator)?null:XMLUtil.toInputSource(pc,strValidator);
			return XMLUtil.validate(xml, validator,strValidator);
		} 
		catch (Exception e) {
			throw Caster.toPageException(e);
		}
		
	}
}