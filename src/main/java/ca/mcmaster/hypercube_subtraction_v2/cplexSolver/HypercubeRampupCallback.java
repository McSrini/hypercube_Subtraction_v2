/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_v2.cplexSolver;
  
import static ca.mcmaster.hypercube_subtraction_v2.Constants.*; 
import static ca.mcmaster.hypercube_subtraction_v2.Parameters.LOGGING_LEVEL; 
import ca.mcmaster.hypercube_subtraction_v2.TestDriver;
import ca.mcmaster.hypercube_subtraction_v2.collection.*;  
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.BranchCallback;
import static java.lang.System.exit;  
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;  
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 */ 
public class HypercubeRampupCallback  extends  BranchCallback{ 
    
    private BranchingVariableSuggestor branchingVarSuggestor = new BranchingVariableSuggestor();
            
    private double[ ][] bounds ;
    private IloNumVar[][] vars;
    private IloCplex.BranchDirection[ ][]  dirs;
    
    protected Map<String, IloNumVar> modelVariables = new TreeMap<>();
    protected List<Rectangle> infeasibleHypercubesList;
    
    public long totalNumberOFBranches=ZERO;
      
    private static Logger logger=Logger.getLogger(HypercubeRampupCallback.class);
    static {
        logger.setLevel(LOGGING_LEVEL);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            RollingFileAppender appender = new  RollingFileAppender(layout,LOG_FOLDER+HypercubeRampupCallback.class.getSimpleName()+ LOG_FILE_EXTENSION);
            appender.setMaxBackupIndex(SIXTY);
            logger.addAppender(appender);
            logger.setAdditivity(false);
        } catch (Exception ex) {
            ///
            System.err.println("Exit: unable to initialize logging"+ex);       
            exit(ONE);
        }
    } 
    
    public HypercubeRampupCallback (IloNumVar[] variables, List<Rectangle> infeasibleHypercubesList) {
        for (IloNumVar var : variables) {
            modelVariables.put (var.getName(), var);
        }
        this.infeasibleHypercubesList =  infeasibleHypercubesList;
    }

    //the job of this callback is to pass on the infeasible hypercubes to each child, after deciding what the branching var should be
    protected void main() throws IloException {
        
        if ( getNbranches()> ZERO ){  
            
            totalNumberOFBranches+=getNbranches();
             
            boolean isMipRoot = ( getNodeId().toString()).equals( MIP_ROOT_ID);
                       
            //get the node attachment for this node, any child nodes will accumulate the branching conditions
            
            if (isMipRoot){
                //root of mip
                
                NodePayload data = new NodePayload (  );
                data.hypercubesList=this.infeasibleHypercubesList;
                setNodeData(data);                
            } 
            
            NodePayload nodeData = (NodePayload) getNodeData();
            double lpEstimate = getObjValue();
                   
            
            // vars needed for child node creation 
            vars = new IloNumVar[TWO][] ;
            bounds = new double[TWO ][];
            dirs = new  IloCplex.BranchDirection[ TWO][];
             
            //get branching var suggestion from hypercube list, if available
            if (null !=nodeData && nodeData.hypercubesList.size()>ZERO    ) {   
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
