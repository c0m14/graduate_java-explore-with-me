package ru.practicum.ewm.main.request.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.main.TestDataProvider;
import ru.practicum.ewm.main.category.model.Category;
import ru.practicum.ewm.main.event.model.Event;
import ru.practicum.ewm.main.event.model.EventState;
import ru.practicum.ewm.main.event.repository.EventRepository;
import ru.practicum.ewm.main.exception.ForbiddenException;
import ru.practicum.ewm.main.exception.NotExistsException;
import ru.practicum.ewm.main.request.dto.EventRequestStatusUpdateRequestDto;
import ru.practicum.ewm.main.request.dto.RequestStatusUpdateDto;
import ru.practicum.ewm.main.request.model.EventParticipationRequest;
import ru.practicum.ewm.main.request.model.RequestStatus;
import ru.practicum.ewm.main.request.repository.RequestRepository;
import ru.practicum.ewm.main.user.model.User;
import ru.practicum.ewm.main.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestServiceImplUnitTests {

    @Mock
    private RequestRepository requestRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private RequestServiceImpl requestService;

    @Captor
    private ArgumentCaptor<EventParticipationRequest> requestArgumentCaptor;
    @Captor
    private ArgumentCaptor<Long> eventIdArgumentCaptor;
    @Captor
    private ArgumentCaptor<EventRequestStatusUpdateRequestDto> updateStatusesArgumentCaptor;

    @Test
    void addRequest_whenUserNotFound_thenNotExistExceptionThrown() {
        Long userId = 0L;
        Long eventId = 1L;
        when(userRepository.findUserById(userId))
                .thenReturn(Optional.empty());

        Executable executable = () -> requestService.addRequest(userId, eventId);

        assertThrows(NotExistsException.class, executable);
    }

    @Test
    void addRequest_whenEventNotFound_thenNotExistExceptionThrown() {
        Long userId = 0L;
        Long eventId = 1L;
        when(userRepository.findUserById(userId))
                .thenReturn(Optional.of(TestDataProvider.getValidUserToSave()));
        when(eventRepository.findEventByIdWithoutCategory(eventId))
                .thenReturn(Optional.empty());

        Executable executable = () -> requestService.addRequest(userId, eventId);

        assertThrows(NotExistsException.class, executable);
    }

    @Test
    void addRequest_whenUserIsEventOwner_thenForbiddenExceptionThrown() {
        Long userId = 0L;
        Long eventId = 1L;
        User user = TestDataProvider.getValidUserToSave();
        user.setId(userId);
        Event event = TestDataProvider.getValidNotSavedEvent(user, new Category());
        when(userRepository.findUserById(userId))
                .thenReturn(Optional.of(TestDataProvider.getValidUserToSave()));
        when(eventRepository.findEventByIdWithoutCategory(eventId))
                .thenReturn(Optional.of(event));

        Executable executable = () -> requestService.addRequest(userId, eventId);

        assertThrows(ForbiddenException.class, executable);
    }

    @Test
    void addRequest_whenEventCancelled_thenForbiddenExceptionThrown() {
        Long userId = 0L;
        Long eventId = 1L;
        User eventOwner = TestDataProvider.getValidUserToSave();
        eventOwner.setId(3L);
        Event event = TestDataProvider.getValidNotSavedEvent(eventOwner, new Category());
        event.setState(EventState.CANCELED);
        when(userRepository.findUserById(userId))
                .thenReturn(Optional.of(TestDataProvider.getValidUserToSave()));
        when(eventRepository.findEventByIdWithoutCategory(eventId))
                .thenReturn(Optional.of(event));

        Executable executable = () -> requestService.addRequest(userId, eventId);

        assertThrows(ForbiddenException.class, executable);
    }

    @Test
    void addRequest_whenEventPending_thenForbiddenExceptionThrown() {
        Long userId = 0L;
        Long eventId = 1L;
        User eventOwner = TestDataProvider.getValidUserToSave();
        eventOwner.setId(3L);
        Event event = TestDataProvider.getValidNotSavedEvent(eventOwner, new Category());
        event.setState(EventState.PENDING);
        when(userRepository.findUserById(userId))
                .thenReturn(Optional.of(TestDataProvider.getValidUserToSave()));
        when(eventRepository.findEventByIdWithoutCategory(eventId))
                .thenReturn(Optional.of(event));

        Executable executable = () -> requestService.addRequest(userId, eventId);

        assertThrows(ForbiddenException.class, executable);
    }

    @Test
    void addRequest_whenParticipationLimitReached_thenForbiddenExceptionThrown() {
        Long userId = 0L;
        Long eventId = 1L;
        User eventOwner = TestDataProvider.getValidUserToSave();
        eventOwner.setId(3L);
        Event event = TestDataProvider.getValidNotSavedEvent(eventOwner, new Category());
        event.setState(EventState.PUBLISHED);
        event.setParticipantLimit(1);
        event.setConfirmedRequests(1);
        when(userRepository.findUserById(userId))
                .thenReturn(Optional.of(TestDataProvider.getValidUserToSave()));
        when(eventRepository.findEventByIdWithoutCategory(eventId))
                .thenReturn(Optional.of(event));

        Executable executable = () -> requestService.addRequest(userId, eventId);

        assertThrows(ForbiddenException.class, executable);
    }

    @Test
    void addRequest_whenEventWithoutModeration_thenRequestStatusConfirmed() {
        Long userId = 0L;
        Long eventId = 1L;
        User eventOwner = TestDataProvider.getValidUserToSave();
        eventOwner.setId(3L);
        Event event = TestDataProvider.getValidNotSavedEvent(eventOwner, new Category());
        event.setState(EventState.PUBLISHED);
        event.setRequestModeration(false);
        when(userRepository.findUserById(userId))
                .thenReturn(Optional.of(TestDataProvider.getValidUserToSave()));
        when(eventRepository.findEventByIdWithoutCategory(eventId))
                .thenReturn(Optional.of(event));

        try {
            requestService.addRequest(userId, eventId);
        } catch (Throwable e) {
            //catch param without full method invocation
        }

        verify(requestRepository, times(1))
                .save(requestArgumentCaptor.capture());

        assertThat(requestArgumentCaptor.getValue().getRequestStatus(),
                equalTo(RequestStatus.CONFIRMED));
    }

    @Test
    void addRequest_whenEventWithModeration_thenRequestStatusPending() {
        Long userId = 0L;
        Long eventId = 1L;
        User eventOwner = TestDataProvider.getValidUserToSave();
        eventOwner.setId(3L);
        Event event = TestDataProvider.getValidNotSavedEvent(eventOwner, new Category());
        event.setState(EventState.PUBLISHED);
        event.setRequestModeration(true);
        when(userRepository.findUserById(userId))
                .thenReturn(Optional.of(TestDataProvider.getValidUserToSave()));
        when(eventRepository.findEventByIdWithoutCategory(eventId))
                .thenReturn(Optional.of(event));

        try {
            requestService.addRequest(userId, eventId);
        } catch (Throwable e) {
            //catch param without full method invocation
        }

        verify(requestRepository, times(1))
                .save(requestArgumentCaptor.capture());

        assertThat(requestArgumentCaptor.getValue().getRequestStatus(),
                equalTo(RequestStatus.PENDING));
    }

    @Test
    void addRequest_whenInvoked_thenUserAndEventAddToRequest() {
        Long userId = 0L;
        Long eventId = 1L;
        User eventOwner = TestDataProvider.getValidUserToSave();
        User requester = TestDataProvider.getValidUserToSave();
        requester.setId(userId);
        eventOwner.setId(3L);
        Event event = TestDataProvider.getValidNotSavedEvent(eventOwner, new Category());
        event.setId(eventId);
        event.setState(EventState.PUBLISHED);
        when(userRepository.findUserById(userId))
                .thenReturn(Optional.of(requester));
        when(eventRepository.findEventByIdWithoutCategory(eventId))
                .thenReturn(Optional.of(event));

        try {
            requestService.addRequest(userId, eventId);
        } catch (Throwable e) {
            //catch param without full method invocation
        }

        verify(requestRepository, times(1))
                .save(requestArgumentCaptor.capture());

        assertThat(requestArgumentCaptor.getValue().getRequester().getId(),
                equalTo(userId));
        assertThat(requestArgumentCaptor.getValue().getEvent().getId(),
                equalTo(eventId));
    }

    @Test
    void addRequest_whenInvoked_thenCreatedFieldAddToRequest() {
        Long userId = 0L;
        Long eventId = 1L;
        User eventOwner = TestDataProvider.getValidUserToSave();
        User requester = TestDataProvider.getValidUserToSave();
        requester.setId(userId);
        eventOwner.setId(3L);
        Event event = TestDataProvider.getValidNotSavedEvent(eventOwner, new Category());
        event.setId(eventId);
        event.setState(EventState.PUBLISHED);
        when(userRepository.findUserById(userId))
                .thenReturn(Optional.of(requester));
        when(eventRepository.findEventByIdWithoutCategory(eventId))
                .thenReturn(Optional.of(event));

        try {
            requestService.addRequest(userId, eventId);
        } catch (Throwable e) {
            //catch param without full method invocation
        }

        verify(requestRepository, times(1))
                .save(requestArgumentCaptor.capture());

        assertThat(requestArgumentCaptor.getValue().getCreated(),
                equalTo(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)));

    }

    @Test
    void findUsersRequests_whenUserNotFound_thenNotExistExceptionThrown() {
        Long userId = 0L;
        when(userRepository.userExists(userId))
                .thenReturn(false);

        Executable executable = () -> requestService.findUserRequests(userId);

        assertThrows(NotExistsException.class, executable);
    }

    @Test
    void cancelRequest_whenNoRequestWithPassedIdAndUserId_thenNotExistExceptionThrown() {
        Long userId = 0L;
        Long requestId = 1L;
        when(requestRepository.findByUserIdAndRequestId(userId, requestId))
                .thenReturn(Optional.empty());

        Executable executable = () -> requestService.cancelRequest(userId, requestId);

        assertThrows(NotExistsException.class, executable);
    }

    @Test
    void cancelRequest_whenRequestStatusIsPending_thenRequestWithCanceledPassToRepository() {
        Long userId = 0L;
        Long eventId = 0L;
        Long requestId = 1L;
        EventParticipationRequest request = TestDataProvider.getValidRequestToSave(
                new User(userId),
                Event.builder().id(eventId).build());
        request.setRequestStatus(RequestStatus.PENDING);
        when(requestRepository.findByUserIdAndRequestId(userId, requestId))
                .thenReturn(Optional.of(request));

        try {
            requestService.cancelRequest(userId, requestId);
        } catch (Throwable e) {
            //catch param without full method invocation
        }

        verify(requestRepository, times(1))
                .update(requestArgumentCaptor.capture());
        assertThat(requestArgumentCaptor.getValue().getRequestStatus(), equalTo(RequestStatus.CANCELED));
    }

    @Test
    void cancelRequest_whenRequestStatusIsCanceled_thenRepositoryNotInvoked() {
        Long userId = 0L;
        Long eventId = 0L;
        Long requestId = 1L;
        EventParticipationRequest request = TestDataProvider.getValidRequestToSave(
                new User(userId),
                Event.builder().id(eventId).build());
        request.setRequestStatus(RequestStatus.CANCELED);
        when(requestRepository.findByUserIdAndRequestId(userId, requestId))
                .thenReturn(Optional.of(request));

        requestService.cancelRequest(userId, requestId);

        verify(requestRepository, times(0))
                .update(request);
    }

    @Test
    void cancelRequest_whenRequestStatusIsRejected_thenForbiddenExceptionThrown() {
        Long userId = 0L;
        Long eventId = 0L;
        Long requestId = 1L;
        EventParticipationRequest request = TestDataProvider.getValidRequestToSave(
                new User(userId),
                Event.builder().id(eventId).build());
        request.setRequestStatus(RequestStatus.REJECTED);
        when(requestRepository.findByUserIdAndRequestId(userId, requestId))
                .thenReturn(Optional.of(request));

        Executable executable = () -> requestService.cancelRequest(userId, requestId);

        verify(requestRepository, times(0))
                .update(request);
        assertThrows(ForbiddenException.class, executable);
    }

    @Test
    void findUserRequestsToEvent_whenInvoked_thenParamsPassedToService() {
        Long userId = 1L;
        Long eventId = 2L;
        Event event = TestDataProvider.getValidNotSavedEvent(new User(userId), new Category());
        when(eventRepository.findEventByIdWithoutCategory(eventId))
                .thenReturn(Optional.of(event));

        try {
            requestService.findRequestsToEvent(userId, eventId);
        } catch (Throwable e) {
            //catch param without full method invocation
        }

        verify(requestRepository, times(1))
                .findRequestsForEvent(
                        eventIdArgumentCaptor.capture()
                );
        assertThat(eventIdArgumentCaptor.getValue(), equalTo(eventId));
    }

    @Test
    void findUserRequestsToEvent_whenUserIsNotEventOwner_thenForbiddenExceptionThrown() {
        Long userId = 1L;
        Long eventId = 2L;
        Event event = TestDataProvider.getValidNotSavedEvent(new User(99L), new Category());
        when(eventRepository.findEventByIdWithoutCategory(eventId))
                .thenReturn(Optional.of(event));

        Executable executable = () -> requestService.findRequestsToEvent(userId, eventId);

        assertThrows(ForbiddenException.class, executable);
    }

    @Test
    void findUserRequestsToEvent_whenEventNotFound_thenNotExistsExceptionThrown() {
        Long userId = 1L;
        Long eventId = 2L;
        when(eventRepository.findEventByIdWithoutCategory(eventId))
                .thenReturn(Optional.empty());

        Executable executable = () -> requestService.findRequestsToEvent(userId, eventId);

        assertThrows(NotExistsException.class, executable);
    }

    @Test
    void updateRequestsStatuses_whenEventNotFound_thenNotExistsExceptionThrown() {
        Long eventOwner = 1L;
        Long eventId = 2L;
        EventRequestStatusUpdateRequestDto statusUpdateRequest = EventRequestStatusUpdateRequestDto.builder().build();
        when(eventRepository.findEventByIdWithoutCategory(eventId))
                .thenReturn(Optional.empty());

        Executable executable = () -> requestService.updateRequestsStatuses(
                eventOwner,
                eventId,
                statusUpdateRequest);

        assertThrows(NotExistsException.class, executable);
    }

    @Test
    void updateRequestsStatuses_whenUserNotEventOwner_thenForbiddenExceptionThrown() {
        Long eventOwner = 1L;
        Long otherUserId = 2L;
        Long eventId = 0L;
        Event event = TestDataProvider.getValidNotSavedEvent(new User(eventOwner), new Category());
        EventRequestStatusUpdateRequestDto statusUpdateRequest = EventRequestStatusUpdateRequestDto.builder().build();
        when(eventRepository.findEventByIdWithoutCategory(eventId))
                .thenReturn(Optional.of(event));

        Executable executable = () -> requestService.updateRequestsStatuses(
                otherUserId,
                eventId,
                statusUpdateRequest);

        assertThrows(ForbiddenException.class, executable);
    }

    @Test
    void updateRequestsStatuses_whenRequestToConfirmAndStatusConfirmed_thenForbiddenExceptionThrown() {
        Long eventOwner = 1L;
        Long eventId = 0L;
        List<Long> requestsIds = List.of(0L);
        Event event = TestDataProvider.getValidNotSavedEvent(new User(eventOwner), new Category());
        EventParticipationRequest request = TestDataProvider.getValidRequestToSave(new User(0L), event);
        request.setRequestStatus(RequestStatus.CONFIRMED);
        EventRequestStatusUpdateRequestDto statusUpdateRequest = EventRequestStatusUpdateRequestDto.builder()
                .requestIds(requestsIds)
                .status(RequestStatusUpdateDto.CONFIRMED)
                .build();
        when(eventRepository.findEventByIdWithoutCategory(eventId))
                .thenReturn(Optional.of(event));
        when(requestRepository.findRequestsForEvent(eventId, requestsIds))
                .thenReturn(List.of(request));

        Executable executable = () -> requestService.updateRequestsStatuses(
                eventOwner,
                eventId,
                statusUpdateRequest);

        assertThrows(ForbiddenException.class, executable);
    }

    @Test
    void updateRequestsStatuses_whenRequestToConfirmAndStatusRejected_thenForbiddenExceptionThrown() {
        Long eventOwner = 1L;
        Long eventId = 0L;
        List<Long> requestsIds = List.of(0L);
        Event event = TestDataProvider.getValidNotSavedEvent(new User(eventOwner), new Category());
        EventParticipationRequest request = TestDataProvider.getValidRequestToSave(new User(0L), event);
        request.setRequestStatus(RequestStatus.REJECTED);
        EventRequestStatusUpdateRequestDto statusUpdateRequest = EventRequestStatusUpdateRequestDto.builder()
                .requestIds(requestsIds)
                .status(RequestStatusUpdateDto.CONFIRMED)
                .build();
        when(eventRepository.findEventByIdWithoutCategory(eventId))
                .thenReturn(Optional.of(event));
        when(requestRepository.findRequestsForEvent(eventId, requestsIds))
                .thenReturn(List.of(request));

        Executable executable = () -> requestService.updateRequestsStatuses(
                eventOwner,
                eventId,
                statusUpdateRequest);

        assertThrows(ForbiddenException.class, executable);
    }

    @Test
    void updateRequestsStatuses_whenRequestToConfirmAndStatusCanceled_thenForbiddenExceptionThrown() {
        Long eventOwnerId = 1L;
        Long eventId = 0L;
        List<Long> requestsIds = List.of(0L);
        Event event = TestDataProvider.getValidNotSavedEvent(new User(eventOwnerId), new Category());
        EventParticipationRequest request = TestDataProvider.getValidRequestToSave(new User(0L), event);
        request.setRequestStatus(RequestStatus.CANCELED);
        EventRequestStatusUpdateRequestDto statusUpdateRequest = EventRequestStatusUpdateRequestDto.builder()
                .requestIds(requestsIds)
                .status(RequestStatusUpdateDto.CONFIRMED)
                .build();
        when(eventRepository.findEventByIdWithoutCategory(eventId))
                .thenReturn(Optional.of(event));
        when(requestRepository.findRequestsForEvent(eventId, requestsIds))
                .thenReturn(List.of(request));

        Executable executable = () -> requestService.updateRequestsStatuses(
                eventOwnerId,
                eventId,
                statusUpdateRequest);

        assertThrows(ForbiddenException.class, executable);
    }

    @Test
    void updateRequestsStatuses_whenListToConfirmMoreThanLimit_thenOnlyPartRequestsConfirmed() {
        Long eventOwnerId = 1L;
        Long eventId = 0L;
        List<Long> requestsIds = List.of(1L, 2L);
        Event event = TestDataProvider.getValidNotSavedEvent(new User(eventOwnerId), new Category());
        event.setParticipantLimit(1);
        event.setConfirmedRequests(0);
        event.setId(eventId);
        EventParticipationRequest request1 = TestDataProvider.getValidRequestToSave(new User(0L), event);
        EventParticipationRequest request2 = TestDataProvider.getValidRequestToSave(new User(0L), event);
        request1.setRequestStatus(RequestStatus.PENDING);
        request2.setRequestStatus(RequestStatus.PENDING);
        EventRequestStatusUpdateRequestDto statusUpdateRequest = EventRequestStatusUpdateRequestDto.builder()
                .status(RequestStatusUpdateDto.CONFIRMED)
                .requestIds(requestsIds)
                .build();
        when(eventRepository.findEventByIdWithoutCategory(eventId))
                .thenReturn(Optional.of(event));
        when(requestRepository.findRequestsForEvent(eventId, requestsIds))
                .thenReturn(List.of(request1, request2));

        try {
            requestService.updateRequestsStatuses(eventOwnerId, eventId, statusUpdateRequest);
        } catch (Throwable e) {
            //catch param without full method invocation
        }

        verify(requestRepository, times(2))
                .updateRequestsStatusForEvent(anyLong(), updateStatusesArgumentCaptor.capture());
        assertThat(updateStatusesArgumentCaptor.getAllValues().get(0).getRequestIds().get(0),
                equalTo(requestsIds.get(0)));
        assertThat(updateStatusesArgumentCaptor.getAllValues().get(0).getStatus(),
                equalTo(RequestStatusUpdateDto.CONFIRMED));
        assertThat(updateStatusesArgumentCaptor.getAllValues().get(1).getRequestIds().get(0),
                equalTo(requestsIds.get(1)));
        assertThat(updateStatusesArgumentCaptor.getAllValues().get(1).getStatus(),
                equalTo(RequestStatusUpdateDto.REJECTED));
    }

    @Test
    void updateRequestsStatuses_whenListToConfirmLessThanLimit_thenAllRequestsConfirmed() {
        Long eventOwnerId = 1L;
        Long eventId = 0L;
        List<Long> requestsIds = List.of(1L, 2L);
        Event event = TestDataProvider.getValidNotSavedEvent(new User(eventOwnerId), new Category());
        event.setParticipantLimit(10);
        event.setConfirmedRequests(0);
        event.setId(eventId);
        EventParticipationRequest request1 = TestDataProvider.getValidRequestToSave(new User(0L), event);
        EventParticipationRequest request2 = TestDataProvider.getValidRequestToSave(new User(0L), event);
        request1.setRequestStatus(RequestStatus.PENDING);
        request2.setRequestStatus(RequestStatus.PENDING);
        EventRequestStatusUpdateRequestDto statusUpdateRequest = EventRequestStatusUpdateRequestDto.builder()
                .status(RequestStatusUpdateDto.CONFIRMED)
                .requestIds(requestsIds)
                .build();
        when(eventRepository.findEventByIdWithoutCategory(eventId))
                .thenReturn(Optional.of(event));
        when(requestRepository.findRequestsForEvent(eventId, requestsIds))
                .thenReturn(List.of(request1, request2));

        try {
            requestService.updateRequestsStatuses(eventOwnerId, eventId, statusUpdateRequest);
        } catch (Throwable e) {
            //catch param without full method invocation
        }

        verify(requestRepository, times(1))
                .updateRequestsStatusForEvent(anyLong(), updateStatusesArgumentCaptor.capture());
        assertThat(updateStatusesArgumentCaptor.getValue().getRequestIds().get(0),
                equalTo(requestsIds.get(0)));
        assertThat(updateStatusesArgumentCaptor.getValue().getRequestIds().get(1),
                equalTo(requestsIds.get(1)));
        assertThat(updateStatusesArgumentCaptor.getValue().getStatus(),
                equalTo(RequestStatusUpdateDto.CONFIRMED));
    }

    @Test
    void updateRequestsStatuses_whenListToRejectLessThanLimit_thenAllRequestsRejected() {
        Long eventOwnerId = 1L;
        Long eventId = 0L;
        List<Long> requestsIds = List.of(1L, 2L);
        Event event = TestDataProvider.getValidNotSavedEvent(new User(eventOwnerId), new Category());
        event.setParticipantLimit(1);
        event.setConfirmedRequests(1);
        event.setId(eventId);
        EventParticipationRequest request1 = TestDataProvider.getValidRequestToSave(new User(0L), event);
        EventParticipationRequest request2 = TestDataProvider.getValidRequestToSave(new User(0L), event);
        request1.setRequestStatus(RequestStatus.PENDING);
        request2.setRequestStatus(RequestStatus.PENDING);
        EventRequestStatusUpdateRequestDto statusUpdateRequest = EventRequestStatusUpdateRequestDto.builder()
                .status(RequestStatusUpdateDto.REJECTED)
                .requestIds(requestsIds)
                .build();
        when(eventRepository.findEventByIdWithoutCategory(eventId))
                .thenReturn(Optional.of(event));
        when(requestRepository.findRequestsForEvent(eventId, requestsIds))
                .thenReturn(List.of(request1, request2));

        try {
            requestService.updateRequestsStatuses(eventOwnerId, eventId, statusUpdateRequest);
        } catch (Throwable e) {
            //catch param without full method invocation
        }

        verify(requestRepository, times(1))
                .updateRequestsStatusForEvent(anyLong(), updateStatusesArgumentCaptor.capture());
        assertThat(updateStatusesArgumentCaptor.getValue().getRequestIds().get(0),
                equalTo(requestsIds.get(0)));
        assertThat(updateStatusesArgumentCaptor.getValue().getRequestIds().get(1),
                equalTo(requestsIds.get(1)));
        assertThat(updateStatusesArgumentCaptor.getValue().getStatus(),
                equalTo(RequestStatusUpdateDto.REJECTED));
    }
}