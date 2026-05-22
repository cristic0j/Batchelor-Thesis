
public class MoveStep {
	
 public int direction;         
 public int destination;        
 public int captureSquare;    
 public int captPiece;     
 
	public	MoveStep(int d, int dst, int c_sq, int c_pc) {
		setMoveStep(d, dst, c_sq, c_pc);
		}
	
	public MoveStep(){}
	
	public MoveStep(MoveStep ms){
		setMoveStep(ms.direction, ms.destination, ms.captureSquare, ms.captPiece);
	}

	public	void setMoveStep(int d, int dst, int c_sq, int c_pc) {
	    direction = d;
	    destination = dst;
	    captureSquare = c_sq;
	    captPiece = c_pc;
	}

}
