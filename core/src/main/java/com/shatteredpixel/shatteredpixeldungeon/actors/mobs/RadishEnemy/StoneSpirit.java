package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.RadishEnemy;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AscensionChallenge;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barkskin;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Berserk;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ChampionEnemy;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ChampionHero;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FireImbue;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FrostImbue;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Fury;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hex;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Preparation;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.DeathMark;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Endure;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.AfterImage;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.CloakofGreyFeather;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Viscosity;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfTenacity;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfChallenge;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.FogSword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Scythe;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingStone;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.RadishEnemySprite.StoneSpiritSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

public class StoneSpirit extends Mob {
    {
        spriteClass = StoneSpiritSprite.class;

        HP = HT = 120;
        defenseSkill = 5;


        EXP = 9;
        maxLvl = 19;

        properties.add(Property.INORGANIC);

        loot = new ThrowingStone().quantity(Random.Int(1,5));
        lootChance = 1f;
    }

    private boolean isNoticed = false;

    private boolean standOnPure = true;

    public int damageRoll() {
        return standOnPure?Random.NormalIntRange( 10, 25 ):Random.NormalIntRange(4,8);
    }
    @Override
    public int drRoll() {
        return standOnPure?Random.NormalIntRange(5, 10):Random.NormalIntRange(0,5);
    }

    @Override
    public int attackSkill( Char target ) {
        return 25;
    }

