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
package lucee.runtime.chart;

import java.text.AttributedString;

import lucee.runtime.op.Caster;

import org.jfree.chart.labels.PieSectionLabelGenerator;
import org.jfree.data.general.PieDataset;

public class PieSectionLabelGeneratorImpl implements PieSectionLabelGenerator {

	private int labelFormat; 

	public PieSectionLabelGeneratorImpl(int labelFormat) {
		this.labelFormat=labelFormat;
	}

	@Override
	public AttributedString generateAttributedSectionLabel(PieDataset arg0, Comparable arg1) {
		return null;
	}

	@Override
	public String generateSectionLabel(PieDataset pd, Comparable c) {
		double value = Caster.toDoubleValue(pd.getValue(c),true,0.0);
		return LabelFormatUtil.format(labelFormat, value);
		}
		
		

}