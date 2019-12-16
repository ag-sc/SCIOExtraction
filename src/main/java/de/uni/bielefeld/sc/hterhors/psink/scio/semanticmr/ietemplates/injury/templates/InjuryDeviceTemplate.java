package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.injury.templates;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import de.hterhors.semanticmr.crf.model.AbstractFactorScope;
import de.hterhors.semanticmr.crf.model.Factor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.SingleFillerSlot;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.injury.templates.InjuryDeviceTemplate.InjuryDeviceScope;

/**
 * @author hterhors
 *
 * @date Nov 15, 2017
 */
public class InjuryDeviceTemplate extends AbstractFeatureTemplate<InjuryDeviceScope> {

	static class InjuryDeviceScope extends AbstractFactorScope {

		public final EntityType injury;
		public final EntityType device;

		public InjuryDeviceScope(InjuryDeviceTemplate injuryDeviceTemplate, EntityType injury, EntityType device) {
			super(injuryDeviceTemplate);
			this.injury = injury;
			this.device = device;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((device == null) ? 0 : device.hashCode());
			result = prime * result + ((injury == null) ? 0 : injury.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			InjuryDeviceScope other = (InjuryDeviceScope) obj;
			if (device == null) {
				if (other.device != null)
					return false;
			} else if (!device.equals(other.device))
				return false;
			if (injury == null) {
				if (other.injury != null)
					return false;
			} else if (!injury.equals(other.injury))
				return false;
			return true;
		}

		@Override
		public int implementHashCode() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public boolean implementEquals(Object obj) {
			// TODO Auto-generated method stub
			return false;
		}

	}

	private static final String PREFIX = "IDT\t";

	@Override
	public List<InjuryDeviceScope> generateFactorScopes(State state) {
		List<InjuryDeviceScope> factors = new ArrayList<>();

		if (SCIOSlotTypes.hasInjuryDevice.isExcluded())
			return factors;

		for (EntityTemplate annotation : super.<EntityTemplate>getPredictedAnnotations(state)) {

			EntityType injury = annotation.getEntityType();

			if (!EntityType.get("Injury").getTransitiveClosureSubEntityTypes().contains(injury))
				continue;

			SingleFillerSlot sfs = annotation.getSingleFillerSlot(SCIOSlotTypes.hasInjuryDevice);

			if (!sfs.containsSlotFiller())
				continue;

			AbstractAnnotation injuryDevice = sfs.getSlotFiller();

			factors.add(new InjuryDeviceScope(this, injury, injuryDevice.getEntityType()));
		}

		return factors;
	}

	@Override
	public void generateFeatureVector(Factor<InjuryDeviceScope> factor) {
		factor.getFeatureVector()
				.set(PREFIX + factor.getFactorScope().injury.name + "\t" + factor.getFactorScope().device.name, true);
	}

}
