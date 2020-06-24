package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.injury_device;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.nerla.annotation.BasicRegExPattern;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.DistanceInterpreter;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.DurationInterpreter;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.ForceInterpreter;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.LengthInterpreter;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.PressureInterpreter;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.ThicknessInterpreter;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.VolumeInterpreter;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.WeightInterpreter;

public class InjuryDevicePattern extends BasicRegExPattern {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList("spinal", "cord"));

	private static final Pattern NYUIMPACTOR_PATTERN_1 = Pattern
			.compile(PRE_BOUNDS + "new.york(.university)?(.impactor)?" + POST_BOUNDS, PATTERN_BITMASK);
	private static final Pattern NYUIMPACTOR_PATTERN_2 = Pattern.compile(PRE_BOUNDS + "impactor" + POST_BOUNDS,
			PATTERN_BITMASK);

	private static final Set<Pattern> NYU_IMPACTOR_REG_EXP = new HashSet<>(
			Arrays.asList(NYUIMPACTOR_PATTERN_1, NYUIMPACTOR_PATTERN_2));

	private static final Pattern UTSIMPACTOR_PATTERN_1 = Pattern.compile(PRE_BOUNDS + "UTS(.?impactor)?" + POST_BOUNDS,
			PATTERN_BITMASK);

	private static final Pattern UTSIMPACTOR_PATTERN_2 = Pattern
			.compile(PRE_BOUNDS + "university.of.trieste?(.impactor)?" + POST_BOUNDS, PATTERN_BITMASK);
	private static final Set<Pattern> UTS_IMPACTOR_REG_EXP = new HashSet<>(
			Arrays.asList(UTSIMPACTOR_PATTERN_1, UTSIMPACTOR_PATTERN_2));

	private Map<EntityType, Set<Pattern>> pattern = new HashMap<>();

	public InjuryDevicePattern(EntityType rootEntityType) {
		super(rootEntityType);
		pattern.put(EntityType.get("Weight"), new HashSet<>(Arrays.asList(WeightInterpreter.PATTERN)));
		pattern.put(EntityType.get("Duration"), new HashSet<>(Arrays.asList(DurationInterpreter.PATTERN)));
		pattern.put(EntityType.get("Force"), new HashSet<>(Arrays.asList(ForceInterpreter.PATTERN)));
		pattern.put(EntityType.get("Pressure"), new HashSet<>(Arrays.asList(PressureInterpreter.PATTERN)));
		pattern.put(EntityType.get("Volume"), new HashSet<>(Arrays.asList(VolumeInterpreter.PATTERN)));

		pattern.put(EntityType.get("Distance"), new HashSet<>(Arrays.asList(DistanceInterpreter.PATTERN)));
		pattern.put(EntityType.get("Length"), new HashSet<>(Arrays.asList(LengthInterpreter.PATTERN)));
		pattern.put(EntityType.get("Thickness"), new HashSet<>(Arrays.asList(ThicknessInterpreter.PATTERN)));
		pattern.put(EntityType.get("NYUImpactor"), NYU_IMPACTOR_REG_EXP);
		pattern.put(EntityType.get("UnivOfTriesteImpactor"), UTS_IMPACTOR_REG_EXP);
	}

	public Map<EntityType, Set<Pattern>> getHandMadePattern() {
		return pattern;

	}

	@Override
	public Set<String> getStopWords() {
		return STOP_WORDS;
	}

	@Override
	public int getMinTokenlength() {
		return 2;
	}

}
