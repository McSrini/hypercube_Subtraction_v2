/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_v2.common;
  
import static ca.mcmaster.hypercube_subtraction_v2.Constants.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author tamvadss
 * 
 * we don't really need this class, remove it later and check if we get faster implementation. We can simply use lower bound constraint throughout.
 * 
 * 
 */
public class UpperBoundConstraint {
    
    public List<VariableCoefficientTuple>   sortedConstraintExpr = new ArrayList<VariableCoefficientTuple>() ;     
    public double upperBound;     
     
     
    //private constructor for local use in this file
    private   UpperBoundConstraint(    ){
       
    }
    
    public boolean isTrivially_Infeasible ( boolean isStrict){
        return this.sortedConstraintExpr.size()==ZERO && (isStrict ? upperBound <=ZERO: upperBound <ZERO);
    }

    public boolean isTrivially_Feasible (boolean isStrict){
        return this.sortedConstraintExpr.size()==ZERO && (isStrict ? upperBound >ZERO : upperBound >=ZERO);
    }
    
    public UpperBoundConstraint (LowerBoundConstraint lbc) {
        this.sortedConstraintExpr= lbc.sortedConstraintExpr ;
        this.upperBound = lbc.lowerBound;
        
    }
    
    //even with the worst possible choice of free vars, am I still feasible ?
    public boolean isGauranteedFeasible (boolean isStrict){
        double worstValue = ZERO ;
        for (VariableCoefficientTuple tuple : sortedConstraintExpr) {
            if (tuple.coeff>ZERO)    worstValue +=tuple.coeff;
        }
        return isStrict? (worstValue < this.upperBound) : (worstValue <= this.upperBound);
    }
    
    //with best possible choice of free vars, am I still unfeasible?
    //in other words , the best possible minimum is still larger than the upper bound
    public boolean isGauranteed_INFeasible (boolean isStrict){
        double bestvalue = ZERO;
        for (VariableCoefficientTuple tuple : sortedConstraintExpr) {
            if (tuple.coeff<ZERO)    bestvalue +=tuple.coeff;
        }
        return isStrict? (bestvalue >= this.upperBound) : (bestvalue >  this.upperBound);
    }
        
    //constraint  , disregarding fixed vars    
    public UpperBoundConstraint  getReducedConstraint (  List <String> varsFixedAtZero, List <String> varsFixedAtOne){
        
        UpperBoundConstraint reducedConstraint = new UpperBoundConstraint( );
        
        reducedConstraint.sortedConstraintExpr = new ArrayList<VariableCoefficientTuple> ();
        reducedConstraint.upperBound = this.upperBound;
                
        List <String> fixedVarNamesZero = new ArrayList <String>();
        List <String> fixedVarNamesOne = new ArrayList <String>();
        for (String binvar : varsFixedAtOne){
            fixedVarNamesOne.add(binvar );
        }
        for (String binvar : varsFixedAtZero){
            fixedVarNamesZero.add(binvar );
        }
                
        for (VariableCoefficientTuple tuple : this.sortedConstraintExpr){
            if (!fixedVarNamesOne.contains(tuple.varName) && !fixedVarNamesZero.contains(tuple.varName)  ) {
                reducedConstraint.sortedConstraintExpr .add(tuple);
            }else {
                //find the val to which this var is fixed                
                if (fixedVarNamesOne.contains(tuple.varName)) reducedConstraint.upperBound-= tuple.coeff;
            }
        }
        return reducedConstraint;
    }
    
    /* public String toString() {
        String str = "Constraint name "+ name ;
        str+=("Upper bound is "+ this.upperBound+"\n");
        for (VariableCoefficientTuple tuple : sortedConstraintExpr) {
            str += ("Var is " + tuple.varName  + " and its coeff is "+ tuple.coeff+"\n") ;
        }
        return str;
    }    */
}
