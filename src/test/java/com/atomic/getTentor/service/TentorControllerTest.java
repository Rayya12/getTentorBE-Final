package com.atomic.getTentor.service;

import com.atomic.getTentor.controller.TentorController;
import com.atomic.getTentor.model.Mahasiswa;
import com.atomic.getTentor.model.Tentor;
import com.atomic.getTentor.model.VerificationStatus;
import com.atomic.getTentor.repository.TentorRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TentorController.class)
@AutoConfigureMockMvc(addFilters = false)
class TentorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private com.atomic.getTentor.service.TentorService tentorService;

    @MockBean
    private TentorRepository tentorRepository;

    @MockBean
    private com.atomic.getTentor.service.AdminService adminService;

    @MockBean(name = "jwtService")
    private com.atomic.getTentor.security.JwtService jwtService;

    @Test
    void getTentorById_return200_whenFound() throws Exception {
        Integer tentorId = 10;

        Mahasiswa mhs = new Mahasiswa();
        mhs.setNim("1302210001");
        mhs.setNama("Aulia");
        mhs.setEmail("aulia@mail.com");
        mhs.setPassword("secret123");
        mhs.setFotoUrl("https://cdn.example.com/aulia.png");
        mhs.setNoTelp("081234567890");

        Tentor tentor = new Tentor();
        tentor.setMahasiswa(mhs);
        tentor.setIpk(3.75);
        tentor.setPengalaman("Asisten Praktikum|Freelance Tutor");
        tentor.setVerificationStatus(VerificationStatus.APPROVED);
        tentor.setFavorite(3);

        when(tentorRepository.findWithMataKuliahById(tentorId))
                .thenReturn(Optional.of(tentor));

        mockMvc.perform(get("/api/tentors/{id}", tentorId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.nim").value("1302210001"))
                .andExpect(jsonPath("$.nama").value("Aulia"))
                .andExpect(jsonPath("$.email").value("aulia@mail.com"))
                .andExpect(jsonPath("$.ipk").value(3.75))
                .andExpect(jsonPath("$.pengalaman").isArray())
                .andExpect(jsonPath("$.pengalaman.length()").value(2))
                .andExpect(jsonPath("$.pengalaman[0]").value("Asisten Praktikum"))
                .andExpect(jsonPath("$.pengalaman[1]").value("Freelance Tutor"))
                .andExpect(jsonPath("$.fotoUrl").value("https://cdn.example.com/aulia.png"))
                .andExpect(jsonPath("$.noTelp").value("081234567890"))
                .andExpect(jsonPath("$.verificationStatus").value("APPROVED"))
                .andExpect(jsonPath("$.countFavorite").value(3))
                .andExpect(jsonPath("$.listReview").isArray())
                .andExpect(jsonPath("$.listReview.length()").value(0))
                .andExpect(jsonPath("$.listMataKuliah").isArray())
                .andExpect(jsonPath("$.listMataKuliah.length()").value(0))
                .andExpect(jsonPath("$.averageRating").value(0.0))
                .andExpect(jsonPath("$.ratingCount").value(0));
    }

    @Test
    void getTentorById_return404_whenNotFound() throws Exception {
        Integer tentorId = 999;

        when(tentorRepository.findWithMataKuliahById(tentorId))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/tentors/{id}", tentorId))
                .andExpect(status().isNotFound());
    }
}
