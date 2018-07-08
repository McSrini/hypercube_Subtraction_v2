/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_v2.collection.cplex;

import static ca.mcmaster.hypercube_subtraction_v2.Constants.*;
import static ca.mcmaster.hypercube_subtraction_v2.Parameters.MIP_FILENAME;
import ca.mcmaster.hypercube_subtraction_v2.common.UpperBoundConstraint;
import ca.mcmaster.hypercube_subtraction_v2.common.VariableCoefficientTuple;
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import static ilog.cplex.IloCplex.MIPEmphasis.BestBound;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author tamvadss
 * 
 * uses CPLEX to find the best vertex
 * 
 */
public class CplexBased_BestVertexFinder {
    
    private IloCplex cplex;
    private  IloNumVar[] variables  ;
    private UpperBoundConstraint reducedConstraint;
    
    public double valueAtBestVertex ;
    
    public CplexBased_BestVertexFinder (UpperBoundConstraint reducedConstraint, 
                                  List <String> zeroFixedVariables, 
                                  List <String> oneFixedVariables) throws IloException{
        
        this.reducedConstraint = reducedConstraint;
        
        //import mip into ilocplex
        cplex = new IloCplex ();
      
        cplex.importModel(MIP_FILENAME);
        
        //remove all constraints
        //remove all constrs
        IloLPMatrix lpMatrix = (IloLPMatrix)cplex.LPMatrixIterator().next();
        variables  =lpMatrix.getNumVars();
        IloRange[] constraints = lpMatrix.getRanges();
        for (IloRange constr: constraints ){
            cplex.delete(constr) ;
        }
        
        //add the UBC
         //add ubc
        addConstraint(reducedConstraint);
        //add the var fixings for this rectangle
        addVarFixing(zeroFixedVariables, ZERO) ;
        addVarFixing(oneFixedVariables, ONE) ;
        
        cplex.use (new IncumbentHandler(variables, reducedConstraint)) ;
        cplex.use (new BranchingAssistorCallback(variables, reducedConstraint)) ;
        
        //it seems incumbent callback is not invoked unless we disable presolve etc., for easy MIPs
        cplex.setParam(IloCplex.Param.Emphasis.MIP, BestBound);
        cplex.setParam( IloCplex.Param.MIP.Strategy.HeuristicFreq , -ONE);
        cplex.setParam(IloCplex.Param.MIP.Limits.CutPasses, -ONE);
        cplex.setParam(IloCplex.Param.Preprocessing.Presolve, false); 
        
        cplex.setParam(IloCplex.IntParam.Reduce, ZERO);
        
        //cplex.exportModel("F:\\temporary files here\\test.lp");
    }
    
    
    //once solved , get the best vertex 
    //return false if infeasible, true otherwise
    public boolean getVarFixingsAtOrigin (List<Boolean> isVarZeroFixedAtOrigin) throws IloException{
         
        isVarZeroFixedAtOrigin.clear();
        boolean retval = true;
        
        //minimize
        cplex.solve();
        IloCplex.Status cplexStatus = cplex.getStatus();
        retval= cplexStatus.equals( IloCplex.Status.Optimal) || cplexStatus.equals(IloCplex.Status.Feasible);
        
        if (retval){
            double[]  values = cplex.getValues(variables);
            //System.out.println("optimal is "+ cplex.getObjValue());

            this.valueAtBestVertex = cplex.getObjValue();

            //create a map of var and value
            Map<String, Double > varValueMap = new HashMap <String, Double> ();
            for (int index = ZERO; index < variables.length; index ++) {
                varValueMap.put( variables[index].getName(), values[index] );
                //System.out.println("Var is "+ variables[index].getName() + " and its value is " + values[index]) ;
            }

            //find var values at optimum vertex
            for (int index = ZERO; index < reducedConstraint.sortedConstraintExpr.size(); index ++) {
                double varValue = varValueMap.get(reducedConstraint.sortedConstraintExpr.get(index).varName);
                isVarZeroFixedAtOrigin.add(Math.round(varValue)==ZERO) ;
            }
        }
        
        cplex.end();
        return retval ;
    }
    
    
    private void addVarFixing (List <String> varNames, int value) throws IloException {
        for (String varname: varNames){
             cplex.addEq(getVar (  varname), value);
        }
    }
    
    
    private void addConstraint(UpperBoundConstraint ubc) throws IloException {
        IloNumExpr expr = cplex.linearNumExpr();
        for (VariableCoefficientTuple tuple : ubc.sortedConstraintExpr){
            IloNumVar var = getVar(tuple.varName) ;
            expr = cplex.sum(expr, cplex.prod( tuple.coeff, var));
        }
        cplex.addLe(expr , ubc.upperBound );
         
    }
    
    
    private IloNumVar getVar (String varname){
        IloNumVar result = null;
        for (IloNumVar var : this.variables) {
            if (var.getName().equals( varname)){
                result= var;
                break;
            }
        }
        return result;
    }
}
