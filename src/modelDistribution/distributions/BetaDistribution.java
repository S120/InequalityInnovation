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
package modelDistribution.distributions;

import net.sourceforge.jabm.distribution.AbstractDelegatedDistribution;

import org.springframework.beans.factory.annotation.Required;

import cern.jet.random.Beta;


/**
 * @author Alessandro Caiani and Antoine Godin
 *
 */
@SuppressWarnings("serial")
public class BetaDistribution extends AbstractDelegatedDistribution {

	
	
	protected double alpha;
	protected double beta;
	/**
	 * 
	 */
	public BetaDistribution() {
	}

	/* (non-Javadoc)
	 * @see net.sourceforge.jabm.distribution.AbstractDelegatedDistribution#initialise()
	 */
	@Override
	public void initialise() {
		this.delegate=new Beta(alpha, beta, prng);
	}
	
	@Required
	public void setAlpha(double alpha){
		this.alpha=alpha;
		reinitialise();
	}
	
	public double getAlpha(){
		return alpha;
	}

	/**
	 * @return the max
	 */
	public double getBeta() {
		return beta;
	}

	/**
	 * @param max the max to set
	 */
	public void setBeta(double beta) {
		this.beta = beta;
		reinitialise();
	}
	

}
