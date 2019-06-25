package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.orgmodel.nerla;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.nerla.annotation.BasicRegExPattern;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.normalizer.interpreter.AgeInterpreter;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.normalizer.interpreter.WeightInterpreter;

public class OrganismModelPattern extends BasicRegExPattern {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Set<String> ORGANISM_MODEL_STOP_WORDS = new HashSet<>(Arrays.asList("animal", "model"));

	/*
	 * Rats
	 */

	private static Pattern RAT_MODEL_PATTERN_1 = Pattern.compile(PRE_BOUNDS + "(albino.?)?rats?" + POST_BOUNDS,
			PATTERN_BITMASK);

	private static Pattern LONG_EVANS_RAT_MODEL_PATTERN_1 = Pattern.compile(
			PRE_BOUNDS + buildRegExpr("long", "evans", new String[] { "rats?" }, "strains?") + POST_BOUNDS,
			PATTERN_BITMASK);

	private static Pattern SPRAGUE_DAWLEY_RAT_MODEL_PATTERN_1 = Pattern.compile(PRE_BOUNDS
			+ buildRegExpr("sprague", "dawle?y", new String[] { "albino.?rats?", "rats?" }, "strains?") + POST_BOUNDS,
			PATTERN_BITMASK);

	private static Pattern WISTAR_RAT_MODEL_PATTERN_1 = Pattern.compile(
			PRE_BOUNDS + buildRegExpr("wistar", new String[] { "albino", "rats?" }, "strains?") + POST_BOUNDS,
			PATTERN_BITMASK);

	/*
	 * Mice
	 */

	private static Pattern MOUSE_MODEL_PATTERN_1 = Pattern.compile(PRE_BOUNDS + "(albino.?)?(mouse|mice)" + POST_BOUNDS,
			PATTERN_BITMASK);

	private static Pattern CD1_MOUSE_MODEL_PATTERN_1 = Pattern.compile(
			PRE_BOUNDS + buildRegExpr("cd.?2?", new String[] { "albino", "mouse", "mice" }, "strains?") + POST_BOUNDS,
			PATTERN_BITMASK);

	private static Pattern CD2_MOUSE_MODEL_PATTERN_1 = Pattern.compile(
			PRE_BOUNDS + buildRegExpr("cd.?1?", new String[] { "albino", "mouse", "mice" }, "strains?") + POST_BOUNDS,
			PATTERN_BITMASK);

	private static Pattern BALB_C_MOUSE_PATTERN_1 = Pattern.compile(
			PRE_BOUNDS + buildRegExpr("balb(.?c?)?", new String[] { "mouse", "mice" }, "strains?") + POST_BOUNDS,
			PATTERN_BITMASK);

	private static Pattern C57_BL6_MOUSE_PATTERN_1 = Pattern.compile(
			PRE_BOUNDS + buildRegExpr("c57", "bl.?6\\w?", new String[] { "mouse", "mice" }, "strains?") + POST_BOUNDS,
			PATTERN_BITMASK);

	/*
	 * Guinea Pig
	 */
	private static Pattern GUINEA_PIG_MODEL_PATTERN_1 = Pattern.compile(PRE_BOUNDS + "guinea(.?pigs?)?" + POST_BOUNDS,
			PATTERN_BITMASK);

	private static Pattern HARTLEY_GUINEA_PIG_PATTERN_1 = Pattern.compile(
			PRE_BOUNDS + buildRegExpr("hartley", new String[] { "guinea", "pigs?" }, "strains?") + POST_BOUNDS,
			PATTERN_BITMASK);

	/*
	 * Gender
	 */
	private static Pattern GENDER_MALE_PATTERN_1 = Pattern.compile(PRE_BOUNDS + "males?" + POST_BOUNDS,
			PATTERN_BITMASK);
	private static Pattern GENDER_FEMALE_PATTERN_1 = Pattern.compile(PRE_BOUNDS + "females?" + POST_BOUNDS,
			PATTERN_BITMASK);
	private static Pattern GENDER_MIXED_PATTERN_1 = Pattern.compile(PRE_BOUNDS + "mixed" + POST_BOUNDS,
			PATTERN_BITMASK);

	/*
	 * AgeCategory
	 */

	private static Pattern AGE_CATEGORY_ADULT_PATTERN_1 = Pattern
			.compile(PRE_BOUNDS + "(matures?|adults?)" + POST_BOUNDS, PATTERN_BITMASK);

	private static Pattern AGE_CATEGORY_YOUNG_PATTERN_1 = Pattern.compile(PRE_BOUNDS + "young" + POST_BOUNDS,
			PATTERN_BITMASK);

	private static Pattern AGE_CATEGORY_AGED_PATTERN_1 = Pattern.compile(PRE_BOUNDS + "aged" + POST_BOUNDS,
			PATTERN_BITMASK);

	public static final Set<Pattern> AGE_REG_EXP = new HashSet<>(Arrays.asList(AgeInterpreter.PATTERN));

	public static final Set<Pattern> WEIGHT_REG_EXP = new HashSet<>(Arrays.asList(WeightInterpreter.PATTERN));

	public static final Set<Pattern> CD1_MOUSE_MODEL_REG_EXP = new HashSet<>(Arrays.asList(CD1_MOUSE_MODEL_PATTERN_1));

	public static final Set<Pattern> CD2_MOUSE_MODEL_REG_EXP = new HashSet<>(Arrays.asList(CD2_MOUSE_MODEL_PATTERN_1));

	public static final Set<Pattern> BALB_C_MOUSE_MODEL_REG_EXP = new HashSet<>(Arrays.asList(BALB_C_MOUSE_PATTERN_1));

