/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_v2.collection.cplex;

import ilog.concert.IloNumVar;
import java.util.*;

/**
 *
 * @author tamvadss
 */
public class NodeAttachment {
    public List<IloNumVar> availableBranchingVariables = new ArrayList<IloNumVar> ();
}
