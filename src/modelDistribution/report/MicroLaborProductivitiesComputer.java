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

import java.util.Map;
import java.util.TreeMap;

import jmab.population.MacroPopulation;
import jmab.report.AbstractMicroComputer;
import jmab.report.MicroMultipleVariablesComputer;
import jmab.simulations.MacroSimulation;
import modelDistribution.StaticValues;
import modelDistribution.agents.CapitalFirm;
import modelDistribution.agents.ConsumptionFirmWithHierarchy;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.agent.Agent;

/**
 * @author Alessandro Caiani and Antoine Godin
 *
 */
public class MicroLaborProductivitiesComputer extends AbstractMicroComputer implements
		MicroMultipleVariablesComputer {
	private int firmsId;

	/* (non-Javadoc)
	 * @see jmab.report.MicroMultipleVariablesComputer#computeVariables(jmab.simulations.MacroSimulation)
	 */
	@Override
	public Map<Long, Double> computeVariables(MacroSimulation sim) {
		MacroPopulation macroPop = (MacroPopulation) sim.getPopulation();
		Population pop = macroPop.getPopulation(firmsId);
		TreeMap<Long,Double> result=new TreeMap<Long,Double>();
		for (Agent i:pop.getAgents()){
			ConsumptionFirmWithHierarchy firm= (ConsumptionFirmWithHierarchy)i;
			double production=firm.getPassedValue(StaticValues.LAG_PRODUCTION, 0);
			int nbEmployees=firm.getEmployees().size();
			if (!firm.isDead() && !(nbEmployees==0)){
			result.put(firm.getAgentId(), production/(double) nbEmployees);
			}
			else{
				result.put(firm.getAgentId(), Double.NaN);
			}
		}
		return result;
	}

	/**
	 * @return the firmsId
	 */
	public int getFirmsId() {
		return firmsId;
	}

	/**
	 * @param firmsId the firmsId to set
	 */
	public void setFirmsId(int firmsId) {
		this.firmsId = firmsId;
	}

}
