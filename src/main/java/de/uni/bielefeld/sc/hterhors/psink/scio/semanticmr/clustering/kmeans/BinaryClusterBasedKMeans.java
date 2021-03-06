package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.clustering.kmeans;

import java.util.ArrayList;
import java.util.Arrays;
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.crf.structure.annotations.LiteralAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.container.TextualContent;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.clustering.groupnames.helper.GroupNamePair;

public class BinaryClusterBasedKMeans<E extends LiteralAnnotation> {
	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	private static final Random random = new Random(1000L);

	final static private int maxIterations = 1000;

	final private Map<String, Map<String, Double>> distances = new HashMap<>();

	public static void main(String[] args) {

		SystemScope.Builder.getScopeHandler()
				.addScopeSpecification(DataStructureLoader.loadSlotFillingDataStructureReader("ExperimentalGroup"))
				.build();
		log.info(getProb(0D));
		log.info(getProb(0D));
		log.info(getProb(1D));
		log.info(getProb(1D));
		/*
		 * First cluster
		 */
		String A = "co-graft rats";
		String B = "co-graft group";
		String C = "co-graft";
		String D = "cograft animals";
		String E = "co-grafted animals";
		/*
		 * Second cluster
		 */
		String F = "BMSC group";

		/*
		 * Third cluster
		 * 
		 */
		String G = "control group";
		String H = "control";

		/*
		 * Fourth cluster
		 */
		String I = "OEC group";

		List<GroupNamePair> gnd = new ArrayList<>();
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(A)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(B)), true, getProb(1D)));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(A)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(C)), true, getProb(1D)));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(A)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(D)), true, getProb(1D)));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(A)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(E)), true, getProb(1D)));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(A)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(F)), true, getProb(0D)));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(A)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(G)), true, getProb(0D)));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(A)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(H)), true, getProb(0D)));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(A)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(I)), true, getProb(0D)));
//		
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(B)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(C)), true, getProb(1D)));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(B)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(D)), true, getProb(1D)));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(B)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(E)), true, getProb(1D)));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(B)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(F)), true, getProb(0D)));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(B)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(G)), true, getProb(0D)));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(B)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(H)), true, getProb(0D)));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(B)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(I)), true, getProb(0D)));
//		
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(C)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(D)), true, getProb(1D)));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(C)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(E)), true, getProb(1D)));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(C)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(F)), true, getProb(0D)));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(C)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(G)), true, getProb(0D)));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(C)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(H)), true, getProb(0D)));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(C)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(I)), true, getProb(0D)));
//		
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(D)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(E)), true, getProb(1D)));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(D)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(F)), true, getProb(0D)));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(D)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(G)), true, getProb(0D)));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(D)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(H)), true, getProb(0D)));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(D)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(I)), true, getProb(0D)));
//		
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(E)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(F)), true, getProb(0D)));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(E)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(G)), true, getProb(0D)));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(E)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(H)), true, getProb(0D)));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(E)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(I)), true, getProb(0D)));
//		
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(F)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(G)), true, getProb(0D)));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(F)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(H)), true, getProb(0D)));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(F)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(I)), true, getProb(0D)));
//		
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(G)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(H)), true, getProb(1D)));
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(G)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(I)), true, getProb(0D)));
//		
		gnd.add(new GroupNamePair(new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(H)),
				new LiteralAnnotation(SCIOEntityTypes.groupName, new TextualContent(I)), true, getProb(0D)));
