package ru.practicum.ewm.main.event.dto.updaterequest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import ru.practicum.ewm.main.event.dto.NewEventDto;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UpdateEventAdminRequestDto extends NewEventDto {
    AdminRequestStateActionDto stateAction;
}
