package cn.erindax.bcya.client.check;

import cn.erindax.bcya.card.SkillTable;

public final class DicePreset {

	private static String skill = "知觉";
	private static int difficulty = 2;

	private DicePreset() {
	}

	public static String skill() {
		if (SkillTable.isKnown(skill)) {
			return skill;
		}
		return SkillTable.ABILITY_OF.keySet().iterator().next();
	}

	public static void setSkill(String value) {
		if (value != null && SkillTable.isKnown(value)) {
			skill = value;
		}
	}

	public static int difficulty() {
		return difficulty;
	}

	public static void setDifficulty(int value) {
		difficulty = Math.max(1, Math.min(4, value));
	}
}