//		

		BinaryClusterBasedKMeans<LiteralAnnotation> c = new BinaryClusterBasedKMeans<>(gnd);

		List<LiteralAnnotation> datapoints = new ArrayList<>(gnd.stream()
				.flatMap(a -> Arrays.asList(a.groupName1, a.groupName2).stream()).collect(Collectors.toSet()));

		List<List<LiteralAnnotation>> clusters = c.cluster(datapoints, 4);
		log.info("Number of clusters = " + clusters.size());
		for (List<LiteralAnnotation> cluster : clusters) {
			log.info("Cluster size = " + cluster.size());
		}

		for (List<LiteralAnnotation> cluster : clusters) {
			for (LiteralAnnotation groupName : cluster) {
				log.info(groupName.getSurfaceForm());
			}
			log.info("-------------------");
		}
	}

	private static double getProb(double d) {
		if (d == 0D)
			return ((double) new Random().nextInt(50)) / 100D;

		return (60D + new Random().nextInt(40)) / 100D;
	}

	/**
	 * BEST VALUE EVALUATED ON TRAIN
	 */
	final public static double BEST_LAMBDA_VALUE = 1.1D;

	public double lambda = BEST_LAMBDA_VALUE;

	public List<List<E>> clusterRSS(List<E> datapoints, int min, int max) {

		Map<Centroid, List<Record<E>>> clusterRecords = new HashMap<>();

		List<Record<E>> vecs = toDataPoints(datapoints);

		double smallestRSS = Double.MAX_VALUE;
		for (int clusterSize = min; clusterSize <= max; clusterSize++) {

			Map<Centroid, List<Record<E>>> cR = kMeans(vecs, clusterSize, maxIterations);

			double RSS = RSS(cR) + lambda * clusterSize;

			if (RSS < smallestRSS) {
				smallestRSS = RSS;
				clusterRecords = cR;
			}

		}

		List<List<E>> clusters = new ArrayList<>();

		for (List<Record<E>> records : clusterRecords.values()) {
			clusters.add(records.stream().map(r -> r.annotation).collect(Collectors.toList()));
		}

		return clusters;

	}

	private double RSS(Map<Centroid, List<Record<E>>> clusterRecords) {

		double rss = 0;

		for (Entry<Centroid, List<Record<E>>> cluster : clusterRecords.entrySet()) {
			Centroid c = cluster.getKey();
			for (Record<E> r : cluster.getValue()) {
				rss += distance(r, c);
			}
		}
		return rss;
	}

	public BinaryClusterBasedKMeans(List<GroupNamePair> gnd) {

		for (GroupNamePair groupNamePair : gnd) {

			distances.putIfAbsent(groupNamePair.groupName1.getSurfaceForm(), new HashMap<>());
			distances.putIfAbsent(groupNamePair.groupName2.getSurfaceForm(), new HashMap<>());

			distances.get(groupNamePair.groupName1.getSurfaceForm()).put(groupNamePair.groupName2.getSurfaceForm(),
					groupNamePair.probability);
			distances.get(groupNamePair.groupName2.getSurfaceForm()).put(groupNamePair.groupName1.getSurfaceForm(),
					groupNamePair.probability);
		}

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

		log.info("Start k (" + k + ") - means clustering...");

		List<Record<E>> vecs = toDataPoints(datapoints);

		Map<Centroid, List<Record<E>>> clusterRecords = kMeans(vecs, k, maxIterations);
		List<List<E>> clusters = new ArrayList<>();
		log.info("Clustering done!");

		for (List<Record<E>> records : clusterRecords.values()) {
			clusters.add(records.stream().map(r -> r.annotation).collect(Collectors.toList()));
		}

		for (int i = clusters.size(); i < k; i++) {
			clusters.add(Collections.emptyList());
		}

		return clusters;

	}

	private List<Record<E>> toDataPoints(List<E> datapoints) {

		List<Record<E>> list = datapoints.stream().map(s -> new Record<E>(s)).collect(Collectors.toList());

		return list;
	}

	static class Centroid {

		public final List<String> words;

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((words == null) ? 0 : words.hashCode());
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
			if (words == null) {
				if (other.words != null)
					return false;
			} else if (!words.equals(other.words))
				return false;
			return true;
		}

		public Centroid(List<String> words) {
			this.words = words;
		}

		public Centroid(String word) {
			this.words = new ArrayList<>();
			this.words.add(word);
		}

	}

	static class Record<E extends LiteralAnnotation> {
		public final String word;
		public final E annotation;

		public Record(E annotation) {
			this.annotation = annotation;
			this.word = annotation.getSurfaceForm();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
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

	private Map<Centroid, List<Record<E>>> kMeans(List<Record<E>> records, int k, int maxIterations) {
		List<Centroid> centroids = plusplusCentroids(records, k);

		Map<Centroid, List<Record<E>>> lastState = new HashMap<>();
//		log.info("Max iterations = " + maxIterations);
		// iterate for a pre-defined number of times
		int i = 0;
		for (i = 0; i < maxIterations; i++) {

//			if (i % ((double) maxIterations / 10) == 0)
//				log.info("Intermediate iteration = " + i);

			final Map<Centroid, List<Record<E>>> clusters = new HashMap<>();

			boolean isLastIteration = i == maxIterations - 1;

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
//		log.info("Last iteration = " + i);
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
					double dist = distance(records.get(i), centroid);
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
			centroids.add(new Centroid(recMap.get(d.recordIndex).word));
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

	private List<Centroid> relocateCentroids(Map<Centroid, List<Record<E>>> clusters) {
		List<Centroid> centroids = new ArrayList<>();
		for (Entry<Centroid, List<Record<E>>> cs : clusters.entrySet()) {
			centroids.add(update(cs.getKey(), cs.getValue()));
		}

		return centroids;
	}

	private Centroid update(Centroid centroid, List<Record<E>> records) {
		if (records == null || records.isEmpty()) {
			return centroid;
		}

		return new Centroid(records.stream().map(r -> r.word).collect(Collectors.toList()));

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
			double currentDistance = distance(record, centroid);

			if (currentDistance < minimumDistance) {
				minimumDistance = currentDistance;
				nearest = centroid;
			}
		}

		return nearest;
	}

	private double distance(Record<E> record, Centroid centroid) {
		double d = 0;
		for (String word : centroid.words) {
			d += distance(record.word, word);
		}
		d /= centroid.words.size();
		return d;
	}

	private double distance(String word1, String word2) {
		if (word1.equals(word2))
			return 0;
		return 1 - distances.get(word1).get(word2);
	}

}
