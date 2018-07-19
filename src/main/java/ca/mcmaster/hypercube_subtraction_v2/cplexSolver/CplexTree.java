/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_v2.cplexSolver;
  
import static ca.mcmaster.hypercube_subtraction_v2.Constants.*;
import static ca.mcmaster.hypercube_subtraction_v2.Parameters.DISABLE_CUTS;
import static ca.mcmaster.hypercube_subtraction_v2.Parameters.DISABLE_HEURISTICS;
import static ca.mcmaster.hypercube_subtraction_v2.Parameters.DISABLE_PRESOLVE;
import static ca.mcmaster.hypercube_subtraction_v2.Parameters.DISABLE_PRESOLVENODE;
import static ca.mcmaster.hypercube_subtraction_v2.Parameters.DISABLE_PROBING;
import static ca.mcmaster.hypercube_subtraction_v2.Parameters.LOGGING_LEVEL;
import static ca.mcmaster.hypercube_subtraction_v2.Parameters.MAX_THREADS;
import static ca.mcmaster.hypercube_subtraction_v2.Parameters.MIP_EMPHASIS;
import static ca.mcmaster.hypercube_subtraction_v2.Parameters.MIP_FILENAME;  
import static ca.mcmaster.hypercube_subtraction_v2.Parameters.USE_PURE_CPLEX;
import ca.mcmaster.hypercube_subtraction_v2.collection.Rectangle;
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;  
import ilog.cplex.IloCplex.BranchCallback;
import static ilog.cplex.IloCplex.IncumbentId;
import ilog.cplex.IloCplex.Status;
import static java.lang.System.exit;
import java.util.List;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 */
public class CplexTree {
    
    private IloCplex cplex ;
    private HypercubeRampupCallback rampUpCallback ;
        
    private static Logger logger=Logger.getLogger(CplexTree.class);
    static {
        logger.setLevel(LOGGING_LEVEL );
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            RollingFileAppender rfa =new  RollingFileAppender(layout,LOG_FOLDER+CplexTree.class.getSimpleName()+ LOG_FILE_EXTENSION);
            rfa.setMaxBackupIndex(SIXTY);
            logger.addAppender(rfa);
            logger.setAdditivity(false);
        } catch (Exception ex) {
            ///
            System.err.println("Exit: unable to initialize logging"+ex);       
            exit(ONE);
        }
    } 
        
    public CplexTree () throws IloException {
        cplex = new IloCplex() ;
        cplex.importModel(MIP_FILENAME);
        
        if (DISABLE_PRESOLVENODE) cplex.setParam(IloCplex.Param.MIP.Strategy.PresolveNode, -ONE);
        if (DISABLE_PROBING) cplex.setParam(IloCplex.Param.MIP.Strategy.Probe, -ONE);
        cplex.setParam(IloCplex.Param.Emphasis.MIP,  MIP_EMPHASIS);
        if (DISABLE_HEURISTICS) cplex.setParam( IloCplex.Param.MIP.Strategy.HeuristicFreq , -ONE);
        if (DISABLE_CUTS) cplex.setParam(IloCplex.Param.MIP.Limits.CutPasses, -ONE);
        if (DISABLE_PRESOLVE) cplex.setParam(IloCplex.Param.Preprocessing.Presolve, false);
        cplex.setParam(IloCplex.Param.MIP.Strategy.File, THREE);         

    }
    
    //ramp up is always single threaded
    public void rampUp (int durationSeconds, boolean useHypercubes , List<Rectangle> infeasibleHypercubesList ) throws IloException{
        
        logger.info("Ramp up started");
        cplex.clearCallbacks();
        cplex.use(  new EmptyBranchCallback());  
        if (useHypercubes){
            IloLPMatrix lpMatrix = (IloLPMatrix)cplex.LPMatrixIterator().next();
            this.rampUpCallback= new HypercubeRampupCallback  ( lpMatrix.getNumVars(), infeasibleHypercubesList) ;
            cplex.use (rampUpCallback);
        }
                
        cplex.setParam( IloCplex.Param.Threads, ONE);
        cplex.setParam( IloCplex.Param.TimeLimit,  durationSeconds);
        cplex.solve();
        
        printStatictics("Ramp up complete");
        if (useHypercubes){
            logger.info( "Number of branches made during ramp up using the hypercubes method " + this.rampUpCallback.totalNumberOFBranches);
        }
        logger.info("Ramp up ended");
    }
    
    
    //solve for a few minutes and print stats
    public boolean solveForDuration (int durationSeconds , int iterationNumber) throws IloException {
        
        boolean isCompletelySolved = true;
       
        cplex.setParam( IloCplex.Param.TimeLimit,  durationSeconds);
        //use cplex default branching
        cplex.clearCallbacks();
        cplex.use (new EmptyBranchCallback());
        cplex.setParam( IloCplex.Param.Threads, MAX_THREADS);
        cplex.solve();
        if (cplex.getStatus().equals(Status.Optimal)){
            //print solution
            this.printSolution();
        }else if (cplex.getStatus().equals(Status.Infeasible)) {
            logger.info("MIP is infeasible" );
        }else {
            printStatictics(" Iteration number "+iterationNumber);
            isCompletelySolved = false;
        }
        
        return isCompletelySolved;
    }
    
    public Status getStatus () throws IloException {
        return cplex.getStatus();
    }
    
    public void printSolution () throws IloException {
        logger.info ("best known solution is " + cplex.getObjValue()) ;
        logger.info ("status is " + cplex.getStatus()) ;
        IloLPMatrix lpMatrix = (IloLPMatrix)cplex.LPMatrixIterator().next();
        for (IloNumVar var :  lpMatrix.getNumVars()) {            
             logger.info ("var is " + var.getName() + " and is soln value is " + cplex.getValue(var, IncumbentId )) ;
        }
    }
    
    private void printStatictics (String header) throws IloException {
        //print stats
        StaticticsBranchCallback statisticsCallback =new StaticticsBranchCallback();
        //cplex.clearCallbacks(); keep the in use branch callback if CPLEX wants to branch something before entering the node callback
        cplex.use(statisticsCallback );  
        //always set thread count to 1 before collecting statistics
        cplex.setParam( IloCplex.Param.Threads, ONE);
        cplex.solve();
        logger.info(header+ 
                " best known solution "+ statisticsCallback.bestKnownSOlution+ 
                " best bound "+ statisticsCallback.bestKnownBound+ 
                " number of leafs  reamining "+ statisticsCallback.numberOFLeafs+ 
                " number of nodes processed   "+ statisticsCallback.numberOFNodesProcessed) ;
        
    }
   
}

