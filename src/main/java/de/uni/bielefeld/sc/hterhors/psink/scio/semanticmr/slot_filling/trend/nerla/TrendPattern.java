package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.trend.nerla;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.nerla.annotation.BasicRegExPattern;

public class TrendPattern extends BasicRegExPattern {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Set<String> TREND_STOP_WORDS = new HashSet<>(Arrays.asList());

	/*
	 * Rats
	 */

	private static Pattern RAT_MODEL_PATTERN_1 = Pattern.compile(PRE_BOUNDS + "(albino.?)?rats?" + POST_BOUNDS,
			PATTERN_BITMASK);

	public static final Set<Pattern> RAT_MODEL_REG_EXP = new HashSet<>(Arrays.asList(RAT_MODEL_PATTERN_1));

	private Map<EntityType, Set<Pattern>> pattern = new HashMap<>();

	public TrendPattern(EntityType rootEntityType) {
		super(rootEntityType);
//		pattern.put(EntityType.get("Trend"), TrendPattern.RAT_MODEL_REG_EXP);
	}

	public Map<EntityType, Set<Pattern>> getHandMadePattern() {
		return pattern;

	}

	@Override
	public Set<String> getStopWords() {
		return TREND_STOP_WORDS;
	}

	@Override
	public int getMinTokenlength() {
		return 1;
	}

}
