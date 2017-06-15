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
package modelDistribution.report;

import jmab.population.MacroPopulation;
import jmab.report.MacroVariableComputer;
import jmab.simulations.MacroSimulation;
import modelDistribution.StaticValues;
import modelDistribution.agents.GovernmentDifferentiatedWorkers;

/**
 * @author Alessandro Caiani and Antoine Godin
 *
 */
public class GovernmentTaxRateMultiplyingFactorComputer implements MacroVariableComputer {
	
	

	/* (non-Javadoc)
	 * @see jmab.report.VariableComputer#computeVariable(jmab.simulations.MacroSimulation)
	 */
	@Override
	public double computeVariable(MacroSimulation sim) {
		MacroPopulation macroPop = (MacroPopulation) sim.getPopulation();
		GovernmentDifferentiatedWorkers gov= (GovernmentDifferentiatedWorkers) macroPop.getPopulation(StaticValues.GOVERNMENT_ID).getAgentList().get(0);
		double taxRateMultiplFactor=gov.getTaxRatesMultiplyingFactor();
		return taxRateMultiplFactor;
	}


	
	
	

}
