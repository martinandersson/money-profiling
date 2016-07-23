package com.martinandersson.money.lib;

import java.io.DataOutput;
import static java.text.MessageFormat.format;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;

/**
 * Offer an API to pack/unpack dates into/from shorts.<p>
 * 
 * {@link LocalDate#writeExternal(DataOutput)} serialize year, month and day
 * separately as int, byte and byte respectively for a total size of 48 bits.<p>
 * 
 * {@link LocalDate#toEpochDay()} can clearly convert a date to a 64-bit
 * long.<p>
 * 
 * The epoch day is a counter that start at 1970-01-01. We can select our own
 * origin and limit the date range to the amount of days that fit until an outer
 * bound is reached.<p>
 * 
 * The numeric space of a 16-bit short/char is 65 536 (2^16).<br>
 * The numeric space of a 32-bit int is 4 294 967 296 (2^32).<p>
 * 
 * Either choice translate to a wide range of dates that is sufficient for our
 * needs.<p>
 * 
 * We have opted for the short, and define the legal range to be 1950-01-01 to
 * 2128-12-31 (total days between = 65 378). This give us the possibility to
 * represent really old dates as well as dates that reach far into the next
 * generation of super humans. I have so far not stumbled across a data provider
 * that offer financial information going back further in time. For example,
 * Nasdaq - the world's first electronic stock market - began trading
 * 1971-02-08.<p>
 * 
 * Please note that a naive algorithm exercises Math voodoo on a date in order
 * to compute an int <i>without</i> doing proper parameter validation. Even
 * though an int can always be computed, this approach will not work for all
 * possible arbitrary dates and might yield hard to debug bugs. See:
 * <pre>
 *   https://github.com/magro/kryo-serializers/blob/master/src/main/java/de/javakaffee/kryoserializers/jodatime/JodaLocalDateSerializer.java
 * </pre>
 * 
 * Even for a short, we could have also exercise Math voodoo and split 16-bits
 * to year, month and day parts. For example, 5 bits can represent 32 different
 * days. 4 bits can represent 16 different months. 7 bits can represent 128
 * different years. That would give us a total range of almost 129 years, but
 * this is still a substantially smaller range than the promised range a few
 * paragraphs back. We achieve this range by converting a date into a day
 * count.<p>
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public final class LocalDates
{
    private LocalDates() {
        // Empty
    }
    
    
    public static final LocalDate MIN = LocalDate.of(1950, Month.JANUARY,   1),
                                  MAX = LocalDate.of(2128, Month.DECEMBER, 31);
    
    /**
     * Turn specified {@code date} into a short.<p>
     * 
     * This method is subject to a date range restriction as specified in
     * JavaDoc of {@link LocalDates}.<p>
     * 
     * The short values produced by this method remain same order semantics as
     * {@code LocalDate}. This is strictly not needed for our use cases, but
     * necessary for many financial apps using Chronicle Map which unfortunately
     * does not provide a sorted or navigable view of keys. Caching keys
     * (shorts) separately is one way to let clients efficiently query
     * information by date.
     * 
     * @param date  local date to convert
     * 
     * @return date as a short
     */
    public static short toShort(LocalDate date) {
        if (date.isBefore(MIN) || date.isAfter(MAX)) {
            throw new IllegalArgumentException(format(
                    "Totally unacceptable: {0}. Accepted range: {1} .. {2}.",
                    date, MIN, MAX));
        }
        
        // No order required? Do:
//        return (short) ChronoUnit.DAYS.between(MIN, date);
        
        // With order:
        int unsignedShort = (int) ChronoUnit.DAYS.between(MIN, date);
        return (short) ((int) Short.MIN_VALUE + unsignedShort);
    }
    
    /**
     * Turn specified {@code val} into a date.
     * 
     * @param val  a date in it's "short" form
     * 
     * @return a date
     */
    public static LocalDate fromShort(short val) {
        // No order requuired? Do:
//        return MIN.plusDays(val & 0xFFFF);
        
        // With order:
        return MIN.plusDays(val - Short.MIN_VALUE);
    }
}