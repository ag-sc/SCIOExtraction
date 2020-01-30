package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.clustering.kmeans;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.templates.helper.LevenShteinSimilarities;

public class WordBasedKMeans {

	private static final Random random = new Random(1000L);

	final static private int maxIterations = 1000;

	public static List<List<DocumentLinkedAnnotation>> cluster(List<DocumentLinkedAnnotation> datapoints, int k) {

		List<Record> vecs = toDataPoints(datapoints);

		Map<Centroid, List<Record>> clusterRecords = kMeans(vecs, k, maxIterations);
		List<List<DocumentLinkedAnnotation>> clusters = new ArrayList<>();

//		for (Entry<Centroid, List<Record>> list : clusterRecords.entrySet()) {
//List<DocumentLinkedAnnotation> anns = new ArrayList<>();
//anns.add(list.getKey().)
//			clusters.add(l);
//		}
//		
		for (List<Record> records : clusterRecords.values()) {
			clusters.add(records.stream().map(r -> r.annotation).collect(Collectors.toList()));
		}

		for (int i = clusters.size(); i < k; i++) {
			clusters.add(Collections.emptyList());
		}

		return clusters;

	}

	static private List<Record> toDataPoints(List<DocumentLinkedAnnotation> datapoints) {

		List<Record> list = datapoints.stream().map(s -> new Record(s)).collect(Collectors.toList());

		return list;
	}

	static class Centroid {

		public final String word;
		public final int index;

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + index;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Centroid other = (Centroid) obj;
			if (index != other.index)
				return false;
			return true;
		}

		public Centroid(String word, int index) {
			this.index = index;
			this.word = word;
		}

