/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_v2.cplexSolver;
  
import static ca.mcmaster.hypercube_subtraction_v2.Constants.ONE;
import static ca.mcmaster.hypercube_subtraction_v2.Parameters.MIP_FILENAME;
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.BranchCallback;
import static ilog.cplex.IloCplex.MIPEmphasis.BestBound;

/**
 *
 * @author tamvadss
 */
public class CplexTree {
    
    private IloCplex cplex ;
    private BaseBranchCallback  branchingCallback= null;
        
    public CplexTree () throws IloException {
        cplex = new IloCplex() ;
        cplex.importModel(MIP_FILENAME);
        
        cplex.setParam(IloCplex.Param.Emphasis.MIP, BestBound);
        cplex.setParam( IloCplex.Param.MIP.Strategy.HeuristicFreq , -ONE);
        cplex.setParam(IloCplex.Param.MIP.Limits.CutPasses, -ONE);
        cplex.setParam(IloCplex.Param.Preprocessing.Presolve, false);
        cplex.setParam(IloCplex.Param.MIP.Strategy.File, ONE+ONE+ONE);
                
        IloLPMatrix lpMatrix = (IloLPMatrix)cplex.LPMatrixIterator().next();
        
        //turn this to true to compare our branching method with CPLEX native branching
        boolean useCplexDefaultBranching = false; 
        if (useCplexDefaultBranching) {
            branchingCallback=new BaseBranchCallback();            
        } else {
            branchingCallback = new BranchHandler  ( lpMatrix.getNumVars());             
        }
        cplex.use( branchingCallback);
    }
    
    public void solve () throws IloException {
        cplex.solve();
        System.out.println("Total number of branches was "+ this.branchingCallback.totalNumberOFBranches);
    }
    
}
