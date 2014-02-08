/**
 * $Id: MAVLinkParam.java 4 2013-04-11 14:04:50Z ghelle31@gmail.com $
 * $Date: 2013-04-11 16:04:50 +0200 (Thu, 11 Apr 2013) $
 *
 * ======================================================
 * Copyright (C) 2012 Guillaume Helle.
 * Project : MAVLink Java Generator
 * Module : org.mavlink.messages
 * File : org.mavlink.messages.MAVLinkParam.java
 * Author : Guillaume Helle
 *
 * ======================================================
 * HISTORY
 * Who       yyyy/mm/dd   Action
 * --------  ----------   ------
 * ghelle	2 avr. 2012		Create
 * 
 * ====================================================================
 * Licence: MAVLink LGPL
 * ====================================================================
 */

package org.mavlink.messages;

/**
 * MAVLink Param type
 * @author ghelle
 * @version $Rev: 4 $
 *
 */
public class MAVLinkParam {

    private int index;

    private String comment;

    public MAVLinkParam(int index) {
        this.index = index;
    }

    /**
     * @return The index
     */
    public int getIndex() {
        return index;
    }

    /**
     * @param index The index to set
     */
    public void setIndex(int index) {
        this.index = index;
    }

    /**
     * @return The comment
     */
    public String getComment() {
        return comment;
    }

    /**
     * @param comment The comment to set
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

}
