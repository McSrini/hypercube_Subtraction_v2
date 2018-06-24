/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_v2.collection;
  
import static ca.mcmaster.hypercube_subtraction_v2.Constants.*;
import static ca.mcmaster.hypercube_subtraction_v2.Parameters.USE_STRICT_INEQUALITY_IN_MIP;
import ca.mcmaster.hypercube_subtraction_v2.collection.cplex.CplexBased_BestVertexFinder;
import ca.mcmaster.hypercube_subtraction_v2.common.*;
import ilog.concert.IloException;
import java.util.*;


/**
 *
 * @author tamvadss
 * 
collect infeasible hypercubes that are better LP than parameterized threshold
also collect hypercubes for every constraint, not just those invalidating best vertex
 */
public class RectangleCollector {
    
    //leaf for which we collect infeasible hypercubes
    //in this project, we only collect infeasible hypercubes for the MIP root
    //
    private LeafNode leaf;
     
   
    public RectangleCollector (LeafNode leaf  ) {
        this. leaf=leaf      ;
        
        leaf.findVarFixingsAtBestVertex();
    }  
    

    public List<Rectangle>  collect_INFeasibleHyperCubes(LowerBoundConstraint lbc) throws IloException{
        
        List<Rectangle> result = new ArrayList<Rectangle> ();
                 
        //find the best hypercubes starting from the best vertex
        Rectangle bestVertex = findBestVertex (lbc) ;
        
        if (bestVertex!=null) {
            bestVertex.printMe(lbc.name);
            //collect best infeasible hypercubes starting from this best vertex
        }
        
         
        return result;
    }
    
    private Rectangle   findBestVertex (LowerBoundConstraint lbc) throws IloException {
        
        Rectangle result = null;
        
        //this is the ubc for which we will collect the best feasible hypercubes    
        UpperBoundConstraint ubc  = new UpperBoundConstraint(  
                                        lbc.getReducedConstraint(leaf.zeroFixedVariables, leaf.oneFixedVariables)) ;
          
        
        //first check if leaf's best vertex is infeasible to the lbc , if yes then that is the best vertex
        if ( leaf.isConstraintViolatedAtBestVertex(lbc)) {
            
            //best vertex for this LBC 
            List <String> zeroFixings = new ArrayList <String>();
            List <String> oneFixings  = new ArrayList <String>();
            zeroFixings.addAll(  leaf.zeroFixedVariables);
            zeroFixings.addAll( leaf.bestVertex_zeroFixedVariables);
            oneFixings.addAll( leaf.oneFixedVariables );
            oneFixings.addAll(  leaf.bestVertex_oneFixedVariables);
            result = new Rectangle (zeroFixings,oneFixings ) ;
             
        }else {
            //must use CPLEX to find the best vertex
            CplexBased_BestVertexFinder bestVertex_Finder = 
                    new CplexBased_BestVertexFinder (  ubc,  leaf. zeroFixedVariables,  leaf. oneFixedVariables);
            
            List<Boolean> isVarZeroFixedAtOrigin = new ArrayList <Boolean> ();
            boolean isSolvedToOptimality = bestVertex_Finder.getVarFixingsAtOrigin(isVarZeroFixedAtOrigin);
              
            if (isSolvedToOptimality) {
                //we have a best vertex, convert it to rectangle and return it
                //note that we do not care for the vars that are missing from the reduced constraint
                List <String> zeroFixings = new ArrayList <String>();
                List <String> oneFixings  = new ArrayList <String>();
                for (int index = ZERO; index < ubc.sortedConstraintExpr.size(); index ++) {
                    String thisVarName = ubc.sortedConstraintExpr.get(index).varName;
                    if (isVarZeroFixedAtOrigin.get(index)){
                        zeroFixings.add( thisVarName);
                    }else {
                        oneFixings.add(thisVarName );
                    }
                }
                result = new Rectangle (zeroFixings,oneFixings ) ;
            }
        }
        
        return result;
    }
}

