package com.atomic.getTentor.Controller;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import com.atomic.getTentor.dto.VerifyOtpRequest;
import com.atomic.getTentor.dto.ChangePassword;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.atomic.getTentor.controller.ForgotPasswordMenteeController;
import com.atomic.getTentor.dto.MailBody;
import com.atomic.getTentor.model.Admin;
import com.atomic.getTentor.model.ForgotPasswordMentee;
import com.atomic.getTentor.model.Mahasiswa;
import com.atomic.getTentor.model.Mentee;
import com.atomic.getTentor.model.Tentor;
import com.atomic.getTentor.model.VerificationStatus;
import com.atomic.getTentor.repository.AdminRepository;
import com.atomic.getTentor.repository.ForgotPasswordMenteeRepository;
import com.atomic.getTentor.repository.MahasiswaRepository;
import com.atomic.getTentor.repository.MenteeRepository;
import com.atomic.getTentor.repository.TentorRepository;
import com.atomic.getTentor.security.JwtService;
import com.atomic.getTentor.service.EmailService;

@ExtendWith(MockitoExtension.class)
public class ForgotPasswordMenteeTest {
    private static final String TEST_EMAIL = "mentee@example.com";
    private static final String NEW_PASSWORD = "newPassword123";
    private static final String REPEATED_PASSWORD = "newPassword123";
    private static final String WRONG_REPEATED_PASSWORD = "WRONG_PASSWORD";
    private static final String RESET_TOKEN = "valid-reset-token";
    private static final String INVALID_RESET_TOKEN = "invalid-token";
    private static final Integer VALID_OTP = 123456;
    private static final Integer INVALID_OTP = 999999;
    private static final String PASSWORD_MISSMATCH = "Tolong masukkan Password ulang!";
    private static final String EMPTY_TOKEN = "Reset token tidak boleh kosong!";
    private static final String INVALID_TOKEN = "Reset token tidak valid atau sudah expired!";
    private static final String NOTRESET_TOKEN = "Token ini bukan token reset password!";
    private static final String SUCCESS_PASSWORD_CHANGE = "Password telah diganti!";

    @Mock
    private MahasiswaRepository mahasiswaRepository;

    @Mock
    private MenteeRepository menteeRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private ForgotPasswordMenteeRepository forgotPasswordMenteeRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private ForgotPasswordMenteeController forgotPasswordMenteeController;

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private Mahasiswa mahasiswa;
    private Mentee mentee;
    private ForgotPasswordMentee forgotPasswordMentee;

    @BeforeEach
    void setup() {
        mahasiswa = new Mahasiswa();
        mahasiswa.setEmail(TEST_EMAIL);
        mahasiswa.setPassword(passwordEncoder.encode("oldPassword"));
        
        mentee = new Mentee();
        mentee.setMahasiswa(mahasiswa);

        forgotPasswordMentee = new ForgotPasswordMentee();
        forgotPasswordMentee.setFpid(1);
        forgotPasswordMentee.setOtp(VALID_OTP);
        forgotPasswordMentee.setMentee(mentee);
        forgotPasswordMentee.setExpirationTime(new Date(System.currentTimeMillis() + 5 * 60 * 1000)); // 5 minutes from now
    }   

    @Test
    void verifikasiemailsukses() {
        when(menteeRepository.findByMahasiswaEmail(TEST_EMAIL)).thenReturn(mentee);
        when(forgotPasswordMenteeRepository.save(any(ForgotPasswordMentee.class))).thenReturn(forgotPasswordMentee);
        ResponseEntity<String> response = forgotPasswordMenteeController.verifyEmail(TEST_EMAIL);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Email sent for verification!", response.getBody());
        verify(emailService).sendSimpleMessage(any(MailBody.class));
        verify(forgotPasswordMenteeRepository).save(any(ForgotPasswordMentee.class));
    }

    @Test
    void verifikasiEmailGagalEmailTidakDitemukan() {
    when(menteeRepository.findByMahasiswaEmail(TEST_EMAIL)).thenReturn(null);

    ResponseEntity<String> response =
            forgotPasswordMenteeController.verifyEmail(TEST_EMAIL);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals("Email tidak ditemukan", response.getBody());
}

