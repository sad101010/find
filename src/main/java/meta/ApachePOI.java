package meta;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.poifs.filesystem.DocumentInputStream;
import org.apache.poi.poifs.filesystem.NPOIFSFileSystem;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import util.DateBean;
import util.TimeBean;

public class ApachePOI {

    public static String getDocText(File file) {
        WordExtractor extractor;
        try {
            NPOIFSFileSystem fs = new NPOIFSFileSystem(file);
            extractor = new WordExtractor(fs.getRoot());
        } catch (Exception|Error e) {
            return null;
        }
        StringBuilder s=new StringBuilder();
        for (String rawText : extractor.getParagraphText()) {
            String text = extractor.stripFields(rawText);
            s.append(text);
        }
        return s.toString();
    }

    public static boolean AddDocTags(File file, Map<String, String> map) {
        POIFSFileSystem fs;
        try {
            fs = new POIFSFileSystem(file);
        } catch (Exception | Error e) {
            return false;
        }
        DirectoryEntry root = fs.getRoot();
        DocumentInputStream stream;
        try {
            stream = fs.createDocumentInputStream(info);
        } catch (Exception | Error e) {
            return false;
        }
        byte[] content = new byte[stream.available()];
        try {
            stream.read(content);
        } catch (IOException ex) {
            return false;
        }
        stream.close();
        return rawDocSummaryInfo(content, map);
    }

    //строка "summaryinformation" с кодом 5 в начале строки
    private static final String info = new String(new byte[]{5, 83, 117, 109, 109, 97, 114, 121, 73, 110, 102, 111, 114, 109, 97, 116, 105, 111, 110});

    private static String fields[] = {
        "Неизвестное поле",//0x00
        "Кодировка полей метаданных",//0x01
        "Название",//0x02
        "Тема",//0x03
        "Автор",//0x04
        "Ключевые слова",//0x05
        "Комментарии",//0x06
        "Шаблон",//0x07
        "Последний автор",//0x08
        "Редакция",//0x09
        "Время редактирования",//0x0A
        "Дата последней печати",//0x0B
        "Дата создания",//0x0C
        "Дата последнего сохранения",//0x0D
        "Количество страниц",//0x0E
        "Количество слов",//0x0F
        "Количество символов",//0x10
        "Неизвестное поле",//11
        "Имя программы",//0x12
        "Защищенный"//0x13
    };

    public static boolean rawDocSummaryInfo(byte[] a, Map map) {
        //ТОЛЬКО КОДИРОВКА WINDOWS-1251 !!!
        ByteBuffer bb = ByteBuffer.wrap(a);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.position(44);
        int base = bb.getInt(), size = bb.getInt(), n = bb.getInt();
        System.out.println();
        for (; n > 0; n--) {
            short type = bb.getShort();
            bb.getShort();//есть padding из нулей
            int offset = bb.getInt();
            String value = getValue(bb, type, base + offset);
            if (type == 0x01 && value != null && !value.equals("1251")) {
                return false;
            }
            if (type < fields.length) {
                if (value != null) {
                    map.put(fields[type], value);
                } else {
                    System.err.println("RawDoc: null value for type " + type);
                }
            } else {
                System.err.println("RawDoc: uknown type " + type);
            }
        }
        return true;
    }

    private static String getValue(ByteBuffer bb, short type, int addr) {
        switch (type) {
            case 0x01:
                return String.valueOf(bb.getShort(addr + 4));
            case 0x02:
            case 0x03:
            case 0x04:
            case 0x05:
            case 0x06:
            case 0x07:
            case 0x08:
            case 0x09:
            case 0x12:
                return getString(bb, addr);
            case 0x0A:
                return TimeBean.valueOf(bb.getLong(addr + 4) / 10000000).toString();
            case 0x0B:
            case 0x0C:
            case 0x0D:
                return DateBean.MSFileTimeToDateBean(bb.getLong(addr + 4)).toString();
            case 0x0E:
            case 0x0F:
            case 0x10:
            case 0x13:
                return String.valueOf(bb.getInt(addr + 4));
            default:
                return null;
        }
    }

    private static String getString(ByteBuffer bb, int addr) {
        int n = 0, pos = bb.position();
        bb.position(addr + 8);//8 байт на инф. про строку
        while (true) {
            byte b = bb.get();
            if (b == 0) {
                break;
            }
            n++;
        }
        if (n == 0) {
            bb.position(pos);
            return "";
        }
        byte bytes[] = new byte[n];
        bb.position(bb.position() - n - 1);
        bb.get(bytes, 0, n);
        bb.position(pos);
        try {
            return new String(bytes, "windows-1251");
        } catch (Exception | Error e) {
            return null;
        }
    }
}
