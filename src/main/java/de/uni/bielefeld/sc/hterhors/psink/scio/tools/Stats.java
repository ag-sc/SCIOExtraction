package de.uni.bielefeld.sc.hterhors.psink.scio.tools;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.MultiFillerSlot;
import de.hterhors.semanticmr.crf.structure.annotations.SingleFillerSlot;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;

public class Stats {

//	public static void main(String[] args) {
//		List<Double> dataDist = new ArrayList<>();
//		List<Double> uniform = new ArrayList<>();
//
//		dataDist.add(0.4);
//		dataDist.add(0.25);
//		dataDist.add(0.15);
//		dataDist.add(0.2);
//
//		System.out.println(gini(dataDist));
//
//		int X = 4;
//
//		for (int i = 0; i < X; i++) {
//			uniform.add(1d / X);
//		}
//
//		System.out.println(dataDist);
//		System.out.println(uniform);
//
//		double l2 = 0;
//		for (int i = 0; i < X; i++) {
//			l2 += Math.pow(dataDist.get(i) - uniform.get(i), 2);
//		}
//		l2 = Math.sqrt(l2);
//		System.out.println(1 - l2);
//		System.exit(1);
//
//		double mean_d = 0;
//
//		int i = 1;
//		for (Double double1 : dataDist) {
//			mean_d += i * double1;
//			i++;
//		}
//		i = 1;
//
//		double var_d = 0;
//		for (Double double1 : dataDist) {
//			var_d += Math.pow(i - mean_d, 2) * double1;
//			i++;
//		}
//		System.out.println("mean_d = " + mean_d);
//		System.out.println("var_d = " + var_d);
//
//		double mean_u = 0;
//
//		i = 1;
//		for (Double double1 : uniform) {
//			mean_u += i * double1;
//			i++;
//		}
//		i = 1;
//
//		double var_u = 0;
//		for (Double double1 : uniform) {
//			var_u += Math.pow(i - mean_u, 2) * double1;
//			i++;
//		}
//
//		System.out.println("mean_u = " + mean_u);
//
//		System.out.println("var_u = " + var_u);
//
//		System.out.println(var_d / var_u);
//	}

	static double gini(List<Double> values) {
		double sumOfDifference = values.stream().flatMapToDouble(v1 -> values.stream().mapToDouble(v2 -> {
			System.out.print("|(" + v1 + "-" + v2 + ")| + ");
			return Math.abs(v1 - v2);
		})).sum();
		System.out.println(sumOfDifference);
		double mean = values.stream().mapToDouble(v -> v).average().getAsDouble();
		System.out.println(mean);
		return sumOfDifference / (2 * values.size() * values.size() * mean);
	}

	private final static DecimalFormat resultFormatter = new DecimalFormat("#.##");

