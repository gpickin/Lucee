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
 * Implements the CFML Function arraydeleteat
 */
package lucee.runtime.functions.arrays;

import lucee.runtime.PageContext;
import lucee.runtime.exp.PageException;
import lucee.runtime.ext.function.BIF;
import lucee.runtime.op.Caster;
import lucee.runtime.type.Array;


public final class ArrayDelete extends BIF {

	private static final long serialVersionUID = 1120923916196967210L;
	
	public static boolean call(PageContext pc , Array array, Object value) throws PageException {
		return call(pc, array, value,null);
	}
	public static boolean call(PageContext pc , Array array, Object value, String scope) throws PageException {
		boolean onlyFirst=!"all".equalsIgnoreCase(scope);
		double pos;
		if((pos=ArrayFindNoCase.call(pc, array, value))>0){
			array.removeE((int)pos);
			if(onlyFirst) return true;
		}
		else return false;
		
		while((pos=ArrayFindNoCase.call(pc, array, value))>0){
			array.removeE((int)pos);
		}
		
		return true;
	}
	
	@Override
	public Object invoke(PageContext pc, Object[] args) throws PageException {
		if(args.length==2)return call(pc,Caster.toArray(args[0]),args[1]);
		return call(pc,Caster.toArray(args[0]),args[1],Caster.toString(args[2]));
	}
	
}