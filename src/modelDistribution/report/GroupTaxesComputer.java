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

import jmab.agents.TaxPayer;
import jmab.population.MacroPopulation;
import jmab.report.MacroVariableComputer;
import jmab.simulations.MacroSimulation;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.agent.Agent;

/**
 * @author Alessandro Caiani and Antoine Godin
 *
 */
public class GroupTaxesComputer implements MacroVariableComputer {
	
	private int groupId;
	private int lagTaxId;

	/* (non-Javadoc)
	 * @see jmab.report.VariableComputer#computeVariable(jmab.simulations.MacroSimulation)
	 */
	@Override
	public double computeVariable(MacroSimulation sim) {
		MacroPopulation macroPop = (MacroPopulation) sim.getPopulation();
		Population pop = macroPop.getPopulation(groupId);
		double taxes=0;
		for (Agent i:pop.getAgents()){
			TaxPayer taxPayer = (TaxPayer) i;
			if (!taxPayer.isDead()){
				taxes+=taxPayer.getPassedValue(lagTaxId, 0);
			}
		}
		return taxes;
	}

	/**
	 * @return the groupId
	 */
	public int getGroupId() {
		return groupId;
	}

	/**
	 * @param groupId the groupId to set
	 */
	public void setGroupId(int groupId) {
		this.groupId = groupId;
	}

	/**
	 * @return the lagTaxId
	 */
	public int getLagTaxId() {
		return lagTaxId;
	}

	/**
	 * @param lagTaxId the lagTaxId to set
	 */
	public void setLagTaxId(int lagTaxId) {
		this.lagTaxId = lagTaxId;
	}

	
}
