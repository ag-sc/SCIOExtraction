package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr;

public class OrganismModelPatternBAK {
//extends BasicRegExPattern {
//
//	/**
//	 * 
//	 */
//	private static final long serialVersionUID = 1L;
//
//
//
//	private static final Set<Pattern> DEPTH_REG_EXP = new HashSet<>(Arrays.asList(SemanticDepth.PATTERN));
//
//	private static final Set<Pattern> DISTANCE_REG_EXP = new HashSet<>(Arrays.asList(SemanticDistance.PATTERN));
//
//	private static final Set<Pattern> FORCE_REG_EXP = new HashSet<>(Arrays.asList(SemanticForce.PATTERN));
//
//	private static final Set<Pattern> PRESSURE_REG_EXP = new HashSet<>(Arrays.asList(SemanticPressure.PATTERN));
//
//	private static final Set<Pattern> LENGTH_REG_EXP = new HashSet<>(Arrays.asList(SemanticLength.PATTERN));
//
//	private static final Set<Pattern> THICKNESS_REG_EXP = new HashSet<>(Arrays.asList(SemanticThickness.PATTERN));
//
//	private static final Set<Pattern> TEMPERATURE_REG_EXP = new HashSet<>(Arrays.asList(SemanticTemperature.PATTERN));
//
//	private static final Set<Pattern> NNUMBER_REG_EXP = new HashSet<>(Arrays.asList(SemanticNNumber.PATTERN));
//
//	private static final Set<Pattern> PVALUE_REG_EXP = new HashSet<>(Arrays.asList(SemanticPValue.PATTERN));
//
//	private static final Set<Pattern> STDERR_REG_EXP = new HashSet<>(Arrays.asList(SemanticStandardError.PATTERN));
//
//	private static final Set<Pattern> STDDEV_REG_EXP = new HashSet<>(Arrays.asList(SemanticStandardDeviation.PATTERN));
//
//	private static final Set<Pattern> VOLUME_REG_EXP = new HashSet<>(Arrays.asList(SemanticVolume.PATTERN));
//
//	private static final Set<Pattern> LIGHT_INTENSE_REG_EXP = new HashSet<>(
//			Arrays.asList(SemanticLightIntensity.PATTERN));

	
//	/**
//	 * TREATMENTS
//	 */
//	private final Set<Pattern> COMPOUND_REG_EXP;
//
//	/**
//	 * INYURIES
//	 */
//	private static final Pattern NYUIMPACTOR_PATTERN_1 = Pattern
//			.compile(PRE_BOUNDS + "new.york(.university)?(.impactor)?" + POST_BOUNDS, PATTERN_BITMASK);
//
//	private static final Set<Pattern> NYUIMPACTOR_REG_EXP = new HashSet<>(Arrays.asList(NYUIMPACTOR_PATTERN_1));
//
//	private static final Pattern BLADDER_EXPRESSION_PATTERN_1 = Pattern.compile(PRE_BOUNDS + "bladders" + POST_BOUNDS,
//			PATTERN_BITMASK);
//
//	private static final Set<Pattern> BLADDER_EXPRESSION_REG_EXP = new HashSet<>(
//			Arrays.asList(BLADDER_EXPRESSION_PATTERN_1));
//
//	private static final Pattern NUTRITION_PATTERN_1 = Pattern.compile(PRE_BOUNDS + "food" + POST_BOUNDS,
//			PATTERN_BITMASK);
//
//	private static final Set<Pattern> NUTRITION_REG_EXP = new HashSet<>(Arrays.asList(NUTRITION_PATTERN_1));
//
//	private static final Pattern HYDRATION_PATTERN_1 = Pattern.compile(PRE_BOUNDS + "water" + POST_BOUNDS,
//			PATTERN_BITMASK);
//
//	private static final Set<Pattern> HYDRATION_REG_EXP = new HashSet<>(Arrays.asList(HYDRATION_PATTERN_1));
//
//	private static final Pattern UTSIMPACTOR_PATTERN_1 = Pattern.compile(PRE_BOUNDS + "UTS(.?impactor)?" + POST_BOUNDS,
//			PATTERN_BITMASK);
//
//	private static final Pattern UTSIMPACTOR_PATTERN_2 = Pattern
//			.compile(PRE_BOUNDS + "university.of.trieste?(.impactor)?" + POST_BOUNDS, PATTERN_BITMASK);
//
//	private static final Set<Pattern> UTSIMPACTOR_REG_EXP = new HashSet<>(
//			Arrays.asList(UTSIMPACTOR_PATTERN_1, UTSIMPACTOR_PATTERN_2));
//
////	public SCIOPattern() {
////		COMPOUND_REG_EXP = patternForCompounds();
////	}
//
//	private Map<EntityType, Set<Pattern>> pattern = new HashMap<>();
//
//	
//	public OrganismModelPatternBAK() {
//		pattern.put(EntityType.get("MouseModel"), OrganismModelPatternBAK.MOUSE_MODEL_REG_EXP);
//		pattern.put(EntityType.get("GuineaPigModel"), OrganismModelPatternBAK.GUINEA_PIG_MODEL_REG_EXP);
//		pattern.put(EntityType.get("RatModel"), OrganismModelPatternBAK.RAT_MODEL_REG_EXP);
//		/*
//		 * Treatment
//		 */
//		pattern.put(EntityType.get("CompoundTreatment"), COMPOUND_REG_EXP);
//		pattern.put(EntityType.get("Compound"), COMPOUND_REG_EXP);
//
//		/*
//		 * injury
//		 */
//		pattern.put(EntityType.get("NYUImpactor"), OrganismModelPatternBAK.NYUIMPACTOR_REG_EXP);
//		pattern.put(EntityType.get("UnivOfTriesteImpactor"), OrganismModelPatternBAK.UTSIMPACTOR_REG_EXP);
//
//		/*
//		 * Datatype Properties
//		 */
//		pattern.put(EntityType.get("Age"), OrganismModelPatternBAK.AGE_REG_EXP);
//		pattern.put(EntityType.get("Weight"), OrganismModelPatternBAK.WEIGHT_REG_EXP);
////		handMadepatternForClasses.put(EntityType.get("Dosage"), SCIOPattern.DOSAGE_REG_EXP);
////		handMadepatternForClasses.put(EntityType.get("Duration"), SCIOPattern.DURATION_REG_EXP);
////		handMadepatternForClasses.put(EntityType.get("Depth"), SCIOPattern.DEPTH_REG_EXP);
////		handMadepatternForClasses.put(EntityType.get("Distance"), SCIOPattern.DISTANCE_REG_EXP);
////		handMadepatternForClasses.put(EntityType.get("Force"), SCIOPattern.FORCE_REG_EXP);
////		handMadepatternForClasses.put(EntityType.get("Volume"), SCIOPattern.VOLUME_REG_EXP);
////		pattern.put(EntityType.get("Pressure"), SCIOPattern.PRESSURE_REG_EXP);
////		pattern.put(EntityType.get("Length"), SCIOPattern.LENGTH_REG_EXP);
////		pattern.put(EntityType.get("Thickness"), SCIOPattern.THICKNESS_REG_EXP);
////		pattern.put(EntityType.get("Temperature"), SCIOPattern.TEMPERATURE_REG_EXP);
////		pattern.put(EntityType.get("NNumber"), SCIOPattern.NNUMBER_REG_EXP);
////		pattern.put(EntityType.get("PValue"), SCIOPattern.PVALUE_REG_EXP);
////		pattern.put(EntityType.get("StandardError"), SCIOPattern.STDERR_REG_EXP);
////		pattern.put(EntityType.get("StandardDeviation"), SCIOPattern.STDDEV_REG_EXP);
////		pattern.put(EntityType.get("LightIntensity"), SCIOPattern.LIGHT_INTENSE_REG_EXP);
//
//		/*
//		 * AnimalModel
//		 */
//		pattern.put(EntityType.get("CD1_Mouse"), OrganismModelPatternBAK.CD1_MOUSE_MODEL_REG_EXP);
//		pattern.put(EntityType.get("CD2_Mouse"), OrganismModelPatternBAK.CD2_MOUSE_MODEL_REG_EXP);
//		pattern.put(EntityType.get("BALB_C_Mouse"), OrganismModelPatternBAK.BALB_C_MOUSE_MODEL_REG_EXP);
//		pattern.put(EntityType.get("C57_BL6_Mouse"), OrganismModelPatternBAK.C57_BL6_MOUSE_MODEL_REG_EXP);
//		pattern.put(EntityType.get("HartleyGuineaPig"), OrganismModelPatternBAK.HARTLEY_GUINEA_PIG_REG_EXP);
//		pattern.put(EntityType.get("WistarRat"), OrganismModelPatternBAK.WISTAR_RAT_REG_EXP);
//		pattern.put(EntityType.get("SpragueDawleyRat"), OrganismModelPatternBAK.SPRAGUE_DAWLEY_RAT_REG_EXP);
//		pattern.put(EntityType.get("LongEvansRat"), OrganismModelPatternBAK.LONG_EVANS_RAT_REG_EXP);
//		pattern.put(EntityType.get("Female"), OrganismModelPatternBAK.GENDER_FEMALE_REG_EXP);
//		pattern.put(EntityType.get("Male"), OrganismModelPatternBAK.GENDER_MALE_REG_EXP);
//		pattern.put(EntityType.get("Mixed"), OrganismModelPatternBAK.GENDER_MIXED_REG_EXP);
//		pattern.put(EntityType.get("Adult"), OrganismModelPatternBAK.AGE_CATEGORY_ADULT_REG_EXP);
//		pattern.put(EntityType.get("Young"), OrganismModelPatternBAK.AGE_CATEGORY_YOUNG_REG_EXP);
//		pattern.put(EntityType.get("Aged"), OrganismModelPatternBAK.AGE_CATEGORY_AGED_REG_EXP);
//		/*
//		 * Treatment
//		 */
//
//		/*
//		 * injury
//		 */
//		pattern.put(EntityType.get("BladderExpression"), OrganismModelPatternBAK.BLADDER_EXPRESSION_REG_EXP);
//		pattern.put(EntityType.get("Nutrition"), OrganismModelPatternBAK.NUTRITION_REG_EXP);
//		pattern.put(EntityType.get("Hydration"), OrganismModelPatternBAK.HYDRATION_REG_EXP);
//		return pattern;
//	}
//
//	public Map<EntityType, Set<Pattern>> getPattern() {
//		return pattern;
//		
//	}
//
//	private Set<Pattern> patternForCompounds() {
//
//		Set<Pattern> pattern = new HashSet<>();
//
//		for (Set<Pattern> compoundClassPattern : autoGeneratePatternForClasses(ICompound.class).values()) {
//			pattern.addAll(compoundClassPattern);
//		}
//		return pattern;
//	}
//
//	@Override
//	public Set<String> getStopWords() {
//		return SCIO_STOP_WORDS;
//	}
//
//	@Override
//	public int getMinTokenlength() {
//		return 2;
//	}

}
