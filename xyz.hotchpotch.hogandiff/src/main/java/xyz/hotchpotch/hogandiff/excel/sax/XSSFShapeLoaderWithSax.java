package xyz.hotchpotch.hogandiff.excel.sax;

import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import xyz.hotchpotch.hogandiff.excel.BookType;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.ShapeLoader;
import xyz.hotchpotch.hogandiff.excel.ShapeReplica;
import xyz.hotchpotch.hogandiff.excel.SheetType;
import xyz.hotchpotch.hogandiff.excel.common.BookHandler;
import xyz.hotchpotch.hogandiff.excel.common.CommonUtil;
import xyz.hotchpotch.hogandiff.excel.common.SheetHandler;
import xyz.hotchpotch.hogandiff.excel.sax.SaxUtil.SheetInfo;

/**
 * SAX (Simple API for XML) を利用して、
 * .xlsx/.xlsm 形式のExcelブックのワークシートから
 * 図形データを抽出する {@link ShapeLoader} の実装です。<br>
 *
 * @author nmby
 */
@BookHandler(targetTypes = { BookType.XLSX, BookType.XLSM })
@SheetHandler(targetTypes = { SheetType.WORKSHEET })
public class XSSFShapeLoaderWithSax implements ShapeLoader {
    
    // [static members] ********************************************************
    
    private static class Handler1 extends DefaultHandler {
        
        // [static members] ----------------------------------------------------
        
        // [instance members] --------------------------------------------------
        
        private final Set<ShapeReplica> shapes = new HashSet<>();
        
        private int id;
        private StringBuilder text;
        
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            
            switch (qName) {
            case "xdr:cNvPr":
                id = Integer.parseInt(attributes.getValue("id"));
                break;
            
            case "a:p":
                if (text == null) {
                    text = new StringBuilder();
                } else {
                    text.append("\n");
                }
                break;
            }
        }
        
        @Override
        public void characters(char ch[], int start, int length) {
            if (text != null) {
                text.append(ch, start, length);
            }
        }
        
        @Override
        public void endElement(String uri, String localName, String qName) {
            if ("xdr:txBody".equals(qName)) {
                if (0 < id && text != null) {
                    shapes.add(ShapeReplica.of(id, text.toString()));
                }
                id = 0;
                text = null;
            }
        }
    }
    
    /**
     * 新しいローダーを構成します。<br>
     * 
     * @param bookPath Excelブックのパス
     * @return 新しいローダー
     * @throws NullPointerException
     *              {@code bookPath} が {@code null} の場合
     * @throws IllegalArgumentException
     *              {@code bookPath} がサポート対象外の形式もしくは不明な形式の場合
     * @throws ExcelHandlingException
     *              ローダーの構成に失敗した場合。
     *              具体的には、Excelブックから共通情報の取得に失敗した場合
     */
    public static ShapeLoader of(Path bookPath) throws ExcelHandlingException {
        Objects.requireNonNull(bookPath, "bookPath");
        CommonUtil.ifNotSupportedBookTypeThenThrow(
                XSSFCellLoaderWithSax.class,
                BookType.of(bookPath));
        
        return new XSSFShapeLoaderWithSax(bookPath);
    }
    
    // [instance members] ******************************************************
    
    private final Path bookPath;
    private final Map<String, SheetInfo> nameToInfo;
    
    private XSSFShapeLoaderWithSax(Path bookPath) throws ExcelHandlingException {
        assert bookPath != null;
        assert CommonUtil.isSupportedBookType(getClass(), BookType.of(bookPath));
        
        this.bookPath = bookPath;
        this.nameToInfo = SaxUtil.loadSheetInfo(bookPath).stream()
                .collect(Collectors.toMap(
                        SheetInfo::name,
                        Function.identity()));
    }
    
    @Override
    public Set<ShapeReplica> loadShapes(Path bookPath, String sheetName)
            throws ExcelHandlingException {
        
        Objects.requireNonNull(bookPath, "bookPath");
        Objects.requireNonNull(sheetName, "sheetName");
        if (!this.bookPath.equals(bookPath)) {
            throw new IllegalArgumentException(String.format(
                    "このローダーは %s 用に構成されています。別ブック（%s）には利用できません。",
                    this.bookPath, bookPath));
        }
        
        try (FileSystem fs = FileSystems.newFileSystem(bookPath)) {
            
            if (!nameToInfo.containsKey(sheetName)) {
                // 例外カスケードポリシーに従い、
                // 後続の catch でさらに ExcelHandlingException にラップする。
                // ちょっと気持ち悪い気もするけど。
                throw new NoSuchElementException("シートが存在しません：" + sheetName);
            }
            SheetInfo info = nameToInfo.get(sheetName);
            // 同じく、後続の catch でさらに ExcelHandlingException にラップする。
            CommonUtil.ifNotSupportedSheetTypeThenThrow(getClass(), EnumSet.of(info.type()));
            
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            
            Handler1 handler1 = new Handler1();
            try (InputStream is = Files.newInputStream(fs.getPath(info.drawingSource()))) {
                parser.parse(is, handler1);
            }
            
            return Set.copyOf(handler1.shapes);
            
        } catch (Exception e) {
            throw new ExcelHandlingException(String.format(
                    "処理に失敗しました：%s - %s", bookPath, sheetName), e);
        }
    }
}
