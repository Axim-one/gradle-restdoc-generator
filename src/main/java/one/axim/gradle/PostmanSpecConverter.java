package one.axim.gradle;

import one.axim.gradle.data.*;
import one.axim.gradle.utils.SpringPageSchema;
import one.axim.gradle.utils.XPageSchema;
import one.axim.gradle.generator.LanguageType;
import one.axim.gradle.generator.ModelGenerator;
import one.axim.gradle.generator.data.*;
import one.axim.gradle.generator.utils.TypeMapUtils;
import one.axim.gradle.postman.data.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.gson.Gson;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.Version;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PostmanSpecConverter {

    private final static String PATH_VARIABLE_FIND_PATTERN = "\\{(.*?)\\}";
    private final static String POSTMAN_SPEC_SCHEMA =
            "https://schema.getpostman.com/json/collection/v2.1.0/collection.json";

    private static final Gson gson = new Gson();
    private static final ObjectMapper objectMapper = new ObjectMapper() {{
        configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }};

    private final String documentPath;

    private final String postmanWorkSpaceId;
    private final String postmanApiKey;

    private final ServiceDefinition serviceDefinition;
    private final ArrayList<APIDefinition> apiList;

    private final HashMap<String, ItemData> savedItemData;

    public PostmanSpecConverter(String apiKey, String workSpaceId, ServiceDefinition serviceDefinition, String docPath) {

        this.postmanApiKey = apiKey;
        this.postmanWorkSpaceId = workSpaceId;
        this.serviceDefinition = serviceDefinition;
        this.documentPath = docPath;
        this.apiList = new ArrayList<>();
        this.savedItemData = new HashMap<>();

        System.out.println("Api Definition Json Load .. " + docPath);
        loadApiJson(docPath);
    }

    private void loadApiJson(String docPath) {

        File file = new File(docPath + File.separator + "api");

        File[] files = file.listFiles();
        if (files == null) return;

        for (File json : files) {

            try {
                APIDefinition[] api = gson.fromJson(FileUtils.readFileToString(json, "UTF-8"), APIDefinition[].class);

                if (api != null) {
                    for (APIDefinition apiDefinition : api) {
                        this.apiList.add(apiDefinition);
                    }
                }
            } catch (IOException io) {
                // TODO ::
            }
        }

    }

    public void build() {
        String description = this.serviceDefinition.getIntroduction();

        ResponseEnvData colls = getPostmanCollectionList("https://api.getpostman.com/collections?workspace=" + this.postmanWorkSpaceId, this.serviceDefinition.getName());

        InfoData infoData = new InfoData();

        String collectionId = null;
        if (colls != null) {
            collectionId = colls.getUid();
            infoData.set_postman_id(colls.getId());
        }

        infoData.setName(this.serviceDefinition.getName());
        infoData.setDescriptionBody(description);
        infoData.setSchema(POSTMAN_SPEC_SCHEMA);
        infoData.setVersion(this.serviceDefinition.getVersion());

        System.out.println("infomation data generate complete");

        final AuthData authData = new AuthData();
        if (this.serviceDefinition.getAuth() != null && this.serviceDefinition.getAuth().getType().equals("token")) {

            authData.setType("apikey");
            authData.setApiKey(new ArrayList<>() {{
                add(new AuthKeyData("string", "key", serviceDefinition.getAuth().getHeaderKey()));
                add(new AuthKeyData("string", "value", serviceDefinition.getAuth().getValue()));
            }});
        } // TODO :: JWT, 또는 다른 인증 방식에 대해서도 추가로 정의 해보자

        if (collectionId != null) {

            CollectionResponse collectionResponse = getPostmanCollection("https://api.getpostman.com/collections/" + collectionId);

            if (collectionResponse != null && collectionResponse.getCollection() != null) {

                List<GroupData> groupList = collectionResponse.getCollection().getItem();

                for (GroupData groupData : groupList) {

                    if (groupData.getItem() != null) {
                        for (ItemData itemData : groupData.getItem()) {
                            this.savedItemData.put(itemData.getId(), itemData);
                        }
                    }
                }
            }
        }

        HashMap<String, Object> postmanData = new HashMap<>() {{
            put("info", infoData);
            put("item", generateApiObject());

            if (authData.getType() != null)
                put("auth", authData);
        }};

        System.out.println("api data generate complete");

        if (collectionId != null) {

            String response = postmanUpdateApiRequest("https://api.getpostman.com/collections/" + collectionId,
                    new HashMap<String, Object>() {{
                        put("collection", postmanData);
                    }});

            System.out.println("updated collection " + collectionId + " in workspace " + postmanWorkSpaceId + " result response :: \n" + response);

        } else {

            String response = postmanCreateApiRequest("https://api.getpostman.com/collections?workspace=" + this.postmanWorkSpaceId,
                    new HashMap<String, Object>() {{
                        put("collection", postmanData);
                    }});

            System.out.println("created collection in workspace " + postmanWorkSpaceId + " result response :: \n" + response);
        }

        if (this.serviceDefinition.getEnvVariable() != null) {

            for (ENVVariableDefinition envVariableDefinition : this.serviceDefinition.getEnvVariable()) {

                ResponseEnvData data = getPostmanEnvList("https://api.getpostman.com/environments?workspace=" + this.postmanWorkSpaceId, envVariableDefinition.getName());

                if (data != null) {
                    envVariableDefinition.setPostmanUid(data.getUid());
                }

                EnvironmentData environmentData = new EnvironmentData();
                environmentData.setName(envVariableDefinition.getName());

                if (envVariableDefinition.getVariables() != null) {

                    ArrayList<EnvironmentKeyValueData> keyValueData = new ArrayList<>();

                    for (ENVVariable variable : envVariableDefinition.getVariables()) {

                        EnvironmentKeyValueData value = new EnvironmentKeyValueData();
                        value.setKey(variable.getName());
                        value.setValue(variable.getValue());
                        keyValueData.add(value);
                    }

                    environmentData.setValues(keyValueData);

                    if (envVariableDefinition.getPostmanUid() != null) {

                        postmanUpdateApiRequest("https://api.getpostman.com/environments/" + envVariableDefinition.getPostmanUid(),
                                new HashMap<String, Object>() {{
                                    put("environment", environmentData);
                                }});
                    } else {

                        postmanCreateApiRequest("https://api.getpostman.com/environments?workspace=" + this.postmanWorkSpaceId,
                                new HashMap<String, Object>() {{
                                    put("environment", environmentData);
                                }});
                    }
                }

                System.out.println("update " + environmentData.getName() + " environment in workspace " + postmanWorkSpaceId);
            }
        }

    }

    public List<GroupData> generateApiObject() {


        ArrayList<GroupData> list = new ArrayList<>();
        HashMap<String, ArrayList<ItemData>> items = new HashMap<>();

        for (APIDefinition apiDefinition : this.apiList) {
            ArrayList<ItemData> itemList;

            if (items.containsKey(apiDefinition.getGroup())) {
                itemList = items.get(apiDefinition.getGroup());
                itemList.add(makePostmanItem(apiDefinition));
            } else {
                itemList = new ArrayList<>();
                itemList.add(makePostmanItem(apiDefinition));
                items.put(apiDefinition.getGroup(), itemList);
            }
        }

        for (String key : items.keySet()) {

            GroupData groupData = new GroupData(key);
            groupData.setItem(items.get(key));
            groupData.setId(getGroupObjectId(this.serviceDefinition, key));

            list.add(groupData);
        }

        return list;
    }

    public ItemData makePostmanItem(APIDefinition apiDefinition) {

        ItemData item = new ItemData();

        String id = getApiObjectId(apiDefinition);

        item.setId(id);
        item.setName(apiDefinition.getName());

        RequestData requestData = getApiRequestData(apiDefinition);

        if (this.savedItemData.containsKey(id)) {
            mergePostItemRequestData(requestData, this.savedItemData.get(id).getRequest());
        }

        item.setRequest(requestData);

        if (this.savedItemData.containsKey(id)) {
            if (this.savedItemData.get(id).getResponse() != null) {
                for (ResponseData responseData : this.savedItemData.get(id).getResponse()) {

                    item.addResponse(responseData);
                }
            }
        } else {
            item.addResponse(getApiResponseData(apiDefinition, id));
        }

        return item;
    }

    private void mergePostItemRequestData(RequestData newData, RequestData oldData) {

        // Header
        if (newData.getHeader() != null && oldData.getHeader() != null) {
            for (HeaderData headerData : newData.getHeader()) {
                for (HeaderData data : oldData.getHeader()) {
                    if (headerData.getKey().equals(data.getKey())) {
                        headerData.setValue(data.getValue());
                        break;
                    }
                }
            }
        }

        // URL QUERY PARAM
        if (newData.getUrl().getQuery() != null && oldData.getUrl().getQuery() != null) {
            for (QueryData queryData : newData.getUrl().getQuery()) {
                for (QueryData data : oldData.getUrl().getQuery()) {
                    if (queryData.getKey().equals(data.getKey())) {
                        queryData.setValue(data.getValue());
                        break;
                    }
                }
            }
        }

        // Path Variables ..
        if (newData.getUrl().getVariable() != null && oldData.getUrl().getVariable() != null) {
            for (PathVariableData pathVariableData : newData.getUrl().getVariable()) {
                for (PathVariableData variableData : oldData.getUrl().getVariable()) {
                    if (pathVariableData.getKey().equals(variableData.getKey())) {
                        pathVariableData.setValue(variableData.getValue());
                        break;
                    }
                }
            }
        }

        // Request Body
        if (newData.getBody() != null && oldData.getBody() != null) {
            if (newData.getBody().getRaw() != null) {
                try {

                    Map newJsonData = objectMapper.readValue(newData.getBody().getRaw(), Map.class);
                    Map oldJsonData = objectMapper.readValue(oldData.getBody().getRaw(), Map.class);

                    oldJsonData.forEach(new BiConsumer() {
                        @Override
                        public void accept(Object o, Object o2) {

                            if (newJsonData.containsKey(o)) {
                                newJsonData.put(o, o2);
                            }
                        }
                    });

                    newData.getBody().setRaw(objectMapper.writeValueAsString(newJsonData));

                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String getApiDescriptionData(APIDefinition apiDefinition) {

        try {

            ModelGenerator generator = new ModelGenerator();
            ModelDefinition requestModelObject =
                    generator.generatorRequestModelObject(apiDefinition, this.documentPath + File.separator + "model",
                            LanguageType.JAVA);

            requestModelObject.setType("Request");
            String request = getAPIModelMarkdown(requestModelObject);

            ModelDefinition responseModelObject =
                    generator.generatorResponseModelObject(apiDefinition, this.documentPath + File.separator + "model",
                            LanguageType.JAVA);

            responseModelObject.setType("Response");
            String response = getAPIModelMarkdown(responseModelObject);

            return (request == null ? "" : request) + (response == null ? "" : response);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private Configuration createFreeMarkerConfig() {
        Configuration cfg = new Configuration(new Version("2.3.31"));
        cfg.setClassLoaderForTemplateLoading(Thread.currentThread().getContextClassLoader(), "template");
        cfg.setIncompatibleImprovements(new Version(2, 3, 31));
        cfg.setDefaultEncoding("UTF-8");
        return cfg;
    }

    private String buildEnumComment(String classPath) {
        APIModelDefinition model = loadModel(classPath);

        if (model != null) {
            return String.join("<br/> ", model.getFields().stream().map(apiField1 -> {
                return apiField1.getName() + ifStringPrefix(apiField1.getDescription(), " : ");
            }).collect(Collectors.toList()));
        } else {
            System.out.println("not found enum model class " + classPath);
            return "";
        }
    }

    private String getAPIParameterMarkdown(ParameterDefinition parameterDefinition) {

        if (parameterDefinition == null || parameterDefinition.getParameters().isEmpty()) {
            return null;
        }

        String output = null;

        Configuration cfg = createFreeMarkerConfig();

        for (ParameterData parameter : parameterDefinition.getParameters()) {

            if (parameter.getIsEnum()) {
                parameter.setEnumTypeComment(buildEnumComment(parameter.getClassPath()));
            } else {
                parameter.setEnumTypeComment("");
            }
        }

        try {
            Template template = cfg.getTemplate("postmanParameterMarkdown.template");

            try (StringWriter out = new StringWriter()) {

                template.process(parameterDefinition, out);

                output = out.toString();

                out.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return output;
    }

    private String getAPIModelMarkdown(ModelDefinition modelDefinition) {
        if (modelDefinition == null || modelDefinition.getModels().isEmpty()) {
            return null;
        }

        if (modelDefinition.getModels() != null) {

            for (ModelData modelData : modelDefinition.getModels()) {

                for (FieldData field : modelData.getFields()) {

                    if (field.isEnum()) {
                        field.setEnumTypeComment(buildEnumComment(field.getClassPath()));
                    } else {
                        field.setEnumTypeComment("");
                    }
                }
            }
        }

        String output = null;

        Configuration cfg = createFreeMarkerConfig();

        try {
            Template template = cfg.getTemplate("postmanBodyMarkdown.template");

            try (StringWriter out = new StringWriter()) {

                template.process(modelDefinition, out);

                output = out.toString();

                out.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return output;
    }

    private RequestData getApiRequestData(APIDefinition apiDefinition) {

        apiDefinition.setUrlMapping(replacePath(apiDefinition.getUrlMapping()));
        RequestData requestData = new RequestData();

        UrlData urlData = new UrlData();

        urlData.setRaw(getRawUrl(apiDefinition));
        urlData.setHost(new String[]{getHost()});
        urlData.setPath(getUrlPaths(apiDefinition));
        urlData.setVariable(getPathVariable(apiDefinition));
        urlData.setQuery(getQueryStringParameter(apiDefinition));

        requestData.setUrl(urlData);
        requestData.setHeader(getHeaders(apiDefinition));
        requestData.setBody(getBody(apiDefinition));
        requestData.setMethod(apiDefinition.getMethod());

        String parameterMarkdown = "";

        if (urlData.getVariable() != null) {
            ParameterDefinition pathParams = new ParameterDefinition();
            pathParams.setType("Path Parameters");

            for (PathVariableData variableData : urlData.getVariable()) {

                ParameterData parameterData = new ParameterData();
                parameterData.setName(variableData.getName());
                parameterData.setComment(StringUtils.replace(variableData.getDescription(), "|", "\\|"));
                parameterData.setOptional(variableData.isOptional());
                parameterData.setType(variableData.getOriginType());
                parameterData.setClassPath(variableData.getClassPath());
                parameterData.setEnum(variableData.isEnum());

                pathParams.addParameter(parameterData);
            }

            parameterMarkdown += getAPIParameterMarkdown(pathParams);
        }

        if (urlData.getQuery() != null) {
            ParameterDefinition pathParams = new ParameterDefinition();
            pathParams.setType("Query Parameters");

            for (QueryData queryData : urlData.getQuery()) {

                ParameterData parameterData = new ParameterData();
                parameterData.setName(queryData.getKey());
                parameterData.setComment(StringUtils.replace(queryData.getDescription(), "|", "\\|"));
                parameterData.setOptional(queryData.isOptional());
                parameterData.setType(queryData.getOriginType());
                parameterData.setClassPath(queryData.getClassPath());
                parameterData.setEnum(queryData.isEnum());

                pathParams.addParameter(parameterData);
            }

            parameterMarkdown += getAPIParameterMarkdown(pathParams);
        }

        requestData.setDescription(parameterMarkdown + getApiDescriptionData(apiDefinition));

        return requestData;
    }

    private ResponseData getApiResponseData(APIDefinition apiDefinition, String id) {

        ResponseData responseData = new ResponseData();
        responseData.setName("success response");

        for (String key : apiDefinition.getResponseStatus().keySet()) {
            responseData.setCode(Integer.valueOf(key));
            responseData.setStatus(apiDefinition.getResponseStatus().get(key));
        }

        if (apiDefinition.getReturnClass() != null && !apiDefinition.getReturnClass().toLowerCase().equals("void")) {

            String pagingType = apiDefinition.getPagingType();
            if (pagingType == null && apiDefinition.getIsPaging()) {
                pagingType = PagingType.XPAGE;
            }
            Object body = makeModel(loadModel(apiDefinition.getReturnClass()), pagingType);

            try {
                responseData.setBody(objectMapper.writeValueAsString(body));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        // TODO :: 우선 REQUEST DATA 만 merge 하고 나머지 response 같은건 생각 좀 해 보자
        RequestData requestData = getApiRequestData(apiDefinition);

        if (this.savedItemData.containsKey(id)) {
            mergePostItemRequestData(requestData, this.savedItemData.get(id).getRequest());
        }

        responseData.setOriginalRequest(requestData);

        return responseData;
    }

    private String getApiObjectId(APIDefinition definition) {

        String key = definition.getUrlMapping() + definition.getName() + definition.getMethod();

        return UUID.nameUUIDFromBytes(key.getBytes()).toString();
    }

    private String getGroupObjectId(ServiceDefinition definition, String groupName) {

        String key = definition.getServiceId() + groupName;

        return UUID.nameUUIDFromBytes(key.getBytes()).toString();
    }

    private String getHost() {
        return this.serviceDefinition.getApiServerUrl();
    }

    private String getRawUrl(APIDefinition definition) {

        return getHost() + "/" + serviceDefinition.getVersion() + replacePath(definition.getUrlMapping());
    }

    private ArrayList<PathVariableData> getPathVariable(APIDefinition apiDefinition) {

        List<APIParameter> apiParameters = apiDefinition.getParameters();

        ArrayList<PathVariableData> variables = new ArrayList<PathVariableData>();
        for (APIParameter apiParameter : apiParameters) {

            if (apiParameter.getParameterKind().equals(APIParameterKind.URL_PATH)) {

                PathVariableData variableData = new PathVariableData();
                variableData.setName(apiParameter.getName());
                variableData.setKey(apiParameter.getName());
                variableData.setDescription(apiParameter.getDescription());

                if (apiParameter.getIsEnum() || apiParameter.getType().equals("Enum")) {
                    variableData.setType("string");
                    variableData.setDescription(variableData.getDescription());
                    variableData.setEnum(true);
                } else {
                    variableData.setType(TypeMapUtils.GetTypeByLanuage(apiParameter.getType(), LanguageType.POSTMAN));
                    variableData.setEnum(false);
                }

                variableData.setClassPath(apiParameter.getClassPath());
                variableData.setOriginType(apiParameter.getType());
                variableData.setOptional(apiParameter.getIsOptional());
                variables.add(variableData);
            }
        }

        if (!variables.isEmpty()) {
            return variables;
        }

        return null;
    }

    private ArrayList<QueryData> getQueryStringParameter(APIDefinition apiDefinition) {
        List<APIParameter> apiParameters = apiDefinition.getParameters();

        ArrayList<QueryData> queryParams = new ArrayList<QueryData>();
        for (APIParameter apiParameter : apiParameters) {

            if (apiParameter.getParameterKind().equals(APIParameterKind.REQUEST_PARAMETER)) {

                if (apiParameter.getClassPath().equals("one.axim.framework.core.data.XPageNation")) {

                    QueryData queryData = new QueryData();
                    queryData.setKey("size");
                    queryData.setDescription("페이지당 데이터 크기");
                    queryData.setType(TypeMapUtils.GetTypeByLanuage("int", LanguageType.POSTMAN));
                    queryData.setOptional(true);
                    queryData.setDefaultValue("10");
                    queryData.setEnum(false);
                    queryData.setOriginType("Integer");
                    queryParams.add(queryData);

                    QueryData queryData1 = new QueryData();
                    queryData1.setKey("page");
                    queryData1.setDescription("조회 하고자 하는 페이지 넘버");
                    queryData1.setType(TypeMapUtils.GetTypeByLanuage("int", LanguageType.POSTMAN));
                    queryData1.setOptional(true);
                    queryData1.setDefaultValue("1");
                    queryData1.setEnum(false);
                    queryData1.setOriginType("Integer");
                    queryParams.add(queryData1);

                } else {

                    QueryData queryData = new QueryData();
                    queryData.setKey(apiParameter.getName());
                    queryData.setDescription(StringUtils.replace(apiParameter.getDescription(), "|", "\\|"));


                    if (apiParameter.getIsEnum() || apiParameter.getType().equals("Enum")) {
                        queryData.setType("string");
                        queryData.setDescription(
                                queryData.getDescription()
                        );
                        queryData.setEnum(true);
                    } else {
                        queryData.setType(TypeMapUtils.GetTypeByLanuage(apiParameter.getType(), LanguageType.POSTMAN));
                        queryData.setEnum(false);
                    }

                    queryData.setClassPath(apiParameter.getClassPath());
                    queryData.setOriginType(apiParameter.getType());
                    queryData.setDefaultValue(apiParameter.getDefaultValue());
                    queryData.setOptional(apiParameter.getIsOptional());

                    queryParams.add(queryData);
                }
            }
        }

        if (!queryParams.isEmpty()) {
            return queryParams;
        }

        return null;
    }

    private ArrayList<HeaderData> getHeaders(APIDefinition apiDefinition) {

        List<APIHeader> apiHeaders = apiDefinition.getHearders();
        ArrayList<HeaderData> headers = new ArrayList<>();
        if (apiHeaders != null && !apiHeaders.isEmpty()) {

            for (APIHeader apiHeader : apiHeaders) {

                HeaderData headerData = new HeaderData();

                headerData.setKey(apiHeader.getName());
                headerData.setDescription(apiHeader.getDescription());
                headerData.setValue(apiHeader.getDefaultValue());

                headers.add(headerData);
            }
        }

        if (serviceDefinition.getHeaders() != null) {
            for (APIHeader header : serviceDefinition.getHeaders()) {
                HeaderData headerData = new HeaderData();

                headerData.setKey(header.getName());
                headerData.setDescription(header.getDescription());
                headerData.setValue(header.getDefaultValue());

                headers.add(headerData);
            }
        }

        if (!headers.isEmpty()) {
            return headers;
        }

        return null;
    }

    private BodyData getBody(APIDefinition apiDefinition) {

        BodyData bodyData = new BodyData();
        if (apiDefinition.getParameters() != null) {

            for (APIParameter parameter : apiDefinition.getParameters()) {

                if (parameter.getParameterKind().equals(APIParameterKind.REQUEST_BODY)) {

                    Object body = makeModel(loadModel(parameter.getClassPath()));

                    try {
                        bodyData.setRaw(objectMapper.writeValueAsString(body));
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                    bodyData.setMode("raw");
                    bodyData.setOptions(new HashMap<>() {{
                        put("raw", new HashMap<>() {{ put("language", "json"); }});
                    }});

                    return bodyData;
                }
            }
        }

        return null;
    }

    private String replacePath(String path) {

        String replacePath = path;
        Pattern pattern = Pattern.compile(PATH_VARIABLE_FIND_PATTERN);
        Matcher matcher = pattern.matcher(path);

        //    if (matcher.matches()) {

        while (matcher.find()) {

            String newPath = ":" + matcher.group(1);
            replacePath = replacePath.replace(matcher.group(0), newPath);
        }
        //    }

        return replacePath;
    }

    private String[] getUrlPaths(APIDefinition apiDefinition) {

        String[] paths = StringUtils.splitPreserveAllTokens(apiDefinition.getUrlMapping(), "/");

        paths[0] = serviceDefinition.getVersion();

        return paths;
    }

    private HashMap<String, Object> makeModel(APIModelDefinition modelDefinition) {
        return makeModel(modelDefinition, (String) null);
    }

    private HashMap<String, Object> makeModel(APIModelDefinition modelDefinition, String pagingType) {

        HashMap<String, Object> reqObj = new HashMap<>();

        if (PagingType.XPAGE.equals(pagingType)) {

            Field[] fields = XPageSchema.class.getDeclaredFields();
            for (Field field : fields) {
                if (field.getName().equals("pageRows")) {
                    ArrayList<Object> obj = new ArrayList<>();
                    obj.add(makeModel(modelDefinition));
                    reqObj.put(field.getName(), obj);
                } else {
                    reqObj.put(field.getName(), field.getType().getTypeName());
                }
            }

        } else if (PagingType.SPRING.equals(pagingType)) {

            Field[] fields = SpringPageSchema.class.getDeclaredFields();
            for (Field field : fields) {
                if (field.getName().equals("content")) {
                    ArrayList<Object> obj = new ArrayList<>();
                    obj.add(makeModel(modelDefinition));
                    reqObj.put(field.getName(), obj);
                } else {
                    reqObj.put(field.getName(), field.getType().getTypeName());
                }
            }

            // sort 중첩 객체
            HashMap<String, Object> sortObj = new HashMap<>();
            sortObj.put("sorted", "boolean");
            sortObj.put("unsorted", "boolean");
            sortObj.put("empty", "boolean");
            reqObj.put("sort", sortObj);

        } else {
            if (modelDefinition != null && modelDefinition.getFields() != null) {
                for (APIField apiField : modelDefinition.getFields()) {
                    if (apiField.getType().equals("Object")) {
                        APIModelDefinition model = loadModel(apiField.getClassPath());
                        reqObj.put(apiField.getName(), makeModel(model));
                    } else if (apiField.getType().equals("Array")) {
                        APIModelDefinition model = loadModel(apiField.getClassPath());

                        if (model != null) {
                            ArrayList<Object> obj = new ArrayList<>();

                            if (modelDefinition.getType().equals(model.getType())) {
                                // same class

                                HashMap<String, String> map = new HashMap<>();
                                for (APIField field : model.getFields()) {
                                    map.put(field.getName(), field.getClassPath());
                                }

                                obj.add(map);
                            } else {
                                obj.add(makeModel(model));
                            }

                            reqObj.put(apiField.getName(), obj);
                        }

                    } else if (apiField.getType().equals("Enum")) {
                        APIModelDefinition model = loadModel(apiField.getClassPath());
                        reqObj.put(apiField.getName(), String.join(" | ", model.getFields().stream().map(apiField1 -> {
                            return apiField1.getName();
                        }).collect(Collectors.toList())));
                    } else {
                        reqObj.put(apiField.getName(), apiField.getClassPath());
                    }
                }
            }
        }
        return reqObj;
    }

    private APIModelDefinition loadModel(String path) {
        try {

            File modelFile = new File(this.documentPath + File.separator + "model" + File.separator + path + ".json");

            APIModelDefinition apiModel =
                    gson.fromJson(FileUtils.readFileToString(modelFile, "UTF-8"), APIModelDefinition.class);
            return apiModel;
        } catch (IOException io) {
        }

        return null;
    }

    private <T> ResponseEnvData findPostmanResourceByName(String url, Class<T> responseType,
                                                          Function<T, List<ResponseEnvData>> extractor, String name) {
        try {
            Response response = Request.Get(url).addHeader("X-API-Key", this.postmanApiKey).execute();

            T res = objectMapper.readValue(response.returnContent().asString(), responseType);

            if (res != null) {
                List<ResponseEnvData> items = extractor.apply(res);
                if (items != null) {
                    for (ResponseEnvData item : items) {
                        if (name.equals(item.getName())) {
                            return item;
                        }
                    }
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    private ResponseEnvData getPostmanEnvList(String url, String name) {
        return findPostmanResourceByName(url, ResponsePostmanEnvData.class,
                ResponsePostmanEnvData::getEnvironments, name);
    }

    private ResponseEnvData getPostmanCollectionList(String url, String name) {
        return findPostmanResourceByName(url, ResponsePostmanCollectionData.class,
                ResponsePostmanCollectionData::getCollections, name);
    }

    private String postmanCreateApiRequest(String url, Object object) {
        String res = null;
        try {

            System.out.println(url);

            String json = null;
            try {
                json = objectMapper.writeValueAsString(object);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

            System.out.println(json);

            Response response = Request.Post(url).addHeader("X-API-Key", this.postmanApiKey).bodyString(json, ContentType.APPLICATION_JSON).execute();
            res = response.returnContent().asString();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return res;
    }

    private String postmanUpdateApiRequest(String url, Object object) {

        String json = null;
        try {
            json = objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            System.out.println("json write exception ... ");
            e.printStackTrace();
        }

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            org.apache.http.client.methods.HttpPut httpPut = new org.apache.http.client.methods.HttpPut(url);
            httpPut.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            httpPut.addHeader("X-API-Key", this.postmanApiKey);

            if (json != null)
                httpPut.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

            CloseableHttpResponse httpresponse = client.execute(httpPut);

            StringBuilder result = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(httpresponse.getEntity().getContent(), "utf-8"))) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    result.append(inputLine);
                }
            }

            return result.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public CollectionResponse getPostmanCollection(String url) {

        try {
            Response response = Request.Get(url).addHeader("X-API-Key", this.postmanApiKey).execute();

            ObjectMapper collectionMapper = JsonMapper
                    .builder()
                    .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
                    .serializationInclusion(JsonInclude.Include.NON_NULL).build();

            String json = response.returnContent().asString();

            return collectionMapper.readValue(json, CollectionResponse.class);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private String ifStringPrefix(String value, String withPrefix) {

        if (value == null) {
            return "";
        } else {
            return withPrefix + value;
        }
    }
}
