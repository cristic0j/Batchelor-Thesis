
// import java.io.*;

public class PDSPlayer extends DFPNPlayer {

    

    public PDSPlayer() {}

    public PDSPlayer(Game eval) {
        super(eval);
    }
       
    public PDSPlayer(Game eval, DFPnBigTable t) {
        super(eval, t);
    }
 
    public Move search(Board b) {
        bord = b; 
        final int side = b.sideToMove;
        ProofNode root;
    
        ProofNode.nodeCounter = 0;
  
        countNodes = 0;
        duration = 0;
    
        table.clear();
       
        secondLevelPlayer.countNodes = 0;
    
        // counterTTHITS =0;
    
        root = new ProofNode(bord, side);
    
        secondLevelPlayer.setDrawValue(root);
        secondLevelPlayer.countNodes = 0;
    
        root = new ProofNode(bord, ProofNode.OR_NODE);   
        beginSide = side;
    
        // Initialise the proof-number treshold at the root
        root.proof = 1;
        root.disproof = 1;
  
        // Get a snapshot of the current time.
        searchStartTime = System.currentTimeMillis();
    
        isMoveEvaluation = false;
    
        Move best_move = null;
    
        while (countNodes < maxNodes) {
            best_move = multipleIterativeDeepening(side, 0, root);
             
            if (root.proof == 0 || root.disproof == 0) {
                break;
            }
       
            if (root.proof <= root.disproof) {
                root.proof++;
            } else {
                root.disproof++;
            }
         
            if (root.proof == ProofNode.INFINITY
                    || root.disproof == ProofNode.INFINITY) {
                best_move.score = ProofNode.INFINITY;
        
                Log.println("Overflow");
                break;
            }
         
            // Log.Log.println(root.proof + " " + root.disproof);
        }

        /* table.Counter = 1000000;
         
         Log.Log.println((long)(table.Counter*(1.0 / (1+ Math.pow(Math.E, (1800000 - table.Counter) / 240000.0))) +1));*/
  
        duration = System.currentTimeMillis() - searchStartTime;
   
        countNodes += secondLevelPlayer.countNodes;
    
        /* ProofNode  entry = LookUpTransTable(0,bord,root.Side,best_move,-side);
         Log.Log.println(entry.proof + " " +  entry.disproof);*****/
       
        Log.println(
                "Nps: " + (int) (1000.0 * countNodes / duration) + " Counter"
                + countNodes);    
    
        if (root.proof == 0 && root.disproof == ProofNode.INFINITY) {
            Log.println("Proven");
            best_move.score = beginSide * Board.WIN_SCORE;
       
            for (Move m = best_move; m != null; m = m.next) {
                Log.print(m.toString() + " ");
            }
           
            Log.println("");
        } else if (root.proof == ProofNode.INFINITY && root.disproof == 0) {
            best_move.score = beginSide * Board.DRAW_SCORE;
           Log.println("DisProven " + best_move.score);
        } else {
            Log.println("I don't know" + root.proof + " " + root.disproof);
        }
  
        Log.println("Table Counter: " + table.getCountStoredEntries());
  
        return best_move;
    }   