	public static void computeNormedVar(List<Instance> instances, EntityType entityType) {

		Map<EntityType, Integer> count = new HashMap<>();
		double total = 0;
		for (Instance instance : instances) {

			for (AbstractAnnotation aa : instance.getGoldAnnotations().getAnnotations()) {

//				if (!aa.isInstanceOfEntityTemplate())
//					continue;

				count.put(aa.getEntityType(), count.getOrDefault(aa.getEntityType(), 0) + 1);
				total++;

//				if (slotType.isSingleValueSlot()) {
//
//					SingleFillerSlot sfs = aa.asInstanceOfEntityTemplate().getSingleFillerSlot(slotType);
//
//					if (!sfs.containsSlotFiller())
//						continue;
//
//					count.put(sfs.getSlotFiller().getEntityType(),
//							count.getOrDefault(sfs.getSlotFiller().getEntityType(), 0) + 1);
//					total++;
//				}
//
//				else {
//					MultiFillerSlot mfs = aa.asInstanceOfEntityTemplate().getMultiFillerSlot(slotType);
//
//					if (!mfs.containsSlotFiller())
//						continue;
//
//					for (AbstractAnnotation mfsa : mfs.getSlotFiller()) {
//
//						count.put(mfsa.getEntityType(), count.getOrDefault(mfsa.getEntityType(), 0) + 1);
//						total++;
//					}
//				}
			}

		}
//		System.out.println("max =" + total);
//		System.out.println(count.size());

		Set<EntityType> entityTypes = new HashSet<>(entityType.getTransitiveClosureSubEntityTypes());
		entityTypes.add(entityType);
		/**
		 * +1 cause of itself
		 */

		double cov = 100 * ((double) count.size()) / entityTypes.size();
//		System.out.println(cov + "%");

		List<Double> dataDist = new ArrayList<>();
//		List<Double> uniform = new ArrayList<>();

		Collections.sort(dataDist);

		double maxMass = 0;

		for (EntityType double1 : entityTypes) {

			double d = 0;
			dataDist.add(d = (count.getOrDefault(double1, 0) / total));
			if (d > maxMass)
				maxMass = d;

//			uniform.add(1d / (double) slotType.getSlotFillerEntityTypes().size());
		}
//		System.out.println(dataDist);
		double gini = gini(dataDist);
//		System.out.println("maxMass = "+maxMass);
//		System.out.println("gini = "+gini);

//		\emph{hasInjuryIntensity}$^E$ & 4 & 100\% & 32.0 & 0.5 & 0.41\

		System.out.println("\\textsc{" + entityType.name + "} & " + entityTypes.size() + " & "
				+ resultFormatter.format(cov) + "\\% & " + total + " & " + resultFormatter.format(maxMass) + " & "
				+ resultFormatter.format(gini) + "\\\\");

	}

	public static void computeNormedVar(List<Instance> instances, SlotType slotType) {

		Map<EntityType, Integer> count = new HashMap<>();
		double total = 0;
		for (Instance instance : instances) {

			for (AbstractAnnotation aa : instance.getGoldAnnotations().getAnnotations()) {

				if (!aa.isInstanceOfEntityTemplate())
					continue;

				if (!aa.asInstanceOfEntityTemplate().hasSlotOfType(slotType))
					continue;

				if (slotType.isSingleValueSlot()) {

					SingleFillerSlot sfs = aa.asInstanceOfEntityTemplate().getSingleFillerSlot(slotType);

					if (!sfs.containsSlotFiller())
						continue;

					count.put(sfs.getSlotFiller().getEntityType(),
							count.getOrDefault(sfs.getSlotFiller().getEntityType(), 0) + 1);
					total++;
				}

				else {
					MultiFillerSlot mfs = aa.asInstanceOfEntityTemplate().getMultiFillerSlot(slotType);

					if (!mfs.containsSlotFiller())
						continue;

					for (AbstractAnnotation mfsa : mfs.getSlotFiller()) {

						count.put(mfsa.getEntityType(), count.getOrDefault(mfsa.getEntityType(), 0) + 1);
						total++;
					}
				}
			}

		}
//		System.out.println("max =" + total);
//		System.out.println(count.size());
		double cov = 100 * ((double) count.size()) / ((double) slotType.getSlotFillerEntityTypes().size());
//		System.out.println(cov + "%");

		List<Double> dataDist = new ArrayList<>();
//		List<Double> uniform = new ArrayList<>();

		Collections.sort(dataDist);

		double maxMass = 0;

		for (EntityType double1 : slotType.getSlotFillerEntityTypes()) {

			double d = 0;
			dataDist.add(d = (count.getOrDefault(double1, 0) / total));
			if (d > maxMass)
				maxMass = d;

//			uniform.add(1d / (double) slotType.getSlotFillerEntityTypes().size());
		}
		double gini = gini(dataDist);
//		System.out.println(dataDist);
//		System.out.println("maxMass = "+maxMass);
//		System.out.println("gini = "+gini);

//		\emph{hasInjuryIntensity}$^E$ & 4 & 100\% & 32.0 & 0.5 & 0.41\

		System.out.println("\\emph{" + slotType.name + "}$^E$ & " + slotType.getSlotFillerEntityTypes().size() + " & "
				+ resultFormatter.format(cov) + "\\% & " + total + " & " + resultFormatter.format(maxMass) + " & "
				+ resultFormatter.format(gini) + "\\");

//		System.out.println(uniform);
//
//		double mean_d = 0;
//
//		int i = 0;
//		for (Double double1 : dataDist) {
//			mean_d += i * double1;
//			i++;
//		}
//		i = 0;
//
//		double var_d = 0;
//		for (Double double1 : dataDist) {
//			var_d += Math.pow(i - mean_d, 2) * double1;
//			i++;
//		}
//		System.out.println("mean_d = " + mean_d);
//		System.out.println("var_d = " + var_d);
//
//		double mean_u = 0;
//
//		i = 0;
//		for (Double double1 : uniform) {
//			mean_u += i * double1;
//			i++;
//		}
//		i = 0;
//
//		double var_u = 0;
//		for (Double double1 : uniform) {
//			var_u += Math.pow(i - mean_u, 2) * double1;
//			i++;
//		}
//
//		System.out.println("mean_u = " + mean_u);
//
//		System.out.println("var_u = " + var_u);
//
//		System.out.println(var_d / var_u);
//
//		double l2 = 0;
//		for (int a = 0; a < dataDist.size(); a++) {
//			l2 += Math.pow(dataDist.get(a) - uniform.get(a), 2);
//		}
//		l2 = Math.sqrt(l2);
//		System.out.println(1 - l2);
	}

