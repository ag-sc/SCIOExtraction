package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter;

import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.struct.IDoubleUnit;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.struct.IDoubleUnitType;

public enum EDosagesUnits implements IDoubleUnit {

	undef(0D, 0D, EType.ELSE),
	//
	percentage(1D, 1D, EType.PERCENT),
	//
	µg(E3, 1D, EType.WEIGHT), mg(1D, 1D, EType.WEIGHT), g(1D / E3, 1D, EType.WEIGHT),
	//
	µl(E3, 1D, EType.VOLUME), cm3(1D, 1D, EType.VOLUME), ml(1D, 1D, EType.VOLUME), l(1D / E3, 1D, EType.VOLUME),
	//
	iu(1D, 1D, EType.UNIT),
	//
	mm(1D, 1D, EType.MOL),
	//
	iu_kg(1D, 1D / E3, EType.UNIT_WEIGHT), iu_g(1D, 1D, EType.UNIT_WEIGHT), iu_mg(1D, E3, EType.UNIT_WEIGHT),
	iu_µg(1D, E6, EType.UNIT_WEIGHT), miu_µg(E3, E6, EType.UNIT_WEIGHT),
	//
	iu_kg_bw(1D, 1D / E3, EType.UNIT_WEIGHT_BW), iu_g_bw(1D, 1D, EType.UNIT_WEIGHT_BW),
	iu_mg_bw(1D, E3, EType.UNIT_WEIGHT_BW), iu_µg_bw(1D, E6, EType.UNIT_WEIGHT_BW),
	//
	iu_ml(1D, 1D, EType.UNIT_VOLUME), iu_µl(1D, E3, EType.UNIT_VOLUME), iu_l(1D, 1 / E3, EType.UNIT_VOLUME),
	miu_ml(E3, 1D, EType.UNIT_VOLUME), miu_µl(E3, E3, EType.UNIT_VOLUME),
	//
	iu_day(1D, 1D, EType.UNIT_TIME), µiu_day(E6, 1D, EType.UNIT_TIME),
	//
	mg_h(1D, 1D, EType.WEIGHT_TIME), mg_day(1D, 1D / 24D, EType.WEIGHT_TIME), µg_h(E3, 1D, EType.WEIGHT_TIME),
	µg_day(E3, 1D / 24D, EType.WEIGHT_TIME), g_h(1D / E3, 1D, EType.WEIGHT_TIME),
	g_day(1D / E3, 1D / 24D, EType.WEIGHT_TIME),
	//
	µmol_kg(1D, 1D, EType.MOL_WEIGHT),
	//
	µl_ml(1D, 1D, EType.VOLUME_VOLUME),
	//
	mmol_l(1D, 1D, EType.MOL_VOLUME), µmol_l(E3, 1D, EType.MOL_VOLUME), mol_l(1D / E3, 1D, EType.MOL_VOLUME),
	//
	cfu_ml(1D, 1D, EType.CFU_WEIGHT),
	//
	µl_kg(E3, 1D / E3, EType.VOLUME_WEIGHT), ml_g(1D, 1D, EType.VOLUME_WEIGHT), ml_kg(1D, 1D / E3, EType.VOLUME_WEIGHT),
	µl_g(E3, 1D, EType.VOLUME_WEIGHT),
	//
	µl_kg_bw(E3, 1D / E3, EType.VOLUME_WEIGHT_BW), ml_g_bw(1D, 1D, EType.VOLUME_WEIGHT_BW),
	ml_mg_bw(1D, E3, EType.VOLUME_WEIGHT_BW), ml_kg_bw(1D, 1D / E3, EType.VOLUME_WEIGHT_BW),
	µl_g_bw(E3, 1D, EType.VOLUME_WEIGHT_BW),
	//
	µl_h(E3, 1D, EType.VOLUME_TIME), ml_h(1D, 1D, EType.VOLUME_TIME), l_h(1D / E3, 1D, EType.VOLUME_TIME),
	l_min(1 / E3, 60D, EType.VOLUME_TIME), µl_day(E3, 1 / 24D, EType.VOLUME_TIME),
	ml_day(1D, 1 / 24D, EType.VOLUME_TIME),
	//
	µg_ml(E3, 1D, EType.WEIGHT_VOLUME), µg_l(E3, 1D / E3, EType.WEIGHT_VOLUME), mg_ml(1D, 1D, EType.WEIGHT_VOLUME),
	mg_l(1D, 1D / E3, EType.WEIGHT_VOLUME), g_ml(1 / E3, 1D, EType.WEIGHT_VOLUME),
	g_l(1D / E3, 1d / E3, EType.WEIGHT_VOLUME), mg_µl(1D, E3, EType.WEIGHT_VOLUME), µg_µl(E3, E3, EType.WEIGHT_VOLUME),
	//
	µg_kg(E3, 1D / E3, EType.WEIGHT_WEIGHT), mg_g(E3, 1D, EType.WEIGHT_WEIGHT), g_g(1D, 1D, EType.WEIGHT_WEIGHT),
	mg_kg(E3, 1D / E3, EType.WEIGHT_WEIGHT), g_kg(1D, 1D / E3, EType.WEIGHT_WEIGHT), g_mg(1D, E3, EType.WEIGHT_WEIGHT),
	µg_mg(E6, E3, EType.WEIGHT_WEIGHT),
	//
	µg_kg_bw(E6, 1D / E3, EType.WEIGHT_WEIGHT_BW), mg_g_bw(E3, 1D, EType.WEIGHT_WEIGHT_BW),
	g_g_bw(1D, 1D, EType.WEIGHT_WEIGHT_BW), mg_kg_bw(E3, 1D / E3, EType.WEIGHT_WEIGHT_BW),
	g_kg_bw(1D, 1D / E3, EType.WEIGHT_WEIGHT_BW), mg_mg_bw(E3, E3, EType.WEIGHT_WEIGHT_BW);

