package ru.practicum.ewm.main.event.dto.updateRequest;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UpdateEventUserRequest extends UpdateEventRequest {

    private UserRequestStateAction stateAction;
}
