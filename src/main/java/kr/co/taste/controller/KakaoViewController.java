package kr.co.taste.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.taste.service.LocationAnalyzer;
import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 1) 검색 폼 페이지 (GET)
 * 2) 키워드 검색 (POST) → 카카오 로컬 API 호출 → 결과 JSON → 뷰로 전달
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/kakao")
public class KakaoViewController {

    @Value("${kakao.api.key}")
    private String kakaoApiKey;  // 카카오 REST API 키 (검색용)

    @Value("${kakao.js.app.key}") // application.properties에서 앱 키 읽기
    private String kakaoAppKey;

    private final ObjectMapper mapper = new ObjectMapper(); // Jackson ObjectMapper

    // 1) 검색 폼 페이지
    @GetMapping("/searchForm")
    public String searchForm(Model model) {
        model.addAttribute("kakaoAppKey", kakaoAppKey); // 템플릿에 전달
        return "searchForm";  // resources/templates/searchForm.html
    }

    // 2) 검색 결과 (POST)
    @PostMapping("/searchResult")
    public String searchResult(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam String categories,
            @RequestParam int radius,
            Model model) {
        model.addAttribute("kakaoAppKey", kakaoAppKey); // 템플릿에 전달

        String[] categoryList = categories.split(",");
        List<double[]> coordinates = new ArrayList<>(); // 모든 좌표 저장
        List<Map<String, Object>> placeDetails = new ArrayList<>(); // 업소 정보 저장
        OkHttpClient client = new OkHttpClient();

        for (String category : categoryList) {
            if (placeDetails.size() >= 5) break; // 키워드는 최대 5개 처리

            String url = "https://dapi.kakao.com/v2/local/search/keyword.json"
                    + "?query=" + URLEncoder.encode(category.trim(), StandardCharsets.UTF_8)
                    + "&x=" + longitude + "&y=" + latitude + "&radius=" + radius;

            try (Response response = client.newCall(new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "KakaoAK " + kakaoApiKey)
                    .build()).execute()) {

                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JsonNode root = mapper.readTree(responseBody);
                    JsonNode documents = root.get("documents");

                    if (documents != null && documents.isArray()) {
                        for (JsonNode place : documents) {
                            double placeLat = place.get("y").asDouble();
                            double placeLng = place.get("x").asDouble();

                            // 좌표 저장
                            coordinates.add(new double[]{placeLat, placeLng});

                            // 장소 상세 정보 저장
                            Map<String, Object> details = new HashMap<>();
                            details.put("name", place.get("place_name").asText());
                            details.put("lat", placeLat);
                            details.put("lng", placeLng);
                            details.put("category", category.trim());
                            placeDetails.add(details);

                            if (placeDetails.size() >= 5) break; // 키워드는 최대 5개
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // 밀집된 클러스터 계산
        double epsilon = 0.01; // 반경 (적절히 조정 필요)
        int minPoints = 3; // 최소 포인트 수 (적절히 조정 필요)
        Map.Entry<double[], Integer> densestCluster = LocationAnalyzer.findDensestClusterWithSize(coordinates, epsilon, minPoints);

        model.addAttribute("latitude", latitude);
        model.addAttribute("longitude", longitude);
        model.addAttribute("radius", radius);
        model.addAttribute("densestPoint", densestCluster.getKey()); // 밀집된 중심 좌표
        model.addAttribute("densestClusterSize", densestCluster.getValue()); // 클러스터 크기
        model.addAttribute("totalLocations", coordinates.size()); // 총 업소 수
        model.addAttribute("placeDetails", placeDetails); // 업소 상세 정보
        return "searchResult";
    }

}
