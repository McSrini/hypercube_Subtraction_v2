/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_v2.collection.cplex;

import static ca.mcmaster.hypercube_subtraction_v2.Constants.ZERO;
import static ca.mcmaster.hypercube_subtraction_v2.Parameters.USE_STRICT_INEQUALITY_IN_MIP;
import ca.mcmaster.hypercube_subtraction_v2.common.UpperBoundConstraint;
import ca.mcmaster.hypercube_subtraction_v2.common.VariableCoefficientTuple;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author tamvadss
 */
public class IncumbentHandler    extends IloCplex.IncumbentCallback{

    private IloNumVar[] variables  ;
    private UpperBoundConstraint ubc ; //can be full or reduced constraint
    public IncumbentHandler (IloNumVar[] variables, UpperBoundConstraint ubc) {
        this.variables= variables  ;
        this.ubc = ubc;
    }
     
    protected void main() throws IloException {
        if (!USE_STRICT_INEQUALITY_IN_MIP){
            double[]  values = getValues(variables);
            
            //make a map
            //create a map of var and value
            Map<String, Double > varValueMap = new HashMap <String, Double> ();
            for (int index = ZERO; index < variables.length; index ++) {
                varValueMap.put( variables[index].getName(), values[index] );
                //if (values[index]==ONE) System.out.println("Var is "+ variables[index].getName() + " and its value is " + values[index]) ;
            }
            
            //see what the constraint value is
            double value = ZERO;
            for (VariableCoefficientTuple tuple : ubc.sortedConstraintExpr) {
                if (Math.round(varValueMap.get(tuple.varName))>ZERO) {
                    value+=tuple.coeff;
                }
            }
            
            if (value == ubc.upperBound) {
                reject ();
                BranchingAssistorCallback.incumbentRejectedFlag=true;
                //System.out.println("solution rejected") ;
                for (double val: values) {
                    //System.out.println(val + ", ") ;
                }
            }
        }
    }
    
}