	final public double determinatorFactor;
	final public double numeratorFactor;
	final EType type;

	private EDosagesUnits(double numeratorFactor, double determinatorFactor, EType type) {
		this.numeratorFactor = numeratorFactor;
		this.determinatorFactor = determinatorFactor;
		this.type = type;
	}

	static enum EType implements IDoubleUnitType {
		ELSE("undef"), PERCENT("percentage"), WEIGHT("mg"), VOLUME("ml"), UNIT("iu"), MOL("mm"),
		//
		UNIT_WEIGHT("iu_g"), WEIGHT_WEIGHT("g_g"), WEIGHT_VOLUME("mg_ml"), VOLUME_TIME("ml_h"), WEIGHT_TIME("mg_h"),
		VOLUME_WEIGHT("ml_g"), UNIT_VOLUME("iu_ml"), VOLUME_VOLUME("µl_ml"), UNIT_WEIGHT_BW("iu_g_bw"),
		WEIGHT_WEIGHT_BW("g_g_bw"), VOLUME_WEIGHT_BW("ml_g_bw"), MOL_WEIGHT("µmol_kg"), MOL_VOLUME("mmol_l"),
		CFU_WEIGHT("cfu_ml"), UNIT_TIME("iu_day");

		final private String defaultUnitName;

		private EType(String defaultUnit) {
			this.defaultUnitName = defaultUnit;
		}

		public EDosagesUnits getDefaultUnit() {
			return EDosagesUnits.valueOf(defaultUnitName);
		}

	}

	@Override
	public IDoubleUnitType getType() {
		return type;
	}

	@Override
	public double getNumeratorFactor() {
		return numeratorFactor;
	}

	@Override
	public double getDeterminatorFactor() {
		return determinatorFactor;
	}

	@Override
	public String getName() {
		return this.name();
	}
}