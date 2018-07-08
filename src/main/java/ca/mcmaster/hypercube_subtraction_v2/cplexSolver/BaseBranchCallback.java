/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_v2.cplexSolver;

import static ca.mcmaster.hypercube_subtraction_v2.Constants.DOUBLE_ZERO;
import static ca.mcmaster.hypercube_subtraction_v2.Constants.LOG_FILE_EXTENSION;
import static ca.mcmaster.hypercube_subtraction_v2.Constants.LOG_FOLDER;
import static ca.mcmaster.hypercube_subtraction_v2.Constants.ONE;
import static ca.mcmaster.hypercube_subtraction_v2.Constants.SIXTY;
import static ca.mcmaster.hypercube_subtraction_v2.Constants.THOUSAND;
import static ca.mcmaster.hypercube_subtraction_v2.Constants.ZERO;
import static ca.mcmaster.hypercube_subtraction_v2.Parameters.BranchingStatictics_logging_Interval_minutes;
import static ca.mcmaster.hypercube_subtraction_v2.Parameters.LOGGING_LEVEL;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import static java.lang.System.exit;
import java.time.Duration;
import java.time.Instant;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 */
public class BaseBranchCallback extends IloCplex.BranchCallback{ 

   
    
    //for statistics
    public long  totalNumberOFBranches=ZERO;    
    public Instant solutionStartTime =null;
    
    
    public long numLeafsAfterRampup  = ZERO;
    public long numLeafsBranchedWithOurMethod = ZERO;
    public   double timeTakenForHypercubeCollection_seconds = DOUBLE_ZERO;
    public Instant rampupStartTime =null;
    
    private static Logger logger=Logger.getLogger(BaseBranchCallback.class);
    static {
        logger.setLevel(LOGGING_LEVEL);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+BaseBranchCallback.class.getSimpleName()+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
        } catch (Exception ex) {
            ///
            System.err.println("Exit: unable to initialize logging"+ex);       
            exit(ONE);
        }
    } 
    
    public BaseBranchCallback (){
         solutionStartTime = Instant.now();
    }
    
    @Override
    protected void main() throws IloException {
        if ( getNbranches()> ZERO ){  
            totalNumberOFBranches+=getNbranches();
        
            double timeSinceLastLog = Duration.between(  solutionStartTime, Instant.now()).toMillis();
            if (BranchingStatictics_logging_Interval_minutes * SIXTY*THOUSAND< timeSinceLastLog) {
                //log useful info
                logger.info( "best bound is " + getBestObjValue());
                logger.info ("number of leafs is " + getNremainingNodes64());
                try {
                    logger.info ("best known feasible solution is " + getIncumbentObjValue()) ;
                } catch (Exception ex) {
                    logger.info("no solution as of yet");
                }

                //reset 
                solutionStartTime = Instant.now();
            }
        }
        
    }
    
}
