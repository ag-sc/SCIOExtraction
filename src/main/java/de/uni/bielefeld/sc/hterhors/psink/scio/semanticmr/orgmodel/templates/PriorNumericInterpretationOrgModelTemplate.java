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

import de.hterhors.semanticmr.crf.model.AbstractFactorScope;
import de.hterhors.semanticmr.crf.model.Factor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.LiteralAnnotation;
import de.hterhors.semanticmr.crf.structure.slots.SingleFillerSlot;
import de.hterhors.semanticmr.crf.structure.slots.SlotType;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.DoubleVector;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.normalizer.AbstractSCIONormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.normalizer.interpreter.struct.ILiteralInterpreter;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.normalizer.interpreter.struct.INumericInterpreter;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.normalizer.interpreter.struct.IUnit;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.orgmodel.templates.PriorNumericInterpretationOrgModelTemplate.NumericInterpretationScope;

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
public class PriorNumericInterpretationOrgModelTemplate extends AbstractFeatureTemplate<NumericInterpretationScope>
		implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Logger log = LogManager.getFormatterLogger(PriorNumericInterpretationOrgModelTemplate.class);

	// final private static String GREATER_OR_EQUAL_TEMPLATE = "Mean_%s_of_%s >=
	// %s %s";
	// final private static String LESS_THEN_TEMPLATE = "Mean_%s_of_%s < %s %s";
	final private static String MEAN_STD_DEVIATION_TEMPLATE = "%s of %s is within %s x std deviation(%s) of mean(%s) unit(%s)";
	final private static String NOT_MEAN_STD_DEVIATION_TEMPLATE = "%s of %s is NOT within %s x std deviation (%s) of mean(%s) unit(%s)";

	private static final String PREFIX = "PNIOMT\t";

	/**
	 * Parent class type, property class type, unit, value
	 */
	private Map<EntityType, Map<EntityType, Map<IUnit, Double>>> stdDeviations;
	private Map<EntityType, Map<EntityType, Map<IUnit, Double>>> meanValues;

	@Override
	public void initalize(Object[] parameter) {
		
		List<Instance> trainingInstances = (List<Instance>) parameter[0];

		Map<EntityType, Map<EntityType, Map<IUnit, List<Double>>>> values = extractLiteralValues(trainingInstances);

		this.meanValues = calculateMeanValues(values);
		this.stdDeviations = calculateStandardDeviations(this.meanValues, values);

		log.info("Mean values with standard deviation:");
		for (Entry<EntityType, Map<EntityType, Map<IUnit, Double>>> parent : this.meanValues.entrySet()) {
			for (Entry<EntityType, Map<IUnit, Double>> property : parent.getValue().entrySet()) {
				for (Entry<IUnit, Double> unit : property.getValue().entrySet()) {
					log.info(parent.getKey().entityName + " -> " + property.getKey().entityName + " = "
							+ unit.getValue() + " +- "
							+ this.stdDeviations.get(parent.getKey()).get(property.getKey()).get(unit.getKey()) + " "
							+ unit.getKey());
				}
			}
		}
	}

	/**
	 * This method calculates the standard deviation for all data type properties.
	 * For that it makes use of the pre-calculated mean values!
	 * 
	 * @param meanValues
	 * 
	 * @param values
	 */
	private Map<EntityType, Map<EntityType, Map<IUnit, Double>>> calculateStandardDeviations(
			Map<EntityType, Map<EntityType, Map<IUnit, Double>>> meanValues,
			Map<EntityType, Map<EntityType, Map<IUnit, List<Double>>>> values) {

		Map<EntityType, Map<EntityType, Map<IUnit, Double>>> stdDeviations = new HashMap<>();

		for (Entry<EntityType, Map<EntityType, Map<IUnit, List<Double>>>> parents : values.entrySet()) {

			final EntityType parentClassType = parents.getKey();
			stdDeviations.put(parentClassType, new HashMap<>());

			for (Entry<EntityType, Map<IUnit, Double>> properties : meanValues.get(parentClassType).entrySet()) {
				final EntityType propertyClassType = properties.getKey();
				stdDeviations.get(parentClassType).put(propertyClassType, new HashMap<>());

				for (Entry<IUnit, Double> units : properties.getValue().entrySet()) {

					final IUnit unit = units.getKey();
					final double meanValue = units.getValue();

					double variance = 0;
					for (Double value : values.get(parentClassType).get(propertyClassType).get(unit)) {
						variance += Math.pow(meanValue - value, 2);
					}
					stdDeviations.get(parentClassType).get(propertyClassType).put(unit,
							Math.sqrt(variance / values.get(parentClassType).get(propertyClassType).get(unit).size()));
				}
			}
		}
		return stdDeviations;
	}

	/**
	 * Calculates the mean values for all data type properties. To do that, it first
	 * calculates the sum for all data type properties which is used to calculate
	 * the mean in a second step.
	 * 
	 * @param values
	 * @return
	 */
	private Map<EntityType, Map<EntityType, Map<IUnit, Double>>> calculateMeanValues(
			Map<EntityType, Map<EntityType, Map<IUnit, List<Double>>>> values) {

		final Map<EntityType, Map<EntityType, Map<IUnit, Double>>> sumValues = new HashMap<>();
		final Map<EntityType, Map<EntityType, Map<IUnit, Double>>> meanValues = new HashMap<>();

		/*
		 * Calculate the sums.
		 */
		for (Entry<EntityType, Map<EntityType, Map<IUnit, List<Double>>>> parents : values.entrySet()) {
			final EntityType parentClassType = parents.getKey();
			sumValues.putIfAbsent(parentClassType, new HashMap<>());

			for (Entry<EntityType, Map<IUnit, List<Double>>> properties : parents.getValue().entrySet()) {
				final EntityType propertyClassType = properties.getKey();
				sumValues.get(parentClassType).putIfAbsent(propertyClassType, new HashMap<>());

				for (Entry<IUnit, List<Double>> units : properties.getValue().entrySet()) {
					final IUnit unit = units.getKey();

					for (Double meanValue : units.getValue()) {
						sumValues.get(parentClassType).get(propertyClassType).put(unit,
								sumValues.get(parentClassType).get(propertyClassType).getOrDefault(unit, 0D)
										+ meanValue);
					}
				}
			}
		}

		/*
		 * Calculate the mean.
		 */
		for (Entry<EntityType, Map<EntityType, Map<IUnit, List<Double>>>> parents : values.entrySet()) {
			final EntityType parentClassType = parents.getKey();
			meanValues.putIfAbsent(parentClassType, new HashMap<>());

			for (Entry<EntityType, Map<IUnit, List<Double>>> properties : parents.getValue().entrySet()) {
				final EntityType propertyClassType = properties.getKey();
				meanValues.get(parentClassType).putIfAbsent(propertyClassType, new HashMap<>());

				for (Entry<IUnit, List<Double>> units : properties.getValue().entrySet()) {
					final IUnit unit = units.getKey();

					meanValues.get(parentClassType).get(propertyClassType).put(unit,
							sumValues.get(parentClassType).get(propertyClassType).get(unit) / units.getValue().size());
				}
			}
		}
		return meanValues;
	}

	/**
	 * Extracts all data type values from the given training data set. Each value is
	 * directly semantically interpreted and normalized.
	 * 
	 * @param train the training data to extract the data type values from.
	 * @return a map of data type values for each property, stored in a list.
	 */
	private Map<EntityType, Map<EntityType, Map<IUnit, List<Double>>>> extractLiteralValues(List<Instance> train) {
		Map<EntityType, Map<EntityType, Map<IUnit, List<Double>>>> values = new HashMap<>();

		for (Instance internalInstance : train) {
			for (AbstractAnnotation internalAnnotation : internalInstance.getGoldAnnotations().getAnnotations()) {

				Map<SlotType, Set<AbstractAnnotation>> x = internalAnnotation.asInstanceOfEntityTemplate().filter()
						.docLinkedAnnoation().literalAnnoation().merge().singleSlots().multiSlots().nonEmpty().build()
						.getMergedAnnotations();

				for (Entry<SlotType, Set<AbstractAnnotation>> instance : x.entrySet()) {

					for (AbstractAnnotation instance2 : instance.getValue()) {
						if (instance2.asInstanceOfLiteralAnnotation().getEntityType().isLiteral)
							collectLiterals(values, internalAnnotation, instance2.asInstanceOfLiteralAnnotation());
					}
				}
			}

		}
		return values;

	}

	private void collectLiterals(Map<EntityType, Map<EntityType, Map<IUnit, List<Double>>>> values,
			AbstractAnnotation internalAnnotation, LiteralAnnotation obieClass) {

		if (!(obieClass.getEntityType().getNormalizationFunction() instanceof AbstractSCIONormalization))
			return;
		ILiteralInterpreter interpreter = getInterpreterFromAnnotation(obieClass);

		SingleFillerSlot sfs = internalAnnotation.asInstanceOfEntityTemplate()
				.getSingleFillerSlot(SlotType.get("hasOrganismSpecies"));

		if (!sfs.containsSlotFiller())
			return;

		EntityType entityType = sfs.getSlotFiller().getEntityType();

		/*
		 * Only for Numeric data type Values.
		 */
		if (interpreter.isNumeric() && interpreter.isInterpretable()) {

			IUnit unit = ((INumericInterpreter) interpreter).getUnit();

			values.putIfAbsent(entityType, new HashMap<>());
			values.get(entityType).putIfAbsent(obieClass.getEntityType(), new HashMap<>());
			values.get(entityType).get(obieClass.getEntityType()).putIfAbsent(unit, new ArrayList<>());
			values.get(entityType).get(obieClass.getEntityType()).get(unit)
					.add(((INumericInterpreter) interpreter).getMeanValue());

			final Set<EntityType> rootTypes = entityType.getDirectSuperEntityTypes();

			for (EntityType rootClassType : rootTypes) {
				values.putIfAbsent(rootClassType, new HashMap<>());
				values.get(rootClassType).putIfAbsent(obieClass.getEntityType(), new HashMap<>());
				values.get(rootClassType).get(obieClass.getEntityType()).putIfAbsent(unit, new ArrayList<>());
				values.get(rootClassType).get(obieClass.getEntityType()).get(unit)
						.add(((INumericInterpreter) interpreter).getMeanValue());
			}
		}
	}

	public ILiteralInterpreter getInterpreterFromAnnotation(LiteralAnnotation obieClass) {
		return ((AbstractSCIONormalization) obieClass.getEntityType().getNormalizationFunction())
				.getInterpreter(obieClass.textualContent.surfaceForm).normalize();
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

	static class NumericInterpretationScope extends AbstractFactorScope {

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

//			final EntityType rootEntityType = entity.asInstanceOfEntityTemplate().getEntityType();

			final Map<SlotType, Set<AbstractAnnotation>> x = entity.asInstanceOfEntityTemplate().filter()
					.docLinkedAnnoation().literalAnnoation().singleSlots().multiSlots().merge().nonEmpty().build()
					.getMergedAnnotations();

			for (Entry<SlotType, Set<AbstractAnnotation>> a : x.entrySet()) {

				for (AbstractAnnotation childEntity : a.getValue()) {

					if (!(childEntity.getEntityType().getNormalizationFunction() instanceof AbstractSCIONormalization))
						continue;

					ILiteralInterpreter interpretation = getInterpreterFromAnnotation(
							childEntity.asInstanceOfLiteralAnnotation());

					if (!interpretation.isNumeric())
						continue;

					/**
					 * Take species instead of root entity type. because root entity type is always
					 * organism model for animal models.
					 */
					SingleFillerSlot sfs = entity.asInstanceOfEntityTemplate()
							.getSingleFillerSlot(SlotType.get("hasOrganismSpecies"));

					if (!sfs.containsSlotFiller())
						continue;

					EntityType entityType = sfs.getSlotFiller().getEntityType();

					factors.add(new NumericInterpretationScope(this, entityType, childEntity.getEntityType(),
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

		final Set<EntityType> rootClassTypes = factor.getFactorScope().parentScioClass.getDirectSuperEntityTypes();

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
			final INumericInterpreter semantics) {
		final String parentClassName = parentClassType.entityName;
		final String className = propertyClassType.entityName;

		if (!semantics.isInterpretable())
			return;

		if (meanValues.containsKey(parentClassType)) {
			final IUnit unit = semantics.getUnit();
			final double meanValue = this.meanValues.getOrDefault(parentClassType, new HashMap<>())
					.getOrDefault(propertyClassType, new HashMap<>()).getOrDefault(unit, 0D);

			final double stdDeviation = this.stdDeviations.getOrDefault(parentClassType, new HashMap<>())
					.getOrDefault(propertyClassType, new HashMap<>()).getOrDefault(unit, 0D);

			for (int i = 4; i >= 1; i--) {

				boolean within = Math.abs(semantics.getMeanValue() - meanValue) <= i * stdDeviation;

				/**
				 * Add only the feature which is the farthest away from mean.
				 */
				if (!within) {
					featureVector.set(PREFIX + String.format(NOT_MEAN_STD_DEVIATION_TEMPLATE, className,
							parentClassName, i, stdDeviation, meanValue, semantics.getUnit().getName()), !within);
					// System.out.println(
					// String.format(NOT_MEAN_STD_DEVIATION_TEMPLATE,
					// className,parentClassName, i, stdDeviation, meanValue,
					// semantics.getUnit().getName())
					// + " -> " + !within);
					break;
				}

			}
			for (int i = 1; i <= 4; i++) {

				boolean within = Math.abs(semantics.getMeanValue() - meanValue) <= i * stdDeviation;

				/**
				 * Add only the feature which is the nearest to the mean.
				 */
				if (within) {
					featureVector.set(PREFIX + String.format(MEAN_STD_DEVIATION_TEMPLATE, className, parentClassName, i,
							stdDeviation, meanValue, semantics.getUnit().getName()), within);
					// System.out.println(
					// String.format(MEAN_STD_DEVIATION_TEMPLATE, className,
					// parentClassName, i, stdDeviation, meanValue,
					// semantics.getUnit().getName())
					// + "->" + within);
					break;
				}
			}

		}
	}

}
