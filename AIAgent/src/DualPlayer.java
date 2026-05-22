public class DualPlayer extends AbstractPlayer {
	
    private static int plyBorder = 35;
 
    private static double ratio = 0.15;
  
    private AbstractPlayer firstPlayer,secondPlayer; 

    private boolean foundWin = false;
    
 
    public DualPlayer() {}
 
    public DualPlayer(AbstractPlayer p1,AbstractPlayer p2) { 
          
        firstPlayer = p1;
        secondPlayer = p2;
      
        foundWin = false;
    }

    public Move search(Board b) {
  
        bord = b;
        final int side = b.sideToMove;
        // Get a snapshot of the current time.
        long start_time = System.currentTimeMillis();
    
        // SearchMaxNodes = 500000;
  
        firstPlayer.timeCheck = true;
 
        Move m = null;
  
        if (winTable != null) {
            secondPlayer.gameAI.isWonTable = true;
            firstPlayer.gameAI.isWonTable = true;
        }
     
        if (!neverEnd
                && (bord.getNumberOfMovesPlayed() + 1 >= getPlyBorder())/* ||( ply > 25 && history[((ply-1)/2)-1][2]* side >= 500)*/) { 
            if (winTable == null) { 
                winTable = new WinTable(15);
                firstPlayer.winTable = winTable;
            }
    
            secondPlayer.gameAI.isWonTable = firstPlayer.gameAI.isWonTable = true;
       
            int score = winTable.retrieve(bord.hashValue);
       
            if (Math.abs(score) >= Board.WIN_SCORE) {
                winTable.emptyPosition(bord.hashValue);
            }
       
            firstPlayer.targetTime = (int) (getRatio() * Board.TIME_LIMIT);
       
            boolean enoughTime = true;
       
            if (Board.isTournament) {
                firstPlayer.setTargetTime(bord.getNumberOfMovesPlayed());
          
                long hulptime = firstPlayer.targetTime;
           
                firstPlayer.targetTime = (int) (getRatio() * hulptime);
                      
                if (hulptime < 10000 && !neverEnd) {
                    enoughTime = false;
                }
            }
        
       
            if (enoughTime) {
            	Log.println("First Player: " + firstPlayer.targetTime);
                m = firstPlayer.search(bord.duplicate());
                m.apply(bord);
                if (bord.checkOnDraw()) {
                    m.score = 0;
                }
                m.undo(bord);
            }
            
            
        } 
   
        if (m== null || (m.score * side < Board.WIN_SCORE  && getRatio()<0.99)) {

        	secondPlayer.winTable = firstPlayer.winTable;
     
        	duration = System.currentTimeMillis() - start_time; 
        	secondPlayer.targetTime = Board.TIME_LIMIT - duration;
      
            if (Board.isTournament) {
            	secondPlayer.decreaseValue = duration;
            }
        
            if (winTable != null) {
                int score = winTable.retrieve(bord.hashValue);
       
                if (Math.abs(score) >= Board.WIN_SCORE) {
                    winTable.emptyPosition(bord.hashValue);
                }
            } 
      
            m = secondPlayer.search(bord.duplicate());
        } else {
            timeoutFlag = true;
            doSomeThing(); 
            foundWin = true;
        }

        duration = System.currentTimeMillis() - start_time; 
  
        countNodes = firstPlayer.countNodes + secondPlayer.countNodes;

        // SearchMaxNodes = oldmax;
 
        firstPlayer.timeCheck = false;
  
        Log.print(
                "Time: " + firstPlayer.duration + "+" +  secondPlayer.duration + "="
                + duration + "\r\n");
        Log.print("Nodes: " + countNodes + "\r\n");
  
  
        // win_table = null;
  
        secondPlayer.gameAI.isWonTable = false;
        firstPlayer.gameAI.isWonTable = false;
        
        Log.print("Done \r\n" );
  
        return m;
    }
   
    public boolean foundWin() {
        return foundWin;
    }

	public static void setRatio(double rat) {
		ratio = rat;
	}

	public static double getRatio() {
		return ratio;
	}

	public static void setPlyBorder(int ply_Border) {
		plyBorder = ply_Border;
	}

	public static int getPlyBorder() {
		return plyBorder;
	}
    
/* protected void setTargetTime(int ply) {
		  
	        nmoves = (gameAI.neededMoves) - ply / 2;
	        
	        if (nmoves < gameAI.minNoMoves) {
	            nmoves = gameAI.minNoMoves;
	        }
	        
	        targetTime = ((Board.TournamentTime) / nmoves) - decreaseValue;
	    }*/
 
 /*   private boolean endgame(int side) {

        int n = (side == Board.B_SIDE) ? bord.B_Counter : bord.R_Counter;

        if (bord.B_Counter + bord.R_Counter - n <= n && n <= 5) {
            return true;
        }
  
        int  SomPos_row = 0;
        int  SomPos_col = 0;
        int i;
        Piece[] list;
  
        if (side == Board.B_SIDE) {
            SomPos_row = bord.sumOfRowBP;
            SomPos_col = bord.sumOfColBP;
            list = bord.arrayB;
        } else {
            SomPos_row = bord.sumOfRowRP;
            SomPos_col = bord.sumOfColRP;
            list = bord.arrayR;
        }
        
        double Gem_row = (double) SomPos_row / n;
        double  Gem_col = (double) SomPos_col / n;	
   
        // calculate concetration
        double Avg_Distance = 1;
        double distance;
        int  row, col;

        // combined calculation, computes the average distance 
        for (i = n; --i >= 0;) {
            row = list[i].row; 
            col = list[i].col;
            distance = 0;
            double difrow = Math.abs(row - Gem_row);
            double difcol = Math.abs(col - Gem_col);
        
            if (difrow > difcol) { 
                distance = difrow - 1;
            } else { 
                distance = difcol - 1;
            } 
        
            if (distance > 1) {
                Avg_Distance += distance;
            }
        }
    
        if (n > 9) {
            Avg_Distance = (Avg_Distance - (n - 9)) / n;
        } else {
            Avg_Distance /= n;
        } 
     
        if (Avg_Distance < 2.8) {
            return true;
        }
 
        return false;  
    }*/
}

