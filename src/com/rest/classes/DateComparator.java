package com.rest.classes;

import java.util.Comparator;

import com.rest.calendar.Event;

/**
 * Used for comparing two dates
 * @author Kshitiz Bhattarai
 */
public class DateComparator implements Comparator<Event> {
	    @Override
	    public int compare(Event e1, Event e2) {
	    	return e1.getBegin().compareTo(e2.getBegin());
	    }
	}
