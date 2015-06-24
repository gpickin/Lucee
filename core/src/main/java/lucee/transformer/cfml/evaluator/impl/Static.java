/**
 *
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
 **/
package lucee.transformer.cfml.evaluator.impl;

import java.util.Iterator;
import java.util.List;

import lucee.transformer.bytecode.Body;
import lucee.transformer.bytecode.Statement;
import lucee.transformer.bytecode.StaticBody;
import lucee.transformer.bytecode.statement.tag.Tag;
import lucee.transformer.bytecode.statement.tag.TagComponent;
import lucee.transformer.bytecode.util.ASMUtil;
import lucee.transformer.cfml.evaluator.EvaluatorException;
import lucee.transformer.cfml.evaluator.EvaluatorSupport;
import lucee.transformer.library.tag.TagLibTag;



/**
 * Prueft den Kontext des Tag case.
 * Das Tag <code>httpparam</code> darf nur innerhalb des Tag <code>http</code> liegen.
 */
public final class Static extends EvaluatorSupport {

	@Override
	public void evaluate(Tag tag,TagLibTag libTag) throws EvaluatorException { 
	
	// check parent
		Body body=null;
		
		String compName=Property.getComponentName(tag);
		
		boolean isCompChild=false;
		Tag p = ASMUtil.getParentTag(tag);

		if(p!=null && (p instanceof TagComponent || p.getFullname().equalsIgnoreCase(compName))) {
			isCompChild=true;
			body=p.getBody();
		}
		 
		Tag pp = p!=null?ASMUtil.getParentTag(p):null;
		if(!isCompChild && pp!=null && (p instanceof TagComponent || pp.getFullname().equalsIgnoreCase(compName))) {
			isCompChild=true;
			body=pp.getBody();
		}
		
		
		
		if(!isCompChild) {
			throw new EvaluatorException("Wrong Context for the the static constructor, "
					+ "a static constructor must inside a component body.");
		}
		
		//Body body=(Body) tag.getParent();
		List<Statement> children = tag.getBody().getStatements();
		
		// remove that tag from parent
		ASMUtil.remove(tag);
		
		
		StaticBody sb=getScriptBody(body);
		ASMUtil.addStatements(sb,children);
		body.addStatement(sb);
		
		
	}

	static StaticBody getScriptBody(Body body) {
		Iterator<Statement> it = body.getStatements().iterator();
		Statement s;
		while(it.hasNext()){
			s=it.next();
			if(s instanceof StaticBody) return (StaticBody) s;
		}
		
		return new StaticBody(body.getFactory());
	}

}