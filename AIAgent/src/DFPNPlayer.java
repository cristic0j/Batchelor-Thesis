
public class DFPNPlayer extends  AbstractPlayer {

    protected int proofChild;
    protected int disproofChild;
    protected int disproof2;

    // /protected final static int A = -1;

    protected ProofNode bestNode;

    private final ProofNode  dummy = new ProofNode();

    protected DFPnBigTable table;

    protected PNSPlayer secondLevelPlayer;

    protected int parameterA, parameterB;

    public boolean secondlevel = false;

    public DFPNPlayer() {}

    public DFPNPlayer(Game eval) {
        gameAI = eval;
        generator = gameAI.getGenerator();
     
        table = new  DFPnBigTable(19);
     
        secondLevelPlayer = new PNSPlayer(eval);
     
        parameterA = 2 * ProofNode.MAX_NODES;
        parameterB = ProofNode.MAX_NODES / 4;
    }
    
    public DFPNPlayer(Game eval, DFPnBigTable t) {
        this(eval);
     
        table = t;
    }    

    public Move search(Board b) {
  
        bord = b; 
        int side = bord.sideToMove;
        ProofNode root;
    
        ProofNode.nodeCounter = 0;
    
        secondlevel = false;
  
        countNodes = 0;
        duration = 0;
    
        table.clear();
        secondLevelPlayer.countNodes = 0;
    
        // counterTTHITS =0;
    
        root = new ProofNode(bord, side);
    
        secondLevelPlayer.setDrawValue(root);
        secondLevelPlayer.countNodes = 0;
    
        // counterTTHITS =0;
    
        root = new ProofNode(bord, ProofNode.OR_NODE);
    
        beginSide = side;
    
        // Initialise the proof-number treshold at the root
        root.proof = ProofNode.INFINITY;
        root.disproof = ProofNode.INFINITY;
  
        // Get a snapshot of the current time.
        searchStartTime = System.currentTimeMillis();
    
        isMoveEvaluation = false;
    
        Move best_move = multipleIterativeDeepening(side, 0, root);
    
 
   
        duration = System.currentTimeMillis() - searchStartTime;
   
        Log.println(
                "Nps: " + (int) (1000.0 * countNodes / duration) + " Counter "
                + countNodes);    
    
        if (root.proof == 0 && root.disproof == ProofNode.INFINITY) {
            Log.println("Proven");
            best_move.score = beginSide * Board.WIN_SCORE;
       
            for (Move m = best_move; m != null; m = m.next) {
               Log.print(m.toString() + " ");
            }
           
            Log.println("");
        }
  
        if (root.proof == ProofNode.INFINITY && root.disproof == 0) {
            Log.println("DisProven");
        }      
  
        return best_move;

    } 
   
    protected Move multipleIterativeDeepening(int side, int depth, ProofNode node) {
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
  
        if (node.proof <= proof || node.disproof <= disproof) {
            node.proof = proof;
            node.disproof =  disproof;
       
            return null;
        }

        if (isTerminal(node, side)) {
            return null;
        }
 
        Move best_move = null;
  
        // expand a node generating all his moves
        Move move_list = generator.generateMoves(bord, side);
 
        if (isMoveEvaluation) {
            for (Move m = move_list; m != null; m = m.next) {
                m.apply(bord);
                m.score =  gameAI.getEvaluator().getNumberOfMoves(bord, -side);
                m.undo(bord);  
            }
        }

        Move opponentMove = null;

        ProofNode n; // = new ProofNode(-node.Side);
 
        ProofNode node_list = createChildren(move_list, node);

        long size = countNodes;

        // iterative deepening  
        while (true) {
 
            if (depth == 0 && (countNodes & 1023) == 0) {
                Log.println("" + countNodes);
            }
  
            // terminate if either proof or disproof is at least its threshold
  
            /* int min = DisProofMin(bord,node.Side, move_list,side);
             int sum = ProofSum(bord, node.Side, move_list,side);*/
 
            boolean alzheimer = setNodes(bord, node_list, move_list);
 
            int min = sumDPN(node_list);
            int sum = sumPN(node_list);
  
            if (node.proof <= min || node.disproof <= sum) { 
                node.proof =  min;
                node.disproof =  sum;
    
                putInTransTable(bord.hashValue, countNodes - size, node);

                recycle(move_list, best_move);
      
                if (best_move != null) {
                    best_move.next = opponentMove;
                }
      
                return  best_move;
            }
  
            disproofChild =  proof;
  
            // best_move  = SelectChild(bord,node.Side,  move_list, side);
 
            best_move = selectChild(node_list, move_list);
  
            n = bestNode;
  
            entry = n; // LookUpTransTable(ZobristHash.Hash(bord,best_move),bord,node.Side,best_move, side);
  
            boolean leaf = false;
  
            if ((entry.proof == 1 && entry.disproof == 1)) { // ) || (entry.proof == ProofNode.INFINITY-1 && entry.disproof == ProofNode.INFINITY-1))
                leaf = true;
            }
    
            int threshold = 0;
  
            if (!alzheimer) {
                threshold = node.disproof + proofChild - sum;
  
                if (threshold >= ProofNode.INFINITY) {
                    n.proof = ProofNode.INFINITY - 1;
                } else {
                    n.proof =  (threshold);
                }
  
                n.disproof =  (Math.min(node.proof, disproof2 + 1));
            } else {
                threshold = proofChild + 1;
   
                if (threshold >= ProofNode.INFINITY) {
                    n.proof = ProofNode.INFINITY - 1;
                } else {
                    n.proof = (threshold);
                }
  
                n.disproof =  (Math.min(node.proof,
                        Math.max(disproof2, min) + 1));
   
            }
   
            if (secondlevel && best_move != null && leaf) {
                secondLevelSearch(best_move, side, n);
            } else if (best_move != null) {
                best_move.apply(bord);
  
                opponentMove = multipleIterativeDeepening(-side, depth + 1, n);
  
                best_move.undo(bord);
            } else {
                Move.applyNullMove(bord);
      
                opponentMove = multipleIterativeDeepening(-side, depth + 1, n);
     
                Move.undoNullMove(bord);
            }
      
            // Check exceeds time
            if (countNodes > maxNodes) {
                return new Move();
            }     
          
        }
 
    }

