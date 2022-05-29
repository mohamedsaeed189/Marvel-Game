package model.effects;

import model.abilities.AreaOfEffect;
import model.abilities.DamagingAbility;
import model.world.Champion;
import model.world.Hero;

public class Embrace extends Effect {
	

	public Embrace(int duration) {
		super("Embrace", duration, EffectType.BUFF);
	}
	
	public void apply(Champion c) {
		c.setCurrentHP((int)(c.getCurrentHP()+c.getMaxHP()*0.2));
		c.setMana((int)(c.getMana()+c.getMana()*0.2));
		c.setSpeed((int)(c.getSpeed()+c.getSpeed()*0.2));
		c.setAttackDamage((int)(c.getAttackDamage()+c.getAttackDamage()*0.2));
	}
	
	public void remove(Champion c) {
		c.setSpeed((int)(c.getSpeed()/1.2));
		c.setAttackDamage((int)(c.getAttackDamage()/1.2));
	}

}
