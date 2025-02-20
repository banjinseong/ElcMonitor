package charge.station.monitor.config.filter;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class ApiKeyFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-KEY";
    private static final String VALID_API_KEY = "g@$SZGa35gsag@$yhasf321!$#@Tdfsf2^#@RTSd"; // 🔹 실제 운영에서는 환경변수로 설정

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 🔹 /rawData/** 요청이 아니면 필터를 건너뜀
        if (!request.getRequestURI().startsWith("/rawData/")) {
            filterChain.doFilter(request, response);
            return;
        }
        // 🔹 API Key 헤더 확인
        String apiKey = request.getHeader(API_KEY_HEADER);
        if (apiKey == null || !apiKey.equals(VALID_API_KEY)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid API Key");
            return;
        }

        // 🔹 인증 성공 시 다음 필터 진행
        filterChain.doFilter(request, response);
    }
}
