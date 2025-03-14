package charge.station.monitor.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EMailService {
    private final JavaMailSender javaMailSender;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SpringTemplateEngine templateEngine;

    private static final long AUTH_CODE_EXPIRATION = 180; // 3분 (180초)

    // ✅ 6자리 랜덤 인증번호 생성
    private String generateAuthCode() {
        return String.format("%06d", new Random().nextInt(1000000));
    }

    // ✅ 이메일 인증번호 요청 (signup / find 구분)
    @Async
    public void sendAuthCode(String email, String type) {
        String authCode = generateAuthCode(); // 난수 생성
        String redisKey = "auth_code:" + type + ":" + email; // "auth_code:signup:email" or "auth_code:reset:email"

        redisTemplate.opsForValue().set(redisKey, authCode, AUTH_CODE_EXPIRATION, TimeUnit.SECONDS); // Redis에 저장

        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, false, "UTF-8");

            mimeMessageHelper.setTo(email);
            mimeMessageHelper.setSubject(type.equals("signup") ? "회원가입 인증번호" : "비밀번호 찾기 인증번호");
            mimeMessageHelper.setText(setContext(authCode), true); // HTML 본문 적용

            javaMailSender.send(mimeMessage);
            log.info("{} 인증번호 발송 완료: {} -> {}", type.toUpperCase(), authCode, email);
        } catch (Exception e) {
            log.error("{} 인증번호 발송 실패: {}", type.toUpperCase(), e.getMessage());
            throw new RuntimeException("이메일 발송 실패");
        }
    }

    // ✅ Thymeleaf로 HTML 이메일 템플릿 적용
    public String setContext(String authCode) {
        Context context = new Context();
        context.setVariable("authCode", authCode);
        return templateEngine.process("todo", context);
    }
}
