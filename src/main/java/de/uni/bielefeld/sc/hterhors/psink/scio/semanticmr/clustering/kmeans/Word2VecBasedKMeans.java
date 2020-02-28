package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.clustering.kmeans;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import de.hterhors.semanticmr.crf.structure.annotations.LiteralAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.container.TextualContent;
import de.hterhors.semanticmr.crf.templates.helper.LevenShteinSimilarities;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.clustering.groupnames.helper.GroupNamePair;

public class Word2VecBasedKMeans<E extends LiteralAnnotation> {
	public static void main(String[] args) {

		SystemScope.Builder.getScopeHandler()
				.addScopeSpecification(DataStructureLoader.loadSlotFillingDataStructureReader("ExperimentalGroup"))
				.build();

		/*
		 * First cluster
		 */
		String A = "co-grafted rats";
		String B = "cograft";
		String C = "co-graft";
		String D = "cograft animals";
		String E = "co-grafted animals";
		/*
		 * Second cluster
		 */
		String F = "BMSC";

		/*
		 * Third cluster
		 * 
		 */
		String G = "control group";
		String H = "control";

		/*
		 * Fourth cluster
		 */
		String I = "OEC";

		List<GroupNamePair> gnd = new ArrayList<>();
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(A)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(B)), true, 0.8D));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(A)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(C)), true, 1D));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(A)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(D)), true, 1D));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(A)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(E)), true, 1D));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(A)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(F)), true, 0D));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(A)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(G)), true, 0D));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(A)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(H)), true, 0D));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(A)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(I)), true, 0D));
//		
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(B)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(C)), true, 1D));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(B)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(D)), true, 1D));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(B)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(E)), true, 1D));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(B)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(F)), true, 0D));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(B)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(G)), true, 0D));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(B)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(H)), true, 0D));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(B)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(I)), true, 0D));
//		
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(C)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(D)), true, 1D));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(C)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(E)), true, 1D));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(C)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(F)), true, 0D));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(C)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(G)), true, 0D));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(C)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(H)), true, 0D));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(C)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(I)), true, 0D));
//		
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(D)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(E)), true, 1D));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(D)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(F)), true, 0D));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(D)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(G)), true, 0D));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(D)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(H)), true, 0D));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(D)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(I)), true, 0D));
//		
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(E)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(F)), true, 0D));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(E)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(G)), true, 0D));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(E)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(H)), true, 0D));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(E)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(I)), true, 0D));
//		
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(F)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(G)), true, 0D));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(F)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(H)), true, 0D));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(F)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(I)), true, 0D));
//		
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(G)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(H)), true, 1D));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(G)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(I)), true, 0D));
//		
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(H)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(I)), true, 0D));
//		

		List<LiteralAnnotation> datapoints = new ArrayList<>(gnd.stream()
				.flatMap(a -> Arrays.asList(a.groupName1, a.groupName2).stream()).collect(Collectors.toSet()));

		Word2VecBasedKMeans<LiteralAnnotation> c = new Word2VecBasedKMeans<>(
				datapoints.stream().map(a -> a.getSurfaceForm()).collect(Collectors.toSet()));
