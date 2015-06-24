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
package lucee.runtime.query.caster;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.TimeZone;

public interface Cast {

	public static final Cast ARRAY=new ArrayCast();
	public static final Cast BIT=new BitCast();
	public static final Cast BLOB=new BlobCast();
	public static final Cast CLOB=new ClobCast();
	public static final Cast DATE=new DateCast();
	public static final Cast ORACLE_OPAQUE=new OracleOpaqueCast();
	public static final Cast OTHER=new OtherCast();
	public static final Cast TIME=new TimeCast();
	public static final Cast TIMESTAMP=new TimestampCast();
	public static final Cast BIGINT=new BigIntCast();
	
	public Object toCFType(TimeZone tz,int type,ResultSet rst, int columnIndex) throws SQLException, IOException;
}