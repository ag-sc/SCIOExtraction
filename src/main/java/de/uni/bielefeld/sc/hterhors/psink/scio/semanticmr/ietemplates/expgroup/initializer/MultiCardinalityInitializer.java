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

public class MultiCardinalityInitializer implements IStateInitializer {

	/**
	 * The maximum number of annotations
	 */
	private final int max;

	/**
	 * The current number of annotations;
	 */
	private int current;

	private final EExtractGroupNamesMode groupNameMode;

	public MultiCardinalityInitializer(EExtractGroupNamesMode groupNameMode, int max) {
		this.max = max;
		this.current = 1;
		this.groupNameMode = groupNameMode;
	}

	@Override
	public State getInitState(Instance instance) {
		return null;
	}

	@Override
	public List<State> getInitMultiStates(Instance instance) {
		current = 1;

		List<State> list = new ArrayList<>();

		do {
			List<AbstractAnnotation> as = new ArrayList<>();

			int count = 0;
			for (EntityTemplate goldAnnotation : instance.getGoldAnnotations().<EntityTemplate>getAnnotations()) {

				if (current == count)
					break;

				count++;

				EntityTemplate init = new EntityTemplate(
						AnnotationBuilder.toAnnotation(SCIOEntityTypes.definedExperimentalGroup));

				if (groupNameMode == EExtractGroupNamesMode.GOLD_CLUSTERED) {

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

			if (count < current)
				for (int i = count; i < current; i++) {
					EntityTemplate init = new EntityTemplate(
							AnnotationBuilder.toAnnotation(SCIOEntityTypes.definedExperimentalGroup));
					as.add(init);
				}

			list.add(new State(instance, new Annotations(as)));
		} while (increase());

		return list;
	}

	/**
	 * Increases the current number of annotations.
	 * 
	 * @return true if current number is smaller or equal to max.
	 */
	private boolean increase() {
		current++;
		if (current <= max)
			return true;
		return false;
	}

}
