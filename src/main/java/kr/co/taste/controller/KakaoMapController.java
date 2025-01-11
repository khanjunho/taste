package kr.co.taste.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class KakaoMapController {

    @Value("${kakao.js.app.key}") // application.properties에서 앱 키 읽기
    private String kakaoAppKey;

    @GetMapping("/searchResult")
    public String showSearchResult(Model model) {
        model.addAttribute("kakaoAppKey", kakaoAppKey); // 템플릿에 전달
        return "searchResult";
    }
}
