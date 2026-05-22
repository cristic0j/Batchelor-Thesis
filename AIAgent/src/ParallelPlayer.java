import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

public class ParallelPlayer extends AbstractPlayer {
	
	private final Player [] playerList;
	private final ThreadSearch [] threadList;
	
	public void quitSearch() {
		
		super.quitSearch();
		for(int i =0; i < playerList.length; i++)
			playerList[i].quitSearch();
		
	}

	public ParallelPlayer(Game ai, int proc){
		
		gameAI = ai;
		final int shift = (int)(Math.log(proc)/Math.log(2));
		playerList = new Player[proc];
		threadList = new ThreadSearch[proc];	
		
		   
	   try {
		   
		   Field [] list = ai.getClass().getFields();
		
			for(int i =0; i<proc;i++){
				
					Game g =  ai.getClass().getDeclaredConstructor().newInstance();
					
					g.setEvaluator(ai.getEvaluator().getClass().getDeclaredConstructor().newInstance());
					
					for(int j = list.length; --j>=0;){
					if(list[j].getType().isPrimitive() && !Modifier.isFinal(list[j].getModifiers())){
							
							//Log.println(list[j] + " " + list[j].get(ai));
							list[j].set(g,list[j].get(ai));
							//Log.println(list[j] + " " + list[j].get(g));
							
						}
					}
						
					g.sizeTT >>=shift;	
					
					//Log.println("Old: " + ai.sizeTT + " " + ai.isRPS);
					//Log.println("New: " + g.sizeTT + " " + g.isRPS);
					
					playerList[i] = new Player(g);
					threadList[i] = new ThreadSearch(playerList[i]);
					
			}
	   
	   }catch (InstantiationException | IllegalArgumentException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
				e.printStackTrace();
			} 
	}

	public Move search(Board b) {
		long hulp = System.currentTimeMillis();
		
		bord = b;
		final int side = b.sideToMove;
		
		ThreadGroup tGroup = new ThreadGroup("Parallel Player");
		
		Move list = gameAI.getGenerator().generateMoves(bord, side);
		
		Move [] moveList = new Move[threadList.length];
		
		Move m = list, next; 
		
		int counter=0;
		
		while(m!=null){
			counter%=threadList.length;
			next = m.next;
			m.next = moveList[counter];
			moveList[counter] = m;
            m = next;
            counter++;
		}
	
		for(int i = 0; i<threadList.length; i++){
			Thread t = new Thread(tGroup,threadList[i]);
			threadList[i].setMoves(moveList[i]);
			t.start();
		}		
		
		
	   synchronized (tGroup) {
			    while (tGroup.activeCount() > 0) {
				try {
					tGroup.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			    }
		    }
	   
	   Move best = threadList[0].result;
	   long totalNodes=playerList[0].totalNodes;
	    countNodes=playerList[0].countNodes;

		
		for(int i = 1; i < threadList.length && threadList[i].result != null;i++){
			totalNodes+=playerList[i].totalNodes;
			countNodes+=playerList[i].countNodes;
			if(best.score*side < threadList[i].result.score*side)
				best = threadList[i].result;
		}
		
		
        duration=(System.currentTimeMillis()-hulp);
		Log.print("Move: " + best +  " parallel nps: " + ((totalNodes*1000)/(System.currentTimeMillis()-hulp)) + "\r\n");
		
		return best;
	}
	
	private class ThreadSearch implements Runnable{
		
		private final Player player;
		
		private Move moveList;
		
		public Move result;
		
		public ThreadSearch(Player p){
			
			player = p;			
		}
		
		public void setMoves(Move list){
			
			moveList = list;
		}

		public void run() {
					
			result = null;
			
			if(moveList==null) return;
			
			result = player.search(bord.duplicate(), moveList); //bord.sideToMove);
			
			if(result.score*bord.sideToMove >= Board.WIN_SCORE)
				for(int i = playerList.length; --i>=0;){
					if(playerList[i]!=player)
						playerList[i].quitSearch();
				}
		}
		
	}
	
	

}
