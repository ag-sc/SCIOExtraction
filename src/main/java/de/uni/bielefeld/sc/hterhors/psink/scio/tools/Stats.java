package de.uni.bielefeld.sc.hterhors.psink.scio.tools;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.MultiFillerSlot;
import de.hterhors.semanticmr.crf.structure.annotations.SingleFillerSlot;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.variables.Instance;

public class Stats {

	public static void main(String[] args) {
		List<Double> dataDist = new ArrayList<>();
		List<Double> uniform = new ArrayList<>();

		dataDist.add(0.4);
		dataDist.add(0.25);
		dataDist.add(0.15);
		dataDist.add(0.2);

		System.out.println(gini(dataDist));

		int X = 4;

		for (int i = 0; i < X; i++) {
			uniform.add(1d / X);
		}

		System.out.println(dataDist);
		System.out.println(uniform);

		double l2 = 0;
		for (int i = 0; i < X; i++) {
			l2 += Math.pow(dataDist.get(i) - uniform.get(i), 2);
		}
		l2 = Math.sqrt(l2);
		System.out.println(1 - l2);
		System.exit(1);

		double mean_d = 0;

		int i = 1;
		for (Double double1 : dataDist) {
			mean_d += i * double1;
			i++;
		}
		i = 1;

		double var_d = 0;
		for (Double double1 : dataDist) {
			var_d += Math.pow(i - mean_d, 2) * double1;
			i++;
		}
		System.out.println("mean_d = " + mean_d);
		System.out.println("var_d = " + var_d);

		double mean_u = 0;

		i = 1;
		for (Double double1 : uniform) {
			mean_u += i * double1;
			i++;
		}
		i = 1;

		double var_u = 0;
		for (Double double1 : uniform) {
			var_u += Math.pow(i - mean_u, 2) * double1;
			i++;
		}

		System.out.println("mean_u = " + mean_u);

		System.out.println("var_u = " + var_u);

		System.out.println(var_d / var_u);
	}

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

		
		Set<EntityType> entityTypes = new HashSet<>( entityType.getTransitiveClosureSubEntityTypes());
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

		
		
		for (EntityType double1 :entityTypes) {

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

		System.out
				.println("\\textsc{" + entityType.name + "} & " + entityTypes.size()
						+ " & " + resultFormatter.format(cov) + "\\% & " + total + " & "
						+ resultFormatter.format(maxMass) + " & " + resultFormatter.format(gini) + "\\\\");

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

}
