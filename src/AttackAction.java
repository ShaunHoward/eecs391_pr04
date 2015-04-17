import java.util.HashMap;
import java.util.Map;


public class AttackAction {
	private Map<Integer, Integer> attack = new HashMap<Integer, Integer>();
	
	public AttackAction(Map<Integer, Integer> attack) {
		this.attack = new HashMap<Integer, Integer>(attack);
	}

	public Map<Integer, Integer> getAttack() {
		return attack;
	}
	
}
