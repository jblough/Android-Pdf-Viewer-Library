/*
 * $Id: FunctionType4.java,v 1.3 2009/02/12 13:53:59 tomoke Exp $
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
package com.sun.pdfview.function;

import java.io.IOException;
import java.util.*;

import net.sf.andpdf.nio.ByteBuffer;

import com.sun.pdfview.PDFObject;
import com.sun.pdfview.PDFParseException;

/**
 * <p>A PostScript function is represented as a stream containing code
 * written in a small subset of the PostScript language. 
 * This reference is taken from the (3200-1:2008:7.10.5)<p>
 *
 * http://www.adobe.com/devnet/acrobat/pdfs/adobe_supplement_iso32000.pdf
 * </p>
 */
public class FunctionType4 extends PDFFunction {

    /** the set of all Operations we support. These operations are defined
     * in Appendix B - Operators.*/
    private static HashSet<Operation> operationSet = null;
    /** the list of tokens and sub-expressions. */
    private LinkedList tokens = new LinkedList();
    /** the stack of operations. The stack contents should all be Comparable. */
    private LinkedList<Object> stack = new LinkedList<Object>();

    /** Creates a new instance of FunctionType4 */
    protected FunctionType4() {
        super(TYPE_4);
        if (operationSet == null) {
            initOperations();
        }
    }

