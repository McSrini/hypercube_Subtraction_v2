/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_v2.collection;
  
import static ca.mcmaster.hypercube_subtraction_v2.Constants.*;
import static ca.mcmaster.hypercube_subtraction_v2.Parameters.USE_STRICT_INEQUALITY_IN_MIP;
import ca.mcmaster.hypercube_subtraction_v2.TestDriver;
import ca.mcmaster.hypercube_subtraction_v2.collection.cplex.CplexBased_BestVertexFinder;
import ca.mcmaster.hypercube_subtraction_v2.common.LowerBoundConstraint;
import ca.mcmaster.hypercube_subtraction_v2.common.UpperBoundConstraint;
import ca.mcmaster.hypercube_subtraction_v2.common.VariableCoefficientTuple;
import ilog.concert.IloException;
import static java.lang.System.exit;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author tamvadss
 * 
 * this class represents a collected infeasible hypercube
 * 
 */
public class Rectangle {
    
    
    //note that some vars can be free, but some are fixed by branching conditions like so
    public List <String> zeroFixedVariables = new ArrayList <String>();
    public List <String> oneFixedVariables = new ArrayList <String>();
     
    //uncontsrained best vertex    
    public List<String> unconstrained_bestVertex_zeroFixedVariables = new ArrayList<String>();
    public List<String> unconstrained_bestVertex_oneFixedVariables = new ArrayList<String>();    
    public Double objectiveValueAtBestUnconstrainedVertex = null;
        
    //best INFEASIBLE vertex for a given constraint. User must keep track of which constraint it is
    //if constraint invalidates the best unconstrained vertex, then this vertex is the same as the best unconstrained vertex
    public Double objectiveValueAtBestVertex_forGivenConstraint = null;
    public List<String> bestVertex_zeroFixedVariables_forGivenConstraint  = new ArrayList<String>();
    public List<String> bestVertex_oneFixedVariables_forGivenConstraint  = new ArrayList<String>();  
    
    public Rectangle (List <String> zeroFixedVariables , List <String> oneFixedVariables ){
        this.zeroFixedVariables .addAll(zeroFixedVariables);
        this.oneFixedVariables  .addAll( oneFixedVariables); 
        this.find_Unconstrained_BestVertex();
    }
    
    public int getSize () {
        return zeroFixedVariables.size()+ this.oneFixedVariables.size();
    }
    
    //if other rect is complimenarty , then return merged rect else return null
    public Rectangle mergeIfComplimentary (Rectangle other) {
        Rectangle result = null;
        
        int myZeroSize = this.zeroFixedVariables.size();
        int myOneSize = this.oneFixedVariables.size();
        int otherZeroSize = other.zeroFixedVariables.size();
        int otherOneSize = other.oneFixedVariables.size();
        
        boolean isSizeMatchOne = ( (myZeroSize == otherZeroSize-ONE) &&(myOneSize==otherOneSize+ONE)) ;
        boolean isSizeMatchTwo =   ( (myZeroSize == otherZeroSize+ONE) &&(myOneSize==otherOneSize-ONE)) ;
        
        boolean isComplimentary = true;
        
        if (isSizeMatchOne) {
            List < String> extraZeroVar = new ArrayList<String> ();
            for (String var : other.zeroFixedVariables) {
                if ( !this.zeroFixedVariables.contains(var)) extraZeroVar.add(var);
                if (extraZeroVar.size()>ONE){
                    //not complimentary
                    isComplimentary= false;
                    break;
                }
            }
            
            List < String> extraOneVar = new ArrayList<String> ();
            if (isComplimentary) {
                for (String var : this.oneFixedVariables  ) {
                    if ( !other.oneFixedVariables.contains(var)) extraOneVar.add(var);
                    if (extraOneVar.size()>ONE){
                        //not complimentary
                        isComplimentary= false;
                        break;
                    }
                }
            }
            
            if (isComplimentary && extraOneVar.get(ZERO).equals(extraZeroVar.get(ZERO))) {
                //well and truly complimentary
                result = new Rectangle (this.zeroFixedVariables,other.oneFixedVariables) ;
            }
            
        }else if (isSizeMatchTwo) {
            
            // ( (myZeroSize == otherZeroSize+ONE) &&(myOneSize==otherOneSize-ONE))
            
            List < String> extraZeroVar = new ArrayList<String> ();
            for (String var : this.zeroFixedVariables) {
                if ( !other.zeroFixedVariables.contains(var)) extraZeroVar.add(var);
                if (extraZeroVar.size()>ONE){
                    //not complimentary
                    isComplimentary= false;
                    break;
                }
            }
            
            List < String> extraOneVar = new ArrayList<String> ();
            if (isComplimentary) {
                for (String var : other.oneFixedVariables  ) {
                    if ( !this.oneFixedVariables.contains(var)) extraOneVar.add(var);
                    if (extraOneVar.size()>ONE){
                        //not complimentary
                        isComplimentary= false;
                        break;
                    }
                }
            }
            
            if (isComplimentary && extraOneVar.get(ZERO).equals(extraZeroVar.get(ZERO))) {
                //well and truly complimentary
                result = new Rectangle (other.zeroFixedVariables,this.oneFixedVariables) ;
            }
        }
        
        return result;
    }
    
