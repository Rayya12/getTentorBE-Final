package com.atomic.getTentor.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.atomic.getTentor.dto.MenteeDTO;
import com.atomic.getTentor.model.Mahasiswa;
import com.atomic.getTentor.model.Mentee;
import com.atomic.getTentor.repository.MahasiswaRepository;
import com.atomic.getTentor.repository.MenteeRepository;

@ExtendWith(MockitoExtension.class)
public class MenteeServiceTest {
    
    // Test data constants
    private static final String TEST_NIM = "123456";
    private static final String TEST_NAMA = "Test Mentee";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String TEST_NO_TELP = "08123456789";
    
    // Alternative test data constants
    private static final String ALT_NIM = "654321";
    private static final String ALT_EMAIL_EXISTING = "existing@example.com";
    private static final String ALT_EMAIL_TESTING = "testing@example.com";
    private static final String NONEXISTENT_EMAIL = "nonexistent@example.com";
    private static final String WRONG_PASSWORD = "wrongpassword";
    
    // Error message constants
    private static final String ERROR_INVALID_CREDENTIALS = "Invalid email or password";
    private static final String ERROR_EMAIL_EXISTS = "Email sudah digunakan!";
    private static final String ERROR_NIM_EXISTS = "Nim sudah digunakan!";
    
    @Mock
    private MenteeRepository menteeRepository;

    @Mock
    private MahasiswaRepository mahasiswaRepository;

    @InjectMocks
    private MenteeService menteeService;

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private MenteeDTO menteeDTO;
    private Mahasiswa mahasiswa;
    private Mentee mentee;

    @BeforeEach
    void setUpAccount() {
        menteeDTO = createMenteeDTO(TEST_NIM, TEST_NAMA, TEST_EMAIL, TEST_PASSWORD, TEST_NO_TELP);
        mahasiswa = createMahasiswa(TEST_NIM, TEST_NAMA, TEST_EMAIL, TEST_PASSWORD, TEST_NO_TELP);
        mentee = createMentee(mahasiswa);
    }
    
    private MenteeDTO createMenteeDTO(String nim, String nama, String email, String password, String noTelp) {
        MenteeDTO dto = new MenteeDTO();
        dto.setNim(nim);
        dto.setNama(nama);
        dto.setEmail(email);
        dto.setPassword(password);
        dto.setNoTelp(noTelp);
        return dto;
    }
    
    private Mahasiswa createMahasiswa(String nim, String nama, String email, String password, String noTelp) {
        Mahasiswa mhs = new Mahasiswa();
        mhs.setNim(nim);
        mhs.setNama(nama);
        mhs.setEmail(email);
        mhs.setPassword(passwordEncoder.encode(password));
        mhs.setNoTelp(noTelp);
        return mhs;
    }
    
    private Mentee createMentee(Mahasiswa mahasiswa) {
        Mentee m = new Mentee();
        m.setMahasiswa(mahasiswa);
        return m;
    }

    @Test
    void login_Success() {
        // Arrange
        when(menteeRepository.findByMahasiswaEmail(TEST_EMAIL)).thenReturn(mentee);

        // Act & Assert
        assertDoesNotThrow(() -> menteeService.login(TEST_EMAIL, TEST_PASSWORD));
    }

    @Test
    void login_UserNotFound() {
        // Arrange
        when(menteeRepository.findByMahasiswaEmail(NONEXISTENT_EMAIL)).thenReturn(null);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> menteeService.login(NONEXISTENT_EMAIL, TEST_PASSWORD));
        assertEquals(ERROR_INVALID_CREDENTIALS, exception.getMessage());
    }

    @Test
    void login_InvalidPassword() {
        // Arrange
        when(menteeRepository.findByMahasiswaEmail(TEST_EMAIL)).thenReturn(mentee);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> menteeService.login(TEST_EMAIL, WRONG_PASSWORD));
        assertEquals(ERROR_INVALID_CREDENTIALS, exception.getMessage());
    }

    @Test
    void register_Success() {
        // Arrange
        when(mahasiswaRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
        when(mahasiswaRepository.existsByNim(TEST_NIM)).thenReturn(false);
        when(mahasiswaRepository.save(any(Mahasiswa.class))).thenReturn(mahasiswa);
        when(menteeRepository.save(any(Mentee.class))).thenReturn(mentee);

        // Act & Assert
        assertDoesNotThrow(() -> menteeService.register(menteeDTO));
        verify(mahasiswaRepository).save(any(Mahasiswa.class));
        verify(menteeRepository).save(any(Mentee.class));
    }

    @Test
    void register_EmailAlreadyExists() {
        // Arrange
        MenteeDTO existingEmailDTO = createMenteeDTO(ALT_NIM, TEST_NAMA, ALT_EMAIL_EXISTING,
                                                      TEST_PASSWORD, TEST_NO_TELP);
        when(mahasiswaRepository.existsByEmail(ALT_EMAIL_EXISTING)).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> menteeService.register(existingEmailDTO));
        assertEquals(ERROR_EMAIL_EXISTS, exception.getMessage());
    }

    @Test
    void register_NimAlreadyExists() {
        // Arrange
        MenteeDTO existingNimDTO = createMenteeDTO(ALT_NIM, TEST_NAMA, ALT_EMAIL_TESTING,
                                                    TEST_PASSWORD, TEST_NO_TELP);
        when(mahasiswaRepository.existsByEmail(ALT_EMAIL_TESTING)).thenReturn(false);
        when(mahasiswaRepository.existsByNim(ALT_NIM)).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> menteeService.register(existingNimDTO));
        assertEquals(ERROR_NIM_EXISTS, exception.getMessage());
    }
}