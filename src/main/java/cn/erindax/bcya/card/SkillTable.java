package cn.erindax.bcya.card;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class SkillTable {

	public static final Map<String, String> ABILITY_OF;

	public static final Set<String> EMPATHIC = Set.of(
		"知觉", "观察", "聆听", "危机察觉", "心理", "洞察", "直觉");

	public static final Set<String> BONUS_SKILLS = Set.of(
		"专业知识", "万事通", "医术", "奥义", "强运");

	static {
		Map<String, String> map = new LinkedHashMap<>();
		map.put("调查", "知力");
		map.put("检索", "知力");
		map.put("洞察", "精神");
		map.put("识路", "五感");
		map.put("直觉", "精神");
		map.put("鉴定", "知力");
		map.put("知觉", "五感");
		map.put("观察", "五感");
		map.put("聆听", "五感");
		map.put("试毒", "身体");
		map.put("危机察觉", "五感");
		map.put("交涉", "魅力");
		map.put("社交术", "魅力");
		map.put("辩论", "魅力");
		map.put("心理", "精神");
		map.put("魅惑", "魅力");
		map.put("知识", "知力");
		map.put("专业知识", "知力");
		map.put("时讯", "社会");
		map.put("信息", "社会");
		map.put("万事通", "知力");
		map.put("业界", "社会");
		map.put("运动", "身体");
		map.put("速度", "灵巧");
		map.put("力量", "身体");
		map.put("特技动作", "灵巧");
		map.put("潜泳", "身体");
		map.put("格斗", "身体");
		map.put("武术", "身体");
		map.put("奥义", "身体");
		map.put("投掷", "灵巧");
		map.put("射击", "灵巧");
		map.put("生存", "身体");
		map.put("耐久", "身体");
		map.put("自我", "精神");
		map.put("毅力", "精神");
		map.put("治疗", "知力");
		map.put("医术", "知力");
		map.put("复苏", "精神");
		map.put("手工", "灵巧");
		map.put("技巧", "灵巧");
		map.put("艺术", "精神");
		map.put("操纵", "灵巧");
		map.put("暗号", "知力");
		map.put("电脑", "知力");
		map.put("隐匿", "灵巧");
		map.put("幸运", "运势");
		map.put("强运", "运势");
		ABILITY_OF = Collections.unmodifiableMap(map);
	}

	private SkillTable() {
	}

	public static boolean isKnown(String skill) {
		return ABILITY_OF.containsKey(skill);
	}

	public static String abilityOf(String skill) {
		return ABILITY_OF.getOrDefault(skill, "精神");
	}

	public static boolean isEmpathic(String skill) {
		return EMPATHIC.contains(skill);
	}

	public static int bonus(String skill) {
		return BONUS_SKILLS.contains(skill) ? 1 : 0;
	}
}
