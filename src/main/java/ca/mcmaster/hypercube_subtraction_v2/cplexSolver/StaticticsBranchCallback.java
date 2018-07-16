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
public   class StaticticsBranchCallback extends IloCplex.BranchCallback{ 

    //for statistics   
    public long numberOFLeafs = ZERO;
    public long numberOFNodesProcessed = ZERO;
    public double bestKnownSOlution ; 
    public double bestKnownBound ;
    
    protected void main() throws IloException {
         
        numberOFLeafs =getNremainingNodes64();
        numberOFNodesProcessed=getNnodes64();
        bestKnownSOlution = getIncumbentObjValue();
        bestKnownBound=getBestObjValue();
        abort();
    }
   
}