    protected final Move multipleIterativeDeepening(int side, int depth, ProofNode node) {
        countNodes++;

        if (countNodes > maxNodes) {
            return new Move();
        }
    
        // Stop to deep
        if (depth >= 340) {
            node.proof = ProofNode.INFINITY - 1;
            node.disproof = ProofNode.INFINITY - 1;
            return null;  
        }   
 
        // ProofNode entry = LookUpTransTable(bord.HashValue, bord,node.Side, null, side);
 
        ProofNode entry = lookUpTransTable(bord.hashValue);
  
        int proof = entry.proof;
        int disproof = entry.disproof;
  
        if (proof == 0 || disproof == 0
                || (node.proof <= proof && node.disproof <= disproof)) {
            node.proof = proof;
            node.disproof = disproof;
       
            return null;
        }

        if (isTerminal(node, side)) {
            return null;
        }
 
        // expand a node generating all his moves
        Move move_list = generator.generateMoves(bord, side);
 
        if (isMoveEvaluation) {
            for (Move m = move_list; m != null; m = m.next) {
                m.apply(bord);
                m.score =  gameAI.getEvaluator().getNumberOfMoves(bord, -side);
                m.undo(bord);  
            }
        }
 
        Move best_move = null;

        Move opponentMove = null;

        long size = countNodes;

        // /ProofNode n = new ProofNode(-node.Side); 

        ProofNode n;
        ProofNode node_list = createChildren(move_list, node);

        // int min = DisProofMin(bord,node.Side, move_list,side);
        // int sum = ProofSum(bord,node.Side, move_list,side);

        // iterative deepening  
        while (true) {
  
            /* int min = DisProofMin(bord,node.Side, move_list,side);
             int sum = ProofSum(bord,node.Side, move_list,side);**/
            setNodes(bord, node_list, move_list);  
  
            int min = sumDPN(node_list);
            int sum = sumPN(node_list);
     
            // terminate if either proof or disproof is at least its threshold
            if ((sum == 0) || (min == 0)
                    || (node.proof <= min && node.disproof <= sum)) { 
                node.proof =  min;
                node.disproof =  sum;
    
                putInTransTable(bord.hashValue, countNodes - size, node);

                recycle(move_list, best_move);
      
                if (best_move != null) {
                    best_move.next = opponentMove;
                }
      
                return  best_move;
            }
 
            proof =  Math.max(proof, min);
  
            best_move = selectChild(node_list, move_list, proof); // SelectChild(bord,node.Side, move_list, side, proof );
            n = bestNode;
  
            entry = n; // LookUpTransTable(ZobristHash.Hash(bord,best_move),bord,node.Side,best_move, side);
  
            boolean leaf = false;
  
            if ((entry.proof == 1 && entry.disproof == 1)) { // ) || (entry.proof == ProofNode.INFINITY-1 && entry.disproof == ProofNode.INFINITY-1))
                leaf = true;
            }
  
            if (node.disproof > sum
                    && (entry.proof <= entry.disproof || node.proof <= min)) {
                n.proof =  (entry.proof + 1);
   
                if (n.proof == ProofNode.INFINITY) {
                    n.proof = ProofNode.INFINITY - 1;
                }
   
                n.disproof = entry.disproof;
            } else {
                n.proof = entry.proof;
                n.disproof = (entry.disproof + 1);
      
                if (n.disproof == ProofNode.INFINITY) {
                    n.disproof = ProofNode.INFINITY - 1;
                }
    
            }
  
            if (secondlevel && best_move != null && leaf) {
                secondLevelSearch(best_move, side, n);
            } else if (best_move != null) {
                best_move.apply(bord);
                opponentMove = multipleIterativeDeepening(-side, depth + 1, n);
    
                // hash = bord.HashValue;
  
                best_move.undo(bord);
            } else {
                Move.applyNullMove(bord);
                opponentMove = multipleIterativeDeepening(-side, depth + 1, n);
      
                // hash = bord.HashValue;
     
                Move.undoNullMove(bord);
            }

            // Check exceeds time
            if (countNodes > maxNodes) {
                return new Move();
            }     
 
        }
 
    }

    // protected  Move SelectChild(Board b,int type ,  Move move_list, int side , short proof)

    protected Move selectChild(ProofNode node_list, Move move_list, int proof) {
        int min = ProofNode.INFINITY;
        int minp = ProofNode.INFINITY;
  
        Move best_move = null;   
   
        ProofNode entry = node_list;
   
        bestNode = node_list;
      
        for (Move m = move_list; m != null; m = m.next) { // entry = LookUpTransTable(ZobristHash.Hash(b, m),b,-type,m, -side);
     
            disproofChild = entry.disproof;
     
            if (disproofChild != 0) {
                disproofChild =  Math.max(disproofChild, proof);
            }
         
            if (disproofChild < min
                    || (disproofChild == min && entry.proof < minp)) {
                min = disproofChild;     
                minp = entry.proof;     
                best_move = m; 
                bestNode = entry;
            }    

            // useless code, this situation normally doesn't happen, because proof number =zero
            if (entry.proof == ProofNode.INFINITY) {
                return best_move;
            }
     
            entry = entry.sibling;
        }
  
        return best_move;
    }

}