    protected Move selectChild(ProofNode node_list, Move move_list) {
        Move best_move = null; 
       int disproof_bound = disproofChild;

        disproofChild = ProofNode.INFINITY;
        disproof2 = ProofNode.INFINITY;
        int proof, disproof;
        ProofNode entry = node_list;

        bestNode = node_list;
   
        for (Move m = move_list; m != null; m = m.next) { // entry = LookUpTransTable(ZobristHash.Hash(b, m),b, -type,m, -side);
     
            proof = entry.proof;
            disproof = entry.disproof;
     
            if (proof != ProofNode.INFINITY) {
                disproof =  Math.max(disproof, disproof_bound);
            }
         
            if (disproof < disproofChild) {
                best_move = m; 
                bestNode = entry;
      
                disproof2 = disproofChild;
                proofChild = proof;
                disproofChild = disproof;
      
            } else if (disproof < disproof2) {
                disproof2 = disproof;
            }
   
            if (proof == ProofNode.INFINITY) {
                return best_move;
            }
  
            entry = entry.sibling;  
  
        }
  
        return best_move;

    }
 
    protected final ProofNode createChildren(Move move_list, ProofNode parent_node) {

        if (move_list == null) {
            return new ProofNode(-parent_node.side);
        }

        ProofNode n = null, node_list = null;

        for (Move m = move_list; m != null; m = m.next) {
            n = new ProofNode(-parent_node.side);
            n.parent = parent_node;
   
            n.sibling = node_list;
            node_list = n;
 
        }
 
        return node_list;
    }
 
    protected boolean setNodes(Board b, ProofNode node_list, Move move_list) {
  
        ProofNode entry; 
        ProofNode n = node_list;
 
        boolean alzheimer = false; 
  
        for (Move m = move_list; m != null; m = m.next, n = n.sibling) {
            entry = lookUpTransTable(m.hash(b));
    
            if (!(entry.proof == 1 && entry.disproof == 1)) {
                n.proof = entry.proof;
                n.disproof = entry.disproof;
                alzheimer = true;
            }
        }
 
        return alzheimer;
    }

   
    protected final ProofNode lookUpTransTable(long HashValue) {

        DFPnBigEntry entry = table.retrieve(HashValue);

        if (entry != null) {
            dummy.proof = entry.getProofNumber();
            dummy.disproof = entry.getDisProofNumber();
        } 

        return dummy;

    }

    protected final void putInTransTable(long HashValue, long size, ProofNode n) {
        table.storeTwoBig(HashValue, size, n);
    }

    protected final int sumDPN(ProofNode node_list) {

    
        int min = ProofNode.INFINITY;

      
   
        for (ProofNode n = node_list; n != null; n = n.sibling) {
            min = Math.min(min, n.disproof);
        }  
   
        return min;   
    }
 
    // protected final int ProofSum(Board b,int type, Move move_list,int side)  

