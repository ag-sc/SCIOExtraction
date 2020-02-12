package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.initializer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.AnnotationBuilder;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.variables.Annotations;
import de.hterhors.semanticmr.crf.variables.IStateInitializer;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.modes.Modes.EExtractGroupNamesMode;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.modes.Modes.EGroupNamesClusteringMode;

public class PredictCardinalityInitializer implements IStateInitializer {
	private final EExtractGroupNamesMode groupNameMode;

	private final EGroupNamesClusteringMode groupNameProcessingMode;

	private final Map<Instance, Integer> cache = new HashMap<>();

	public PredictCardinalityInitializer(EExtractGroupNamesMode groupNameMode,
			EGroupNamesClusteringMode groupNameProcessingMode, List<Instance> instances) {
		this.groupNameMode = groupNameMode;
		this.groupNameProcessingMode = groupNameProcessingMode;

		for (Instance instance : instances) {
			cache.put(instance, predictNumber());
		}
	}

	private Integer predictNumber() {
		return 2;
	}

	@Override
	public State getInitState(Instance instance) {
		List<AbstractAnnotation> as = new ArrayList<>();

		final int num = cache.getOrDefault(instance, 0);
		int count = 0;

		for (EntityTemplate goldAnnotation : instance.getGoldAnnotations().<EntityTemplate>getAnnotations()) {

			if (num == count)
				break;

			count++;

			EntityTemplate init = new EntityTemplate(
					AnnotationBuilder.toAnnotation(SCIOEntityTypes.definedExperimentalGroup));

			if (groupNameMode == EExtractGroupNamesMode.GOLD
					&& groupNameProcessingMode == EGroupNamesClusteringMode.GOLD_CLUSTERING) {

				if (goldAnnotation.getRootAnnotation().isInstanceOfDocumentLinkedAnnotation())
					init.addMultiSlotFiller(SCIOSlotTypes.hasGroupName,
							AnnotationBuilder.toAnnotation(instance.getDocument(), SCIOEntityTypes.groupName,
									goldAnnotation.getRootAnnotation().asInstanceOfDocumentLinkedAnnotation()
											.getSurfaceForm(),
									goldAnnotation.getRootAnnotation().asInstanceOfDocumentLinkedAnnotation()
											.getStartDocCharOffset()));

				for (AbstractAnnotation groupName : goldAnnotation.asInstanceOfEntityTemplate()
						.getMultiFillerSlot(SCIOSlotTypes.hasGroupName).getSlotFiller()) {
					init.addMultiSlotFiller(SCIOSlotTypes.hasGroupName, groupName);
				}
			}

			as.add(init);
		}

		if (count < num)
			for (int i = count; i < num; i++) {
				EntityTemplate init = new EntityTemplate(
						AnnotationBuilder.toAnnotation(SCIOEntityTypes.definedExperimentalGroup));
				as.add(init);
			}

		return new State(instance, new Annotations(as));
	}

}
