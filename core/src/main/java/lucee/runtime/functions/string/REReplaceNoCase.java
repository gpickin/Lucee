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
/**
 * Implements the CFML Function rereplacenocase
 */
package lucee.runtime.functions.string;

import lucee.runtime.PageContext;
import lucee.runtime.exp.ExpressionException;
import lucee.runtime.exp.FunctionException;
import lucee.runtime.ext.function.Function;
import lucee.runtime.regex.Perl5Util;

import org.apache.oro.text.regex.MalformedPatternException;

public final class REReplaceNoCase implements Function {

	public static String call(PageContext pc , String string, String regExp, String replace) throws ExpressionException {
        try {
            return Perl5Util.replace(string,regExp,replace,false,false);
        } 
        catch (MalformedPatternException e) {
            throw new FunctionException(pc,"reReplaceNoCase",2,"regularExpression",e.getMessage());
        }
	}
    
	public static String call(PageContext pc , String string, String regExp, String replace, String scope) throws ExpressionException {
		try {
			if(scope.equalsIgnoreCase("all"))return Perl5Util.replace(string,regExp,replace,false,true);
			return Perl5Util.replace(string,regExp,replace,false,false);
		} 
		catch (MalformedPatternException e) {
			throw new FunctionException(pc,"reReplaceNoCase",2,"regularExpression",e.getMessage());
		}
	}
}