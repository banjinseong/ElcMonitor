package charge.station.monitor.controller;

import charge.station.monitor.dto.user.EMailRequestDTO;
import charge.station.monitor.dto.user.UserJoinRequestDTO;
import charge.station.monitor.dto.user.UserLoginRequestDTO;
import charge.station.monitor.dto.user.VerifyRequestDTO;
import charge.station.monitor.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("user/*")
public class UserController {
    private final UserService userService;



    /**
     * 회원가입 유저의 유저 번호를 반환함.
     */
    @PostMapping("join")
    public ResponseEntity<?> joinUser(@Valid @RequestBody UserJoinRequestDTO userJoinRequestDTO, BindingResult bindingResult){
        if (bindingResult.hasErrors()) {
            List<String> errors = bindingResult.getAllErrors().stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .toList();
            // 오류 상태 코드 추가
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", HttpStatus.BAD_REQUEST.value(),
                    "errors", errors
            ));
        }
        Long id = userService.join(userJoinRequestDTO);
        return ResponseEntity.status(HttpStatus.OK).body(id);
    }

    /**
     * 현재 토큰을 반환중인데, 나중에 수정 필요할수도
     */
    @PostMapping("login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody UserLoginRequestDTO userLoginRequestDTO, BindingResult bindingResult){
        if (bindingResult.hasErrors()) {
            List<String> errors = bindingResult.getAllErrors().stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .toList();
            // 오류 상태 코드 추가
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", HttpStatus.BAD_REQUEST.value(),
                    "errors", errors
            ));
        }
        String token = userService.login(userLoginRequestDTO);
        return ResponseEntity.status(HttpStatus.OK).body(token);
    }


    /**
     * Access Token 재발급 (Refresh Token 없이 Redis 검증)
     */
    @PostMapping("refresh")
    public ResponseEntity<?> refreshAccessToken(@RequestHeader("Authorization") String authorizationHeader, HttpServletRequest request, HttpServletResponse response)throws IOException {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("잘못된 토큰 형식입니다.");
        }

        String accessToken = authorizationHeader.substring(7).trim(); // ✅ "Bearer " 제거 후 공백 제거
        String newAccessToken = userService.refreshAccessToken(accessToken, request, response);
        return ResponseEntity.status(HttpStatus.OK).body(newAccessToken);
    }

    /**
     * 로그아웃 - Redis에서 Refresh Token 삭제
     */
    @PostMapping("logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authorizationHeader, HttpServletRequest request, HttpServletResponse response)throws IOException {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("잘못된 토큰 형식입니다.");
        }

        // ✅ "Bearer " 제거 후 순수한 Access Token 추출
        String accessToken = authorizationHeader.substring(7).trim();

        userService.logout(accessToken, request, response);
        return ResponseEntity.ok("로그아웃되었습니다.");
    }



    // ✅ 인증번호 요청 API (signup / find)
    @PostMapping("/sendCode")
    public ResponseEntity<String> sendAuthCode(@RequestBody EMailRequestDTO request) {
        if (!request.getType().equals("signup") && !request.getType().equals("find")) {
            throw new IllegalArgumentException("잘못된 요청 유형입니다.");
        }
        userService.requestAuthCode(request.getEmail(), request.getType());
        return ResponseEntity.ok("인증번호가 이메일로 전송되었습니다.");
    }

    // ✅ 인증번호 검증 API (signup / find)
    @PostMapping("/verifyCode")
    public ResponseEntity<String> verifyAuthCode(@RequestBody VerifyRequestDTO request) {
        if (!request.getType().equals("signup") && !request.getType().equals("find")) {
            throw new IllegalArgumentException("잘못된 요청 유형입니다.");
        }
        boolean isVerified = userService.verifyAuthCode(request.getEmail(), request.getType(), request.getCode());
        return ResponseEntity.ok(isVerified ? "인증 성공" : "인증 실패");
    }

}
