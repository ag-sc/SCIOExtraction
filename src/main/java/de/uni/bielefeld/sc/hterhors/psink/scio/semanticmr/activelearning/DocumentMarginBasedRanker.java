package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.activelearning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.activelearning.IActiveLearningDocumentRanker;
import de.hterhors.semanticmr.activelearning.RankedInstance;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.AbstractSlotFillingPredictor;

public class DocumentMarginBasedRanker implements IActiveLearningDocumentRanker {

	final Logger log = LogManager.getRootLogger();

	final private AbstractSlotFillingPredictor runner;

	public DocumentMarginBasedRanker(AbstractSlotFillingPredictor runner) {
		this.runner = runner;
	}

	@Override
	public List<Instance> rank(List<Instance> remainingInstances) {

		List<RankedInstance> entropyInstances = new ArrayList<>();

		log.info("Compute variations for margin...");
		Map<Instance, List<State>> results = runner.crf.collectNBestStates(remainingInstances, 2, runner.maxStepCrit);

		log.info("Compute margin...");
		for (Entry<Instance, List<State>> predictedInstance : results.entrySet()) {

			double margin;

			if (predictedInstance.getValue().size() != 2) {
				// Highest Value =
				margin = Double.MAX_VALUE;
			} else {
				// Always negative
				margin = Math.abs(predictedInstance.getValue().get(0).getModelScore()
						- predictedInstance.getValue().get(1).getModelScore());
			}

			/*
			 * Sorted by highest first
			 */
			entropyInstances.add(new RankedInstance(margin, predictedInstance.getKey()));

		}

		log.info("Sort...");

		Collections.sort(entropyInstances);
		// Reverse order since greater margin = better
		Collections.reverse(entropyInstances);

		return entropyInstances.stream().map(e -> e.instance).collect(Collectors.toList());

	}

}