	public static void computePropsVar(List<Instance> instances, Set<SlotType> slotTypes) {

		Map<SlotType, Integer> count = new HashMap<>();

		double total = 0;
		for (Instance instance : instances) {

			for (SlotType slotType : slotTypes) {

				for (AbstractAnnotation aa : instance.getGoldAnnotations().getAnnotations()) {

					if (!aa.isInstanceOfEntityTemplate())
						continue;

					if (!aa.asInstanceOfEntityTemplate().hasSlotOfType(slotType))
						continue;

					if (slotType.isSingleValueSlot()) {

						SingleFillerSlot sfs = aa.asInstanceOfEntityTemplate().getSingleFillerSlot(slotType);

						if (!sfs.containsSlotFiller())
							continue;

						count.put(sfs.slotType, count.getOrDefault(sfs.slotType, 0) + 1);
						total++;
					}

					else {
						MultiFillerSlot mfs = aa.asInstanceOfEntityTemplate().getMultiFillerSlot(slotType);

						if (!mfs.containsSlotFiller())
							continue;

						for (AbstractAnnotation mfsa : mfs.getSlotFiller()) {

							count.put(slotType, count.getOrDefault(slotType, 0) + 1);
							total++;
						}
					}
				}
			}

		}
		for (Entry<SlotType, Integer> slotType : count.entrySet()) {
			System.out.println(slotType.getKey().name + "\t" + slotType.getValue());
		}

	}

