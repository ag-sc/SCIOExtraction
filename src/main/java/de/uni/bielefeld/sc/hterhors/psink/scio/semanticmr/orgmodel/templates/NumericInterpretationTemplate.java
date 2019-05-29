package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.orgmodel.templates;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.crf.factor.AbstractFactorScope;
import de.hterhors.semanticmr.crf.factor.Factor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.LiteralAnnotation;
import de.hterhors.semanticmr.crf.structure.slots.SlotType;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.DoubleVector;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.normalizer.SCIONormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.normalizer.interpreter.struct.ILiteralInterpreter;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.normalizer.interpreter.struct.INumericInterpreter;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.normalizer.interpreter.struct.IUnit;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.orgmodel.templates.NumericInterpretationTemplate.NumericInterpretationScope;

/**
 * Captures the semantics of datatype properties.
 * 
 * Converts raw input text (surface form) of annotations into interpretable
 * format. Checks whether the value of the data type property is within the
 * range of the mean. The mean is pre-calculated over the training data.
 * 
 * @author hterhors
 *
 * @date May 12, 2017
 */
public class NumericInterpretationTemplate extends AbstractFeatureTemplate<NumericInterpretationScope>
		implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Logger log = LogManager.getFormatterLogger(NumericInterpretationTemplate.class);

	public ILiteralInterpreter getInterpreterFromAnnotation(LiteralAnnotation obieClass) {
		return ((SCIONormalization) obieClass.getEntityType().getNormalizationFunction())
				.getInterpreter(obieClass.asInstanceOfDocumentLinkedAnnotation().textualContent.surfaceForm)
				.normalize();
	}

	/**
	 * TODO: make it deep!
	 */
