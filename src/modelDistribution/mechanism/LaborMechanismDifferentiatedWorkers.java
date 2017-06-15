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
package modelDistribution.mechanism;

import java.util.List;

import modelDistribution.StaticValues;
import modelDistribution.agents.LaborDemanderDifferentiatedWorkers;
import jmab.agents.LaborDemander;
import jmab.agents.LaborSupplier;
import jmab.agents.MacroAgent;
import jmab.mechanisms.AbstractMechanism;
import jmab.mechanisms.Mechanism;
import jmab.simulations.MarketSimulation;

/**
 * @author Alessandro Caiani and Antoine Godin
 *
 */
public class LaborMechanismDifferentiatedWorkers extends AbstractMechanism implements Mechanism {

	/* (non-Javadoc)
	 * @see jmab.mechanisms.Mechanism#execute(jmab.agents.MacroAgent, jmab.agents.MacroAgent, int)
	 */
	public void execute( MacroAgent buyer, MacroAgent seller, int idMarket) {
		execute((LaborDemander) buyer, (LaborSupplier) seller, idMarket);
		}
	
	private void execute(LaborDemander employer, LaborSupplier worker, int idMarket){
		worker.setEmployer(employer);
        LaborDemanderDifferentiatedWorkers employer1=(LaborDemanderDifferentiatedWorkers) employer;
        employer1.addEmployee(worker, idMarket);
		worker.setActive(false, idMarket);
		if (employer1.getLaborDemand(idMarket)==0){
			employer1.setActive(false, idMarket);
		}		
	}

	/* (non-Javadoc)
	 * @see jmab.mechanisms.Mechanism#execute(jmab.agents.MacroAgent, java.util.List, int)
	 */
	@Override
	public void execute(MacroAgent buyer, List<MacroAgent> seller, int idMarket) {
		// TODO Auto-generated method stub	
	}


}
