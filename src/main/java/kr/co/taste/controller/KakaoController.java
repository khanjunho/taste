package kr.co.taste.controller;

import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/kakao")
public class KakaoController {

    @Value("${kakao.api.key}")
    private String kakaoApiKey;

    /**
     * 장소 검색 (키워드 기반)
     * GET /api/kakao/search?query={검색어}
     */
    @GetMapping("/search")
    public ResponseEntity<String> search(@RequestParam String query) {
        // 카카오 로컬 API (키워드 검색)
        String url = "https://dapi.kakao.com/v2/local/search/keyword.json?query=" + query;

        // OkHttpClient 생성
        OkHttpClient client = new OkHttpClient();

        // 요청 생성 (헤더: Authorization 에 KakaoAK {REST API Key} 추가)
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "KakaoAK " + kakaoApiKey)
                .build();

        try (Response response = client.newCall(request).execute()) {
            // 성공적으로 응답을 받은 경우
            if (response.isSuccessful()) {
                // 카카오에서 보내준 JSON 데이터를 그대로 내려줌
                String result = response.body().string();
                return ResponseEntity.ok(result);
            } else {
                // 응답 코드가 200이 아닌 경우
                String errorBody = response.body() != null ? response.body().string() : "No Response Body";
                return ResponseEntity.status(response.code()).body(errorBody);
            }
        } catch (IOException e) {
            // API 요청 실패 또는 응답 처리 중 예외가 발생한 경우
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}
