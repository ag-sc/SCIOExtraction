package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.injury.nerla;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.nerla.annotation.BasicRegExPattern;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.normalizer.interpreter.DosageInterpreter;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.normalizer.interpreter.DurationInterpreter;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.normalizer.interpreter.WeightInterpreter;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.orgmodel.nerla.OrganismModelPattern;

public class InjuryPattern extends BasicRegExPattern {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Set<Pattern> WEIGHT_REG_EXP = new HashSet<>(Arrays.asList(WeightInterpreter.PATTERN));

	private static final Set<String> INJURY_STOP_WORDS = new HashSet<>(
			Arrays.asList("injury", "spinal", "cord", "animal", "model"));

	private Map<EntityType, Set<Pattern>> pattern = new HashMap<>();

	private static final Set<Pattern> DOSAGE_REG_EXP = new HashSet<>(Arrays.asList(DosageInterpreter.PATTERN));
	private static final Set<Pattern> DURATION_REG_EXP = new HashSet<>(Arrays.asList(DurationInterpreter.PATTERN));

	public InjuryPattern() {

		pattern.put(EntityType.get("Weight"), InjuryPattern.WEIGHT_REG_EXP);
		pattern.put(EntityType.get("Dosage"), InjuryPattern.DOSAGE_REG_EXP);
		pattern.put(EntityType.get("Duration"), InjuryPattern.DURATION_REG_EXP);
		pattern.put(EntityType.get("CD1_Mouse"), OrganismModelPattern.CD1_MOUSE_MODEL_REG_EXP);
		pattern.put(EntityType.get("CD2_Mouse"), OrganismModelPattern.CD2_MOUSE_MODEL_REG_EXP);
		pattern.put(EntityType.get("BALB_C_Mouse"), OrganismModelPattern.BALB_C_MOUSE_MODEL_REG_EXP);
		pattern.put(EntityType.get("C57_BL6_Mouse"), OrganismModelPattern.C57_BL6_MOUSE_MODEL_REG_EXP);
		pattern.put(EntityType.get("HartleyGuineaPig"), OrganismModelPattern.HARTLEY_GUINEA_PIG_REG_EXP);
		pattern.put(EntityType.get("WistarRat"), OrganismModelPattern.WISTAR_RAT_REG_EXP);
		pattern.put(EntityType.get("SpragueDawleyRat"), OrganismModelPattern.SPRAGUE_DAWLEY_RAT_REG_EXP);
		pattern.put(EntityType.get("LongEvansRat"), OrganismModelPattern.LONG_EVANS_RAT_REG_EXP);
	}

	public Map<EntityType, Set<Pattern>> getHandMadePattern() {
		return pattern;

	}

	@Override
	public Set<String> getStopWords() {
		return INJURY_STOP_WORDS;
	}

	@Override
	public int getMinTokenlength() {
		return 2;
	}

}
