/**
 * Package of all price models.<p>
 * 
 * Each model is required to store two things: A date, and some kind of money
 * amount information.<p>
 * 
 * The common interface {@link Price} is an accessor for the date.<p>
 * 
 * There are a numerous amount of ways one could represent a monetary amount.
 * The core goal of this project is to model/demonstrate a few alternatives, and
 * profile how these models perform when being serialized/deserialized.
 */
package com.martinandersson.money.lib.model;