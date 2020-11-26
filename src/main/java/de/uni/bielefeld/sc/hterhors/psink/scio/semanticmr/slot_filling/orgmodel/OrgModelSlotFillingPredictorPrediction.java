package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.orgmodel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.ENERModus;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.orgmodel.OrganismModelRestrictionProvider.EOrgModelModifications;

/**
 * Slot filling for organism models.
 * 
 * @author hterhors
 *
 */
public class OrgModelSlotFillingPredictorPrediction extends OrgModelSlotFillingPredictor {

	public OrgModelSlotFillingPredictorPrediction(String modelName, List<String> trainingInstances,
			EOrgModelModifications rule, ENERModus modus) {
		super(modelName, trainingInstances, new ArrayList<>(), new ArrayList<>(), rule, modus);
	}

	@Override
	protected File getExternalNerlaFile() {

		return new File("prediction/nerla/");

	}

	@Override
	protected File getInstanceDirectory() {
		return new File("prediction/instances/");
	}

}
