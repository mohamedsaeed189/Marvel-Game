package model.effects;

import java.util.ArrayList;

import model.abilities.Ability;
import model.abilities.AreaOfEffect;
import model.abilities.DamagingAbility;
import model.abilities.HealingAbility;
import model.world.Champion;

public class PowerUp extends Effect {
	

	public PowerUp(int duration) {
		super("PowerUp", duration, EffectType.BUFF);
		
	}
	
	
	public void apply(Champion c) {
		for(int i=0;i<c.getAbilities().size();i++) {
			Ability e =c.getAbilities().get(i);
			if(e instanceof HealingAbility) {
				HealingAbility ability = (HealingAbility)e;
				ability.setHealAmount((int)(ability.getHealAmount()+ability.getHealAmount()*0.2));
			}
			else if (e instanceof DamagingAbility) {
				DamagingAbility ability = (DamagingAbility)e;
				ability.setDamageAmount((int)(ability.getDamageAmount()+ability.getDamageAmount()*0.2));
			}
		}	
	}
	
	public void remove(Champion c) {
		for(int i=0;i<c.getAbilities().size();i++) {
			Ability e =c.getAbilities().get(i);
			if(e instanceof HealingAbility) {
				HealingAbility ability = (HealingAbility)e;
				ability.setHealAmount((int)(ability.getHealAmount()/1.2));
			}
			else if (e instanceof DamagingAbility) {
				DamagingAbility ability = (DamagingAbility)e;
				ability.setDamageAmount((int)(ability.getDamageAmount()/1.2));
			}
		}
	}
	
}
