package model.world;

import java.util.ArrayList;

import model.effects.Effect;
import model.effects.EffectType;
import model.effects.Embrace;

public class Hero extends Champion {

	public Hero(String name, int maxHP, int maxMana, int actions, int speed, int attackRange, int attackDamage) {
		super(name, maxHP, maxMana, actions, speed, attackRange, attackDamage);

	}

	public void useLeaderAbility(ArrayList<Champion> targets) {
		for(int i=0;i<targets.size();i++) {
			Champion champion = targets.get(i);
			for(int j=0;j<champion.getAppliedEffects().size();j++) { 
				Effect effect = champion.getAppliedEffects().get(j);
				if(effect.getType()==EffectType.DEBUFF) {
					effect.remove(champion);
					champion.getAppliedEffects().remove(effect);
					j--;
				}
			}
			Embrace embrace = new Embrace(2);
			champion.getAppliedEffects().add(embrace);
			embrace.apply(champion);
		}		
	}

	
}
