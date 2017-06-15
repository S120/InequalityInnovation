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

import jmab.population.MacroPopulation;
import jmab.strategies.RandDOutcome;
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
public class RandDOutcomeStrategy extends AbstractStrategy implements
		RandDOutcome {
	
	double expParameter;
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
	 * @return the expParameter
	 */
	public double getExpParameter() {
		return expParameter;
	}


	/**
	 * @param expParameter the expParameter to set
	 */
	public void setExpParameter(double expParameter) {
		this.expParameter = expParameter;
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
			double bernoulliParameter=1-Math.exp(-(expParameter*nbResearchers));
			Binomial bernoulli = new Binomial(1, bernoulliParameter, prng);
			int success=bernoulli.nextInt();
			double productivityGain=0;
			if (success==1){
				productivityGain=distributionProductivityGain.nextDouble();

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
		buf.putDouble(expParameter);
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
		this.expParameter = buf.getDouble();
		
	}

}
