package com.atomic.getTentor.service;

import java.util.Arrays;
import java.util.List;

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

import com.atomic.getTentor.dto.TentorDTO;
import com.atomic.getTentor.model.Mahasiswa;
import com.atomic.getTentor.model.Tentor;
import com.atomic.getTentor.model.VerificationStatus;
import com.atomic.getTentor.repository.MahasiswaRepository;
import com.atomic.getTentor.repository.TentorRepository;

@ExtendWith(MockitoExtension.class)
public class TentorServiceTest {
    
    // Test data constants
    private static final String TEST_NIM = "191003221556";
    private static final String TEST_NAMA = "John Doe Tentor";
    private static final String TEST_EMAIL = "johndoe@example.com";
    private static final String TEST_PASSWORD = "tentor123";
    private static final String TEST_NO_TELP = "083991667553";
    private static final double TEST_IPK = 3.5;
    private static final List<String> TEST_PENGALAMAN_LIST = Arrays.asList(
        "Teaching DSA Class",
        "Anthropic School Representative"
    );
    private static final String TEST_PENGALAMAN_STRING = "Teaching DSA Class|Anthropic School Representative";
    
    // Alternative test data constants
    private static final String ALT_NIM = "191003221006";
    private static final String ALT_EMAIL_EXISTING = "existing@example.com";
    private static final String ALT_EMAIL_TESTING = "johndoetesting@example.com";
    private static final String NONEXISTENT_EMAIL = "nonexistent@example.com";
    private static final String WRONG_PASSWORD = "wrongpassword";
    
    // Error message constants
    private static final String ERROR_INVALID_CREDENTIALS = "Invalid email or password";
    private static final String ERROR_EMAIL_EXISTS = "Email sudah digunakan!";
    private static final String ERROR_NIM_EXISTS = "Nim telah digunakan";
    
    @Mock
    private TentorRepository tentorRepository;

    @Mock
    private MahasiswaRepository mahasiswaRepository;

    @InjectMocks
    private TentorService tentorService;

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private TentorDTO tentorDTO;
    private Mahasiswa mahasiswa;
    private Tentor tentor;

    @BeforeEach
    void setUpAccount() {
        tentorDTO = createTentorDTO(TEST_NIM, TEST_NAMA, TEST_EMAIL, TEST_PASSWORD, TEST_NO_TELP, TEST_IPK, TEST_PENGALAMAN_LIST);
        mahasiswa = createMahasiswa(TEST_NIM, TEST_NAMA, TEST_EMAIL, TEST_PASSWORD, TEST_NO_TELP);
        tentor = createTentor(mahasiswa, TEST_IPK, TEST_PENGALAMAN_STRING, VerificationStatus.APPROVED);
    }
    
    private TentorDTO createTentorDTO(String nim, String nama, String email, String password,
                                      String noTelp, double ipk, List<String> pengalaman) {
        TentorDTO dto = new TentorDTO();
        dto.setNim(nim);
        dto.setNama(nama);
        dto.setEmail(email);
        dto.setPassword(password);
        dto.setNoTelp(noTelp);
        dto.setIpk(ipk);
        dto.setPengalaman(pengalaman);
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
    
    private Tentor createTentor(Mahasiswa mahasiswa, double ipk, String pengalaman, VerificationStatus status) {
        Tentor t = new Tentor();
        t.setMahasiswa(mahasiswa);
        t.setIpk(ipk);
        t.setPengalaman(pengalaman);
        t.setVerificationStatus(status);
        return t;
    }

    @Test
    void login_Success(){
        // Arrange
        when(tentorRepository.findByMahasiswaEmail(TEST_EMAIL)).thenReturn(tentor);

        // Act & Assert
        assertDoesNotThrow(() -> tentorService.login(TEST_EMAIL, TEST_PASSWORD));
    }

    @Test
    void login_UserNotFound() {
        // Arrange
        when(tentorRepository.findByMahasiswaEmail(NONEXISTENT_EMAIL)).thenReturn(null);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> tentorService.login(NONEXISTENT_EMAIL, TEST_PASSWORD));
        assertEquals(ERROR_INVALID_CREDENTIALS, exception.getMessage());
    }

    @Test
    void login_InvalidPassword() {
        // Arrange
        when(tentorRepository.findByMahasiswaEmail(TEST_EMAIL)).thenReturn(tentor);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> tentorService.login(TEST_EMAIL, WRONG_PASSWORD));
        assertEquals(ERROR_INVALID_CREDENTIALS, exception.getMessage());
    }

    @Test
    void login_NotApproved() {
        // Arrange
        Tentor pendingTentor = createTentor(mahasiswa, TEST_IPK, TEST_PENGALAMAN_STRING, VerificationStatus.PENDING);
        when(tentorRepository.findByMahasiswaEmail(TEST_EMAIL)).thenReturn(pendingTentor);

        // Act & Assert
        assertDoesNotThrow(() -> tentorService.login(TEST_EMAIL, TEST_PASSWORD));
    }

    @Test
    void register_Success() {
        // Arrange
        when(mahasiswaRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
        when(mahasiswaRepository.existsByNim(TEST_NIM)).thenReturn(false);
        when(mahasiswaRepository.save(any(Mahasiswa.class))).thenReturn(mahasiswa);
        when(tentorRepository.save(any(Tentor.class))).thenReturn(tentor);

        // Act & Assert
        assertDoesNotThrow(() -> tentorService.register(tentorDTO));
        verify(mahasiswaRepository).save(any(Mahasiswa.class));
        verify(tentorRepository).save(any(Tentor.class));
    }

    @Test
    void register_EmailAlreadyExists() {
        // Arrange
        TentorDTO existingEmailDTO = createTentorDTO(ALT_NIM, TEST_NAMA, ALT_EMAIL_EXISTING,
                                                      TEST_PASSWORD, TEST_NO_TELP, TEST_IPK, TEST_PENGALAMAN_LIST);
        when(mahasiswaRepository.existsByEmail(ALT_EMAIL_EXISTING)).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> tentorService.register(existingEmailDTO));
        assertEquals(ERROR_EMAIL_EXISTS, exception.getMessage());
    }

    @Test
    void register_NimAlreadyExists() {
        // Arrange
        TentorDTO existingNimDTO = createTentorDTO(ALT_NIM, TEST_NAMA, ALT_EMAIL_TESTING,
                                                    TEST_PASSWORD, TEST_NO_TELP, TEST_IPK, TEST_PENGALAMAN_LIST);
        when(mahasiswaRepository.existsByEmail(ALT_EMAIL_TESTING)).thenReturn(false);
        when(mahasiswaRepository.existsByNim(ALT_NIM)).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> tentorService.register(existingNimDTO));
        assertEquals(ERROR_NIM_EXISTS, exception.getMessage());
    }
}

