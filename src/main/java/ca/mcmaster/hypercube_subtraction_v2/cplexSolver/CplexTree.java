/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_v2.cplexSolver;
  
import static ca.mcmaster.hypercube_subtraction_v2.Constants.*;
import static ca.mcmaster.hypercube_subtraction_v2.Parameters.LOGGING_LEVEL;
import static ca.mcmaster.hypercube_subtraction_v2.Parameters.MIP_EMPHASIS;
import static ca.mcmaster.hypercube_subtraction_v2.Parameters.MIP_FILENAME; 
import static ca.mcmaster.hypercube_subtraction_v2.Parameters.TOTAL_SOLUTION_TIME_LIMIT_SECONDS;
import static ca.mcmaster.hypercube_subtraction_v2.Parameters.USE_PURE_CPLEX;
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.cplex.IloCplex;  
import static java.lang.System.exit;
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
    private BaseBranchCallback  branchingCallback= null;
    
       
    private static Logger logger=Logger.getLogger(CplexTree.class);
    static {
        logger.setLevel(LOGGING_LEVEL );
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+CplexTree.class.getSimpleName()+ LOG_FILE_EXTENSION));
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
        
        //cplex.setParam(IloCplex.Param.MIP.Strategy.PresolveNode, -ONE);
        //cplex.setParam(IloCplex.Param.MIP.Strategy.Probe, -ONE);
        cplex.setParam(IloCplex.Param.Emphasis.MIP,  MIP_EMPHASIS);
        cplex.setParam( IloCplex.Param.MIP.Strategy.HeuristicFreq , -ONE);
        //cplex.setParam(IloCplex.Param.MIP.Limits.CutPasses, -ONE);
        //cplex.setParam(IloCplex.Param.Preprocessing.Presolve, false);
        cplex.setParam(IloCplex.Param.MIP.Strategy.File, THREE);
        cplex.setParam( IloCplex.Param.TimeLimit, TOTAL_SOLUTION_TIME_LIMIT_SECONDS);
                
        IloLPMatrix lpMatrix = (IloLPMatrix)cplex.LPMatrixIterator().next();
        if (USE_PURE_CPLEX) {
            branchingCallback=new BaseBranchCallback();            
        } else {
            branchingCallback = new HypercubeBranchCallback  ( lpMatrix.getNumVars());             
        }
        cplex.use( branchingCallback);
        

    }
    
    public void solve () throws IloException {
        
       
        cplex.solve();
        //log statistics
        logger.info("numLeafsAfterRampup "+ branchingCallback.numLeafsAfterRampup );
        logger.info( "numLeafsBranchedWithOurMethod "+branchingCallback.numLeafsBranchedWithOurMethod );
        logger.info ("total number of branches up till solution " + branchingCallback.totalNumberOFBranches) ;
        logger.info ("Time for hypercube collection seconds "+branchingCallback.timeTakenForHypercubeCollection_seconds) ;
        logger.info ("best bound is " + cplex.getBestObjValue()) ;
        try {
            logger.info ("best known solution is " + cplex.getObjValue()) ;
        }catch (Exception ex) {
            logger.warn(ex) ;
            logger.info ("no known solution  "  ) ;
        }
          
    }
    
}
