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
            IloCplex mip =  new IloCplex();
            mip.importModel(MIP_FILENAME);
            mip.exportModel(MIP_FILENAME+ ".lp");
           
            allVariablesInModel = MIP_Reader.getVariables(mip) ;
            objective= MIP_Reader.getObjective(mip);            
            mipConstraintList= MIP_Reader.getConstraints(mip);

            logger.info ("Collected objective and constraints. Dumping parameters:" ) ;
            logger.info("MIP_FILENAME "+ MIP_FILENAME) ;
            //public static final String MIP_FILENAME = "nvmxnnmnm,mncmn , ,mnmvc,vmcnishani.mps";
            logger.info("HYPERCUBE_COLLECTION_LP_THRESHOLD "+ HYPERCUBE_COLLECTION_LP_THRESHOLD) ;
            logger.info("HYPERCUBE_COLLECTION_COUNT_THRESHOLD "+ HYPERCUBE_COLLECTION_COUNT_THRESHOLD) ;
            logger.info("RAMP_UP_FOR_THIS_MANY_MINUTES "+RAMP_UP_FOR_THIS_MANY_MINUTES) ;  
            logger.info("USE_STRICT_INEQUALITY_IN_MIP "+USE_STRICT_INEQUALITY_IN_MIP) ; 
            logger.info("MIP_EMPHASIS "+MIP_EMPHASIS) ;
            logger.info("USE_PURE_CPLEX "+USE_PURE_CPLEX) ;
            logger.info("TOTAL_SOLUTION_TIME_LIMIT_SECONDS "+TOTAL_SOLUTION_TIME_LIMIT_SECONDS) ;
       
           
            
            CplexTree cplexRefTree = new CplexTree () ;
            cplexRefTree.solve();
             
        }catch (Exception ex){
            System.err.println(ex) ;
            System.err.println(ex.getMessage()) ;
            ex.printStackTrace();
            exit(ONE);
        }
        
    }//end main method
    
}
