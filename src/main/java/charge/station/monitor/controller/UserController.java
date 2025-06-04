package charge.station.monitor.controller;

import charge.station.monitor.dto.ApiResponse;
import charge.station.monitor.dto.error.CustomException;
import charge.station.monitor.dto.user.*;
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
    public ResponseEntity<?> joinUser(@Valid @RequestBody JoinRequestDTO joinRequestDTO, BindingResult bindingResult){
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
        //db 아이디 번호 반환
        Long id = userService.join(joinRequestDTO);
        JoinResponseDTO joinResponseDTO = new JoinResponseDTO();
        joinResponseDTO.setId(id);
        ApiResponse<JoinResponseDTO> apiResponse = new ApiResponse<>();
        apiResponse.setCode(200);
        apiResponse.setMessage("회원가입 성공");
        apiResponse.setData(joinResponseDTO);
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }

    /**
     * 현재 토큰을 반환중인데, 나중에 수정 필요할수도
     */
    @PostMapping("login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody LoginRequestDTO loginRequestDTO, BindingResult bindingResult){
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
        String token = userService.login(loginRequestDTO);
        UserTokenResponseDTO tokenResponseDTO = new UserTokenResponseDTO();
        tokenResponseDTO.setToken(token);
        return ResponseEntity.ok(new ApiResponse<>(200, "로그인하셨습니다..", tokenResponseDTO));
    }


    /**
     * Access Token 재발급 (Refresh Token 없이 Redis 검증)
     */
    @PostMapping("refresh")
    public ResponseEntity<?> refreshAccessToken(@RequestHeader("Authorization") String authorizationHeader, HttpServletRequest request, HttpServletResponse response)throws IOException {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new CustomException("잘못된 토큰 형식입니다.", HttpStatus.BAD_REQUEST, 400);
        }

        String accessToken = authorizationHeader.substring(7).trim(); // ✅ "Bearer " 제거 후 공백 제거
        String newAccessToken = userService.refreshAccessToken(accessToken, request, response);

        UserTokenResponseDTO tokenResponseDTO = new UserTokenResponseDTO();
        tokenResponseDTO.setToken(newAccessToken);
        return ResponseEntity.ok(new ApiResponse<>(200, "토큰 재발급 하였습니다.", tokenResponseDTO));

    }

    /**
     * 로그아웃 - Redis에서 Refresh Token 삭제
     */
    @PostMapping("logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authorizationHeader, HttpServletRequest request, HttpServletResponse response)throws IOException {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new CustomException("잘못된 토큰 형식입니다.", HttpStatus.BAD_REQUEST, 400);
        }

        // ✅ "Bearer " 제거 후 순수한 Access Token 추출
        String accessToken = authorizationHeader.substring(7).trim();

        userService.logout(accessToken, request, response);
        return ResponseEntity.ok(new ApiResponse<>(200, "로그아웃 했습니다.", null));
    }



    // ✅ 인증번호 요청 API (signup / find)
    @PostMapping("sendCode")
    public ResponseEntity<?> sendAuthCode(@RequestBody EMailRequestDTO request) {
        if (!request.getType().equals("signup") && !request.getType().equals("find")) {
            throw new CustomException("잘못된 요청 유형입니다.", HttpStatus.BAD_REQUEST, 400);
        }
        userService.requestAuthCode(request);
        return ResponseEntity.ok(new ApiResponse<>(200, "인증번호가 이메일로 전송되었습니다.", null));
    }

    // ✅ 인증번호 검증 API (signup / find)
    @PostMapping("verifyCode")
    public ResponseEntity<?> verifyAuthCode(@RequestBody VerifyRequestDTO request) {
        if (!request.getType().equals("signup") && !request.getType().equals("find")) {
            throw new CustomException("잘못된 요청 유형입니다.", HttpStatus.BAD_REQUEST, 400);
        }
        userService.verifyAuthCode(request.getEmail(), request.getType(), request.getCode());
        return ResponseEntity.ok(new ApiResponse<>(200, "인증 성공.", null));
    }


    /**
     * 비밀번호 찾기에서 비밀번호 변경 코드.
     */
    @PostMapping("findPassword")
    public ResponseEntity<?> findPassword(@RequestBody UpdatePasswordDTO dto){
        userService.updatePassword(dto.getLoginId(), dto.getPassword());
        return ResponseEntity.ok(new ApiResponse<>(200, "비밀번호 변경 성공.", null));
    }


    /**
     * 비밀번호 변경시 요청 코드.
     */
    @PostMapping("resetPssword")
    public ResponseEntity<?> resetPassword(@RequestHeader("Authorization") String authorizationHeader,
                                                @RequestBody UpdatePasswordDTO dto){

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new CustomException("잘못된 토큰 형식입니다.", HttpStatus.BAD_REQUEST, 400);
        }

        // ✅ "Bearer " 제거 후 순수한 Access Token 추출
        String accessToken = authorizationHeader.substring(7).trim();

        userService.updatePassword(accessToken, dto.getPassword(), dto.getNewPassword());

        return ResponseEntity.ok(new ApiResponse<>(200, "비밀번호 변경 성공.", null));
    }


    /**
     * 마이페이지 요청.
     */
    @GetMapping("myPage")
    public ResponseEntity<?> myPage(@RequestHeader("Authorization") String authorizationHeader){

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new CustomException("잘못된 토큰 형식입니다.", HttpStatus.BAD_REQUEST, 400);
        }

        // ✅ "Bearer " 제거 후 순수한 Access Token 추출
        String accessToken = authorizationHeader.substring(7).trim();

        MyPageDTO myPageDTO = userService.myPage(accessToken);

        return ResponseEntity.ok(new ApiResponse<>(200, "마이페이지 진입", myPageDTO));

    }


    /**
     * 마이페이지 수정.
     */
    @PostMapping("myPage/update")
    public ResponseEntity<?> myPageUpdate(@RequestHeader("Authorization") String authorizationHeader, MyPageDTO myPageDTO){

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new CustomException("잘못된 토큰 형식입니다.", HttpStatus.BAD_REQUEST, 400);
        }

        // ✅ "Bearer " 제거 후 순수한 Access Token 추출
        String accessToken = authorizationHeader.substring(7).trim();

        userService.myPageUpdate(accessToken, myPageDTO);

        return ResponseEntity.ok(new ApiResponse<>(200, "마이페이지 내정보 수정", null));
    }



}
