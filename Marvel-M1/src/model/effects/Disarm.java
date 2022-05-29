package model.effects;

import java.util.ArrayList;

import exceptions.ChampionDisarmedException;
import model.abilities.Ability;
import model.abilities.AreaOfEffect;
import model.abilities.DamagingAbility;
import model.world.Champion;
import model.world.Hero;

public class Disarm extends Effect {
	
	public Disarm( int duration) {
		super("Disarm", duration, EffectType.DEBUFF);
	}
	
	
	public void apply(Champion c){	
		DamagingAbility damagingAbility= new DamagingAbility("Punch",0,1,1,AreaOfEffect.SINGLETARGET,1,50);
		c.getAbilities().add(damagingAbility);		
	}
	
	public void remove(Champion c) {
		for(int i=0;i<c.getAbilities().size();i++) 
			if(c.getAbilities().get(i) instanceof DamagingAbility
					&& ((DamagingAbility)c.getAbilities().get(i)).getName()=="Punch"
					&& ((DamagingAbility)c.getAbilities().get(i)).getCastArea()==AreaOfEffect.SINGLETARGET
					&& ((DamagingAbility)c.getAbilities().get(i)).getManaCost()==0
					&& ((DamagingAbility)c.getAbilities().get(i)).getDamageAmount()==50
					&& ((DamagingAbility)c.getAbilities().get(i)).getBaseCooldown()==1
					&&((DamagingAbility)c.getAbilities().get(i)).getCastRange()==1
					&& ((DamagingAbility)c.getAbilities().get(i)).getRequiredActionPoints()==1
			) 
				c.getAbilities().remove((DamagingAbility)c.getAbilities().get(i));		
	}
}