    //is other absorbed into this ?
    public boolean isAbsorbed (Rectangle other) {
        boolean result = true ;
        
        //check if all my zero fixings are in other
        for (String var : this.zeroFixedVariables){
            if (!other.zeroFixedVariables.contains(var)) {
                result = false ;
                break;
            }   
        }
        
        //check if all my one fixings are in other
        if (result) {
            for (String var : this.oneFixedVariables){
                if (!other.oneFixedVariables.contains(var)) {
                    result = false ;
                    break;
                }   
            }
        }
        
        return result;
    }
        
    public boolean isConstraintViolatedAt_unconstrained_BestVertex(LowerBoundConstraint lbc ){
         
        LowerBoundConstraint reducedConstraint = lbc.getReducedConstraint( this.zeroFixedVariables, this.oneFixedVariables  );
        reducedConstraint = reducedConstraint.getReducedConstraint(unconstrained_bestVertex_zeroFixedVariables ,  unconstrained_bestVertex_oneFixedVariables);
        return ! reducedConstraint.isGauranteedFeasible( USE_STRICT_INEQUALITY_IN_MIP) ;
         
    }
    
    public String printMe (String name){
        
        String result=name+"\n"; 
        result += " --- Zero fixed vars :";
        for (String str: zeroFixedVariables){
            result += str + ",";
        }
        result += "  -- One fixed vars :";
        for (String str: oneFixedVariables){
            result += str + ",";
        }
        
        result += "\n  -- unconstrained best vertex zero fixed vars :";
        for (String str:unconstrained_bestVertex_zeroFixedVariables) {
            result += str + ",";
        }
        result += "  -- unconstrained best vertex one fixed vars :";
        for (String str:unconstrained_bestVertex_oneFixedVariables) {
            result += str + ",";
        }
        
        if (objectiveValueAtBestVertex_forGivenConstraint!=null) {
            result += "\n  -- constrained best vertex zero fixed vars :";
            for (String str: this.bestVertex_zeroFixedVariables_forGivenConstraint) {
                result += str + ",";
            }
            result += "  -- Constrained best vertex one fixed vars :";
            for (String str: this.bestVertex_oneFixedVariables_forGivenConstraint) {
                result += str + ",";
            }
            result += " \n  -- constrained best vertex value = "+objectiveValueAtBestVertex_forGivenConstraint;
        }
               
          
        //System.out.println( "Rectangle is " + result);
        return result;

    }
    
