/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_v2.cplexSolver;
  
import static ca.mcmaster.hypercube_subtraction_v2.Constants.*;
import static ca.mcmaster.hypercube_subtraction_v2.Parameters.*;
import ca.mcmaster.hypercube_subtraction_v2.TestDriver;
import ca.mcmaster.hypercube_subtraction_v2.collection.*; 
import ca.mcmaster.hypercube_subtraction_v2.common.*;
import ca.mcmaster.hypercube_subtraction_v2.merge.RectangleMerger;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap; 

/**
 *
 * @author tamvadss
 */
import static java.lang.System.exit;
public class BranchHandler  extends BaseBranchCallback{ 
    
    private BranchingVariableSuggestor branchingVarSuggestor = new BranchingVariableSuggestor();
    private Map<String, IloNumVar> modelVariables = new TreeMap<>();
        
    private double[ ][] bounds ;
    private IloNumVar[][] vars;
    private IloCplex.BranchDirection[ ][]  dirs;
    
    private  boolean isRampUpComplete = false;
     
    public BranchHandler (IloNumVar[] variables) {
        for (IloNumVar var : variables) {
            modelVariables.put (var.getName(), var);
        }
       
        
    }

    @Override
    protected void main() throws IloException {
        
        if ( getNbranches()> ZERO ){  
            
            totalNumberOFBranches+=getNbranches();
            
            //take cplex default branching after ramp up
            
            boolean isMipRoot = ( getNodeId().toString()).equals( MIP_ROOT_ID);
                       
            //get the node attachment for this node, any child nodes will accumulate the branching conditions
            
            if (isMipRoot){
                //root of mip
                
                NodePayload data = new NodePayload (  );
                setNodeData(data);                
            } 
            
            NodePayload nodeData = (NodePayload) getNodeData();
            double lpEstimate = getObjValue();
            
            boolean wasRampupComplete = isRampUpComplete;
            isRampUpComplete=    isRampUpComplete || ( getNremainingNodes64()> RAMP_UP_TO_THIS_MANY_LEAFS);
            
            if (!wasRampupComplete && isRampUpComplete) {
                System.out.println("RAMP UP PHASE IS OVER") ;
            }
            
            if (isMipRoot) {
                //get all hypercubes
                LeafNode thisLeaf = new LeafNode ( nodeData.zeroFixedVars , nodeData.oneFixedVars) ; 
                RectangleCollector collector =   new RectangleCollector(thisLeaf);
                nodeData.hypercubesList = new ArrayList<Rectangle>();
                for ( LowerBoundConstraint lbc :  TestDriver.mipConstraintList){
                                
                    List<Rectangle> hyperCubes = collector.collect_INFeasibleHyperCubes(lbc);
                    //logger.debug ("biggest infes rect for " +lbc  + " is " + hyperCube); 
                    nodeData.hypercubesList.addAll(hyperCubes);
                }
                
                //merge and absorb hypercubes
                RectangleMerger merger = new RectangleMerger (nodeData.hypercubesList) ;
                nodeData.hypercubesList= merger.absorbAndMerge() ;
                
                if( merger.isMIP_Infeasible) {
                    System.out.println("MIP is unfeasible; no need for branching") ;
                    exit(ZERO);
                }
                        
            } else {
                //use cplex default branching, or use hyper cubes got from parent
            } 
                         
            
            // vars needed for child node creation 
            vars = new IloNumVar[TWO][] ;
            bounds = new double[TWO ][];
            dirs = new  IloCplex.BranchDirection[ TWO][];
             
            //get branching var suggestion from hypercube list, if available
            if (null !=nodeData && !  isRampUpComplete) {   
                List<String> suggestedBranchingVars=new ArrayList<String>();
                
                List<String> excludedVars=new ArrayList<String>();
                excludedVars.addAll( nodeData.zeroFixedVars);
                excludedVars.addAll( nodeData.oneFixedVars);
                
                suggestedBranchingVars = this.branchingVarSuggestor.getBranchingVar( nodeData.hypercubesList ,  excludedVars);
                //pick a branching var, and split hypercubes into left and right sections
                String branchingVariable =  getVarWithLargestObjCoeff( suggestedBranchingVars) ; 
                List<Rectangle> zeroChild_hypercubesList = new ArrayList<Rectangle> ();
                List<Rectangle> oneChild_hypercubesList = new ArrayList<Rectangle> ();
                splitHyperCubes (branchingVariable  ,zeroChild_hypercubesList ,oneChild_hypercubesList, nodeData.hypercubesList) ;
                
                getArraysNeededForCplexBranching(branchingVariable);
                
                //create node attachments for left and right child  
                NodePayload zeroChild_payload = getChildPayload (true,  branchingVariable, nodeData, zeroChild_hypercubesList) ;
                NodePayload oneChild_payload =getChildPayload (false,  branchingVariable,nodeData, oneChild_hypercubesList) ;
                
                //create both kids
                makeBranch( vars[ZERO],  bounds[ZERO],dirs[ZERO],  lpEstimate  ,  zeroChild_payload );
                makeBranch( vars[ONE],  bounds[ONE],dirs[ONE],   lpEstimate, oneChild_payload  );

            }else {
                                
                //use cplex default
                getBranches( vars, bounds,  dirs) ;
               
                //create both kids
                makeBranch( vars[ZERO],  bounds[ZERO],dirs[ZERO],  lpEstimate   );
                makeBranch( vars[ONE],  bounds[ONE],dirs[ONE],   lpEstimate);
            }
            
        }
         
    }
    
