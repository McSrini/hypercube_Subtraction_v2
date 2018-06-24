/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_v2;

/**
 *
 * @author tamvadss
 */
public class Parameters {
        
//public static final String MIP_FILENAME = "F:\\temporary files here\\neos-807456.mps";  ////x1 x7  x4  x2
    //public static final String MIP_FILENAME = "F:\\temporary files here\\knapsackTinyInfeasible.lp";  ////x1 x7  x4  x2
    //public static final String MIP_FILENAME = "F:\\temporary files here\\knapsackSmall.lp";  ////x1 x7  x4  x2
    public static final String MIP_FILENAME = "F:\\temporary files here\\knapsackFourTest.lp"; 
    //public static final String MIP_FILENAME = "F:\\temporary files here\\bab5.mps";
    //public static final String MIP_FILENAME = "F:\\temporary files here\\p6b.mps";
    //public static final String MIP_FILENAME = "F:\\temporary files here\\harp2.mps";
    //public static final String MIP_FILENAME = "F:\\temporary files here\\cov1075.mps";
    // public static final String MIP_FILENAME = "F:\\temporary files here\\seymour-disj-10.mps";
    //public static final String MIP_FILENAME = "harp2.mps";
    //public static final String MIP_FILENAME = "cov1075.mps";
    //public static final String MIP_FILENAME = "stp3d.mps";
    //public static final String MIP_FILENAME = "seymour-disj-10.mps";
    //public static final String MIP_FILENAME = "p6b.mps";
    
    //for minimization problems , collect infeasible hypercubes whose LP is strictly lower (i.e better) than this
    public static final double HYPERCUBE_COLLECTION_LP_THRESHOLD = -63;
    
    //our method is used to branch only for the first few leafs
    public static final int RAMP_UP_TO_THIS_MANY_LEAFS = 1000000;
   
    public static boolean USE_STRICT_INEQUALITY_IN_MIP = false;
}
