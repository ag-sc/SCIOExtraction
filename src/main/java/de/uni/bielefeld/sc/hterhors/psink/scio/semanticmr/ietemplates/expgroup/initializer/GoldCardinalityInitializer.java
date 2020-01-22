package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.initializer;

import java.util.ArrayList;
import java.util.List;

import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.AnnotationBuilder;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.variables.Annotations;
import de.hterhors.semanticmr.crf.variables.IStateInitializer;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.modes.Modes.EExtractGroupNamesMode;

public class GoldCardinalityInitializer implements IStateInitializer {

	private final EExtractGroupNamesMode groupNameMode;

	public GoldCardinalityInitializer(EExtractGroupNamesMode groupNameMode) {
		this.groupNameMode = groupNameMode;
	}

	@Override
	public State getInitState(Instance instance) {
		List<AbstractAnnotation> as = new ArrayList<>();

		for (EntityTemplate goldAnnotation : instance.getGoldAnnotations().<EntityTemplate>getAnnotations()) {

			EntityTemplate init = new EntityTemplate(
					AnnotationBuilder.toAnnotation(SCIOEntityTypes.definedExperimentalGroup));
			if (groupNameMode == EExtractGroupNamesMode.GOLD_CLUSTERED) {

				for (AbstractAnnotation groupName : goldAnnotation.asInstanceOfEntityTemplate()
						.getMultiFillerSlot(SCIOSlotTypes.hasGroupName).getSlotFiller()) {
					init.addMultiSlotFiller(SCIOSlotTypes.hasGroupName, groupName);
				}
			}

			as.add(init);
		}

		return new State(instance, new Annotations(as));

	}
}