    @Override
    public void die( Object cause ) {
        super.die( cause );
    }
    public boolean attack( Char enemy, float dmgMulti, float dmgBonus, float accMulti ) {

        if (enemy == null) return false;

        boolean visibleFight = Dungeon.level.heroFOV[pos] || Dungeon.level.heroFOV[enemy.pos];

        if (enemy.isInvulnerable(getClass())) {

            if (visibleFight) {
                enemy.sprite.showStatus( CharSprite.POSITIVE, Messages.get(this, "invulnerable") );

                Sample.INSTANCE.play(Assets.Sounds.HIT_PARRY, 1f, Random.Float(0.96f, 1.05f));
            }

            return false;

        } else if (hit( this, enemy, accMulti )) {
            if (enemy.buff(AfterImage.Blur.class)!=null){
                enemy.buff(AfterImage.Blur.class).gainDodge();
            }
            int dr = Math.round(enemy.drRoll() * AscensionChallenge.statModifier(enemy));

            Barkskin bark = enemy.buff(Barkskin.class);
            if (bark != null)   dr += Random.NormalIntRange( 0 , bark.level() );

            //we use a float here briefly so that we don't have to constantly round while
            // potentially applying various multiplier effects
            float dmg;
            Preparation prep = buff(Preparation.class);
            if (prep != null){
                dmg = prep.damageRoll(this);
            } else {
                dmg = damageRoll();
            }
            boolean crit=false;
            boolean surprise =enemy instanceof Mob && ((Mob) enemy).surprisedBy(this);
            float current_crit=critSkill(),current_critdamage=critDamage();

            current_critdamage=Math.min(current_critdamage,critDamageCap);
            if (this.buff(Scythe.scytheSac.class)!=null){
                current_crit+=10f;
                current_critdamage+=0.1f;
            }

            if (this.buff(RingOfTenacity.Tenacity.class)!=null) {current_crit=0;}
            if (Random.Float()*100<current_crit || crit) {
                dmg*=current_critdamage;
                crit = true;
            }

            dmg = Math.round(dmg*dmgMulti);

            Berserk berserk = buff(Berserk.class);
            if (berserk != null) dmg = berserk.damageFactor(dmg);

            if (buff( Fury.class ) != null) {
                dmg *= 1.5f;
            }
            if (buff(RingOfTenacity.Tenacity.class)!=null){
                dmg*=RingOfTenacity.attackMultiplier(this);
            }
            for (ChampionEnemy buff : buffs(ChampionEnemy.class)){
                dmg *= buff.meleeDamageFactor();
            }
            for (ChampionHero buff : buffs(ChampionHero.class)){
                dmg *= buff.meleeDamageFactor();
            }
            dmg *= AscensionChallenge.statModifier(this);

            //flat damage bonus is applied after positive multipliers, but before negative ones
            dmg += dmgBonus;

            //friendly endure
            Endure.EndureTracker endure = buff(Endure.EndureTracker.class);
            if (endure != null) dmg = endure.damageFactor(dmg);

            //enemy endure
            endure = enemy.buff(Endure.EndureTracker.class);
            if (endure != null){
                dmg = endure.adjustDamageTaken(dmg);
            }

            if (enemy.buff(ScrollOfChallenge.ChallengeArena.class) != null){
                dmg *= 0.67f;
            }

            if ( buff(Weakness.class) != null ){
                dmg *= 0.67f;
            }

            int effectiveDamage = enemy.defenseProc( this, Math.round(dmg) );

            // created by DoggingDog on 20240718
            // for Torturer using

            if (enemy.buff(Viscosity.ViscosityTracker.class) != null){
                effectiveDamage = enemy.buff(Viscosity.ViscosityTracker.class).deferDamage(effectiveDamage);
                enemy.buff(Viscosity.ViscosityTracker.class).detach();
            }

            //vulnerable specifically applies after armor reductions
            if ( enemy.buff( Vulnerable.class ) != null){
                effectiveDamage *= 1.33f;
            }

            effectiveDamage = attackProc( enemy, effectiveDamage );

            if (visibleFight) {
                if (effectiveDamage > 0 || !enemy.blockSound(Random.Float(0.96f, 1.05f))) {
                    hitSound(Random.Float(0.87f, 1.15f));
                }
            }

            // If the enemy is already dead, interrupt the attack.
            // This matters as defence procs can sometimes inflict self-damage, such as armor glyphs.
            if (!enemy.isAlive()){
                return true;
            }

            if(crit){
                enemy.sprite.showStatus(CharSprite.NEGATIVE,Messages.get(this,"crit"));
            }
            enemy.damage( effectiveDamage, this );

            if (buff(FireImbue.class) != null)  buff(FireImbue.class).proc(enemy);
            if (buff(FrostImbue.class) != null) buff(FrostImbue.class).proc(enemy);

            if (enemy.isAlive() && enemy.alignment != alignment && prep != null && prep.canKO(enemy)){
                enemy.HP = 0;
                if (!enemy.isAlive()) {
                    enemy.die(this);
                } else {
                    //helps with triggering any on-damage effects that need to activate
                    enemy.damage(-1, this);
                    DeathMark.processFearTheReaper(enemy);
                }
                enemy.sprite.showStatus(CharSprite.NEGATIVE, Messages.get(Preparation.class, "assassinated"));
            }

            enemy.sprite.bloodBurstA( sprite.center(), effectiveDamage );
            enemy.sprite.flash();

            if (!enemy.isAlive() && visibleFight && Dungeon.hero != null) {
                if (enemy == Dungeon.hero) {

                    Dungeon.fail( getClass() );
                    GLog.n( Messages.capitalize(Messages.get(StoneSpirit.class, "kill")) );

                }
            }
            return true;
        } else {
            if (enemy.buff(CloakofGreyFeather.hexDodge.class)!=null){
                for (Char ch : Actor.chars()) {
                    if (ch.alignment != enemy.alignment && enemy.fieldOfView[ch.pos] && ch.alignment!= Alignment.NEUTRAL){
                        Buff.affect(ch, Hex.class,2f+0.75f*enemy.buff(CloakofGreyFeather.hexDodge.class).buffedLvl());
                    }
                }
            }
            enemy.sprite.showStatus( CharSprite.NEUTRAL, enemy.defenseVerb() );

            if(Dungeon.hero != null){
                if (Dungeon.hero.belongings.weapon() instanceof FogSword) {
                    Buff.affect(Dungeon.hero, Invisibility.class,1f);
                }
            }

            if (visibleFight) {
                Sample.INSTANCE.play(Assets.Sounds.MISS);
            }

            return false;

        }
    }
    @Override
    protected boolean act() {
        if(Dungeon.level.map[pos]== Terrain.EMPTY){
            standOnPure = true;
            if(Dungeon.hero != null && fieldOfView != null)
                if(fieldOfView[Dungeon.hero.pos] && !isNoticed){
                    GLog.n('\n'+ Messages.get(this, "boost"));
                    isNoticed = true;
                }
        }
        else {
            standOnPure=false;
        }
        return super.act();
    }
}
