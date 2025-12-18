package com.atomic.getTentor.service;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.atomic.getTentor.model.Admin;
import com.atomic.getTentor.model.Mahasiswa;
import com.atomic.getTentor.model.Tentor;
import com.atomic.getTentor.model.VerificationStatus;
import com.atomic.getTentor.repository.AdminRepository;
import com.atomic.getTentor.repository.TentorRepository;

@ExtendWith(MockitoExtension.class)
public class AdminServiceTest {

    // Test data constants
    private static final String TEST_NIM = "123456";
    private static final String TEST_NAMA = "Test Admin";
    private static final String TEST_EMAIL = "admin@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String TEST_NO_TELP = "08123456789";
    private static final Integer TEST_TENTOR_ID = 1;
    private static final String TEST_STATUS = "APPROVED";
    private static final String INVALID_STATUS = "INVALID";
    private static final String TEST_QUERY = "test";
    private static final double TEST_IPK = 3.5;
    private static final String TEST_PENGALAMAN = "Teaching Experience";

    // Alternative test data constants
    private static final String NONEXISTENT_EMAIL = "nonexistent@example.com";
    private static final String WRONG_PASSWORD = "wrongpassword";
    private static final Integer NONEXISTENT_TENTOR_ID = 999;

    // Error message constants
    private static final String ERROR_INVALID_CREDENTIALS = "Invalid email or password";
    private static final String ERROR_TENTOR_NOT_FOUND = "Tentor not found";
    private static final String ERROR_INVALID_STATUS = "Status tidak valid. Gunakan: PENDING, VERIFIED, REJECTED, SUSPENDED";

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private TentorRepository tentorRepository;

    @InjectMocks
    private AdminService adminService;

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private Mahasiswa mahasiswa;
    private Admin admin;
    private Tentor tentor;
    private List<Tentor> tentorList;

    @BeforeEach
    void setUpAccount() {
        mahasiswa = createMahasiswa(TEST_NIM, TEST_NAMA, TEST_EMAIL, TEST_PASSWORD, TEST_NO_TELP);
        admin = createAdmin(mahasiswa);
        tentor = createTentor(TEST_TENTOR_ID, mahasiswa, TEST_IPK, TEST_PENGALAMAN, VerificationStatus.PENDING);
        tentorList = Arrays.asList(tentor);
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

    private Admin createAdmin(Mahasiswa mahasiswa) {
        Admin adm = new Admin();
        adm.setMahasiswa(mahasiswa);
        return adm;
    }

    private Tentor createTentor(Integer id, Mahasiswa mahasiswa, double ipk, String pengalaman, VerificationStatus status) {
        Tentor t = new Tentor();
        t.setMahasiswa(mahasiswa);
        t.setIpk(ipk);
        t.setPengalaman(pengalaman);
        t.setVerificationStatus(status);
        return t;
    }

    @Test
    void login_Success() {
        // Arrange
        when(adminRepository.findByMahasiswaEmail(TEST_EMAIL)).thenReturn(admin);

        // Act & Assert
        assertDoesNotThrow(() -> adminService.login(TEST_EMAIL, TEST_PASSWORD));
    }

    @Test
    void login_UserNotFound() {
        // Arrange
        when(adminRepository.findByMahasiswaEmail(NONEXISTENT_EMAIL)).thenReturn(null);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> adminService.login(NONEXISTENT_EMAIL, TEST_PASSWORD));
        assertEquals(ERROR_INVALID_CREDENTIALS, exception.getMessage());
    }

    @Test
    void login_InvalidPassword() {
        // Arrange
        when(adminRepository.findByMahasiswaEmail(TEST_EMAIL)).thenReturn(admin);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> adminService.login(TEST_EMAIL, WRONG_PASSWORD));
        assertEquals(ERROR_INVALID_CREDENTIALS, exception.getMessage());
    }
}