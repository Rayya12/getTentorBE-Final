package com.atomic.getTentor.Controller;

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

import com.atomic.getTentor.controller.MenteeController;
import com.atomic.getTentor.dto.MenteeDTO;
import com.atomic.getTentor.security.JwtService;
import com.atomic.getTentor.service.MenteeService;

@ExtendWith(MockitoExtension.class)
public class UpdateProfileMenteeTest {

    @Mock
    private MenteeService menteeService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private MenteeController menteeController;

    private MenteeDTO menteeDTO;
    private MultipartFile mockFile;
    

    private final String TOKEN_HEADER = "Bearer valid-token";
    private final String PURE_TOKEN = "valid-token";
    private final String EMAIL = "seixin25@gmail.com";

    private final String ERR_NAMA_INVALID = "Nama lengkap hanya boleh berisi huruf dan spasi.";
    private final String ERR_TELP_INVALID = "Nomor telepon tidak valid. Gunakan 10-13 digit angka.";
    private final String ERR_USER_NOT_FOUND = "Mentee tidak ditemukan";

    @BeforeEach
    void setup() {
        menteeDTO = new MenteeDTO();
        menteeDTO.setNama("Seixin"); 
        menteeDTO.setNoTelp("0893927372");
        menteeDTO.setEmail(EMAIL);
        
        mockFile = mock(MultipartFile.class);
    }

    @Test
    void updateProfile_Sukses() throws Exception {
        when(jwtService.getEmailFromToken(PURE_TOKEN)).thenReturn(EMAIL);
        ResponseEntity<?> response = menteeController.updateProfile(TOKEN_HEADER, menteeDTO, mockFile);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertEquals("Profil berhasil diperbarui", body.get("message"));
        verify(menteeService).updateMenteeProfile(eq(EMAIL), eq(menteeDTO), eq(mockFile));
    }

    @Test
    void updateProfile_Gagal_NamaInvalid() throws Exception {
        menteeDTO.setNama(""); 
        when(jwtService.getEmailFromToken(PURE_TOKEN)).thenReturn(EMAIL);
        doThrow(new RuntimeException(ERR_NAMA_INVALID))
            .when(menteeService).updateMenteeProfile(eq(EMAIL), eq(menteeDTO), any());
        ResponseEntity<?> response = menteeController.updateProfile(TOKEN_HEADER, menteeDTO, mockFile);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(ERR_NAMA_INVALID, ((Map)response.getBody()).get("error"));
    }

    @Test
    void updateProfile_Gagal_NoTelpInvalid() throws Exception {
        menteeDTO.setNoTelp("123"); 
        when(jwtService.getEmailFromToken(PURE_TOKEN)).thenReturn(EMAIL);
        doThrow(new RuntimeException(ERR_TELP_INVALID))
            .when(menteeService).updateMenteeProfile(eq(EMAIL), eq(menteeDTO), any());
        ResponseEntity<?> response = menteeController.updateProfile(TOKEN_HEADER, menteeDTO, mockFile);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(ERR_TELP_INVALID, ((Map)response.getBody()).get("error"));
    }

    @Test
    void updateProfile_Gagal_UserTidakDitemukan() throws Exception {
        when(jwtService.getEmailFromToken(PURE_TOKEN)).thenReturn(EMAIL);
        doThrow(new RuntimeException(ERR_USER_NOT_FOUND))
            .when(menteeService).updateMenteeProfile(eq(EMAIL), eq(menteeDTO), any());
        ResponseEntity<?> response = menteeController.updateProfile(TOKEN_HEADER, menteeDTO, mockFile);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(ERR_USER_NOT_FOUND, ((Map)response.getBody()).get("error"));
    }
}