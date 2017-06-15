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
package modelDistribution.strategies;

import java.nio.ByteBuffer;

import jmab.agents.IncomeTaxPayer;
import jmab.agents.MacroAgent;
import jmab.population.MacroPopulation;
import modelDistribution.StaticValues;
import modelDistribution.agents.Government;
import modelDistribution.agents.GovernmentDifferentiatedWorkers;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.SimulationController;
import net.sourceforge.jabm.strategy.AbstractStrategy;

/**
 * @author Alessandro Caiani and Antoine Godin
 *
 */
@SuppressWarnings("serial")
public class IncomeWealthTaxStrategy extends AbstractStrategy implements TaxPayerStrategyWithDeficitManagement {


	

	/**
	 * Generate the byte array structure of the strategy. The structure is as follow:
	 * [wealthTaxRate][incomeTaxRate]
	 * @return the byte array content
	 */
	@Override
	public byte[] getBytes() {
		ByteBuffer buf = ByteBuffer.allocate(16);
		
		return buf.array();
	}


	/**
	 * Populates the strategy from the byte array content. The structure should be as follows:
	 * [wealthTaxRate][incomeTaxRate]
	 * @param content the byte array containing the structure of the strategy
	 * @param pop the Macro Population of agents
	 */
	@Override
	public void populateFromBytes(byte[] content, MacroPopulation pop) {
		ByteBuffer buf = ByteBuffer.wrap(content);
			}



	@Override
	public double computeTaxes(MacroAgent government) {
		GovernmentDifferentiatedWorkers gov= (GovernmentDifferentiatedWorkers) government;
		IncomeTaxPayer taxPayer = (IncomeTaxPayer)this.getAgent();
		double income = taxPayer.getGrossIncome();
		double wealth=taxPayer.getNetWealth();
		//double wealth=taxPayer.getNumericBalanceSheet()[0][StaticValues.SM_DEP];
		double taxes=0;
        if (taxPayer.getPopulationId()==StaticValues.WORKERS_ID){
        	double incomeTaxRate=gov.getWorkersIncomeTaxRate();
        	double wealthTaxRate=gov.getWorkersWealthTaxRate();
        	taxes=Math.max(wealthTaxRate*wealth+incomeTaxRate*income, 0);
		}
		if (taxPayer.getPopulationId()==StaticValues.MANAGERS_ID||taxPayer.getPopulationId()==StaticValues.RESEARCHERS_ID){
			double incomeTaxRate=gov.getManagersIncomeTaxRate();
        	double wealthTaxRate=gov.getManagersWealthTaxRate();
			taxes=Math.max(wealthTaxRate*wealth+incomeTaxRate*income, 0);
		}
		if (taxPayer.getPopulationId()==StaticValues.TOPMANAGERS_ID){
			double incomeTaxRate=gov.getTopManagersIncomeTaxRate();
        	double wealthTaxRate=gov.getTopManagersWealthTaxRate();
			taxes=Math.max(wealthTaxRate*wealth+incomeTaxRate*income, 0);
		}
		return taxes;
	}
	
}
