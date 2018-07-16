/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_v2;

import static ca.mcmaster.hypercube_subtraction_v2.Constants.*;
import ilog.cplex.IloCplex;
import org.apache.log4j.Level;

/**
 *
 * @author tamvadss
 */
public class Parameters {
        
    //public static final String MIP_FILENAME = "F:\\temporary files here\\neos-807456.mps";  ////x1 x7  x4  x2
    //public static final String MIP_FILENAME = "F:\\temporary files here\\knapsackTinyInfeasible.lp";  ////x1 x7  x4  x2
    //public static final String MIP_FILENAME = "F:\\temporary files here\\knapsackSmall.lp";  ////x1 x7  x4  x2
    //public static final String MIP_FILENAME = "F:\\temporary files here\\knapsackFourTest.lp"; 
    //public static final String MIP_FILENAME = "F:\\temporary files here\\opm2-z12-s7.mps";
    //public static final String MIP_FILENAME = "F:\\temporary files here\\opm2-z12-s14.mps";
    //public static final String MIP_FILENAME = "F:\\temporary files here\\bab5.mps";
    //public static final String MIP_FILENAME = "F:\\temporary files here\\protfold.mps";
    //public static final String MIP_FILENAME = "F:\\temporary files here\\harp2.mps";
    //public static final String MIP_FILENAME = "F:\\temporary files here\\cov1075.mps";
    //public static final String MIP_FILENAME = "F:\\temporary files here\\p6b.mps";
    // public static final String MIP_FILENAME = "F:\\temporary files here\\seymour-disj-10.mps";
    //public static final String MIP_FILENAME = "harp2.mps";
    //public static final String MIP_FILENAME = "wnq.mps";
    //public static final String MIP_FILENAME = "neos-807456.mps";
    //public static final String MIP_FILENAME = "stp3d.mps";
    //public static final String MIP_FILENAME = "seymour-disj-10.mps";
    //public static final String MIP_FILENAME = "p6b.mps";
    //public static final String MIP_FILENAME = "protfold.mps";
    //public static final String MIP_FILENAME ="opm2-z12-s7.mps";
    public static final String MIP_FILENAME =        "opm2-z12-s14.mps";
    //public static final String MIP_FILENAME = "cov1075.mps";
    //public static final String MIP_FILENAME = "nvmxnnmnm,mncmn , ,mnmvc,vmcnishani.mps";
    
    //for minimization problems , collect infeasible hypercubes whose LP is strictly lower (i.e better) than this
    public static final double HYPERCUBE_COLLECTION_LP_THRESHOLD = -31;
    //at most 10 hypercubes per constraint
    public static final int HYPERCUBE_COLLECTION_COUNT_THRESHOLD = 1000;
    
    //our method is used to branch only for the first few leafs
    public static final int RAMP_UP_FOR_THIS_MANY_MINUTES   = 60; //5, 15, 30, 60, 120  ;
    public static final int SOLUTION_DURATION_HOURS_BEFORE_LOGGING_STATITICS= 1;
    public static int TOTAL_SOLUTION_ITERATIONS =  24*14 ; //14 days of logging
        
    public static final boolean USE_PURE_CPLEX =  true;
    public static final boolean USE_ABSORB_AND_MERGE =  false;
    
    public static boolean USE_STRICT_INEQUALITY_IN_MIP = false;
    public static final Level LOGGING_LEVEL= Level.INFO ;    
    public static final int MIP_EMPHASIS= ZERO;
    public static final int MAX_THREADS= 32;
    
    public static final boolean DISABLE_PRESOLVENODE = true;
    public static final boolean DISABLE_PRESOLVE = true;
    public static final boolean DISABLE_CUTS = true;
    public static final boolean DISABLE_HEURISTICS= true; 
    public static final boolean DISABLE_PROBING= true; 

}
