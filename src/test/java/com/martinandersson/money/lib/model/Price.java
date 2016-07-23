package com.martinandersson.money.lib.model;

import java.time.LocalDate;

/**
 * A price has a date, called the "relevant" date for whatever else
 * price-information the concrete type may provide.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 * 
 * @see com.martinandersson.money.lib.model package-info.java
 */
public interface Price
{
    /**
     * Returns the relevant date.
     * 
     * @return the relevant date
     */
    LocalDate getDate();
}