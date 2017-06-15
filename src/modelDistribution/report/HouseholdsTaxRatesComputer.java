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
public class HouseholdsTaxRatesComputer implements MacroVariableComputer {
	
	private int populationId;
	private boolean income;

	
	/**
	 * @return the populationId
	 */
	public int getPopulationId() {
		return populationId;
	}


	/**
	 * @param populationId the populationId to set
	 */
	public void setPopulationId(int populationId) {
		this.populationId = populationId;
	}


	

	/* (non-Javadoc)
	 * @see jmab.report.VariableComputer#computeVariable(jmab.simulations.MacroSimulation)
	 */
	@Override
	public double computeVariable(MacroSimulation sim) {
		MacroPopulation macroPop = (MacroPopulation) sim.getPopulation();
		GovernmentDifferentiatedWorkers gov= (GovernmentDifferentiatedWorkers) macroPop.getPopulation(StaticValues.GOVERNMENT_ID).getAgentList().get(0);
		double taxRate=0;
		if (populationId==StaticValues.WORKERS_ID){
			if (income==true){
				taxRate=gov.getPassedValue(StaticValues.LAG_WORKERSTAXRATEINCOME, 0);
			}
			else{
				taxRate=gov.getPassedValue(StaticValues.LAG_WORKERSTAXRATEWEALTH, 0);
			}
		}
		if (populationId==StaticValues.MANAGERS_ID||populationId==StaticValues.RESEARCHERS_ID){
			if (income==true){
				taxRate=gov.getPassedValue(StaticValues.LAG_MANAGERSRESEARCHERSTAXRATEINCOME, 0);
			}
			else{
				taxRate=gov.getPassedValue(StaticValues.LAG_MANAGERSRESEARCHERSTAXRATEWEALTH, 0);
			}
		}
		if (populationId==StaticValues.TOPMANAGERS_ID){
			if (income==true){
				taxRate=gov.getPassedValue(StaticValues.LAG_TOPMANAGERSTAXRATEINCOME, 0);
			}
			else{
				taxRate=gov.getPassedValue(StaticValues.LAG_TOPMANAGERSTAXRATEWEALTH, 0);
			}
		}
		return taxRate;
	}


	/**
	 * @return the income
	 */
	public boolean isIncome() {
		return income;
	}


	/**
	 * @param income the income to set
	 */
	public void setIncome(boolean income) {
		this.income = income;
	}

	
	

}
