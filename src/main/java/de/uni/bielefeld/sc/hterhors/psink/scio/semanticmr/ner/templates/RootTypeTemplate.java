package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.templates;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import de.hterhors.semanticmr.crf.model.AbstractFactorScope;
import de.hterhors.semanticmr.crf.model.Factor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.State;

/**
 * Compares the RootType of two Mentions
 */
public class RootTypeTemplate extends AbstractFeatureTemplate<RootTypeTemplate.RootTypeScope> {


	private List<EntityType> rootEntitytypes;

	static class RootTypeScope
			extends AbstractFactorScope {

		EntityType typeOne;
		EntityType typeTwo;

		public RootTypeScope(AbstractFeatureTemplate template, EntityType typeOne, EntityType typeTwo) {
			super(template);
			this.typeOne = typeOne;
			this.typeTwo = typeTwo;
		}

		@Override
		public int implementHashCode() {
			return 0;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			if (!super.equals(o)) return false;
			RootTypeScope that = (RootTypeScope) o;
			return Objects.equals(typeOne, that.typeOne) &&
					Objects.equals(typeTwo, that.typeTwo);
		}

		@Override
		public int hashCode() {
			return Objects.hash(super.hashCode(), typeOne, typeTwo);
		}

		@Override
		public boolean implementEquals(Object obj) {
			return false;
		}

	}

	@Override
	public List<RootTypeScope> generateFactorScopes(State state) {
		List<RootTypeScope> factors = new ArrayList<>();

		for (DocumentLinkedAnnotation annotation : super.<DocumentLinkedAnnotation>getPredictedAnnotations(state)) {
			for (DocumentLinkedAnnotation annotation2 : super.<DocumentLinkedAnnotation>getPredictedAnnotations(state))
				if(!annotation.equals(annotation2))
			factors.add(new RootTypeScope(this, annotation.getEntityType(),annotation2.getEntityType()));
		}
		return factors;
	}

	@Override
	public void generateFeatureVector(Factor<RootTypeScope> factor) {
			// factor.getFeatureVector().set();

		Set<EntityType> superTypesOne = factor.getFactorScope().typeOne.getHierarchicalEntityTypes();
		Set<EntityType> superTypesTwo = factor.getFactorScope().typeTwo.getHierarchicalEntityTypes();

		for(EntityType superTypeOne: superTypesOne){
			for(EntityType superTypeTwo: superTypesTwo){
				factor.getFeatureVector().set("SuperTypes in same Document: <" + superTypeOne.name + ", " + superTypeTwo.name +">",true);
			}
		}
	}
}
