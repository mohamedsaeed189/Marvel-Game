package model.effects;

import model.abilities.AreaOfEffect;
import model.abilities.DamagingAbility;
import model.world.Champion;

public class Shield extends Effect {

	public Shield( int duration) {
		super("Shield", duration, EffectType.BUFF);
		
	}

	
	public void apply(Champion c) {
		c.setSpeed((int)(c.getSpeed()+c.getSpeed()*0.02));
	}

	public void remove(Champion c) {
		c.setSpeed((int)(c.getSpeed()/1.02));
		
	}
}
