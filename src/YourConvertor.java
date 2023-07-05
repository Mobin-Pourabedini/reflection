import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YourConvertor{
    public Object deserialize(String input, String className) {
        Class<?> clazz = null;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        Object o = new Object();
        try {
            o = clazz.getConstructor().newInstance();
            o.getClass();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }
        List<Field> myFields = new ArrayList<>();
        // add all fields
        myFields.addAll(Arrays.asList(o.getClass().getDeclaredFields()));
        myFields.addAll(Arrays.asList(o.getClass().getSuperclass().getDeclaredFields()));
        // remove static fields
        myFields.removeIf(field -> java.lang.reflect.Modifier.isStatic(field.getModifiers()));
        // deserialize
        // split by comma if not in brackets
        String[] fieldStrings = input.substring(1, input.length() - 1).split(",(?![^\\(\\[]*[\\]\\)])");

        for (String string : fieldStrings) {
            System.out.println(string);
            // split by first colon
            String[] keyValue = string.split(":", 2);
            String key = keyValue[0].substring(1, keyValue[0].length() - 1);
            String value = keyValue[1];
            Field field = null;
            try {
                field = clazz.getDeclaredField(key);
            } catch (NoSuchFieldException e) {
                try {
                    field = clazz.getSuperclass().getDeclaredField(key);
                } catch (NoSuchFieldException ex) {
                    throw new RuntimeException(ex);
                }
            }
            Class type = field.getType();
            field.setAccessible(true);
            if (type.equals(String.class)) {
                try {
                    // check field has setter
                    try {
                        // check if field has getter
                        Method declaredMethod = field.getDeclaringClass()
                                .getDeclaredMethod("set" + field.getName().substring(0, 1)
                                        .toUpperCase() + field.getName().substring(1), String.class);
                        declaredMethod.invoke(o, value.substring(1, value.length() - 1));
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    } catch (NoSuchMethodException e) {
                        field.set(o, value.substring(1, value.length() - 1));
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            } else if (type.equals(Character.TYPE)) {
                try {
                    field.set(o, value.charAt(1));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            } else if (type.equals(Integer.TYPE)) {
                try {
                    field.set(o, Integer.parseInt(value));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            } else if (type.equals(Double.TYPE)) {
                try {
                    field.set(o, Double.parseDouble(value));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            } else if (type.equals(Float.TYPE)) {
                try {
                    field.set(o, Float.parseFloat(value));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            } else if (type.equals(ArrayList.class)) {
                Pattern pattern = Pattern.compile("\\d+");
                Matcher matcher = pattern.matcher(value);
                ArrayList<Integer> array = new ArrayList<>();
                while (matcher.find()) {
                    array.add(Integer.parseInt(matcher.group()));
                }
                try {
                    field.set(o, array);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    field.set(o, deserialize(value, type.getName()));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return o;
    }

    public String serialize(Object input) {
        List<Field> myFields = new ArrayList<>();
        // add all fields
        myFields.addAll(Arrays.asList(input.getClass().getDeclaredFields()));
        myFields.addAll(Arrays.asList(input.getClass().getSuperclass().getDeclaredFields()));
        // remove static fields
        myFields.removeIf(field -> java.lang.reflect.Modifier.isStatic(field.getModifiers()));
        // sort fields by name
        myFields.sort(Comparator.comparing(Field::getName));
        // serialize
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (Field field : myFields) {
            field.setAccessible(true);
            if (field.getType().equals(ArrayList.class)) {
                sb.append("\"").append(field.getName()).append("\":");
                sb.append("[");
                try {
                    ArrayList<Object> array = (ArrayList<Object>) field.get(input);
                    for (int i = 0; i < array.size(); i++) {
                        sb.append(array.get(i));
                        if (i != array.size() - 1) {
                            sb.append(",");
                        }
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                sb.append("]");
            } else if (field.getType().equals(String.class) || field.getType().equals(Character.TYPE)) {
                sb.append("\"").append(field.getName()).append("\":");
                try {
                    // check if field has getter
                    Method declaredMethod = field.getDeclaringClass()
                            .getDeclaredMethod("get" + field.getName().substring(0, 1)
                                    .toUpperCase() + field.getName().substring(1));
                    sb.append("\"").append(declaredMethod.invoke(input)).append("\"");
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    try {
                        sb.append("\"").append(field.get(input)).append("\"");
                    } catch (IllegalAccessException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            } else if (field.getType().isPrimitive()) {
                sb.append("\"").append(field.getName()).append("\":");
                try {
                    sb.append(field.get(input));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            } else {
                sb.append("\"").append(field.getName()).append("\":");
                try {
                    sb.append(serialize(field.get(input)));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            sb.append(",");
        }
        sb.replace(sb.length()-1, sb.length(), "}");
        return sb.toString();
    }
}