    /**
     * Initialize the operations that we can perform.
     */
    private void initOperations() {
        /** these operators consider the left hand arguments as deeper in
         * the stack than the right hand arguments, thus the right-hand is
         * is the top of the stack and is popped first.
         *
         * Operation details in PostScript Language Reference Manual:
         * http://www.adobe.com/products/postscript/pdfs/PLRM.pdf
         * Chapter 8 - Operator Details
         */
        if (operationSet == null) {
            operationSet = new HashSet<Operation>();
            // Arithmetic Operators
            operationSet.add(new Operation("abs") {

                /**
                 * <i>num1</i> <b>abs</b> <i>num2</i> <p>
                 *
                 * The type of the result is the same as the type of num1,
                 * unless num1 is the smallest (most negative) integer,
                 * in which case the result is a real number.<p>
                 *
                 * errors: stackunderflow, typecheck
                 */
                void eval() {
                    pushDouble(Math.abs(popDouble()));
                }
            });
            operationSet.add(new Operation("add") {

                /**
                 * <i>num1 num2</i> <b>add</b> <i>sum</i> <p>
                 *
                 * If both operands are integers and the result is
                 * within integer range, the result is an integer;
                 * otherwise, the result is a real number.<p>
                 *
                 * errors: stackunderflow, typecheck, undefinedresult
                 */
                void eval() {
                    pushDouble(popDouble() + popDouble());
                }
            });
            operationSet.add(new Operation("atan") {

                /**
                 * <i>num den</i> <b>atan</b> <i>angle</i> <p>
                 *
                 * returns the angle (in degress between
                 * 0 and 360) whose tangent is num divided by den.
                 * Either num or den may be 0, but not both. The signs
                 * of num and den determine the quadrant in which the
                 * result will lie: positive num yeilds a result in the
                 * positive y plane, while a positive den yeilds a result in
                 * the positive x plane. The result is a real number.<p>
                 *
                 * errors: stackunderflow, typecheck, undefinedresult
                 */
                void eval() {
                    double den = popDouble();
                    double num = popDouble();
                    if (den == 0.0) {
                        pushDouble(90.0);
                    } else {
                        pushDouble(Math.toDegrees(Math.atan(num / den)));
                    }
                }
            });
            operationSet.add(new Operation("ceiling") {

                /**
                 * <i>num1</i> <b>ceiling</b> <i>num2</i> <p>
                 *
                 * returns the least integer value greater than or equal
                 * to num1. The type of the result is the same as the type
                 * of the operand. <p>
                 *
                 * errors: stackunderflow, typecheck
                 */
                void eval() {
                    pushDouble(Math.ceil(popDouble()));
                }
            });
            operationSet.add(new Operation("cvi") {

                /**
                 * <i>num</i> <b>cvi</b> <i>int</i> <u>or</u> <i>string</i> <b>cvi</b> <i>int</i> <p>
                 *
                 * takes an integer, real, or string and produces an
                 * integer result. If the operand is an integer, cvi
                 * simply returns it. If the operand is a real number,
                 * it truncates any fractional part (that is, rounds
                 * it toward 0) and converts it to an integer.
                 * If the operand is a string, cvi invokes the equivalent
                 * of the token operator to interpret the characters
                 * of the string as a number according to the PostScript
                 * syntax rules. If that number is a real number, cvi converts
                 * it to an integer.
                 * A rangecheck error occurs if a real number is too
                 * large to convert to an integer. <p>
                 *
                 * errors: invalidaccess, rangecheck, stackunderflow,
                 *         syntaxError, typecheck,
                 */
                void eval() {
                    pushDouble((double) ((int) popDouble()));
                }
            });
            operationSet.add(new Operation("cvr") {

                /**
                 * <i>num</i> <b>cvr</b> <i>real</i> <u>or</u> <i>string</i> <b>cvr</b> <i>real</i> <p>
                 *
                 * (convert to real) takes an integer, real, or string
                 * object and produces a real result. If the operand
                 * is an integer, cvr converts it to a real number.
                 * If the operand is a real number, cvr simply returns it.
                 * If the operand is a string, cvr invokes the equivalent
                 * of the token operator to interpret the characters of
                 * the string as a number according to the PostScript
                 * syntax rules. If that number is an integer, cvr converts
                 * it to a real number. <p>
                 *
                 * errors: invalidaccess, limitcheck, stackunderflow,
                 *         syntaxerror, typecheck, undefinedresult
                 */
                void eval() {

                    // YOUR CODE IN THIS SPACE
                }
            });
            operationSet.add(new Operation("div") {

                /**
                 * <i>num1 num2</i> <b>div</b> <i>quotient</i> <p>
                 *
                 * divides num1 by num2, producing a result that is
                 * always a real number even if both operands are integers.
                 * Use idiv instead if the operands are integers and an
                 * integer result is desired. <p>
                 *
                 * errors: stackunderflow, typecheck, undefinedresult
                 */
                void eval() {
                    double num2 = popDouble();
                    double num1 = popDouble();
                    pushDouble(num1 / num2);
                }
            });
            operationSet.add(new Operation("exp") {

                /**
                 * <i>base exponent</i> <b>exp</b> <i>real</i> <p>
                 *
                 * raises base to the exponent power. The operands may be
                 * either integers or real numbers. If the exponent has a
                 * fractional part, the result is meaningful only if the
                 * base is nonnegative. The result is always a real number. <p>
                 *
                 * errors: stackunderflow, typecheck, undefinedresult
                 */
                void eval() {
                    double exponent = popDouble();
                    double base = popDouble();
                    pushDouble(Math.pow(exponent, base));
                }
            });
            operationSet.add(new Operation("floor") {

                /**
                 * <i>num1</i> <b>floor</b> <i>num2</i> <p>
                 *
                 * returns the greatest integer value less than or equal
                 * to num1. The type of the result is the same as the type
                 * of the operand. <p>
                 *
                 * errors: stackunderflow, typecheck
                 */
                void eval() {
                    pushDouble(Math.floor(popDouble()));
                }
            });
            operationSet.add(new Operation("idiv") {

                /**
                 * <i>int1 int2</i> <b>idiv</b> <i>quotient</i> <p>
                 *
                 * divides int1 by int2 and returns the integer part
                 * of the quotient, with any fractional part discarded.
                 * Both operands of idiv must be integers and the result
                 * is an integer. <p>
                 *
                 * stackunderflow, typecheck, undefinedresult
                 */
                void eval() {
                    long int2 = popLong();
                    long int1 = popLong();
                    pushLong(int1 / int2);
                }
            });
            operationSet.add(new Operation("ln") {

                /**
                 * <i>num</i> <b>ln</b> <i>real</i> <p>
                 *
                 * returns the natural logarithm (base e) of num.
                 * The result is a real number. <p>
                 *
                 * errors: rangecheck, stackunderflow, typecheck
                 */
                void eval() {
                    pushDouble(Math.log(popDouble()));
                }
            });
            operationSet.add(new Operation("log") {

                /**
                 * <i>num</i> <b>log</b> <i>real</i> <p>
                 *
                 * returns the common logarithm (base 10) of num.
                 * The result is a real number. <p>
                 *
                 * errors:  rangecheck, stackunderflow, typecheck
                 */
                void eval() {
                    pushDouble(Math.log10(popDouble()));
                }
            });
            operationSet.add(new Operation("mod") {

                /**
                 * <i>int1 int2</i> <b>mod</b> <i>remainder</i> <p>
                 *
                 * returns the remainder that results from
                 * dividing int1 by int2. The sign of the result
                 * is the same as the sign of the dividend int1.
                 * Both operands must be integers and the result
                 * is an integer. <p>
                 *
                 * errors: stackunderflow, typecheck, undefinedresult
                 */
                void eval() {
                    long int2 = popLong();
                    long int1 = popLong();
                    pushLong(int1 % int2);
                }
            });
            operationSet.add(new Operation("mul") {

                /**
                 * <i>num1 num2</i> <b>mul</b> <i>product</i> <p>
                 *
                 * returns the product of num1 and num2.
                 * If both operands are integers and the result
                 * is within integer range, the result is an integer;
                 * otherwise, the result is a real number. <p>
                 *
                 * errors: stackunderflow, typecheck, undefinedresult
                 */
                void eval() {
                    pushDouble(popDouble() * popDouble());
                }
            });
            operationSet.add(new Operation("neg") {

                /**
                 * <i>num1</i> <b>neg</b> <i>num2</i> <p>
                 *
                 * returns the negative of num1. The type of the result
                 * is the same as the type of num1 unless num1 is the
                 * smallest (most negative) integer, in which case the
                 * result is a real number. <p>
                 *
                 * errors: stackunderflow, typecheck
                 */
                void eval() {
                    pushDouble(-popDouble());
                }
            });
            operationSet.add(new Operation("round") {

                /**
                 * <i>num1</i> <b>round</b> <i>num2</i> <p>
                 *
                 * returns the integer value nearest to num1.
                 * If num1 is equally close to its two nearest
                 * integers, round returns the greater of the two.
                 * The type of the result is the same as
                 * the type of the operand. <p>
                 *
                 * errors: stackunderflow, typecheck
                 */
                void eval() {
                    pushLong(Math.round(popDouble()));
                }
            });
            operationSet.add(new Operation("sin") {

                /**
                 * <i>angle</i> <b>sin</b> <i>real</i> <p>
                 *
                 * returns the sine of angle, which is interpreted as an
                 * angle in degrees. The result is a real number. <p>
                 *
                 * errors: stackunderflow, typecheck
                 */
                void eval() {
                    double radians = Math.toRadians(popDouble());
                    pushDouble(Math.toDegrees(Math.sin(radians)));
                }
            });
            operationSet.add(new Operation("sqrt") {

                /**
                 * <i>num</i> <b>sqrt</b> <i>real</i> <p>
                 *
                 * returns the square root of num, which must be a
                 * nonnegative number. The result is a real number. <p>
                 *
                 * errors: rangecheck, stackunderflow, typecheck
                 */
                void eval() {
                    pushDouble(Math.sqrt(popDouble()));
                }
            });
            operationSet.add(new Operation("sub") {

                /**
                 * <i>num1 num2</i> <b>sub</b> <i>difference</i> <p>
                 *
                 * returns the result of subtracting num2 from num1.
                 * If both operands are integers and the result is within
                 * integer range, the result is an integer; otherwise,
                 * the result is a real number. <p>
                 *
                 * errors: stackunderflow, typecheck, undefinedresult
                 */
                void eval() {
                    double num2 = popDouble();
                    double num1 = popDouble();
                    pushDouble(num1 - num2);
                }
            });
            operationSet.add(new Operation("truncate") {

                /**
                 * <i>num1</i> <b>truncate</b> <i>num2</i> <p>
                 *
                 * truncates num1 toward 0 by removing its fractional part.
                 * The type of the result is the same as the type of the
                 * operand. <p>
                 *
                 * errors: stackunderflow, typecheck
                 */
                void eval() {
                    double num1 = popDouble();
                    pushDouble(((double) ((long) num1) - num1));
                }
            });

            // Relational, boolean, and bitwise operators
            operationSet.add(new Operation("and") {

                /**
                 * <i>bool1|int1 bool2|int2</i> <b>and</b> <i>bool3|int3</i> <p>
                 *
                 * returns the logical conjunction of the operands
                 * if they are boolean. If the operands are integers,
                 * and returns the bitwise "and" of their binary
                 * representations. <p>
                 *
                 * errors: stackunderflow, typecheck
                 */
                void eval() {
                    pushLong(popLong() & popLong());
                }
            });
            operationSet.add(new Operation("bitshift") {

                /**
                 * <i>int1 <i>shift</i> <b>bitshift</b> <i>int2</i> <p>
                 *
                 * shifts the binary representation of int1 left by
                 * shift bits and returns the result. Bits shifted out
                 * are lost; bits shifted in are 0. If shift is negative,
                 * a right shift by –shift bits is performed.
                 * This operation produces an arithmetically correct
                 * result only for positive values of int1.
                 * Both int1 and shift must be integers. <p>
                 *
                 * errors: stackunderflow, typecheck
                 */
                void eval() {
                    long shift = popLong();
                    long int1 = popLong();
                    pushLong(int1 << shift);
                }
            });
            operationSet.add(new Operation("eq") {

                /**
                 * <i>any1 <i>any2</i> <b>eq</b> <i>bool</i> <p>
                 *
                 * pops two objects from the operand stack and pushes\
                 * true if they are equal, or false if not.
                 * The definition of equality depends on the types of
                 * the objects being compared.
                 * Simple objects are equal if their types and values
                 * are the same. Strings are equal if their lengths and
                 * individual elements are equal. Other composite objects
                 * (arrays and dictionaries) are equal only if they share
                 * the same value. Separate values are considered unequal,
                 * even if all the components of those values are the
                 * same.
                 * This operator performs some type conversions.
                 * Integers and real numbers can be compared freely:
                 * an integer and a real number representing the same
                 * mathematical value are considered equal by eq.
                 * Strings and names can likewise be compared freely:
                 * a name defined by some sequence of characters is equal
                 * to a string whose elements are the same sequence of
                 * characters.
                 * The literal/executable and access attributes of
                 * objects are not considered in comparisons
                 * between objects. <p>
                 *
                 * errors: invalidaccess, stackunderflow
                 */
                void eval() {
                    pushBoolean(popObject().equals(popObject()));
                }
            });
            operationSet.add(new Operation("false") {
                /**
                 * <b>false</b> <i>false</i> <p>
                 *
                 * pushes a boolean object whose value is false on the
                 * operand stack. false is not an operator; it is a name in
                 * systemdict associated with the boolean value false. <p>
                 *
                 * errors: stackoverflow
                 */
                void eval() {   
                    pushBoolean(false);
                }
            });
            operationSet.add(new Operation("ge") {
                /**
                 * <i>num1 num2</i> <b>ge</b> <i>bool</i> <p>
                 *
                 * pops two objects from the operand stack and pushes true
                 * if the first operand is greater than or equal to the second,
                 * or false otherwise. If both operands are numbers,
                 * ge compares their mathematical values. If both operands
                 * are strings, ge compares them element by element, treating
                 * the elements as integers in the range 0 to 255, to determine
                 * whether the first string is lexically greater than or equal
                 * to the second. If the operands are of other types or one
                 * is a string and the other is a number, a typecheck
                 * error occurs. <p>
                 *
                 * errors: invalidaccess, stackunderflow, typecheck
                 */
                void eval() {
                    double num2 = popDouble();
                    double num1 = popDouble();
                    pushBoolean(num1 >= num2);
                }
            });
            operationSet.add(new Operation("gt") {
                /**
                 * <i>num1 num2</i> <b>gt</b> <i>bool</i> <p>
                 *
                 * pops two objects from the operand stack and pushes true
                 * if the first operand is greater than the second, or
                 * false otherwise. If both operands are numbers, gt compares
                 * their mathematical values. If both operands are strings,
                 * gt compares them element by element, treating the elements
                 * as integers in the range 0 to 255, to determine whether
                 * the first string is lexically greater than the second.
                 * If the operands are of other types or one is a string
                 * and the other is a number, a typecheck error occurs. <p>
                 *
                 * errors: invalidaccess, stackunderflow, typecheck
                 */
                void eval() {
                    double num2 = popDouble();
                    double num1 = popDouble();
                    pushBoolean(num1 > num2);
                }
            });
            operationSet.add(new Operation("le") {
                /**
                 * <i>num1 num2</i> <b>le</b> <i>bool</i> <p>
                 *
                 * pops two objects from the operand stack and pushes true
                 * if the first operand is less than or equal to the second,
                 * or false otherwise. If both operands are numbers, le
                 * compares their mathematical values. If both operands are
                 * strings, le compares them element by element, treating
                 * the elements as integers in the range 0 to 255,
                 * to determine whether the first string is lexically less
                 * than or equal to the second. If the operands are of other
                 * types or one is a string and the other is a number, a
                 * typecheck error occurs.<p>
                 *
                 * errors: invalidaccess, stackunderflow, typecheck
                 */
                void eval() {
                    double num2 = popDouble();
                    double num1 = popDouble();
                    pushBoolean(num1 <= num2);
                }
            });
            operationSet.add(new Operation("lt") {
                /**
                 * <i>num1 num2</i> <b>lt</b> <i>bool</i> <p>
                 *
                 * pops two objects from the operand stack and pushes true
                 * if the first operand is less than the second, or false
                 * otherwise. If both operands are numbers, lt compares
                 * their mathematical values. If both operands are strings,
                 * lt compares them element by element, treating the elements
                 * as integers in the range 0 to 255, to determine whether
                 * the first string is lexically less than the second.
                 * If the operands are of other types or one is a string
                 * and the other is a number, a typecheck error occurs. <p>
                 *
                 * errors: invalidaccess, stackunderflow, typecheck
                 */
                void eval() {
                    double num2 = popDouble();
                    double num1 = popDouble();
                    pushBoolean(num1 < num2);
                }
            });
            operationSet.add(new Operation("ne") {
                /**
                 * <i>any1 any2</i> <b>ne</b> <i>bool</i> <p>
                 *
                 * pops two objects from the operand stack and pushes false
                 * if they are equal, or true if not. What it means for objects
                 * to be equal is presented in the description of the
                 * eq operator. <p>
                 *
                 * errors: invalidaccess, stackunderflow
                 */
                void eval() {
                    pushBoolean(!popObject().equals(popObject()));
                }
            });
            operationSet.add(new Operation("not") {
                /**
                 * <i>bool1|int1</i> <b>not</b> <i>bool2|int2</i> <p>
                 *
                 * returns the logical negation of the operand if it is
                 * boolean. If the operand is an integer, not returns the
                 * bitwise complement (ones complement) of its binary
                 * representation. <p>
                 *
                 * errors: stackunderflow, typecheck
                 */
                void eval() {
                    pushLong(~popLong());
                }
            });
            operationSet.add(new Operation("or") {
                /**
                 * <i>bool1|int1 bool2|int2</i> <b>or</b> <i>bool3|int3</i> <p>
                 *
                 * returns the logical disjunction of the operands if they
                 * are boolean. If the operands are integers, or returns
                 * the bitwise "inclusive or" of their binary representations. <p>
                 *
                 * errors: stackunderflow, typecheck
                 */
                void eval() {
                    pushLong(popLong() | popLong());
                }
            });
            operationSet.add(new Operation("true") {
                /**
                 * <b>true</b> <i>true</i> <p>
                 *
                 * pushes a boolean object whose value is true on the operand
                 * stack. true is not an operator; it is a name in systemdict
                 * associated with the boolean value true. <p>
                 *
                 * errors: stackoverflow
                 */
                void eval() {
                    pushBoolean(true);
                }
            });
            operationSet.add(new Operation("xor") {
                /**
                 * <i>bool1|int1 bool2|int2</i> <b>xor</b> <i>bool3|int3</i> <p>
                 *
                 * returns the logical "exclusive or" of the operands if they
                 * are boolean. If the operands are integers, xor returns the
                 * bitwise "exclusive or" of their binary representations. <p>
                 *
                 * errors: stackunderflow, typecheck
                 */
                void eval() {
                    pushLong(popLong() ^ popLong());
                }
            });

            // Conditional Operators
            operationSet.add(new Operation("if") {
                /**
                 * <i>bool {proc}</i> <b>if</b> - <p>
                 *
                 * removes both operands from the stack, then executes proc
                 * if bool is true. The if operator pushes no results of
                 * its own on the operand stack, but proc may do so (see
                 * Section 3.5, "Execution"). <p>
                 *
                 * Examples <p>
                 * 3 4 lt {(3 is less than 4)} if <p>
                 *
                 * errors: stackunderflow, typecheck
                 */
                void eval() {
                    if (popBoolean()) {
                        stack.addFirst(popExpression());
                    } else {
                        popExpression();
                    }
                }
            });
            operationSet.add(new Operation("ifelse") {
                /**
                 * <i>bool {expr1} {expr2}</i> <b>ifelse</b> - <p>
                 *
                 * removes all three operands from the stack, then
                 * executes proc1 if bool is true or proc2 if bool is false.
                 * The ifelse operator pushes no results of its own on the
                 * operand stack, but the procedure it executes may do so
                 * (see Section 3.5, "Execution"). <p>
                 *
                 * Examples <p>
                 * 4 3 lt {(TruePart)} {(FalsePart)} ifelse <br>
                 * results in FalsePart, since 4 is not less than 3 <p>
                 *
                 * errors: stackunderflow, typecheck
                 */
                void eval() {
                    // execute expr1 if bool is true, expr2 if false
                    if (popBoolean()) {
//                        expression.push(popExpression());
                        popExpression();
                    } else {
                        popExpression();
//                        expression.push(popExpression());
                    }
                }
            });

            // Stack Operators
            operationSet.add(new Operation("copy") {
                /**
                 * <i>any1 ... anyn n</i> <b>copy</b> <i>any1 ... anyn any1 ... anyn</i>
                 * <i>array1 array2</i> <b>copy</b> <i>subarray2</i> <br>
                 * <i>string1 string2</i> <b>copy</b> <i>substring2</i> <p>
                 *
                 * performs two entirely different functions, depending on the
                 * type of the topmost operand.
                 * In the first form, where the top element on the operand
                 * stack is a nonnegative integer n, copy pops n from the
                 * stack and duplicates the top n elements on the stack
                 * as shown above. This form of copy operates only on the
                 * objects themselves, not on the values of composite objects. <p>
                 *
                 * Examples<br>
                 * (a) (b) (c) 2 copy Þ (a) (b) (c) (b) (c) <br>
                 * (a) (b) (c) 0 copy Þ (a) (b) (c) <p>
                 *
                 * In the other forms, copy copies all the elements of the 
                 * first composite object into the second. The composite
                 * object operands must be of the same type, except that
                 * a packed array can be copied into an array (and only into
                 * an array—copy cannot copy into packed arrays, because
                 * they are read-only). This form of copy copies the value of
                 * a composite object. This is quite different from dup and
                 * other operators that copy only the objects themselves
                 * (see Section 3.3.1, "Simple and Composite Objects").
                 * However, copy performs only one level of copying.
                 * It does not apply recursively to elements that are
                 * themselves composite objects; instead, the values
                 * of those elements become shared. In the case of arrays or
                 * strings, the length of the second object must be at least as
                 * great as the first; copy returns the initial subarray or
                 * substring of the second operand into which the elements
                 * were copied. Any remaining elements of array2 or
                 * string2 are unaffected. <p>
                 * 
                 * Example: <br>
                 * /a1 [1 2 3] def<br>
                 * a1 dup length array copy Þ [1 2 3] <p>
                 *
                 * errors: invalidaccess, rangecheck, stackoverflow,
                 * stackunderflow, typecheck
                 */
                void eval() {
                    long count = popLong();
// ????
                    Object obj = stack.removeFirst();
                    stack.addFirst(obj);
                    stack.addFirst(obj);
                }
            });
            operationSet.add(new Operation("dup") {
                /**
                 * <i>any</i> <b>dup</b> <i>any any</i> <p>
                 *
                 * duplicates the top element on the operand stack.
                 * dup copies only the object; the value of a composite
                 * object is not copied but is shared.
                 * See Section 3.3, "Data Types and Objects." <p>
                 *
                 * errors: stackoverflow, stackunderflow
                 */
                void eval() {
                    Object obj = popObject();
                    pushObject(obj);
                    pushObject(obj);
                }
            });
            operationSet.add(new Operation("exch") {

                void eval() {   // <i>any1 any2</i> <b>exch</b> <i>any2 any1</i> - exchange top of stack
                    Object any1 = popObject();
                    Object any2 = popObject();
                    pushObject(any2);
                    pushObject(any1);
                }
            });
            operationSet.add(new Operation("index") {

                void eval() {   // <i>anyn ... any0 n</i> <b>index</b> <i>anyn ... any0 anyn</i>
                    Object obj = stack.removeFirst();
                    stack.addFirst(obj);
                    stack.addFirst(obj);
                }
            });
            operationSet.add(new Operation("pop") {

                void eval() {   // discard top element
                    stack.removeFirst();
                }
            });
            operationSet.add(new Operation("roll") {

                void eval() {
                    // <i>anyn-1 ... any0 n j</i> <b>roll</b> <i>any(j-1)mod n ... anyn-1 ... any</i>
                    // Roll n elements up j times
                    Object obj = stack.removeFirst();
                    stack.addFirst(obj);
                    stack.addFirst(obj);
                }
            });
        }
    }

