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
/*
*

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package lucee.runtime.img.filter;import java.awt.image.BufferedImage;

import lucee.runtime.engine.ThreadLocalPageContext;
import lucee.runtime.exp.FunctionException;
import lucee.runtime.exp.PageException;
import lucee.runtime.img.ImageUtil;
import lucee.runtime.type.KeyImpl;
import lucee.runtime.type.Struct;
import lucee.runtime.type.util.CollectionUtil;



/**
 * A filter which simply multiplies pixel values by a given scale factor.
 */
public class RescaleFilter extends TransferFilter  implements DynFiltering {
	
	private float scale = 1.0f;
	
	public RescaleFilter() {
    }
    
	public RescaleFilter(float scale) {
		this.scale = scale;
    }
    
    @Override
	protected float transferFunction( float v ) {
		return v * scale;
	}

	/**
     * Specifies the scale factor.
     * @param scale the scale factor.
     * @min-value 1
     * @max-value 5+
     * @see #getScale
     */
	public void setScale(float scale) {
		this.scale = scale;
		initialized = false;
	}
	
	/**
     * Returns the scale factor.
     * @return the scale factor.
     * @see #setScale
     */
	public float getScale() {
		return scale;
	}

	@Override
	public String toString() {
		return "Colors/Rescale...";
	}

	@Override
	public BufferedImage filter(BufferedImage src, Struct parameters) throws PageException {BufferedImage dst=ImageUtil.createBufferedImage(src);
		Object o;
		if((o=parameters.removeEL(KeyImpl.init("Scale")))!=null)setScale(ImageFilterUtil.toFloatValue(o,"Scale"));
		if((o=parameters.removeEL(KeyImpl.init("Dimensions")))!=null){
			int[] dim=ImageFilterUtil.toDimensions(o,"Dimensions");
			setDimensions(dim[0],dim[1]);
		}

		// check for arguments not supported
		if(parameters.size()>0) {
			throw new FunctionException(ThreadLocalPageContext.get(), "ImageFilter", 3, "parameters", "the parameter"+(parameters.size()>1?"s":"")+" ["+CollectionUtil.getKeyList(parameters,", ")+"] "+(parameters.size()>1?"are":"is")+" not allowed, only the following parameters are supported [Scale, Dimensions]");
		}

		return filter(src, dst);
	}
}