    //finds best INFEASIBLE vertex for this constraint, which could be the best unconstrained vertex
    //if not, then use cplex to find best vertex
    //best vertex may not exist in which case we return NULL
    public Double   findBestVertex (LowerBoundConstraint lbc) throws IloException {
        
        //reset
        bestVertex_zeroFixedVariables_forGivenConstraint.clear();
        bestVertex_oneFixedVariables_forGivenConstraint.clear();
        objectiveValueAtBestVertex_forGivenConstraint=null;
        
        //this is the ubc for which we will collect the best feasible vertex    
        UpperBoundConstraint ubc  = new UpperBoundConstraint(  
                                        lbc.getReducedConstraint(this.zeroFixedVariables, this.oneFixedVariables)) ;
          
        
        //first check if leaf's best unconstrained vertex is infeasible to the lbc , if yes then that is the best vertex
        if ( this.isConstraintViolatedAt_unconstrained_BestVertex(lbc)) {
            
            //best INFEASIBLE vertex for this LBC found, same as best unconstrainted vertex
            
            bestVertex_zeroFixedVariables_forGivenConstraint  .addAll(this.unconstrained_bestVertex_zeroFixedVariables) ;
            bestVertex_oneFixedVariables_forGivenConstraint.addAll(this.unconstrained_bestVertex_oneFixedVariables) ;
            
            objectiveValueAtBestVertex_forGivenConstraint= this.objectiveValueAtBestUnconstrainedVertex;
             
        }else {
            //must use CPLEX to find the best vertex
            CplexBased_BestVertexFinder bestVertex_Finder = 
                    new CplexBased_BestVertexFinder (  ubc,  this. zeroFixedVariables,  this. oneFixedVariables);
            
            List<Boolean> isVarZeroFixedAtOrigin = new ArrayList <Boolean> ();
            boolean isSolvedToOptimality = bestVertex_Finder.getVarFixingsAtOrigin(isVarZeroFixedAtOrigin);
              
            if (isSolvedToOptimality) {
                //we have a best vertex 
                //note that we only care for the vars that are in the reduced constraint
                List <String> zero_Fixings = new ArrayList <String>();
                List <String> one_Fixings  = new ArrayList <String>();
                for (int index = ZERO; index < ubc.sortedConstraintExpr.size(); index ++) {
                    String thisVarName = ubc.sortedConstraintExpr.get(index).varName;
                    if (isVarZeroFixedAtOrigin.get(index)){
                        zero_Fixings.add( thisVarName);
                    }else {
                        one_Fixings.add(thisVarName );
                    }
                }
                
                bestVertex_zeroFixedVariables_forGivenConstraint.addAll(zero_Fixings );
                bestVertex_oneFixedVariables_forGivenConstraint.addAll(one_Fixings) ;
                objectiveValueAtBestVertex_forGivenConstraint=  bestVertex_Finder.valueAtBestVertex;
            } 
        }
        
        return objectiveValueAtBestVertex_forGivenConstraint;
    }
    
    public boolean isVarZeroAtBestVertex_forGivenConstraint (String var) {
        return this.bestVertex_zeroFixedVariables_forGivenConstraint.contains(var);
    }
    
    //finds     vars fixings to get  best unconstrained vertex
    //return objective value at this vertex
    private Double find_Unconstrained_BestVertex () {
        
        objectiveValueAtBestUnconstrainedVertex = DOUBLE_ZERO;
        
        for (VariableCoefficientTuple tuple : TestDriver.objective.objectiveExpr){
            String thisVar = tuple.varName;
            if (this.zeroFixedVariables.contains(thisVar) || this.oneFixedVariables.contains(thisVar )) {
                //already fixed, do nothing
                if (this.oneFixedVariables.contains(thisVar )) objectiveValueAtBestUnconstrainedVertex+=tuple.coeff;
            }else {
                //choose fixing so that objective becomes lowest possible
                if (tuple.coeff < ZERO){
                    unconstrained_bestVertex_oneFixedVariables.add(thisVar);
                    objectiveValueAtBestUnconstrainedVertex+=tuple.coeff;
                } else {
                    unconstrained_bestVertex_zeroFixedVariables.add(thisVar);
                }
            }
        }
         
        return objectiveValueAtBestUnconstrainedVertex;
    }
        
}
