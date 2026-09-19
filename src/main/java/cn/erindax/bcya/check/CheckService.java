package cn.erindax.bcya.check;

import cn.erindax.bcya.card.CardData;
import cn.erindax.bcya.card.SkillTable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;

public final class CheckService {

	public record Info(String skill, int level, int checkValue, int modifier, int pool,
			boolean unskilled, boolean empathic, boolean blocked, int emotion) {

		public CompoundTag toTag() {
			CompoundTag tag = new CompoundTag();
			tag.putString("skill", skill);
			tag.putInt("level", level);
			tag.putInt("check_value", checkValue);
			tag.putInt("modifier", modifier);
			tag.putInt("pool", pool);
			tag.putBoolean("unskilled", unskilled);
			tag.putBoolean("empathic", empathic);
			tag.putBoolean("blocked", blocked);
			tag.putInt("emotion", emotion);
			return tag;
		}
	}

	public enum Grade {
		CRITICAL_SUCCESS,
		SUCCESS,
		FAIL,
		CRITICAL_FAIL;

		public String id() {
			return switch (this) {
				case CRITICAL_SUCCESS -> "critical_success";
				case SUCCESS -> "success";
				case FAIL -> "fail";
				case CRITICAL_FAIL -> "critical_fail";
			};
		}

		public boolean passed() {
			return this == CRITICAL_SUCCESS || this == SUCCESS;
		}

		public static Grade of(int successes, int difficulty, int[] dice) {
			boolean allOne = dice.length > 0;
			boolean allSix = dice.length > 0;
			for (int value : dice) {
				if (value != 1) {
					allOne = false;
				}
				if (value != 6) {
					allSix = false;
				}
			}
			if (allOne) {
				return CRITICAL_FAIL;
			}
			if (allSix) {
				return CRITICAL_SUCCESS;
			}
			if (successes >= difficulty + 2) {
				return CRITICAL_SUCCESS;
			}
			if (successes >= difficulty) {
				return SUCCESS;
			}
			if (successes <= 0) {
				return CRITICAL_FAIL;
			}
			return FAIL;
		}
	}

	public record Outcome(Info info, int difficulty, int[] dice, int successes, boolean success, Grade grade) {

		public CompoundTag toTag() {
			CompoundTag tag = info.toTag();
			tag.putInt("difficulty", difficulty);
			tag.putIntArray("dice", dice);
			tag.putInt("successes", successes);
			tag.putBoolean("success", grade.passed());
			tag.putString("outcome", grade.id());
			return tag;
		}
	}

	private CheckService() {
	}

	public static Info inspect(CompoundTag card, String skill) {
		int level = CardData.skill(card, skill);
		int checkValue = CardData.checkValue(card, skill);
		int modifier = CardData.checkModifier(checkValue);
		boolean unskilled = level <= 0;
		boolean empathic = SkillTable.isEmpathic(skill);
		int emotion = CardData.emotion(card);
		boolean blocked = emotion <= CardData.EMOTION_BLOCK_MAX;
		int pool;
		if (unskilled) {
			pool = 1;
		} else {
			pool = Math.max(1, level + modifier);
			if (emotion >= CardData.EMOTION_HIGH && empathic) {
				pool += 1;
			}
		}
		return new Info(skill, level, checkValue, modifier, pool, unskilled, empathic, blocked, emotion);
	}

	public static Outcome roll(Info info, int difficulty, RandomSource random) {
		if (info.blocked()) {
			return new Outcome(info, difficulty, new int[0], 0, false, Grade.FAIL);
		}
		int[] dice = new int[info.pool()];
		int successes = 0;
		for (int i = 0; i < dice.length; i++) {
			int value = random.nextInt(CardData.DICE_FACES) + 1;
			dice[i] = value;
			if (info.unskilled()) {
				if (value == CardData.UNTRAINED_SUCCESS) {
					successes++;
				}
			} else if (value >= CardData.DICE_SUCCESS_MIN) {
				successes++;
			}
		}
		if (info.empathic() && info.emotion() >= CardData.EMOTION_LOW_MIN && info.emotion() <= CardData.EMOTION_LOW_MAX) {
			successes = Math.max(0, successes - 1);
		}
		Grade grade = Grade.of(successes, difficulty, dice);
		return new Outcome(info, difficulty, dice, successes, grade.passed(), grade);
	}
}
