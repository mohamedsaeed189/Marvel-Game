package engine;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import exceptions.AbilityUseException;
import exceptions.ChampionDisarmedException;
import exceptions.InvalidTargetException;
import exceptions.LeaderAbilityAlreadyUsedException;
import exceptions.LeaderNotCurrentException;
import exceptions.NotEnoughResourcesException;
import exceptions.UnallowedMovementException;
import model.abilities.Ability;
import model.abilities.AreaOfEffect;
import model.abilities.CrowdControlAbility;
import model.abilities.DamagingAbility;
import model.abilities.HealingAbility;
import model.effects.Disarm;
import model.effects.Dodge;
import model.effects.Effect;
import model.effects.EffectType;
import model.effects.Embrace;
import model.effects.PowerUp;
import model.effects.Root;
import model.effects.Shield;
import model.effects.Shock;
import model.effects.Silence;
import model.effects.SpeedUp;
import model.effects.Stun;
import model.world.AntiHero;
import model.world.Champion;
import model.world.Condition;
import model.world.Cover;
import model.world.Damageable;
import model.world.Direction;
import model.world.Hero;
import model.world.Villain;


public class Game {
	private static ArrayList<Champion> availableChampions;
	private static ArrayList<Ability> availableAbilities;
	private Player firstPlayer;
	private Player secondPlayer;
	private Object[][] board;
	private PriorityQueue turnOrder;
	private boolean firstLeaderAbilityUsed;
	private boolean secondLeaderAbilityUsed;
	private final static int BOARDWIDTH = 5;
	private final static int BOARDHEIGHT = 5;

	public Game(Player first, Player second) {
		firstPlayer = first;
		secondPlayer = second;
		availableChampions = new ArrayList<Champion>();
		availableAbilities = new ArrayList<Ability>();
		board = new Object[BOARDWIDTH][BOARDHEIGHT];
		turnOrder = new PriorityQueue(6);
		placeChampions();
		placeCovers();
		prepareChampionTurns();
	}

