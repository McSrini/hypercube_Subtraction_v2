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
public class RectangleCollector {
     
    
    //here are the nodes which are by products of creating the feasible node. These need to be
    //decomposed further to get more feasible nodes
    private    Map<Double, List<Rectangle>  > pendingJobs = new TreeMap <Double, List<Rectangle>>  ();  
     
    public     List<Rectangle>   collectedHypercubes = new ArrayList<Rectangle>  ();  
    
    private static Logger logger=Logger.getLogger(RectangleCollector.class);
    static {
        logger.setLevel(LOGGING_LEVEL);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            RollingFileAppender appender = new  RollingFileAppender(layout,LOG_FOLDER+RectangleCollector.class.getSimpleName()+ LOG_FILE_EXTENSION);
            appender.setMaxBackupIndex(SIXTY);
            logger.addAppender(appender);
             
            logger.setAdditivity(false);
        } catch (Exception ex) {
            ///
            System.err.println("Exit: unable to initialize logging"+ex);       
            exit(ONE);
        }
    } 
   
    public RectangleCollector (   ) {
        reset();
    }  
    
    public void reset () {
        this.pendingJobs.clear();
        this.collectedHypercubes.clear();
        //in this project, we only collect infeasible hypercubes starting from the MIP root
        Rectangle mipRoot = new  Rectangle(new ArrayList <String>(), new ArrayList <String>()) ;         
        addPendingJob(mipRoot );
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
            logger.debug("best job is "+job.printMe( lbc.name)) ;
            
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
    
    
    
    private int getNumberOfHypercubesCollected () {
         
        return this.collectedHypercubes.size();
    }
    
    private int getNumberOfPendingJobs() {
        int size = ZERO;
        for ( Collection<Rectangle> rects:this.pendingJobs.values() ) {
            size += rects.size();
        }
        return size;
    }
    
    //add into cube colletion, key is best vertex value of the LBC for which this cube was collected
    private void addCollectedHypercube (Rectangle cube ) {
        
        this.collectedHypercubes.add(cube);
         
    }
    
    private void addPendingJob (Rectangle job )  {
        
        List<Rectangle> rects= this.pendingJobs.get(job.objectiveValueAtBestUnconstrainedVertex);
        if (rects==null) rects =new ArrayList<Rectangle> ();
        rects.add(job) ;
        this.pendingJobs.put( job.objectiveValueAtBestUnconstrainedVertex, rects);
        
    }
 
    //for a given rect and constraint, get slack at best vertex  for this constraint
    //job must already have its best vertex determined for this constraint
    //ubc is the reduced constarint
    private double getSlack ( UpperBoundConstraint ubc, Rectangle job ) {
        UpperBoundConstraint reduced=ubc.getReducedConstraint( job.bestVertex_zeroFixedVariables_forGivenConstraint, job.bestVertex_oneFixedVariables_forGivenConstraint);
        return reduced.upperBound;
    }
    
    //collect 1 feasible hypercube and insert it into collected queue, and insert any resulting pending jobs into pending queue    
    private void collectOneHyperCube (Rectangle job, LowerBoundConstraint lbc)  throws IloException{
         
        //this is the UBC we will use
        UpperBoundConstraint reducedConstraint = new UpperBoundConstraint( lbc.getReducedConstraint(job.zeroFixedVariables, job.oneFixedVariables));
        logger.debug(reducedConstraint.printMe()) ;
        
        if (reducedConstraint.isGauranteedFeasible(! USE_STRICT_INEQUALITY_IN_MIP)) {
            //collect the whole hypercube, and no remaining pending jobs
            Rectangle wholeRect =  new Rectangle (job.zeroFixedVariables,  job.oneFixedVariables) ;
            this.addCollectedHypercube( wholeRect );
            logger.debug("Collected full hypercube "+wholeRect.printMe(lbc.name)) ;
        } else {
            
            //flip vars to collect the best hypercube and generate pending jobs
            
            //find the slack in this constraint at this best vertex
            final double SLACK =  getSlack(reducedConstraint, job);
            logger.debug("Slack is " + SLACK) ;
            //delta keeps track of how much we have reduced the slack by flipping vars
            double delta =ZERO;
            List<Integer> indexOfVarsWhichCanBeFree = new ArrayList<Integer> () ;
            //start flipping vars
            for (int index = ZERO; index < reducedConstraint.sortedConstraintExpr.size(); index ++){   
                VariableCoefficientTuple tuple= reducedConstraint.sortedConstraintExpr.get(index);

                //if this var is already at its best value for increasing the slack, do not flip it
                //in other words, if flipping it is going to increase the slack then do not flip
                boolean isFlipCandidate = tuple.coeff>ZERO &&  job.isVarZeroAtBestVertex_forGivenConstraint(tuple.varName);
                isFlipCandidate= isFlipCandidate ||(tuple.coeff<ZERO && ! job.isVarZeroAtBestVertex_forGivenConstraint(tuple.varName));
                  
                //if var is flip candidate, see if we have enough slack left, if yes flip it
                boolean isEnoughSlackLeft = !USE_STRICT_INEQUALITY_IN_MIP && (SLACK > (delta +Math.abs(tuple.coeff)));
                isEnoughSlackLeft = isEnoughSlackLeft || 
                                        ( USE_STRICT_INEQUALITY_IN_MIP && (SLACK >= (delta +Math.abs(tuple.coeff))) ) ;
                
                if (isFlipCandidate && isEnoughSlackLeft) {
                    //flip var
                    
                    delta += Math.abs(tuple.coeff);
                    logger.debug("can flip var "+tuple.varName + " coeff is " +tuple.coeff +" and remaining slack is "+ (SLACK-delta) ) ;
                    indexOfVarsWhichCanBeFree.add(index);
                }
                    
            }//end for
            
            //we now know the vars which are free to take on either value
            Rectangle collectedCube  = createFeasibleRectangle (job ,indexOfVarsWhichCanBeFree , reducedConstraint) ;
            this.addCollectedHypercube(collectedCube );
            logger.debug("collectedCube "+collectedCube.printMe(lbc.name)) ;
            
            List<Rectangle> newJobs = createMoreJobs  (job,  indexOfVarsWhichCanBeFree , reducedConstraint) ;
            for (Rectangle newJob : newJobs){
                this.addPendingJob(newJob  );
                logger.debug("add pending job "+newJob.printMe(lbc.name));
            }
            
        }//end else        
        
    }//end method collect one hypercube

    private List<Rectangle> createMoreJobs (Rectangle job ,List<Integer> indexOfVarsWhichCanBeFree , UpperBoundConstraint reducedConstraint) {
        List<Rectangle> newJobs = new ArrayList<Rectangle>() ;
        
        List<VariableCoefficientTuple> varsWhichAre_Not_Free = new ArrayList<VariableCoefficientTuple> ();
        for (int index = ZERO; index < reducedConstraint.sortedConstraintExpr.size(); index ++){   
            if (! indexOfVarsWhichCanBeFree.contains(index)) {
                VariableCoefficientTuple tuple= reducedConstraint.sortedConstraintExpr.get(index);
                boolean isZeroFix = job.isVarZeroAtBestVertex_forGivenConstraint(tuple.varName);
                VariableCoefficientTuple thisVarFixing = new VariableCoefficientTuple (reducedConstraint.sortedConstraintExpr.get(index).varName,
                                                                                       isZeroFix ? ZERO: ONE);
                varsWhichAre_Not_Free.add(thisVarFixing);
            }
        }
        
        //for the j-th fixed var, create a job with (j-1) vars fixed at their fixing, but the j-th one flipped
        for ( int jj = ZERO; jj < varsWhichAre_Not_Free.size(); jj ++) {
            VariableCoefficientTuple fixingTuple= varsWhichAre_Not_Free.get(jj);
            List<String> zero_Fixings = new ArrayList<> ( );
            List<String> one_Fixings = new ArrayList<> ( );
            if (fixingTuple.coeff==ONE) {
                //note the flip
                zero_Fixings.add(fixingTuple.varName);
            }else {
                //note the flip
                one_Fixings.add(fixingTuple.varName);
            }
            for (int ii = ZERO ; ii < jj ; ii++) {
                VariableCoefficientTuple previousTuple= varsWhichAre_Not_Free.get(ii);
                if (previousTuple.coeff==ZERO) {
                    zero_Fixings.add(previousTuple.varName);
                }else {
                    one_Fixings.add(previousTuple.varName);
                }
            }
            
            //create the new job corresponding to the jj-th fixing
            zero_Fixings.addAll(job.zeroFixedVariables) ;
            one_Fixings.addAll(job.oneFixedVariables );
            Rectangle newJob = new Rectangle (zero_Fixings, one_Fixings) ;
            newJobs.add(newJob );
        }
        
        return newJobs;
    }
    
    //create feasible hypercube which will be collected for the UBC
    //job must already have its best vertex determined for this constraint
    private Rectangle createFeasibleRectangle (Rectangle job ,List<Integer> indexOfVarsWhichCanBeFree , UpperBoundConstraint reducedConstraint) {
        
        List<String> zero_fixings = new ArrayList<String> () ;
        List<String> one_fixings = new ArrayList<String> () ;
        zero_fixings.addAll(job.zeroFixedVariables) ;
        one_fixings.addAll(job.oneFixedVariables );
        for (int index = ZERO; index < reducedConstraint.sortedConstraintExpr.size(); index ++){   
            if (! indexOfVarsWhichCanBeFree.contains(index)) {
                //fix this var at its value at the best vertex
                VariableCoefficientTuple tuple= reducedConstraint.sortedConstraintExpr.get(index);
                boolean isZeroFix = job.isVarZeroAtBestVertex_forGivenConstraint(tuple.varName);
                if (isZeroFix) {
                    zero_fixings.add( tuple.varName);
                }else {
                    one_fixings.add(tuple.varName );
                }
            }  
        }
        
        return new Rectangle (zero_fixings,  one_fixings) ;
    }
    
    
    
   
    
}

