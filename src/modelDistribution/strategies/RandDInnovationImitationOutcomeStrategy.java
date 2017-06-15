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
import java.util.ArrayList;
import java.util.List;

import jmab.agents.MacroAgent;
import jmab.population.MacroPopulation;
import jmab.strategies.RandDOutcome;
import modelDistribution.StaticValues;
import modelDistribution.agents.CapitalFirmWithHierarchy;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.SimulationController;
import net.sourceforge.jabm.distribution.AbstractDelegatedDistribution;
import net.sourceforge.jabm.strategy.AbstractStrategy;
import cern.jet.random.Beta;
import cern.jet.random.Binomial;
import cern.jet.random.Uniform;
import cern.jet.random.engine.RandomEngine;

/**
 * @author Alessandro Caiani and Antoine Godin
 *
 */
@SuppressWarnings("serial")
public class RandDInnovationImitationOutcomeStrategy extends AbstractStrategy implements
		RandDOutcome {
	
	double expParameterInno;
	double expParameterImit;
	int numberCompetitorsImitated;
	protected RandomEngine prng;
	private AbstractDelegatedDistribution distributionProductivityGain;
	

	
	


	/**
	 * @return the distributionProductivityGain
	 */
	public AbstractDelegatedDistribution getDistributionProductivityGain() {
		return distributionProductivityGain;
	}


	/**
	 * @param distributionProductivityGain the distributionProductivityGain to set
	 */
	public void setDistributionProductivityGain(
			AbstractDelegatedDistribution distributionProductivityGain) {
		this.distributionProductivityGain = distributionProductivityGain;
	}






	/**
	 * @return the expParameterInno
	 */
	public double getExpParameterInno() {
		return expParameterInno;
	}


	/**
	 * @param expParameterInno the expParameterInno to set
	 */
	public void setExpParameterInno(double expParameterInno) {
		this.expParameterInno = expParameterInno;
	}


	/**
	 * @return the expParameterImit
	 */
	public double getExpParameterImit() {
		return expParameterImit;
	}


	/**
	 * @param expParameterImit the expParameterImit to set
	 */
	public void setExpParameterImit(double expParameterImit) {
		this.expParameterImit = expParameterImit;
	}


	/**
	 * @return the numberCompetitorsImitated
	 */
	public int getNumberCompetitorsImitated() {
		return numberCompetitorsImitated;
	}


	/**
	 * @param numberCompetitorsImitated the numberCompetitorsImitated to set
	 */
	public void setNumberCompetitorsImitated(int numberCompetitorsImitated) {
		this.numberCompetitorsImitated = numberCompetitorsImitated;
	}


	/**
	 * @return the prng
	 */
	public RandomEngine getPrng() {
		return prng;
	}


	/**
	 * @param prng the prng to set
	 */
	public void setPrng(RandomEngine prng) {
		this.prng = prng;
	}


	/* (non-Javadoc)
	 * @see jmab.strategies.RandDOutcome#computeRandDOutcome()
	 */
	@Override
	public double computeRandDOutcome(int nbResearchers) {
		if(nbResearchers==0)
			return 0;
		else{
			double bernoulliParameterInno=1-Math.exp(-(expParameterInno*nbResearchers));
			double bernoulliParameterImit=1-Math.exp(-(expParameterImit*nbResearchers));
			Binomial bernoulliInno = new Binomial(1, bernoulliParameterInno, prng);
			Binomial bernoulliImit = new Binomial(1, bernoulliParameterImit, prng);
			int successInno=bernoulliInno.nextInt();
			int successImit=bernoulliImit.nextInt();
			double productivityGain=0;
			double productivityGainInno=0;
			double productivityGainImit=0;
			if (successInno==1 && successImit==1){
				CapitalFirmWithHierarchy firm= (CapitalFirmWithHierarchy) this.agent;
				productivityGainInno=distributionProductivityGain.nextDouble();
				SimulationController controller = (SimulationController)this.getScheduler();
				MacroPopulation macroPop = (MacroPopulation) controller.getPopulation();
				Population capitalFirms = macroPop.getPopulation(StaticValues.CAPITALFIRMS_ID);
			    List< MacroAgent> listImitated=new ArrayList<MacroAgent>();
				Uniform uniform= new Uniform(0, (capitalFirms.getSize()-1), prng);
			    for (int i=0;i<numberCompetitorsImitated;i++){
					int index=uniform.nextInt();
			    	CapitalFirmWithHierarchy imitatedFirm= (CapitalFirmWithHierarchy)capitalFirms.getAgentList().get(index);
			    	if (!listImitated.contains(imitatedFirm)&&!(imitatedFirm.getAgentId()==firm.getAgentId())){
			    		listImitated.add(imitatedFirm);
				    	double potentialProductivityGainImit=(imitatedFirm.getCapitalProductivity()-firm.getCapitalProductivity())/firm.getCapitalProductivity();
				    	if (potentialProductivityGainImit>productivityGainImit){
				    		productivityGainImit=potentialProductivityGainImit;
				    	}
			    	}
			    	else{
			    		i=i-1;
			    	}
			    }
				productivityGain= Math.max(productivityGainImit, productivityGainInno);	
			}
			else if (successInno==0 && successImit==1){
				CapitalFirmWithHierarchy firm= (CapitalFirmWithHierarchy) this.agent;
				SimulationController controller = (SimulationController)this.getScheduler();
				MacroPopulation macroPop = (MacroPopulation) controller.getPopulation();
				Population capitalFirms = macroPop.getPopulation(StaticValues.CAPITALFIRMS_ID);
			    List< MacroAgent> listImitated=new ArrayList<MacroAgent>();
				Uniform uniform= new Uniform(0, (capitalFirms.getSize()-1), prng);
				for (int i=0;i<numberCompetitorsImitated;i++){
					int index=uniform.nextInt();
			    	CapitalFirmWithHierarchy imitatedFirm= (CapitalFirmWithHierarchy)capitalFirms.getAgentList().get(index);
			    	if (!listImitated.contains(imitatedFirm)&&!(imitatedFirm.getAgentId()==firm.getAgentId())){
			    		listImitated.add(imitatedFirm);
				    	double potentialProductivityGainImit=(imitatedFirm.getCapitalProductivity()-firm.getCapitalProductivity())/firm.getCapitalProductivity();
				    	if (potentialProductivityGainImit>productivityGainImit){
				    		productivityGainImit=potentialProductivityGainImit;
				    	}
			    	}
			    	else{
			    		i=i-1;
			    	}
			    }
				productivityGain=productivityGainImit;		
			}
			else if (successInno==1 && successImit==0){
				productivityGainInno=distributionProductivityGain.nextDouble();
				productivityGain=productivityGainInno;
			}
			return productivityGain;
		}
	}

	/**
	 * Generate the byte array structure of the strategy. The structure is as follow:
	 * [expParameter][alphaBetaDistribution][betaBetaDistribution]
	 * @return the byte array content
	 */
	@Override
	public byte[] getBytes() {
		ByteBuffer buf = ByteBuffer.allocate(24);
		buf.putDouble(expParameterInno);
		return buf.array();
	}


	/**
	 * Populates the strategy from the byte array content. The structure should be as follows:
	 * [expParameter][alphaBetaDistribution][betaBetaDistribution]
	 * @param content the byte array containing the structure of the strategy
	 * @param pop the Macro Population of agents
	 */
	@Override
	public void populateFromBytes(byte[] content, MacroPopulation pop) {
		ByteBuffer buf = ByteBuffer.wrap(content);
		this.expParameterInno = buf.getDouble();
		
	}

}