    @Test
    void verifikasiOtpSukses() {
        VerifyOtpRequest request = new VerifyOtpRequest(TEST_EMAIL, VALID_OTP);
        when(menteeRepository.findByMahasiswaEmail(TEST_EMAIL)).thenReturn(mentee);
        when(forgotPasswordMenteeRepository.findByOtpAndMentee(VALID_OTP, mentee))
                .thenReturn(Optional.of(forgotPasswordMentee));
        when(jwtService.generateResetPasswordToken(TEST_EMAIL)).thenReturn(RESET_TOKEN);
        ResponseEntity<Map<String, String>> response = forgotPasswordMenteeController.verifyOTP(request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("OTP berhasil diverifikasi", response.getBody().get("message"));
        assertEquals(RESET_TOKEN, response.getBody().get("resetToken"));
        verify(forgotPasswordMenteeRepository).deleteById(any());
    }

    @Test
    void verifikasiOtpInvalid() {
        VerifyOtpRequest request = new VerifyOtpRequest(TEST_EMAIL, INVALID_OTP);
        when(menteeRepository.findByMahasiswaEmail(TEST_EMAIL)).thenReturn(mentee);
        when(forgotPasswordMenteeRepository.findByOtpAndMentee(INVALID_OTP, mentee))
                .thenReturn(Optional.empty());
        ResponseEntity<Map<String, String>> response = forgotPasswordMenteeController.verifyOTP(request);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("OTP Tidak Sesuai", response.getBody().get("message"));
    }

    @Test
    void verifikasiOtpExpired() {
        VerifyOtpRequest request = new VerifyOtpRequest(TEST_EMAIL, VALID_OTP);
        forgotPasswordMentee.setExpirationTime(new Date(System.currentTimeMillis() - 10000)); 
        when(menteeRepository.findByMahasiswaEmail(TEST_EMAIL)).thenReturn(mentee);
        when(forgotPasswordMenteeRepository.findByOtpAndMentee(VALID_OTP, mentee))
                .thenReturn(Optional.of(forgotPasswordMentee));

        ResponseEntity<Map<String, String>> response = forgotPasswordMenteeController.verifyOTP(request);
        assertEquals(HttpStatus.EXPECTATION_FAILED, response.getStatusCode());
        assertEquals("OTP telah kadaluarsa", response.getBody().get("message"));
        verify(forgotPasswordMenteeRepository).deleteById(any());
    }

    @Test
    void gantiPasswordBerhasil() {
    ChangePassword request = new ChangePassword(NEW_PASSWORD, REPEATED_PASSWORD, RESET_TOKEN);
    
    when(jwtService.getEmailFromToken(RESET_TOKEN)).thenReturn(TEST_EMAIL);
    when(jwtService.validateToken(RESET_TOKEN)).thenReturn(true);
    when(jwtService.isResetPasswordToken(RESET_TOKEN)).thenReturn(true);
    ResponseEntity<String> response = forgotPasswordMenteeController.changePasswordHandlerMentee(request);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(SUCCESS_PASSWORD_CHANGE, response.getBody());
    verify(mahasiswaRepository).updatePassword(eq(TEST_EMAIL), anyString());
}

    @Test
    void gantiPasswordGagalMismatch() {
        ChangePassword request = new ChangePassword(NEW_PASSWORD, WRONG_REPEATED_PASSWORD, RESET_TOKEN);
        ResponseEntity<String> response = forgotPasswordMenteeController.changePasswordHandlerMentee(request);
        assertEquals(HttpStatus.EXPECTATION_FAILED, response.getStatusCode());
        assertEquals(PASSWORD_MISSMATCH, response.getBody());
    }

    @Test
    void gantiPasswordGagalTokenEmpty() {
        ChangePassword request = new ChangePassword(NEW_PASSWORD, REPEATED_PASSWORD, "");
        ResponseEntity<String> response = forgotPasswordMenteeController.changePasswordHandlerMentee(request);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(EMPTY_TOKEN, response.getBody());
    }

    @Test
    void gantiPasswordGagalInvalidToken() {
        ChangePassword request = new ChangePassword(NEW_PASSWORD, REPEATED_PASSWORD, INVALID_RESET_TOKEN);
        when(jwtService.getEmailFromToken(INVALID_RESET_TOKEN)).thenReturn(TEST_EMAIL);
        when(jwtService.validateToken(INVALID_RESET_TOKEN)).thenReturn(false);
        ResponseEntity<String> response = forgotPasswordMenteeController.changePasswordHandlerMentee(request);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(INVALID_TOKEN, response.getBody());
    }

    @Test
    void gantiPasswordGagalExpiredToken() {
        ChangePassword request = new ChangePassword(NEW_PASSWORD, REPEATED_PASSWORD, INVALID_RESET_TOKEN);
        when(jwtService.getEmailFromToken(INVALID_RESET_TOKEN)).thenThrow(new RuntimeException("Expired JWT"));
        ResponseEntity<String> response = forgotPasswordMenteeController.changePasswordHandlerMentee(request);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(INVALID_TOKEN, response.getBody());
    }

    @Test
    void gantiPasswordGagalNotResetToken() {
        ChangePassword request = new ChangePassword(NEW_PASSWORD, REPEATED_PASSWORD, RESET_TOKEN);
        when(jwtService.getEmailFromToken(RESET_TOKEN)).thenReturn(TEST_EMAIL);
        when(jwtService.validateToken(RESET_TOKEN)).thenReturn(true);
        when(jwtService.isResetPasswordToken(RESET_TOKEN)).thenReturn(false);
        ResponseEntity<String> response = forgotPasswordMenteeController.changePasswordHandlerMentee(request);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(NOTRESET_TOKEN, response.getBody());
    }

}


