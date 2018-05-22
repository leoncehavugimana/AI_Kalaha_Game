package ai;

import java.util.ArrayList;
import kalaha.GameState;

/**
 *
 * NOTES: should be proper GET/SET methods and not public variables. However keeping it since it's easier to read.
 * 
 */
public class Node {


    public GameState state;
    public int nodeId;
    // Previous node exists to easily keep track of the previous node for backtracking up the tree, prevNode is in the end = bestMove 
    public int prevNode;
    public int utilityScore;
    public ArrayList<Node> children = new ArrayList();
    public boolean isTerminalNode;
    
    public int alpha;
    public int beta;
    
    public Node(GameState state, int nodeId) {
        this.state = state;
        this.nodeId = nodeId;
    }
    
    public Node(GameState state, int nodeId, int alpha, int beta) {
        this.state = state;
        this.nodeId = nodeId;
        this.alpha = alpha;
        this.beta = beta;
    }

    public void expandNode() {
        
        for (int i = 1; i < 7; i++) {
            // If the move is possible -> store in children, else discard it. 
            if (state.moveIsPossible(i)) {
                isTerminalNode = false;
                children.add(new Node(state.clone(), i));
            }
        }
        // If there's no children the node is a terminal node / leaf node. 
        if(children.size() <= 0){
            isTerminalNode = true;
        }

    }
    
    // Calculate utility score.
    public void calculateUtilityScore() {
        utilityScore = 10 * state.getScore(1) - state.getScore(2);
    }

}
