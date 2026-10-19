package com.ctu.service;

import com.ctu.exception.PaymentFailedException;
import com.ctu.exception.RoomNotAvailableException;
import com.ctu.model.BookingReceipt;
import com.ctu.model.Room;
import com.ctu.repository.RoomRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingService Unit Test Suite")
class BookingServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private PaymentGateway paymentGateway;

    @InjectMocks
    private BookingService bookingService;

    @Test
    @DisplayName("Đặt phòng thành công khi thông tin hợp lệ và phòng còn trống")
    void testBookRoom_Success() {
        Room room = new Room("R101", 100.0, true);
        when(roomRSepository.findById("R101")).thenReturn(Optional.of(room));
        when(paymentGateway.charge("1234-5678", 200.0)).thenReturn(true);

        BookingReceipt receipt = bookingService.bookRoom("R101", 2, "1234-5678");

        assertNotNull(receipt);
        assertEquals("R101", receipt.getRoomId());
        assertEquals(200.0, receipt.getTotalAmount());
        assertFalse(room.isAvailable());

        verify(paymentGateway, times(1)).charge("1234-5678", 200.0);
        verify(roomRepository, times(1)).save(room);
    }

    @Test
    @DisplayName("Ném RoomNotAvailableException khi không tìm thấy phòng")
    void testBookRoom_RoomNotFound_ThrowsException() {
        when(roomRepository.findById("R999")).thenReturn(Optional.empty());

        assertThrows(RoomNotAvailableException.class, () -> 
            bookingService.bookRoom("R999", 1, "1234-5678")
        );
        verify(paymentGateway, never()).charge(anyString(), anyDouble());
    }

    @Test
    @DisplayName("Ném RoomNotAvailableException khi phòng đã được đặt trước")
    void testBookRoom_RoomAlreadyBooked_ThrowsException() {
        Room occupiedRoom = new Room("R101", 100.0, false);
        when(roomRepository.findById("R101")).thenReturn(Optional.of(occupiedRoom));

        assertThrows(RoomNotAvailableException.class, () -> 
            bookingService.bookRoom("R101", 1, "1234-5678")
        );
        verify(paymentGateway, never()).charge(anyString(), anyDouble());
    }

    @Test
    @DisplayName("Ném PaymentFailedException khi thanh toán thất bại")
    void testBookRoom_PaymentFailed_ThrowsException() {
        Room room = new Room("R101", 100.0, true);
        when(roomRepository.findById("R101")).thenReturn(Optional.of(room));
        when(paymentGateway.charge(anyString(), anyDouble())).thenReturn(false);

        assertThrows(PaymentFailedException.class, () -> 
            bookingService.bookRoom("R101", 1, "1234-5678")
        );
        verify(roomRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném IllegalArgumentException khi số đêm nights <= 0")
    void testBookRoom_InvalidNights_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> 
            bookingService.bookRoom("R101", 0, "1234-5678")
        );
        assertThrows(IllegalArgumentException.class, () -> 
            bookingService.bookRoom("R101", -1, "1234-5678")
        );
    }

    @Test
    @DisplayName("Ném IllegalArgumentException khi creditCard null hoặc rỗng")
    void testBookRoom_NullOrEmptyCreditCard_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> 
            bookingService.bookRoom("R101", 1, null)
        );
        assertThrows(IllegalArgumentException.class, () -> 
            bookingService.bookRoom("R101", 1, "   ")
        );
    }
}