    private NodePayload getChildPayload (boolean isZeroChild,String branchingVariable,  NodePayload parent_nodeData,  List<Rectangle> child_hypercubesList) {
        
        NodePayload payload = new NodePayload ();
        
        payload.zeroFixedVars.addAll( parent_nodeData.zeroFixedVars );
        payload.oneFixedVars.addAll( parent_nodeData.oneFixedVars);
        if (isZeroChild) {
            payload.zeroFixedVars.add(  branchingVariable ) ;
        }else {
            payload.oneFixedVars.add( branchingVariable );
        }
        
        payload.hypercubesList=child_hypercubesList;
        
        return payload;
    }
    
    private void getArraysNeededForCplexBranching (String branchingVar ){
        //get var with given name, and create up and down branch conditions
        vars[ZERO] = new IloNumVar[ONE];
        vars[ZERO][ZERO]= this.modelVariables.get(branchingVar );
        bounds[ZERO]=new double[ONE ];
        bounds[ZERO][ZERO]=ZERO;
        dirs[ZERO]= new IloCplex.BranchDirection[ONE];
        dirs[ZERO][ZERO]=IloCplex.BranchDirection.Down;

        vars[ONE] = new IloNumVar[ONE];
        vars[ONE][ZERO]= this.modelVariables.get(branchingVar );
        bounds[ONE]=new double[ONE ];
        bounds[ONE][ZERO]=ONE;
        dirs[ONE]= new IloCplex.BranchDirection[ONE];
        dirs[ONE][ZERO]=IloCplex.BranchDirection.Up;
    }
       
    private void splitHyperCubes(String    branchingVariable,  List<Rectangle> zeroChild_hypercubesList,   
                                 List<Rectangle> oneChild_hypercubesList,  List<Rectangle> parent_hypercubesList  ){
        
        while (parent_hypercubesList.size()>ZERO) {
            Rectangle rect=parent_hypercubesList.remove(ZERO);
            if (rect.zeroFixedVariables.contains(branchingVariable )) {
                zeroChild_hypercubesList.add(rect );
            }else if (rect.oneFixedVariables.contains( branchingVariable)) {
                oneChild_hypercubesList.add(rect);
            }else {
                //add to both sides
                zeroChild_hypercubesList.add(rect );
                oneChild_hypercubesList.add(rect);
            }
        }
         
    }
    
    private String getVarWithLargestObjCoeff(List<String> suggestedBranchingVars){
        String result = null;
        double highestCoeffMagnitude = -Double.MAX_VALUE;
        for (String var : suggestedBranchingVars) {
            double thisMagnitude = TestDriver.objective.getObjectiveCoeffMagnitude(var );
            if (highestCoeffMagnitude< thisMagnitude) {
                result = var;
                highestCoeffMagnitude =thisMagnitude;
            }
        }
        return result;
    }
}
