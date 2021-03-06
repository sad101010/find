package meta;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;
import static meta.FAttr.FAttrFields;
import static meta.db.mimedb;
import util.DateBean;
import util.TimeBean;
import util.breader;
import static util.util.exit_with_err_msg;

public class type {

    private static final Map<String, String> typeMap = new TreeMap<String, String>();

    static {
        breader reader = null;
        try {
            reader = new breader(new File("data/types.txt"));
        } catch (Exception | Error e) {
            exit_with_err_msg("Ошибка чтения типов", null);
        }
        String type = null;
        while (true) {
            String line = null;
            try {
                line = reader.readLine();
            } catch (Exception | Error e) {
                exit_with_err_msg("Ошибка чтения типов", null);
            }
            if (line == null) {
                break;
            }
            if (line.startsWith("@")) {
                type = line;
                continue;
            }
            if (type == null) {
                exit_with_err_msg("Ошибка чтения типов", null);
            }
            typeMap.put(line, type);
        }
        try {
            reader.close();
        } catch (Exception | Error e) {
            exit_with_err_msg("Ошибка закрытия breader", null);
        }
        for (String mime : mimedb.keySet()) {
            Map<String, String> map = mimedb.get(mime);
            for (Map.Entry<String, String> e : map.entrySet()) {
                if (getFieldType(e.getValue()) == null) {
                    System.err.println("Нет значения>" + e.getValue() + ">mime>" + mime);
                    System.exit(-1);
                }
            }
        }
        for (String FAttrField : FAttrFields) {
            if (getFieldType(FAttrField) == null) {
                System.err.println("Нет значения>" + FAttrField + " >mime> FAttr");
                System.exit(-1);
            }
        }
    }

    public static Object parseFieldValue(String field, String value) {
        if (field.equals("Время редактирования")) {
            return TimeBean.valueOf(value);
        }
        switch (getFieldType(field)) {
            case "@Long":
                try {
                return Long.valueOf(value);
            } catch (Exception | Error e) {
                return null;
            }
            case "@DateBean":
                return DateBean.valueOf(value);
            case "@String":
                return value;
            default:
                return null;
        }
    }

    public static String getFieldType(String field) {
        return typeMap.get(field);
    }
}
