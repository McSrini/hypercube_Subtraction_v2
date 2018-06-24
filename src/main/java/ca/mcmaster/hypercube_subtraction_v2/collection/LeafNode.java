/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_v2.collection;
 
import static ca.mcmaster.hypercube_subtraction_v2.Constants.*;
import static ca.mcmaster.hypercube_subtraction_v2.Parameters.USE_STRICT_INEQUALITY_IN_MIP;
import ca.mcmaster.hypercube_subtraction_v2.TestDriver;
import ca.mcmaster.hypercube_subtraction_v2.common.*;
import java.util.*;

/**
 *
 * @author tamvadss
 * 
 * leaf node of BNB tree
 * 
 */
public class LeafNode extends Rectangle{
    
    public List<String> bestVertex_zeroFixedVariables = new ArrayList<String>();
    public List<String> bestVertex_oneFixedVariables = new ArrayList<String>();
    
    public LeafNode(List<String> zeroFixedVariables, List<String> oneFixedVariables) {
        super(zeroFixedVariables, oneFixedVariables);
    }
    
    //finds     vars fixings to get  best vertex
    public void findVarFixingsAtBestVertex () {
        for (VariableCoefficientTuple tuple : TestDriver.objective.objectiveExpr){
            String thisVar = tuple.varName;
            if (this.zeroFixedVariables.contains(thisVar) || this.oneFixedVariables.contains(thisVar )) {
                //already fixed, do nothing
            }else {
                //choose fixing so that objective becomes lowest possible
                if (tuple.coeff < ZERO){
                    bestVertex_oneFixedVariables.add(thisVar);
                } else {
                    bestVertex_zeroFixedVariables.add(thisVar);
                }
            }
        }
    }
        
    public boolean isVariableZeroAtBestVertex (String var) {
        //is this variable which is about to be branched on, zero at best vertex?
        return this.bestVertex_zeroFixedVariables.contains(var);
    }
        
           
    public boolean isConstraintViolatedAtBestVertex(LowerBoundConstraint lbc ){
         
        LowerBoundConstraint reducedConstraint = lbc.getReducedConstraint( this.zeroFixedVariables, this.oneFixedVariables  );
        reducedConstraint = reducedConstraint.getReducedConstraint(bestVertex_zeroFixedVariables ,  bestVertex_oneFixedVariables);
        return (! reducedConstraint.isGauranteedFeasible( USE_STRICT_INEQUALITY_IN_MIP))  ;
         
    }
    
    
}
