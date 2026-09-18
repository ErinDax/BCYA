package cn.erindax.bcya.card;

import cn.erindax.bcya.item.ModItems;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class CardData {

	public static final String INVESTIGATOR = "investigator";
	public static final String OWNER_UUID = "owner_uuid";
	public static final String OWNER_NAME = "owner_name";
	public static final String OWNER_SKIN = "owner_skin";
	public static final String OWNER_SKIN_SIG = "owner_skin_sig";
	public static final String CARD_ID = "card_id";
	public static final String ABILITIES = "abilities";
	public static final String SKILLS = "skills";
	public static final String EMOTION = "emotion";
	public static final String EMOTION_DEEP = "emotion_deep";
	public static final String AWAKENED = "awakened";
	public static final String ABILITY_NAME = "ability_name";
	public static final String CHECK_VALUES = "check_values";
	public static final String LOCKED = "locked";

	public static final int ABILITY_DEFAULT = 0;
	public static final int ABILITY_MAX = 6;
	public static final int ABILITY_POINTS_TOTAL = 25;
	public static final int SKILL_POINTS_TOTAL = 30;
	public static final int SKILL_MAX = 3;
	public static final int EMOTION_DEFAULT = 50;
	public static final int EMOTION_MAX = 100;
	public static final int CHECK_MIN = 1;
	public static final int CHECK_MAX = 7;

	public static final String ABILITY_LUCK = "运势";

	public static final List<String> ABILITY_NAMES = List.of(
		"身体", "灵巧", "精神", "五感", "知力", "魅力", "社会", ABILITY_LUCK);

	public static final Map<String, List<String>> SKILL_CATEGORIES;

	public static final Set<String> BASE_SKILLS = Set.of(
		"调查", "知觉", "交涉", "知识", "时讯", "运动", "格斗", "投掷",
		"生存", "自我", "治疗", "手工", "幸运");

	static {
		Map<String, List<String>> map = new LinkedHashMap<>();
		map.put("调查系", List.of("调查", "检索", "洞察", "识路", "直觉", "鉴定"));
		map.put("知觉系", List.of("知觉", "观察", "聆听", "试毒", "危机察觉"));
		map.put("交涉系", List.of("交涉", "社交术", "辩论", "心理", "魅惑"));
		map.put("情报系", List.of("知识", "专业知识", "时讯", "信息", "万事通", "业界"));
		map.put("运动系", List.of("运动", "速度", "力量", "特技动作", "潜泳", "格斗", "武术", "奥义", "投掷", "射击"));
		map.put("生存系", List.of("生存", "耐久", "自我", "毅力", "治疗", "医术", "复苏"));
		map.put("特殊技能", List.of("手工", "技巧", "艺术", "操纵", "暗号", "电脑", "隐匿", "幸运", "强运"));
		SKILL_CATEGORIES = Collections.unmodifiableMap(map);
	}

	private CardData() {
	}

	public static CompoundTag read(ItemStack stack) {
		return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
	}

	public static void write(ItemStack stack, CompoundTag tag) {
		CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
	}

	public static CompoundTag normalize(CompoundTag input) {
		CompoundTag out = input.copy();

		CompoundTag abilities = out.getCompound(ABILITIES);
		for (String name : ABILITY_NAMES) {
			int value = abilities.contains(name) ? abilities.getInt(name) : ABILITY_DEFAULT;
			abilities.putInt(name, Mth.clamp(value, ABILITY_DEFAULT, ABILITY_MAX));
		}
		trimAbilities(abilities);
		out.put(ABILITIES, abilities);

		CompoundTag skills = out.getCompound(SKILLS);
		for (List<String> list : SKILL_CATEGORIES.values()) {
			for (String name : list) {
				int value = isBaseSkill(name)
					? 1
					: (skills.contains(name) ? skills.getInt(name) : 0);
				skills.putInt(name, Mth.clamp(value, 0, SKILL_MAX));
			}
		}
		trimSkills(skills);
		out.put(SKILLS, skills);

		if (!out.contains(CHECK_VALUES)) {
			out.put(CHECK_VALUES, new CompoundTag());
		}
		recomputeAllCheckValues(out);

		int emotion = out.contains(EMOTION) ? out.getInt(EMOTION) : EMOTION_DEFAULT;
		out.putInt(EMOTION, Mth.clamp(emotion, 0, EMOTION_MAX));

		if (!out.contains(INVESTIGATOR)) {
			out.putString(INVESTIGATOR, "");
		}
		if (!out.contains(EMOTION_DEEP)) {
			out.putString(EMOTION_DEEP, "");
		}
		if (!out.contains(ABILITY_NAME)) {
			out.putString(ABILITY_NAME, "");
		}
		if (!out.contains(OWNER_UUID)) {
			out.putString(OWNER_UUID, "");
		}
		if (!out.contains(OWNER_NAME)) {
			out.putString(OWNER_NAME, "");
		}
		if (!out.contains(OWNER_SKIN)) {
			out.putString(OWNER_SKIN, "");
		}
		if (!out.contains(OWNER_SKIN_SIG)) {
			out.putString(OWNER_SKIN_SIG, "");
		}
		out.putBoolean(LOCKED, out.getBoolean(LOCKED));
		return out;
	}

	private static void trimAbilities(CompoundTag abilities) {
		int spent = 0;
		for (String name : ABILITY_NAMES) {
			if (name.equals(ABILITY_LUCK)) {
				continue;
			}
			spent += abilities.getInt(name) - ABILITY_DEFAULT;
		}
		while (spent > ABILITY_POINTS_TOTAL) {
			String target = null;
			int highest = ABILITY_DEFAULT;
			for (String name : ABILITY_NAMES) {
				if (name.equals(ABILITY_LUCK)) {
					continue;
				}
				int value = abilities.getInt(name);
				if (value > highest) {
					highest = value;
					target = name;
				}
			}
			if (target == null) {
				return;
			}
			abilities.putInt(target, abilities.getInt(target) - 1);
			spent--;
		}
	}

	private static void trimSkills(CompoundTag skills) {
		int spent = skillSpentOf(skills);
		while (spent > SKILL_POINTS_TOTAL) {
			String target = null;
			int highest = 0;
			for (List<String> list : SKILL_CATEGORIES.values()) {
				for (String name : list) {
					if (isBaseSkill(name)) {
						continue;
					}
					int value = skills.getInt(name);
					if (value > 0 && value > highest) {
						highest = value;
						target = name;
					}
				}
			}
			if (target == null) {
				return;
			}
			skills.putInt(target, skills.getInt(target) - 1);
			spent = skillSpentOf(skills);
		}
	}

	private static int skillSpentOf(CompoundTag skills) {
		int spent = 0;
		for (List<String> list : SKILL_CATEGORIES.values()) {
			for (String name : list) {
				if (isBaseSkill(name)) {
					continue;
				}
				spent += skillCost(skills.contains(name) ? skills.getInt(name) : 0);
			}
		}
		return spent;
	}

	public static int skillCost(int level) {
		return switch (level) {
			case 1 -> 1;
			case 2 -> 5;
			case 3 -> 15;
			default -> 0;
		};
	}

	public static int nextSkillCost(int level) {
		if (level >= SKILL_MAX) {
			return Integer.MAX_VALUE;
		}
		return skillCost(level + 1) - skillCost(level);
	}

	public static int ability(CompoundTag tag, String name) {
		CompoundTag abilities = tag.getCompound(ABILITIES);
		int value = abilities.contains(name) ? abilities.getInt(name) : ABILITY_DEFAULT;
		return Mth.clamp(value, ABILITY_DEFAULT, ABILITY_MAX);
	}

	public static int skill(CompoundTag tag, String name) {
		if (isBaseSkill(name)) {
			return 1;
		}
		CompoundTag skills = tag.getCompound(SKILLS);
		int value = skills.contains(name) ? skills.getInt(name) : 0;
		return Mth.clamp(value, 0, SKILL_MAX);
	}

	public static void setAbility(CompoundTag tag, String name, int value) {
		CompoundTag abilities = tag.getCompound(ABILITIES);
		abilities.putInt(name, Mth.clamp(value, ABILITY_DEFAULT, ABILITY_MAX));
		tag.put(ABILITIES, abilities);
		for (String skill : SkillTable.ABILITY_OF.keySet()) {
			if (SkillTable.abilityOf(skill).equals(name)) {
				recomputeCheckValue(tag, skill);
			}
		}
	}

	public static void setSkill(CompoundTag tag, String name, int value) {
		if (isBaseSkill(name)) {
			return;
		}
		CompoundTag skills = tag.getCompound(SKILLS);
		skills.putInt(name, Mth.clamp(value, 0, SKILL_MAX));
		tag.put(SKILLS, skills);
		recomputeCheckValue(tag, name);
	}

	public static int abilitySpent(CompoundTag tag) {
		int spent = 0;
		for (String name : ABILITY_NAMES) {
			if (name.equals(ABILITY_LUCK)) {
				continue;
			}
			spent += ability(tag, name) - ABILITY_DEFAULT;
		}
		return spent;
	}

	public static int skillSpent(CompoundTag tag) {
		int spent = 0;
		for (List<String> list : SKILL_CATEGORIES.values()) {
			for (String name : list) {
				if (isBaseSkill(name)) {
					continue;
				}
				spent += skillCost(skill(tag, name));
			}
		}
		return spent;
	}

	public static int computeCheckValue(CompoundTag tag, String skill) {
		int value = ability(tag, SkillTable.abilityOf(skill)) + skill(tag, skill) - 1 + SkillTable.bonus(skill);
		return Mth.clamp(value, CHECK_MIN, CHECK_MAX);
	}

	public static int checkValue(CompoundTag tag, String skill) {
		CompoundTag values = tag.getCompound(CHECK_VALUES);
		if (values.contains(skill)) {
			return Mth.clamp(values.getInt(skill), CHECK_MIN, CHECK_MAX);
		}
		return computeCheckValue(tag, skill);
	}

	public static int checkModifier(int checkValue) {
		int value = Mth.clamp(checkValue, CHECK_MIN, CHECK_MAX);
		if (value <= 2) {
			return 0;
		}
		if (value <= 4) {
			return 1;
		}
		if (value <= 6) {
			return 2;
		}
		return 3;
	}

	public static void recomputeCheckValue(CompoundTag tag, String skill) {
		CompoundTag values = tag.getCompound(CHECK_VALUES);
		values.putInt(skill, computeCheckValue(tag, skill));
		tag.put(CHECK_VALUES, values);
	}

	public static void recomputeAllCheckValues(CompoundTag tag) {
		for (String skill : SkillTable.ABILITY_OF.keySet()) {
			recomputeCheckValue(tag, skill);
		}
	}

	public static int hp(CompoundTag tag) {
		return 10 + ability(tag, "身体");
	}

	public static int mp(CompoundTag tag) {
		return ability(tag, "精神") + ability(tag, "知力");
	}

	public static int emotion(CompoundTag tag) {
		int value = tag.contains(EMOTION) ? tag.getInt(EMOTION) : EMOTION_DEFAULT;
		return Mth.clamp(value, 0, EMOTION_MAX);
	}

	public static void setEmotion(CompoundTag tag, int value) {
		tag.putInt(EMOTION, Mth.clamp(value, 0, EMOTION_MAX));
	}

	public static ItemStack findCard(Player player) {
		String uuid = player.getUUID().toString();
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack stack = player.getInventory().getItem(i);
			if (stack.is(ModItems.ID_CARD) && uuid.equals(read(stack).getString(OWNER_UUID))) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	public static boolean isBaseSkill(String skill) {
		return BASE_SKILLS.contains(skill);
	}

	public static boolean isLocked(CompoundTag tag) {
		return tag.getBoolean(LOCKED);
	}

	public static void setLocked(CompoundTag tag, boolean locked) {
		tag.putBoolean(LOCKED, locked);
	}

	public static void keepProfile(CompoundTag source, CompoundTag target) {
		target.putInt(EMOTION, emotion(source));
		target.putString(EMOTION_DEEP, source.getString(EMOTION_DEEP));
		target.putBoolean(AWAKENED, source.getBoolean(AWAKENED));
		target.putString(ABILITY_NAME, source.getString(ABILITY_NAME));
		setAbility(target, ABILITY_LUCK, ability(source, ABILITY_LUCK));
	}

	public static void resetForm(CompoundTag tag) {
		tag.putString(INVESTIGATOR, "");
		tag.putInt(EMOTION, EMOTION_DEFAULT);
		tag.putString(EMOTION_DEEP, "");
		tag.putBoolean(AWAKENED, false);
		tag.putString(ABILITY_NAME, "");
		for (String name : ABILITY_NAMES) {
			setAbility(tag, name, ABILITY_DEFAULT);
		}
		for (List<String> list : SKILL_CATEGORIES.values()) {
			for (String name : list) {
				if (isBaseSkill(name)) {
					CompoundTag skills = tag.getCompound(SKILLS);
					skills.putInt(name, 1);
					tag.put(SKILLS, skills);
					recomputeCheckValue(tag, name);
				} else {
					setSkill(tag, name, 0);
				}
			}
		}
	}

	public static boolean tryAwaken(CompoundTag tag) {
		String deep = tag.getString(EMOTION_DEEP).trim();
		if (emotion(tag) >= EMOTION_MAX && !deep.isEmpty()) {
			tag.putBoolean(AWAKENED, true);
			if (tag.getString(ABILITY_NAME).trim().isEmpty()) {
				tag.putString(ABILITY_NAME, deep);
			}
			return true;
		}
		return false;
	}
}
