/*
 * JMAB - Java Macroeconomic Agent Based Modeling Toolkit
 * Copyright (C) 2013 Alessandro Caiani and Antoine Godin
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */
package benchmark.agents;

import jmab2.agents.DepositDemander;
import jmab2.agents.GoodDemander;
import jmab2.agents.IncomeTaxPayer;
import jmab2.agents.LaborSupplier;
import jmab2.agents.WageSetterWithTargets;


/**
 * @author Alessandro Caiani and Antoine Godin
 *
 */
@SuppressWarnings("serial")
public class HouseholdsWithDole extends Households implements GoodDemander, LaborSupplier,
DepositDemander, IncomeTaxPayer, WageSetterWithTargets {


	@Override
	public double getNetIncome(){
			return super.getNetIncome();
	}

}
