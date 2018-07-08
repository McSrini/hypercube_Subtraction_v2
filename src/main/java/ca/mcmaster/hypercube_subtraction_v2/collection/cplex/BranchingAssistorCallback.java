/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_v2.collection.cplex;

import static ca.mcmaster.hypercube_subtraction_v2.Constants.MIP_ROOT_ID;
import static ca.mcmaster.hypercube_subtraction_v2.Constants.ONE;
import static ca.mcmaster.hypercube_subtraction_v2.Constants.TWO;
import static ca.mcmaster.hypercube_subtraction_v2.Constants.ZERO;
import ca.mcmaster.hypercube_subtraction_v2.common.UpperBoundConstraint;
import ca.mcmaster.hypercube_subtraction_v2.common.VariableCoefficientTuple;
import ca.mcmaster.hypercube_subtraction_v2.cplexSolver.NodePayload;
 
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

/**
 *
 * @author tamvadss
 */
public class BranchingAssistorCallback extends IloCplex.BranchCallback{ 
    
    private IloNumVar[] variables  ;
    private UpperBoundConstraint ubc ; 
    
    public static boolean incumbentRejectedFlag = false;
    
    public BranchingAssistorCallback (IloNumVar[] variables, UpperBoundConstraint ubc) {
        this.ubc = ubc;
        this.variables= variables;
    }
 
    protected void main() throws IloException {
        
        if ( getNbranches()> ZERO ){  
             boolean isMipRoot = ( getNodeId().toString()).equals( MIP_ROOT_ID);

            if (isMipRoot){
                //root of mip

                NodeAttachment data = new NodeAttachment (  );
                for (VariableCoefficientTuple tuple: ubc.sortedConstraintExpr) {
                    for (IloNumVar var : variables){
                        if (var.getName().equals(tuple.varName)){
                            data.availableBranchingVariables.add(var);
                        }
                    }
                }
                setNodeData(data);                
            } 

            NodeAttachment nodeData = (NodeAttachment) getNodeData();
            // vars needed for child node creation 
            double[ ][] bounds= new double[TWO ][];
            IloNumVar[][] vars= new IloNumVar[TWO][] ;
            IloCplex.BranchDirection[ ][]  dirs= new  IloCplex.BranchDirection[ TWO][];
            //our branching var
            IloNumVar branchingVar=null;

            //take cplex branching unless incumbent rejected
            if (incumbentRejectedFlag){

                //reset flag
                incumbentRejectedFlag= false;

                if (nodeData.availableBranchingVariables.size()>ZERO) {
                    //choose to branch on var from available list
                    branchingVar = nodeData.availableBranchingVariables.get(ZERO);

                    vars[ZERO] = new IloNumVar[ONE];
                    vars[ZERO][ZERO]=  branchingVar;
                    bounds[ZERO]=new double[ONE ];
                    bounds[ZERO][ZERO]=ZERO;
                    dirs[ZERO]= new IloCplex.BranchDirection[ONE];
                    dirs[ZERO][ZERO]=IloCplex.BranchDirection.Down;

                    vars[ONE] = new IloNumVar[ONE];
                    vars[ONE][ZERO]= branchingVar;
                    bounds[ONE]=new double[ONE ];
                    bounds[ONE][ZERO]=ONE;
                    dirs[ONE]= new IloCplex.BranchDirection[ONE];
                    dirs[ONE][ZERO]=IloCplex.BranchDirection.Up;
                }else {
                    //prune this node, no more branching possible
                    branchingVar =null;
                    prune();
                }

            }else {
                //default branching 
                 getBranches( vars, bounds,  dirs) ;
                 branchingVar = vars[ZERO][ZERO];
            }

            if (null!=branchingVar) {
                // make a note of which vars are available to kids
                NodeAttachment left = new NodeAttachment ();
                NodeAttachment right = new NodeAttachment ();
                left.availableBranchingVariables.addAll( nodeData.availableBranchingVariables);
                left.availableBranchingVariables.remove( branchingVar);
                right.availableBranchingVariables.addAll( left.availableBranchingVariables);

                makeBranch( vars[ZERO],  bounds[ZERO],dirs[ZERO],  getObjValue()  ,  left );
                makeBranch( vars[ONE],  bounds[ONE],dirs[ONE],   getObjValue(), right  );
            }

        }//if num bracnhes >=2


    }
    
}
