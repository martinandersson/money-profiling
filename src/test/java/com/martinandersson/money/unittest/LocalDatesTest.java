package com.martinandersson.money.unittest;

import com.martinandersson.money.lib.LocalDates;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;

/**
 * Test of {@code LocalDates}.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public class LocalDatesTest
{
    @Test
    public void test() {
        Stream.iterate(LocalDates.MIN, date -> date.plusDays(1))
                .limit(ChronoUnit.DAYS.between(LocalDates.MIN, LocalDates.MAX) + 1)
                .forEach(this::run);
    }
    
    private short lastSeen = Short.MIN_VALUE;
    
    private boolean first = true;
    
    private void run(LocalDate date) {
        final short s = LocalDates.toShort(date);
        
        if (!first) {
            assertEquals(s, lastSeen + 1);
        }
        
        lastSeen = s;
        first = false;
        
        LocalDate d = LocalDates.fromShort(s);
        
        assertEquals(d, date);
    }
}