package one.axim.gradle.generator;

import one.axim.gradle.data.*;
import one.axim.gradle.utils.XPageSchema;
import one.axim.gradle.generator.data.FieldData;
import one.axim.gradle.generator.data.ModelData;
import one.axim.gradle.generator.data.ModelDefinition;
import one.axim.gradle.generator.utils.TypeMapUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

public class ModelGenerator {

    private static final Gson gson = new Gson();

    public ModelDefinition generatorRequestModelObject(APIDefinition api, String modelPath, LanguageType languageType)
            throws IOException {
        java.util.List<APIParameter> params = api.getParameters();
        ModelDefinition modelDefinition = new ModelDefinition("", api.getClassName());

        for (APIParameter param : params) {
            if (param.getType().equals("Object") && param.getParameterKind().equals(APIParameterKind.REQUEST_BODY)) {
                makeModelDefinition(languageType, modelDefinition, api.getClassName(), "Request", param.getClassPath(),
                        modelPath);
            }
        }

        return modelDefinition;
    }

    public ModelDefinition generatorResponseModelObject(APIDefinition api, String modelPath, LanguageType languageType)
            throws IOException {

        ModelDefinition modelDefinition = new ModelDefinition("", api.getClassName());

        if (!api.getReturnClass().toLowerCase().equals("void")) {
            makeModelDefinition(languageType, modelDefinition, api.getClassName(), "Response", api.getReturnClass(),
                    modelPath, api.getIsPaging());
        }

        return modelDefinition;
    }

    public void makeModelDefinition(LanguageType languageType, ModelDefinition modelDefinition, String className,
                                    String suffix, String modelClass, String modelPath) throws IOException {
        makeModelDefinition(languageType, modelDefinition, className, suffix, modelClass, modelPath, false);
    }

    public void makeModelDefinition(LanguageType languageType, ModelDefinition modelDefinition, String className,
                                    String suffix, String modelClass, String modelPath, boolean useXPage) throws IOException {

        File modelFile = new File(modelPath + File.separator + modelClass + ".json");
        if (modelFile.exists()) {

            String clsName = className + suffix;

            try {

                if (useXPage) {
                    ModelData modelData = new ModelData();
                    modelData.setClassName(className + "Page" + suffix);

                    Field[] fields = XPageSchema.class.getDeclaredFields();
                    for (Field field : fields) {

                        FieldData fieldData = new FieldData();
                        fieldData.setName(field.getName());
                        fieldData.setComment(getXPageObjectCommentByName(field.getName()));
                        fieldData.setOptional(false);

                        if (fieldData.getName().equals("orders")) {
                            fieldData.setOptional(true);

                            String orderClassName = className + "PageOrder" + suffix;
                            fieldData.setType(TypeMapUtils.GetTypeByLanuage("Array", languageType) + "<" + orderClassName + ">");

                            ModelData orderModelData = new ModelData();
                            orderModelData.setClassName(orderClassName);

                            FieldData orderFieldData = new FieldData();
                            orderFieldData.setName("direction");
                            orderFieldData.setComment("정렬 방식 (ASC,DESC)");
                            orderFieldData.setOptional(true);
                            orderFieldData.setType(TypeMapUtils.GetTypeByLanuage("String", languageType));

                            orderModelData.addField(orderFieldData);

                            FieldData orderColumnFieldData = new FieldData();
                            orderColumnFieldData.setName("column");
                            orderColumnFieldData.setComment("정렬에 사용된 컬럼 이름");
                            orderColumnFieldData.setOptional(true);
                            orderColumnFieldData.setType(TypeMapUtils.GetTypeByLanuage("String", languageType));

                            orderModelData.addField(orderColumnFieldData);

                            modelDefinition.addModel(orderModelData);
                        } else {

                            String type = StringUtils.substringAfterLast(field.getType().getTypeName(), ".");

                            if (type.equals("List")) {
                                type = TypeMapUtils.GetTypeByLanuage("Array", languageType) + "<" + clsName + ">";
                            } else {
                                type = TypeMapUtils.GetTypeByLanuage(type, languageType);
                            }

                            if (type == null) {
                                type = TypeMapUtils.GetTypeByLanuage(field.getType().getTypeName(), languageType);
                            }

                            if (type == null) {
                                throw new RuntimeException("gen code exception " + field.getType() + " type is not supported ...");
                            }

                            fieldData.setType(type);
                        }

                        modelData.addField(fieldData);
                    }

                    modelDefinition.setKeepModel(modelData);
                }

                APIModelDefinition apiModel =
                        gson.fromJson(FileUtils.readFileToString(modelFile, "UTF-8"), APIModelDefinition.class);

                ModelData modelData = new ModelData();
                modelData.setClassName(clsName);

                for (APIField field : apiModel.getFields()) {
                    FieldData fieldData = new FieldData();
                    fieldData.setName(field.getName());
                    fieldData.setComment(field.getDescription() == null ? "" : StringUtils.replace(field.getDescription(), "|", "\\|"));
                    fieldData.setOptional(field.isOptional());
                    fieldData.setEnum(false);
                    fieldData.setClassPath(field.getClassPath());

                    String type;
                    if (field.getType().equals("Object")) {

                        String cls = className + toNameUpperCase(field.getName());
                        makeModelDefinition(languageType, modelDefinition, cls, suffix, field.getClassPath(), modelPath);

                        type = cls + suffix;
                    } else if (field.getType().equals("Array")) {

                        String cls = className + toNameUpperCase(field.getName());

                        if (TypeMapUtils.isNormalDataType(field.getClassPath())) {
                            String tempType = TypeMapUtils.GetTypeByLanuage(getClassFileName(field.getClassPath()), languageType);
                            type = TypeMapUtils.GetTypeByLanuage("Array", languageType) + "<" + tempType + ">";
                        } else {
                            makeModelDefinition(languageType, modelDefinition, cls, suffix, field.getClassPath(), modelPath);
                            type = TypeMapUtils.GetTypeByLanuage("Array", languageType) + "<" + cls + suffix + ">";
                        }
                    } else if (field.getType().equals("Enum")) {

                        type = StringUtils.substringAfterLast(field.getClassPath(), ".");
                        fieldData.setEnum(true);

                    } else {

                        type = TypeMapUtils.GetTypeByLanuage(field.getType(), languageType);
                    }

                    fieldData.setType(type);
                    modelData.addField(fieldData);
                }

                modelDefinition.addModel(modelData);

            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private String getXPageObjectCommentByName(String name) {
        return switch (name) {
            case "page" -> "페이지 수";
            case "size" -> "페이지 크기";
            case "offset" -> "size * page == offset";
            case "sort" -> "정렬 필드 및 방식 설정";
            case "hasNext" -> "다음 페이지 존재 여부";
            case "totalCount" -> "전체 데이터 카운트";
            case "orders" -> "정렬 필드 및 방식 설정";
            case "pageRows" -> "조회된 데이터 목록";
            default -> "";
        };
    }

    private String toNameUpperCase(String name) {
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    private String getClassFileName(String name) {
        String type = StringUtils.substringAfterLast(name, ".");
        return toNameUpperCase(type);
    }

}
