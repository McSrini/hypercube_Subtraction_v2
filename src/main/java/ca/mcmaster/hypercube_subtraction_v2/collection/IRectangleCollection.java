/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_v2.collection;

import ca.mcmaster.hypercube_subtraction_v2.common.LowerBoundConstraint;
import ilog.concert.IloException;

/**
 *
 * @author tamvadss
 */
public interface IRectangleCollection {
     public void  collect_INFeasibleHyperCubes(LowerBoundConstraint lbc) throws IloException;
     public void reset () ;    
}