	public static double countVariables(int m, List<Instance> instances) {

		double total = 0;
		double perDocument = 0;
		double perInstance = 0;
		int countIndividual = 0;
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;

		int minIndDoc = Integer.MAX_VALUE;
		int maxIndDoc = Integer.MIN_VALUE;

		int minDoc = Integer.MAX_VALUE;
		int maxDoc = Integer.MIN_VALUE;

		Set<AbstractAnnotation> annotations = new HashSet<>();

		for (Instance instance : instances) {
			int docCount = 0;
			System.out.println(instance.getName());
			for (AbstractAnnotation aa : instance.getGoldAnnotations().getAnnotations()) {
				countIndividual++;
				System.out.println(aa.toPrettyString());
				int n = m + rec(annotations, aa.asInstanceOfEntityTemplate());
				docCount += n;
				System.out.println(n);
				min = Math.min(min, n);
				max = Math.max(max, n);
				perInstance += n;
				perDocument += n;
			}
			System.out.println("docCount = " + docCount);
			minDoc = Math.min(minDoc, docCount);
			maxDoc = Math.max(maxDoc, docCount);
			minIndDoc = Math.min(minIndDoc, instance.getGoldAnnotations().getAnnotations().size());
			maxIndDoc = Math.max(maxIndDoc, instance.getGoldAnnotations().getAnnotations().size());
		}
//		System.out.println(annotations);

		System.out.println(minDoc + " & " + maxDoc);
		System.out.println("minDoc" + minDoc);
		System.out.println("maxDoc" + maxDoc);
		System.out.println("minIndDoc" + minIndDoc);
		System.out.println("maxIndDoc" + maxIndDoc);
		System.out.println(minIndDoc + " & " + maxIndDoc);
		System.out.println("min" + min);
		System.out.println("max" + max);
		System.out.println("Total number = " + perDocument);
		System.out.println("Count Individual= " + countIndividual);
		System.out.println("Count documents  =" + instances.size());
		perInstance /= countIndividual;
		perDocument /= instances.size();
//		total += perDocument;
		System.out.println("perIndividual= " + perInstance);
		System.out.println("perDoc = " + perDocument);
//		total /=instances.size();
		System.out.println(min+ " & "+ max + " & "+ new DecimalFormat("0.00").format(perInstance) +" & "+minDoc + " & " + maxDoc + " &" + new DecimalFormat("0.00").format(perDocument) + " &"
				+ minIndDoc + " & " + maxIndDoc);
		return total;
	}

	public static void countVariables(int m, List<Instance> instances, Set<SlotType> slotTypes) {

		double perDocument = 0;
		double perInstance = 0;
		int countIndividual = 0;
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;

		int minIndDoc = Integer.MAX_VALUE;
		int maxIndDoc = Integer.MIN_VALUE;

		int minDoc = Integer.MAX_VALUE;
		int maxDoc = Integer.MIN_VALUE;
		SCIOSlotTypes.hasGroupName.exclude();
		Map<SlotType, Integer> count = new HashMap<>();
		Map<SlotType, Set<Instance>> countDocs = new HashMap<>();
//		Map<EntityType, Set<Instance>> countDocsRoot = new HashMap<>();

		for (SlotType slotType : slotTypes) {
			countDocs.put(slotType, new HashSet<>());
			for (Instance instance : instances) {


				for (AbstractAnnotation aa : instance.getGoldAnnotations().getAnnotations()) {

//					countDocsRoot.get(aa.getEntityType()).add(instance);

					if (!aa.isInstanceOfEntityTemplate())
						continue;

					if (!aa.asInstanceOfEntityTemplate().hasSlotOfType(slotType))
						continue;

					if (slotType.isSingleValueSlot()) {

						SingleFillerSlot sfs = aa.asInstanceOfEntityTemplate().getSingleFillerSlot(slotType);

						if (!sfs.containsSlotFiller())
							continue;

						countDocs.get(slotType).add(instance);
					}

					else {
						MultiFillerSlot mfs = aa.asInstanceOfEntityTemplate().getMultiFillerSlot(slotType);

						if (!mfs.containsSlotFiller())
							continue;
						countDocs.get(slotType).add(instance);

					}
				}
			}

		}

		for (Instance instance : instances) {

			for (AbstractAnnotation aa : instance.getGoldAnnotations().getAnnotations()) {

				for (SlotType slotType : slotTypes) {

					if (!aa.isInstanceOfEntityTemplate())
						continue;

					if (!aa.asInstanceOfEntityTemplate().hasSlotOfType(slotType))
						continue;

					if (slotType.isSingleValueSlot()) {

						SingleFillerSlot sfs = aa.asInstanceOfEntityTemplate().getSingleFillerSlot(slotType);

						if (!sfs.containsSlotFiller())
							continue;

						count.put(sfs.slotType, count.getOrDefault(sfs.slotType, 0) + 1);
					}

					else {
						MultiFillerSlot mfs = aa.asInstanceOfEntityTemplate().getMultiFillerSlot(slotType);

						if (!mfs.containsSlotFiller())
							continue;

						for (AbstractAnnotation mfsa : mfs.getSlotFiller()) {

							count.put(slotType, count.getOrDefault(slotType, 0) + 1);
						}
					}
				}
			}

		}
		Set<AbstractAnnotation> annotations = new HashSet<>();

		for (Instance instance : instances) {
			int docCount = 0;
			System.out.println(instance.getName());
			for (AbstractAnnotation aa : instance.getGoldAnnotations().getAnnotations()) {
				countIndividual++;
				System.out.println(aa.toPrettyString());
				int n = m + rec(annotations, aa.asInstanceOfEntityTemplate());
				docCount += n;
				System.out.println(n);
				min = Math.min(min, n);
				max = Math.max(max, n);
				perInstance += n;
				perDocument += n;
			}
			System.out.println("docCount = " + docCount);
			minDoc = Math.min(minDoc, docCount);
			maxDoc = Math.max(maxDoc, docCount);
			minIndDoc = Math.min(minIndDoc, instance.getGoldAnnotations().getAnnotations().size());
			maxIndDoc = Math.max(maxIndDoc, instance.getGoldAnnotations().getAnnotations().size());
		}
//		System.out.println(annotations);

		System.out.println(minDoc + " & " + maxDoc);
		System.out.println("minDoc" + minDoc);
		System.out.println("maxDoc" + maxDoc);
		System.out.println("minIndDoc" + minIndDoc);
		System.out.println("maxIndDoc" + maxIndDoc);
		System.out.println(minIndDoc + " & " + maxIndDoc);
		System.out.println("min" + min);
		System.out.println("max" + max);
		System.out.println("Total number = " + perDocument);
		System.out.println("Count Individual= " + countIndividual);
		System.out.println("Count documents  =" + instances.size());
		perInstance /= countIndividual;
		perDocument /= instances.size();
//		total += perDocument;
		System.out.println("perIndividual= " + perInstance);
		System.out.println("perDoc = " + perDocument);
//		total /=instances.size();
		System.out.println(minDoc + " & " + maxDoc + " &" + new DecimalFormat("0.00").format(perDocument) + " &"
				+ minIndDoc + " & " + maxIndDoc);

		System.out.println(instances.size() + " & " + countIndividual + " & "
				+ new DecimalFormat("0.00").format((double) countIndividual / (double) instances.size()));

		for (SlotType slotType : count.keySet()) {
			System.out.println(slotType.name);
			System.out.println(countDocs.get(slotType).size() + " & " + count.get(slotType) + " & "
					+ new DecimalFormat("0.00").format((double) count.get(slotType) / (double) instances.size()));
		}

	}

