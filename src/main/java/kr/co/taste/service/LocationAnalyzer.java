package kr.co.taste.service;

import java.util.*;
import java.util.stream.Collectors;

public class LocationAnalyzer {

    public static double[] findDensestCluster(List<double[]> coordinates, double epsilon, int minPoints) {
        if (coordinates.isEmpty()) {
            return new double[]{0.0, 0.0};
        }

        // DBSCAN 클러스터링
        Map<Integer, List<double[]>> clusters = dbscan(coordinates, epsilon, minPoints);

        // 가장 큰 클러스터 찾기
        List<double[]> densestCluster = clusters.values().stream()
                .max(Comparator.comparingInt(List::size))
                .orElse(Collections.emptyList());

        // 가장 큰 클러스터의 중심 좌표 계산
        return calculateClusterCenter(densestCluster);
    }

    public static Map.Entry<double[], Integer> findDensestClusterWithSize(List<double[]> coordinates, double epsilon, int minPoints) {
        if (coordinates.isEmpty()) {
            return new AbstractMap.SimpleEntry<>(new double[]{0.0, 0.0}, 0);
        }

        // DBSCAN 클러스터링
        Map<Integer, List<double[]>> clusters = dbscan(coordinates, epsilon, minPoints);

        // 가장 큰 클러스터 찾기
        Map.Entry<Integer, List<double[]>> densestClusterEntry = clusters.entrySet().stream()
                .max(Comparator.comparingInt(entry -> entry.getValue().size()))
                .orElse(new AbstractMap.SimpleEntry<>(0, Collections.emptyList()));

        List<double[]> densestCluster = densestClusterEntry.getValue();
        double[] densestClusterCenter = calculateClusterCenter(densestCluster);

        // 반환: 중심 좌표와 클러스터 크기
        return new AbstractMap.SimpleEntry<>(densestClusterCenter, densestCluster.size());
    }

    public static void printClusterDetails(Map<Integer, List<double[]>> clusters) {
        clusters.forEach((clusterId, clusterPoints) -> {
            double[] center = calculateClusterCenter(clusterPoints);
            System.out.printf("Cluster ID: %d, Size: %d, Center: (%.6f, %.6f)%n",
                    clusterId, clusterPoints.size(), center[0], center[1]);
        });
    }

    private static Map<Integer, List<double[]>> dbscan(List<double[]> points, double epsilon, int minPoints) {
        Map<Integer, List<double[]>> clusters = new HashMap<>();
        Set<double[]> visited = new HashSet<>();
        int clusterId = 0;

        for (double[] point : points) {
            if (visited.contains(point)) {
                continue;
            }

            visited.add(point);
            List<double[]> neighbors = getNeighbors(point, points, epsilon);

            if (neighbors.size() >= minPoints) {
                clusterId++;
                clusters.put(clusterId, new ArrayList<>());
                expandCluster(point, neighbors, clusters.get(clusterId), visited, points, epsilon, minPoints);
            }
        }

        return clusters;
    }

    private static void expandCluster(
            double[] point,
            List<double[]> neighbors,
            List<double[]> cluster,
            Set<double[]> visited,
            List<double[]> points,
            double epsilon,
            int minPoints) {

        cluster.add(point);

        // neighbors 리스트 복사본을 사용
        List<double[]> neighborsCopy = new ArrayList<>(neighbors);

        for (double[] neighbor : neighborsCopy) {
            if (!visited.contains(neighbor)) {
                visited.add(neighbor);
                List<double[]> newNeighbors = getNeighbors(neighbor, points, epsilon);

                if (newNeighbors.size() >= minPoints) {
                    // neighbors 리스트에 중복을 피하며 추가
                    for (double[] newNeighbor : newNeighbors) {
                        if (!neighbors.contains(newNeighbor)) {
                            neighbors.add(newNeighbor);
                        }
                    }
                }
            }

            // cluster 리스트에 중복을 피하며 추가
            if (cluster.stream().noneMatch(p -> Arrays.equals(p, neighbor))) {
                cluster.add(neighbor);
            }
        }
    }

    private static List<double[]> getNeighbors(double[] point, List<double[]> points, double epsilon) {
        return points.stream()
                .filter(p -> distance(point, p) <= epsilon)
                .collect(Collectors.toList());
    }

    private static double distance(double[] p1, double[] p2) {
        double latDiff = p1[0] - p2[0];
        double lngDiff = p1[1] - p2[1];
        return Math.sqrt(latDiff * latDiff + lngDiff * lngDiff);
    }

    private static double[] calculateClusterCenter(List<double[]> cluster) {
        double sumLat = 0;
        double sumLng = 0;

        for (double[] point : cluster) {
            sumLat += point[0];
            sumLng += point[1];
        }

        return new double[]{sumLat / cluster.size(), sumLng / cluster.size()};
    }
}
