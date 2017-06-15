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

import java.util.List;

import jmab.agents.MacroAgent;
import jmab.population.MacroPopulation;
import jmab.report.MacroVariableComputer;
import jmab.simulations.MacroSimulation;
import jmab.stockmatrix.ConsumptionGood;
import jmab.stockmatrix.Item;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.agent.Agent;

/**
 * @author Alessandro Caiani and Antoine Godin
 *
 */
public class HouseholdsConsumptionComputer implements MacroVariableComputer {
	
	private int populationId;
	private int consGoodId;

	
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


	/**
	 * @return the loansId
	 */
	public int getConsGoodId() {
		return consGoodId;
	}


	/**
	 * @param loansId the loansId to set
	 */
	public void setConsGoodId(int consGoodId) {
		this.consGoodId = consGoodId;
	}

	/* (non-Javadoc)
	 * @see jmab.report.VariableComputer#computeVariable(jmab.simulations.MacroSimulation)
	 */
	@Override
	public double computeVariable(MacroSimulation sim) {
		MacroPopulation macroPop = (MacroPopulation) sim.getPopulation();
		Population pop = macroPop.getPopulation(populationId);
		double consumption=0;
		for (Agent i:pop.getAgents()){ 
			MacroAgent agent=(MacroAgent) i;
			if (!agent.isDead()){
			List<Item> consStock=agent.getItemsStockMatrix(true, consGoodId);
			double agentCons=0;
			for (Item j:consStock){
				ConsumptionGood good= (ConsumptionGood)j;
				agentCons+=good.getQuantity();
			}
			consumption+=agentCons;
			}
		}
		return consumption;
	}

	
	

}
