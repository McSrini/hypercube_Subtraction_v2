/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_v2.collection;

import static ca.mcmaster.hypercube_subtraction_v2.Constants.LOG_FILE_EXTENSION;
import static ca.mcmaster.hypercube_subtraction_v2.Constants.LOG_FOLDER;
import static ca.mcmaster.hypercube_subtraction_v2.Constants.ONE;
import static ca.mcmaster.hypercube_subtraction_v2.Constants.SIXTY;
import static ca.mcmaster.hypercube_subtraction_v2.Constants.ZERO;
import static ca.mcmaster.hypercube_subtraction_v2.Parameters.HYPERCUBE_COLLECTION_COUNT_THRESHOLD;
import static ca.mcmaster.hypercube_subtraction_v2.Parameters.HYPERCUBE_COLLECTION_LP_THRESHOLD;
import static ca.mcmaster.hypercube_subtraction_v2.Parameters.LOGGING_LEVEL;
import ca.mcmaster.hypercube_subtraction_v2.common.LowerBoundConstraint;
import ca.mcmaster.hypercube_subtraction_v2.common.UpperBoundConstraint;
import ilog.concert.IloException;
import static java.lang.System.exit;
import java.util.Collections;
import java.util.List;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 */
public class ConstraintAnchor_RectangleCollector extends Base_RectangleCollector {
 
    private static Logger logger=Logger.getLogger(ConstraintAnchor_RectangleCollector.class);
    static {
        logger.setLevel(LOGGING_LEVEL);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            RollingFileAppender appender = new  RollingFileAppender(layout,LOG_FOLDER+ConstraintAnchor_RectangleCollector.class.getSimpleName()+ LOG_FILE_EXTENSION);
            appender.setMaxBackupIndex(SIXTY);
            logger.addAppender(appender);
             
            logger.setAdditivity(false);
        } catch (Exception ex) {
            ///
            System.err.println("Exit: unable to initialize logging"+ex);       
            exit(ONE);
        }
    } 
   
    public ConstraintAnchor_RectangleCollector (   ) {
        reset();
    } 
    
 
    public void collect_INFeasibleHyperCubes(LowerBoundConstraint lbc) throws IloException {
        while (! pendingJobs.isEmpty() && HYPERCUBE_COLLECTION_COUNT_THRESHOLD >getNumberOfHypercubesCollected() ){
            
            logger.debug ("Number of pending jobs "+getNumberOfPendingJobs()) ;
            
            //remove the best pending job
            //recall that value is -ve amount by which constraint is violated
            double bestValue = Collections.min( pendingJobs.keySet());
            logger.debug("best job "+bestValue) ;
            
            
            List<Rectangle> bestJobs = pendingJobs.get(bestValue);
            Rectangle job = bestJobs.remove(ZERO);
            if (bestJobs.size()==ZERO) {
                pendingJobs.remove(bestValue);
            }else {
                pendingJobs.put(bestValue, bestJobs);
            }
                        
            // find vertex most distant from the constraint plane
            Double bestVertexValueForThisConstraint=job.findMostViolatedVertex(lbc);
             
            if (bestVertexValueForThisConstraint!=null ) {
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
