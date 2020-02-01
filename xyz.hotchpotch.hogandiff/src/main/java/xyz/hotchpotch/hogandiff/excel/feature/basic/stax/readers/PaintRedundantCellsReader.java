package xyz.hotchpotch.hogandiff.excel.feature.basic.stax.readers;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import xyz.hotchpotch.hogandiff.excel.CellReplica;
import xyz.hotchpotch.hogandiff.excel.feature.basic.stax.XSSFBookPainterWithStax.StylesManager;
import xyz.hotchpotch.hogandiff.excel.util.StaxUtil;
import xyz.hotchpotch.hogandiff.excel.util.StaxUtil.NONS_QNAME;
import xyz.hotchpotch.hogandiff.excel.util.StaxUtil.QNAME;
import xyz.hotchpotch.hogandiff.util.Pair;

/**
 * 余剰行や余剰列上のセルに色を付ける {@link XMLEventReader} の実装です。<br>
 * 具体的には、.xlsx/.xlsm 形式のExcelファイルの各ワークシートに対応する
 * xl/worksheets/sheet?.xml エントリを処理対象とし、
 * {@code <c>} 要素に対する操作を行います。<br>
 *
 * @author nmby
 */
public class PaintRedundantCellsReader extends BufferingReader {
    
    // [static members] ********************************************************
    
    private static final XMLEventFactory eventFactory = XMLEventFactory.newFactory();
    
    /**
     * 新しいリーダーを構成します。<br>
     * 
     * @param source ソースリーダー
     * @param stylesManager スタイルマネージャ
     * @param redundantRows 余剰行インデックス（0 開始）
     * @param redundantColumns 余剰列インデックス（0 開始）
     * @param colorIdx 着色する色のインデックス
     * @return 新しいリーダー
     */
    public static XMLEventReader of(
            XMLEventReader source,
            StylesManager stylesManager,
            List<Integer> redundantRows,
            List<Integer> redundantColumns,
            short colorIdx) {
        
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(stylesManager, "stylesManager");
        Objects.requireNonNull(redundantRows, "redundantRows");
        Objects.requireNonNull(redundantColumns, "redundantColumns");
        
        return new PaintRedundantCellsReader(
                source,
                stylesManager,
                redundantRows,
                redundantColumns,
                colorIdx);
    }
    
    // [instance members] ******************************************************
    
    private final StylesManager stylesManager;
    private final Set<Integer> redundantRows;
    private final Set<Integer> redundantColumns;
    private final short colorIdx;
    
    private PaintRedundantCellsReader(
            XMLEventReader source,
            StylesManager stylesManager,
            List<Integer> redundantRows,
            List<Integer> redundantColumns,
            short colorIdx) {
        
        super(source);
        
        assert stylesManager != null;
        assert redundantRows != null;
        assert redundantColumns != null;
        
        this.stylesManager = stylesManager;
        this.redundantRows = Set.copyOf(redundantRows);
        this.redundantColumns = Set.copyOf(redundantColumns);
        this.colorIdx = colorIdx;
    }
    
    @Override
    protected void seekNext() throws XMLStreamException {
        if (!source.hasNext()) {
            return;
        }
        XMLEvent event = source.peek();
        if (!StaxUtil.isStart(event, QNAME.C)) {
            return;
        }
        
        String address = event.asStartElement().getAttributeByName(NONS_QNAME.R).getValue();
        Pair<Integer> idx = CellReplica.addressToIdx(address);
        int row = idx.a();
        int column = idx.b();
        
        if (redundantRows.contains(row) || redundantColumns.contains(column)) {
            buffer.add(paintCell(event.asStartElement()));
            source.nextEvent();
        }
    }
    
    /**
     * c 要素開始イベントを受け取り、適用するスタイルを着色スタイルに変更して返します。<br>
     * 
     * @param original c 要素開始イベント
     * @return 適用するスタイルを着色スタイルに変更した c 要素開始イベント
     */
    private StartElement paintCell(StartElement original) {
        int currStyleIdx = Optional
                .ofNullable(original.getAttributeByName(NONS_QNAME.S))
                .map(Attribute::getValue)
                .map(Integer::parseInt)
                .orElse(0);
        int newStyleIdx = stylesManager.getPaintedStyle(currStyleIdx, colorIdx);
        
        Map<QName, Attribute> newAttrs = new HashMap<>();
        newAttrs.put(
                NONS_QNAME.S,
                eventFactory.createAttribute(NONS_QNAME.S, Integer.toString(newStyleIdx)));
        
        Iterator<Attribute> itr = original.getAttributes();
        while (itr.hasNext()) {
            Attribute attr = itr.next();
            if (!newAttrs.containsKey(attr.getName())) {
                newAttrs.put(attr.getName(), attr);
            }
        }
        
        return eventFactory.createStartElement(QNAME.C, newAttrs.values().iterator(), null);
    }
}