	public static final Set<Pattern> C57_BL6_MOUSE_MODEL_REG_EXP = new HashSet<>(
			Arrays.asList(C57_BL6_MOUSE_PATTERN_1));

	public static final Set<Pattern> GUINEA_PIG_MODEL_REG_EXP = new HashSet<>(
			Arrays.asList(GUINEA_PIG_MODEL_PATTERN_1));

	public static final Set<Pattern> HARTLEY_GUINEA_PIG_REG_EXP = new HashSet<>(
			Arrays.asList(HARTLEY_GUINEA_PIG_PATTERN_1));

	public static final Set<Pattern> SPRAGUE_DAWLEY_RAT_REG_EXP = new HashSet<>(
			Arrays.asList(SPRAGUE_DAWLEY_RAT_MODEL_PATTERN_1));

	public static final Set<Pattern> WISTAR_RAT_REG_EXP = new HashSet<>(Arrays.asList(WISTAR_RAT_MODEL_PATTERN_1));

	public static final Set<Pattern> RAT_MODEL_REG_EXP = new HashSet<>(Arrays.asList(RAT_MODEL_PATTERN_1));

	public static final Set<Pattern> MOUSE_MODEL_REG_EXP = new HashSet<>(Arrays.asList(MOUSE_MODEL_PATTERN_1));

	public static final Set<Pattern> GENDER_FEMALE_REG_EXP = new HashSet<>(Arrays.asList(GENDER_FEMALE_PATTERN_1));

	public static final Set<Pattern> GENDER_MALE_REG_EXP = new HashSet<>(Arrays.asList(GENDER_MALE_PATTERN_1));

	public static final Set<Pattern> GENDER_MIXED_REG_EXP = new HashSet<>(Arrays.asList(GENDER_MIXED_PATTERN_1));

	public static final Set<Pattern> AGE_CATEGORY_ADULT_REG_EXP = new HashSet<>(
			Arrays.asList(AGE_CATEGORY_ADULT_PATTERN_1));

	public static final Set<Pattern> AGE_CATEGORY_YOUNG_REG_EXP = new HashSet<>(
			Arrays.asList(AGE_CATEGORY_YOUNG_PATTERN_1));

	public static final Set<Pattern> AGE_CATEGORY_AGED_REG_EXP = new HashSet<>(
			Arrays.asList(AGE_CATEGORY_AGED_PATTERN_1));

	public static final Set<Pattern> LONG_EVANS_RAT_REG_EXP = new HashSet<>(
			Arrays.asList(LONG_EVANS_RAT_MODEL_PATTERN_1));

	private Map<EntityType, Set<Pattern>> pattern = new HashMap<>();

	public OrganismModelPattern() {
		
		/**
		 * All models are of type OrganismModel so we do not need to add this here
		 */
//		pattern.put(EntityType.get("MouseModel"), OrganismModelPattern.MOUSE_MODEL_REG_EXP);
//		pattern.put(EntityType.get("GuineaPigModel"), OrganismModelPattern.GUINEA_PIG_MODEL_REG_EXP);
//		pattern.put(EntityType.get("RatModel"), OrganismModelPattern.RAT_MODEL_REG_EXP);

		pattern.put(EntityType.get("CD1_Mouse"), OrganismModelPattern.CD1_MOUSE_MODEL_REG_EXP);
		pattern.put(EntityType.get("CD2_Mouse"), OrganismModelPattern.CD2_MOUSE_MODEL_REG_EXP);
		pattern.put(EntityType.get("BALB_C_Mouse"), OrganismModelPattern.BALB_C_MOUSE_MODEL_REG_EXP);
		pattern.put(EntityType.get("C57_BL6_Mouse"), OrganismModelPattern.C57_BL6_MOUSE_MODEL_REG_EXP);
		pattern.put(EntityType.get("HartleyGuineaPig"), OrganismModelPattern.HARTLEY_GUINEA_PIG_REG_EXP);
		pattern.put(EntityType.get("WistarRat"), OrganismModelPattern.WISTAR_RAT_REG_EXP);
		pattern.put(EntityType.get("SpragueDawleyRat"), OrganismModelPattern.SPRAGUE_DAWLEY_RAT_REG_EXP);
		pattern.put(EntityType.get("LongEvansRat"), OrganismModelPattern.LONG_EVANS_RAT_REG_EXP);
		pattern.put(EntityType.get("Female"), OrganismModelPattern.GENDER_FEMALE_REG_EXP);
		pattern.put(EntityType.get("Male"), OrganismModelPattern.GENDER_MALE_REG_EXP);
		pattern.put(EntityType.get("Mixed"), OrganismModelPattern.GENDER_MIXED_REG_EXP);
		pattern.put(EntityType.get("Adult"), OrganismModelPattern.AGE_CATEGORY_ADULT_REG_EXP);
		pattern.put(EntityType.get("Young"), OrganismModelPattern.AGE_CATEGORY_YOUNG_REG_EXP);
		pattern.put(EntityType.get("Aged"), OrganismModelPattern.AGE_CATEGORY_AGED_REG_EXP);
		pattern.put(EntityType.get("Age"), OrganismModelPattern.AGE_REG_EXP);
		pattern.put(EntityType.get("Weight"), OrganismModelPattern.WEIGHT_REG_EXP);
	}

	public Map<EntityType, Set<Pattern>> getHandMadePattern() {
		return pattern;

	}

	@Override
	public Set<String> getStopWords() {
		return ORGANISM_MODEL_STOP_WORDS;
	}

	@Override
	public int getMinTokenlength() {
		return 2;
	}

}