		@Override
		public String toString() {
			return "Centroid [word=" + word + ", index=" + index + "]";
		}

	}

	static class Record {
		public final String word;
		public final DocumentLinkedAnnotation annotation;

		public Record(DocumentLinkedAnnotation annotation) {
			this.annotation = annotation;
			this.word = annotation.getSurfaceForm();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((annotation == null) ? 0 : annotation.hashCode());
			result = prime * result + ((word == null) ? 0 : word.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Record other = (Record) obj;
			if (annotation == null) {
				if (other.annotation != null)
					return false;
			} else if (!annotation.equals(other.annotation))
				return false;
			if (word == null) {
				if (other.word != null)
					return false;
			} else if (!word.equals(other.word))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "Record [word=" + word + "]";
		}

	}

	private static Map<Centroid, List<Record>> kMeans(List<Record> records, int k, int maxIterations) {
//		List<Centroid> centroids = randomCentroids(records, k);
		List<Centroid> centroids = plusplusCentroids(records, k);

		Map<Centroid, List<Record>> lastState = new HashMap<>();

		// iterate for a pre-defined number of times
		for (int i = 0; i < maxIterations; i++) {

			final Map<Centroid, List<Record>> clusters = new HashMap<>();

			boolean isLastIteration = i == maxIterations - 1;

			// in each iteration we should find the nearest centroid for each record

//			final List<Centroid> cent = new ArrayList<>(centroids);
//			records.parallelStream().forEach(record -> {
//				Centroid centroid = nearestCentroid(record, cent);
//				assignToCluster(clusters, record, centroid);
//			});

			for (Record record : records) {
				Centroid centroid = nearestCentroid(record, centroids);
				assignToCluster(clusters, record, centroid);
			}

			// if the assignments do not change, then the algorithm terminates
			boolean shouldTerminate = isLastIteration || clusters.equals(lastState);

			lastState = clusters;
			if (shouldTerminate) {
				break;
			}

			// at the end of each iteration we should relocate the centroids
			centroids = relocateCentroids(clusters);
		}

		return lastState;
	}

	static class DistToPoint {

		public final int recordIndex;
		public final String word;
		public final double distance;

		public DistToPoint(String word, int recordIndex, double distance) {
			this.word = word;
			this.recordIndex = recordIndex;
			this.distance = distance;
		}

		@Override
		public String toString() {
			return "DistToPoint [recordIndex=" + recordIndex + ", word=" + word + ", distance=" + distance + "]";
		}

	}

	private static List<Centroid> plusplusCentroids(List<Record> records, int k) {
		List<Centroid> centroids = new ArrayList<>();

		Map<Integer, Record> recMap = new HashMap<>();

		Integer index = 0;
		for (Record rec : records) {
			recMap.put(index, rec);
			index++;
		}

		Set<Integer> chosen = new HashSet<>();
		Integer rand = random.nextInt(records.size());
		chosen.add(rand);
		centroids.add(new Centroid(records.get(rand).word, 0));

		for (int c = 1; c < k; c++) {
			List<DistToPoint> distances = new ArrayList<>();
			for (int i = 0; i < records.size(); i++) {

				if (chosen.contains(i))
					continue;

				double minDist = Double.MAX_VALUE;

				for (Centroid centroid : centroids) {
					double dist = levenshteinDistance(centroid.word, records.get(i).word);
					if (dist < minDist) {
						minDist = dist;
					}
				}
				distances.add(new DistToPoint(records.get(i).word, i, Math.pow(minDist, 2)));

			}

//			records.stream().filter(r -> !chosen.contains(r.word)).forEach(r -> {
//
//				double minDist = Double.MAX_VALUE;
//
//				for (Centroid centroid : centroids) {
//					double dist = levenshteinDistance(centroid.word, r.word);
//					if (dist < minDist) {
//						minDist = dist;
//					}
//				}
//				distances.add(new DistToPoint(r.word, Math.pow(minDist, 2)));
//			});
//			List<DistToPoint> distances = new ArrayList<>();
//			for (int i = 0; i < records.size(); i++) {
//
//				double minDist = Double.MAX_VALUE;
//
//				if (chosen.contains(records.get(i).word))
//					continue;
//
//				for (Centroid centroid : centroids) {
//
//					double dist = calcEuklidDistance(centroid.coordinates, records.get(i).features);
//					if (dist < minDist) {
//						minDist = dist;
//					}
//				}
//				distances.add(new DistToPoint(records.get(i).word, Math.pow(minDist, 2)));
//			}
			Collections.sort(distances, new Comparator<DistToPoint>() {

				@Override
				public int compare(DistToPoint o1, DistToPoint o2) {
					return o1.word.compareTo(o2.word);
				}
			});
			DistToPoint d;
			if (!distances.isEmpty()) {
				d = drawFromDistribution(distances);
			} else {
				int r = new Random().nextInt(records.size());
				d = new DistToPoint(records.get(r).word, r, 0);
			}

			chosen.add(d.recordIndex);
			centroids.add(new Centroid(recMap.get(d.recordIndex).word, c));
		}

		return centroids;
	}

	private static DistToPoint drawFromDistribution(List<DistToPoint> nextStates) {

		// compute total sum of scores
		double totalSum = 0;
		for (DistToPoint s : nextStates) {
			totalSum += s.distance;
		}
		double randomIndex = random.nextDouble() * totalSum;
		double sum = 0;
		int i = 0;
		while (sum < randomIndex) {
			sum += nextStates.get(i++).distance;
		}
		return nextStates.get(Math.max(0, i - 1));

	}

//	private static List<Centroid> randomCentroids(List<Record> records, int k) {
//		List<Centroid> centroids = new ArrayList<>();
//		double max = Double.MIN_VALUE;
//		double min = Double.MAX_VALUE;
//
//		System.out.println("Calculate min and max... ");
//		for (Record record : records) {
//			for (double v : record.features) {
////				max += Math.abs(v);
////				min -= Math.abs(v);
//				max = Math.max(v, max);
//				min = Math.min(v, min);
//			}
//		}
////		System.out.println(records.size() * NUM_OF_DIM);
////		System.out.println("max = " + max);
////		System.out.println("min = " + min);
////		max /= records.size() * NUM_OF_DIM;
////		min /= records.size() * NUM_OF_DIM;
//
////		max = 1;
////		min = -1;
//		System.out.println("max = " + max);
//		System.out.println("min = " + min);
//
//		for (int i = 0; i < k; i++) {
//			double[] coordinates = new double[NUM_OF_DIM];
//			for (int j = 0; j < coordinates.length; j++) {
//				coordinates[j] = random.nextDouble() * (max - min) + min;
//			}
//			centroids.add(new Centroid(word, i));
//		}
//
//		return centroids;
//	}

	private static List<Centroid> relocateCentroids(Map<Centroid, List<Record>> clusters) {
		List<Centroid> centroids = new ArrayList<>();
		for (Entry<Centroid, List<Record>> cs : clusters.entrySet()) {
			centroids.add(average(cs.getKey(), cs.getValue()));
		}

		return centroids;
	}

	private static Centroid average(Centroid centroid, List<Record> records) {
		if (records == null || records.isEmpty()) {
			return centroid;
		}

		int avgWordLength = (int) ((double) records.stream().map(r -> r.word.length()).reduce(0, Integer::sum)
				/ records.size());

		StringBuffer average = new StringBuffer();

		for (int i = 0; i < avgWordLength; i++) {

			Map<Character, Integer> count = new HashMap<>();

			for (Record record : records) {

				if (record.word.length() > i)
					count.put(record.word.charAt(i), count.getOrDefault(record.word.charAt(i), 0) + 1);
				else
					count.put('#', count.getOrDefault('#', 0) + 1);

			}

			int max = 0;
			char maxChar = '#';
			for (Entry<Character, Integer> record : count.entrySet()) {

				if (max < record.getValue()) {
					max = record.getValue();
					maxChar = record.getKey();
				}
			}
			average.append(maxChar);
		}

		return new Centroid(average.toString(), centroid.index);

	}

	private static void assignToCluster(Map<Centroid, List<Record>> clusters, Record record, Centroid centroid) {

		List<Record> records;

		if ((records = clusters.get(centroid)) == null)
			clusters.put(centroid, records = new ArrayList<>());

		records.add(record);

	}

	private static Centroid nearestCentroid(Record record, List<Centroid> centroids) {
		double minimumDistance = Double.MAX_VALUE;
		Centroid nearest = null;

		for (Centroid centroid : centroids) {
			double currentDistance = levenshteinDistance(record.word, centroid.word);

			if (currentDistance < minimumDistance) {
				minimumDistance = currentDistance;
				nearest = centroid;
			}
		}

		return nearest;
	}

	private static double levenshteinDistance(String word1, String word2) {
		return 1 - LevenShteinSimilarities.levenshteinSimilarity(word1, word2, 100);

	}

}