    /** Read the function information from a PDF Object */
    protected void parse(PDFObject obj) throws IOException {
        // read the postscript from the stream
        readPS(obj.getStreamBuffer());
        throw new PDFParseException("Unsupported function type 4.");
    }

    /**
     * Map from <i>m</i> input values to <i>n</i> output values.
     * The number of inputs <i>m</i> must be exactly one half the size of the
     * domain.  The number of outputs should match one half the size of the
     * range.
     *
     * @param inputs an array of <i>m</i> input values
     * @param outputs an array of size <i>n</i> which will be filled
     *                with the output values, or null to return a new array
     */
    protected void doFunction(float[] inputs, int inputOffset,
            float[] outputs, int outputOffset) {
    }

    private boolean popBoolean() {
        return false;
    }

    private void pushBoolean(boolean arg) {
    }

    private double popDouble() {
        return 0;
    }

    private void pushDouble(double arg) {
    }

    private Expression popExpression() {
        return null;
    }

    private void pushExpression(Expression expresson) {
    }

    private long popLong() {
        return 0L;
    }

    private void pushLong(long arg) {
    }

    private Object popObject() {
        return stack.removeFirst();
    }

    private void pushObject(Object obj) {
        stack.addFirst(obj);
    }

    /**
     * <p>parse the postscript operators and aguments from the stream.</p>
     *
     * <p>Syntax is to read a set of tokens, including expressions and
     * to queue them as they come including other expressions. Expressions are
     * enclosed in curly brackets and constitute a reference to the
     * expression body.</p>
     *
     * @param buf the stream of postscript tokens
     */
    private void readPS(ByteBuffer buf) {
    }

    class Expression extends LinkedList {

        public boolean equals(Object obj) {
            if (obj instanceof Expression) {
                // actually validate the list contents are the same expressions
                return true;
            }
            return false;
        }
    }

    abstract class Operation {

        private String operatorName;

        public Operation(String operatorName) {
            if (operatorName == null) {
                throw new RuntimeException("Cannot have a null operator name");
            }
            this.operatorName = operatorName;
        }

        public String getOperatorName() {
            return operatorName;
        }

        /**
         * evaluate the function, popping the stack as needed and pushing results.
         */
        abstract void eval();

        /**
         * return true if our operator is the same as the supplied one.
         *
         * @param obj
         * @return
         */
        public boolean equals(Object obj) {
            if (obj instanceof Operation) {
                return ((Operation) obj).operatorName.equals(operatorName);
            } else if (obj instanceof String) {
                return operatorName.equals(obj);
            }
            return false;
        }
    }
}


