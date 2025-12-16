package com.atomic.getTentor.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.atomic.getTentor.model.Tentor;
import com.atomic.getTentor.model.VerificationStatus;
import com.atomic.getTentor.repository.TentorRepository;

@ExtendWith(MockitoExtension.class)
public class TentorStatusChange {

    // constants biar rapi & konsisten
    private static final Integer EXISTING_ID = 1;
    private static final Integer NON_EXISTING_ID = 999;
    private static final VerificationStatus STATUS_APPROVED = VerificationStatus.APPROVED;
    private static final VerificationStatus STATUS_PENDING = VerificationStatus.PENDING;
    private static final VerificationStatus STATUS_REJECTED = VerificationStatus.REJECTED;
    private static final VerificationStatus STATUS_SUSPENDED = VerificationStatus.SUSPENDED;

    @Mock
    private TentorRepository tentorRepository;

    @InjectMocks
    private AdminService adminService; 

    private Tentor tentor;

    @BeforeEach
    void setUp() {
        tentor = createTentor(STATUS_PENDING);
    }

    private Tentor createTentor(VerificationStatus status) {
        Tentor t = new Tentor();
        t.setVerificationStatus(status);
        return t;
    }

    @Test
    void ubahStatus_Success_UpdateStatus() {
        // Arrange: tentor ada
        when(tentorRepository.findById(EXISTING_ID)).thenReturn(Optional.of(tentor));
        when(tentorRepository.save(any(Tentor.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Tentor updated = assertDoesNotThrow(() -> adminService.ubahStatus(EXISTING_ID, "APPROVED"));

        // Assert
        assertNotNull(updated);
        assertEquals(STATUS_APPROVED, updated.getVerificationStatus());

        verify(tentorRepository).findById(EXISTING_ID);
        verify(tentorRepository).save(any(Tentor.class));
    }

    @Test
    void ubahStatus_TentorNotFound_Throws() {
        // Arrange: tentor gak ada
        when(tentorRepository.findById(NON_EXISTING_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class,
                () -> adminService.ubahStatus(NON_EXISTING_ID, "APPROVED"));

        verify(tentorRepository).findById(NON_EXISTING_ID);
        verify(tentorRepository, never()).save(any());
    }



    @Test
    void ubahStatus_Success_UpdateToNonaktif() {
        when(tentorRepository.findById(EXISTING_ID)).thenReturn(Optional.of(tentor));
        when(tentorRepository.save(any(Tentor.class))).thenAnswer(inv -> inv.getArgument(0));

        Tentor updated = assertDoesNotThrow(() -> adminService.ubahStatus(EXISTING_ID, "PENDING"));

        assertEquals(STATUS_PENDING, updated.getVerificationStatus());
        verify(tentorRepository).save(any(Tentor.class));
    }

    @Test
    void ubahStatus_Success_UpdateToRejected() {
        when(tentorRepository.findById(EXISTING_ID)).thenReturn(Optional.of(tentor));
        when(tentorRepository.save(any(Tentor.class))).thenAnswer(inv -> inv.getArgument(0));

        Tentor updated = assertDoesNotThrow(() -> adminService.ubahStatus(EXISTING_ID, "REJECTED"));

        assertEquals(STATUS_REJECTED, updated.getVerificationStatus());
        verify(tentorRepository).save(any(Tentor.class));
    }

    @Test
    void ubahStatus_Success_UpdateToSuspended() {
        when(tentorRepository.findById(EXISTING_ID)).thenReturn(Optional.of(tentor));
        when(tentorRepository.save(any(Tentor.class))).thenAnswer(inv -> inv.getArgument(0));

        Tentor updated = assertDoesNotThrow(() -> adminService.ubahStatus(EXISTING_ID, "SUSPENDED"));

        assertEquals(STATUS_SUSPENDED, updated.getVerificationStatus());
        verify(tentorRepository).save(any(Tentor.class));
    }

}
