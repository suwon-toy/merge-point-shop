package product.demo.shop.domain.verification.service;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import product.demo.shop.domain.verification.entity.EmailVerificationEntity;
import product.demo.shop.domain.verification.enums.VerificationCodeStatus;
import product.demo.shop.domain.verification.exception.EmailVerificationErrorCode;
import product.demo.shop.domain.verification.exception.EmailVerificationException;
import product.demo.shop.domain.verification.repository.EmailAuthenticationRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith({MockitoExtension.class})
class EmailAuthenticationServiceTest {
    @InjectMocks
    EmailAuthenticationService emailAuthenticationService;

    @Mock
    EmailAuthenticationRepository emailAuthenticationRepository;
    final String VERIFICATION_CODE = "0afdf841-e2f7-4a70-8d75-1865332ec375";
    final long USER_ID = 1L;


    @Nested
    class 이메일_인증_코드을_통한_이메일_인증 {

        private EmailVerificationEntity createEmailVerificationEntity(String verificationCode, LocalDateTime expiredDate, long userId, VerificationCodeStatus verificationCodeStatus) {
            return EmailVerificationEntity
                    .builder()
                    .emailVerificationCodeId(1L)
                    .verificationCode(verificationCode)
                    .expiredDate(expiredDate)
                    .verificationCodeStatus(verificationCodeStatus)
                    .userId(userId)
                    .build();
        }

        @Test
        void 해당_코드가_정상적이면_인증에_성공한다() {
            EmailVerificationEntity emailVerificationEntity = createEmailVerificationEntity(VERIFICATION_CODE, LocalDateTime.now().plus(3L, ChronoUnit.DAYS), USER_ID, VerificationCodeStatus.CREATED);
            given(emailAuthenticationRepository.findByVerificationCode(VERIFICATION_CODE)).willReturn(Optional.of(emailVerificationEntity));

            emailAuthenticationService.verifyEmailUsingVerificationCode(VERIFICATION_CODE);

            verify(emailAuthenticationRepository, times(1)).findByVerificationCode(VERIFICATION_CODE);
            assertThat(emailVerificationEntity.getVerificationCodeStatus(),is(equalTo(VerificationCodeStatus.CONFIRMED)));
        }

        @Test
        void 해당_코드가_존재하지_않으면_에러를_내보낸다() {
            given(emailAuthenticationRepository.findByVerificationCode(VERIFICATION_CODE)).willReturn(Optional.empty());

            EmailVerificationException emailVerificationException = assertThrows(EmailVerificationException.class, () -> {
                emailAuthenticationService.verifyEmailUsingVerificationCode(VERIFICATION_CODE);
            });
            assertThat(emailVerificationException.getErrorStatus(), is(equalTo(HttpStatus.BAD_REQUEST)));
            assertThat(emailVerificationException.getErrorMessage(), is(equalTo(EmailVerificationErrorCode.NOT_FOUND_TOKEN.getErrorMessage())));
        }

        @Test
        void 해당_코드가_유효기간이_지났으면_에러를_내보낸다() {
            EmailVerificationEntity emailVerificationEntity = createEmailVerificationEntity(VERIFICATION_CODE, LocalDateTime.now().minus(3L, ChronoUnit.DAYS), USER_ID, VerificationCodeStatus.CREATED);
            given(emailAuthenticationRepository.findByVerificationCode(VERIFICATION_CODE)).willReturn(Optional.of(emailVerificationEntity));

            EmailVerificationException emailVerificationException = assertThrows(EmailVerificationException.class, () -> {
                emailAuthenticationService.verifyEmailUsingVerificationCode(VERIFICATION_CODE);
            });
            assertThat(emailVerificationException.getErrorStatus(), is(equalTo(HttpStatus.BAD_REQUEST)));
            assertThat(emailVerificationException.getErrorMessage(), is(equalTo(EmailVerificationErrorCode.EXPIRED_TOKEN.getErrorMessage())));
        }

        @Test
        void 해당_코드가_이미_사용되었으면_에러를_내보낸다() {
            EmailVerificationEntity emailVerificationEntity = createEmailVerificationEntity(VERIFICATION_CODE, LocalDateTime.now().plus(3L, ChronoUnit.DAYS), USER_ID, VerificationCodeStatus.CONFIRMED);
            given(emailAuthenticationRepository.findByVerificationCode(VERIFICATION_CODE)).willReturn(Optional.of(emailVerificationEntity));

            EmailVerificationException emailVerificationException = assertThrows(EmailVerificationException.class, () -> {
                emailAuthenticationService.verifyEmailUsingVerificationCode(VERIFICATION_CODE);
            });
            assertThat(emailVerificationException.getErrorStatus(), is(equalTo(HttpStatus.BAD_REQUEST)));
            assertThat(emailVerificationException.getErrorMessage(), is(equalTo(EmailVerificationErrorCode.ALREADY_USED_TOKEN.getErrorMessage())));
        }
    }

    @Nested
    class 이메일_인증_코드_생성 {
        @Test
        void 이메일_인증_코드_생성에_성공한다() {
            String code = emailAuthenticationService.createVerificationCode(USER_ID);
            assertThat(code, is(equalTo(VERIFICATION_CODE)));
        }
    }
}