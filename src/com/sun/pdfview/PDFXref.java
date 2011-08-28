/*
 * $Id: PDFXref.java,v 1.4 2009/02/12 13:53:56 tomoke Exp $
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
package com.sun.pdfview;

import net.sf.andpdf.refs.SoftReference;

/**
 * a cross reference representing a line in the PDF cross referencing
 * table.
 * <p>
 * There are two forms of the PDFXref, destinguished by absolutely nothing.
 * The first type of PDFXref is used as indirect references in a PDFObject.
 * In this type, the id is an index number into the object cross reference
 * table.  The id will range from 0 to the size of the cross reference
 * table.
 * <p>
 * The second form is used in the Java representation of the cross reference
 * table.  In this form, the id is the file position of the start of the
 * object in the PDF file.  See the use of both of these in the 
 * PDFFile.dereference() method, which takes a PDFXref of the first form,
 * and uses (internally) a PDFXref of the second form.
 * <p>
 * This is an unhappy state of affairs, and should be fixed.  Fortunatly,
 * the two uses have already been factored out as two different methods.
 *
 * @author Mike Wessler
 */
public class PDFXref {

    private int id;
    private int generation;
    private boolean compressed;
    
    // this field is only used in PDFFile.objIdx
    private SoftReference<PDFObject> reference = null;

    /**
     * create a new PDFXref, given a parsed id and generation.
     */
    public PDFXref(int id, int gen) {
        this.id = id;
        this.generation = gen;
        this.compressed = false;
    }

    /**
     * create a new PDFXref, given a parsed id, compressedObjId and index
     */
    public PDFXref(int id, int gen, boolean compressed) {
        this.id = id;
        this.generation = gen;
        this.compressed = compressed;
    }

    /**
     * create a new PDFXref, given a sequence of bytes representing the
     * fixed-width cross reference table line
     */
    public PDFXref(byte[] line) {
        if (line == null) {
            id = -1;
            generation = -1;
        } else {
            id = Integer.parseInt(new String(line, 0, 10));
            generation = Integer.parseInt(new String(line, 11, 5));
        }
        compressed = false;
    }

    /**
     * get the character index into the file of the start of this object
     */
    public int getFilePos() {
        return id;
    }

    /**
     * get the generation of this object
     */
    public int getGeneration() {
        return generation;
    }

    /**
     * get the generation of this object
     */
    public int getIndex() {
        return generation;
    }

    /**
     * get the object number of this object
     */
    public int getID() {
        return id;
    }

    /**
     * get compressed flag of this object
     */
    public boolean getCompressed() {
        return compressed;
    }


    /**
     * Get the object this reference refers to, or null if it hasn't been
     * set.
     * @return the object if it exists, or null if not
     */
    public PDFObject getObject() {
        if (reference != null) {
            return (PDFObject) reference.get();
        }

        return null;
    }

    /**
     * Set the object this reference refers to.
     */
    public void setObject(PDFObject obj) {
        this.reference = new SoftReference<PDFObject>(obj);
    }
}
