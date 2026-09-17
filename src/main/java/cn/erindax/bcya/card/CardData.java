package cn.erindax.bcya.card;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
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

	public static final int ABILITY_DEFAULT = 1;
	public static final int ABILITY_MAX = 5;
	public static final int ABILITY_POINTS_TOTAL = 25;
	public static final int SKILL_POINTS_TOTAL = 30;
	public static final int SKILL_MAX = 30;
	public static final int EMOTION_DEFAULT = 50;
	public static final int EMOTION_MAX = 100;

	public static final String ABILITY_LUCK = "运势";

	public static final List<String> ABILITY_NAMES = List.of(
		"身体", "灵巧", "精神", "五感", "知力", "魅力", "社会", ABILITY_LUCK);

	public static final Map<String, List<String>> SKILL_CATEGORIES;

	public static final Set<String> BASE_SKILLS = Set.of(
		"调查", "知觉", "交涉", "知识", "运动", "生存");

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
				int min = BASE_SKILLS.contains(name) ? 1 : 0;
				int value = skills.contains(name) ? skills.getInt(name) : min;
				skills.putInt(name, Mth.clamp(value, min, SKILL_MAX));
			}
		}
		trimSkills(skills);
		out.put(SKILLS, skills);

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
		return out;
	}

	private static void trimAbilities(CompoundTag abilities) {
		int spent = 0;
		for (String name : ABILITY_NAMES) {
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
		int spent = rawSkillSum(skills) - BASE_SKILLS.size();
		while (spent > SKILL_POINTS_TOTAL) {
			String target = null;
			int highest = 0;
			for (List<String> list : SKILL_CATEGORIES.values()) {
				for (String name : list) {
					int value = skills.getInt(name);
					int min = BASE_SKILLS.contains(name) ? 1 : 0;
					if (value > min && value > highest) {
						highest = value;
						target = name;
					}
				}
			}
			if (target == null) {
				return;
			}
			skills.putInt(target, skills.getInt(target) - 1);
			spent--;
		}
	}

	private static int rawSkillSum(CompoundTag skills) {
		int sum = 0;
		for (List<String> list : SKILL_CATEGORIES.values()) {
			for (String name : list) {
				sum += skills.getInt(name);
			}
		}
		return sum;
	}

	public static int ability(CompoundTag tag, String name) {
		CompoundTag abilities = tag.getCompound(ABILITIES);
		int value = abilities.contains(name) ? abilities.getInt(name) : ABILITY_DEFAULT;
		return Mth.clamp(value, ABILITY_DEFAULT, ABILITY_MAX);
	}

	public static int skill(CompoundTag tag, String name) {
		CompoundTag skills = tag.getCompound(SKILLS);
		int min = BASE_SKILLS.contains(name) ? 1 : 0;
		int value = skills.contains(name) ? skills.getInt(name) : min;
		return Mth.clamp(value, min, SKILL_MAX);
	}

	public static void setAbility(CompoundTag tag, String name, int value) {
		CompoundTag abilities = tag.getCompound(ABILITIES);
		abilities.putInt(name, Mth.clamp(value, ABILITY_DEFAULT, ABILITY_MAX));
		tag.put(ABILITIES, abilities);
	}

	public static void setSkill(CompoundTag tag, String name, int value) {
		int min = BASE_SKILLS.contains(name) ? 1 : 0;
		CompoundTag skills = tag.getCompound(SKILLS);
		skills.putInt(name, Mth.clamp(value, min, SKILL_MAX));
		tag.put(SKILLS, skills);
	}

	public static int abilitySpent(CompoundTag tag) {
		int spent = 0;
		for (String name : ABILITY_NAMES) {
			spent += ability(tag, name) - ABILITY_DEFAULT;
		}
		return spent;
	}

	public static int skillSpent(CompoundTag tag) {
		int sum = 0;
		for (List<String> list : SKILL_CATEGORIES.values()) {
			for (String name : list) {
				sum += skill(tag, name);
			}
		}
		return Math.max(0, sum - BASE_SKILLS.size());
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

	public static boolean requiredComplete(CompoundTag tag) {
		return !tag.getString(INVESTIGATOR).trim().isEmpty();
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
				setSkill(tag, name, BASE_SKILLS.contains(name) ? 1 : 0);
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
