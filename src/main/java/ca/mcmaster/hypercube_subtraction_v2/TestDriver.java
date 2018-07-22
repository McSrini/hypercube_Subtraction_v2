/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_v2;

import static ca.mcmaster.hypercube_subtraction_v2.Constants.*;
import static ca.mcmaster.hypercube_subtraction_v2.Parameters.*; 
import ca.mcmaster.hypercube_subtraction_v2.collection.Rectangle;
import ca.mcmaster.hypercube_subtraction_v2.collection.RectangleCollector;
import ca.mcmaster.hypercube_subtraction_v2.common.*;
import ca.mcmaster.hypercube_subtraction_v2.cplexSolver.CplexTree;
import ca.mcmaster.hypercube_subtraction_v2.merge.RectangleMerger;
import ca.mcmaster.hypercube_subtraction_v2.utils.MIP_Reader;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.Status;
import static java.lang.System.exit;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 */
public class TestDriver {
         
    private static Logger logger=Logger.getLogger(TestDriver.class);
    
    //constraints in this mip
    public  static List<LowerBoundConstraint> mipConstraintList ;
    public static  Objective objective ;
    public static  List<String> allVariablesInModel ;
    
    static {
        logger.setLevel(LOGGING_LEVEL);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+TestDriver.class.getSimpleName()+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
        } catch (Exception ex) {
            ///
            System.err.println("Exit: unable to initialize logging"+ex);       
            exit(ONE);
        }
    }
    
    public static void main(String[] args) throws Exception {
                 
        try {
            logger.info ("Start !" );
            IloCplex mip =  new IloCplex();
            mip.importModel(MIP_FILENAME);
            mip.exportModel(MIP_FILENAME+ ".lp");
           
            allVariablesInModel = MIP_Reader.getVariables(mip) ;
            objective= MIP_Reader.getObjective(mip);            
            mipConstraintList= MIP_Reader.getConstraints(mip);
            
            //check that every var appears in the objective 
            for (String var :allVariablesInModel){
                if (ZERO==objective.getObjectiveCoeff(var)) {
                    System.err.println("Variable "+ var + " does not occur in the objective.");
                    exit(ONE);
                }
            }

            logger.info ("Collected objective and constraints. Dumping parameters:" ) ;
            logger.info("MIP_FILENAME "+ MIP_FILENAME) ;
            //public static final String MIP_FILENAME = "nvmxnnmnm,mncmn , ,mnmvc,vmcnishani.mps";
            logger.info("HYPERCUBE_COLLECTION_LP_THRESHOLD "+ HYPERCUBE_COLLECTION_LP_THRESHOLD) ;
            logger.info("HYPERCUBE_COLLECTION_COUNT_THRESHOLD "+ HYPERCUBE_COLLECTION_COUNT_THRESHOLD) ;
            logger.info("RAMP_UP_FOR_THIS_MANY_MINUTES "+RAMP_UP_FOR_THIS_MANY_HOURS) ;  
            logger.info("USE_STRICT_INEQUALITY_IN_MIP "+USE_STRICT_INEQUALITY_IN_MIP) ; 
            logger.info("MIP_EMPHASIS "+MIP_EMPHASIS) ;
            logger.info("USE_PURE_CPLEX "+USE_PURE_CPLEX) ;
            logger.info("USE_ABSORB_AND_MERGE "+USE_MERGE_AND_ABSORB) ; 
            logger.info("SOLUTION_DURATION_HOURS_BEFORE_LOGGING_STATITICS "+SOLUTION_DURATION_HOURS_BEFORE_LOGGING_STATITICS );
            logger.info("TOTAL_SOLUTION_ITERATIONS "+TOTAL_SOLUTION_ITERATIONS);
            
            logger.info(" Cplex config");
            logger.info("DISABLE_PRESOLVENODE , DISABLE_PRESOLVE ,DISABLE_CUTS ,DISABLE_HEURISTICS,DISABLE_PROBING "+
                    DISABLE_PRESOLVENODE +" "+ DISABLE_PRESOLVE +" "+ DISABLE_CUTS +" "+ DISABLE_HEURISTICS+" "+ DISABLE_PROBING );
            
           
            List<Rectangle> infeasibleHypercubesList = new ArrayList<Rectangle>();
            if (!USE_PURE_CPLEX){
                //collect infeasible hypercubes
                logger.info("start hypercube collection");
                 
                RectangleCollector collector =   new RectangleCollector( );   
               
                for ( LowerBoundConstraint lbc :  TestDriver.mipConstraintList){
                    collector.reset();
                    collector.collect_INFeasibleHyperCubes(lbc);
                    
                    if (USE_MERGE_AND_ABSORB && collector.collectedHypercubes.size()>ZERO){   
                       infeasibleHypercubesList = mergeAndAbsorb (   infeasibleHypercubesList, collector.collectedHypercubes);                        
                    } else {
                       infeasibleHypercubesList .addAll(collector.collectedHypercubes);   
                    }
                                        
                    logger.debug ("for cosntarint " + lbc.name + " collected this many infeasible hypercubes " + collector.collectedHypercubes.size()
                    + " and infeasibleHypercubesList size "+infeasibleHypercubesList.size());
                }
                                
                logger.info("end hypercube collection. Collected this many "+infeasibleHypercubesList.size() );
                print_Largest_and_Smallest_BestVertexValue(infeasibleHypercubesList);
            }
            
            CplexTree cplexRefTree = new CplexTree () ;
            cplexRefTree.rampUp(RAMP_UP_FOR_THIS_MANY_HOURS, !USE_PURE_CPLEX,infeasibleHypercubesList  );
            boolean isCompletelySolved = false;
            for (int solutionIteration = ONE; solutionIteration<= TOTAL_SOLUTION_ITERATIONS; solutionIteration++) {
                isCompletelySolved = cplexRefTree.solveForDuration( SOLUTION_DURATION_HOURS_BEFORE_LOGGING_STATITICS *SIXTY*SIXTY  ,solutionIteration );
                if (isCompletelySolved)  break;
            }
            if (!isCompletelySolved && cplexRefTree.getStatus().equals(Status.Feasible)){
                //print best known solution
                logger.info("not fully solved to optimality in the time available, but feasible") ;
                cplexRefTree.printSolution();
            }
            logger.info("Done !" );
             
        }catch (Exception ex){
            System.err.println(ex) ;
            System.err.println(ex.getMessage()) ;
            ex.printStackTrace();
            exit(ONE);
        }
        
    }//end main method
    
    private static void print_Largest_and_Smallest_BestVertexValue ( List<Rectangle> infeasibleHypercubesList) {
        double largest = - Double.MAX_VALUE;
        double smallest =   Double.MAX_VALUE;
        for (Rectangle rect: infeasibleHypercubesList){
            if (rect.objectiveValueAtBestUnconstrainedVertex>largest) {
                largest = rect.objectiveValueAtBestUnconstrainedVertex;
            }
            if ( rect.objectiveValueAtBestUnconstrainedVertex< smallest) {
                smallest =    rect.objectiveValueAtBestUnconstrainedVertex;
            }
        }
        logger.info ( " largest best vertex value is "+ largest);
        logger.info ( " smallest best vertex value is "+ smallest);
    }
    
    private static  List<Rectangle> mergeAndAbsorb ( List<Rectangle> existingList, List<Rectangle> incomingList) {
        List<Rectangle> result = null;
         
        //merge and absorb hypercubes
        //logger.info("merge and absorb start ...") ;
        RectangleMerger merger = new RectangleMerger (existingList, incomingList) ;
        result= merger.absorbAndMerge( ) ;
        //logger.info("merge and absorb completed ! ") ;

        if( merger.isMIP_Infeasible) {
            System.out.println("MIP is unfeasible; no need for branching") ;
            exit(ZERO);
        }         
        return result;
    }
    
}
