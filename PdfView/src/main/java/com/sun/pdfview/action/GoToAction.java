/*
 * $Id: GoToAction.java,v 1.2 2007/12/20 18:33:34 rbair Exp $
 *
 * Copyright 2004 Sun Microsystems, Inc., 4150 Network Circle,
 * Santa Clara, California 95054, U.S.A. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package com.sun.pdfview.action;

import java.io.IOException;

import com.sun.pdfview.PDFObject;
import com.sun.pdfview.PDFDestination;
import com.sun.pdfview.PDFParseException;

/**
 * An action which specifies going to a particular destination
 */
public class GoToAction extends PDFAction {
    /** the destination to go to */
    private PDFDestination dest;
    
    /** 
     * Creates a new instance of GoToAction from an object
     *
     * @param obj the PDFObject with the action information
     */
    public GoToAction(PDFObject obj, PDFObject root) throws IOException {
        super("GoTo");
        
        // find the destination
        PDFObject destObj = obj.getDictRef("D");
        if (destObj == null) {
            throw new PDFParseException("No destination in GoTo action " + obj);
        }
        
        // parse it
        dest = PDFDestination.getDestination(destObj, root);
    }
    
    /**
     * Create a new GoToAction from a destination
     */
    public GoToAction(PDFDestination dest) {
        super("GoTo");
    
        this.dest = dest;
    }
      
    /**
     * Get the destination this action refers to
     */
    public PDFDestination getDestination() {
        return dest;
    }
}
