package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.delivery_method;

import java.io.File;
import java.util.List;

import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.ENERModus;

/**
 * Slot filling for organism models.
 * 
 * @author hterhors
 *
 */
public class DeliveryMethodPredictorPrediction extends DeliveryMethodPredictor {

	public DeliveryMethodPredictorPrediction(String modelName, List<String> trainingInstanceNames,
			List<String> developInstanceNames, List<String> testInstanceNames, IModificationRule rule,
			ENERModus modus) {
		super(modelName, trainingInstanceNames, developInstanceNames, testInstanceNames, rule, modus);
	}

	@Override
	protected File getInstanceDirectory() {
		return new File("prediction/instances/");
	}

	@Override
	protected File getExternalNerlaFile() {
		return new File("prediction/nerla/");
	}

}
