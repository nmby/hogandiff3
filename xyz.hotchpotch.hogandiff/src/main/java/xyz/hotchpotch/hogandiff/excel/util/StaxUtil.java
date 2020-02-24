package xyz.hotchpotch.hogandiff.excel.util;

import java.util.Objects;

import javax.xml.namespace.QName;
import javax.xml.stream.events.XMLEvent;

/**
 * StAX (Streaming API for XML) と組み合わせて利用すると便利な機能を集めた
 * ユーティリティクラスです。<br>
 *
 * @author nmby
 */
public class StaxUtil {
    
    // [static members] ********************************************************
    
    /**
     * xmlns = {@code "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}
     * の各種QNAMEを提供します。<br>
     *
     * @author nmby
     */
    public static class QNAME {
        
        // [static members] ----------------------------------------------------
        
        /** xmlns */
        public static final String XMLNS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
        
        /** bgColor */
        public static final QName BG_COLOR = new QName(XMLNS, "bgColor");
        
        /** border */
        public static final QName BORDER = new QName(XMLNS, "border");
        
        /** borders */
        public static final QName BORDERS = new QName(XMLNS, "borders");
        
        /** bottom */
        public static final QName BOTTOM = new QName(XMLNS, "bottom");
        
        /** c */
        public static final QName C = new QName(XMLNS, "c");
        
        /** col */
        public static final QName COL = new QName(XMLNS, "col");
        
        /** cols */
        public static final QName COLS = new QName(XMLNS, "cols");
        
        /** color */
        public static final QName COLOR = new QName(XMLNS, "color");
        
        /** conditionalFormatting */
        public static final QName CONDITIONAL_FORMATTING = new QName(XMLNS, "conditionalFormatting");
        
        /** diagonal */
        public static final QName DIAGONAL = new QName(XMLNS, "diagonal");
        
        /** fgColor */
        public static final QName FG_COLOR = new QName(XMLNS, "fgColor");
        
        /** fill */
        public static final QName FILL = new QName(XMLNS, "fill");
        
        /** fills */
        public static final QName FILLS = new QName(XMLNS, "fills");
        
        /** font */
        public static final QName FONT = new QName(XMLNS, "font");
        
        /** fonts */
        public static final QName FONTS = new QName(XMLNS, "fonts");
        
        /** gradientFill */
        public static final QName GRADIENT_FILL = new QName(XMLNS, "gradientFill");
        
        /** left */
        public static final QName LEFT = new QName(XMLNS, "left");
        
        /** patternFill */
        public static final QName PATTERN_FILL = new QName(XMLNS, "patternFill");
        
        /** right */
        public static final QName RIGHT = new QName(XMLNS, "right");
        
        /** row */
        public static final QName ROW = new QName(XMLNS, "row");
        
        /** sheetData */
        public static final QName SHEET_DATA = new QName(XMLNS, "sheetData");
        
        /** sheetPr */
        public static final QName SHEET_PR = new QName(XMLNS, "sheetPr");
        
        /** tabColor */
        public static final QName TAB_COLOR = new QName(XMLNS, "tabColor");
        
        /** top */
        public static final QName TOP = new QName(XMLNS, "top");
        
        // [instance members] --------------------------------------------------
        
        private QNAME() {
        }
    }
    
    /**
     * それ自体は xmlns の定義されない各種QNAMEを提供します。<br>
     * 
     * @author nmby
     */
    public static class NONS_QNAME {
        
        // [static members] ----------------------------------------------------
        
        /** customFormat */
        public static final QName CUSTOM_FORMAT = new QName(null, "customFormat");
        
        /** max */
        public static final QName MAX = new QName(null, "max");
        
        /** min */
        public static final QName MIN = new QName(null, "min");
        
        /** patternFill */
        public static final QName PATTERN_TYPE = new QName(null, "patternType");
        
        /** r */
        public static final QName R = new QName(null, "r");
        
        /** s */
        public static final QName S = new QName(null, "s");
        
        /** spans */
        public static final QName SPANS = new QName(null, "spans");
        
        /** style */
        public static final QName STYLE = new QName(null, "style");
        
        /** width */
        public static final QName WIDTH = new QName(null, "width");
        
        // [instance members] --------------------------------------------------
        
        private NONS_QNAME() {
        }
    }
    
    /**
     * 指定されたイベントが指定されたQNAMEの開始要素であるかを返します。<br>
     * 
     * @param event XMLイベント
     * @param qName QNAME
     * @return 指定されたイベントが指定されたQNAMEの開始要素である場合は {@code true}
     * @throws NullPointerException {@code event}, {@code qName} のいずれかが {@code null} の場合
     */
    public static boolean isStart(XMLEvent event, QName qName) {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(qName, "qName");
        
        return event.isStartElement() && qName.equals(event.asStartElement().getName());
    }
    
    /**
     * 指定されたイベントが指定されたQNAMEの終了要素であるかを返します。<br>
     * 
     * @param event XMLイベント
     * @param qName QNAME
     * @return 指定されたイベントが指定されたQNAMEの終了要素である場合は {@code true}
     * @throws NullPointerException {@code event}, {@code qName} のいずれかが {@code null} の場合
     */
    public static boolean isEnd(XMLEvent event, QName qName) {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(qName, "qName");
        
        return event.isEndElement() && qName.equals(event.asEndElement().getName());
    }
    
    // [instance members] ******************************************************
    
    private StaxUtil() {
    }
}
