package com.atomic.getTentor.Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.atomic.getTentor.controller.TentorController;
import com.atomic.getTentor.dto.MataKuliahDTO;
import com.atomic.getTentor.dto.TentorDTO;
import com.atomic.getTentor.security.JwtService;
import com.atomic.getTentor.service.TentorService;

@ExtendWith(MockitoExtension.class)
public class UpdateProfileTentorTest {

    @Mock
    private TentorService tentorService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private TentorController tentorController;

    private TentorDTO tentorDTO;
    private MultipartFile mockFile;

    // --- KONSTANTA DATA ---
    private final String TOKEN_HEADER = "Bearer valid-token";
    private final String PURE_TOKEN = "valid-token";
    private final String EMAIL = "tentor@example.com";

    // --- KONSTANTA ERROR ---
    private final String ERR_NAMA_INVALID = "Nama lengkap hanya boleh berisi huruf dan spasi.";
    private final String ERR_TELP_INVALID = "Nomor telepon tidak valid. Gunakan 10-13 digit angka.";
    private final String ERR_IPK_INVALID  = "IPK harus berupa angka antara 0.00 hingga 4.00 (gunakan titik sebagai pemisah desimal).";
    private final String ERR_USER_NOT_FOUND = "Tentor tidak ditemukan";

    @BeforeEach
    void setup() {
        // --- SETUP DATA LENGKAP TENTOR ---
        tentorDTO = new TentorDTO();
        tentorDTO.setNama("Tentor Profesional");
        tentorDTO.setNoTelp("081234567890");
        tentorDTO.setEmail(EMAIL);
        tentorDTO.setIpk(3.85); // Set IPK
        
        // Setup Pengalaman (List String)
        List<String> pengalaman = new ArrayList<>();
        pengalaman.add("Juara 1 Lomba Coding");
        pengalaman.add("Asisten Dosen 2023");
        tentorDTO.setPengalaman(pengalaman);

        // Setup Mata Kuliah (List Object DTO)
        List<MataKuliahDTO> matkul = new ArrayList<>();
        MataKuliahDTO mk1 = new MataKuliahDTO(); mk1.setId(1); mk1.setNama("Algoritma");
        MataKuliahDTO mk2 = new MataKuliahDTO(); mk2.setId(2); mk2.setNama("Struktur Data");
        matkul.add(mk1);
        matkul.add(mk2);
        tentorDTO.setListMataKuliah(matkul);

        mockFile = mock(MultipartFile.class);
    }

    // ==========================================
    // 1. POSITIVE CASES (SUKSES)
    // ==========================================

    @Test
    void updateProfile_Sukses() throws Exception {
        when(jwtService.getEmailFromToken(PURE_TOKEN)).thenReturn(EMAIL);
        ResponseEntity<?> response = tentorController.updateProfile(TOKEN_HEADER, tentorDTO, mockFile);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertEquals("Profil berhasil diperbarui", body.get("message"));
        verify(tentorService).updateTentorProfile(eq(EMAIL), eq(tentorDTO), eq(mockFile));
    }

    @Test
    void updateProfile_Gagal_NamaInvalid() throws Exception {
        tentorDTO.setNama(""); 
        when(jwtService.getEmailFromToken(PURE_TOKEN)).thenReturn(EMAIL);
        doThrow(new RuntimeException(ERR_NAMA_INVALID))
            .when(tentorService).updateTentorProfile(eq(EMAIL), eq(tentorDTO), any());
        ResponseEntity<?> response = tentorController.updateProfile(TOKEN_HEADER, tentorDTO, mockFile);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(ERR_NAMA_INVALID, ((Map)response.getBody()).get("error"));
    }

    @Test
    void updateProfile_Gagal_NoTelpInvalid() throws Exception {
        tentorDTO.setNoTelp("abc"); 
        when(jwtService.getEmailFromToken(PURE_TOKEN)).thenReturn(EMAIL);
        doThrow(new RuntimeException(ERR_TELP_INVALID))
            .when(tentorService).updateTentorProfile(eq(EMAIL), eq(tentorDTO), any());
        ResponseEntity<?> response = tentorController.updateProfile(TOKEN_HEADER, tentorDTO, mockFile);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(ERR_TELP_INVALID, ((Map)response.getBody()).get("error"));
    }

    @Test
    void updateProfile_Gagal_IPKInvalid() throws Exception {
        tentorDTO.setIpk(5.00); 
        when(jwtService.getEmailFromToken(PURE_TOKEN)).thenReturn(EMAIL);
        doThrow(new RuntimeException(ERR_IPK_INVALID))
            .when(tentorService).updateTentorProfile(eq(EMAIL), eq(tentorDTO), any());
        ResponseEntity<?> response = tentorController.updateProfile(TOKEN_HEADER, tentorDTO, mockFile);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(ERR_IPK_INVALID, ((Map)response.getBody()).get("error"));
    }

    @Test
    void updateProfile_Gagal_UserTidakDitemukan() throws Exception {
        when(jwtService.getEmailFromToken(PURE_TOKEN)).thenReturn(EMAIL);
        doThrow(new RuntimeException(ERR_USER_NOT_FOUND))
            .when(tentorService).updateTentorProfile(eq(EMAIL), eq(tentorDTO), any());

        ResponseEntity<?> response = tentorController.updateProfile(TOKEN_HEADER, tentorDTO, mockFile);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(ERR_USER_NOT_FOUND, ((Map)response.getBody()).get("error"));
    }
}