	public static void loadAbilities(String filePath) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(filePath));
		String line = br.readLine();
		while (line != null) {
			String[] content = line.split(",");
			Ability a = null;
			AreaOfEffect ar = null;
			switch (content[5]) {
			case "SINGLETARGET":
				ar = AreaOfEffect.SINGLETARGET;
				break;
			case "TEAMTARGET":
				ar = AreaOfEffect.TEAMTARGET;
				break;
			case "SURROUND":
				ar = AreaOfEffect.SURROUND;
				break;
			case "DIRECTIONAL":
				ar = AreaOfEffect.DIRECTIONAL;
				break;
			case "SELFTARGET":
				ar = AreaOfEffect.SELFTARGET;
				break;

			}
			Effect e = null;
			if (content[0].equals("CC")) {
				switch (content[7]) {
				case "Disarm":
					e = new Disarm(Integer.parseInt(content[8]));
					break;
				case "Dodge":
					e = new Dodge(Integer.parseInt(content[8]));
					break;
				case "Embrace":
					e = new Embrace(Integer.parseInt(content[8]));
					break;
				case "PowerUp":
					e = new PowerUp(Integer.parseInt(content[8]));
					break;
				case "Root":
					e = new Root(Integer.parseInt(content[8]));
					break;
				case "Shield":
					e = new Shield(Integer.parseInt(content[8]));
					break;
				case "Shock":
					e = new Shock(Integer.parseInt(content[8]));
					break;
				case "Silence":
					e = new Silence(Integer.parseInt(content[8]));
					break;
				case "SpeedUp":
					e = new SpeedUp(Integer.parseInt(content[8]));
					break;
				case "Stun":
					e = new Stun(Integer.parseInt(content[8]));
					break;
				}
			}
			switch (content[0]) {
			case "CC":
				a = new CrowdControlAbility(content[1], Integer.parseInt(content[2]), Integer.parseInt(content[4]),
						Integer.parseInt(content[3]), ar, Integer.parseInt(content[6]), e);
				break;
			case "DMG":
				a = new DamagingAbility(content[1], Integer.parseInt(content[2]), Integer.parseInt(content[4]),
						Integer.parseInt(content[3]), ar, Integer.parseInt(content[6]), Integer.parseInt(content[7]));
				break;
			case "HEL":
				a = new HealingAbility(content[1], Integer.parseInt(content[2]), Integer.parseInt(content[4]),
						Integer.parseInt(content[3]), ar, Integer.parseInt(content[6]), Integer.parseInt(content[7]));
				break;
			}
			availableAbilities.add(a);
			line = br.readLine();
		}
		br.close();
	}

	public static void loadChampions(String filePath) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(filePath));
		String line = br.readLine();
		while (line != null) {
			String[] content = line.split(",");
			Champion c = null;
			switch (content[0]) {
			case "A":
				c = new AntiHero(content[1], Integer.parseInt(content[2]), Integer.parseInt(content[3]),
						Integer.parseInt(content[4]), Integer.parseInt(content[5]), Integer.parseInt(content[6]),
						Integer.parseInt(content[7]));
				break;

			case "H":
				c = new Hero(content[1], Integer.parseInt(content[2]), Integer.parseInt(content[3]),
						Integer.parseInt(content[4]), Integer.parseInt(content[5]), Integer.parseInt(content[6]),
						Integer.parseInt(content[7]));
				break;
			case "V":
				c = new Villain(content[1], Integer.parseInt(content[2]), Integer.parseInt(content[3]),
						Integer.parseInt(content[4]), Integer.parseInt(content[5]), Integer.parseInt(content[6]),
						Integer.parseInt(content[7]));
				break;
			}

			c.getAbilities().add(findAbilityByName(content[8]));
			c.getAbilities().add(findAbilityByName(content[9]));
			c.getAbilities().add(findAbilityByName(content[10]));
			availableChampions.add(c);
			line = br.readLine();
		}
		br.close();
	}

	private static Ability findAbilityByName(String name) {
		for (Ability a : availableAbilities) {
			if (a.getName().equals(name))
				return a;
		}
		return null;
	}

	public void placeCovers() {
		int i = 0;
		while (i < 5) {
			int x = ((int) (Math.random() * (BOARDWIDTH - 2))) + 1;
			int y = (int) (Math.random() * BOARDHEIGHT);

			if (board[x][y] == null) {
				board[x][y] = new Cover(x, y);
				i++;
			}
		}

	}

	public void placeChampions() {
		int i = 1;
		for (Champion c : firstPlayer.getTeam()) {
			board[0][i] = c;
			c.setLocation(new Point(0, i));
			i++;
		}
		i = 1;
		for (Champion c : secondPlayer.getTeam()) {
			board[BOARDHEIGHT - 1][i] = c;
			c.setLocation(new Point(BOARDHEIGHT - 1, i));
			i++;
		}
	
	}

	public static ArrayList<Champion> getAvailableChampions() {
		return availableChampions;
	}

	public static ArrayList<Ability> getAvailableAbilities() {
		return availableAbilities;
	}

	public Player getFirstPlayer() {
		return firstPlayer;
	}

	public Player getSecondPlayer() {
		return secondPlayer;
	}

	public Object[][] getBoard() {
		return board;
	}

	public PriorityQueue getTurnOrder() {
		return turnOrder;
	}

	public boolean isFirstLeaderAbilityUsed() {
		return firstLeaderAbilityUsed;
	}

	public boolean isSecondLeaderAbilityUsed() {
		return secondLeaderAbilityUsed;
	}

	public static int getBoardwidth() {
		return BOARDWIDTH;
	}

	public static int getBoardheight() {
		return BOARDHEIGHT;
	}
	
	public Champion getCurrentChampion() {
		Champion c = (Champion) turnOrder.peekMin();
		return c;
	}
	
	public Player checkGameOver() {
		if(firstPlayer.getTeam().size()==0)
			return secondPlayer;
		else if(secondPlayer.getTeam().size()==0) 
			return firstPlayer;
		else 
			return null;
	}
	
	public void move(Direction d) throws UnallowedMovementException, NotEnoughResourcesException {
		Champion c = getCurrentChampion();
		Point p = c.getLocation();
		if(c.getCondition()==Condition.ROOTED ){
			throw new UnallowedMovementException("You cannot move");
		}
		else if(c.getCurrentActionPoints()==0) {
			throw new NotEnoughResourcesException("current action points isnot sufficient");
		}
		else {
			if(d==Direction.RIGHT) {
				if(p.y==4) {
					throw new UnallowedMovementException("You cannot move here");
				}else if(board[p.x][p.y+1]!=null) {
					throw new UnallowedMovementException("You cannot move here");
				}else {
					board[p.x][p.y]=null;
					p.y++;
					board[p.x][p.y]= c;	
					c.setLocation(new Point(p.x,p.y));
					c.setCurrentActionPoints(c.getCurrentActionPoints()-1);
				}
			}else if(d==Direction.LEFT){
				if(p.y==0) {
					throw new UnallowedMovementException("You cannot move here");
				}else if(board[p.x][p.y-1]!=null) {
					throw new UnallowedMovementException("You cannot move here");
				}else {
					board[p.x][p.y]=null;
					p.y--;
					board[p.x][p.y]= c;
					c.setLocation(new Point(p.x,p.y));
					c.setCurrentActionPoints(c.getCurrentActionPoints()-1);
				}
			}else if (d==Direction.UP) {
				if(p.x==4) {
					throw new UnallowedMovementException("You cannot move here");
				}else if(board[p.x+1][p.y]!=null) {
					throw new UnallowedMovementException("You cannot move here");
				}else {
					board[p.x][p.y]=null;
					p.x++;
					board[p.x][p.y]= c;
					c.setLocation(new Point(p.x,p.y));
					c.setCurrentActionPoints(c.getCurrentActionPoints()-1);
				}
			}else  if (d==Direction.DOWN) {
				if(p.x==0) {
					throw new UnallowedMovementException("You cannot move here");
				}else if(board[p.x-1][p.y]!=null) {
					throw new UnallowedMovementException("You cannot move here");
				}else {
					board[p.x][p.y]=null;
					p.x--;
					board[p.x][p.y]= c;
					c.setLocation(new Point(p.x,p.y));
					c.setCurrentActionPoints(c.getCurrentActionPoints()-1);
				}
			}
		}
		
	}
	public void attack(Direction d) throws ChampionDisarmedException, NotEnoughResourcesException {
		Champion c = getCurrentChampion();
		int dx=-1;
		int dy=-1;
		int px =(int)c.getLocation().getX();
		int py =(int)c.getLocation().getY();
		boolean damageableHasShield=false;
		boolean damageableHasDodge=false;
		boolean isCover=false;
		if(checkDisarm(c.getAppliedEffects())==true) {
			throw new ChampionDisarmedException("Champion is currently Disarmed");
		}
		if(c.getCurrentActionPoints()<2) {
			throw new NotEnoughResourcesException("current action points is not sufficient");
		}
		else {	
			if(d==Direction.RIGHT) {
				for(int i=1; i<=c.getAttackRange(); i++) {
					if(py+i>4) {
						break;
					}else if(board[px][py+i]!=null){
						dx=px;
						dy=py+i;
						break;
					}	
				}
			}else if(d==Direction.LEFT) {
				for(int i=1; i<=c.getAttackRange(); i++) {
					if(py-i<0) {
						break;
					}else if(board[px][py-i]!=null){
						dx=px;
						dy=py-i;
						break;
					}
					
				}
			}else if(d==Direction.UP) {
				for(int i=1; i<=c.getAttackRange(); i++) {
					if(px+i>4) {
						break;
					}else if(board[px+i][py]!=null){
						dx=px+i;
						dy=py;
						break;
					}				
				}
			}else if(d==Direction.DOWN) {
				for(int i=1; i<=c.getAttackRange(); i++) {
					if(px-i<0) {
						break;
					}else if(board[px-i][py]!=null){
						dx=px-i;
						dy=py;
						break;
					}					
				}
			}	
		}
		c.setCurrentActionPoints(c.getCurrentActionPoints()-2);
		if(dx!=-1 && dy!=-1) {
			Damageable damageable = (Damageable)board[dx][dy];
			if(damageable instanceof Champion) {
				Champion champion = (Champion)damageable;
				if(checkShield(champion.getAppliedEffects())) {
					damageableHasShield=true;
				}
				if (checkDodge(champion.getAppliedEffects())) {
					damageableHasDodge=true;
				}
			}
			else {
				isCover=true;
				damageable.setCurrentHP(damageable.getCurrentHP()-c.getAttackDamage());
				if(damageable.getCurrentHP()<=0)
					board[dx][dy]=null;
			}
			if(damageableHasShield==false && isCover==false) {
				if(damageableHasDodge==true) {
					Random rand = new Random();
					int prob = rand.nextInt();
					Champion champ = (Champion)damageable;
					if(prob==0) {
						if( (c instanceof Hero && damageable instanceof Villain)
								|| (c instanceof Villain && damageable instanceof Hero )
								|| (c instanceof AntiHero && damageable instanceof Villain)
								|| (c instanceof Villain && damageable instanceof AntiHero)
								|| (c instanceof AntiHero && damageable instanceof Hero)
								|| (c instanceof Hero && damageable instanceof AntiHero)) {
							int extraDamage = (int)(c.getAttackDamage()/2);
							damageable.setCurrentHP((int)(((damageable.getCurrentHP()-(c.getAttackDamage()+extraDamage)))));	
						}
						else {
							damageable.setCurrentHP((int)(damageable.getCurrentHP()-c.getAttackDamage()));
						}
					}				
					else if(prob==1) {
						if( (c instanceof Hero && damageable instanceof Villain)
								|| (c instanceof Villain && damageable instanceof Hero )
								|| (c instanceof AntiHero && damageable instanceof Villain)
								|| (c instanceof Villain && damageable instanceof AntiHero)
								|| (c instanceof AntiHero && damageable instanceof Hero)
								|| (c instanceof Hero && damageable instanceof AntiHero)) {
							int extraDamage = (int)(c.getAttackDamage()/2);
							damageable.setCurrentHP((int)(damageable.getCurrentHP()-(c.getAttackDamage()*0.5)+extraDamage));	
						}
						else {
							damageable.setCurrentHP((int)(damageable.getCurrentHP()-(c.getAttackDamage()*0.5)));
						}
					}
					for(int i=0;i<champ.getAppliedEffects().size();i++) {
						if(champ.getAppliedEffects().get(i) instanceof Dodge) {
							champ.getAppliedEffects().get(i).remove(champ);
							champ.getAppliedEffects().remove(i);
							break;
						}
					}	
				}
				else if(damageableHasDodge==false) {
					if( (c instanceof Hero && damageable instanceof Villain)
							|| (c instanceof Villain && damageable instanceof Hero )
							|| (c instanceof AntiHero && damageable instanceof Villain)
							|| (c instanceof Villain && damageable instanceof AntiHero)
							|| (c instanceof AntiHero && damageable instanceof Hero)
							|| (c instanceof Hero && damageable instanceof AntiHero)) {
						int extraDamage = (int)(c.getAttackDamage()/2);
						damageable.setCurrentHP((int)(((damageable.getCurrentHP()-(c.getAttackDamage()+extraDamage)))));	
					}
					else {
						damageable.setCurrentHP((int)(damageable.getCurrentHP()-c.getAttackDamage()));
					}
				}
						
			}
			else if(damageableHasShield==true && isCover==false) {
				Champion champion = (Champion)damageable;
				for(int i=0;i<champion.getAppliedEffects().size();i++) {
					if(champion.getAppliedEffects().get(i) instanceof Shield) {
						champion.getAppliedEffects().get(i).remove(champion);
						champion.getAppliedEffects().remove(i);
						break;
					}
				}
			}
			if(damageable.getCurrentHP()<=0 && isCover==false) {
				board[dx][dy]=null;
				Champion champion = (Champion)damageable;
				champion.setCondition(Condition.KNOCKEDOUT);
				ArrayList<Champion> team1 =firstPlayer.getTeam();
				ArrayList<Champion> team2 =secondPlayer.getTeam();
				if(team1.contains((Champion)damageable)) {
					firstPlayer.getTeam().remove((Champion)damageable);
				}
				else if(team2.contains((Champion)damageable)) {
					secondPlayer.getTeam().remove((Champion)damageable);
				}
				ArrayList<Champion> turnOrdertemp = new  ArrayList<Champion>();
				while(!turnOrder.isEmpty()) {
					Champion removedChampion = (Champion)turnOrder.remove();
					if(removedChampion!=(Champion)damageable) {
						turnOrdertemp.add(removedChampion);
					}
				}
				for(int j=0;j<turnOrdertemp.size();j++) {
					turnOrder.insert(turnOrdertemp.get(j));
				}
			}	
		}
	}
	public static boolean checkDisarm (ArrayList<Effect> Var) {
		for(int i=0;i<Var.size();i++) {
			if(Var.get(i) instanceof Disarm) {
				return true;
			}
		}
		return false;
	}
	public static boolean checkDodge (ArrayList<Effect> Var) {
		for(int i=0;i<Var.size();i++) {
			if(Var.get(i) instanceof Dodge) {
				return true;
			}
		}
		return false;
	}
	public static boolean checkShield (ArrayList<Effect> Var) {
		for(int i=0;i<Var.size();i++) {
			if(Var.get(i) instanceof Shield) {
				return true;
			}
		}
		return false;
	}
	
	public int calculateManhattanDistance(Point a, Point b) {
		int distance = Math.abs(a.x-b.x)+ Math.abs(a.y-b.y);
		return distance;
	}
	
	public void castAbility(Ability a) throws AbilityUseException, NotEnoughResourcesException, CloneNotSupportedException {
		Champion champion = getCurrentChampion();
		if(a.getCurrentCooldown()!=0)
			throw new AbilityUseException("You cannot cast this ability now");
		if(a.getRequiredActionPoints()>champion.getCurrentActionPoints())
			throw new NotEnoughResourcesException("You don't have enough action points");
		if(a.getManaCost()>champion.getMana())
			throw new NotEnoughResourcesException("You don't have enough Mana points");
		for(int i=0;i<champion.getAppliedEffects().size();i++) {
			if(champion.getAppliedEffects().get(i) instanceof Silence) {
				throw new AbilityUseException("You cannot cast this ability now");
			}
		}
		ArrayList<Damageable> Targets = new ArrayList<Damageable>();
		Point championLocation=champion.getLocation();
		if(a instanceof DamagingAbility) {
			if (a.getCastArea()==AreaOfEffect.SURROUND) {
				ArrayList <Damageable> temp1 = new ArrayList<Damageable>();
				temp1=getAttackedSurroundChampions(champion,championLocation);
				for(int i=0;i<temp1.size();i++) {
					Targets.add(temp1.get(i));
				}
				ArrayList <Damageable> temp2 = new ArrayList<Damageable>();
				temp2=getAttackedSurroundCovers(champion,championLocation);
				for(int i=0;i<temp2.size();i++) {
					Targets.add(temp2.get(i));
				}
			}		
			else if (a.getCastArea()==AreaOfEffect.TEAMTARGET) {
				ArrayList <Damageable> temp = new ArrayList<Damageable>();
				temp= getAttackedTeamChampions(champion,championLocation,a.getCastRange());
				for(int i=0;i<temp.size();i++) {
					Targets.add(temp.get(i));
				}
			}
		}
		else if(a instanceof HealingAbility ) {
			if(a.getCastArea()==AreaOfEffect.TEAMTARGET) {
				ArrayList <Damageable> temp = new ArrayList<Damageable>();
				temp=getHealingTeamChampions(champion,championLocation,a.getCastRange());
				for(int i=0;i<temp.size();i++) {
					Targets.add(temp.get(i));
				}
			}
			else if(a.getCastArea()==AreaOfEffect.SELFTARGET) {
				Targets.add(champion);
			}
			else if(a.getCastArea()==AreaOfEffect.SURROUND) {
				ArrayList <Damageable> temp = new ArrayList<Damageable>();
				temp=getHealingSurroundChampions(champion,championLocation);
				for(int i=0;i<temp.size();i++) {
					Targets.add(temp.get(i));
				}			
			}
		}
		else if (a instanceof CrowdControlAbility) {
			if (a.getCastArea()==AreaOfEffect.SURROUND) {
				ArrayList <Damageable> temp = new ArrayList<Damageable>();
				temp=getEffectedSurroundChampions(champion,championLocation,a.getCastRange(),((CrowdControlAbility)a).getEffect().getType());
				for(int i=0;i<temp.size();i++) {
					Targets.add(temp.get(i));
				}
			}		
			else if (a.getCastArea()==AreaOfEffect.TEAMTARGET) {
				ArrayList <Damageable> temp = new ArrayList<Damageable>();
				temp=getEffectedTeamChampions(champion,championLocation,a.getCastRange(),((CrowdControlAbility)a).getEffect().getType());
				for(int i=0;i<temp.size();i++) {
					Targets.add(temp.get(i));
				}
			}
			else if (a.getCastArea()==AreaOfEffect.SELFTARGET) {
				Targets.add(champion);
			}
		}
		
		a.execute(Targets);
		
		for(int i=0;i<Targets.size();i++) {
			Damageable d = Targets.get(i);
			if(d.getCurrentHP()<=0) {
				board[d.getLocation().x][d.getLocation().y]=null;
				if(d instanceof Champion) {
					Champion c = (Champion)Targets.get(i);
					c.setCondition(Condition.KNOCKEDOUT);
					ArrayList<Champion> team1 =firstPlayer.getTeam();
					ArrayList<Champion> team2 =secondPlayer.getTeam();
					if(team1.contains(c)) {
						firstPlayer.getTeam().remove(c);
					}
					else if(team2.contains(c)) {
						secondPlayer.getTeam().remove(c);
					}
					ArrayList<Champion> turnOrdertemp = new  ArrayList<Champion>();
					while(!turnOrder.isEmpty()) {
						Champion removedChampion = (Champion)turnOrder.remove();
						if(removedChampion!=c) {
							turnOrdertemp.add(removedChampion);
						}
					}
					for(int j=0;j<turnOrdertemp.size();j++) {
						turnOrder.insert(turnOrdertemp.get(j));
					}
				}
				else if(Targets.get(i) instanceof Cover) {
					board[d.getLocation().x][d.getLocation().y]=null;
				}	
			}
		}
		
		champion.setCurrentActionPoints(champion.getCurrentActionPoints()-a.getRequiredActionPoints());
		champion.setMana(champion.getMana()-a.getManaCost());
		a.setCurrentCooldown(a.getBaseCooldown());
	}
	
	public ArrayList<Damageable> getAttackedSurroundChampions(Champion champion,Point championLocation){
		int px =(int)champion.getLocation().getX();
		int py =(int)champion.getLocation().getY();
		ArrayList<Damageable> targets = new ArrayList<Damageable>();
		if(firstPlayer.getTeam().contains(champion)==true) {
			if ( (px>=0 && px<=4 && py+1>=0 && py+1<=4) && board[px][py+1] instanceof Champion) {
				if (secondPlayer.getTeam().contains((Champion)board[px][py+1])) {
					targets.add((Champion)board[px][py+1]);
				}
			}	
			if ((px>=0 && px<=4 && py-1>=0 && py-1<=4) && board[px][py-1] instanceof Champion) {
				if (secondPlayer.getTeam().contains((Champion)board[px][py-1])) {
					targets.add((Champion)board[px][py-1]);
				}
			}			
			if ((px+1>=0 && px+1<=4 && py>=0 && py<=4) && board[px+1][py] instanceof Champion) {
				if (secondPlayer.getTeam().contains((Champion)board[px+1][py])) {
					targets.add((Champion)board[px+1][py]);
				}
			}
			if ((px-1>=0 && px-1<=4 && py>=0 && py<=4) && board[px-1][py] instanceof Champion) {
				if (secondPlayer.getTeam().contains((Champion)board[px-1][py])) {
					targets.add((Champion)board[px-1][py]);
				}
			}
			if((px+1>=0 && px+1<=4 && py+1>=0 && py+1<=4) && board[px+1][py+1] instanceof Champion){
				if (secondPlayer.getTeam().contains((Champion)board[px+1][py+1])) {
					targets.add((Champion)board[px+1][py+1]);
				}
			}
			
			if((px+1>=0 && px+1<=4 && py-1>=0 && py-1<=4) && board[px+1][py-1] instanceof Champion) {
				if (secondPlayer.getTeam().contains((Champion)board[px+1][py-1])) {
					targets.add((Champion)board[px+1][py-1]);
				}
			}
			if ((px-1>=0 && px-1<=4 && py-1>=0 && py-1<=4) && board[px-1][py-1] instanceof Champion) {
				if (secondPlayer.getTeam().contains((Champion)board[px-1][py-1])) {
					targets.add((Champion)board[px-1][py-1]);
				}
			}
			if ((px-1>=0 && px-1<=4 && py+1>=0 && py+1<=4) && board[px-1][py+1] instanceof Champion){
				if (secondPlayer.getTeam().contains((Champion)board[px-1][py+1])) {
					targets.add((Champion)board[px-1][py+1]);
				}
			}			
		}
		if(secondPlayer.getTeam().contains(champion)==true) {
			if ((px>=0 && px<=4 && py+1>=0 && py+1<=4) && board[px][py+1] instanceof Champion) {
				if (firstPlayer.getTeam().contains((Champion)board[px][py+1])) {
					targets.add((Champion)board[px][py+1]);
				}
			}	
			if ((px>=0 && px<=4 && py-1>=0 && py-1<=4) && board[px][py-1] instanceof Champion) {
				if (firstPlayer.getTeam().contains((Champion)board[px][py-1])) {
					targets.add((Champion)board[px][py-1]);
				}
			}			
			if ((px+1>=0 && px+1<=4 && py>=0 && py<=4) && board[px+1][py] instanceof Champion) {
				if (firstPlayer.getTeam().contains((Champion)board[px+1][py])) {
					targets.add((Champion)board[px+1][py]);
				}
			}
			if ((px-1>=0 && px-1<=4 && py>=0 && py<=4) && board[px-1][py] instanceof Champion) {
				if (firstPlayer.getTeam().contains((Champion)board[px-1][py])) {
					targets.add((Champion)board[px-1][py]);
				}
			}
			if((px+1>=0 && px+1<=4 && py+1>=0 && py+1<=4) && board[px+1][py+1] instanceof Champion){
				if (firstPlayer.getTeam().contains((Champion)board[px+1][py+1])) {
					targets.add((Champion)board[px+1][py+1]);
				}
			}
			
			if((px+1>=0 && px+1<=4 && py-1>=0 && py-1<=4) && board[px+1][py-1] instanceof Champion) {
				if (firstPlayer.getTeam().contains((Champion)board[px+1][py-1])) {
					targets.add((Champion)board[px+1][py-1]);
				}
			}
			if ((px-1>=0 && px-1<=4 && py-1>=0 && py-1<=4) && board[px-1][py-1] instanceof Champion) {
				if (firstPlayer.getTeam().contains((Champion)board[px-1][py-1])) {
					targets.add((Champion)board[px-1][py-1]);
				}
			}
			if ((px-1>=0 && px-1<=4 && py+1>=0 && py+1<=4) && board[px-1][py+1] instanceof Champion){
				if (firstPlayer.getTeam().contains((Champion)board[px-1][py+1])) {
					targets.add((Champion)board[px-1][py+1]);
				}
			}			
		}
		return targets;
	}
	
	public ArrayList<Damageable> getAttackedSurroundCovers(Champion champion,Point championLocation){
		int px =(int)champion.getLocation().getX();
		int py =(int)champion.getLocation().getY();
		ArrayList<Damageable> targets = new ArrayList<Damageable>();	
		if ((px>=0 && px<=4 && py+1>=0 && py+1<=4) && board[px][py+1] instanceof Cover) {
			targets.add((Cover)board[px][py+1]);
		}	
		if ((px>=0 && px<=4 && py-1>=0 && py-1<=4) && board[px][py-1] instanceof Cover) {
			targets.add((Cover)board[px][py-1]);
		}			
		if ((px+1>=0 && px+1<=4 && py>=0 && py<=4) && board[px+1][py] instanceof Cover) {
			targets.add((Cover)board[px+1][py]);	
		}
		if ((px-1>=0 && px-1<=4 && py>=0 && py<=4) && board[px-1][py] instanceof Cover) {
			targets.add((Cover)board[px-1][py]);
		}
		if((px+1>=0 && px+1<=4 && py+1>=0 && py+1<=4) && board[px+1][py+1] instanceof Cover){
			targets.add((Cover)board[px+1][py+1]);
		}
		if((px+1>=0 && px+1<=4 && py-1>=0 && py-1<=4) && board[px+1][py-1] instanceof Cover) {
			targets.add((Cover)board[px+1][py-1]);
		}
		if ((px-1>=0 && px-1<=4 && py-1>=0 && py-1<=4) && board[px-1][py-1] instanceof Cover) {	
			targets.add((Cover)board[px-1][py-1]);
		}
		if ((px-1>=0 && px-1<=4 && py+1>=0 && py+1<=4) && board[px-1][py+1] instanceof Cover){
			targets.add((Cover)board[px-1][py+1]);
		}			
		return targets;
	}
	
	public ArrayList<Damageable> getAttackedTeamChampions(Champion champion,Point championLocation, int abilityRange){
		ArrayList<Damageable> targets = new ArrayList<Damageable>();
		if(firstPlayer.getTeam().contains(champion)==true) {
			for(int i=0;i<secondPlayer.getTeam().size();i++) {
				Point damageableChampionLocation=secondPlayer.getTeam().get(i).getLocation();
				int distance = calculateManhattanDistance(championLocation,damageableChampionLocation);
				if(distance<=abilityRange) {
					targets.add(secondPlayer.getTeam().get(i));
				}
			}
		}	
		if(secondPlayer.getTeam().contains(champion)==true) {
			for(int i=0;i<firstPlayer.getTeam().size();i++) {
				Point damageableChampionLocation=firstPlayer.getTeam().get(i).getLocation();
				int distance = calculateManhattanDistance(championLocation,damageableChampionLocation);
				if(distance<=abilityRange) {
					targets.add(firstPlayer.getTeam().get(i));
				}
			}
		}
		return targets;
	}
	
	public ArrayList<Damageable> getHealingTeamChampions(Champion champion,Point championLocation,int abilityRange){
		ArrayList<Damageable> targets = new ArrayList<Damageable>();
		if(firstPlayer.getTeam().contains(champion)==true) {
			for(int i=0;i<firstPlayer.getTeam().size();i++) {
				Point friendChampionLocation=firstPlayer.getTeam().get(i).getLocation();
				int distance = calculateManhattanDistance(championLocation,friendChampionLocation);
				if(distance<=abilityRange) {
					targets.add(firstPlayer.getTeam().get(i));
				}
			}
		}
		else if(secondPlayer.getTeam().contains(champion)==true) {
			for(int i=0;i<secondPlayer.getTeam().size();i++) {
				Point friendChampionLocation=secondPlayer.getTeam().get(i).getLocation();
				int distance = calculateManhattanDistance(championLocation,friendChampionLocation);
				if(distance<=abilityRange) {
					targets.add(secondPlayer.getTeam().get(i));
				}
			}
		}
		return targets;
	}
	
	public ArrayList<Damageable> getEffectedTeamChampions(Champion champion, Point championLocation,int abilityRange,EffectType type) {
		ArrayList<Damageable> targets = new ArrayList<Damageable>();
		if(type==EffectType.BUFF) {			
			if(firstPlayer.getTeam().contains(champion)==true) {
				for(int i=0;i<firstPlayer.getTeam().size();i++) {
					Point friendChampionLocation=firstPlayer.getTeam().get(i).getLocation();
					int distance = calculateManhattanDistance(championLocation,friendChampionLocation);
					if(distance<=abilityRange) {
						targets.add(firstPlayer.getTeam().get(i));
					}
				}
			}
			else if(secondPlayer.getTeam().contains(champion)==true) {
				for(int i=0;i<secondPlayer.getTeam().size();i++) {
					Point friendChampionLocation=secondPlayer.getTeam().get(i).getLocation();
					int distance = calculateManhattanDistance(championLocation,friendChampionLocation);
					if(distance<=abilityRange) {
						targets.add(secondPlayer.getTeam().get(i));
					}
				}
			}
		}
		else if(type==EffectType.DEBUFF) {
			if(firstPlayer.getTeam().contains(champion)==true) {
				for(int i=0;i<secondPlayer.getTeam().size();i++) {
					Point damageableChampionLocation=secondPlayer.getTeam().get(i).getLocation();
					int distance = calculateManhattanDistance(championLocation,damageableChampionLocation);
					if(distance<=abilityRange) {
						targets.add(secondPlayer.getTeam().get(i));
					}
				}
			}	
			else if(secondPlayer.getTeam().contains(champion)==true) {
				for(int i=0;i<firstPlayer.getTeam().size();i++) {
					Point damageableChampionLocation=firstPlayer.getTeam().get(i).getLocation();
					int distance = calculateManhattanDistance(championLocation,damageableChampionLocation);
					if(distance<=abilityRange) {
						targets.add(firstPlayer.getTeam().get(i));
					}
				}
			}
		}
		return targets;
	}
	
	public ArrayList<Damageable> getEffectedSurroundChampions(Champion champion, Point championLocation,int abilityRange,EffectType type){
		ArrayList<Damageable> targets = new ArrayList<Damageable>();
		int px =(int)champion.getLocation().getX();
		int py =(int)champion.getLocation().getY();
		if(type==EffectType.BUFF) {
			if(firstPlayer.getTeam().contains(champion)==true) {
				if ((px>=0 && px<=4 && py+1>=0 && py+1<=4) && board[px][py+1] instanceof Champion) {
					Champion c = (Champion) board[px][py+1];
					if(firstPlayer.getTeam().contains(c)) {
						targets.add(c);
					}
				}	
				if ((px>=0 && px<=4 && py-1>=0 && py-1<=4) && board[px][py-1] instanceof Champion) {
					Champion c = (Champion) board[px][py-1];
					if (firstPlayer.getTeam().contains(c)) {
						targets.add(c);
					}
				}			
				if ((px+1>=0 && px+1<=4 && py>=0 && py<=4) && board[px+1][py] instanceof Champion) {
					Champion c = (Champion) board[px+1][py];
					if (firstPlayer.getTeam().contains(c)) {
						targets.add(c);
					}
				}
				if ((px-1>=0 && px-1<=4 && py>=0 && py<=4) && board[px-1][py] instanceof Champion) {
					Champion c = (Champion) board[px-1][py];
					if (firstPlayer.getTeam().contains(c)) {
						targets.add(c);
					}
				}
				if((px+1>=0 && px+1<=4 && py+1>=0 && py+1<=4) && board[px+1][py+1] instanceof Champion){
					Champion c = (Champion) board[px+1][py+1];
					if (firstPlayer.getTeam().contains(c)) {
						targets.add(c);
					}
				}
				
				if((px+1>=0 && px+1<=4 && py-1>=0 && py-1<=4) && board[px+1][py-1] instanceof Champion) {
					Champion c = (Champion) board[px+1][py-1];
					if (firstPlayer.getTeam().contains(c)) {
						targets.add(c);
					}
				}
				if ((px-1>=0 && px-1<=4 && py-1>=0 && py-1<=4) && board[px-1][py-1] instanceof Champion) {
					Champion c = (Champion) board[px-1][py-1];
					if (firstPlayer.getTeam().contains(c)) {
						targets.add(c);
					}
				}
				if ((px-1>=0 && px-1<=4 && py+1>=0 && py+1<=4) && board[px-1][py+1] instanceof Champion){
					Champion c = (Champion) board[px-1][py+1];
					if (firstPlayer.getTeam().contains(c)) {
						targets.add(c);
					}
				}			
			}
			else if(secondPlayer.getTeam().contains(champion)==true) {
				if ((px>=0 && px<=4 && py+1>=0 && py+1<=4) && board[px][py+1] instanceof Champion) {
					Champion c = (Champion) board[px][py+1];
					if (secondPlayer.getTeam().contains(c)) {
						targets.add(c);
					}
				}	
				if ((px>=0 && px<=4 && py-1>=0 && py-1<=4) && board[px][py-1] instanceof Champion) {
					Champion c = (Champion) board[px][py-1];
					if (secondPlayer.getTeam().contains(c)) {
						targets.add(c);
					}
				}			
				if ((px+1>=0 && px+1<=4 && py>=0 && py<=4) && board[px+1][py] instanceof Champion) {
					Champion c = (Champion) board[px+1][py];
					if (secondPlayer.getTeam().contains(c)) {
						targets.add(c);
					}
				}
				if ((px-1>=0 && px-1<=4 && py>=0 && py<=4) && board[px-1][py] instanceof Champion) {
					Champion c = (Champion) board[px-1][py];
					if (secondPlayer.getTeam().contains(c)) {
						targets.add(c);
					}
				}
				if((px+1>=0 && px+1<=4 && py+1>=0 && py+1<=4) && board[px+1][py+1] instanceof Champion){
					Champion c = (Champion) board[px+1][py+1];
					if (secondPlayer.getTeam().contains(c)) {
						targets.add(c);
					}
				}
				
				if((px+1>=0 && px+1<=4 && py-1>=0 && py-1<=4) && board[px+1][py-1] instanceof Champion) {
					Champion c = (Champion) board[px+1][py-1];
					if (secondPlayer.getTeam().contains(c)) {
						targets.add(c);
					}
				}
				if ((px-1>=0 && px-1<=4 && py-1>=0 && py-1<=4) && board[px-1][py-1] instanceof Champion) {
					Champion c = (Champion) board[px-1][py-1];
					if (secondPlayer.getTeam().contains(c)) {
						targets.add(c);
					}
				}
				if ((px-1>=0 && px-1<=4 && py+1>=0 && py+1<=4) && board[px-1][py+1] instanceof Champion){
					Champion c = (Champion) board[px-1][py+1];
					if (secondPlayer.getTeam().contains(c)) {
						targets.add(c);
					}
				}			
			}
		}
		else if (type==EffectType.DEBUFF) {
			if(firstPlayer.getTeam().contains(champion)==true) {
				if ((px>=0 && px<=4 && py+1>=0 && py+1<=4) && board[px][py+1] instanceof Champion) {
					Champion c = (Champion) board[px][py+1];
					if (secondPlayer.getTeam().contains(c)) {
						targets.add(c);
					}
				}	
				if ((px>=0 && px<=4 && py-1>=0 && py-1<=4) && board[px][py-1] instanceof Champion) {
					Champion c = (Champion) board[px][py-1];
					if (secondPlayer.getTeam().contains(c)) {
						targets.add(c);
					}
				}			
				if ((px+1>=0 && px+1<=4 && py>=0 && py<=4) && board[px+1][py] instanceof Champion) {
					Champion c = (Champion) board[px+1][py];
					if (secondPlayer.getTeam().contains(c)) {
						targets.add(c);
					}
				}
				if ((px-1>=0 && px-1<=4 && py>=0 && py<=4) && board[px-1][py] instanceof Champion) {
					Champion c = (Champion) board[px-1][py];
					if (secondPlayer.getTeam().contains(c)) {
						targets.add(c);
					}
				}
				if((px+1>=0 && px+1<=4 && py+1>=0 && py+1<=4) && board[px+1][py+1] instanceof Champion){
					Champion c = (Champion) board[px+1][py+1];
					if (secondPlayer.getTeam().contains(c)) {
						targets.add(c);
					}
				}
				
				if((px+1>=0 && px+1<=4 && py-1>=0 && py-1<=4) && board[px+1][py-1] instanceof Champion) {
					Champion c = (Champion) board[px+1][py-1];
					if (secondPlayer.getTeam().contains(c)) {
						targets.add(c);
					}
				}
				if ((px-1>=0 && px-1<=4 && py-1>=0 && py-1<=4) && board[px-1][py-1] instanceof Champion) {
					Champion c = (Champion) board[px-1][py-1];
					if (secondPlayer.getTeam().contains(c)) {
						targets.add(c);
					}
				}
				if ((px-1>=0 && px-1<=4 && py+1>=0 && py+1<=4) && board[px-1][py+1] instanceof Champion){
					Champion c = (Champion) board[px-1][py+1];
					if (secondPlayer.getTeam().contains(c)) {
						targets.add(c);
					}
				}			
			}
			else if(secondPlayer.getTeam().contains(champion)==true) {
				if ((px>=0 && px<=4 && py+1>=0 && py+1<=4) && board[px][py+1] instanceof Champion) {
					Champion c = (Champion) board[px][py+1];
					if (firstPlayer.getTeam().contains(c)) {
						targets.add(c);
					}
				}	
				if ((px>=0 && px<=4 && py-1>=0 && py-1<=4) && board[px][py-1] instanceof Champion) {
					Champion c = (Champion) board[px][py-1];
					if (firstPlayer.getTeam().contains(c)) {
						targets.add(c);
					}
				}			
				if ((px+1>=0 && px+1<=4 && py>=0 && py<=4) && board[px+1][py] instanceof Champion) {
					Champion c = (Champion) board[px+1][py];
					if (firstPlayer.getTeam().contains(c)) {
						targets.add(c);
					}
				}
				if ((px-1>=0 && px-1<=4 && py>=0 && py<=4) && board[px-1][py] instanceof Champion) {
					Champion c = (Champion) board[px-1][py];
					if (firstPlayer.getTeam().contains(c)) {
						targets.add(c);
					}
				}
				if((px+1>=0 && px+1<=4 && py+1>=0 && py+1<=4) && board[px+1][py+1] instanceof Champion){
					Champion c = (Champion) board[px+1][py+1];
					if (firstPlayer.getTeam().contains(c)) {
						targets.add(c);
					}
				}
				
				if((px+1>=0 && px+1<=4 && py-1>=0 && py-1<=4) && board[px+1][py-1] instanceof Champion) {
					Champion c = (Champion) board[px+1][py-1];
					if (firstPlayer.getTeam().contains(c)) {
						targets.add(c);
					}
				}
				if ((px-1>=0 && px-1<=4 && py-1>=0 && py-1<=4) && board[px-1][py-1] instanceof Champion) {
					Champion c = (Champion) board[px-1][py-1];
					if (firstPlayer.getTeam().contains(c)) {
						targets.add(c);
					}
				}
				if ((px-1>=0 && px-1<=4 && py+1>=0 && py+1<=4) && board[px-1][py+1] instanceof Champion){
					Champion c = (Champion) board[px-1][py+1];
					if (firstPlayer.getTeam().contains(c)) {
						targets.add(c);
					}
				}			
			}
		}
		return targets;
	}
	
	public ArrayList<Damageable> getHealingSurroundChampions(Champion champion,Point championLocation){
		int px =(int)champion.getLocation().getX();
		int py =(int)champion.getLocation().getY();
		ArrayList<Damageable> targets = new ArrayList<Damageable>();
		if(firstPlayer.getTeam().contains(champion)==true) {
			if ( (px>=0 && px<=4 && py+1>=0 && py+1<=4) && board[px][py+1] instanceof Champion) {
				if (firstPlayer.getTeam().contains((Champion)board[px][py+1])) {
					targets.add((Champion)board[px][py+1]);
				}
			}	
			if ((px>=0 && px<=4 && py-1>=0 && py-1<=4) && board[px][py-1] instanceof Champion) {
				if (firstPlayer.getTeam().contains((Champion)board[px][py-1])) {
					targets.add((Champion)board[px][py-1]);
				}
			}			
			if ((px+1>=0 && px+1<=4 && py>=0 && py<=4) && board[px+1][py] instanceof Champion) {
				if (firstPlayer.getTeam().contains((Champion)board[px+1][py])) {
					targets.add((Champion)board[px+1][py]);
				}
			}
			if ((px-1>=0 && px-1<=4 && py>=0 && py<=4) && board[px-1][py] instanceof Champion) {
				if (firstPlayer.getTeam().contains((Champion)board[px-1][py])) {
					targets.add((Champion)board[px-1][py]);
				}
			}
			if((px+1>=0 && px+1<=4 && py+1>=0 && py+1<=4) && board[px+1][py+1] instanceof Champion){
				if (firstPlayer.getTeam().contains((Champion)board[px+1][py+1])) {
					targets.add((Champion)board[px+1][py+1]);
				}
			}
			
			if((px+1>=0 && px+1<=4 && py-1>=0 && py-1<=4) && board[px+1][py-1] instanceof Champion) {
				if (firstPlayer.getTeam().contains((Champion)board[px+1][py-1])) {
					targets.add((Champion)board[px+1][py-1]);
				}
			}
			if ((px-1>=0 && px-1<=4 && py-1>=0 && py-1<=4) && board[px-1][py-1] instanceof Champion) {
				if (firstPlayer.getTeam().contains((Champion)board[px-1][py-1])) {
					targets.add((Champion)board[px-1][py-1]);
				}
			}
			if ((px-1>=0 && px-1<=4 && py+1>=0 && py+1<=4) && board[px-1][py+1] instanceof Champion){
				if (firstPlayer.getTeam().contains((Champion)board[px-1][py+1])) {
					targets.add((Champion)board[px-1][py+1]);
				}
			}			
		}
		if(secondPlayer.getTeam().contains(champion)==true) {
			if ((px>=0 && px<=4 && py+1>=0 && py+1<=4) && board[px][py+1] instanceof Champion) {
				if (secondPlayer.getTeam().contains((Champion)board[px][py+1])) {
					targets.add((Champion)board[px][py+1]);
				}
			}	
			if ((px>=0 && px<=4 && py-1>=0 && py-1<=4) && board[px][py-1] instanceof Champion) {
				if (secondPlayer.getTeam().contains((Champion)board[px][py-1])) {
					targets.add((Champion)board[px][py-1]);
				}
			}			
			if ((px+1>=0 && px+1<=4 && py>=0 && py<=4) && board[px+1][py] instanceof Champion) {
				if (secondPlayer.getTeam().contains((Champion)board[px+1][py])) {
					targets.add((Champion)board[px+1][py]);
				}
			}
			if ((px-1>=0 && px-1<=4 && py>=0 && py<=4) && board[px-1][py] instanceof Champion) {
				if (secondPlayer.getTeam().contains((Champion)board[px-1][py])) {
					targets.add((Champion)board[px-1][py]);
				}
			}
			if((px+1>=0 && px+1<=4 && py+1>=0 && py+1<=4) && board[px+1][py+1] instanceof Champion){
				if (secondPlayer.getTeam().contains((Champion)board[px+1][py+1])) {
					targets.add((Champion)board[px+1][py+1]);
				}
			}
			
			if((px+1>=0 && px+1<=4 && py-1>=0 && py-1<=4) && board[px+1][py-1] instanceof Champion) {
				if (secondPlayer.getTeam().contains((Champion)board[px+1][py-1])) {
					targets.add((Champion)board[px+1][py-1]);
				}
			}
			if ((px-1>=0 && px-1<=4 && py-1>=0 && py-1<=4) && board[px-1][py-1] instanceof Champion) {
				if (secondPlayer.getTeam().contains((Champion)board[px-1][py-1])) {
					targets.add((Champion)board[px-1][py-1]);
				}
			}
			if ((px-1>=0 && px-1<=4 && py+1>=0 && py+1<=4) && board[px-1][py+1] instanceof Champion){
				if (secondPlayer.getTeam().contains((Champion)board[px-1][py+1])) {
					targets.add((Champion)board[px-1][py+1]);
				}
			}			
		}
		return targets;
	}
			
	public ArrayList<Damageable> getAttackedDirectionalChampions(Champion champion,Point championLocation, Direction d){
		int chamAttRange = champion.getAttackRange();
		int px = championLocation.x;
		int py = championLocation.y;
		ArrayList<Damageable> targets = new ArrayList<Damageable>();
		if(d==Direction.RIGHT) {
			for(int i=1; i<=chamAttRange; i++) {
				if(py+i>4) {
					break;
				}else if(board[px][py+i]!=null){
					if(board[px][py+i] instanceof Champion) {
						if(firstPlayer.getTeam().contains(champion)==true) {
							if(secondPlayer.getTeam().contains((Champion)board[px][py+i])==true) {
								targets.add((Champion)board[px][py+i]);
							}
						}else if(secondPlayer.getTeam().contains(champion)==true) {
							if(firstPlayer.getTeam().contains((Champion)board[px][py+i])==true) {
								targets.add((Champion)board[px][py+i]);
							}
						}
					}else if(board[px][py+i] instanceof Cover) {
						targets.add((Cover)board[px][py+i]);
					}
				}
			}
		}else if(d==Direction.LEFT) { 
			for(int i=1; i<=chamAttRange; i++) {
				if(py-i<0) {
					break;
				}else if(board[px][py-i]!=null){
					if(board[px][py-i] instanceof Champion) {
						if(firstPlayer.getTeam().contains(champion)==true) {
							if(secondPlayer.getTeam().contains((Champion)board[px][py-i])==true) {
								targets.add((Champion)board[px][py-i]);
							}
						}else if(secondPlayer.getTeam().contains(champion)==true) {
							if(firstPlayer.getTeam().contains((Champion)board[px][py-i])==true) {
								targets.add((Champion)board[px][py-i]);
							}
						}
					}else if(board[px][py-i] instanceof Cover) {
						targets.add((Cover)board[px][py-i]);
					}
				}
			}
		}else if(d==Direction.UP) {
			for(int i=1; i<=chamAttRange; i++) {
				if(px+i>4) {
					break;
				}else if(board[px+i][py]!=null){
					if(board[px+i][py] instanceof Champion) {
						if(firstPlayer.getTeam().contains(champion)==true) {
							if(secondPlayer.getTeam().contains((Champion)board[px+i][py])==true) {
								targets.add((Champion)board[px+i][py]);
							}
						}else if(secondPlayer.getTeam().contains(champion)==true) {
							if(firstPlayer.getTeam().contains((Champion)board[px+i][py])==true) {
								targets.add((Champion)board[px+i][py]);
							}
						}
					}else if(board[px+i][py] instanceof Cover) {
						targets.add((Cover)board[px+i][py]);
					}
				}
			}
		}else if(d==Direction.DOWN) {
			for(int i=1; i<=chamAttRange; i++) {
				if(px-i<0) {
					break;
				}else if(board[px-i][py]!=null){
					if(board[px-i][py] instanceof Champion) {
						if(firstPlayer.getTeam().contains(champion)==true) {
							if(secondPlayer.getTeam().contains((Champion)board[px-i][py])==true) {
								targets.add((Champion)board[px-i][py]);
							}
						}else if(secondPlayer.getTeam().contains(champion)==true) {
							if(firstPlayer.getTeam().contains((Champion)board[px-i][py])==true) {
								targets.add((Champion)board[px-i][py]);
							}
						}
					}else if(board[px-i][py] instanceof Cover) {
						targets.add((Cover)board[px-i][py]);
					}
				}
			}
		}
		return targets;
	}

	public ArrayList<Damageable> getHealedDirectionalChampions(Champion champion,Point championLocation, Direction d){
		int chamAttRange = champion.getAttackRange();
		int px = championLocation.x;
		int py = championLocation.y;
		ArrayList<Damageable> targets = new ArrayList<Damageable>();
		if(d==Direction.RIGHT) {
			for(int i=1; i<=chamAttRange; i++) {
				if(py+i>4) {
					break;
				}else if(board[px][py+i]!=null){
					if(board[px][py+i] instanceof Champion) {
						if(firstPlayer.getTeam().contains(champion)==true) {
							if(firstPlayer.getTeam().contains((Champion)board[px][py+i])==true) {
								targets.add((Champion)board[px][py+i]);
							}
						}else if(secondPlayer.getTeam().contains(champion)==true) {
							if(secondPlayer.getTeam().contains((Champion)board[px][py+i])==true) {
								targets.add((Champion)board[px][py+i]);
							}
						}
					}
				}
			}
		}else if(d==Direction.LEFT) { 
			for(int i=1; i<=chamAttRange; i++) {
				if(py-i<0) {
					break;
				}else if(board[px][py-i]!=null){
					if(board[px][py-i] instanceof Champion) {
						if(firstPlayer.getTeam().contains(champion)==true) {
							if(firstPlayer.getTeam().contains((Champion)board[px][py-i])==true) {
								targets.add((Champion)board[px][py-i]);
							}
						}else if(secondPlayer.getTeam().contains(champion)==true) {
							if(secondPlayer.getTeam().contains((Champion)board[px][py-i])==true) {
								targets.add((Champion)board[px][py-i]);
							}
						}
					}
				}
			}
		}else if(d==Direction.UP) {
			for(int i=1; i<=chamAttRange; i++) {
				if(px+i>4) {
					break;
				}else if(board[px+i][py]!=null){
					if(board[px+i][py] instanceof Champion) {
						if(firstPlayer.getTeam().contains(champion)==true) {
							if(firstPlayer.getTeam().contains((Champion)board[px+i][py])==true) {
								targets.add((Champion)board[px+i][py]);
							}
						}else if(secondPlayer.getTeam().contains(champion)==true) {
							if(secondPlayer.getTeam().contains((Champion)board[px+i][py])==true) {
								targets.add((Champion)board[px+i][py]);
							}
						}
					}
				}
			}
		}else if(d==Direction.DOWN) {
			for(int i=1; i<=chamAttRange; i++) {
				if(px-i<0) {
					break;
				}else if(board[px-i][py]!=null){
					if(board[px-i][py] instanceof Champion) {
						if(firstPlayer.getTeam().contains(champion)==true) {
							if(firstPlayer.getTeam().contains((Champion)board[px-i][py])==true) {
								targets.add((Champion)board[px-i][py]);
							}
						}else if(secondPlayer.getTeam().contains(champion)==true) {
							if(secondPlayer.getTeam().contains((Champion)board[px-i][py])==true) {
								targets.add((Champion)board[px-i][py]);
							}
						}
					}
				}
			}
		}
		return targets;	
	}

	public ArrayList<Damageable> getEffectedDirectionalChampions(Champion champion,Point championLocation, Direction d,EffectType type){
		int chamAttRange = champion.getAttackRange();
		int px = championLocation.x;
		int py = championLocation.y;
		ArrayList<Damageable> targets = new ArrayList<Damageable>();
		if(d==Direction.RIGHT) {
			for(int i=1; i<=chamAttRange; i++) {
				if(py+i>4) {
					break;
				}else if(board[px][py+i]!=null){
					if(board[px][py+i] instanceof Champion) {
						if(type==EffectType.BUFF) {
							if(firstPlayer.getTeam().contains(champion)==true) {
								if(firstPlayer.getTeam().contains((Champion)board[px][py+i])==true) {
									targets.add((Champion)board[px][py+i]);
								}
							}else if(secondPlayer.getTeam().contains(champion)==true) {
								if(secondPlayer.getTeam().contains((Champion)board[px][py+i])==true) {
									targets.add((Champion)board[px][py+i]);
								}
							}
						}else if(type==EffectType.DEBUFF) {
							if(firstPlayer.getTeam().contains(champion)==true) {
								if(secondPlayer.getTeam().contains((Champion)board[px][py+i])==true) {
									targets.add((Champion)board[px][py+i]);
								}
							}else if(secondPlayer.getTeam().contains(champion)==true) {
								if(firstPlayer.getTeam().contains((Champion)board[px][py+i])==true) {
									targets.add((Champion)board[px][py+i]);
								}
							}
						}
					}else if(board[px][py+i] instanceof Cover) {
						targets.add((Cover)board[px][py+i]);
					}
				}
			}
		}else if(d==Direction.LEFT) { 
			for(int i=1; i<=chamAttRange; i++) {
				if(py-i<0) {
					break;
				}else if(board[px][py-i]!=null){
					if(board[px][py-i] instanceof Champion) {
						if(type==EffectType.BUFF) {
							if(firstPlayer.getTeam().contains(champion)==true) {
								if(firstPlayer.getTeam().contains((Champion)board[px][py-i])==true) {
									targets.add((Champion)board[px][py-i]);
								}
							}else if(secondPlayer.getTeam().contains(champion)==true) {
								if(secondPlayer.getTeam().contains((Champion)board[px][py-i])==true) {
									targets.add((Champion)board[px][py-i]);
								}
							}
						}else if(type==EffectType.DEBUFF) {
							if(firstPlayer.getTeam().contains(champion)==true) {
								if(secondPlayer.getTeam().contains((Champion)board[px][py-i])==true) {
									targets.add((Champion)board[px][py-i]);
								}
							}else if(secondPlayer.getTeam().contains(champion)==true) {
								if(firstPlayer.getTeam().contains((Champion)board[px][py-i])==true) {
									targets.add((Champion)board[px][py-i]);
								}
							}
						}
					}else if(board[px][py-i] instanceof Cover) {
						targets.add((Cover)board[px][py-i]);
					}
				}
			}
		}else if(d==Direction.UP) {
			for(int i=1; i<=chamAttRange; i++) {
				if(px+i>4) {
					break;
				}else if(board[px+i][py]!=null){
					if(board[px+i][py] instanceof Champion) {
						if(type==EffectType.BUFF) {
							if(firstPlayer.getTeam().contains(champion)==true) {
								if(firstPlayer.getTeam().contains((Champion)board[px+i][py])==true) {
									targets.add((Champion)board[px+i][py]);
								}
							}else if(secondPlayer.getTeam().contains(champion)==true) {
								if(secondPlayer.getTeam().contains((Champion)board[px+i][py])==true) {
									targets.add((Champion)board[px+i][py]);
								}
							}
						}else if(type==EffectType.DEBUFF) {
							if(firstPlayer.getTeam().contains(champion)==true) {
								if(secondPlayer.getTeam().contains((Champion)board[px+i][py])==true) {
									targets.add((Champion)board[px+i][py]);
								}
							}else if(secondPlayer.getTeam().contains(champion)==true) {
								if(firstPlayer.getTeam().contains((Champion)board[px+i][py])==true) {
									targets.add((Champion)board[px+i][py]);
								}
							}
						}
					}else if(board[px+i][py] instanceof Cover) {
						targets.add((Cover)board[px+i][py]);
					}
				}
			}
		}else if(d==Direction.DOWN) {
			for(int i=1; i<=chamAttRange; i++) {
				if(px-i<0) {
					break;
				}else if(board[px-i][py]!=null){
					if(board[px-i][py] instanceof Champion) {
						if(type==EffectType.BUFF) {
							if(firstPlayer.getTeam().contains(champion)==true) {
								if(firstPlayer.getTeam().contains((Champion)board[px-i][py])==true) {
									targets.add((Champion)board[px-i][py]);
								}
							}else if(secondPlayer.getTeam().contains(champion)==true) {
								if(secondPlayer.getTeam().contains((Champion)board[px-i][py])==true) {
									targets.add((Champion)board[px-i][py]);
								}
							}
						}else if(type==EffectType.DEBUFF) {
							if(firstPlayer.getTeam().contains(champion)==true) {
								if(secondPlayer.getTeam().contains((Champion)board[px-i][py])==true) {
									targets.add((Champion)board[px-i][py]);
								}
							}else if(secondPlayer.getTeam().contains(champion)==true) {
								if(firstPlayer.getTeam().contains((Champion)board[px-i][py])==true) {
									targets.add((Champion)board[px-i][py]);
								}
							}
						}
					}else if(board[px-i][py] instanceof Cover) {
						targets.add((Cover)board[px-i][py]);
					}
				}
			}
		}
		return targets;	
	}

	public void castAbility(Ability a, Direction d) throws AbilityUseException, NotEnoughResourcesException, CloneNotSupportedException  {
		Champion champion = getCurrentChampion();
		if(a.getCurrentCooldown()!=0)
			throw new AbilityUseException("You cannot cast this ability now");
		if(a.getRequiredActionPoints()>champion.getCurrentActionPoints())
			throw new NotEnoughResourcesException("You don't have enough action points");
		if(a.getManaCost()>champion.getMana())
			throw new NotEnoughResourcesException("You don't have enough Mana points");
		for(int i=0;i<champion.getAppliedEffects().size();i++) {
			if(champion.getAppliedEffects().get(i) instanceof Silence) {
				throw new AbilityUseException("You cannot cast this ability now");
			}
		}
		ArrayList<Damageable> Targets = new ArrayList<Damageable>();
		Point championLocation=champion.getLocation();
		if(a instanceof DamagingAbility) {
			if (a.getCastArea()==AreaOfEffect.DIRECTIONAL) {
				ArrayList <Damageable> temp = new ArrayList<Damageable>();
				temp=getAttackedDirectionalChampions(champion,championLocation, d);
				for(int i=0;i<temp.size();i++) {
					Targets.add(temp.get(i));
				}
			}
		}else if(a instanceof HealingAbility) {
			if (a.getCastArea()==AreaOfEffect.DIRECTIONAL) {
				ArrayList <Damageable> temp = new ArrayList<Damageable>();
				temp=getHealedDirectionalChampions(champion,championLocation, d);
				for(int i=0;i<temp.size();i++) {
					Targets.add(temp.get(i));
				}
			}
		}else if(a instanceof CrowdControlAbility) {
			if (a.getCastArea()==AreaOfEffect.DIRECTIONAL) {
				ArrayList <Damageable> temp = new ArrayList<Damageable>();
				temp=getEffectedDirectionalChampions(champion,championLocation, d,((CrowdControlAbility)a).getEffect().getType());
				for(int i=0;i<temp.size();i++) {
					Targets.add(temp.get(i));
				}
			}
		}
		a.execute(Targets);

		for(int i=0;i<Targets.size();i++) {
			Damageable damg = Targets.get(i);
			if(damg.getCurrentHP()<=0) {
				board[damg.getLocation().x][damg.getLocation().y]=null;
				if(damg instanceof Champion) {
					Champion c = (Champion)Targets.get(i);
					c.setCondition(Condition.KNOCKEDOUT);
					ArrayList<Champion> team1 =firstPlayer.getTeam();
					ArrayList<Champion> team2 =secondPlayer.getTeam();
					if(team1.contains(c)) {
						firstPlayer.getTeam().remove(c);
					}
					else if(team2.contains(c)) {
						secondPlayer.getTeam().remove(c);
					}
					ArrayList<Champion> turnOrdertemp = new  ArrayList<Champion>();
					while(!turnOrder.isEmpty()) {
						Champion removedChampion = (Champion)turnOrder.remove();
						if(removedChampion!=c) {
							turnOrdertemp.add(removedChampion);
						}
					}
					for(int j=0;j<turnOrdertemp.size();j++) {
						turnOrder.insert(turnOrdertemp.get(j));
					}
				}
				else if(Targets.get(i) instanceof Cover) {
					board[damg.getLocation().x][damg.getLocation().y]=null;
				}	
			}
		}
		
		champion.setCurrentActionPoints(champion.getCurrentActionPoints()-a.getRequiredActionPoints());
		champion.setMana(champion.getMana()-a.getManaCost());
		a.setCurrentCooldown(a.getBaseCooldown());
		
	}
	
	public void castAbility(Ability a, int x, int y) throws AbilityUseException, NotEnoughResourcesException, CloneNotSupportedException, InvalidTargetException {
		Champion champion = getCurrentChampion();
		Point championLocation = champion.getLocation();
		if(a.getCurrentCooldown()!=0)
			throw new AbilityUseException("You cannot cast this ability now");
		if(a.getRequiredActionPoints()>champion.getCurrentActionPoints())
			throw new NotEnoughResourcesException("You don't have enough action points");
		if(a.getManaCost()>champion.getMana())
			throw new NotEnoughResourcesException("You don't have enough Mana points");
		for(int i=0;i<champion.getAppliedEffects().size();i++) {
			if(champion.getAppliedEffects().get(i) instanceof Silence) {
				throw new AbilityUseException("You cannot cast this ability now");
			}
		}
		if(board[x][y]==null) {
			throw new InvalidTargetException("place is empty");
		}
		if(x<0 || x>4 || y<0 || y>4 ){
			throw new InvalidTargetException("place is null");
		}
		ArrayList<Damageable> target = new ArrayList<Damageable>();
		int range = a.getCastRange();
		Damageable d = (Damageable)board[x][y];
		Point targetLocation = new Point(x,y);
		int distance = calculateManhattanDistance(targetLocation,championLocation);
		if(a.getCastArea()==AreaOfEffect.SINGLETARGET) {				
			if(a instanceof CrowdControlAbility) {
				if(distance<=range) {
					EffectType type =((CrowdControlAbility)a).getEffect().getType();
					if(d instanceof Champion ) {						
						if(type==EffectType.BUFF) {
							if(x==championLocation.x && y==championLocation.y) {
								target.add(d);								
							}
							else if(firstPlayer.getTeam().contains(champion) && firstPlayer.getTeam().contains((Champion)d)) {
								target.add(d);
							}	
							else if(secondPlayer.getTeam().contains(champion) && secondPlayer.getTeam().contains((Champion)d)) {
								target.add(d);
							}
							else if(firstPlayer.getTeam().contains(champion) && secondPlayer.getTeam().contains((Champion)d)) {
								throw new InvalidTargetException("you cannot buff enemy");
							}
							else if(firstPlayer.getTeam().contains(champion) && secondPlayer.getTeam().contains((Champion)d)) {
								throw new InvalidTargetException("you cannot buff enemy");
							}														
						}
						else if(type==EffectType.DEBUFF) {
							if(x==championLocation.x && y==championLocation.y) {
								throw new InvalidTargetException("you cannot apply bad effect on yourself");
							}
							else if(firstPlayer.getTeam().contains(champion) && secondPlayer.getTeam().contains((Champion)d)) {
								target.add(d);
							}	
							else if(secondPlayer.getTeam().contains(champion) && firstPlayer.getTeam().contains((Champion)d)) {
								target.add(d);
							}
							else if(firstPlayer.getTeam().contains(champion) && firstPlayer.getTeam().contains((Champion)d)) {
								throw new InvalidTargetException("you cannot buff friend");
							}
							else if(secondPlayer.getTeam().contains(champion) && secondPlayer.getTeam().contains((Champion)d)) {
								throw new InvalidTargetException("you cannot buff friend");
							}
						}						
					}
					else if(d instanceof Cover){
						throw new InvalidTargetException("you cannot apply effect on cover");
					}
				}
				else {
					throw new AbilityUseException("out of range");
				}
			}	
				else if(a instanceof HealingAbility) {
					if(distance<=range) {
						if(d instanceof Champion) {
							if(firstPlayer.getTeam().contains(champion) && firstPlayer.getTeam().contains((Champion)d)) {
								target.add(d);
							}	
							else if(secondPlayer.getTeam().contains(champion) && secondPlayer.getTeam().contains((Champion)d)) {
								target.add(d);
							}
							else if(firstPlayer.getTeam().contains(champion) && secondPlayer.getTeam().contains((Champion)d)) {
								throw new InvalidTargetException("you cannot heal enemy");
							}
							else if(firstPlayer.getTeam().contains(champion) && secondPlayer.getTeam().contains((Champion)d)) {
								throw new InvalidTargetException("you cannot heal enemy");
							}
						}
						else if(d instanceof Cover){
							throw new InvalidTargetException("you cannot heal cover");
						}
					}
					else {
						throw new AbilityUseException("out of range");
					}
				}	
				else if(a instanceof DamagingAbility) {
					if(distance<=range) {
						if(d instanceof Champion ) {
							if(x==championLocation.x && y==championLocation.y) {
								throw new InvalidTargetException("you cannot damage yourself");
							}
							else if(firstPlayer.getTeam().contains(champion) && secondPlayer.getTeam().contains((Champion)d)) {
								target.add(d);
							}	
							else if(secondPlayer.getTeam().contains(champion) && firstPlayer.getTeam().contains((Champion)d)) {
								target.add(d);
							}
							else if(firstPlayer.getTeam().contains(champion) && firstPlayer.getTeam().contains((Champion)d)) {
								throw new InvalidTargetException("you cannot damage friend");
							}
							else if(secondPlayer.getTeam().contains(champion) && secondPlayer.getTeam().contains((Champion)d)) {
								throw new InvalidTargetException("you cannot damage friend");
							}
						}
						else if(d instanceof Cover){
							target.add(d);
						}
					}
					else {
						throw new AbilityUseException("out of range");
					}
				}	
			a.execute(target);
			for(int i=0;i<target.size();i++) {
				Damageable damg = target.get(i);
				if(damg.getCurrentHP()<=0) {
					board[damg.getLocation().x][damg.getLocation().y]=null;
					if(damg instanceof Champion) {
						Champion c = (Champion)target.get(i);
						c.setCondition(Condition.KNOCKEDOUT);
						ArrayList<Champion> team1 =firstPlayer.getTeam();
						ArrayList<Champion> team2 =secondPlayer.getTeam();
						if(team1.contains(c)) {
							firstPlayer.getTeam().remove(c);
						}
						else if(team2.contains(c)) {
							secondPlayer.getTeam().remove(c);
						}
						ArrayList<Champion> turnOrdertemp = new  ArrayList<Champion>();
						while(!turnOrder.isEmpty()) {
							Champion removedChampion = (Champion)turnOrder.remove();
							if(removedChampion!=c) {
								turnOrdertemp.add(removedChampion);
							}
						}
						for(int j=0;j<turnOrdertemp.size();j++) {
							turnOrder.insert(turnOrdertemp.get(j));
						}
					}
					else if(target.get(i) instanceof Cover) {
						board[damg.getLocation().x][damg.getLocation().y]=null;
					}	
				}
			}	
			champion.setCurrentActionPoints(champion.getCurrentActionPoints()-a.getRequiredActionPoints());
			champion.setMana(champion.getMana()-a.getManaCost());
			a.setCurrentCooldown(a.getBaseCooldown());
		}
	}
	
	public void useLeaderAbility() throws LeaderNotCurrentException, LeaderAbilityAlreadyUsedException {
		Champion champion = getCurrentChampion();
		ArrayList<Champion> team = new ArrayList<Champion>();
		
		if(champion!=firstPlayer.getLeader() && champion!=secondPlayer.getLeader()) {
			throw new LeaderNotCurrentException("you arenot a leader");
		}
		if ((firstPlayer.getTeam ().contains(champion) && firstLeaderAbilityUsed == true) || secondPlayer.getTeam().contains(champion) && secondLeaderAbilityUsed == true) {
			throw new LeaderAbilityAlreadyUsedException("Leader ability already used before");
		}	
		if (champion instanceof Hero) {
			if (firstPlayer.getTeam().contains(champion)) {
				team = firstPlayer.getTeam();
			}
			else {
				team = secondPlayer.getTeam();
			}	
		}	
		else if (champion instanceof Villain) {
			if (firstPlayer.getTeam().contains(champion)==true) {
				for(int j=0;j<secondPlayer.getTeam().size();j++) {
					if (secondPlayer.getTeam().get(j).getCurrentHP()/secondPlayer.getTeam().get(j).getMaxHP()<0.3) {				
						team.add(secondPlayer.getTeam().get(j));
					}
				}
				
			}
			else if (secondPlayer.getTeam().contains(champion)==true){
				for(int j=0;j<firstPlayer.getTeam().size();j++) {
					if (firstPlayer.getTeam().get(j).getCurrentHP()/firstPlayer.getTeam().get(j).getMaxHP()<0.3) {
						team.add(firstPlayer.getTeam().get(j));
					}
				}
			}
		}
		else if (champion instanceof AntiHero) {
			for (int i = 0; i < firstPlayer.getTeam().size(); i++) {
				if (firstPlayer.getTeam().get(i) != firstPlayer.getLeader()){
					team.add(firstPlayer.getTeam().get(i));
				}	
			}	
			for (int i = 0; i< secondPlayer.getTeam().size(); i++){
				if (secondPlayer.getTeam().get(i) != secondPlayer.getLeader()){
					team.add(secondPlayer.getTeam().get(i));
				}
			}
		}
		
		champion.useLeaderAbility(team);
		
		if(champion instanceof Villain){
			if(firstPlayer.getTeam().contains(champion)) {
				for(int i=0;i<team.size();i++) {
					secondPlayer.getTeam().remove(team.get(i));
					ArrayList<Champion> turnOrdertemp = new  ArrayList<Champion>();
					while(!turnOrder.isEmpty()) {
						Champion removedChampion = (Champion)turnOrder.remove();
						if(removedChampion!=team.get(i)) {
							turnOrdertemp.add(removedChampion);
						}
					}
					for(int j=0;j<turnOrdertemp.size();j++) {
						turnOrder.insert(turnOrdertemp.get(j));
					}
				}
			}
			else {
				for(int i=0;i<team.size();i++) {
					firstPlayer.getTeam().remove(team.get(i));
					ArrayList<Champion> turnOrdertemp = new  ArrayList<Champion>();
					while(!turnOrder.isEmpty()) {
						Champion removedChampion = (Champion)turnOrder.remove();
						if(removedChampion!=team.get(i)) {
							turnOrdertemp.add(removedChampion);
						}
					}
					for(int j=0;j<turnOrdertemp.size();j++) {
						turnOrder.insert(turnOrdertemp.get(j));
					}
				}
			}
		}
				
		if(firstPlayer.getTeam().contains(champion)) {
			firstLeaderAbilityUsed=true;
		}
		else if (secondPlayer.getTeam().contains(champion)) {
			secondLeaderAbilityUsed=true;
		}
		
	}
	

	public void endTurn() {
		turnOrder.remove();
		if(turnOrder.isEmpty()) {
			prepareChampionTurns();
		}
		while(!turnOrder.isEmpty() && ((Champion)turnOrder.peekMin()).getCondition()==Condition.INACTIVE) {
			turnOrder.remove();
		}
		Champion c = (Champion)turnOrder.peekMin();
	
		for(int i=0;i<firstPlayer.getTeam().size();i++) {
			Champion champ = firstPlayer.getTeam().get(i);
			for(int j=0;j<champ.getAbilities().size();j++) {
				Ability a =champ.getAbilities().get(j);
				a.setCurrentCooldown(a.getCurrentCooldown()-1);
			}
			for(int z=0;z<champ.getAppliedEffects().size();z++) {
				Effect e = champ.getAppliedEffects().get(z);
				e.setDuration(e.getDuration()-1);
				if(e.getDuration()==0 ) {
					e.remove(champ);
					champ.getAppliedEffects().remove(e);
				}				
			}
		}
		for(int i=0;i<secondPlayer.getTeam().size();i++) {
			Champion champ = secondPlayer.getTeam().get(i);
			for(int j=0;j<champ.getAbilities().size();j++) {
				Ability a =champ.getAbilities().get(j);
				a.setCurrentCooldown(a.getCurrentCooldown()-1);
			}
			for(int z=0;z<champ.getAppliedEffects().size();z++) {
				Effect e = champ.getAppliedEffects().get(z);
				e.setDuration(e.getDuration()-1);
				if(e.getDuration()==0 ) {
					e.remove(champ);
					champ.getAppliedEffects().remove(e);
				}				
			}
		}
	
		c.setCurrentActionPoints(c.getMaxActionPointsPerTurn());
	}
	
	
	private void prepareChampionTurns() {
		for(int i=0; i<firstPlayer.getTeam().size();i++) {
			Comparable<Object> champion = firstPlayer.getTeam().get(i);
			if(((Champion) champion).getCondition()==Condition.ACTIVE||((Champion) champion).getCondition()==Condition.ROOTED) {
				turnOrder.insert(champion);
			}
		}
		for(int i=0; i<secondPlayer.getTeam().size();i++) {
			Comparable<Object> champion = secondPlayer.getTeam().get(i);
			if(((Champion) champion).getCondition()==Condition.ACTIVE||((Champion) champion).getCondition()==Condition.ROOTED) {
					turnOrder.insert(champion);
			}
		}	
	}
	
}
 