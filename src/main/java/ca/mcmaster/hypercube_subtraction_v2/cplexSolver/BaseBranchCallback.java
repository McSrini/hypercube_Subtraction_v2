/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_v2.cplexSolver;

import static ca.mcmaster.hypercube_subtraction_v2.Constants.ZERO;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;

/**
 *
 * @author tamvadss
 */
public class BaseBranchCallback extends IloCplex.BranchCallback{ 

    public long  totalNumberOFBranches=ZERO;
    @Override
    protected void main() throws IloException {
         totalNumberOFBranches+=getNbranches();
    }
    
}
