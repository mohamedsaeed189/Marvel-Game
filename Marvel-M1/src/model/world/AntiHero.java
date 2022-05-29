package model.world;

import java.util.ArrayList;

import model.effects.Stun;

public class AntiHero extends Champion {

	public AntiHero(String name, int maxHP, int maxMana, int actions, int speed, int attackRange, int attackDamage) {
		super(name, maxHP, maxMana, actions, speed, attackRange, attackDamage);

	}

	public void useLeaderAbility(ArrayList<Champion> targets) {
		
		for(Champion champion:targets) {
			Stun stun=new Stun(2);
			champion.getAppliedEffects().add(stun);
			stun.apply(champion);
		}
	}
}
