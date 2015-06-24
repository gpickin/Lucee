/**
 * Copyright (c) 2015, Lucee Assosication Switzerland. All rights reserved.
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
package lucee.transformer.bytecode;

import lucee.runtime.config.Config;
import lucee.runtime.engine.ThreadLocalPageContext;
import lucee.runtime.op.Caster;
import lucee.runtime.type.util.KeyConstants;
import lucee.transformer.Context;
import lucee.transformer.Factory;
import lucee.transformer.Position;
import lucee.transformer.TransformerException;
import lucee.transformer.bytecode.cast.CastBoolean;
import lucee.transformer.bytecode.cast.CastDouble;
import lucee.transformer.bytecode.cast.CastInt;
import lucee.transformer.bytecode.cast.CastString;
import lucee.transformer.bytecode.expression.var.DataMemberImpl;
import lucee.transformer.bytecode.expression.var.VariableImpl;
import lucee.transformer.bytecode.literal.LitBooleanImpl;
import lucee.transformer.bytecode.literal.LitDoubleImpl;
import lucee.transformer.bytecode.literal.LitFloatImpl;
import lucee.transformer.bytecode.literal.LitIntegerImpl;
import lucee.transformer.bytecode.literal.LitLongImpl;
import lucee.transformer.bytecode.literal.LitStringImpl;
import lucee.transformer.bytecode.literal.Null;
import lucee.transformer.bytecode.op.OpBool;
import lucee.transformer.bytecode.op.OpString;
import lucee.transformer.bytecode.util.Types;
import lucee.transformer.expression.ExprBoolean;
import lucee.transformer.expression.ExprDouble;
import lucee.transformer.expression.ExprInt;
import lucee.transformer.expression.ExprString;
import lucee.transformer.expression.Expression;
import lucee.transformer.expression.literal.LitBoolean;
import lucee.transformer.expression.literal.LitDouble;
import lucee.transformer.expression.literal.LitFloat;
import lucee.transformer.expression.literal.LitInteger;
import lucee.transformer.expression.literal.LitLong;
import lucee.transformer.expression.literal.LitString;
import lucee.transformer.expression.literal.Literal;
import lucee.transformer.expression.var.DataMember;
import lucee.transformer.expression.var.Variable;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

public class BytecodeFactory extends Factory {
	private final static Method INIT= new Method("init",
			Types.COLLECTION_KEY,
			new Type[]{Types.STRING});



	private static final Type KEY_CONSTANTS = Type.getType(KeyConstants.class);
	
	private static BytecodeFactory instance;
	
	public static Factory getInstance(Config config) {
		if(instance==null)
			instance=new BytecodeFactory(config==null?ThreadLocalPageContext.getConfig():config);
		return instance;
	}

	private final  LitBoolean TRUE;
	private final LitBoolean FALSE;
	private final LitString EMPTY;
	private final LitString NULL;
	private final LitDouble DOUBLE_ZERO;
	private final LitDouble DOUBLE_ONE;



	private final Config config;
	
	public BytecodeFactory(Config config){
		TRUE=createLitBoolean(true);
		FALSE=createLitBoolean(false);
		EMPTY=createLitString("");
		NULL=createLitString("NULL");
		DOUBLE_ZERO=createLitDouble(0);
		DOUBLE_ONE=createLitDouble(1);
		this.config=config;
	}

	@Override
	public LitString createLitString(String str) {
		return new LitStringImpl(this,str,null,null);
	}

	@Override
	public LitString createLitString(String str, Position start, Position end) {
		return new LitStringImpl(this,str,start,end);
	}

	@Override
	public LitBoolean createLitBoolean(boolean b) {
		return new LitBooleanImpl(this, b, null, null);
	}

	@Override
	public LitBoolean createLitBoolean(boolean b, Position start, Position end) {
		return new LitBooleanImpl(this, b, start, end);
	}

	@Override
	public LitDouble createLitDouble(double d) {
		return new LitDoubleImpl(this, d, null, null);
	}

	@Override
	public LitDouble createLitDouble(double d, Position start, Position end) {
		return new LitDoubleImpl(this, d, start, end);
	}

	@Override
	public LitFloat createLitFloat(float f) {
		return new LitFloatImpl(this, f, null, null);
	}

	@Override
	public LitFloat createLitFloat(float f, Position start, Position end) {
		return new LitFloatImpl(this, f, start, end);
	}

	@Override
	public LitLong createLitLong(long l) {
		return new LitLongImpl(this, l, null, null);
	}

	@Override
	public LitLong createLitLong(long l, Position start, Position end) {
		return new LitLongImpl(this, l, start, end);
	}

	@Override
	public LitInteger createLitInteger(int i) {
		return new LitIntegerImpl(this, i, null, null);
	}

	@Override
	public LitInteger createLitInteger(int i, Position start, Position end) {
		return new LitIntegerImpl(this, i, start, end);
	}

	@Override
	public boolean isNull(Expression e) {
		return e instanceof Null;
	}

	@Override
	public Expression createNull() {
		return new Null(this,null,null);
	}

	@Override
	public Expression createNull(Position start, Position end) {
		return new Null(this,start,end);
	}

	@Override
	public DataMember createDataMember(ExprString name) {
		return new DataMemberImpl(name);
	}

	@Override
	public Literal createLiteral(Object obj,Literal defaultValue) {
		if(obj instanceof Boolean) return createLitBoolean(((Boolean)obj).booleanValue());
		if(obj instanceof Number) {
			if(obj instanceof Float)return createLitFloat(((Float)obj).floatValue());
			else if(obj instanceof Integer)return createLitInteger(((Integer)obj).intValue());
			else if(obj instanceof Long)return createLitLong(((Long)obj).longValue());
			else return createLitDouble(((Number)obj).doubleValue());
		}
		String str = Caster.toString(obj,null);
		if(str!=null) return createLitString(str);
		return defaultValue;
	}

	@Override
	public LitBoolean TRUE() {
		return TRUE;
	}

	@Override
	public LitBoolean FALSE() {
		return FALSE;
	}

	@Override
	public LitString EMPTY() {
		return EMPTY;
	}

	@Override
	public LitDouble DOUBLE_ZERO() {
		return DOUBLE_ZERO;
	}

	@Override
	public LitDouble DOUBLE_ONE() {
		return DOUBLE_ONE;
	}

	@Override
	public LitString NULL() {
		return NULL;
	}

	@Override
	public ExprDouble toExprDouble(Expression expr) {
		return CastDouble.toExprDouble(expr);
	}

	@Override
	public ExprString toExprString(Expression expr) {
		return CastString.toExprString(expr);
	}

	@Override
	public ExprBoolean toExprBoolean(Expression expr) {
		return CastBoolean.toExprBoolean(expr);
	}

	@Override
	public ExprInt toExprInt(Expression expr) {
		return CastInt.toExprInt(expr);
	}

	@Override
	public Variable createVariable(Position start, Position end) {
		return new VariableImpl(this, start, end);
	}

	@Override
	public Variable createVariable(int scope,Position start, Position end) {
		return new VariableImpl(this, scope, start, end);
	}
	

	@Override
	public ExprString opString(Expression left,Expression right){
		return OpString.toExprString(left, right,true);
	}

	@Override
	public ExprString opString(Expression left, Expression right, boolean concatStatic) {
		return OpString.toExprString(left, right, concatStatic);
	}

	@Override
	public ExprBoolean opBool(Expression left,Expression right,int operation){
		return OpBool.toExprBoolean(left, right,operation);
	}

	@Override
	public void registerKey(Context c,Expression name,boolean doUpperCase) throws TransformerException {
		BytecodeContext bc=(BytecodeContext) c;
		if(name instanceof Literal) {
			Literal l=(Literal) name;
			
			LitString ls = name instanceof LitString?(LitString)l:c.getFactory().createLitString(l.getString());
			if(doUpperCase){
				ls=ls.duplicate();
				ls.upperCase();
			}
			String key=KeyConstants.getFieldName(ls.getString());
			if(key!=null){
				bc.getAdapter().getStatic(KEY_CONSTANTS, key, Types.COLLECTION_KEY);
				return;
			}
			int index=bc.registerKey(ls);
			bc.getAdapter().visitVarInsn(Opcodes.ALOAD, 0);
			bc.getAdapter().visitFieldInsn(Opcodes.GETFIELD,  bc.getClassName(), "keys", Types.COLLECTION_KEY_ARRAY.toString());
			bc.getAdapter().push(index);
			bc.getAdapter().visitInsn(Opcodes.AALOAD);
			
			
			//ExpressionUtil.writeOutSilent(lit,bc, Expression.MODE_REF);
			//bc.getAdapter().invokeStatic(Page.KEY_IMPL, Page.KEY_INTERN);
			
			return;
		}
		name.writeOut(bc, Expression.MODE_REF);
		bc.getAdapter().invokeStatic(Page.KEY_IMPL, INIT);
		//bc.getAdapter().invokeStatic(Types.CASTER, TO_KEY);
		return;
	}

	@Override
	public Config getConfig() {
		return config;
	}
}