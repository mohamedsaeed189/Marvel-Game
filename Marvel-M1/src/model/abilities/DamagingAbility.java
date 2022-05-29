package model.abilities;

import java.util.ArrayList;

import model.effects.Shield;
import model.world.Champion;
import model.world.Cover;
import model.world.Damageable;

public class DamagingAbility extends Ability {
	
	private int damageAmount;
	
	public DamagingAbility(String name, int cost, int baseCoolDown, int castRadius, AreaOfEffect area,int required,int damageAmount) {
		super(name, cost, baseCoolDown, castRadius, area,required);
		this.damageAmount=damageAmount;
	}
	public int getDamageAmount() {
		return damageAmount;
	}
	public void setDamageAmount(int damageAmount) {
		this.damageAmount = damageAmount;
	}

	public void execute(ArrayList<Damageable> targets) {
		boolean hasShield=false;
		for(int i=0;i<targets.size();i++) {
			if(targets.get(i) instanceof Champion ) {
				Champion tmp = (Champion) targets.get(i);
				for(int j=0;j<tmp.getAppliedEffects().size();j++) {
					if(tmp.getAppliedEffects().get(i) instanceof Shield) {
						tmp.getAppliedEffects().get(i).remove(tmp);
						tmp.getAppliedEffects().remove(i);
						hasShield=true;
						break;
					}					
				}
				if(hasShield==false) {
					tmp.setCurrentHP(tmp.getCurrentHP()-damageAmount);
				}
			}
			else if(targets.get(i) instanceof Cover ) {
				Cover tmp = (Cover) targets.get(i);
				tmp.setCurrentHP(tmp.getCurrentHP()-damageAmount);
			}
		}
			
	}
	
}
