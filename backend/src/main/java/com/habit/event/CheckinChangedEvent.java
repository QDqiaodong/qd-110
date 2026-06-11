package com.habit.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDate;

@Getter
public class CheckinChangedEvent extends ApplicationEvent {
    
    private final LocalDate date;
    private final ChangeType changeType;
    
    public enum ChangeType {
        CHECKIN_TOGGLE,
        HABIT_CHANGED,
        HABIT_DELETED
    }
    
    public CheckinChangedEvent(Object source, LocalDate date, ChangeType changeType) {
        super(source);
        this.date = date;
        this.changeType = changeType;
    }
}