    protected final int sumPN(ProofNode node_list) {

        /* int sum =0;
         
         ProofNode entry =null;
         
         
         int costproof =0;
         
         if(type == ProofNode.OR_NODE)
         costproof = A;
         
         for(Move m = move_list; m != null; m = m.next)
         {
         entry = LookUpTransTable(m.hash(b),b,-type,m,-side);  
         
         if(sum + entry.proof >= ProofNode.INFINITY)
         {if(entry.proof == ProofNode.INFINITY)
         return entry.proof;
         else
         return (ProofNode.INFINITY-1);
         
         }
         
         sum = sum + entry.proof;// +costproof;  
         }  
         
         return sum;*/
 
 
        int sum = 0;  

        for (ProofNode n = node_list; n != null; n = n.sibling) {
            if (sum + n.proof >= ProofNode.INFINITY) {
                if (n.proof == ProofNode.INFINITY) {
                    return n.proof;
                } else {
                    sum = (ProofNode.INFINITY - 1);
                    continue;
           
                }
       
            }
        
            sum = sum + n.proof; // +costproof;  
        }  
 
        return sum;

    }

    protected boolean isTerminal(ProofNode node, int side) {
        int score = gameAI.gameTheoreticEvaluation(bord, -side);
   
        if (score == Board.DRAW_SCORE) {
            if (node.side == ProofNode.AND_NODE) {
                node.proof = 0;
                node.disproof = ProofNode.INFINITY;
            } else {
                node.proof = ProofNode.INFINITY; 
                node.disproof = 0;
            }
    
            putInTransTable(bord.hashValue, 1, node);
    
            return true;
        } 
        // check if terminal
        if (Math.abs(score) >= Board.WIN_SCORE) {
            if (node.side == ProofNode.AND_NODE) {
                if (beginSide * score == Board.WIN_SCORE) {
                    node.proof = ProofNode.INFINITY; 
                    node.disproof = 0;
                } else {
                    node.proof = 0;
                    node.disproof = ProofNode.INFINITY;
                }
            } else if (beginSide * score == -Board.WIN_SCORE) {
                node.proof = ProofNode.INFINITY; 
                node.disproof = 0;
            } else {
                node.proof = 0;
                node.disproof = ProofNode.INFINITY;
            }
          
            putInTransTable(bord.hashValue, 1, node);   
       
            return   true;
        }
      
        // Ckeck on draws
        if (bord.checkOnDraw()) {
            if (node.side == ProofNode.AND_NODE) {
                node.proof = 0;
                node.disproof = ProofNode.INFINITY;
            } else {
                node.proof = ProofNode.INFINITY; 
                node.disproof = 0;
            }
    
            putInTransTable(bord.hashValue, 1, node);
    
            return true;
        }
 
        // otherwise setposition,  because we go into a second level pn search, and drawing is checked by the proofnodes
        node.setPosition(bord.posB, bord.posW);
 
        return false;
    }
 
    public void secondLevelSearch(Move best_move, int side, ProofNode n) {
        best_move.apply(bord);
  
        secondLevelPlayer.countNodes = 0; 
  
        if (!isTerminal(n, -side)) {
  
            n.proof = 1;
            n.disproof = 1;
 
            ProofNode root = new ProofNode(bord, -side);
 
            root.parent = n.parent;
 
            ProofNode.nodeCounter = table.getCountStoredEntries();

            secondLevelPlayer.proofNumberSearch(root, // /2*table.Counter); 
                    (long) Math.min(maxNodes - countNodes + 1 + table.getCountStoredEntries(),
                    (table.getCountStoredEntries()
                    * (1.0
                            / (1
                                    + Math.pow(Math.E,
                                    (parameterA - table.getCountStoredEntries()) / (double) parameterB)))
                                            + 1
                                            + table.getCountStoredEntries())));
  
            /* for(ProofNode c = root.children; c!= null; c = c.sibling)
             { 
             if(c.Side ==Board.W_SIDE)
             { n.proof = c.disproof;
             n.disproof = c.proof;       
             }
             else
             { n.proof = c.proof;
             n.disproof = c.disproof;
             }
             
             PutInTransTable(TranspositionTable.setHashValue(c),1+ (secondLevelPlayer.Counter /30),n);
             
             }*/
      
            if ((root.side == Board.W_SIDE)) {
                n.proof = root.disproof;
                n.disproof = root.proof;      
            } else {
                n.proof = root.proof;
                n.disproof = root.disproof;     
            }  
              
            putInTransTable(bord.hashValue, secondLevelPlayer.countNodes, n);   
         
            root.parent = null;  
        }
        countNodes += secondLevelPlayer.countNodes + 1;
    
        best_move.undo(bord);
    }
  
    public void setParameters(int a, int b) {
        parameterA = a;
        parameterB = b;
    }
 
}