//

		for (GroupNamePair gnp : gnd) {
			System.out.println(
					gnp.groupName1.getSurfaceForm() + " " + gnp.groupName2.getSurfaceForm() + " -> " + gnp.probability);
			System.out
					.println(Word2VecBasedKMeans.squareDistance(c.getAdditiveEmbedding(gnp.groupName1.getSurfaceForm()),
							c.getAdditiveEmbedding(gnp.groupName2.getSurfaceForm())));
		}

		List<List<LiteralAnnotation>> clusters = c.clusterRSS(datapoints, 1, 10);
		System.out.println("Number of clusters = " + clusters.size());

		for (List<LiteralAnnotation> cluster : clusters) {
			for (LiteralAnnotation groupName : cluster) {
				System.out.println(groupName.getSurfaceForm());
			}
			System.out.println("-------------------");
		}

	}

	public Word2VecBasedKMeans(Set<String> words) {
		Set<String> _words = words.stream().flatMap(a -> Arrays.stream(a.split(" "))).collect(Collectors.toSet());

		readVectors(_words);
		System.out.println(wordEmbeddings.size());
	}

	private Map<String, Double[]> wordEmbeddings = new HashMap<>();

	private final Random random = new Random(1000L);

	final private int maxIterations = 1000;

	public double lambda;

	public List<List<E>> clusterRSS(List<E> datapoints, int min, int max) {

//		double lambda = (double) min / (double) max;

		Map<Centroid, List<Record<E>>> clusterRecords = new HashMap<>();

		List<Record<E>> vecs = toDataPoints(datapoints);

		double smallestRSS = Double.MAX_VALUE;

		for (int clusterSize = min; clusterSize <= max; clusterSize++) {
			Map<Centroid, List<Record<E>>> cR = kMeans(vecs, clusterSize, maxIterations);

			double RSS = RSS(cR) + (lambda * clusterSize);

			if (RSS < smallestRSS) {
				smallestRSS = RSS;
				clusterRecords = cR;
			}

			List<List<E>> clusters = new ArrayList<>();

			for (List<Record<E>> records : cR.values()) {
				clusters.add(records.stream().map(r -> r.annotation).collect(Collectors.toList()));
			}
//			System.out.println("Number of clusters = " + clusters.size());
//			System.out.println("RSS = " + RSS);

//			for (List<E> cluster : clusters) {
//				for (LiteralAnnotation groupName : cluster) {
//					System.out.println(groupName.getSurfaceForm());
//				}
//				System.out.println("-------------------");
//			}
//			System.out.println();
//			System.out.println();
//			System.out.println();

		}

		List<List<E>> clusters = new ArrayList<>();

		for (List<Record<E>> records : clusterRecords.values()) {
			clusters.add(records.stream().map(r -> r.annotation).collect(Collectors.toList()));
		}

		return clusters;

	}

	private double RSS(Map<Centroid, List<Record<E>>> clusterRecords) {

		double rss = 0;

		for (Entry<Centroid, List<Word2VecBasedKMeans<E>.Record<E>>> cluster : clusterRecords.entrySet()) {
			Centroid c = cluster.getKey();
			for (Word2VecBasedKMeans<E>.Record<E> r : cluster.getValue()) {
				rss += squareDistance(c.word, r.word);
			}
		}
		return rss;
	}

	public List<List<E>> cluster(List<E> datapoints, int k) {
		if (datapoints.isEmpty() || k >= datapoints.size()) {
			List<List<E>> unsufficient = new ArrayList<>();
			for (int i = 0; i < k; i++) {
				if (i < datapoints.size())
					unsufficient.add(Arrays.asList(datapoints.get(i)));
				else
					unsufficient.add(Collections.emptyList());
			}
			return unsufficient;

		}

//		Set<String> words = datapoints.stream()
//				.flatMap(a -> Arrays.stream(a.asInstanceOfLiteralAnnotation().getSurfaceForm().split(" ")))
//				.collect(Collectors.toSet());

//		Set<String> words =
//				datapoints.stream().flatMap(
//						a -> a.asInstanceOfDocumentLinkedAnnotation().relatedTokens.stream().map(t -> t.getText()))
//				.collect(Collectors.toSet());

//		readVectors(words);

		List<Record<E>> vecs = toDataPoints(datapoints);

		Map<Centroid, List<Record<E>>> clusterRecords = kMeans(vecs, k, maxIterations);
		List<List<E>> clusters = new ArrayList<>();

		for (List<Record<E>> records : clusterRecords.values()) {
			clusters.add(records.stream().map(r -> r.annotation).collect(Collectors.toList()));
		}

		for (int i = clusters.size(); i < k; i++) {
			clusters.add(Collections.emptyList());
		}

		return clusters;

	}

	private void readVectors(Set<String> words) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File("wordvector/w2v.csv")));
			String line = null;
			int lineC = 0;
			System.out.println("Read embeddings...");
			while ((line = br.readLine()) != null) {

				lineC++;
				if (lineC % 100000 == 0)
					System.out.println(lineC);

				final String data[] = line.split(" ");

				String word = data[0];

				if (!words.contains(word))
					continue;

				Double[] vec = new Double[200];

				for (int i = 1; i < data.length; i++) {
					vec[i - 1] = Double.parseDouble(data[i].trim());
				}

				wordEmbeddings.put(word, vec);
			}

			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private List<Record<E>> toDataPoints(List<E> datapoints) {

		List<Record<E>> list = datapoints.stream().map(s -> new Record<E>(s)).collect(Collectors.toList());

		return list;
	}

	static class Centroid {

		public final Double[] word;

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(word);
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
			if (!Arrays.equals(word, other.word))
				return false;
			return true;
		}

		public Centroid() {
			this.word = new Double[] { Math.random() };
		}

		public Centroid(Double[] word) {
			this.word = word;
		}

	}

	public Double[] getAdditiveEmbedding(String surfaceForm) {
		Double[] vec = new Double[200];
		for (int i = 0; i < vec.length; i++) {
			vec[i] = 0D;
		}

		String[] words = surfaceForm.split(" ");
		int count = 0;
		for (String d : words) {
			Double[] embedding = getBestEmbedding(d);
			if (embedding == null)
				continue;
			count++;
			for (int i = 0; i < embedding.length; i++) {

				vec[i] += embedding[i];
			}

		}
		if (count > 0)
			for (int i = 0; i < words.length; i++) {
				vec[i] /= count;
			}
		else {
			for (int i = 0; i < vec.length; i++) {
				/**
				 * TODO:
				 */
				// In case vector is 0 which means that the datapoint is has no embedding at all
				// than make random vector to put i in any cluster.
				vec[i] = Math.random();
			}
		}
		return vec;
	}

	class Record<E extends LiteralAnnotation> {
		public final Double[] word;
		public final E annotation;

		public Record(E annotation) {
			this.annotation = annotation;
			this.word = getAdditiveEmbedding(annotation.getSurfaceForm());

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
			Record<E> other = (Record<E>) obj;
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
			return "Record<E> [word=" + word + "]";
		}

	}

	private Double[] getBestEmbedding(String d) {

		if (wordEmbeddings.containsKey(d))
			return wordEmbeddings.get(d);

		double mostSim = 0.7;
		String bestMatch = null;
		for (String embedding : wordEmbeddings.keySet()) {

			double sim = levenshteinSimilarty(embedding, d);

			if (mostSim <= sim) {
				mostSim = sim;
				bestMatch = embedding;
			}

		}
		if (bestMatch == null)
			return null;

		return wordEmbeddings.get(bestMatch);

	}

	private Map<Centroid, List<Record<E>>> kMeans(List<Record<E>> records, int k, int maxIterations) {

		Map<Centroid, List<Record<E>>> lastState = new HashMap<>();

		if (records.isEmpty() || k >= records.size()) {
			for (int clusterC = 0; clusterC < k; clusterC++) {
				if (clusterC < records.size())
					lastState.put(new Centroid(), Arrays.asList(records.get(clusterC)));
				else
					lastState.put(new Centroid(), Collections.emptyList());
			}
			return lastState;

		}

		// List<Centroid> centroids = randomCentroids(records, k);
		List<Centroid> centroids = plusplusCentroids(records, k);

		// iterate for a pre-defined number of times
		for (int i = 0; i < maxIterations; i++) {

			final Map<Centroid, List<Record<E>>> clusters = new HashMap<>();

			boolean isLastIteration = i == maxIterations - 1;

			// in each iteration we should find the nearest centroid for each record

//			final List<Centroid> cent = new ArrayList<>(centroids);
//			records.parallelStream().forEach(record -> {
//				Centroid centroid = nearestCentroid(record, cent);
//				assignToCluster(clusters, record, centroid);
//			});

			for (Record<E> record : records) {
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

	class DistToPoint {

		public final int recordIndex;
		public final Double[] word;
		public final double distance;

		public DistToPoint(Double[] word, int recordIndex, double distance) {
			this.word = word;
			this.recordIndex = recordIndex;
			this.distance = distance;
		}

		@Override
		public String toString() {
			return "DistToPoint [recordIndex=" + recordIndex + ", word=" + word + ", distance=" + distance + "]";
		}

	}

	private List<Centroid> plusplusCentroids(List<Record<E>> records, int k) {
		List<Centroid> centroids = new ArrayList<>();

		Map<Integer, Record<E>> recMap = new HashMap<>();

		Integer index = 0;
		for (Record<E> rec : records) {
			recMap.put(index, rec);
			index++;
		}

		Set<Integer> chosen = new HashSet<>();
		Integer rand = random.nextInt(records.size());
		chosen.add(rand);
		centroids.add(new Centroid(records.get(rand).word));

		for (int c = 1; c < k; c++) {
			List<DistToPoint> distances = new ArrayList<>();
			for (int i = 0; i < records.size(); i++) {

				if (chosen.contains(i))
					continue;

				double minDist = Double.MAX_VALUE;

				for (Centroid centroid : centroids) {
					double dist = cosineSimilarityDistance(centroid.word, records.get(i).word);
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
//			Collections.sort(distances, new Comparator<DistToPoint>() {
//
//				@Override
//				public int compare(DistToPoint o1, DistToPoint o2) {
//					return o1.word.compareTo(o2.word);
//				}
//			});
			DistToPoint d;
			if (!distances.isEmpty()) {
				d = drawFromDistribution(distances);
			} else {
				int r = new Random().nextInt(records.size());
				d = new DistToPoint(records.get(r).word, r, 0);
			}

			chosen.add(d.recordIndex);
			centroids.add(new Centroid(recMap.get(d.recordIndex).word));
		}

		return centroids;
	}

	private DistToPoint drawFromDistribution(List<DistToPoint> nextStates) {

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

//	private  List<Centroid> randomCentroids(List<Record<E>> records, int k) {
//		List<Centroid> centroids = new ArrayList<>();
//		double max = Double.MIN_VALUE;
//		double min = Double.MAX_VALUE;
//
//		System.out.println("Calculate min and max... ");
//		for (Record<E> record : records) {
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

	private List<Centroid> relocateCentroids(Map<Centroid, List<Record<E>>> clusters) {
		List<Centroid> centroids = new ArrayList<>();
		for (Entry<Centroid, List<Record<E>>> cs : clusters.entrySet()) {
			centroids.add(average(cs.getKey(), cs.getValue()));
		}

		return centroids;
	}

	private Centroid average(Centroid centroid, List<Record<E>> records) {
		if (records == null || records.isEmpty()) {
			return centroid;
		}

		Double[] vector = new Double[200];
		for (int i = 0; i < vector.length; i++) {
			vector[i] = 0D;
		}

		for (Word2VecBasedKMeans<E>.Record<E> rec : records) {
			for (int i = 0; i < rec.word.length; i++) {
				vector[i] += rec.word[i];
			}
		}
		for (int i = 0; i < vector.length; i++) {
			vector[i] /= records.size();
		}

		return new Centroid(vector);

	}

	private void assignToCluster(Map<Centroid, List<Record<E>>> clusters, Record<E> record, Centroid centroid) {

		List<Record<E>> records;

		if ((records = clusters.get(centroid)) == null)
			clusters.put(centroid, records = new ArrayList<>());

		records.add(record);

	}

	private Centroid nearestCentroid(Record<E> record, List<Centroid> centroids) {
		double minimumDistance = Double.MAX_VALUE;
		Centroid nearest = null;

		for (Centroid centroid : centroids) {
			double currentDistance = cosineSimilarityDistance(record.word, centroid.word);

			if (currentDistance < minimumDistance) {
				minimumDistance = currentDistance;
				nearest = centroid;
			}
		}
		return nearest;
	}

	public static double cosineSimilarityDistance(Double[] vec1, Double[] vec2) {

		if (vec1 == null || vec2 == null)
			return 1;

		double dotProduct = 0.0;
		double normA = 0.0;
		double normB = 0.0;
		for (int i = 0; i < vec1.length; i++) {
			dotProduct += vec1[i] * vec2[i];
			normA += Math.pow(vec1[i], 2);
			normB += Math.pow(vec2[i], 2);
		}
		return 1 - (dotProduct / (Math.sqrt(normA) * Math.sqrt(normB)));
	}

	private static double squareDistance(Double[] vec1, Double[] vec2) {

		double dist = 0;

		for (int i = 0; i < vec1.length; i++) {

			dist += Math.pow(vec1[i] - vec2[i], 2);

		}
		return dist;

	}

	private static double levenshteinSimilarty(String word1, String word2) {
		return LevenShteinSimilarities.levenshteinSimilarity(word1, word2, 100);
	}
}
