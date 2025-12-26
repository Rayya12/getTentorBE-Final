package com.atomic.getTentor.service;

import com.atomic.getTentor.controller.ReviewController;
import com.atomic.getTentor.dto.ReviewDTO;
import com.atomic.getTentor.model.Mahasiswa;
import com.atomic.getTentor.model.Mentee;
import com.atomic.getTentor.model.Review;
import com.atomic.getTentor.model.Tentor;
import com.atomic.getTentor.service.ReviewService;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReviewService reviewService;

    // Mock security bean yang biasa dibutuhkan karena ada JwtAuthFilter
    @MockBean(name = "jwtService")
    private com.atomic.getTentor.security.JwtService jwtService;

    @Test
    void simpanReview_return200_whenSuccess() throws Exception {
        // ===== Arrange (data request) =====
        String requestJson = """
            {
              "menteeId": 1,
              "tentorId": 10,
              "rating": 5,
              "komentar": "Penjelasan jelas, fast respon, sangat membantu."
            }
        """;

        // ===== Arrange (mock response dari service) =====
        // ReviewDTO di project kamu ambil createdAt dari Review model,
        // jadi paling gampang bikin Review model dummy lalu new ReviewDTO(review).
        Mahasiswa mhs = new Mahasiswa();
        mhs.setNim("1302210001");
        mhs.setNama("Budi");
        mhs.setPassword("aku_ganteng");

        Mentee mentee = new Mentee();
        mentee.setMahasiswa(mhs);

        Tentor tentor = new Tentor(); // ReviewDTO kamu tidak expose tentor, jadi cukup dummy

        Review review = new Review(mentee, tentor, "Penjelasan jelas, fast respon, sangat membantu.", 5);
        review.setId(1);
        review.setCreatedAt(LocalDateTime.of(2025, 12, 26, 15, 0, 0));

        ReviewDTO mockSavedDto = new ReviewDTO(review);

        when(reviewService.simpanReview(any())).thenReturn(mockSavedDto);

        mockMvc.perform(
                post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
        )
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.rating").value(5))
        .andExpect(jsonPath("$.komentar").value("Penjelasan jelas, fast respon, sangat membantu."))
        .andExpect(jsonPath("$.reviewerNama").value("Budi"))
        .andExpect(jsonPath("$.reviewerNim").value("1302210001"))
        .andExpect(jsonPath("$.createdAt").value("2025-12-26 15:00:00"));
    }

@Test
void simpanReview_throwServletException_whenDuplicateReview() throws Exception {
    String requestJson = """
        {
          "menteeId": 1,
          "tentorId": 10,
          "rating": 4,
          "komentar": "Sudah pernah review sebelumnya."
        }
    """;

    when(reviewService.simpanReview(any()))
            .thenThrow(new IllegalArgumentException("Review already exists for this tentor by this mentee"));

    Assertions.assertThrows(jakarta.servlet.ServletException.class, () -> {
        mockMvc.perform(post("/api/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        ).andReturn();
    });
}
}