	private static int rec(Set<AbstractAnnotation> annotations, EntityTemplate aa) {
		int total = 0;
		for (SlotType slotType : aa.getEntityType().getSlots()) {

			if (slotType.isExcluded())
				continue;

			if (!aa.isInstanceOfEntityTemplate())
				continue;

			if (!aa.asInstanceOfEntityTemplate().hasSlotOfType(slotType))
				continue;

			if (slotType.isSingleValueSlot()) {

				SingleFillerSlot sfs = aa.asInstanceOfEntityTemplate().getSingleFillerSlot(slotType);

				if (!sfs.containsSlotFiller())
					continue;

				total++;
				if (sfs.getSlotFiller().isInstanceOfEntityTemplate())
					total += rec(annotations, sfs.getSlotFiller().asInstanceOfEntityTemplate());
				else {
					annotations.add(sfs.getSlotFiller());
				}

			}

			else {
				MultiFillerSlot mfs = aa.asInstanceOfEntityTemplate().getMultiFillerSlot(slotType);

				if (!mfs.containsSlotFiller())
					continue;

				for (AbstractAnnotation mfsa : mfs.getSlotFiller()) {

					total++;
					if (mfsa.isInstanceOfEntityTemplate())
						total += rec(annotations, mfsa.asInstanceOfEntityTemplate());
					else
						annotations.add(mfsa);
				}
			}
		}
		return total;
	}

}