//		ReflectionUtils.getFields(obieClass.getClass(), obieClass.getInvestigationRestriction()).forEach(field -> {
//			try {
//				if (field.isAnnotationPresent(RelationTypeCollection.class)) {
//					for (IOBIEThing element : (List<IOBIEThing>) field.get(obieClass)) {
//						collectDataTypes(values, obieClass.getClass(), element);
//					}
//				} else {
//					collectDataTypes(values, obieClass.getClass(), (IOBIEThing) field.get(obieClass));
//				}
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		});

	static class NumericInterpretationScope extends AbstractFactorScope<NumericInterpretationScope> {

		final EntityType parentScioClass;
		final EntityType scioClass;
		final INumericInterpreter interpretation;

		public NumericInterpretationScope(AbstractFeatureTemplate<NumericInterpretationScope> template,
				EntityType parentClassName, EntityType className, final INumericInterpreter interpretation) {
			super(template);
			this.scioClass = className;
			this.parentScioClass = parentClassName;
			this.interpretation = interpretation;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((parentScioClass == null) ? 0 : parentScioClass.hashCode());
			result = prime * result + ((scioClass == null) ? 0 : scioClass.hashCode());
			result = prime * result + ((interpretation == null) ? 0 : interpretation.hashCode());
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
			NumericInterpretationScope other = (NumericInterpretationScope) obj;
			if (parentScioClass == null) {
				if (other.parentScioClass != null)
					return false;
			} else if (!parentScioClass.equals(other.parentScioClass))
				return false;
			if (scioClass == null) {
				if (other.scioClass != null)
					return false;
			} else if (!scioClass.equals(other.scioClass))
				return false;
			if (interpretation == null) {
				if (other.interpretation != null)
					return false;
			} else if (!interpretation.equals(other.interpretation))
				return false;
			return true;
		}

		@Override
		public int implementHashCode() {
			return 0;
		}

		@Override
		public boolean implementEquals(Object obj) {
			return false;
		}

	}

	@Override
	public List<NumericInterpretationScope> generateFactorScopes(State state) {
		List<NumericInterpretationScope> factors = new ArrayList<>();

		for (AbstractAnnotation entity : state.getCurrentPredictions().getAnnotations()) {

			final EntityType rootEntityType = entity.asInstanceOfEntityTemplate().getEntityType();

			final Map<SlotType, Set<AbstractAnnotation>> x = entity.asInstanceOfEntityTemplate().filter()
					.docLinkedAnnoation().literalAnnoation().singleSlots().multiSlots().merge().nonEmpty().build()
					.getMergedAnnotations();

			for (Entry<SlotType, Set<AbstractAnnotation>> a : x.entrySet()) {

				for (AbstractAnnotation childEntity : a.getValue()) {

					if (!(childEntity.getEntityType().getNormalizationFunction() instanceof SCIONormalization))
						continue;

					ILiteralInterpreter interpretation = getInterpreterFromAnnotation(
							childEntity.asInstanceOfLiteralAnnotation());

					if (!interpretation.isNumeric())
						continue;

					factors.add(new NumericInterpretationScope(this, rootEntityType, childEntity.getEntityType(),
							(INumericInterpreter) interpretation));

				}

			}

			/**
			 * TODO: make it deep!
			 */
//			/*
//			 * Add factors for object type properties.
//			 */
//			Arrays.stream(obieClass.getClass().getDeclaredFields())
//					.filter(f -> f.isAnnotationPresent(OntologyModelContent.class)).forEach(field -> {
//						field.setAccessible(true);
//						try {
//							if (field.isAnnotationPresent(RelationTypeCollection.class)) {
//								for (IOBIEThing element : (List<IOBIEThing>) field.get(obieClass)) {
//									factors.addAll(addFactorRecursive(rootClassType, obieClass.getClass(), element));
//								}
//							} else {
//								factors.addAll(addFactorRecursive(rootClassType, obieClass.getClass(),
//										(IOBIEThing) field.get(obieClass)));
//							}
//						} catch (Exception e) {
//							e.printStackTrace();
//						}
//					});
		}

		return factors;
	}

	@Override
	public void generateFeatureVector(Factor<NumericInterpretationScope> factor) {
		DoubleVector featureVector = factor.getFeatureVector();

		addFeatures(factor.getFactorScope().parentScioClass, factor.getFactorScope().scioClass, featureVector,
				factor.getFactorScope().interpretation);

		final Set<EntityType> rootClassTypes = factor.getFactorScope().parentScioClass.getTransitiveClosureSuperEntityTypes();

		for (EntityType rootClassType : rootClassTypes) {
			addFeatures(rootClassType, factor.getFactorScope().scioClass, featureVector,
					factor.getFactorScope().interpretation);
		}
		// for (final double level : levels) {
		//
		// final boolean greaterOrEqual = semantics.getMeanValue() >= level;
		//
		// featureVector.set(String.format(GREATER_OR_EQUAL_TEMPLATE, className,
		// parentClassName, level,
		// semantics.getUnitName()), greaterOrEqual);
		//
		// featureVector.set(
		// String.format(LESS_THEN_TEMPLATE, className, parentClassName, level,
		// semantics.getUnitName()),
		// !greaterOrEqual);
		//
		// }

	}

	// public static void main(String[] args) {
	//
	// final double meanValue = 200;
	//
	// final double stdDeviation = 20;
	// final double v = 2;
	// for (int i = 4; i >= 1; i--) {
	//
	// boolean within = Math.abs(v - meanValue) <= i * stdDeviation;
	//
	// /**
	// * Add only the feature which is the farthest away from mean.
	// */
	// if (!within) {
	// System.out.println(
	// String.format(NOT_MEAN_STD_DEVIATION_TEMPLATE, "Weight", "Rat", i,
	// stdDeviation, meanValue, "g")
	// + " -> " + !within);
	// break;
	// }
	//
	// }
	// for (int i = 1; i <= 4; i++) {
	//
	// boolean within = Math.abs(v - meanValue) <= i * stdDeviation;
	//
	// /**
	// * Add only the feature which is the nearest to the mean.
	// */deleted
	// if (within) {
	// System.out.println(
	// String.format(MEAN_STD_DEVIATION_TEMPLATE, "Weight", "Rat", i,
	// stdDeviation, meanValue, "g")
	// + "->" + within);
	// break;
	// }
	// }
	//
	// }

	private void addFeatures(EntityType parentClassType, EntityType propertyClassType, DoubleVector featureVector,
			final INumericInterpreter interpretation) {
		final String parentClassName = parentClassType.entityName;
		final String className = propertyClassType.entityName;

		final IUnit unit = interpretation.getUnit();

		int norm = (100 * ((int) interpretation.getMeanValue() / 100));
		final double dist = norm * 0.1;

		for (int i = -4; i <= 4; i++) {

			int v = (int) (norm + i * dist);

			final String feature = parentClassName + "->" + className + v + " " + unit.getName();
			featureVector.set(feature, true);

		}

	}

}
