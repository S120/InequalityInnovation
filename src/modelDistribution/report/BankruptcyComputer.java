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

import jmab.agents.MacroAgent;
import jmab.population.MacroPopulation;
import jmab.report.MacroVariableComputer;
import jmab.simulations.MacroSimulation;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.agent.Agent;

/**
 * @author Alessandro Caiani and Antoine Godin
 * This computer computes the number of bankruptcies over the period within a specific class of agents, such as banks
 * or a specific type of firms.
 *
 */
public class BankruptcyComputer implements MacroVariableComputer {
	private int populationId; // the ID of the population on which we want to compute the number of bankruptcies (banks, types of firms etc.)
	
	/* (non-Javadoc)
	 * @see jmab.report.VariableComputer#computeVariable(jmab.simulations.MacroSimulation)
	 */
	

	@Override
	public double computeVariable(MacroSimulation sim) {
		MacroPopulation macroPop = (MacroPopulation) sim.getPopulation();
		Population pop = macroPop.getPopulation(populationId);
		int bankruptcies=0;
		for(Agent i:pop.getAgents()){
			MacroAgent agent = (MacroAgent) i;
			if (agent.isDefaulted()){
				bankruptcies+=1;
			}
		}
		return bankruptcies;
	}

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
}
