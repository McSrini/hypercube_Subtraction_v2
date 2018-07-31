/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_v2.collection;
  
import static ca.mcmaster.hypercube_subtraction_v2.Constants.*;
import static ca.mcmaster.hypercube_subtraction_v2.Parameters.*;  
import ca.mcmaster.hypercube_subtraction_v2.common.*;
import ca.mcmaster.hypercube_subtraction_v2.cplexSolver.HypercubeRampupCallback;
import ilog.concert.IloException;
import static java.lang.System.exit;
import java.util.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;


/**
 *
 * @author tamvadss
 * 
collect infeasible hypercubes that are better LP than parameterized threshold
also collect hypercubes for every constraint, not just those invalidating best vertex
* 
* 
* 
 */
public class ObjectiveAnchor_RectangleCollector extends Base_RectangleCollector{
         
    private static Logger logger=Logger.getLogger(ObjectiveAnchor_RectangleCollector.class);
    static {
        logger.setLevel(LOGGING_LEVEL);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            RollingFileAppender appender = new  RollingFileAppender(layout,LOG_FOLDER+ObjectiveAnchor_RectangleCollector.class.getSimpleName()+ LOG_FILE_EXTENSION);
            appender.setMaxBackupIndex(SIXTY);
            logger.addAppender(appender);
             
            logger.setAdditivity(false);
        } catch (Exception ex) {
            ///
            System.err.println("Exit: unable to initialize logging"+ex);       
            exit(ONE);
        }
    } 
   
    public ObjectiveAnchor_RectangleCollector (   ) {
        reset();
    }  
    

    
    //return list of hypercubes whose best vertex objective value for this constraint is better than HYPERCUBE_COLLECTION_LP_THRESHOLD
    public void  collect_INFeasibleHyperCubes(LowerBoundConstraint lbc) throws IloException{
                 
        while (! pendingJobs.isEmpty() && HYPERCUBE_COLLECTION_COUNT_THRESHOLD >getNumberOfHypercubesCollected() ){
            
            logger.debug ("Number of pending jobs "+getNumberOfPendingJobs()) ;
            
            //remove the best pending job
            double bestValue = Collections.min( pendingJobs.keySet());
            logger.debug("best job "+bestValue) ;
            if (HYPERCUBE_COLLECTION_LP_THRESHOLD<= bestValue) {
                //end of collection
                logger.debug("  HYPERCUBE_COLLECTION_LP_THRESHOLD ended collection") ;
                break;
            } 
            
            List<Rectangle> bestJobs = pendingJobs.get(bestValue);
            Rectangle job = bestJobs.remove(ZERO);
            if (bestJobs.size()==ZERO) {
                pendingJobs.remove(bestValue);
            }else {
                pendingJobs.put(bestValue, bestJobs);
            }
                        
            //first find the best vertex for this job
            Double bestVertexValueForThisConstraint=job.findBestVertex(lbc);
            //logger.debug("best job is "+job.printMe( lbc.name)) ;
            
            if (bestVertexValueForThisConstraint!=null && HYPERCUBE_COLLECTION_LP_THRESHOLD> bestVertexValueForThisConstraint) {
                //collect one hypercube and insert any resulting pending jobs into pending queue
                collectOneHyperCube(job,lbc);
                logger.debug ("Collection size is "+ this.collectedHypercubes.size()) ;
            } else {
                //discard job
                logger.debug ("discarding inferior job" );
            }
            
        } //end while
        
        if (pendingJobs.isEmpty()) {
            logger.debug ("no more pending jobs" );
        }
    }
    

    

 
    
}

