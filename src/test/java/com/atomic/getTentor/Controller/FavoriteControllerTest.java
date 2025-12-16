package com.atomic.getTentor.Controller;

import com.atomic.getTentor.controller.FavoriteController;
import com.atomic.getTentor.model.Favorite;
import com.atomic.getTentor.model.Mahasiswa;
import com.atomic.getTentor.model.Mentee;
import com.atomic.getTentor.model.Tentor;
import com.atomic.getTentor.repository.FavoriteRepository;
import com.atomic.getTentor.repository.MenteeRepository;
import com.atomic.getTentor.repository.TentorRepository;

import io.micrometer.common.lang.NonNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FavoriteController.class)
@AutoConfigureMockMvc(addFilters = false)
class FavoriteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FavoriteRepository favoriteRepository;

    @MockBean
    private MenteeRepository menteeRepository;

    @MockBean
    private TentorRepository tentorRepository;
    
    // Mock semua security beans yang dibutuhkan
    @MockBean(name = "jwtService")
    private com.atomic.getTentor.security.JwtService jwtService;
    

    @Test
    void getFavoritesByMentee_return200_withListOfTentors() throws Exception {
        Integer menteeId = 1;

        Mahasiswa mahasiswa1 = new Mahasiswa();
        Mahasiswa mahasiswa2 = new Mahasiswa();

        Mentee mentee = new Mentee();

        Tentor tentor1 = new Tentor();
        tentor1.setMahasiswa(mahasiswa1);
        tentor1.setPassword("tahu12345");
        tentor1.getMahasiswa().setNama("My Wife");

        Tentor tentor2 = new Tentor();
        tentor2.setMahasiswa(mahasiswa2);
        tentor2.getMahasiswa().setNama("Olivia Rodrigo");
        tentor2.setPassword("tahu12345");

        Favorite fav1 = new Favorite();
        fav1.setMentee(mentee);
        fav1.setTentor(tentor1);

        Favorite fav2 = new Favorite();
        fav2.setMentee(mentee);
        fav2.setTentor(tentor2);

        List<Favorite> favorites = Arrays.asList(fav1, fav2);

        when(favoriteRepository.findByMentee_Id(eq(menteeId))).thenReturn(favorites);

        mockMvc.perform(get("/api/favorites/{menteeId}", menteeId)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nama").value("My Wife"))
                .andExpect(jsonPath("$[1].nama").value("Olivia Rodrigo"));

        verify(favoriteRepository, times(1)).findByMentee_Id(eq(menteeId));
    }

    @Test
    void getFavoritesByMentee_return200_withEmptyList_whenNoFavorites() throws Exception {
        Integer menteeId = 99;

        when(favoriteRepository.findByMentee_Id(eq(menteeId))).thenReturn(List.of());

        mockMvc.perform(get("/api/favorites/{menteeId}", menteeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }


    @Test
    void addFavorite_return201_whenSuccess() throws Exception {
        Integer menteeId = 1;
        Integer tentorId = 2;

        Mentee mentee = new Mentee();
        
        Tentor tentor = new Tentor();

        when(favoriteRepository.existsByMentee_IdAndTentor_Id(eq(menteeId), eq(tentorId))).thenReturn(false);
        when(menteeRepository.findById(eq(menteeId))).thenReturn(Optional.of(mentee));
        when(tentorRepository.findById(eq(tentorId))).thenReturn(Optional.of(tentor));
        when(favoriteRepository.save(any(Favorite.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/favorites")
                        .param("menteeId", menteeId.toString())
                        .param("tentorId", tentorId.toString()))
                .andExpect(status().isCreated());

        verify(favoriteRepository, times(1)).save(any(Favorite.class));
    }

    @Test
    void addFavorite_return409_whenFavoriteAlreadyExists() throws Exception {
        Integer menteeId = 1;
        Integer tentorId = 2;

        when(favoriteRepository.existsByMentee_IdAndTentor_Id(eq(menteeId), eq(tentorId))).thenReturn(true);

        mockMvc.perform(post("/api/favorites")
                        .param("menteeId", menteeId.toString())
                        .param("tentorId", tentorId.toString()))
                .andExpect(status().isConflict())
                .andExpect(status().reason("Favorite sudah ada"));

        verify(favoriteRepository, never()).save(any(Favorite.class));
    }

    @Test
    void addFavorite_return404_whenMenteeNotFound() throws Exception {
        Integer menteeId = 999;
        Integer tentorId = 2;

        when(favoriteRepository.existsByMentee_IdAndTentor_Id(eq(menteeId), eq(tentorId))).thenReturn(false);
        when(menteeRepository.findById(eq(menteeId))).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/favorites")
                        .param("menteeId", menteeId.toString())
                        .param("tentorId", tentorId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(status().reason("Mentee tidak ditemukan"));

        verify(favoriteRepository, never()).save(any(Favorite.class));
    }

    @Test
    void addFavorite_return404_whenTentorNotFound() throws Exception {
        Integer menteeId = 1;
        Integer tentorId = 999;

        Mentee mentee = new Mentee();

        when(favoriteRepository.existsByMentee_IdAndTentor_Id(eq(menteeId), eq(tentorId))).thenReturn(false);
        when(menteeRepository.findById(eq(menteeId))).thenReturn(Optional.of(mentee));
        when(tentorRepository.findById(eq(tentorId))).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/favorites")
                        .param("menteeId", menteeId.toString())
                        .param("tentorId", tentorId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(status().reason("Tentor tidak ditemukan"));

        verify(favoriteRepository, never()).save(any(Favorite.class));
    }

    @Test
    void removeFavorite_return204_whenSuccess() throws Exception {
        Integer menteeId = 1;
        Integer tentorId = 2;

        Tentor tentor = new Tentor();
        tentor.setFavorite(5);

        when(favoriteRepository.existsByMentee_IdAndTentor_Id(eq(menteeId), eq(tentorId))).thenReturn(true);
        when(tentorRepository.findById(eq(tentorId))).thenReturn(Optional.of(tentor));
        when(tentorRepository.save(any(Tentor.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(favoriteRepository).deleteByMentee_IdAndTentor_Id(eq(menteeId), eq(tentorId));

        mockMvc.perform(delete("/api/favorites")
                        .param("menteeId", menteeId.toString())
                        .param("tentorId", tentorId.toString()))
                .andExpect(status().isNoContent());

        verify(favoriteRepository, times(1)).deleteByMentee_IdAndTentor_Id(eq(menteeId), eq(tentorId));
        verify(tentorRepository, times(1)).save(argThat(t -> t.getCountFavorite() == 4));
    }

    @Test
    void removeFavorite_return404_whenFavoriteNotExists() throws Exception {
        Integer menteeId = 1;
        Integer tentorId = 999;

        when(favoriteRepository.existsByMentee_IdAndTentor_Id(eq(menteeId), eq(tentorId))).thenReturn(false);

        mockMvc.perform(delete("/api/favorites")
                        .param("menteeId", menteeId.toString())
                        .param("tentorId", tentorId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(status().reason("Favorite tidak ditemukan"));

        verify(favoriteRepository, never()).deleteByMentee_IdAndTentor_Id(anyInt(), anyInt());
    }

    @Test
    void removeFavorite_return500_whenTentorNotFound() throws Exception {
        Integer menteeId = 1;
        Integer tentorId = 999;

        when(favoriteRepository.existsByMentee_IdAndTentor_Id(eq(menteeId), eq(tentorId))).thenReturn(true);
        when(tentorRepository.findById(eq(tentorId))).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/favorites")
                        .param("menteeId", menteeId.toString())
                        .param("tentorId", tentorId.toString()))
                .andExpect(status().isNotFound());

        verify(favoriteRepository, never()).deleteByMentee_IdAndTentor_Id(anyInt(), anyInt());
    }
}