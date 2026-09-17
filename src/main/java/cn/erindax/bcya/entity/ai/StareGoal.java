package cn.erindax.bcya.entity.ai;

import cn.erindax.bcya.entity.PatrollerEntity;

import java.util.EnumSet;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

public class StareGoal extends Goal {

	private final PatrollerEntity mob;

	public StareGoal(PatrollerEntity mob) {
		this.mob = mob;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
	}

	@Override
	public boolean canUse() {
		return mob.getStareTarget() != null;
	}

	@Override
	public boolean canContinueToUse() {
		return canUse();
	}

	@Override
	public void start() {
		mob.getNavigation().stop();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		LivingEntity target = mob.getStareTarget();
		if (target != null) {
			mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
		}
	}
}
