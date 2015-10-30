/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015  Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2015 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.effects.Chains;
import com.shatteredpixel.shatteredpixeldungeon.effects.Pushing;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.sprites.GuardSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

public class Guard extends Mob {

	//they can only use their chains once
	private boolean chainsUsed = false;

	{
		name = "prison guard";
		spriteClass = GuardSprite.class;

		HP = HT = 30;
		defenseSkill = 10;

		EXP = 6;
		maxLvl = 14;

		loot = null;    //see createloot.
		lootChance = 1;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(4, 10);
	}

	@Override
	protected boolean act() {
		if (state == HUNTING &&
				Level.fieldOfView[target] &&
				Level.distance( pos, target ) < 4 && !Level.adjacent( pos, target ) &&
				chain(target)) {

			return false;

		} else {
			return super.act();
		}
	}

	private boolean chain(int target){
		if (chainsUsed)
			return false;

		Ballistica chain = new Ballistica(pos, target, Ballistica.PROJECTILE);

		if (chain.collisionPos != Dungeon.hero.pos)
			return false;
		else {
			int newPos = -1;
			for (int i : chain.subPath(1, chain.dist)){
				if (!Level.solid[i] && Actor.findChar(i) == null){
					newPos = i;
					break;
				}
			}

			if (newPos == -1){
				return false;
			} else {
				final int newHeroPos = newPos;
				yell("get over here!");
				sprite.parent.add(new Chains(pos, Dungeon.hero.pos, new Callback() {
					public void call() {
						Actor.addDelayed(new Pushing(Dungeon.hero, Dungeon.hero.pos, newHeroPos), -1);
						Dungeon.hero.pos = newHeroPos;
						Dungeon.observe();
						Dungeon.level.press(newHeroPos, Dungeon.hero);
						Cripple.prolong(Dungeon.hero, Cripple.class, 4f);
						next();
					}
				}));
			}
		}
		chainsUsed = true;
		return true;
	}

	@Override
	public int attackSkill( Char target ) {
		return 14;
	}

	@Override
	public int dr() {
		return 7;
	}

	@Override
	public String defenseVerb() {
		return "blocked";
	}

	@Override
	protected Item createLoot() {
		//first see if we drop armor, chance is 1/8 (0.125f)
		if (Random.Int(8) == 0){
			return Generator.randomArmor();
		//otherwise, we may drop a health potion. Chance is 1/(7+potions dropped)
		//including the chance for armor before it, effective droprate is ~1/(8 + (1.15*potions dropped))
		} else {
			if (Random.Int(7 + Dungeon.limitedDrops.guardHP.count) == 0){
				Dungeon.limitedDrops.guardHP.drop();
				return new PotionOfHealing();
			}
		}

		return null;
	}

	@Override
	public String description() {
		return ""; //TODO
	}

	private final String CHAINSUSED = "chainsused";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(CHAINSUSED, chainsUsed);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		chainsUsed = bundle.getBoolean(CHAINSUSED);
	}
}